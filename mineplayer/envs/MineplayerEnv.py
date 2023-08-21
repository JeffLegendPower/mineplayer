import base64
import json
import os
import socket
import gymnasium as gym
import numpy as np
from PIL import Image
from gymnasium import spaces
import sys
from utils import encoded_frame_to_image

parent_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# sys.path.insert(0, os.path.join(parent_dir, "minecraft"))
# sys.path.insert(0, parent_dir)
# sys.path.insert(2, os.path.join())

from minecraft import install_minecraft_client, launch_minecraft_client


class MineplayerEnv(gym.Env):
    metadata = {'render.modes': ['human']}

    def __init__(self, mc_server_address="localhost", mc_server_port=25565, connection_timeout=None, env_type="treechop",
                 headless=False, window_width=640, window_height=360, username="TESTBOT",
                 props: dict = {},
                 num_keys=5, num_mouse_buttons=2
                 ):

        install_minecraft_client()
        self.minecraft_client = launch_minecraft_client(username)

        self.running = True

        self.window_width = window_width
        self.window_height = window_height

        self.address = "127.0.0.1"
        self.port = 2880

        self.env_type = env_type

        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.bind((self.address, self.port))
        self.server_socket.listen(1)

        print(f"Listening for connections at {self.address}:{self.port}...")

        self.client_socket, _ = self.server_socket.accept()
        self.client_socket.settimeout(connection_timeout)
        print(f"Connected to client")

        # Establish that this is an environment
        self.send_message({"context": "ENV"})
        response = self.receive_message()
        if response != "ENV":
            raise ValueError(f"Received invalid response from Minecraft client, expected ENV, got {response}")

        init_message = {
            "context": "init",
            "body": {
                "address": mc_server_address,
                "port": mc_server_port,
                "env_type": self.env_type,
                "props": props,
                "window_width": self.window_width,
                "window_height": self.window_height,
            }
        }

        print(f"Sending init message to client")
        self.send_message(init_message)

        response = self.receive_message()
        response = json.loads(response)

        if response["context"] != "init":
            raise ValueError(f"Received invalid response from Minecraft client, expected init, got {response}")
        if response["body"]["status"] != "success":
            raise ValueError(
                f"Received error response from Minecraft client (check Minecraft logs for error): {response}")

        self.observation_space = spaces.Dict(
            {
                "window": spaces.Box(low=0, high=255, shape=(self.window_height, self.window_width, 4), dtype=np.float16),
                "key_states": spaces.Box(low=0, high=np.PINF, shape=(num_keys,), dtype=np.int8),
                "mouse_states": spaces.Box(low=0, high=1, shape=(num_mouse_buttons,), dtype=np.int8),
                "mouse_pos": spaces.Box(low=np.NINF, high=np.PINF, shape=(2,), dtype=np.float32),
            }
        )

        self.action_space = spaces.Dict(
            {
                "key_toggles": spaces.Box(low=0, high=np.PINF, shape=(num_keys,), dtype=np.int8),
                "mouse_toggles": spaces.Box(low=0, high=np.PINF, shape=(num_mouse_buttons,), dtype=np.int8),
                "mouse_move": spaces.Box(low=np.NINF, high=np.PINF, shape=(2,), dtype=np.float32),
            }
        )

        print("Environment setup complete")

    def send_message(self, message):
        self.client_socket.sendall(bytes(json.dumps(message), encoding="utf-8"))

    def receive_message(self):
        data = b""
        while True:
            chunk = self.client_socket.recv(4096)
            data += chunk
            if len(chunk) < 4096:
                break
        return data.decode("utf-8").rstrip('\r\n')

    def reset(self, seed=None, options=None):
        super().reset(seed=seed)
        reset_message = {
            "context": "reset",
            "body": {}
        }

        self.send_message(reset_message)

        response = self.receive_message()
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
            encoded_frame += self.client_socket.recv(bytes_to_receive)

        # frame_string = encoded_frame.decode('utf-8').rstrip('\r\n')
        # frame_buffer = base64.b64decode(frame_string)
        #
        # np_image = np.frombuffer(frame_buffer, dtype=np.uint8).reshape((image_height, image_width, 4))
        # np_image = np_image[:, :, :3]
        #
        # image = Image.fromarray(np_image, 'RGB')
        # image = image.transpose(Image.FLIP_TOP_BOTTOM)
        image = encoded_frame_to_image(encoded_frame, image_width, image_height)

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

        self.send_message(step_message)

        response = self.receive_message()
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
            encoded_frame += self.client_socket.recv(bytes_to_receive)

        # frame_string = encoded_frame.decode('utf-8').rstrip('\r\n')
        # frame_buffer = base64.b64decode(frame_string)
        #
        # np_image = np.frombuffer(frame_buffer, dtype=np.uint8).reshape((image_height, image_width, 4))
        # np_image = np_image[:, :, :3] # Remove alpha channel
        #
        # image = Image.fromarray(np_image, 'RGB')
        # image = image.transpose(Image.FLIP_TOP_BOTTOM)
        image = encoded_frame_to_image(encoded_frame, image_width, image_height)

        obs["window"] = np.array(image)

        terminated = response["body"]["terminated"]

        reward = response["body"]["reward"]

        info = None

        return obs, reward, terminated, False, info

    def close(self):
        close_message = {
            "context": "close",
            "body": {}
        }

        try:
            self.send_message(close_message)
            # Now wait for the client to respond
            response = self.receive_message()
            response = json.loads(response)
            if response["context"] != "close":
                raise ValueError(f"Received invalid response from Minecraft client: {response}")
            if response["body"]["status"] != "success":
                raise ValueError(
                    f"Received error response from Minecraft client (check Minecraft logs for error): {response}")
        finally:
            self.client_socket.close()
            self.server_socket.close()
            print("Closed mineplayer environment")