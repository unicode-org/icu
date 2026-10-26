// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.tool.errorprone;

import java.util.StringJoiner;

/**
 * Error prone issue as parsed from the maven standard output.
 *
 * <p>Very similar to `com.google.errorprone.BugCheckerInfo`
 */
class ErrorProneEntry {
    final String path;
    final int line;

    @Override
    public String toString() {
        StringJoiner builder = new StringJoiner("\n    ", "ErrorEntry {\n    ", "\n}");
        builder.add("path:\"" + path + "\"")
                .add("line:" + line)
                .add("column:" + column)
                .add("type:" + type)
                .add("message:\"" + message + "\"")
                .add("extra:\"" + extra + "\"")
                .add("url:\"" + url + "\"");
        return builder.toString();
    }

    final int column;
    final String type;
    final String message;
    String extra;
    String url;
    String severity;

    void addExtra(String toAdd) {
        if (extra == null) {
            extra = toAdd;
        } else {
            extra = extra + "\n" + toAdd;
        }
    }

    /** Creates an ErrorEntry object. */
    ErrorProneEntry(String path, int line, int column, String type, String message) {
        this.path = path;
        this.line = line;
        this.column = column;
        this.type = type;
        this.message = message;
        this.extra = null;
        this.url = null;
        this.severity = "???";
    }
}
