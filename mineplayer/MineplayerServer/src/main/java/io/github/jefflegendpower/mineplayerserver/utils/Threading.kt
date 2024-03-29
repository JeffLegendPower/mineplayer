package io.github.jefflegendpower.mineplayerserver.utils

import io.github.jefflegendpower.mineplayerserver.MineplayerServer
import org.bukkit.Bukkit

/**
 * Runs the given [runnable] on the main thread. Blocks until the runnable returns.
 * @param runnable The runnable to run on the main thread
 */
fun runOnMainThread(runnable: Runnable) {
    Bukkit.getScheduler().runTask(MineplayerServer.instance, runnable)
//    Bukkit.getScheduler().callSyncMethod(MineplayerServer.instance, supplier).get()
}

/**
 * Runs the given [supplier] on the main thread. Blocks until the supplier returns.
 * @param supplier The runnable to run on the main thread
 *
 */
fun <T> supplyFromMainThread(supplier: () -> T): T =
    Bukkit.getScheduler().callSyncMethod(MineplayerServer.instance, supplier).get()