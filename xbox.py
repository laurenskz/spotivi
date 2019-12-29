import json
import os
import sys
import time
from xbox360controller import Xbox360Controller

import signal
def create_callback(command):
    def on_button_released(button):
        print(command,flush=True)
    return on_button_released


if __name__ == '__main__':
    try:
        with Xbox360Controller(0, axis_threshold=0.2) as controller:
            controller.button_a.when_released = create_callback("playpause")
            controller.button_b.when_released = create_callback("previous-context")
            controller.button_y.when_released = create_callback("play-album-current")
            controller.button_x.when_released = create_callback("random-music")
            controller.button_trigger_r.when_released = create_callback("next-track")
            controller.button_trigger_l.when_released = create_callback("previous-track")
            controller.button_start.when_released = create_callback("start-on raspberry spotify:user:11102248483:playlist:3ar6blvho0KSTjNaYfzlt2")
            controller.button_mode.when_released = create_callback("add-current-to spotify:user:11102248483:playlist:3ar6blvho0KSTjNaYfzlt2")
            
            signal.pause()
    except KeyboardInterrupt:
        pass
    
