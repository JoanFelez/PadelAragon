import type { Gender } from "../types";

export interface AgedPlayer {
  name: string;
  age: number;
}

export interface SuggestedPair {
  player1: AgedPlayer;
  player2: AgedPlayer;
  tierIndex: number;
  requiredSum: number;
  ageSum: number;
}

export interface CoupleCombination {
  pairs: SuggestedPair[];
  totalAgeSum: number;
}

export type CouplesResult =
  | { status: "success"; combinations: CoupleCombination[] }
  | { status: "no_combinations_possible" | "not_enough_players" | "too_many_players" };

export const THRESHOLDS: Record<Gender, number[]> = {
  MASCULINA: [95, 100, 105],
  FEMENINA: [85, 90, 95],
};
export const MIN_REQUIRED_PLAYERS = 6;

// Port of GeneratePossibleCouplesUseCase (desktop app).
export function generatePossibleCouples(players: AgedPlayer[], gender: Gender, maxSelected = 16): CouplesResult {
  if (players.length < MIN_REQUIRED_PLAYERS) return { status: "not_enough_players" };
  if (players.length > maxSelected) return { status: "too_many_players" };

  const thresholds = THRESHOLDS[gender];
  const combinations: CoupleCombination[] = [];
  const seen = new Set<string>();
  const n = players.length;
  const sum = (p: [number, number]) => players[p[0]].age + players[p[1]].age;

  for (let i1 = 0; i1 < n; i1++)
    for (let j1 = i1 + 1; j1 < n; j1++)
      for (let i2 = 0; i2 < n; i2++) {
        if (i2 === i1 || i2 === j1) continue;
        for (let j2 = i2 + 1; j2 < n; j2++) {
          if (j2 === i1 || j2 === j1) continue;
          if (i2 < i1 || (i2 === i1 && j2 <= j1)) continue;
          for (let i3 = 0; i3 < n; i3++) {
            if (i3 === i1 || i3 === j1 || i3 === i2 || i3 === j2) continue;
            for (let j3 = i3 + 1; j3 < n; j3++) {
              if (j3 === i1 || j3 === j1 || j3 === i2 || j3 === j2) continue;
              if (i3 < i2 || (i3 === i2 && j3 <= j2)) continue;
              const sorted = ([[i1, j1], [i2, j2], [i3, j3]] as [number, number][]).sort((a, b) => sum(a) - sum(b));
              if (!sorted.every((p, i) => sum(p) >= thresholds[i])) continue;
              const key = sorted
                .map(([a, b]) => [players[a].name, players[b].name].sort().join(","))
                .join("|");
              if (seen.has(key)) continue;
              seen.add(key);
              const pairs = sorted.map(([a, b], i) => ({
                player1: players[a],
                player2: players[b],
                tierIndex: i,
                requiredSum: thresholds[i],
                ageSum: sum([a, b]),
              }));
              combinations.push({ pairs, totalAgeSum: pairs.reduce((s, p) => s + p.ageSum, 0) });
            }
          }
        }
      }

  if (combinations.length === 0) return { status: "no_combinations_possible" };
  combinations.sort((a, b) => a.totalAgeSum - b.totalAgeSum);
  return { status: "success", combinations };
}
