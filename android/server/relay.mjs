import { createHash } from "node:crypto";
import { createInterface } from "node:readline";
import { connect } from "node:tls";
import WebSocket from "ws";
import { acceptInit, isInit, joinProof, MAX_RELAY_FRAME_BYTES } from "./relay-crypto.mjs";

const RETRY_MS = [1_000, 2_000, 4_000, 8_000, 16_000, 30_000];
const HEARTBEAT_MS = 20_000;
const MAX_BUFFERED_BYTES = 4 * MAX_RELAY_FRAME_BYTES;

/**
 * Decrypted v1 lines enter the same pinned local TLS listener as LAN connections.
 * The relay never receives the TLS certificate key, the pairing token or the provider key.
 */
export function startRelay({ url, room, key, access, host, port, cert, fingerprint }) {
  const roomUrl = new URL(`/v1/room/${room}?role=pc`, url);
  let client;
  let retry;
  let delayIndex = 0;
  let stopping = false;
  let previousStatus;

  function status(message) {
    if (message !== previousStatus) console.error(message);
    previousStatus = message;
  }

  function join() {
    const ws = new WebSocket(roomUrl, ["coucou.v1", `coucou.join.${joinProof(key)}`], {
      headers: { Authorization: `Bearer ${access}` },
      followRedirects: false,
      perMessageDeflate: false,
      maxPayload: MAX_RELAY_FRAME_BYTES,
      handshakeTimeout: 10_000,
    });
    client = ws;
    let live;
    let bad = 0;
    let quietUntil = 0;
    let lastHeard = Date.now();
    let heartbeat;
    let upgradeRejected = false;

    function endChannel() {
      const old = live;
      live = undefined;
      old?.socket?.destroy();
    }

    function badHandshake() {
      if (++bad >= 3) {
        bad = 0;
        quietUntil = Date.now() + 10_000;
      }
    }

    function send(message, binary = true) {
      if (ws.readyState !== WebSocket.OPEN || ws.bufferedAmount > MAX_BUFFERED_BYTES) {
        ws.terminate();
        return;
      }
      ws.send(message, { binary }, error => { if (error) ws.terminate(); });
    }

    function bridge(conversation, line) {
      if (!conversation.socket) {
        const socket = connect({
          host, port, ca: cert, minVersion: "TLSv1.2",
          checkServerIdentity: (_, certificate) => {
            const actual = createHash("sha256").update(certificate.raw).digest("hex");
            if (actual !== fingerprint) return new Error("local server certificate changed");
          },
        });
        conversation.socket = socket;
        const output = createInterface({ input: socket, crlfDelay: Infinity, terminal: false });
        output.on("line", text => {
          if (live !== conversation) return;
          try {
            send(conversation.channel.seal(Buffer.from(text)));
          } catch {
            endChannel();
            ws.terminate();
          }
        });
        socket.on("error", () => {});
        socket.once("close", () => {
          output.close();
          if (live === conversation) live = undefined;
        });
      }

      const socket = conversation.socket;
      if (!socket.writable || socket.writableLength > MAX_BUFFERED_BYTES) {
        endChannel();
        ws.terminate();
        return;
      }
      socket.write(Buffer.concat([line, Buffer.from("\n")]));
    }

    ws.on("open", () => {
      if (ws.protocol !== "coucou.v1") return ws.terminate();
      delayIndex = 0;
      lastHeard = Date.now();
      status("Relay connected. Waiting for the phone.");
      heartbeat = setInterval(() => {
        if (Date.now() - lastHeard > HEARTBEAT_MS * 2) ws.terminate();
        else send("ping", false);
      }, HEARTBEAT_MS);
    });

    ws.on("message", (message, binary) => {
      lastHeard = Date.now();
      if (!binary) {
        if (message.toString() === '{"peer":"offline"}') endChannel();
        return;
      }
      const frame = Buffer.from(message);
      if (isInit(frame)) {
        if (Date.now() < quietUntil) return;
        const accepted = acceptInit(key, room, frame);
        if (!accepted) return badHandshake();
        bad = 0;
        endChannel();
        live = { channel: accepted.channel };
        send(accepted.reply);
        return;
      }
      if (!live) {
        if (Date.now() >= quietUntil) badHandshake();
        return;
      }
      try {
        bridge(live, live.channel.open(frame));
      } catch {
        endChannel();
        ws.terminate();
      }
    });

    ws.on("error", () => {
      if (!stopping && !upgradeRejected) status("Could not reach the relay. Check the Worker address and network.");
    });
    ws.on("unexpected-response", (_, response) => {
      upgradeRejected = true;
      if (response.statusCode === 401 || response.statusCode === 403) {
        status("Relay refused the access key. Check the server and Worker secrets.");
      } else if (response.statusCode === 429) {
        status("Relay is limiting connections. Retrying.");
      } else {
        status("Relay refused the connection. Check the Worker address.");
      }
      response.resume();
      ws.terminate();
    });
    ws.on("close", () => {
      clearInterval(heartbeat);
      endChannel();
      if (stopping) return;
      retry = setTimeout(join, RETRY_MS[Math.min(delayIndex++, RETRY_MS.length - 1)]);
    });
  }

  join();
  return () => {
    stopping = true;
    clearTimeout(retry);
    client?.terminate();
  };
}
