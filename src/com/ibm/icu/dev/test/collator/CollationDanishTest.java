/*
 *******************************************************************************
 * Copyright (C) 2002, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 * $Source: 
 * $Date: 
 * $Revision: 
 *
 *****************************************************************************************
 */

/** 
 * Port From:   ICU4C v2.1 : Collate/CollationDanishTest
 * Source File: $ICU4CRoot/source/test/intltest/dacoll.cpp
 **/

package com.ibm.icu.dev.test.collator;
 
import com.ibm.icu.dev.test.*;
import com.ibm.icu.text.*;
import java.util.Locale;
 
public class CollationDanishTest extends TestFmwk{
    public static void main(String[] args) throws Exception {
        new CollationDanishTest().run(args);
    }
    
    private static char[][] testSourceCases = {
        {0x004C /* 'L' */, 0x0075 /* 'u' */, 0x0063 /* 'c' */},
        {0x006C /* 'l' */, 0x0075 /* 'u' */, 0x0063 /* 'c' */, 0x006B /* 'k' */},
        {0x004C /* 'L' */, 0x00FC, 0x0062 /* 'b' */, 0x0065 /* 'e' */, 0x0063 /* 'c' */, 0x006B /* 'k' */},
        {0x004C /* 'L' */, 0x00E4, 0x0076 /* 'v' */, 0x0069 /* 'i' */},
        {0x004C /* 'L' */, 0x00F6, 0x0077 /* 'w' */, 0x0077 /* 'w' */},
        {0x004C /* 'L' */, 0x0076 /* 'v' */, 0x0069 /* 'i' */},
        {0x004C /* 'L' */, 0x00E4, 0x0076 /* 'v' */, 0x0069 /* 'i' */},
        {0x004C /* 'L' */, 0x00FC, 0x0062 /* 'b' */, 0x0065 /* 'e' */, 0x0063 /* 'c' */, 0x006B /* 'k' */}
    };

    private static char[][] testTargetCases = {
        {0x006C /* 'l' */, 0x0075 /* 'u' */, 0x0063 /* 'c' */, 0x006B /* 'k' */},
        {0x004C /* 'L' */, 0x00FC, 0x0062 /* 'b' */, 0x0065 /* 'e' */, 0x0063 /* 'c' */, 0x006B /* 'k' */},
        {0x006C /* 'l' */, 0x0079 /* 'y' */, 0x0062 /* 'b' */, 0x0065 /* 'e' */, 0x0063 /* 'c' */, 0x006B /* 'k' */},
        {0x004C /* 'L' */, 0x00F6, 0x0077 /* 'w' */, 0x0065 /* 'e' */},
        {0x006D /* 'm' */, 0x0061 /* 'a' */, 0x0073 /* 's' */, 0x0074 /* 't' */},
        {0x004C /* 'L' */, 0x0077 /* 'w' */, 0x0069 /* 'i' */},
        {0x004C /* 'L' */, 0x00F6, 0x0077 /* 'w' */, 0x0069 /* 'i' */},
        {0x004C /* 'L' */, 0x0079 /* 'y' */, 0x0062 /* 'b' */, 0x0065 /* 'e' */, 0x0063 /* 'c' */, 0x006B /* 'k' */}
    };

    private static int[] results = {
        -1,
        -1,
        1,
        -1,
        -1,
        /* test primary > 5*/
        0,
        -1,
        0
    };

