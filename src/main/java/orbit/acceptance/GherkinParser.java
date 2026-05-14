package orbit.acceptance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GherkinParser {
  private static final Pattern STEP = Pattern.compile("^(Given|When|Then|And)\\s+(.+)$");
  private static final Pattern PARAMETER = Pattern.compile("<([A-Za-z0-9_]+)>");

  public static void main(String[] args) {
    System.exit(exitCode(args));
  }

  static int exitCode(String[] args) {
    if (args.length != 2) {
      System.err.println("usage: gherkin-parser <feature-file> <json-output>");
      return 2;
    }
    try {
      Feature feature = new GherkinParser().parse(Files.readAllLines(Path.of(args[0])));
      Path output = Path.of(args[1]);
      Files.createDirectories(output.getParent());
      Files.writeString(output, FeatureJson.write(feature));
      return 0;
    } catch (Exception error) {
      System.err.println(error.getMessage());
      return 1;
    }
  }

  public Feature parse(List<String> lines) {
    ParseState state = new ParseState();
    for (String rawLine : lines) {
      String line = rawLine.trim();
      if (isContent(line)) {
        parseLine(state, line);
      }
    }
    if (state.current != null) {
      state.scenarios.add(state.current.toScenario());
    }
    if (state.featureName == null || state.featureName.isBlank()) {
      throw new IllegalArgumentException("Missing feature declaration");
    }
    return new Feature(state.featureName, List.copyOf(state.background), List.copyOf(state.scenarios));
  }

  private static boolean isContent(String line) {
    return !line.isEmpty() && !line.startsWith("#");
  }

  private static void parseLine(ParseState state, String line) {
    if (parseFeatureOrBackground(state, line)) {
      return;
    }
    if (isScenario(line)) {
      state.startScenario(line);
    } else if (line.equals("Examples:")) {
      state.startExamples();
    } else {
      parseStepOrExample(state, line);
    }
  }

  private static void parseStepOrExample(ParseState state, String line) {
    Matcher step = STEP.matcher(line);
    if (step.matches()) {
      state.addStep(step);
    } else if (line.startsWith("|")) {
      state.addExampleRow(cells(line));
    }
  }

  private static boolean parseFeatureOrBackground(ParseState state, String line) {
    if (line.startsWith("Feature:")) {
      state.featureName = line.substring("Feature:".length()).trim();
      return true;
    }
    if (line.equals("Background:")) {
      state.startBackground();
      return true;
    }
    return false;
  }

  private static boolean isScenario(String line) {
    return line.startsWith("Scenario:") || line.startsWith("Scenario Outline:");
  }

  private static List<String> parameters(String text) {
    List<String> parameters = new ArrayList<>();
    Matcher matcher = PARAMETER.matcher(text);
    while (matcher.find()) {
      parameters.add(matcher.group(1));
    }
    return parameters;
  }

  private static List<String> cells(String line) {
    String row = line;
    if (row.startsWith("|")) {
      row = row.substring(1);
    }
    if (row.endsWith("|")) {
      row = row.substring(0, row.length() - 1);
    }
    String[] parts = row.split("\\|", -1);
    List<String> cells = new ArrayList<>();
    for (String part : parts) {
      cells.add(part.trim());
    }
    return cells;
  }

  private enum Section {
    NONE, BACKGROUND, SCENARIO, EXAMPLES
  }

  private static final class ParseState {
    private String featureName;
    private final List<Feature.Step> background = new ArrayList<>();
    private final List<Feature.Scenario> scenarios = new ArrayList<>();
    private MutableScenario current;
    private Section section = Section.NONE;
    private List<String> headers;

    void startBackground() {
      section = Section.BACKGROUND;
      current = null;
      headers = null;
    }

    void startScenario(String line) {
      if (current != null) {
        scenarios.add(current.toScenario());
      }
      String prefix = line.startsWith("Scenario Outline:") ? "Scenario Outline:" : "Scenario:";
      current = new MutableScenario(line.substring(prefix.length()).trim());
      section = Section.SCENARIO;
      headers = null;
    }

    void startExamples() {
      if (current == null) {
        throw new IllegalArgumentException("Examples outside a scenario");
      }
      section = Section.EXAMPLES;
      headers = null;
    }

    void addStep(Matcher step) {
      Feature.Step parsedStep = new Feature.Step(step.group(1), step.group(2).trim(), parameters(step.group(2)));
      if (section == Section.BACKGROUND) {
        background.add(parsedStep);
      } else if (current != null && section == Section.SCENARIO) {
        current.steps.add(parsedStep);
      } else {
        throw new IllegalArgumentException("Step outside a background or scenario");
      }
    }

    void addExampleRow(List<String> cells) {
      if (section != Section.EXAMPLES || current == null) {
        throw new IllegalArgumentException("Example row outside examples");
      }
      if (headers == null) {
        headers = cells;
        return;
      }
      current.examples.add(exampleFrom(cells));
    }

    private Map<String, String> exampleFrom(List<String> cells) {
      if (cells.size() != headers.size()) {
        throw new IllegalArgumentException("Example row cell count differs from header");
      }
      Map<String, String> example = new LinkedHashMap<>();
      for (int i = 0; i < headers.size(); i++) {
        example.put(headers.get(i), cells.get(i));
      }
      return example;
    }
  }

  private static final class MutableScenario {
    private final String name;
    private final List<Feature.Step> steps = new ArrayList<>();
    private final List<Map<String, String>> examples = new ArrayList<>();

    MutableScenario(String name) {
      this.name = name;
    }

    Feature.Scenario toScenario() {
      return new Feature.Scenario(name, List.copyOf(steps), List.copyOf(examples));
    }
  }
}
