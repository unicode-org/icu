/*
**********************************************************************
* Copyright (c) 2002-2006, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: November 5 2002
* Since: ICU 2.4
**********************************************************************
*/
package com.ibm.icu.impl;

import java.io.*;

/**
 * A DataInputStream that implements random-access seeking.  For this
 * to work, the size of the data stream must be known in advance, or
 * the data must be supplied as a raw byte[] array.
 *
 * Seeking doesn't work directly on all streams.  If a given stream
 * doesn't support seeking, extract the bytes into a byte[] array and
 * use the byte[] constructor.
 */
class ICUBinaryStream extends DataInputStream {

    /**
     * Construct a stream from the given stream and size.
     * @param stream the stream of data
     * @param size the number of bytes that should be made available
     * for seeking.  Bytes beyond this may be read, but seeking will
     * not work for offset >= size.
     */
    public ICUBinaryStream(InputStream stream, int size) {
        super(stream);
        mark(size);
    }

    /**
     * Construct a stream from the given raw bytes.
     */
    public ICUBinaryStream(byte[] raw) {
        this(new ByteArrayInputStream(raw), raw.length);
    }

    /**
     * Seek to the given offset.  Offset is from the position of the
     * stream passed to the constructor, or from the start of the
     * byte[] array.
     */
    public void seek(int offset) throws IOException {
        reset();
        int actual = skipBytes(offset);
        if (actual != offset) {
            throw new IllegalStateException("Skip(" + offset + ") only skipped " +
                                       actual + " bytes");
        }
        if (false) System.out.println("(seek " + offset + ")");
    }
}
