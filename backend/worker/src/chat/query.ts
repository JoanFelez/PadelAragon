import type { GroupBundle, LeagueBundle } from "../types";
import { normalize } from "./rules";

type Row = Record<string, string | number | boolean | null>;

export const TABLES: Record<string, string> = {
  players:
    "Un registro por jugador de plantilla: leagueId, league, groupId, group, gender, category, teamId, team, player, isCaptain, points (puntos del jugador, número), birthYear, age.",
  standings:
    "Un registro por equipo en la clasificación: leagueId, league, groupId, group, gender, category, teamId, team, position, points, played, encountersWon, encountersLost, matchesWon, matchesLost, setsWon, setsLost, gamesWon, gamesLost.",
  matches:
    "Un registro por encuentro entre equipos: leagueId, league, groupId, group, gender, category, jornada, date, venue, homeTeam, awayTeam, homeScore, awayScore, played (boolean), winner (nombre del equipo o null), totalPairsWon (partidos de pareja del ganador).",
  teamMatches:
    "Un registro por equipo y encuentro jugado: leagueId, league, groupId, group, gender, category, jornada, date, team, opponent, side ('home'|'away'), won (1 victoria, 0 derrota, null empate), pairsFor, pairsAgainst, homeWins/awayWins (1 si ganó jugando de local/visitante, si no 0), homeGames/awayGames (1 si jugó de local/visitante, si no 0; suma para contar encuentros). Útil para local vs visitante.",
  pairs:
    "Un registro por partido de pareja (detalle): leagueId, league, groupId, group, gender, category, jornada, date, homeTeam, awayTeam, pairNumber (1-3), homePlayers, awayPlayers, homeSets, awaySets, homeGames, awayGames, winnerSide ('home'|'away'|null), setsPlayed.",
  playerMatches:
    "Un registro por jugador y partido de pareja jugado: leagueId, league, groupId, group, gender, category, jornada, date, player, team, opponentTeam, side ('home'|'away'), pairNumber, partner, won (1 o 0, usa avg para % de victorias), setsWon, setsLost, gamesWon, gamesLost.",
};

export interface QuerySpec {
  table: string;
  filters?: { field: string; op: string; value: unknown }[];
  having?: { field: string; op: string; value: unknown }[];
  groupBy?: string[];
  metrics?: { fn: string; field?: string; as?: string; where?: { field: string; op: string; value: unknown }[] }[];
  orderBy?: string;
  desc?: boolean;
  limit?: number;
  leagueId?: number;
}

const MAX_ROWS = 50;

function toNum(v: string | null | undefined): number | null {
  if (v === null || v === undefined) return null;
  const n = Number.parseFloat(String(v).replace(",", "."));
  return Number.isFinite(n) ? n : null;
}

function base(b: LeagueBundle, g: GroupBundle): Row {
  return {
    leagueId: b.league.id,
    league: b.league.name,
    groupId: g.group.id,
    group: g.group.name,
    gender: g.group.gender,
    category: g.group.category,
  };
}

