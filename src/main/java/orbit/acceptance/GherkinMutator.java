package orbit.acceptance;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DynamicTest;

public class GherkinMutator {
  private static final Set<String> EQUIVALENT_GRAVITY_KEYS = Set.of(
      "first_body",
      "second_body",
      "first_vx",
      "first_vy",
      "second_vx",
      "second_vy"
  );
  private static final Set<String> EQUIVALENT_RESTART_KEYS = Set.of(
      "elapsed_seconds",
      "gravity_constant"
  );
  private static final Set<String> EQUIVALENT_VELOCITY_PREVIEW_KEYS = Set.of(
      "mouse_x",
      "mouse_y"
  );
  private static final Set<String> EQUIVALENT_SEPARATE_COLLISION_KEYS = Set.of(
      "first_body",
      "first_color",
      "first_mass",
      "first_radius_px",
      "first_vx",
      "first_vy",
      "first_x",
      "first_y",
      "second_body",
      "second_color",
      "second_mass",
      "second_radius_px",
      "second_vx",
      "second_vy",
      "second_x",
      "second_y"
  );
  private static final Set<String> EQUIVALENT_MERGED_COLLISION_KEYS = Set.of(
      "first_body",
      "second_body",
      "second_color"
  );
  private static final Set<String> EQUIVALENT_SCREEN_SEPARATE_COLLISION_KEYS = Set.of(
      "first_body",
      "first_color",
      "first_mass",
      "first_vx",
      "first_vy",
      "first_x",
      "first_y",
      "second_body",
      "second_color",
      "second_mass",
      "second_radius_px",
      "second_vx",
      "second_vy",
      "second_x",
      "second_y"
  );
  private static final Set<String> EQUIVALENT_RADIUS_CORRELATION_KEYS = Set.of(
      "larger_body",
      "larger_mass",
      "larger_radius_px",
      "smaller_body",
      "smaller_mass",
      "smaller_radius_px"
  );
  private static final Set<String> EQUIVALENT_TIDAL_DEFORMATION_KEYS = Set.of(
      "body",
      "mass",
      "source_body",
      "source_mass",
      "source_x"
  );
  private static final Set<String> EQUIVALENT_ELASTIC_GRAVITY_KEYS = Set.of(
      "source_body",
      "target_body",
      "target_mass",
      "target_vx",
      "target_vy"
  );
  private static final Map<String, Set<String>> EQUIVALENT_KEYS_BY_SCENARIO = Map.ofEntries(
      Map.entry("Gravity is applied between every pair of bodies", EQUIVALENT_GRAVITY_KEYS),
      Map.entry("Pause stops physics updates", Set.of("paused_seconds")),
      Map.entry("Restart restores the initial simulation", EQUIVALENT_RESTART_KEYS),
      Map.entry("Near-body click adds a body in circular orbit around that body", Set.of("diameter_count")),
      Map.entry("Speed slider thumb can be dragged", Set.of("start_speed")),
      Map.entry("Body radius increases with mass", EQUIVALENT_RADIUS_CORRELATION_KEYS),
      Map.entry("Tidal forces stretch elastic bodies into ellipses", EQUIVALENT_TIDAL_DEFORMATION_KEYS),
      Map.entry("Elastic body gravity is split between ellipse foci", EQUIVALENT_ELASTIC_GRAVITY_KEYS),
      Map.entry("Dragging a body previews its velocity change", EQUIVALENT_VELOCITY_PREVIEW_KEYS),
      Map.entry("Bodies outside collision range remain separate", EQUIVALENT_SEPARATE_COLLISION_KEYS),
      Map.entry("Bodies whose rendered edges do not touch remain separate", EQUIVALENT_SCREEN_SEPARATE_COLLISION_KEYS),
      Map.entry("Colliding bodies merge into one body", EQUIVALENT_MERGED_COLLISION_KEYS),
      Map.entry("Bodies merge when their rendered edges touch on screen", EQUIVALENT_MERGED_COLLISION_KEYS)
  );

