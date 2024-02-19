// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
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


import java.io.PrintStream;

import com.ibm.icu.impl.Utility;

abstract class OpenTypeTableWriter
{
    static class OpenTypeTableDumper
    {
        private short[] table;
        private int tableLength;
        
        OpenTypeTableDumper(short[] data, int outputIndex)
        {
            table = data;
            tableLength = outputIndex;
        }
        
        int length()
        {
            return tableLength;
        }
        
        void appendValue(StringBuffer line, int index)
        {
            short value = table[index];
            
            line.append("0x");
            line.append(Utility.hex((value >> 8) & 0xFF, 2));
            line.append(", ");

            line.append("0x");
            line.append(Utility.hex(value & 0xFF, 2));
        }

        void dumpTable(PrintStream output, int valuesPerLine) {
            StringBuffer line = new StringBuffer("    "); // four spaces
            int maxIndex = length();
        
            for (int i = 0; i < maxIndex; i += 1) {
                
                if (i > 0 && i % valuesPerLine == 0) {
                    output.println(line.toString());
                    line.setLength(4);
                }
        
                appendValue(line, i);
                line.append(", ");
            }
        
            line.setLength(line.length() - 2);
        
            output.println(line.toString());
        }
    }
            
    protected short[] data;
    protected int outputIndex;

    public OpenTypeTableWriter(int initialBufferSize)
    {
        data = new short[initialBufferSize];
        outputIndex = 0;
    }
    
    public OpenTypeTableWriter()
    {
        this(1024);
    }
    
    public int getOutputIndex()
    {
        return outputIndex;
    }
    
    public void writeData(int value)
    {
        if (outputIndex >= data.length)
        {
            short[] newData = new short[data.length + 512];
            
            System.arraycopy(data, 0, newData, 0, data.length);
            
            data = newData;
        }
        
        data[outputIndex] = (short) value;
        outputIndex += 1;
    }
    
    public void writeTag(String tag)
    {
        char[] charArray = {'\0', '\0', '\0', '\0'};
        int max = Math.min(tag.length(), 4);
        
        tag.getChars(0, max, charArray, 0);
        
        writeData(((charArray[0] & 0xFF) << 8) + (charArray[1] & 0xFF));
        writeData(((charArray[2] & 0xFF) << 8) + (charArray[3] & 0xFF));
    }
    
    public void fixOffset(int offset, int base)
    {
        // * 2 to convert from short to byte index
        data[offset] = (short) ((outputIndex - base) * 2);
    }
    
    public void dumpTable(PrintStream output, int valuesPerLine)
    {
        OpenTypeTableDumper dumper = new OpenTypeTableDumper(data, outputIndex);
        
        dumper.dumpTable(output, valuesPerLine);
    }
    
    abstract public void writeTable(PrintStream output);
}
