import os
import subprocess

from setuptools import setup
from setuptools.command.install import install


class BuildAndInstallCommand(install):
    def run(self):
        mydir = os.path.abspath(os.path.dirname(__file__))
        # Change to the repository directory

        os.chdir("mineplayer/MineplayerClient")

        # Build the JAR file using gradle
        # check if windows
        gradlew = "gradlew.bat" if os.name == "nt" else "./gradlew"

        workdir = os.path.join(mydir, 'mineplayer')

        # self.gradle_downloadAssets(gradlew, os.path.join(workdir, "MineplayerClient"))
        self.gradle_build(gradlew, os.path.join(workdir, "MineplayerClient"))

        # self.gradle_downloadAssets(gradlew, os.path.join(workdir, "MineplayerServer"))
        self.gradle_build(gradlew, os.path.join(workdir, "MineplayerServer"))

        os.chdir(mydir)

        # copy the jar files to the mineplayer directory
        copy = "copy" if os.name == "nt" else "cp"
        subprocess.check_output(
            [f"{copy}",
             os.path.join(
                 mydir, "mineplayer", "MineplayerClient", "build", "libs", "MineplayerClient-1.0-SNAPSHOT.jar"),
             os.path.join(mydir, 'mineplayer')],
            cwd=mydir, shell=True)

        subprocess.check_output(
            [f"{copy}",
                os.path.join(
                    mydir, "mineplayer", "MineplayerServer", "build", "libs", "MineplayerServer-1.0-SNAPSHOT.jar"),
                os.path.join(mydir, 'mineplayer')],
            cwd=mydir, shell=True)

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
    # packages=["mineplayer", "mineplayer.envs", "mineplayer.minecraft"],
    include_package_data=True,
    package_data={
        "": ["*.jar"],
    },
    cmdclass={
        "install": BuildAndInstallCommand,
    },
)
