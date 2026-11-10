import { bundleKey } from "../data";
import type { GroupBundle, LeagueBundle, LeagueRef, MatchResult, TeamBundle } from "../types";
import { generatePossibleCouples } from "./couples";
import { normalize } from "./rules";

export interface ToolDeclaration {
  name: string;
  description: string;
  parameters: Record<string, unknown>;
}

export interface ToolOutcome {
  result: unknown;
  source?: { tool: string; label: string };
}

const MAX_ITEMS = 40;

export const TOOL_DECLARATIONS: ToolDeclaration[] = [
  {
    name: "listLeagues",
    description: "Lista las ligas disponibles (id y nombre).",
    parameters: { type: "object", properties: {} },
  },
  {
    name: "findGroups",
    description: "Busca grupos/categorías por texto (por ejemplo '1ª categoría masculina grupo A').",
    parameters: {
      type: "object",
      properties: {
        query: { type: "string", description: "Texto a buscar en el nombre del grupo" },
        leagueId: { type: "integer", description: "Opcional: id de liga" },
      },
      required: ["query"],
    },
  },
  {
    name: "findTeams",
    description:
      "Busca equipos por nombre (parcial, sin importar mayúsculas ni acentos) en todas las ligas y grupos. Úsala siempre que el usuario mencione un equipo; devuelve id, nombre, grupo y liga.",
    parameters: {
      type: "object",
      properties: {
        query: { type: "string", description: "Parte del nombre del equipo, por ejemplo 'sandra style'" },
        leagueId: { type: "integer", description: "Opcional: id de liga" },
      },
      required: ["query"],
    },
  },
  {
    name: "getStandings",
    description: "Clasificación de un grupo.",
    parameters: {
      type: "object",
      properties: { groupId: { type: "integer" }, leagueId: { type: "integer" } },
      required: ["groupId"],
    },
  },
  {
    name: "getTeam",
    description: "Detalle de un equipo: clasificación, partidos, jugadores y capitán. Acepta id o nombre.",
    parameters: {
      type: "object",
      properties: { team: { type: "string", description: "Id numérico o nombre del equipo" }, leagueId: { type: "integer" } },
      required: ["team"],
    },
  },
  {
    name: "getResults",
    description: "Resultados de un grupo, opcionalmente de una jornada.",
    parameters: {
      type: "object",
      properties: { groupId: { type: "integer" }, jornada: { type: "integer" }, leagueId: { type: "integer" } },
      required: ["groupId"],
    },
  },
  {
    name: "getPlayerStats",
    description: "Victorias/derrotas de un jugador como local y visitante y en qué pareja (1/2/3) ha jugado.",
    parameters: {
      type: "object",
      properties: { name: { type: "string" }, leagueId: { type: "integer" } },
      required: ["name"],
    },
  },
  {
    name: "possibleCouples",
    description:
      "Combinaciones posibles de 3 parejas que cumplen la suma mínima de edades (M 95/100/105, F 85/90/95) dados los jugadores disponibles de un equipo.",
    parameters: {
      type: "object",
      properties: {
        team: { type: "string", description: "Id numérico o nombre del equipo" },
        available: { type: "array", items: { type: "string" }, description: "Nombres de los jugadores disponibles" },
        leagueId: { type: "integer" },
      },
      required: ["team", "available"],
    },
  },
];

export class ToolRunner {
  private bundles = new Map<number, LeagueBundle | null>();
  private leagues: LeagueRef[] | null = null;

  constructor(
    private kv: KVNamespace,
    private defaultLeagueId?: number,
    private currentYear = new Date().getFullYear(),
  ) {}

  private async listLeagues(): Promise<LeagueRef[]> {
    if (!this.leagues) {
      const raw = await this.kv.get("leagues", "text");
      this.leagues = raw ? (JSON.parse(raw) as LeagueRef[]) : [];
    }
    return this.leagues;
  }

  private async bundle(id: number): Promise<LeagueBundle | null> {
    if (!this.bundles.has(id)) {
      const raw = await this.kv.get(bundleKey(id), "text");
      this.bundles.set(id, raw ? (JSON.parse(raw) as LeagueBundle) : null);
    }
    return this.bundles.get(id)!;
  }

