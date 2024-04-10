/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.utils

import org.jetbrains.annotations.ApiStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sun.misc.Unsafe
import java.lang.invoke.LambdaMetafactory
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.*
import java.util.function.Consumer
import java.util.function.Function

/**
 * A collection of (naughty) MethodHandle utilities.
 * <br></br>
 * This class allows for Java 8 and below style reflection-type methods, in modern Java versions due to a simple exploit to get the trusted method handle lookup instance.
 * <br></br>
 * MethodHandles also are "directly supported by the VM" and are "more efficient than the equivalent reflective operations", according to the documentation.
 */
object MHUtils {
    private val LOGGER: Logger = LoggerFactory.getLogger("OneConfig/MHUtils")

    @Suppress("UNCHECKED_CAST")
    class Result<out T>(@PublishedApi @JvmSynthetic internal val value: Any?) {
        inline val isSuccess get() = value !is Failure
        inline val isFailure get() = value is Failure

        inline fun getOrThrow() = if (isSuccess) value as T else throw (value as Failure).t
        inline fun getOrNull() = if (isSuccess) value as T else null
        inline fun getOrElse(default: @UnsafeVariance T) = if (isSuccess) value as T else default



        companion object {
            @JvmStatic
            fun <T> success(value: T) = Result<T>(value)
            @JvmStatic
            fun <T> failure(e: Throwable) = Result<T>(Failure(e))
        }

        @PublishedApi
        internal class Failure(@JvmSynthetic val t: Throwable)
    }

    private val theUnsafe = try {
        val f = Unsafe::class.java.getDeclaredField("theUnsafe")
        f.isAccessible = true
        f.get(null) as Unsafe
    } catch (e: Exception) {
        throw RuntimeException("Failed to get unsafe instance!", e)
    }

    /**
     * A reference to the trusted IMPL_LOOKUP field in [MethodHandles.Lookup].
     * <br></br> this field was extracted using unsupported methods, which breaks Java security checks. please be careful with it.
     */
    @ApiStatus.Internal
    @JvmField
    val trustedLookup: MethodHandles.Lookup = try {
        // tee hee
        val implLookup = MethodHandles.Lookup::class.java.getDeclaredField("IMPL_LOOKUP")
        theUnsafe.getObject(theUnsafe.staticFieldBase(implLookup), theUnsafe.staticFieldOffset(implLookup)) as MethodHandles.Lookup
    } catch (e: Exception) {
        LOGGER.error("Failed to get trusted lookup, things may break!", e)
        MethodHandles.lookup()
    }

    /**
     * # gav
     */
    private val gav: MethodHandle by lazy {
        trustedLookup.unreflectGetter(Proxy.getInvocationHandler(Deprecated::class.java.getAnnotation(Target::class.java)).javaClass.getDeclaredField("memberValues"))
    }


    // --- get --- //
    /**
     * Return a field handle, using reflection to get the field, then unreflecting it.
     *
     * @param owner the owner of the field.
     * @return a field handle, or null if it failed.
     */
    @JvmStatic
    fun getFieldGetter(fieldName: String, owner: Any) = try {
        val f = getFieldGetter(owner.javaClass.getDeclaredField(fieldName), owner)
        Result.success(f.getOrThrow())
    } catch (e: Exception) {
        Result.failure(ReflectiveOperationException("Failed to get field getter for $fieldName from $owner", e))
    }

    /**
     * Return a field handle by unreflecting the field.
     *
     * @param owner the owner of the field. If the field is static, this can be null.
     * @return a field handle, or null if it failed.
     */
    @JvmStatic
    fun getFieldGetter(f: Field, owner: Any?) = try {
        Result.success(
            if (Modifier.isStatic(f.modifiers)) trustedLookup.unreflectGetter(f)
            else trustedLookup.unreflectGetter(f).bindTo(owner)
        )
    } catch (e: Exception) {
        Result.failure(ReflectiveOperationException("Failed to get field getter for $f from $owner", e))
    }

    /**
     * Return a field handle using the trusted lookup.
     *
     * @return a field handle, or null if it failed.
     */
    @JvmStatic
    fun getFieldGetter(owner: Class<*>, fieldName: String, type: Class<*>) = try {
        Result.success(trustedLookup.findGetter(owner, fieldName, type))
    } catch (e: Exception) {
        Result.failure(ReflectiveOperationException("Failed to get field getter for $fieldName from $owner", e))
    }