export function buildRows(table: string, bundles: LeagueBundle[], year: number): Row[] | null {
  const rows: Row[] = [];
  for (const b of bundles) {
    for (const g of b.groups) {
      const bs = base(b, g);
      if (table === "players") {
        for (const t of g.teams) {
          for (const p of t.players) {
            const y = p.birthYear ? Number.parseInt(p.birthYear, 10) : NaN;
            rows.push({
              ...bs,
              teamId: t.teamId,
              team: t.teamName,
              player: p.name,
              isCaptain: !!p.isCaptain,
              points: toNum(p.points),
              birthYear: Number.isFinite(y) ? y : null,
              age: Number.isFinite(y) ? year - y : null,
            });
          }
        }
      } else if (table === "standings") {
        for (const s of g.standings) {
          rows.push({
            ...bs,
            teamId: s.teamId,
            team: s.teamName,
            position: s.position,
            points: s.points,
            played: s.matchesPlayed,
            encountersWon: s.encountersWon,
            encountersLost: s.encountersLost,
            matchesWon: s.matchesWon,
            matchesLost: s.matchesLost,
            setsWon: s.setsWon,
            setsLost: s.setsLost,
            gamesWon: s.gamesWon,
            gamesLost: s.gamesLost,
          });
        }
      } else if (table === "matches" || table === "teamMatches") {
        for (const m of g.results) {
          const h = toNum(m.localScore);
          const a = toNum(m.visitorScore);
          const played = h !== null && a !== null;
          if (table === "matches") rows.push({
            ...bs,
            jornada: m.jornada,
            date: m.date ?? null,
            venue: m.venue ?? null,
            homeTeam: m.localTeam,
            awayTeam: m.visitorTeam,
            homeScore: h,
            awayScore: a,
            played,
            winner: played && h !== a ? (h! > a! ? m.localTeam : m.visitorTeam) : null,
            totalPairsWon: played ? Math.max(h!, a!) : null,
          });
          if (table === "teamMatches" && played) {
            for (const [team, opp, side, f, ag] of [
              [m.localTeam, m.visitorTeam, "home", h!, a!],
              [m.visitorTeam, m.localTeam, "away", a!, h!],
            ] as [string, string, string, number, number][]) {
              rows.push({ ...bs, jornada: m.jornada, date: m.date ?? null, team, opponent: opp, side, won: f === ag ? null : f > ag ? 1 : 0, pairsFor: f, pairsAgainst: ag, homeWins: side === "home" && f > ag ? 1 : 0, awayWins: side === "away" && f > ag ? 1 : 0, homeGames: side === "home" ? 1 : 0, awayGames: side === "away" ? 1 : 0 });
            }
          }
        }
      } else if (table === "pairs" || table === "playerMatches") {
        for (const m of g.results) {
          const d = m.detailUrl ? g.matchDetails[m.detailUrl] : undefined;
          if (!d) continue;
          for (const p of d.pairs) {
            if (!p.sets.length) continue;
            const hs = p.sets.filter((s) => s.localScore > s.visitorScore).length;
            const as = p.sets.filter((s) => s.visitorScore > s.localScore).length;
            const hg = p.sets.reduce((n, s) => n + s.localScore, 0);
            const ag = p.sets.reduce((n, s) => n + s.visitorScore, 0);
            const winnerSide = hs === as ? null : hs > as ? "home" : "away";
            const common = { ...bs, jornada: m.jornada, date: m.date ?? null };
            const home = [p.localPlayer1, p.localPlayer2].map((x) => x.trim()).filter(Boolean);
            const away = [p.visitorPlayer1, p.visitorPlayer2].map((x) => x.trim()).filter(Boolean);
            if (table === "pairs") {
              rows.push({
                ...common,
                homeTeam: m.localTeam,
                awayTeam: m.visitorTeam,
                pairNumber: p.pairNumber,
                homePlayers: home.join(" / "),
                awayPlayers: away.join(" / "),
                homeSets: hs,
                awaySets: as,
                homeGames: hg,
                awayGames: ag,
                winnerSide,
                setsPlayed: p.sets.length,
              });
            } else {
              for (const [names, isHome] of [[home, true], [away, false]] as [string[], boolean][]) {
                for (const name of names) {
                  rows.push({
                    ...common,
                    player: name,
                    team: isHome ? m.localTeam : m.visitorTeam,
                    opponentTeam: isHome ? m.visitorTeam : m.localTeam,
                    side: isHome ? "home" : "away",
                    pairNumber: p.pairNumber,
                    partner: names.find((n) => n !== name) ?? null,
                    won: winnerSide === null ? null : (winnerSide === "home") === isHome ? 1 : 0,
                    setsWon: isHome ? hs : as,
                    setsLost: isHome ? as : hs,
                    gamesWon: isHome ? hg : ag,
                    gamesLost: isHome ? ag : hg,
                  });
                }
              }
            }
          }
        }
      } else {
        return null;
      }
    }
  }
  return rows;
}