  private final ValueMutator valueMutator = new ValueMutator();

  public static void main(String[] args) {
    System.exit(exitCode(args));
  }

  static int exitCode(String[] args) {
    try {
      return runCli(args);
    } catch (Usage error) {
      return fail(error, 2);
    } catch (Exception error) {
      return fail(error, 1);
    }
  }

  private static int fail(Exception error, int code) {
    System.err.println(error.getMessage());
    return code;
  }

  static int runCli(String[] args) throws IOException {
    Options options = Options.parse(args);
    List<Result> results = new GherkinMutator().run(options);
    System.out.print(report(results, options.json));
    return ResultSummary.from(results).failed() ? 1 : 0;
  }

  public List<Result> run(Options options) throws IOException {
    Feature base = new GherkinParser().parse(Files.readAllLines(options.feature));
    List<Mutation> mutations = mutations(base);
    Files.createDirectories(options.workDir);
    long deadline = System.nanoTime() + options.timeout.toNanos();
    List<Result> results = new ArrayList<>();
    for (Mutation mutation : mutations) {
      results.add(runUntilDeadline(options, base, mutation, deadline));
    }
    return results;
  }

  private Result runUntilDeadline(Options options, Feature base, Mutation mutation, long deadline) {
    if (System.nanoTime() >= deadline) {
      return new Result(mutation, "error", "", "mutation timeout expired", 0);
    }
    return runMutation(options, base, mutation);
  }

  private Result runMutation(Options options, Feature base, Mutation mutation) {
    long started = System.nanoTime();
    MutationWorkspace workspace = MutationWorkspace.create(options.workDir, mutation);
    try {
      Feature mutated = apply(base, mutation);
      workspace.create();
      Files.writeString(workspace.ir, FeatureJson.write(mutated));
      new AcceptanceGenerator().generate(workspace.ir, workspace.generated);
      RunnerResult runner = runAcceptance(mutated);
      return resultFor(mutation, runner, started);
    } catch (Exception error) {
      return new Result(mutation, "error", "", error.getMessage(), System.nanoTime() - started);
    }
  }

  public List<Mutation> mutations(Feature feature) {
    List<Mutation> mutations = new ArrayList<>();
    int id = 1;
    for (int s = 0; s < feature.scenarios().size(); s++) {
      List<Map<String, String>> examples = feature.scenarios().get(s).examples();
      for (int e = 0; e < examples.size(); e++) {
        List<String> keys = new ArrayList<>(examples.get(e).keySet());
        keys.sort(Comparator.naturalOrder());
        for (String key : keys) {
          if (isEquivalentMutation(feature.scenarios().get(s), key)) {
            continue;
          }
          String path = "$.scenarios[" + s + "].examples[" + e + "]." + key;
          String original = examples.get(e).get(key);
          String mutated = valueMutator.mutate(path, original);
          if (!mutated.equals(original)) {
            mutations.add(new Mutation("m" + id++, path, original, mutated));
          }
        }
      }
    }
    return mutations;
  }

  private boolean isEquivalentMutation(Feature.Scenario scenario, String key) {
    return EQUIVALENT_KEYS_BY_SCENARIO.getOrDefault(scenario.name(), Set.of()).contains(key);
  }

  private Feature apply(Feature feature, Mutation mutation) {
    MutationLocation location = MutationLocation.from(mutation.path());
    List<Feature.Scenario> scenarios = new ArrayList<>();
    for (int s = 0; s < feature.scenarios().size(); s++) {
      scenarios.add(applyToScenario(feature.scenarios().get(s), s, location, mutation.mutated()));
    }
    return new Feature(feature.name(), feature.background(), scenarios);
  }

