import { beforeEach, describe, expect, it, vi } from "vitest";
import { createApp } from "../src/app";
import { GeminiProvider, WorkersAiProvider } from "../src/chat/providers";
import { findCitedRules, selectRules, parseRules } from "../src/chat/rules";
import { ToolRunner } from "../src/chat/tools";
import { appHeaders, clearKv, makeEnv, seedFixtures } from "./helpers";

const RULES = `# Reglamento sintético

## Art. 1 Objeto
Texto sintético sobre el objeto.

## Art. 3 Clasificación
Los equipos puntúan por encuentro ganado. Texto sintético.
`;

const sse = (...events: unknown[]) =>
  new Response(events.map((e) => `data: ${JSON.stringify(e)}\r\n\r\n`).join(""), {
    headers: { "content-type": "text/event-stream" },
  });
const geminiText = (text: string) => ({ candidates: [{ content: { role: "model", parts: [{ text }] } }] });
const geminiCall = (name: string, args: object) => ({
  candidates: [{ content: { role: "model", parts: [{ functionCall: { name, args }, thoughtSignature: "sig-1" }] } }],
});

function chat(env: any, body: unknown, headers: Record<string, string> = appHeaders, deps = {}) {
  return createApp(deps).fetch(
    new Request("https://w.test/api/chat", { method: "POST", headers: { "content-type": "application/json", ...headers }, body: JSON.stringify(body) }),
    env,
  );
}

async function frames(res: Response) {
  const text = await res.text();
  return text
    .split("\n\n")
    .filter(Boolean)
    .map((f) => {
      expect(f.startsWith("data: ")).toBe(true);
      const raw = f.slice(6);
      return raw === "[DONE]" ? raw : JSON.parse(raw);
    });
}

const ask = (content: string) => ({ messages: [{ role: "user", content }], leagueId: 27951 });

beforeEach(clearKv);

describe("chat request validation and auth", () => {
  const env = () => makeEnv({ GEMINI_API_KEY: "k" });

  it("requires X-App-Key", async () => {
    expect((await chat(env(), ask("hola"), {})).status).toBe(401);
  });

  it("enforces input limits", async () => {
    const e = env();
    const many = Array.from({ length: 11 }, (_, i) => ({ role: i % 2 ? "assistant" : "user", content: "x" }));
    expect((await chat(e, { messages: many })).status).toBe(400);
    expect((await chat(e, ask("x".repeat(1001)))).status).toBe(400);
    expect((await chat(e, { messages: [{ role: "assistant", content: "hola" }] })).status).toBe(400);
    expect((await chat(e, { messages: [{ role: "system", content: "hola" }] })).status).toBe(400);
    expect((await chat(e, { messages: [] })).status).toBe(400);
    expect((await chat(e, { ...ask("hola"), favoriteGroupIds: "x" })).status).toBe(400);
  });

  it("accepts exactly 10 turns and 1000 characters", async () => {
    const fetchFn = vi.fn(async () => sse(geminiText("ok")));
    const providers = () => [new GeminiProvider("k", "m", fetchFn as any)];
    const msgs = Array.from({ length: 10 }, (_, i) => ({ role: i % 2 ? "assistant" : "user", content: "x".repeat(1000) }));
    msgs[9].role = "user";
    const res = await chat(env(), { messages: msgs }, appHeaders, { providers });
    expect(res.status).toBe(200);
    await res.text();
  });

  it("returns 503 when no provider is configured", async () => {
    const res = await chat(makeEnv(), ask("hola"));
    expect(res.status).toBe(503);
  });
});

