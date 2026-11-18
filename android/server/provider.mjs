import { setTimeout } from "node:timers/promises";

export const DEEPSEEK = Object.freeze({
  id: "deepseek",
  label: "DeepSeek Flash",
  baseUrl: "https://api.deepseek.com",
  model: "deepseek-flash",
});

export class ProviderError extends Error {
  constructor(reason) {
    super(reason);
    this.reason = reason;
  }
}

/** The link layer only calls answer(). Model selection and provider authentication live here. */
export function createProvider({ config = DEEPSEEK, apiKey, fake = false, fetchImpl = fetch } = {}) {
  const model = {
    id: `${config.id}/${config.model}`,
    provider: config.id,
    label: fake ? `${config.label} (fake test)` : config.label,
  };

  return {
    model,
    async answer(messages, signal) {
      if (fake) {
        await setTimeout(300, undefined, { signal });
        return "Fake test answer from Coumpanion. The server received your message.";
      }

      if (!apiKey) throw new ProviderError("no_key");
      const providerSignal = AbortSignal.any([signal, AbortSignal.timeout(120_000)]);
      let response;
      try {
        response = await fetchImpl(new URL("chat/completions", config.baseUrl + "/"), {
          method: "POST",
          headers: { authorization: `Bearer ${apiKey}`, "content-type": "application/json" },
          body: JSON.stringify({ model: config.model, messages, stream: false, max_tokens: 4096 }),
          redirect: "error",
          signal: providerSignal,
        });
      } catch {
        throw new ProviderError(signal.aborted ? "canceled" : "unreachable");
      }
      if (!response.ok) {
        await response.body?.cancel().catch(() => {});
        if (response.status === 401 || response.status === 403) throw new ProviderError("auth");
        if (response.status === 429) throw new ProviderError("rate");
        throw new ProviderError("provider");
      }

      let answer;
      try {
        const result = await response.json();
        const choice = result.choices?.[0];
        if (choice?.finish_reason === "stop") answer = choice.message?.content;
      } catch {
        throw new ProviderError(signal.aborted ? "canceled" : "provider");
      }
      if (typeof answer !== "string" || !answer.trim()) throw new ProviderError("provider");
      return answer;
    },
  };
}
