package com.rafptor.parser;

import com.rafptor.parser.audit.ByteAccountant;
import com.rafptor.parser.audit.ByteAccountant.SfStatus;
import com.rafptor.parser.audit.ByteAccountingReport;
import com.rafptor.parser.audit.SfInfo;
import com.rafptor.parser.audit.SfRegistry;
import com.rafptor.parser.exception.AfpParseException;
import com.rafptor.parser.model.AfpDocument;
import com.rafptor.parser.model.AfpPage;
import com.rafptor.parser.model.AfpResource;
import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.OpaqueSf;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.modca.BeginDocument;
import com.rafptor.parser.modca.BeginPage;
import com.rafptor.parser.modca.EndDocument;
import com.rafptor.parser.modca.EndPage;
import com.rafptor.parser.modca.IncludeObject;
import com.rafptor.parser.modca.IncludePageOverlay;
import com.rafptor.parser.modca.IncludePageSegment;
import com.rafptor.parser.modca.BeginGraphicsObject;
import com.rafptor.parser.modca.BeginImageObject;
import com.rafptor.parser.modca.EndGraphicsObject;
import com.rafptor.parser.modca.EndImageObject;
import com.rafptor.parser.modca.GraphicsData;
import com.rafptor.parser.modca.ImageRasterData;
import com.rafptor.parser.model.AfpGraphicObject;
import com.rafptor.parser.modca.BeginNamedResource;
import com.rafptor.parser.modca.EmbeddedObjectData;
import com.rafptor.parser.modca.MapCodedFont;
import com.rafptor.parser.modca.MapDataResource;
import com.rafptor.parser.modca.PageDescriptor;
import com.rafptor.parser.modca.PresentationTextData;
import com.rafptor.parser.modca.PresentationTextDescriptor;
import com.rafptor.parser.modca.TagLogicalElement;
import com.rafptor.parser.modca.UnknownStructuredField;
import com.rafptor.parser.model.AfpImageObject;
import com.rafptor.parser.model.PageGeometry;
import com.rafptor.parser.ptoca.PtocaParser;
import com.rafptor.parser.reader.RecordReader;
import com.rafptor.parser.reader.StructuredFieldReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Entry point of the Rafptor AFP parser.
 *
 * <p>Reads a binary AFP stream, dispatches Structured Fields to their typed parsers,
 * and assembles an {@link AfpDocument} AST. The whole parse runs inside a
 * {@link CompletableFuture} so that a single pathological document cannot stall
 * a worker indefinitely.
 */
public final class RafptorParser {

    private static final Logger LOG = LoggerFactory.getLogger(RafptorParser.class);

    private final ParserLimits limits;
    private final PtocaParser ptocaParser;

    public RafptorParser() {
        this(ParserLimits.defaults(), new PtocaParser());
    }

    public RafptorParser(ParserLimits limits, PtocaParser ptocaParser) {
        if (limits == null) {
            throw new IllegalArgumentException("limits must not be null");
        }
        if (ptocaParser == null) {
            throw new IllegalArgumentException("ptocaParser must not be null");
        }
        this.limits = limits;
        this.ptocaParser = ptocaParser;
    }

