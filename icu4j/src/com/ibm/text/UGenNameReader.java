/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/UGenNameReader.java,v $ 
* $Date: 2001/03/23 19:51:38 $ 
* $Revision: 1.3 $
*
*******************************************************************************
*/
package com.ibm.text;

import java.io.DataInputStream;
import java.io.IOException;

/**
* Internal reader class reading binary data from unames.dat created by ICU 
* programs gennames. 
* It arranges the header and index data apart into meaningful data before 
* populating <a href=UCharacterNameDB.html>UCharacterNameDB</a>. UGenNameReader
* does not have or require the ability to decipher the rest of the data in 
* unames.dat and hence stores it as a block of data in an array of char in 
* <a href=UCharacterNameDB.html>UCharacterNameDB</a>. The ability to decipher
* the block of data lies in <a href=UCharacterName.html>UCharacterName</a>.
* For more information about the format of unames.dat refer to
* <a href=oss.software.ibm.com/icu4j/icu4jhtml/com/ibm/icu/text/readme.html>
* ReadMe</a>.<br>
* unames.dat which is in big-endian format is jared together with this package.
* @author Syn Wee Quek
* @since oct1000
*/

final class UGenNameReader extends UGenReader
{
  // private variables ===========================================
  
  /**
  * Size of the group information block in number of char
  */
  private static final int GROUP_INFO_SIZE_ = 3;

  /**
  * Index of the offset information
  */
  private int m_tokenstringindex_;
  private int m_groupindex_;
  private int m_groupstringindex_;
  private int m_algnamesindex_;
  
  /**
  * Size of an algorithmic name information group
  * start code point size + end code point size + type size + variant size + 
  * size of data size
  */
  private static final int ALG_INFO_SIZE_ = 12;
  
  /**
  * File format version and id that this class understands.
  * No guarantees are made if a older version is used
  */
  private static final byte DATA_FORMAT_VERSION_[] = 
                                  {(byte)0x1, (byte)0x0, (byte)0x0, (byte)0x0};
  private static final byte DATA_FORMAT_ID_[] = {(byte)0x75, (byte)0x6E, 
                                                 (byte)0x61, (byte)0x6D};
                                                 
  /**
  * Corrupted error string
  */
  private static final String CORRUPTED_DATA_ERROR_ =
                               "Data corrupted in character name data file";
  
  // constructor ==================================================
  
  /**
  * Constructor
  */
  protected UGenNameReader()
  {
  }
  
  // protected methods ============================================
  
  /**
  * Read and break up the stream of data passed in as arguments
  * and fills up UCharacterNameDB.
  * If unsuccessful false will be returned.
  * @param input data input stream
  * @param data instance of datablock
  * @exception thrown when there's a data error.
  */
  protected void read(DataInputStream input, UCharacterNameDB data)
                                                            throws IOException
  {
    if (!(super.read(input, data) && readIndex(input) && 
          readToken(input, data) && readGroup(input, data) && 
          readAlg(input, data))) {
      throw new IOException(CORRUPTED_DATA_ERROR_);
    }
  }
  
  /**
  * Checking the file for the correct format
  * @param dataformatid
  * @param dataformatversion
  * @return true if the file format version is correct
  */
  protected boolean authenticate(byte dataformatid[],
                                 byte dataformatversion[])
  {
    int size = DATA_FORMAT_ID_.length;
    for (int i = 0; i < size; i ++) {
      if (DATA_FORMAT_ID_[i] != dataformatid[i]) {
        return false;
      }
    }
    size = DATA_FORMAT_VERSION_.length;
    for (int i = 0; i < size; i ++) {
      if (DATA_FORMAT_VERSION_[i] != dataformatversion[i]) {
        return false;
      }
    }
    return true;
  }
  
  /**
  * Gets the size of the file id version
  * @return size of file format version in bytes
  */
  protected int getFileFormatIDSize()
  {
    return DATA_FORMAT_ID_.length;
  }
  
  /**
  * Gets the size of the file format version
  * @return size of file format version in bytes
  */
  protected int getFileFormatVersionSize()
  {
    return DATA_FORMAT_VERSION_.length;
  }
   
