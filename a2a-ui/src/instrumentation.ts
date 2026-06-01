export async function register(): Promise<void> {
  if (process.env.NEXT_RUNTIME === "nodejs") {
    // Local LLMs (Ollama) can take many minutes to respond.
    // Node.js default headersTimeout is 300s — not enough for two sequential LLM calls.
    const { setGlobalDispatcher, Agent } = await import("undici");
    setGlobalDispatcher(
      new Agent({ headersTimeout: 600_000, bodyTimeout: 600_000 })
    );
  }
}
