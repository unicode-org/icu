// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.test.serializable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.ibm.icu.dev.test.serializable.SerializableTestUtility.Handler;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.VersionInfo;

/**
 * This class writes the test objects for each class to a file. The work is
 * actually done by the superclass, CoverageTest. This class just constructs
 * a CoverageTest w/ a non-null path, which tells it to write the data.
 *
 */
public class SerializableWriter
{
    String path;

    public SerializableWriter(String path)
    {
        this.path = path;
    }

    private static String folderName()
    {
        int major = VersionInfo.ICU_VERSION.getMajor();
        int minor = VersionInfo.ICU_VERSION.getMinor();
        int milli = VersionInfo.ICU_VERSION.getMilli();
        int micro = VersionInfo.ICU_VERSION.getMicro();
        StringBuffer result = new StringBuffer("ICU_");

        result.append(major);
        result.append(".");
        result.append(minor);

        if (milli != 0 || micro != 0) {
            result.append(".");
            result.append(milli);

            if (micro != 0) {
                result.append(".");
                result.append(micro);
            }
        }

        return result.toString();
    }

    public static void main(String[] args) throws IOException
    {
        String outDir = null;
        if (args.length == 0) {
            URL dataURL = SerializableWriter.class.getResource("data");
            outDir = dataURL.getPath() + "/" + folderName();
        } else {
            outDir = args[0] + "/" + folderName();
        }

        // Override default TimeZone, so serialized data always use
        // the consistent zone if not specified.
        TimeZone savedZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));

            SerializableWriter writer = new SerializableWriter(outDir);

            writer.serialize();
        } finally {
            TimeZone.setDefault(savedZone);
        }
    }

    public void serialize() throws IOException {
        File outDir = new File(this.path);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        List<String> classList = SerializableTestUtility.getSerializationClassList(this);
        for (String className : classList) {
            Handler classHandler = SerializableTestUtility.getHandler(className);
            if (classHandler == null) {
                System.out.println("No Handler - Skipping Class: " + className);
                continue;
            }
            Object[] testObjects = classHandler.getTestObjects();
            File oof = new File(this.path, className + ".dat");
            SerializableTestUtility.serializeObjects(oof, testObjects);
        }
    }
}
