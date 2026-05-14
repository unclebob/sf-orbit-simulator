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

  Scenario Outline: Pause stops physics updates
    Given the default orbit simulator bodies are running
    And the simulator has advanced by <before_pause_seconds> seconds using gravity constant <gravity_constant> and velocity-first integration
    When the pause button is pressed
    And the simulator attempts to advance by <paused_seconds> seconds using gravity constant <gravity_constant> and velocity-first integration
    Then the simulation is paused
    And the control button label is <resume_label>
    And the body <body> has position <x>, <y> and velocity <vx>, <vy>

    Examples:
      | before_pause_seconds | paused_seconds | gravity_constant | resume_label | body  | x        | y      | vx      | vy     |
      | 1                    | 5              | 1                | Resume       | earth | 219.9592 | 3.0151 | -0.0408 | 3.0151 |
      | 1                    | 5              | 1                | Resume       | moon  | 263.9196 | 4.5227 | -0.0804 | 4.5227 |
      | 1                    | 5              | 1                | Resume       | sun   | 0.0021   | 0      | 0.0021  | 0      |

  Scenario Outline: Restart restores the initial simulation
    Given the default orbit simulator bodies are running
    And the simulator has advanced by <elapsed_seconds> seconds using gravity constant <gravity_constant> and velocity-first integration
    When the restart button is pressed
    Then the simulation is running
    And the control button label is <pause_label>
    And the body <body> has position <x>, <y> and velocity <vx>, <vy>

    Examples:
      | elapsed_seconds | gravity_constant | pause_label | body  | x   | y | vx | vy     |
      | 3               | 1                | Pause       | sun   | 0   | 0 | 0  | 0      |
      | 3               | 1                | Pause       | earth | 220 | 0 | 0  | 3.0151 |
      | 3               | 1                | Pause       | moon  | 264 | 0 | 0  | 4.5227 |

  Scenario Outline: Speed slider is available with a default multiplier
    Then the speed slider has minimum <minimum_speed>, maximum <maximum_speed>, step <speed_step>, and value <default_speed>

    Examples:
      | minimum_speed | maximum_speed | speed_step | default_speed |
      | 0.25          | 4             | 0.25       | 1             |

  Scenario Outline: Speed slider scales simulated time
    Given the default orbit simulator bodies are running
    When the speed slider is set to <speed_multiplier>
    And the simulator advances display time by <display_seconds> seconds using gravity constant <gravity_constant> and velocity-first integration
    Then the simulator has advanced physics time by <physics_seconds> seconds
    And the body <body> has position <x>, <y> and velocity <vx>, <vy>

    Examples:
      | speed_multiplier | display_seconds | gravity_constant | physics_seconds | body  | x        | y      | vx       | vy     |
      | 2                | 1               | 1                | 2               | earth | 219.8368 | 6.0302 | -0.0816  | 3.0151 |
      | 2                | 1               | 1                | 2               | moon  | 263.6786 | 9.0454 | -0.1607  | 4.5227 |
      | 2                | 1               | 1                | 2               | sun   | 0.0083   | 0      | 0.0042   | 0      |
