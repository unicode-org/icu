package com.ibm.tools.normalizer;

import com.ibm.text.*;
import com.ibm.util.CompactCharArray;
import com.ibm.util.CompactByteArray;

//===========================================================================================
// Utilities for writing data out as compilable source code
//
public abstract class SourceWriter {
    abstract public void close();
    abstract public void write(String name, short value);
    abstract public void write(String name, int value);

    abstract public void write(String name, CompactCharArray array);
    abstract public void write(String name, CompactByteArray array);
    abstract public void write(String name, StringBuffer str);

    abstract public void write(String name, char[] array);
    abstract public void write(String name, int[] array);

    abstract public void writeHex(String name, char value);
    abstract public void writeHex(String name, int value);
}

