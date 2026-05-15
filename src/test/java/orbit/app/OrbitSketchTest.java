package orbit.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrbitSketchTest {
  @Test
  void controlButtonsIncludePauseRestartAndCenterSunActions() throws Exception {
    List<?> buttons = buttons();

    assertEquals(3, buttons.size());
    assertButtonAction(buttons.get(0), "PAUSE");
    assertButtonAction(buttons.get(1), "RESTART");
    assertButtonAction(buttons.get(2), "CENTER_SUN");
  }

  private static List<?> buttons() throws Exception {
    Field field = OrbitSketch.class.getDeclaredField("BUTTONS");
    field.setAccessible(true);
    List<?> buttons = (List<?>) field.get(null);
    assertNotNull(buttons);
    return buttons;
  }

  private static void assertButtonAction(Object button, String actionName) throws Exception {
    assertNotNull(button);
    Method action = button.getClass().getDeclaredMethod("action");
    action.setAccessible(true);
    assertEquals(actionName, action.invoke(button).toString());
  }
}
