from setuptools import setup

setup(
    name="mineplayer",
    version="0.0.1",
    install_requires=["gymnasium>=0.27.1",
                      "numpy>=1.23.5",
                      "Pillow>=9.4.0",
                      "minecraft-launcher-lib>=5.3",
                      "tqdm>=4.65.0"]
)