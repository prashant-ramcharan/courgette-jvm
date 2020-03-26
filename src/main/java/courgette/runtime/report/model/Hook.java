package courgette.runtime.report.model;

import java.util.List;

import static io.cucumber.core.backend.Status.AMBIGUOUS;
import static io.cucumber.core.backend.Status.FAILED;
import static io.cucumber.core.backend.Status.PASSED;
import static io.cucumber.core.backend.Status.SKIPPED;

public class Hook {
    private String location;
    private Result result;
    private List<Embedding> embeddings;
    private List<String> output;

    public Hook(String location, Result result, List<Embedding> embeddings, List<String> output) {
        this.location = location;
        this.result = result;
        this.embeddings = embeddings;
        this.output = output;
    }

    public String getLocation() {
        return location;
    }

    public Result getResult() {
        return result;
    }

    public List<Embedding> getEmbeddings() {
        return embeddings;
    }

    public List<String> getOutput() {
        return output;
    }

    public boolean passed(boolean isStrict) {
        if (isStrict) {
            return result.getStatus().equalsIgnoreCase(PASSED.toString()) || result.getStatus().equalsIgnoreCase(SKIPPED.toString());
        } else {
            return !result.getStatus().equalsIgnoreCase(FAILED.toString()) && !result.getStatus().equalsIgnoreCase(AMBIGUOUS.toString());
        }
    }
}