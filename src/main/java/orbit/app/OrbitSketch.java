package orbit.app;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import orbit.Body;
import orbit.OrbitSimulator;
import orbit.Vector2;
import processing.core.PApplet;
import processing.event.MouseEvent;

public class OrbitSketch extends PApplet {
  private static final double BODY_DRAG_VELOCITY_SCALE = 0.01;
  private static final double WHEEL_SCROLL_SCALE = 20;
  private static final Map<String, Paint> PALETTE = Map.of(
      "yellow", new Paint(255, 214, 64),
      "blue", new Paint(70, 130, 255),
      "gray", new Paint(180, 180, 180)
  );
  private static final Paint DEFAULT_PAINT = new Paint(255, 255, 255);
  private static final int CONTROL_Y = 16;
  private static final int BUTTON_WIDTH = 100;
  private static final int BUTTON_HEIGHT = 32;
  private static final int SLIDER_X = 372;
  private static final int SLIDER_Y = 32;
  private static final int SLIDER_WIDTH = 220;
  private static final int SLIDER_HANDLE_RADIUS = 8;
  private static final Button PAUSE_BUTTON = new Button(16, CONTROL_Y, ControlAction.PAUSE);
  private static final Button RESTART_BUTTON = new Button(128, CONTROL_Y, ControlAction.RESTART);
  private static final Button CENTER_SUN_BUTTON = new Button(240, CONTROL_Y, ControlAction.CENTER_SUN);
  private static final Slider SPEED_SLIDER = new Slider(SLIDER_X, SLIDER_Y, SLIDER_WIDTH, SLIDER_HANDLE_RADIUS);
  private static final List<Button> BUTTONS = List.of(PAUSE_BUTTON, RESTART_BUTTON, CENTER_SUN_BUTTON);

