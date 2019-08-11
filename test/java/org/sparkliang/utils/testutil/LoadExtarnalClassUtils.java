package org.sparkliang.utils.testutil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class LoadExtarnalClassUtils {


    public static Class loadClassFromJar(String jarPath, String className) throws IOException, ClassNotFoundException {
        if (className == null)
            return null;

        URL url1 = new URL("file:" + jarPath);
        URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{url1}, Thread.currentThread()
                .getContextClassLoader());
        List<JarEntry> jarEntryList = new ArrayList<>();
        JarFile jarFile = new JarFile(jarPath);
        Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();
        while (jarEntryEnumeration.hasMoreElements()) {
            JarEntry jarEntry = jarEntryEnumeration.nextElement();
            if (jarEntry.getName().endsWith(".class")) {
                jarEntryList.add(jarEntry);
            }
        }

        for (JarEntry entry : jarEntryList) {
            String tmpClassName = entry.getName().replace('/', '.');
            tmpClassName = tmpClassName.substring(0, tmpClassName.length() - 6);
            if (className.equals(tmpClassName))
                return urlClassLoader.loadClass(tmpClassName);
        }
        return null;
    }
}