    private static char[][] testBugs = {
        {0x0041 /* 'A' */, 0x002F /* '/' */, 0x0053 /* 'S' */},
        {0x0041 /* 'A' */, 0x004E /* 'N' */, 0x0044 /* 'D' */, 0x0052 /* 'R' */, 0x0045 /* 'E' */},
        {0x0041 /* 'A' */, 0x004E /* 'N' */, 0x0044 /* 'D' */, 0x0052 /* 'R' */, 0x00C9},
        {0x0041 /* 'A' */, 0x004E /* 'N' */, 0x0044 /* 'D' */, 0x0052 /* 'R' */, 0x0045 /* 'E' */, 0x0041 /* 'A' */, 0x0053 /* 'S' */},
        {0x0041 /* 'A' */, 0x0053 /* 'S' */},
        {0x0043 /* 'C' */, 0x0041 /* 'A' */},
        {0x00C7, 0x0041 /* 'A' */},
        {0x0043 /* 'C' */, 0x0042 /* 'B' */},
        {0x00C7, 0x0043 /* 'C' */,0x0000 /* '\0' */},
        {0x0044 /* 'D' */, 0x002E /* '.' */, 0x0053 /* 'S' */, 0x002E /* '.' */, 0x0042 /* 'B' */, 0x002E /* '.' */},
        {0x0044 /* 'D' */, 0x0041 /* 'A' */},                                                                           
        {0x0044 /* 'D' */, 0x0042 /* 'B' */},
        {0x0044 /* 'D' */, 0x0053 /* 'S' */, 0x0042 /* 'B' */},
        {0x0044 /* 'D' */, 0x0053 /* 'S' */, 0x0043 /* 'C' */},
        {0x00D0, /*0x0110,*/ 0x0041 /* 'A' */},
        {0x00D0, /*0x0110,*/ 0x0043 /* 'C' */},
        {0x0045 /* 'E' */, 0x004B /* 'K' */, 0x0053 /* 'S' */, 0x0054 /* 'T' */, 0x0052 /* 'R' */, 0x0041 /* 'A' */, 0x005F /* '_' */, 0x0041 /* 'A' */, 0x0052 /* 'R' */, 0x0042 /* 'B' */, 0x0045 /* 'E' */, 0x004A /* 'J' */, 0x0044 /* 'D' */, 0x0045 /* 'E' */},
        {0x0045 /* 'E' */, 0x004B /* 'K' */, 0x0053 /* 'S' */, 0x0054 /* 'T' */, 0x0052 /* 'R' */, 0x0041 /* 'A' */, 0x0042 /* 'B' */, 0x0055 /* 'U' */, 0x0044 /* 'D' */, 0},
        {0x0048 /* 'H' */, 0x00D8, 0x0053 /* 'S' */, 0x0054 /* 'T' */},  
        {0x0048 /* 'H' */, 0x0041 /* 'A' */, 0x0041 /* 'A' */, 0x0047 /* 'G' */},                                                                 
        {0x0048 /* 'H' */, 0x00C5, 0x004E /* 'N' */, 0x0044 /* 'D' */, 0x0042 /* 'B' */, 0x004F /* 'O' */, 0x0047 /* 'G' */},
        {0x0048 /* 'H' */, 0x0041 /* 'A' */, 0x0041 /* 'A' */, 0x004E /* 'N' */, 0x0044 /* 'D' */, 0x0056 /* 'V' */, 0x00C6, 0x0052 /* 'R' */, 0x004B /* 'K' */, 0x0053 /* 'S' */, 0x0042 /* 'B' */, 0x0041 /* 'A' */, 0x004E /* 'N' */, 0x004B /* 'K' */, 0x0045 /* 'E' */, 0x004E /* 'N' */},
        {0x006B /* 'k' */, 0x0061 /* 'a' */, 0x0072 /* 'r' */, 0x006C /* 'l' */},
        {0x004B /* 'K' */, 0x0061 /* 'a' */, 0x0072 /* 'r' */, 0x006C /* 'l' */},
        {0x004E /* 'N' */, 0x0049 /* 'I' */, 0x0045 /* 'E' */, 0x004C /* 'L' */, 0x0053 /* 'S' */, 0x0020 /* ' ' */, 0x004A /* 'J' */, 0x00D8, 0x0052 /* 'R' */, 0x0047 /* 'G' */, 0x0045 /* 'E' */, 0x004E /* 'N' */},
        {0x004E /* 'N' */, 0x0049 /* 'I' */, 0x0045 /* 'E' */, 0x004C /* 'L' */, 0x0053 /* 'S' */, 0x002D /* '-' */, 0x004A /* 'J' */, 0x00D8, 0x0052 /* 'R' */, 0x0047 /* 'G' */, 0x0045 /* 'E' */, 0x004E /* 'N' */},
        {0x004E /* 'N' */, 0x0049 /* 'I' */, 0x0045 /* 'E' */, 0x004C /* 'L' */, 0x0053 /* 'S' */, 0x0045 /* 'E' */, 0x004E /* 'N' */},
        {0x0052 /* 'R' */, 0x00C9, 0x0045 /* 'E' */, 0x002C /* ',' */, 0x0020 /* ' ' */, 0x0041 /* 'A' */},
        {0x0052 /* 'R' */, 0x0045 /* 'E' */, 0x0045 /* 'E' */, 0x002C /* ',' */, 0x0020 /* ' ' */, 0x0042 /* 'B' */},
        {0x0052 /* 'R' */, 0x00C9, 0x0045 /* 'E' */, 0x002C /* ',' */, 0x0020 /* ' ' */, 0x004C /* 'L' */},                                                    
        {0x0052 /* 'R' */, 0x0045 /* 'E' */, 0x0045 /* 'E' */, 0x002C /* ',' */, 0x0020 /* ' ' */, 0x0056 /* 'V' */},
        {0x0053 /* 'S' */, 0x0043 /* 'C' */, 0x0048 /* 'H' */, 0x0059 /* 'Y' */, 0x0054 /* 'T' */, 0x0054 /* 'T' */, 0x002C /* ',' */, 0x0020 /* ' ' */, 0x0042 /* 'B' */},
        {0x0053 /* 'S' */, 0x0043 /* 'C' */, 0x0048 /* 'H' */, 0x0059 /* 'Y' */, 0x0054 /* 'T' */, 0x0054 /* 'T' */, 0x002C /* ',' */, 0x0020 /* ' ' */, 0x0048 /* 'H' */},
        {0x0053 /* 'S' */, 0x0043 /* 'C' */, 0x0048 /* 'H' */, 0x00DC, 0x0054 /* 'T' */, 0x0054 /* 'T' */, 0x002C /* ',' */, 0x0020 /* ' ' */, 0x0048 /* 'H' */},
        {0x0053 /* 'S' */, 0x0043 /* 'C' */, 0x0048 /* 'H' */, 0x0059 /* 'Y' */, 0x0054 /* 'T' */, 0x0054 /* 'T' */, 0x002C /* ',' */, 0x0020 /* ' ' */, 0x004C /* 'L' */},
        {0x0053 /* 'S' */, 0x0043 /* 'C' */, 0x0048 /* 'H' */, 0x00DC, 0x0054 /* 'T' */, 0x0054 /* 'T' */, 0x002C /* ',' */, 0x0020 /* ' ' */, 0x004D /* 'M' */},
        {0x0053 /* 'S' */, 0x0053 /* 'S' */},
        {0x00DF},
        {0x0053 /* 'S' */, 0x0053 /* 'S' */, 0x0041 /* 'A' */},
        {0x0053 /* 'S' */, 0x0054 /* 'T' */, 0x004F /* 'O' */, 0x0052 /* 'R' */, 0x0045 /* 'E' */, 0x0020 /* ' ' */, 0x0056 /* 'V' */, 0x0049 /* 'I' */, 0x004C /* 'L' */, 0x0044 /* 'D' */, 0x004D /* 'M' */, 0x004F /* 'O' */, 0x0053 /* 'S' */, 0x0045 /* 'E' */},               
        {0x0053 /* 'S' */, 0x0054 /* 'T' */, 0x004F /* 'O' */, 0x0052 /* 'R' */, 0x0045 /* 'E' */, 0x004B /* 'K' */, 0x00C6, 0x0052 /* 'R' */, 0},
        {0x0053 /* 'S' */, 0x0054 /* 'T' */, 0x004F /* 'O' */, 0x0052 /* 'R' */, 0x004D /* 'M' */, 0x0020 /* ' ' */, 0x0050 /* 'P' */, 0x0045 /* 'E' */, 0x0054 /* 'T' */, 0x0045 /* 'E' */, 0x0052 /* 'R' */, 0x0053 /* 'S' */, 0x0045 /* 'E' */, 0x004E /* 'N' */},
        {0x0053 /* 'S' */, 0x0054 /* 'T' */, 0x004F /* 'O' */, 0x0052 /* 'R' */, 0x004D /* 'M' */, 0x004C /* 'L' */, 0x0059 /* 'Y' */},
        {0x0054 /* 'T' */, 0x0048 /* 'H' */, 0x004F /* 'O' */, 0x0052 /* 'R' */, 0x0056 /* 'V' */, 0x0041 /* 'A' */, 0x004C /* 'L' */, 0x0044 /* 'D' */},
        {0x0054 /* 'T' */, 0x0048 /* 'H' */, 0x004F /* 'O' */, 0x0052 /* 'R' */, 0x0056 /* 'V' */, 0x0041 /* 'A' */, 0x0052 /* 'R' */, 0x0044 /* 'D' */, 0x0055 /* 'U' */, 0x0052 /* 'R' */},
        {0x0054 /* 'T' */, 0x0048 /* 'H' */, 0x0059 /* 'Y' */, 0x0047 /* 'G' */, 0x0045 /* 'E' */, 0x0053 /* 'S' */, 0x0045 /* 'E' */, 0x004E /* 'N' */},
        {0x00FE, 0x004F /* 'O' */, 0x0052 /* 'R' */, 0x0056 /* 'V' */, 0x0041 /* 'A' */, 0x0052 /* 'R' */, 0x00D0, /*0x0110,*/ 0x0055 /* 'U' */, 0x0052 /* 'R' */},
        {0x0056 /* 'V' */, 0x0045 /* 'E' */, 0x0053 /* 'S' */, 0x0054 /* 'T' */, 0x0045 /* 'E' */, 0x0052 /* 'R' */, 0x0047 /* 'G' */, 0x00C5, 0x0052 /* 'R' */, 0x0044 /* 'D' */, 0x002C /* ',' */, 0x0020 /* ' ' */, 0x0041 /* 'A' */},
        {0x0056 /* 'V' */, 0x0045 /* 'E' */, 0x0053 /* 'S' */, 0x0054 /* 'T' */, 0x0045 /* 'E' */, 0x0052 /* 'R' */, 0x0047 /* 'G' */, 0x0041 /* 'A' */, 0x0041 /* 'A' */, 0x0052 /* 'R' */, 0x0044 /* 'D' */, 0x002C /* ',' */, 0x0020 /* ' ' */, 0x0041 /* 'A' */},
        {0x0056 /* 'V' */, 0x0045 /* 'E' */, 0x0053 /* 'S' */, 0x0054 /* 'T' */, 0x0045 /* 'E' */, 0x0052 /* 'R' */, 0x0047 /* 'G' */, 0x00C5, 0x0052 /* 'R' */, 0x0044 /* 'D' */, 0x002C /* ',' */, 0x0020 /* ' ' */, 0x0042 /* 'B' */},                 
        {0x00C6, 0x0042 /* 'B' */, 0x004C /* 'L' */, 0x0045 /* 'E' */},
        {0x00C4, 0x0042 /* 'B' */, 0x004C /* 'L' */, 0x0045 /* 'E' */},
        {0x00D8, 0x0042 /* 'B' */, 0x0045 /* 'E' */, 0x0052 /* 'R' */, 0x0047 /* 'G' */},
        {0x00D6, 0x0042 /* 'B' */, 0x0045 /* 'E' */, 0x0052 /* 'R' */, 0x0047 /* 'G' */}
    };

