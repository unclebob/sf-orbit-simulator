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
          "the speed slider is set to <start_speed>",
          (world, example) -> world.simulator.setSpeedMultiplier((int) number(example, "start_speed"))
      ),
      Map.entry(
          "the simulator advances display time by <display_seconds> seconds using gravity constant <gravity_constant> and velocity-first integration",
          (world, example) -> world.simulator.advanceDisplayTime(number(example, "display_seconds"), number(example, "gravity_constant"))
      ),
      Map.entry(
          "the simulator has advanced physics time by <physics_seconds> seconds",
          (world, example) -> assertNumber(example, "physics_seconds", world.simulator.elapsedPhysicsSeconds())
      ),
      Map.entry(
          "the speed slider label is <speed_label>",
          (world, example) -> assertEquals(text(example, "speed_label"), world.simulator.speedLabel())
      ),
      Map.entry(
          "the speed slider thumb is dragged to <end_speed>",
          (world, example) -> world.simulator.setSpeedMultiplier((int) number(example, "end_speed"))
      ),
      Map.entry("the speed slider value is <end_speed>", (world, example) -> assertNumber(example, "end_speed", world.simulator.speedMultiplier())),
      Map.entry(
          "the empty orbit area is clicked at position <x>, <y> using gravity constant <gravity_constant>",
          (world, example) -> world.addedBody = world.simulator.addBodyInCircularOrbit(position(example, "x", "y"), "sun", number(example, "gravity_constant"))
      ),
      Map.entry(
          "a body <body> is added with color <color>, radius <radius_px>, mass <mass>, position <x>, <y>, and velocity <vx>, <vy>",
          this::assertAddedBody
      ),
      Map.entry(
          "the body <body> has circular orbit speed <speed> around the sun",
          (world, example) -> assertCircularOrbitSpeed(world, example, "sun")
      ),
      Map.entry("the simulator has <body_count> bodies", (world, example) -> assertNumber(example, "body_count", world.simulator.bodyCount())),
      Map.entry(
          "the body <body> is dragged to aphelion position <aphelion_x>, <aphelion_y> using gravity constant <gravity_constant>",
          (world, example) -> world.simulator.dragBodyToApoapsis(text(example, "body"), position(example, "aphelion_x", "aphelion_y"), number(example, "gravity_constant"))
      ),
      Map.entry(
          "the body <body> has perihelion distance <perihelion_distance> and aphelion distance <aphelion_distance> around the sun",
          (world, example) -> assertApsides(world, example, "perihelion_distance", "aphelion_distance", "sun")
      ),
      Map.entry(
          "the orbit area is clicked at position <x>, <y> within <diameter_count> diameters of <center_body> using gravity constant <gravity_constant>",
          (world, example) -> world.addedBody = world.simulator.addBodyInCircularOrbit(position(example, "x", "y"), text(example, "center_body"), number(example, "gravity_constant"))
      ),
      Map.entry(
          "a body <body> is added orbiting <center_body> with color <color>, radius <radius_px>, mass <mass>, position <x>, <y>, and velocity <vx>, <vy>",
          this::assertAddedOrbitingBody
      ),
      Map.entry(
          "the body <body> has circular orbit speed <speed> around <center_body>",
          (world, example) -> assertCircularOrbitSpeed(world, example, text(example, "center_body"))
      ),
      Map.entry("the body <body> orbits <center_body>", this::assertBodyOrbitsCenter),
      Map.entry(
          "the body <body> is dragged to apoapsis position <apoapsis_x>, <apoapsis_y> using gravity constant <gravity_constant>",
          (world, example) -> world.simulator.dragBodyToApoapsis(text(example, "body"), position(example, "apoapsis_x", "apoapsis_y"), number(example, "gravity_constant"))
      ),
      Map.entry(
          "the body <body> has periapsis distance <periapsis_distance> and apoapsis distance <apoapsis_distance> around <center_body>",
          (world, example) -> assertApsides(world, example, "periapsis_distance", "apoapsis_distance", text(example, "center_body"))
      ),
      Map.entry(
          "the body <body> has position <aphelion_x>, <aphelion_y> and velocity <vx>, <vy>",
          (world, example) -> assertBodyState(world, example, "aphelion_x", "aphelion_y")
      ),
      Map.entry(
          "the body <body> has position <apoapsis_x>, <apoapsis_y> and velocity <vx>, <vy>",
          (world, example) -> assertBodyState(world, example, "apoapsis_x", "apoapsis_y")
      )
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

  private void assertAddedBody(World world, Map<String, String> example) {
    assertBodyMatches(world.addedBody, example);
  }

  private void assertAddedOrbitingBody(World world, Map<String, String> example) {
    assertBodyMatches(world.addedBody, example);
    assertEquals(text(example, "center_body"), world.addedBody.orbitCenter());
  }

  private void assertBodyMatches(Body body, Map<String, String> example) {
    assertEquals(text(example, "body"), body.name());
    assertEquals(text(example, "color"), body.color());
    assertNumber(example, "radius_px", body.radiusPixels());
    assertNumber(example, "mass", body.mass());
    assertVector(body.position(), example, "x", "y");
    assertVector(body.velocity(), example, "vx", "vy");
  }

  private void assertCircularOrbitSpeed(World world, Map<String, String> example, String centerName) {
    Body body = find(world, text(example, "body"));
    Body center = find(world, centerName);
    double relativeSpeed = body.velocity().minus(center.velocity()).magnitude();
    assertNumber(example, "speed", relativeSpeed);
  }

  private void assertApsides(
      World world,
      Map<String, String> example,
      String periapsisKey,
      String apoapsisKey,
      String centerName
  ) {
    Body body = find(world, text(example, "body"));
    assertEquals(centerName, body.orbitCenter());
    assertNumber(example, periapsisKey, body.periapsisDistance());
    assertNumber(example, apoapsisKey, world.simulator.apoapsisDistance(body.name()));
  }

  private void assertBodyOrbitsCenter(World world, Map<String, String> example) {
    Body body = find(world, text(example, "body"));
    assertEquals(text(example, "center_body"), body.orbitCenter());
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
    assertBodyState(world, example, "x", "y");
  }

  private void assertBodyState(World world, Map<String, String> example, String xKey, String yKey) {
    Body body = find(world, text(example, "body"));
    assertVector(body.position(), example, xKey, yKey);
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
    return Math.pow(10, -(expected.length() - decimal - 1)) * 0.8;
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

  private Vector2 position(Map<String, String> example, String xKey, String yKey) {
    return new Vector2(number(example, xKey), number(example, yKey));
  }
}
