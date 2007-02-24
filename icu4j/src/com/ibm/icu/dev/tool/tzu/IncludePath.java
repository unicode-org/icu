/**
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.tzu;

import java.io.File;

public class IncludePath {
    public IncludePath(File path, boolean include) {
        this.path = path;
        this.include = include;
    }

    public File getPath() {
        return path;
    }

    public boolean isIncluded() {
        return include;
    }

    public String toString() {
        return (include ? '+' : '-') + path.toString();
    }

    public boolean equals(Object other) {
        return !(other instanceof IncludePath) ? false : path
                .equals(((IncludePath) other).path);
    }

    private File path;

    private boolean include;
}