    public AfpDocument parse(InputStream input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        CompletableFuture<AfpDocument> future = CompletableFuture.supplyAsync(() -> parseInternal(input));
        try {
            return future.get(limits.parseTimeoutMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new AfpParseException("Parse timeout after " + limits.parseTimeoutMillis() + " ms", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AfpParseException("Parse interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AfpParseException afp) {
                throw afp;
            }
            throw new AfpParseException("Parse failed", cause);
        }
    }

    private AfpDocument parseInternal(InputStream input) {
        RecordReader reader = new RecordReader(input, limits);
        ByteAccountant accountant = new ByteAccountant();
        AfpDocument document = new AfpDocument("UNNAMED");
        AfpPage currentPage = null;
        int depth = 0;
        String currentImageName = null;
        ByteArrayOutputStream currentImageRaw = null;
        String currentGraphicName = null;
        ByteArrayOutputStream currentGraphicRaw = null;
        // Most-recently-opened named-resource envelope (BRS / BFN / BDG).
        // When an EmbeddedObjectData arrives inside such an envelope, the
        // raw bytes are stored at the document level under this name so
        // subsequent IncludeObject references can resolve them.
        String currentNamedResource = null;

        while (reader.hasNext()) {
            RawStructuredField raw = reader.next();
            long sfOffset = reader.lastRecordOffset();
            int sfTotalLength = reader.lastRecordTotalLength();
            String sfIdHex = String.format("%02X%02X%02X",
                    raw.id().classByte(), raw.id().typeByte(), raw.id().categoryByte());
            SfInfo info = SfRegistry.lookup(sfIdHex).orElse(SfInfo.UNKNOWN);
            SfStatus initialStatus = classifyStatus(sfIdHex, info);
            int pageIndex = document.pages().size();
            accountant.register(sfOffset, sfTotalLength, sfIdHex, info.mnemonic(),
                    initialStatus, pageIndex);
            AfpStructuredField sf = StructuredFieldReader.dispatch(raw);
            if (sf instanceof UnknownStructuredField) {
                // Preserve the raw body so an operator can inspect it later.
                document.addOpaqueSf(new OpaqueSf(sfOffset, sfIdHex, pageIndex, raw.data()));
                if (!SfRegistry.lookup(sfIdHex).isPresent()) {
                    LOG.warn("unknown SF id={} len={} offset={} — preserved as OpaqueSf",
                            sfIdHex, sfTotalLength, sfOffset);
                }
            }

            if (isBegin(sf)) {
                depth++;
                if (depth > limits.maxNestingDepth()) {
                    throw new AfpParseException("Nesting depth exceeds " + limits.maxNestingDepth());
                }
            } else if (isEnd(sf)) {
                depth = Math.max(0, depth - 1);
            }

            if (sf instanceof BeginDocument bdt) {
                // Some MO:DCA/P5 streams prefix BDT with a BRS / BFN envelope
                // that carries resources (embedded JPEGs, fonts). Those are
                // already captured on the throwaway document — copy them
                // forward into the new named document instead of discarding.
                AfpDocument prior = document;
                document = new AfpDocument(defaultNameIfBlank(bdt.documentName(), "UNNAMED"));
                if (prior != null) {
                    for (var e : prior.embeddedObjects().entrySet()) {
                        document.putEmbeddedObject(e.getKey(), e.getValue(),
                                prior.embeddedObjectKind(e.getKey()));
                    }
                }
            } else if (sf instanceof EndDocument edt) {
                LOG.debug("end document name={}", edt.documentName());
            } else if (sf instanceof BeginPage bpg) {
                currentPage = new AfpPage(defaultNameIfBlank(bpg.pageName(), "PAGE"));
            } else if (sf instanceof EndPage epg) {
                if (currentPage != null) {
                    document.addPage(currentPage);
                    currentPage = null;
                } else {
                    LOG.warn("EndPage without matching BeginPage: {}", epg.pageName());
                }
            } else if (sf instanceof MapCodedFont mcf) {
                for (MapCodedFont.Entry e : mcf.entries()) {
                    String name = defaultNameIfBlank(e.codedFontName(), "FONT");
                    document.addResource(new AfpResource(name, AfpResource.ResourceType.CODED_FONT));
                    if (currentPage != null) {
                        currentPage.putFontAssignment(e.localId(), name);
                        if (e.codePageName() != null && !e.codePageName().isEmpty()) {
                            currentPage.putCodePageAssignment(e.localId(), e.codePageName());
                        }
                    }
                }
            } else if (sf instanceof BeginNamedResource bnr) {
                // Track the most specific (non-empty) resource name so a
                // subsequent EmbeddedObjectData can be keyed by it.
                if (!bnr.resourceName().isEmpty()) {
                    currentNamedResource = bnr.resourceName();
                }
            } else if (sf instanceof EmbeddedObjectData eod) {
                if (currentNamedResource != null && eod.payload().length > 0
                        && eod.kind() != EmbeddedObjectData.Kind.UNKNOWN) {
                    document.putEmbeddedObject(currentNamedResource,
                            eod.payload(), eod.kind().name());
                }
            } else if (sf instanceof MapDataResource mdr) {
                // MO:DCA/P5 streams bind fonts via MDR repeating groups
                // instead of MCF. Each FontEntry carries the SCFL local id,
                // the TrueType font name, and the declared point size.
                for (MapDataResource.FontEntry e : mdr.fontEntries()) {
                    if (!e.fontName().isEmpty()) {
                        document.addResource(new AfpResource(e.fontName(),
                                AfpResource.ResourceType.CODED_FONT));
                        if (currentPage != null) {
                            currentPage.putFontAssignment(e.localId(), e.fontName());
                        }
                    }
                    if (currentPage != null && e.pointSize() > 0) {
                        currentPage.putFontPointSize(e.localId(), e.pointSize());
                    }
                }
            } else if (sf instanceof IncludePageOverlay ipo) {
                addResource(document, currentPage,
                        new AfpResource(defaultNameIfBlank(ipo.overlayName(), "OVERLAY"),
                                AfpResource.ResourceType.PAGE_OVERLAY));
            } else if (sf instanceof IncludePageSegment ips) {
                addResource(document, currentPage,
                        new AfpResource(defaultNameIfBlank(ips.segmentName(), "SEGMENT"),
                                AfpResource.ResourceType.PAGE_SEGMENT));
            } else if (sf instanceof IncludeObject iob) {
                addResource(document, currentPage,
                        new AfpResource(defaultNameIfBlank(iob.objectName(), "OBJECT"),
                                AfpResource.ResourceType.OBJECT_CONTAINER));
            } else if (sf instanceof TagLogicalElement tle) {
                if (tle.attributeName() != null && tle.attributeValue() != null) {
                    document.putTag(tle.attributeName(), tle.attributeValue());
                }
            } else if (sf instanceof PresentationTextData ptx) {
                if (currentPage != null) {
                    com.rafptor.parser.ptoca.PtocaParser.Result result =
                            ptocaParser.parseWithRules(ptx, currentPage.codePageAssignments());
                    result.runs().forEach(currentPage::addTextRun);
                    result.rules().forEach(currentPage::addRule);
                    if (result.opcodeReport() != null) {
                        document.addPtocaReport(result.opcodeReport());
                    }
                }
            } else if (sf instanceof PageDescriptor pgd) {
                if (currentPage != null) {
                    mergeGeometry(currentPage, pgd, null);
                }
            } else if (sf instanceof PresentationTextDescriptor ptd) {
                if (currentPage != null) {
                    mergeGeometry(currentPage, null, ptd);
                }
            } else if (sf instanceof BeginImageObject bim) {
                currentImageName = bim.name();
                currentImageRaw = new ByteArrayOutputStream();
            } else if (sf instanceof ImageRasterData ird) {
                if (currentImageRaw != null) {
                    byte[] data = ird.data();
                    currentImageRaw.write(data, 0, data.length);
                }
            } else if (sf instanceof EndImageObject eim) {
                if (currentPage != null && currentImageRaw != null) {
                    byte[] imgBytes = currentImageRaw.toByteArray();
                    AfpImageObject.Encoding enc = detectEncoding(imgBytes);
                    currentPage.addImage(new AfpImageObject(
                            currentImageName != null ? currentImageName : defaultNameIfBlank(eim.name(), "IMAGE"),
                            enc, 0, 0, imgBytes));
                }
                currentImageName = null;
                currentImageRaw = null;
            } else if (sf instanceof BeginGraphicsObject bgr) {
                currentGraphicName = bgr.name();
                currentGraphicRaw = new ByteArrayOutputStream();
            } else if (sf instanceof GraphicsData gad) {
                if (currentGraphicRaw != null) {
                    byte[] d = gad.data();
                    currentGraphicRaw.write(d, 0, d.length);
                }
            } else if (sf instanceof EndGraphicsObject egr) {
                if (currentPage != null && currentGraphicRaw != null) {
                    byte[] raw2 = currentGraphicRaw.toByteArray();
                    currentPage.addGraphic(new AfpGraphicObject(
                            currentGraphicName != null ? currentGraphicName
                                    : defaultNameIfBlank(egr.name(), "GRAPHIC"),
                            0, 0, 0, 0, raw2));
                }
                currentGraphicName = null;
                currentGraphicRaw = null;
            }

            if (currentPage != null) {
                currentPage.addStructuredField(sf);
            } else {
                document.addStructuredField(sf);
            }
        }
        accountant.setTotalFileBytes(reader.bytesConsumed());
        ByteAccountingReport report = accountant.generateReport();
        document.setByteAccountingReport(report);
        LOG.info("parsed records={} bytes={} pages={} coverage={}%",
                reader.recordsRead(), reader.bytesConsumed(),
                document.pages().size(),
                String.format("%.2f", report.coverage()));
        if (report.coverage() < 100.0 || !report.bigIgnored().isEmpty()) {
            LOG.warn("AFP byte coverage {}% — {} unknown bytes across {} SF(s)",
                    String.format("%.2f", report.coverage()),
                    report.unknownBytes() + report.ignoredBytes(),
                    report.bigIgnored().size());
            for (var r : report.bigIgnored().stream().limit(5).toList()) {
                LOG.warn("  opaque SF id={} ({}) offset={} len={}",
                        r.idHex(), r.mnemonic(), r.offset(), r.length());
            }
        }
        return document;
    }

    /**
     * Pick a conservative initial {@link SfStatus} based on the SF category
     * in the registry. The parser may upgrade this later (ex: an envelope
     * that ended up carrying payload, or a PARSED_USED SF we know produced
     * IR elements). Unknown IDs stay UNKNOWN until proven otherwise.
     */
    private static SfStatus classifyStatus(String idHex, SfInfo info) {
        if (info == SfInfo.UNKNOWN) {
            return SfStatus.UNKNOWN;
        }
        return switch (info.category()) {
            case DOCUMENT, PAGE, PAGE_GROUP, OBJECT_ENV, RESOURCE, CONTAINER ->
                    SfStatus.ENVELOPE_ONLY;
            case TEXT, IMAGE, GRAPHIC, BARCODE, FONT, INCLUDE, INDEX, MAP, DESCRIPTOR ->
                    info.implemented() ? SfStatus.PARSED_USED : SfStatus.PARSED_IGNORED;
            case OVERLAY, PAGE_SEGMENT, ENV_CONTROL, MISC ->
                    info.implemented() ? SfStatus.PARSED_USED : SfStatus.PARSED_IGNORED;
        };
    }

    private static boolean isBegin(AfpStructuredField sf) {
        return sf instanceof BeginDocument
                || sf instanceof BeginPage
                || sf instanceof com.rafptor.parser.modca.BeginActiveEnvironmentGroup
                || sf instanceof com.rafptor.parser.modca.BeginResourceGroup
                || sf instanceof com.rafptor.parser.modca.BeginObjectEnvironmentGroup
                || sf instanceof BeginGraphicsObject;
    }

    private static boolean isEnd(AfpStructuredField sf) {
        return sf instanceof EndDocument
                || sf instanceof EndPage
                || sf instanceof com.rafptor.parser.modca.EndActiveEnvironmentGroup
                || sf instanceof com.rafptor.parser.modca.EndResourceGroup
                || sf instanceof com.rafptor.parser.modca.EndObjectEnvironmentGroup
                || sf instanceof EndGraphicsObject;
    }

    private static void addResource(AfpDocument document, AfpPage currentPage, AfpResource resource) {
        if (currentPage != null) {
            currentPage.addResource(resource);
        } else {
            document.addResource(resource);
        }
    }

    private static String defaultNameIfBlank(String name, String fallback) {
        return (name == null || name.isBlank()) ? fallback : name;
    }

    private static AfpImageObject.Encoding detectEncoding(byte[] raw) {
        if (raw.length >= 2) {
            int b0 = raw[0] & 0xFF;
            int b1 = raw[1] & 0xFF;
            if (b0 == 0xFF && b1 == 0xD8) return AfpImageObject.Encoding.JPEG;
            if (b0 == 0x42 && b1 == 0x4D) return AfpImageObject.Encoding.BMP;
        }
        // IOCA encodings start with Function-Set / Image-Segment markers
        // (0x91 = Begin Image Content, 0x9C = Image Encoding etc.); we do not
        // decode them at this stage — just flag as FS45 (uncompressed IOCA),
        // the most common case for banking-grade AFP.
        return AfpImageObject.Encoding.IOCA_FS45;
    }

    /**
     * Fold PGD or PTD values into the current page's geometry, preserving any
     * previously-seen values from the sibling descriptor.
     */
    private static void mergeGeometry(AfpPage page, PageDescriptor pgd, PresentationTextDescriptor ptd) {
        PageGeometry current = page.geometry();
        int width = current != null ? current.widthLUnits() : 0;
        int height = current != null ? current.heightLUnits() : 0;
        int xRes = current != null ? current.xResolution() : 0;
        int yRes = current != null ? current.yResolution() : 0;
        int ptxX = current != null ? current.ptxXResolution() : 0;
        int ptxY = current != null ? current.ptxYResolution() : 0;
        if (pgd != null) {
            width = pgd.widthLUnits();
            height = pgd.heightLUnits();
            xRes = Math.max(1, pgd.xResolution());
            yRes = Math.max(1, pgd.yResolution());
        }
        if (ptd != null) {
            ptxX = Math.max(0, ptd.xResolution());
            ptxY = Math.max(0, ptd.yResolution());
        }
        if (xRes == 0) xRes = 240;
        if (yRes == 0) yRes = 240;
        page.setGeometry(new PageGeometry(width, height, xRes, yRes, ptxX, ptxY));
    }
}