  private async scope(leagueId?: number): Promise<LeagueBundle[]> {
    const id = leagueId ?? this.defaultLeagueId;
    const ids = id !== undefined ? [id] : (await this.listLeagues()).map((l) => l.id);
    const out: LeagueBundle[] = [];
    for (const i of ids) {
      const b = await this.bundle(i);
      if (b) out.push(b);
    }
    return out;
  }

  private async groupById(groupId: number, leagueId?: number) {
    for (const b of await this.scope(leagueId)) {
      const g = b.groups.find((x) => x.group.id === groupId);
      if (g) return { league: b.league, g };
    }
    return null;
  }

  private async teamLookup(team: string | number, leagueId?: number) {
    const asId = /^\d+$/.test(String(team)) ? Number(team) : null;
    const wanted = normalize(String(team)).trim();
    const words = wanted.split(/\s+/).filter(Boolean);
    let partial: { league: LeagueRef; g: GroupBundle; t: TeamBundle } | null = null;
    for (const b of await this.scope(leagueId)) {
      for (const g of b.groups) {
        for (const t of g.teams) {
          if (asId !== null && t.teamId === asId) return { league: b.league, g, t };
          const n = normalize(t.teamName);
          if (n === wanted) return { league: b.league, g, t };
          if (!partial && words.length && words.every((w) => n.includes(w))) partial = { league: b.league, g, t };
        }
      }
    }
    return partial;
  }

  private async findTeams(query: string, leagueId?: number) {
    const words = normalize(query).split(/\s+/).filter(Boolean);
    const found: unknown[] = [];
    if (!words.length) return found;
    for (const b of await this.scope(leagueId)) {
      for (const g of b.groups) {
        for (const t of g.teams) {
          const n = normalize(t.teamName);
          if (!words.every((w) => n.includes(w))) continue;
          const standing = g.standings.find((s) => s.teamId === t.teamId) ?? null;
          found.push({ leagueId: b.league.id, league: b.league.name, groupId: g.group.id, group: g.group.name, teamId: t.teamId, teamName: t.teamName, standing });
        }
      }
    }
    return found;
  }

  async execute(name: string, args: Record<string, any>): Promise<ToolOutcome> {
    try {
      switch (name) {
        case "listLeagues": {
          return { result: await this.listLeagues(), source: { tool: name, label: "Ligas" } };
        }
        case "findGroups": {
          const words = normalize(String(args.query ?? "")).split(/\s+/).filter(Boolean);
          const found: unknown[] = [];
          for (const b of await this.scope(args.leagueId)) {
            for (const { group } of b.groups) {
              const hay = normalize(group.name);
              if (words.every((w) => hay.includes(w))) found.push({ leagueId: b.league.id, ...group });
            }
          }
          return { result: found.slice(0, MAX_ITEMS), source: { tool: name, label: `Grupos: ${args.query}` } };
        }
        case "findTeams": {
          const found = await this.findTeams(String(args.query ?? ""), args.leagueId);
          return { result: found.slice(0, MAX_ITEMS), source: { tool: name, label: `Equipos: ${args.query}` } };
        }
        case "getStandings": {
          const r = await this.groupById(Number(args.groupId), args.leagueId);
          if (!r) return { result: { error: "group_not_found" } };
          return {
            result: { group: r.g.group.name, standings: r.g.standings },
            source: { tool: name, label: `Clasificación ${r.g.group.name}` },
          };
        }
        case "getTeam": {
          const r = await this.teamLookup(args.team, args.leagueId);
          if (!r) return { result: { error: "team_not_found" } };
          const { g, t } = r;
          const matches = g.results.filter((m) => m.localTeamId === t.teamId || m.visitorTeamId === t.teamId);
          return {
            result: {
              teamId: t.teamId,
              teamName: t.teamName,
              group: g.group.name,
              groupId: g.group.id,
              standing: g.standings.find((s) => s.teamId === t.teamId) ?? null,
              matches: matches.sort((a, b) => a.jornada - b.jornada),
              category: t.category,
              captainName: t.captainName,
              players: t.players.map((p) => ({ ...p, age: this.age(p.birthYear) })),
            },
            source: { tool: name, label: `Equipo ${t.teamName}` },
          };
        }
        case "getResults": {
          const r = await this.groupById(Number(args.groupId), args.leagueId);
          if (!r) return { result: { error: "group_not_found" } };
          const jornada = args.jornada === undefined ? undefined : Number(args.jornada);
          const results: MatchResult[] = r.g.results.filter((m) => jornada === undefined || m.jornada === jornada);
          return {
            result: { group: r.g.group.name, jornadas: r.g.jornadas, results: results.slice(0, MAX_ITEMS) },
            source: { tool: name, label: `Resultados ${r.g.group.name}${jornada ? ` J${jornada}` : ""}` },
          };
        }
        case "getPlayerStats": {
          const wanted = normalize(String(args.name ?? "")).trim();
          if (!wanted) return { result: { error: "name_required" } };
          return { result: await this.playerStats(wanted, args.leagueId), source: { tool: name, label: `Jugador ${args.name}` } };
        }
        case "possibleCouples": {
          const r = await this.teamLookup(args.team, args.leagueId);
          if (!r) return { result: { error: "team_not_found" } };
          const available: string[] = Array.isArray(args.available) ? args.available.map(String) : [];
          const aged = [];
          const unresolved: string[] = [];
          for (const wanted of available) {
            const w = normalize(wanted).trim();
            const p = r.t.players.find((x) => normalize(x.name).includes(w) || w.includes(normalize(x.name)));
            const age = p ? this.age(p.birthYear) : null;
            if (p && age !== null) aged.push({ name: p.name, age });
            else unresolved.push(wanted);
          }
          const res = generatePossibleCouples(aged, r.g.group.gender);
          const base = { team: r.t.teamName, gender: r.g.group.gender, unresolved };
          const source = { tool: "possibleCouples", label: `Parejas posibles ${r.t.teamName}` };
          if (res.status !== "success") return { result: { ...base, status: res.status }, source };
          return {
            result: {
              ...base,
              status: res.status,
              totalCombinations: res.combinations.length,
              combinations: res.combinations.slice(0, 10),
            },
            source,
          };
        }
        default:
          return { result: { error: `unknown_tool:${name}` } };
      }
    } catch (e) {
      return { result: { error: "tool_failed", detail: e instanceof Error ? e.message : String(e) } };
    }
  }

