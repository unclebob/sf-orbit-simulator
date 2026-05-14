package orbit.acceptance;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DynamicTest;

public class AcceptanceRuntime {
  private final StepHandlers handlers;

  public AcceptanceRuntime(StepHandlers handlers) {
    this.handlers = handlers;
  }

  public Collection<DynamicTest> tests(Feature feature) {
    List<DynamicTest> tests = new ArrayList<>();
    for (Feature.Scenario scenario : feature.scenarios()) {
      List<Map<String, String>> examples = scenario.examples().isEmpty() ? List.of(Map.of()) : scenario.examples();
      for (int i = 0; i < examples.size(); i++) {
        Map<String, String> example = examples.get(i);
        int index = i + 1;
        tests.add(DynamicTest.dynamicTest(scenario.name() + "/example_" + index, () -> run(feature, scenario, example)));
      }
    }
    return tests;
  }

  private void run(Feature feature, Feature.Scenario scenario, Map<String, String> example) {
    World world = new World();
    List<Feature.Step> steps = new ArrayList<>();
    steps.addAll(feature.background());
    steps.addAll(scenario.steps());
    for (Feature.Step step : steps) {
      try {
        handlers.handle(step.text(), world, example);
      } catch (UnsupportedStep unsupported) {
        fail("Unsupported step: " + step.text());
      } catch (AssertionError assertion) {
        throw assertion;
      } catch (RuntimeException error) {
        fail(error.getMessage());
      }
    }
  }
}
