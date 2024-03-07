// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.ibm.icu.dev.test.AbstractTestLog;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.locale.XCldrStub.FileUtilities;
import com.ibm.icu.impl.locale.XCldrStub.Splitter;
import com.ibm.icu.util.ICUUncheckedIOException;

abstract public class DataDrivenTestHelper {

    public static final List<String> DEBUG_LINE = Collections.singletonList("@debug");
    public static final Splitter SEMICOLON = Splitter.on(';').trimResults();
    public static final Splitter EQUAL_SPLIT = Splitter.on('=').trimResults();
    public static final String SEPARATOR = " ; \t";

    protected TestFmwk framework = null;
    protected int minArgumentCount = 3;
    protected int maxArgumentCount = 4;
    private List<List<String>> lines = new ArrayList<>();
    private List<String> comments = new ArrayList<>();

    public DataDrivenTestHelper setFramework(TestFmwk testFramework) {
        this.framework = testFramework;
        return this;
    }

    public <T extends Appendable> T appendLines(T out) {
        try {
            for (int i = 0; i < lines.size(); ++i) {
                List<String> components = lines.get(i);
                String comment = comments.get(i);
                if (components.isEmpty()) {
                    if(!comment.isEmpty()) {
                        out.append("# ").append(comment);
                    }
                } else {
                    String first = components.iterator().next();
                    String sep = first.startsWith("@") ? "=" : SEPARATOR;
                    out.append(CollectionUtilities.join(components, sep));
                    if (!comment.isEmpty()) {
                        out.append("\t# ").append(comment);
                    }
                }
                out.append('\n');
            }
            return out;
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    protected DataDrivenTestHelper addLine(List<String> arguments, String commentBase) {
        lines.add(Collections.unmodifiableList(arguments));
        comments.add(commentBase);
        return this;
    }

    public DataDrivenTestHelper run(Class<?> classFileIsRelativeTo, String file) {
        return load(classFileIsRelativeTo, file)
            .test();
    }

    public boolean isTestLine(List<String> arguments) {
        return !arguments.isEmpty() && !arguments.equals(DEBUG_LINE);
    }

    public DataDrivenTestHelper test() {
        boolean breakpoint = false;
        for (int i = 0; i < lines.size(); ++i) {
            List<String> arguments = lines.get(i);
            String comment = comments.get(i);
            if (arguments.isEmpty()) {
                if (!comment.isEmpty()) {
                    AbstractTestLog.logln(comment);
                }
                continue;
            } else if (arguments.equals(DEBUG_LINE)) {
                breakpoint = true;
                continue;
            } else {
                String first = arguments.get(0);
                if (first.startsWith("@")) {
                    handleParams(comment, arguments);
                    continue;
                }
            }
            try {
                handle(i, breakpoint, comment, arguments);
            } catch (Exception e) {
                e.printStackTrace();
                AbstractTestLog.errln("Illegal data test file entry (" + i + "): " + arguments + " # " + comment);
            }
            breakpoint = false;
        }
        return this;
    }

    public DataDrivenTestHelper load(Class<?> classFileIsRelativeTo, String file) {
        BufferedReader in = null;
        try {
            in = FileUtilities.openFile(classFileIsRelativeTo, file);
            //boolean breakpoint = false;

            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.isEmpty()) {
                    addLine(Collections.<String>emptyList(), "");
                    continue;
                }
                int hash = line.indexOf('#');
                String comment = "";
                String commentBase = "";
                if (hash >= 0) {
                    commentBase = line.substring(hash+1).trim();
                    line = line.substring(0,hash).trim();
                    comment = "# " + commentBase;
                    if (!line.isEmpty()) {
                        comment = "\t" + comment;
                    }
                }
                if (line.isEmpty()) {
                    addLine(Collections.<String>emptyList(), commentBase);
                    continue;
                }
                if (line.startsWith("@")) {
                    List<String> keyValue = EQUAL_SPLIT.splitToList(line);
                    addLine(keyValue, comment);
                    continue;
                }
                List<String> arguments = SEMICOLON.splitToList(line);
                if (arguments.size() < minArgumentCount || arguments.size() > maxArgumentCount) {
                    AbstractTestLog.errln("Malformed data line:" + line + comment);
                    continue;
                }
                addLine(arguments, commentBase);
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new ICUUncheckedIOException(e);
                }
            }
        }
        lines = Collections.unmodifiableList(lines); // should do deep unmodifiable...
        comments = Collections.unmodifiableList(comments);
        return this;
    }

    protected boolean assertEquals(String message, Object expected, Object actual) {
        return TestFmwk.handleAssert(Objects.equals(expected, actual), message, stringFor(expected), stringFor(actual), null, false);
    }

    private final String stringFor(Object obj) {
        return obj == null ? "null"
            : obj instanceof String ? "\"" + obj + '"'
                : obj instanceof Number ? String.valueOf(obj)
                    : obj.getClass().getName() + "<" + obj + ">";
    }

    abstract public void handle(int lineNumber, boolean breakpoint, String commentBase, List<String> arguments);

    public void handleParams(String comment, List<String> arguments) {
        throw new IllegalArgumentException("Unrecognized parameter: " + arguments);
    }

    public List<List<String>> getLines() {
        return lines;
    }
}
