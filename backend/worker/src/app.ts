import { Hono } from "hono";
import { chatRoutes, type ChatDeps } from "./chat";
import { dataRoutes } from "./data";
import type { Env } from "./types";

export function createApp(deps: ChatDeps = {}) {
  const app = new Hono<{ Bindings: Env }>();
  app.route("/", dataRoutes());
  app.route("/", chatRoutes(deps));
  app.notFound((c) => c.json({ error: "not_found" }, 404));
  return app;
}
