package orbit.acceptance;

import java.util.List;
import orbit.Body;
import orbit.OrbitSimulator;
import orbit.TidalDeformation;
import orbit.Vector2;

public class World {
  OrbitSimulator simulator;
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
  Body tidalBody;
  Body tidalSource;
  Body elasticSource;
  Body elasticTarget;
  TidalDeformation tidalDeformation;
  TidalDeformation weakerTidalDeformation;
  TidalDeformation strongerTidalDeformation;
  Vector2 elasticAcceleration;
}
