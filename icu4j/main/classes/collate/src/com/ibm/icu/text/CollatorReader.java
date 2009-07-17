/**
*******************************************************************************
* Copyright (C) 1996-2009, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
package com.ibm.icu.text;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.ibm.icu.impl.ICUBinary;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.IntTrie;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.VersionInfo;
import com.ibm.icu.text.CollationParsedRuleBuilder.InverseUCA;
import com.ibm.icu.text.RuleBasedCollator.UCAConstants;

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
*/

final class CollatorReader
{          
    static char[] read(RuleBasedCollator rbc, UCAConstants ucac) throws IOException {
        InputStream i = ICUData.getRequiredStream(ICUResourceBundle.ICU_BUNDLE+"/coll/ucadata.icu");
        BufferedInputStream b = new BufferedInputStream(i, 90000);
        CollatorReader reader = new CollatorReader(b);
        char[] result = reader.readImp(rbc, ucac);
        b.close();
        return result;
    }

    public static InputStream makeByteBufferInputStream(final ByteBuffer buf) {
        return new InputStream() {
            public int read() throws IOException {
                if (!buf.hasRemaining()) {
                    return -1;
                }
                return buf.get() & 0xff;
            }
            public int read(byte[] bytes, int off, int len) throws IOException {
                len = Math.min(len, buf.remaining());
                buf.get(bytes, off, len);
                return len;
            }
        };
    }

    static void initRBC(RuleBasedCollator rbc, ByteBuffer data) throws IOException {
        final int MIN_BINARY_DATA_SIZE_ = (42 + 25) << 2;
        int dataLength = data.remaining();
        // TODO: Change the rest of this class to use the ByteBuffer directly, rather than
        // a DataInputStream, except for passing an InputStream to ICUBinary.readHeader().
        // Consider changing ICUBinary to also work with a ByteBuffer.
        CollatorReader reader = new CollatorReader(makeByteBufferInputStream(data), false);
        if (dataLength > MIN_BINARY_DATA_SIZE_) {
            reader.readImp(rbc, null);
        } else {
            reader.readHeader(rbc);
            reader.readOptions(rbc);
            // duplicating UCA_'s data
            rbc.setWithUCATables();
        }
    }
    
    static InverseUCA getInverseUCA() throws IOException {
        InverseUCA result = null;
        InputStream i = ICUData.getRequiredStream(ICUResourceBundle.ICU_BUNDLE+"/coll/invuca.icu");
//        try    {
//            String invdat = "/com/ibm/icu/impl/data/invuca.icu";
//            InputStream i = CollationParsedRuleBuilder.class.getResourceAsStream(invdat);
            BufferedInputStream b = new BufferedInputStream(i, 110000);
            result = CollatorReader.readInverseUCA(b);
            b.close();
            i.close();
            return result;
//        } catch (Exception e) {
//            throw new RuntimeException(e.getMessage());
//        }
    }
    
    // protected constructor ---------------------------------------------
    
    /**
    * <p>Protected constructor.</p>
    * @param inputStream ICU collator file input stream
    * @exception IOException throw if data file fails authentication 
    */
    private CollatorReader(InputStream inputStream) throws IOException
    {
        this(inputStream, true);
        /*
        byte[] UnicodeVersion = ICUBinary.readHeader(inputStream, DATA_FORMAT_ID_, UCA_AUTHENTICATE_);
        // weiv: check that we have the correct Unicode version in 
        // binary files
        VersionInfo UCDVersion = UCharacter.getUnicodeVersion();
        if(UnicodeVersion[0] != UCDVersion.getMajor() 
        || UnicodeVersion[1] != UCDVersion.getMinor()) {
            throw new IOException(WRONG_UNICODE_VERSION_ERROR_);
        }
        m_dataInputStream_ = new DataInputStream(inputStream);
        */
    }
    
