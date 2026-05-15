package orbit.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import orbit.Vector2;
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

  @Test
  void zoomOutSliderIsConfiguredBesideTheSpeedControls() throws Exception {
    assertEquals(1, staticInt("MINIMUM_ZOOM_OUT"));
    assertEquals(5, staticInt("MAXIMUM_ZOOM_OUT"));
    assertNotNull(staticField("ZOOM_SLIDER"));
  }

  @Test
  void zoomOutMultiplierScalesTheViewAndMouseCoordinates() throws Exception {
    OrbitSketch sketch = new OrbitSketch();
    sketch.width = 800;
    sketch.height = 600;
    sketch.mouseX = 500;
    sketch.mouseY = 250;
    setInstanceField(sketch, "zoomOutMultiplier", 2);
    setInstanceField(sketch, "viewCenter", new Vector2(10, -20));

    assertEquals(0.5f, (float) instanceMethod(sketch, "viewScale").invoke(sketch));
    Vector2 mouse = (Vector2) instanceMethod(sketch, "worldMouse").invoke(sketch);
    assertEquals(210, mouse.x(), 0.000001);
    assertEquals(-120, mouse.y(), 0.000001);
  }

  private static List<?> buttons() throws Exception {
    List<?> buttons = (List<?>) staticField("BUTTONS");
    assertNotNull(buttons);
    return buttons;
  }

  private static int staticInt(String name) throws Exception {
    return (int) staticField(name);
  }

  private static Object staticField(String name) throws Exception {
    Field field = OrbitSketch.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(null);
  }

  private static void setInstanceField(OrbitSketch sketch, String name, Object value) throws Exception {
    Field field = OrbitSketch.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(sketch, value);
  }

  private static Method instanceMethod(OrbitSketch sketch, String name) throws Exception {
    Method method = sketch.getClass().getDeclaredMethod(name);
    method.setAccessible(true);
    return method;
  }

  private static void assertButtonAction(Object button, String actionName) throws Exception {
    assertNotNull(button);
    Method action = button.getClass().getDeclaredMethod("action");
    action.setAccessible(true);
    assertEquals(actionName, action.invoke(button).toString());
  }
}
