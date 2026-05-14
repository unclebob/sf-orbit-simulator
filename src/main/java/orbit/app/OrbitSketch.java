package orbit.app;

import java.util.Map;
import orbit.Body;
import orbit.OrbitSimulator;
import processing.core.PApplet;

public class OrbitSketch extends PApplet {
  private static final Map<String, Paint> PALETTE = Map.of(
      "yellow", new Paint(255, 214, 64),
      "blue", new Paint(70, 130, 255),
      "gray", new Paint(180, 180, 180)
  );
  private static final Paint DEFAULT_PAINT = new Paint(255, 255, 255);

  private OrbitSimulator simulator;

  public static void main(String[] args) {
    PApplet.main(OrbitSketch.class);
  }

  @Override
  public void settings() {
    size(800, 600);
  }

  @Override
  public void setup() {
    simulator = OrbitSimulator.defaults();
  }

  @Override
  public void draw() {
    background(10, 12, 18);
    translate(width / 2f, height / 2f);
    simulator.tick(1.0 / 60.0, 1.0);
    for (Body body : simulator.bodies()) {
      fillFor(body.color());
      noStroke();
      float diameter = (float) body.radiusPixels() * 2f;
      ellipse((float) body.position().x(), (float) body.position().y(), diameter, diameter);
    }
  }

  private void fillFor(String color) {
    PALETTE.getOrDefault(color, DEFAULT_PAINT).applyTo(this);
  }

  private record Paint(int red, int green, int blue) {
    void applyTo(PApplet sketch) {
      sketch.fill(red, green, blue);
    }
  }
}
