/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/Attic/UGenPropReader.java,v $ 
* $Date: 2001/02/26 23:45:37 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/
package com.ibm.icu.text;

import java.io.DataInputStream;
import java.util.Arrays;

/**
* Internal reader class reading binary data from uprops.dat created by ICU 
* programs genprops. 
* It arranges the header and index data apart into meaningful data before 
* populating <a href=UCharacterPropDB.html>UCharacterPropDB</a>. UGenPropReader
* does not have or require the ability to decipher the rest of the data in 
* uprop.dat and hence stores it as a block of data in an array of char in 
* <a href=UCharacterPropDB.html>UCharacterPropDB</a>. The ability to decipher
* the block of data lies in <a href=UCharacterProp.html>UCharacterProp</a>.
* For more information about the format of uprops.dat refer to
* <a href=oss.software.ibm.com/icu4j/icu4jhtml/com/ibm/icu/text/readme.html>
* ReadMe</a>.<br>
* uprops.dat which is in big-endian format is jared together with this package.
* @author Syn Wee Quek
* @since oct0200
*/

final class UGenPropReader extends UGenReader
{
  // private variables ===========================================
  
  /**
  * Index size
  */
  private static final int INDEX_SIZE_ = 8;
  
  /**
  * Elements in the index where addresses are in number of chars.
  * Size is basically the count and does not depend on the type.
  */
  private char m_stage2indexsize_;
  private char m_stage3indexsize_;
  private int m_exception_;
  private char m_stage3_;
  private int m_prop_;
  private int m_case_;
  private char m_end_;
                              
  /**
  * Size of actual number of bits used in surrogate unicode character
  */
  private static final int USED_SURROGATE_BIT_SIZE_ = 21;
  
  /**
  * File format version that this class understands.
  * No guarantees are made if a older version is used
  */
  private static final byte DATA_FORMAT_ID_[] = {(byte)0x55, (byte)0x50, 
                                                 (byte)0x72, (byte)0x6F};
  private static final byte DATA_FORMAT_VERSION_[] = 
                                  {(byte)0x1, (byte)0x2, (byte)0x0, (byte)0x0};
     
  // constructor =============================================
  
  /**
  * Constructor
  */
  protected UGenPropReader()
  {
  }
 
  // protected methods ==================================================
  
  /**
  * Read and fills up UCharacterPptyDB.
  * If unsuccessful false will be returned
  * @param input data stream
  * @param data data instance
  * @return true if successfully filled
  * @exception thrown when data reading fails
  */
  protected boolean read(DataInputStream input, UCharacterPropertyDB data)
                    throws Exception
  {
    if (super.read(input, data) &&
      // read the indexes
      readIndex(input, data) && 
      // read the stages block
      readStage(input, data) && 
      // read the property data
      readProperty(input, data) &&
      // read the exception data
      readException(input, data) &&
      // read the case data
      readCase(input,data))
        return true;
    return false;
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
    return Arrays.equals(DATA_FORMAT_ID_, dataformatid) &&
           Arrays.equals(DATA_FORMAT_VERSION_, dataformatversion);
  }
  
  /**
  * Gets the size of the file format version
  * @return size of file format version in bytes
  */
  protected int getFileFormatVersionSize()
  {
    return DATA_FORMAT_VERSION_.length;
  }
  
  /**
  * Gets the size of the file format id
  * @return size of file format id in bytes
  */
  protected int getFileFormatIDSize()
  {
    return DATA_FORMAT_ID_.length;
  }
  
  // private methods ===================================================
  
