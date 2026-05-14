package orbit.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ValueMutatorTest {
  @Test
  void mutatesBooleansAndNumbersAndStrings() {
    assertMutated(Map.of(
        "$.null", "null",
        "$.integer", "20",
        "$.float", "3.14",
        "$.string", "accepted"
    ));
    assertEquals("false", new ValueMutator().mutate("$.boolean", "true"));
  }

  @Test
  void mutationIsDeterministicForPathAndValue() {
    ValueMutator mutator = new ValueMutator();

    assertEquals(mutator.mutate("$.path", "accepted"), mutator.mutate("$.path", "accepted"));
  }

  @Test
  void mutatesCompoundAndTemporalValues() {
    assertMutated(Map.of(
        "$.list", "1, 2, 3",
        "$.time", "2026-05-14T12:00:00",
        "$.date", "2026-05-14",
        "$.clock", "12:00:00",
        "$.duration", "5s"
    ));
  }

  private static void assertMutated(Map<String, String> examples) {
    ValueMutator mutator = new ValueMutator();
    for (Map.Entry<String, String> example : examples.entrySet()) {
      assertNotEquals(example.getValue(), mutator.mutate(example.getKey(), example.getValue()));
    }
  }
}