    private static char[][] testNTList = {
        {0x0061 /* 'a' */, 0x006E /* 'n' */, 0x0064 /* 'd' */, 0x0065 /* 'e' */, 0x0072 /* 'r' */, 0x0065 /* 'e' */},
        {0x0063 /* 'c' */, 0x0068 /* 'h' */, 0x0061 /* 'a' */, 0x0071 /* 'q' */, 0x0075 /* 'u' */, 0x0065 /* 'e' */},
        {0x0063 /* 'c' */, 0x0068 /* 'h' */, 0x0065 /* 'e' */, 0x006D /* 'm' */, 0x0069 /* 'i' */, 0x006E /* 'n' */},
        {0x0063 /* 'c' */, 0x006F /* 'o' */, 0x0074 /* 't' */, 0x0065 /* 'e' */},
        {0x0063 /* 'c' */, 0x006F /* 'o' */, 0x0074 /* 't' */, 0x00e9},
        {0x0063 /* 'c' */, 0x00f4, 0x0074 /* 't' */, 0x0065 /* 'e' */},
        {0x0063 /* 'c' */, 0x00f4, 0x0074 /* 't' */, 0x00e9},
        {0x010d, 0x0075 /* 'u' */, 0x010d, 0x0113, 0x0074 /* 't' */},
        {0x0043 /* 'C' */, 0x007A /* 'z' */, 0x0065 /* 'e' */, 0x0063 /* 'c' */, 0x0068 /* 'h' */},
        {0x0068 /* 'h' */, 0x0069 /* 'i' */, 0x0161, 0x0061 /* 'a' */},
        {0x0069 /* 'i' */, 0x0072 /* 'r' */, 0x0064 /* 'd' */, 0x0069 /* 'i' */, 0x0073 /* 's' */, 0x0063 /* 'c' */, 0x0068 /* 'h' */},
        {0x006C /* 'l' */, 0x0069 /* 'i' */, 0x0065 /* 'e' */},
        {0x006C /* 'l' */, 0x0069 /* 'i' */, 0x0072 /* 'r' */, 0x0065 /* 'e' */},
        {0x006C /* 'l' */, 0x006C /* 'l' */, 0x0061 /* 'a' */, 0x006D /* 'm' */, 0x0061 /* 'a' */},
        {0x006C /* 'l' */, 0x00f5, 0x0075 /* 'u' */, 0x0067 /* 'g' */},
        {0x006C /* 'l' */, 0x00f2, 0x007A /* 'z' */, 0x0061 /* 'a' */},
        {0x006C /* 'l' */, 0x0075 /* 'u' */, 0x010d},                                
        {0x006C /* 'l' */, 0x0075 /* 'u' */, 0x0063 /* 'c' */, 0x006B /* 'k' */},
        {0x004C /* 'L' */, 0x00fc, 0x0062 /* 'b' */, 0x0065 /* 'e' */, 0x0063 /* 'c' */, 0x006B /* 'k' */},
        {0x006C /* 'l' */, 0x0079 /* 'y' */, 0x0065 /* 'e' */},                               
        {0x006C /* 'l' */, 0x00e4, 0x0076 /* 'v' */, 0x0069 /* 'i' */},
        {0x004C /* 'L' */, 0x00f6, 0x0077 /* 'w' */, 0x0065 /* 'e' */, 0x006E /* 'n' */},
        {0x006D /* 'm' */, 0x00e0, 0x0161, 0x0074 /* 't' */, 0x0061 /* 'a' */},
        {0x006D /* 'm' */, 0x00ee, 0x0072 /* 'r' */},
        {0x006D /* 'm' */, 0x0079 /* 'y' */, 0x006E /* 'n' */, 0x0064 /* 'd' */, 0x0069 /* 'i' */, 0x0067 /* 'g' */},
        {0x004D /* 'M' */, 0x00e4, 0x006E /* 'n' */, 0x006E /* 'n' */, 0x0065 /* 'e' */, 0x0072 /* 'r' */},
        {0x006D /* 'm' */, 0x00f6, 0x0063 /* 'c' */, 0x0068 /* 'h' */, 0x0074 /* 't' */, 0x0065 /* 'e' */, 0x006E /* 'n' */},
        {0x0070 /* 'p' */, 0x0069 /* 'i' */, 0x00f1, 0x0061 /* 'a' */},
        {0x0070 /* 'p' */, 0x0069 /* 'i' */, 0x006E /* 'n' */, 0x0074 /* 't' */},
        {0x0070 /* 'p' */, 0x0079 /* 'y' */, 0x006C /* 'l' */, 0x006F /* 'o' */, 0x006E /* 'n' */},
        {0x0161, 0x00e0, 0x0072 /* 'r' */, 0x0061 /* 'a' */, 0x006E /* 'n' */},
        {0x0073 /* 's' */, 0x0061 /* 'a' */, 0x0076 /* 'v' */, 0x006F /* 'o' */, 0x0069 /* 'i' */, 0x0072 /* 'r' */},
        {0x0160, 0x0065 /* 'e' */, 0x0072 /* 'r' */, 0x0062 /* 'b' */, 0x016b, 0x0072 /* 'r' */, 0x0061 /* 'a' */},
        {0x0053 /* 'S' */, 0x0069 /* 'i' */, 0x0065 /* 'e' */, 0x0074 /* 't' */, 0x006C /* 'l' */, 0x0061 /* 'a' */},
        {0x015b, 0x006C /* 'l' */, 0x0075 /* 'u' */, 0x0062 /* 'b' */},
        {0x0073 /* 's' */, 0x0075 /* 'u' */, 0x0062 /* 'b' */, 0x0074 /* 't' */, 0x006C /* 'l' */, 0x0065 /* 'e' */},
        {0x0073 /* 's' */, 0x0079 /* 'y' */, 0x006D /* 'm' */, 0x0062 /* 'b' */, 0x006F /* 'o' */, 0x006C /* 'l' */},
        {0x0073 /* 's' */, 0x00e4, 0x006D /* 'm' */, 0x0074 /* 't' */, 0x006C /* 'l' */, 0x0069 /* 'i' */, 0x0063 /* 'c' */, 0x0068 /* 'h' */},
        {0x0077 /* 'w' */, 0x0061 /* 'a' */, 0x0066 /* 'f' */, 0x0066 /* 'f' */, 0x006C /* 'l' */, 0x0065 /* 'e' */},
        {0x0076 /* 'v' */, 0x0065 /* 'e' */, 0x0072 /* 'r' */, 0x006B /* 'k' */, 0x0065 /* 'e' */, 0x0068 /* 'h' */, 0x0072 /* 'r' */, 0x0074 /* 't' */},
        {0x0077 /* 'w' */, 0x006F /* 'o' */, 0x006F /* 'o' */, 0x0064 /* 'd' */},
        {0x0076 /* 'v' */, 0x006F /* 'o' */, 0x0078 /* 'x' */},                                 
        {0x0076 /* 'v' */, 0x00e4, 0x0067 /* 'g' */, 0x0061 /* 'a' */},
        {0x0079 /* 'y' */, 0x0065 /* 'e' */, 0x006E /* 'n' */},
        {0x0079 /* 'y' */, 0x0075 /* 'u' */, 0x0061 /* 'a' */, 0x006E /* 'n' */},
        {0x0079 /* 'y' */, 0x0075 /* 'u' */, 0x0063 /* 'c' */, 0x0063 /* 'c' */, 0x0061 /* 'a' */},
        {0x017e, 0x0061 /* 'a' */, 0x006C /* 'l' */},
        {0x017e, 0x0065 /* 'e' */, 0x006E /* 'n' */, 0x0061 /* 'a' */},
        {0x017d, 0x0065 /* 'e' */, 0x006E /* 'n' */, 0x0113, 0x0076 /* 'v' */, 0x0061 /* 'a' */},
        {0x007A /* 'z' */, 0x006F /* 'o' */, 0x006F /* 'o' */, 0},
        {0x005A /* 'Z' */, 0x0076 /* 'v' */, 0x0069 /* 'i' */, 0x0065 /* 'e' */, 0x0064 /* 'd' */, 0x0072 /* 'r' */, 0x0069 /* 'i' */, 0x006A /* 'j' */, 0x0061 /* 'a' */},
        {0x005A /* 'Z' */, 0x00fc, 0x0072 /* 'r' */, 0x0069 /* 'i' */, 0x0063 /* 'c' */, 0x0068 /* 'h' */},
        {0x007A /* 'z' */, 0x0079 /* 'y' */, 0x0073 /* 's' */, 0x006B /* 'k' */, 0},             
        {0x00e4, 0x006E /* 'n' */, 0x0064 /* 'd' */, 0x0065 /* 'e' */, 0x0072 /* 'r' */, 0x0065 /* 'e' */}                  
    };
    