  // private methods =========================================
  
  /**
  * Read the indexes
  * @param input data stream
  * @return true if successfully read
  * @exception thrown when data reading fails
  */
  private boolean readIndex(DataInputStream input) throws IOException
  {
    m_tokenstringindex_ = input.readInt();
    m_groupindex_ = input.readInt();
    m_groupstringindex_ = input.readInt();
    m_algnamesindex_ = input.readInt();
    return true;
  }
  
  /**
  * Read the tokens
  * @param input data stream
  * @param data instance of UCharacterName to populate
  * @return true if successfully read
  * @exception thrown when data reading fails
  */
  private boolean readToken(DataInputStream input, UCharacterNameDB data) 
                  throws IOException
  {
    char count = input.readChar();
    char token[] = new char[count];
    for (char i = 0; i < count; i ++) {
      token[i] = input.readChar();
    }
    
    int size = m_groupindex_ - m_tokenstringindex_;
    byte tokenstr[] = new byte[size];
    input.readFully(tokenstr);
    return data.setToken(token, tokenstr);
  }
  
  /**
  * Read the groups
  * @param input data stream
  * @param data instance of UCharacterName to populate
  * @return true if successfully read
  * @exception thrown when data reading fails
  */
  private boolean readGroup(DataInputStream input, UCharacterNameDB data) 
                  throws IOException
  {
    // reading the group information records
    int count = input.readChar();
    data.setGroupCountSize(count, GROUP_INFO_SIZE_);
    count *= GROUP_INFO_SIZE_;
    char group[] = new char[count];
    for (int i = 0; i < count; i ++) {
      group[i] = input.readChar();
    }
    
    int size = m_algnamesindex_ - m_groupstringindex_;
    byte groupstring[] = new byte[size];
    input.readFully(groupstring);
    return data.setGroup(group, groupstring);
  }
  
  /**
  * Read the algorithmic names
  * @param input data stream
  * @param data instance of UCharacterName to populate
  * @return true if successfully read
  * @exception thrown when data reading fails
  */
  private boolean readAlg(DataInputStream input, UCharacterNameDB data) 
                  throws IOException
  {
    int count = input.readInt();
    UCharacterNameDB.AlgorithmName alg[] = 
                                     new UCharacterNameDB.AlgorithmName[count];
 
    for (int i = 0; i < count; i ++)
    {
      UCharacterNameDB.AlgorithmName an = readAlg(input);
      if (an == null) {
        return false;
      }
      alg[i] = an;
    }
    data.setAlgorithm(alg);
    return true;
  }
  
  /**
  * Reads an individual record of AlgorithmNames
  * @param input stream
  * @return an instance of AlgorithNames if read is successful otherwise null
  * @exception thrown when file read error occurs or data is corrupted
  */
  private UCharacterNameDB.AlgorithmName readAlg(DataInputStream input) 
                                         throws IOException
  {
    UCharacterNameDB.AlgorithmName result = 
                                          new UCharacterNameDB.AlgorithmName();
    int rangestart = input.readInt();
    int rangeend = input.readInt();
    byte type = input.readByte();
    byte variant = input.readByte();
    if (!result.setInfo(rangestart, rangeend, type, variant)) {
      return null;
    }
                     
    int size = input.readChar();
    if (type == UCharacterNameDB.AlgorithmName.TYPE_1_)
    {
      char factor[] = new char[variant];
      for (int j = 0; j < variant; j ++) {
        factor[j] = input.readChar();
      }
          
      result.setFactor(factor);
      size -= (variant << 1);
    }
      
    StringBuffer prefix = new StringBuffer();
    char c = (char)(input.readByte() & 0x00FF);
    while (c != 0)
    {
      prefix.append(c);
      c = (char)(input.readByte() & 0x00FF);
    }
    
    result.setPrefix(prefix.toString());
    
    size -= (ALG_INFO_SIZE_ + prefix.length() + 1);
    
    if (size > 0)
    {
      byte string[] = new byte[size];
      input.readFully(string);
      result.setFactorString(string);
    }
    return result;
  }
}

