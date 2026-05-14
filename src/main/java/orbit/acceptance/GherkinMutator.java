package orbit.acceptance;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.ToolProvider;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

public class GherkinMutator {
  private static final Set<String> EQUIVALENT_GRAVITY_KEYS = Set.of(
      "first_body",
      "second_body",
      "first_vx",
      "first_vy",
      "second_vx",
      "second_vy"
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
    List<Result> results = new ArrayList<>();
    for (Mutation mutation : mutations) {
      results.add(runMutation(options, base, mutation));
    }
    return results;
  }

  private Result runMutation(Options options, Feature base, Mutation mutation) {
    long started = System.nanoTime();
    MutationWorkspace workspace = MutationWorkspace.create(options.workDir, mutation);
    try {
      workspace.create();
      Files.writeString(workspace.ir, FeatureJson.write(apply(base, mutation)));
      new AcceptanceGenerator().generate(workspace.ir, workspace.generated);
      RunnerResult runner = runGeneratedTest(workspace.generated, workspace.classes);
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
    return scenario.name().equals("Gravity is applied between every pair of bodies")
        && EQUIVALENT_GRAVITY_KEYS.contains(key);
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

  private RunnerResult runGeneratedTest(Path generatedSource, Path classes) throws Exception {
    RunnerResult compilationFailure = compileGeneratedTest(generatedSource, classes);
    if (compilationFailure != null) {
      return compilationFailure;
    }

    URLClassLoader classLoader = new GeneratedTestClassLoader(
        new URL[] {classes.toUri().toURL()},
        Thread.currentThread().getContextClassLoader()
    );
    try {
      return runTestClass(classLoader);
    } finally {
      classLoader.close();
    }
  }

  private RunnerResult compileGeneratedTest(Path generatedSource, Path classes) {
    int compileExit = ToolProvider.getSystemJavaCompiler().run(
        null,
        null,
        null,
        "-cp", System.getProperty("java.class.path"),
        "-d", classes.toString(),
        generatedSource.toString()
    );
    if (compileExit != 0) {
      return new RunnerResult(2, "generated test compilation failed");
    }
    return null;
  }

  private RunnerResult runTestClass(URLClassLoader classLoader) throws ClassNotFoundException {
    Class<?> testClass = Class.forName("orbit.acceptance.generated.OrbitSimulatorAcceptanceTest", true, classLoader);
    SummaryGeneratingListener listener = new SummaryGeneratingListener();
    LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(DiscoverySelectors.selectClass(testClass))
        .build();
    Thread current = Thread.currentThread();
    ClassLoader previous = current.getContextClassLoader();
    current.setContextClassLoader(classLoader);
    try {
      LauncherFactory.create().execute(request, listener);
    } finally {
      current.setContextClassLoader(previous);
    }

    TestExecutionSummary summary = listener.getSummary();
    String output = summaryOutput(summary);
    return new RunnerResult(summary.getFailures().isEmpty() ? 0 : 1, output);
  }

  private String summaryOutput(TestExecutionSummary summary) {
    StringWriter text = new StringWriter();
    PrintWriter writer = new PrintWriter(text);
    writer.printf(
        "tests=%d succeeded=%d failed=%d%n",
        summary.getTestsFoundCount(),
        summary.getTestsSucceededCount(),
        summary.getTestsFailedCount()
    );
    for (TestExecutionSummary.Failure failure : summary.getFailures()) {
      writer.println(failure.getTestIdentifier().getDisplayName());
      failure.getException().printStackTrace(writer);
    }
    writer.flush();
    return text.toString();
  }

  private static final class GeneratedTestClassLoader extends URLClassLoader {
    private static final String GENERATED_TEST = "orbit.acceptance.generated.OrbitSimulatorAcceptanceTest";

    GeneratedTestClassLoader(URL[] urls, ClassLoader parent) {
      super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (GENERATED_TEST.equals(name)) {
        Class<?> loaded = findLoadedClass(name);
        if (loaded == null) {
          loaded = findClass(name);
        }
        if (resolve) {
          resolveClass(loaded);
        }
        return loaded;
      }
      return super.loadClass(name, resolve);
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
    json.append("{\"Mutation\":{\"ID\":\"").append(result.mutation.id())
        .append("\",\"Path\":\"").append(result.mutation.path())
        .append("\",\"Description\":\"").append(result.mutation.description())
        .append("\",\"Original\":\"").append(result.mutation.original())
        .append("\",\"Mutated\":\"").append(result.mutation.mutated())
        .append("\"},\"Status\":\"").append(result.status)
        .append("\",\"Output\":\"").append(escape(result.output))
        .append("\",\"Error\":\"").append(escape(result.error))
        .append("\",\"Duration\":").append(result.duration)
        .append('}');
  }

  private static String escape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
  }

  public record Result(Mutation mutation, String status, String output, String error, long duration) {
  }

  private record RunnerResult(int exitCode, String output) {
  }

  private record MutationWorkspace(Path ir, Path generated, Path classes) {
    static MutationWorkspace create(Path workDir, Mutation mutation) {
      Path mutationDir = workDir.resolve(mutation.id());
      return new MutationWorkspace(
          mutationDir.resolve("feature.json"),
          mutationDir.resolve("generated/orbit/acceptance/generated/OrbitSimulatorAcceptanceTest.java"),
          mutationDir.resolve("classes")
      );
    }

    void create() throws IOException {
      Files.createDirectories(generated.getParent());
      Files.createDirectories(classes);
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
