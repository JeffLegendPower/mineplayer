import os
import subprocess

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
            gradlew = "gradlew.bat" if os.name == "nt" else "./gradlew"

            mydir = os.path.abspath(os.path.dirname(__file__))
            workdir = os.path.join(mydir, 'mineplayer', 'MineplayerClient')

        except Exception:
            raise Exception("2")

        self.gradle_downloadAssets(gradlew, workdir)
        self.gradle_build(gradlew, workdir)

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

    def gradle_downloadAssets(self, gradlew, workdir):
        # This may fail on the first try. Try few times
        n_trials = 3
        for i in range(n_trials):
            try:
                subprocess.check_call('{} downloadAssets'.format(gradlew).split(' '), cwd=workdir)
                return  # success
            except subprocess.CalledProcessError as e:
                if i == n_trials - 1:
                    raise e  # failed on last trial

    def gradle_build(self, gradlew, workdir):
        try:
            # check_call(["gradlew.bat", "build"])
            subprocess.call(f"{gradlew} clean build", shell=True, cwd=workdir)
        except Exception:
            raise Exception("gradle build windows")


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
