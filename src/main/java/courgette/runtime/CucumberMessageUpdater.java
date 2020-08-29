package courgette.runtime;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class CucumberMessageUpdater {
    private LinkedList<String> messages = new LinkedList<>();
    private HashMap<String, Integer> uriMap = new HashMap<>();

    private final String KEY = "window.CUCUMBER_MESSAGES";

    public void filterMessages(String report) {
        Optional<String> message = extract(report);
        message.ifPresent(s -> messages.add(s));

        Optional<String> uri = extractUri(report);
        if (uri.isPresent()) {
            int counter = uriMap.getOrDefault(uri.get(), 0) + 1;
            uriMap.put(uri.get(), counter);
        }
    }

    public void addMessage(String cucumberMessage) {
        if (!cucumberMessage.isEmpty()) {
            messages.add(cucumberMessage);

            Optional<String> uri = extractUri(cucumberMessage);
            if (uri.isPresent()) {
                int counter = uriMap.getOrDefault(uri.get(), 0) + 1;
                uriMap.put(uri.get(), counter);
            }
        }
    }

    public String updateMessages(String report) {
        Optional<String> cucumberMessages = extract(report);

        if (cucumberMessages.isPresent()) {
            updateMessageUris();
            report = report.replace(cucumberMessages.get(), combineMessages());
        }
        return report;
    }

    public LinkedList<String> updateAndGetMessages() {
        updateMessageUris();
        return messages;
    }

    private String combineMessages() {
        return String.join(",", messages);
    }

    private void updateMessageUris() {
        if (!uriMap.isEmpty()) {
            uriMap.values().removeIf(count -> count < 2);
        }

        if (!messages.isEmpty()) {
            uriMap.keySet().forEach(uri -> {
                List<String> uriToUpdate = messages.stream().filter(t -> t.contains(uri)).collect(Collectors.toList());

                AtomicInteger counterLow = new AtomicInteger(1);
                final int counterHigh = uriToUpdate.size();

                uriToUpdate.forEach(current -> {
                    String copy = current;
                    String newUri = uri.substring(0, uri.length() - 1) + String.format(" (scenario %d of %d)\"", counterLow.getAndIncrement(), counterHigh);
                    copy = copy.replace(uri, newUri);
                    messages.remove(current);
                    messages.add(copy);
                });
            });
        }
    }

    private Optional<String> extract(String report) {
        if (report != null && report.contains(KEY)) {
            String cucumberMessage = report.substring(report.indexOf(KEY));
            cucumberMessage = cucumberMessage.substring(cucumberMessage.indexOf("[") + 1);
            return Optional.of(cucumberMessage.substring(0, cucumberMessage.indexOf("];")));
        }
        return Optional.empty();
    }

    private Optional<String> extractUri(String report) {
        Optional<String> uri = Optional.empty();

        while (report.contains("\"uri\":")) {
            int start = report.indexOf("uri");
            String uriSubStr = report.substring(start);
            uri = Optional.of(uriSubStr.split(",")[0]);
            report = report.replace(uri.get(), "");
        }
        return uri;
    }
}
