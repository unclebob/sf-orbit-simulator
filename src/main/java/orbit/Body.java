package orbit;

public record Body(
    String name,
    String color,
    double radiusPixels,
    double mass,
    Vector2 position,
    Vector2 velocity,
    String orbitCenter,
    double periapsisDistance
) {
  public Body(String name, String color, double radiusPixels, double mass, Vector2 position, Vector2 velocity) {
    this(name, color, radiusPixels, mass, position, velocity, "", 0);
  }

  public Body withPositionAndVelocity(Vector2 newPosition, Vector2 newVelocity) {
    return new Body(name, color, radiusPixels, mass, newPosition, newVelocity, orbitCenter, periapsisDistance);
  }

  public Body withOrbit(String center, double periapsis) {
    return new Body(name, color, radiusPixels, mass, position, velocity, center, periapsis);
  }

  public Body withOrbitState(Vector2 newPosition, Vector2 newVelocity, String center, double periapsis) {
    return new Body(name, color, radiusPixels, mass, newPosition, newVelocity, center, periapsis);
  }
}
