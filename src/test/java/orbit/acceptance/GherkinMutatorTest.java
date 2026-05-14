package orbit.acceptance;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
