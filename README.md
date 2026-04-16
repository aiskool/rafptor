# Rafptor

**End-to-end AFP migration orchestration and automation platform.**

Rafptor is a SaaS platform designed to automate the migration of IBM AFP (Advanced Function Presentation) document workflows to modern formats (PDF/PDF-A), targeting enterprises in banking, insurance, and government sectors locked into legacy AFP/IPDS ecosystems.

---

## The problem

Large enterprises (banks, insurers, public sector) are locked into **two** costly IBM-era dependencies:

**1. IBM AFP** — Advanced Function Presentation for high-volume document production (statements, invoices, policies). AFP is deeply integrated into mainframe workflows, and IBM has discontinued its own AFP tooling (AFP Utilities dropped in IBM i 7.3, InfoPrint Designer EOL). Migration remains colossal because AFP is not just a file format — it's an ecosystem of interdependent architectures (MO:DCA, PTOCA, GOCA, IOCA, BCOCA, FOCA, CMOCA, MOCA), proprietary fonts, external resources, and decades of custom configurations.

**2. Axway Transfer CFT** — The enterprise MFT layer used to move AFP streams and other files across mainframes, distributed systems, and partners. Annual contracts typically start around $75K with ~$30K/year support renewals. Implementation requires 3-9+ months with dedicated project teams. Specialized MFT engineers command $88K-$200K/year salaries. For many organizations, CFT has become an expensive dependency that could be replaced by modern, standards-based secure transfer at a fraction of the cost.

Rafptor eliminates both lock-ins in a single platform.

## The solution

Rafptor provides an automated pipeline that handles the full migration lifecycle:

| Layer | Component | Function |
|-------|-----------|----------|
| 1 | **Collector agent** | Deployed on IBM i / z/OS — inventories and exports AFP streams + all referenced resources |
| 2 | **Secure transfer (built-in)** | Replaces Axway Transfer CFT — lightweight, standards-based (SFTP/TLS 1.3), guaranteed delivery, checkpoint/restart, full audit trail |
| 3 | **Ingestion & normalization** | MO:DCA parsing, external resource resolution, deduplication, complexity classification |
| 4 | **Intelligent mapping** | Deterministic mapping for standard IBM fonts + AI-powered mapping for custom fonts via computer vision |
| 5 | **Conversion engine** | AFP → intermediate XML representation → high-fidelity PDF/PDF-A with embedded fonts |
| 6 | **Automated QA** | Pixel-diff visual comparison, structural validation, metadata verification, confidence scoring |
| 7 | **Dashboard** | Real-time migration tracking, quality metrics, exception management, human review queue |

### Why replace Transfer CFT?

Rafptor includes a purpose-built secure transfer module that replaces expensive MFT solutions:

| | Axway Transfer CFT | Rafptor Secure Transfer |
|--|---------------------|------------------------|
| **Annual cost** | ~$75K license + ~$30K support | Included in platform |
| **Implementation** | 3-9+ months | Same-day (part of collector agent) |
| **Specialized staff** | MFT engineers ($88K-$200K/yr) | Not required |
| **Protocols** | PeSIT (proprietary), SFTP | SFTP, HTTPS (open standards only) |
| **Encryption** | TLS 1.3, PGP | TLS 1.3, AES-256, end-to-end |
| **Delivery guarantee** | Yes (PeSIT) | Yes (checkpoint/restart, integrity verification) |
| **Audit/compliance** | Yes | Yes (DORA, SOX, HIPAA-ready) |
| **Platforms** | 16+ OS | Linux, IBM i, z/OS, containers |

## Target market

- **Mainframe market**: $5.3B in 2024, growing at 5-7% CAGR
- **Mainframe modernization market**: $8.4B in 2025, projected $13.3B by 2030 (9.7% CAGR)
- **BFSI sector**: 45.6% of mainframe market share
- **Key trend**: AFP expertise retiring, IBM disengaging from AFP tools, DORA regulation pushing modernization