    private Collator myCollation = null;
    
    public CollationDanishTest() {
        try {
            myCollation = Collator.getInstance(new Locale("da", "DK"));
        } catch (Exception e) {
            errln("ERROR: in creation of collator of DANISH locale");
            return;
        }
    }
    
    public void TestTertiary() {
        int i = 0;
        myCollation.setStrength(Collator.TERTIARY);
        
        for (i = 0; i < 5 ; i++) {
            doTest(testSourceCases[i], testTargetCases[i], results[i]);
        }
        int j = 0;
        logln("Test internet data list : ");
        for (i = 0; i < 53; i++) {
            for (j = i+1; j < 54; j++) {
                doTest(testBugs[i], testBugs[j], -1);
            }
        }
        logln("Test NT data list : ");
        for (i = 0; i < 52; i++) {
            for (j = i+1; j < 53; j++) {
            	doTest(testNTList[i], testNTList[j], -1);
            }
        }
    }

    public void TestPrimary() {
        int i;
        myCollation.setStrength(Collator.PRIMARY);
        for (i = 5; i < 8; i++) {
            doTest(testSourceCases[i], testTargetCases[i], results[i]);
        }
    }

    
    
    // main test routine, Tests rules specific to danish collation
    private void doTest(char[] source, char[] target, int result) {
        String s = new String(source);
        String t = new String(target);
        int compareResult = myCollation.compare(s, t);
        CollationKey sortKey1, sortKey2;
        sortKey1 = myCollation.getCollationKey(s);
        sortKey2 = myCollation.getCollationKey(t);
        int keyResult = sortKey1.compareTo(sortKey2);
        reportCResult(s, t, sortKey1, sortKey2, compareResult, keyResult, compareResult, result);   
    }
    
