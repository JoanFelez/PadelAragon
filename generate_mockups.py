# -*- coding: utf-8 -*-
from __future__ import annotations

from datetime import datetime
from pathlib import Path
import re

from fpdf import FPDF

OUTPUT_PATH = Path("/home/joanfelez/Escritorio/Proyectos/PadelAragon/PadelAragon_Data_Schema.pdf")

FONT_CANDIDATES = [
    Path("/usr/share/fonts/liberation-sans-fonts/LiberationSans-Regular.ttf"),
    Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
    Path("/usr/share/fonts/TTF/DejaVuSans.ttf"),
]

COLOR_GREEN = (46, 125, 50)
COLOR_TEXT = (30, 30, 30)
COLOR_HEADER_TEXT = (255, 255, 255)
COLOR_ROW_A = (248, 252, 248)
COLOR_ROW_B = (236, 245, 236)
COLOR_BORDER = (200, 220, 200)
COLOR_LIGHT_GRAY = (245, 245, 245)
COLOR_SHADOW = (210, 210, 210)


class MockupPDF(FPDF):
    def footer(self) -> None:
        self.set_y(-12)
        try:
            self.set_font("MainFont", size=9)
        except Exception:
            self.set_font("helvetica", size=9)
        self.set_text_color(110, 110, 110)
        self.cell(0, 8, f"Página {self.page_no()}", align="C")


def detect_existing_pdf_pages(pdf_path: Path) -> int:
    if not pdf_path.exists():
        return 0

    try:
        from pypdf import PdfReader  # type: ignore

        return len(PdfReader(str(pdf_path)).pages)
    except Exception:
        pass

    try:
        raw = pdf_path.read_bytes()
        matches = re.findall(rb"/Count\s+(\d+)", raw)
        if matches:
            return max(int(m.decode("ascii", errors="ignore")) for m in matches)
    except Exception:
        pass

    return 0


def add_fonts(pdf: FPDF) -> None:
    for path in FONT_CANDIDATES:
        if path.exists():
            pdf.add_font("MainFont", style="", fname=str(path))
            pdf.add_font("MainFont", style="B", fname=str(path))
            return

    pdf.set_font("helvetica", size=12)


def set_font(pdf: FPDF, bold: bool = False, size: int = 11) -> None:
    try:
        pdf.set_font("MainFont", style="B" if bold else "", size=size)
    except Exception:
        pdf.set_font("helvetica", style="B" if bold else "", size=size)


def section_heading(pdf: FPDF, title: str, source: str) -> None:
    pdf.set_text_color(*COLOR_TEXT)
    set_font(pdf, bold=True, size=16)
    pdf.cell(0, 11, title, new_x="LMARGIN", new_y="NEXT")

    set_font(pdf, bold=False, size=11)
    pdf.set_text_color(70, 70, 70)
    pdf.multi_cell(0, 6, f"Fuente: {source}")
    pdf.ln(2)


