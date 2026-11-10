# PadelAragon Worker (Cloudflare Workers Free)

Hono API that serves the league bundles (stored in KV) and the rulebook chatbot. Contract: [`contracts/README.md`](../../contracts/README.md).

## Routes
| Route | Auth | Notes |
|---|---|---|
| `GET /api/manifest`, `GET /api/leagues`, `GET /api/leagues/:id/bundle` | `X-App-Key` | Strong ETag = SHA-256 of the stored bytes, `304` on `If-None-Match`, `Cache-Control: private, max-age=60` |
| `POST /api/chat` | `X-App-Key` | Body `{messages:[{role,content}], leagueId?, favoriteGroupIds?}` (max 10 turns, 1000 chars each). SSE: `data:{"delta"}`, `data:{"citations":[...]}`, `data:[DONE]`; errors as `data:{"error"}`; `503` if no provider is configured |
| `PUT /internal/manifest`, `PUT /internal/leagues`, `PUT /internal/leagues/:id/bundle` | `Authorization: Bearer <SCRAPER_TOKEN>` | Raw JSON body; KV is written only if the hash changed. Response `{hash, changed}` |

Cron `*/30 6-22 * * *` triggers `workflow_dispatch` of `scrape.yml` (repo `GITHUB_REPO`, ref `GITHUB_REF`, default `JoanFelez/PadelAragon`@`master`). It is skipped silently without `GITHUB_DISPATCH_TOKEN`.

## Chat
Gemini (`streamGenerateContent?alt=sse`, `GEMINI_MODEL`, default `gemini-3.5-flash-lite`, free tier) is primary. On 429/5xx/network errors or without `GEMINI_API_KEY` it falls back to Workers AI (`WORKERS_AI_MODEL`, default `@cf/meta/llama-3.3-70b-instruct-fp8-fast`, non-streaming). Up to 4 tool rounds (`listLeagues`, `findGroups`, `getStandings`, `getTeam`, `getResults`, `getPlayerStats`, `possibleCouples`) read the KV bundles. The rulebook is the markdown in KV `rules:current` (`## Art. N (Tag) Title` headings; if larger than ~20k chars only the sections matching the question are sent). Check the model names against the provider docs before deploying; both are plain vars.

## Bindings, vars and secrets
- KV `PADEL` (replace the placeholder id in `wrangler.toml` after `npx wrangler kv namespace create PADEL`), `AI`.
- Vars: `GEMINI_MODEL`, `WORKERS_AI_MODEL`, `GITHUB_REPO`, `GITHUB_REF`.
- Secrets (`npx wrangler secret put NAME`): `APP_KEY`, `SCRAPER_TOKEN`, `GEMINI_API_KEY` (optional), `GITHUB_DISPATCH_TOKEN` (optional, fine-grained PAT with Actions: write on the repo).

## Local development
```
npm ci
cp .dev.vars.example .dev.vars   # gitignored; fill in values
npm test            # vitest + @cloudflare/vitest-pool-workers (Miniflare KV, no account needed)
npx tsc --noEmit
npx wrangler dev    # the AI binding always calls Cloudflare remotely
```

## Deploy
1. `npx wrangler login`; create the KV namespace and paste its id in `wrangler.toml`.
2. Set the secrets above.
3. `npx wrangler deploy`.
4. Rulebook (public federation PDFs, versioned): put the PDFs in `backend/rules/`, run `backend/scripts/convert-rules` and `backend/scripts/upload-rules.sh`.

Free-tier safety: KV writes happen only on content change (limit 1,000/day); no billing is configured anywhere.
