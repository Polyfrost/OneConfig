package cc.polyfrost.oneconfig.utils.dsl

import cc.polyfrost.oneconfig.utils.NetworkUtils
import cc.polyfrost.oneconfig.libs.universal.UDesktop
import java.io.File

/**
 * Returns the SHA-256 hash of the given [File].
 *
 * @see NetworkUtils.getFileChecksum
 */
fun File.checksum() = NetworkUtils.getFileChecksum(this)

/**
 * Downloads the given [url] to the given [File].
 *
 * @see NetworkUtils.downloadFile
 */
fun File.download(url: String, userAgent: String = "OneConfig/1.0.0", timeout: Int = 5000, useCaches: Boolean = false) =
    NetworkUtils.downloadFile(url, this, userAgent, timeout, useCaches)

/**
 * Launches a URL in the default browser.
 *
 * @see NetworkUtils.browseLink
 */
@Suppress("unused")
fun UDesktop.browseLink(uri: String) = NetworkUtils.browseLink(uri)