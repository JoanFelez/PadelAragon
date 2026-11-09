import { describe, expect, it } from "vitest";
import { bundleFixture } from "./helpers";
import type { LeagueBundle } from "../src/types";
import manifest from "../../../contracts/fixtures/manifest.sample.json";

describe("contract fixtures (schemaVersion 1)", () => {
  const bundle = bundleFixture as unknown as LeagueBundle;

  it("bundle has the documented shape", () => {
    expect(bundle.schemaVersion).toBe(1);
    expect(Object.keys(bundle).sort()).toEqual(["generatedAt", "groups", "league", "schemaVersion"]);
    expect(Number.isNaN(Date.parse(bundle.generatedAt))).toBe(false);
    for (const g of bundle.groups) {
      expect(Object.keys(g).sort()).toEqual(["group", "jornadas", "matchDetails", "results", "standings", "teams"]);
      expect(["MASCULINA", "FEMENINA"]).toContain(g.group.gender);
      for (const r of g.results) {
        if (r.detailUrl) expect(g.matchDetails[r.detailUrl]).toBeDefined();
        expect(g.jornadas).toContain(r.jornada);
      }
      const teamIds = new Set(g.teams.map((t) => t.teamId));
      for (const s of g.standings) expect(teamIds.has(s.teamId)).toBe(true);
    }
  });

  it("manifest has the documented shape", () => {
    expect(Object.keys(manifest).sort()).toEqual(["generatedAt", "leagues", "schemaVersion"]);
    for (const l of manifest.leagues) {
      expect(Object.keys(l).sort()).toEqual(["hash", "id", "name", "updatedAt"]);
      expect(l.hash).toMatch(/^[0-9a-f]{64}$/);
    }
  });
});
