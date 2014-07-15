/*
 *******************************************************************************
 * Copyright (C) 1996-2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.ibm.icu.util.VersionInfo;

public final class ICUBinary 
{    
    // public inner interface ------------------------------------------------
    
    /**
     * Special interface for data authentication
     */
    public static interface Authenticate
    {
        /**
         * Method used in ICUBinary.readHeader() to provide data format
         * authentication. 
         * @param version version of the current data
         * @return true if dataformat is an acceptable version, false otherwise
         */
        public boolean isDataVersionAcceptable(byte version[]);
    }
    
    // public methods --------------------------------------------------------

    /**
     * Same as readHeader(), but returns a VersionInfo rather than a compact int.
     */
    public static final VersionInfo readHeaderAndDataVersion(ByteBuffer bytes,
                                                             int dataFormat,
                                                             Authenticate authenticate)
                                                                throws IOException {
        return getVersionInfoFromCompactInt(readHeader(bytes, dataFormat, authenticate));
    }

    /**
     * Reads an ICU data header, checks the data format, and returns the data version.
     *
     * <p>Assumes that the ByteBuffer position is 0 on input.
     * The buffer byte order is set according to the data.
     * The buffer position is advanced past the header (including UDataInfo and comment).
     *
     * <p>See C++ ucmndata.h and unicode/udata.h.
     *
     * @return dataVersion
     * @throws IOException if this is not a valid ICU data item of the expected dataFormat
     */
    public static final int readHeader(ByteBuffer bytes, int dataFormat, Authenticate authenticate)
            throws IOException {
        assert bytes.position() == 0;
        byte magic1 = bytes.get(2);
        byte magic2 = bytes.get(3);
        if (magic1 != MAGIC1 || magic2 != MAGIC2) {
            throw new IOException(MAGIC_NUMBER_AUTHENTICATION_FAILED_);
        }

        byte isBigEndian = bytes.get(8);
        byte charsetFamily = bytes.get(9);
        byte sizeofUChar = bytes.get(10);
        if (isBigEndian < 0 || 1 < isBigEndian ||
                charsetFamily != CHAR_SET_ || sizeofUChar != CHAR_SIZE_) {
            throw new IOException(HEADER_AUTHENTICATION_FAILED_);
        }
        bytes.order(isBigEndian != 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        int headerSize = bytes.getChar(0);
        int sizeofUDataInfo = bytes.getChar(4);
        if (sizeofUDataInfo < 20 || headerSize < (sizeofUDataInfo + 4)) {
            throw new IOException("Internal Error: Header size error");
        }
        // TODO: Change Authenticate to take int major, int minor, int milli, int micro
        // to avoid array allocation.
        byte[] formatVersion = new byte[] {
            bytes.get(16), bytes.get(17), bytes.get(18), bytes.get(19)
        };
        if (bytes.get(12) != (byte)(dataFormat >> 24) ||
                bytes.get(13) != (byte)(dataFormat >> 16) ||
                bytes.get(14) != (byte)(dataFormat >> 8) ||
                bytes.get(15) != (byte)dataFormat ||
                (authenticate != null && !authenticate.isDataVersionAcceptable(formatVersion))) {
            throw new IOException(HEADER_AUTHENTICATION_FAILED_);
        }

        bytes.position(headerSize);
        return  // dataVersion
                ((int)bytes.get(20) << 24) |
                ((bytes.get(21) & 0xff) << 16) |
                ((bytes.get(22) & 0xff) << 8) |
                (bytes.get(23) & 0xff);
    }

    public static final void skipBytes(ByteBuffer bytes, int skipLength) {
        if (skipLength > 0) {
            bytes.position(bytes.position() + skipLength);
        }
    }

    /**
     * Reads the entire contents from the stream into a byte array
     * and wraps it into a ByteBuffer. Closes the InputStream at the end.
     */
    public static final ByteBuffer getByteBufferFromInputStream(InputStream is) throws IOException {
        try {
            int avail = is.available();
            byte[] bytes = new byte[avail];
            readFully(is, bytes, 0, avail);
            while((avail = is.available()) != 0) {
                // TODO Java 6 replace new byte[] and arraycopy(): byte[] newBytes = Arrays.copyOf(bytes, bytes.length + avail);
                byte[] newBytes = new byte[bytes.length + avail];
                System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
                readFully(is, newBytes, bytes.length, avail);
                bytes = newBytes;
            }
            return ByteBuffer.wrap(bytes);
        } finally {
            is.close();
        }
    }

    private static final void readFully(InputStream is, byte[] bytes, int offset, int avail)
            throws IOException {
        while (avail > 0) {
            int numRead = is.read(bytes, offset, avail);
            assert numRead > 0;
            offset += numRead;
            avail -= numRead;
        }
    }

    /**
     * Returns a VersionInfo for the bytes in the compact version integer.
     */
    public static VersionInfo getVersionInfoFromCompactInt(int version) {
        return VersionInfo.getInstance(
                version >>> 24, (version >> 16) & 0xff, (version >> 8) & 0xff, version & 0xff);
    }

    /**
     * Returns an array of the bytes in the compact version integer.
     */
    public static byte[] getVersionByteArrayFromCompactInt(int version) {
        return new byte[] {
                (byte)(version >> 24),
                (byte)(version >> 16),
                (byte)(version >> 8),
                (byte)(version)
        };
    }

    // private variables -------------------------------------------------
  
    /**
    * Magic numbers to authenticate the data file
    */
    private static final byte MAGIC1 = (byte)0xda;
    private static final byte MAGIC2 = (byte)0x27;
      
    /**
    * File format authentication values
    */
    private static final byte CHAR_SET_ = 0;
    private static final byte CHAR_SIZE_ = 2;
                                                    
    /**
    * Error messages
    */
    private static final String MAGIC_NUMBER_AUTHENTICATION_FAILED_ = 
                       "ICU data file error: Not an ICU data file";
    private static final String HEADER_AUTHENTICATION_FAILED_ =
        "ICU data file error: Header authentication failed, please check if you have a valid ICU data file";
}
