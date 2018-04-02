package courgette.runtime.report.model;

public class Hook {
    private String location;
    private Result result;

    public Hook(String location, Result result) {
        this.location = location;
        this.result = result;
    }

    public String getLocation() {
        return location;
    }

    public Result getResult() {
        return result;
    }
}
