package io.github.jefflegendpower.mineplayerserver.utils

import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


@Throws(IOException::class)
fun unzip(input: InputStream, targetDir: Path) {
    var targetDirNew: Path = targetDir.toAbsolutePath()
    targetDirNew = targetDirNew.toAbsolutePath()
    ZipInputStream(input).use { zipIn ->
        var ze: ZipEntry?
        while (zipIn.nextEntry.also { ze = it } != null) {
            val resolvedPath: Path = targetDirNew.resolve(ze!!.name).normalize()
            if (ze!!.isDirectory) {
                Files.createDirectories(resolvedPath)
            } else {
                Files.createDirectories(resolvedPath.parent)
                Files.copy(zipIn, resolvedPath)
            }
        }
    }
}