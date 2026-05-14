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
}