  /**
  * Read the INDEX_SIZE_ indexes and updates the instance of 
  * UCharacterPropertyDB with the processed shifts and mask
  * @param input data stream
  * @param data instance of UCharacterPropertyDB
  * @return true if successfully read
  * @exception thrown when data reading fails
  */
  private boolean readIndex(DataInputStream input, UCharacterPropertyDB data) 
                  throws Exception
  {
    int count = INDEX_SIZE_;
    m_stage2indexsize_ = input.readChar();
    count --;
    m_stage3indexsize_ = input.readChar();
    count --;
    m_exception_ = input.readChar();
    count --;
    m_stage3_ = input.readChar();
    count --;
    m_prop_ = input.readChar();
    count --;
    m_case_ = input.readChar();
    count --;
    m_end_ = input.readChar();
    count --;
    input.skipBytes(count << 1);
        
    return data.setInfo(m_stage3indexsize_ + m_stage2indexsize_,
                        (int)m_stage3indexsize_,
                        (1 << m_stage2indexsize_) - 1,
                        (1 << m_stage3indexsize_) - 1);
  }
  
  /**
  * Read the stages block and updates the instance of UCharacterPropertyDB 
  * with the stages data
  * @param input data stream
  * @param data instance of UCharacterPropertyDB
  * @return true if successfully read
  * @exception thrown when data reading fails
  */
  private boolean readStage(DataInputStream input, UCharacterPropertyDB data) 
                  throws Exception
  {  
    // size of the 3 stages
    int stagesize = (m_prop_ << 1) - INDEX_SIZE_;
      
    char array[] = new char[stagesize];
    
    int max = 0;
    int props = m_prop_ - INDEX_SIZE_;
    // setting up the propery index for stage 1 to 3
    for (int count = 0; count < stagesize; count ++)
    {
      array[count] = (char)(input.readChar() - INDEX_SIZE_);
      if (max < array[count] && count < 0x448)
        max = array[count];
      
      // setting up the property index for stage 3
      // uprops.dat contain data that includes the address from the top of 
      // index to property data. since the blocks are split up, so now i have 
      // to subtract the excess address from it.
      if (count >= m_stage3_ - INDEX_SIZE_)
        array[count] -= props;    
    }
    
    // synwee : hmm... gaps in stage 2.
    /*
    System.out.println("stage 3 " + (int)m_stage3_);
    System.out.println("stage  2 top " + (max - 0x440 - INDEX_SIZE_));
    */
    
    // setting up the stages block in the instance of UCharacterPropertyDB
    return data.setStage(array);
  }

  /**
  * Read the propery data block and updates the instance of 
  * UCharacterPropertyDB with the data
  * @param input data stream
  * @param data instance of UCharacterPropertyDB
  * @return true if successfully read
  * @exception thrown when data reading fails
  */
  private boolean readProperty(DataInputStream input, 
                               UCharacterPropertyDB data) throws Exception
  {  
    // getting size of the property block
    int size = m_exception_ - m_prop_;
    int ppty[] = new int[size];
    for (int i = 0; i < size; i ++)
      ppty[i] = input.readInt();     
       
    // setting up the property block in the instance of UCharacterPropertyDB
    return data.setProperty(ppty);
  }
  
  /**
  * Read the character case data block and updates the instance of 
  * UCharacterPropertyDB with the data
  * @param input data stream
  * @param data instance of UCharacterPropertyDB
  * @return true if successfully read
  * @exception thrown when data reading fails
  */
  private boolean readCase(DataInputStream input, 
                           UCharacterPropertyDB data) throws Exception
  {  
    // getting size of the case block
    int size = (m_end_ - m_case_) << 1;
    char casetable[] = new char[size];
    for (int i = 0; i < size; i ++)
      casetable[i] = input.readChar();     
         
    // setting up the case block in the instance of UCharacterPropertyDB
    return data.setCase(casetable);
  }
  
  /**
  * Read the exception data block and updates the instance of 
  * UCharacterPropertyDB with the data
  * @param input data stream
  * @param data instance of UCharacterPropertyDB
  * @return true if successfully read
  * @exception thrown when data reading fails
  */
  private boolean readException(DataInputStream input, 
                                UCharacterPropertyDB data) throws Exception
  {  
    int size = m_case_ - m_exception_;
    int exception[] = new int[size];
    for (int i = 0; i < size; i ++)
      exception[i] = input.readInt();     
       
    // setting up the property block in the instance of UCharacterPropertyDB
    return data.setException(exception);
  }
}