  private Feature.Scenario applyToScenario(
      Feature.Scenario scenario,
      int scenarioIndex,
      MutationLocation location,
      String mutated
  ) {
    List<Map<String, String>> examples = scenario.examples().stream()
        .<Map<String, String>>map(LinkedHashMap::new)
        .toList();
    if (location.scenario == scenarioIndex) {
      examples.get(location.example).put(location.key, mutated);
    }
    return new Feature.Scenario(scenario.name(), scenario.steps(), examples);
  }

  private RunnerResult runAcceptance(Feature feature) {
    List<DynamicTest> tests = new AcceptanceRuntime(new OrbitStepHandlers()).tests(feature).stream().toList();
    AcceptanceResult result = execute(tests);
    return new RunnerResult(result.exitCode(), summaryOutput(tests.size(), result.succeeded, result.failures));
  }

  private AcceptanceResult execute(List<DynamicTest> tests) {
    AcceptanceResult result = new AcceptanceResult();
    for (DynamicTest test : tests) {
      result.record(test, execute(test));
    }
    return result;
  }

  private Throwable execute(DynamicTest test) {
    try {
      test.getExecutable().execute();
      return null;
    } catch (Throwable failure) {
      return failure;
    }
  }

  private String summaryOutput(int found, int succeeded, List<TestFailure> failures) {
    StringWriter text = new StringWriter();
    PrintWriter writer = new PrintWriter(text);
    writer.printf(
        "tests=%d succeeded=%d failed=%d%n",
        found,
        succeeded,
        failures.size()
    );
    for (TestFailure failure : failures) {
      writer.println(failure.displayName());
      failure.error().printStackTrace(writer);
    }
    writer.flush();
    return text.toString();
  }

  private record TestFailure(String displayName, Throwable error) {
  }

  private static final class AcceptanceResult {
    private final List<TestFailure> failures = new ArrayList<>();
    private int succeeded;

    void record(DynamicTest test, Throwable failure) {
      if (failure == null) {
        succeeded++;
      } else {
        failures.add(new TestFailure(test.getDisplayName(), failure));
      }
    }

    int exitCode() {
      return failures.isEmpty() ? 0 : 1;
    }
  }

  private static Result resultFor(Mutation mutation, RunnerResult runner, long started) {
    String status = runner.exitCode == 0 ? "survived" : "killed";
    return new Result(mutation, status, runner.output, "", System.nanoTime() - started);
  }

  static String report(List<Result> results, boolean json) {
    return json ? jsonReport(results) : textReport(results);
  }

  private static String textReport(List<Result> results) {
    ResultSummary summary = ResultSummary.from(results);
    StringBuilder report = new StringBuilder();
    report.append("total=").append(summary.total)
        .append(" killed=").append(summary.killed)
        .append(" survived=").append(summary.survived)
        .append(" errors=").append(summary.errors)
        .append('\n');
    for (Result result : results) {
      appendTextResult(report, result);
    }
    return report.toString();
  }

  private static String jsonReport(List<Result> results) {
    ResultSummary summary = ResultSummary.from(results);
    StringBuilder json = new StringBuilder();
    json.append("{\"summary\":{\"Total\":").append(summary.total)
        .append(",\"Killed\":").append(summary.killed)
        .append(",\"Survived\":").append(summary.survived)
        .append(",\"Errors\":").append(summary.errors)
        .append("},\"results\":[");
    for (int i = 0; i < results.size(); i++) {
      if (i > 0) {
        json.append(',');
      }
      appendJsonResult(json, results.get(i));
    }
    return json.append("]}\n").toString();
  }

  private static void appendTextResult(StringBuilder report, Result result) {
    report.append(String.format("%-8s %s%n", result.status, result.mutation.description()));
    if (!result.status.equals("killed")) {
      appendTextDetail(report, "error", result.error);
      appendTextDetail(report, "output", result.output);
    }
  }

  private static void appendTextDetail(StringBuilder report, String label, String value) {
    if (!value.isBlank()) {
      report.append("  ").append(label).append(":");
      appendTextSeparator(report, label);
      report.append(value).append('\n');
    }
  }

