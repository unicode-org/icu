// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2001-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import com.ibm.icu.impl.PatternProps;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A reader for text resource data in the current package or the package of a given class object.
 * The resource data is loaded through the class loader, so it will typically be a file in the same
 * directory as the *.class files, or a file within a JAR file in the corresponding subdirectory.
 * The file must be a text file in one of the supported encodings; when the resource is opened by
 * constructing a <code>ResourceReader</code> object the encoding is specified.
 *
 * <p>2015-sep-03 TODO: Only used in com.ibm.icu.dev.test.format, move there.
 *
 * @author Alan Liu
 */
public class ResourceReader implements Closeable {
    private LineNumberReader reader = null;
    private String resourceName;

    /**
     * Construct a reader object for the input stream associated with the given resource name.
     *
     * @param is the input stream of the resource
     * @param resourceName the name of the resource
     */
    public ResourceReader(InputStream is, String resourceName, Charset cs)
            throws UnsupportedEncodingException {
        this.resourceName = resourceName;
        InputStreamReader isr = new InputStreamReader(is, cs == null ? cs : StandardCharsets.UTF_8);
        this.reader = new LineNumberReader(isr);
    }

    /**
     * Read and return the next line of the file or <code>null</code> if the end of the file has
     * been reached.
     */
    public String readLine() throws IOException {
        if (reader.getLineNumber() == 0) {
            // Remove BOMs
            String line = reader.readLine();
            if (line != null && (line.charAt(0) == '\uFFEF' || line.charAt(0) == '\uFEFF')) {
                line = line.substring(1);
            }
            return line;
        }
        return reader.readLine();
    }

    /**
     * Read a line, ignoring blank lines and lines that start with '#'.
     *
     * @param trim if true then trim leading Pattern_White_Space.
     */
    public String readLineSkippingComments(boolean trim) throws IOException {
        while (true) {
            String line = readLine();
            if (line == null) {
                return line;
            }
            // Skip over white space
            int pos = PatternProps.skipWhiteSpace(line, 0);
            // Ignore blank lines and comment lines
            if (pos == line.length() || line.charAt(pos) == '#') {
                continue;
            }
            // Process line
            if (trim) line = line.substring(pos);
            return line;
        }
    }

    /**
     * Read a line, ignoring blank lines and lines that start with '#'. Do not trim leading
     * Pattern_White_Space.
     */
    public String readLineSkippingComments() throws IOException {
        return readLineSkippingComments(false);
    }

    /**
     * Return the one-based line number of the last line returned by readLine() or
     * readLineSkippingComments(). Should only be called after a call to one of these methods;
     * otherwise the return value is undefined.
     */
    public int getLineNumber() {
        return reader.getLineNumber();
    }

    /**
     * Return a string description of the position of the last line returned by readLine() or
     * readLineSkippingComments().
     */
    public String describePosition() {
        return resourceName + ':' + reader.getLineNumber();
    }

    /**
     * Closes the underlying reader and releases any system resources associated with it. If the
     * stream is already closed then invoking this method has no effect.
     */
    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
            reader = null;
        }
    }
}
