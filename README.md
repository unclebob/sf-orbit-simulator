# Orbit Simulator

A small Java/Processing 2D orbit simulator. It starts with a sun, earth, and
moon, applies Newtonian gravity between every pair of bodies, and advances the
system with velocity Verlet integration using fixed physics substeps.

## Requirements

- Java 21
- Maven

## Run

```sh
./scripts/run.sh
```

This compiles the project and starts the Processing sketch.

## Test

Run the unit tests:

```sh
mvn test
```

Generate and run the Gherkin-backed acceptance tests:

```sh
./scripts/acceptance.sh
```

The acceptance script parses `features/orbit-simulator.feature`, writes an
intermediate JSON file under `build/acceptance`, generates JUnit tests under
`acceptance/generated`, and then runs the full Maven test suite.

## Acceptance Mutation Checks

To mutate the feature file values and run the generated acceptance checks:

```sh
./scripts/acceptance-mutate.sh
```

Additional arguments are passed through to `orbit.acceptance.GherkinMutator`.

## Controls

- `Pause` / `Resume`: stop or resume physics updates.
- `Restart`: restore the initial sun, earth, and moon state.
- `Center Sun`: move the viewport back to the sun.
- Speed slider: change simulated time from `1X` to `100X`.
- Zoom slider: zoom the viewport out.
- Click empty space: add a small body in circular orbit around the nearest
  orbit center.
- Drag an existing body: adjust its velocity.
- Mouse wheel: pan vertically.
- Shift + mouse wheel: pan horizontally.

## Project Layout

- `src/main/java/orbit`: core simulation model and physics.
- `src/main/java/orbit/app`: Processing UI sketch.
- `src/main/java/orbit/acceptance`: Gherkin parser, acceptance runtime, and
  generator.
- `src/test/java`: unit tests.
- `features`: executable acceptance specification.
- `scripts`: project run and acceptance helpers.
