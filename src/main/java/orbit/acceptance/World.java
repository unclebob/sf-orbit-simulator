package orbit.acceptance;

import java.util.List;
import orbit.Body;
import orbit.OrbitSimulator;
import orbit.Vector2;

public class World {
  OrbitSimulator simulator;
  OrbitSimulator otherSimulator;
  List<Body> bodies;
  List<Vector2> accelerations;
  Body addedBody;
  Vector2 viewCenter;
  boolean viewCenterSet;
  String draggedBodyName;
  Vector2 dragStart;
  Vector2 dragEnd;
  Body firstCollisionBody;
  Body secondCollisionBody;
}
