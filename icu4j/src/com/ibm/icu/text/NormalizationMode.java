/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/Attic/NormalizationMode.java,v $ 
* $Date: 2001/03/09 00:34:42 $ 
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.text;

/**
* Internal interface for storing ICU normalization equivalent enum values.
* Used by RuleBaseCollator.
* @author syn wee quek
* @since Jan 18 01
*/

public final class NormalizationMode
{ 
  // public static data members -----------------------------------
  
  public static final int NO_NORMALIZATION = 1;
  /** 
  * Canonical decomposition 
  */
  public static final int DECOMP_CAN = 2;
  /** 
  * Compatibility decomposition 
  */
  public static final int DECOMP_COMPAT = 3;
  /** 
  * Default normalization 
  */
  public static final int DEFAULT_NORMALIZATION = DECOMP_COMPAT;
  /** 
  * Canonical decomposition followed by canonical composition 
  */
  public static final int DECOMP_CAN_COMP_COMPAT = 4;
  /** 
  * Compatibility decomposition followed by canonical composition 
  */
  public static final int DECOMP_COMPAT_COMP_CAN = 5;
  
  /** 
  * Do not normalize Hangul 
  */
  public static final int IGNORE_HANGUL = 16;
  
  // public methods ------------------------------------------------------
  
  /**
  * Checks if argument is a valid collation strength
  * @param strength potential collation strength
  * @return true if strength is a valid collation strength, false otherwise
  */
  static boolean check(int normalizationmode)
  {
    if (normalizationmode < NO_NORMALIZATION || 
        (normalizationmode > DECOMP_COMPAT_COMP_CAN && 
         normalizationmode != IGNORE_HANGUL))
      return false;
    return true;
  }
}
