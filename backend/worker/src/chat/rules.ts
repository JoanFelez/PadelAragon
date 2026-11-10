export interface RuleSection {
  label: string;
  title: string;
  text: string;
  number?: string;
  tag?: string;
}

// "## Art. 8.5 (Liga) Título": number may be dotted; the optional (Tag) tells which document it comes from.
const ARTICLE = /^##\s*Art(?:ículo|\.)?\s*\.?\s*(\d+(?:\.\d+)*)\.?(?:\s*\(([^)]+)\))?/i;

export function normalize(s: string): string {
  return s.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
}

export function parseRules(markdown: string): RuleSection[] {
  const sections: RuleSection[] = [];
  let current: { heading: string; lines: string[] } | null = null;
  const flush = () => {
    if (!current) return;
    const m = ARTICLE.exec(current.heading);
    const title = current.heading.replace(/^##\s*/, "").trim();
    sections.push({
      label: m ? `Art. ${m[1]}${m[2] ? ` (${m[2]})` : ""}` : title,
      title,
      text: current.lines.join("\n").trim(),
      number: m?.[1],
      tag: m?.[2],
    });
  };
  for (const line of markdown.split("\n")) {
    if (/^##\s/.test(line)) {
      flush();
      current = { heading: line, lines: [] };
    } else if (current) {
      current.lines.push(line);
    }
  }
  flush();
  return sections;
}

const STOPWORDS = new Set(["de", "la", "el", "los", "las", "un", "una", "que", "en", "y", "o", "se", "es", "por", "con", "para", "del", "al", "como", "si", "no", "mi", "su"]);

function keywords(text: string): string[] {
  return normalize(text)
    .split(/[^a-z0-9]+/)
    .filter((w) => w.length > 2 && !STOPWORDS.has(w));
}

// Gemini copes with the whole rulebook (~270 KB); keyword selection only kicks in beyond this.
export const RULES_BUDGET_CHARS = 450000;

export const COMPACT_RULES_BUDGET_CHARS = 20000;

export function selectRules(sections: RuleSection[], question: string, budget = RULES_BUDGET_CHARS): RuleSection[] {
  const render = (s: RuleSection) => s.title.length + s.text.length + 8;
  const total = sections.reduce((n, s) => n + render(s), 0);
  if (total <= budget) return sections;

  const words = [...new Set(keywords(question))];
  const scored = sections
    .map((s, index) => {
      const hay = normalize(`${s.title} ${s.text}`);
      const titleHay = normalize(s.title);
      let score = 0;
      for (const w of words) {
        if (hay.includes(w)) score += 1;
        if (titleHay.includes(w)) score += 2;
      }
      return { s, index, score };
    })
    .filter((x) => x.score > 0)
    .sort((a, b) => b.score - a.score || a.index - b.index);

  const picked: typeof scored = [];
  let used = 0;
  for (const x of scored) {
    const size = render(x.s);
    if (used + size > budget) continue;
    picked.push(x);
    used += size;
  }
  return picked.sort((a, b) => a.index - b.index).map((x) => x.s);
}

export function formatRules(sections: RuleSection[]): string {
  return sections.map((s) => `## ${s.title}\n${s.text}`).join("\n\n");
}

export function findCitedRules(answer: string, all: RuleSection[]): RuleSection[] {
  const found = new Map<string, RuleSection>();
  const re = /Art(?:ículo|iculo|\.)?\s*\.?\s*(\d+(?:\.\d+)*)(?:\s*\(([^)]+)\))?/gi;
  for (const m of answer.matchAll(re)) {
    for (const s of all) {
      if (s.number !== m[1]) continue;
      if (m[2] && s.tag && normalize(m[2]) !== normalize(s.tag)) continue;
      found.set(s.label, s);
    }
  }
  return [...found.values()];
}
