/**
*******************************************************************************
* Copyright (C) 1996-2003, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/CollatorReader.java,v $ 
* $Date: 2003/06/03 18:49:34 $ 
* $Revision: 1.13 $
*
*******************************************************************************
*/
package com.ibm.icu.text;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import com.ibm.icu.impl.ICUBinary;
import com.ibm.icu.impl.IntTrie;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.VersionInfo;

/**
* <p>Internal reader class for ICU data file uca.icu containing 
* Unicode Collation Algorithm data.</p> 
* <p>This class simply reads uca.icu, authenticates that it is a valid
* ICU data file and split its contents up into blocks of data for use in
* <a href=Collator.html>com.ibm.icu.text.Collator</a>.
* </p> 
* <p>uca.icu which is in big-endian format is jared together with this 
* package.</p>
* @author Syn Wee Quek
* @since release 2.2, April 18 2002
* @draft 2.2
*/

final class CollatorReader
{          
    // protected constructor ---------------------------------------------
    
    /**
    * <p>Protected constructor.</p>
    * @param inputStream ICU callator file input stream
    * @exception IOException throw if data file fails authentication 
    * @draft 2.1
    */
    protected CollatorReader(InputStream inputStream) throws IOException
    {
        byte[] UnicodeVersion = ICUBinary.readHeader(inputStream, DATA_FORMAT_ID_, UCA_AUTHENTICATE_);
        // weiv: check that we have the correct Unicode version in 
        // binary files
        VersionInfo UCDVersion = UCharacter.getUnicodeVersion();
        if(UnicodeVersion[0] != UCDVersion.getMajor() 
        || UnicodeVersion[1] != UCDVersion.getMinor()) {
            throw new IOException(WRONG_UNICODE_VERSION_ERROR_);
        }
        m_dataInputStream_ = new DataInputStream(inputStream);
    }
    
    /**
    * <p>Protected constructor.</p>
    * @param inputStream ICU uprops.icu file input stream
    * @param readICUHeader flag to indicate if the ICU header has to be read
    * @exception IOException throw if data file fails authentication 
    * @draft 2.1
    */
    protected CollatorReader(InputStream inputStream, boolean readICUHeader) 
    														throws IOException
    {
    	if (readICUHeader) {
        	byte[] UnicodeVersion = ICUBinary.readHeader(inputStream, DATA_FORMAT_ID_, 
                                 UCA_AUTHENTICATE_);
            // weiv: check that we have the correct Unicode version in 
            // binary files
            VersionInfo UCDVersion = UCharacter.getUnicodeVersion();
            if(UnicodeVersion[0] != UCDVersion.getMajor() 
            || UnicodeVersion[1] != UCDVersion.getMinor()) {
                throw new IOException(WRONG_UNICODE_VERSION_ERROR_);
            }
    	}
        m_dataInputStream_ = new DataInputStream(inputStream);
    }
  
    // protected methods -------------------------------------------------
      
