/*
******************************************************************************
* Copyright (C) 2003-2010, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

package com.ibm.icu.dev.tool.localeconverter;

import java.math.BigInteger;

/*
 *  The code is from  http://www.theorem.com/java/CRC32.java
 * Calculates the CRC32 - 32 bit Cyclical Redundancy Check
 * <P> This check is used in numerous systems to verify the integrity
 * of information.  It's also used as a hashing function.  Unlike a regular
 * checksum, it's sensitive to the order of the characters.
 * It produces a 32 bit
 * 
 * @author Michael Lecuyer (mjl@theorem.com)
 * @version 1.1 August 11, 1998
 */
 
/* ICU is not endian portable, because ICU data generated on big endian machines can be
 * ported to big endian machines but not to little endian machines and vice versa. The 
 * conversion is not portable across platforms with different endianess. 
 */
 
public class CalculateCRC32 {
    static int CRCTable[];
    static int cachedCRC;

    static void buildCRCTable() {
        final int CRC32_POLYNOMIAL = 0xEDB88320;
        int i, j;
        int crc;
        CRCTable = new int[256];

        for (i = 0; i <= 255; i++) {
            crc = i;
            for (j = 8; j > 0; j--) {
                if ((crc & 1) == 1) {
                    crc = (crc >>> 1) ^ CRC32_POLYNOMIAL;
                } else {
                    crc >>>= 1;
                }
            }
            CRCTable[i] = crc;
        }
    }

    public static int computeCRC32(String buffer) {
        return computeCRC32(buffer, 0xFFFFFFFF);
    }

    public static int computeCRC32(byte buffer[]) {
        return computeCRC32(buffer, 0xFFFFFFFF);
    }
   
    public static int computeCRC32(String buffer, int crc){
        return computeCRC32(buffer.getBytes(), crc);
    }

    public static int computeCRC32(byte buffer[], int crc) {
        return computeCRC32(buffer, 0, buffer.length, crc);
    }

    public static int computeCRC32(byte buffer[], int start, int count, int lastcrc){
        buildCRCTable();  
        int temp1, temp2;
        int i = start;
        cachedCRC = lastcrc;
      
        while (count-- != 0){
            temp1 = cachedCRC >>> 8;
            byte s = buffer[i++];
            temp2 = CRCTable[(cachedCRC ^s) & 0xFF];
            cachedCRC = temp1 ^ temp2;
        }
        return cachedCRC;
    }

    public byte [] toBytes() {
        return new BigInteger(new Integer(cachedCRC).toString()).toByteArray();
    }
}
