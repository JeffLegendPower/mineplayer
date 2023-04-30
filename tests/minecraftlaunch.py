# import minecraft_launcher_lib
# import subprocess
import os

import minecraft_launcher_lib
import tqdm
import modrinth
import requests


def install_minecraft():
    minecraft_directory = minecraft_launcher_lib.utils.get_minecraft_directory()

    progress_bar = tqdm.tqdm(total=100)

    def set_status(new_status):
        try:
            progress_bar.set_description(new_status)
        except NameError:
            print(new_status)

    def set_progress(new_progress):
        progress_bar.update(n=new_progress)

    def set_max(new_max):
        progress_bar.total = new_max
        progress_bar.refresh()

    installation_callback = {
        "setStatus": set_status,
        "setProgress": set_progress,
        "setMax": set_max
    }

    minecraft_launcher_lib.fabric.install_fabric("1.19.4", minecraft_directory, callback=installation_callback)

if __name__ == '__main__':
    install_minecraft()
    # minecraft_directory = r"C:\Users\goyal_hfho3dz\AppData\Roaming\.tlauncher\legacy\Minecraft\game"
    #
    # # print(minecraft_launcher_lib.utils.get_available_versions(minecraft_directory)[-1])
    #
    # for version in minecraft_launcher_lib.utils.get_available_versions(minecraft_directory):
    #     print(version['id'])
    #
    # # minecraft_launcher_lib.fabric.install_fabric("1.19.4", minecraft_directory)
    #
    # options = minecraft_launcher_lib.utils.generate_test_options()
    # options["username"] = "JeffLegendPower"
    #
    # minecraft_command = minecraft_launcher_lib.command.get_minecraft_command("Quilt 1.19.4", minecraft_directory, options)
    # print(minecraft_command)

    # subprocess.call(minecraft_command)
