package orbit.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class ValueMutatorTest {
  @Test
  void mutatesBooleansAndNumbersAndStrings() {
    ValueMutator mutator = new ValueMutator();

    assertEquals("false", mutator.mutate("$.x", "true"));
    assertNotEquals("null", mutator.mutate("$.x", "null"));
    assertNotEquals("20", mutator.mutate("$.x", "20"));
    assertNotEquals("3.14", mutator.mutate("$.x", "3.14"));
    assertNotEquals("accepted", mutator.mutate("$.x", "accepted"));
  }

  @Test
  void mutationIsDeterministicForPathAndValue() {
    ValueMutator mutator = new ValueMutator();

    assertEquals(mutator.mutate("$.path", "accepted"), mutator.mutate("$.path", "accepted"));
  }

  @Test
  void mutatesCompoundAndTemporalValues() {
    ValueMutator mutator = new ValueMutator();

    assertNotEquals("1, 2, 3", mutator.mutate("$.list", "1, 2, 3"));
    assertNotEquals("2026-05-14T12:00:00", mutator.mutate("$.time", "2026-05-14T12:00:00"));
    assertNotEquals("2026-05-14", mutator.mutate("$.date", "2026-05-14"));
    assertNotEquals("12:00:00", mutator.mutate("$.clock", "12:00:00"));
    assertNotEquals("5s", mutator.mutate("$.duration", "5s"));
  }
}
