import type { Env } from "../types";
import type { ToolDeclaration } from "./tools";

export interface ToolCall {
  name: string;
  args: Record<string, unknown>;
  raw?: unknown;
}

export type ChatMessage =
  | { role: "user"; text: string }
  | { role: "assistant"; text: string; toolCalls?: ToolCall[] }
  | { role: "tool"; name: string; result: unknown };

export type ProviderEvent = { type: "text"; text: string } | { type: "toolCall"; call: ToolCall };

export interface ProviderRequest {
  system: string;
  /** Shorter prompt for providers with a small context window (Workers AI: 24k tokens). */
  systemCompact?: string;
  messages: ChatMessage[];
  tools: ToolDeclaration[];
}

export interface Provider {
  name: string;
  stream(req: ProviderRequest): AsyncIterable<ProviderEvent>;
}

export class ProviderError extends Error {
  constructor(
    public provider: string,
    public status: number,
    message: string,
  ) {
    super(message);
  }
}

export const DEFAULT_GEMINI_MODEL = "gemini-3.5-flash-lite";
export const DEFAULT_WORKERS_AI_MODEL = "@cf/meta/llama-3.3-70b-instruct-fp8-fast";

export function createProviders(env: Env, fetchFn: typeof fetch = (input, init) => fetch(input, init)): Provider[] {
  const providers: Provider[] = [];
  if (env.GEMINI_API_KEY) {
    providers.push(new GeminiProvider(env.GEMINI_API_KEY, env.GEMINI_MODEL || DEFAULT_GEMINI_MODEL, fetchFn));
  }
  if (env.AI) {
    providers.push(new WorkersAiProvider(env.AI, env.WORKERS_AI_MODEL || DEFAULT_WORKERS_AI_MODEL));
  }
  return providers;
}

export class GeminiProvider implements Provider {
  name = "gemini";
  constructor(
    private apiKey: string,
    private model: string,
    private fetchFn: typeof fetch,
  ) {}

  private contents(messages: ChatMessage[]) {
    const out: { role: string; parts: unknown[] }[] = [];
    for (const m of messages) {
      if (m.role === "user") {
        out.push({ role: "user", parts: [{ text: m.text }] });
      } else if (m.role === "assistant") {
        const parts: unknown[] = [];
        if (m.text) parts.push({ text: m.text });
        for (const c of m.toolCalls ?? []) parts.push(c.raw ?? { functionCall: { name: c.name, args: c.args } });
        out.push({ role: "model", parts });
      } else {
        const part = { functionResponse: { name: m.name, response: { result: m.result } } };
        const last = out[out.length - 1];
        if (last && last.role === "user" && last.parts.every((p) => "functionResponse" in (p as object))) last.parts.push(part);
        else out.push({ role: "user", parts: [part] });
      }
    }
    return out;
  }

  async *stream(req: ProviderRequest): AsyncIterable<ProviderEvent> {
    const body: Record<string, unknown> = {
      systemInstruction: { parts: [{ text: req.system }] },
      contents: this.contents(req.messages),
      generationConfig: { temperature: 0.2 },
    };
    if (req.tools.length) body.tools = [{ functionDeclarations: req.tools }];

    const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(this.model)}:streamGenerateContent?alt=sse`;
    let res: Response | undefined;
    for (let attempt = 0; attempt < 3; attempt++) {
      try {
        res = await this.fetchFn(url, {
          method: "POST",
          headers: { "content-type": "application/json", "x-goog-api-key": this.apiKey },
          body: JSON.stringify(body),
        });
      } catch (e) {
        throw new ProviderError(this.name, 0, `network: ${e instanceof Error ? e.message : e}`);
      }
      if (res.ok && res.body) break;
      const detail = (await res.text().catch(() => "")).replace(/\s+/g, " ");
      // Free-tier limits are per minute and Gemini overloads are brief, so a short wait is worth it before falling back.
      const hint = /retry in ([\d.]+)s/.exec(detail);
      const waitMs = res.status === 503 ? 1500 * (attempt + 1) : res.status === 429 && hint ? Number(hint[1]) * 1000 + 500 : 0;
      if (attempt < 2 && waitMs > 0 && waitMs <= 12000) {
        await new Promise((r) => setTimeout(r, waitMs));
        continue;
      }
      throw new ProviderError(this.name, res.status, `gemini http ${res.status} ${detail.slice(0, 300)}`);
    }
    if (!res || !res.body) throw new ProviderError(this.name, 0, "gemini empty response");

    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    const handle = function* (block: string): Generator<ProviderEvent> {
      const data = block
        .split(/\r?\n/)
        .filter((l) => l.startsWith("data:"))
        .map((l) => l.slice(5).trimStart())
        .join("");
      if (!data) return;
      let json: any;
      try {
        json = JSON.parse(data);
      } catch {
        return;
      }
      for (const part of json?.candidates?.[0]?.content?.parts ?? []) {
        if (part.functionCall) {
          yield { type: "toolCall", call: { name: part.functionCall.name, args: part.functionCall.args ?? {}, raw: part } };
        } else if (typeof part.text === "string" && part.text && !part.thought) {
          yield { type: "text", text: part.text };
        }
      }
    };

    for (;;) {
      const { done, value } = await reader.read();
      if (value) buffer += decoder.decode(value, { stream: !done });
      let idx: number;
      while ((idx = buffer.search(/\r?\n\r?\n/)) >= 0) {
        const block = buffer.slice(0, idx);
        buffer = buffer.slice(idx).replace(/^\r?\n\r?\n/, "");
        yield* handle(block);
      }
      if (done) break;
    }
    if (buffer.trim()) yield* handle(buffer);
  }
}

export class WorkersAiProvider implements Provider {
  name = "workers-ai";
  constructor(
    private ai: NonNullable<Env["AI"]>,
    private model: string,
  ) {}

  private messages(req: ProviderRequest) {
    const out: Record<string, unknown>[] = [{ role: "system", content: req.systemCompact ?? req.system }];
    for (const m of req.messages) {
      if (m.role === "user") out.push({ role: "user", content: m.text });
      else if (m.role === "assistant") {
        out.push({
          role: "assistant",
          content: m.text,
          ...(m.toolCalls?.length
            ? {
                tool_calls: m.toolCalls.map((c, i) => ({
                  id: `call_${i}`,
                  type: "function",
                  function: { name: c.name, arguments: JSON.stringify(c.args) },
                })),
              }
            : {}),
        });
      } else {
        out.push({ role: "tool", name: m.name, content: JSON.stringify(m.result) });
      }
    }
    return out;
  }

  async *stream(req: ProviderRequest): AsyncIterable<ProviderEvent> {
    let out: any;
    try {
      out = await this.ai.run(this.model, {
        messages: this.messages(req),
        ...(req.tools.length ? { tools: req.tools } : {}),
        temperature: 0.2,
        max_tokens: 1024,
      });
    } catch (e) {
      const status = typeof (e as any)?.status === "number" ? (e as any).status : 500;
      throw new ProviderError(this.name, status, `workers-ai: ${e instanceof Error ? e.message : e}`);
    }

    const message = out?.choices?.[0]?.message;
    const text: string | null = typeof out?.response === "string" ? out.response : (message?.content ?? null);
    const calls: any[] = out?.tool_calls ?? message?.tool_calls ?? [];
    if (text) yield { type: "text", text };
    for (const c of calls) {
      const fn = c.function ?? c;
      let args = fn.arguments ?? {};
      if (typeof args === "string") {
        try {
          args = JSON.parse(args);
        } catch {
          args = {};
        }
      }
      yield { type: "toolCall", call: { name: fn.name, args } };
    }
  }
}
