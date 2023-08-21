import time

import numpy as np
import tensorflow as tf
from keras.layers import Dense, Input, Conv2D, Flatten
# import keras
from tensorflow import keras

from mineplayer.envs import MineplayerEnv
from mineplayer.human import HumanEnv
from mineplayer.minecraft.minecraft_server_launcher import install_minecraft_server, launch_minecraft_server

import tkinter as tk


def get_model(num_keys, num_buttons):
    input_window = Input(shape=(180, 320, 3))
    input_keys = Input(shape=(num_keys,))
    input_buttons = Input(shape=(num_buttons,))
    input_mouse_loc = Input(shape=(2,))
    x = Conv2D(4, (2, 2), activation='relu')(input_window)
    # x = Conv2D(8, (2, 2), activation='relu')(x)
    x = Flatten()(x)
    x = keras.layers.concatenate([x, input_keys, input_buttons, input_mouse_loc])
    x = Dense(64, activation='relu')(x)
    x = Dense(32, activation='relu')(x)

    output_keys = Dense(num_keys, activation='sigmoid', name='output_keys')(x)
    output_buttons = Dense(num_buttons, activation='sigmoid', name='output_buttons')(x)
    output_mouse_move = Dense(2, activation='linear', name='output_mouse_move')(x)

    return keras.Model(inputs=[input_window, input_keys, input_buttons, input_mouse_loc],
                       outputs=[output_keys, output_buttons, output_mouse_move])


def test_mineplayer():
    install_minecraft_server(25565)
    server = launch_minecraft_server()

    def button_click():
        # This function is called when the button is clicked
        window.destroy()  # Close the window
    # Continue with the rest of your code here

    env = MineplayerEnv(
        env_type="treechop",
        props={
            "log_goal": 4
        },
        window_width=320,
        window_height=180,
    )

    # model = get_model(5, 2)
    #
    # optimizer = keras.optimizers.Adam(learning_rate=0.001)
    #
    # rewards_history = []
    # done_history = []
    # episode_reward_history = []
    # running_reward = 0
    # episode_count = 0
    # frame_count = 0
    #
    # max_memory_length = 100000
    #
    # for i in range(10):  # run 10 times
    #     obs, info = env.reset()
    #     episode_reward = 0
    #
    #     average_tick_time = 0
    #
    #     for timestep in range(1, 1000):
    #         start_time = time.time()
    #         frame_count += 1
    #
    #         # Predict action Q-values
    #         # From environment state
    #         window_obs = np.array(obs["window"])
    #         window_obs = np.expand_dims(window_obs, 0)
    #
    #         key_obs = obs["key_states"]
    #         key_obs = np.array([
    #             1 if 87 in key_obs else 0,
    #             1 if 65 in key_obs else 0,
    #             1 if 83 in key_obs else 0,
    #             1 if 68 in key_obs else 0,
    #             1 if 32 in key_obs else 0
    #         ])
    #         key_obs = np.expand_dims(key_obs, 0)
    #
    #         mouse_obs = obs["mouse_states"]
    #         mouse_obs = np.array([
    #             1 if 1 in mouse_obs else 0,
    #             1 if 2 in mouse_obs else 0
    #         ])
    #         mouse_obs = np.expand_dims(mouse_obs, 0)
    #
    #         mouse_loc_obs = np.array(obs["mouse_pos"])
    #         mouse_loc_obs = np.expand_dims(mouse_loc_obs, 0)
    #
    #         obs = [window_obs, key_obs, mouse_obs, mouse_loc_obs]
    #         action = model(obs, training=False)
    #         action_keys = action[0][0]
    #         action_buttons = action[1][0]
    #         action_mouse_move = action[2][0]
    #
    #         key_toggles = [
    #             87 if action_keys[0] > 0.5 else None,
    #             65 if action_keys[1] > 0.5 else None,
    #             83 if action_keys[2] > 0.5 else None,
    #             68 if action_keys[3] > 0.5 else None,
    #             32 if action_keys[4] > 0.5 else None
    #         ]
    #         key_toggles = [key for key in key_toggles if key is not None]
    #
    #         mouse_toggles = [
    #             1 if action_buttons[0] > 0.5 else None,
    #             2 if action_buttons[1] > 0.5 else None
    #         ]
    #         mouse_toggles = [mouse for mouse in mouse_toggles if mouse is not None]
    #
    #         action = {
    #             "key_toggles": key_toggles,
    #             "mouse_toggles": mouse_toggles,
    #             "mouse_move": np.array(action_mouse_move).astype('float64')
    #         }
    #         print(action_mouse_move) if timestep % 10 == 0 else None
    #         obs, reward, done, _, _ = env.step(action)
    #
    #         # episode_reward += reward
    #         # done_history.append(done)
    #         # rewards_history.append(reward)
    #
    #         average_tick_time = (average_tick_time + (time.time() - start_time)) / 2
    #         print("Average tick time: {:.2f}ms".format(average_tick_time * 1000)) if timestep % 10 == 0 else None
    #
    #         # Limit the state and reward history
    #         # if len(rewards_history) > max_memory_length:
    #         #     del rewards_history[:1]
    #             # del state_history[:1]
    #             # del state_next_history[:1]
    #             # del action_history[:1]
    #             # del done_history[:1]
    #
    #         # if done:
    #         #     with tf.GradientTape() as tape:
    #         #         loss = pow(4 / (episode_reward + 1), 3)
    #         #         gradients = tape.gradient(loss, model.trainable_variables)
    #         #         optimizer.apply_gradients(zip(gradients, model.trainable_variables))
    #         #     template = "running reward: {:.2f} at episode {}, frame count {}"
    #         #     print(template.format(running_reward, episode_count, frame_count))
    #         #     break
    #
    #     # episode_reward_history.append(episode_reward)
    #     # if len(episode_reward_history) > 100:
    #     #     del episode_reward_history[:1]
    #     # running_reward = np.mean(episode_reward_history)
    #
    #     episode_count += 1
    #
    #     if running_reward > 40:  # Condition to consider the task solved
    #         print("Solved at episode {}!".format(episode_count))
    #         break
    #
    # # env.reset()
    # #
    # # time.sleep(20)
    # #
    # # action = {
    # #     "key_toggles": [1],
    # #     "mouse_toggles": [],
    # #     "mouse_move": [0, 0]
    # # }
    # #
    # # env.step(action)
    # #
    # # time.sleep(20)
    #
    window = tk.Tk()

    # Create a button widget
    button = tk.Button(window, text="Click Me", command=button_click)
    button.pack()

    # Start the main event loop
    window.mainloop()

    env.close()

    # server.terminate()

def test_human_env():
    try:
        install_minecraft_server(25565)
        server = launch_minecraft_server(ramGB=3)

        def button_click():
            # This function is called when the button is clicked
            window.destroy()  # Close the window
        # Continue with the rest of your code here

        window = tk.Tk()

        # Create a button widget
        button = tk.Button(window, text="Click Me", command=button_click)
        button.pack()

        # Start the main event loop
        window.mainloop()

        env = HumanEnv(
            output_dir="/Users/ishaangoyal/IdeaProjects/mineplayer/tests/data",
            keys=["87", "65", "83", "68", "32"],
            mouse_buttons=["0", "1"],
            env_type="treechop",
            props={
                "log_goal": 4
            },
            window_width=320,
            window_height=180,
        )
    finally:
        server.terminate()
        env.minecraft_client.terminate()


if __name__ == '__main__':
    print("Start testing mineplayer...")
    # test_mineplayer()
    test_human_env()
    print("Test complete")