    /**
    * Read and break up the header stream of data passed in as arguments into 
    * meaningful Collator data.
    * @param rbc RuleBasedCollator to populate with header information
    * @exception IOException thrown when there's a data error.
    */
    protected void readHeader(RuleBasedCollator rbc) throws IOException
    {
    	int size = m_dataInputStream_.readInt();
    	// all the offsets are in bytes
      	// to get the address add to the header address and cast properly 
      	// Default options int options
        m_dataInputStream_.skip(4); // options
        // structure which holds values for indirect positioning and implicit 
        // ranges
      	int UCAConst = m_dataInputStream_.readInt(); 
        // this one is needed only for UCA, to copy the appropriate 
        // contractions
        m_dataInputStream_.skip(4);
      	// reserved for future use
      	m_dataInputStream_.skipBytes(4);
      	// const uint8_t *mappingPosition; 
      	int mapping = m_dataInputStream_.readInt(); 
      	// uint32_t *expansion; 
      	rbc.m_expansionOffset_ = m_dataInputStream_.readInt(); 
      	// UChar *contractionIndex;     
      	rbc.m_contractionOffset_ = m_dataInputStream_.readInt(); 
      	// uint32_t *contractionCEs;
      	int contractionCE = m_dataInputStream_.readInt();   
      	// needed for various closures int contractionSize 
      	int contractionSize = m_dataInputStream_.readInt();  
      	// array of last collation element in expansion
      	int expansionEndCE = m_dataInputStream_.readInt();  
      	// array of maximum expansion size corresponding to the expansion
        // collation elements with last element in expansionEndCE
      	int expansionEndCEMaxSize = m_dataInputStream_.readInt();     
      	// size of endExpansionCE int expansionEndCESize
      	m_dataInputStream_.skipBytes(4); 
      	// hash table of unsafe code points 
      	int unsafe = m_dataInputStream_.readInt();            
      	// hash table of final code points in contractions.
      	int contractionEnd = m_dataInputStream_.readInt();
      	// int CEcount = m_dataInputStream_.readInt();
      	m_dataInputStream_.skipBytes(4);
      	// is jamoSpecial
      	rbc.m_isJamoSpecial_ = m_dataInputStream_.readBoolean(); 
        // padding
      	m_dataInputStream_.skipBytes(3);
        rbc.m_version_ = readVersion(m_dataInputStream_);
        rbc.m_UCA_version_ = readVersion(m_dataInputStream_);
        rbc.m_UCD_version_ = readVersion(m_dataInputStream_);
      	// byte charsetName[] = new byte[32]; // for charset CEs
      	m_dataInputStream_.skipBytes(32);
      	m_dataInputStream_.skipBytes(56); // for future use 
      	if (rbc.m_contractionOffset_ == 0) { // contraction can be null
      		rbc.m_contractionOffset_ = mapping;
      		contractionCE = mapping;
      	}
      	m_expansionSize_ = rbc.m_contractionOffset_ - rbc.m_expansionOffset_;
      	m_contractionIndexSize_ = contractionCE - rbc.m_contractionOffset_;
      	m_contractionCESize_ = mapping - contractionCE;
      	m_trieSize_ = expansionEndCE - mapping;
      	m_expansionEndCESize_ = expansionEndCEMaxSize - expansionEndCE;
      	m_expansionEndCEMaxSizeSize_ = unsafe - expansionEndCEMaxSize;
      	m_unsafeSize_ = contractionEnd - unsafe;
        m_UCAValuesSize_ = size - UCAConst; // UCA value, will be handled later
        // treat it as normal collator first
        // for normal collator there is no UCA contraction
        m_contractionEndSize_ = size - contractionEnd;    
        
      	rbc.m_contractionOffset_ >>= 1; // casting to ints
      	rbc.m_expansionOffset_ >>= 2; // casting to chars
    }
    
    /**
     * Read and break up the collation options passed in the stream of data
     * and update the argument Collator with the results
     * @param rbc RuleBasedCollator to populate
     * @exception IOException thrown when there's a data error.
     * @draft 2.2
     */
    protected void readOptions(RuleBasedCollator rbc) throws IOException
    {
    	rbc.m_defaultVariableTopValue_ = m_dataInputStream_.readInt();
    	rbc.m_defaultIsFrenchCollation_ = (m_dataInputStream_.readInt()
    	                                == RuleBasedCollator.AttributeValue.ON_);
        rbc.m_defaultIsAlternateHandlingShifted_ 
                                   = (m_dataInputStream_.readInt() == 
                                    RuleBasedCollator.AttributeValue.SHIFTED_);
        rbc.m_defaultCaseFirst_ = m_dataInputStream_.readInt();
        rbc.m_defaultIsCaseLevel_ = (m_dataInputStream_.readInt() 
                                     == RuleBasedCollator.AttributeValue.ON_);
        int value = m_dataInputStream_.readInt();
    	if (value == RuleBasedCollator.AttributeValue.ON_) {
    		value = Collator.CANONICAL_DECOMPOSITION;
    	}
    	else {
    		value = Collator.NO_DECOMPOSITION;
    	}
    	rbc.m_defaultDecomposition_ = value;
    	rbc.m_defaultStrength_ = m_dataInputStream_.readInt();
    	rbc.m_defaultIsHiragana4_ = (m_dataInputStream_.readInt() 
    	                             == RuleBasedCollator.AttributeValue.ON_);
        m_dataInputStream_.skip(64); // reserved for future use
    }
    
