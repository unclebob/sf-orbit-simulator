package orbit.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import orbit.Body;
import orbit.OrbitSimulator;
import orbit.Physics;
import orbit.Vector2;

public class OrbitStepHandlers implements StepHandlers {
  private static final double TOLERANCE = 0.0001;

  @Override
  public void handle(String stepText, World world, Map<String, String> example) {
    switch (stepText) {
      case "the orbit simulator is opened" -> world.simulator = OrbitSimulator.defaults();
      case "the body <body> is visible with color <color>, radius <radius_px>, mass <mass>, position <x>, <y>, and velocity <vx>, <vy>" ->
          assertVisibleBody(world, example);
      case "the body <orbiter> starts <distance> units from <center>" -> assertDistance(world, example);
      case "a body <first_body> has mass <first_mass>, position <first_x>, <first_y>, and velocity <first_vx>, <first_vy>" ->
          world.bodies = List.of(body(example, "first"));
      case "a body <second_body> has mass <second_mass>, position <second_x>, <second_y>, and velocity <second_vx>, <second_vy>" ->
          world.bodies = List.of(world.bodies.getFirst(), body(example, "second"));
      case "gravitational acceleration is calculated using gravity constant <gravity_constant>" ->
          world.accelerations = Physics.accelerations(world.bodies, number(example, "gravity_constant"));
      case "the acceleration of <first_body> is <first_ax>, <first_ay>" ->
          assertVector(world.accelerations.get(0), example, "first_ax", "first_ay");
      case "the acceleration of <second_body> is <second_ax>, <second_ay>" ->
          assertVector(world.accelerations.get(1), example, "second_ax", "second_ay");
      case "the default orbit simulator bodies are running" -> world.simulator = OrbitSimulator.defaults();
      case "the simulator has advanced by <before_pause_seconds> seconds using gravity constant <gravity_constant> and velocity-first integration" ->
          world.simulator.tick(number(example, "before_pause_seconds"), number(example, "gravity_constant"));
      case "the simulator has advanced by <elapsed_seconds> seconds using gravity constant <gravity_constant> and velocity-first integration" ->
          world.simulator.tick(number(example, "elapsed_seconds"), number(example, "gravity_constant"));
      case "the pause button is pressed" -> world.simulator.togglePause();
      case "the simulator attempts to advance by <paused_seconds> seconds using gravity constant <gravity_constant> and velocity-first integration" ->
          world.simulator.tick(number(example, "paused_seconds"), number(example, "gravity_constant"));
      case "the simulation is paused" -> assertTrue(world.simulator.isPaused(), "simulation should be paused");
      case "the restart button is pressed" -> world.simulator.restart();
      case "the simulation is running" -> assertTrue(world.simulator.isRunning(), "simulation should be running");
      case "the control button label is <resume_label>" -> assertEquals(text(example, "resume_label"), world.simulator.controlButtonLabel());
      case "the control button label is <pause_label>" -> assertEquals(text(example, "pause_label"), world.simulator.controlButtonLabel());
      case "the simulator advances by <seconds> seconds using gravity constant <gravity_constant> and velocity-first integration" ->
          world.simulator.tick(number(example, "seconds"), number(example, "gravity_constant"));
      case "the body <body> has position <x>, <y> and velocity <vx>, <vy>" -> assertBodyState(world, example);
      case "the speed slider has minimum <minimum_speed>, maximum <maximum_speed>, step <speed_step>, value <default_speed>, and label <default_label>" ->
          assertDefaultSpeedSlider(world, example);
      case "the speed slider is set to <speed_multiplier>" ->
          world.simulator.setSpeedMultiplier((int) number(example, "speed_multiplier"));
      case "the simulator advances display time by <display_seconds> seconds using gravity constant <gravity_constant> and velocity-first integration" ->
          world.simulator.advanceDisplayTime(number(example, "display_seconds"), number(example, "gravity_constant"));
      case "the simulator has advanced physics time by <physics_seconds> seconds" ->
          assertNumber(example, "physics_seconds", world.simulator.elapsedPhysicsSeconds());
      case "the speed slider label is <speed_label>" ->
          assertEquals(text(example, "speed_label"), world.simulator.speedLabel());
      default -> throw new UnsupportedStep();
    }
  }

  private void assertDefaultSpeedSlider(World world, Map<String, String> example) {
    assertNumber(example, "minimum_speed", OrbitSimulator.MINIMUM_SPEED);
    assertNumber(example, "maximum_speed", OrbitSimulator.MAXIMUM_SPEED);
    assertNumber(example, "speed_step", OrbitSimulator.SPEED_STEP);
    assertNumber(example, "default_speed", world.simulator.speedMultiplier());
    assertEquals(text(example, "default_label"), world.simulator.speedLabel());
  }

  private void assertVisibleBody(World world, Map<String, String> example) {
    Body body = find(world, text(example, "body"));
    assertEquals(text(example, "color"), body.color());
    assertNumber(example, "radius_px", body.radiusPixels());
    assertNumber(example, "mass", body.mass());
    assertVector(body.position(), example, "x", "y");
    assertVector(body.velocity(), example, "vx", "vy");
  }

  private void assertDistance(World world, Map<String, String> example) {
    Body orbiter = find(world, text(example, "orbiter"));
    Body center = find(world, text(example, "center"));
    assertNumber(example, "distance", orbiter.position().minus(center.position()).magnitude());
  }

  private void assertBodyState(World world, Map<String, String> example) {
    Body body = find(world, text(example, "body"));
    assertVector(body.position(), example, "x", "y");
    assertVector(body.velocity(), example, "vx", "vy");
  }

  private Body find(World world, String name) {
    assertTrue(world.simulator != null, "simulator has not been opened");
    return world.simulator.findBody(name).orElseThrow(() -> new IllegalArgumentException("missing body: " + name));
  }

  private Body body(Map<String, String> example, String prefix) {
    return new Body(
        text(example, prefix + "_body"),
        "",
        1,
        number(example, prefix + "_mass"),
        new Vector2(number(example, prefix + "_x"), number(example, prefix + "_y")),
        new Vector2(number(example, prefix + "_vx"), number(example, prefix + "_vy"))
    );
  }

  private void assertVector(Vector2 actual, double expectedX, double expectedY) {
    assertEquals(expectedX, actual.x(), TOLERANCE);
    assertEquals(expectedY, actual.y(), TOLERANCE);
  }

  private void assertVector(Vector2 actual, Map<String, String> example, String xKey, String yKey) {
    assertNumber(example, xKey, actual.x());
    assertNumber(example, yKey, actual.y());
  }

  private void assertNumber(Map<String, String> example, String key, double actual) {
    String expected = text(example, key);
    assertEquals(Double.parseDouble(expected), actual, toleranceFor(expected));
  }

  private double toleranceFor(String expected) {
    int decimal = expected.indexOf('.');
    if (decimal < 0) {
      return 0.000001;
    }
    return Math.pow(10, -(expected.length() - decimal - 1)) * 0.55;
  }

  private String text(Map<String, String> example, String key) {
    String value = example.get(key);
    if (value == null) {
      throw new IllegalArgumentException("missing example value: " + key);
    }
    return value;
  }

  private double number(Map<String, String> example, String key) {
    return Double.parseDouble(text(example, key));
  }
}