    /**
     * Return a static field handle using the trusted lookup.
     *
     * @return a field handle, or null if it failed.
     */
    @JvmStatic
    fun getStaticFieldGetter(owner: Class<*>, fieldName: String, type: Class<*>) = try {
        Result.success(trustedLookup.findStaticGetter(owner, fieldName, type))
    } catch (e: Exception) {
        Result.failure(ReflectiveOperationException("Failed to get static field getter for $fieldName from $owner", e))
    }


    // --- set --- //
    /**
     * Return a field setter, using reflection to get the field, then unreflecting it.
     *
     * @param owner the owner of the field.
     * @return a field setter, or null if it failed.
     */
    @JvmStatic
    fun getFieldSetter(fieldName: String, owner: Any) = try {
        val f = getFieldSetter(owner.javaClass.getDeclaredField(fieldName), owner)
        Result.success(f.getOrThrow())
    } catch (e: Exception) {
        Result.failure(ReflectiveOperationException("Failed to get field setter for $fieldName from $owner", e))
    }

    /**
     * Return a field setter by unreflecting the field.
     *
     * @param owner the owner of the field. If the field is static, this can be null.
     * @return a field setter, or null if it failed.
     */
    @JvmStatic
    fun getFieldSetter(f: Field, owner: Any) = try {
        Result.success(
            if (Modifier.isStatic(f.modifiers)) trustedLookup.unreflectSetter(f)
            else trustedLookup.unreflectSetter(f).bindTo(owner)
        )
    } catch (e: Exception) {
        Result.failure(ReflectiveOperationException("Failed to get field setter for $f from $owner", e))
    }

    /**
     * Return a field setter using the trusted lookup.
     *
     * @return a field setter, or null if it failed.
     */
    @JvmStatic
    fun getFieldSetter(owner: Class<*>, fieldName: String, type: Class<*>) = try {
        Result.success(trustedLookup.findSetter(owner, fieldName, type))
    } catch (e: Exception) {
        Result.failure(ReflectiveOperationException("Failed to get field setter for $fieldName from $owner", e))
    }

    /**
     * Return a static field setter using the trusted lookup.
     *
     * @return a field setter, or null if it failed.
     */
    @JvmStatic
    fun getStaticFieldSetter(owner: Class<*>, fieldName: String, type: Class<*>) = try {
        Result.success(trustedLookup.findStaticSetter(owner, fieldName, type))
    } catch (e: Exception) {
        Result.failure(ReflectiveOperationException("Failed to get static field setter for $fieldName from $owner", e))
    }


    // --- method --- //
    /**
     * Return a method handle using the trusted lookup.
     *
     * @param owner the object instance where the method is located
     * @return a method handle, or null if it failed.
     */
    @JvmStatic
    fun getMethodHandle(owner: Any, methodName: String, returnType: Class<*>, vararg params: Class<*>?) = try {
        Result.success(trustedLookup.findVirtual(owner.javaClass, methodName, MethodType.methodType(returnType, params)).bindTo(owner))
    } catch (e: Exception) {
        Result.failure(ReflectiveOperationException("Failed to get method handle for $methodName from $owner", e))
    }

    /**
     * Return a method handle using the trusted lookup.
     *
     * @param owner the class where the method is located
     * @return a method handle, or null if it failed.
     */
    @JvmStatic
    fun getMethodHandle(owner: Class<*>, methodName: String, returnType: Class<*>, vararg params: Class<*>?) = try {
        Result.success(trustedLookup.findVirtual(owner, methodName, MethodType.methodType(returnType, params)))
    } catch (e: Exception) {
        Result.failure(ReflectiveOperationException("Failed to get method handle for $methodName from $owner", e))
    }

    /**
     * Return a static method handle using the trusted lookup.
     *
     * @return a method handle, or null if it failed.
     */
    @JvmStatic
    fun getStaticMethodHandle(owner: Class<*>, methodName: String, returnType: Class<*>, vararg params: Class<*>?) = try {
        Result.success(trustedLookup.findStatic(owner, methodName, MethodType.methodType(returnType, params)))
    } catch (e: Exception) {
        Result.failure(ReflectiveOperationException("Failed to get static method handle for $methodName from $owner", e))
    }

    /**
     * Return a method handle by unreflecting the method.
     *
     * @param owner the object instance where the method is located
     * @return a method handle, or null if it failed.
     */
    @JvmStatic
    fun getMethodHandle(m: Method, owner: Any) = try {
        Result.success(
            if (Modifier.isStatic(m.modifiers)) trustedLookup.unreflect(m)
            else trustedLookup.unreflect(m).bindTo(owner)
        )
    } catch (e: Exception) {
        Result.failure(ReflectiveOperationException("Failed to get method handle for $m from $owner", e))
    }


