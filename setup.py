import os
from subprocess import check_call

from setuptools import setup
from setuptools.command.install import install


class BuildAndInstallCommand(install):
    def run(self):
        # Clone the repository
        os.system("git clone https://github.com/JeffLegendPower/mineplayer")

        # Change to the repository directory
        os.chdir("mineplayer/MineplayerClient")

        # Build the JAR file using gradle
        check_call(["./gradlew", "build"])

        # Copy the JAR file to the module directory
        # check if mineplayer dir already exists
        if not os.path.isdir("mineplayer"):
            os.makedirs("mineplayer", exist_ok=True)
        os.system("cp build/libs/MineplayerClient-1.0-SNAPSHOT.jar mineplayer/")

        # Call the parent run() method to complete the installation
        install.run(self)


setup(
    name="mineplayer",
    version="0.0.1",
    install_requires=["gymnasium>=0.27.1",
                      "numpy>=1.23.5",
                      "Pillow>=9.4.0",
                      "minecraft-launcher-lib>=5.3",
                      "tqdm>=4.65.0"],
    cmdclass={
        "install": BuildAndInstallCommand,
    }
)
