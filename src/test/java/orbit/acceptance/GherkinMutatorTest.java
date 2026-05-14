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
  void reportsTextAndJsonResults() {
    Mutation mutation = new Mutation("m1", "$.scenarios[0].examples[0].mass", "1", "2");
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
    assertTrue(json.contains("\"Error\":\"compile failed\""));
  }

  @Test
  void returnsUsageExitCodeForBadOptions() {
    assertEquals(2, GherkinMutator.exitCode(new String[] {"--unknown"}));
  }
}
