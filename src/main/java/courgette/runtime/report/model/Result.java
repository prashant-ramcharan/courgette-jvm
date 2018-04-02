package courgette.runtime.report.model;

public class Result {
    private String status;
    private long duration;
    private String errorMessage;

    public Result(String status, long duration, String errorMessage) {
        this.status = status;
        this.duration = duration;
        this.errorMessage = errorMessage;
    }

    public String getStatus() {
        return status;
    }

    public long getDuration() {
        return duration > 0 ? duration / 1000000 : 0;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}