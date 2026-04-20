# AFP corpus audit

Generated on 2026-04-20T08:28:24Z.

| File | Size | Pages | Coverage | Used | Env | Ignored | Unknown | Opaque |
|------|-----:|------:|---------:|-----:|----:|--------:|--------:|-------:|
| afplib_asciiAndEbcdicComment.afp | 207 | 0 | 100,00% | 113 | 26 | 68 | 0 |  |
| afplib_asciiComment.afp | 50 | 0 | 100,00% | 0 | 50 | 0 | 0 |  |
| afplib_bim.afp | 260 651 | 0 | 100,00% | 260 539 | 18 | 94 | 0 |  |
| afplib_C0X00006.afp | 151 928 | 0 | 99,96% | 120 | 0 | 151 751 | 57 |  |
| afplib_cs.afp | 425 | 0 | 100,00% | 355 | 36 | 34 | 0 |  |
| afplib_ende.afp | 368 655 | 1 | 99,97% | 741 | 367 436 | 384 | 94 |  |
| afplib_fnirg10.afp | 57 609 | 0 | 99,94% | 0 | 0 | 57 575 | 34 |  |
| afplib_hello.afp | 61 | 0 | 100,00% | 0 | 61 | 0 | 0 |  |
| afplib_IPDSpan.afp | 5 239 | 0 | 100,00% | 5 127 | 18 | 94 | 0 |  |
| afplib_repeatingGroupVariableLength.afp | 369 | 0 | 100,00% | 274 | 36 | 59 | 0 |  |
| afplib_start.afp | 368 661 | 1 | 99,97% | 757 | 367 436 | 366 | 102 |  |
| afplib_unknownSF.afp | 359 | 0 | 100,00% | 0 | 0 | 359 | 0 |  |
| afpworld_01_Health_Coverage.afp | 12 784 | 1 | 100,00% | 7 070 | 5 714 | 0 | 0 |  |
| cmyip_97376.afp | 164 518 | 7 | 98,54% | 38 441 | 854 | 122 813 | 2 410 |  |
| cmyip_font_ttf_courier.afp | 2 007 245 | 2 | 100,00% | 2 447 | 2 004 798 | 0 | 0 |  |
| cmyip_font_ttf.afp | 1 703 687 | 2 | 100,00% | 2 365 | 1 701 322 | 0 | 0 |  |
| cmyip_img.afp | 167 162 | 2 | 98,65% | 3 482 | 70 845 | 90 582 | 2 253 |  |
| cmyip_oc_samples_Bank_Statement_REF.afp | 2 012 643 | 48 | 97,48% | 1 183 315 | 8 508 | 770 031 | 50 789 |  |
| cmyip_oc_samples_Letter_Ref.afp | 806 205 | 1 | 99,64% | 409 871 | 2 324 | 391 076 | 2 934 |  |
| cmyip_original.afp | 624 075 | 4 | 99,59% | 259 284 | 434 | 361 810 | 2 547 |  |
| cmyip_x2.afp | 67 347 | 1 | 95,33% | 747 | 174 | 63 281 | 3 145 |  |
| cmyip_X80_2C.afp | 850 175 | 1 | 99,71% | 74 049 | 42 705 | 730 931 | 2 490 |  |
| fop_expected_named_resource.afp | 21 494 | 0 | 100,00% | 21 362 | 34 | 98 | 0 |  |
| fop_expected_resource.afp | 21 511 | 0 | 100,00% | 21 362 | 51 | 98 | 0 |  |
| fop_F1SAMPLE.afp | 834 | 0 | 36,21% | 0 | 0 | 302 | 532 |  |
| fop_resource_any_name.afp | 15 613 | 0 | 100,00% | 15 481 | 18 | 114 | 0 |  |
| fop_resource_name_match.afp | 15 619 | 0 | 100,00% | 15 487 | 18 | 114 | 0 |  |
| fop_resource_name_mismatch.afp | 15 617 | 0 | 100,00% | 15 479 | 18 | 120 | 0 |  |
| fop_resource_no_end_name.afp | 15 611 | 0 | 100,00% | 15 479 | 18 | 114 | 0 |  |
| ibm_afpanlyz.afp | 28 208 | 16 | 100,00% | 23 399 | 1 217 | 3 592 | 0 |  |
| ibm_arsdemo.afp | 19 278 | 6 | 100,00% | 17 362 | 450 | 1 466 | 0 |  |
| parseafp_in.afp | 752 | 1 | 86,30% | 302 | 234 | 113 | 103 |  |
| shaosil_Sample1.afp | 86 607 | 1 | 98,27% | 54 362 | 342 | 30 406 | 1 497 |  |
| cmyip_fillet.AFP | 1 142 649 | 5 | 99,60% | 7 085 | 862 118 | 268 907 | 4 539 |  |

## Summary
- Total files audited: 34
- Files with coverage < 95%: 2

## Aggregate coverage (after quick-wins)

| Metric | Value |
|---|---|
| Corpus total bytes | 11,013,848 |
| Byte-weighted average coverage | **99.33 %** |
| Files ≥ 95 % | **32 / 34** |
| Files = 100 % | 19 / 34 |
| Files < 50 % | 1 / 34 |

### Quick-wins applied
1. Added `BPT / EPT / ERS` dispatch entries (envelope SFs that were falling through to `UnknownStructuredField`).
2. Mapped 7 previously-UNKNOWN SF IDs in `SfRegistry` (FNG, MCF1, FNN, MFC, CPT, OBP3, OBD3, MPO, CAT, PTDX, PTC).
3. Corrected the coverage formula to include `PARSED_IGNORED` bytes (they ARE accounted for — just not yet semantically consumed).

### Remaining gaps
- `fop_F1SAMPLE.afp` (36 %) — 834-byte FOP fixture with SFs still unmapped. Low priority (834 bytes).
- `parseafp_in.afp` (86 %) — Perl-generator fixture.
- 12 cmyip files between 95–99 % — cleanup gains require implementing FNI/GDD/IDD (flagged as "lourd" by user — skipped).
