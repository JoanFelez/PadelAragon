# -*- coding: utf-8 -*-
from datetime import datetime
from pathlib import Path

from fpdf import FPDF


OUTPUT_PATH = Path("/home/joanfelez/Escritorio/Proyectos/PadelAragon/PadelAragon_Data_Schema.pdf")

FONT_REGULAR = Path("/usr/share/fonts/abattis-cantarell-fonts/Cantarell-Regular.otf")
FONT_BOLD = Path("/usr/share/fonts/abattis-cantarell-fonts/Cantarell-Bold.otf")

COLOR_HEADER_BG = (46, 125, 50)  # #2E7D32
COLOR_HEADER_TEXT = (255, 255, 255)
COLOR_ROW_A = (248, 252, 248)
COLOR_ROW_B = (236, 245, 236)
COLOR_BORDER = (200, 220, 200)
COLOR_TEXT = (30, 30, 30)


class SchemaPDF(FPDF):
    def footer(self):
        self.set_y(-12)
        self.set_font("PadelFont", size=9)
        self.set_text_color(110, 110, 110)
        self.cell(0, 8, f"Página {self.page_no()}", align="C")



def add_fonts(pdf: FPDF) -> None:
    if not FONT_REGULAR.exists() or not FONT_BOLD.exists():
        raise FileNotFoundError(
            f"No se encontraron fuentes Unicode requeridas: {FONT_REGULAR} / {FONT_BOLD}"
        )

    pdf.add_font("PadelFont", style="", fname=str(FONT_REGULAR))
    pdf.add_font("PadelFont", style="B", fname=str(FONT_BOLD))



def title_page(pdf: FPDF) -> None:
    pdf.add_page()
    pdf.set_text_color(*COLOR_TEXT)

    pdf.ln(35)
    pdf.set_font("PadelFont", style="B", size=42)
    pdf.cell(0, 18, "PadelAragón", align="C", new_x="LMARGIN", new_y="NEXT")

    pdf.ln(8)
    pdf.set_font("PadelFont", style="", size=18)
    pdf.cell(
        0,
        12,
        "Esquema de Datos Extraídos - Liga de Aragón 2026",
        align="C",
        new_x="LMARGIN",
        new_y="NEXT",
    )

    pdf.ln(5)
    pdf.set_font("PadelFont", style="", size=13)
    pdf.set_text_color(70, 70, 70)
    pdf.cell(0, 10, "Marzo 2026", align="C", new_x="LMARGIN", new_y="NEXT")

    pdf.ln(16)
    pdf.set_text_color(*COLOR_TEXT)
    pdf.set_font("PadelFont", style="", size=12)
    pdf.multi_cell(
        0,
        8,
        "Fuente principal: https://arapadel.com/Pages/26-liga\n"
        "Datos alojados en padelfederacion.es\n"
        "Prueba de caracteres UTF-8: ñ á é í ó ú ü ¡¿",
        align="C",
    )



def section_heading(pdf: FPDF, title: str, source: str) -> None:
    pdf.set_text_color(*COLOR_TEXT)
    pdf.set_font("PadelFont", style="B", size=16)
    pdf.cell(0, 11, title, new_x="LMARGIN", new_y="NEXT")

    pdf.set_font("PadelFont", style="", size=11)
    pdf.set_text_color(70, 70, 70)
    pdf.multi_cell(0, 6, f"Scraped from: {source}")
    pdf.ln(2)



