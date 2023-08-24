// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*   Copyright (C) 2001-2012, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

package com.ibm.icu.dev.test.shaping;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.ArabicShaping;

/**
 * Regression test for Arabic shaping.
 */
@RunWith(JUnit4.class)
public class ArabicShapingRegTest extends TestFmwk {

    /* constants copied from ArabicShaping for convenience */

    public static final int LENGTH_GROW_SHRINK = 0;
    public static final int LENGTH_FIXED_SPACES_NEAR = 1;
    public static final int LENGTH_FIXED_SPACES_AT_END = 2;
    public static final int LENGTH_FIXED_SPACES_AT_BEGINNING = 3;

    public static final int TEXT_DIRECTION_LOGICAL = 0;
    public static final int TEXT_DIRECTION_VISUAL_LTR = 4;

    public static final int LETTERS_NOOP = 0;
    public static final int LETTERS_SHAPE = 8;
    public static final int LETTERS_SHAPE_TASHKEEL_ISOLATED = 0x18;
    public static final int LETTERS_UNSHAPE = 0x10;

    public static final int DIGITS_NOOP = 0;
    public static final int DIGITS_EN2AN = 0x20;
    public static final int DIGITS_AN2EN = 0x40;
    public static final int DIGITS_EN2AN_INIT_LR = 0x60;
    public static final int DIGITS_EN2AN_INIT_AL = 0x80;
//    private static final int DIGITS_RESERVED = 0xa0;

    public static final int DIGIT_TYPE_AN = 0;
    public static final int DIGIT_TYPE_AN_EXTENDED = 0x100;


    @Test
    public void TestEquals()
    {
        ArabicShaping as1 = new ArabicShaping(LETTERS_SHAPE | TEXT_DIRECTION_VISUAL_LTR | LENGTH_FIXED_SPACES_NEAR);
        ArabicShaping as2 = new ArabicShaping(LETTERS_SHAPE | TEXT_DIRECTION_VISUAL_LTR | LENGTH_FIXED_SPACES_NEAR);
        ArabicShaping as3 = new ArabicShaping(LETTERS_UNSHAPE | TEXT_DIRECTION_LOGICAL | LENGTH_FIXED_SPACES_AT_BEGINNING);

        if (! as1.equals(as1)) {
            err("as1: " + as1 + " does not equal itself!\n");
        }

        if (! as1.equals(as2)) {
            err("as1: " + as1 + ", as2: " + as2 + " are not equal, but should be.\n");
        }

        if (as1.equals(as3)) {
            err("as1: " + as1 + ", as3: " + as3 + " are equal but should not be.\n");
        }
    }

