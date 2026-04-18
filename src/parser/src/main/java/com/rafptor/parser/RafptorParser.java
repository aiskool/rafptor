package com.rafptor.parser;

import com.rafptor.parser.exception.AfpParseException;
import com.rafptor.parser.model.AfpDocument;
import com.rafptor.parser.model.AfpPage;
import com.rafptor.parser.model.AfpResource;
import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.modca.BeginDocument;
import com.rafptor.parser.modca.BeginPage;
import com.rafptor.parser.modca.EndDocument;
import com.rafptor.parser.modca.EndPage;
import com.rafptor.parser.modca.IncludeObject;
import com.rafptor.parser.modca.IncludePageOverlay;
import com.rafptor.parser.modca.IncludePageSegment;
import com.rafptor.parser.modca.MapCodedFont;
import com.rafptor.parser.modca.PageDescriptor;
import com.rafptor.parser.modca.PresentationTextData;
import com.rafptor.parser.modca.PresentationTextDescriptor;
import com.rafptor.parser.modca.TagLogicalElement;
import com.rafptor.parser.model.PageGeometry;
import com.rafptor.parser.ptoca.PtocaParser;
import com.rafptor.parser.reader.RecordReader;
import com.rafptor.parser.reader.StructuredFieldReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        AfpDocument document = new AfpDocument("UNNAMED");
        AfpPage currentPage = null;
        int depth = 0;

        while (reader.hasNext()) {
            RawStructuredField raw = reader.next();
            AfpStructuredField sf = StructuredFieldReader.dispatch(raw);

            if (isBegin(sf)) {
                depth++;
                if (depth > limits.maxNestingDepth()) {
                    throw new AfpParseException("Nesting depth exceeds " + limits.maxNestingDepth());
                }
            } else if (isEnd(sf)) {
                depth = Math.max(0, depth - 1);
            }

            if (sf instanceof BeginDocument bdt) {
                document = new AfpDocument(defaultNameIfBlank(bdt.documentName(), "UNNAMED"));
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
                    document.addResource(new AfpResource(
                            defaultNameIfBlank(e.codedFontName(), "FONT"),
                            AfpResource.ResourceType.CODED_FONT));
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
                    ptocaParser.parse(ptx).forEach(currentPage::addTextRun);
                }
            } else if (sf instanceof PageDescriptor pgd) {
                if (currentPage != null) {
                    mergeGeometry(currentPage, pgd, null);
                }
            } else if (sf instanceof PresentationTextDescriptor ptd) {
                if (currentPage != null) {
                    mergeGeometry(currentPage, null, ptd);
                }
            }

            if (currentPage != null) {
                currentPage.addStructuredField(sf);
            } else {
                document.addStructuredField(sf);
            }
        }
        LOG.info("parsed records={} bytes={} pages={}",
                reader.recordsRead(), reader.bytesConsumed(), document.pages().size());
        return document;
    }

    private static boolean isBegin(AfpStructuredField sf) {
        return sf instanceof BeginDocument
                || sf instanceof BeginPage
                || sf instanceof com.rafptor.parser.modca.BeginActiveEnvironmentGroup
                || sf instanceof com.rafptor.parser.modca.BeginResourceGroup
                || sf instanceof com.rafptor.parser.modca.BeginObjectEnvironmentGroup;
    }

    private static boolean isEnd(AfpStructuredField sf) {
        return sf instanceof EndDocument
                || sf instanceof EndPage
                || sf instanceof com.rafptor.parser.modca.EndActiveEnvironmentGroup
                || sf instanceof com.rafptor.parser.modca.EndResourceGroup
                || sf instanceof com.rafptor.parser.modca.EndObjectEnvironmentGroup;
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
