/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/UGenReader.java,v $ 
* $Date: 2001/08/22 22:38:30 $ 
* $Revision: 1.4 $
*
*******************************************************************************
*/
package com.ibm.text;

import java.io.DataInputStream;
import java.io.EOFException;
import java.util.Arrays;

/**
* Internal parent reader class reading binary header data from uprops.dat and 
* unames.dat, created by ICU programs genprops and gennames. 
* It arranges the header data into meaningful data before 
* populating <a href=UCharacterDB.html>UCharacterDB</a>. It also authenticates
* that the data files before proceeding on.
* For more information about the format of uprops.dat refer to
* <a href=oss.software.ibm.com/icu4j/icu4jhtml/com/ibm/icu/text/readme.html>
* ReadMe</a>.<br>
* uprops.data and unames.dat which are in big-endian format are jared together 
* with this package.
* @author Syn Wee Quek
* @since oct1000
*/

abstract class UGenReader
{
  // private variables ===========================================
  
  /**
  * Magic numbers to authenticate the data file
  */
  private static final byte MAGIC1 = (byte)0xda;
  private static final byte MAGIC2 = (byte)0x27;
  
  /**
  * Size of the field datasize and reservedcharacter
  */
  private static final int SKIP_BYTES_ = 4;
  
  /**
  * File format authentication values
  */
  private static final byte BIG_ENDIAN_ = 1;
  private static final byte CHAR_SET_ = 0;
  private static final byte CHAR_SIZE_ = 2;
  private static final byte UNICODE_VERSION_[] = {(byte)0x3, (byte)0x0, 
                                                 (byte)0x0, (byte)0x0};
  
  // constructor =================================================
  
  /**
  * Protected constructor
  */
  protected UGenReader()
  {
  }
  
  // protected methods ===========================================
  
  /**
  * Read the data header and fills the relevant information into UCharacterDB.
  * If unsuccessful false will be returned
  * @param input data stream
  * @param data data instance
  * @return true if successfully filled
  */
  protected boolean read(DataInputStream input, UCharacterDB data)
  {
    try
    {
      char headersize = input.readChar();
      headersize -= 2;
      //reading the header format
      byte magic1 = input.readByte();
      headersize --;
      byte magic2 = input.readByte();
      headersize --;
      input.skipBytes(SKIP_BYTES_);
      headersize -= SKIP_BYTES_;
      if (authenticate(magic1, magic2))
      {
        byte bigendian = input.readByte();
        headersize --;
        byte charset = input.readByte();
        headersize --;
        byte charsize = input.readByte();
        headersize --;
        input.readByte();
        headersize --;
        
        byte dataformatid[] = new byte[getFileFormatIDSize()];
        input.readFully(dataformatid);
        headersize -= getFileFormatIDSize();
        byte dataformatversion[] = new byte[getFileFormatVersionSize()];
        input.readFully(dataformatversion);
        headersize -= getFileFormatVersionSize();
        byte unicodeversion[] = new byte[UNICODE_VERSION_.length];
        input.readFully(unicodeversion);
        headersize -= UNICODE_VERSION_.length;
        input.skipBytes(headersize);
        
        if (authenticate(bigendian, charset, charsize, unicodeversion) && 
            authenticate(dataformatid, dataformatversion)) {
          return setUCharacterDB(data, unicodeversion);
        }
      }
    } 
    catch (Exception e) {
        return false;
    }
    return false;
  }
  
  /**
  * Abstract method for verifying the file format version
  * @param formatid file format identification
  * @param formatversion file format version of input file to be verified
  * @return true if the right file format version is used
  */
  protected abstract boolean authenticate(byte formatid[], 
                                          byte formatversion[]);
  
  /**
  * Abstract method for getting the size of the file format version
  * @return size of file format version in bytes
  */
  protected abstract int getFileFormatVersionSize();
  
  /**
  * Abstract method for getting the size of the file format id
  * @return size of file format id in bytes
  */
  protected abstract int getFileFormatIDSize();
  
  // private methods ====================================================
  
  /**
  * Checking the file against the magic numbers for authenticity
  * @param m1 magic number 1
  * @param m2 magic number 2
  * @return true if the magic numbers are correct
  */
  private boolean authenticate(byte m1, byte m2)
  {
    if (m1 == MAGIC1 && m2 == MAGIC2) {
      return true;
    }
    return false;
  }
  
  /**
  * Checking the file for the correct format
  * @param bigendian
  * @param charset
  * @param charsize
  * @param dataformatid
  * @param dataformatversion
  * @param unicodeversion
  * @return true if the file is in bigendian, charset , charsize == 2, 
  *         dataformatid 85.80.114.111, dataformatversion dependent on file,
  *         and unicodeversion > 3.0.0.0
  */
  private boolean authenticate(byte bigendian, byte charset, byte charsize, 
                               byte unicodeversion[])
  {
    if (bigendian != BIG_ENDIAN_ || charset != CHAR_SET_ || 
        charsize != CHAR_SIZE_) {
      return false;
    }
    return Arrays.equals(UNICODE_VERSION_, unicodeversion);
  }
  
  /**
  * Sets the relevant data into UCharacterDB
  * @param data UCharacterDB instance to populate
  * @param unicodeversion version number of the Unicode data information used
  * @param formatversion icu version number of the uprops.dat and unames.dat
  *        used
  * @return true if operation is successful, false otherwise
  */
  private boolean setUCharacterDB(UCharacterDB data, byte[] unicodeversion)
  {
    boolean result = data.setUnicodeVersion(unicodeversion);
    return result;
  }
}

