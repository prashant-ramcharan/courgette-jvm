package courgette.runtime.report.model;

import java.util.List;

import static io.cucumber.core.event.Status.PASSED;
import static io.cucumber.core.event.Status.SKIPPED;
import static io.cucumber.core.event.Status.FAILED;
import static io.cucumber.core.event.Status.AMBIGUOUS;

public class Step {
    private String name;
    private String keyword;
    private Result result;
    private List<Hook> before;
    private List<Hook> after;
    private List<Embedding> embeddings;
    private List<String> output;
    private List<String> rowData;

    public Step(String name,
                String keyword,
                Result result,
                List<Hook> before,
                List<Hook> after,
                List<Embedding> embeddings,
                List<String> output,
                List<String> rowData) {
        this.name = name;
        this.keyword = keyword;
        this.result = result;
        this.before = before;
        this.after = after;
        this.embeddings = embeddings;
        this.output = output;
        this.rowData = rowData;
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

    public List<Hook> getBefore() {
        return before;
    }

    public List<Hook> getAfter() {
        return after;
    }

    public List<Embedding> getEmbeddings() {
        return embeddings;
    }

    public List<String> getOutput() {
        return output;
    }

    public List<String> getRowData() {
        return rowData;
    }

    public boolean passed(boolean isStrict) {
        if (isStrict) {
            return result.getStatus().equalsIgnoreCase(PASSED.toString()) || result.getStatus().equalsIgnoreCase(SKIPPED.toString());
        } else {
            return !result.getStatus().equalsIgnoreCase(FAILED.toString()) && !result.getStatus().equalsIgnoreCase(AMBIGUOUS.toString());
        }
    }
}