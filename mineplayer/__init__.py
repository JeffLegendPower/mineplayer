from gymnasium.envs.registration import register

register(
    id="mineplayer/Mineplayer-v0",
    entry_point="mineplayer.envs:MineplayerEnv",
)