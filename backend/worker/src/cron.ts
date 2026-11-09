import type { Env } from "./types";

export async function dispatchScrape(env: Env, fetchFn: typeof fetch = fetch): Promise<boolean> {
  if (!env.GITHUB_DISPATCH_TOKEN) return false;
  const repo = env.GITHUB_REPO ?? "JoanFelez/PadelAragon";
  const ref = env.GITHUB_REF ?? "master";
  const res = await fetchFn(`https://api.github.com/repos/${repo}/actions/workflows/scrape.yml/dispatches`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${env.GITHUB_DISPATCH_TOKEN}`,
      Accept: "application/vnd.github+json",
      "User-Agent": "padelaragon-worker",
      "X-GitHub-Api-Version": "2022-11-28",
      "content-type": "application/json",
    },
    body: JSON.stringify({ ref }),
  });
  if (!res.ok) console.error(`workflow_dispatch failed: ${res.status}`);
  return res.ok;
}
