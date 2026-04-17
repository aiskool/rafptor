package com.rafptor.api.documents;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@org.springframework.data.mongodb.core.mapping.Document(collection = "documents")
public class Document {

    @Id private String id;
    @Indexed private String tenantId;
    @Indexed private String pipelineJobId;
    private String sourceAfpName;
    private String pdfPath;
    private DocumentStatus status = DocumentStatus.RECEIVED;
    private double compositeScore;
    private double visualScore;
    private double structuralScore;
    private double metadataScore;
    private String verdict;
    private int pageCount;
    private List<String> warnings = new ArrayList<>();
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getPipelineJobId() { return pipelineJobId; }
    public void setPipelineJobId(String pipelineJobId) { this.pipelineJobId = pipelineJobId; }
    public String getSourceAfpName() { return sourceAfpName; }
    public void setSourceAfpName(String sourceAfpName) { this.sourceAfpName = sourceAfpName; }
    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public double getCompositeScore() { return compositeScore; }
    public void setCompositeScore(double s) { this.compositeScore = s; }
    public double getVisualScore() { return visualScore; }
    public void setVisualScore(double s) { this.visualScore = s; }
    public double getStructuralScore() { return structuralScore; }
    public void setStructuralScore(double s) { this.structuralScore = s; }
    public double getMetadataScore() { return metadataScore; }
    public void setMetadataScore(double s) { this.metadataScore = s; }
    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public int getPageCount() { return pageCount; }
    public void setPageCount(int n) { this.pageCount = n; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
