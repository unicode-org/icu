/*
 *******************************************************************************
 * Copyright (C) 1998-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 3, 2003
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;


import com.ibm.icu.impl.Utility;

public class TaggedRecord
{
    private String tag;
        
    public TaggedRecord(String theTag)
    {
        tag = theTag;
    }
        
    public String getTag()
    {
        return tag;
    }
        
    //
    // Straight insertion sort from Knuth vol. III, pg. 81
    //
    public static void sort(TaggedRecord[] table, int count)
    {
        for (int j = 1; j < count; j += 1) {
            int i;
            TaggedRecord v = table[j];
            String vTag = v.getTag();

            for (i = j - 1; i >= 0; i -= 1) {
                if (vTag.compareTo(table[i].getTag()) >= 0) {
                    break;
                }

                table[i + 1] = table[i];
            }

            table[i + 1] = v;
        }
    }
    
    public static int search(TaggedRecord[] table, int count, String tag)
    {
        int log2 = Utility.highBit(count);
        int power = 1 << log2;
        int extra = count - power;
        int probe = power;
        int index = 0;

        if (table[extra].getTag().compareTo(tag) <= 0) {
            index = extra;
        }

        while (probe > (1 << 0)) {
            probe >>= 1;

            if (table[index + probe].getTag().compareTo(tag) <= 0) {
                index += probe;
            }
        }

        if (table[index].getTag().equals(tag)) {
            return index;
        }

        return -1;
    }
}
    