function matches(row: Row, f: { field: string; op: string; value: unknown }): boolean {
  const v = row[f.field];
  const op = f.op;
  if (op === "contains") {
    const w = normalize(String(f.value ?? "")).split(/\s+/).filter(Boolean);
    const hay = normalize(String(v ?? ""));
    return w.every((x) => hay.includes(x));
  }
  if (op === "in") {
    const set = (Array.isArray(f.value) ? f.value : [f.value]).map((x) => normalize(String(x)));
    return set.includes(normalize(String(v ?? "")));
  }
  if (v === null || v === undefined) return op === "ne";
  const numeric = typeof v === "number" || typeof v === "boolean";
  const a: string | number = typeof v === "boolean" ? (v ? 1 : 0) : (v as string | number);
  const b: string | number = numeric ? Number(f.value === true ? 1 : f.value === false ? 0 : f.value) : normalize(String(f.value));
  const left = numeric ? a : normalize(String(a));
  switch (op) {
    case "eq": return left === b;
    case "ne": return left !== b;
    case "gt": return left > b;
    case "gte": return left >= b;
    case "lt": return left < b;
    case "lte": return left <= b;
    default: return false;
  }
}

export function runQuery(rows: Row[], spec: QuerySpec): { rows: Row[]; totalGroups: number } | { error: string } {
  let data = rows;
  for (const f of spec.filters ?? []) {
    if (!f || typeof f.field !== "string") return { error: "invalid_filter" };
    data = data.filter((r) => matches(r, f));
  }
  const metrics = spec.metrics ?? [];
  const groupBy = spec.groupBy ?? [];
  let out: Row[];
  if (!metrics.length && !groupBy.length) {
    out = data.map((r) => ({ ...r }));
  } else {
    const ms = metrics.length ? metrics : [{ fn: "count" }];
    const buckets = new Map<string, Row[]>();
    for (const r of data) {
      const key = JSON.stringify(groupBy.map((f) => r[f] ?? null));
      const arr = buckets.get(key);
      if (arr) arr.push(r);
      else buckets.set(key, [r]);
    }
    out = [];
    for (const rs of buckets.values()) {
      const o: Row = {};
      for (const f of groupBy) o[f] = rs[0][f] ?? null;
      for (const m of ms) {
        if (m.fn !== "count" && !m.field) return { error: `metric_needs_field:${m.fn} (usa 'field', p.ej. 'won' o 'homeWins')` };
        const name = m.as ?? (m.field ? `${m.fn}_${m.field}` : m.fn);
        const wh = m.where ? (Array.isArray(m.where) ? m.where : [m.where as never]) : [];
        const rsm = wh.length ? rs.filter((r) => wh.every((w) => matches(r, w))) : rs;
        const vals = m.field
          ? rsm.map((r) => r[m.field!]).filter((x) => x !== null && x !== undefined).map((x) => (typeof x === "boolean" ? (x ? 1 : 0) : x))
          : [];
        const nums = vals.filter((x): x is number => typeof x === "number");
        switch (m.fn) {
          case "count": o[name] = m.field ? vals.length : rsm.length; break;
          case "sum": o[name] = nums.reduce((a, x) => a + x, 0); break;
          case "avg": o[name] = nums.length ? Math.round((nums.reduce((a, x) => a + x, 0) / nums.length) * 100) / 100 : null; break;
          case "min": o[name] = nums.length ? Math.min(...nums) : null; break;
          case "max": o[name] = nums.length ? Math.max(...nums) : null; break;
          case "countDistinct": o[name] = new Set(vals).size; break;
          default: return { error: `unknown_metric:${m.fn}` };
        }
      }
      out.push(o);
    }
  }
  for (const f of spec.having ?? []) {
    if (!f || typeof f.field !== "string") return { error: "invalid_having" };
    out = out.filter((r) => {
      const key = typeof f.value === "string" ? f.value.replace(/^@/, "") : "";
      const ref = key && key in r ? r[key] : undefined;
      return matches(r, ref === undefined ? f : { ...f, value: ref });
    });
  }
  if (spec.orderBy) {
    const k = spec.orderBy;
    const dir = spec.desc ? -1 : 1;
    out.sort((a, b) => {
      const x = a[k] ?? null;
      const y = b[k] ?? null;
      if (x === y) return 0;
      if (x === null) return 1;
      if (y === null) return -1;
      return (x > y ? 1 : -1) * dir;
    });
  }
  const limit = Math.min(Math.max(Number(spec.limit) || 20, 1), MAX_ROWS);
  return { rows: out.slice(0, limit), totalGroups: out.length };
}
