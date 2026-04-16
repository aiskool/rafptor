package com.rafptor.converter;

import com.rafptor.converter.ir.IrDocument;
import com.rafptor.converter.render.PdfRenderer;
import com.rafptor.converter.transform.AfpToIrTransformer;
import com.rafptor.converter.validation.ConversionValidator;
import com.rafptor.converter.validation.ValidationReport;
import com.rafptor.parser.model.AfpDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point of the AFP → PDF pipeline.
 *
 * <p>The converter is stateless apart from the immutable {@link ConversionConfig}
 * passed at construction time. A single instance is safe to reuse across
 * conversions on the same thread.
 */
public final class RafptorConverter {

    private static final Logger LOG = LoggerFactory.getLogger(RafptorConverter.class);

    private final ConversionConfig config;
    private final AfpToIrTransformer transformer;
    private final PdfRenderer renderer;
    private final ConversionValidator validator = new ConversionValidator();

    public RafptorConverter() {
        this(ConversionConfig.defaults());
    }

    public RafptorConverter(ConversionConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
        this.transformer = new AfpToIrTransformer(config);
        this.renderer = new PdfRenderer(config);
    }

    public ConversionResult convert(AfpDocument afpDoc, Path outputPath) {
        if (afpDoc == null) {
            throw new IllegalArgumentException("afpDoc must not be null");
        }
        if (outputPath == null) {
            throw new IllegalArgumentException("outputPath must not be null");
        }
        Instant start = Instant.now();
        ConversionResult.Builder b = ConversionResult.builder()
                .sourceDocument(afpDoc.name())
                .outputPath(outputPath)
                .startTime(start);
        List<String> warnings = new ArrayList<>();
        try {
            IrDocument ir = transformer.transform(afpDoc);
            warnings.addAll(transformer.warnings());
            b.pageCount(ir.pages().size());
            try (OutputStream out = Files.newOutputStream(outputPath)) {
                List<String> renderWarnings = renderer.render(ir, out, config);
                warnings.addAll(renderWarnings);
            }
            long size = Files.size(outputPath);
            b.outputSize(size);
            ValidationReport report = validator.validate(outputPath, ir, config.maxOutputBytes());
            b.validation(report);
            ConversionResult.Status status;
            if (!report.isPassed()) {
                status = ConversionResult.Status.FAILED;
            } else if (!warnings.isEmpty() || !report.warnings().isEmpty()) {
                status = ConversionResult.Status.WARNING;
            } else {
                status = ConversionResult.Status.SUCCESS;
            }
            b.status(status).addWarnings(warnings).addWarnings(report.warnings());
        } catch (IOException | RuntimeException e) {
            LOG.error("conversion failed for {}: {}", afpDoc.name(), e.getClass().getSimpleName());
            b.status(ConversionResult.Status.FAILED)
                    .error(e.getClass().getSimpleName() + ": " + safeMessage(e));
        }
        Instant end = Instant.now();
        return b.endTime(end)
                .durationMs(Duration.between(start, end).toMillis())
                .build();
    }

    private static String safeMessage(Throwable t) {
        // Only keep the exception class and a short hint — never the message
        // content, which might echo back AFP bytes.
        return t.getMessage() == null ? "" : t.getMessage().substring(0, Math.min(120, t.getMessage().length()));
    }

    public ConversionConfig config() {
        return config;
    }
}