  private age(birthYear?: string | null): number | null {
    const y = birthYear ? Number.parseInt(birthYear, 10) : NaN;
    if (!Number.isFinite(y) || y < 1900 || y > this.currentYear) return null;
    return this.currentYear - y;
  }

  private async playerStats(wanted: string, leagueId?: number) {
    type Split = { wins: number; losses: number; pair1Count: number; pair2Count: number; pair3Count: number };
    const blank = (): Split => ({ wins: 0, losses: 0, pair1Count: 0, pair2Count: 0, pair3Count: 0 });
    const stats = new Map<string, { name: string; home: Split; away: Split }>();

    for (const b of await this.scope(leagueId)) {
      for (const g of b.groups) {
        for (const m of g.results) {
          const detail = m.detailUrl ? g.matchDetails[m.detailUrl] : undefined;
          if (!detail) continue;
          for (const pair of detail.pairs) {
            const localSets = pair.sets.filter((s) => s.localScore > s.visitorScore).length;
            const visitorSets = pair.sets.filter((s) => s.visitorScore > s.localScore).length;
            if (pair.sets.length === 0 || localSets === visitorSets) continue;
            const localWon = localSets > visitorSets;
            for (const [players, isHome] of [
              [[pair.localPlayer1, pair.localPlayer2], true],
              [[pair.visitorPlayer1, pair.visitorPlayer2], false],
            ] as [string[], boolean][]) {
              for (const raw of players) {
                const name = raw.trim();
                if (!name || !normalize(name).includes(wanted)) continue;
                const key = normalize(name);
                const acc = stats.get(key) ?? { name, home: blank(), away: blank() };
                const split = isHome ? acc.home : acc.away;
                if (isHome === localWon) split.wins++;
                else split.losses++;
                const k = `pair${pair.pairNumber}Count` as "pair1Count" | "pair2Count" | "pair3Count";
                if (k in split) split[k]++;
                stats.set(key, acc);
              }
            }
          }
        }
      }
    }
    const list = [...stats.values()].slice(0, 5);
    return list.length ? list : { error: "player_not_found" };
  }
}
