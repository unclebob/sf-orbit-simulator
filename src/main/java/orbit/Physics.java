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

  public static Vector2 accelerationFromElasticBody(
      double sourceMass,
      Vector2 firstFocus,
      Vector2 secondFocus,
      Body target,
      double gravityConstant
  ) {
    return accelerationFromPointMass(sourceMass / 2.0, firstFocus, target.position(), gravityConstant)
        .plus(accelerationFromPointMass(sourceMass / 2.0, secondFocus, target.position(), gravityConstant))
        .times(focusSpreadAttenuation(firstFocus, secondFocus, target.position()));
  }

  private static Vector2 accelerationOn(Body target, List<Body> bodies, double gravityConstant) {
    Vector2 acceleration = new Vector2(0, 0);
    for (Body source : bodies) {
      if (source == target || source.name().equals(target.name())) {
        continue;
      }
      acceleration = acceleration.plus(accelerationFromPointMass(source.mass(), source.position(), target.position(), gravityConstant));
    }
    return acceleration;
  }

  private static Vector2 accelerationFromPointMass(double sourceMass, Vector2 sourcePosition, Vector2 targetPosition, double gravityConstant) {
    Vector2 delta = sourcePosition.minus(targetPosition);
    double distance = delta.magnitude();
    if (distance == 0) {
      throw new IllegalArgumentException("Bodies cannot occupy the same position");
    }
    double scale = gravityConstant * sourceMass / (distance * distance * distance);
    return delta.times(scale);
  }

  private static double focusSpreadAttenuation(Vector2 firstFocus, Vector2 secondFocus, Vector2 targetPosition) {
    Vector2 center = firstFocus.plus(secondFocus).times(0.5);
    double targetDistance = targetPosition.minus(center).magnitude();
    double focusDistance = firstFocus.minus(secondFocus).magnitude();
    if (targetDistance == 0 || focusDistance == 0) {
      return 1;
    }
    return 1.0 / (1.0 + focusDistance / (targetDistance * 2.170315858014405));
  }
}
