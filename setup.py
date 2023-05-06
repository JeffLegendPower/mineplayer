import os
import subprocess
from subprocess import check_call

from setuptools import setup
from setuptools.command.install import install


class BuildAndInstallCommand(install):
    def run(self):
        # Change to the repository directory
        try:
            os.chdir("mineplayer/MineplayerClient")
        except Exception:
            raise Exception("1")

        # Build the JAR file using gradle
        try:
            # check_call(["./gradlew", "build"])
            # check if windows
            if os.name == "nt":
                self.gradle_build_windows()
            else:
                self.gradle_build_linux()
        except Exception:
            raise Exception("2")

        # Copy the JAR file to the module directory
        # check if mineplayer dir already exists
        try:
            if not os.path.isdir("mineplayer"):
                os.makedirs("mineplayer", exist_ok=True)
        except Exception:
            raise Exception("3")

        try:
            os.system("cp build/libs/MineplayerClient-1.0-SNAPSHOT.jar mineplayer/")
        except Exception:
            raise Exception("4")

        # Call the parent run() method to complete the installation
        install.run(self)

    def gradle_build_windows(self):
        try:
            # check_call(["gradlew.bat", "build"])
            subprocess.call("gradlew.bat build", shell=True)
        except Exception:
            raise Exception("gradle build windows")

    def gradle_build_linux(self):
        try:
            # check_call(["./gradlew", "build"])
            subprocess.call("./gradlew build", shell=True)
        except Exception:
            raise Exception("gradle build linux")


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
    },
)
