// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.ant;

import main.java.org.unicode.icu.tool.cldrtoicu.CodeGenerator;
import main.java.org.unicode.icu.tool.cldrtoicu.generator.ResourceFallbackCodeGenerator;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.google.common.base.Preconditions.checkNotNull;

// Note: Auto-magical Ant methods are listed as "unused" by IDEs, unless the warning is suppressed.
public final class GenerateCodeTask extends Task {
    private Path cldrPath;
    private Path cOutDir;
    private Path javaOutDir;
    private String action;

    private class GeneratedFileDef {
        String cRelativePath;
        String javaRelativePath;
        CodeGenerator generator;

        public GeneratedFileDef(String cRelativePath, String javaRelativePath, CodeGenerator generator) {
            this.cRelativePath = cRelativePath;
            this.javaRelativePath = javaRelativePath;
            this.generator = generator;
        }
    }

    private GeneratedFileDef[] generatedFileDefs = {
        new GeneratedFileDef("common/localefallback_data.h", "src/com/ibm/icu/impl/LocaleFallbackData.java", new ResourceFallbackCodeGenerator()),
    };

    @SuppressWarnings("unused")
    public void setCldrDir(String path) {
        // Use String here since on some systems Ant doesn't support automatically converting Path instances.
        this.cldrPath = checkNotNull(Paths.get(path));
    }

    @SuppressWarnings("unused")
    public void setCOutDir(String path) {
        // Use String here since on some systems Ant doesn't support automatically converting Path instances.
        this.cOutDir = Paths.get(path);
    }

    @SuppressWarnings("unused")
    public void setJavaOutDir(String path) {
        // Use String here since on some systems Ant doesn't support automatically converting Path instances.
        this.javaOutDir = Paths.get(path);
    }

    @SuppressWarnings("unused")
    public void setAction(String action) {
        // Use String here since on some systems Ant doesn't support automatically converting Path instances.
        this.action = action;
    }

    @SuppressWarnings("unused")
    public void execute() throws BuildException {
        for (GeneratedFileDef task : generatedFileDefs) {
            Path cOutPath = cOutDir.resolve(task.cRelativePath);
            Path javaOutPath = javaOutDir.resolve(task.javaRelativePath);

            try {
                if (this.action != null && this.action.equals("clean")) {
                    log("Deleting " + cOutPath + " and " + javaOutPath + "...");
                    Files.deleteIfExists(cOutPath);
                    Files.deleteIfExists(javaOutPath);
                } else {
                    Files.createDirectories(cOutPath.getParent());
                    Files.createDirectories(javaOutPath.getParent());

                    try (PrintWriter cOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(cOutPath.toFile())));
                         PrintWriter javaOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(javaOutPath.toFile())))) {

                        log("Generating " + cOutPath + " and " + javaOutPath + "...");
                        task.generator.generateCode(cldrPath, cOut, javaOut);
                    }
                }
            } catch (IOException ioException) {
                throw new BuildException("IOException: " + ioException.toString());
            }
        }
    }

}
