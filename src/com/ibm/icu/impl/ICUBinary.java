/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/ICUBinary.java,v $
 * $Date: 2002/10/09 23:53:24 $
 * $Revision: 1.7 $
 *  *****************************************************************************************
 */
package com.ibm.icu.impl;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

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
    * @draft 2.1
    */
    public static final byte[] readHeader(InputStream inputStream,
                                        byte dataFormatIDExpected[],
                                        Authenticate authenticate) 
                                                          throws IOException
    {
        DataInputStream input = new DataInputStream(inputStream);
        char headersize = input.readChar();
        headersize -= 2;
        //reading the header format
        byte magic1 = input.readByte();
        headersize --;
        byte magic2 = input.readByte();
        headersize --;
        if (magic1 != MAGIC1 || magic2 != MAGIC2) {
            throw new IOException(MAGIC_NUMBER_AUTHENTICATION_FAILED_);
        }
        
        input.readChar(); // reading size
        headersize -= 2;
        input.readChar(); // reading reserved word
        headersize -= 2;
        byte bigendian    = input.readByte();
        headersize --;
        byte charset      = input.readByte();
        headersize --;
        byte charsize     = input.readByte();
        headersize --;
        input.readByte(); // reading reserved byte
        headersize --;
                
        byte dataFormatID[] = new byte[4];
        input.readFully(dataFormatID);
        headersize -= 4;
        byte dataVersion[] = new byte[4];
        input.readFully(dataVersion);
        headersize -= 4;
        byte unicodeVersion[] = new byte[4];
        input.readFully(unicodeVersion);
        headersize -= 4;
        input.skipBytes(headersize);

        if (bigendian != BIG_ENDIAN_ || charset != CHAR_SET_
            || charsize != CHAR_SIZE_
            || !Arrays.equals(dataFormatIDExpected, dataFormatID)
            || (authenticate != null 
                && !authenticate.isDataVersionAcceptable(dataVersion))) {
            throw new IOException(HEADER_AUTHENTICATION_FAILED_);
        }
        return unicodeVersion;
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
