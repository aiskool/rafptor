# Secure Transfer Module — Replacing Axway Transfer CFT

## The CFT lock-in problem

Axway Transfer CFT is deeply embedded in enterprise BFSI infrastructure. While it delivers reliable file transfer, it comes with significant cost and complexity:

| Cost factor | Typical range | Source |
|-------------|---------------|--------|
| Annual license | ~$75,000 | Industry estimates, Coviant Software analysis (2026) |
| Support renewal | ~$30,000/year | Industry estimates |
| Implementation | 3-9+ months | G2 user reviews, Axway partner reports |
| MFT engineers | $88,500-$200,000/year | Indeed salary data |
| Total 5-year TCO | $500K-$1M+ | License + support + staff + professional services |

These costs are often disproportionate to the actual transfer workload, especially for AFP migration where the pattern is predictable: large batch files moving from mainframe to processing platform on a scheduled basis.

## Rafptor's approach: purpose-built secure transfer

Instead of building a general-purpose MFT competitor, Rafptor includes a lightweight, purpose-built secure transfer module optimized specifically for AFP migration workloads.

### Design principles

1. **Open standards only** — SFTP and HTTPS. No proprietary protocols (no PeSIT dependency). Any standard SFTP client or server can interoperate.

2. **Zero infrastructure overhead** — The transfer module runs as part of the collector agent on the mainframe side, and as a microservice on the Rafptor platform side. No separate MFT server to deploy, license, or maintain.

3. **Built-in reliability** — Checkpoint/restart, integrity verification (SHA-256 checksums), automatic retry with exponential backoff, transfer resume on network failure. Enterprise-grade delivery guarantees without enterprise-grade pricing.

4. **Compliance-ready** — Full audit trail (every transfer logged with timestamp, source, destination, size, checksum, status), encryption at rest (AES-256) and in transit (TLS 1.3), non-repudiation via signed transfer receipts.

5. **Bandwidth-aware** — Configurable throttling to avoid impacting production mainframe workloads during business hours. Scheduled transfer windows. Priority queues.

### Architecture

```
CLIENT MAINFRAME                              RAFPTOR PLATFORM
┌────────────────────────────┐                ┌────────────────────────────┐
│                            │                │                            │
│  ┌──────────────────────┐  │                │  ┌──────────────────────┐  │
│  │  Rafptor Collector   │  │                │  │  Rafptor Receiver    │  │
│  │                      │  │                │  │                      │  │
│  │  1. Inventory AFP    │  │   SFTP/TLS 1.3 │  │  1. Validate bundle  │  │
│  │  2. Package bundle   │──│───────────────>│──│  2. Verify checksums │  │
│  │  3. Encrypt (AES)    │  │   Checkpoint/  │  │  3. Decrypt          │  │
│  │  4. Transfer         │  │   restart      │  │  4. Route to parser  │  │
│  │  5. Verify receipt   │<─│───────────────<│──│  5. Send receipt     │  │
│  └──────────────────────┘  │                │  └──────────────────────┘  │
│                            │                │                            │
│  No Axway CFT required     │                │  Runs as microservice      │
│  No MFT server             │                │  Docker / Kubernetes       │
│  No additional license     │                │  Auto-scales               │
└────────────────────────────┘                └────────────────────────────┘
```

### Transfer bundle format

The collector packages all AFP assets into a single encrypted bundle:

```
rafptor-bundle-{timestamp}.rpb
├── manifest.json          # Inventory, checksums, metadata
├── streams/               # AFP print streams (binary AFPDS)
│   ├── batch_001.afp
│   └── batch_002.afp
├── resources/             # External resources
│   ├── fonts/             # Raster, outline, TrueType fonts
│   ├── overlays/          # OVL files
│   ├── formdefs/          # FDF files
│   ├── pagedefs/          # PDF files
│   └── pagesegments/      # PSG files
└── collection.log         # Agent activity log
```