  private OrbitSimulator simulator;
  private Vector2 viewCenter = new Vector2(0, 0);
  private String draggedBodyName;
  private Vector2 dragEnd;
  private DragAction dragAction = DragAction.NONE;

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
    translate(width / 2f - (float) viewCenter.x(), height / 2f - (float) viewCenter.y());
    simulator.advanceDisplayTime(1.0 / 60.0, 1.0);
    for (Body body : simulator.bodies()) {
      fillFor(body.color());
      noStroke();
      float diameter = (float) body.radiusPixels() * 2f;
      ellipse((float) body.position().x(), (float) body.position().y(), diameter, diameter);
    }
    drawVelocityPreview();
  }

  @Override
  public void mousePressed() {
    if (SPEED_SLIDER.contains(mouseX, mouseY)) {
      dragAction = DragAction.SPEED;
      setSpeedFromMouse();
      return;
    }
    BUTTONS.stream()
        .filter(button -> button.contains(mouseX, mouseY))
        .findFirst()
        .ifPresentOrElse(button -> button.press(this), this::pressOrbitArea);
  }

  @Override
  public void mouseDragged() {
    dragAction.drag(this);
  }

  @Override
  public void mouseReleased() {
    dragAction.release(this);
    clearDrag();
  }

  @Override
  public void mouseWheel(MouseEvent event) {
    adjustViewCenter(wheelScroll(event), WHEEL_SCROLL_SCALE);
  }

  private void pressOrbitArea() {
    simulator.bodyAt(worldMouse())
        .ifPresentOrElse(this::startBodyDrag, () -> simulator.addBodyFromClick(worldMouse(), 1.0));
  }

  private void startBodyDrag(Body body) {
    draggedBodyName = body.name();
    dragEnd = null;
    dragAction = DragAction.BODY;
  }

  private void dragBody() {
    dragEnd = worldMouse();
  }

  private void releaseBodyDrag() {
    Optional.ofNullable(draggedBodyName)
        .filter(name -> dragEnd != null)
        .ifPresent(name -> simulator.setBodyVelocityFromDrag(name, dragEnd, BODY_DRAG_VELOCITY_SCALE));
  }

  private void clearDrag() {
    draggedBodyName = null;
    dragEnd = null;
    dragAction = DragAction.NONE;
  }

  private void setSpeedFromMouse() {
    simulator.setSpeedMultiplier(SPEED_SLIDER.speedFrom(mouseX, this));
  }

  private orbit.Vector2 worldMouse() {
    return new orbit.Vector2(mouseX - width / 2.0, mouseY - height / 2.0).plus(viewCenter);
  }

  private void adjustViewCenter(Vector2 scroll, double scrollScale) {
    viewCenter = viewCenter.plus(scroll.times(scrollScale));
  }

  private void centerViewOnSun() {
    simulator.findBody("sun").ifPresent(sun -> viewCenter = sun.position());
  }

  private Vector2 wheelScroll(MouseEvent event) {
    if (event.isShiftDown()) {
      return new Vector2(event.getCount(), 0);
    }
    return new Vector2(0, event.getCount());
  }

  private void drawVelocityPreview() {
    bodyDragPreview().ifPresent(body -> {
      stroke(245);
      line((float) body.position().x(), (float) body.position().y(), (float) dragEnd.x(), (float) dragEnd.y());
    });
  }

  private Optional<Body> bodyDragPreview() {
    return Optional.ofNullable(draggedBodyName)
        .filter(name -> dragEnd != null)
        .flatMap(simulator::findBody);
  }

  private void fillFor(String color) {
    PALETTE.getOrDefault(color, DEFAULT_PAINT).applyTo(this);
  }

  private void drawControls() {
    textSize(14);
    drawButton(PAUSE_BUTTON, simulator.controlButtonLabel());
    drawButton(RESTART_BUTTON, "Restart");
    drawButton(CENTER_SUN_BUTTON, "Center Sun");
    drawSpeedSlider();
  }

  private void drawButton(Button button, String label) {
    fill(245);
    stroke(80);
    rect(button.x, button.y, BUTTON_WIDTH, BUTTON_HEIGHT, 4);
    fill(20);
    textAlign(CENTER, CENTER);
    text(label, button.centerX(), button.centerY());
  }

  private void drawSpeedSlider() {
    stroke(180);
    line(SPEED_SLIDER.x, SPEED_SLIDER.y, SPEED_SLIDER.endX(), SPEED_SLIDER.y);
    fill(245);
    stroke(80);
    ellipse(SPEED_SLIDER.positionFor(simulator.speedMultiplier()), SPEED_SLIDER.y, SPEED_SLIDER.diameter(), SPEED_SLIDER.diameter());
    fill(245);
    textAlign(LEFT, CENTER);
    text(simulator.speedLabel(), SPEED_SLIDER.labelX(), SPEED_SLIDER.y);
  }

  private record Paint(int red, int green, int blue) {
    void applyTo(PApplet sketch) {
      sketch.fill(red, green, blue);
    }
  }

  private record Button(int x, int y, ControlAction action) {
    void press(OrbitSketch sketch) {
      action.applyTo(sketch);
    }

    boolean contains(int pointX, int pointY) {
      return inRange(pointX, x, x + BUTTON_WIDTH) && inRange(pointY, y, y + BUTTON_HEIGHT);
    }

    private boolean inRange(int value, int minimum, int maximum) {
      return value >= minimum && value <= maximum;
    }

    float centerX() {
      return x + BUTTON_WIDTH / 2f;
    }

    float centerY() {
      return y + BUTTON_HEIGHT / 2f;
    }
  }

  private record Slider(int x, int y, int width, int handleRadius) {
    boolean contains(int pointX, int pointY) {
      return pointX >= x && pointX <= endX() && Math.abs(pointY - y) <= handleRadius * 2;
    }

    int speedFrom(int mouseX, PApplet sketch) {
      float fraction = sketch.constrain((mouseX - x) / (float) width, 0, 1);
      int range = OrbitSimulator.MAXIMUM_SPEED - OrbitSimulator.MINIMUM_SPEED;
      return OrbitSimulator.MINIMUM_SPEED + Math.round(fraction * range);
    }

    float positionFor(int speed) {
      float range = OrbitSimulator.MAXIMUM_SPEED - OrbitSimulator.MINIMUM_SPEED;
      float fraction = (speed - OrbitSimulator.MINIMUM_SPEED) / range;
      return x + fraction * width;
    }

    int endX() {
      return x + width;
    }

    int diameter() {
      return handleRadius * 2;
    }

    int labelX() {
      return endX() + 16;
    }
  }

  private enum ControlAction {
    PAUSE {
      @Override
      void applyTo(OrbitSketch sketch) {
        sketch.simulator.togglePause();
      }
    },
    RESTART {
      @Override
      void applyTo(OrbitSketch sketch) {
        sketch.simulator.restart();
      }
    },
    CENTER_SUN {
      @Override
      void applyTo(OrbitSketch sketch) {
        sketch.centerViewOnSun();
      }
    };

    abstract void applyTo(OrbitSketch sketch);
  }

  private enum DragAction {
    NONE {
      @Override
      void drag(OrbitSketch sketch) {
      }

      @Override
      void release(OrbitSketch sketch) {
      }
    },
    SPEED {
      @Override
      void drag(OrbitSketch sketch) {
        sketch.setSpeedFromMouse();
      }

      @Override
      void release(OrbitSketch sketch) {
      }
    },
    BODY {
      @Override
      void drag(OrbitSketch sketch) {
        sketch.dragBody();
      }

      @Override
      void release(OrbitSketch sketch) {
        sketch.releaseBodyDrag();
      }
    };

    abstract void drag(OrbitSketch sketch);

    abstract void release(OrbitSketch sketch);
  }
}
