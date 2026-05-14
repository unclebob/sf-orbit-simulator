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
}
