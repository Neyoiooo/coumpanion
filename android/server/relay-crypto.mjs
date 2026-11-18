import { createCipheriv, createDecipheriv, createHmac, hkdfSync, randomBytes, timingSafeEqual } from "node:crypto";

const LABEL = "coucou-relay/v1";
const VERSION = 1;
const INIT = 1;
const ACCEPT = 2;
const DATA = 3;
const PHONE = 1;
const HOST = 2;
const HEADER_BYTES = 10;
const TAG_BYTES = 16;
const BLOCK_BYTES = 128;
const MAX_LINE_BYTES = 64 * 1024;
const MAX_COUNTER = 0xffffffff;
export const MAX_RELAY_FRAME_BYTES = 66_000;

function derive(key, label, length, salt = Buffer.alloc(0)) {
  return Buffer.from(hkdfSync("sha256", key, salt, `${LABEL} ${label}`, length));
}

function mac(key, ...parts) {
  const hmac = createHmac("sha256", key);
  for (const part of parts) hmac.update(part);
  return hmac.digest();
}

function header(type, counter) {
  const bytes = Buffer.alloc(HEADER_BYTES);
  bytes[0] = VERSION;
  bytes[1] = type;
  bytes.writeBigUInt64BE(BigInt(counter), 2);
  return bytes;
}

function nonce(direction, counter) {
  const bytes = Buffer.alloc(12);
  bytes[0] = direction;
  bytes.writeBigUInt64BE(BigInt(counter), 4);
  return bytes;
}

function associatedData(room, direction, frameHeader) {
  return Buffer.concat([Buffer.from(LABEL + room), Buffer.from([direction]), frameHeader]);
}

export const joinProof = key => derive(key, "join", 32).toString("base64url");
export const isInit = frame => frame.length > 1 && frame[1] === INIT;

/** Server side of the v1 relay handshake. A fresh host nonce makes each session independent. */
export function acceptInit(key, room, frame, hostNonce = randomBytes(16)) {
  if (frame.length !== HEADER_BYTES + 16 + 32 ||
      !frame.subarray(0, HEADER_BYTES).equals(header(INIT, 0))) return null;
  const auth = derive(key, "auth", 32);
  const phoneNonce = frame.subarray(HEADER_BYTES, HEADER_BYTES + 16);
  if (!timingSafeEqual(frame.subarray(HEADER_BYTES + 16), mac(auth, "init", room, phoneNonce))) return null;

  const sessionKeys = derive(key, "session", 64, Buffer.concat([phoneNonce, hostNonce]));
  return {
    reply: Buffer.concat([header(ACCEPT, 0), hostNonce, mac(auth, "accept", room, phoneNonce, hostNonce)]),
    channel: new HostChannel(room, sessionKeys),
  };
}

class HostChannel {
  #room;
  #phoneKey;
  #hostKey;
  #received = 0;
  #sent = 0;

  constructor(room, keys) {
    this.#room = room;
    this.#phoneKey = keys.subarray(0, 32);
    this.#hostKey = keys.subarray(32);
  }

  seal(line) {
    if (!line.length || line.length > MAX_LINE_BYTES || this.#sent > MAX_COUNTER) throw new Error("invalid outgoing relay line");
    const plain = Buffer.alloc(Math.ceil((4 + line.length) / BLOCK_BYTES) * BLOCK_BYTES);
    plain.writeUInt32BE(line.length);
    line.copy(plain, 4);

    const frameHeader = header(DATA, this.#sent);
    const cipher = createCipheriv("aes-256-gcm", this.#hostKey, nonce(HOST, this.#sent), { authTagLength: TAG_BYTES });
    cipher.setAAD(associatedData(this.#room, HOST, frameHeader));
    const frame = Buffer.concat([frameHeader, cipher.update(plain), cipher.final(), cipher.getAuthTag()]);
    this.#sent++;
    return frame;
  }

  open(frame) {
    if (frame.length < HEADER_BYTES + TAG_BYTES + BLOCK_BYTES || frame.length > MAX_RELAY_FRAME_BYTES ||
        this.#received > MAX_COUNTER) throw new Error("invalid relay frame length or counter");
    const frameHeader = frame.subarray(0, HEADER_BYTES);
    if (!frameHeader.equals(header(DATA, this.#received))) throw new Error("unexpected relay frame");

    const decipher = createDecipheriv("aes-256-gcm", this.#phoneKey, nonce(PHONE, this.#received), { authTagLength: TAG_BYTES });
    decipher.setAAD(associatedData(this.#room, PHONE, frameHeader));
    decipher.setAuthTag(frame.subarray(-TAG_BYTES));
    const plain = Buffer.concat([decipher.update(frame.subarray(HEADER_BYTES, -TAG_BYTES)), decipher.final()]);
    const length = plain.readUInt32BE();
    if (!length || length > MAX_LINE_BYTES || length > plain.length - 4 ||
        plain.length % BLOCK_BYTES || plain.subarray(4 + length).some(byte => byte !== 0)) {
      throw new Error("invalid relay padding");
    }
    const line = plain.subarray(4, 4 + length);
    if (line.includes(10) || line.includes(13)) throw new Error("invalid relay line");
    this.#received++;
    return line;
  }
}
