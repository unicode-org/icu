/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/Attic/CollationAttribute.java,v $ 
* $Date: 2001/03/09 23:42:30 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.text;

/**
* Interface for storing ICU collation equivalent enum values.
* Constants with the prefix VALUE corresponds to ICU's UColAttributeValues,
* the rest corresponds to UColAttribute.
* @author syn wee quek
* @since Jan 18 01
*/

public final class CollationAttribute
{ 
  // Collation strength constants ----------------------------------
  
  /** 
  * Primary collation strength 
  */
  public static final int VALUE_PRIMARY = 0;
  /** 
  * Secondary collation strength 
  */
  public static final int VALUE_SECONDARY = 1;
  /** 
  * Tertiary collation strength 
  */
  public static final int VALUE_TERTIARY = 2;
  /** 
  * Default collation strength 
  */
  public static final int VALUE_DEFAULT_STRENGTH = VALUE_TERTIARY;
  /** 
  * Quaternary collation strength 
  */
  public static final int VALUE_QUATERNARY = 3;
  /** 
  * Identical collation strength 
  */
  public static final int VALUE_IDENTICAL = 15;

  // French collation mode constants ---------------------------------
  // FRENCH_COLLATION; CASE_LEVEL & DECOMPOSITION_MODE
  public static final int VALUE_OFF = 16;
  public static final int VALUE_ON = 17;
  
  // ALTERNATE_HANDLING mode constants --------------------------
  public static final int VALUE_SHIFTED = 20;
  public static final int VALUE_NON_IGNORABLE = 21;

  // CASE_FIRST mode constants ----------------------------------
  public static final int VALUE_LOWER_FIRST = 24;
  public static final int VALUE_UPPER_FIRST = 25;

  // NORMALIZATION_MODE mode constants --------------------------
  public static final int VALUE_ON_WITHOUT_HANGUL = 28;

  // Number of attribute value constants -----------------------------
  public static final int VALUE_ATTRIBUTE_VALUE_COUNT = 29;

  // Collation attribute constants -----------------------------------
  
  // attribute for direction of secondary weights
  public static final int FRENCH_COLLATION = 0;
  // attribute for handling variable elements
  public static final int ALTERNATE_HANDLING = 1;
  // who goes first, lower case or uppercase
  public static final int CASE_FIRST = 2;
  // do we have an extra case level
  public static final int CASE_LEVEL = 3;
  // attribute for normalization
  public static final int NORMALIZATION_MODE = 4; 
  // attribute for strength 
  public static final int STRENGTH = 5;
  // attribute count
  public static final int ATTRIBUTE_COUNT = 6;
  
  // public methods --------------------------------------------------
  
  /**
  * Checks if argument is a valid collation strength
  * @param strength potential collation strength
  * @return true if strength is a valid collation strength, false otherwise
  */
  protected static boolean checkStrength(int strength)
  {
    if (strength < VALUE_PRIMARY || 
        (strength > VALUE_QUATERNARY && strength != VALUE_IDENTICAL))
      return false;
    return true;
  }
  
  /**
  * Checks if argument is a valid collation type
  * @param type collation type to be checked
  * @return true if type is a valid collation type, false otherwise
  */
  protected static boolean checkType(int type)
  {
    if (type < FRENCH_COLLATION || type > STRENGTH)
      return false;
    return true;
  }
  
  /**
  * Checks if attribute type and corresponding attribute value is valid
  * @param type attribute type
  * @param value attribute value
  * @return true if the pair is valid, false otherwise
  */
  protected static boolean checkAttribute(int type, int value)
  {
    switch (type)
    {
      case FRENCH_COLLATION :
                          if (value >= VALUE_OFF && value <= VALUE_ON)
                            return true;
                          break;
      case ALTERNATE_HANDLING :
                          if (value >= VALUE_SHIFTED && 
                              value <= VALUE_NON_IGNORABLE)
                            return true;
                          break;
      case CASE_FIRST :
                          if (value >= VALUE_LOWER_FIRST && 
                              value <= VALUE_UPPER_FIRST)
                            return true;
                          break;
      case CASE_LEVEL :
                          if (value >= VALUE_LOWER_FIRST && 
                              value <= VALUE_UPPER_FIRST)
                            return true;
                          break;
      case NORMALIZATION_MODE : 
                    return NormalizationMode.check(value);
      case STRENGTH :
                          checkStrength(value);
    }
    return false;
  }
}
