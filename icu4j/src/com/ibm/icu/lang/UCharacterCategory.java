/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*      /usr/cvs/icu4j/icu4j/src/com/ibm/icu/text/UCharacterCategory.java $ 
* $Date: 2001/03/23 19:51:38 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text;

/**
* Enumerated Unicode category types from the UnicodeData.txt file.
* Used as return results from <a href=UCharacter.html>UCharacter</a>
* Equivalent to icu's UCharCategory.
* Refer to <a href=http://www.unicode.org/Public/UNIDATA/UnicodeData.html>
* Unicode Consortium</a> for more information about UnicodeData.txt.
* @author Syn Wee Quek
* @since oct0300
*/

public class UCharacterCategory
{
  // private constructor ===================================================
  
  /**
  * Private constructor to prevent initialisation
  */
  private UCharacterCategory()
  {
  }
  
  // public variable =======================================================
  
  /**
  * Unassigned character type
  */
  public static final int UNASSIGNED              = 0; 
  /**
  * Character type Lu
  */
  public static final int UPPERCASE_LETTER        = 1;
  /**
  * Character type Ll
  */
	public static final int LOWERCASE_LETTER        = 2;
	/**
  * Character type Lt
  */
	public static final int TITLECASE_LETTER        = 3;
	/**
  * Character type Lm
  */
	public static final int MODIFIER_LETTER         = 4;
	/**
  * Character type Lo
  */
	public static final int OTHER_LETTER            = 5;
	/**
  * Character type Lu
  */
	public static final int NON_SPACING_MARK        = 6;
	/**
  * Character type Me
  */
	public static final int ENCLOSING_MARK          = 7;
	/**
  * Character type Mc
  */
	public static final int COMBINING_SPACING_MARK  = 8;
	/**
  * Character type Nd
  */
	public static final int DECIMAL_DIGIT_NUMBER    = 9;
	/**
  * Character type Nl
  */
	public static final int LETTER_NUMBER           = 10;
	
	// start of 11------------
	
	/**
  * Character type No
  */
	public static final int OTHER_NUMBER            = 11;
	/**
  * Character type Zs
  */
	public static final int SPACE_SEPARATOR         = 12;
	/**
  * Character type Zl
  */
	public static final int LINE_SEPARATOR          = 13;
	/**
  * Character type Zp
  */
	public static final int PARAGRAPH_SEPARATOR     = 14;
	/**
  * Character type Cc
  */
	public static final int CONTROL                 = 15;
	/**
  * Character type Cf
  */
	public static final int FORMAT                  = 16;
	/**
  * Character type Co
  */
	public static final int PRIVATE_USE             = 17;
	/**
  * Character type Cs
  */
	public static final int SURROGATE               = 18;
	/**
  * Character type Pd
  */
	public static final int DASH_PUNCTUATION        = 19;
	/**
  * Character type Ps
  */
	public static final int START_PUNCTUATION       = 20;
	
	// start of 21 ------------

	/**
  * Character type Pe
  */
	public static final int END_PUNCTUATION         = 21;
	/**
  * Character type Pc
  */
	public static final int CONNECTOR_PUNCTUATION   = 22;
	/**
  * Character type Po
  */
	public static final int OTHER_PUNCTUATION       = 23;
	/**
  * Character type Sm
  */
	public static final int MATH_SYMBOL             = 24;
	/**
  * Character type Sc
  */
	public static final int CURRENCY_SYMBOL         = 25;
	/**
  * Character type Sk
  */
	public static final int MODIFIER_SYMBOL         = 26;
	/**
  * Character type So
  */
	public static final int OTHER_SYMBOL            = 27;
	/**
  * Character type Pi
  */
	public static final int INITIAL_PUNCTUATION     = 28;
	/**
  * Character type Pf
  */
	public static final int FINAL_PUNCTUATION       = 29;
	/**
  * Character type Cn
  */
	public static final int GENERAL_OTHER_TYPES     = 30;
	
	// start of 31 ------------

	/**
  * Character type count
  */
	public static final int CHAR_CATEGORY_COUNT     = 31;
	
	/**
	* Gets the name of the argument category
	* @param category to retrieve name
	* @return category name
	*/
	public static String toString(int category)
	{
	  switch (category)
	  {
	    case UPPERCASE_LETTER :
	      return "Letter, Uppercase";
	    case LOWERCASE_LETTER :
	      return "Letter, Lowercase";
	    case TITLECASE_LETTER :
	      return "Letter, Titlecase";
	    case MODIFIER_LETTER :
	      return "Letter, Modifier";
	    case OTHER_LETTER :
	      return "Letter, Other";
	    case NON_SPACING_MARK :
	      return "Mark, Non-Spacing";
	    case ENCLOSING_MARK : 
	      return "Mark, Enclosing";
	    case COMBINING_SPACING_MARK :
	      return "Mark, Spacing Combining";
	    case DECIMAL_DIGIT_NUMBER :
	      return "Number, Decimal Digit";
	    case LETTER_NUMBER :
	      return "Number, Letter";
	    case OTHER_NUMBER :
	      return "Number, Other";
	    case SPACE_SEPARATOR :
	      return "Separator, Space";
	    case LINE_SEPARATOR :
	      return "Separator, Line";
	    case PARAGRAPH_SEPARATOR :
	      return "Separator, Paragraph";
	    case CONTROL :
	      return "Other, Control";
	    case FORMAT :
	      return "Other, Format";
	    case PRIVATE_USE :
	      return "Other, Private Use";
	    case SURROGATE :
	      return "Other, Surrogate";
	    case DASH_PUNCTUATION :
	      return "Punctuation, Dash";
	    case START_PUNCTUATION :
	      return "Punctuation, Open";
	    case END_PUNCTUATION :
	      return "Punctuation, Close";
	    case CONNECTOR_PUNCTUATION :
	      return "Punctuation, Connector";
	    case OTHER_PUNCTUATION :
	      return "Punctuation, Other";
	    case MATH_SYMBOL :
	      return "Symbol, Math";
	    case CURRENCY_SYMBOL :
	      return "Symbol, Currency";
	    case MODIFIER_SYMBOL :
	      return "Symbol, Modifier";
	    case OTHER_SYMBOL :
	      return "Symbol, Other";
	    case INITIAL_PUNCTUATION :
	      return "Punctuation, Initial quote ";
	    case FINAL_PUNCTUATION :
	      return "Punctuation, Final quote ";
	  }
	  return "Unassigned";
	}
}
