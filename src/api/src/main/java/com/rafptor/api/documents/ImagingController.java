package com.rafptor.api.documents;

import com.rafptor.api.config.AuthDetails;
import com.rafptor.api.config.TenantContext;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Provides rasterised page images and visual-diff endpoints for converted PDF documents.
 */
@RestController
@RequestMapping("/api/documents/{id}")
public class ImagingController {

    private static final Logger log = LoggerFactory.getLogger(ImagingController.class);

    private static final int THUMBNAIL_WIDTH = 200;
    private static final int THUMBNAIL_HEIGHT = 280;
    private static final float THUMBNAIL_DPI = 72f;

    private static final Path CACHE_ROOT = Path.of("/tmp/rafptor-cache");
    private static final Path THUMBNAIL_CACHE = CACHE_ROOT.resolve("thumbnails");
    private static final Path PAGE_CACHE = CACHE_ROOT.resolve("pages");
    private static final Path REFERENCE_ROOT = Path.of("/tmp/rafptor-e2e/reference");

    private final DocumentService documentService;

    public ImagingController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Renders page 0 at 72 dpi, scaled to 200x280, and returns a PNG.
     * Result is cached on disk.
     */
    @GetMapping("/thumbnail")
    public ResponseEntity<byte[]> thumbnail(@PathVariable String id, Authentication auth) {
        return documentService.findOne(resolveTenant(auth), id)
                .map(doc -> {
                    try {
                        Path cacheFile = THUMBNAIL_CACHE.resolve(id + ".png");
                        if (Files.exists(cacheFile)) {
                            return pngResponse(Files.readAllBytes(cacheFile));
                        }
                        byte[] png = renderThumbnail(doc.getPdfPath());
                        writeCached(cacheFile, png);
                        return pngResponse(png);
                    } catch (IOException e) {
                        log.warn("Thumbnail generation failed for document {}: {}", id, e.getMessage());
                        return ResponseEntity.internalServerError().<byte[]>build();
                    }
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Renders page {@code n} (1-based) at the requested dpi.
     * Result is cached on disk.
     */
    @GetMapping("/page/{n}/image")
    public ResponseEntity<byte[]> pageImage(
            @PathVariable String id,
            @PathVariable int n,
            @RequestParam(defaultValue = "150") int dpi,
            Authentication auth) {
        return documentService.findOne(resolveTenant(auth), id)
                .map(doc -> {
                    try {
                        Path cacheFile = PAGE_CACHE.resolve(id + "_" + n + "_" + dpi + ".png");
                        if (Files.exists(cacheFile)) {
                            return pngResponse(Files.readAllBytes(cacheFile));
                        }
                        byte[] png = renderPage(doc.getPdfPath(), n - 1, dpi);
                        writeCached(cacheFile, png);
                        return pngResponse(png);
                    } catch (IOException e) {
                        log.warn("Page image generation failed for document {} page {}: {}", id, n, e.getMessage());
                        return ResponseEntity.internalServerError().<byte[]>build();
                    }
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Serves a pre-existing reference PNG for page {@code n} (1-based).
     * Returns a gray placeholder when no reference exists.
     */
    @GetMapping("/page/{n}/reference")
    public ResponseEntity<byte[]> pageReference(
            @PathVariable String id,
            @PathVariable int n,
            Authentication auth) {
        resolveTenant(auth); // Authorize the caller (tenant scoping not needed for static files)
        Path refFile = REFERENCE_ROOT.resolve(id).resolve(String.format("page_%03d.png", n));
        try {
            if (Files.exists(refFile)) {
                return pngResponse(Files.readAllBytes(refFile));
            }
            return pngResponse(grayPlaceholder());
        } catch (IOException e) {
            log.warn("Reference read failed for document {} page {}: {}", id, n, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Returns an absolute-difference image between the rendered page and the reference.
     * Result is cached on disk.
     */
    @GetMapping("/page/{n}/diff")
    public ResponseEntity<byte[]> pageDiff(
            @PathVariable String id,
            @PathVariable int n,
            @RequestParam(defaultValue = "150") int dpi,
            Authentication auth) {
        return documentService.findOne(resolveTenant(auth), id)
                .map(doc -> {
                    try {
                        Path cacheFile = PAGE_CACHE.resolve(id + "_" + n + "_" + dpi + "_diff.png");
                        if (Files.exists(cacheFile)) {
                            return pngResponse(Files.readAllBytes(cacheFile));
                        }
                        byte[] rendered = renderPage(doc.getPdfPath(), n - 1, dpi);
                        Path refFile = REFERENCE_ROOT.resolve(id).resolve(String.format("page_%03d.png", n));
                        byte[] reference = Files.exists(refFile)
                                ? Files.readAllBytes(refFile)
                                : grayPlaceholder();
                        byte[] diff = computeDiff(rendered, reference);
                        writeCached(cacheFile, diff);
                        return pngResponse(diff);
                    } catch (IOException e) {
                        log.warn("Diff generation failed for document {} page {}: {}", id, n, e.getMessage());
                        return ResponseEntity.internalServerError().<byte[]>build();
                    }
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static String resolveTenant(Authentication auth) {
        if (auth != null && auth.getDetails() instanceof AuthDetails details) {
            return details.tenantId();
        }
        // Fallback to thread-bound context (JWT filter already set it).
        String fromContext = TenantContext.getOrNull();
        if (fromContext != null) {
            return fromContext;
        }
        throw new IllegalStateException("no tenant id available");
    }

    private byte[] renderThumbnail(String pdfPath) throws IOException {
        if (pdfPath == null || pdfPath.isBlank()) {
            throw new IOException("Document has no PDF path");
        }
        try (PDDocument pdf = org.apache.pdfbox.Loader.loadPDF(Path.of(pdfPath).toFile())) {
            PDFRenderer renderer = new PDFRenderer(pdf);
            BufferedImage raw = renderer.renderImageWithDPI(0, THUMBNAIL_DPI);
            BufferedImage scaled = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(raw, 0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, null);
            g.dispose();
            return toPngBytes(scaled);
        }
    }

    private byte[] renderPage(String pdfPath, int pageIndex, int dpi) throws IOException {
        if (pdfPath == null || pdfPath.isBlank()) {
            throw new IOException("Document has no PDF path");
        }
        try (PDDocument pdf = org.apache.pdfbox.Loader.loadPDF(Path.of(pdfPath).toFile())) {
            PDFRenderer renderer = new PDFRenderer(pdf);
            BufferedImage img = renderer.renderImageWithDPI(pageIndex, dpi);
            return toPngBytes(img);
        }
    }

    private byte[] computeDiff(byte[] renderedPng, byte[] referencePng) throws IOException {
        BufferedImage a = ImageIO.read(new java.io.ByteArrayInputStream(renderedPng));
        BufferedImage b = ImageIO.read(new java.io.ByteArrayInputStream(referencePng));

        int w = Math.min(a.getWidth(), b.getWidth());
        int h = Math.min(a.getHeight(), b.getHeight());
        BufferedImage diff = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int ca = a.getRGB(x, y);
                int cb = b.getRGB(x, y);
                int dr = Math.abs(((ca >> 16) & 0xFF) - ((cb >> 16) & 0xFF));
                int dg = Math.abs(((ca >> 8) & 0xFF) - ((cb >> 8) & 0xFF));
                int db = Math.abs((ca & 0xFF) - (cb & 0xFF));
                diff.setRGB(x, y, (dr << 16) | (dg << 8) | db);
            }
        }
        return toPngBytes(diff);
    }

    private static byte[] grayPlaceholder() throws IOException {
        BufferedImage img = new BufferedImage(200, 280, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, 200, 280);
        g.dispose();
        return toPngBytes(img);
    }

    private static byte[] toPngBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", out);
        return out.toByteArray();
    }

    private static void writeCached(Path target, byte[] data) {
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, data);
        } catch (IOException e) {
            log.debug("Cache write skipped for {}: {}", target, e.getMessage());
        }
    }

    private static ResponseEntity<byte[]> pngResponse(byte[] data) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(data);
    }
}
