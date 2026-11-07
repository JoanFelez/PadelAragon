import { beforeEach, describe, expect, it } from "vitest";
import { createApp } from "../src/app";
import { sha256Hex } from "../src/data";
import { appHeaders, bundleFixture, clearKv, leaguesFixture, makeEnv, scraperHeaders } from "./helpers";
import manifestFixture from "../../../contracts/fixtures/manifest.sample.json";

const app = createApp();
const bundleBytes = JSON.stringify(bundleFixture);
const put = (env: ReturnType<typeof makeEnv>, path: string, body: string, headers: Record<string, string> = scraperHeaders) =>
  app.fetch(new Request(`https://w.test${path}`, { method: "PUT", body, headers }), env);

beforeEach(clearKv);

describe("auth", () => {
  it("rejects /api without or with a wrong X-App-Key", async () => {
    const env = makeEnv();
    expect((await app.fetch(new Request("https://w.test/api/manifest"), env)).status).toBe(401);
    const bad = new Request("https://w.test/api/manifest", { headers: { "X-App-Key": "nope" } });
    expect((await app.fetch(bad, env)).status).toBe(401);
  });

  it("rejects /internal without the scraper token, and the app key does not work there", async () => {
    const env = makeEnv();
    expect((await put(env, "/internal/manifest", "{}", {})).status).toBe(401);
    expect((await put(env, "/internal/manifest", "{}", appHeaders)).status).toBe(401);
    expect((await put(env, "/internal/manifest", "{}", { Authorization: "Bearer wrong" })).status).toBe(401);
  });

  it("fails closed when secrets are not configured", async () => {
    const env = makeEnv({ APP_KEY: undefined, SCRAPER_TOKEN: undefined });
    expect((await app.fetch(new Request("https://w.test/api/manifest", { headers: appHeaders }), env)).status).toBe(401);
    expect((await put(env, "/internal/manifest", "{}")).status).toBe(401);
  });
});

describe("bundle upload and serving", () => {
  it("stores a bundle, serves it with ETag = sha256 and 304 on If-None-Match", async () => {
    const env = makeEnv();
    const expectedHash = await sha256Hex(new TextEncoder().encode(bundleBytes).buffer as ArrayBuffer);

    const up = await put(env, "/internal/leagues/27951/bundle", bundleBytes);
    expect(up.status).toBe(200);
    expect(await up.json()).toEqual({ hash: expectedHash, changed: true });

    const get = await app.fetch(new Request("https://w.test/api/leagues/27951/bundle", { headers: appHeaders }), env);
    expect(get.status).toBe(200);
    expect(get.headers.get("ETag")).toBe(`"${expectedHash}"`);
    expect(get.headers.get("Cache-Control")).toBe("private, max-age=60");
    expect(await get.text()).toBe(bundleBytes);

    const cached = await app.fetch(
      new Request("https://w.test/api/leagues/27951/bundle", { headers: { ...appHeaders, "If-None-Match": `"${expectedHash}"` } }),
      env,
    );
    expect(cached.status).toBe(304);
    expect(await cached.text()).toBe("");

    const stale = await app.fetch(
      new Request("https://w.test/api/leagues/27951/bundle", { headers: { ...appHeaders, "If-None-Match": '"other"' } }),
      env,
    );
    expect(stale.status).toBe(200);
  });

  it("writes KV only when the hash changed", async () => {
    const env = makeEnv();
    await put(env, "/internal/leagues/27951/bundle", bundleBytes);
    expect(env.puts).toEqual(["bundle:27951"]);

    const again = await put(env, "/internal/leagues/27951/bundle", bundleBytes);
    expect((await again.json()) as any).toMatchObject({ changed: false });
    expect(env.puts).toHaveLength(1);

    const changed = JSON.stringify({ ...bundleFixture, generatedAt: "2026-11-08T00:00:00Z" });
    expect(((await (await put(env, "/internal/leagues/27951/bundle", changed)).json()) as any).changed).toBe(true);
    expect(env.puts).toHaveLength(2);
  });

  it("validates uploads", async () => {
    const env = makeEnv();
    expect((await put(env, "/internal/leagues/27951/bundle", "not json")).status).toBe(400);
    expect((await put(env, "/internal/leagues/1/bundle", bundleBytes)).status).toBe(400);
    expect((await put(env, "/internal/leagues/27951/bundle", JSON.stringify({ ...bundleFixture, schemaVersion: 2 }))).status).toBe(400);
    const wrongHash = await put(env, "/internal/leagues/27951/bundle", bundleBytes, { ...scraperHeaders, "X-Content-Sha256": "00" });
    expect(wrongHash.status).toBe(400);
    expect(env.puts).toHaveLength(0);
  });

  it("returns 404 for a missing bundle", async () => {
    const res = await app.fetch(new Request("https://w.test/api/leagues/27951/bundle", { headers: appHeaders }), makeEnv());
    expect(res.status).toBe(404);
  });
});

describe("manifest and leagues", () => {
  it("round-trips the contract fixtures", async () => {
    const env = makeEnv();
    const manifest = JSON.stringify(manifestFixture);
    const leagues = JSON.stringify(leaguesFixture);
    expect((await put(env, "/internal/manifest", manifest)).status).toBe(200);
    expect((await put(env, "/internal/leagues", leagues)).status).toBe(200);

    const m = await app.fetch(new Request("https://w.test/api/manifest", { headers: appHeaders }), env);
    expect(await m.json()).toEqual(manifestFixture);
    const l = await app.fetch(new Request("https://w.test/api/leagues", { headers: appHeaders }), env);
    expect(await l.json()).toEqual(leaguesFixture);
    expect(l.headers.get("ETag")).toMatch(/^"[0-9a-f]{64}"$/);
  });
});
