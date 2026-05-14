package orbit.acceptance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GherkinMutatorTest {
  @Test
  void filtersGravityInputsThatDoNotAffectAcceleration() {
    List<String> paths = mutationPaths(
        scenario(
            "Gravity is applied between every pair of bodies",
            Map.of(
                "first_body", "sun",
                "first_vx", "0",
                "first_mass", "2000",
                "first_x", "0",
                "first_ax", "0.002066"
            )
        )
    );

    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".first_body")));
    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".first_vx")));
    assertTrue(paths.stream().anyMatch(path -> path.endsWith(".first_mass")));
  }

  @Test
  void filtersControlInputsThatAreIntentionallyDiscarded() {
    List<String> paths = mutationPaths(
        scenario(
            "Pause stops physics updates",
            Map.of(
                "paused_seconds", "5",
                "before_pause_seconds", "1",
                "x", "219.9592"
            )
        ),
        scenario(
            "Restart restores the initial simulation",
            Map.of(
                "elapsed_seconds", "3",
                "gravity_constant", "1",
                "x", "220"
            )
        )
    );

    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".paused_seconds")));
    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".elapsed_seconds")));
    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".gravity_constant")));
    assertTrue(paths.stream().anyMatch(path -> path.endsWith(".before_pause_seconds")));
    assertTrue(paths.stream().anyMatch(path -> path.endsWith(".x")));
  }

  @Test
  void filtersNearBodyClickPreconditionDistance() {
    List<String> paths = mutationPaths(
        scenario(
            "Near-body click adds a body in circular orbit around that body",
            Map.of(
                "diameter_count", "4",
                "center_body", "earth",
                "x", "220",
                "y", "60"
            )
        )
    );

    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".diameter_count")));
    assertTrue(paths.stream().anyMatch(path -> path.endsWith(".center_body")));
  }

  @Test
  void filtersVelocityPreviewMouseTargetEcho() {
    List<String> paths = mutationPaths(
        scenario(
            "Dragging a body previews its velocity change",
            Map.of(
                "body", "earth",
                "body_x", "220",
                "mouse_x", "220",
                "mouse_y", "-50"
            )
        )
    );

    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".mouse_x")));
    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".mouse_y")));
    assertTrue(paths.stream().anyMatch(path -> path.endsWith(".body_x")));
  }

  @Test
  void filtersCollisionSetupValuesThatDoNotAffectTheAssertedOutcome() {
    List<String> paths = mutationPaths(
        scenario(
            "Bodies outside collision range remain separate",
            Map.ofEntries(
                Map.entry("first_body", "alpha"),
                Map.entry("first_color", "blue"),
                Map.entry("first_mass", "3"),
                Map.entry("first_radius_px", "4"),
                Map.entry("first_x", "0"),
                Map.entry("first_y", "0"),
                Map.entry("first_vx", "2"),
                Map.entry("second_body", "beta"),
                Map.entry("second_color", "gray"),
                Map.entry("second_mass", "1"),
                Map.entry("second_radius_px", "3"),
                Map.entry("second_x", "5"),
                Map.entry("second_y", "0"),
                Map.entry("second_vx", "-2")
            )
        ),
        scenario(
            "Colliding bodies merge into one body",
            Map.of(
                "first_body", "alpha",
                "first_color", "blue",
                "first_mass", "3",
                "second_body", "beta",
                "second_color", "gray",
                "second_mass", "1"
            )
        )
    );

    assertFalse(paths.stream().anyMatch(path -> path.contains("scenarios[0]") && path.endsWith(".first_body")));
    assertFalse(paths.stream().anyMatch(path -> path.contains("scenarios[0]") && path.endsWith(".first_radius_px")));
    assertFalse(paths.stream().anyMatch(path -> path.contains("scenarios[0]") && path.endsWith(".second_vx")));
    assertTrue(paths.stream().anyMatch(path -> path.contains("scenarios[0]") && path.endsWith(".first_x")));
    assertFalse(paths.stream().anyMatch(path -> path.contains("scenarios[1]") && path.endsWith(".second_color")));
    assertTrue(paths.stream().anyMatch(path -> path.contains("scenarios[1]") && path.endsWith(".first_mass")));
  }

  @Test
  void reportsTextAndJsonResults() {
    Mutation mutation = new Mutation("m\"1", "$.scenarios[0].examples[0].mass", "one\n1", "two\t2");
    List<GherkinMutator.Result> results = List.of(
        new GherkinMutator.Result(mutation, "killed", "", "", 10),
        new GherkinMutator.Result(mutation, "survived", "still passed", "", 20),
        new GherkinMutator.Result(mutation, "error", "", "compile failed", 30)
    );

    String text = GherkinMutator.report(results, false);
    String json = GherkinMutator.report(results, true);

    assertTrue(text.contains("total=3 killed=1 survived=1 errors=1"));
    assertTrue(text.contains("output:\nstill passed"));
    assertTrue(json.contains("\"Survived\":1"));
    assertTrue(json.contains("\"ID\":\"m\\\"1\""));
    assertTrue(json.contains("\"Original\":\"one\\n1\""));
    assertTrue(json.contains("\"Mutated\":\"two\\t2\""));
    assertTrue(json.contains("\"Error\":\"compile failed\""));
  }

  @Test
  void returnsUsageExitCodeForBadOptions() {
    assertEquals(2, GherkinMutator.exitCode(new String[] {"--unknown"}));
  }

  @Test
  void normalizesWorkerCountAndParsesTimeout() {
    GherkinMutator.Options options = GherkinMutator.Options.parse(new String[] {
        "--workers", "0",
        "--timeout", "5s"
    });

    assertEquals(1, options.workers());
    assertEquals(5, options.timeout().toSeconds());
  }

  private static List<String> mutationPaths(Feature.Scenario... scenarios) {
    Feature feature = new Feature("Example", List.of(), List.of(scenarios));
    return new GherkinMutator().mutations(feature).stream().map(Mutation::path).toList();
  }

  private static Feature.Scenario scenario(String name, Map<String, String> example) {
    return new Feature.Scenario(name, List.of(), List.of(example));
  }
}