    /**
    * Read and break up the stream of data passed in as arguments into 
    * meaningful Collator data.
    * @param rbc RuleBasedCollator to populate
    * @param UCAConst object to fill up with UCA constants if we are reading 
    *                 the UCA collator, if not use a null
    * @return UCAContractions array filled up with the UCA contractions if we
    *                        are reading the UCA collator
    * @exception IOException thrown when there's a data error.
    * @draft 2.2
    */
    protected char[] read(RuleBasedCollator rbc, 
                          RuleBasedCollator.UCAConstants UCAConst) 
                                                            throws IOException
    {
    	readHeader(rbc);
    	readOptions(rbc);
    	m_expansionSize_ >>= 2;
    	rbc.m_expansion_ = new int[m_expansionSize_];
    	for (int i = 0; i < m_expansionSize_; i ++) {
    		rbc.m_expansion_[i] = m_dataInputStream_.readInt();
    	}
        if (m_contractionIndexSize_ > 0) { 
        	m_contractionIndexSize_ >>= 1;
        	rbc.m_contractionIndex_ = new char[m_contractionIndexSize_];
        	for (int i = 0; i < m_contractionIndexSize_; i ++) {
        		rbc.m_contractionIndex_[i] = m_dataInputStream_.readChar();
        	}
        	m_contractionCESize_ >>= 2;
        	rbc.m_contractionCE_ = new int[m_contractionCESize_];
        	for (int i = 0; i < m_contractionCESize_; i ++) {
        		rbc.m_contractionCE_[i] = m_dataInputStream_.readInt();
        	}
        }
    	rbc.m_trie_ = new IntTrie(m_dataInputStream_, 
                           	  RuleBasedCollator.DataManipulate.getInstance());
    	if (!rbc.m_trie_.isLatin1Linear()) {
    		throw new IOException("Data corrupted, " 
    		                      + "Collator Tries expected to have linear "
    		                      + "latin one data arrays");
    	}
    	m_expansionEndCESize_ >>= 2;
    	rbc.m_expansionEndCE_ = new int[m_expansionEndCESize_];
    	for (int i = 0; i < m_expansionEndCESize_; i ++) {
    		rbc.m_expansionEndCE_[i] = m_dataInputStream_.readInt();
    	}
    	rbc.m_expansionEndCEMaxSize_ = new byte[m_expansionEndCEMaxSizeSize_];
    	for (int i = 0; i < m_expansionEndCEMaxSizeSize_; i ++) {
    		rbc.m_expansionEndCEMaxSize_[i] = m_dataInputStream_.readByte();
    	}
    	rbc.m_unsafe_ = new byte[m_unsafeSize_];
    	for (int i = 0; i < m_unsafeSize_; i ++) {
    		rbc.m_unsafe_[i] = m_dataInputStream_.readByte();
    	}
        if (UCAConst != null) {
            // we are reading the UCA
            // unfortunately the UCA offset in any collator data is not 0 and
            // only refers to the UCA data
            m_contractionEndSize_ -= m_UCAValuesSize_;       
        }
    	rbc.m_contractionEnd_ = new byte[m_contractionEndSize_];
    	for (int i = 0; i < m_contractionEndSize_; i ++) {
    		rbc.m_contractionEnd_[i] = m_dataInputStream_.readByte();
    	}
        if (UCAConst != null) {
            UCAConst.FIRST_TERTIARY_IGNORABLE_[0] 
                                               = m_dataInputStream_.readInt();
            m_UCAValuesSize_ -= 4;
            UCAConst.FIRST_TERTIARY_IGNORABLE_[1] 
                                               = m_dataInputStream_.readInt();
            m_UCAValuesSize_ -= 4;
            UCAConst.LAST_TERTIARY_IGNORABLE_[0] 
                                               = m_dataInputStream_.readInt();
            m_UCAValuesSize_ -= 4;
            UCAConst.LAST_TERTIARY_IGNORABLE_[1] 
                                               = m_dataInputStream_.readInt();
            m_UCAValuesSize_ -= 4;
            UCAConst.FIRST_PRIMARY_IGNORABLE_[0] 
                                               = m_dataInputStream_.readInt();
            m_UCAValuesSize_ -= 4;
            UCAConst.FIRST_PRIMARY_IGNORABLE_[1] 
                                               = m_dataInputStream_.readInt();
            m_UCAValuesSize_ -= 4;
            UCAConst.FIRST_SECONDARY_IGNORABLE_[0] 
                                               = m_dataInputStream_.readInt();
            m_UCAValuesSize_ -= 4;
            UCAConst.FIRST_SECONDARY_IGNORABLE_[1] 
                                               = m_dataInputStream_.readInt();
            m_UCAValuesSize_ -= 4;
            UCAConst.LAST_SECONDARY_IGNORABLE_[0] 
                                               = m_dataInputStream_.readInt();
            m_UCAValuesSize_ -= 4;
            UCAConst.LAST_SECONDARY_IGNORABLE_[1] 
                                               = m_dataInputStream_.readInt();
            m_UCAValuesSize_ -= 4;
            UCAConst.LAST_PRIMARY_IGNORABLE_[0] 
                                               = m_dataInputStream_.readInt();
            m_UCAValuesSize_ -= 4;
            UCAConst.LAST_PRIMARY_IGNORABLE_[1] 
                                               = m_dataInputStream_.readInt();
            m_UCAValuesSize_ -= 4;
            UCAConst.FIRST_VARIABLE_[0] = m_dataInputStream_.readInt();     
            m_UCAValuesSize_ -= 4;
            UCAConst.FIRST_VARIABLE_[1] = m_dataInputStream_.readInt();
            m_UCAValuesSize_ -= 4;
            UCAConst.LAST_VARIABLE_[0] = m_dataInputStream_.readInt(); 
            m_UCAValuesSize_ -= 4;
            UCAConst.LAST_VARIABLE_[1] = m_dataInputStream_.readInt();                     
            m_UCAValuesSize_ -= 4;
            UCAConst.FIRST_NON_VARIABLE_[0] = m_dataInputStream_.readInt();  
            m_UCAValuesSize_ -= 4;
            UCAConst.FIRST_NON_VARIABLE_[1] = m_dataInputStream_.readInt();  
            m_UCAValuesSize_ -= 4;
            UCAConst.LAST_NON_VARIABLE_[0] = m_dataInputStream_.readInt();  
            m_UCAValuesSize_ -= 4;
            UCAConst.LAST_NON_VARIABLE_[1] = m_dataInputStream_.readInt();  
            m_UCAValuesSize_ -= 4;
            UCAConst.RESET_TOP_VALUE_[0] = m_dataInputStream_.readInt();  
            m_UCAValuesSize_ -= 4;
            UCAConst.RESET_TOP_VALUE_[1] = m_dataInputStream_.readInt();  
            m_UCAValuesSize_ -= 4;
            UCAConst.FIRST_IMPLICIT_[0] = m_dataInputStream_.readInt();  
            m_UCAValuesSize_ -= 4;
            UCAConst.FIRST_IMPLICIT_[1] = m_dataInputStream_.readInt();  
            m_UCAValuesSize_ -= 4;
            UCAConst.LAST_IMPLICIT_[0] = m_dataInputStream_.readInt();  
            m_UCAValuesSize_ -= 4;
            UCAConst.LAST_IMPLICIT_[1] = m_dataInputStream_.readInt();  
            m_UCAValuesSize_ -= 4;
            UCAConst.FIRST_TRAILING_[0] = m_dataInputStream_.readInt();  
            m_UCAValuesSize_ -= 4;
            UCAConst.FIRST_TRAILING_[1] = m_dataInputStream_.readInt();  
            m_UCAValuesSize_ -= 4;
            UCAConst.LAST_TRAILING_[0] = m_dataInputStream_.readInt();  
            m_UCAValuesSize_ -= 4;
            UCAConst.LAST_TRAILING_[1] = m_dataInputStream_.readInt();   
            m_UCAValuesSize_ -= 4; 
            UCAConst.PRIMARY_TOP_MIN_ = m_dataInputStream_.readInt();  
            m_UCAValuesSize_ -= 4;
            UCAConst.PRIMARY_IMPLICIT_MIN_ = m_dataInputStream_.readInt();   
            m_UCAValuesSize_ -= 4;
            UCAConst.PRIMARY_IMPLICIT_MAX_ = m_dataInputStream_.readInt();   
            m_UCAValuesSize_ -= 4;
            UCAConst.PRIMARY_TRAILING_MIN_ = m_dataInputStream_.readInt();   
            m_UCAValuesSize_ -= 4;
            UCAConst.PRIMARY_TRAILING_MAX_ = m_dataInputStream_.readInt();   
            m_UCAValuesSize_ -= 4;
            UCAConst.PRIMARY_SPECIAL_MIN_ = m_dataInputStream_.readInt();   
            m_UCAValuesSize_ -= 4;
            UCAConst.PRIMARY_SPECIAL_MAX_ = m_dataInputStream_.readInt();   
            m_UCAValuesSize_ -= 4;
            m_UCAValuesSize_ >>= 1;
            char result[] = new char[m_UCAValuesSize_];
            for (int i = 0; i < m_UCAValuesSize_; i ++) {
                result[i] = m_dataInputStream_.readChar();
            }
            return result;
        }
        return null;
    }
    
