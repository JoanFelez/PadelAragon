import type { MiddlewareHandler } from "hono";
import type { Env } from "./types";

function safeEqual(a: string, b: string): boolean {
  const ea = new TextEncoder().encode(a);
  const eb = new TextEncoder().encode(b);
  let diff = ea.length ^ eb.length;
  for (let i = 0; i < Math.max(ea.length, eb.length); i++) {
    diff |= (ea[i] ?? 0) ^ (eb[i] ?? 0);
  }
  return diff === 0;
}

const unauthorized = () =>
  new Response(JSON.stringify({ error: "unauthorized" }), {
    status: 401,
    headers: { "content-type": "application/json" },
  });

export const requireAppKey: MiddlewareHandler<{ Bindings: Env }> = async (c, next) => {
  const expected = c.env.APP_KEY;
  const given = c.req.header("X-App-Key");
  if (!expected || !given || !safeEqual(given, expected)) return unauthorized();
  await next();
};

export const requireScraperToken: MiddlewareHandler<{ Bindings: Env }> = async (c, next) => {
  const expected = c.env.SCRAPER_TOKEN;
  const header = c.req.header("Authorization") ?? "";
  const given = header.startsWith("Bearer ") ? header.slice(7) : "";
  if (!expected || !given || !safeEqual(given, expected)) return unauthorized();
  await next();
};