  private static void appendTextSeparator(StringBuilder report, String label) {
    report.append(label.equals("output") ? "\n" : " ");
  }

  private static void appendJsonResult(StringBuilder json, Result result) {
    json.append("{\"Mutation\":{");
    appendJsonStringField(json, "ID", result.mutation.id(), true);
    appendJsonStringField(json, "Path", result.mutation.path(), true);
    appendJsonStringField(json, "Description", result.mutation.description(), true);
    appendJsonStringField(json, "Original", result.mutation.original(), true);
    appendJsonStringField(json, "Mutated", result.mutation.mutated(), false);
    json.append("},");
    appendJsonStringField(json, "Status", result.status, true);
    appendJsonStringField(json, "Output", result.output, true);
    appendJsonStringField(json, "Error", result.error, true);
    json.append("\"Duration\":").append(result.duration).append('}');
  }

  private static void appendJsonStringField(StringBuilder json, String name, String value, boolean comma) {
    json.append('"').append(name).append("\":\"").append(escape(value)).append('"');
    if (comma) {
      json.append(',');
    }
  }

  private static String escape(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  public record Result(Mutation mutation, String status, String output, String error, long duration) {
  }

  private record RunnerResult(int exitCode, String output) {
  }

  private record MutationWorkspace(Path ir, Path generated) {
    static MutationWorkspace create(Path workDir, Mutation mutation) {
      Path mutationDir = workDir.resolve(mutation.id());
      return new MutationWorkspace(
          mutationDir.resolve("feature.json"),
          mutationDir.resolve("generated/orbit/acceptance/generated/OrbitSimulatorAcceptanceTest.java")
      );
    }

    void create() throws IOException {
      Files.createDirectories(generated.getParent());
    }
  }

  private record MutationLocation(int scenario, int example, String key) {
    static MutationLocation from(String path) {
      String[] parts = path.substring("$.scenarios[".length()).split("].examples\\[|\\]\\.", 3);
      return new MutationLocation(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), parts[2]);
    }

    boolean matches(int scenarioIndex, int exampleIndex) {
      return scenario == scenarioIndex && example == exampleIndex;
    }
  }

  private record ResultSummary(long total, long killed, long survived, long errors) {
    static ResultSummary from(List<Result> results) {
      return new ResultSummary(
          results.size(),
          count(results, "killed"),
          count(results, "survived"),
          count(results, "error")
      );
    }

    boolean failed() {
      return survived > 0 || errors > 0;
    }

    private static long count(List<Result> results, String status) {
      return results.stream().filter(result -> result.status.equals(status)).count();
    }
  }

  public record Options(Path feature, Path workDir, int workers, Duration timeout, boolean json) {
    static Options parse(String[] args) {
      Path feature = Path.of("features/orbit-simulator.feature");
      Path workDir = Path.of("build/acceptance-mutation");
      int workers = 1;
      Duration timeout = Duration.ofSeconds(60);
      boolean json = false;
      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "--feature" -> feature = Path.of(value(args, ++i, "--feature"));
          case "--work-dir" -> workDir = Path.of(value(args, ++i, "--work-dir"));
          case "--workers" -> workers = Math.max(1, Integer.parseInt(value(args, ++i, "--workers")));
          case "--timeout" -> timeout = parseDuration(value(args, ++i, "--timeout"));
          case "--json" -> json = true;
          default -> throw new Usage("unknown option: " + args[i]);
        }
      }
      return new Options(feature, workDir, workers, timeout, json);
    }

    private static String value(String[] args, int index, String option) {
      if (index >= args.length) {
        throw new Usage("missing value for " + option);
      }
      return args[index];
    }

    private static Duration parseDuration(String value) {
      if (value.endsWith("s")) {
        return Duration.ofSeconds(Long.parseLong(value.substring(0, value.length() - 1)));
      }
      return Duration.ofSeconds(Long.parseLong(value));
    }
  }

  private static class Usage extends RuntimeException {
    Usage(String message) {
      super(message);
    }
  }
}
