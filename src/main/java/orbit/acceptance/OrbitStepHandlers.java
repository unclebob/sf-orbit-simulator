package orbit.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import orbit.Body;
import orbit.OrbitSimulator;
import orbit.Physics;
import orbit.Vector2;

public class OrbitStepHandlers implements StepHandlers {
  private static final double TOLERANCE = 0.0001;
  private final Map<String, BiConsumer<World, Map<String, String>>> handlers = Map.ofEntries(
      Map.entry("the orbit simulator is opened", (world, example) -> world.simulator = OrbitSimulator.defaults()),
      Map.entry(
          "the body <body> is visible with color <color>, radius <radius_px>, mass <mass>, position <x>, <y>, and velocity <vx>, <vy>",
          this::assertVisibleBody
      ),
      Map.entry("the body <orbiter> starts <distance> units from <center>", this::assertDistance),
      Map.entry(
          "a body <first_body> has mass <first_mass>, position <first_x>, <first_y>, and velocity <first_vx>, <first_vy>",
          (world, example) -> world.bodies = List.of(body(example, "first"))
      ),
      Map.entry(
          "a body <second_body> has mass <second_mass>, position <second_x>, <second_y>, and velocity <second_vx>, <second_vy>",
          (world, example) -> world.bodies = List.of(world.bodies.getFirst(), body(example, "second"))
      ),
      Map.entry(
          "gravitational acceleration is calculated using gravity constant <gravity_constant>",
          (world, example) -> world.accelerations = Physics.accelerations(world.bodies, number(example, "gravity_constant"))
      ),
      Map.entry(
          "the acceleration of <first_body> is <first_ax>, <first_ay>",
          (world, example) -> assertVector(world.accelerations.get(0), example, "first_ax", "first_ay")
      ),
      Map.entry(
          "the acceleration of <second_body> is <second_ax>, <second_ay>",
          (world, example) -> assertVector(world.accelerations.get(1), example, "second_ax", "second_ay")
      ),
      Map.entry("the default orbit simulator bodies are running", (world, example) -> world.simulator = OrbitSimulator.defaults()),
      Map.entry(
          "the simulator has advanced by <before_pause_seconds> seconds using gravity constant <gravity_constant> and velocity-first integration",
          (world, example) -> tick(world, example, "before_pause_seconds")
      ),
      Map.entry(
          "the simulator has advanced by <elapsed_seconds> seconds using gravity constant <gravity_constant> and velocity-first integration",
          (world, example) -> tick(world, example, "elapsed_seconds")
      ),
      Map.entry("the pause button is pressed", (world, example) -> world.simulator.togglePause()),
      Map.entry(
          "the simulator attempts to advance by <paused_seconds> seconds using gravity constant <gravity_constant> and velocity-first integration",
          (world, example) -> tick(world, example, "paused_seconds")
      ),
      Map.entry("the simulation is paused", (world, example) -> assertTrue(world.simulator.isPaused(), "simulation should be paused")),
      Map.entry("the restart button is pressed", (world, example) -> world.simulator.restart()),
      Map.entry("the simulation is running", (world, example) -> assertTrue(world.simulator.isRunning(), "simulation should be running")),
      Map.entry("the control button label is <resume_label>", (world, example) -> assertControlLabel(world, example, "resume_label")),
      Map.entry("the control button label is <pause_label>", (world, example) -> assertControlLabel(world, example, "pause_label")),
      Map.entry(
          "the simulator advances by <seconds> seconds using gravity constant <gravity_constant> and velocity-first integration",
          (world, example) -> tick(world, example, "seconds")
      ),
      Map.entry("the body <body> has position <x>, <y> and velocity <vx>, <vy>", this::assertBodyState),
      Map.entry(
          "the speed slider has minimum <minimum_speed>, maximum <maximum_speed>, step <speed_step>, value <default_speed>, and label <default_label>",
          this::assertDefaultSpeedSlider
      ),
      Map.entry(
          "the speed slider is set to <speed_multiplier>",
          (world, example) -> world.simulator.setSpeedMultiplier((int) number(example, "speed_multiplier"))
      ),
      Map.entry(
          "the simulator advances display time by <display_seconds> seconds using gravity constant <gravity_constant> and velocity-first integration",
          (world, example) -> world.simulator.advanceDisplayTime(number(example, "display_seconds"), number(example, "gravity_constant"))
      ),
      Map.entry(
          "the simulator has advanced physics time by <physics_seconds> seconds",
          (world, example) -> assertNumber(example, "physics_seconds", world.simulator.elapsedPhysicsSeconds())
      ),
      Map.entry("the speed slider label is <speed_label>", (world, example) -> assertEquals(text(example, "speed_label"), world.simulator.speedLabel()))
  );

  @Override
  public void handle(String stepText, World world, Map<String, String> example) {
    handlerFor(stepText).accept(world, example);
  }

  private BiConsumer<World, Map<String, String>> handlerFor(String stepText) {
    BiConsumer<World, Map<String, String>> handler = handlers.get(stepText);
    if (handler == null) {
      throw new UnsupportedStep();
    }
    return handler;
  }

  private void tick(World world, Map<String, String> example, String secondsKey) {
    world.simulator.tick(number(example, secondsKey), number(example, "gravity_constant"));
  }

  private void assertControlLabel(World world, Map<String, String> example, String labelKey) {
    assertEquals(text(example, labelKey), world.simulator.controlButtonLabel());
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
