import { createApp } from "./app";
import { dispatchScrape } from "./cron";
import type { Env } from "./types";

const app = createApp();

export default {
  fetch: app.fetch,
  async scheduled(_event: ScheduledController, env: Env, ctx: ExecutionContext) {
    ctx.waitUntil(dispatchScrape(env));
  },
} satisfies ExportedHandler<Env>;
