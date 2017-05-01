package courgette.runtime;

import cucumber.runtime.junit.FeatureRunner;
import cucumber.runtime.model.CucumberFeature;
import gherkin.formatter.model.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.lang.System.out;

public class CourgetteRunnerFilter {

    public static List<FeatureRunner> filter(List<FeatureRunner> featureRunners, String[] tags) {
        if (featureRunners.isEmpty()) {
            return featureRunners;
        }
        return featureRunnerFilter.apply(featureRunners, tags);
    }

    private static BiFunction<List<FeatureRunner>, String[], List<FeatureRunner>> featureRunnerFilter = (features, tags) -> {
        if (tags.length == 0) {
            return features;
        }

        final List<FeatureRunner> filteredFeatureRunners = new ArrayList<>();

        final List<String> runnerTags = new ArrayList<>(Arrays.asList(tags));

        features.forEach(featureRunner -> {
            final CucumberFeature cucumberFeature = new CourgetteFeature(featureRunner).getCucumberFeature();

            List<String> excludedTags = new ArrayList<>();

            runnerTags.stream()
                    .filter(tag -> tag.contains("~"))
                    .forEach(excludedTag -> excludedTags.add(excludedTag.replace("~", "")));

            runnerTags.removeIf(t -> t.contains("~"));

            final List<String> featureTags = cucumberFeature.getGherkinFeature()
                    .getTags().stream()
                    .map(Tag::getName)
                    .collect(Collectors.toList());

            if (featureTags.stream().noneMatch(excludedTags::contains)) {
                if (featureTags.containsAll(runnerTags)) {
                    filteredFeatureRunners.add(featureRunner);
                }
            }
        });

        if (filteredFeatureRunners.isEmpty()) {
            out.println(String.format("None of the features matched the filters: %s", Arrays.toString(tags)));
        }
        return filteredFeatureRunners;
    };
}
