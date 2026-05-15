Feature: 2D orbit simulator

  The simulator shows a sun, an earth, and a moon in a two-dimensional
  scene. Each body has mass, position, and velocity, and every physics update
  uses Newton's law of gravity between every pair of bodies. Display frame
  time is accumulated into fixed-size physics substeps, and each substep uses
  velocity Verlet integration so orbit behavior does not depend on coarse
  display frame timing.

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
    When the simulator advances by <seconds> seconds using gravity constant <gravity_constant>, velocity Verlet integration, and fixed substep <substep_seconds>
    Then the body <body> has position <x>, <y> and velocity <vx>, <vy>

    Examples:
      | seconds | gravity_constant | substep_seconds | body  | x        | y      | vx       | vy     |
      | 1       | 1                | 0.016667        | earth | 219.9796 | 3.0150 | -0.0408  | 3.0148 |
      | 1       | 1                | 0.016667        | moon  | 263.9598 | 4.5223 | -0.0803  | 4.5216 |
      | 1       | 1                | 0.016667        | sun   | 0.0010   | 0      | 0.0021   | 0      |

  Scenario Outline: Pause stops physics updates
    Given the default orbit simulator bodies are running
    And the simulator has advanced by <before_pause_seconds> seconds using gravity constant <gravity_constant>, velocity Verlet integration, and fixed substep <substep_seconds>
    When the pause button is pressed
    And the simulator attempts to advance by <paused_seconds> seconds using gravity constant <gravity_constant>, velocity Verlet integration, and fixed substep <substep_seconds>
    Then the simulation is paused
    And the control button label is <resume_label>
    And the body <body> has position <x>, <y> and velocity <vx>, <vy>

    Examples:
      | before_pause_seconds | paused_seconds | gravity_constant | substep_seconds | resume_label | body  | x        | y      | vx      | vy     |
      | 1                    | 5              | 1                | 0.016667        | Resume       | earth | 219.9796 | 3.0150 | -0.0408 | 3.0148 |
      | 1                    | 5              | 1                | 0.016667        | Resume       | moon  | 263.9598 | 4.5223 | -0.0803 | 4.5216 |
      | 1                    | 5              | 1                | 0.016667        | Resume       | sun   | 0.0010   | 0      | 0.0021  | 0      |

  Scenario Outline: Restart restores the initial simulation
    Given the default orbit simulator bodies are running
    And the simulator has advanced by <elapsed_seconds> seconds using gravity constant <gravity_constant>, velocity Verlet integration, and fixed substep <substep_seconds>
    And the view center is <start_center_x>, <start_center_y>
    And the zoom-out slider is set to <start_zoom>
    When the restart button is pressed
    Then the simulation is running
    And the control button label is <pause_label>
    And the view center is <end_center_x>, <end_center_y>
    And the zoom-out slider value is <end_zoom>
    And the zoom-out slider label is <zoom_label>
    And the body <body> has position <x>, <y> and velocity <vx>, <vy>

    Examples:
      | elapsed_seconds | gravity_constant | substep_seconds | start_center_x | start_center_y | start_zoom | pause_label | end_center_x | end_center_y | end_zoom | zoom_label | body  | x   | y | vx | vy     |
      | 3               | 1                | 0.016667        | 120            | -80            | 4          | Pause       | 0            | 0            | 1        | 1X         | sun   | 0   | 0 | 0  | 0      |
      | 3               | 1                | 0.016667        | 120            | -80            | 4          | Pause       | 0            | 0            | 1        | 1X         | earth | 220 | 0 | 0  | 3.0151 |
      | 3               | 1                | 0.016667        | 120            | -80            | 4          | Pause       | 0            | 0            | 1        | 1X         | moon  | 264 | 0 | 0  | 4.5227 |

  Scenario Outline: Speed slider is available with a default multiplier
    Then the speed slider has minimum <minimum_speed>, maximum <maximum_speed>, step <speed_step>, value <default_speed>, and label <default_label>

    Examples:
      | minimum_speed | maximum_speed | speed_step | default_speed | default_label |
      | 1             | 20            | 1          | 1             | 1X            |

  Scenario Outline: Speed slider scales simulated time
    Given the default orbit simulator bodies are running
    When the speed slider is set to <speed_multiplier>
    And the simulator advances display time by <display_seconds> seconds using gravity constant <gravity_constant>, velocity Verlet integration, and fixed substep <substep_seconds>
    Then the simulator has advanced physics time by <physics_seconds> seconds
    And the speed slider label is <speed_label>
    And the body <body> has position <x>, <y> and velocity <vx>, <vy>

    Examples:
      | speed_multiplier | display_seconds | gravity_constant | substep_seconds | physics_seconds | speed_label | body  | x        | y      | vx       | vy     |
      | 2                | 1               | 1                | 0.016667        | 2               | 2X          | earth | 219.9184 | 6.0295 | -0.0816  | 3.0140 |
      | 2                | 1               | 1                | 0.016667        | 2               | 2X          | moon  | 263.8394 | 9.0424 | -0.1606  | 4.5182 |
      | 2                | 1               | 1                | 0.016667        | 2               | 2X          | sun   | 0.0042   | 0      | 0.0042   | 0.0001 |

  Scenario Outline: Display frame size does not change physics results
    Given the default orbit simulator bodies are running
    When one simulator advances <physics_seconds> seconds in one display frame using gravity constant <gravity_constant>, velocity Verlet integration, and fixed substep <substep_seconds>
    And another identical simulator advances <physics_seconds> seconds in <frame_count> display frames using gravity constant <gravity_constant>, velocity Verlet integration, and fixed substep <substep_seconds>
    Then both simulations place body <body> at the same position and velocity

    Examples:
      | physics_seconds | frame_count | gravity_constant | substep_seconds | body  |
      | 20              | 1200        | 1                | 0.016667        | earth |
      | 20              | 1200        | 1                | 0.016667        | moon  |
      | 20              | 1200        | 1                | 0.016667        | sun   |

  Scenario Outline: Speed slider thumb can be dragged
    Given the default orbit simulator bodies are running
    And the speed slider is set to <start_speed>
    When the speed slider thumb is dragged to <end_speed>
    Then the speed slider value is <end_speed>
    And the speed slider label is <speed_label>

    Examples:
      | start_speed | end_speed | speed_label |
      | 1           | 12        | 12X         |

  Scenario Outline: Zoom-out slider is available with a default multiplier
    Then the zoom-out slider has minimum <minimum_zoom>, maximum <maximum_zoom>, step <zoom_step>, value <default_zoom>, and label <default_label>

    Examples:
      | minimum_zoom | maximum_zoom | zoom_step | default_zoom | default_label |
      | 1            | 5            | 1         | 1            | 1X            |

  Scenario Outline: Zoom-out slider scales the orbit view
    Given the default orbit simulator bodies are running
    And the view center is <start_center_x>, <start_center_y>
    When the zoom-out slider is set to <zoom_out_multiplier>
    Then the zoom-out slider value is <zoom_out_multiplier>
    And the zoom-out slider label is <zoom_label>
    And the orbit view renders <screen_pixels> screen pixels for <world_units> world units
    And the view center is <end_center_x>, <end_center_y>

    Examples:
      | start_center_x | start_center_y | zoom_out_multiplier | zoom_label | screen_pixels | world_units | end_center_x | end_center_y |
      | 0              | 0              | 2                   | 2X         | 50            | 100         | 0            | 0            |

  Scenario Outline: Zoom-out slider thumb can be dragged
    Given the default orbit simulator bodies are running
    And the zoom-out slider is set to <start_zoom>
    When the zoom-out slider thumb is dragged to <end_zoom>
    Then the zoom-out slider value is <end_zoom>
    And the zoom-out slider label is <zoom_label>

    Examples:
      | start_zoom | end_zoom | zoom_label |
      | 1          | 4        | 4X         |

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

  Scenario Outline: Vertical scroll wheel adjusts the view center up and down
    Given the default orbit simulator bodies are running
    And the view center is <start_center_x>, <start_center_y>
    When the orbit area receives vertical scroll input <scroll_y> with scroll scale <scroll_scale>
    Then the view center is <end_center_x>, <end_center_y>
    And the body <body> has position <body_x>, <body_y> and velocity <vx>, <vy>

    Examples:
      | start_center_x | start_center_y | scroll_y | scroll_scale | end_center_x | end_center_y | body  | body_x | body_y | vx | vy     |
      | 0              | 0              | -20      | 1            | 0            | -20          | earth | 220    | 0      | 0  | 3.0151 |
      | 0              | 0              | 20       | 1            | 0            | 20           | earth | 220    | 0      | 0  | 3.0151 |

  Scenario Outline: Zoomed-out scroll wheel adjusts the view center by zoomed world distance
    Given the default orbit simulator bodies are running
    And the view center is <start_center_x>, <start_center_y>
    And the zoom-out slider is set to <zoom_out_multiplier>
    When the orbit area receives scroll input <scroll_x>, <scroll_y> with scroll scale <scroll_scale>
    Then the view center is <end_center_x>, <end_center_y>
    And the body <body> has position <body_x>, <body_y> and velocity <vx>, <vy>

    Examples:
      | start_center_x | start_center_y | zoom_out_multiplier | scroll_x | scroll_y | scroll_scale | end_center_x | end_center_y | body  | body_x | body_y | vx | vy     |
      | 0              | 0              | 2                   | -30      | 20       | 1            | -60          | 40           | earth | 220    | 0      | 0  | 3.0151 |

  Scenario Outline: Sun recenter button is available
    Then the sun recenter button label is <recenter_label>

    Examples:
      | recenter_label |
      | Center Sun     |

  Scenario Outline: Sun recenter button centers the view on the sun
    Given the default orbit simulator bodies are running
    And the simulator has advanced by <elapsed_seconds> seconds using gravity constant <gravity_constant>, velocity Verlet integration, and fixed substep <substep_seconds>
    And the view center is <start_center_x>, <start_center_y>
    And the zoom-out slider is set to <start_zoom>
    When the sun recenter button is pressed
    Then the view center is <sun_x>, <sun_y>
    And the zoom-out slider value is <end_zoom>
    And the zoom-out slider label is <zoom_label>
    And the body <body> has position <sun_x>, <sun_y> and velocity <vx>, <vy>

    Examples:
      | elapsed_seconds | gravity_constant | substep_seconds | start_center_x | start_center_y | start_zoom | sun_x  | sun_y | end_zoom | zoom_label | body | vx     | vy |
      | 1               | 1                | 0.016667        | 120            | -80            | 4          | 0.0010 | 0     | 4        | 4X         | sun  | 0.0021 | 0  |

  Scenario Outline: Empty orbit area click adds a body in circular orbit around the sun
    Given the default orbit simulator bodies are running
    When the empty orbit area is clicked at position <x>, <y> using gravity constant <gravity_constant>
    Then a body <body> is added with color <color>, radius <radius_px>, mass <mass>, position <x>, <y>, and velocity <vx>, <vy>
    And the body <body> has circular orbit speed <speed> around the sun
    And the simulator has <body_count> bodies

    Examples:
      | x   | y | gravity_constant | body   | color | radius_px | mass | vx | vy     | speed  | body_count |
      | 300 | 0 | 1                | body_1 | gray  | 4         | 1    | 0  | 2.5820 | 2.5820 | 4          |

  Scenario Outline: Zoomed-out empty orbit area click adds a body at the zoomed world position
    Given the default orbit simulator bodies are running
    And the view center is <start_center_x>, <start_center_y>
    And the zoom-out slider is set to <zoom_out_multiplier>
    When the empty orbit area is clicked at screen offset <screen_x>, <screen_y> from the view center using gravity constant <gravity_constant>
    Then a body <body> is added with color <color>, radius <radius_px>, mass <mass>, position <x>, <y>, and velocity <vx>, <vy>
    And the body <body> has circular orbit speed <speed> around the sun
    And the simulator has <body_count> bodies

    Examples:
      | start_center_x | start_center_y | zoom_out_multiplier | screen_x | screen_y | gravity_constant | x   | y | body   | color | radius_px | mass | vx | vy     | speed  | body_count |
      | 0              | 0              | 2                   | 150      | 0        | 1                | 300 | 0 | body_1 | gray  | 4         | 1    | 0  | 2.5820 | 2.5820 | 4          |

  Scenario Outline: Dragging a body previews its velocity change
    Given the default orbit simulator bodies are running
    When the body <body> is dragged toward mouse position <mouse_x>, <mouse_y>
    Then a velocity preview line is drawn from <body_x>, <body_y> to <mouse_x>, <mouse_y>
    And the body <body> still has position <body_x>, <body_y> and velocity <vx>, <vy>

    Examples:
      | body  | body_x | body_y | mouse_x | mouse_y | vx | vy     |
      | earth | 220    | 0      | 220     | -50     | 0  | 3.0151 |

  Scenario Outline: Zoomed-out body drag previews velocity toward the zoomed world position
    Given the default orbit simulator bodies are running
    And the view center is <start_center_x>, <start_center_y>
    And the zoom-out slider is set to <zoom_out_multiplier>
    When the body <body> is dragged toward screen offset <screen_x>, <screen_y> from the view center
    Then a velocity preview line is drawn from <body_x>, <body_y> to <mouse_x>, <mouse_y>
    And the body <body> still has position <body_x>, <body_y> and velocity <vx>, <vy>

    Examples:
      | start_center_x | start_center_y | zoom_out_multiplier | body  | screen_x | screen_y | body_x | body_y | mouse_x | mouse_y | vx | vy     |
      | 0              | 0              | 2                   | earth | 110      | -25      | 220    | 0      | 220     | -50     | 0  | 3.0151 |

  Scenario Outline: Releasing a dragged body updates its velocity vector
    Given the default orbit simulator bodies are running
    When the body <body> is dragged from position <body_x>, <body_y> to mouse position <mouse_x>, <mouse_y>
    And the mouse button is released with velocity scale <velocity_scale>
    Then the body <body> has position <body_x>, <body_y> and velocity <vx>, <vy>

    Examples:
      | body  | body_x | body_y | mouse_x | mouse_y | velocity_scale | vx | vy     |
      | earth | 220    | 0      | 220     | -50     | 0.01           | 0  | 2.5151 |

  Scenario Outline: Releasing a zoomed-out body drag updates velocity toward the zoomed world position
    Given the default orbit simulator bodies are running
    And the view center is <start_center_x>, <start_center_y>
    And the zoom-out slider is set to <zoom_out_multiplier>
    When the body <body> is dragged from position <body_x>, <body_y> to screen offset <screen_x>, <screen_y> from the view center
    And the mouse button is released with velocity scale <velocity_scale>
    Then the body <body> has position <body_x>, <body_y> and velocity <vx>, <vy>

    Examples:
      | start_center_x | start_center_y | zoom_out_multiplier | body  | body_x | body_y | screen_x | screen_y | velocity_scale | vx | vy     |
      | 0              | 0              | 2                   | earth | 220    | 0      | 110      | -25      | 0.01           | 0  | 2.5151 |

  Scenario Outline: Near-body click adds a body in circular orbit around that body
    Given the default orbit simulator bodies are running
    When the orbit area is clicked at position <x>, <y> within <diameter_count> diameters of <center_body> using gravity constant <gravity_constant>
    Then a body <body> is added orbiting <center_body> with color <color>, radius <radius_px>, mass <mass>, position <x>, <y>, and velocity <vx>, <vy>
    And the body <body> has circular orbit speed <speed> around <center_body>
    And the simulator has <body_count> bodies

    Examples:
      | x   | y  | diameter_count | center_body | gravity_constant | body   | color | radius_px | mass | vx      | vy     | speed  | body_count |
      | 220 | 60 | 4              | earth       | 1                | body_1 | gray  | 4         | 1    | -1.2910 | 3.0151 | 1.2910 | 4          |

  Scenario Outline: Zoomed-out near-body click adds a body orbiting the zoomed world center
    Given the default orbit simulator bodies are running
    And the view center is <start_center_x>, <start_center_y>
    And the zoom-out slider is set to <zoom_out_multiplier>
    When the orbit area is clicked at screen offset <screen_x>, <screen_y> from the view center within <diameter_count> diameters of <center_body> using gravity constant <gravity_constant>
    Then a body <body> is added orbiting <center_body> with color <color>, radius <radius_px>, mass <mass>, position <x>, <y>, and velocity <vx>, <vy>
    And the body <body> has circular orbit speed <speed> around <center_body>
    And the simulator has <body_count> bodies

    Examples:
      | start_center_x | start_center_y | zoom_out_multiplier | screen_x | screen_y | diameter_count | center_body | gravity_constant | x   | y  | body   | color | radius_px | mass | vx      | vy     | speed  | body_count |
      | 0              | 0              | 2                   | 110      | 30       | 4              | earth       | 1                | 220 | 60 | body_1 | gray  | 4         | 1    | -1.2910 | 3.0151 | 1.2910 | 4          |

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

  Scenario Outline: Bodies whose rendered edges do not touch remain separate
    Given a body <first_body> has color <first_color>, radius <first_radius_px>, mass <first_mass>, position <first_x>, <first_y>, and velocity <first_vx>, <first_vy>
    And a body <second_body> has color <second_color>, radius <second_radius_px>, mass <second_mass>, position <second_x>, <second_y>, and velocity <second_vx>, <second_vy>
    When screen collisions are resolved using rendered body edges
    Then the simulator has <body_count> bodies
    And the body <first_body> has position <first_x>, <first_y> and velocity <first_vx>, <first_vy>
    And the body <second_body> has position <second_x>, <second_y> and velocity <second_vx>, <second_vy>

    Examples:
      | first_body | first_color | first_radius_px | first_mass | first_x | first_y | first_vx | first_vy | second_body | second_color | second_radius_px | second_mass | second_x | second_y | second_vx | second_vy | body_count |
      | alpha      | blue        | 4               | 3          | 0       | 0       | 2        | 0        | beta        | gray         | 3                | 1           | 8        | 0        | -2        | 0         | 2          |

  Scenario Outline: Bodies collide inelastically when their rendered edges touch on screen
    Given a body <first_body> has color <first_color>, radius <first_radius_px>, mass <first_mass>, position <first_x>, <first_y>, and velocity <first_vx>, <first_vy>
    And a body <second_body> has color <second_color>, radius <second_radius_px>, mass <second_mass>, position <second_x>, <second_y>, and velocity <second_vx>, <second_vy>
    When screen collisions are resolved using rendered body edges and restitution <restitution>
    Then the simulator has <body_count> bodies
    And the original rendered body centers were <screen_distance_px> pixels apart
    And the original rendered body edges were touching at screen distance <touch_distance_px>
    And the body <first_body> is still visible with color <first_color>, radius <first_radius_px>, mass <first_mass>, position <first_x>, <first_y>, and velocity <first_after_vx>, <first_after_vy>
    And the body <second_body> is still visible with color <second_color>, radius <second_radius_px>, mass <second_mass>, position <second_x>, <second_y>, and velocity <second_after_vx>, <second_after_vy>

    Examples:
      | first_body | first_color | first_radius_px | first_mass | first_x | first_y | first_vx | first_vy | second_body | second_color | second_radius_px | second_mass | second_x | second_y | second_vx | second_vy | restitution | screen_distance_px | touch_distance_px | body_count | first_after_vx | first_after_vy | second_after_vx | second_after_vy |
      | alpha      | blue        | 4               | 3          | 0       | 0       | 2        | 0        | beta        | gray         | 3                | 1           | 7        | 0        | -2        | 0         | 0.5         | 7                  | 7                 | 2          | 0.5            | 0              | 2.5             | 0               |

  Scenario Outline: Bodies whose rendered edges overlap are separated until they only touch
    Given a body <first_body> has color <first_color>, radius <first_radius_px>, mass <first_mass>, position <first_x>, <first_y>, and velocity <first_vx>, <first_vy>
    And a body <second_body> has color <second_color>, radius <second_radius_px>, mass <second_mass>, position <second_x>, <second_y>, and velocity <second_vx>, <second_vy>
    When screen collisions are resolved using rendered body edges and restitution <restitution>
    Then the simulator has <body_count> bodies
    And the original rendered body centers were <screen_distance_px> pixels apart
    And the resolved rendered body centers are at least <minimum_screen_distance_px> pixels apart
    And the resolved rendered body edges can touch at screen distance <touch_distance_px>

    Examples:
      | first_body | first_color | first_radius_px | first_mass | first_x | first_y | first_vx | first_vy | second_body | second_color | second_radius_px | second_mass | second_x | second_y | second_vx | second_vy | restitution | screen_distance_px | minimum_screen_distance_px | touch_distance_px | body_count |
      | alpha      | blue        | 4               | 3          | 0       | 0       | 0        | 0        | beta        | gray         | 3                | 1           | 6        | 0        | 0         | 0         | 0.5         | 6                  | 7                          | 7                 | 2          |
