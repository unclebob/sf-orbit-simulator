package orbit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrbitSimulator {
  public static final int MINIMUM_SPEED = 1;
  public static final int MAXIMUM_SPEED = 20;
  public static final int SPEED_STEP = 1;
  public static final double DEFAULT_SUBSTEP_SECONDS = 1.0 / 60.0;
  private static final String ADDED_BODY_COLOR = "gray";
  private static final double ADDED_BODY_RADIUS = 4;
  private static final double ADDED_BODY_MASS = 1;
  private static final double DEFAULT_RESTITUTION = 0.5;

  private final List<Body> bodies;
  private final List<Body> initialBodies;
  private int addedBodyCount;
  private boolean paused;
  private int speedMultiplier = MINIMUM_SPEED;
  private double elapsedPhysicsSeconds;

  public OrbitSimulator(List<Body> bodies) {
    this.bodies = new ArrayList<>(bodies);
    this.initialBodies = new ArrayList<>(bodies);
  }

  public static OrbitSimulator defaults() {
    return new OrbitSimulator(List.of(
        new Body("sun", "yellow", 36, 2000, new Vector2(0, 0), new Vector2(0, 0)),
        new Body("earth", "blue", 12, 100, new Vector2(220, 0), new Vector2(0, 3.0151), "sun", 220),
        new Body("moon", "gray", 4, 1, new Vector2(264, 0), new Vector2(0, 4.5227), "earth", 44)
    ));
  }

  public List<Body> bodies() {
    return List.copyOf(bodies);
  }

  public Optional<Body> findBody(String name) {
    return bodies.stream().filter(body -> body.name().equals(name)).findFirst();
  }

  public Optional<Body> bodyAt(Vector2 position) {
    return bodies.stream()
        .filter(body -> position.minus(body.position()).magnitude() <= body.radiusPixels())
        .findFirst();
  }

  public int bodyCount() {
    return bodies.size();
  }

  public Body addBodyInCircularOrbit(Vector2 position, String centerName, double gravityConstant) {
    Body center = requireBody(centerName);
    Vector2 velocity = circularOrbitVelocity(position, center, gravityConstant);
    Body body = new Body(
        "body_" + (++addedBodyCount),
        ADDED_BODY_COLOR,
        ADDED_BODY_RADIUS,
        ADDED_BODY_MASS,
        position,
        velocity,
        centerName,
        position.minus(center.position()).magnitude()
    );
    bodies.add(body);
    return body;
  }

  public Body addBodyFromClick(Vector2 position, double gravityConstant) {
    String center = nearbyOrbitCenter(position).map(Body::name).orElse("sun");
    return addBodyInCircularOrbit(position, center, gravityConstant);
  }

  public Body dragBodyToApoapsis(String bodyName, Vector2 apoapsisPosition, double gravityConstant) {
    Body body = requireBody(bodyName);
    String centerName = body.orbitCenter().isBlank() ? "sun" : body.orbitCenter();
    Body center = requireBody(centerName);
    double periapsis = body.periapsisDistance() > 0
        ? body.periapsisDistance()
        : body.position().minus(center.position()).magnitude();
    Vector2 velocity = ellipticalApoapsisVelocity(apoapsisPosition, center, periapsis, gravityConstant);
    Body updated = body.withOrbitState(apoapsisPosition, velocity, centerName, periapsis);
    replaceBody(updated);
    return updated;
  }

  public Body setBodyVelocityFromDrag(String bodyName, Vector2 mousePosition, double velocityScale) {
    Body body = requireBody(bodyName);
    Vector2 velocityDelta = mousePosition.minus(body.position()).times(velocityScale);
    Body updated = body.withPositionAndVelocity(body.position(), body.velocity().plus(velocityDelta));
    replaceBody(updated);
    return updated;
  }

  public double apoapsisDistance(String bodyName) {
    Body body = requireBody(bodyName);
    return body.position().minus(requireBody(body.orbitCenter()).position()).magnitude();
  }

  public void tick(double seconds, double gravityConstant) {
    tick(seconds, gravityConstant, DEFAULT_SUBSTEP_SECONDS);
  }

  public void tick(double seconds, double gravityConstant, double substepSeconds) {
    if (paused) {
      return;
    }
    elapsedPhysicsSeconds += seconds;
    int substeps = Math.max(1, (int) Math.round(seconds / substepSeconds));
    double stepSeconds = seconds / substeps;
    for (int step = 0; step < substeps; step++) {
      velocityVerletStep(stepSeconds, gravityConstant);
    }
    resolveCollisions();
  }

  private void velocityVerletStep(double seconds, double gravityConstant) {
    List<Vector2> startingAccelerations = Physics.accelerations(bodies, gravityConstant);
    for (int i = 0; i < bodies.size(); i++) {
      Body body = bodies.get(i);
      Vector2 position = body.position()
          .plus(body.velocity().times(seconds))
          .plus(startingAccelerations.get(i).times(0.5 * seconds * seconds));
      bodies.set(i, body.withPositionAndVelocity(position, body.velocity()));
    }
    List<Vector2> endingAccelerations = Physics.accelerations(bodies, gravityConstant);
    for (int i = 0; i < bodies.size(); i++) {
      Body body = bodies.get(i);
      Vector2 velocity = body.velocity()
          .plus(startingAccelerations.get(i).plus(endingAccelerations.get(i)).times(0.5 * seconds));
      bodies.set(i, body.withPositionAndVelocity(body.position(), velocity));
    }
  }

  public void resolveCollisions() {
    resolveCollisions(DEFAULT_RESTITUTION);
  }

  public void resolveCollisions(double restitution) {
    for (int first = 0; first < bodies.size(); first++) {
      for (int second = first + 1; second < bodies.size(); second++) {
        if (areColliding(bodies.get(first), bodies.get(second))) {
          collide(first, second, restitution);
        }
      }
    }
  }

  public void togglePause() {
    paused = !paused;
  }

  public void restart() {
    bodies.clear();
    bodies.addAll(initialBodies);
    addedBodyCount = 0;
    paused = false;
    speedMultiplier = MINIMUM_SPEED;
    elapsedPhysicsSeconds = 0;
  }

  public boolean isPaused() {
    return paused;
  }

  public boolean isRunning() {
    return !paused;
  }

  public String controlButtonLabel() {
    return paused ? "Resume" : "Pause";
  }

  public void setSpeedMultiplier(int speedMultiplier) {
    if (speedMultiplier < MINIMUM_SPEED || speedMultiplier > MAXIMUM_SPEED) {
      throw new IllegalArgumentException("speed multiplier out of range: " + speedMultiplier);
    }
    this.speedMultiplier = speedMultiplier;
  }

  public int speedMultiplier() {
    return speedMultiplier;
  }

  public String speedLabel() {
    return speedMultiplier + "X";
  }

  public double elapsedPhysicsSeconds() {
    return elapsedPhysicsSeconds;
  }

  public void advanceDisplayTime(double displaySeconds, double gravityConstant) {
    advanceDisplayTime(displaySeconds, gravityConstant, DEFAULT_SUBSTEP_SECONDS);
  }

  public void advanceDisplayTime(double displaySeconds, double gravityConstant, double substepSeconds) {
    tick(displaySeconds * speedMultiplier, gravityConstant, substepSeconds);
  }

  private Optional<Body> nearbyOrbitCenter(Vector2 position) {
    return bodies.stream()
        .filter(body -> position.minus(body.position()).magnitude() <= body.radiusPixels() * 2 * 4)
        .findFirst();
  }

  private boolean areColliding(Body first, Body second) {
    double distance = first.position().minus(second.position()).magnitude();
    return distance <= first.radiusPixels() + second.radiusPixels();
  }

  private void collide(int firstIndex, int secondIndex, double restitution) {
    Body first = bodies.get(firstIndex);
    Body second = bodies.get(secondIndex);
    Vector2 unitNormal = collisionNormal(first, second);
    separateOverlap(firstIndex, secondIndex, first, second, unitNormal, overlap(first, second));
    applyInelasticImpulse(firstIndex, secondIndex, restitution);
  }

  private Vector2 collisionNormal(Body first, Body second) {
    Vector2 normal = second.position().minus(first.position());
    double distance = normal.magnitude();
    return distance == 0 ? new Vector2(1, 0) : normal.times(1.0 / distance);
  }

  private double overlap(Body first, Body second) {
    double distance = first.position().minus(second.position()).magnitude();
    return first.radiusPixels() + second.radiusPixels() - distance;
  }

  private void applyInelasticImpulse(int firstIndex, int secondIndex, double restitution) {
    Body first = bodies.get(firstIndex);
    Body second = bodies.get(secondIndex);
    Vector2 normal = second.position().minus(first.position());
    double distance = normal.magnitude();
    if (distance == 0) {
      return;
    }
    Vector2 unitNormal = normal.times(1.0 / distance);
    Vector2 relativeVelocity = second.velocity().minus(first.velocity());
    double velocityAlongNormal = relativeVelocity.x() * unitNormal.x() + relativeVelocity.y() * unitNormal.y();
    if (velocityAlongNormal >= 0) {
      return;
    }
    double impulse = -(1.0 + restitution) * velocityAlongNormal / (1.0 / first.mass() + 1.0 / second.mass());
    Vector2 impulseVector = unitNormal.times(impulse);
    Body updatedFirst = first.withPositionAndVelocity(first.position(), first.velocity().minus(impulseVector.times(1.0 / first.mass())));
    Body updatedSecond = second.withPositionAndVelocity(second.position(), second.velocity().plus(impulseVector.times(1.0 / second.mass())));
    bodies.set(firstIndex, updatedFirst);
    bodies.set(secondIndex, updatedSecond);
  }

  private void separateOverlap(
      int firstIndex,
      int secondIndex,
      Body first,
      Body second,
      Vector2 unitNormal,
      double overlap
  ) {
    if (overlap <= 0) {
      return;
    }
    double inverseMassSum = 1.0 / first.mass() + 1.0 / second.mass();
    Vector2 firstAdjustment = unitNormal.times(-overlap * (1.0 / first.mass()) / inverseMassSum);
    Vector2 secondAdjustment = unitNormal.times(overlap * (1.0 / second.mass()) / inverseMassSum);
    bodies.set(firstIndex, first.withPositionAndVelocity(first.position().plus(firstAdjustment), first.velocity()));
    bodies.set(secondIndex, second.withPositionAndVelocity(second.position().plus(secondAdjustment), second.velocity()));
  }

  private Vector2 circularOrbitVelocity(Vector2 position, Body center, double gravityConstant) {
    Vector2 relativePosition = position.minus(center.position());
    double distance = relativePosition.magnitude();
    if (distance == 0) {
      throw new IllegalArgumentException("orbiting body cannot start at its center");
    }
    double speed = Math.sqrt(gravityConstant * center.mass() / distance);
    return center.velocity().plus(tangent(relativePosition).times(speed));
  }

  private Vector2 ellipticalApoapsisVelocity(
      Vector2 apoapsisPosition,
      Body center,
      double periapsis,
      double gravityConstant
  ) {
    Vector2 relativePosition = apoapsisPosition.minus(center.position());
    double apoapsis = relativePosition.magnitude();
    double semiMajorAxis = (periapsis + apoapsis) / 2.0;
    double speed = Math.sqrt(gravityConstant * center.mass() * (2.0 / apoapsis - 1.0 / semiMajorAxis));
    return center.velocity().plus(tangent(relativePosition).times(speed));
  }

  private Vector2 tangent(Vector2 relativePosition) {
    double distance = relativePosition.magnitude();
    return new Vector2(-relativePosition.y() / distance, relativePosition.x() / distance);
  }

  private Body requireBody(String name) {
    return findBody(name).orElseThrow(() -> new IllegalArgumentException("missing body: " + name));
  }

  private void replaceBody(Body updated) {
    for (int i = 0; i < bodies.size(); i++) {
      if (bodies.get(i).name().equals(updated.name())) {
        bodies.set(i, updated);
        return;
      }
    }
    throw new IllegalArgumentException("missing body: " + updated.name());
  }
}
