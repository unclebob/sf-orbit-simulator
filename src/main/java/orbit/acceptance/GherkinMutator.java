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
import javax.tools.ToolProvider;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

public class GherkinMutator {
  private final ValueMutator valueMutator = new ValueMutator();

  public static void main(String[] args) {
    try {
      Options options = Options.parse(args);
      List<Result> results = new GherkinMutator().run(options);
      if (options.json) {
        System.out.print(jsonReport(results));
      } else {
        System.out.print(textReport(results));
      }
      boolean failed = results.stream().anyMatch(result -> !result.status.equals("killed"));
      System.exit(failed ? 1 : 0);
    } catch (Usage error) {
      System.err.println(error.getMessage());
      System.exit(2);
    } catch (Exception error) {
      System.err.println(error.getMessage());
      System.exit(1);
    }
  }

  public List<Result> run(Options options) throws IOException {
    Feature base = new GherkinParser().parse(Files.readAllLines(options.feature));
    List<Mutation> mutations = mutations(base);
    List<Result> results = new ArrayList<>();
    Files.createDirectories(options.workDir);
    for (Mutation mutation : mutations) {
      long started = System.nanoTime();
      Path mutationDir = options.workDir.resolve(mutation.id());
      Path ir = mutationDir.resolve("feature.json");
      Path generated = mutationDir.resolve("generated/orbit/acceptance/generated/OrbitSimulatorAcceptanceTest.java");
      Path classes = mutationDir.resolve("classes");
      try {
        Files.createDirectories(generated.getParent());
        Files.createDirectories(classes);
        Feature mutated = apply(base, mutation);
        Files.writeString(ir, FeatureJson.write(mutated));
        new AcceptanceGenerator().generate(ir, generated);
        RunnerResult runner = runGeneratedTest(generated, classes);
        String status = runner.exitCode == 0 ? "survived" : "killed";
        results.add(new Result(mutation, status, runner.output, "", System.nanoTime() - started));
      } catch (Exception error) {
        results.add(new Result(mutation, "error", "", error.getMessage(), System.nanoTime() - started));
      }
    }
    return results;
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
    if (!scenario.name().equals("Gravity is applied between every pair of bodies")) {
      return false;
    }
    return key.equals("first_body")
        || key.equals("second_body")
        || key.equals("first_vx")
        || key.equals("first_vy")
        || key.equals("second_vx")
        || key.equals("second_vy");
  }

  private Feature apply(Feature feature, Mutation mutation) {
    List<Feature.Scenario> scenarios = new ArrayList<>();
    for (int s = 0; s < feature.scenarios().size(); s++) {
      Feature.Scenario scenario = feature.scenarios().get(s);
      List<Map<String, String>> examples = new ArrayList<>();
      for (int e = 0; e < scenario.examples().size(); e++) {
        Map<String, String> example = new LinkedHashMap<>(scenario.examples().get(e));
        String prefix = "$.scenarios[" + s + "].examples[" + e + "].";
        if (mutation.path().startsWith(prefix)) {
          example.put(mutation.path().substring(prefix.length()), mutation.mutated());
        }
        examples.add(example);
      }
      scenarios.add(new Feature.Scenario(scenario.name(), scenario.steps(), examples));
    }
    return new Feature(feature.name(), feature.background(), scenarios);
  }

  private RunnerResult runGeneratedTest(Path generatedSource, Path classes) throws Exception {
    String classpath = System.getProperty("java.class.path");
    int compileExit = ToolProvider.getSystemJavaCompiler().run(
        null,
        null,
        null,
        "-cp", classpath,
        "-d", classes.toString(),
        generatedSource.toString()
    );
    if (compileExit != 0) {
      return new RunnerResult(2, "generated test compilation failed");
    }

    URLClassLoader classLoader = new GeneratedTestClassLoader(
        new URL[] {classes.toUri().toURL()},
        Thread.currentThread().getContextClassLoader()
    );
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
      classLoader.close();
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

  private static String textReport(List<Result> results) {
    long killed = results.stream().filter(result -> result.status.equals("killed")).count();
    long survived = results.stream().filter(result -> result.status.equals("survived")).count();
    long errors = results.stream().filter(result -> result.status.equals("error")).count();
    StringBuilder report = new StringBuilder();
    report.append("total=").append(results.size())
        .append(" killed=").append(killed)
        .append(" survived=").append(survived)
        .append(" errors=").append(errors)
        .append('\n');
    for (Result result : results) {
      report.append(String.format("%-8s %s%n", result.status, result.mutation.description()));
      if (!result.status.equals("killed")) {
        if (!result.error.isBlank()) {
          report.append("  error: ").append(result.error).append('\n');
        }
        if (!result.output.isBlank()) {
          report.append("  output:\n").append(result.output).append('\n');
        }
      }
    }
    return report.toString();
  }

  private static String jsonReport(List<Result> results) {
    long killed = results.stream().filter(result -> result.status.equals("killed")).count();
    long survived = results.stream().filter(result -> result.status.equals("survived")).count();
    long errors = results.stream().filter(result -> result.status.equals("error")).count();
    StringBuilder json = new StringBuilder();
    json.append("{\"summary\":{\"Total\":").append(results.size())
        .append(",\"Killed\":").append(killed)
        .append(",\"Survived\":").append(survived)
        .append(",\"Errors\":").append(errors)
        .append("},\"results\":[");
    for (int i = 0; i < results.size(); i++) {
      Result result = results.get(i);
      if (i > 0) {
        json.append(',');
      }
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
    return json.append("]}\n").toString();
  }

  private static String escape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
  }

  public record Result(Mutation mutation, String status, String output, String error, long duration) {
  }

  private record RunnerResult(int exitCode, String output) {
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
