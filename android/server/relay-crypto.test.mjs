import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import { acceptInit, joinProof } from "./relay-crypto.mjs";

const vectors = JSON.parse(readFileSync(new URL("../relay/test-vectors.json", import.meta.url), "utf8"));
const key = Buffer.from(vectors.inputs.key, "hex");
const room = vectors.inputs.room;
const init = Buffer.from(vectors.init.frame, "hex");
const hostNonce = Buffer.from(vectors.inputs.nPc, "hex");

test("v1 host matches the shared handshake and data vectors", () => {
  assert.equal(joinProof(key), Buffer.from(vectors.derived.kJoin, "hex").toString("base64url"));
  const { reply, channel } = acceptInit(key, room, init, hostNonce);
  assert.equal(reply.toString("hex"), vectors.accept.frame);
  for (const item of vectors.data.filter(item => item.counter < 3)) {
    if (item.dir === "p2h") {
      assert.equal(channel.open(Buffer.from(item.frame, "hex")).toString(), item.line);
    } else {
      assert.equal(channel.seal(Buffer.from(item.line)).toString("hex"), item.frame);
    }
  }
});

test("v1 host rejects tampered handshakes, data and replay", () => {
  const wrongInit = Buffer.from(init);
  wrongInit[wrongInit.length - 1] ^= 1;
  assert.equal(acceptInit(key, room, wrongInit, hostNonce), null);
  assert.equal(acceptInit(key, "another-room", init, hostNonce), null);
  assert.equal(acceptInit(Buffer.alloc(32, 255), room, init, hostNonce), null);

  const { channel } = acceptInit(key, room, init, hostNonce);
  const phoneFrames = vectors.data.filter(item => item.dir === "p2h");
  const hostFrame = vectors.data.find(item => item.dir === "h2p");
  for (const item of vectors.mustRefuse.filter(item => item.dir === "p2h" && !item.room && !item.key)) {
    assert.throws(() => channel.open(Buffer.from(item.frame, "hex")), undefined, item.name);
  }
  assert.throws(() => channel.open(Buffer.from(hostFrame.frame, "hex")));
  assert.throws(() => channel.open(Buffer.from(phoneFrames[1].frame, "hex")));
  const first = Buffer.from(phoneFrames[0].frame, "hex");
  assert.equal(channel.open(first).toString(), phoneFrames[0].line);
  assert.throws(() => channel.open(first));
});
