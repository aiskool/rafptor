"""Rafptor automated QA — AFP → PDF conversion quality validator."""

from .scoring.models import QaDecision, QaScore, QaVerdict

__version__ = "0.1.0"
__all__ = ["QaDecision", "QaScore", "QaVerdict", "__version__"]
