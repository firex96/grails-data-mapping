/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.orm.hibernate.support;

import org.grails.datastore.mapping.core.grailsversion.GrailsVersion;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * Methods to deal with the differences in different Hibernate versions
 *
 * @author Graeme Rocher
 * @author Juergen Hoeller
 *
 * @since 6.0
 *
 */
public class HibernateVersionSupport {

    private static Method getFlushMode;
    private static Method setFlushMode;
    private static Method resolveAttributeIndexes;
    private static boolean arrayAttributeIndexes = true;

    static {
        try {
            resolveAttributeIndexes = EntityPersister.class.getMethod("resolveAttributeIndexes", String[].class);
            resolveAttributeIndexes.setAccessible(true);
        } catch (NoSuchMethodException e) {
            try {
                resolveAttributeIndexes = EntityPersister.class.getMethod("resolveAttributeIndexes", Set.class);
                resolveAttributeIndexes.setAccessible(true);
                arrayAttributeIndexes = false;
            } catch (NoSuchMethodException e1) {
                throw new IllegalStateException("No compatible Hibernate resolveAttributeIndexes signature found", e1);
            }
        }
        try {
            // Hibernate 5.2+ getHibernateFlushMode()

            getFlushMode = Session.class.getMethod("getHibernateFlushMode");
            setFlushMode = Session.class.getMethod("setHibernateFlushMode", FlushMode.class);
            getFlushMode.setAccessible(true);
            setFlushMode.setAccessible(true);
        }
        catch (NoSuchMethodException ex) {
            try {
                // Hibernate 5.0/5.1 getFlushMode() with FlushMode return type
                getFlushMode = Session.class.getMethod("getFlushMode");
                setFlushMode = Session.class.getMethod("setFlushMode", FlushMode.class);
                getFlushMode.setAccessible(true);
                setFlushMode.setAccessible(true);
            }
            catch (NoSuchMethodException ex2) {
                throw new IllegalStateException("No compatible Hibernate getFlushMode signature found", ex2);
            }
        }
        // Check that it is the Hibernate FlushMode type, not JPA's...
        Assert.state(FlushMode.class == getFlushMode.getReturnType());
    }

    /**
     * Get the native Hibernate FlushMode, adapting between Hibernate 5.0/5.1 and 5.2+.
     * @param session the Hibernate Session to get the flush mode from
     * @return the FlushMode (never {@code null})
     * @since 4.3
     */
    public static FlushMode getFlushMode(Session session) {
        if(session != null) {
            return (FlushMode) ReflectionUtils.invokeMethod(getFlushMode, session);
        }
        return FlushMode.MANUAL;
    }

    /**
     * Set the native Hibernate FlushMode, adapting between Hibernate 5.0/5.1 and 5.2+.
     * @param session the Hibernate Session to get the flush mode from
     * @return the FlushMode (never {@code null})
     * @since 4.3
     */
    public static void setFlushMode(Session session, FlushMode flushMode) {
        ReflectionUtils.invokeMethod(setFlushMode, session, flushMode);
    }

    /**
     * Check the current hibernate version
     * @param required The required version
     * @return True if it is at least the given version
     */
    public static boolean isAtLeastVersion(String required) {
        String hibernateVersion = Hibernate.class.getPackage().getImplementationVersion();
        if (hibernateVersion != null) {
            return GrailsVersion.isAtLeast(hibernateVersion, required);
        } else {
            return false;
        }
    }

    public static int[] resolveAttributeIndexes(EntityPersister persister, Set<String> properties) {
        if(arrayAttributeIndexes) {
            Object[] propertiesArray = new Object[]{ properties.toArray(new String[properties.size()]) };
            return (int[]) ReflectionUtils.invokeMethod(resolveAttributeIndexes, persister, propertiesArray);
        }
        else {
            return (int[]) ReflectionUtils.invokeMethod(resolveAttributeIndexes, persister, properties);
        }
    }

    public static int[] resolveAttributeIndexes(EntityPersister persister, String[] properties) {
        if(arrayAttributeIndexes) {
            return (int[]) ReflectionUtils.invokeMethod(resolveAttributeIndexes, persister, new Object[]{properties});
        }
        else {
            Set<String> propertySet = new LinkedHashSet<>(Arrays.asList(properties));
            return (int[]) ReflectionUtils.invokeMethod(resolveAttributeIndexes, persister, propertySet);
        }
    }
}