describe("chat flow", () => {
  it("runs a Gemini tool loop and answers with SSE deltas, citations and [DONE]", async () => {
    const env = makeEnv({ GEMINI_API_KEY: "k" });
    await seedFixtures(env);
    await env.PADEL.put("rules:current", RULES);

    const bodies: any[] = [];
    const fetchFn = vi.fn(async (url: string, init: RequestInit) => {
      bodies.push(JSON.parse(init.body as string));
      expect(url).toContain(":streamGenerateContent?alt=sse");
      expect((init.headers as any)["x-goog-api-key"]).toBe("k");
      return bodies.length === 1
        ? sse(geminiCall("getStandings", { groupId: 90001 }))
        : sse(geminiText("El líder es CP Ficticio "), geminiText("Alfa (Art. 3)."));
    });
    const providers = () => [new GeminiProvider("k", "gemini-test", fetchFn as any)];

    const res = await chat(env, ask("¿Quién lidera el grupo A?"), appHeaders, { providers });
    expect(res.headers.get("content-type")).toContain("text/event-stream");
    const out = await frames(res);

    expect(out[out.length - 1]).toBe("[DONE]");
    const deltas = out.filter((f) => f !== "[DONE]" && "delta" in f).map((f: any) => f.delta);
    expect(deltas.join("")).toBe("El líder es CP Ficticio Alfa (Art. 3).");
    const citations = out.find((f) => f !== "[DONE]" && "citations" in f).citations;
    expect(citations).toContainEqual({ type: "rule", label: "Art. 3", title: "Art. 3 Clasificación" });
    expect(citations).toContainEqual(expect.objectContaining({ type: "data", tool: "getStandings" }));

    expect(bodies[0].tools[0].functionDeclarations.map((t: any) => t.name)).toContain("possibleCouples");
    expect(bodies[0].systemInstruction.parts[0].text).toContain("Art. 3 Clasificación");
    const second = bodies[1].contents;
    expect(second[1]).toEqual({ role: "model", parts: [{ functionCall: { name: "getStandings", args: { groupId: 90001 } }, thoughtSignature: "sig-1" }] });
    const resp = second[2].parts[0].functionResponse;
    expect(resp.name).toBe("getStandings");
    expect(JSON.stringify(resp.response)).toContain("CP Ficticio Alfa");
  });

  it("falls back to Workers AI on a Gemini 429 and supports its tool calls", async () => {
    const env = makeEnv({ GEMINI_API_KEY: "k" });
    await seedFixtures(env);
    const fetchFn = vi.fn(async () => new Response("quota", { status: 429 }));
    const run = vi
      .fn()
      .mockResolvedValueOnce({ response: null, tool_calls: [{ name: "findGroups", arguments: { query: "masculina" } }] })
      .mockResolvedValueOnce({ response: "Grupo A masculino." });
    const providers = () => [new GeminiProvider("k", "m", fetchFn as any), new WorkersAiProvider({ run }, "@cf/test")];

    const out = await frames(await chat(env, ask("¿Qué grupos masculinos hay?"), appHeaders, { providers }));
    expect(out.filter((f: any) => f.delta).map((f: any) => f.delta).join("")).toBe("Grupo A masculino.");
    expect(out[out.length - 1]).toBe("[DONE]");
    expect(fetchFn).toHaveBeenCalledTimes(2);
    expect(run).toHaveBeenCalledTimes(2);
    const toolMsg = run.mock.calls[1][1].messages.find((m: any) => m.role === "tool");
    expect(toolMsg.content).toContain("1ª CATEGORÍA MASCULINA - GRUPO A");
  });

  it("uses only Workers AI when GEMINI_API_KEY is unset (default provider factory)", async () => {
    const run = vi.fn().mockResolvedValue({ response: "Hola" });
    const env = makeEnv({ AI: { run } });
    const out = await frames(await chat(env, ask("hola")));
    expect(out[0]).toEqual({ delta: "Hola" });
    expect(run).toHaveBeenCalledWith("@cf/meta/llama-3.3-70b-instruct-fp8-fast", expect.anything());
  });

  it("emits an error frame when every provider fails", async () => {
    const env = makeEnv({ GEMINI_API_KEY: "k" });
    const providers = () => [new GeminiProvider("k", "m", (async () => new Response("x", { status: 500 })) as any)];
    const out = await frames(await chat(env, ask("hola"), appHeaders, { providers }));
    expect(out).toEqual([{ error: "provider_failed" }, "[DONE]"]);
  });

  it("limits tool rounds to 4 and then forces a final answer without tools", async () => {
    const env = makeEnv({ GEMINI_API_KEY: "k" });
    await seedFixtures(env);
    const bodies: any[] = [];
    const fetchFn = vi.fn(async (_u: string, init: RequestInit) => {
      const b = JSON.parse(init.body as string);
      bodies.push(b);
      return b.tools ? sse(geminiCall("listLeagues", {})) : sse(geminiText("Fin"));
    });
    const providers = () => [new GeminiProvider("k", "m", fetchFn as any)];
    const out = await frames(await chat(env, ask("bucle"), appHeaders, { providers }));
    expect(bodies).toHaveLength(5);
    expect(bodies[4].tools).toBeUndefined();
    expect(out.filter((f: any) => f.delta).map((f: any) => f.delta).join("")).toBe("Fin");
  });
});

