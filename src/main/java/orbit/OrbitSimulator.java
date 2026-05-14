package orbit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrbitSimulator {
  private final List<Body> bodies;

  public OrbitSimulator(List<Body> bodies) {
    this.bodies = new ArrayList<>(bodies);
  }

  public static OrbitSimulator defaults() {
    return new OrbitSimulator(List.of(
        new Body("sun", "yellow", 36, 2000, new Vector2(0, 0), new Vector2(0, 0)),
        new Body("earth", "blue", 12, 100, new Vector2(220, 0), new Vector2(0, 3.0151)),
        new Body("moon", "gray", 4, 1, new Vector2(264, 0), new Vector2(0, 4.5227))
    ));
  }

  public List<Body> bodies() {
    return List.copyOf(bodies);
  }

  public Optional<Body> findBody(String name) {
    return bodies.stream().filter(body -> body.name().equals(name)).findFirst();
  }

  public void tick(double seconds, double gravityConstant) {
    List<Vector2> accelerations = Physics.accelerations(bodies, gravityConstant);
    for (int i = 0; i < bodies.size(); i++) {
      Body body = bodies.get(i);
      Vector2 velocity = body.velocity().plus(accelerations.get(i).times(seconds));
      Vector2 position = body.position().plus(velocity.times(seconds));
      bodies.set(i, body.withPositionAndVelocity(position, velocity));
    }
  }
}
