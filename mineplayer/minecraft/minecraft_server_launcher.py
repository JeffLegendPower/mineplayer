import os
import shutil
import subprocess

import minecraft_launcher_lib
import modrinth
import requests


def install_minecraft_server(port):
    server_directory = os.path.join(minecraft_launcher_lib.utils.get_minecraft_directory(), "server")
    if not os.path.exists(server_directory):
        os.mkdir(server_directory)
    r = requests.get(
        "https://api.papermc.io/v2/projects/paper/versions/1.19.4/builds/540/downloads/paper-1.19.4-540.jar")

    with open(os.path.join(server_directory, "server.jar"), "wb") as f:
        f.write(r.content)

    with open(os.path.join(server_directory, "eula.txt"), "w") as f:
        f.write("eula=true")

    with open(os.path.join(server_directory, "server.properties"), "w") as f:
        write_server_properties(f, port)

    plugins_directory = os.path.join(server_directory, "plugins")
    if not os.path.exists(plugins_directory):
        os.mkdir(plugins_directory)

    install_plugins(plugins_directory)
    install_mineplayer(plugins_directory)

def launch_minecraft_server(ramGB=2):
    server_directory = os.path.join(minecraft_launcher_lib.utils.get_minecraft_directory(), "server")

    launch_cmd = ['java', f'-Xmx{ramGB}G', '-jar', 'server.jar', 'nogui']
    return subprocess.Popen(launch_cmd, cwd=server_directory)

def install_mineplayer(plugins_dir):
    parentdir = os.path.dirname(os.path.dirname(os.path.realpath(__file__)))
    mineplayer_client = os.path.join(parentdir, "MineplayerServer-1.0.0.jar")

    output_file = os.path.join(plugins_dir, "mineplayer_server.jar")

    shutil.copyfile(mineplayer_client, output_file)


def install_plugins(plugins_dir):
    def install_plugin_modrinth(modrinth_id, version_id, output_file):
        project = modrinth.Projects.ModrinthProject(modrinth_id)
        version = project.getVersion(version_id)
        primary_file = version.getPrimaryFile()
        download = version.getDownload(primary_file)

        r = requests.get(download)

        with open(os.path.join(plugins_dir, output_file), "wb") as f:
            f.write(r.content)

    def install_plugin(download, output_file):
        r = requests.get(download)

        with open(os.path.join(plugins_dir, output_file), "wb") as f:
            f.write(r.content)

    install_plugin_modrinth('1iWA0pjH', '4vsHFPCE', 'mckotlin-paper.jar')
    install_plugin('https://github.com/Multiverse/Multiverse-Core/releases/download/4.3.9/multiverse-core-4.3.9.jar', 'multiverse.jar')
    install_plugin('https://github.com/dmulloy2/ProtocolLib/releases/download/5.0.0/ProtocolLib.jar', 'protocollib.jar')

def write_server_properties(f, port):
    f.write("enable-jmx-monitoring=false\n")
    f.write("rcon.port=25575\n")
    f.write("level-seed=\n")
    f.write("gamemode=survival\n")
    f.write("enable-command-block=false\n")
    f.write("enable-query=false\n")
    f.write("generator-settings={}\n")
    f.write("enforce-secure-profile=true\n")
    f.write("level-name=world\n")
    f.write("motd=A Minecraft Server\n")
    f.write(f"query.port={port}\n")
    f.write("pvp=true\n")
    f.write("generate-structures=true\n")
    f.write("max-chained-neighbor-updates=1000000\n")
    f.write("difficulty=easy\n")
    f.write("network-compression-threshold=256\n")
    f.write("max-tick-time=60000\n")
    f.write("require-resource-pack=false\n")
    f.write("use-native-transport=true\n")
    f.write("max-players=20\n")
    f.write("online-mode=false\n")
    f.write("enable-status=true\n")
    f.write("allow-flight=false\n")
    f.write("initial-disabled-packs=\n")
    f.write("broadcast-rcon-to-ops=true\n")
    f.write("view-distance=10\n")
    f.write("server-ip=\n")
    f.write("resource-pack-prompt=\n")
    f.write("allow-nether=true\n")
    f.write(f"server-port={port}\n")
    f.write("enable-rcon=false\n")
    f.write("sync-chunk-writes=true\n")
    f.write("op-permission-level=4\n")
    f.write("prevent-proxy-connections=false\n")
    f.write("hide-online-players=false\n")
    f.write("resource-pack=\n")
    f.write("entity-broadcast-range-percentage=100\n")
    f.write("simulation-distance=10\n")
    f.write("rcon.password=\n")
    f.write("player-idle-timeout=0\n")
    f.write("debug=false\n")
    f.write("force-gamemode=false\n")
    f.write("rate-limit=0\n")
    f.write("hardcore=false\n")
    f.write("white-list=false\n")
    f.write("broadcast-console-to-ops=true\n")
    f.write("spawn-npcs=true\n")
    f.write("spawn-animals=true\n")
    f.write("function-permission-level=2\n")
    f.write("initial-enabled-packs=vanilla\n")
    f.write("level-type=minecraft\:normal\n")
    f.write("text-filtering-config=\n")
    f.write("spawn-monsters=true\n")
    f.write("enforce-whitelist=false\n")
    f.write("spawn-protection=16\n")
    f.write("resource-pack-sha1=\n")
    f.write("max-world-size=29999984\n")