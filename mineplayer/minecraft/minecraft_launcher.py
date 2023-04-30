import minecraft_launcher_lib
import requests
import tqdm
import os
import modrinth

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

    print("Installed Minecraft 1.19.4")
    print("Installing generic performance-enhancing mods...")
    install_mods(minecraft_directory)
    print("Installed generic performance-enhancing mods")
    print("Installing mineplayer mod...")
    # TODO: Install mineplayer mod

def install_mods(minecraft_dir):
    mods_dir = os.path.join(minecraft_dir, "mods")
    if not os.path.exists(mods_dir):
        os.makedirs(mods_dir)

    def install_mod(modrinth_id, version_id, output_file):
        project = modrinth.Projects.ModrinthProject(modrinth_id)
        version = project.getVersion(version_id)
        primary_file = version.getPrimaryFile()
        download = version.getDownload(primary_file)

        r = requests.get(download)

        with open(os.path.join(mods_dir, output_file), "wb") as f:
            f.write(r.content)

    install_mod('P7dR8mSH', 'qvrUMd9Z', 'fabric-api.jar')
    install_mod('AANobbMI', 'b4hTi3mo', 'sodium.jar')
    install_mod('NNAgCjsB', 'UvJN5Cy4', 'entityculling.jar')
    install_mod('uXXizFIs', 'RbR7EG8T', 'ferritecore.jar')
    install_mod('fQEb0iXm', 'Tncui9tU', 'krypton.jar')
    install_mod('FWumhS4T', 'I9TkHxLI', 'smoothboot.jar')
    install_mod('NRjRiSSD', 'PtXTwQt6', 'memoryleakfix.jar')
    install_mod('H8CaAYZC', 'qH1xCwoC', 'starlight.jar')
    install_mod('5ZwdcRci', '8IFFeKYy', 'immediatelyfast.jar')

def run_minecraft():
