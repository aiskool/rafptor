"""Font-mapping database abstractions.

``InMemoryFontDatabase`` is the default used in tests and dev.
``MongoFontDatabase`` is provided as a thin wrapper around ``pymongo`` and
is only constructed when MongoDB is actually reachable; the dependency is
imported lazily so that environments without MongoDB still work.
"""

from __future__ import annotations

from dataclasses import asdict
from datetime import datetime, timezone
from typing import Protocol

from ..matching.models import MatchResult


class FontDatabase(Protocol):
    def save_mapping(self, result: MatchResult, client_id: str) -> None: ...
    def find_existing_mapping(self, afp_font_name: str) -> dict | None: ...
    def get_mapping_stats(self) -> dict: ...


class InMemoryFontDatabase:
    """Reference implementation used by tests and the dev CLI."""

    def __init__(self) -> None:
        self._items: list[dict] = []

    def save_mapping(self, result: MatchResult, client_id: str) -> None:
        doc = asdict(result)
        doc["client_id"] = client_id
        doc["validated_at"] = datetime.now(timezone.utc).isoformat()
        doc["validated"] = True
        self._items.append(doc)

    def find_existing_mapping(self, afp_font_name: str) -> dict | None:
        for doc in self._items:
            if doc.get("afp_font_name") == afp_font_name:
                return doc
        return None

    def get_mapping_stats(self) -> dict:
        total = len(self._items)
        confidences = {"high": 0, "medium": 0, "low": 0}
        for doc in self._items:
            confidences[doc.get("confidence", "low")] = (
                confidences.get(doc.get("confidence", "low"), 0) + 1
            )
        return {"total": total, "confidences": confidences}


class MongoFontDatabase:
    """Thin MongoDB wrapper. Imports pymongo lazily."""

    def __init__(self, uri: str, database: str = "rafptor") -> None:
        try:
            from pymongo import MongoClient  # type: ignore[import-not-found]
        except ImportError as exc:
            raise RuntimeError(
                "pymongo is required for MongoFontDatabase but is not installed"
            ) from exc
        self._client = MongoClient(uri)
        self._db = self._client[database]
        self._coll = self._db["font_mappings"]

    def save_mapping(self, result: MatchResult, client_id: str) -> None:
        doc = asdict(result)
        doc["client_id"] = client_id
        doc["validated_at"] = datetime.now(timezone.utc).isoformat()
        doc["validated"] = True
        self._coll.insert_one(doc)

    def find_existing_mapping(self, afp_font_name: str) -> dict | None:
        return self._coll.find_one({"afp_font_name": afp_font_name})

    def get_mapping_stats(self) -> dict:
        total = self._coll.count_documents({})
        by_confidence: dict[str, int] = {}
        for doc in self._coll.find({}, {"confidence": 1}):
            level = doc.get("confidence", "low")
            by_confidence[level] = by_confidence.get(level, 0) + 1
        return {"total": total, "confidences": by_confidence}
