/**
 *******************************************************************************
 * Copyright (C) 2001-2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.lang;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.UCharacterDirection;

/**
* Testing UCharacterDirection
* @author Syn Wee Quek
* @since July 22 2002
*/
public class UCharacterDirectionTest extends TestFmwk
{    
    // constructor -----------------------------------------------------------
    
    /**
    * Private constructor to prevent initialisation
    */
    public UCharacterDirectionTest()
    {
    }
    
    // public methods --------------------------------------------------------
      
    public static void main(String[] arg)  
    {
        try
        {
            UCharacterDirectionTest test = new UCharacterDirectionTest();
            test.run(arg);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    /**
    * Gets the name of the argument category
    * @returns category name
    */
    public void TestToString()
    {
        String name[] = {"Left-to-Right",
                         "Right-to-Left", 
                         "European Number",
                         "European Number Separator",
                         "European Number Terminator",
                         "Arabic Number",
                         "Common Number Separator",
                         "Paragraph Separator",
                         "Segment Separator",
                         "Whitespace",
                         "Other Neutrals",
                         "Left-to-Right Embedding",
                         "Left-to-Right Override",
                         "Right-to-Left Arabic",
                         "Right-to-Left Embedding",
                         "Right-to-Left Override",
                         "Pop Directional Format",
                         "Non-Spacing Mark",
                         "Boundary Neutral",
                         "Unassigned"};
        for (int i = UCharacterDirection.LEFT_TO_RIGHT; 
                 i < UCharacterDirection.CHAR_DIRECTION_COUNT; i ++) {
             if (!UCharacterDirection.toString(i).equals(name[i])) {
                errln("Error toString for direction " + i + " expected " +
                      name[i]);
             }
        }
    }
}
