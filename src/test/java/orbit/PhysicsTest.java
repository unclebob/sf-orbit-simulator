package orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  void symplecticTickUpdatesVelocityBeforePosition() {
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
    assertEquals(4, body.radiusPixels(), 0.000001);
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

  @Test
  void releasingVelocityDragChangesVelocityWithoutMovingTheBody() {
    OrbitSimulator simulator = OrbitSimulator.defaults();

    Body earth = simulator.setBodyVelocityFromDrag("earth", new Vector2(220, -50), 0.01);
    Body moon = simulator.setBodyVelocityFromDrag("moon", new Vector2(264, 30), 0.01);

    assertEquals(220, earth.position().x(), 0.000001);
    assertEquals(0, earth.position().y(), 0.000001);
    assertEquals(0, earth.velocity().x(), 0.000001);
    assertEquals(2.5151, earth.velocity().y(), 0.0001);
    assertEquals("earth", moon.orbitCenter());
    assertEquals(0, moon.velocity().x(), 0.000001);
    assertEquals(4.8227, moon.velocity().y(), 0.0001);
  }

  @Test
  void collidingBodiesMergeWithConservedMassAreaMomentumAndCenterOfMass() {
    OrbitSimulator simulator = collisionSimulator(2, 7, -2);

    simulator.resolveCollisions();

    Body merged = singleMergedBody(simulator);
    assertEquals("blue", merged.color());
    assertEquals(5, merged.radiusPixels(), 0.000001);
    assertEquals(4, merged.mass(), 0.000001);
    assertEquals(1.75, merged.position().x(), 0.000001);
    assertEquals(0, merged.position().y(), 0.000001);
    assertEquals(1, merged.velocity().x(), 0.000001);
    assertEquals(0, merged.velocity().y(), 0.000001);
  }

  @Test
  void tickResolvesCollisionsAfterUpdatingPositions() {
    OrbitSimulator simulator = collisionSimulator(3, 10, -3);

    simulator.tick(1, 0);

    Body merged = singleMergedBody(simulator);
    assertEquals(4, merged.position().x(), 0.000001);
    assertEquals(1.5, merged.velocity().x(), 0.000001);
  }

  private static OrbitSimulator collisionSimulator(double alphaVelocityX, double betaX, double betaVelocityX) {
    return new OrbitSimulator(List.of(
        new Body("alpha", "blue", 4, 3, new Vector2(0, 0), new Vector2(alphaVelocityX, 0)),
        new Body("beta", "gray", 3, 1, new Vector2(betaX, 0), new Vector2(betaVelocityX, 0))
    ));
  }

  private static Body singleMergedBody(OrbitSimulator simulator) {
    assertEquals(1, simulator.bodyCount());
    return simulator.bodies().getFirst();
  }

  @Test
  void bodiesOutsideLargerRadiusDoNotMerge() {
    OrbitSimulator simulator = new OrbitSimulator(List.of(
        new Body("alpha", "blue", 4, 3, new Vector2(0, 0), new Vector2(2, 0)),
        new Body("beta", "gray", 3, 1, new Vector2(8, 0), new Vector2(-2, 0))
    ));

    simulator.resolveCollisions();

    assertEquals(2, simulator.bodyCount());
    assertTrue(simulator.findBody("alpha").isPresent());
    assertTrue(simulator.findBody("beta").isPresent());
  }
}
