package orbit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrbitSimulator {
  public static final int MINIMUM_SPEED = 1;
  public static final int MAXIMUM_SPEED = 20;
  public static final int SPEED_STEP = 1;

  private final List<Body> bodies;
  private final List<Body> initialBodies;
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
    if (paused) {
      return;
    }
    elapsedPhysicsSeconds += seconds;
    List<Vector2> accelerations = Physics.accelerations(bodies, gravityConstant);
    for (int i = 0; i < bodies.size(); i++) {
      Body body = bodies.get(i);
      Vector2 velocity = body.velocity().plus(accelerations.get(i).times(seconds));
      Vector2 position = body.position().plus(velocity.times(seconds));
      bodies.set(i, body.withPositionAndVelocity(position, velocity));
    }
  }

  public void togglePause() {
    paused = !paused;
  }

  public void restart() {
    bodies.clear();
    bodies.addAll(initialBodies);
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
    tick(displaySeconds * speedMultiplier, gravityConstant);
  }
}
