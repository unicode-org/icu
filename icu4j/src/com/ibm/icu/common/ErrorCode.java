/**
******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/common/Attic/ErrorCode.java,v $ 
* $Date: 2001/03/09 00:33:59 $ 
* $Revision: 1.1 $
*
******************************************************************************
*/

package com.ibm.icu4jni.common;

/**
* Error exception class mapping ICU error codes of the enum UErrorCode
* @author syn wee quek
* @since Jan 18 01
*/   
public final class ErrorCode extends Exception
{ 
  // static library loading ---------------------------------------
  
  static
  {
    ErrorCode.LIBRARY_LOADED = true;
    System.loadLibrary("icuinterface");
  }
  
  // public methods --------------------------------------------------------
  
  /**
  * Generic mapping from the error codes to java default exceptions.
  * @param error error code
  * @return java default exception that maps to the argument error code, 
  *         otherwise if error is not a valid error code, null is returned.
  */
  public static final RuntimeException getException(int error)
  {
    if (error <= U_ZERO_ERROR && error >= U_ERROR_LIMIT) {
      return null;
    }
    String errorname = ERROR_NAMES_[U_ILLEGAL_ARGUMENT_ERROR];
    switch (error) {
      case U_ILLEGAL_ARGUMENT_ERROR :
        return new IllegalArgumentException(errorname);
      case U_INDEX_OUTOFBOUNDS_ERROR :
        return new ArrayIndexOutOfBoundsException(errorname);
      case U_BUFFER_OVERFLOW_ERROR :
        return new ArrayIndexOutOfBoundsException(errorname);
      case U_UNSUPPORTED_ERROR :
        return new UnsupportedOperationException(errorname);
      default :
        return new RuntimeException(errorname);
    }
  }
  
  // public static data member ---------------------------------------------
  
  /**
  * Start of information results (semantically successful) 
  */
  public static final int U_ERROR_INFO_START = -128;
  /** 
  * A resource bundle lookup returned a fallback result (not an error) 
  */
  public static final int U_USING_FALLBACK_ERROR = -128;
  /**
  * A resource bundle lookup returned a result from the root locale (not an 
  * error) 
  */
  public static final int U_USING_DEFAULT_ERROR = -127;
  /**
  * This must always be the last warning value to indicate the limit for 
  * UErrorCode warnings (last warning code +1) 
  */
  public static final int U_ERROR_INFO_LIMIT = -126;
  
  /**
  * No error, no warning
  */
  public static final int U_ZERO_ERROR = 0;
  /**
  * Start of codes indicating failure
  */
  public static final int U_ILLEGAL_ARGUMENT_ERROR = 1;
  public static final int U_MISSING_RESOURCE_ERROR = 2;
  public static final int U_INVALID_FORMAT_ERROR = 3;
  public static final int U_FILE_ACCESS_ERROR = 4;
  /**
  * Indicates a bug in the library code
  */
  public static final int U_INTERNAL_PROGRAM_ERROR = 5;
  public static final int U_MESSAGE_PARSE_ERROR = 6;
  /**
  * Memory allocation error
  */
  public static final int U_MEMORY_ALLOCATION_ERROR = 7;
  public static final int U_INDEX_OUTOFBOUNDS_ERROR = 8;
  /**
  * Equivalent to Java ParseException
  */
  public static final int U_PARSE_ERROR = 9;
  /**
  * In the Character conversion routines: Invalid character or sequence was 
  * encountered
  */
  public static final int U_INVALID_CHAR_FOUND = 10;
  /**
  * In the Character conversion routines: More bytes are required to complete 
  * the conversion successfully
  */
  public static final int U_TRUNCATED_CHAR_FOUND = 11;
  /**
  * In codeset conversion: a sequence that does NOT belong in the codepage has 
  * been encountered
  */
  public static final int U_ILLEGAL_CHAR_FOUND = 12;
  /**
  * Conversion table file found, but corrupted
  */
  public static final int U_INVALID_TABLE_FORMAT = 13;
  /**
  * Conversion table file not found
  */
  public static final int U_INVALID_TABLE_FILE = 14;
  /**
  * A result would not fit in the supplied buffer
  */
  public static final int U_BUFFER_OVERFLOW_ERROR = 15;
  /**
  * Requested operation not supported in current context
  */
  public static final int U_UNSUPPORTED_ERROR = 16;
  /**
  * an operation is requested over a resource that does not support it
  */
  public static final int U_RESOURCE_TYPE_MISMATCH = 17;
  /**
  * ISO-2022 illlegal escape sequence 
  */
  public static final int U_ILLEGAL_ESCAPE_SEQUENCE = 18;
  /**
  * ISO-2022 unsupported escape sequence
  */
  public static final int U_UNSUPPORTED_ESCAPE_SEQUENCE = 19;
  /** 
  * No space available for in-buffer expansion for Arabic shaping 
  */
  public static final int U_NO_SPACE_AVAILABLE = 20;
  /**
  * This must always be the last value to indicate the limit for UErrorCode 
  * (last error code +1) 
  */
  public static final int U_ERROR_LIMIT = 21;
  /**
  * Load library flag
  */
  public static boolean LIBRARY_LOADED = false;
  
  // private data member ----------------------------------------------------
  
  /**
  * Array of error code names corresponding to the errorcodes.
  * ie ERROR_NAMES_[0] = name of U_ZERO_ERROR
  */
  private static final String ERROR_NAMES_[] = { 
    "U_ZERO_ERROR",               "U_ILLEGAL_ARGUMENT_ERROR", 
    "U_MISSING_RESOURCE_ERROR",   "U_INVALID_FORMAT_ERROR", 
    "U_FILE_ACCESS_ERROR",        "U_INTERNAL_PROGRAM_ERROR", 
    "U_MESSAGE_PARSE_ERROR",      "U_MEMORY_ALLOCATION_ERROR",
    "U_INDEX_OUTOFBOUNDS_ERROR",  "U_PARSE_ERROR",
    "U_INVALID_CHAR_FOUND",       "U_TRUNCATED_CHAR_FOUND", 
    "U_ILLEGAL_CHAR_FOUND",       "U_INVALID_TABLE_FORMAT",
    "U_INVALID_TABLE_FILE",       "U_BUFFER_OVERFLOW_ERROR",
    "U_UNSUPPORTED_ERROR",        "U_RESOURCE_TYPE_MISMATCH",
    "U_ILLEGAL_ESCAPE_SEQUENCE",  "U_UNSUPPORTED_ESCAPE_SEQUENCE"
  };
}

