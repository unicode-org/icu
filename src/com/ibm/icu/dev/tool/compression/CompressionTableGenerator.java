/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.compression;


/**
 * Utility class to generate the tables used by the SCSU interface and
 * the UnicodeCompressor class.
 *
 * @author Stephen F. Booth
 * @version 1.0 08 Mar 99
 */
class CompressionTableGenerator
{
    // duplicate of constants in SCSU

    final static int LATININDEX                  = 0xF9;
    final static int IPAEXTENSIONINDEX           = 0xFA;
    final static int GREEKINDEX                  = 0xFB;
    final static int ARMENIANINDEX               = 0xFC;
    final static int HIRAGANAINDEX               = 0xFD;
    final static int KATAKANAINDEX               = 0xFE;
    final static int HALFWIDTHKATAKANAINDEX      = 0xFF;

    final static int SDEFINEX                    = 0x0B;
    final static int SRESERVED                   = 0x0C;  // reserved value
    final static int SQUOTEU                     = 0x0E;
    final static int SCHANGEU                    = 0x0F;

    final static int SQUOTE0                     = 0x01;
    final static int SQUOTE1                     = 0x02;
    final static int SQUOTE2                     = 0x03;
    final static int SQUOTE3                     = 0x04;
    final static int SQUOTE4                     = 0x05;
    final static int SQUOTE5                     = 0x06;
    final static int SQUOTE6                     = 0x07;
    final static int SQUOTE7                     = 0x08;

    final static int SCHANGE0                    = 0x10;
    final static int SCHANGE1                    = 0x11;
    final static int SCHANGE2                    = 0x12;
    final static int SCHANGE3                    = 0x13;
    final static int SCHANGE4                    = 0x14;
    final static int SCHANGE5                    = 0x15;
    final static int SCHANGE6                    = 0x16;
    final static int SCHANGE7                    = 0x17;

    final static int SDEFINE0                    = 0x18;
    final static int SDEFINE1                    = 0x19;
    final static int SDEFINE2                    = 0x1A;
    final static int SDEFINE3                    = 0x1B;
    final static int SDEFINE4                    = 0x1C;
    final static int SDEFINE5                    = 0x1D;
    final static int SDEFINE6                    = 0x1E;
    final static int SDEFINE7                    = 0x1F;

    //==========================
    // Unicode mode tags
    //==========================
    final static int UCHANGE0                    = 0xE0;
    final static int UCHANGE1                    = 0xE1;
    final static int UCHANGE2                    = 0xE2;
    final static int UCHANGE3                    = 0xE3;
    final static int UCHANGE4                    = 0xE4;
    final static int UCHANGE5                    = 0xE5;
    final static int UCHANGE6                    = 0xE6;
    final static int UCHANGE7                    = 0xE7;

    final static int UDEFINE0                    = 0xE8;
    final static int UDEFINE1                    = 0xE9;
    final static int UDEFINE2                    = 0xEA;
    final static int UDEFINE3                    = 0xEB;
    final static int UDEFINE4                    = 0xEC;
    final static int UDEFINE5                    = 0xED;
    final static int UDEFINE6                    = 0xEE;
    final static int UDEFINE7                    = 0xEF;

    final static int UQUOTEU                     = 0xF0;
    final static int UDEFINEX                    = 0xF1;
    final static int URESERVED                   = 0xF2;  // reserved value

    final static int BLOCKSIZE = 0xFF;
    
