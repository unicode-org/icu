// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package main.java.org.unicode.icu.tool.cldrtoicu;

import java.io.PrintWriter;
import java.nio.file.Path;

public interface CodeGenerator {
    public void generateCode(Path cldrPath, PrintWriter cFileOut, PrintWriter javaFileOut);
}
