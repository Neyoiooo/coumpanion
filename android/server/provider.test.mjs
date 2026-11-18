import assert from "node:assert/strict";
import test from "node:test";
import { createProvider } from "./provider.mjs";

const fixtureCredential = "local-test-fixture";
const prompt = [{ role: "user", content: "Give me a short greeting." }];

test("DeepSeek uses the documented model and the OpenAI-compatible chat endpoint", async () => {
  const provider = createProvider({
    apiKey: fixtureCredential,
    fetchImpl: async (url, options) => {
      assert.equal(url.href, "https://api.deepseek.com/chat/completions");
      assert.equal(options.method, "POST");
      assert.equal(options.headers.authorization, `Bearer ${fixtureCredential}`);
      assert.equal(options.redirect, "error");
      assert.equal(JSON.parse(options.body).model, "deepseek-flash");
      assert.deepEqual(JSON.parse(options.body).messages, prompt);
      return Response.json({
        choices: [{ finish_reason: "stop", message: { content: "Hello from the test provider." } }],
      });
    },
  });
  assert.equal(await provider.answer(prompt, new AbortController().signal), "Hello from the test provider.");
});

test("provider errors and incomplete answers do not look like successful replies", async () => {
  for (const [response, reason] of [
    [new Response("provider-specific diagnostics", { status: 401 }), "auth"],
    [new Response("provider-specific diagnostics", { status: 429 }), "rate"],
    [Response.json({ choices: [{ finish_reason: "length", message: { content: "Half an answer" } }] }), "provider"],
  ]) {
    const provider = createProvider({ apiKey: fixtureCredential, fetchImpl: async () => response });
    await assert.rejects(provider.answer(prompt, new AbortController().signal), { message: reason });
  }
});
