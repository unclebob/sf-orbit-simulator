package orbit.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FeatureJsonTest {
  @Test
  void roundTripsFeatureIr() {
    Feature feature = new Feature(
        "Example",
        List.of(new Feature.Step("Given", "setup", List.of())),
        List.of(new Feature.Scenario(
            "sample",
            List.of(new Feature.Step("Then", "value is <value>", List.of("value"))),
            List.of(Map.of("value", "42"))
        ))
    );

    Feature read = FeatureJson.read(FeatureJson.write(feature));

    assertEquals("Example", read.name());
    assertEquals("setup", read.background().getFirst().text());
    assertEquals("42", read.scenarios().getFirst().examples().getFirst().get("value"));
  }
}
