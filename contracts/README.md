# Data contract (schemaVersion 1)

JSON documents produced by the scraper, served by the Worker (`backend/worker`) and consumed by the apps.
Property names are the camelCase names of the Kotlin models, so `kotlinx.serialization` maps 1:1.
Changes within a `schemaVersion` are **additive only** (consumers must ignore unknown keys).
Fixtures in `fixtures/` are **synthetic**; real bundles contain player names/ages and are never committed.

## Documents

| Document | Worker route | KV key |
|---|---|---|
| `leagues.json` | `GET /api/leagues` | `leagues` |
| `manifest.json` | `GET /api/manifest` | `manifest` |
| `<leagueId>.json` (LeagueBundle) | `GET /api/leagues/:id/bundle` | `bundle:<leagueId>` |

**Hash**: lowercase hex SHA-256 of the exact bytes uploaded (the file as written by the scraper). It is the strong ETag (`"<hash>"`) and the `hash` in the manifest. Serialize once, hash those bytes, upload those same bytes.

## `leagues.json`: `League[]`
`{ id: Int, name: String }` e.g. `{ "id": 27951, "name": "Absoluta" }`.

## `Manifest`
```
{ schemaVersion: Int (1), generatedAt: ISO-8601 string,
  leagues: [ { id: Int, name: String, hash: String, updatedAt: ISO-8601 string } ] }
```
`updatedAt` changes only when `hash` changes.

## `LeagueBundle`
```
{ schemaVersion: Int (1),
  league: { id: Int, name: String },
  generatedAt: ISO-8601 string,
  groups: [ GroupBundle ] }
```

### `GroupBundle`
| Field | Type |
|---|---|
| `group` | `LeagueGroup` `{ id: Int, name: String, gender: "MASCULINA"\|"FEMENINA", category: String, groupLetter: String? }` |
| `standings` | `StandingRow[]` `{ position, teamName, teamId, teamHref (""), points, matchesPlayed, encountersWon, encountersLost, matchesWon, matchesLost, setsWon, setsLost, gamesWon, gamesLost }` (all Int except strings) |
| `jornadas` | `Int[]` (jornada numbers, ascending) |
| `results` | `MatchResult[]` `{ localTeam, localTeamId, visitorTeam, visitorTeamId, localScore: String, visitorScore: String, date: String?, venue: String?, jornada: Int, detailUrl: String? }`. Unplayed matches use `"--"` for both scores. |
| `teams` | `TeamBundle[]` `{ teamId: Int, teamName: String, category: String?, captainName: String?, players: Player[] }` with `Player` `{ name: String, isCaptain: Boolean, points: String?, birthYear: String? }` |
| `matchDetails` | `Map<detailUrl, MatchDetail>`; `MatchDetail` `{ pairs: [ { pairNumber: Int, localPlayer1, localPlayer2, visitorPlayer1, visitorPlayer2: String, sets: [ { localScore: Int, visitorScore: Int } ] } ] }`. Keys equal `MatchResult.detailUrl`. |

Nullable fields may be `null` or omitted. Player age = current year − `birthYear`.

## Worker upload API
`PUT /internal/leagues` (leagues.json), `PUT /internal/manifest`, `PUT /internal/leagues/:id/bundle` with `Authorization: Bearer <SCRAPER_TOKEN>` and the raw JSON body. Optional header `X-Content-Sha256` is verified against the body. Response `{ "hash": "...", "changed": true|false }`. KV is only written when the hash changed.
