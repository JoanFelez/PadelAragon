import { env as baseEnv } from "cloudflare:workers";
import type { Env } from "../src/types";
import bundleFixture from "../../../contracts/fixtures/league-bundle.sample.json";
import leaguesFixture from "../../../contracts/fixtures/leagues.sample.json";

export { bundleFixture, leaguesFixture };

export const APP_KEY = "test-app-key";
export const SCRAPER_TOKEN = "test-scraper-token";

export function makeEnv(overrides: Partial<Env> = {}): Env & { puts: string[] } {
  const kv = baseEnv.PADEL as KVNamespace;
  const puts: string[] = [];
  const counting = new Proxy(kv, {
    get(target, prop) {
      if (prop === "put") {
        return (key: string, ...rest: unknown[]) => {
          puts.push(key);
          return (target.put as Function).call(target, key, ...rest);
        };
      }
      const v = (target as any)[prop];
      return typeof v === "function" ? v.bind(target) : v;
    },
  });
  return { PADEL: counting, APP_KEY, SCRAPER_TOKEN, puts, ...overrides } as Env & { puts: string[] };
}

export async function clearKv() {
  const kv = baseEnv.PADEL as KVNamespace;
  const { keys } = await kv.list();
  await Promise.all(keys.map((k) => kv.delete(k.name)));
}

export const appHeaders = { "X-App-Key": APP_KEY };
export const scraperHeaders = { Authorization: `Bearer ${SCRAPER_TOKEN}` };

export async function seedFixtures(env: Env) {
  const put = (key: string, doc: unknown) => env.PADEL.put(key, JSON.stringify(doc), { metadata: { hash: "seed" } });
  await put("leagues", leaguesFixture);
  await put("bundle:27951", bundleFixture);
}
