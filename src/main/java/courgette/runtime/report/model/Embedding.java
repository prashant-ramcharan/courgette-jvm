package courgette.runtime.report.model;

import java.util.UUID;

public class Embedding {
    private String courgetteEmbeddingId;
    private String data;
    private String mimeType;

    public Embedding(String data, String mimeType) {
        this.courgetteEmbeddingId = UUID.randomUUID().toString();
        this.data = data;
        this.mimeType = mimeType;
    }

    public String getCourgetteEmbeddingId() {
        return courgetteEmbeddingId;
    }

    public String getData() {
        return data;
    }

    public String getMimeType() {
        return mimeType;
    }
}
