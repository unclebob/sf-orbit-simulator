package orbit.acceptance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

public class ValueMutator {
  private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");
  private static final Pattern FLOAT = Pattern.compile("[+-]?(?:\\d+\\.\\d+|\\d+\\.|\\.\\d+)(?:[eE][+-]?\\d+)?|[+-]?\\d+[eE][+-]?\\d+");
  private static final Pattern DURATION = Pattern.compile("\\d+(?:\\.\\d+)?(?:ms|s|m|h|d)");

  public String mutate(String path, String value) {
    String trimmed = value.trim();
    Random random = new Random((path + "\n" + value).hashCode());
    return mutateValue(path, value, trimmed, random);
  }

  private String mutateValue(String path, String original, String trimmed, Random random) {
    if (trimmed.contains(",")) {
      return mutateList(path, trimmed, random);
    }
    if (INTEGER.matcher(trimmed).matches()) {
      return mutateInteger(trimmed, random);
    }
    if (FLOAT.matcher(trimmed).matches()) {
      return mutateFloat(trimmed, random);
    }
    String temporal = mutateTemporal(trimmed, random);
    if (temporal != null) {
      return temporal;
    }
    String literal = mutateLiteral(trimmed, random);
    if (literal != null) {
      return literal;
    }
    return dither(original, random);
  }

  private String mutateLiteral(String trimmed, Random random) {
    String lower = trimmed.toLowerCase(Locale.ROOT);
    if (lower.equals("true") || lower.equals("false")) {
      return Boolean.toString(!Boolean.parseBoolean(lower));
    }
    if (lower.equals("null") || lower.equals("nil") || lower.equals("none")) {
      return "value" + (random.nextInt(9) + 1);
    }
    return null;
  }

  private String mutateTemporal(String trimmed, Random random) {
    String dateTime = mutateDateTime(trimmed, random);
    if (dateTime != null) {
      return dateTime;
    }
    if (DURATION.matcher(trimmed).matches()) {
      return mutateDuration(trimmed, random);
    }
    return null;
  }

  private String mutateInteger(String trimmed, Random random) {
    long value = Long.parseLong(trimmed);
    long delta = random.nextInt(9) + 1L;
    return Long.toString(value + signed(delta, random));
  }

  private String mutateFloat(String trimmed, Random random) {
    BigDecimal value = new BigDecimal(trimmed);
    BigDecimal delta = BigDecimal.valueOf((random.nextInt(9) + 1) / 10.0);
    return value.add(signed(delta, random)).stripTrailingZeros().toPlainString();
  }

  private long signed(long delta, Random random) {
    return random.nextBoolean() ? delta : -delta;
  }

  private BigDecimal signed(BigDecimal delta, Random random) {
    return random.nextBoolean() ? delta : delta.negate();
  }

  private String mutateList(String path, String trimmed, Random random) {
    String[] parts = trimmed.split(",");
    List<String> values = new ArrayList<>();
    for (String part : parts) {
      values.add(part.trim());
    }
    int selected = random.nextInt(values.size());
    values.set(selected, mutateValue(path + "[" + selected + "]", values.get(selected), values.get(selected).trim(), random));
    return String.join(", ", values);
  }

  private String mutateDateTime(String trimmed, Random random) {
    int amount = random.nextBoolean() ? 1 : -1;
    try {
      return LocalDateTime.parse(trimmed).plusSeconds(amount).toString();
    } catch (Exception ignored) {
    }
    try {
      return LocalDate.parse(trimmed).plusDays(amount).toString();
    } catch (Exception ignored) {
    }
    try {
      return LocalTime.parse(trimmed).plusSeconds(amount).toString();
    } catch (Exception ignored) {
    }
    return null;
  }

  private String mutateDuration(String trimmed, Random random) {
    int split = 0;
    while (split < trimmed.length() && (Character.isDigit(trimmed.charAt(split)) || trimmed.charAt(split) == '.')) {
      split++;
    }
    BigDecimal number = new BigDecimal(trimmed.substring(0, split));
    BigDecimal delta = BigDecimal.valueOf(random.nextInt(9) + 1);
    return number.add(delta).stripTrailingZeros().toPlainString() + trimmed.substring(split);
  }

  private String dither(String value, Random random) {
    if (value.isEmpty()) {
      return "x";
    }
    int index = random.nextInt(value.length());
    char replacement = value.charAt(index) == 'x' ? 'y' : 'x';
    return value.substring(0, index) + replacement + value.substring(index + 1);
  }
}
