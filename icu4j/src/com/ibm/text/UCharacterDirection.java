/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*     /usr/cvs/icu4j/icu4j/src/com/ibm/icu/text/UCharacterDirection.java $ 
* $Date: 2001/02/28 20:59:44 $ 
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text;

/**
* Enumerated Unicode character linguistic direction constants.
* Used as return results from <a href=UCharacter.html>UCharacter</a>
* @author Syn Wee Quek
* @since oct0300
*/

public final class UCharacterDirection
{
  // private constructor =========================================
  
  /**
  * Private constructor to prevent initialisation
  */
  private UCharacterDirection()
  {
  }
  
  // public variable =============================================
  
  /**
  * Directional type L
  */
  public static final int LEFT_TO_RIGHT              = 0;
  /**
  * Directional type R
  */
	public static final int RIGHT_TO_LEFT              = 1;
	/**
  * Directional type EN
  */
	public static final int EUROPEAN_NUMBER            = 2;
	/**
  * Directional type ES
  */
	public static final int EUROPEAN_NUMBER_SEPARATOR  = 3;
	/**
  * Directional type ET
  */
	public static final int EUROPEAN_NUMBER_TERMINATOR = 4;
  /**
  * Directional type AN
  */	                                               
	public static final int ARABIC_NUMBER              = 5;
	/**
  * Directional type CS
  */
	public static final int COMMON_NUMBER_SEPARATOR    = 6;
	/**
  * Directional type B
  */
	public static final int BLOCK_SEPARATOR            = 7;
	/**
  * Directional type S
  */      
	public static final int SEGMENT_SEPARATOR          = 8;
	/**
  * Directional type WS
  */
	public static final int WHITE_SPACE_NEUTRAL        = 9;
	
	// start of 11 ---------------
	
	/**
  * Directional type ON
  */
	public static final int OTHER_NEUTRAL              = 10;
	/**
  * Directional type LRE
  */
	public static final int LEFT_TO_RIGHT_EMBEDDING    = 11;
	/**
  * Directional type LRO
  */
	public static final int LEFT_TO_RIGHT_OVERRIDE     = 12;  
  /**
  * Directional type AL
  */
	public static final int RIGHT_TO_LEFT_ARABIC       = 13;
	/**
  * Directional type RLE
  */
	public static final int RIGHT_TO_LEFT_EMBEDDING    = 14;
	/**
  * Directional type RLO
  */
	public static final int RIGHT_TO_LEFT_OVERRIDE     = 15;
	/**
  * Directional type PDF
  */
	public static final int POP_DIRECTIONAL_FORMAT     = 16;
	/**
  * Directional type NSM
  */
	public static final int DIR_NON_SPACING_MARK       = 17;
	/**
  * Directional type BN
  */
	public static final int BOUNDARY_NEUTRAL           = 18;
	/**
  * Number of directional type
  */
	public static final int CHAR_DIRECTION_COUNT       = 19;
	
	/**
	* Gets the name of the argument direction
	* @param dir direction type to retrieve name
	* @return directional name
	*/
	public static String toString(int dir)
	{
	  switch(dir)
	  {
	    case LEFT_TO_RIGHT :
	      return "Left-to-Right";
	    case RIGHT_TO_LEFT :
	      return "Right-to-Left";
	    case EUROPEAN_NUMBER :
	      return "European Number";
	    case EUROPEAN_NUMBER_SEPARATOR :
	      return "European Number Separator";
	    case EUROPEAN_NUMBER_TERMINATOR :
	      return "European Number Terminator";
	    case ARABIC_NUMBER :
	      return "Arabic Number";
	    case COMMON_NUMBER_SEPARATOR :
	      return "Common Number Separator";
	    case BLOCK_SEPARATOR :
	      return "Paragraph Separator";
	    case SEGMENT_SEPARATOR :
	      return "Segment Separator";
	    case WHITE_SPACE_NEUTRAL :
	      return "Whitespace";
	    case OTHER_NEUTRAL :
	      return "Other Neutrals";
	    case LEFT_TO_RIGHT_EMBEDDING :
	      return "Left-to-Right Embedding";
	    case LEFT_TO_RIGHT_OVERRIDE :
	      return "Left-to-Right Override";
	    case RIGHT_TO_LEFT_ARABIC :
	      return "Right-to-Left Arabic";
	    case RIGHT_TO_LEFT_EMBEDDING :
	      return "Right-to-Left Embedding";
	    case RIGHT_TO_LEFT_OVERRIDE :
	      return "Right-to-Left Override";
	    case POP_DIRECTIONAL_FORMAT :
	      return "Pop Directional Format";
	    case DIR_NON_SPACING_MARK :
	      return "Non-Spacing Mark";
	    case BOUNDARY_NEUTRAL :
	      return "Boundary Neutral";
	  }
	  return "Unassigned";
	}
}