The bundle is encrypted with AES-256 before transfer. Manifest includes SHA-256 checksums for every file, enabling end-to-end integrity verification.

### Reliability guarantees

| Feature | Implementation |
|---------|---------------|
| Guaranteed delivery | SHA-256 checksum verification + signed receipt |
| Checkpoint/restart | Transfer resumes from last successfully transferred block on failure |
| Automatic retry | Exponential backoff (1s, 2s, 4s, 8s... max 5min), configurable max attempts |
| Bandwidth control | Configurable throttle (MB/s), time-window scheduling |
| Transfer monitoring | Real-time status via REST API + webhook notifications |
| Audit trail | Every transfer logged: timestamp, size, duration, checksum, status, receipt |
| Non-repudiation | Digitally signed transfer receipts (RSA-2048 or Ed25519) |

### Technology choice: Go or Rust

The transfer module should be implemented in Go or Rust for:

- **Minimal footprint** — binary deploys anywhere, no runtime dependencies (JVM, Python, etc.)
- **High performance** — concurrent transfers, efficient memory usage for multi-GB files
- **Cross-compilation** — single codebase compiles to IBM i (PASE), z/OS (USS), Linux, containers
- **Security** — memory-safe (Rust) or garbage-collected (Go), no buffer overflow risks

Go is the pragmatic choice (faster development, excellent SFTP libraries like `pkg/sftp`, easy cross-compilation). Rust is the premium choice (zero-cost abstractions, tighter security guarantees, but steeper learning curve).

### Migration path from Transfer CFT

For clients currently using Axway Transfer CFT, Rafptor provides a phased migration path:

**Phase 1 — Coexistence** : Rafptor transfer module runs alongside existing CFT. AFP migration flows use Rafptor; other business flows continue on CFT. Zero disruption.

**Phase 2 — Gradual migration** : As the client gains confidence, non-AFP file transfer workloads can optionally migrate to Rafptor's transfer module or to lighter MFT alternatives (e.g., AWS Transfer Family, SFTP-only solutions at ~$1K/year).

**Phase 3 — CFT retirement** : Once all critical flows are migrated, the client can decommission CFT entirely, saving $75K+/year in license costs alone.

### ROI calculation for clients

| Item | With CFT | With Rafptor | Annual saving |
|------|----------|-------------|---------------|
| MFT license | $75,000 | $0 (included) | $75,000 |
| MFT support | $30,000 | $0 (included) | $30,000 |
| MFT engineer (0.5 FTE) | $75,000 | $0 | $75,000 |
| **Total transfer costs** | **$180,000/yr** | **$0** | **$180,000/yr** |

This saving alone can fund the entire Rafptor subscription, making the AFP migration essentially self-financing through CFT cost elimination.

### Competitive positioning

Rafptor is NOT a general-purpose MFT competitor. It does not aim to replace CFT for all file transfer needs (B2B/EDI, partner exchanges, etc.). It specifically replaces CFT for:

- AFP document stream transfers (mainframe → processing platform)
- Migration-related batch transfers
- Document delivery (converted PDFs back to client systems)

This focused scope means Rafptor's transfer module can be simpler, lighter, and more reliable than a general-purpose MFT for these specific workloads — without the $75K/year price tag.

## Security and compliance

| Requirement | How Rafptor addresses it |
|-------------|--------------------------|
| DORA (EU) | Full audit trail, encryption, guaranteed delivery, incident reporting |
| SOX | Non-repudiation, transfer logging, access control |
| HIPAA | AES-256 encryption at rest, TLS 1.3 in transit, access logging |
| PCI-DSS | Encrypted data flows, no sensitive data stored in transit |
| GDPR | Data minimization (bundle only what's needed), encryption, audit |

## References

- Coviant Software — Axway MFT Alternative analysis (2026)
- G2 — Axway MFT reviews and implementation timelines
- Indeed — MFT engineer salary data (2025)
- DORA regulation — Digital Operational Resilience Act requirements
