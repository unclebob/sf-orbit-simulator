package orbit;

public record Vector2(double x, double y) {
  public Vector2 plus(Vector2 other) {
    return new Vector2(x + other.x, y + other.y);
  }

  public Vector2 minus(Vector2 other) {
    return new Vector2(x - other.x, y - other.y);
  }

  public Vector2 times(double scalar) {
    return new Vector2(x * scalar, y * scalar);
  }

  public double magnitude() {
    return Math.hypot(x, y);
  }
}
