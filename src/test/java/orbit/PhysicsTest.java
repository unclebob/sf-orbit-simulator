package orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PhysicsTest {
  @Test
  void appliesPairwiseNewtonianAcceleration() {
    Body sun = new Body("sun", "yellow", 36, 2000, new Vector2(0, 0), new Vector2(0, 0));
    Body earth = new Body("earth", "blue", 12, 100, new Vector2(220, 0), new Vector2(0, 3.0151));

    List<Vector2> accelerations = Physics.accelerations(List.of(sun, earth), 1);

    assertEquals(0.002066, accelerations.get(0).x(), 0.000001);
    assertEquals(-0.041322, accelerations.get(1).x(), 0.000001);
  }

  @Test
  void velocityFirstTickUsesAccelerationBeforePosition() {
    OrbitSimulator simulator = OrbitSimulator.defaults();

    simulator.tick(1, 1);

    Body earth = simulator.findBody("earth").orElseThrow();
    assertEquals(219.9592, earth.position().x(), 0.0001);
    assertEquals(3.0151, earth.position().y(), 0.0001);
    assertEquals(-0.0408, earth.velocity().x(), 0.0001);
    assertEquals(3.0151, earth.velocity().y(), 0.0001);
  }

  @Test
  void pauseSkipsTicksAndChangesControlLabel() {
    OrbitSimulator simulator = OrbitSimulator.defaults();
    simulator.tick(1, 1);
    simulator.togglePause();

    simulator.tick(5, 1);

    Body earth = simulator.findBody("earth").orElseThrow();
    assertEquals(219.9592, earth.position().x(), 0.0001);
    assertEquals("Resume", simulator.controlButtonLabel());
  }

  @Test
  void restartRestoresInitialBodiesAndRunningState() {
    OrbitSimulator simulator = OrbitSimulator.defaults();
    simulator.tick(3, 1);
    simulator.togglePause();

    simulator.restart();

    Body earth = simulator.findBody("earth").orElseThrow();
    assertEquals(220, earth.position().x(), 0.000001);
    assertEquals(0, earth.velocity().x(), 0.000001);
    assertEquals("Pause", simulator.controlButtonLabel());
  }

  @Test
  void speedMultiplierScalesDisplayTimeIntoPhysicsTime() {
    OrbitSimulator simulator = OrbitSimulator.defaults();
    simulator.setSpeedMultiplier(2);

    simulator.advanceDisplayTime(1, 1);

    Body earth = simulator.findBody("earth").orElseThrow();
    assertEquals(2, simulator.elapsedPhysicsSeconds(), 0.000001);
    assertEquals(219.8368, earth.position().x(), 0.0001);
    assertEquals(6.0302, earth.position().y(), 0.0001);
    assertEquals("2X", simulator.speedLabel());
  }

  @Test
  void speedSliderDefaultsToOneThroughTwentyInWholeSteps() {
    OrbitSimulator simulator = OrbitSimulator.defaults();

    assertEquals(1, OrbitSimulator.MINIMUM_SPEED);
    assertEquals(20, OrbitSimulator.MAXIMUM_SPEED);
    assertEquals(1, OrbitSimulator.SPEED_STEP);
    assertEquals(1, simulator.speedMultiplier());
    assertEquals("1X", simulator.speedLabel());
  }

  @Test
  void emptyClickAddsCircularOrbitBodyAroundSun() {
    OrbitSimulator simulator = OrbitSimulator.defaults();

    Body body = simulator.addBodyInCircularOrbit(new Vector2(300, 0), "sun", 1);

    assertEquals("body_1", body.name());
    assertEquals("sun", body.orbitCenter());
    assertEquals(300, body.periapsisDistance(), 0.000001);
    assertEquals(0, body.velocity().x(), 0.0001);
    assertEquals(2.5820, body.velocity().y(), 0.0001);
    assertEquals(4, simulator.bodyCount());
  }

  @Test
  void nearBodyClickAddsCircularOrbitBodyAroundThatBody() {
    OrbitSimulator simulator = OrbitSimulator.defaults();

    Body body = simulator.addBodyInCircularOrbit(new Vector2(220, 60), "earth", 1);

    assertEquals("earth", body.orbitCenter());
    assertEquals(-1.2910, body.velocity().x(), 0.0001);
    assertEquals(3.0151, body.velocity().y(), 0.0001);
  }

  @Test
  void draggingBodyToApoapsisKeepsOriginalPeriapsisAndSetsApoapsisVelocity() {
    OrbitSimulator simulator = OrbitSimulator.defaults();

    Body earth = simulator.dragBodyToApoapsis("earth", new Vector2(330, 0), 1);
    OrbitSimulator moonSimulator = OrbitSimulator.defaults();
    Body moon = moonSimulator.dragBodyToApoapsis("moon", new Vector2(286, 0), 1);

    assertEquals(220, earth.periapsisDistance(), 0.000001);
    assertEquals(330, simulator.apoapsisDistance("earth"), 0.000001);
    assertEquals(2.2020, earth.velocity().y(), 0.0001);
    assertEquals(44, moon.periapsisDistance(), 0.000001);
    assertEquals(66, moonSimulator.apoapsisDistance("moon"), 0.000001);
    assertEquals(4.1161, moon.velocity().y(), 0.0001);
  }
}
