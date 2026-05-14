package orbit.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import orbit.Body;
import orbit.OrbitSimulator;
import orbit.Physics;
import orbit.TidalDeformation;
import orbit.Vector2;

public class OrbitStepHandlers implements StepHandlers {
  private static final double TOLERANCE = 0.0001;
  private final Map<String, BiConsumer<World, Map<String, String>>> handlers = Map.ofEntries(
      Map.entry("the orbit simulator is opened", (world, example) -> world.simulator = OrbitSimulator.defaults()),
      Map.entry(
          "the body <body> is visible with color <color>, radius <radius_px>, mass <mass>, position <x>, <y>, and velocity <vx>, <vy>",
          this::assertVisibleBody
      ),
      Map.entry(
          "a body <smaller_body> has mass <smaller_mass> and radius <smaller_radius_px>",
          (world, example) -> world.bodies = List.of(massRadiusBody(example, "smaller"))
      ),
      Map.entry(
          "a body <larger_body> has mass <larger_mass> and radius <larger_radius_px>",
          (world, example) -> world.bodies = List.of(world.bodies.getFirst(), massRadiusBody(example, "larger"))
      ),
      Map.entry(
          "<larger_body> has greater radius than <smaller_body>",
          this::assertLargerBodyHasGreaterRadius
      ),
      Map.entry(
          "a body <body> has mass <mass>, radius <radius_px>, position <x>, <y>, and elasticity <elasticity>",
          this::setTidalBody
      ),
      Map.entry(
          "a tidal source <source_body> has mass <source_mass> and position <source_x>, <source_y>",
          this::setTidalSource
      ),
      Map.entry("tidal deformation is calculated", this::calculateTidalDeformation),
      Map.entry(
          "tidal deformation is calculated by integrating gravity over <sample_count> body samples using gravity constant <gravity_constant>",
          this::calculateIntegratedTidalDeformation
      ),
      Map.entry(
          "tidal deformation is calculated by summing gravity over <sample_count> surface samples using gravity constant <gravity_constant>",
          this::calculateIntegratedTidalDeformation
      ),
      Map.entry(
          "the integrated tidal stretch vector of <body> is <stretch_x>, <stretch_y>",
          this::assertTidalStretchVector
      ),
      Map.entry(
          "the integrated tidal stretch magnitude of <body> is <stretch_magnitude>",
          this::assertTidalStretchMagnitude
      ),
      Map.entry(
          "the body <body> is rendered as an ellipse centered at <x>, <y> with major radius <major_radius_px>, minor radius <minor_radius_px>, and major axis pointing toward <source_body>",
          this::assertTidalEllipse
      ),
      Map.entry(
          "the body <body> is rendered as an ellipse centered at <x>, <y> with major radius <major_radius_px>, minor radius <minor_radius_px>, and major axis aligned with the tidal stretch vector",
          this::assertTidalEllipseAlignedWithStretch
      ),
      Map.entry(
          "the body <body> has gravity foci at <first_focus_x>, <first_focus_y> and <second_focus_x>, <second_focus_y>",
          this::assertTidalFoci
      ),
      Map.entry(
          "a <focus_line_color> line segment is drawn from <first_focus_x>, <first_focus_y> to <second_focus_x>, <second_focus_y>",
          this::assertFocusLine
      ),
      Map.entry(
          "a body <weaker_body> has tidal stretch magnitude <weaker_stretch_magnitude>, major radius <weaker_major_radius_px>, and minor radius <weaker_minor_radius_px>",
          this::setWeakerTidalDeformation
      ),
      Map.entry(
          "a body <stronger_body> has tidal stretch magnitude <stronger_stretch_magnitude>, major radius <stronger_major_radius_px>, and minor radius <stronger_minor_radius_px>",
          this::setStrongerTidalDeformation
      ),
      Map.entry(
          "<stronger_body> has greater elongation than <weaker_body>",
          this::assertStrongerTidalElongation
      ),
      Map.entry(
          "an elastic body <source_body> has mass <source_mass>, first focus <first_focus_x>, <first_focus_y>, and second focus <second_focus_x>, <second_focus_y>",
          this::setElasticSource
      ),
      Map.entry(
          "a body <target_body> has mass <target_mass>, position <target_x>, <target_y>, and velocity <target_vx>, <target_vy>",
          this::setElasticTarget
      ),
      Map.entry(
          "gravitational acceleration from <source_body> to <target_body> is calculated using gravity constant <gravity_constant>",
          this::calculateElasticAcceleration
      ),
      Map.entry(
          "the acceleration of <target_body> is <target_ax>, <target_ay>",
          (world, example) -> assertVector(world.elasticAcceleration, example, "target_ax", "target_ay")
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
          "a body <first_body> has color <first_color>, radius <first_radius_px>, mass <first_mass>, position <first_x>, <first_y>, and velocity <first_vx>, <first_vy>",
          (world, example) -> setCollisionBody(world, example, "first")
      ),
      Map.entry(
          "a body <second_body> has color <second_color>, radius <second_radius_px>, mass <second_mass>, position <second_x>, <second_y>, and velocity <second_vx>, <second_vy>",
          (world, example) -> setCollisionBody(world, example, "second")
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
          "the simulator has advanced by <before_pause_seconds> seconds using gravity constant <gravity_constant> and symplectic integration",
          (world, example) -> tick(world, example, "before_pause_seconds")
      ),
      Map.entry(
          "the simulator has advanced by <elapsed_seconds> seconds using gravity constant <gravity_constant> and symplectic integration",
          (world, example) -> tick(world, example, "elapsed_seconds")
      ),
      Map.entry("the pause button is pressed", (world, example) -> world.simulator.togglePause()),
      Map.entry(
          "the simulator attempts to advance by <paused_seconds> seconds using gravity constant <gravity_constant> and symplectic integration",
          (world, example) -> tick(world, example, "paused_seconds")
      ),
      Map.entry("the simulation is paused", (world, example) -> assertTrue(world.simulator.isPaused(), "simulation should be paused")),
      Map.entry("the restart button is pressed", (world, example) -> world.simulator.restart()),
      Map.entry("the simulation is running", (world, example) -> assertTrue(world.simulator.isRunning(), "simulation should be running")),
      Map.entry("the control button label is <resume_label>", (world, example) -> assertControlLabel(world, example, "resume_label")),
      Map.entry("the control button label is <pause_label>", (world, example) -> assertControlLabel(world, example, "pause_label")),
      Map.entry(
          "the simulator advances by <seconds> seconds using gravity constant <gravity_constant> and symplectic integration",
          (world, example) -> tick(world, example, "seconds")
      ),
      Map.entry("the body <body> has position <x>, <y> and velocity <vx>, <vy>", this::assertBodyState),
      Map.entry(
          "the body <body> has position <body_x>, <body_y> and velocity <vx>, <vy>",
          (world, example) -> assertBodyState(world, example, "body_x", "body_y")
      ),
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
          "the simulator advances display time by <display_seconds> seconds using gravity constant <gravity_constant> and symplectic integration",
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
          "the view center is <start_center_x>, <start_center_y>",
          (world, example) -> setOrAssertViewCenter(world, example, "start_center_x", "start_center_y")
      ),
      Map.entry(
          "the view center is <end_center_x>, <end_center_y>",
          (world, example) -> setOrAssertViewCenter(world, example, "end_center_x", "end_center_y")
      ),
      Map.entry(
          "the orbit area receives scroll input <scroll_x>, <scroll_y> with scroll scale <scroll_scale>",
          this::scrollViewCenter
      ),
      Map.entry(
          "the orbit area receives horizontal scroll input <scroll_x> with scroll scale <scroll_scale>",
          this::scrollViewCenterHorizontally
      ),
      Map.entry(
          "the orbit area receives vertical scroll input <scroll_y> with scroll scale <scroll_scale>",
          this::scrollViewCenterVertically
      ),
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
          "the body <body> is dragged toward mouse position <mouse_x>, <mouse_y>",
          this::startVelocityDrag
      ),
      Map.entry(
          "a velocity preview line is drawn from <body_x>, <body_y> to <mouse_x>, <mouse_y>",
          this::assertVelocityPreview
      ),
      Map.entry(
          "the body <body> still has position <body_x>, <body_y> and velocity <vx>, <vy>",
          (world, example) -> assertBodyState(world, example, "body_x", "body_y")
      ),
      Map.entry(
          "the body <body> is dragged from position <body_x>, <body_y> to mouse position <mouse_x>, <mouse_y>",
          this::startVelocityDragFromPosition
      ),
      Map.entry(
          "the mouse button is released with velocity scale <velocity_scale>",
          this::releaseVelocityDrag
      ),
      Map.entry(
          "the body <body> still orbits <center_body>",
          this::assertBodyOrbitsCenter
      ),
      Map.entry("collisions are resolved", (world, example) -> world.simulator.resolveCollisions()),
      Map.entry("screen collisions are resolved using rendered body edges", (world, example) -> world.simulator.resolveCollisions()),
      Map.entry(
          "the body <first_body> has position <first_x>, <first_y> and velocity <first_vx>, <first_vy>",
          (world, example) -> assertPrefixedBodyState(world, example, "first")
      ),
      Map.entry(
          "the body <second_body> has position <second_x>, <second_y> and velocity <second_vx>, <second_vy>",
          (world, example) -> assertPrefixedBodyState(world, example, "second")
      ),
      Map.entry(
          "the original body centers were within collision radius <collision_radius_px>",
          this::assertOriginalCollisionRadius
      ),
      Map.entry(
          "the original rendered body centers were <screen_distance_px> pixels apart",
          this::assertOriginalScreenDistance
      ),
      Map.entry(
          "the original rendered body edges were touching at screen distance <touch_distance_px>",
          this::assertOriginalTouchDistance
      ),
      Map.entry(
          "the merged body has color <merged_color>, radius <merged_radius_px>, mass <merged_mass>, position <merged_x>, <merged_y>, and velocity <merged_vx>, <merged_vy>",
          this::assertMergedBody
      ),
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

  private void assertLargerBodyHasGreaterRadius(World world, Map<String, String> example) {
    Body smaller = find(world.bodies, text(example, "smaller_body"));
    Body larger = find(world.bodies, text(example, "larger_body"));
    assertTrue(larger.radiusPixels() > smaller.radiusPixels(), "larger mass should render with larger radius");
  }

  private void setTidalBody(World world, Map<String, String> example) {
    world.tidalBody = new Body(
        text(example, "body"),
        "",
        number(example, "radius_px"),
        number(example, "mass"),
        position(example, "x", "y"),
        new Vector2(0, 0)
    );
  }

  private void setTidalSource(World world, Map<String, String> example) {
    world.tidalSource = new Body(
        text(example, "source_body"),
        "",
        0,
        number(example, "source_mass"),
        position(example, "source_x", "source_y"),
        new Vector2(0, 0)
    );
  }

  private void calculateTidalDeformation(World world, Map<String, String> example) {
    world.tidalDeformation = TidalDeformation.calculate(world.tidalBody, world.tidalSource, number(example, "elasticity"));
  }

  private void calculateIntegratedTidalDeformation(World world, Map<String, String> example) {
    world.tidalDeformation = TidalDeformation.calculate(
        world.tidalBody,
        world.tidalSource,
        number(example, "elasticity"),
        (int) number(example, "sample_count"),
        number(example, "gravity_constant")
    );
  }

  private void assertTidalStretchVector(World world, Map<String, String> example) {
    assertVector(tidalDeformation(world, example).stretchVector(), example, "stretch_x", "stretch_y");
  }

  private void assertTidalStretchMagnitude(World world, Map<String, String> example) {
    assertNumber(example, "stretch_magnitude", tidalDeformation(world, example).stretchMagnitude());
  }

  private void assertTidalEllipse(World world, Map<String, String> example) {
    TidalDeformation deformation = tidalDeformation(world, example);
    assertEquals(text(example, "source_body"), deformation.sourceName());
    assertVector(deformation.center(), example, "x", "y");
    assertNumber(example, "major_radius_px", deformation.majorRadiusPixels());
    assertNumber(example, "minor_radius_px", deformation.minorRadiusPixels());
    Vector2 expectedAxis = world.tidalSource.position().minus(world.tidalBody.position());
    assertTrue(deformation.axisTowardSource().minus(expectedAxis.times(1.0 / expectedAxis.magnitude())).magnitude() < TOLERANCE);
  }

  private void assertTidalEllipseAlignedWithStretch(World world, Map<String, String> example) {
    TidalDeformation deformation = tidalDeformation(world, example);
    assertVector(deformation.center(), example, "x", "y");
    assertNumber(example, "major_radius_px", deformation.majorRadiusPixels());
    assertNumber(example, "minor_radius_px", deformation.minorRadiusPixels());
    Vector2 stretchAxis = deformation.stretchVector().times(1.0 / deformation.stretchMagnitude());
    assertTrue(deformation.axisTowardSource().minus(stretchAxis).magnitude() < TOLERANCE);
  }

  private void assertTidalFoci(World world, Map<String, String> example) {
    TidalDeformation deformation = tidalDeformation(world, example);
    assertVector(deformation.firstFocus(), example, "first_focus_x", "first_focus_y");
    assertVector(deformation.secondFocus(), example, "second_focus_x", "second_focus_y");
  }

  private void assertFocusLine(World world, Map<String, String> example) {
    TidalDeformation deformation = world.tidalDeformation;
    assertEquals("black", text(example, "focus_line_color"));
    assertVector(deformation.firstFocus(), example, "first_focus_x", "first_focus_y");
    assertVector(deformation.secondFocus(), example, "second_focus_x", "second_focus_y");
  }

  private void setWeakerTidalDeformation(World world, Map<String, String> example) {
    world.weakerTidalDeformation = tidalShape(example, "weaker");
  }

  private void setStrongerTidalDeformation(World world, Map<String, String> example) {
    world.strongerTidalDeformation = tidalShape(example, "stronger");
  }

  private TidalDeformation tidalShape(Map<String, String> example, String prefix) {
    return new TidalDeformation(
        text(example, prefix + "_body"),
        "",
        new Vector2(0, 0),
        number(example, prefix + "_major_radius_px"),
        number(example, prefix + "_minor_radius_px"),
        new Vector2(number(example, prefix + "_stretch_magnitude"), 0),
        number(example, prefix + "_stretch_magnitude"),
        new Vector2(1, 0),
        new Vector2(0, 0),
        new Vector2(0, 0)
    );
  }

  private void assertStrongerTidalElongation(World world, Map<String, String> example) {
    assertEquals(text(example, "weaker_body"), world.weakerTidalDeformation.bodyName());
    assertEquals(text(example, "stronger_body"), world.strongerTidalDeformation.bodyName());
    double weakerElongation = elongation(world.weakerTidalDeformation);
    double strongerElongation = elongation(world.strongerTidalDeformation);
    assertTrue(strongerElongation > weakerElongation, "stronger tidal stretch should create greater elongation");
  }

  private double elongation(TidalDeformation deformation) {
    return deformation.majorRadiusPixels() - deformation.minorRadiusPixels();
  }

  private TidalDeformation tidalDeformation(World world, Map<String, String> example) {
    assertEquals(text(example, "body"), world.tidalDeformation.bodyName());
    return world.tidalDeformation;
  }

  private void setElasticSource(World world, Map<String, String> example) {
    world.elasticSource = new Body(
        text(example, "source_body"),
        "",
        0,
        number(example, "source_mass"),
        new Vector2(0, 0),
        new Vector2(0, 0)
    );
    world.tidalDeformation = new TidalDeformation(
        text(example, "source_body"),
        "",
        new Vector2(0, 0),
        0,
        0,
        new Vector2(0, 0),
        0,
        new Vector2(1, 0),
        position(example, "first_focus_x", "first_focus_y"),
        position(example, "second_focus_x", "second_focus_y")
    );
  }

  private void setElasticTarget(World world, Map<String, String> example) {
    world.elasticTarget = new Body(
        text(example, "target_body"),
        "",
        0,
        number(example, "target_mass"),
        position(example, "target_x", "target_y"),
        new Vector2(number(example, "target_vx"), number(example, "target_vy"))
    );
  }

  private void calculateElasticAcceleration(World world, Map<String, String> example) {
    assertEquals(text(example, "source_body"), world.elasticSource.name());
    assertEquals(text(example, "target_body"), world.elasticTarget.name());
    world.elasticAcceleration = Physics.accelerationFromElasticBody(
        world.elasticSource.mass(),
        world.tidalDeformation.firstFocus(),
        world.tidalDeformation.secondFocus(),
        world.elasticTarget,
        number(example, "gravity_constant")
    );
  }

  private void setOrAssertViewCenter(World world, Map<String, String> example, String xKey, String yKey) {
    Vector2 expected = position(example, xKey, yKey);
    if (!world.viewCenterSet) {
      world.viewCenter = expected;
      world.viewCenterSet = true;
      return;
    }
    assertVector(world.viewCenter, expected.x(), expected.y());
  }

  private void scrollViewCenter(World world, Map<String, String> example) {
    Vector2 scroll = position(example, "scroll_x", "scroll_y");
    world.viewCenter = world.viewCenter.plus(scroll.times(number(example, "scroll_scale")));
  }

  private void scrollViewCenterHorizontally(World world, Map<String, String> example) {
    scrollViewCenterBy(world, example, number(example, "scroll_x"), 0);
  }

  private void scrollViewCenterVertically(World world, Map<String, String> example) {
    scrollViewCenterBy(world, example, 0, number(example, "scroll_y"));
  }

  private void scrollViewCenterBy(World world, Map<String, String> example, double scrollX, double scrollY) {
    Vector2 scroll = new Vector2(scrollX, scrollY);
    world.viewCenter = world.viewCenter.plus(scroll.times(number(example, "scroll_scale")));
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

  private void startVelocityDrag(World world, Map<String, String> example) {
    String bodyName = text(example, "body");
    world.draggedBodyName = bodyName;
    world.dragStart = find(world, bodyName).position();
    world.dragEnd = position(example, "mouse_x", "mouse_y");
  }

  private void startVelocityDragFromPosition(World world, Map<String, String> example) {
    Body body = find(world, text(example, "body"));
    assertVector(body.position(), example, "body_x", "body_y");
    startVelocityDrag(world, example);
  }

  private void assertVelocityPreview(World world, Map<String, String> example) {
    assertVector(world.dragStart, example, "body_x", "body_y");
    assertVector(world.dragEnd, example, "mouse_x", "mouse_y");
  }

  private void releaseVelocityDrag(World world, Map<String, String> example) {
    world.simulator.setBodyVelocityFromDrag(world.draggedBodyName, world.dragEnd, number(example, "velocity_scale"));
  }

  private void setCollisionBody(World world, Map<String, String> example, String prefix) {
    Body body = body(example, prefix);
    if ("first".equals(prefix)) {
      world.firstCollisionBody = body;
      world.bodies = List.of(body);
    } else {
      world.secondCollisionBody = body;
      world.bodies = List.of(world.firstCollisionBody, body);
      world.simulator = new OrbitSimulator(world.bodies);
    }
  }

  private void assertOriginalCollisionRadius(World world, Map<String, String> example) {
    double collisionRadius = Math.max(world.firstCollisionBody.radiusPixels(), world.secondCollisionBody.radiusPixels());
    double originalDistance = world.firstCollisionBody.position().minus(world.secondCollisionBody.position()).magnitude();
    assertNumber(example, "collision_radius_px", collisionRadius);
    assertTrue(originalDistance <= collisionRadius, "original bodies should be within collision radius");
  }

  private void assertOriginalScreenDistance(World world, Map<String, String> example) {
    assertNumber(example, "screen_distance_px", originalRenderedCenterDistance(world));
  }

  private void assertOriginalTouchDistance(World world, Map<String, String> example) {
    double touchDistance = world.firstCollisionBody.radiusPixels() + world.secondCollisionBody.radiusPixels();
    assertNumber(example, "touch_distance_px", touchDistance);
    assertNumber(example, "touch_distance_px", originalRenderedCenterDistance(world));
  }

  private double originalRenderedCenterDistance(World world) {
    return world.firstCollisionBody.position().minus(world.secondCollisionBody.position()).magnitude();
  }

  private void assertMergedBody(World world, Map<String, String> example) {
    assertEquals(1, world.simulator.bodyCount());
    Body body = world.simulator.bodies().getFirst();
    assertEquals(text(example, "merged_color"), body.color());
    assertNumber(example, "merged_radius_px", body.radiusPixels());
    assertNumber(example, "merged_mass", body.mass());
    assertVector(body.position(), example, "merged_x", "merged_y");
    assertVector(body.velocity(), example, "merged_vx", "merged_vy");
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

  private void assertPrefixedBodyState(World world, Map<String, String> example, String prefix) {
    Body body = find(world, text(example, prefix + "_body"));
    assertVector(body.position(), example, prefix + "_x", prefix + "_y");
    assertVector(body.velocity(), example, prefix + "_vx", prefix + "_vy");
  }

  private Body find(World world, String name) {
    assertTrue(world.simulator != null, "simulator has not been opened");
    return world.simulator.findBody(name).orElseThrow(() -> new IllegalArgumentException("missing body: " + name));
  }

  private Body find(List<Body> bodies, String name) {
    return bodies.stream()
        .filter(body -> body.name().equals(name))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("missing body: " + name));
  }

  private Body body(Map<String, String> example, String prefix) {
    return new Body(
        text(example, prefix + "_body"),
        textOrDefault(example, prefix + "_color", ""),
        numberOrDefault(example, prefix + "_radius_px", 1),
        number(example, prefix + "_mass"),
        new Vector2(number(example, prefix + "_x"), number(example, prefix + "_y")),
        new Vector2(number(example, prefix + "_vx"), number(example, prefix + "_vy"))
    );
  }

  private Body massRadiusBody(Map<String, String> example, String prefix) {
    return new Body(
        text(example, prefix + "_body"),
        "",
        number(example, prefix + "_radius_px"),
        number(example, prefix + "_mass"),
        new Vector2(0, 0),
        new Vector2(0, 0)
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

  private double numberOrDefault(Map<String, String> example, String key, double defaultValue) {
    String value = example.get(key);
    return value == null ? defaultValue : Double.parseDouble(value);
  }

  private String textOrDefault(Map<String, String> example, String key, String defaultValue) {
    return example.getOrDefault(key, defaultValue);
  }

  private Vector2 position(Map<String, String> example, String xKey, String yKey) {
    return new Vector2(number(example, xKey), number(example, yKey));
  }
}