    /**
    * <p>Protected constructor.</p>
    * @param inputStream ICU uprops.icu file input stream
    * @param readICUHeader flag to indicate if the ICU header has to be read
    * @exception IOException throw if data file fails authentication 
    */
    private CollatorReader(InputStream inputStream, boolean readICUHeader) 
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
    private void readHeader(RuleBasedCollator rbc) throws IOException
    {
        m_size_ = m_dataInputStream_.readInt();
        // all the offsets are in bytes
        // to get the address add to the header address and cast properly
        // Default options int options
        m_headerSize_ = m_dataInputStream_.readInt(); // start of options
        int readcount = 8; // for size and headersize
        // structure which holds values for indirect positioning and implicit
        // ranges
        int UCAConst = m_dataInputStream_.readInt();
        readcount += 4;
        // this one is needed only for UCA, to copy the appropriate
        // contractions
        m_dataInputStream_.skip(4);
        readcount += 4;
        // reserved for future use
        m_dataInputStream_.skipBytes(4);
        readcount += 4;
        // const uint8_t *mappingPosition;
        int mapping = m_dataInputStream_.readInt();
        readcount += 4;
        // uint32_t *expansion;
        rbc.m_expansionOffset_ = m_dataInputStream_.readInt();
        readcount += 4;
        // UChar *contractionIndex;
        rbc.m_contractionOffset_ = m_dataInputStream_.readInt();
        readcount += 4;
        // uint32_t *contractionCEs;
        int contractionCE = m_dataInputStream_.readInt();
        readcount += 4;
        // needed for various closures int contractionSize
        /*int contractionSize = */m_dataInputStream_.readInt();
        readcount += 4;
        // array of last collation element in expansion
        int expansionEndCE = m_dataInputStream_.readInt();
        readcount += 4;
        // array of maximum expansion size corresponding to the expansion
        // collation elements with last element in expansionEndCE
        int expansionEndCEMaxSize = m_dataInputStream_.readInt();
        readcount += 4;
        // size of endExpansionCE int expansionEndCESize
        m_dataInputStream_.skipBytes(4);
        readcount += 4;
        // hash table of unsafe code points
        int unsafe = m_dataInputStream_.readInt();
        readcount += 4;
        // hash table of final code points in contractions.
        int contractionEnd = m_dataInputStream_.readInt();
        readcount += 4;
        // int CEcount = m_dataInputStream_.readInt();
        m_dataInputStream_.skipBytes(4);
        readcount += 4;
        // is jamoSpecial
        rbc.m_isJamoSpecial_ = m_dataInputStream_.readBoolean();
        readcount++;
        // padding
        m_dataInputStream_.skipBytes(3);
        readcount += 3;
        rbc.m_version_ = readVersion(m_dataInputStream_);
        readcount += 4;
        rbc.m_UCA_version_ = readVersion(m_dataInputStream_);
        readcount += 4;
        rbc.m_UCD_version_ = readVersion(m_dataInputStream_);
        readcount += 4;
        // byte charsetName[] = new byte[32]; // for charset CEs
        m_dataInputStream_.skipBytes(32);
        readcount += 32;
        m_dataInputStream_.skipBytes(56); // for future use
        readcount += 56;
        if (m_headerSize_ < readcount) {
            ///CLOVER:OFF
            throw new IOException("Internal Error: Header size error");
            ///CLOVER:ON
        }
        m_dataInputStream_.skipBytes(m_headerSize_ - readcount);

        if (rbc.m_contractionOffset_ == 0) { // contraction can be null
            rbc.m_contractionOffset_ = mapping;
            contractionCE = mapping;
        }
        m_optionSize_ = rbc.m_expansionOffset_ - m_headerSize_;
        m_expansionSize_ = rbc.m_contractionOffset_ - rbc.m_expansionOffset_;
        m_contractionIndexSize_ = contractionCE - rbc.m_contractionOffset_;
        m_contractionCESize_ = mapping - contractionCE;
        //m_trieSize_ = expansionEndCE - mapping;
        m_expansionEndCESize_ = expansionEndCEMaxSize - expansionEndCE;
        m_expansionEndCEMaxSizeSize_ = unsafe - expansionEndCEMaxSize;
        m_unsafeSize_ = contractionEnd - unsafe;
        m_UCAValuesSize_ = m_size_ - UCAConst; // UCA value, will be handled
                                                // later
        // treat it as normal collator first
        // for normal collator there is no UCA contraction
        m_contractionEndSize_ = m_size_ - contractionEnd;

        rbc.m_contractionOffset_ >>= 1; // casting to ints
        rbc.m_expansionOffset_ >>= 2; // casting to chars
    }
    
