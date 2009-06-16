/*
 *******************************************************************************
 * Copyright (C) 1998-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 09, 2003
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import com.ibm.icu.impl.Utility;
import java.io.PrintStream;

public class ThaiCharacterClasses
{
    public static final int NON    =  0;
    public static final int CON    =  1;
    public static final int COA    =  2;
    public static final int COD    =  3;
    public static final int LVO =  4;
    public static final int FV1    =  5;
    public static final int FV2    =  6;
    public static final int FV3    =  7;
    public static final int BV1    =  8;
    public static final int BV2    =  9;
    public static final int BDI = 10;
    public static final int TON    = 11;
    public static final int AD1    = 12;
    public static final int AD2    = 13;
    public static final int AD3    = 14;
    public static final int NIK    = 15;
    public static final int AV1    = 16;
    public static final int AV2    = 17;
    public static final int AV3    = 18;
    public static final int cCount = 19;

    // Indexed by unicode - '\u0E00'
    // FIXME: MS Fonts - Should 0E2E has no ascender (it does in WT font)
    // FIXME: MS Fonts - 0E47 (MAITAIKHU) and 0E4D (NIKHAHIT) only have vowel forms
    // FIXME: MS Fonts - 0E4E (YAMAKKAN) only has one form
    private static final int classTable[] = {
    //       0    1    2    3    4    5    6    7    8    9    A    B    C    D    E    F
    //       -------------------------------------------------------------------------------
    /*0E00*/ NON, CON, CON, CON, CON, CON, CON, CON, CON, CON, CON, CON, CON, COD, COD, COD, 
    /*0E10*/ COD, CON, CON, CON, CON, CON, CON, CON, CON, CON, CON, COA, CON, COA, CON, COA, 
    /*0E20*/ CON, CON, CON, CON, FV3, CON, FV3, CON, CON, CON, CON, CON, CON, CON, CON, NON, 
    /*0E30*/ FV1, AV2, FV1, FV1, AV1, AV3, AV2, AV3, BV1, BV2, BDI, NON, NON, NON, NON, NON, 
    /*0E40*/ LVO, LVO, LVO, LVO, LVO, FV2, NON, AD2, TON, TON, TON, TON, AD1, NIK, AD3, NON, 
    /*0E50*/ NON, NON, NON, NON, NON, NON, NON, NON, NON, NON, NON, NON
    };

    private static String[] classNames =
    {
        "NON",
        "CON",
        "COA",
        "COD",
        "LVO",
        "FV1",
        "FV2",
        "FV3",
        "BV1",
        "BV2",
        "BDI",
        "TON",
        "AD1",
        "AD2",
        "AD3",
        "NIK",
        "AV1",
        "AV2",
        "AV3"
    };

    private static final char pairTable[][] = {
      //------------------------------------------------------------------------------------------------
      //  N    C    C    C    L    F    F    F    B    B    B    T    A    A    A    N    A    A    A
      //  O    O    O    O    V    V    V    V    V    V    D    O    D    D    D    I    V    V    V
      //  N    N    A    D    O    1    2    3    1    2    I    N    1    2    3    K    1    2    3
      //------------------------------------------------------------------------------------------------
/*NON*/ {'A', 'A', 'A', 'A', 'A', 'S', 'S', 'A', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R'},
/*CON*/ {'A', 'A', 'A', 'A', 'A', 'A', 'S', 'A', 'C', 'C', 'C', 'E', 'E', 'E', 'C', 'E', 'C', 'C', 'C'},
/*COA*/ {'A', 'A', 'A', 'A', 'A', 'A', 'S', 'A', 'C', 'C', 'C', 'F', 'F', 'F', 'D', 'F', 'D', 'D', 'D'},
/*COD*/ {'A', 'A', 'A', 'A', 'A', 'A', 'S', 'A', 'H', 'H', 'H', 'E', 'E', 'E', 'C', 'E', 'C', 'C', 'C'},
/*LVO*/ {'S', 'A', 'A', 'A', 'S', 'S', 'S', 'S', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R'},
/*FV1*/ {'S', 'A', 'A', 'A', 'A', 'S', 'S', 'A', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R'},
/*FV2*/ {'A', 'A', 'A', 'A', 'A', 'A', 'S', 'A', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R'},
/*FV3*/ {'A', 'A', 'A', 'A', 'A', 'S', 'A', 'S', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R'},
/*BV1*/ {'A', 'A', 'A', 'A', 'A', 'S', 'S', 'A', 'R', 'R', 'R', 'E', 'E', 'R', 'R', 'E', 'R', 'R', 'R'},
/*BV2*/ {'A', 'A', 'A', 'A', 'A', 'S', 'S', 'A', 'R', 'R', 'R', 'E', 'R', 'R', 'R', 'R', 'R', 'R', 'R'},
/*BDI*/ {'A', 'A', 'A', 'A', 'A', 'S', 'S', 'A', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R'},
/*TON*/ {'A', 'A', 'A', 'A', 'A', 'I', 'A', 'A', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R'},
/*AD1*/ {'A', 'A', 'A', 'A', 'A', 'S', 'S', 'A', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R'},
/*AD2*/ {'A', 'A', 'A', 'A', 'A', 'S', 'S', 'A', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R'},
/*AD3*/ {'A', 'A', 'A', 'A', 'A', 'S', 'S', 'A', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R'},
/*NIK*/ {'A', 'A', 'A', 'A', 'A', 'S', 'S', 'A', 'R', 'R', 'R', 'G', 'G', 'R', 'R', 'R', 'R', 'R', 'R'},
/*AV1*/ {'A', 'A', 'A', 'A', 'A', 'S', 'S', 'A', 'R', 'R', 'R', 'G', 'G', 'R', 'R', 'G', 'R', 'R', 'R'},
/*AV2*/ {'A', 'A', 'A', 'A', 'A', 'S', 'S', 'A', 'R', 'R', 'R', 'G', 'R', 'R', 'R', 'R', 'R', 'R', 'R'},
/*AV3*/ {'A', 'A', 'A', 'A', 'A', 'S', 'S', 'A', 'R', 'R', 'R', 'G', 'R', 'G', 'R', 'R', 'R', 'R', 'R'}
    };

    public static int getCharClass(char ch)
    {
        int charClass = NON;
        
        if (ch >= '\u0E00' && ch <= '\u0E5B') {
            charClass = classTable[ch - '\u0E00'];
        }
        
        return charClass;
    }
    
    public static String getClassName(int classID)
    {
        if (classID < 0 || classID >= cCount) {
            return "***";
        }
        
        return classNames[classID];
    }
    
    public static char getPairAction(int prevClass, int currClass)
    {
        if (prevClass < 0 || prevClass >= cCount |
            currClass < 0 || currClass >= cCount) {
            return 'A';
        }
            
        return pairTable[prevClass][currClass];
    }
    
    private static String classTableHeader =
"const le_uint8 ThaiShaping::classTable[] = {\n" +
"    //       0    1    2    3    4    5    6    7    8    9    A    B    C    D    E    F\n" +
"    //       -------------------------------------------------------------------------------";
    
    public static void writeClassTable(PrintStream output)
    {
        System.out.print("Writing class table...");
        
        output.print(classTableHeader);
        
        for (char ch = '\u0E00'; ch <= '\u0E5B'; ch += 1) {
            int charClass = getCharClass(ch);
            
            if ((ch & 0x000F) == 0) {
                output.print("\n    /*" + Utility.hex(ch, 4) + "*/ ");
            }
            
            output.print(getClassName(charClass));
            
            if (ch < '\u0E5B') {
                output.print(", ");
            } else {
                output.print("\n};\n\n");
            }
        }
        
        System.out.println(" done.");
    }
    
}
