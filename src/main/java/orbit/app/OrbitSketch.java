package orbit.app;

import orbit.Body;
import orbit.OrbitSimulator;
import processing.core.PApplet;

public class OrbitSketch extends PApplet {
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
    switch (color) {
      case "yellow" -> fill(255, 214, 64);
      case "blue" -> fill(70, 130, 255);
      case "gray" -> fill(180);
      default -> fill(255);
    }
  }
}
