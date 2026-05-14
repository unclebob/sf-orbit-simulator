package orbit.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;

class AcceptanceRuntimeTest {
  @Test
  void createsOneDynamicTestPerScenarioExample() {
    Feature feature = new Feature(
        "Example",
        List.of(),
        List.of(new Feature.Scenario(
            "sample",
            List.of(new Feature.Step("Then", "ok", List.of())),
            List.of(Map.of("value", "one"), Map.of("value", "two"))
        ))
    );

    List<DynamicTest> tests = new AcceptanceRuntime((step, world, example) -> { }).tests(feature).stream().toList();

    assertEquals(2, tests.size());
  }
}
