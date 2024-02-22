package courgette.runtime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.sql.Timestamp;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourgetteRun {
    private final String featureUri;
    private final Long threadId;
    private final Timestamp startTimestamp;
    private final Timestamp endTimestamp;
    private final Boolean isRerun;
    @JsonIgnore
    private final Integer exitCode;
    private final String error;
    private final CourgetteMobileDevice mobileDevice;

    public CourgetteRun(String featureUri,
                        long threadId,
                        Timestamp startTimestamp,
                        Timestamp endTimestamp,
                        Boolean isRerun,
                        Integer exitCode,
                        String error,
                        CourgetteMobileDevice mobileDevice) {
        this.featureUri = featureUri;
        this.threadId = threadId;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.isRerun = isRerun;
        this.exitCode = exitCode;
        this.error = error;
        this.mobileDevice = mobileDevice;
    }

    public String getFeatureUri() {
        return featureUri;
    }

    public Long getThreadId() {
        return threadId;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    public Timestamp getStartTimestamp() {
        return startTimestamp;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    public Timestamp getEndTimestamp() {
        return endTimestamp;
    }

    public Boolean isRerun() {
        return isRerun;
    }

    public Boolean isSuccessful() {
        return exitCode == 0;
    }

    public String getError() {
        return error;
    }

    public CourgetteMobileDevice getMobileDevice() {
        return mobileDevice;
    }
}
