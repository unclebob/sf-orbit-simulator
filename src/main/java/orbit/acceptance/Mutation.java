package orbit.acceptance;

public record Mutation(String id, String path, String original, String mutated) {
  public String description() {
    return path + ": " + original + " -> " + mutated;
  }
}
