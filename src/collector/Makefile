.PHONY: install lint typecheck test test-cov all clean demo

install:
	pip install -e .[dev]

lint:
	ruff check src tests

typecheck:
	mypy src

test:
	pytest

test-cov:
	pytest --cov=src/rafptor_collector --cov-report=term-missing --cov-fail-under=80

demo:
	python -m rafptor_collector.cli simulate --scenario simple --output /tmp/rafptor-demo-bundle

all: lint typecheck test-cov

clean:
	rm -rf .pytest_cache .mypy_cache .ruff_cache build dist *.egg-info
	find . -name __pycache__ -type d -exec rm -rf {} +
