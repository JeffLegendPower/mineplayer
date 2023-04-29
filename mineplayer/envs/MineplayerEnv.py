import base64
import json
import socket

import gymnasium as gym
import numpy as np
from PIL import Image
from gymnasium import spaces


class MineplayerEnv(gym.Env):
    metadata = {'render.modes': ['human']}

    # Valid keys and buttons follow GLFW enumerations
    def __init__(self, address="localhost", port=25565,
                 env_address="localhost", env_port=444,
                 env_type="treechop",
                 headless=False,
                 window_width=640, window_height=360,
                 valid_keys: list[int] = [],
                 valid_buttons: list[int] = [],
                 props: dict = {}):

        self.running = True

        self.window_width = window_width
        self.window_height = window_height

        self.address = address
        self.port = port

        self.env_type = env_type

        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

        print(f"Connecting to Minecraft client at {env_address}:{env_port}...")
        self.sock.connect((env_address, env_port))
        self.sock.settimeout(30.0)
        print(f"Connected to Minecraft client at {env_address}:{env_port}")

        init_message = {
            "context": "init",
            "body": {
                "address": self.address,
                "port": self.port,
                "env_type": self.env_type,
                "valid_keys": valid_keys,
                "valid_buttons": valid_buttons,
                "props": props,
                "window_width": self.window_width,
                "window_height": self.window_height,
            }
        }

        print(f"Sending init message to Minecraft client")
        print(bytes(json.dumps(init_message), encoding="utf-8"))

        self.sock.sendall(bytes(json.dumps(init_message), encoding="utf-8"))
        # Now wait for the client to respond
        print("Setting up environment...")
        response = self.sock.recv(4096)
        response = json.loads(response)
        if response["context"] != "init":
            raise ValueError(f"Received invalid response from Minecraft client: {response}")
        if response["body"]["status"] != "success":
            raise ValueError(
                f"Received error response from Minecraft client (check Minecraft logs for error): {response}")

        self.observation_space = spaces.Dict(
            {
                "window": spaces.Box(low=0, high=255, shape=(self.window_height, self.window_width, 4), dtype=np.float16),
                "key_states": spaces.Box(low=0, high=1, shape=(len(valid_keys),), dtype=np.int8),
                "mouse_states": spaces.Box(low=0, high=1, shape=(len(valid_buttons),), dtype=np.int8),
                "mouse_pos": spaces.Box(low=np.NINF, high=np.PINF, shape=(2,), dtype=np.float32),
            }
        )

        self.action_space = spaces.Dict(
            {
                "key_toggles": spaces.Box(low=0, high=1, shape=(len(valid_keys),), dtype=np.int8),
                "mouse_toggles": spaces.Box(low=0, high=1, shape=(len(valid_buttons),), dtype=np.int8),
                "mouse_move": spaces.Box(low=np.NINF, high=np.PINF, shape=(2,), dtype=np.float32),
            }
        )

        print("Environment setup complete")

    def render(self, mode='human', close=False):
        # TODO
        pass

    def reset(self, seed=None, options=None):
        super().reset(seed=seed)
        # TODO

        reset_message = {
            "context": "reset",
            "body": {}
        }

        self.sock.sendall(bytes(json.dumps(reset_message), encoding="utf-8"))
        # Now wait for the client to respond
        response_bytes = self.sock.recv(4096)
        response = response_bytes.decode('utf-8').rstrip('\r\n')

        response = json.loads(response)
        if response["context"] != "reset":
            raise ValueError(f"Received invalid response from Minecraft client: {response}")
        if response["body"]["status"] != "success":
            raise ValueError(
                f"Received error response from Minecraft client (check Minecraft logs for error): {response}")

        obs = response["body"]["observation"]

        image_width = obs["viewport_info"]["width"]
        image_height = obs["viewport_info"]["height"]

        encoded_length = obs["viewport_info"]["encoded_length"] + 2  # For \r\n

        encoded_frame = b""

        while len(encoded_frame) < encoded_length:
            bytes_to_receive = min(encoded_length - len(encoded_frame), 4096)
            encoded_frame += self.sock.recv(bytes_to_receive)

        frame_string = encoded_frame.decode('utf-8').rstrip('\r\n')
        frame_buffer = base64.b64decode(frame_string)

        np_image = np.frombuffer(frame_buffer, dtype=np.uint8).reshape((image_height, image_width, 4))
        np_image = np_image[:, :, :3]

        image = Image.fromarray(np_image, 'RGB')
        image = image.transpose(Image.FLIP_TOP_BOTTOM)

        obs["window"] = np.array(image)

        info = None

        return obs, info

    def step(self, action):
        key_toggles = action["key_toggles"].tolist() \
            if isinstance(action["key_toggles"], np.ndarray) \
            else action["key_toggles"]

        mouse_toggles = action["mouse_toggles"].tolist() \
            if isinstance(action["mouse_toggles"], np.ndarray) \
            else action["mouse_toggles"]

        mouse_move = {
            "x": action["mouse_move"][0],
            "y": action["mouse_move"][1],
        }

        step_message = {
            "context": "step",
            "body": {
                "key_toggles": key_toggles,
                "mouse_toggles": mouse_toggles,
                "mouse_move": mouse_move,
            }
        }

        self.sock.sendall(bytes(json.dumps(step_message), encoding="utf-8"))
        # Now wait for the client to respond
        response_bytes = self.sock.recv(4096)
        response = response_bytes.decode('utf-8').rstrip('\r\n')

        response = json.loads(response)
        if response["context"] != "step":
            raise ValueError(f"Received invalid response from Minecraft client: {response}")
        if response["body"]["status"] != "success":
            raise ValueError(
                f"Received error response from Minecraft client (check Minecraft logs for error): {response}")

        obs = response["body"]["observation"]

        image_width = obs["viewport_info"]["width"]
        image_height = obs["viewport_info"]["height"]

        encoded_length = obs["viewport_info"]["encoded_length"] + 2  # For \r\n

        encoded_frame = b""

        while len(encoded_frame) < encoded_length:
            bytes_to_receive = min(encoded_length - len(encoded_frame), 4096)
            encoded_frame += self.sock.recv(bytes_to_receive)

        frame_string = encoded_frame.decode('utf-8').rstrip('\r\n')
        frame_buffer = base64.b64decode(frame_string)

        np_image = np.frombuffer(frame_buffer, dtype=np.uint8).reshape((image_height, image_width, 4))
        np_image = np_image[:, :, :3]

        image = Image.fromarray(np_image, 'RGB')
        image = image.transpose(Image.FLIP_TOP_BOTTOM)

        obs["window"] = np.array(image)

        terminated = response["body"]["terminated"]

        reward = response["body"]["reward"]

        info = None

        return obs, reward, terminated, False, info

    def close(self):
        # TODO
        close_message = {
            "context": "close",
            "body": {}
        }

        try:
            self.sock.sendall(bytes(json.dumps(close_message), encoding="utf-8"))
            # Now wait for the client to respond
            response_bytes = self.sock.recv(4096)
            response = response_bytes.decode('utf-8').rstrip('\r\n')
            response = json.loads(response)
            if response["context"] != "close":
                raise ValueError(f"Received invalid response from Minecraft client: {response}")
            if response["body"]["status"] != "success":
                raise ValueError(
                    f"Received error response from Minecraft client (check Minecraft logs for error): {response}")
        finally:
            self.sock.close()