    /**
     * Reads in the inverse uca data
     * @param input input stream with the inverse uca data
     * @return an object containing the inverse uca data
     * @exception IOException thrown when error occurs while reading the 
     *            inverse uca
     */
    protected static CollationParsedRuleBuilder.InverseUCA readInverseUCA(
                                                      InputStream inputStream)
                                                      throws IOException
    {
         byte[] UnicodeVersion = ICUBinary.readHeader(inputStream, INVERSE_UCA_DATA_FORMAT_ID_, 
                              INVERSE_UCA_AUTHENTICATE_);
                              
        // weiv: check that we have the correct Unicode version in 
        // binary files
        VersionInfo UCDVersion = UCharacter.getUnicodeVersion();
        if(UnicodeVersion[0] != UCDVersion.getMajor() 
        || UnicodeVersion[1] != UCDVersion.getMinor()) {
            throw new IOException(WRONG_UNICODE_VERSION_ERROR_);
        }
                              
        CollationParsedRuleBuilder.InverseUCA result = 
                                  new CollationParsedRuleBuilder.InverseUCA();
        DataInputStream input = new DataInputStream(inputStream);        
        input.readInt(); // bytesize
        int tablesize = input.readInt(); // in int size
        int contsize = input.readInt();  // in char size
        input.readInt(); // table in bytes
        input.readInt(); // conts in bytes
        result.m_UCA_version_ = readVersion(input);
        input.skipBytes(8); // skip padding
        
        int size = tablesize * 3; // one column for each strength
        result.m_table_ = new int[size];
        result.m_continuations_ = new char[contsize];
        
        for (int i = 0; i < size; i ++) {
            result.m_table_[i] = input.readInt();
        }
        for (int i = 0; i < contsize; i ++) {
            result.m_continuations_[i] = input.readChar();
        }
        input.close();
        return result;
    }
    
