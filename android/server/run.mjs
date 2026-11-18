import { randomBytes, timingSafeEqual, X509Certificate } from "node:crypto";
import { execFileSync, spawn } from "node:child_process";
import {
  existsSync, linkSync, mkdirSync, mkdtempSync, readFileSync, renameSync, rmSync, statSync, writeFileSync,
} from "node:fs";
import { isIPv4 } from "node:net";
import { homedir } from "node:os";
import { join, resolve } from "node:path";
import { createServer } from "node:tls";
import { parseArgs } from "node:util";
import { createProvider, ProviderError } from "./provider.mjs";

const VERSION = 1;
const MAX_LINE_BYTES = 64 * 1024;
const DELTA_BYTES = 8 * 1024;
const IDLE_MS = 20 * 60_000;
const SEND_WINDOW_MS = 10 * 60_000;
const MAX_SENDS = 12;
const MAX_INPUT_CHARS = 4000;
const MAX_ANSWER_CHARS = 40_000; // ChatSession.MAX_TEXT on Android.
const PILL = "ai_deepseek";

const errors = {
  auth: "The AI provider rejected the server's credentials.",
  busy: "An answer is already running.",
  canceled: "The answer was canceled.",
  empty: "Write a message first.",
  internal: "Something went wrong on the server.",
  no_key: "The server has no AI provider key.",
  not_allowed: "This model is not available on the server.",
  provider: "The AI provider could not finish the answer.",
  rate: "Too many requests. Try again in a few minutes.",
  too_long: "This message is too long.",
  unreachable: "The server could not reach the AI provider.",
};

function isLocal(address) {
  if (!isIPv4(address)) return false;
  const [a, b] = address.split(".").map(Number);
  return a === 10 || a === 127 || (a === 192 && b === 168) || (a === 172 && b >= 16 && b <= 31) ||
    (a === 169 && b === 254) || (a === 100 && b >= 64 && b <= 127);
}

function privateWrite(file, text, replace = true) {
  const temporary = `${file}.${randomBytes(8).toString("hex")}.tmp`;
  try {
    writeFileSync(temporary, text, { flag: "wx", mode: 0o600, flush: true });
    if (replace) renameSync(temporary, file);
    else linkSync(temporary, file);
  } finally {
    rmSync(temporary, { force: true });
  }
}

function loadIdentity(dir) {
  const file = join(dir, "identity.json");
  if (existsSync(file)) return JSON.parse(readFileSync(file, "utf8"));

  const work = mkdtempSync(join(dir, "tls-"));
  try {
    execFileSync("openssl", [
      "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "3650",
      "-subj", "/CN=Coumpanion Server", "-keyout", join(work, "key.pem"), "-out", join(work, "cert.pem"),
    ], { stdio: "ignore" });
    const identity = {
      key: readFileSync(join(work, "key.pem"), "utf8"),
      cert: readFileSync(join(work, "cert.pem"), "utf8"),
      token: randomBytes(32).toString("base64url"),
    };
    try {
      privateWrite(file, JSON.stringify(identity), false);
      return identity;
    } catch (error) {
      if (error.code !== "EEXIST") throw error;
      return JSON.parse(readFileSync(file, "utf8"));
    }
  } finally {
    rmSync(work, { recursive: true, force: true });
  }
}

function sessionStore(dir, agent) {
  const file = join(dir, "session.json");
  let session = existsSync(file) ? JSON.parse(readFileSync(file, "utf8")) : null;

  function save(state, statusText, stepIndex) {
    const next = { pillId: PILL, agent, state, statusText, stepIndex, stepCount: 2, updatedAt: Date.now() };
    privateWrite(file, JSON.stringify(next) + "\n");
    session = next;
  }

  function recover() {
    if (session?.state === "working") save("error", `Interrupted at step ${session.stepIndex}`, session.stepIndex);
  }

  return { save, recover, snapshot: () => session ? [session] : [] };
}

function readLines(socket, handle) {
  let pending = Buffer.alloc(0);
  const decoder = new TextDecoder("utf-8", { fatal: true });
  socket.on("data", chunk => {
    pending = pending.length ? Buffer.concat([pending, chunk]) : chunk;
    let newline;
    while (!socket.destroyed && (newline = pending.indexOf(10)) >= 0) {
      if (!newline || newline > MAX_LINE_BYTES) return socket.destroy();
      const line = pending.subarray(0, newline);
      pending = pending.subarray(newline + 1);
      try {
        const message = JSON.parse(decoder.decode(line));
        if (!message || typeof message.type !== "string") return socket.destroy();
        handle(message);
      } catch {
        return socket.destroy();
      }
    }
    if (pending.length > MAX_LINE_BYTES) socket.destroy();
  });
}

