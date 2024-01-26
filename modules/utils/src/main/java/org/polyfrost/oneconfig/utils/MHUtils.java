/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.utils;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A collection of (naughty) MethodHandle utilities.
 * <br>
 * This class allows for Java 8 and below style reflection-type methods, in modern Java versions due to a simple exploit to get the trusted method handle lookup instance.
 * <br>
 * MethodHandles also are "directly supported by the VM" and are "more efficient than the equivalent reflective operations", according to the documentation.
 */
@SuppressWarnings({"unused", "unchecked"})
public class MHUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("OneConfig/MHUtils");
    private static final sun.misc.Unsafe theUnsafe;
    /**
     * A reference to the trusted IMPL_LOOKUP field in {@link MethodHandles.Lookup}.
     * <br> this field was extracted using unsupported methods, which breaks Java security checks. please be careful with it.
     */
    @ApiStatus.Internal
    public static final MethodHandles.Lookup trustedLookup;
    /**
     * <h1>gav</h1>
     */
    private static MethodHandle gav /* by lazy { getAnnotationValuesMethodHandle() } */;


    // --- get --- //

    /**
     * Return a field handle, using reflection to get the field, then unreflecting it.
     *
     * @param owner the owner of the field.
     * @return a field handle, or null if it failed.
     */
    public static @Nullable MethodHandle getFieldGetter(String fieldName, @NotNull Object owner) {
        try {
            Field f = owner.getClass().getDeclaredField(fieldName);
            return getFieldGetter(f, owner);
        } catch (Exception e) {
            LOGGER.error("Failed to get field getter for {} from {}", fieldName, owner, e);
            return null;
        }
    }

    /**
     * Return a field handle by unreflecting the field.
     *
     * @param owner the owner of the field. If the field is static, this can be null.
     * @return a field handle, or null if it failed.
     */
    public static @Nullable MethodHandle getFieldGetter(Field f, @Nullable Object owner) {
        try {
            MethodHandle mh = trustedLookup.unreflectGetter(f);
            if (!Modifier.isStatic(f.getModifiers())) {
                mh = mh.bindTo(owner);
            }
            return mh;
        } catch (Exception e) {
            LOGGER.error("Failed to get field getter for {} from {}", f, owner, e);
            return null;
        }
    }

    /**
     * Return a field handle using the trusted lookup.
     *
     * @return a field handle, or null if it failed.
     */
    public static @Nullable MethodHandle getFieldGetter(Class<?> owner, String fieldName, Class<?> type) {
        try {
            return trustedLookup.findGetter(owner, fieldName, type);
        } catch (Exception e) {
            LOGGER.error("Failed to get field getter for {} from {}", fieldName, owner, e);
            return null;
        }
    }


    /**
     * Return a static field handle using the trusted lookup.
     *
     * @return a field handle, or null if it failed.
     */
    public static @Nullable MethodHandle getStaticFieldGetter(Class<?> owner, String fieldName, Class<?> type) {
        try {
            return trustedLookup.findStaticGetter(owner, fieldName, type);
        } catch (Exception e) {
            LOGGER.error("Failed to get static field getter for {} from {}", fieldName, owner, e);
            return null;
        }
    }


    // --- set --- //

    /**
     * Return a field setter, using reflection to get the field, then unreflecting it.
     *
     * @param owner the owner of the field.
     * @return a field setter, or null if it failed.
     */
    public static @Nullable MethodHandle getFieldSetter(String fieldName, @NotNull Object owner) {
        try {
            Field f = owner.getClass().getDeclaredField(fieldName);
            return getFieldSetter(f, owner);
        } catch (Exception e) {
            LOGGER.error("Failed to get field setter for {} from {}", fieldName, owner, e);
            return null;
        }
    }

    /**
     * Return a field setter by unreflecting the field.
     *
     * @param owner the owner of the field. If the field is static, this can be null.
     * @return a field setter, or null if it failed.
     */
    public static @Nullable MethodHandle getFieldSetter(Field f, @Nullable Object owner) {
        try {
            MethodHandle mh = trustedLookup.unreflectSetter(f);
            if (!Modifier.isStatic(f.getModifiers())) {
                mh = mh.bindTo(owner);
            }
            return mh;
        } catch (Exception e) {
            LOGGER.error("Failed to get field setter for {} from {}", f, owner, e);
            return null;
        }
    }

    /**
     * Return a field setter using the trusted lookup.
     *
     * @return a field setter, or null if it failed.
     */
    public static @Nullable MethodHandle getFieldSetter(Class<?> owner, String fieldName, Class<?> type) {
        try {
            return trustedLookup.findSetter(owner, fieldName, type);
        } catch (Exception e) {
            LOGGER.error("Failed to get field setter for {} from {}", fieldName, owner, e);
            return null;
        }
    }

    /**
     * Return a static field setter using the trusted lookup.
     *
     * @return a field setter, or null if it failed.
     */
    public static @Nullable MethodHandle getStaticFieldSetter(Class<?> owner, String fieldName, Class<?> type) {
        try {
            return trustedLookup.findStaticSetter(owner, fieldName, type);
        } catch (Exception e) {
            LOGGER.error("Failed to get static field setter for {} from {}", fieldName, owner, e);
            return null;
        }
    }


    // --- method --- //

    /**
     * Return a method handle using the trusted lookup.
     *
     * @param owner the object instance where the method is located
     * @return a method handle, or null if it failed.
     */
    public static @Nullable MethodHandle getMethodHandle(@NotNull Object owner, String methodName, Class<?> returnType, Class<?>... params) {
        try {
            return trustedLookup.findVirtual(owner.getClass(), methodName, MethodType.methodType(returnType, params)).bindTo(owner);
        } catch (Exception e) {
            LOGGER.error("Failed to get method handle for {} from {}", methodName, owner, e);
            return null;
        }
    }

    /**
     * Return a method handle using the trusted lookup.
     *
     * @param owner the class where the method is located
     * @return a method handle, or null if it failed.
     */
    public static @Nullable MethodHandle getMethodHandle(Class<?> owner, String methodName, Class<?> returnType, Class<?>... params) {
        try {
            return trustedLookup.findVirtual(owner, methodName, MethodType.methodType(returnType, params));
        } catch (Exception e) {
            LOGGER.error("Failed to get method handle for {} from {}", methodName, owner, e);
            return null;
        }
    }

    /**
     * Return a static method handle using the trusted lookup.
     *
     * @return a method handle, or null if it failed.
     */
    public static @Nullable MethodHandle getStaticMethodHandle(Class<?> owner, String methodName, Class<?> returnType, Class<?>... params) {
        try {
            return trustedLookup.findStatic(owner, methodName, MethodType.methodType(returnType, params));
        } catch (Exception e) {
            LOGGER.error("Failed to get static method handle for {} from {}", methodName, owner, e);
            return null;
        }
    }

    /**
     * Return a method handle using reflection.
     *
     * @param owner the object instance where the method is located
     * @return a method handle, or null if it failed.
     */
    public static @Nullable MethodHandle getMethodHandle(Object owner, String methodName, Class<?> params) {
        try {
            Method m = owner.getClass().getDeclaredMethod(methodName, params);
            return getMethodHandle(m, owner);
        } catch (Exception e) {
            LOGGER.error("Failed to get method handle for {} from {}", methodName, owner, e);
            return null;
        }
    }

    /**
     * Return a method handle by unreflecting the method.
     *
     * @param owner the object instance where the method is located
     * @return a method handle, or null if it failed.
     */
    public static @Nullable MethodHandle getMethodHandle(Method m, Object owner) {
        try {
            if (Modifier.isStatic(m.getModifiers())) {
                return trustedLookup.unreflect(m);
            } else {
                return trustedLookup.unreflect(m).bindTo(owner);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get method handle for {} from {}", m, owner, e);
            return null;
        }
    }


    // --- ctors --- //

    /**
     * Return a constructor handle using the trusted lookup.
     *
     * @return a constructor handle, or null if it failed.
     */
    public static @Nullable MethodHandle getConstructorHandle(Class<?> owner, Class<?>... params) {
        try {
            MethodType mt = params.length == 0 ? MethodType.methodType(void.class) : MethodType.methodType(void.class, params);
            return trustedLookup.findConstructor(owner, mt);
        } catch (Exception e) {
            if (params.length != 0) LOGGER.error("Failed to get constructor handle for {}", owner, e);
            return null;
        }
    }

    /**
     * Return a constructor handle by unreflecting the constructor.
     *
     * @return a constructor handle, or null if it failed.
     */
    public static @Nullable MethodHandle getConstructorHandle(Constructor<?> ctor) {
        try {
            return trustedLookup.unreflectConstructor(ctor);
        } catch (Exception e) {
            LOGGER.error("Failed to get constructor handle for {}", ctor, e);
            return null;
        }
    }


    // -- lambda -- //

    /**
     * Returns a direct, fast lambda call site to the given consumer type method: void method(Object param)
     *
     * @param object the object on which the method is located - if it is static, pass a class instance here.
     * @return a fast wrapped method handle, or null if it failed.
     */
    public static <T> @Nullable Consumer<T> getConsumerFunctionHandle(Object object, String methodName, Class<T> paramType) {
        try {
            boolean isStatic = object instanceof Class;
            Class<?> cls = isStatic ? (Class<?>) object : object.getClass();
            MethodType mt = MethodType.methodType(void.class, paramType);
            MethodHandle mh = isStatic ? trustedLookup.findStatic(cls, methodName, mt) : trustedLookup.findVirtual(cls, methodName, mt);
            MethodHandle target = LambdaMetafactory.metafactory(
                    // asm: due to the fact we use the trusted lookup, the only visible classes are ones on the bootstrap class loader,
                    // as it is the lookup of java.lang.Object. therefore to access the object's class, we must make sure the lookup
                    // has the access to the classloader of the object to avoid NoDefErrors, unfortunately meaning we have to lookup.in(cls)
                    // fortunately we keep all privileges thanks to the short path of lookup.in(cls) so it still works
                    // https://stackoverflow.com/questions/60144712/noclassdeffounderror-for-my-own-class-when-creating-callsite-with-lambdametafact
                    trustedLookup.in(cls),
                    "accept",
                    MethodType.methodType(Consumer.class, cls),
                    MethodType.methodType(void.class, Object.class),
                    mh,
                    mt
            ).getTarget();
            return (Consumer<T>) (isStatic ? target.invokeExact() : target.invoke(object));
        } catch (Throwable e) {
            LOGGER.error("Failed to get wrapped method handle for {} from {}", methodName, object, e);
            return null;
        }
    }

    /**
     * Returns a direct, fast lambda call site to the given one arg, object returning method.
     *
     * @param object the object on which the method is located - if static, pass a class instance here.
     * @return a fast wrapped method handle, or null if it failed.
     */
    public static <T, R> @Nullable Function<T, R> getFunctionHandle(Object object, String methodName, Class<R> returnType, Class<T> paramType) {
        try {
            boolean isStatic = object instanceof Class;
            Class<?> cls = isStatic ? (Class<?>) object : object.getClass();
            MethodType mt = MethodType.methodType(returnType, paramType);
            MethodHandle mh = isStatic ? trustedLookup.findStatic(cls, methodName, mt) : trustedLookup.findVirtual(cls, methodName, mt);
            MethodHandle target = LambdaMetafactory.metafactory(
                    trustedLookup.in(cls),
                    "apply",
                    MethodType.methodType(Function.class, cls),
                    MethodType.methodType(Object.class, Object.class),
                    mh,
                    mt
            ).getTarget();
            return (Function<T, R>) (isStatic ? target.invokeExact() : target.invoke(object));
        } catch (Throwable e) {
            LOGGER.error("Failed to get wrapped method handle for {} from {}", methodName, object, e);
            return null;
        }
    }


    // --- direct access methods --- //

    /**
     * Return a field value using reflection and the trusted lookup.
     *
     * @param owner the object instance where the field is located
     * @return a field value, or null if it failed.
     */
    public static @Nullable Object getField(@NotNull Object owner, String fieldName) {
        try {
            MethodHandle mh = getFieldGetter(fieldName, owner);
            if (mh == null) return null;
            return mh.invoke();
        } catch (Throwable e) {
            LOGGER.error("Failed to get field value for {} from {}", fieldName, owner, e);
            return null;
        }
    }

    /**
     * Return a static field value using reflection and the trusted lookup.
     *
     * @return a field value, or null if it failed.
     */
    public static @Nullable Object getStaticField(Class<?> cls, String fieldName) {
        try {
            MethodHandle mh = getFieldGetter(cls.getDeclaredField(fieldName), null);
            if (mh == null) return null;
            return mh.invoke();
        } catch (Throwable e) {
            LOGGER.error("Failed to get static field value for {} from {}", fieldName, cls, e);
            return null;
        }
    }

    /**
     * Set a field value using reflection.
     *
     * @param owner the object instance where the field is located
     * @return true if it succeeded.
     */
    public static boolean setField(@NotNull Object owner, String fieldName, Object value) {
        try {
            MethodHandle mh = getFieldSetter(fieldName, owner);
            if (mh == null) return false;
            mh.invoke(value);
            return true;
        } catch (Throwable e) {
            LOGGER.error("Failed to set field value for {} from {}", fieldName, owner, e);
            return false;
        }
    }

    /**
     * Set a static field value using reflection.
     *
     * @return true if it succeeded.
     */
    public static boolean setStaticField(Class<?> cls, String fieldName, Object value) {
        try {
            MethodHandle mh = getFieldSetter(cls.getDeclaredField(fieldName), null);
            if (mh == null) return false;
            mh.invoke(value);
            return true;
        } catch (Throwable e) {
            LOGGER.error("Failed to set static field value for {} from {}", fieldName, cls, e);
            return false;
        }
    }

    /**
     * Invoke a method using the trusted lookup.
     *
     * @param owner the object instance where the method is located
     * @return the return value of the method, or null if it failed.
     */
    public static <T> @Nullable T invoke(@NotNull Object owner, String methodName, Class<T> returnType, Object... params) {
        try {
            Class<?>[] classes = new Class<?>[params.length];
            for (int i = 0; i < params.length; i++) {
                classes[i] = params[i].getClass();
            }
            MethodHandle mh = getMethodHandle(owner, methodName, returnType, classes);
            if (mh == null) return null;
            return (T) mh.invoke(params);
        } catch (Throwable e) {
            LOGGER.error("Failed to invoke method {} from {}", methodName, owner, e);
            return null;
        }
    }

    /**
     * Invoke a static method using the trusted lookup.
     *
     * @return the return value of the method, or null if it failed.
     */
    public static <T> @Nullable T invokeStatic(Class<?> owner, String methodName, Class<T> returnType, @NotNull Object... params) {
        try {
            Class<?>[] classes = new Class<?>[params.length];
            for (int i = 0; i < params.length; i++) {
                classes[i] = params[i].getClass();
            }
            MethodHandle mh = getStaticMethodHandle(owner, methodName, returnType, classes);
            if (mh == null) return null;
            return (T) mh.invoke(params);
        } catch (Throwable e) {
            LOGGER.error("Failed to invoke static method {} from {}", methodName, owner, e);
            return null;
        }
    }

    /**
     * Instantiate a class using the ctor matching the given params.
     */
    public static @Nullable Object instantiate(Class<?> cls, @NotNull Object... params) {
        try {
            Class<?>[] classes = new Class<?>[params.length];
            for (int i = 0; i < params.length; i++) {
                classes[i] = params[i].getClass();
            }
            MethodHandle mh = getConstructorHandle(cls, classes);
            if (mh == null) return null;
            return mh.invoke(params);
        } catch (Throwable e) {
            LOGGER.error("Failed to instantiate {}", cls, e);
            return null;
        }
    }

    /**
     * Instantiate a class using the no-args ctor. If allocateAnyway is true and there is no no-args ctor, it will be allocated using the unsafe.
     */
    @SuppressWarnings("DataFlowIssue" /*, reason = "I want it to fail so it can allocate anyway" */)
    public static @Nullable Object instantiate(Class<?> cls, boolean allocateAnyway) {
        try {
            MethodHandle mh = getConstructorHandle(cls);
            if (mh == null && !allocateAnyway) return null;
            return mh.invoke();
        } catch (Throwable e) {
            if (!allocateAnyway) {
                LOGGER.error("Failed to instantiate {}", cls, e);
                return null;
            }
            try {
                return theUnsafe.allocateInstance(cls);
            } catch (Exception ee) {
                ee.initCause(e);
                LOGGER.error("Failed to Unsafe allocate {}", cls, ee);
                return null;
            }
        }
    }

    public static <T> @Nullable T invokeCatching(MethodHandle mh, Object... args) {
        try {
            return (T) mh.invoke(args);
        } catch (Throwable e) {
            LOGGER.error("Failed to invoke method handle {}", mh, e);
            return null;
        }
    }

    public static <T> @Nullable T invokeExactCatching(MethodHandle mh, Object... args) {
        try {
            return (T) mh.invokeExact(args);
        } catch (Throwable e) {
            LOGGER.error("Failed to invoke method handle {}", mh, e);
            return null;
        }
    }

    // --- annotation --- //

    /**
     * Return a map of all values attached to this annotation.
     * <br> This method is considerably faster than reflection.
     */
    public static @Nullable Map<String, Object> getAnnotationValues(Annotation a) {
        try {
            return (Map<String, Object>) getAnnotationValuesMethodHandle().invoke(Proxy.getInvocationHandler(a));
        } catch (Throwable e) {
            LOGGER.error("Failed to get annotation values for {}", a, e);
            return null;
        }
    }


    // --- internal --- //

    private static MethodHandle getAnnotationValuesMethodHandle() {
        if (gav != null) return gav;
        try {
            // AnnotationInvocationHandler#memberValues
            // we steal it by asking it for the invocation handler of an annotation instance, which we steal from @Deprecated's @Retention marker (lol)
            return gav = trustedLookup.unreflectGetter(Proxy.getInvocationHandler(Deprecated.class.getAnnotation(Retention.class)).getClass().getDeclaredField("memberValues"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to get memberValues getter", e);
        }
    }

    static {
        sun.misc.Unsafe unsafe;
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get unsafe instance", e);
        }
        theUnsafe = unsafe;

        MethodHandles.Lookup lookup;
        try {
            // tee hee
            Field implLookup = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            lookup = (MethodHandles.Lookup) unsafe.getObject(unsafe.staticFieldBase(implLookup), unsafe.staticFieldOffset(implLookup));
        } catch (Exception e) {
            LOGGER.error("Failed to get trusted lookup, things may break!", e);
            lookup = MethodHandles.lookup();
        }
        trustedLookup = lookup;
    }
}