## Tech stack

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| AFP parser / conversion | Java 17+ | All OSS AFP parsers are Java-based; enterprise ecosystem fit |
| AI analysis / font mapping | Python (PyTorch, OpenCV) | Mature ML/CV ecosystem for glyph comparison |
| Collector agent | RPGLE/CL, JCL/REXX | Native IBM i / z/OS languages for mainframe resource access |
| Secure transfer | Go or Rust (SFTP/TLS 1.3) | Lightweight, high-performance replacement for Axway Transfer CFT |
| Dashboard | React / TypeScript | Modern frontend stack |
| Backend API | Spring Boot or Node.js | Pipeline orchestration, job management, REST API |

## Key technical references

- [AFP Consortium — MO:DCA, PTOCA, GOCA specifications](https://www.afpconsortium.org)
- [ISO 18565:2015 — AFP/Archive](https://www.iso.org/standard/62901.html)
- [ISO 22550:2021 — AFP interchange for PDF](https://www.iso.org/standard/83085.html)
- [Alpheus AFP Parser (Java, GPL v3)](https://github.com/afpdev/alpheusafpparser)
- [afpbox (Java)](https://github.com/michaelknigge/afpbox)
- [Apache FOP — AFP Renderer](https://xmlgraphics.apache.org/fop/)

## Project structure

```
rafptor/
├── docs/
│   ├── development-plan.md              # 18-month plan, milestones, staffing
│   ├── architecture/
│   │   ├── secure-transfer-cft-replacement.md
│   │   ├── security-architecture.md     # Threat model, crypto, compliance
│   │   ├── timeline.md                  # Gantt + milestones
│   │   └── afp_migration_feasibility.pdf
│   └── pwa/                             # Interactive architecture viewer (PWA, port 4040)
├── src/                                 # Source modules (scaffolded)
│   ├── parser/                          # Java 17 — AFP / MO:DCA
│   ├── transport/                       # Go 1.22 — replaces Axway Transfer CFT
│   ├── collector/                       # RPGLE / JCL — mainframe agent
│   ├── mapper/                          # Python 3.12 — font mapping
│   ├── converter/                       # Java 17 — AFP → PDF/A
│   ├── validator/                       # Python 3.12 — automated QA
│   ├── api/                             # Spring Boot 3 — backend
│   └── dashboard/                       # React 18 + TS — frontend
├── tests/
│   └── fixtures/                        # AFP samples (simple / medium / complex / adversarial)
├── .github/
│   ├── workflows/ci.yml + security.yml
│   ├── dependabot.yml
│   ├── CODEOWNERS
│   └── pull_request_template.md
├── CONTRIBUTING.md
├── SECURITY.md
├── LICENSE
└── README.md
```

## Documentation index

| Document | Purpose |
|----------|---------|
| [`docs/development-plan.md`](docs/development-plan.md) | Full 18-month plan: phases, modules, milestones, staffing, risks |
| [`docs/architecture/security-architecture.md`](docs/architecture/security-architecture.md) | Banking-grade security architecture, threat model, compliance matrix |
| [`docs/architecture/secure-transfer-cft-replacement.md`](docs/architecture/secure-transfer-cft-replacement.md) | Why and how Rafptor replaces Axway Transfer CFT |
| [`docs/architecture/timeline.md`](docs/architecture/timeline.md) | ASCII Gantt + milestone calendar |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Branching model, commit conventions, PR process |
| [`SECURITY.md`](SECURITY.md) | Vulnerability disclosure policy |

## Status

**Phase**: Planning complete — Phase 1 (Foundations) begins at T0 = 2026-05-01  
**MVP target**: 2027-08-01 (15 months from T0)  
**Current focus**: Hiring, lab environment setup, Phase 1 kickoff

## License

Proprietary — All rights reserved.

---

*Rafptor — because legacy AFP shouldn't hold your documents hostage.*
