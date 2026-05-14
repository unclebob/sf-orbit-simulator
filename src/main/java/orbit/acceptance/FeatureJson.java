package orbit.acceptance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FeatureJson {
  private static final Map<Character, String> JSON_ESCAPES = jsonEscapes();

  private FeatureJson() {
  }

  public static String write(Feature feature) {
    StringBuilder json = new StringBuilder();
    writeFeature(json, feature, 0);
    json.append('\n');
    return json.toString();
  }

  public static Feature read(String json) {
    Object value = new Parser(json).parse();
    Map<?, ?> object = requireMap(value, "feature");
    String name = requireString(object.get("name"), "name");
    Object backgroundValue = object.containsKey("background") ? object.get("background") : List.of();
    List<Feature.Step> background = readSteps(backgroundValue, "background");
    List<Feature.Scenario> scenarios = readScenarios(object.get("scenarios"));
    return new Feature(name, background, scenarios);
  }

  private static void writeFeature(StringBuilder json, Feature feature, int indent) {
    line(json, indent, "{");
    field(json, indent + 2, "name", feature.name(), true);
    key(json, indent + 2, "background");
    writeSteps(json, feature.background(), indent + 2);
    json.append(",\n");
    key(json, indent + 2, "scenarios");
    writeScenarios(json, feature.scenarios(), indent + 2);
    json.append('\n');
    line(json, indent, "}");
  }

  private static void writeScenarios(StringBuilder json, List<Feature.Scenario> scenarios, int indent) {
    writeObjectArray(json, scenarios, indent, FeatureJson::writeScenario);
  }

  private static void writeScenario(StringBuilder json, Feature.Scenario scenario, int indent) {
    field(json, indent, "name", scenario.name(), true);
    key(json, indent, "steps");
    writeSteps(json, scenario.steps(), indent);
    json.append(",\n");
    key(json, indent, "examples");
    writeExamples(json, scenario.examples(), indent);
    json.append('\n');
  }

  private static void writeSteps(StringBuilder json, List<Feature.Step> steps, int indent) {
    writeObjectArray(json, steps, indent, FeatureJson::writeStep);
  }

  private static void writeStep(StringBuilder json, Feature.Step step, int indent) {
    field(json, indent, "keyword", step.keyword(), true);
    field(json, indent, "text", step.text(), true);
    key(json, indent, "parameters");
    writeStringArray(json, step.parameters(), indent);
    json.append('\n');
  }

  private static void writeExamples(StringBuilder json, List<Map<String, String>> examples, int indent) {
    writeObjectArray(json, examples, indent, FeatureJson::writeExample);
  }

  private static void writeExample(StringBuilder json, Map<String, String> example, int indent) {
    int field = 0;
    for (Map.Entry<String, String> entry : example.entrySet()) {
      field(json, indent, entry.getKey(), entry.getValue(), ++field < example.size());
    }
  }

  private static <T> void writeObjectArray(
      StringBuilder json,
      List<T> values,
      int indent,
      JsonObjectWriter<T> writer
  ) {
    json.append("[\n");
    for (int i = 0; i < values.size(); i++) {
      line(json, indent + 2, "{");
      writer.write(json, values.get(i), indent + 4);
      indent(json, indent + 2).append(i + 1 == values.size() ? "}" : "},").append('\n');
    }
    indent(json, indent).append("]");
  }

  private interface JsonObjectWriter<T> {
    void write(StringBuilder json, T value, int indent);
  }

  private static void writeStringArray(StringBuilder json, List<String> values, int indent) {
    json.append("[");
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        json.append(", ");
      }
      json.append(quote(values.get(i)));
    }
    json.append("]");
  }

  private static List<Feature.Scenario> readScenarios(Object value) {
    List<?> array = requireList(value, "scenarios");
    List<Feature.Scenario> scenarios = new ArrayList<>();
    for (Object item : array) {
      Map<?, ?> object = requireMap(item, "scenario");
      scenarios.add(new Feature.Scenario(
          requireString(object.get("name"), "scenario.name"),
          readSteps(object.get("steps"), "scenario.steps"),
          readExamples(object.get("examples"))
      ));
    }
    return scenarios;
  }

  private static List<Feature.Step> readSteps(Object value, String name) {
    List<?> array = requireList(value, name);
    List<Feature.Step> steps = new ArrayList<>();
    for (Object item : array) {
      Map<?, ?> object = requireMap(item, "step");
      steps.add(new Feature.Step(
          requireString(object.get("keyword"), "step.keyword"),
          requireString(object.get("text"), "step.text"),
          readStringList(object.containsKey("parameters") ? object.get("parameters") : List.of(), "step.parameters")
      ));
    }
    return steps;
  }

  private static List<Map<String, String>> readExamples(Object value) {
    List<?> array = requireList(value, "examples");
    List<Map<String, String>> examples = new ArrayList<>();
    for (Object item : array) {
      Map<?, ?> object = requireMap(item, "example");
      Map<String, String> example = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : object.entrySet()) {
        example.put(requireString(entry.getKey(), "example.key"), requireString(entry.getValue(), "example.value"));
      }
      examples.add(example);
    }
    return examples;
  }

  private static List<String> readStringList(Object value, String name) {
    List<?> array = requireList(value, name);
    List<String> strings = new ArrayList<>();
    for (Object item : array) {
      strings.add(requireString(item, name));
    }
    return strings;
  }

  @SuppressWarnings("unchecked")
  private static Map<?, ?> requireMap(Object value, String name) {
    if (!(value instanceof Map<?, ?> map)) {
      throw new IllegalArgumentException("Expected object for " + name);
    }
    return map;
  }

  private static List<?> requireList(Object value, String name) {
    if (!(value instanceof List<?> list)) {
      throw new IllegalArgumentException("Expected array for " + name);
    }
    return list;
  }

  private static String requireString(Object value, String name) {
    if (!(value instanceof String string)) {
      throw new IllegalArgumentException("Expected string for " + name);
    }
    return string;
  }

  private static void field(StringBuilder json, int indent, String name, String value, boolean comma) {
    indent(json, indent).append(quote(name)).append(": ").append(quote(value));
    json.append(comma ? ",\n" : "\n");
  }

  private static void key(StringBuilder json, int indent, String name) {
    indent(json, indent).append(quote(name)).append(": ");
  }

  private static void line(StringBuilder json, int indent, String text) {
    indent(json, indent).append(text).append('\n');
  }

  private static StringBuilder indent(StringBuilder json, int indent) {
    return json.append(" ".repeat(indent));
  }

  private static String quote(String value) {
    StringBuilder quoted = new StringBuilder("\"");
    for (int i = 0; i < value.length(); i++) {
      appendQuoted(quoted, value.charAt(i));
    }
    return quoted.append('"').toString();
  }

  private static void appendQuoted(StringBuilder quoted, char c) {
    quoted.append(JSON_ESCAPES.getOrDefault(c, Character.toString(c)));
  }

  private static Map<Character, String> jsonEscapes() {
    Map<Character, String> escapes = new HashMap<>();
    escapes.put('\\', "\\\\");
    escapes.put('"', "\\\"");
    escapes.put('\n', "\\n");
    escapes.put('\r', "\\r");
    escapes.put('\t', "\\t");
    return Map.copyOf(escapes);
  }

  private static final class Parser {
    private final String source;
    private int index;

    Parser(String source) {
      this.source = source;
    }

    Object parse() {
      Object value = value();
      whitespace();
      if (index != source.length()) {
        throw new IllegalArgumentException("Unexpected trailing JSON");
      }
      return value;
    }

    private Object value() {
      whitespace();
      if (peek('{')) {
        return object();
      }
      if (peek('[')) {
        return array();
      }
      if (peek('"')) {
        return string();
      }
      throw new IllegalArgumentException("Unsupported JSON value at " + index);
    }

    private Map<String, Object> object() {
      expect('{');
      Map<String, Object> object = new LinkedHashMap<>();
      whitespace();
      if (take('}')) {
        return object;
      }
      do {
        String key = string();
        whitespace();
        expect(':');
        object.put(key, value());
        whitespace();
      } while (take(','));
      expect('}');
      return object;
    }

    private List<Object> array() {
      expect('[');
      List<Object> array = new ArrayList<>();
      whitespace();
      if (take(']')) {
        return array;
      }
      do {
        array.add(value());
        whitespace();
      } while (take(','));
      expect(']');
      return array;
    }

    private String string() {
      expect('"');
      StringBuilder string = new StringBuilder();
      while (index < source.length()) {
        char c = source.charAt(index++);
        if (c == '"') {
          return string.toString();
        }
        if (c == '\\') {
          char escaped = source.charAt(index++);
          switch (escaped) {
            case '"' -> string.append('"');
            case '\\' -> string.append('\\');
            case 'n' -> string.append('\n');
            case 'r' -> string.append('\r');
            case 't' -> string.append('\t');
            default -> throw new IllegalArgumentException("Unsupported escape: " + escaped);
          }
        } else {
          string.append(c);
        }
      }
      throw new IllegalArgumentException("Unterminated string");
    }

    private void whitespace() {
      while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
        index++;
      }
    }

    private boolean peek(char expected) {
      return index < source.length() && source.charAt(index) == expected;
    }

    private boolean take(char expected) {
      if (peek(expected)) {
        index++;
        return true;
      }
      return false;
    }

    private void expect(char expected) {
      whitespace();
      if (!take(expected)) {
        throw new IllegalArgumentException("Expected '" + expected + "' at " + index);
      }
    }
  }
}
