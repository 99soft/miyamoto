/*
 *    Copyright 2010 The Miyamoto Team
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.googlecode.miyamoto;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 *
 * @param <A> The annotation has to be proxed.
 * @version $Id$
 */
public final class AnnotationProxy<A extends Annotation> implements Annotation, InvocationHandler {

    private static final int MEMBER_NAME_MULTIPLICATOR = 127;

    public static <A extends Annotation> AnnotationProxy<A> newProxy(Class<A> annotationType) {
        if (annotationType == null) {
            throw new IllegalArgumentException("Parameter 'annotationType' must be not null");
        }
        return new AnnotationProxy<A>(annotationType);
    }

    @SuppressWarnings("unchecked")
    public static <A extends Annotation> AnnotationProxy<A> getAnnotationHandler(Object obj) {
        if (Proxy.isProxyClass(obj.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(obj);
            if (handler instanceof AnnotationProxy) {
                return (AnnotationProxy<A>) handler;
            }
        }
        return null;
    }

    private static <A extends Annotation> Method[] getDeclaredMethods(final Class<A> annotationType) {
        return AccessController.doPrivileged(
                new PrivilegedAction<Method[]>() {
                    public Method[] run() {
                        final Method[] declaredMethods = annotationType.getDeclaredMethods();
                        AccessibleObject.setAccessible(declaredMethods, true);
                        return declaredMethods;
                    }
                });
    }

    private final Class<A> annotationType;

    private final Map<String, AnnotationProperty> properties = new LinkedHashMap<String, AnnotationProperty>();

    private final A proxedAnnotation;

    private AnnotationProxy(Class<A> annotationType) {
        this.annotationType = annotationType;

        String propertyName;
        Class<?> returnType;
        Object defaultValue;
        for (Method method : getDeclaredMethods(annotationType)) {
            propertyName = method.getName();
            returnType = method.getReturnType();
            defaultValue = method.getDefaultValue();

            AnnotationProperty property = new AnnotationProperty(propertyName, returnType);
            property.setValue(defaultValue);
            this.properties.put(propertyName, property);
        }

        this.proxedAnnotation = annotationType.cast(Proxy.newProxyInstance(annotationType.getClassLoader(),
                new Class<?>[]{ annotationType },
                this));
    }

    public void setProperty(String name, Object value) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter 'name' must be not null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Parameter 'value' must be not null");
        }

        if (!this.properties.containsKey(name)) {
            throw new IllegalArgumentException("Annotation '"
                    + this.annotationType.getName()
                    + "' does not contain a property named '"
                    + name
                    + "'");
        }

        this.properties.get(name).setValue(value);
    }

    public Object getProperty(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter 'name' must be not null");
        }
        return this.properties.get(name).getValue();
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if (this.properties.containsKey(name)) {
            return this.properties.get(name).getValue();
        }
        return method.invoke(this, args);
    }

    public Class<? extends Annotation> annotationType() {
        return this.annotationType;
    }

    public A getProxedAnnotation() {
        return this.proxedAnnotation;
    }

    public AnnotationProperty[] describe() {
        Collection<AnnotationProperty> props = this.properties.values();
        AnnotationProperty[] properties = new AnnotationProperty[props.size()];
        return props.toArray(properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!this.annotationType.isInstance(obj)) {
            return false;
        }

        String propertyName;
        AnnotationProperty expected;
        for (Method method : getDeclaredMethods(this.annotationType())) {
            propertyName = method.getName();

            if (!this.properties.containsKey(propertyName)) {
                return false;
            }

            expected = this.properties.get(propertyName);
            AnnotationProperty actual = new AnnotationProperty(propertyName, method.getReturnType());

            AnnotationProxy<?> proxy = getAnnotationHandler(obj);
            if (proxy != null) {
                actual.setValue(proxy.getProperty(propertyName));
            } else {
                try {
                    actual.setValue(method.invoke(obj));
                } catch (IllegalArgumentException e) {
                    return false;
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                } catch (InvocationTargetException e) {
                    return false;
                }
            }

            if (!expected.equals(actual)) {
                return false;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hashCode = 0;

        for (Entry<String, AnnotationProperty> property : this.properties.entrySet()) {
            hashCode += (MEMBER_NAME_MULTIPLICATOR * property.getKey().hashCode() ^ property.getValue().getValueHashCode());
        }

        return hashCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("@")
                                        .append(this.annotationType.getName())
                                        .append('(');
        int counter = 0;
        for (Entry<String, AnnotationProperty> property : this.properties.entrySet()) {
            if (counter > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append(property.getKey())
                         .append('=')
                         .append(property.getValue().valueToString());
            counter++;
        }
        return stringBuilder.append(')').toString();
    }

}
