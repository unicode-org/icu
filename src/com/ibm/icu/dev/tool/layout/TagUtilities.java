/*
 *******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Apr 14, 2003
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/TagUtilities.java,v $ $Date: 2003/04/15 20:15:53 $ $Revision: 1.2 $
 * 
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

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
        
        StringBuffer result = new StringBuffer("LE_MAKE_TAG(");
        
        for (int i = 0; i < 4; i += 1) {
            if (i > 0) {
                result.append(", ");
            }
            
            result.append('\'');
            result.append(i < tag.length()? tag.charAt(i) : ' ');
            result.append('\'');
        }
        
        result.append(")");
        
        return result.toString(); 
    }
    
    public static String tagLabel(String tag)
    {
        if (tag == null || tag.length() == 0) {
            return "null";
        } else {
            return tag.toLowerCase();
        }
    }
        
}

