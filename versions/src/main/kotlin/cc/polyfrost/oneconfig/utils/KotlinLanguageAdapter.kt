//#if MC<=11202
package cc.polyfrost.oneconfig.utils

import net.minecraftforge.fml.common.FMLModContainer
import net.minecraftforge.fml.common.ILanguageAdapter
import net.minecraftforge.fml.common.ModContainer
import net.minecraftforge.fml.relauncher.Side
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * An adapter for FML to allow for the use of Kotlin objects as a mod class.
 * This is not required if you use a Kotlin class, only if you use a Kotlin object.
 *
 * Adapted from Crimson under LGPL 3.0
 * https://github.com/Deftu-Archive/Crimson/blob/main/LICENSE
 */
class KotlinLanguageAdapter : ILanguageAdapter {

    override fun supportsStatics(): Boolean = false
    override fun getNewInstance(
        container: FMLModContainer?, objectClass: Class<*>, classLoader: ClassLoader?, factoryMarkedAnnotation: Method?
    ): Any = objectClass.kotlin.objectInstance ?: objectClass.getDeclaredConstructor().newInstance()

    override fun setProxy(target: Field, proxyTarget: Class<*>, proxy: Any?) {
        target.set(proxyTarget.kotlin.objectInstance, proxy)
    }

    override fun setInternalProxies(mod: ModContainer?, side: Side?, loader: ClassLoader?) {
    }

}
//#endif
