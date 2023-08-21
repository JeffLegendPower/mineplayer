import base64
import json
import os
import socket
import numpy as np
from PIL import Image
from gymnasium import spaces
import sys
from datetime import datetime
import csv
from utils import encoded_frame_to_image

# parent_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# sys.path.insert(0, os.path.join(parent_dir, "minecraft"))
#
# from minecraft_client_launcher import install_minecraft_client, launch_minecraft_client
from minecraft import install_minecraft_client, launch_minecraft_client


def flatten_array(nested_array):
    flattened_list = []
    for item in nested_array:
        if isinstance(item, list):
            flattened_list.extend(flatten_array(item))
        else:
            flattened_list.append(item)
    return flattened_list


class HumanEnv:

    def __init__(self, output_dir: str,
                 keys: list, mouse_buttons: list,
                 mc_server_address="localhost", mc_server_port=25565, connection_timeout=None,
                 env_type="treechop",
                 window_width=640, window_height=360, username="TESTBOT",
                 props: dict = {},
                 ):

        # throw error if no output_dir, keys, or mouse_buttons
        if output_dir is None:
            raise ValueError("output_dir cannot be None")
        if keys is None:
            raise ValueError("keys cannot be None")
        if mouse_buttons is None:
            raise ValueError("mouse_buttons cannot be None")

        install_minecraft_client()
        self.minecraft_client = launch_minecraft_client(username)

        self.running = True

        self.window_width = window_width
        self.window_height = window_height
        self.output_dir = output_dir
        self.env_type = env_type

        self.episode_dir = None
        self.state_csv = None
        self.state_csv_writer = None

        self.frame_index = 0

        self.keys = keys
        self.mouse_buttons = mouse_buttons

        try:
            os.makedirs(self.output_dir)
        except FileExistsError:
            pass

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

        # Establish that this is a human play environment
        self.send_message({"context": "HUMAN"})
        response = self.receive_message()
        if response != "HUMAN":
            raise ValueError(f"Received invalid response from Minecraft client, expected HUMAN, got {response}")

        init_message = {
            "context": "start",
            "body": {
                "address": mc_server_address,
                "port": mc_server_port,
                "env_type": self.env_type,
                "props": props,
                "window_width": self.window_width,
                "window_height": self.window_height,
            }
        }

        print("Sending start message to client")
        self.send_message(init_message)

        response = self.receive_message()
        response = json.loads(response)

        if response["context"] != "start":
            raise ValueError(f"Received invalid response from Minecraft client, expected start, got {response}")
        if response["body"]["status"] != "success":
            raise ValueError(
                f"Received error response from Minecraft client (check Minecraft logs for error): {response}")

        print("Environment setup complete")

        # TODO
        while self.handle_state():
            pass
        self.minecraft_client.terminate()

    def handle_state(self):
        state = self.receive_message()
        state = json.loads(state)
        if state["context"] == "state":
            if state["body"]["stage"] == "start":
                # create a new directory in self.output_dir
                now = datetime.now()
                formatted = now.strftime("%m-%d-%Y-%H:%M:%S")
                index = 0
                self.episode_dir = os.path.join(self.output_dir, f"{self.env_type}_{formatted}-{index}")
                while os.path.isdir(self.episode_dir):
                    index += 1
                    self.episode_dir = os.path.join(self.output_dir, f"{self.env_type}_{formatted}-{index}")
                os.mkdir(self.episode_dir)

                # create a csv file to store the actions
                self.state_csv = open(os.path.join(self.episode_dir, "state.csv"), "w")
                self.state_csv_writer = csv.writer(self.state_csv, delimiter=",")
                columns = [
                    [str(key) + "_key_obs" for key in self.keys],
                    [str(mouse_button) + "_mouse_obs" for mouse_button in self.mouse_buttons],
                    "mouse_x_obs",
                    "mouse_y_obs",
                    "img_path_obs",
                    [str(key) + "_key_toggle_action" for key in self.keys],
                    [str(mouse_button) + "_mouse_toggle_action" for mouse_button in self.mouse_buttons],
                    "mouse_move_x_action",
                    "mouse_move_y_action",
                ]
                columns = flatten_array(columns)
                self.write_to_csv(columns)
            elif state["body"]["stage"] == "end":
                self.episode_dir = None
                self.state_csv.close()
                self.state_csv = None
                self.state_csv_writer = None
            elif state["body"]["stage"] == "game":
                observation = state["body"]["observation"]
                action = state["body"]["action"]

                key_states = observation["key_states"]
                # key_states is a json array, if key is in key_states, set to 1 else 0
                key_obs = [1 if key in key_states else 0 for key in self.keys]

                mouse_states = observation["mouse_states"]
                # mouse_states is a json array, if mouse button is in mouse_states, set to 1 else 0
                mouse_obs = [1 if mouse_button in mouse_states else 0 for mouse_button in self.mouse_buttons]

                mouse_x_obs = observation["mouse_pos"][0]
                mouse_y_obs = observation["mouse_pos"][1]

                image_width = observation["viewport_info"]["width"]
                image_height = observation["viewport_info"]["height"]
                encoded_length = observation["viewport_info"]["encoded_length"] + 2  # For \r\n

                encoded_frame = b""

                while len(encoded_frame) < encoded_length:
                    bytes_to_receiee = min(encoded_length - len(encoded_frame), 4096)
                    encoded_frame += self.client_socket.recv(bytes_to_receiee)

                image = encoded_frame_to_image(encoded_frame, image_width, image_height)
                image_path = f"{self.frame_index}.png"
                image.save(os.path.join(self.episode_dir, image_path))
                self.frame_index += 1

                key_actions = action["key_toggles"]
                # key_actions is a json array, if the key is in it, set to 1, else set to 0
                key_actions = [1 if key in key_actions else 0 for key in self.keys]

                mouse_actions = action["mouse_toggles"]
                # mouse_actions is a json array, if the mouse button is in it, set to 1, else set to 0
                mouse_actions = [1 if mouse_button in mouse_actions else 0 for mouse_button in self.mouse_buttons]

                mouse_move_x_action = action["mouse_move"][0]
                mouse_move_y_action = action["mouse_move"][1]

                state = flatten_array(
                    [key_obs, mouse_obs, mouse_x_obs, mouse_y_obs, image_path, key_actions, mouse_actions,
                     mouse_move_x_action, mouse_move_y_action])
                self.write_to_csv(state)
            return True
        elif state["context"] == "reset":
            status = state["body"]["status"]
            if status == "success":
                print("Environment reset successful")
                return True
            elif status == "failure":
                print("Environment reset failed, exiting")
                self.minecraft_client.terminate()
                return False
            elif status == "user_cancelled":
                print("Environment reset cancelled by user, exiting")
                self.minecraft_client.terminate()
                return False
            else:
                raise ValueError(f"Recieved invalid response from Minecraft client, expected status to be 'success', "
                                 f"'failure', or 'user_cancelled', got {status}")
        else:
            raise ValueError(f"Recieved invalid response from Minecraft client, expected context to be 'state' or "
                             f"'reset', got {state['context']}")




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

    def write_to_csv(self, state):
        self.state_csv_writer.writerow(state)
        print('wrote to csv')