function* chatDeltas(answer) {
  let text = "";
  let bytes = 0;
  for (const character of answer) {
    const length = Buffer.byteLength(character);
    if (bytes + length > DELTA_BYTES) {
      yield text;
      text = "";
      bytes = 0;
    }
    text += character;
    bytes += length;
  }
  if (text) yield text;
}

function listen({ identity, store, host, port, provider }) {
  const { key, cert, token } = identity;
  const secret = Buffer.from(token, "utf8");
  const pending = new Map();
  const knownAddresses = new Set();
  const sentAt = [];
  let currentPhone;
  let running;
  let history = [];

  function broadcast() {
    currentPhone?.send({ type: "sessions", sessions: store.snapshot() });
  }

  function progress(state, statusText, index) {
    store.save(state, statusText, index);
    broadcast();
  }

  const sendError = (phone, id, reason) => phone.send({ type: "chatError", id, reason, message: errors[reason] });

  function cancel(phone, reply) {
    if (running?.phone !== phone) return;
    const task = running;
    running = undefined;
    task.controller.abort();
    progress("error", reply ? "Canceled at step 1" : "Interrupted at step 1", 1);
    if (reply) sendError(phone, task.id, "canceled");
  }

  async function startTask(phone, { id, model, text }) {
    if (typeof id !== "string" || !/^[A-Za-z0-9_-]{1,64}$/.test(id)) return;
    if (model !== provider.model.id) return sendError(phone, id, "not_allowed");
    if (typeof text !== "string" || !text.trim()) return sendError(phone, id, "empty");
    if ([...text].length > MAX_INPUT_CHARS) return sendError(phone, id, "too_long");
    if (running) return sendError(phone, id, "busy");
    const now = Date.now();
    while (sentAt.length && now - sentAt[0] >= SEND_WINDOW_MS) sentAt.shift();
    if (sentAt.length >= MAX_SENDS) return sendError(phone, id, "rate");
    const task = { phone, id, controller: new AbortController() };
    const conversation = [...history, { role: "user", content: text.trim() }];
    running = task;
    sentAt.push(now);
    try {
      progress("working", "Waiting for the AI provider", 1);
      const answer = await provider.answer(conversation, task.controller.signal);
      if (running !== task) return;
      // A result the phone would truncate is an explicit failure rather than a partial success.
      if (answer.length > MAX_ANSWER_CHARS) throw new ProviderError("provider");
      progress("finished", "Answer ready in Chat", 2);
      for (const part of chatDeltas(answer)) phone.send({ type: "chatDelta", id, text: part });
      phone.send({ type: "chatDone", id });
      history = [...conversation, { role: "assistant", content: answer }].slice(-24);
    } catch (error) {
      if (running !== task) return;
      const reason = error instanceof ProviderError && Object.hasOwn(errors, error.reason) ? error.reason : "internal";
      try {
        progress("error", errors[reason], 1);
      } catch {
        phone.socket.destroy();
        return;
      }
      sendError(phone, id, reason);
    } finally {
      if (running === task) running = undefined;
    }
  }

  const server = createServer({ key, cert, minVersion: "TLSv1.2", handshakeTimeout: 10_000 });
  const peer = socket => `${socket.remoteAddress}:${socket.remotePort}`;

  function release(peerId) {
    clearTimeout(pending.get(peerId)?.timer);
    pending.delete(peerId);
  }

  server.maxConnections = 8;
  server.prependListener("connection", socket => {
    const address = socket.remoteAddress;
    const halfOpen = [...pending.values()];
    const stranger = !knownAddresses.has(address);
    if (!isLocal(address) || halfOpen.filter(p => p.address === address).length >= 2 ||
        (stranger && halfOpen.filter(p => !knownAddresses.has(p.address)).length >= 4)) {
      socket.destroy();
      return;
    }
    const peerId = peer(socket);
    const timer = setTimeout(() => socket.destroy(), pending.size >= 3 ? 3_000 : 10_000);
    timer.unref();
    pending.set(peerId, { address, timer });
    socket.once("close", () => release(peerId));
  });

  server.on("secureConnection", socket => {
    const peerId = peer(socket);
    if (!pending.has(peerId)) return socket.destroy();
    socket.setNoDelay(true);
    const phone = {
      socket,
      chat: false,
      send(message) {
        if (!socket.writable || socket.writableLength > 4 * MAX_LINE_BYTES) {
          socket.destroy();
          return;
        }
        socket.write(JSON.stringify(message) + "\n");
      },
    };
    let rejected = false;
    readLines(socket, message => {
      if (rejected) return;
      if (phone !== currentPhone) {
        const offered = Buffer.from(typeof message.token === "string" ? message.token : "", "utf8");
        if (message.type !== "hello" || message.v !== VERSION || offered.length !== secret.length ||
            !timingSafeEqual(offered, secret)) {
          rejected = true;
          setTimeout(() => socket.end(JSON.stringify({
            type: "error", code: "auth", message: "Pairing was rejected.",
          }) + "\n"), 250).unref();
          return;
        }
        release(peerId);
        if (knownAddresses.size >= 8 && !knownAddresses.has(socket.remoteAddress)) {
          knownAddresses.delete(knownAddresses.values().next().value);
        }
        knownAddresses.add(socket.remoteAddress);
        const old = currentPhone;
        if (old) {
          cancel(old, false);
          old.socket.destroy();
        }
        currentPhone = phone;
        phone.chat = Array.isArray(message.caps) && message.caps.includes("chat");
        socket.setTimeout(IDLE_MS, () => socket.destroy());
        phone.send({ type: "welcome", v: VERSION, desktop: "Coumpanion Server", os: process.platform, caps: phone.chat ? ["chat"] : [] });
        broadcast();
        return;
      }

      if (message.type === "ping") return phone.send({ type: "pong" });
      if (message.type === "bye") return socket.end();
      if (!phone.chat) return;
      switch (message.type) {
        case "chatModels": return phone.send({ type: "chatModels", models: [provider.model] });
        case "chatSend": return void startTask(phone, message);
        case "chatCancel":
          if (running?.id === message.id) cancel(phone, true);
          break;
        case "chatReset":
          cancel(phone, true);
          history = [];
          break;
      }
    });
    socket.on("error", () => {});
    socket.on("close", () => {
      if (currentPhone !== phone) return;
      currentPhone = undefined;
      try {
        cancel(phone, false);
      } catch {
        console.error("Could not save the interrupted task.");
      }
    });
  });

  return new Promise((ready, fail) => {
    server.once("error", fail);
    server.listen(port, host, () => ready(server));
  });
}

