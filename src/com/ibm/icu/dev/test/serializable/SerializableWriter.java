/*
 *******************************************************************************
 * Copyright (C) 1996-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.test.serializable;

import com.ibm.icu.util.VersionInfo;

import java.net.URL;

/**
 * This class writes the test objects for each class to a file. The work is
 * actually done by the superclass, CoverageTest. This class just constructs
 * a CoverageTest w/ a non-null path, which tells it to write the data.
 * 
 */
public class SerializableWriter extends CoverageTest
{
    public SerializableWriter(String path)
    {
        super(path);
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

    public static void main(String[] args)
    {
        URL dataURL = SerializableWriter.class.getResource("data");
        CoverageTest test = new SerializableWriter(dataURL.getPath() + "/" + folderName());
        
        test.run(args);
        
    }
}
