package cc.polyfrost.oneconfig.utils.dsl

import cc.polyfrost.oneconfig.utils.IOUtils
import cc.polyfrost.oneconfig.utils.NetworkUtils
import java.io.File

/**
 * Returns the SHA-256 hash of the given [File].
 *
 * @see NetworkUtils.getFileChecksum
 */
fun File.checksum() = IOUtils.getFileChecksum(this)