    /**
     * Reads four bytes from the input and returns a VersionInfo
     * object. Use it to read different collator versions.
     * @param input already instantiated DataInputStream, positioned 
     *              at the start of four version bytes
     * @return a ready VersionInfo object
     * @throws IOException thrown when error occurs while reading  
     *            version bytes
     */
    
    protected static VersionInfo readVersion(DataInputStream input) 
        throws IOException {
        byte[] version = new byte[4];
        version[0] = input.readByte();
        version[1] = input.readByte();
        version[2] = input.readByte();
        version[3] = input.readByte();
        
        VersionInfo result = 
        VersionInfo.getInstance(
            (int)version[0], (int)version[1], 
            (int)version[2], (int)version[3]);
        
        return result;
    }
    
    // private inner class -----------------------------------------------
    
    // private variables -------------------------------------------------
    
    /**
     * Authenticate uca data format version
     */
    private static final ICUBinary.Authenticate UCA_AUTHENTICATE_ 
                = new ICUBinary.Authenticate() {
                        public boolean isDataVersionAcceptable(byte version[])
                        {
                            return version[0] == DATA_FORMAT_VERSION_[0] 
                                   && version[1] >= DATA_FORMAT_VERSION_[1];
                                   // Too harsh 
                                   //&& version[1] == DATA_FORMAT_VERSION_[1]
                                   //&& version[2] == DATA_FORMAT_VERSION_[2] 
                                   //&& version[3] == DATA_FORMAT_VERSION_[3];
                        }
                };
                
