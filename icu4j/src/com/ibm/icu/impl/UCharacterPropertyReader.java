/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/UCharacterPropertyReader.java,v $ 
* $Date: 2002/03/13 05:44:14 $ 
* $Revision: 1.3 $
*
*******************************************************************************
*/
package com.ibm.icu.impl;

import java.io.InputStream;
import java.io.DataInputStream;
import java.util.Arrays;
import java.io.IOException;
import com.ibm.icu.util.VersionInfo;

/**
* <p>Internal reader class for ICU data file uprops.dat containing 
* Unicode codepoint data.</p> 
* <p>This class simply reads uprops.dat, authenticates that it is a valid
* ICU data file and split its contents up into blocks of data for use in
* <a href=UCharacterProperty.html>com.ibm.icu.impl.UCharacterProperty</a>.
* </p> 
* <p>For more information about the format of uprops.dat refer to
* <a href=oss.software.ibm.com/icu4j/icu4jhtml/com/ibm/icu/lang/readme.html>
* ReadMe</a>.<\p>
* <p>uprops.dat which is in big-endian format is jared together with this 
* package.</p>
* @author Syn Wee Quek
* @since release 2.1, February 1st 2002
* @draft 2.1
*/

final class UCharacterPropertyReader
{
    // protected constructor ---------------------------------------------
    
    /**
    * <p>Protected constructor.</p>
    * @param inputStream ICU uprop.dat file input stream
    * @exception IOException throw if data file fails authentication 
    * @draft 2.1
    */
    protected UCharacterPropertyReader(InputStream inputStream) 
                                                        throws IOException
    {
        ICUBinary.readHeader(inputStream, DATA_FORMAT_ID_, 
                             DATA_FORMAT_VERSION_, UNICODE_VERSION_);
        m_dataInputStream_ = new DataInputStream(inputStream);
    }
    
    // protected methods -------------------------------------------------
      
    /**
    * <p>Reads uprops.dat, parse it into blocks of data to be stored in
    * UCharacterProperty.</P
    * @param ucharppty UCharacterProperty instance
    * @exception thrown when data reading fails
    * @draft 2.1
    */
    protected void read(UCharacterProperty ucharppty) throws IOException
    {
        // read the indexes
        int count = INDEX_SIZE_;
        m_propertyOffset_          = m_dataInputStream_.readInt();
        count --;
        m_exceptionOffset_         = m_dataInputStream_.readInt();
        count --;
        m_caseOffset_              = m_dataInputStream_.readInt();
        count --;
        m_additionalOffset_        = m_dataInputStream_.readInt();
        count --;
    	m_additionalVectorsOffset_ = m_dataInputStream_.readInt();
    	count --;
    	m_additionalColumnsCount_  = m_dataInputStream_.readInt();
    	count --;
        m_reservedOffset_          = m_dataInputStream_.readInt();
        count --;
        m_dataInputStream_.skipBytes(count << 2);
        
        // read the trie index block
        // m_props_index_ in terms of ints
        ucharppty.m_trie_ = new CharTrie(m_dataInputStream_, ucharppty);
        
        // reads the 32 bit properties block
        int size = m_exceptionOffset_ - m_propertyOffset_;
        ucharppty.m_property_ = new int[size];
        for (int i = 0; i < size; i ++) {
            ucharppty.m_property_[i] = m_dataInputStream_.readInt();
        }
        
        // reads the 32 bit exceptions block
        size = m_caseOffset_ - m_exceptionOffset_;
        ucharppty.m_exception_ = new int[size];
        for (int i = 0; i < size; i ++) {
            ucharppty.m_exception_[i] = m_dataInputStream_.readInt();
        }
        
        // reads the 32 bit case block
        size = (m_additionalOffset_ - m_caseOffset_) << 1;
        ucharppty.m_case_ = new char[size];
        for (int i = 0; i < size; i ++) {
            ucharppty.m_case_[i] = m_dataInputStream_.readChar();
        }
        
        // reads the additional property block
        ucharppty.m_additionalTrie_ = new CharTrie(m_dataInputStream_, 
                                                   ucharppty);
                                                           
        // additional properties
        size = m_reservedOffset_ - m_additionalVectorsOffset_;
        ucharppty.m_additionalVectors_ = new int[size];
        for (int i = 0; i < size; i ++) {
            ucharppty.m_additionalVectors_[i] = m_dataInputStream_.readInt();
        }
        
        m_dataInputStream_.close();
        ucharppty.m_additionalColumnsCount_ = m_additionalColumnsCount_;
        ucharppty.m_unicodeVersion_ = VersionInfo.getInstance(
                         (int)UNICODE_VERSION_[0], (int)UNICODE_VERSION_[1],
                         (int)UNICODE_VERSION_[2], (int)UNICODE_VERSION_[3]);
    }
    
    // private variables -------------------------------------------------
      
    /**
    * Index size
    */
    private static final int INDEX_SIZE_ = 16;
    
    /**
    * ICU data file input stream
    */
    private DataInputStream m_dataInputStream_;
      
    /**
    * Offset information in the indexes.
    */
    private int m_propertyOffset_;
    private int m_exceptionOffset_;
    private int m_caseOffset_;
    private int m_additionalOffset_;
    private int m_additionalVectorsOffset_;
    private int m_additionalColumnsCount_;
    private int m_reservedOffset_;
                                      
    /**
    * File format version that this class understands.
    * No guarantees are made if a older version is used
    */
    private static final byte DATA_FORMAT_ID_[] = {(byte)0x55, (byte)0x50, 
                                                    (byte)0x72, (byte)0x6F};
    private static final byte DATA_FORMAT_VERSION_[] = {(byte)0x3, (byte)0x0, 
                                             (byte)Trie.INDEX_STAGE_1_SHIFT_, 
                                             (byte)Trie.INDEX_STAGE_2_SHIFT_};
    private static final byte UNICODE_VERSION_[] = {(byte)0x3, (byte)0x1, 
                                                    (byte)0x1, (byte)0x0};  
}
