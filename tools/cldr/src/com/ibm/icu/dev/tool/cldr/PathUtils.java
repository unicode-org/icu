package com.ibm.icu.dev.tool.cldr;

import java.nio.file.Path;
import java.nio.file.Paths;

final class PathUtils {

    // Horrible hack that relies on all sorts of unspecified behaviour in "toAbsolutePath()" and
    // assumes that this code was run from the project root (e.g. by setting Eclipse up to do so).
    public static Path getCldrRoot() {
        return Paths.get(System.getProperty("CLDR_DIR"));
    }

    public static Path getIcuRoot() {
        return Paths.get(System.getProperty("ICU_DIR"));
    }

    public static Path getPackageDirectoryFor(Class<?> cls) {
        return getIcuRoot()
            .resolve("tools/cldr/src")
            .resolve(cls.getPackage().getName().replace(".", "/"));
    }

    private PathUtils() {}
}
