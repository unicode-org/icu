/**
*******************************************************************************
* Copyright (C) 1996-2002, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/CollatorReader.java,v $ 
* $Date: 2002/05/14 16:48:49 $ 
* $Revision: 1.1 $
*
*******************************************************************************
*/
package com.ibm.icu.text;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import com.ibm.icu.impl.ICUBinary;
import com.ibm.icu.impl.IntTrie;

/**
* <p>Internal reader class for ICU data file uca.dat containing 
* Unicode Collation Algorithm data.</p> 
* <p>This class simply reads uca.dat, authenticates that it is a valid
* ICU data file and split its contents up into blocks of data for use in
* <a href=Collator.html>com.ibm.icu.text.Collator</a>.
* </p> 
* <p>uca.dat which is in big-endian format is jared together with this 
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
    * @param inputStream ICU uprop.dat file input stream
    * @exception IOException throw if data file fails authentication 
    * @draft 2.1
    */
    protected CollatorReader(InputStream inputStream) throws IOException
    {
        ICUBinary.readHeader(inputStream, DATA_FORMAT_ID_, 
                             DATA_FORMAT_VERSION_, UNICODE_VERSION_);
        m_dataInputStream_ = new DataInputStream(inputStream);
    }
    
    /**
    * <p>Protected constructor.</p>
    * @param inputStream ICU uprop.dat file input stream
    * @param readICUHeader flag to indicate if the ICU header has to be read
    * @exception IOException throw if data file fails authentication 
    * @draft 2.1
    */
    protected CollatorReader(InputStream inputStream, boolean readICUHeader) 
    														throws IOException
    {
    	if (readICUHeader) {
        	ICUBinary.readHeader(inputStream, DATA_FORMAT_ID_, 
            		                 DATA_FORMAT_VERSION_, UNICODE_VERSION_);
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
      	m_dataInputStream_.skipBytes(4);
      	// this one is needed only for UCA, to copy the appropriate 
      	// contractions  
      	m_dataInputStream_.skipBytes(4);
      	// reserved for future use
      	m_dataInputStream_.readInt(); 
      	// const uint8_t *mappingPosition; 
      	int mapping = m_dataInputStream_.readInt(); 
      	// uint32_t *expansion; 
      	rbc.m_expansionOffset_ = m_dataInputStream_.readInt(); 
      	// UChar *contractionIndex;     
      	rbc.m_contractionOffset_ = m_dataInputStream_.readInt(); 
      	// uint32_t *contractionCEs;
      	int contractionCE = m_dataInputStream_.readInt();   
      	// needed for various closures int contractionSize 
      	m_dataInputStream_.skipBytes(4);  
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
      	m_dataInputStream_.skipBytes(3);
      	// byte version[] = new byte[4];
      	m_dataInputStream_.skipBytes(4);
      	// byte charsetName[] = new byte[32]; // for charset CEs
      	m_dataInputStream_.skipBytes(32);
      	m_dataInputStream_.skipBytes(64); // for future use 
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
    public void readOptions(RuleBasedCollator rbc) throws IOException
    {
    	rbc.m_variableTopValue_ = m_dataInputStream_.readInt();
    	rbc.setAttributeDefault(RuleBasedCollator.Attribute.FRENCH_COLLATION_,
    	                 m_dataInputStream_.readInt());
    	rbc.setAttributeDefault(
    	                 RuleBasedCollator.Attribute.ALTERNATE_HANDLING_,
    	                 m_dataInputStream_.readInt());
    	rbc.setAttributeDefault(RuleBasedCollator.Attribute.CASE_FIRST_,
    	                 m_dataInputStream_.readInt());
      	rbc.setAttributeDefault(RuleBasedCollator.Attribute.CASE_LEVEL_,
    	                 m_dataInputStream_.readInt());
      	rbc.setAttributeDefault(
      	                 RuleBasedCollator.Attribute.NORMALIZATION_MODE_,
    	                 m_dataInputStream_.readInt());
      	rbc.setAttributeDefault(RuleBasedCollator.Attribute.STRENGTH_,
    	                 m_dataInputStream_.readInt());
		rbc.setAttributeDefault(
		                 RuleBasedCollator.Attribute.HIRAGANA_QUATERNARY_MODE_,
    	                 m_dataInputStream_.readInt());
    }
    
    /**
    * Read and break up the stream of data passed in as arguments into 
    * meaningful Collator data.b
    * @param rbc RuleBasedCollator to populate
    * @exception IOException thrown when there's a data error.
    * @draft 2.2
    */
    public void read(RuleBasedCollator rbc) throws IOException
    {
    	readHeader(rbc);
    	readOptions(rbc);
    	m_expansionSize_ >>= 2;
    	rbc.m_expansion_ = new int[m_expansionSize_];
    	for (int i = 0; i < m_expansionSize_; i ++) {
    		rbc.m_expansion_[i] = m_dataInputStream_.readInt();
    	}
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
    	rbc.m_trie_ = new IntTrie(m_dataInputStream_, rbc);
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
    	rbc.m_contractionEnd_ = new byte[m_contractionEndSize_];
    	for (int i = 0; i < m_contractionEndSize_; i ++) {
    		rbc.m_contractionEnd_[i] = m_dataInputStream_.readByte();
    	}
    }
    
    // private variables -------------------------------------------------
  
    /**
    * Data input stream for uca.dat 
    */
    private DataInputStream m_dataInputStream_;
   
    /**
    * File format version and id that this class understands.
    * No guarantees are made if a older version is used
    */
    private static final byte DATA_FORMAT_VERSION_[] = 
                                   {(byte)0x2, (byte)0x0, (byte)0x0, (byte)0x0};
    private static final byte DATA_FORMAT_ID_[] = {(byte)0x55, (byte)0x43,  
                                                    (byte)0x6f, (byte)0x6c};
    private static final byte UNICODE_VERSION_[] = {(byte)0x3, (byte)0x0, 
                                                    (byte)0x0, (byte)0x0};
    /**
    * Corrupted error string
    */
    private static final String CORRUPTED_DATA_ERROR_ =
                                "Data corrupted in Collation data file";
                                
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
    private int m_UCAContractionSize_;
      
    // private methods ---------------------------------------------------
      
}