    /**
     * Read and break up the collation options passed in the stream of data and
     * update the argument Collator with the results
     * 
     * @param rbc
     *            RuleBasedCollator to populate
     * @exception IOException
     *                thrown when there's a data error.
     */
    private void readOptions(RuleBasedCollator rbc) throws IOException
    {
        int readcount = 0;
        rbc.m_defaultVariableTopValue_ = m_dataInputStream_.readInt();
        readcount += 4;
        rbc.m_defaultIsFrenchCollation_ = (m_dataInputStream_.readInt()
                                      == RuleBasedCollator.AttributeValue.ON_);
        readcount += 4;
        rbc.m_defaultIsAlternateHandlingShifted_ 
                                   = (m_dataInputStream_.readInt() == 
                                    RuleBasedCollator.AttributeValue.SHIFTED_);
        readcount += 4;
        rbc.m_defaultCaseFirst_ = m_dataInputStream_.readInt();
        readcount += 4;
        rbc.m_defaultIsCaseLevel_ = (m_dataInputStream_.readInt() 
                                     == RuleBasedCollator.AttributeValue.ON_);
        readcount += 4;
        int value = m_dataInputStream_.readInt();
        readcount += 4;
        if (value == RuleBasedCollator.AttributeValue.ON_) {
            value = Collator.CANONICAL_DECOMPOSITION;
        }
        else {
            value = Collator.NO_DECOMPOSITION;
        }
        rbc.m_defaultDecomposition_ = value;
        rbc.m_defaultStrength_ = m_dataInputStream_.readInt();
        readcount += 4;
        rbc.m_defaultIsHiragana4_ = (m_dataInputStream_.readInt() 
                                     == RuleBasedCollator.AttributeValue.ON_);
        readcount += 4;
        rbc.m_defaultIsNumericCollation_ = (m_dataInputStream_.readInt() 
                                      == RuleBasedCollator.AttributeValue.ON_);
        readcount += 4;
        m_dataInputStream_.skip(60); // reserved for future use
        readcount += 60;
        m_dataInputStream_.skipBytes(m_optionSize_ - readcount);
        if (m_optionSize_ < readcount) {
            ///CLOVER:OFF
            throw new IOException("Internal Error: Option size error");
            ///CLOVER:ON
        }
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
    */
    private char[] readImp(RuleBasedCollator rbc, 
                          RuleBasedCollator.UCAConstants UCAConst) 
                                                            throws IOException
    {
        readHeader(rbc);
        // header size has been checked by readHeader
        int readcount = m_headerSize_; 
        // option size has been checked by readOptions
        readOptions(rbc);
        readcount += m_optionSize_;
        m_expansionSize_ >>= 2;
        rbc.m_expansion_ = new int[m_expansionSize_];
        for (int i = 0; i < m_expansionSize_; i ++) {
            rbc.m_expansion_[i] = m_dataInputStream_.readInt();
        }
        readcount += (m_expansionSize_ << 2);
        if (m_contractionIndexSize_ > 0) { 
            m_contractionIndexSize_ >>= 1;
            rbc.m_contractionIndex_ = new char[m_contractionIndexSize_];
            for (int i = 0; i < m_contractionIndexSize_; i ++) {
                rbc.m_contractionIndex_[i] = m_dataInputStream_.readChar();
            }
            readcount += (m_contractionIndexSize_ << 1);
            m_contractionCESize_ >>= 2;
            rbc.m_contractionCE_ = new int[m_contractionCESize_];
            for (int i = 0; i < m_contractionCESize_; i ++) {
                rbc.m_contractionCE_[i] = m_dataInputStream_.readInt();
            }
            readcount += (m_contractionCESize_ << 2);
        }
        rbc.m_trie_ = new IntTrie(m_dataInputStream_, 
                                 RuleBasedCollator.DataManipulate.getInstance());
        if (!rbc.m_trie_.isLatin1Linear()) {
            throw new IOException("Data corrupted, " 
                                  + "Collator Tries expected to have linear "
                                  + "latin one data arrays");
        }
        readcount += rbc.m_trie_.getSerializedDataSize();
        m_expansionEndCESize_ >>= 2;
        rbc.m_expansionEndCE_ = new int[m_expansionEndCESize_];
        for (int i = 0; i < m_expansionEndCESize_; i ++) {
            rbc.m_expansionEndCE_[i] = m_dataInputStream_.readInt();
        }
        readcount += (m_expansionEndCESize_ << 2);
        rbc.m_expansionEndCEMaxSize_ = new byte[m_expansionEndCEMaxSizeSize_];
        for (int i = 0; i < m_expansionEndCEMaxSizeSize_; i ++) {
            rbc.m_expansionEndCEMaxSize_[i] = m_dataInputStream_.readByte();
        }
        readcount += m_expansionEndCEMaxSizeSize_;
        rbc.m_unsafe_ = new byte[m_unsafeSize_];
        for (int i = 0; i < m_unsafeSize_; i ++) {
            rbc.m_unsafe_[i] = m_dataInputStream_.readByte();
        }
        readcount += m_unsafeSize_;
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
        readcount += m_contractionEndSize_;
        if (UCAConst != null) {
            UCAConst.FIRST_TERTIARY_IGNORABLE_[0] 
                                               = m_dataInputStream_.readInt();
            int readUCAConstcount = 4;
            UCAConst.FIRST_TERTIARY_IGNORABLE_[1] 
                                               = m_dataInputStream_.readInt();
            readUCAConstcount += 4;
            UCAConst.LAST_TERTIARY_IGNORABLE_[0] 
                                               = m_dataInputStream_.readInt();
            readUCAConstcount += 4;
            UCAConst.LAST_TERTIARY_IGNORABLE_[1] 
                                               = m_dataInputStream_.readInt();
            readUCAConstcount += 4;
            UCAConst.FIRST_PRIMARY_IGNORABLE_[0] 
                                               = m_dataInputStream_.readInt();
            readUCAConstcount += 4;
            UCAConst.FIRST_PRIMARY_IGNORABLE_[1] 
                                               = m_dataInputStream_.readInt();
            readUCAConstcount += 4;
            UCAConst.FIRST_SECONDARY_IGNORABLE_[0] 
                                               = m_dataInputStream_.readInt();
            readUCAConstcount += 4;
            UCAConst.FIRST_SECONDARY_IGNORABLE_[1] 
                                               = m_dataInputStream_.readInt();
            readUCAConstcount += 4;
            UCAConst.LAST_SECONDARY_IGNORABLE_[0] 
                                               = m_dataInputStream_.readInt();
            readUCAConstcount += 4;
            UCAConst.LAST_SECONDARY_IGNORABLE_[1] 
                                               = m_dataInputStream_.readInt();
            readUCAConstcount += 4;
            UCAConst.LAST_PRIMARY_IGNORABLE_[0] 
                                               = m_dataInputStream_.readInt();
            readUCAConstcount += 4;
            UCAConst.LAST_PRIMARY_IGNORABLE_[1] 
                                               = m_dataInputStream_.readInt();
            readUCAConstcount += 4;
            UCAConst.FIRST_VARIABLE_[0] = m_dataInputStream_.readInt();     
            readUCAConstcount += 4;
            UCAConst.FIRST_VARIABLE_[1] = m_dataInputStream_.readInt();
            readUCAConstcount += 4;
            UCAConst.LAST_VARIABLE_[0] = m_dataInputStream_.readInt(); 
            readUCAConstcount += 4;
            UCAConst.LAST_VARIABLE_[1] = m_dataInputStream_.readInt();                     
            readUCAConstcount += 4;
            UCAConst.FIRST_NON_VARIABLE_[0] = m_dataInputStream_.readInt();  
            readUCAConstcount += 4;
            UCAConst.FIRST_NON_VARIABLE_[1] = m_dataInputStream_.readInt();  
            readUCAConstcount += 4;
            UCAConst.LAST_NON_VARIABLE_[0] = m_dataInputStream_.readInt();  
            readUCAConstcount += 4;
            UCAConst.LAST_NON_VARIABLE_[1] = m_dataInputStream_.readInt();  
            readUCAConstcount += 4;
            UCAConst.RESET_TOP_VALUE_[0] = m_dataInputStream_.readInt();  
            readUCAConstcount += 4;
            UCAConst.RESET_TOP_VALUE_[1] = m_dataInputStream_.readInt();  
            readUCAConstcount += 4;
            UCAConst.FIRST_IMPLICIT_[0] = m_dataInputStream_.readInt();  
            readUCAConstcount += 4;
            UCAConst.FIRST_IMPLICIT_[1] = m_dataInputStream_.readInt();  
            readUCAConstcount += 4;
            UCAConst.LAST_IMPLICIT_[0] = m_dataInputStream_.readInt();  
            readUCAConstcount += 4;
            UCAConst.LAST_IMPLICIT_[1] = m_dataInputStream_.readInt();  
            readUCAConstcount += 4;
            UCAConst.FIRST_TRAILING_[0] = m_dataInputStream_.readInt();  
            readUCAConstcount += 4;
            UCAConst.FIRST_TRAILING_[1] = m_dataInputStream_.readInt();  
            readUCAConstcount += 4;
            UCAConst.LAST_TRAILING_[0] = m_dataInputStream_.readInt();  
            readUCAConstcount += 4;
            UCAConst.LAST_TRAILING_[1] = m_dataInputStream_.readInt();   
            readUCAConstcount += 4; 
            UCAConst.PRIMARY_TOP_MIN_ = m_dataInputStream_.readInt();  
            readUCAConstcount += 4;
            UCAConst.PRIMARY_IMPLICIT_MIN_ = m_dataInputStream_.readInt();   
            readUCAConstcount += 4;
            UCAConst.PRIMARY_IMPLICIT_MAX_ = m_dataInputStream_.readInt();   
            readUCAConstcount += 4;
            UCAConst.PRIMARY_TRAILING_MIN_ = m_dataInputStream_.readInt();   
            readUCAConstcount += 4;
            UCAConst.PRIMARY_TRAILING_MAX_ = m_dataInputStream_.readInt();   
            readUCAConstcount += 4;
            UCAConst.PRIMARY_SPECIAL_MIN_ = m_dataInputStream_.readInt();   
            readUCAConstcount += 4;
            UCAConst.PRIMARY_SPECIAL_MAX_ = m_dataInputStream_.readInt();   
            readUCAConstcount += 4;
            int resultsize = (m_UCAValuesSize_ - readUCAConstcount) >> 1;
            char result[] = new char[resultsize];
            for (int i = 0; i < resultsize; i ++) {
                result[i] = m_dataInputStream_.readChar();
            }
            readcount += m_UCAValuesSize_;
            if (readcount != m_size_) {
                ///CLOVER:OFF
                throw new IOException("Internal Error: Data file size error");
                ///CLOVER:ON
            }
            return result;
        }
        if (readcount != m_size_) {
            ///CLOVER:OFF
            throw new IOException("Internal Error: Data file size error");
            ///CLOVER:ON
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
    private static CollationParsedRuleBuilder.InverseUCA readInverseUCA(
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
    /*
     * Size of the Trie in bytes
     */
    //private int m_trieSize_;
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
     * Size of the option table that contains information about the collation
     * options
     */
    private int m_optionSize_;
    /**
     * Size of the whole data file minusing the ICU header
     */
    private int m_size_;
    /**
     * Size of the collation data header
     */
    private int m_headerSize_;
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