    // --- ctors --- //
    /**
     * Return a constructor handle using the trusted lookup.
     *
     * @return a constructor handle, or null if it failed.
     */
    @JvmStatic
    fun getConstructorHandle(owner: Class<*>, vararg params: Class<*>?) = try {
        Result.success(trustedLookup.findConstructor(owner, MethodType.methodType(Void.TYPE, params)))
    } catch (e: Exception) {
        Result.failure(ReflectiveOperationException("Failed to get constructor handle for $owner", e))
    }

    /**
     * Return a constructor handle by unreflecting the constructor.
     *
     * @return a constructor handle, or null if it failed.
     */
    @JvmStatic
    fun getConstructorHandle(ctor: Constructor<*>) = try {
        Result.success(trustedLookup.unreflectConstructor(ctor))
    } catch (e: Exception) {
        Result.failure(ReflectiveOperationException("Failed to get constructor handle for $ctor", e))
    }


    // -- lambda -- //
    /**
     * Returns a direct, fast lambda call site to the given consumer type method: void method(Object param)
     *
     * @param it the object on which the method is located - if it is static, pass a class instance here.
     * @return a fast wrapped method handle, or null if it failed.
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T> getConsumerFunctionHandle(it: Any, methodName: String, paramType: Class<T>) = try {
        val isStatic = it is Class<*>
        val cls = if (isStatic) it as Class<*> else it.javaClass
        val mt = MethodType.methodType(Void.TYPE, paramType)
        val mh = if (isStatic) trustedLookup.findStatic(cls, methodName, mt) else trustedLookup.findVirtual(cls, methodName, mt)
        val target = LambdaMetafactory.metafactory(
            // asm: due to the fact we use the trusted lookup, the only visible classes are ones on the bootstrap class loader,
            // as it is the lookup of java.lang.Object. therefore to access the object's class, we must make sure the lookup
            // has the access to the classloader of the object to avoid NoDefErrors, unfortunately meaning we have to lookup.in(cls)
            // fortunately we keep all privileges thanks to the short path of lookup.in(cls) so it still works
            // https://stackoverflow.com/questions/60144712/noclassdeffounderror-for-my-own-class-when-creating-callsite-with-lambdametafact
            trustedLookup.`in`(cls),
            "accept",
            MethodType.methodType(Consumer::class.java, cls),
            MethodType.methodType(Void.TYPE, Any::class.java),
            mh,
            mt
        ).target
        Result.success((if (isStatic) target.invokeExact() else target.invoke(it)) as Consumer<T>)
    } catch (e: Throwable) {
        Result.failure(ReflectiveOperationException("Failed to get wrapped method handle for $methodName from $it", e))

    }


    /**
     * Returns a direct, fast lambda call site to the given one arg, object returning method.
     *
     * @param it the object on which the method is located - if static, pass a class instance here.
     * @return a fast wrapped method handle, or null if it failed.
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T, R> getFunctionHandle(it: Any, methodName: String, returnType: Class<R>, paramType: Class<T>) = try {
        val isStatic = it is Class<*>
        val cls = if (isStatic) it as Class<*> else it.javaClass
        val mt = MethodType.methodType(returnType, paramType)
        val mh = if (isStatic) trustedLookup.findStatic(cls, methodName, mt) else trustedLookup.findVirtual(cls, methodName, mt)
        val target = LambdaMetafactory.metafactory(
            trustedLookup.`in`(cls),
            "apply",
            MethodType.methodType(Function::class.java, cls),
            MethodType.methodType(Any::class.java, Any::class.java),
            mh,
            mt
        ).target
        Result.success((if (isStatic) target.invokeExact() else target.invoke(it)) as Function<T, R>)
    } catch (e: Throwable) {
        Result.failure(ReflectiveOperationException("Failed to get wrapped method handle for $methodName from $it", e))
    }


// --- direct access methods --- //
    /**
     * Return a field value using reflection and the trusted lookup.
     *
     * @param owner the object instance where the field is located
     * @return a field value, or null if it failed.
     */
    @JvmStatic
    fun getField(owner: Any, fieldName: String) = try {
        Result.success(trustedLookup.unreflectGetter(owner.javaClass.getDeclaredField(fieldName)).invoke(owner))
    } catch (e: Throwable) {
        Result.failure(ReflectiveOperationException("Failed to get field value for $fieldName from $owner", e))
    }


