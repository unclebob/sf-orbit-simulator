package orbit;

public record TidalDeformation(
    String bodyName,
    String sourceName,
    Vector2 center,
    double majorRadiusPixels,
    double minorRadiusPixels,
    Vector2 axisTowardSource,
    Vector2 firstFocus,
    Vector2 secondFocus
) {
  public static TidalDeformation calculate(Body body, Body source, double elasticity) {
    Vector2 sourceDirection = source.position().minus(body.position());
    double sourceDistance = sourceDirection.magnitude();
    if (sourceDistance == 0) {
      throw new IllegalArgumentException("tidal source cannot share body position");
    }
    Vector2 axis = sourceDirection.times(1.0 / sourceDistance);
    double deformation = Math.round(elasticity * source.mass() / sourceDistance);
    double majorRadius = body.radiusPixels() + deformation;
    double minorRadius = Math.max(0, body.radiusPixels() - deformation);
    double focusDistance = Math.sqrt(majorRadius * majorRadius - minorRadius * minorRadius);
    return new TidalDeformation(
        body.name(),
        source.name(),
        body.position(),
        majorRadius,
        minorRadius,
        axis,
        body.position().plus(axis.times(focusDistance)),
        body.position().minus(axis.times(focusDistance))
    );
  }
}
