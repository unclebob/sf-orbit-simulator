package orbit;

import java.util.ArrayList;
import java.util.List;

public final class Physics {
  private Physics() {
  }

  public static List<Vector2> accelerations(List<Body> bodies, double gravityConstant) {
    List<Vector2> accelerations = new ArrayList<>();
    for (Body body : bodies) {
      accelerations.add(accelerationOn(body, bodies, gravityConstant));
    }
    return accelerations;
  }

  private static Vector2 accelerationOn(Body target, List<Body> bodies, double gravityConstant) {
    Vector2 acceleration = new Vector2(0, 0);
    for (Body source : bodies) {
      if (source == target || source.name().equals(target.name())) {
        continue;
      }
      Vector2 delta = source.position().minus(target.position());
      double distance = delta.magnitude();
      if (distance == 0) {
        throw new IllegalArgumentException("Bodies cannot occupy the same position");
      }
      double scale = gravityConstant * source.mass() / (distance * distance * distance);
      acceleration = acceleration.plus(delta.times(scale));
    }
    return acceleration;
  }
}