describe("tools and rules", () => {
  it("selects relevant rule sections when the rulebook exceeds the budget", () => {
    const filler = "relleno ".repeat(200);
    const md = ["## Art. 1 Pelotas", `bote ${filler}`, "## Art. 2 Saque", `el saque debe ser por debajo ${filler}`, "## Art. 3 Red", `altura ${filler}`].join("\n");
    const sections = parseRules(md);
    expect(selectRules(sections, "¿cómo debe ser el saque?", 3000).map((s) => s.label)).toEqual(["Art. 2"]);
    expect(selectRules(sections, "cualquier cosa", 100000)).toHaveLength(3);
  });

  it("finds teams by partial name across leagues, ignoring case and order", async () => {
    const env = makeEnv();
    await seedFixtures(env);
    const r = await new ToolRunner(env.PADEL).execute("findTeams", { query: "ALFA ficticio" });
    expect((r.result as any[]).map((t) => t.teamId)).toContain(1001);
  });

  it("aggregates stats with queryStats (group, having, order)", async () => {
    const env = makeEnv();
    await seedFixtures(env);
    const r = await new ToolRunner(env.PADEL).execute("queryStats", {
      table: "playerMatches",
      groupBy: ["player"],
      metrics: [{ fn: "avg", field: "won", as: "winRate" }, { fn: "count", as: "n" }],
      having: [{ field: "n", op: "gte", value: 1 }],
      orderBy: "winRate",
      desc: true,
    });
    const rows = (r.result as any).rows;
    expect(rows.length).toBeGreaterThan(0);
    expect(rows[0].winRate).toBeGreaterThanOrEqual(rows[rows.length - 1].winRate);
    const t = await new ToolRunner(env.PADEL).execute("queryStats", { table: "standings", metrics: [{ fn: "sum", field: "points", as: "p" }] });
    expect((t.result as any).rows[0].p).toBe(8);
    const tm = await new ToolRunner(env.PADEL).execute("queryStats", { table: "teamMatches", groupBy: ["side"], metrics: [{ fn: "count", as: "n" }] });
    expect((tm.result as any).rows.map((x: any) => x.side).sort()).toEqual(["away", "home"]);
    const sp = await new ToolRunner(env.PADEL).execute("queryStats", {
      table: "teamMatches",
      groupBy: ["team"],
      metrics: [
        { fn: "sum", field: "won", as: "home", where: [{ field: "side", op: "eq", value: "home" }] },
        { fn: "sum", field: "won", as: "away", where: [{ field: "side", op: "eq", value: "away" }] },
      ],
      having: [{ field: "home", op: "gte", value: "@away" }],
    });
    expect((sp.result as any).rows.every((x: any) => x.home >= x.away)).toBe(true);
    const all = await new ToolRunner(env.PADEL).execute("queryStats", {
      table: "teamMatches",
      groupBy: ["team"],
      metrics: [{ fn: "sum", field: "won", as: "away", where: [{ field: "side", op: "eq", value: "away" }] }],
      filters: [{ field: "team", op: "contains", value: "alfa" }],
    });
    expect((all.result as any).rows[0].away).toBe(1);
    expect(((await new ToolRunner(env.PADEL).execute("queryStats", { table: "nope" })).result as any).error).toBe("unknown_table");
  });

  it("computes player stats from match details", async () => {
    const env = makeEnv();
    await seedFixtures(env);
    const r = await new ToolRunner(env.PADEL, 27951).execute("getPlayerStats", { name: "uno alfa" });
    expect(r.result).toEqual([
      {
        name: "Jugador Uno Alfa",
        home: { wins: 1, losses: 0, pair1Count: 1, pair2Count: 0, pair3Count: 0 },
        away: { wins: 1, losses: 0, pair1Count: 1, pair2Count: 0, pair3Count: 0 },
      },
    ]);
  });

  it("possibleCouples resolves ages from birth years and gender from the group", async () => {
    const env = makeEnv();
    await seedFixtures(env);
    const names = ["Uno", "Dos", "Tres", "Cuatro", "Cinco", "Seis"].map((n) => `Jugador ${n} Alfa`);
    const young = await new ToolRunner(env.PADEL, 27951, 2026).execute("possibleCouples", { team: "ficticio alfa", available: names });
    expect((young.result as any).status).toBe("no_combinations_possible");
    const old = await new ToolRunner(env.PADEL, 27951, 2040).execute("possibleCouples", { team: "1001", available: [...names, "Desconocido"] });
    expect((old.result as any).status).toBe("success");
    expect((old.result as any).unresolved).toEqual(["Desconocido"]);
    expect((old.result as any).gender).toBe("MASCULINA");
  });
});

describe("rule citations", () => {
  it("parses dotted numbers and document tags, and resolves citations by tag", () => {
    const sections = parseRules("## Art. 6 (FIP) Saque\nA\n\n## Art. 6 (Liga) Jornadas\nB\n\n## Art. 8.5 (Liga) Convocatoria\nC\n\n## Pista (FIP)\nD");
    expect(sections.map((s) => s.label)).toEqual(["Art. 6 (FIP)", "Art. 6 (Liga)", "Art. 8.5 (Liga)", "Pista (FIP)"]);
    expect(findCitedRules("Según el Art. 6 (Liga) y el Art. 8.5.", sections).map((s) => s.label)).toEqual(["Art. 6 (Liga)", "Art. 8.5 (Liga)"]);
    expect(findCitedRules("Art. 6", sections)).toHaveLength(2);
  });
});
