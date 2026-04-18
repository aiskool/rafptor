package com.rafptor.parser;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Raw byte-level dump of an AFP file: every structured field identified by
 * magic byte 0x5A, name resolved from the 3-byte id, plus PTOCA control
 * sequence breakdown for PTX payloads.
 *
 * <p>Usage:
 * <pre>
 *   java -cp target/rafptor-parser-&lt;version&gt;.jar \
 *        com.rafptor.parser.DebugDump &lt;file.afp&gt;
 * </pre>
 *
 * <p>Deliberately byte-level and independent of the main parser — if the parser
 * is broken this tool keeps working. Prints structure names and byte counts only,
 * never text content.
 */
public final class DebugDump {

    private DebugDump() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: DebugDump <afp_file>");
            System.exit(1);
        }
        Path afpFile = Path.of(args[0]);
        byte[] data = Files.readAllBytes(afpFile);
        System.out.println("=== DEBUG DUMP: " + afpFile.getFileName() + " ===");
        System.out.println("Size: " + data.length + " bytes");
        System.out.println();

        int pos = 0;
        int sfCount = 0;
        int ptxCount = 0;
        int mcfCount = 0;
        int bpgCount = 0;
        int epgCount = 0;
        int bdtCount = 0;
        int edtCount = 0;
        int bptCount = 0;
        int eptCount = 0;
        int totalPtocaBytes = 0;

        while (pos < data.length) {
            if ((data[pos] & 0xFF) != 0x5A) {
                pos++;
                continue;
            }
            if (pos + 5 >= data.length) {
                System.out.printf("[%06X] truncated header (remaining %d bytes)%n",
                        pos, data.length - pos);
                break;
            }
            int length = ((data[pos + 1] & 0xFF) << 8) | (data[pos + 2] & 0xFF);
            int b1 = data[pos + 3] & 0xFF;
            int b2 = data[pos + 4] & 0xFF;
            int b3 = data[pos + 5] & 0xFF;
            int flags = (pos + 6 < data.length) ? (data[pos + 6] & 0xFF) : 0;
            int sfType = (b1 << 16) | (b2 << 8) | b3;
            String sfName = identifySF(sfType);
            sfCount++;

            switch (sfType) {
                case 0xD3A8A8 -> bdtCount++;
                case 0xD3A9A8 -> edtCount++;
                case 0xD3A8AF -> bpgCount++;
                case 0xD3A9AF -> epgCount++;
                case 0xD3A8AD -> bptCount++;
                case 0xD3A9AD -> eptCount++;
                case 0xD3AB8A -> mcfCount++;
                case 0xD3EE9B -> ptxCount++;
                default -> {
                    // no-op
                }
            }

            System.out.printf("[%06X] %-35s len=%5d  flags=0x%02X%n",
                    pos, sfName, length, flags);

            if (sfType == 0xD3EE9B) {
                int payloadStart = pos + 9;
                int payloadLen = length - 8;
                if (payloadLen > 0 && payloadStart + payloadLen <= data.length) {
                    boolean chained = (flags & 0x04) != 0;
                    totalPtocaBytes += payloadLen;
                    dumpPtoca(data, payloadStart, payloadLen, chained);
                }
            }

            pos += 1 + length;
        }

        System.out.println();
        System.out.println("=== SUMMARY ===");
        System.out.println("Structured fields: " + sfCount);
        System.out.println("BDT (Begin Document): " + bdtCount);
        System.out.println("EDT (End Document):   " + edtCount);
        System.out.println("BPG (Begin Page):     " + bpgCount);
        System.out.println("EPG (End Page):       " + epgCount);
        System.out.println("BPT (Begin Presentation Text): " + bptCount);
        System.out.println("EPT (End Presentation Text):   " + eptCount);
        System.out.println("MCF (Map Coded Font): " + mcfCount);
        System.out.println("PTX (Presentation Text Data): " + ptxCount);
        System.out.println("Total PTOCA bytes: " + totalPtocaBytes);

        if (bdtCount == 0) {
            System.out.println();
            System.out.println("NOTE: no BDT found — this is likely a resource, "
                    + "not a complete document.");
        }
        if (ptxCount == 0 && bpgCount > 0) {
            System.out.println();
            System.out.println("NOTE: pages present but no PTX — the document has "
                    + "either image-only pages or references external resources.");
        }
    }

    private static void dumpPtoca(byte[] data, int offset, int length, boolean chained) {
        System.out.printf("    PTOCA payload: %d bytes, format=%s%n",
                length, chained ? "CHAINED" : "UNCHAINED");
        int pos = offset;
        int end = offset + length;
        int trnCount = 0;
        int scflCount = 0;
        int amiCount = 0;
        int ambCount = 0;
        int unknown = 0;
        int shown = 0;
        int limit = 12;

        while (pos < end) {
            int csLen;
            int csType;
            int csHeaderSize;
            if (chained) {
                // chained format: [type] [data] — length implicit per opcode
                csType = data[pos] & 0xFF;
                csLen = chainedCsLength(csType);
                csHeaderSize = 1;
            } else {
                if (pos + 2 > end) break;
                csLen = data[pos] & 0xFF;
                csType = data[pos + 1] & 0xFF;
                csHeaderSize = 0;
                if (csLen < 2) break;
            }
            if (pos + Math.max(csLen, csHeaderSize) > end) break;

            String name = ptocaName(csType);
            switch (csType) {
                case 0xDA -> trnCount++;
                case 0xF1 -> scflCount++;
                case 0xC6 -> amiCount++;
                case 0xD2 -> ambCount++;
                default -> unknown++;
            }
            if (shown < limit) {
                System.out.printf("      [+%4d] %-30s len=%d%n",
                        pos - offset, name, csLen);
                shown++;
                if (shown == limit && pos < end) {
                    System.out.println("      …(truncated)");
                }
            }
            pos += csLen > 0 ? csLen : (csHeaderSize + 1);
        }
        System.out.printf("      Control-sequence tally: TRN=%d SCFL=%d AMI=%d AMB=%d unknown=%d%n",
                trnCount, scflCount, amiCount, ambCount, unknown);
    }

    /**
     * Approximate chained-format length for the handful of PTOCA opcodes that
     * are unambiguous from the opcode alone. TRN is variable-length in both
     * formats so chained TRN is not safe to assume — flagged as unknown below
     * when encountered in chained mode.
     */
    private static int chainedCsLength(int csType) {
        return switch (csType) {
            case 0xF0 -> 1;   // NOP
            case 0xF1 -> 2;   // SCFL: opcode + local id
            case 0xC6, 0xC8, 0xD2, 0xD4 -> 3;  // AMI, RMI, AMB, RMB: opcode + 2-byte coord
            case 0x74 -> 1;   // DBR terminator
            case 0xDA -> 0;   // TRN — variable; caller must fall back
            default -> 0;
        };
    }

    private static String ptocaName(int csType) {
        return switch (csType) {
            case 0xF1 -> "SCFL (Set Coded Font Local)";
            case 0xD2 -> "AMB (Absolute Move Baseline)";
            case 0xC6 -> "AMI (Absolute Move Inline)";
            case 0xC8 -> "RMI (Relative Move Inline)";
            case 0xD4 -> "RMB (Relative Move Baseline)";
            case 0xDA -> "TRN (Transparent Data)";
            case 0xE4 -> "DBR (Draw B-axis Rule)";
            case 0xE6 -> "DIR (Draw I-axis Rule)";
            case 0x80 -> "STC (Set Text Color)";
            case 0xF0 -> "SEC (Set Extended Color)";
            case 0x70 -> "SIM (Set Intercharacter Increment)";
            case 0x72 -> "SBI (Set Baseline Increment)";
            case 0x74 -> "SVI (Set Variable-Space Incr)";
            default  -> String.format("UNKNOWN (0x%02X)", csType);
        };
    }

    private static String identifySF(int sfType) {
        return switch (sfType) {
            case 0xD3A8A8 -> "BDT (Begin Document)";
            case 0xD3A9A8 -> "EDT (End Document)";
            case 0xD3A8AF -> "BPG (Begin Page)";
            case 0xD3A9AF -> "EPG (End Page)";
            case 0xD3A8C9 -> "BAG (Begin Active Env Group)";
            case 0xD3A9C9 -> "EAG (End Active Env Group)";
            case 0xD3A8C6 -> "BRG (Begin Resource Group)";
            case 0xD3A9C6 -> "ERG (End Resource Group)";
            case 0xD3A8EB -> "BOC (Begin Object Container)";
            case 0xD3A9EB -> "EOC (End Object Container)";
            case 0xD3EE9B -> "PTX (Presentation Text Data)";
            case 0xD3AB8A -> "MCF (Map Coded Font)";
            case 0xD3AFD8 -> "IPO (Include Page Overlay)";
            case 0xD3AF5F -> "IPS (Include Page Segment)";
            case 0xD3A090 -> "TLE (Tag Logical Element)";
            case 0xD3EEEE -> "NOP (No Operation)";
            case 0xD3A68A -> "MCC (Medium Copy Count)";
            case 0xD3A88A -> "MPG (Map Page)";
            case 0xD3A689 -> "FND (Font Descriptor)";
            case 0xD3A789 -> "FNC (Font Control)";
            case 0xD38C89 -> "FNI (Font Index)";
            case 0xD38E89 -> "FNP (Font Patterns)";
            case 0xD3A6AB -> "PGD (Page Descriptor)";
            case 0xD3A7AB -> "AEG (Active Env Group Descriptor)";
            case 0xD3A6BB -> "OBD (Object Area Descriptor)";
            case 0xD3A7BB -> "OBP (Object Area Position)";
            case 0xD3A8AD -> "BPT (Begin Presentation Text)";
            case 0xD3A9AD -> "EPT (End Presentation Text)";
            case 0xD3A8DF -> "BII (Begin Image)";
            case 0xD3A9DF -> "EII (End Image)";
            case 0xD3A6FB -> "IDD (Image Data Descriptor)";
            case 0xD3EEFB -> "IRD (Image Raster Data)";
            case 0xD3A8BB -> "BOG (Begin Object Environment Group)";
            case 0xD3A9BB -> "EOG (End Object Environment Group)";
            case 0xD3ABCC -> "MMC (Map Media)";
            case 0xD3A6CC -> "MDD (Medium Descriptor)";
            default -> String.format("SF(0x%06X)", sfType);
        };
    }
}
