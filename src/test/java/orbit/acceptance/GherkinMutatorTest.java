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
    assertFilteredOutAndRetained(
        "Gravity is applied between every pair of bodies",
        Map.of(
            "first_body", "sun",
            "first_vx", "0",
            "first_mass", "2000",
            "first_x", "0",
            "first_ax", "0.002066"
        ),
        List.of("first_body", "first_vx"),
        List.of("first_mass")
    );
  }

  @Test
  void filtersVelocityPreviewMouseTargetEcho() {
    assertFilteredOutAndRetained(
        "Dragging a body previews its velocity change",
        Map.of(
            "body", "earth",
            "body_x", "220",
            "mouse_x", "220",
            "mouse_y", "-50"
        ),
        List.of("mouse_x", "mouse_y"),
        List.of("body_x")
    );
  }

  @Test
  void filtersRadiusCorrelationExampleValuesThatPreserveTheOrderingProperty() {
    assertFilteredOutAndRetained(
        "Body radius increases with mass",
        Map.of(
            "smaller_body", "moon",
            "smaller_mass", "1",
            "smaller_radius_px", "4",
            "larger_body", "earth",
            "larger_mass", "100",
            "larger_radius_px", "12"
        ),
        List.of("smaller_body", "smaller_mass", "smaller_radius_px", "larger_body", "larger_mass", "larger_radius_px"),
        List.of()
    );
  }

  @Test
  void filtersSpeedSliderStartingValueThatIsReplacedByDrag() {
    assertFilteredOutAndRetained(
        "Speed slider thumb can be dragged",
        Map.of(
            "start_speed", "1",
            "end_speed", "12",
            "speed_label", "12X"
        ),
        List.of("start_speed"),
        List.of("end_speed")
    );
  }

  @Test
  void filtersVerletSubstepValuesThatPreserveRoundedVisibleState() {
    assertFilteredOutAndRetained(
        "Physics ticks update velocity and position from gravity",
        Map.of(
            "body", "earth",
            "seconds", "1",
            "gravity_constant", "1",
            "substep_seconds", "0.016667",
            "x", "219.9796"
        ),
        List.of("substep_seconds"),
        List.of("seconds", "gravity_constant", "x")
    );
  }

  @Test
  void filtersFrameIndependenceValuesThatPreserveTheComparisonProperty() {
    assertFilteredOutAndRetained(
        "Display frame size does not change physics results",
        Map.of(
            "body", "earth",
            "frame_count", "1200",
            "gravity_constant", "1",
            "physics_seconds", "20",
            "substep_seconds", "0.016667"
        ),
        List.of("frame_count", "gravity_constant", "physics_seconds", "substep_seconds"),
        List.of("body")
    );
  }

  private static void assertFilteredOutAndRetained(
      String scenarioName,
      Map<String, String> example,
      List<String> filteredKeys,
      List<String> retainedKeys
  ) {
    List<String> paths = mutationPaths(scenario(scenarioName, example));
    filteredKeys.forEach(key ->
        assertFalse(
            paths.stream().anyMatch(path -> path.endsWith("." + key)),
            () -> "Expected key to be filtered: " + key
        )
    );
    retainedKeys.forEach(key ->
        assertTrue(
            paths.stream().anyMatch(path -> path.endsWith("." + key)),
            () -> "Expected key to be retained: " + key
        )
    );
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
                "start_center_x", "120",
                "start_center_y", "-80",
                "x", "220"
            )
        )
    );

    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".paused_seconds")));
    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".elapsed_seconds")));
    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".gravity_constant")));
    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".start_center_x")));
    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".start_center_y")));
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
  void filtersCollisionSetupValuesThatDoNotAffectTheAssertedOutcome() {
    List<String> paths = mutationPaths(
        scenario(
            "Bodies whose rendered edges do not touch remain separate",
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
            "Bodies collide inelastically when their rendered edges touch on screen",
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
    assertFalse(paths.stream().anyMatch(path -> path.contains("scenarios[0]") && path.endsWith(".second_x")));
    assertFalse(paths.stream().anyMatch(path -> path.contains("scenarios[0]") && path.endsWith(".second_vx")));
    assertFalse(paths.stream().anyMatch(path -> path.contains("scenarios[0]") && path.endsWith(".first_x")));
    assertFalse(paths.stream().anyMatch(path -> path.contains("scenarios[1]") && path.endsWith(".first_color")));
    assertFalse(paths.stream().anyMatch(path -> path.contains("scenarios[1]") && path.endsWith(".second_color")));
    assertTrue(paths.stream().anyMatch(path -> path.contains("scenarios[1]") && path.endsWith(".first_mass")));
  }

  @Test
  void filtersOverlapSeparationSetupValuesThatDoNotAffectTheAssertedDistance() {
    assertFilteredOutAndRetained(
        "Bodies whose rendered edges overlap are separated until they only touch",
        Map.ofEntries(
            Map.entry("first_body", "alpha"),
            Map.entry("first_color", "blue"),
            Map.entry("first_mass", "3"),
            Map.entry("first_radius_px", "4"),
            Map.entry("first_x", "0"),
            Map.entry("first_y", "0"),
            Map.entry("first_vx", "0"),
            Map.entry("first_vy", "0"),
            Map.entry("restitution", "0.5"),
            Map.entry("second_body", "beta"),
            Map.entry("second_color", "gray"),
            Map.entry("second_mass", "1"),
            Map.entry("second_radius_px", "3"),
            Map.entry("second_x", "6"),
            Map.entry("second_y", "0"),
            Map.entry("second_vx", "0"),
            Map.entry("second_vy", "0")
        ),
        List.of(
            "first_body",
            "first_color",
            "first_mass",
            "first_vx",
            "first_vy",
            "restitution",
            "second_body",
            "second_color",
            "second_mass",
            "second_vx",
            "second_vy"
        ),
        List.of("first_radius_px", "second_radius_px", "second_x")
    );
  }

  @Test
  void filtersSunRecenterSetupValuesThatDoNotAffectTheCenteredView() {
    assertFilteredOutAndRetained(
        "Sun recenter button centers the view on the sun",
        Map.of(
            "elapsed_seconds", "1",
            "gravity_constant", "1",
            "substep_seconds", "0.016667",
            "start_center_x", "120",
            "start_center_y", "-80",
            "sun_x", "0.0010",
            "sun_y", "0"
        ),
        List.of("start_center_x", "start_center_y", "substep_seconds"),
        List.of("elapsed_seconds", "gravity_constant", "sun_x", "sun_y")
    );
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
