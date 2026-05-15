package orbit.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import orbit.OrbitSimulator;
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

    assertEquals(0.5f, (float) instanceMethod("viewScale").invoke(sketch));
    Vector2 mouse = (Vector2) instanceMethod("worldMouse").invoke(sketch);
    assertEquals(210, mouse.x(), 0.000001);
    assertEquals(-120, mouse.y(), 0.000001);
  }

  @Test
  void zoomSliderPressStartsZoomDragAndUpdatesTheMultiplier() throws Exception {
    OrbitSketch sketch = new OrbitSketch();
    Object slider = staticField("ZOOM_SLIDER");
    sketch.mouseX = (int) invoke(slider, "endX");
    sketch.mouseY = (int) invoke(slider, "y");

    sketch.mousePressed();

    assertEquals("ZOOM", instanceField(sketch, "dragAction").toString());
    assertEquals(5, instanceField(sketch, "zoomOutMultiplier"));
  }

  @Test
  void speedSliderTrackClickChangesSpeedWithoutAddingABody() throws Exception {
    OrbitSketch sketch = new OrbitSketch();
    OrbitSimulator simulator = OrbitSimulator.defaults();
    setInstanceField(sketch, "simulator", simulator);
    Object slider = staticField("SPEED_SLIDER");
    sketch.mouseX = (int) (float) invoke(slider, "positionFor", 75, OrbitSimulator.MINIMUM_SPEED, OrbitSimulator.MAXIMUM_SPEED);
    sketch.mouseY = (int) invoke(slider, "y");

    sketch.mousePressed();

    assertEquals("SPEED", instanceField(sketch, "dragAction").toString());
    assertEquals(75, simulator.speedMultiplier());
    assertEquals(3, simulator.bodyCount());
  }

  @Test
  void speedSliderGutterClickIsIgnored() throws Exception {
    OrbitSketch sketch = new OrbitSketch();
    OrbitSimulator simulator = OrbitSimulator.defaults();
    simulator.setSpeedMultiplier(12);
    setInstanceField(sketch, "simulator", simulator);
    Object slider = staticField("SPEED_SLIDER");
    sketch.mouseX = (int) invoke(slider, "x") - 20;
    sketch.mouseY = (int) invoke(slider, "y");

    sketch.mousePressed();

    assertEquals("NONE", instanceField(sketch, "dragAction").toString());
    assertEquals(12, simulator.speedMultiplier());
    assertEquals(3, simulator.bodyCount());
  }

  @Test
  void speedSliderGutterEdgeClickIsIgnored() throws Exception {
    OrbitSketch sketch = new OrbitSketch();
    OrbitSimulator simulator = OrbitSimulator.defaults();
    simulator.setSpeedMultiplier(12);
    setInstanceField(sketch, "simulator", simulator);
    Object slider = staticField("SPEED_SLIDER");
    sketch.mouseX = (int) invoke(slider, "x") - 20;
    sketch.mouseY = (int) invoke(slider, "y") + (int) invoke(slider, "handleRadius") * 2;

    sketch.mousePressed();

    assertEquals("NONE", instanceField(sketch, "dragAction").toString());
    assertEquals(12, simulator.speedMultiplier());
    assertEquals(3, simulator.bodyCount());
  }

  @Test
  void zoomSliderTrackClickChangesZoomWithoutAddingABody() throws Exception {
    OrbitSketch sketch = new OrbitSketch();
    OrbitSimulator simulator = OrbitSimulator.defaults();
    setInstanceField(sketch, "simulator", simulator);
    Object slider = staticField("ZOOM_SLIDER");
    sketch.mouseX = (int) (float) invoke(slider, "positionFor", 4, staticInt("MINIMUM_ZOOM_OUT"), staticInt("MAXIMUM_ZOOM_OUT"));
    sketch.mouseY = (int) invoke(slider, "y");

    sketch.mousePressed();

    assertEquals("ZOOM", instanceField(sketch, "dragAction").toString());
    assertEquals(4, instanceField(sketch, "zoomOutMultiplier"));
    assertEquals(3, simulator.bodyCount());
  }

  @Test
  void zoomSliderGutterClickIsIgnored() throws Exception {
    OrbitSketch sketch = new OrbitSketch();
    OrbitSimulator simulator = OrbitSimulator.defaults();
    setInstanceField(sketch, "simulator", simulator);
    setInstanceField(sketch, "zoomOutMultiplier", 4);
    Object slider = staticField("ZOOM_SLIDER");
    sketch.mouseX = (int) invoke(slider, "x") - 20;
    sketch.mouseY = (int) invoke(slider, "y");

    sketch.mousePressed();

    assertEquals("NONE", instanceField(sketch, "dragAction").toString());
    assertEquals(4, instanceField(sketch, "zoomOutMultiplier"));
    assertEquals(3, simulator.bodyCount());
  }

  @Test
  void viewTransformCentersThenScalesThenOffsetsTheWorld() throws Exception {
    RecordingSketch sketch = new RecordingSketch();
    sketch.width = 800;
    sketch.height = 600;
    setInstanceField(sketch, "zoomOutMultiplier", 2);
    setInstanceField(sketch, "viewCenter", new Vector2(10, -20));

    instanceMethod("applyViewTransform").invoke(sketch);

    assertEquals(List.of("translate 400.0 300.0", "scale 0.5", "translate -10.0 20.0"), sketch.operations);
  }

  @Test
  void scrollAdjustsTheViewCenterByZoomedWorldDistance() throws Exception {
    OrbitSketch sketch = new OrbitSketch();
    setInstanceField(sketch, "zoomOutMultiplier", 2);
    setInstanceField(sketch, "viewCenter", new Vector2(0, 0));

    instanceMethod("adjustViewCenter", Vector2.class, double.class).invoke(sketch, new Vector2(-30, 20), 1.0);

    Vector2 viewCenter = (Vector2) instanceField(sketch, "viewCenter");
    assertEquals(-60, viewCenter.x(), 0.000001);
    assertEquals(40, viewCenter.y(), 0.000001);
  }

  @Test
  void restartButtonRestartsCentersTheViewAndResetsZoom() throws Exception {
    OrbitSketch sketch = new OrbitSketch();
    OrbitSimulator simulator = OrbitSimulator.defaults();
    simulator.tick(1, 1, 0.016667);
    setInstanceField(sketch, "simulator", simulator);
    setInstanceField(sketch, "viewCenter", new Vector2(120, -80));
    setInstanceField(sketch, "zoomOutMultiplier", 4);

    pressButton(buttons().get(1), sketch);

    Vector2 viewCenter = (Vector2) instanceField(sketch, "viewCenter");
    assertEquals(0, viewCenter.x(), 0.000001);
    assertEquals(0, viewCenter.y(), 0.000001);
    assertEquals(1, instanceField(sketch, "zoomOutMultiplier"));
    assertEquals(0, simulator.findBody("sun").orElseThrow().position().x(), 0.000001);
    assertEquals(0, simulator.findBody("sun").orElseThrow().position().y(), 0.000001);
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
    return field(name).get(null);
  }

  private static void setInstanceField(OrbitSketch sketch, String name, Object value) throws Exception {
    field(name).set(sketch, value);
  }

  private static Object instanceField(OrbitSketch sketch, String name) throws Exception {
    return field(name).get(sketch);
  }

  private static Field field(String name) throws Exception {
    Field field = OrbitSketch.class.getDeclaredField(name);
    return accessible(field);
  }

  private static Method instanceMethod(String name) throws Exception {
    return instanceMethod(name, new Class<?>[0]);
  }

  private static Method instanceMethod(String name, Class<?>... parameterTypes) throws Exception {
    Method method = OrbitSketch.class.getDeclaredMethod(name, parameterTypes);
    return accessible(method);
  }

  private static Object invoke(Object target, String methodName) throws Exception {
    Method method = target.getClass().getDeclaredMethod(methodName);
    return accessible(method).invoke(target);
  }

  private static Object invoke(Object target, String methodName, Object... arguments) throws Exception {
    Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes(arguments));
    return accessible(method).invoke(target, arguments);
  }

  private static Class<?>[] parameterTypes(Object[] arguments) {
    Class<?>[] types = new Class<?>[arguments.length];
    for (int i = 0; i < arguments.length; i++) {
      types[i] = primitiveType(arguments[i]);
    }
    return types;
  }

  private static Class<?> primitiveType(Object argument) {
    return argument instanceof Integer ? int.class : argument.getClass();
  }

  private static <T extends AccessibleObject> T accessible(T object) {
    object.setAccessible(true);
    return object;
  }

  private static void pressButton(Object button, OrbitSketch sketch) throws Exception {
    Method press = button.getClass().getDeclaredMethod("press", OrbitSketch.class);
    accessible(press).invoke(button, sketch);
  }

  private static void assertButtonAction(Object button, String actionName) throws Exception {
    assertNotNull(button);
    Method action = button.getClass().getDeclaredMethod("action");
    assertEquals(actionName, accessible(action).invoke(button).toString());
  }

  private static class RecordingSketch extends OrbitSketch {
    private final List<String> operations = new ArrayList<>();

    @Override
    public void translate(float x, float y) {
      operations.add("translate " + x + " " + y);
    }

    @Override
    public void scale(float size) {
      operations.add("scale " + size);
    }
  }
}
