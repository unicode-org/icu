/**
*******************************************************************************
* Copyright (C) 1996-2003, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*      /usr/cvs/icu4j/icu4j/src/com/ibm/icu/text/UCharacterCategory.java $ 
* $Date: 2004/01/07 20:06:24 $ 
* $Revision: 1.14 $
*
*******************************************************************************
*/

package com.ibm.icu.lang;

/**
 * Enumerated Unicode category types from the UnicodeData.txt file.
 * Used as return results from <a href=UCharacter.html>UCharacter</a>
 * Equivalent to icu's UCharCategory.
 * Refer to <a href="http://www.unicode.org/Public/UNIDATA/UCD.html">
 * Unicode Consortium</a> for more information about UnicodeData.txt.
 * <p>
 * <em>NOTE:</em> the UCharacterCategory values are <em>not</em> compatible with
 * those returned by java.lang.Character.getType.  UCharacterCategory values
 * match the ones used in ICU4C, while java.lang.Character type
 * values, though similar, skip the value 17.</p>
 * <p>
 * This class is not subclassable
 * </p>
 * @author Syn Wee Quek
 * @stable ICU 2.1
 */

public final class UCharacterCategory
{
    // public variable -----------------------------------------------------
  
    /**
     * Unassigned character type
     * @stable ICU 2.1
     */
    public static final int UNASSIGNED              = 0; 
    /**
     * Character type Cn
     * Not Assigned (no characters in [UnicodeData.txt] have this property) 
     * @stable ICU 2.6
     */
    public static final int GENERAL_OTHER_TYPES     = 0;
    /**
     * Character type Lu
     * @stable ICU 2.1
     */
    public static final int UPPERCASE_LETTER        = 1;
    /**
     * Character type Ll
     * @stable ICU 2.1
     */
	public static final int LOWERCASE_LETTER        = 2;
	/**
     * Character type Lt
     * @stable ICU 2.1
     */
  	public static final int TITLECASE_LETTER        = 3;
   	/**
     * Character type Lm
     * @stable ICU 2.1
     */
   	public static final int MODIFIER_LETTER         = 4;
   	/**
     * Character type Lo
     * @stable ICU 2.1
     */
   	public static final int OTHER_LETTER            = 5;
   	/**
     * Character type Mn
     * @stable ICU 2.1
     */
  	public static final int NON_SPACING_MARK        = 6;
    /**
     * Character type Me
     * @stable ICU 2.1
     */
    public static final int ENCLOSING_MARK          = 7;
    /**
     * Character type Mc
     * @stable ICU 2.1
     */
    public static final int COMBINING_SPACING_MARK  = 8;
    /**
     * Character type Nd
     * @stable ICU 2.1      
     */
    public static final int DECIMAL_DIGIT_NUMBER    = 9;
    /**
     * Character type Nl
     * @stable ICU 2.1
     */
    public static final int LETTER_NUMBER           = 10;
    	
    // start of 11------------
    	
    /**
     * Character type No
     * @stable ICU 2.1
     */
    public static final int OTHER_NUMBER            = 11;
    /**
     * Character type Zs
     * @stable ICU 2.1
     */
    public static final int SPACE_SEPARATOR         = 12;
    /**
     * Character type Zl
     * @stable ICU 2.1
     */
    public static final int LINE_SEPARATOR          = 13;
    /**
     * Character type Zp
     * @stable ICU 2.1
     */
    public static final int PARAGRAPH_SEPARATOR     = 14;
    /**
     * Character type Cc
     * @stable ICU 2.1
     */
   	public static final int CONTROL                 = 15;
   	/**
     * Character type Cf
     * @stable ICU 2.1
     */
    public static final int FORMAT                  = 16;
    /**
     * Character type Co
     * @stable ICU 2.1
     */
   	public static final int PRIVATE_USE             = 17;
   	/**
     * Character type Cs
     * @stable ICU 2.1
     */
   	public static final int SURROGATE               = 18;
   	/**
     * Character type Pd
     * @stable ICU 2.1
     */
   	public static final int DASH_PUNCTUATION        = 19;
   	/**
     * Character type Ps
     * @stable ICU 2.1
     */
   	public static final int START_PUNCTUATION       = 20;
    	
   	// start of 21 ------------
    
   	/**
     * Character type Pe
     * @stable ICU 2.1
     */
   	public static final int END_PUNCTUATION         = 21;
   	/**
     * Character type Pc
     * @stable ICU 2.1
     */
   	public static final int CONNECTOR_PUNCTUATION   = 22;
   	/**
     * Character type Po
     * @stable ICU 2.1
     */
   	public static final int OTHER_PUNCTUATION       = 23;
   	/**
     * Character type Sm
     * @stable ICU 2.1
     */
    public static final int MATH_SYMBOL             = 24;
    /**
     * Character type Sc
     * @stable ICU 2.1
     */
   	public static final int CURRENCY_SYMBOL         = 25;
   	/**
     * Character type Sk
     * @stable ICU 2.1
     */
   	public static final int MODIFIER_SYMBOL         = 26;
   	/**
     * Character type So
     * @stable ICU 2.1
     */
   	public static final int OTHER_SYMBOL            = 27;
   	/**
     * Character type Pi
     * @see #INITIAL_QUOTE_PUNCTUATION
     * @stable ICU 2.1
     */
    public static final int INITIAL_PUNCTUATION     = 28;
   	/**
     * Character type Pi
     * This name is compatible with java.lang.Character's name for this type.
     * @see #INITIAL_PUNCTUATION
     * @draft ICU 2.8
     */
    public static final int INITIAL_QUOTE_PUNCTUATION     = 28;
    /**
     * Character type Pf
     * @see #FINAL_QUOTE_PUNCTUATION
     * @stable ICU 2.1
     */
    public static final int FINAL_PUNCTUATION       = 29;
    /**
     * Character type Pf
     * This name is compatible with java.lang.Character's name for this type.
     * @see #FINAL_PUNCTUATION
     * @draft ICU 2.8
     */
    public static final int FINAL_QUOTE_PUNCTUATION       = 29;
    	
    // start of 31 ------------
    
    /**
     * Character type count
     * @stable ICU 2.1
     */
    public static final int CHAR_CATEGORY_COUNT     = 30;
    	
    /**
     * Gets the name of the argument category
     * @param category to retrieve name
     * @return category name
     * @stable ICU 2.1
     */
    public static String toString(int category)
    {
        switch (category) {
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
    	      return "Punctuation, Initial quote";
    	    case FINAL_PUNCTUATION :
    	      return "Punctuation, Final quote";
     	}
    	return "Unassigned";
    }
    	
    // private constructor -----------------------------------------------
    ///CLOVER:OFF 
    /**
     * Private constructor to prevent initialisation
     */
    private UCharacterCategory()
    {
    }
    ///CLOVER:ON
}
