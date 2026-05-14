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
    Feature feature = new Feature(
        "Example",
        List.of(),
        List.of(new Feature.Scenario(
            "Gravity is applied between every pair of bodies",
            List.of(),
            List.of(Map.of(
                "first_body", "sun",
                "first_vx", "0",
                "first_mass", "2000",
                "first_x", "0",
                "first_ax", "0.002066"
            ))
        ))
    );

    List<String> paths = new GherkinMutator().mutations(feature).stream().map(Mutation::path).toList();

    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".first_body")));
    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".first_vx")));
    assertTrue(paths.stream().anyMatch(path -> path.endsWith(".first_mass")));
  }

  @Test
  void filtersControlInputsThatAreIntentionallyDiscarded() {
    Feature feature = new Feature(
        "Example",
        List.of(),
        List.of(
            new Feature.Scenario(
                "Pause stops physics updates",
                List.of(),
                List.of(Map.of(
                    "paused_seconds", "5",
                    "before_pause_seconds", "1",
                    "x", "219.9592"
                ))
            ),
            new Feature.Scenario(
                "Restart restores the initial simulation",
                List.of(),
                List.of(Map.of(
                    "elapsed_seconds", "3",
                    "gravity_constant", "1",
                    "x", "220"
                ))
            )
        )
    );

    List<String> paths = new GherkinMutator().mutations(feature).stream().map(Mutation::path).toList();

    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".paused_seconds")));
    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".elapsed_seconds")));
    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".gravity_constant")));
    assertTrue(paths.stream().anyMatch(path -> path.endsWith(".before_pause_seconds")));
    assertTrue(paths.stream().anyMatch(path -> path.endsWith(".x")));
  }

  @Test
  void filtersNearBodyClickPreconditionDistance() {
    Feature feature = new Feature(
        "Example",
        List.of(),
        List.of(new Feature.Scenario(
            "Near-body click adds a body in circular orbit around that body",
            List.of(),
            List.of(Map.of(
                "diameter_count", "4",
                "center_body", "earth",
                "x", "220",
                "y", "60"
            ))
        ))
    );

    List<String> paths = new GherkinMutator().mutations(feature).stream().map(Mutation::path).toList();

    assertFalse(paths.stream().anyMatch(path -> path.endsWith(".diameter_count")));
    assertTrue(paths.stream().anyMatch(path -> path.endsWith(".center_body")));
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
}
