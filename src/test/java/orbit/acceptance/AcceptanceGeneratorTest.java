package orbit.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AcceptanceGeneratorTest {
  @Test
  void returnsUsageExitCodeForWrongArguments() {
    assertEquals(2, AcceptanceGenerator.exitCode(new String[] {"ir-only"}));
  }

  @Test
  void writesGeneratedAcceptanceTest(@TempDir Path directory) throws Exception {
    Path ir = directory.resolve("feature.json");
    Path output = directory.resolve("generated/OrbitSimulatorAcceptanceTest.java");
    Feature feature = new Feature(
        "Example",
        List.of(),
        List.of(new Feature.Scenario("sample", List.of(), List.of()))
    );
    Files.writeString(ir, FeatureJson.write(feature));

    assertEquals(0, AcceptanceGenerator.exitCode(new String[] {ir.toString(), output.toString()}));

    assertTrue(Files.readString(output).contains("OrbitSimulatorAcceptanceTest"));
  }
}
