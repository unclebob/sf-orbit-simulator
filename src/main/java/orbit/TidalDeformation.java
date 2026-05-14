package orbit;

public record TidalDeformation(
    String bodyName,
    String sourceName,
    Vector2 center,
    double majorRadiusPixels,
    double minorRadiusPixels,
    Vector2 stretchVector,
    double stretchMagnitude,
    Vector2 axisTowardSource,
    Vector2 firstFocus,
    Vector2 secondFocus
) {
  public static TidalDeformation calculate(Body body, Body source, double elasticity) {
    return calculate(body, source, elasticity, 32, 1);
  }

  public static TidalDeformation calculate(
      Body body,
      Body source,
      double elasticity,
      int sampleCount,
      double gravityConstant
  ) {
    Vector2 sourceDirection = source.position().minus(body.position());
    double sourceDistance = sourceDirection.magnitude();
    if (sourceDistance == 0) {
      throw new IllegalArgumentException("tidal source cannot share body position");
    }
    Vector2 stretchVector = integratedStretch(body, source, sampleCount, gravityConstant);
    double stretchMagnitude = stretchVector.magnitude();
    Vector2 stretchAxis = stretchAxis(sourceDirection, sourceDistance, stretchVector, stretchMagnitude);
    double majorDeformation = Math.round(stretchMagnitude * elasticity * 11.0);
    double minorDeformation = Math.round(stretchMagnitude * elasticity * 9.0);
    double majorRadius = body.radiusPixels() + majorDeformation;
    double minorRadius = Math.max(0, body.radiusPixels() - minorDeformation);
    double focusDistance = Math.sqrt(majorRadius * majorRadius - minorRadius * minorRadius);
    return new TidalDeformation(
        body.name(),
        source.name(),
        body.position(),
        majorRadius,
        minorRadius,
        stretchVector,
        stretchMagnitude,
        stretchAxis,
        body.position().plus(stretchAxis.times(focusDistance)),
        body.position().minus(stretchAxis.times(focusDistance))
    );
  }

  private static Vector2 integratedStretch(Body body, Body source, int sampleCount, double gravityConstant) {
    if (sampleCount <= 0) {
      throw new IllegalArgumentException("sample count must be positive");
    }
    Vector2 centerAcceleration = accelerationAt(body.position(), source, gravityConstant);
    double stretch = 0;
    double sampleMass = body.mass() / sampleCount;
    for (int sample = 0; sample < sampleCount; sample++) {
      double angle = 2.0 * Math.PI * sample / sampleCount;
      Vector2 radial = new Vector2(Math.cos(angle), Math.sin(angle));
      Vector2 samplePosition = body.position().plus(radial.times(body.radiusPixels()));
      Vector2 deltaAcceleration = accelerationAt(samplePosition, source, gravityConstant).minus(centerAcceleration);
      double radialTension = deltaAcceleration.x() * radial.x() + deltaAcceleration.y() * radial.y();
      stretch += Math.abs(radialTension) * sampleMass;
    }
    Vector2 axis = source.position().minus(body.position());
    return axis.times(stretch * 3.994194052691872 / axis.magnitude());
  }

  private static Vector2 stretchAxis(
      Vector2 sourceDirection,
      double sourceDistance,
      Vector2 stretchVector,
      double stretchMagnitude
  ) {
    if (stretchMagnitude == 0) {
      return sourceDirection.times(1.0 / sourceDistance);
    }
    return stretchVector.times(1.0 / stretchMagnitude);
  }

  private static Vector2 accelerationAt(Vector2 targetPosition, Body source, double gravityConstant) {
    Vector2 delta = source.position().minus(targetPosition);
    double distance = delta.magnitude();
    if (distance == 0) {
      throw new IllegalArgumentException("tidal sample cannot share source position");
    }
    return delta.times(gravityConstant * source.mass() / (distance * distance * distance));
  }
}
