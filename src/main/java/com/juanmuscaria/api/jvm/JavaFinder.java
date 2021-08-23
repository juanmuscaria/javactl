package com.juanmuscaria.api.jvm;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Heavily based on MultiMc and Gradle java detection
// https://github.com/gradle/gradle/tree/36d579971ab09506668facf4e4491b47c43f74cf/subprojects/jvm-services/src/main/java/org/gradle/internal/jvm/inspection
// https://github.com/MultiMC/MultiMC5/blob/94fd9a3535ae9a55c228720858292ed2bb69ff98/launcher/java/JavaUtils.cpp#L356
public class JavaFinder {
    private JavaFinder() {
        throw new IllegalStateException("Sealed class");
    }

    public static List<JavaInstallation> findJavaInstallations() {
        ArrayList<JavaInstallation> found = new ArrayList<>();
        String home = System.getProperty("user.home");
        String[] possiblePaths = new String[]{
                "/usr/java", "/usr/lib/jvm",
                "/usr/lib32/jvm", "/opt/jdk",
                "/opt/jdks", home + "/.sdkman/candidates/java"
        };
        for (String possiblePath : possiblePaths) {
            found.addAll(scan(new File(possiblePath)));
        }
        return found;
    }

    // Recursion all the way
    private static List<JavaInstallation> scan(File path) {
        if (!path.isDirectory())
            return Collections.emptyList();
        File[] files = path.listFiles();
        if (files == null || files.length == 0)
            return Collections.emptyList();
        ArrayList<JavaInstallation> found = new ArrayList<>();
        for (File file : files) {
            if (!file.isDirectory() &&
                    "java".equals(file.getName()) &&
                    file.canRead() &&
                    file.canExecute())
                found.add(new JavaInstallation(file));
            else if (file.isDirectory())
                found.addAll(scan(file));
        }
        return found;
    }
}