    /**
     * Return a static field value using reflection and the trusted lookup.
     *
     * @return a field value, or null if it failed.
     */
    @JvmStatic
    fun getStaticField(cls: Class<*>, fieldName: String) = try {
        Result.success(trustedLookup.unreflectGetter(cls.getDeclaredField(fieldName)).invoke())
    } catch (e: Throwable) {
        Result.failure(ReflectiveOperationException("Failed to get static field value for $fieldName from $cls", e))
    }

    /**
     * Set a field value using reflection.
     *
     * @param owner the object instance where the field is located
     * @return true if it succeeded.
     */
    @JvmStatic
    fun setField(owner: Any, fieldName: String, value: Any?): Result<Any?> = try {
        Result.success(getFieldSetter(fieldName, owner).getOrThrow().invoke(value))
    } catch (e: Throwable) {
        Result.failure(ReflectiveOperationException("Failed to set field value for $fieldName from $owner", e))
    }

    /**
     * Set a static field value.
     *
     * @return true if it succeeded.
     */
    @JvmStatic
    fun setStaticField(cls: Class<*>, fieldName: String, value: Any): Result<Any?> = try {
        Result.success(getStaticFieldSetter(cls, fieldName, value.javaClass).getOrThrow().invoke(value))
    } catch (e: Throwable) {
        Result.failure(ReflectiveOperationException("Failed to set static field value for $fieldName from $cls", e))
    }

    /**
     * Invoke a method using the trusted lookup.
     *
     * @param owner the object instance where the method is located
     * @return the return value of the method, or null if it failed.
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T> invoke(owner: Any, methodName: String, returnType: Class<T>, vararg params: Any) = try {
        val classes = Array<Class<*>>(params.size) { params[it].javaClass }
        val mh = getMethodHandle(owner, methodName, returnType, *classes).getOrThrow()
        Result.success(mh.invoke(params) as T)
    } catch (e: Throwable) {
        Result.failure(ReflectiveOperationException("Failed to invoke method $methodName from $owner", e))
    }

    /**
     * Invoke a static method using the trusted lookup.
     *
     * @return the return value of the method, or null if it failed.
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T> invokeStatic(owner: Class<*>, methodName: String, returnType: Class<T>, vararg params: Any) = try {
        val classes = Array<Class<*>>(params.size) { params[it].javaClass }
        val mh = getStaticMethodHandle(owner, methodName, returnType, *classes).getOrThrow()
        Result.success(mh.invoke(params) as T)
    } catch (e: Throwable) {
        Result.failure(ReflectiveOperationException("Failed to invoke static method $methodName from $owner", e))
    }

    /**
     * Instantiate a class using the ctor matching the given params.
     */
    @JvmStatic
    fun instantiate(cls: Class<*>, vararg params: Any) = try {
        val classes = Array<Class<*>>(params.size) { params[it].javaClass }
        val mh = getConstructorHandle(cls, *classes).getOrThrow()
        Result.success(mh.invoke(params))
    } catch (e: Throwable) {
        Result.failure(ReflectiveOperationException("Failed to instantiate $cls", e))
    }

    /**
     * Instantiate a class using the no-args ctor. If allocateAnyway is true and there is no no-args ctor, it will be allocated using the unsafe.
     */
    @JvmStatic
    fun instantiate(cls: Class<*>, allocateAnyway: Boolean) = try {
        val mh = getConstructorHandle(cls).getOrThrow()
        Result.success(mh.invoke())
    } catch (e: Throwable) {
        if (!allocateAnyway) {
            Result.failure(ReflectiveOperationException("Failed to instantiate $cls", e))
        } else try {
            Result.success(theUnsafe.allocateInstance(cls))
        } catch (ee: Exception) {
            Result.failure(ReflectiveOperationException("Failed to Unsafe allocate $cls", ee.initCause(e)))
        }
    }


    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T> invokeCatching(mh: MethodHandle, vararg args: Any?) = try {
        Result.success(mh.invoke(args) as T)
    } catch (e: Throwable) {
        Result.failure(ReflectiveOperationException("Failed to invoke method handle $mh", e))
    }

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T> invokeExactCatching(mh: MethodHandle, vararg args: Any?) = try {
        Result.success(mh.invokeExact(args) as T)
    } catch (e: Throwable) {
        Result.failure(ReflectiveOperationException("Failed to invoke exact method handle $mh", e))
    }

// --- annotation --- //
    /**
     * Return a map of all values attached to this annotation.
     *
     * This method is considerably faster than reflection.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun getAnnotationValues(a: Annotation?) = try {
        Result.success(gav.invoke(Proxy.getInvocationHandler(a)) as Map<String, Any>)
    } catch (e: Throwable) {
        Result.failure(ReflectiveOperationException("Failed to get annotation values for $a", e))
    }

}