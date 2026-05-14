Feature: 2D orbit simulator

  The simulator shows a sun, an earth, and a moon in a two-dimensional
  scene. Each body has mass, position, and velocity, and every physics update
  uses Newton's law of gravity between every pair of bodies.

  Background:
    Given the orbit simulator is opened

  Scenario Outline: Default bodies are visible with physical state
    Then the body <body> is visible with color <color>, radius <radius_px>, mass <mass>, position <x>, <y>, and velocity <vx>, <vy>

    Examples:
      | body  | color  | radius_px | mass | x   | y | vx | vy     |
      | sun   | yellow | 36        | 2000 | 0   | 0 | 0  | 0      |
      | earth | blue   | 12        | 100  | 220 | 0 | 0  | 3.0151 |
      | moon  | gray   | 4         | 1    | 264 | 0 | 0  | 4.5227 |

  Scenario Outline: Default bodies are arranged as nested orbits
    Then the body <orbiter> starts <distance> units from <center>

    Examples:
      | orbiter | center | distance |
      | earth   | sun    | 220      |
      | moon    | earth  | 44       |

  Scenario Outline: Gravity is applied between every pair of bodies
    Given a body <first_body> has mass <first_mass>, position <first_x>, <first_y>, and velocity <first_vx>, <first_vy>
    And a body <second_body> has mass <second_mass>, position <second_x>, <second_y>, and velocity <second_vx>, <second_vy>
    When gravitational acceleration is calculated using gravity constant <gravity_constant>
    Then the acceleration of <first_body> is <first_ax>, <first_ay>
    And the acceleration of <second_body> is <second_ax>, <second_ay>

    Examples:
      | first_body | first_mass | first_x | first_y | first_vx | first_vy | second_body | second_mass | second_x | second_y | second_vx | second_vy | gravity_constant | first_ax | first_ay | second_ax | second_ay |
      | sun        | 2000       | 0       | 0       | 0        | 0        | earth       | 100         | 220      | 0        | 0         | 3.0151    | 1                | 0.002066 | 0        | -0.041322 | 0         |
      | earth      | 100        | 220     | 0       | 0        | 3.0151   | moon        | 1           | 264      | 0        | 0         | 4.5227    | 1                | 0.000517 | 0        | -0.051653 | 0         |

  Scenario Outline: Physics ticks update velocity and position from gravity
    Given the default orbit simulator bodies are running
    When the simulator advances by <seconds> seconds using gravity constant <gravity_constant> and velocity-first integration
    Then the body <body> has position <x>, <y> and velocity <vx>, <vy>

    Examples:
      | seconds | gravity_constant | body  | x        | y      | vx        | vy     |
      | 1       | 1                | earth | 219.9592 | 3.0151 | -0.0408   | 3.0151 |
      | 1       | 1                | moon  | 263.9196 | 4.5227 | -0.0804   | 4.5227 |
      | 1       | 1                | sun   | 0.0021   | 0      | 0.0021    | 0      |
