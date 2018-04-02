package courgette.runtime.report.model;

import java.util.List;

public class Step {
    private String name;
    private String keyword;
    private Result result;
    private List<Embedding> embeddings;

    public Step(String name, String keyword, Result result, List<Embedding> embeddings) {
        this.name = name;
        this.keyword = keyword;
        this.result = result;
        this.embeddings = embeddings;
    }

    public String getName() {
        return name;
    }

    public String getKeyword() {
        return keyword;
    }

    public Result getResult() {
        return result;
    }

    public List<Embedding> getEmbeddings() {
        return embeddings;
    }
}
