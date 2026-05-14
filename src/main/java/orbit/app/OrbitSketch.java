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
  private static final int CONTROL_Y = 16;
  private static final int BUTTON_WIDTH = 92;
  private static final int BUTTON_HEIGHT = 32;
  private static final int SLIDER_X = 248;
  private static final int SLIDER_Y = 32;
  private static final int SLIDER_WIDTH = 220;
  private static final int SLIDER_HANDLE_RADIUS = 8;

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
    drawControls();
    translate(width / 2f, height / 2f);
    simulator.advanceDisplayTime(1.0 / 60.0, 1.0);
    for (Body body : simulator.bodies()) {
      fillFor(body.color());
      noStroke();
      float diameter = (float) body.radiusPixels() * 2f;
      ellipse((float) body.position().x(), (float) body.position().y(), diameter, diameter);
    }
  }

  @Override
  public void mousePressed() {
    if (insideButton(16, CONTROL_Y)) {
      simulator.togglePause();
    } else if (insideButton(124, CONTROL_Y)) {
      simulator.restart();
    } else if (insideSlider()) {
      simulator.setSpeedMultiplier(speedFromMouse());
    }
  }

  private void fillFor(String color) {
    PALETTE.getOrDefault(color, DEFAULT_PAINT).applyTo(this);
  }

  private void drawControls() {
    textSize(14);
    drawButton(16, CONTROL_Y, simulator.controlButtonLabel());
    drawButton(124, CONTROL_Y, "Restart");
    drawSpeedSlider();
  }

  private void drawButton(int x, int y, String label) {
    fill(245);
    stroke(80);
    rect(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, 4);
    fill(20);
    textAlign(CENTER, CENTER);
    text(label, x + BUTTON_WIDTH / 2f, y + BUTTON_HEIGHT / 2f);
  }

  private boolean insideButton(int x, int y) {
    return mouseX >= x && mouseX <= x + BUTTON_WIDTH && mouseY >= y && mouseY <= y + BUTTON_HEIGHT;
  }

  private void drawSpeedSlider() {
    stroke(180);
    line(SLIDER_X, SLIDER_Y, SLIDER_X + SLIDER_WIDTH, SLIDER_Y);
    float handleX = sliderPosition(simulator.speedMultiplier());
    fill(245);
    stroke(80);
    ellipse(handleX, SLIDER_Y, SLIDER_HANDLE_RADIUS * 2f, SLIDER_HANDLE_RADIUS * 2f);
    fill(245);
    textAlign(LEFT, CENTER);
    text(simulator.speedLabel(), SLIDER_X + SLIDER_WIDTH + 16, SLIDER_Y);
  }

  private float sliderPosition(int speed) {
    float range = OrbitSimulator.MAXIMUM_SPEED - OrbitSimulator.MINIMUM_SPEED;
    float fraction = (speed - OrbitSimulator.MINIMUM_SPEED) / range;
    return SLIDER_X + fraction * SLIDER_WIDTH;
  }

  private boolean insideSlider() {
    return mouseX >= SLIDER_X
        && mouseX <= SLIDER_X + SLIDER_WIDTH
        && Math.abs(mouseY - SLIDER_Y) <= SLIDER_HANDLE_RADIUS * 2;
  }

  private int speedFromMouse() {
    float fraction = constrain((mouseX - SLIDER_X) / (float) SLIDER_WIDTH, 0, 1);
    int range = OrbitSimulator.MAXIMUM_SPEED - OrbitSimulator.MINIMUM_SPEED;
    return OrbitSimulator.MINIMUM_SPEED + Math.round(fraction * range);
  }

  private record Paint(int red, int green, int blue) {
    void applyTo(PApplet sketch) {
      sketch.fill(red, green, blue);
    }
  }
}