    /**
     * Generate the table used as sOffsetTable in SCSU.
     * This table contains preformed indices so we can do array lookups 
     * instead of calculations for speed during decompression.
     */
    static void printOffsetTable()
    {
        int     i           = 0;
    int []    offsetTable = new int [ BLOCKSIZE + 1 ];

        // 0x00 is reserved

        // half blocks from U+0080 to U+3380
        for( i = 0x01; i < 0x68; i++ )
            offsetTable[i] = i * 0x80;
    
        // half blocks from U+E000 to U+FF80
        for( i = 0x68; i < 0xA8; i++ )
            offsetTable[i] = (i * 0x80) + 0xAC00;
    
        // 0xA8..0xF8 is reserved
 
        offsetTable[ LATININDEX ] = 0x00C0;
        offsetTable[ IPAEXTENSIONINDEX ] = 0x0250;
        offsetTable[ GREEKINDEX ] = 0x0370;
        offsetTable[ ARMENIANINDEX ] = 0x0530;
        offsetTable[ HIRAGANAINDEX ] = 0x3040;
        offsetTable[ KATAKANAINDEX ] = 0x30A0;
        offsetTable[ HALFWIDTHKATAKANAINDEX ] = 0xFF60;

        // dump the generated table
    System.out.println("static int [] sOffsetTable = {");
        for(i = 0; i < offsetTable.length - 1; i++)
            System.out.print("0x" + Integer.toHexString(offsetTable[i])
                 + ", ");
        for(i = offsetTable.length - 1; i < offsetTable.length; i++)
            System.out.print("0x" + Integer.toHexString(offsetTable[i]));
        System.out.println();
        System.out.println("};");
    }
    
    /**
     * Generate the table used as sSingleTagTable in UnicodeCompressor.
     * This table contains boolean values indicating if a byte is a
     * single-byte mode tag.
     */
    static void printSingleTagTable()
    {
        int        i              = 0;
    boolean [] singleTagTable = new boolean  [ BLOCKSIZE + 1 ];

        for( i = 0x00; i <= BLOCKSIZE; i++ ) {
            switch( i ) {
        
        case SQUOTEU:  case SCHANGEU: 
        case SDEFINEX: case SRESERVED:
        case SQUOTE0:  case SQUOTE1:  
        case SQUOTE2:  case SQUOTE3:
        case SQUOTE4:  case SQUOTE5:  
        case SQUOTE6:  case SQUOTE7:
        case SCHANGE0: case SCHANGE1: 
        case SCHANGE2: case SCHANGE3:
        case SCHANGE4: case SCHANGE5: 
        case SCHANGE6: case SCHANGE7:
        case SDEFINE0: case SDEFINE1: 
        case SDEFINE2: case SDEFINE3:
        case SDEFINE4: case SDEFINE5: 
        case SDEFINE6: case SDEFINE7:
        singleTagTable[i] = true;
                break;
        
        default:
        singleTagTable[i] = false;
                break;
            }
        }
    
        // dump the generated table
        System.out.println("private static boolean [] sSingleTagTable = {");
        for(i = 0; i < singleTagTable.length - 1; i++)
            System.out.print(singleTagTable[i] + ", ");
        for(i = singleTagTable.length - 1; i < singleTagTable.length; i++)
            System.out.print(singleTagTable[i]);
        System.out.println();
        System.out.println("};");
    }
    
    
    /**
     * Generate the table used as sUnicodeTagTable in 
     * This table contains boolean values indicating if a byte is a
     * unicode mode tag.
     */
    static void printUnicodeTagTable()
    {
        int        i               = 0;
    boolean [] unicodeTagTable = new boolean  [ BLOCKSIZE + 1 ];

        for( i = 0x00; i <= BLOCKSIZE; i++ ) {
            switch( i ) {
        case UQUOTEU:  case UDEFINEX: 
        case URESERVED:
        case UCHANGE0: case UCHANGE1: 
        case UCHANGE2: case UCHANGE3:
        case UCHANGE4: case UCHANGE5: 
        case UCHANGE6: case UCHANGE7:
        case UDEFINE0: case UDEFINE1: 
        case UDEFINE2: case UDEFINE3:
        case UDEFINE4: case UDEFINE5: 
        case UDEFINE6: case UDEFINE7:
        unicodeTagTable[i] = true;
                break;
        
        default:
        unicodeTagTable[i] = false;
                break;
            }
        }
    
        // dump the generated table
        System.out.println("private static boolean [] sUnicodeTagTable = {");
        for(i = 0; i < unicodeTagTable.length - 1; i++)
            System.out.print(unicodeTagTable[i] + ", ");
        for(i = unicodeTagTable.length - 1; i < unicodeTagTable.length; i++)
            System.out.print(unicodeTagTable[i]);
        System.out.println();
        System.out.println("};");
    }
    
    public static void main(String[] argv)
    {
        printOffsetTable();
        printSingleTagTable();
        printUnicodeTagTable();
    }
};
