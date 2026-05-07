// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.ant;

public class Task {
    public static class BuildException extends RuntimeException {
        private static final long serialVersionUID = 2430911677116799373L;

        public BuildException(String message, Throwable cause) {
            super(message, cause);
        }

        public BuildException(String message) {
            super(message);
        }
    }

    void log(String format) {
        System.out.println(format);
    }

    public void execute() throws BuildException {}

    public void init() throws BuildException {}
}
