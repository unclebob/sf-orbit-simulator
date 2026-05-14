package orbit;

public record Body(
    String name,
    String color,
    double radiusPixels,
    double mass,
    Vector2 position,
    Vector2 velocity
) {
  public Body withPositionAndVelocity(Vector2 newPosition, Vector2 newVelocity) {
    return new Body(name, color, radiusPixels, mass, newPosition, newVelocity);
  }
}
