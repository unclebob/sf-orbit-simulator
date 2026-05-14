package orbit.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

  @Test
  void returnsUsageExitCodeForWrongArguments() {
    assertEquals(2, GherkinParser.exitCode(new String[] {"feature-only"}));
  }

  @Test
  void writesJsonIrFromFeatureFile(@TempDir Path directory) throws Exception {
    Path feature = directory.resolve("example.feature");
    Path json = directory.resolve("feature.json");
    Files.writeString(feature, """
        Feature: Example
        Scenario: sample
          Then value is present
        """);

    assertEquals(0, GherkinParser.exitCode(new String[] {feature.toString(), json.toString()}));

    assertEquals("Example", FeatureJson.read(Files.readString(json)).name());
  }
}