    private void reportCResult( String source, String target, CollationKey sourceKey, CollationKey targetKey,
                                int compareResult, int keyResult, int incResult, int expectedResult ){
        if (expectedResult < -1 || expectedResult > 1) {
            errln("***** invalid call to reportCResult ****");
            return;
        }

        boolean ok1 = (compareResult == expectedResult);
        boolean ok2 = (keyResult == expectedResult);
        boolean ok3 = (incResult == expectedResult);

        if (ok1 && ok2 && ok3 && !isVerbose()) {
            return;    
        } else {
            String msg1 = ok1? "Ok: compare(\"" : "FAIL: compare(\"";
            String msg2 = "\", \"";
            String msg3 = "\") returned ";
            String msg4 = "; expected ";
            
            String sExpect = new String("");
            String sResult = new String("");
            sResult = appendCompareResult(compareResult, sResult);
            sExpect = appendCompareResult(expectedResult, sExpect);
            if (ok1) {
                logln(msg1 + source + msg2 + target + msg3 + sResult);
            } else {
                errln(msg1 + source + msg2 + target + msg3 + sResult + msg4 + sExpect);
            }
            
            msg1 = ok2 ? "Ok: key(\"" : "FAIL: key(\"";
            msg2 = "\").compareTo(key(\"";
            msg3 = "\")) returned ";
            sResult = appendCompareResult(keyResult, sResult);
            if (ok2) {
                logln(msg1 + source + msg2 + target + msg3 + sResult);
            } else {
                errln(msg1 + source + msg2 + target + msg3 + sResult + msg4 + sExpect);
                msg1 = "  ";
                msg2 = " vs. ";
                errln(msg1 + prettify(sourceKey) + msg2 + prettify(targetKey));
            }
            
            msg1 = ok3 ? "Ok: incCompare(\"" : "FAIL: incCompare(\"";
            msg2 = "\", \"";
            msg3 = "\") returned ";

            sResult = appendCompareResult(incResult, sResult);

            if (ok3) {
                logln(msg1 + source + msg2 + target + msg3 + sResult);
            } else {
                errln(msg1 + source + msg2 + target + msg3 + sResult + msg4 + sExpect);
            }                
        }
    }
    
    private String appendCompareResult(int result, String target){
        if (result == -1) {
            target += "LESS";
        } else if (result == 0) {
            target += "EQUAL";
        } else if (result == 1) {
            target += "GREATER";
        } else {
            String huh = "?";
            target += huh + result;
        }
        return target;
    }
    
    String prettify(CollationKey sourceKey) {
        int i;
        byte[] bytes= sourceKey.toByteArray();
        String target = "[";
        for (i = 0; i < bytes.length; i++) {
            target += Integer.toHexString(bytes[i]);
            target += " ";
        }
        target += "]";
        return target;
    }
}
    