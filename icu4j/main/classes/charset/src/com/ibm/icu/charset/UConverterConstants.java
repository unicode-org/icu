// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
* Copyright (C) 2006-2008, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/ 
package com.ibm.icu.charset;

interface UConverterConstants {

    static final short UNSIGNED_BYTE_MASK = 0xff;
    static final int UNSIGNED_SHORT_MASK = 0xffff;
    static final long UNSIGNED_INT_MASK = 0xffffffffL;
    
    static final int U_IS_BIG_ENDIAN = 0;

    /**
     * Useful constant for the maximum size of the whole locale ID
     * (including the terminating NULL).
     */
    static final int ULOC_FULLNAME_CAPACITY = 56;

    /**
     * This value is intended for sentinel values for APIs that
     * (take or) return single code points (UChar32).
     * It is outside of the Unicode code point range 0..0x10ffff.
     * 
     * For example, a "done" or "error" value in a new API
     * could be indicated with U_SENTINEL.
     *
     * ICU APIs designed before ICU 2.4 usually define service-specific "done"
     * values, mostly 0xffff.
     * Those may need to be distinguished from
     * actual U+ffff text contents by calling functions like
     * CharacterIterator::hasNext() or UnicodeString::length().
     */
    static final int U_SENTINEL = -1;

    //end utf.h

    //begin ucnv.h
    /**
     * Character that separates converter names from options and options from each other.
     * @see CharsetICU#forNameICU(String)
     */
    static final byte OPTION_SEP_CHAR  = ',';

    /** Maximum length of a converter name including the terminating NULL */
    static final int MAX_CONVERTER_NAME_LENGTH  = 60;
    /** Maximum length of a converter name including path and terminating NULL */
    static final int MAX_FULL_FILE_NAME_LENGTH = (600+MAX_CONVERTER_NAME_LENGTH);

    /** Shift in for EBDCDIC_STATEFUL and iso2022 states */
    static final int SI = 0x0F;
    /** Shift out for EBDCDIC_STATEFUL and iso2022 states */
    static final int SO = 0x0E;

    //end ucnv.h

    // begin bld.h
    /* size of the overflow buffers in UConverter, enough for escaping callbacks */
    //#define ERROR_BUFFER_LENGTH 32
    static final int ERROR_BUFFER_LENGTH = 32;

    /* at most 4 bytes per substitution character (part of .cnv file format! see UConverterStaticData) */
    static final int MAX_SUBCHAR_LEN = 4;

    /* at most 8 bytes per character in toUBytes[] (UTF-8 uses up to 6) */
    static final int MAX_CHAR_LEN = 8;

    /* converter options bits */
    static final int OPTION_VERSION     = 0xf;
    static final int OPTION_SWAP_LFNL   = 0x10;
    static final int OPTION_MAC   = 0x20; //agljport:comment added for Mac ISCII encodings

    static final String OPTION_SWAP_LFNL_STRING = ",swaplfnl";

    /** values for the unicodeMask */
    static final int HAS_SUPPLEMENTARY = 1;
    static final int HAS_SURROGATES =   2;
    // end bld.h

    // begin cnv.h
    /* this is used in fromUnicode DBCS tables as an "unassigned" marker */
    static final int missingCharMarker = 0xFFFF;
     /**
      * 
      * @author ram
      */
    static interface UConverterResetChoice {
        static final int RESET_BOTH = 0;
        static final int RESET_TO_UNICODE = RESET_BOTH + 1;
        static final int RESET_FROM_UNICODE = RESET_TO_UNICODE + 1;
    }

    // begin utf16.h
    /**
     * The maximum number of 16-bit code units per Unicode code point (U+0000..U+10ffff).
     */
    static final int U16_MAX_LENGTH = 2;
    // end utf16.h

    // begin err.h
    /**
     * FROM_U, TO_U context options for sub callback
     */
    static byte[] SUB_STOP_ON_ILLEGAL = {'i'};

    /**
     * FROM_U, TO_U context options for skip callback
     */
    static byte[] SKIP_STOP_ON_ILLEGAL = {'i'};

    /** 
     * The process condition code to be used with the callbacks.  
     * Codes which are greater than IRREGULAR should be 
     * passed on to any chained callbacks.
     */
    static interface UConverterCallbackReason {
         static final int UNASSIGNED = 0;  /**< The code point is unassigned.
                                 The error code U_INVALID_CHAR_FOUND will be set. */
         static final int ILLEGAL = 1;     /**< The code point is illegal. For example, 
                                 \\x81\\x2E is illegal in SJIS because \\x2E
                                 is not a valid trail byte for the \\x81 
                                 lead byte.
                                 Also, starting with Unicode 3.0.1, non-shortest byte sequences
                                 in UTF-8 (like \\xC1\\xA1 instead of \\x61 for U+0061)
                                 are also illegal, not just irregular.
                                 The error code U_ILLEGAL_CHAR_FOUND will be set. */
         static final int IRREGULAR = 2;   /**< The codepoint is not a regular sequence in 
                                 the encoding. For example, \\xED\\xA0\\x80..\\xED\\xBF\\xBF
                                 are irregular UTF-8 byte sequences for single surrogate
                                 code points.
                                 The error code U_INVALID_CHAR_FOUND will be set. */
         static final int RESET = 3;       /**< The callback is called with this reason when a
                                 'reset' has occured. Callback should reset all
                                 state. */
         static final int CLOSE = 4;        /**< Called when the converter is closed. The
                                 callback should release any allocated memory.*/
         static final int CLONE = 5;         /**< Called when safeClone() is called on the
                                  converter. the pointer available as the
                                  'context' is an alias to the original converters'
                                  context pointer. If the context must be owned
                                  by the new converter, the callback must clone 
                                  the data and call setFromUCallback 
                                  (or setToUCallback) with the correct pointer.
                               */
    }
    //end err.h


    static final String DATA_TYPE = "cnv";
    static final int CNV_DATA_BUFFER_SIZE = 25000;
    static final int SIZE_OF_UCONVERTER_SHARED_DATA = 100;

    static final int MAXIMUM_UCS2 =            0x0000FFFF;
    static final int MAXIMUM_UTF =             0x0010FFFF;
    //static final int MAXIMUM_UCS4 =            0x7FFFFFFF;
    static final int HALF_SHIFT =              10;
    static final int HALF_BASE =               0x0010000;
    static final int HALF_MASK =               0x3FF;
    static final int SURROGATE_HIGH_START =    0xD800;
    static final int SURROGATE_HIGH_END =      0xDBFF;
    static final int SURROGATE_LOW_START =     0xDC00;
    static final int SURROGATE_LOW_END =       0xDFFF;
    
    /* -SURROGATE_LOW_START + HALF_BASE */
    static final int SURROGATE_LOW_BASE =      9216;
}
