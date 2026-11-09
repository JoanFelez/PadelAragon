import { Hono } from "hono";
import { requireAppKey, requireScraperToken } from "./auth";
import { SCHEMA_VERSION, type Env } from "./types";

const JSON_HEADERS = { "content-type": "application/json; charset=utf-8" };
const KEY_MANIFEST = "manifest";
const KEY_LEAGUES = "leagues";
export const bundleKey = (id: number | string) => `bundle:${id}`;

export async function sha256Hex(bytes: ArrayBuffer): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

function matchesEtag(header: string | undefined, hash: string): boolean {
  if (!header) return false;
  if (header.trim() === "*") return true;
  return header.split(",").some((t) => t.trim().replace(/^W\//, "") === `"${hash}"`);
}

async function serveStored(kv: KVNamespace, key: string, ifNoneMatch: string | undefined) {
  const { value, metadata } = await kv.getWithMetadata<{ hash: string }>(key, "text");
  if (value === null || !metadata?.hash) {
    return new Response(JSON.stringify({ error: "not_found" }), { status: 404, headers: JSON_HEADERS });
  }
  const headers = {
    ETag: `"${metadata.hash}"`,
    "Cache-Control": "private, max-age=60",
  };
  if (matchesEtag(ifNoneMatch, metadata.hash)) return new Response(null, { status: 304, headers });
  return new Response(value, { status: 200, headers: { ...JSON_HEADERS, ...headers } });
}

type Validator = (doc: any) => string | null;

async function storeIfChanged(
  kv: KVNamespace,
  key: string,
  request: Request,
  validate: Validator,
): Promise<Response> {
  const bytes = await request.arrayBuffer();
  const hash = await sha256Hex(bytes);
  const claimed = request.headers.get("X-Content-Sha256");
  if (claimed && claimed.toLowerCase() !== hash) {
    return Response.json({ error: "hash_mismatch" }, { status: 400 });
  }
  const text = new TextDecoder().decode(bytes);
  let doc: unknown;
  try {
    doc = JSON.parse(text);
  } catch {
    return Response.json({ error: "invalid_json" }, { status: 400 });
  }
  const problem = validate(doc);
  if (problem) return Response.json({ error: "invalid_document", detail: problem }, { status: 400 });

  const existing = await kv.getWithMetadata<{ hash: string }>(key, "text");
  if (existing.metadata?.hash === hash && existing.value !== null) {
    return Response.json({ hash, changed: false });
  }
  await kv.put(key, text, { metadata: { hash } });
  return Response.json({ hash, changed: true });
}

const isObject = (v: unknown): v is Record<string, any> => typeof v === "object" && v !== null && !Array.isArray(v);

const validateManifest: Validator = (d) =>
  isObject(d) && d.schemaVersion === SCHEMA_VERSION && Array.isArray(d.leagues) ? null : "bad manifest";

const validateLeagues: Validator = (d) =>
  Array.isArray(d) && d.every((l) => isObject(l) && typeof l.id === "number" && typeof l.name === "string")
    ? null
    : "bad leagues list";

const validateBundle =
  (id: string): Validator =>
  (d) => {
    if (!isObject(d) || d.schemaVersion !== SCHEMA_VERSION) return "bad schemaVersion";
    if (!isObject(d.league) || String(d.league.id) !== id) return "league id mismatch";
    if (!Array.isArray(d.groups)) return "groups missing";
    return null;
  };

export function dataRoutes() {
  const app = new Hono<{ Bindings: Env }>();

  app.use("/api/*", requireAppKey);
  app.use("/internal/*", requireScraperToken);

  app.get("/api/manifest", (c) => serveStored(c.env.PADEL, KEY_MANIFEST, c.req.header("If-None-Match")));
  app.get("/api/leagues", (c) => serveStored(c.env.PADEL, KEY_LEAGUES, c.req.header("If-None-Match")));
  app.get("/api/leagues/:id/bundle", async (c) => {
    const id = c.req.param("id");
    if (!/^\d+$/.test(id)) return c.json({ error: "bad_id" }, 400);
    return serveStored(c.env.PADEL, bundleKey(id), c.req.header("If-None-Match"));
  });

  app.put("/internal/manifest", (c) => storeIfChanged(c.env.PADEL, KEY_MANIFEST, c.req.raw, validateManifest));
  app.put("/internal/leagues", (c) => storeIfChanged(c.env.PADEL, KEY_LEAGUES, c.req.raw, validateLeagues));
  app.put("/internal/leagues/:id/bundle", async (c) => {
    const id = c.req.param("id");
    if (!/^\d+$/.test(id)) return c.json({ error: "bad_id" }, 400);
    return storeIfChanged(c.env.PADEL, bundleKey(id), c.req.raw, validateBundle(id));
  });

  return app;
}
