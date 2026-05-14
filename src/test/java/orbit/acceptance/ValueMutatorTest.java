package orbit.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class ValueMutatorTest {
  @Test
  void mutatesBooleansAndNumbersAndStrings() {
    ValueMutator mutator = new ValueMutator();

    assertEquals("false", mutator.mutate("$.x", "true"));
    assertNotEquals("20", mutator.mutate("$.x", "20"));
    assertNotEquals("3.14", mutator.mutate("$.x", "3.14"));
    assertNotEquals("accepted", mutator.mutate("$.x", "accepted"));
  }

  @Test
  void mutationIsDeterministicForPathAndValue() {
    ValueMutator mutator = new ValueMutator();

    assertEquals(mutator.mutate("$.path", "accepted"), mutator.mutate("$.path", "accepted"));
  }
}
