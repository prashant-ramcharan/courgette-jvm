package courgette.integration.reportportal.request;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import courgette.runtime.report.model.Embedding;
import courgette.runtime.report.model.Hook;
import courgette.runtime.report.model.Result;
import courgette.runtime.report.model.Scenario;
import courgette.runtime.utils.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class ScenarioLogRequest {

    public HttpEntity create(final Scenario scenario, final String scenarioId, final String launchId) {

        List<File> fileAttachments = new ArrayList<>();

        Instant startTime = Instant.parse(scenario.getStartTimestamp());

        final JsonArray logArray = new JsonArray();

        addHooks(logArray, scenario.getBefore(), launchId, scenarioId, startTime, -1000, fileAttachments);
        addSteps(logArray, scenario, launchId, scenarioId, startTime, fileAttachments);
        addHooks(logArray, scenario.getAfter(), launchId, scenarioId, startTime, 1000, fileAttachments);

        MultipartEntityBuilder mb = MultipartEntityBuilder.create()
                .addPart("json_request_part", new StringBody(logArray.toString(), ContentType.APPLICATION_JSON));

        fileAttachments.forEach(f -> mb.addPart("file", new FileBody(f, ContentType.IMAGE_PNG)));

        return mb.build();
    }

    private static void addSteps(JsonArray logArray,
                                 Scenario scenario,
                                 String launchId,
                                 String scenarioId,
                                 Instant startTime, List<File> fileAttachments) {

        scenario.getSteps().forEach(step -> {

            final Instant stepTime = startTime.minusMillis(step.getResult().getDuration());

            final JsonObject logBody = new JsonObject();

            addHooks(logArray, step.getBefore(), launchId, scenarioId, stepTime, 0, fileAttachments);

            logBody.addProperty("launchUuid", launchId);
            logBody.addProperty("itemUuid", scenarioId);
            logBody.addProperty("time", String.valueOf(stepTime));
            logBody.addProperty("message", step.getKeyword() + step.getName() + toStatusName(step.getResult()));
            logBody.addProperty("level", toLevel(step.getResult()));

            addImageAttachment(logBody, step.getEmbeddings(), fileAttachments);

            addHooks(logArray, step.getAfter(), launchId, scenarioId, stepTime, 200, fileAttachments);

            logArray.add(logBody);
        });
    }

    private static void addHooks(JsonArray logArray, List<Hook> hooks,
                                 String launchId, String scenarioId,
                                 Instant startTime, int timeOffset,
                                 List<File> fileAttachments) {

        hooks.forEach(hook -> {
            final JsonObject logBody = new JsonObject();
            logBody.addProperty("launchUuid", launchId);
            logBody.addProperty("itemUuid", scenarioId);
            logBody.addProperty("time", String.valueOf(startTime.minusMillis(hook.getResult().getDuration() - timeOffset)));
            logBody.addProperty("message", hook.getLocation() + toStatusName(hook.getResult()));
            logBody.addProperty("level", toLevel(hook.getResult()));

            addImageAttachment(logBody, hook.getEmbeddings(), fileAttachments);

            logArray.add(logBody);
        });
    }

    private static void addImageAttachment(JsonObject logBody, List<Embedding> embeddings, List<File> fileAttachments) {
        final Optional<Embedding> image = embeddings.stream().filter(e -> e.getMimeType().contains("image")).findFirst();

        if (image.isPresent()) {

            final byte[] decodedBytes = Base64.getDecoder().decode(image.get().getData());

            try {
                final File imageAttachment = FileUtils.getTempFile("png");
                fileAttachments.add(imageAttachment);

                final FileOutputStream fos = new FileOutputStream(imageAttachment);
                fos.write(decodedBytes);
                fos.close();

                final JsonObject fileBody = new JsonObject();
                fileBody.addProperty("name", imageAttachment.getName());
                fileBody.addProperty("contentType", "image");

                logBody.add("file", fileBody);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String toLevel(Result result) {
        final String status = result.getStatus();

        if (status.equalsIgnoreCase("skipped")) {
            return "warn";
        } else if (status.equalsIgnoreCase("passed")) {
            return "info";
        }
        return "error";
    }

    private static String toStatusName(Result result) {
        return String.format(" (%s)", result.getStatus());
    }
}