def table(pdf: FPDF, headers: list[str], rows: list[tuple[str, str, str]]) -> None:
    col_widths = [38, 42, 110]

    pdf.set_font("PadelFont", style="B", size=11)
    pdf.set_fill_color(*COLOR_HEADER_BG)
    pdf.set_text_color(*COLOR_HEADER_TEXT)
    for i, header in enumerate(headers):
        pdf.cell(col_widths[i], 9, header, border=1, fill=True)
    pdf.ln()

    pdf.set_text_color(*COLOR_TEXT)
    pdf.set_font("PadelFont", style="", size=10)

    for idx, row in enumerate(rows):
        fill_color = COLOR_ROW_A if idx % 2 == 0 else COLOR_ROW_B
        pdf.set_fill_color(*fill_color)

        x_start = pdf.get_x()
        y_start = pdf.get_y()

        # Estimate row height from wrapped text in each column.
        line_counts = []
        for value, width in zip(row, col_widths):
            text_width = max(pdf.get_string_width(value), 1)
            lines = int(text_width // (width - 4)) + 1
            line_counts.append(max(lines, 1))
        row_height = max(line_counts) * 6

        # Draw colored cells first.
        current_x = x_start
        for width in col_widths:
            pdf.set_xy(current_x, y_start)
            pdf.set_draw_color(*COLOR_BORDER)
            pdf.cell(width, row_height, "", border=1, fill=True)
            current_x += width

        # Then place text inside each cell.
        current_x = x_start
        for value, width in zip(row, col_widths):
            pdf.set_xy(current_x + 1.5, y_start + 1.0)
            pdf.multi_cell(width - 3, 5, value, border=0)
            current_x += width

        pdf.set_xy(x_start, y_start + row_height)



def add_group_section(pdf: FPDF) -> None:
    pdf.add_page()
    section_heading(
        pdf,
        "1. Grupos / Categorías (LeagueGroup)",
        "Ligas_Calendario.asp - <select name=\"grupo\">",
    )

    rows = [
        ("id", "Entero", "ID del grupo (ej: 30941), usado en parámetros URL"),
        ("name", "Texto", "Nombre completo (ej: 1ª CATEGORÍA MASCULINA - GRUPO A)"),
        ("gender", "Enumeración", "MASCULINA o FEMENINA"),
        ("category", "Texto", "Categoría (ej: 1ª CATEGORÍA)"),
        ("groupLetter", "Texto (opcional)", "Letra de grupo (ej: A, B) o nulo si no hay subdivisión"),
    ]
    table(pdf, ["Campo", "Tipo", "Descripción"], rows)

    pdf.ln(5)
    pdf.set_text_color(*COLOR_TEXT)
    pdf.set_font("PadelFont", style="", size=10)
    pdf.multi_cell(
        0,
        6,
        "Los grupos se organizan bajo <optgroup label=\"Masculina\"> y "
        "<optgroup label=\"Femenina\"> en el HTML.\n"
        "Aproximadamente 24 grupos: ~15 Masculina, ~9 Femenina.",
    )



def add_standings_section(pdf: FPDF) -> None:
    pdf.add_page()
    section_heading(
        pdf,
        "2. Clasificación / Standings (StandingRow)",
        "Ligas_Clasificacion.asp (POST con parámetros Liga y grupo)",
    )

    pdf.set_font("PadelFont", size=10)
    pdf.set_text_color(*COLOR_TEXT)
    pdf.multi_cell(0, 6, "Filas HTML: tr.LineasRnk con 12 celdas <td>")
    pdf.ln(2)

    rows = [
        ("position", "Entero", "Posición en la clasificación"),
        ("teamName", "Texto", "Nombre del equipo"),
        ("teamId", "Entero", "ID del equipo (extraído del href del enlace)"),
        ("points", "Entero", "Puntos (Pt)"),
        ("matchesPlayed", "Entero", "Jornadas Jugadas (JJ)"),
        ("encountersWon", "Entero", "Encuentros Ganados (EG)"),
        ("encountersLost", "Entero", "Encuentros Perdidos (EP)"),
        ("matchesWon", "Entero", "Partidos Ganados (PG)"),
        ("matchesLost", "Entero", "Partidos Perdidos (PP)"),
        ("setsWon", "Entero", "Sets Ganados (SG)"),
        ("setsLost", "Entero", "Sets Perdidos (SP)"),
        ("gamesWon", "Entero", "Juegos Ganados (JG)"),
        ("gamesLost", "Entero", "Juegos Perdidos (JP)"),
    ]
    table(pdf, ["Campo", "Tipo", "Descripción"], rows)



def add_results_section(pdf: FPDF) -> None:
    pdf.add_page()
    section_heading(
        pdf,
        "3. Resultados de Partidos (MatchResult)",
        "Ligas_Calendario.asp (GET con Liga, grupo, jornada)",
    )

    pdf.set_font("PadelFont", size=10)
    pdf.set_text_color(*COLOR_TEXT)
    pdf.multi_cell(0, 6, "Filas HTML: tr.LineasRnk")
    pdf.ln(2)

    rows = [
        ("localTeam", "Texto", "Nombre del equipo local"),
        ("localTeamId", "Entero", "ID del equipo local"),
        ("visitorTeam", "Texto", "Nombre del equipo visitante"),
        ("visitorTeamId", "Entero", "ID del equipo visitante"),
        ("localScore", "Texto", "Marcador local (ej: 3, o -- si no se ha jugado)"),
        ("visitorScore", "Texto", "Marcador visitante"),
        ("date", "Texto (opcional)", "Fecha del partido"),
        ("venue", "Texto (opcional)", "Sede / lugar del partido"),
        ("jornada", "Entero", "Número de jornada"),
    ]
    table(pdf, ["Campo", "Tipo", "Descripción"], rows)

    pdf.ln(4)
    pdf.set_font("PadelFont", size=10)
    pdf.multi_cell(
        0,
        6,
        "Casos especiales:\n"
        "- Marcador --: partido no jugado aún\n"
        "- Equipo Descansa (id = -2): ronda de descanso\n"
        "- ID de equipo desconocido = -1",
    )



def add_rounds_and_footer_section(pdf: FPDF) -> None:
    pdf.add_page()
    section_heading(
        pdf,
        "4. Jornadas (Round numbers)",
        "Ligas_Calendario.asp - <select name=\"jornada\">",
    )

    pdf.set_font("PadelFont", size=11)
    pdf.set_text_color(*COLOR_TEXT)
    pdf.multi_cell(
        0,
        7,
        "Lista simple de enteros que representa los números de jornada disponibles.",
    )

    pdf.ln(8)
    pdf.set_font("PadelFont", style="B", size=13)
    pdf.cell(0, 9, "Enfoque de scraping", new_x="LMARGIN", new_y="NEXT")

    pdf.set_font("PadelFont", size=11)
    pdf.set_fill_color(242, 247, 242)
    pdf.set_draw_color(*COLOR_BORDER)
    approach_text = (
        "Los datos se extraen mediante web scraping de las páginas HTML del servidor "
        "padelfederacion.es usando Jsoup. Las páginas utilizan codificación ISO-8859-1. "
        "No existe API REST; toda la información se obtiene parseando tablas HTML."
    )
    x = pdf.get_x()
    y = pdf.get_y()
    w = pdf.w - pdf.l_margin - pdf.r_margin
    h = 34
    pdf.rect(x, y, w, h, style="DF")
    pdf.set_xy(x + 3, y + 3)
    pdf.multi_cell(w - 6, 7, approach_text)

    pdf.ln(5)
    pdf.set_text_color(70, 70, 70)
    pdf.set_font("PadelFont", size=9)
    generated = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    pdf.cell(0, 6, f"Documento generado: {generated}")



def build_pdf() -> None:
    pdf = SchemaPDF(orientation="P", unit="mm", format="A4")
    pdf.set_auto_page_break(auto=True, margin=15)
    add_fonts(pdf)

    title_page(pdf)
    add_group_section(pdf)
    add_standings_section(pdf)
    add_results_section(pdf)
    add_rounds_and_footer_section(pdf)

    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    pdf.output(str(OUTPUT_PATH))


if __name__ == "__main__":
    build_pdf()
