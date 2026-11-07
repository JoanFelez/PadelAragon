import { describe, expect, it, vi } from "vitest";
import { dispatchScrape } from "../src/cron";
import { makeEnv } from "./helpers";

describe("cron dispatch", () => {
  it("skips silently without a token", async () => {
    const f = vi.fn();
    expect(await dispatchScrape(makeEnv(), f as any)).toBe(false);
    expect(f).not.toHaveBeenCalled();
  });

  it("dispatches scrape.yml on the configured ref", async () => {
    const f = vi.fn(async () => new Response(null, { status: 204 }));
    const ok = await dispatchScrape(makeEnv({ GITHUB_DISPATCH_TOKEN: "t", GITHUB_REF: "master", GITHUB_REPO: "o/r" }), f as any);
    expect(ok).toBe(true);
    const [url, init] = f.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe("https://api.github.com/repos/o/r/actions/workflows/scrape.yml/dispatches");
    expect(JSON.parse(init.body as string)).toEqual({ ref: "master" });
  });
});
