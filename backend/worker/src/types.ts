export interface Env {
  PADEL: KVNamespace;
  AI?: { run(model: string, input: unknown): Promise<unknown> };
  APP_KEY?: string;
  SCRAPER_TOKEN?: string;
  GEMINI_API_KEY?: string;
  GITHUB_DISPATCH_TOKEN?: string;
  GEMINI_MODEL?: string;
  WORKERS_AI_MODEL?: string;
  GITHUB_REPO?: string;
  GITHUB_REF?: string;
}

export type Gender = "MASCULINA" | "FEMENINA";

export interface LeagueRef {
  id: number;
  name: string;
}

export interface LeagueGroup {
  id: number;
  name: string;
  gender: Gender;
  category: string;
  groupLetter?: string | null;
}

export interface StandingRow {
  position: number;
  teamName: string;
  teamId: number;
  teamHref?: string;
  points: number;
  matchesPlayed: number;
  encountersWon: number;
  encountersLost: number;
  matchesWon: number;
  matchesLost: number;
  setsWon: number;
  setsLost: number;
  gamesWon: number;
  gamesLost: number;
}

export interface MatchResult {
  localTeam: string;
  localTeamId: number;
  visitorTeam: string;
  visitorTeamId: number;
  localScore: string;
  visitorScore: string;
  date?: string | null;
  venue?: string | null;
  jornada: number;
  detailUrl?: string | null;
}

export interface Player {
  name: string;
  isCaptain?: boolean;
  points?: string | null;
  birthYear?: string | null;
}

export interface TeamBundle {
  teamId: number;
  teamName: string;
  category?: string | null;
  captainName?: string | null;
  players: Player[];
}

export interface SetScore {
  localScore: number;
  visitorScore: number;
}

export interface PairDetail {
  pairNumber: number;
  localPlayer1: string;
  localPlayer2: string;
  visitorPlayer1: string;
  visitorPlayer2: string;
  sets: SetScore[];
}

export interface MatchDetail {
  pairs: PairDetail[];
}

export interface GroupBundle {
  group: LeagueGroup;
  standings: StandingRow[];
  jornadas: number[];
  results: MatchResult[];
  teams: TeamBundle[];
  matchDetails: Record<string, MatchDetail>;
}

export interface LeagueBundle {
  schemaVersion: number;
  league: LeagueRef;
  generatedAt: string;
  groups: GroupBundle[];
}

export interface ManifestEntry {
  id: number;
  name: string;
  hash: string;
  updatedAt: string;
}

export interface Manifest {
  schemaVersion: number;
  generatedAt: string;
  leagues: ManifestEntry[];
}

export const SCHEMA_VERSION = 1;
