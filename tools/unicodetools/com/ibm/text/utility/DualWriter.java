package com.ibm.text.utility;

import java.awt.*;
import java.io.*;

final public class DualWriter extends Writer {
    private static final String copyright = "(C) Copyright IBM Corp. 1998 - All Rights Reserved";
    // Abstract class for writing to character streams.
    // The only methods that a subclass must implement are
    // write(char[], int, int), flush(), and close().

    private boolean autoflush ;
    private Writer a;
    private Writer b;

    public DualWriter (Writer a, Writer b) {
        this.a = a;
        this.b = b;
    }

    public DualWriter (Writer a, Writer b, boolean autoFlush) {
        this.a = a;
        this.b = b;
        autoflush = autoFlush;
    }

    public void setAutoFlush(boolean value) {
        autoflush = value;
    }

    public boolean getAutoFlush() {
        return autoflush;
    }

    public void write(char cbuf[],
                        int off,
                        int len) throws IOException {
        a.write(cbuf, off, len);
        b.write(cbuf, off, len);
        if (autoflush) flush();
    }

    public void close() throws IOException {
        a.close();
        b.close();
    }

    public void flush() throws IOException {
        a.flush();
        b.flush();
    }
}
