/*
 *******************************************************************************
 * Copyright (C) 1996-2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

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
    * <p>ICU data header reader method. 
    * Takes a ICU generated big-endian input stream, parse the ICU standard 
    * file header and authenticates them.</p>
    * <p>Header format: 
    * <ul>
    *     <li> Header size (char)
    *     <li> Magic number 1 (byte)
    *     <li> Magic number 2 (byte)
    *     <li> Rest of the header size (char)
    *     <li> Reserved word (char)
    *     <li> Big endian indicator (byte)
    *     <li> Character set family indicator (byte)
    *     <li> Size of a char (byte) for c++ and c use
    *     <li> Reserved byte (byte)
    *     <li> Data format identifier (4 bytes), each ICU data has its own
    *          identifier to distinguish them. [0] major [1] minor 
    *                                          [2] milli [3] micro 
    *     <li> Data version (4 bytes), the change version of the ICU data
    *                             [0] major [1] minor [2] milli [3] micro 
    *     <li> Unicode version (4 bytes) this ICU is based on.
    * </ul>
    * </p>
    * <p>
    * Example of use:<br>
    * <pre>
    * try {
    *    FileInputStream input = new FileInputStream(filename);
    *    If (Utility.readICUDataHeader(input, dataformat, dataversion, 
    *                                  unicode) {
    *        System.out.println("Verified file header, this is a ICU data file");
    *    }
    * } catch (IOException e) {
    *    System.out.println("This is not a ICU data file");
    * }
    * </pre>
    * </p>
    * @param inputStream input stream that contains the ICU data header
    * @param dataFormatIDExpected Data format expected. An array of 4 bytes 
    *                     information about the data format.
    *                     E.g. data format ID 1.2.3.4. will became an array of 
    *                     {1, 2, 3, 4}
    * @param authenticate user defined extra data authentication. This value
    *                     can be null, if no extra authentication is needed.
    * @exception IOException thrown if there is a read error or 
    *            when header authentication fails.
    */
    public static final byte[] readHeader(InputStream inputStream,
                                        byte dataFormatIDExpected[],
                                        Authenticate authenticate) 
                                                          throws IOException
    {
        DataInputStream input = new DataInputStream(inputStream);
        char headersize = input.readChar();
        int readcount = 2;
        //reading the header format
        byte magic1 = input.readByte();
        readcount ++;
        byte magic2 = input.readByte();
        readcount ++;
        if (magic1 != MAGIC1 || magic2 != MAGIC2) {
            throw new IOException(MAGIC_NUMBER_AUTHENTICATION_FAILED_);
        }
        
        input.readChar(); // reading size
        readcount += 2;
        input.readChar(); // reading reserved word
        readcount += 2;
        byte bigendian    = input.readByte();
        readcount ++;
        byte charset      = input.readByte();
        readcount ++;
        byte charsize     = input.readByte();
        readcount ++;
        input.readByte(); // reading reserved byte
        readcount ++;
                
        byte dataFormatID[] = new byte[4];
        input.readFully(dataFormatID);
        readcount += 4;
        byte dataVersion[] = new byte[4];
        input.readFully(dataVersion);
        readcount += 4;
        byte unicodeVersion[] = new byte[4];
        input.readFully(unicodeVersion);
        readcount += 4;
        if (headersize < readcount) {
            throw new IOException("Internal Error: Header size error");
        }
        input.skipBytes(headersize - readcount);

        if (bigendian != BIG_ENDIAN_ || charset != CHAR_SET_
            || charsize != CHAR_SIZE_
            || !Arrays.equals(dataFormatIDExpected, dataFormatID)
            || (authenticate != null 
                && !authenticate.isDataVersionAcceptable(dataVersion))) {
            throw new IOException(HEADER_AUTHENTICATION_FAILED_);
        }
        return unicodeVersion;
    }

    /**
     * Same as readHeader(), but returns a VersionInfo rather than a byte[].
     */
    public static final VersionInfo readHeaderAndDataVersion(InputStream inputStream,
                                                             byte dataFormatIDExpected[],
                                                             Authenticate authenticate)
                                                                throws IOException {
        byte[] dataVersion = readHeader(inputStream, dataFormatIDExpected, authenticate);
        return VersionInfo.getInstance(dataVersion[0], dataVersion[1],
                                       dataVersion[2], dataVersion[3]);
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
            assert avail == is.read(bytes);
            while((avail = is.available()) != 0) {
                // TODO Java 6 replace new byte[] and arraycopy(): byte[] newBytes = Arrays.copyOf(bytes, bytes.length + avail);
                byte[] newBytes = new byte[bytes.length + avail];
                System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
                int numRead = is.read(newBytes, bytes.length, avail);
                assert avail == numRead;
                bytes = newBytes;
            }
            return ByteBuffer.wrap(bytes);
        } finally {
            is.close();
        }
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
    private static final byte BIG_ENDIAN_ = 1;
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
