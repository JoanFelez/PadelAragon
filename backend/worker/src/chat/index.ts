import { Hono } from "hono";
import { requireAppKey } from "../auth";
import type { Env } from "../types";
import { buildSystemPrompt } from "./prompt";
import { createProviders, type ChatMessage, type Provider, type ProviderRequest, type ToolCall } from "./providers";
import { findCitedRules, formatRules, parseRules, selectRules } from "./rules";
import { TOOL_DECLARATIONS, ToolRunner } from "./tools";

export const MAX_TURNS = 10;
export const MAX_MESSAGE_CHARS = 1000;
export const MAX_TOOL_ROUNDS = 4;

interface ChatRequest {
  messages: { role: "user" | "assistant"; content: string }[];
  leagueId?: number;
  favoriteGroupIds?: number[];
}

export function validateChatRequest(body: any): { ok: true; value: ChatRequest } | { ok: false; error: string } {
  if (!body || typeof body !== "object" || !Array.isArray(body.messages)) return { ok: false, error: "messages_required" };
  const messages = body.messages;
  if (messages.length === 0) return { ok: false, error: "messages_required" };
  if (messages.length > MAX_TURNS) return { ok: false, error: "too_many_messages" };
  for (const m of messages) {
    if (!m || (m.role !== "user" && m.role !== "assistant") || typeof m.content !== "string" || !m.content.trim()) {
      return { ok: false, error: "invalid_message" };
    }
    if (m.content.length > MAX_MESSAGE_CHARS) return { ok: false, error: "message_too_long" };
  }
  if (messages[messages.length - 1].role !== "user") return { ok: false, error: "last_message_must_be_user" };
  if (body.leagueId !== undefined && !Number.isInteger(body.leagueId)) return { ok: false, error: "invalid_leagueId" };
  if (
    body.favoriteGroupIds !== undefined &&
    (!Array.isArray(body.favoriteGroupIds) || body.favoriteGroupIds.length > 10 || !body.favoriteGroupIds.every(Number.isInteger))
  ) {
    return { ok: false, error: "invalid_favoriteGroupIds" };
  }
  return { ok: true, value: body as ChatRequest };
}

const encoder = new TextEncoder();
const frame = (payload: unknown) => encoder.encode(`data: ${typeof payload === "string" ? payload : JSON.stringify(payload)}\n\n`);

export interface ChatDeps {
  providers?: (env: Env) => Provider[];
  now?: () => Date;
}

export function chatRoutes(deps: ChatDeps = {}) {
  const app = new Hono<{ Bindings: Env }>();
  app.use("/api/chat", requireAppKey);

  app.post("/api/chat", async (c) => {
    let body: unknown;
    try {
      body = await c.req.json();
    } catch {
      return c.json({ error: "invalid_json" }, 400);
    }
    const parsed = validateChatRequest(body);
    if (!parsed.ok) return c.json({ error: parsed.error }, 400);

    const providers = (deps.providers ?? ((e: Env) => createProviders(e)))(c.env);
    if (providers.length === 0) return c.json({ error: "chat_unavailable" }, 503);

    const { readable, writable } = new TransformStream<Uint8Array>();
    const writer = writable.getWriter();
    const job = runChat(c.env, parsed.value, providers, writer, deps.now?.() ?? new Date()).finally(() => writer.close().catch(() => {}));
    try {
      c.executionCtx.waitUntil(job);
    } catch {
      // no execution context (tests)
    }

    return new Response(readable, {
      headers: {
        "content-type": "text/event-stream; charset=utf-8",
        "cache-control": "no-store",
        "x-accel-buffering": "no",
      },
    });
  });

  return app;
}

async function runChat(
  env: Env,
  req: ChatRequest,
  providers: Provider[],
  writer: WritableStreamDefaultWriter<Uint8Array>,
  now: Date,
) {
  const send = (payload: unknown) => writer.write(frame(payload));
  try {
    const question = req.messages.filter((m) => m.role === "user").map((m) => m.content).join(" ");
    const rulesMarkdown = await env.PADEL.get("rules:current", "text");
    const allRules = rulesMarkdown ? parseRules(rulesMarkdown) : [];
    const selected = allRules.length ? selectRules(allRules, question) : [];
    const system = buildSystemPrompt(selected.length ? formatRules(selected) : null, req);

    const runner = new ToolRunner(env.PADEL, req.leagueId, now.getFullYear());
    const history: ChatMessage[] = req.messages.map((m) =>
      m.role === "user" ? { role: "user", text: m.content } : { role: "assistant", text: m.content, toolCalls: [] },
    );
    const sources = new Map<string, { tool: string; label: string }>();
    let answer = "";

    for (let round = 0; round <= MAX_TOOL_ROUNDS; round++) {
      const request: ProviderRequest = {
        system,
        messages: history,
        tools: round < MAX_TOOL_ROUNDS ? TOOL_DECLARATIONS : [],
      };
      const { text, toolCalls } = await streamWithFallback(providers, request, async (delta) => {
        answer += delta;
        await send({ delta });
      });
      if (toolCalls.length === 0) break;

      history.push({ role: "assistant", text, toolCalls });
      for (const call of toolCalls) {
        const outcome = await runner.execute(call.name, call.args);
        if (outcome.source) sources.set(`${outcome.source.tool}:${outcome.source.label}`, outcome.source);
        history.push({ role: "tool", name: call.name, result: outcome.result });
      }
    }

    const citations = [
      ...findCitedRules(answer, allRules).map((s) => ({ type: "rule", label: s.label, title: s.title })),
      ...[...sources.values()].map((s) => ({ type: "data", tool: s.tool, label: s.label })),
    ];
    await send({ citations });
  } catch (e) {
    console.error("chat failed", e instanceof Error ? e.message : e);
    await send({ error: "provider_failed" }).catch(() => {});
  }
  await send("[DONE]").catch(() => {});
}

async function streamWithFallback(
  providers: Provider[],
  request: ProviderRequest,
  onText: (delta: string) => Promise<void>,
): Promise<{ text: string; toolCalls: ToolCall[] }> {
  let lastError: unknown;
  for (const provider of providers) {
    let emitted = false;
    let text = "";
    const toolCalls: ToolCall[] = [];
    try {
      for await (const ev of provider.stream(request)) {
        if (ev.type === "text") {
          emitted = true;
          text += ev.text;
          await onText(ev.text);
        } else {
          toolCalls.push(ev.call);
        }
      }
      return { text, toolCalls };
    } catch (e) {
      lastError = e;
      if (emitted) throw e;
    }
  }
  throw lastError ?? new Error("no_provider");
}
