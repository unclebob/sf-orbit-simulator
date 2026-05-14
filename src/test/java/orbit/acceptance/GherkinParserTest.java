package orbit.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class GherkinParserTest {
  @Test
  void parsesFeatureBackgroundScenarioAndExamples() {
    Feature feature = new GherkinParser().parse(List.of(
        "Feature: Example",
        "Background:",
        "  Given setup",
        "Scenario Outline: sample",
        "  Then value is <value>",
        "Examples:",
        "  | value |",
        "  | 42    |"
    ));

    assertEquals("Example", feature.name());
    assertEquals("setup", feature.background().getFirst().text());
    assertEquals("sample", feature.scenarios().getFirst().name());
    assertEquals(List.of("value"), feature.scenarios().getFirst().steps().getFirst().parameters());
    assertEquals("42", feature.scenarios().getFirst().examples().getFirst().get("value"));
  }

  @Test
  void rejectsMissingFeature() {
    assertThrows(IllegalArgumentException.class, () -> new GherkinParser().parse(List.of(
        "Scenario: sample",
        "  Then value is present"
    )));
  }
}
