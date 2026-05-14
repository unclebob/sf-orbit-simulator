package orbit.acceptance;

import java.util.Map;

public interface StepHandlers {
  void handle(String stepText, World world, Map<String, String> example);
}
