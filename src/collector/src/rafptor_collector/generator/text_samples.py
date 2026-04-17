"""Synthetic, deterministic text samples for test/demo documents.

No real client data — every name, account number and amount is fabricated.
The per-document RNG is seeded so the same ``(page_num, doc_name)`` pair
always produces the same output.
"""

from __future__ import annotations

import random

_HEADER_LINES: list[tuple[int, int, str]] = [
    (150, 200, "BANQUE NATIONALE DE DEMONSTRATION"),
    (150, 260, "Service des Operations Bancaires"),
    (150, 320, "12 rue de la Paix, 75002 Paris"),
    (150, 380, "Tel: 01 42 00 00 00"),
]

_OPERATIONS: list[str] = [
    "Virement recu",
    "Prelevement EDF",
    "Carte bancaire CARREFOUR",
    "Virement emis",
    "Prelevement ORANGE",
    "Retrait distributeur",
    "Cheque numero",
    "Commission tenue compte",
    "Carte bancaire SNCF",
    "Prelevement assurance",
    "Virement salaire",
    "Carte bancaire AMAZON",
    "Prelevement impots",
    "Remise cheque",
    "Frais de tenue de compte",
]


def _seed_for(page_num: int, doc_name: str) -> int:
    # hash() is randomised across runs; use a stable deterministic hash.
    h = 0
    for ch in doc_name:
        h = (h * 131 + ord(ch)) & 0xFFFFFFFF
    return (page_num * 1000 + h) & 0xFFFFFFFF


def get_sample_text(
    page_num: int = 1,
    total_pages: int = 1,
    lines: int = 30,
    doc_name: str = "TESTDOC",
) -> list[tuple[int, int, str]]:
    """Return positioned text lines in L-units (300 dpi)."""
    result: list[tuple[int, int, str]] = []
    y = 200

    if page_num == 1:
        result.extend(_HEADER_LINES)
        y = 500
        result.append((150, y, "-" * 70))
        y += 60
        result.append((150, y, f"Releve de compte - Document {doc_name}"))
        y += 60
        result.append((150, y, f"Page {page_num}/{total_pages}"))
        y += 80
        result.append(
            (150, y, f"{'Date':<12} {'Description':<35} {'Debit':>10} {'Credit':>10} {'Solde':>12}")
        )
        y += 40
        result.append((150, y, "-" * 70))
        y += 50
    else:
        result.append((150, y, f"Releve de compte (suite) - Page {page_num}/{total_pages}"))
        y += 80

    rng = random.Random(_seed_for(page_num, doc_name))
    solde = 1523.45 + page_num * 100

    available_lines = max(1, min(lines, (3200 - y) // 50))
    for _ in range(available_lines):
        day = rng.randint(1, 28)
        month = rng.randint(1, 12)
        operation = rng.choice(_OPERATIONS)
        is_debit = rng.random() > 0.4
        amount = round(rng.uniform(5, 500), 2)
        if is_debit:
            solde -= amount
            debit_str = f"{amount:>10.2f}"
            credit_str = " " * 10
        else:
            solde += amount
            debit_str = " " * 10
            credit_str = f"{amount:>10.2f}"
        line = (
            f"{day:02d}/{month:02d}/2024  "
            f"{operation:<35} {debit_str} {credit_str} {solde:>12.2f}"
        )
        result.append((150, y, line))
        y += 50

    result.append((150, 3300, f"Page {page_num}/{total_pages}"))
    result.append((1800, 3300, "BANQUE NATIONALE DE DEMONSTRATION"))
    return result
