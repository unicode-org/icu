/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

import java.io.*;

/**
 * A LineCharNumberReader is a BufferedReader that
 * keeps track of the line number and character offset
 * on that line of the current input stream.
 */
public class LineCharNumberReader extends BufferedReader {
    private int lineNumber = 0;
    private int charNumber = 0;
    private int markedLineNumber;
    private int markedCharNumber;
    private boolean skipLF;

    public LineCharNumberReader(Reader in) {
        super(in);
            //{{INIT_CONTROLS
        //}}
}

    public LineCharNumberReader(Reader in, int sz) {
        super(in, sz);
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getCharNumber() {
        return charNumber;
    }

    public int read() throws IOException {
        synchronized (lock) {
            int c = super.read();
            charNumber++;
            if (skipLF) {
                if (c == '\n') c = super.read();
                skipLF = false;
            }
            switch (c) {
            case '\r':
                skipLF = true;
            case '\n':      /* Fall through */
            case '\u2028':      /* Fall through */
            case '\u2029':      /* Fall through */
                lineNumber++;
                charNumber = 0;
                return '\n';
            }
            return c;
        }
    }

    public int read(char cbuf[], int off, int len) throws IOException {
        synchronized (lock) {
            int n = super.read(cbuf, off, len);

            for (int i = off; i < off + len; i++) {
                int c = cbuf[i];
                charNumber++;
                if (skipLF) {
                    skipLF = false;
                    if (c == '\n')
                    continue;
                }
                switch (c) {
                case '\r':
                    skipLF = true;
                case '\n':  /* Fall through */
                case '\u2028':  /* Fall through */
                case '\u2029':  /* Fall through */
                    lineNumber++;
                    charNumber = 0;
                    break;
                }
            }

            return n;
        }
    }

    public String readLine() throws IOException {
        synchronized (lock) {
            String l = super.readLine();
            if (l != null)
            lineNumber++;
            charNumber = 0;
            skipLF = false;
            return l;
        }
    }

    private static final int maxSkipBufferSize = 8192;

    private char skipBuffer[] = null;

    public long skip(long n) throws IOException {
        int nn = (int) Math.min(n, maxSkipBufferSize);
        synchronized (lock) {
            if ((skipBuffer == null) || (skipBuffer.length < nn))
            skipBuffer = new char[nn];
            long r = n;
            while (r > 0) {
            int nc = read(skipBuffer, 0, nn);
            if (nc == -1)
                break;
            r -= nc;
            }
            return n - r;
        }
    }

    public void mark(int readAheadLimit) throws IOException {
        synchronized (lock) {
            super.mark(readAheadLimit);
            markedLineNumber = lineNumber;
            markedCharNumber = charNumber;
        }
    }

    public void reset() throws IOException {
        synchronized (lock) {
            super.reset();
            lineNumber = markedLineNumber;
            charNumber = markedCharNumber;
        }
    }

    //{{DECLARE_CONTROLS
    //}}
}