    /**
     * Authenticate uca data format version
     */
    private static final ICUBinary.Authenticate INVERSE_UCA_AUTHENTICATE_ 
                = new ICUBinary.Authenticate() {
                        public boolean isDataVersionAcceptable(byte version[])
                        {
                            return version[0] 
                                    == INVERSE_UCA_DATA_FORMAT_VERSION_[0] 
                                && version[1] 
                                    >= INVERSE_UCA_DATA_FORMAT_VERSION_[1];
                        }
                };
  
    /**
    * Data input stream for uca.icu 
    */
    private DataInputStream m_dataInputStream_;
   
    /**
    * File format version and id that this class understands.
    * No guarantees are made if a older version is used
    */
    private static final byte DATA_FORMAT_VERSION_[] = 
                                   {(byte)0x2, (byte)0x2, (byte)0x0, (byte)0x0};
    private static final byte DATA_FORMAT_ID_[] = {(byte)0x55, (byte)0x43,  
                                                    (byte)0x6f, (byte)0x6c};
    /**
    * Inverse UCA file format version and id that this class understands.
    * No guarantees are made if a older version is used
    */
    private static final byte INVERSE_UCA_DATA_FORMAT_VERSION_[] = 
                                   {(byte)0x2, (byte)0x1, (byte)0x0, (byte)0x0};
    private static final byte INVERSE_UCA_DATA_FORMAT_ID_[] = {(byte)0x49, 
                                                               (byte)0x6e,  
                                                               (byte)0x76, 
                                                               (byte)0x43};
    /**
    * Corrupted error string
    */
    private static final String CORRUPTED_DATA_ERROR_ =
                                "Data corrupted in Collation data file";
                                
    /**
    * Wrong unicode version error string
    */
    private static final String WRONG_UNICODE_VERSION_ERROR_ =
                                "Unicode version in binary image is not compatible with the current Unicode version";

    /**
     * Size of expansion table in bytes
     */
    private int m_expansionSize_;
    /**
     * Size of contraction index table in bytes
     */
    private int m_contractionIndexSize_;
    /**
     * Size of contraction table in bytes
     */
    private int m_contractionCESize_;
    /**
     * Size of the Trie in bytes
     */
    private int m_trieSize_;
    /**
     * Size of the table that contains information about collation elements
     * that end with an expansion 
     */
    private int m_expansionEndCESize_;
    /**
     * Size of the table that contains information about the maximum size of 
     * collation elements that end with a particular expansion CE corresponding
     * to the ones in expansionEndCE
     */
    private int m_expansionEndCEMaxSizeSize_;
    /**
     * Size of the table that contains information about the "Unsafe" 
     * codepoints
     */
    private int m_unsafeSize_;
    /**
     * Size of the table that contains information about codepoints that ends
     * with a contraction
     */
    private int m_contractionEndSize_;
    /**
     * Size of the table that contains UCA contraction information
     */
    private int m_UCAValuesSize_;
      
    // private methods ---------------------------------------------------
      
}

