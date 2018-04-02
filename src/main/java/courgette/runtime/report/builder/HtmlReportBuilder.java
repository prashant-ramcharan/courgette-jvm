package courgette.runtime.report.builder;

import courgette.runtime.report.model.Feature;
import courgette.runtime.report.model.Hook;
import courgette.runtime.report.model.Result;
import courgette.runtime.report.model.Scenario;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class HtmlReportBuilder {
    private List<Feature> featureList;

    private static final String PASSED = "passed";
    private static final String FAILED = "failed";
    private static final String SUCCESS = "success";
    private static final String DANGER = "danger";
    private static final String WARNING = "warning";

    private HtmlReportBuilder(List<Feature> featureList) {
        this.featureList = featureList;
    }

    public static HtmlReportBuilder create(List<Feature> featureList) {
        return new HtmlReportBuilder(featureList);
    }

    public String getHtmlTableFeatureRows() {
        final StringBuilder tableRows = new StringBuilder();
        featureList.forEach(feature -> tableRows.append(TableRowBuilder.create(feature).getFeatureRow()));
        return tableRows.toString();
    }

    public String getHtmlTableScenarioRows() {
        final StringBuilder tableRows = new StringBuilder();
        featureList.forEach(feature -> tableRows.append(TableRowBuilder.create(feature).getScenarioRow()));
        return tableRows.toString();
    }

    public String getHtmlModals() {
        final StringBuilder modals = new StringBuilder();

        featureList
                .stream()
                .map(Feature::getScenarios)
                .collect(Collectors.toList())
                .forEach(e ->
                        e.forEach(scenario -> {
                            modals.append(ModalBuilder.create(scenario).getModal());
                        }));
        return modals.toString();
    }

    private static Function<Result, String> statusBadge = (result) -> {
        String status = result.getStatus();
        return status.equalsIgnoreCase(PASSED) ? SUCCESS : status.equalsIgnoreCase(FAILED) ? DANGER : WARNING;
    };

    private static Predicate<Feature> successFeature = (feature ->
            feature.getScenarios().stream().allMatch(e -> e.getBefore().stream().allMatch(t -> t.getResult().getStatus().equalsIgnoreCase(PASSED)))
                    && feature.getScenarios().stream().allMatch(e -> e.getAfter().stream().allMatch(t -> t.getResult().getStatus().equalsIgnoreCase(PASSED)))
                    && feature.getScenarios().stream().allMatch(e -> e.getSteps().stream().allMatch(t -> t.getResult().getStatus().equalsIgnoreCase(PASSED))));

    private static Predicate<Scenario> successScenario = (scenario ->
            scenario.getBefore().stream().allMatch(t -> t.getResult().getStatus().equalsIgnoreCase(PASSED))
                    && scenario.getAfter().stream().allMatch(t -> t.getResult().getStatus().equalsIgnoreCase(PASSED))
                    && scenario.getSteps().stream().allMatch(t -> t.getResult().getStatus().equalsIgnoreCase(PASSED)));

    static class TableRowBuilder {
        private Feature feature;

        private TableRowBuilder(Feature feature) {
            this.feature = feature;
        }

        public static TableRowBuilder create(Feature feature) {
            return new TableRowBuilder(feature);
        }

        private final String FEATURE_ROW =
                "<div class=\"row\">\n" +
                        "<div class=\"col-lg-10\">" +
                        "<a data-toggle=\"collapse\" href=\"#%s\">%s</a>\n" +
                        "</div>\n" +
                        "<div class=\"col-lg-auto\">\n" +
                        "<span class=\"badge badge-%s\">Feature %s</span>\n" +
                        "</div>\n" +
                        "</div>\n" +
                        "<div class=\"collapse mt-2\" id=\"%s\">\n";

        private final String SCENARIO_ROW =
                "<div class=\"row\">\n" +
                        "<div class=\"col-lg-9\">%s\n" +
                        "</div>\n" +
                        "<div id=\"row-badge\" class=\"col-lg-2\">\n" +
                        "<span class=\"float-left badge badge-%s\">Scenario %s</span>\n" +
                        "<a href=\"\" data-toggle=\"modal\" data-target=\"#%s\" class=\"float-right badge badge-primary\">View Steps</a>\n" +
                        "</div>\n" +
                        "</div>";

        private final String SCENARIO_CHILD_ROW =
                "<hr>\n" +
                        "<div class=\"row\">\n" +
                        "<div id=\"child-row\" class=\"col-lg-8\">%s\n" +
                        "</div>\n" +
                        "<div id=\"child-row-badge\" class=\"col-lg-2\">\n" +
                        "<span class=\"float-left badge badge-%s\">Scenario %s</span>\n" +
                        "<a href=\"\" data-toggle=\"modal\" data-target=\"#%s\" class=\"float-right badge badge-primary\">View Steps</a>\n" +
                        "</div>\n" +
                        "</div>";

        private String getScenarioRow() {
            final StringBuilder scenarioRows = new StringBuilder();

            feature.getScenarios().forEach(scenario -> {
                scenarioRows.append("<tr>\n<td>\n");
                scenarioRows.append(getScenario(scenario, SCENARIO_ROW));
                scenarioRows.append("</td>\n</tr>\n");
            });

            return scenarioRows.toString();
        }

        private String getFeatureRow() {
            final StringBuilder featureRows = new StringBuilder();

            String featureBadge = successFeature.test(feature) ? SUCCESS : DANGER;
            String featureStatus = featureBadge.equals(SUCCESS) ? PASSED : FAILED;
            String featureName = feature.getName();
            String featureId = feature.getCourgetteFeatureId();

            featureRows.append("<tr>\n<td>\n");
            featureRows.append(String.format(FEATURE_ROW, featureId, featureName, featureBadge, featureStatus, featureId));

            feature.getScenarios().forEach(scenario -> featureRows.append(getScenario(scenario, SCENARIO_CHILD_ROW)));

            featureRows.append("</div>\n" +
                    "</td>\n" +
                    "</tr>");

            return featureRows.toString();
        }

        private String getScenario(Scenario scenario, final String format) {
            final StringBuilder scenarioRows = new StringBuilder();

            String scenarioName = scenario.getName();
            String scenarioBadge = successScenario.test(scenario) ? SUCCESS : DANGER;
            String scenarioStatus = scenarioBadge.equals(SUCCESS) ? PASSED : FAILED;
            String scenarioId = scenario.getCourgetteScenarioId();

            scenarioRows.append(String.format(format, scenarioName, scenarioBadge, scenarioStatus, scenarioId));

            return scenarioRows.toString();
        }
    }

    static class ModalBuilder {
        private Scenario scenario;

        private ModalBuilder(Scenario scenario) {
            this.scenario = scenario;
        }

        public static ModalBuilder create(Scenario scenario) {
            return new ModalBuilder(scenario);
        }

        private final String MODEL_HEADER =
                "<div class=\"modal fade\" id=\"%s\" tabindex=\"-1\" role=\"dialog\" aria-labelledby=\"%s\" aria-hidden=\"true\">\n" +
                        "<div class=\"modal-dialog modal-lg\" role=\"document\">\n" +
                        "<div class=\"modal-content\">\n" +
                        "<div class=\"modal-header\">\n" +
                        "<h5 class=\"modal-title\">%s</h5>\n" +
                        "<button type=\"button\" class=\"close\" data-dismiss=\"modal\" aria-label=\"Close\">\n" +
                        "<span aria-hidden=\"true\">&times;</span>\n" +
                        "</button>\n" +
                        "</div>\n";

        private final String MODEL_BODY_ROW =
                "<div class=\"row\">\n" +
                        "<div class=\"col-lg-9\">\n" +
                        "%s\n" +
                        "</div>\n\n" +
                        "<div class=\"col-lg-3\">\n" +
                        "<span class=\"float-right\">\n" +
                        "<span class=\"badge badge-info\">%s ms</span>\n" +
                        "<span class=\"badge badge-%s\">%s</span>\n" +
                        "</span>\n" +
                        "</div>\n" +
                        "</div>\n";

        private final String MODAL_BODY_ROW_EMBEDDINGS =
                "<div class=\"row mt-2\">\n" +
                        "<div class=\"col-lg-12\">\n" +
                        "<img src=\"images/%s\" class=\"img-thumbnail\">\n" +
                        "</div>\n" +
                        "</div>\n";

        private final String MODAL_BODY_ROW_ERROR_MESSAGE =
                "<div class=\"row mt-2\">\n" +
                        "<div class=\"col-lg-12 text-danger\">\n" +
                        "%s\n" +
                        "</div>\n" +
                        "</div>\n";

        private Function<Hook, String> hookFunc = (hook -> {
            StringBuilder hookBuilder = new StringBuilder();

            String name = hook.getLocation();
            Result result = hook.getResult();

            hookBuilder.append(String.format(MODEL_BODY_ROW, name, result.getDuration(), statusBadge.apply(result), result.getStatus()));

            if (result.getErrorMessage() != null) {
                hookBuilder.append(String.format(MODAL_BODY_ROW_ERROR_MESSAGE, result.getErrorMessage()));
            }
            hookBuilder.append("<hr>\n");
            return hookBuilder.toString();
        });

        private String getModal() {
            final StringBuilder modal = new StringBuilder();

            modal.append(String.format(MODEL_HEADER, scenario.getCourgetteScenarioId(), scenario.getCourgetteScenarioId(), scenario.getName()));

            modal.append("<div class=\"modal-body\">\n");

            scenario.getBefore().forEach(before -> modal.append(hookFunc.apply(before)));

            scenario.getSteps().forEach(step -> {
                String status = step.getResult().getStatus();
                String statusBadge = status.equalsIgnoreCase(PASSED) ? SUCCESS : status.equalsIgnoreCase(FAILED) ? DANGER : WARNING;

                modal.append(String.format(MODEL_BODY_ROW, step.getKeyword() + step.getName(), step.getResult().getDuration(), statusBadge, status));

                step.getEmbeddings().forEach(embedding -> {
                    if (embedding.getMimeType().startsWith("image")) {
                        final String imageName = embedding.getCourgetteEmbeddingId();
                        final String imageFormat = embedding.getMimeType().split("/")[1];

                        modal.append(String.format(MODAL_BODY_ROW_EMBEDDINGS, imageName + "." + imageFormat));
                    }
                });

                if (step.getResult().getErrorMessage() != null) {
                    modal.append(String.format(MODAL_BODY_ROW_ERROR_MESSAGE, step.getResult().getErrorMessage()));
                }
                modal.append("<hr>\n");
            });

            scenario.getAfter().forEach(after -> modal.append(hookFunc.apply(after)));

            modal.append("</div>\n" +
                    "</div>\n" +
                    "</div>\n" +
                    "</div>\n\n");

            return modal.toString();
        }
    }
}
