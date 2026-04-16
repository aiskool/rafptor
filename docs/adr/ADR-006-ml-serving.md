# ADR-006 — ML serving: embedded PyTorch vs inference server (Triton)

- **Status:** Accepted
- **Date:** 2026-04-16
- **Deciders:** Lead architect, ML engineer, SRE lead
- **Related modules:** mapper (Module 4)

## Context and problem statement

Module 4 uses computer vision to map AFP custom fonts to TrueType/OpenType equivalents. The inference pattern is **batch + online**: batch when ingesting a large font library (thousands of glyphs), online when the pipeline encounters an unmapped glyph at processing time. Latency target online: < 200 ms per glyph.

## Decision drivers

- **Latency** — online path must not block the conversion pipeline
- **Throughput** — batch path should saturate GPU (when GPU present) and CPU otherwise
- **Deployment portability** — same code runs on-prem (likely CPU-only) and in cloud (GPU available)
- **Operational simplicity** — prefer one container per service rather than a separate inference tier
- **Model governance** — model versioning, SHA-verified artefacts, auditable inferences

## Considered options

- Option A — **Embedded PyTorch** in the mapper Python service
- Option B — **NVIDIA Triton Inference Server** as a sidecar or dedicated tier
- Option C — **TorchServe** as a dedicated tier
- Option D — Export to ONNX and serve via ONNX Runtime embedded

## Decision outcome

**Chosen: Option A (embedded PyTorch) for Phase 2 and Phase 3; re-evaluate before MVP (2027-08-01).**

Phase 2 and 3 workloads do not require multi-GPU, multi-model, or high-QPS serving. Embedded PyTorch minimises operational surface (one container, one image, one deploy) and keeps the feedback loop short while the model is still being iterated.

If Phase 4 performance benchmarks show that a single mapper replica saturates below 50 glyphs/s on the target hardware, we revisit with Triton as the incumbent candidate (ADR-006-revised).

## Pros and cons per option

### Option A — Embedded PyTorch

- Good: one service, one deploy; no IPC serialisation cost; easy debugging.
- Good: works on CPU and GPU with no architectural change.
- Neutral: model loaded once at startup; memory footprint known.
- Bad: no dynamic batching; no multi-model hosting.
- Bad: Python GIL — partial mitigation via `torch.compile` and CPU-only inference.

### Option B — Triton Inference Server

- Good: dynamic batching, multi-model, TensorRT / OpenVINO backends, metrics built-in.
- Good: clear model-governance boundary (models loaded from a signed repository).
- Bad: extra container + network hop; larger operational surface.
- Bad: overkill for Phase 2 scale; adoption cost slows the team.

### Option C — TorchServe

- Good: simpler than Triton, same Python ecosystem.
- Neutral: maintenance by PyTorch Foundation is active but tooling less polished than Triton.

### Option D — ONNX Runtime

- Good: export once, run anywhere; no Python runtime needed.
- Bad: our model uses contrastive embeddings with custom layers — ONNX export adds friction during iteration.
- Defer: revisit if we freeze the model for a stable MVP.

## Consequences

- **Short-term**: `src/mapper/requirements.txt` already pins `torch==2.3.0`, `torchvision==0.18.0`. Dockerfile runs inference in the same container as the mapper service.
- **Model artefacts**: fetched at startup from Vault-backed artefact store (SHA-verified); never baked into the image.
- **Observability**: per-inference latency + confidence histograms exported to Prometheus.
- **Long-term trigger**: if benchmark at Phase 4 Task 4.4 shows < 50 glyphs/s/replica, open ADR-006-revised proposing Triton.

## Links

- `docs/development-plan.md` §3 Module 4
- `docs/architecture/security-architecture.md` §4 Module 4 STRIDE