async function main() {
  const { values } = parseArgs({ options: {
    host: { type: "string", default: "127.0.0.1" },
    port: { type: "string", default: "47821" },
    "state-dir": { type: "string", default: join(homedir(), ".local", "state", "coumpanion") },
    fake: { type: "boolean", default: false },
  } });
  const host = values.host;
  const port = Number(values.port);
  if (!isLocal(host) || !/^\d+$/.test(values.port) || port > 65535) {
    throw new Error("Use a local IPv4 address and a port from 0 to 65535.");
  }
  if (!values.fake && !process.env.DEEPSEEK_API_KEY?.trim()) {
    throw new Error("Set DEEPSEEK_API_KEY on the server, or use --fake for a local test.");
  }

  const dir = resolve(values["state-dir"]);
  mkdirSync(dir, { recursive: true, mode: 0o700 });
  if ((statSync(dir).mode & 0o077) !== 0) throw new Error("The server state directory must be private (chmod 700).");

  const identity = loadIdentity(dir);
  const agent = values.fake ? "DeepSeek (fake test)" : "DeepSeek";
  const provider = createProvider({ apiKey: process.env.DEEPSEEK_API_KEY, fake: values.fake });
  const store = sessionStore(dir, agent);
  const server = await listen({ identity, host, port, provider, store });
  store.recover();
  const realPort = server.address().port;
  const fp = new X509Certificate(identity.cert).fingerprint256.replaceAll(":", "").toLowerCase();
  const params = new URLSearchParams({
    v: String(VERSION), host, port: String(realPort), fp, token: identity.token, name: "Coumpanion Server",
  });
  const linkPath = join(dir, "pairing.txt");
  privateWrite(linkPath, `coucou://pair?${params}\n`);
  console.log(JSON.stringify({ listening: realPort, pairingFile: linkPath, fake: values.fake }));

  let discovery;
  if (!host.startsWith("127.")) {
    discovery = spawn("avahi-publish-service", ["Coumpanion Server", "_coucou._tcp", String(realPort), "v=1", `fp=${fp.slice(0, 16)}`], { stdio: "ignore" });
    discovery.on("error", () => console.error("mDNS unavailable. Pair or reconnect using the server address."));
  }
  process.once("exit", () => discovery?.kill());
  for (const signal of ["SIGINT", "SIGTERM"]) process.once(signal, () => process.exit(0));
}

main().catch(error => {
  // Keep secrets, paths from the provider, and error bodies out of stdout and stderr.
  if (error.code === "EADDRINUSE") console.error("The server address and port are already in use.");
  else if (error.code === "EADDRNOTAVAIL") console.error("The local IPv4 address is not assigned to this server.");
  else if (/^(Use a local IPv4|Set DEEPSEEK_API_KEY|The server state directory)/.test(error.message)) console.error(error.message);
  else console.error("Coumpanion Server could not start. Check the state directory and OpenSSL installation.");
  process.exit(1);
});
