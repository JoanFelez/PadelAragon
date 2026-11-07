import { describe, expect, it } from "vitest";
import { generatePossibleCouples, THRESHOLDS } from "../src/chat/couples";
import parity from "../../../contracts/fixtures/couples.parity.json";

describe("possibleCouples parity with GeneratePossibleCouplesUseCase", () => {
  it("uses the desktop thresholds", () => {
    expect(THRESHOLDS).toEqual(parity.thresholds);
  });

  for (const c of parity.cases) {
    it(c.name, () => {
      const res = generatePossibleCouples(c.players, c.gender as "MASCULINA" | "FEMENINA");
      if (c.expectedCount === 0) {
        expect(res.status).toBe("no_combinations_possible");
        return;
      }
      expect(res.status).toBe("success");
      if (res.status !== "success") return;
      expect(res.combinations).toHaveLength(c.expectedCount);
      expect(res.combinations[0].totalAgeSum).toBe(c.expectedMinTotalAge);
      const t = (parity.thresholds as any)[c.gender] as number[];
      for (const combo of res.combinations) {
        expect(combo.pairs.map((p) => p.requiredSum)).toEqual(t);
        expect(combo.pairs.every((p) => p.ageSum >= p.requiredSum)).toBe(true);
      }
    });
  }

  it("reports not enough / too many players", () => {
    const p = (n: number) => Array.from({ length: n }, (_, i) => ({ name: `P${i}`, age: 50 }));
    expect(generatePossibleCouples(p(5), "MASCULINA").status).toBe("not_enough_players");
    expect(generatePossibleCouples(p(7), "MASCULINA", 6).status).toBe("too_many_players");
  });
});
