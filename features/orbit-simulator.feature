Feature: 2D orbit simulator

  The simulator shows a sun, an earth, and a moon in a two-dimensional
  scene. Each body has mass, position, and velocity, and every physics update
  uses Newton's law of gravity between every pair of bodies. Physics updates
  use a symplectic integrator so each step updates velocity from gravity
  before updating position from the new velocity.

  Background:
    Given the orbit simulator is opened

  Scenario Outline: Default bodies are visible with physical state
    Then the body <body> is visible with color <color>, radius <radius_px>, mass <mass>, position <x>, <y>, and velocity <vx>, <vy>

    Examples:
      | body  | color  | radius_px | mass | x   | y | vx | vy     |
      | sun   | yellow | 36        | 2000 | 0   | 0 | 0  | 0      |
      | earth | blue   | 12        | 100  | 220 | 0 | 0  | 3.0151 |
      | moon  | gray   | 4         | 1    | 264 | 0 | 0  | 4.5227 |

  Scenario Outline: Body radius increases with mass
    Given a body <smaller_body> has mass <smaller_mass> and radius <smaller_radius_px>
    And a body <larger_body> has mass <larger_mass> and radius <larger_radius_px>
    Then <larger_body> has greater radius than <smaller_body>

    Examples:
      | smaller_body | smaller_mass | smaller_radius_px | larger_body | larger_mass | larger_radius_px |
      | moon         | 1            | 4                 | earth       | 100         | 12               |
      | earth        | 100          | 12                | sun         | 2000        | 36               |

  Scenario Outline: Tidal forces stretch elastic bodies into ellipses
    Given a body <body> has mass <mass>, radius <radius_px>, position <x>, <y>, and elasticity <elasticity>
    And a tidal source <source_body> has mass <source_mass> and position <source_x>, <source_y>
    When tidal deformation is calculated
    Then the body <body> is rendered as an ellipse centered at <x>, <y> with major radius <major_radius_px>, minor radius <minor_radius_px>, and major axis pointing toward <source_body>
    And the body <body> has gravity foci at <first_focus_x>, <first_focus_y> and <second_focus_x>, <second_focus_y>

    Examples:
      | body  | mass | radius_px | x   | y | elasticity | source_body | source_mass | source_x | source_y | major_radius_px | minor_radius_px | first_focus_x | first_focus_y | second_focus_x | second_focus_y |
      | earth | 100  | 12        | 220 | 0 | 0.6        | sun         | 2000        | 0        | 0        | 18              | 6               | 203.029       | 0             | 236.971        | 0              |

  Scenario Outline: Elastic body gravity is split between ellipse foci
    Given an elastic body <source_body> has mass <source_mass>, first focus <first_focus_x>, <first_focus_y>, and second focus <second_focus_x>, <second_focus_y>
    And a body <target_body> has mass <target_mass>, position <target_x>, <target_y>, and velocity <target_vx>, <target_vy>
    When gravitational acceleration from <source_body> to <target_body> is calculated using gravity constant <gravity_constant>
    Then the acceleration of <target_body> is <target_ax>, <target_ay>

    Examples:
      | source_body | source_mass | first_focus_x | first_focus_y | second_focus_x | second_focus_y | target_body | target_mass | target_x | target_y | target_vx | target_vy | gravity_constant | target_ax | target_ay |
      | earth       | 100         | 203.029       | 0             | 236.971        | 0              | moon        | 1           | 264      | 0        | 0         | 4.5227    | 1                | -0.060416 | 0         |

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
    When the simulator advances by <seconds> seconds using gravity constant <gravity_constant> and symplectic integration
    Then the body <body> has position <x>, <y> and velocity <vx>, <vy>

    Examples:
      | seconds | gravity_constant | body  | x        | y      | vx        | vy     |
      | 1       | 1                | earth | 219.9592 | 3.0151 | -0.0408   | 3.0151 |
      | 1       | 1                | moon  | 263.9196 | 4.5227 | -0.0804   | 4.5227 |
      | 1       | 1                | sun   | 0.0021   | 0      | 0.0021    | 0      |

  Scenario Outline: Pause stops physics updates
    Given the default orbit simulator bodies are running
    And the simulator has advanced by <before_pause_seconds> seconds using gravity constant <gravity_constant> and symplectic integration
    When the pause button is pressed
    And the simulator attempts to advance by <paused_seconds> seconds using gravity constant <gravity_constant> and symplectic integration
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
    And the simulator has advanced by <elapsed_seconds> seconds using gravity constant <gravity_constant> and symplectic integration
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
    Then the speed slider has minimum <minimum_speed>, maximum <maximum_speed>, step <speed_step>, value <default_speed>, and label <default_label>

    Examples:
      | minimum_speed | maximum_speed | speed_step | default_speed | default_label |
      | 1             | 20            | 1          | 1             | 1X            |

  Scenario Outline: Speed slider scales simulated time
    Given the default orbit simulator bodies are running
    When the speed slider is set to <speed_multiplier>
    And the simulator advances display time by <display_seconds> seconds using gravity constant <gravity_constant> and symplectic integration
    Then the simulator has advanced physics time by <physics_seconds> seconds
    And the speed slider label is <speed_label>
    And the body <body> has position <x>, <y> and velocity <vx>, <vy>

    Examples:
      | speed_multiplier | display_seconds | gravity_constant | physics_seconds | speed_label | body  | x        | y      | vx       | vy     |
      | 2                | 1               | 1                | 2               | 2X          | earth | 219.8368 | 6.0302 | -0.0816  | 3.0151 |
      | 2                | 1               | 1                | 2               | 2X          | moon  | 263.6786 | 9.0454 | -0.1607  | 4.5227 |
      | 2                | 1               | 1                | 2               | 2X          | sun   | 0.0083   | 0      | 0.0042   | 0      |

  Scenario Outline: Speed slider thumb can be dragged
    Given the default orbit simulator bodies are running
    And the speed slider is set to <start_speed>
    When the speed slider thumb is dragged to <end_speed>
    Then the speed slider value is <end_speed>
    And the speed slider label is <speed_label>

    Examples:
      | start_speed | end_speed | speed_label |
      | 1           | 12        | 12X         |

  Scenario Outline: Horizontal scroll wheel adjusts the view center left and right
    Given the default orbit simulator bodies are running
    And the view center is <start_center_x>, <start_center_y>
    When the orbit area receives horizontal scroll input <scroll_x> with scroll scale <scroll_scale>
    Then the view center is <end_center_x>, <end_center_y>
    And the body <body> has position <body_x>, <body_y> and velocity <vx>, <vy>

    Examples:
      | start_center_x | start_center_y | scroll_x | scroll_scale | end_center_x | end_center_y | body  | body_x | body_y | vx | vy     |
      | 0              | 0              | -30      | 1            | -30          | 0            | earth | 220    | 0      | 0  | 3.0151 |
      | 0              | 0              | 30       | 1            | 30           | 0            | earth | 220    | 0      | 0  | 3.0151 |

  Scenario Outline: Empty orbit area click adds a body in circular orbit around the sun
    Given the default orbit simulator bodies are running
    When the empty orbit area is clicked at position <x>, <y> using gravity constant <gravity_constant>
    Then a body <body> is added with color <color>, radius <radius_px>, mass <mass>, position <x>, <y>, and velocity <vx>, <vy>
    And the body <body> has circular orbit speed <speed> around the sun
    And the simulator has <body_count> bodies

    Examples:
      | x   | y | gravity_constant | body   | color | radius_px | mass | vx | vy     | speed  | body_count |
      | 300 | 0 | 1                | body_1 | gray  | 4         | 1    | 0  | 2.5820 | 2.5820 | 4          |

  Scenario Outline: Dragging a body previews its velocity change
    Given the default orbit simulator bodies are running
    When the body <body> is dragged toward mouse position <mouse_x>, <mouse_y>
    Then a velocity preview line is drawn from <body_x>, <body_y> to <mouse_x>, <mouse_y>
    And the body <body> still has position <body_x>, <body_y> and velocity <vx>, <vy>

    Examples:
      | body  | body_x | body_y | mouse_x | mouse_y | vx | vy     |
      | earth | 220    | 0      | 220     | -50     | 0  | 3.0151 |

  Scenario Outline: Releasing a dragged body updates its velocity vector
    Given the default orbit simulator bodies are running
    When the body <body> is dragged from position <body_x>, <body_y> to mouse position <mouse_x>, <mouse_y>
    And the mouse button is released with velocity scale <velocity_scale>
    Then the body <body> has position <body_x>, <body_y> and velocity <vx>, <vy>

    Examples:
      | body  | body_x | body_y | mouse_x | mouse_y | velocity_scale | vx | vy     |
      | earth | 220    | 0      | 220     | -50     | 0.01           | 0  | 2.5151 |

  Scenario Outline: Near-body click adds a body in circular orbit around that body
    Given the default orbit simulator bodies are running
    When the orbit area is clicked at position <x>, <y> within <diameter_count> diameters of <center_body> using gravity constant <gravity_constant>
    Then a body <body> is added orbiting <center_body> with color <color>, radius <radius_px>, mass <mass>, position <x>, <y>, and velocity <vx>, <vy>
    And the body <body> has circular orbit speed <speed> around <center_body>
    And the simulator has <body_count> bodies

    Examples:
      | x   | y  | diameter_count | center_body | gravity_constant | body   | color | radius_px | mass | vx      | vy     | speed  | body_count |
      | 220 | 60 | 4              | earth       | 1                | body_1 | gray  | 4         | 1    | -1.2910 | 3.0151 | 1.2910 | 4          |

  Scenario Outline: Releasing a dragged orbiting body updates its velocity vector
    Given the default orbit simulator bodies are running
    And the body <body> orbits <center_body>
    When the body <body> is dragged from position <body_x>, <body_y> to mouse position <mouse_x>, <mouse_y>
    And the mouse button is released with velocity scale <velocity_scale>
    Then the body <body> still orbits <center_body>
    And the body <body> has position <body_x>, <body_y> and velocity <vx>, <vy>

    Examples:
      | body | center_body | body_x | body_y | mouse_x | mouse_y | velocity_scale | vx | vy     |
      | moon | earth       | 264    | 0      | 264     | 30      | 0.01           | 0  | 4.8227 |

  Scenario Outline: Bodies outside collision range remain separate
    Given a body <first_body> has color <first_color>, radius <first_radius_px>, mass <first_mass>, position <first_x>, <first_y>, and velocity <first_vx>, <first_vy>
    And a body <second_body> has color <second_color>, radius <second_radius_px>, mass <second_mass>, position <second_x>, <second_y>, and velocity <second_vx>, <second_vy>
    When collisions are resolved
    Then the simulator has <body_count> bodies
    And the body <first_body> has position <first_x>, <first_y> and velocity <first_vx>, <first_vy>
    And the body <second_body> has position <second_x>, <second_y> and velocity <second_vx>, <second_vy>

    Examples:
      | first_body | first_color | first_radius_px | first_mass | first_x | first_y | first_vx | first_vy | second_body | second_color | second_radius_px | second_mass | second_x | second_y | second_vx | second_vy | body_count |
      | alpha      | blue        | 4               | 3          | 0       | 0       | 2        | 0        | beta        | gray         | 3                | 1           | 5        | 0        | -2        | 0         | 2          |

  Scenario Outline: Colliding bodies merge into one body
    Given a body <first_body> has color <first_color>, radius <first_radius_px>, mass <first_mass>, position <first_x>, <first_y>, and velocity <first_vx>, <first_vy>
    And a body <second_body> has color <second_color>, radius <second_radius_px>, mass <second_mass>, position <second_x>, <second_y>, and velocity <second_vx>, <second_vy>
    When collisions are resolved
    Then the simulator has <body_count> bodies
    And the original body centers were within collision radius <collision_radius_px>
    And the merged body has color <merged_color>, radius <merged_radius_px>, mass <merged_mass>, position <merged_x>, <merged_y>, and velocity <merged_vx>, <merged_vy>

    Examples:
      | first_body | first_color | first_radius_px | first_mass | first_x | first_y | first_vx | first_vy | second_body | second_color | second_radius_px | second_mass | second_x | second_y | second_vx | second_vy | collision_radius_px | body_count | merged_color | merged_radius_px | merged_mass | merged_x | merged_y | merged_vx | merged_vy |
      | alpha      | blue        | 4               | 3          | 0       | 0       | 2        | 0        | beta        | gray         | 3                | 1           | 4        | 0        | -2        | 0         | 4                   | 1          | blue         | 5                | 4           | 1        | 0        | 1         | 0         |
