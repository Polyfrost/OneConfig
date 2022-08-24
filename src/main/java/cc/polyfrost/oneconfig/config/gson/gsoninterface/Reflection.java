package cc.polyfrost.oneconfig.config.gson.gsoninterface;

/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
 *
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>

 * This file contains an adaptation of code from gson-interface
 * Project found at <https://github.com/mintern/gson-interface>
 * For the avoidance of doubt, this file is still licensed under the terms
 * of OneConfig's Licensing.
 *
 *                 LICENSE NOTICE FOR ADAPTED CODE
 *
 * Copyright (C) 2012, Brandon Mintern, EasyESI, Berkeley, CA
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   * Neither gson-interface nor the names of its contributors may be used
 *     to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL BRANDON MINTERN OR EASYESI BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.EmptyStackException;

/**
 * Provides various static helper methods that add a high-level interface to
 * introspection and reflection.
 *
 * @author mintern
 */
public class Reflection {
    /**
     * A wrapper for Class.newInstance() that throws an unchecked Exception.
     * It also ensures that even private constructors can be called.
     *
     * @param c the class on which to call newInstance()
     * @return the object returned from c.newInstance()
     * @throws IllegalArgumentException if there was an exception
     *                                  constructing the instance
     */
    public static <T> T newInstance(Class<T> c) {
        try {
            return constructAnyway(c.getDeclaredConstructor());
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Gets the aClass constructor with the given paramaters, throwing an
     * unchecked exception in the case of errors.
     *
     * @param aClass the class whose constructor should be fetched.
     * @param params the parameters to the desired constructor
     * @return the constructor for aClass that accepts params, or null if
     * there is no such constructor
     */
    public static <T> Constructor<T> getConstructor(Class<T> aClass, Class... params) {
        try {
            return aClass.getDeclaredConstructor(params);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Invokes the given constructor with the given args even if it's not
     * accessible.
     *
     * @param <T>         the type of value to be constructed
     * @param constructor the constructor to invoke (see getConstructor)
     * @param args        the args to send to the constructor
     * @return a new instance of type T
     * @throws IllegalArgumentException if there was an exception while
     *                                  setting the constructor to be accessible or invoking it
     */
    public static <T> T constructAnyway(Constructor<T> constructor, Object... args)
            throws IllegalArgumentException {
        try {
            boolean wasAccessible = constructor.isAccessible();
            constructor.setAccessible(true);
            try {
                T instance = constructor.newInstance(args);
                return instance;
            } finally {
                constructor.setAccessible(wasAccessible);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Checks whether a class is abstract.
     *
     * @param c the class to check
     * @return true iff c is an interface or abstract class
     */
    public static boolean isAbstract(Class c) {
        return c.isInterface() || Modifier.isAbstract(c.getModifiers());
    }

    /**
     * Obtain the value of a field even if it is not public.
     *
     * @param field    the field value to obtain
     * @param fieldObj an instantiated object which defines field
     * @return the value of field in fieldObj
     * @throws IllegalArgumentException on any error
     */
    public static Object getFieldValue(Field field, Object fieldObj)
            throws IllegalArgumentException {
        try {
            boolean wasAccessible = field.isAccessible();
            field.setAccessible(true);
            try {
                return field.get(fieldObj);
            } finally {
                field.setAccessible(wasAccessible);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns a field on which field.get(...) can be called, even if the field
     * is private.
     *
     * @param aClass    the class defining the desired field
     * @param fieldName the name of the desired field
     * @return a field that is accessible regardless of its modifiers
     * @throws IllegalArgumentException on any error
     */
    public static Field getAccessibleField(Class aClass, String fieldName)
            throws IllegalArgumentException {
        try {
            Field field = aClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            // NoSuchFieldException, SecurityException
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * For a class implClass that instantiates a generic class or interface
     * genClass, return the types that genClass is instantiated with. This
     * method has a runtime linear in the size of the type hierarchy, so if
     * the results are used at runtime, it is recommended to cache them.
     * <p>
     * An example of its usage:
     * <p>
     * import java.util.*;
     * public class IdToNameMap extends HashMap<Integer, String> {
     * public static void main (String[] args) {
     * for (Class c: getTypeParameters(IdToNameMap.class, Map.class) {
     * System.out.println(c);
     * }
     * }
     * }
     * <p>
     * Calling main() would print out:
     * <p>
     * Integer.class
     * String.class
     * <p>
     * Note that this method ascends and descends the class hierarchy to
     * determine the best bounds possible on all type parameters. For
     * example, even if the class hierarchy is:
     * <p>
     * public class IdMap<V> extends HashMap<Integer, V> {}
     * public class IdToNameMap extends IdMap<String> {}
     * <p>
     * the main() call would print the same thing. For type parameters that
     * remain unbounded, the tightest bound available is printed. So:
     * <p>
     * getTypeParameters(EnumSet.class, Set.class)
     * <p>
     * would return Enum.class. Sometimes the tightest bound available is
     * Object.class.
     * <p>
     * This method should NOT be called with implClass or genClass as a
     * primitive class. This is not sensible, anyway.
     *
     * @param implClass a class instantiating a generic class or interface
     * @param genClass  the generic class or interface which has type
     *                  parameters you want to know
     * @return the array of instantiated parameters. If implClass does not
     * extend or implement genClass, null is returned. For any genClass
     * type parameters not instantiated by implClass, we return the
     * parameters' upper bounds.
     */
    public static Class[] getTypeParameters(Class implClass, Class genClass) {
        return getTypeParameters(implClass, genClass, new Stack());
    }

    /**
     * @param genClass typically a Class with generic type parameters
     * @return the array of Class bounds on those parameters, or an empty
     * Class array if genClass is not generic.
     */
    public static Class[] getParameterBounds(Class genClass) {
        Type[] parameters = genClass.getTypeParameters();
        Class[] paramClasses = new Class[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            paramClasses[i] = classOfType(parameters[i]);
        }
        return paramClasses;
    }

    /**
     * @param type any Type object
     * @return the most specific class of type that we can determine
     */
    public static Class classOfType(Type type) {
        if (type instanceof Class) {
            return (Class) type;
        } else if (type instanceof ParameterizedType) {
            return classOfType(((ParameterizedType) type).getRawType());
        } else if (type instanceof TypeVariable) {
            return classOfType(((TypeVariable) type).getBounds()[0]);
        } else if (type instanceof WildcardType) {
            return classOfType(((WildcardType) type).getUpperBounds()[0]);
        } else {
            // this should never happen in principle, but just in case...
            return Object.class;
        }
    }

    // Internal helper methods used above.

    /**
     * See public ... getTypeParameters. This method and the others below
     * that implement it work by ascending and descending the type hierarchy
     * in order to gather the desired information. As we ascend the type
     * hierarchy looking for genClass, we push the classes that have led us
     * there onto classStack. Later, if we find genClass but the associated
     * implClass is generic, we descend the classStack to figure out how
     * implClass is instantiated. Eventually, each type parameter is either
     * fully instantiated, in which case we use the instantiation class, or
     * the type parameter is not instantiated, in which case we return its
     * upper bound, which in some cases will be Object.class.  @param
     * implClass  the class extending/implementing genClass @param genClass
     * a class with generic type parameters
     *
     * @param classStack the stack of subclasses we've visited before
     *                   reaching this implClass
     * @return the tightest bounds implClass places on those type
     * parameters
     */
    private static Class[] getTypeParameters(Class implClass, Class genClass, Stack<Class> classStack) {
        if (genClass.isInterface()) {
            return getInterfaceParameters(implClass, genClass, classStack);
        }
        return getSuperParameters(implClass, genClass, classStack);
    }

    /**
     * This method implements getTypeParameters when genClass is an
     * interface.  Since a class can implement multiple interfaces and since
     * they must be retrieved differently than with a superclass, we keep
     * the functions separate.
     * <p>
     * At each level, we check each interface against genClass. If none of
     * them match, we look at any interface's implemented interfaces, and so
     * on all the way up until we reach an interface with no implemented
     * interfaces.
     * <p>
     * If we still haven't found genClass at this point, we ascend into
     * implClass's superclass and perform the same work.
     * <p>
     * As mentioned in private ... getTypeParameters, any time we ascend the
     * type hierarchy, we push the implClass onto the classStack so that we
     * can later descend the class hierarchy to resolve type parameters
     * which are set further down the tree.
     */
    private static Class[] getInterfaceParameters(Class implClass, Class genClass, Stack<Class> classStack) {
        Type[] interfaces = implClass.getGenericInterfaces();
        // Check each of the interfaces implemented by implClass to see if
        // one matches genClass.
        for (Type iface : interfaces) {
            Class[] result = getParametersIfMatches(iface, implClass, genClass, classStack);
            if (result != null) {
                // We found it.
                return result;
            }
        }
        // None of the implemented interfaces matched genClass. Ascend each
        // interface's type hierarchy looking for genClass.
        for (Type iface : interfaces) {
            Class interfaceClass = classOfType(iface);
            classStack.push(implClass);
            Class[] result = getInterfaceParameters(interfaceClass, genClass, classStack);
            classStack.pop();
            if (result != null) {
                return result;
            }
        }
        // We visited the entire interface hierarchy of implClass. Ascend to
        // implClass's superclass and look for the interface there.
        Class superclass = implClass.getSuperclass();
        if (superclass == null) {
            // implClass is an interface or Object, so it has no superclass.
            // We didn't find genClass along this path.
            return null;
        }
        // The ascent.
        classStack.push(implClass);
        Class[] result = getInterfaceParameters(superclass, genClass, classStack);
        classStack.pop();
        // The result may still be null at this point, but we've visited the
        // entire type hierarchy and haven't found genClass. In these cases, we
        // want to return null, anyway.
        return result;
    }

    /**
     * This method implements getTypeParameters when genClass is not an
     * interface, and therefore should be found as a superclass of
     * implClass.  Since a class can only have one superclass, we simply
     * check that superclass and then ascend the type hierarchy if it is not
     * a match.
     */
    private static Class[] getSuperParameters(Class implClass, Class genClass, Stack<Class> classStack) {
        Type supertype = implClass.getGenericSuperclass();
        if (supertype == null) {
            // Base case; implClass is Object; we didn't find genClass.
            return null;
        }
        Class[] result = getParametersIfMatches(supertype, implClass, genClass, classStack);
        if (result != null) {
            // We found it.
            return result;
        }
        // The superclass of implClass didn't match genClass. Ascend class
        // hierarchy.
        classStack.push(implClass);
        result = getSuperParameters(classOfType(supertype), genClass, classStack);
        classStack.pop();
        // We either have our result or null. Return.
        return result;
    }

    /**
     * If the Class represented by type matches genClass, return the
     * genClass type parameters. If it is not a match, returns null.
     *
     * @param type       the type being considered, with all information present
     *                   in implClass
     * @param implClass  the current class, for which type was a superclass
     *                   or an implemented interface
     * @param genClass   the class we are looking for (to be compared against
     *                   type)
     * @param classStack
     * @return null if type is not of Class genClass, genClass
     * instantiated type parameters otherwise. Class[0] if genClasses has
     * no type parameters
     */
    private static Class[] getParametersIfMatches(Type type, Class implClass, Class genClass, Stack<Class> classStack) {
        if (type == genClass) {
            // type matches genClass and it's a Class Type; if genClass has
            // type parameters, they have not been instantiated by
            // implClass.  Simply return the bounds on genClass's
            // parameters, if it has any parameters. If it doesn't have
            // parameters, we'll return Class[0]
            return getParameterBounds(genClass);
        }
        if (type instanceof ParameterizedType && classOfType(type) == genClass) {
            // We've found our parameterized genClass. For each type,
            // convert it to its corresponding class. If it's a TypeVariable
            // and it matches a type argument in implClass, descend down the
            // classStack to determine THAT parameter type.  Otherwise, or
            // if the TypeVariable can't be resolved, simply perform a dumb
            // conversion on the type.
            Type[] types = ((ParameterizedType) type).getActualTypeArguments();
            Class[] classes = new Class[types.length];
            // The following two types declared here to avoid repeated work
            Type[] implParams = null;
            Class[] implInstantiatedParams = null;
            // This loop is probably the most complex bit in the code. An
            // instructive example is where we have a class hierarchy like:
            //     IdNameMap extends IdMap<String>
            //     IdMap<E> extends HashMap<Integer, E>
            //     getTypeParameters(IdNameMap.class, HashMap.class)
            // When we reach this point:
            //     types = [Integer, E]
            //     implClass = IdMap
            //     classes = []
            //     classStack = [IdNameMap]
            // The outer loop iterates over types. After its first iteration
            // (for which the if-test fails), classes=[Integer]. In the
            // second iteration, E is a TypeVariable and the classStack is
            // not empty.  We fetch IdMap's TypeParameters:
            //     implParams = [E]
            // The inner loop iterates over implParams, finding that when j
            // = 0, implParams[j] matches types[i] (both are E). We then
            // call getTypeParameters(IdNameMap, IdMap, []) to determine the
            // type of E, which turns out to be String:
            //     implInstantiatedParams = [String]
            // We update classes:
            //     classes = [Integer, String]
            // and we continue to the next iteration of the outer loop,
            // where it turns out to terminate. Finally, we return classes.
            types:
            for (int i = 0; i < types.length; i++) {
                if (types[i] instanceof TypeVariable && !classStack.isEmpty()) {
                    if (implParams == null) {
                        implParams = implClass.getTypeParameters();
                    }
                    for (int j = 0; j < implParams.length; j++) {
                        if (((TypeVariable) types[i]).equals(implParams[j])) {
                            if (implInstantiatedParams == null) {
                                Class subClass = classStack.pop();
                                // the descent
                                implInstantiatedParams = getTypeParameters(subClass, implClass, classStack);
                                classStack.push(subClass);
                            }
                            classes[i] = implInstantiatedParams[j];
                            continue types;
                        }
                    }
                    // If we reach this point (we were unable to resolve the
                    // parameter), type[i] will be resolved to its bound in the
                    // line below.
                }
                classes[i] = classOfType(types[i]);
            }
            return classes;
        }
        // type did not match genClass
        return null;
    }

    /**
     * A non-threadsafe alternative to java.util.Stack.
     * Since the Reflection code is single threaded, using this should be
     * faster.
     *
     * @param E the type of elements in the stack
     */
    public static class Stack<E> extends ArrayList<E> {
        /**
         * Push an item onto the stack. The next call to pop() will return
         * the most-recently push()ed item.
         *
         * @param elt the element to add to the stack
         * @return this; convenient for chaining: stack.push(...).push(...)
         */
        public Stack<E> push(E elt) {
            add(elt);
            return this;
        }

        /**
         * Pop from this and return the item that was most recently pushed
         * onto this. This should never be called on an empty stack.
         *
         * @return the item that was pop()ed
         * @throws EmptyStackException if this is empty
         */
        public E pop() {
            try {
                return remove(size() - 1);
            } catch (IndexOutOfBoundsException e) {
                throw new EmptyStackException();
            }
        }
    }
}