def draw_schema_table(pdf: FPDF, rows: list[tuple[str, str, str]]) -> None:
    headers = ["Campo", "Tipo", "Descripción"]
    widths = [42, 44, 104]

    set_font(pdf, bold=True, size=11)
    pdf.set_fill_color(*COLOR_GREEN)
    pdf.set_text_color(*COLOR_HEADER_TEXT)
    for i, header in enumerate(headers):
        pdf.cell(widths[i], 9, header, border=1, fill=True)
    pdf.ln()

    set_font(pdf, bold=False, size=10)
    pdf.set_text_color(*COLOR_TEXT)

    for idx, row in enumerate(rows):
        pdf.set_fill_color(*(COLOR_ROW_A if idx % 2 == 0 else COLOR_ROW_B))
        x0, y0 = pdf.get_x(), pdf.get_y()

        line_counts: list[int] = []
        for value, width in zip(row, widths):
            tw = max(pdf.get_string_width(value), 1)
            line_counts.append(max(int(tw // max(width - 5, 1)) + 1, 1))
        row_h = max(line_counts) * 5.5 + 1

        x = x0
        for width in widths:
            pdf.set_xy(x, y0)
            pdf.set_draw_color(*COLOR_BORDER)
            pdf.cell(width, row_h, "", border=1, fill=True)
            x += width

        x = x0
        for value, width in zip(row, widths):
            pdf.set_xy(x + 1.5, y0 + 1)
            pdf.multi_cell(width - 3, 5, value, border=0)
            x += width

        pdf.set_xy(x0, y0 + row_h)


def page_title(pdf: FPDF, original_pages: int) -> None:
    pdf.add_page()
    pdf.set_text_color(*COLOR_TEXT)

    pdf.ln(35)
    set_font(pdf, bold=True, size=42)
    pdf.cell(0, 18, "PadelAragón", align="C", new_x="LMARGIN", new_y="NEXT")

    pdf.ln(8)
    set_font(pdf, bold=False, size=18)
    pdf.cell(
        0,
        12,
        "Esquema de Datos Extraídos — Liga de Aragón 2026",
        align="C",
        new_x="LMARGIN",
        new_y="NEXT",
    )

    pdf.ln(5)
    set_font(pdf, bold=False, size=13)
    pdf.set_text_color(70, 70, 70)
    pdf.cell(0, 10, "Marzo 2026", align="C", new_x="LMARGIN", new_y="NEXT")

    pdf.ln(14)
    pdf.set_text_color(*COLOR_TEXT)
    set_font(pdf, bold=False, size=11)
    pdf.multi_cell(
        0,
        7,
        f"Documento regenerado con esquema de datos y mockups UI. Páginas detectadas en PDF previo: {original_pages}",
        align="C",
    )


def page_groups(pdf: FPDF) -> None:
    pdf.add_page()
    section_heading(pdf, "Grupos / Categorías", 'Ligas_Calendario.asp — <select name="grupo"> dropdown')
    rows = [
        ("id", "Entero", "Identificador del grupo"),
        ("name", "Texto", "Nombre completo del grupo"),
        ("gender", "Enumeración", "MASCULINA o FEMENINA"),
        ("category", "Texto", "Categoría de competición"),
        ("groupLetter", "Texto opcional", "Letra de grupo o nulo"),
    ]
    draw_schema_table(pdf, rows)


def page_standings(pdf: FPDF) -> None:
    pdf.add_page()
    section_heading(pdf, "Clasificación / Standings", "Ligas_Clasificacion.asp (POST)")
    rows = [
        ("position", "Entero", "Posición"),
        ("teamName", "Texto", "Nombre del equipo"),
        ("teamId", "Entero", "ID del equipo"),
        ("points", "Entero", "Puntos"),
        ("matchesPlayed", "Entero", "Partidos jugados"),
        ("encountersWon", "Entero", "Encuentros ganados"),
        ("encountersLost", "Entero", "Encuentros perdidos"),
        ("matchesWon", "Entero", "Partidos ganados"),
        ("matchesLost", "Entero", "Partidos perdidos"),
        ("setsWon", "Entero", "Sets ganados"),
        ("setsLost", "Entero", "Sets perdidos"),
        ("gamesWon", "Entero", "Juegos ganados"),
        ("gamesLost", "Entero", "Juegos perdidos"),
    ]
    draw_schema_table(pdf, rows)


def page_results(pdf: FPDF) -> None:
    pdf.add_page()
    section_heading(pdf, "Resultados de Partidos", "Ligas_Calendario.asp (GET)")
    rows = [
        ("localTeam", "Texto", "Equipo local"),
        ("localTeamId", "Entero", "ID equipo local"),
        ("visitorTeam", "Texto", "Equipo visitante"),
        ("visitorTeamId", "Entero", "ID equipo visitante"),
        ("localScore", "Texto", "Marcador local"),
        ("visitorScore", "Texto", "Marcador visitante"),
        ("date", "Texto", "Fecha"),
        ("venue", "Texto", "Sede"),
        ("jornada", "Entero", "Número de jornada"),
    ]
    draw_schema_table(pdf, rows)

    pdf.ln(4)
    set_font(pdf, size=10)
    pdf.multi_cell(0, 6, "Casos especiales:\n- '--' partido no jugado\n- 'Descansa' indica jornada de descanso")


def page_technical_notes(pdf: FPDF) -> None:
    pdf.add_page()
    section_heading(pdf, "Notas Técnicas", "Estrategia de scraping")

    set_font(pdf, bold=True, size=13)
    pdf.set_text_color(*COLOR_TEXT)
    pdf.cell(0, 8, "Enfoque de scraping", new_x="LMARGIN", new_y="NEXT")

    set_font(pdf, size=11)
    pdf.set_fill_color(242, 247, 242)
    pdf.set_draw_color(*COLOR_BORDER)

    content = (
        "Los datos se extraen por scraping HTML con peticiones GET/POST hacia las páginas de liga. "
        "No hay API REST oficial; se parsean tablas y selectores para obtener grupos, clasificación, "
        "jornadas y resultados. Se contemplan partidos no jugados ('--') y descansos ('Descansa')."
    )

    x, y = pdf.get_x(), pdf.get_y()
    w = pdf.w - pdf.l_margin - pdf.r_margin
    h = 44
    pdf.rect(x, y, w, h, style="DF")
    pdf.set_xy(x + 3, y + 3)
    pdf.multi_cell(w - 6, 7, content)

    pdf.ln(6)
    set_font(pdf, size=10)
    pdf.set_text_color(80, 80, 80)
    pdf.cell(0, 6, f"Generado: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")


def page_divider(pdf: FPDF) -> None:
    pdf.add_page()
    pdf.set_y(105)
    pdf.set_text_color(*COLOR_TEXT)
    set_font(pdf, bold=True, size=30)
    pdf.cell(0, 16, "Vista Previa de la Aplicación", align="C", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(4)
    set_font(pdf, size=16)
    pdf.set_text_color(70, 70, 70)
    pdf.cell(0, 10, "Mockups de las pantallas principales", align="C")


def draw_phone(pdf: FPDF, x: float = 65, y: float = 35, w: float = 80, h: float = 150) -> tuple[float, float, float, float]:
    pdf.set_draw_color(60, 60, 60)
    pdf.set_fill_color(250, 250, 250)
    pdf.rect(x, y, w, h, style="DF", round_corners=True)

    sx, sy, sw, sh = x + 4, y + 4, w - 8, h - 8
    pdf.set_draw_color(120, 120, 120)
    pdf.set_fill_color(255, 255, 255)
    pdf.rect(sx, sy, sw, sh, style="DF")
    return sx, sy, sw, sh


def shadow_card(pdf: FPDF, x: float, y: float, w: float, h: float, text: str, size: int = 6) -> None:
    pdf.set_fill_color(*COLOR_SHADOW)
    pdf.rect(x + 0.8, y + 0.8, w, h, style="F")
    pdf.set_fill_color(255, 255, 255)
    pdf.set_draw_color(210, 210, 210)
    pdf.rect(x, y, w, h, style="DF")
    set_font(pdf, size=size)
    pdf.set_text_color(*COLOR_TEXT)
    pdf.set_xy(x + 1.5, y + h / 2 - 2)
    pdf.cell(w - 3, 4, text)


def page_mockup_group_list(pdf: FPDF) -> None:
    pdf.add_page()
    sx, sy, sw, _ = draw_phone(pdf)

    pdf.set_fill_color(*COLOR_GREEN)
    pdf.rect(sx, sy, sw, 14, style="F")
    pdf.set_text_color(255, 255, 255)
    set_font(pdf, bold=True, size=7)
    pdf.set_xy(sx, sy + 5)
    pdf.cell(sw, 4, "Liga de Aragón 2026", align="C")

    y = sy + 18

    def section(label: str) -> None:
        nonlocal y
        pdf.set_fill_color(232, 242, 232)
        pdf.rect(sx + 4, y, sw - 8, 6.5, style="F")
        pdf.set_text_color(*COLOR_GREEN)
        set_font(pdf, bold=True, size=6)
        pdf.set_xy(sx + 6, y + 1.7)
        pdf.cell(sw - 12, 3, label)
        y += 8.2

    section("MASCULINA")
    for label in ["1ª CAT. MASC - A", "1ª CAT. MASC - B", "2ª CAT. MASC - A", "2ª CAT. MASC - B"]:
        shadow_card(pdf, sx + 4, y, sw - 8, 7, label)
        y += 9

    y += 2
    section("FEMENINA")
    for label in ["1ª CAT. FEM", "2ª CAT. FEM"]:
        shadow_card(pdf, sx + 4, y, sw - 8, 7, label)
        y += 9

    pdf.set_text_color(*COLOR_TEXT)
    set_font(pdf, bold=True, size=14)
    pdf.set_xy(20, 200)
    pdf.cell(170, 8, "Pantalla 1: Listado de Grupos", align="C")

    set_font(pdf, size=11)
    pdf.set_xy(20, 210)
    pdf.multi_cell(170, 6, "Vista inicial con grupos por género y categoría. Cada tarjeta abre el detalle del grupo.", align="C")


def draw_tabs(pdf: FPDF, sx: float, sy: float, sw: float, selected: str) -> None:
    tab_y, tab_h = sy + 15, 8
    half = sw / 2
    pdf.set_fill_color(245, 245, 245)
    pdf.rect(sx, tab_y, sw, tab_h, style="F")

    set_font(pdf, bold=(selected == "clasificacion"), size=6)
    pdf.set_text_color(*COLOR_TEXT)
    pdf.set_xy(sx, tab_y + 2.5)
    pdf.cell(half, 3, "Clasificación", align="C")

    set_font(pdf, bold=(selected == "resultados"), size=6)
    pdf.set_xy(sx + half, tab_y + 2.5)
    pdf.cell(half, 3, "Resultados", align="C")

    pdf.set_draw_color(*COLOR_GREEN)
    if selected == "clasificacion":
        pdf.rect(sx + 2, tab_y + tab_h - 1.5, half - 4, 1, style="F")
    else:
        pdf.rect(sx + half + 2, tab_y + tab_h - 1.5, half - 4, 1, style="F")


def page_mockup_standings(pdf: FPDF) -> None:
    pdf.add_page()
    sx, sy, sw, _ = draw_phone(pdf)

    pdf.set_fill_color(*COLOR_GREEN)
    pdf.rect(sx, sy, sw, 14, style="F")
    pdf.set_text_color(255, 255, 255)
    set_font(pdf, bold=True, size=7)
    pdf.set_xy(sx + 3, sy + 5)
    pdf.cell(sw - 6, 4, "← 1ª CAT. MASC - A")

    draw_tabs(pdf, sx, sy, sw, "clasificacion")

    y = sy + 27
    pdf.set_fill_color(*COLOR_LIGHT_GRAY)
    pdf.rect(sx + 2, y, sw - 4, 7, style="F")
    pdf.set_text_color(60, 60, 60)
    set_font(pdf, bold=True, size=6)
    pdf.set_xy(sx + 4, y + 2)
    pdf.cell(sw - 8, 3, "Pos   Equipo                Pt   JJ")

    y += 8
    teams = [
        ("1", "Club Padel XY", "25", "10"),
        ("2", "CD Aragón", "22", "10"),
        ("3", "CT Zaragoza", "18", "10"),
        ("4", "Club Huesca", "15", "10"),
        ("5", "AD Teruel", "12", "9"),
        ("6", "CP Calatayud", "8", "9"),
        ("7", "CD Monzón", "5", "10"),
        ("8", "Club Ejea", "3", "10"),
    ]

    set_font(pdf, size=6)
    for i, (pos, team, pts, jj) in enumerate(teams):
        if i % 2 == 1:
            pdf.set_fill_color(250, 250, 250)
            pdf.rect(sx + 2, y, sw - 4, 6, style="F")
        pdf.set_text_color(*COLOR_TEXT)
        pdf.set_xy(sx + 4, y + 2)
        pdf.cell(sw - 8, 3, f"{pos:<2}   {team:<18} {pts:>2}   {jj:>2}")
        y += 6

    pdf.set_text_color(*COLOR_TEXT)
    set_font(pdf, bold=True, size=14)
    pdf.set_xy(20, 200)
    pdf.cell(170, 8, "Pantalla 2: Clasificación", align="C")

    set_font(pdf, size=11)
    pdf.set_xy(20, 210)
    pdf.multi_cell(170, 6, "Pestaña de clasificación con tabla compacta de posiciones, puntos y jornadas jugadas.", align="C")


def page_mockup_results(pdf: FPDF) -> None:
    pdf.add_page()
    sx, sy, sw, _ = draw_phone(pdf)

    pdf.set_fill_color(*COLOR_GREEN)
    pdf.rect(sx, sy, sw, 14, style="F")
    pdf.set_text_color(255, 255, 255)
    set_font(pdf, bold=True, size=7)
    pdf.set_xy(sx + 3, sy + 5)
    pdf.cell(sw - 6, 4, "← 1ª CAT. MASC - A")

    draw_tabs(pdf, sx, sy, sw, "resultados")

    y = sy + 27
    pdf.set_fill_color(250, 250, 250)
    pdf.rect(sx + 4, y, sw - 8, 8, style="DF")
    pdf.set_text_color(*COLOR_TEXT)
    set_font(pdf, size=6)
    pdf.set_xy(sx + 6, y + 2.5)
    pdf.cell(sw - 12, 3, "▼ Jornada 5")

    y += 11

    def card(line1: str, line2: str) -> None:
        nonlocal y
        pdf.set_fill_color(*COLOR_SHADOW)
        pdf.rect(sx + 4.8, y + 0.8, sw - 9.6, 18, style="F")
        pdf.set_fill_color(255, 255, 255)
        pdf.rect(sx + 4, y, sw - 9.6, 18, style="DF")
        set_font(pdf, size=6)
        pdf.set_text_color(*COLOR_TEXT)
        pdf.set_xy(sx + 6, y + 3)
        pdf.cell(sw - 14, 3, line1)
        pdf.set_xy(sx + 6, y + 12)
        pdf.cell(sw - 14, 3, line2)
        y += 21

    card("Club Padel XY      3-0  CD Aragón", "15/01/26   Club XY")
    card("CT Zaragoza        2-1  Club Huesca", "16/01/26   CT Zgz")
    card("AD Teruel          1-2  CP Calatayud", "15/01/26   AD Ter")

    pdf.set_text_color(*COLOR_TEXT)
    set_font(pdf, bold=True, size=14)
    pdf.set_xy(20, 200)
    pdf.cell(170, 8, "Pantalla 3: Resultados por Jornada", align="C")

    set_font(pdf, size=11)
    pdf.set_xy(20, 210)
    pdf.multi_cell(170, 6, "Pestaña de resultados con selector de jornada y tarjetas de partido con marcador, fecha y sede.", align="C")


def build_pdf() -> None:
    original_pages = detect_existing_pdf_pages(OUTPUT_PATH)

    pdf = MockupPDF(orientation="P", unit="mm", format="A4")
    pdf.set_auto_page_break(auto=True, margin=15)
    add_fonts(pdf)

    page_title(pdf, original_pages)
    page_groups(pdf)
    page_standings(pdf)
    page_results(pdf)
    page_technical_notes(pdf)

    page_divider(pdf)
    page_mockup_group_list(pdf)
    page_mockup_standings(pdf)
    page_mockup_results(pdf)

    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    pdf.output(str(OUTPUT_PATH))


if __name__ == "__main__":
    build_pdf()
