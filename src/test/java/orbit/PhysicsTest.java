package orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
  void tickUsesVelocityVerletSubsteps() {
    OrbitSimulator simulator = OrbitSimulator.defaults();

    simulator.tick(1, 1, 0.016667);

    Body earth = simulator.findBody("earth").orElseThrow();
    assertEquals(219.9796, earth.position().x(), 0.0001);
    assertEquals(3.0150, earth.position().y(), 0.0001);
    assertEquals(-0.0408, earth.velocity().x(), 0.0001);
    assertEquals(3.0148, earth.velocity().y(), 0.0001);
  }

  @Test
  void defaultTickUsesSixtyHertzSubsteps() {
    OrbitSimulator simulator = OrbitSimulator.defaults();
    OrbitSimulator explicitSubsteps = OrbitSimulator.defaults();

    simulator.tick(1, 1);
    explicitSubsteps.tick(1, 1, 1.0 / 60.0);

    Body earth = simulator.findBody("earth").orElseThrow();
    Body explicitlySteppedEarth = explicitSubsteps.findBody("earth").orElseThrow();
    assertEquals(explicitlySteppedEarth.position().x(), earth.position().x(), 0.000001);
    assertEquals(explicitlySteppedEarth.position().y(), earth.position().y(), 0.000001);
    assertEquals(explicitlySteppedEarth.velocity().x(), earth.velocity().x(), 0.000001);
    assertEquals(explicitlySteppedEarth.velocity().y(), earth.velocity().y(), 0.000001);
  }

  @Test
  void tinyTickStillAdvancesOneSubstep() {
    OrbitSimulator simulator = new OrbitSimulator(List.of(
        new Body("probe", "gray", 1, 1, new Vector2(0, 0), new Vector2(10, 0))
    ));

    simulator.tick(0.001, 0, 1);

    Body probe = simulator.findBody("probe").orElseThrow();
    assertEquals(0.01, probe.position().x(), 0.000001);
    assertEquals(0.001, simulator.elapsedPhysicsSeconds(), 0.000001);
  }

  @Test
  void pauseSkipsTicksAndChangesControlLabel() {
    OrbitSimulator simulator = OrbitSimulator.defaults();
    simulator.tick(1, 1, 0.016667);
    simulator.togglePause();

    simulator.tick(5, 1);

    Body earth = simulator.findBody("earth").orElseThrow();
    assertEquals(219.9796, earth.position().x(), 0.0001);
    assertEquals("Resume", simulator.controlButtonLabel());
  }

  @Test
  void restartRestoresInitialBodiesAndRunningState() {
    OrbitSimulator simulator = OrbitSimulator.defaults();
    simulator.addBodyInCircularOrbit(new Vector2(300, 0), "sun", 1);
    simulator.tick(3, 1, 0.016667);
    simulator.togglePause();

    simulator.restart();
    Body addedAfterRestart = simulator.addBodyInCircularOrbit(new Vector2(300, 0), "sun", 1);

    Body earth = simulator.findBody("earth").orElseThrow();
    assertEquals(220, earth.position().x(), 0.000001);
    assertEquals(0, earth.velocity().x(), 0.000001);
    assertEquals("Pause", simulator.controlButtonLabel());
    assertEquals("body_1", addedAfterRestart.name());
    assertEquals(0, simulator.elapsedPhysicsSeconds(), 0.000001);
  }

  @Test
  void speedMultiplierScalesDisplayTimeIntoPhysicsTime() {
    OrbitSimulator simulator = OrbitSimulator.defaults();
    simulator.setSpeedMultiplier(2);

    simulator.advanceDisplayTime(1, 1, 0.016667);

    Body earth = simulator.findBody("earth").orElseThrow();
    assertEquals(2, simulator.elapsedPhysicsSeconds(), 0.000001);
    assertEquals(219.9184, earth.position().x(), 0.0001);
    assertEquals(6.0295, earth.position().y(), 0.0001);
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
  void speedMultiplierAcceptsOnlyConfiguredRange() {
    OrbitSimulator simulator = OrbitSimulator.defaults();

    assertThrows(IllegalArgumentException.class, () -> simulator.setSpeedMultiplier(0));
    assertThrows(IllegalArgumentException.class, () -> simulator.setSpeedMultiplier(21));
    simulator.setSpeedMultiplier(OrbitSimulator.MINIMUM_SPEED);
    assertEquals(1, simulator.speedMultiplier());
    simulator.setSpeedMultiplier(OrbitSimulator.MAXIMUM_SPEED);
    assertEquals(20, simulator.speedMultiplier());
  }

  @Test
  void bodyAtIncludesTheRenderedEdge() {
    OrbitSimulator simulator = OrbitSimulator.defaults();

    assertEquals("sun", simulator.bodyAt(new Vector2(36, 0)).orElseThrow().name());
    assertTrue(simulator.bodyAt(new Vector2(36.0001, 0)).isEmpty());
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
  void clickAtNearbyCenterBoundaryAddsBodyAroundThatCenter() {
    OrbitSimulator simulator = OrbitSimulator.defaults();

    Body body = simulator.addBodyFromClick(new Vector2(316, 0), 1);

    assertEquals("earth", body.orbitCenter());
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
  void draggingBodyWithoutStoredPeriapsisUsesCurrentDistance() {
    assertProbeApoapsisDrag(0, 10, 1.8257);
  }

  @Test
  void draggingBodyKeepsPositiveStoredPeriapsisEvenWhenItIsSmall() {
    assertProbeApoapsisDrag(1, 1, 0.6901);
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
  void collidingBodiesBounceInelasticallyAndRemainSeparate() {
    OrbitSimulator simulator = collisionSimulator(2, 7, -2);

    simulator.resolveCollisions(0.5);

    assertBodiesRemainWithVelocities(simulator, 0.5, 2.5);
  }

  @Test
  void tickResolvesCollisionsAfterUpdatingPositionsWithoutMerging() {
    OrbitSimulator simulator = collisionSimulator(3, 10, -3);

    simulator.tick(1, 0);

    assertBodiesRemainWithVelocities(simulator, 0.75, 3.75);
    Body alpha = simulator.findBody("alpha").orElseThrow();
    Body beta = simulator.findBody("beta").orElseThrow();
    assertEquals(2.25, alpha.position().x(), 0.000001);
    assertEquals(9.25, beta.position().x(), 0.000001);
    assertEquals(7, beta.position().minus(alpha.position()).magnitude(), 0.000001);
  }

  @Test
  void overlappingBodiesAreSeparatedUntilRenderedEdgesTouch() {
    OrbitSimulator simulator = collisionSimulator(0, 6, 0);

    simulator.resolveCollisions(0.5);

    Body alpha = simulator.findBody("alpha").orElseThrow();
    Body beta = simulator.findBody("beta").orElseThrow();
    assertEquals(2, simulator.bodyCount());
    assertEquals(7, beta.position().minus(alpha.position()).magnitude(), 0.000001);
    assertEquals(-0.25, alpha.position().x(), 0.000001);
    assertEquals(6.75, beta.position().x(), 0.000001);
    assertEquals(0, alpha.velocity().x(), 0.000001);
    assertEquals(0, beta.velocity().x(), 0.000001);
  }

  @Test
  void fullyOverlappedBodiesSeparateAlongDefaultNormalByInverseMass() {
    assertOverlapSeparation(
        collisionBody("alpha", 4, 3, new Vector2(0, 0), new Vector2(0, 0)),
        collisionBody("beta", 3, 1, new Vector2(0, 0), new Vector2(0, 0)),
        -1.75,
        5.25
    );
  }

  @Test
  void closeOverlappingBodiesStillReceiveInelasticImpulse() {
    OrbitSimulator simulator = collisionSimulator(
        collisionBody("alpha", 4, 3, new Vector2(0, 0), new Vector2(0, 0)),
        collisionBody("beta", 3, 1, new Vector2(0.5, 0), new Vector2(-2, 0))
    );

    simulator.resolveCollisions(0.5);

    assertXVelocities(simulator, -0.75, 0.25);
  }

  @Test
  void separatingOverlappingBodiesKeepTheirVelocities() {
    OrbitSimulator simulator = collisionSimulator(
        collisionBody("alpha", 4, 3, new Vector2(0, 0), new Vector2(0, 0)),
        collisionBody("beta", 3, 1, new Vector2(6, 0), new Vector2(0.5, 0))
    );

    simulator.resolveCollisions(0.5);

    assertXVelocities(simulator, 0, 0.5);
  }

  @Test
  void unequalMassesAffectBothSidesOfCollisionImpulse() {
    OrbitSimulator simulator = collisionSimulator(
        collisionBody("alpha", 4, 2, new Vector2(0, 0), new Vector2(2, 0)),
        collisionBody("beta", 3, 5, new Vector2(7, 0), new Vector2(-2, 0))
    );

    simulator.resolveCollisions(0.5);

    assertXVelocities(simulator, -2.285714, -0.285714);
  }

  @Test
  void unequalMassesAffectBothSidesOfOverlapSeparation() {
    assertOverlapSeparation(
        collisionBody("alpha", 4, 2, new Vector2(0, 0), new Vector2(0, 0)),
        collisionBody("beta", 3, 5, new Vector2(6, 0), new Vector2(0, 0)),
        -0.714286,
        6.285714
    );
  }

  @Test
  void verticalCollisionUsesVerticalNormalForImpulseAndSeparation() {
    OrbitSimulator simulator = collisionSimulator(
        collisionBody("alpha", 4, 3, new Vector2(0, 0), new Vector2(0, 2)),
        collisionBody("beta", 3, 1, new Vector2(0, 6), new Vector2(0, -2))
    );

    simulator.resolveCollisions(0.5);

    Body alpha = simulator.findBody("alpha").orElseThrow();
    Body beta = simulator.findBody("beta").orElseThrow();
    assertEquals(-0.25, alpha.position().y(), 0.000001);
    assertEquals(6.75, beta.position().y(), 0.000001);
    assertEquals(0.5, alpha.velocity().y(), 0.000001);
    assertEquals(2.5, beta.velocity().y(), 0.000001);
  }

  private static OrbitSimulator collisionSimulator(double alphaVelocityX, double betaX, double betaVelocityX) {
    return collisionSimulator(
        collisionBody("alpha", 4, 3, new Vector2(0, 0), new Vector2(alphaVelocityX, 0)),
        collisionBody("beta", 3, 1, new Vector2(betaX, 0), new Vector2(betaVelocityX, 0))
    );
  }

  private static OrbitSimulator collisionSimulator(Body alpha, Body beta) {
    return new OrbitSimulator(List.of(alpha, beta));
  }

  private static OrbitSimulator apoapsisDragSimulator(double storedPeriapsis) {
    return new OrbitSimulator(List.of(
        new Body("sun", "yellow", 36, 100, new Vector2(0, 0), new Vector2(0, 0)),
        new Body("probe", "gray", 1, 1, new Vector2(10, 0), new Vector2(0, 0), "sun", storedPeriapsis)
    ));
  }

  private static Body collisionBody(String name, double radius, double mass, Vector2 position, Vector2 velocity) {
    return new Body(name, name.equals("alpha") ? "blue" : "gray", radius, mass, position, velocity);
  }

  private static void assertProbeApoapsisDrag(double storedPeriapsis, double expectedPeriapsis, double expectedVelocityY) {
    OrbitSimulator simulator = apoapsisDragSimulator(storedPeriapsis);

    Body probe = simulator.dragBodyToApoapsis("probe", new Vector2(20, 0), 1);

    assertEquals(expectedPeriapsis, probe.periapsisDistance(), 0.000001);
    assertEquals(20, simulator.apoapsisDistance("probe"), 0.000001);
    assertEquals(expectedVelocityY, probe.velocity().y(), 0.0001);
  }

  private static void assertXPositionsAndDistance(
      OrbitSimulator simulator,
      double expectedAlphaX,
      double expectedBetaX,
      double expectedDistance
  ) {
    Body alpha = simulator.findBody("alpha").orElseThrow();
    Body beta = simulator.findBody("beta").orElseThrow();
    assertEquals(expectedAlphaX, alpha.position().x(), 0.000001);
    assertEquals(expectedBetaX, beta.position().x(), 0.000001);
    assertEquals(expectedDistance, beta.position().minus(alpha.position()).magnitude(), 0.000001);
  }

  private static void assertOverlapSeparation(
      Body alpha,
      Body beta,
      double expectedAlphaX,
      double expectedBetaX
  ) {
    OrbitSimulator simulator = collisionSimulator(alpha, beta);

    simulator.resolveCollisions(0.5);

    assertXPositionsAndDistance(simulator, expectedAlphaX, expectedBetaX, 7);
  }

  private static void assertXVelocities(
      OrbitSimulator simulator,
      double expectedAlphaVelocityX,
      double expectedBetaVelocityX
  ) {
    Body alpha = simulator.findBody("alpha").orElseThrow();
    Body beta = simulator.findBody("beta").orElseThrow();
    assertEquals(expectedAlphaVelocityX, alpha.velocity().x(), 0.000001);
    assertEquals(expectedBetaVelocityX, beta.velocity().x(), 0.000001);
  }

  private static void assertBodiesRemainWithVelocities(
      OrbitSimulator simulator,
      double alphaVelocityX,
      double betaVelocityX
  ) {
    Body alpha = simulator.findBody("alpha").orElseThrow();
    Body beta = simulator.findBody("beta").orElseThrow();
    assertEquals(2, simulator.bodyCount());
    assertEquals(alphaVelocityX, alpha.velocity().x(), 0.000001);
    assertEquals(betaVelocityX, beta.velocity().x(), 0.000001);
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

  @Test
  void bodyCanOrbitOnePixelFromItsCenter() {
    OrbitSimulator simulator = OrbitSimulator.defaults();

    Body body = simulator.addBodyInCircularOrbit(new Vector2(1, 0), "sun", 1);

    assertEquals("sun", body.orbitCenter());
    assertEquals(44.7214, body.velocity().y(), 0.0001);
  }

  @Test
  void velocityDragCanUpdateTheFirstBody() {
    OrbitSimulator simulator = OrbitSimulator.defaults();

    Body sun = simulator.setBodyVelocityFromDrag("sun", new Vector2(10, 0), 0.1);

    assertEquals(1, sun.velocity().x(), 0.000001);
    assertEquals(1, simulator.findBody("sun").orElseThrow().velocity().x(), 0.000001);
  }
}
