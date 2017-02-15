package com.actemium.sdk.runtimeaspect;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

public final class DynamicInstrumentationReflections {

    private static Set<String> pathsAddedToSystemClassLoader = Collections.synchronizedSet(new HashSet<String>());

    private DynamicInstrumentationReflections() {}

    /**
     * http://stackoverflow.com/questions/1010919/adding-files-to-java-classpath-at-runtime
     */
    public static void addPathToSystemClassLoader(final String dirOrJar) {
        try {
            final String normalizedPath = FilenameUtils.normalize(dirOrJar);
            org.assertj.core.api.Assertions.assertThat(pathsAddedToSystemClassLoader.add(normalizedPath))
            .as("Path [%s] has already been added before!", normalizedPath)
            .isTrue();
            final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            final URL url = new File(normalizedPath).toURI().toURL();
            method.invoke(ClassLoader.getSystemClassLoader(), url);
        } catch (final NoSuchMethodException | MalformedURLException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        }
    }

}