    /* Tests the method
     *      public int shape(char[] source, int sourceStart, int sourceLength,
     *      char[] dest, int destStart, int destSize) throws ArabicShapingException)
     */
    @Test
    public void TestShape(){
        // Tests when
        //      if (sourceStart < 0 || sourceLength < 0 || sourceStart + sourceLength > source.length)
        // Is true
        ArabicShaping as = new ArabicShaping(0);
        char[] source = {'d','u','m','m','y'};
        char[] dest = {'d','u','m','m','y'};
        int[] negNum = {-1,-2,-5,-10,-100};


        for(int i=0; i<negNum.length; i++){
            try{
                // Checks when "sourceStart < 0"
                as.shape(source, negNum[i], 0, dest, 0, 0);
                errln("ArabicShaping.shape(char[],int,int,char[],int,int) was " +
                        "suppose to return an exception when 'sourceStart < 0'.");
            } catch(Exception e){}

            try{
                // Checks when "sourceLength < 0"
                as.shape(source, 0, negNum[i], dest, 0, 0);
                errln("ArabicShaping.shape(char[],int,int,char[],int,int) was " +
                        "suppose to return an exception when 'sourceLength < 0'.");
            } catch(Exception e){}
        }

        // Checks when "sourceStart + sourceLength > source.length"
        try{
            as.shape(source, 3, 3, dest, 0, 0);
            errln("ArabicShaping.shape(char[],int,int,char[],int,int) was " +
                    "suppose to return an exception when 'sourceStart + sourceLength > source.length'.");
        } catch(Exception e){}
        try{
            as.shape(source, 2, 4, dest, 0, 0);
            errln("ArabicShaping.shape(char[],int,int,char[],int,int) was " +
                    "suppose to return an exception when 'sourceStart + sourceLength > source.length'.");
        } catch(Exception e){}
        try{
            as.shape(source, 1, 5, dest, 0, 0);
            errln("ArabicShaping.shape(char[],int,int,char[],int,int) was " +
                    "suppose to return an exception when 'sourceStart + sourceLength > source.length'.");
        } catch(Exception e){}
        try{
            as.shape(source, 0, 6, dest, 0, 0);
            errln("ArabicShaping.shape(char[],int,int,char[],int,int) was " +
                    "suppose to return an exception when 'sourceStart + sourceLength > source.length'.");
        } catch(Exception e){}

        // Checks when "if (dest == null && destSize != 0)" is true
        try{
            as.shape(source, 2, 2, null, 0, 1);
            errln("ArabicShaping.shape(char[],int,int,char[],int,int) was " +
                    "suppose to return an exception when 'dest == null && destSize != 0'.");
        } catch(Exception e){}

        // Checks when
        // if ((destSize != 0) && (destStart < 0 || destSize < 0 || destStart + destSize > dest.length))
        for(int i=0; i<negNum.length; i++){
            try{
                as.shape(source, 2, 2, dest, negNum[i], 1);
                errln("ArabicShaping.shape(char[],int,int,char[],int,int) was " +
                        "suppose to return an exception when " +
                        "(destSize != 0) && (destStart < 0 || destSize < 0 || destStart + destSize > dest.length).");
            } catch(Exception e){}

            try{
                as.shape(source, 2, 2, dest, 0, negNum[i]);
                errln("ArabicShaping.shape(char[],int,int,char[],int,int) was " +
                        "suppose to return an exception when " +
                        "(destSize != 0) && (destStart < 0 || destSize < 0 || destStart + destSize > dest.length).");
            } catch(Exception e){}
        }

        // Checks when "destStart + destSize > dest.length"
        try{
            as.shape(source, 2, 2, dest, 3, 3);
            errln("ArabicShaping.shape(char[],int,int,char[],int,int) was " +
                    "suppose to return an exception when " +
                    "(destSize != 0) && (destStart < 0 || destSize < 0 || destStart + destSize > dest.length).");
        } catch(Exception e){}
        try{
            as.shape(source, 2, 2, dest, 2, 4);
            errln("ArabicShaping.shape(char[],int,int,char[],int,int) was " +
                    "suppose to return an exception when " +
                    "(destSize != 0) && (destStart < 0 || destSize < 0 || destStart + destSize > dest.length).");
        } catch(Exception e){}
        try{
            as.shape(source, 2, 2, dest, 1, 5);
            errln("ArabicShaping.shape(char[],int,int,char[],int,int) was " +
                    "suppose to return an exception when " +
                    "(destSize != 0) && (destStart < 0 || destSize < 0 || destStart + destSize > dest.length).");
        } catch(Exception e){}
        try{
            as.shape(source, 2, 2, dest, 0, 6);
            errln("ArabicShaping.shape(char[],int,int,char[],int,int) was " +
                    "suppose to return an exception when " +
                    "(destSize != 0) && (destStart < 0 || destSize < 0 || destStart + destSize > dest.length).");
        } catch(Exception e){}

        // Tests when "throw new IllegalArgumentException("Wrong Tashkeel argument")"
        int[] invalid_Tashkeel = {-1000, -500, -100};
        for(int i=0; i < invalid_Tashkeel.length; i++){
            ArabicShaping arabicShape = new ArabicShaping(invalid_Tashkeel[i]);
            try {
                arabicShape.shape(source,0,0,dest,0,1);
                errln("ArabicShaping.shape(char[],int,int,char[],int,int) was " +
                        "suppose to return an exception for 'Wrong Tashkeel argument' for " +
                        "an option value of " + invalid_Tashkeel[i]);
            } catch (Exception e) {}
        }
    }

    @Test
    public void TestCoverage() {
        ArabicShaping shp = new ArabicShaping(LETTERS_SHAPE | TEXT_DIRECTION_VISUAL_LTR | LENGTH_FIXED_SPACES_NEAR);

        // Test ArabicShaping#toString();
        assertEquals("ArabicShaping#toString() failed.",
                shp.toString(),
                "com.ibm.icu.text.ArabicShaping@d[LamAlef spaces at near, visual, shape letters," +
                        " no digit shaping, standard Arabic-Indic digits]");

        // Test ArabicShaping#hashCode()
        assertEquals("ArabicShaping#hashCode() failed.", shp.hashCode(), 13);
    }

    private boolean getStaticCharacterHelperFunctionValue(String methodName, char testValue) throws Exception {
        Method m = ArabicShaping.class.getDeclaredMethod(methodName, Character.TYPE);
        m.setAccessible(true);
        Object returnValue = m.invoke(null, testValue);

        if (Integer.class.isInstance(returnValue)) {
            return (Integer)returnValue == 1;
        }
        return (Boolean)returnValue;
    }

    @Test
    public void TestHelperFunctions() throws Exception {
        // Test private static helper functions that are used internally:

        // ArabicShaping.isSeenTailFamilyChar(char)
        assertTrue("ArabicShaping.isSeenTailFamilyChar(char) failed.",
                getStaticCharacterHelperFunctionValue("isSeenTailFamilyChar", (char)0xfeb1));

        // ArabicShaping.isAlefMaksouraChar(char)
        assertTrue("ArabicShaping.isAlefMaksouraChar(char) failed.",
                getStaticCharacterHelperFunctionValue("isAlefMaksouraChar", (char)0xfeef));

        // ArabicShaping.isTailChar(char)
        assertTrue("ArabicShaping.isTailChar(char) failed.",
                getStaticCharacterHelperFunctionValue("isTailChar", (char)0x200B));

        // ArabicShaping.isYehHamzaChar(char)
        assertTrue("ArabicShaping.isYehHamzaChar(char) failed.",
                getStaticCharacterHelperFunctionValue("isYehHamzaChar", (char)0xfe89));

    }
}

