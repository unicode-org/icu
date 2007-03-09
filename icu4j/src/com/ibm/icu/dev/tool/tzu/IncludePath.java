/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.io.File;

/**
 * Represents a path and whether it should be included or excluded.
 */
public class IncludePath {
    /**
     * Constructs an IncludePath around a file or directory and whether it
     * should be included or excluded.
     * 
     * @param path
     *            The file / directory to be used.
     * @param include
     *            Whether the file should be included / excluded.
     */
    public IncludePath(File path, boolean include) {
        this.path = path;
        this.include = include;
    }

    /**
     * Returns the path of this IncludePath.
     * 
     * @return The path of this IncludePath.
     */
    public File getPath() {
        return path;
    }

    /**
     * Returns whether the path is included or not.
     * 
     * @return Whether the path is included or not.
     */
    public boolean isIncluded() {
        return include;
    }

    /**
     * Outputs this IncludePath in the form (<b>+</b>|<b>-</b>)<i>pathstring</i>.
     */
    @Override
    public String toString() {
        return (include ? '+' : '-') + path.toString();
    }

    /**
     * Returns true if the other object is an IncludePath and the path that both
     * objects represent are the same. It is not required for both IncludePaths
     * to be included or excluded.
     */
    @Override
    public boolean equals(Object other) {
        return !(other instanceof IncludePath) ? false : path.equals(((IncludePath) other).path);
    }

    private File path;

    private boolean include;
}
