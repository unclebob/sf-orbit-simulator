package orbit.acceptance;

import java.util.List;
import java.util.Map;

public record Feature(String name, List<Step> background, List<Scenario> scenarios) {
  public record Scenario(String name, List<Step> steps, List<Map<String, String>> examples) {
  }

  public record Step(String keyword, String text, List<String> parameters) {
  }
}
