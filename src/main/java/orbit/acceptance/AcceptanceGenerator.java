package orbit.acceptance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AcceptanceGenerator {
  public static void main(String[] args) {
    System.exit(exitCode(args));
  }

  static int exitCode(String[] args) {
    if (args.length != 2) {
      System.err.println("usage: acceptance-generator <json-ir> <generated-test-output>");
      return 2;
    }
    try {
      new AcceptanceGenerator().generate(Path.of(args[0]), Path.of(args[1]));
      return 0;
    } catch (Exception error) {
      System.err.println(error.getMessage());
      return 1;
    }
  }

  public void generate(Path jsonIr, Path output) throws IOException {
    String json = Files.readString(jsonIr);
    FeatureJson.read(json);
    Files.createDirectories(output.getParent());
    Files.writeString(output, source(json));
  }

  private String source(String json) {
    return """
        package orbit.acceptance.generated;

        import java.util.Collection;
        import org.junit.jupiter.api.DynamicTest;
        import org.junit.jupiter.api.TestFactory;
        import orbit.acceptance.AcceptanceRuntime;
        import orbit.acceptance.FeatureJson;
        import orbit.acceptance.OrbitStepHandlers;

        public class OrbitSimulatorAcceptanceTest {
          private static final String JSON = \"\"\"
        %s
              \"\"\";

          @TestFactory
          Collection<DynamicTest> generatedAcceptanceTests() {
            return new AcceptanceRuntime(new OrbitStepHandlers()).tests(FeatureJson.read(JSON));
          }
        }
        """.formatted(indent(json, 4));
  }

  private static String indent(String text, int spaces) {
    String prefix = " ".repeat(spaces);
    return prefix + text.replace("\n", "\n" + prefix);
  }
}
