/**
 *******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

import java.io.IOException;
import java.io.Writer;

public final class TestLogWriter extends Writer {
    private TestLog log;
    private int level;
    private boolean closed;

    public TestLogWriter(TestLog log, int level) {
	this.log = log;
	this.level = level;
    }

    public void write(char cbuf[], int off, int len) throws IOException {
	write(new String(cbuf, off, len));
    }

    public void write(String str) throws IOException {
	if (closed) {
	    throw new IOException("stream closed");
	}
	if ("\r\n".indexOf(str) != -1) {
	    log.msg("", level, level == TestLog.ERR, true);
	} else {
	    log.msg(str, level, level == TestLog.ERR, false);
	}
    }

    public void flush() throws IOException {
    }

    public void close() throws IOException {
	closed = true;
    }
}
