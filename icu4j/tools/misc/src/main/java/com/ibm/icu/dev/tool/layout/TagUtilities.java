// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1998-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Apr 14, 2003
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import com.ibm.icu.impl.Utility;

/**
 *  This class contains utility methods for dealing with
 * four-letter tags.
 * 
 * @author emader
 *
 */
public class TagUtilities
{
    public static String makeTag(String tag)
    {
        if (tag == null || tag.length() == 0) {
            return "0x00000000";
        }
        
        int tagValue = 0;
        
        for (int i = 0; i < 4; i += 1) {
            tagValue <<= 8;
            tagValue += (int) ((i < tag.length()? tag.charAt(i) : ' ') & 0xFF);
        }
        
        return "0x" + Utility.hex(tagValue, 8);
    }
    
//    public static String makeTagOld(String tag)
//    {
//        if (tag == null || tag.length() == 0) {
//            return "0x00000000";
//        }
//        
//        StringBuffer result = new StringBuffer("LE_MAKE_TAG(");
//        
//        for (int i = 0; i < 4; i += 1) {
//            if (i > 0) {
//                result.append(", ");
//            }
//            
//            result.append('\'');
//            result.append(i < tag.length()? tag.charAt(i) : ' ');
//            result.append('\'');
//        }
//        
//        result.append(")");
//        
//        return result.toString(); 
//    }
    
    public static String tagLabel(String tag)
    {
        if (tag == null || tag.length() == 0) {
            return "null";
        } else {
            return tag.toLowerCase();
        }
    }
        
}

