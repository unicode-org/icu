/*
 *******************************************************************************
 *   Copyright (C) 2002, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/USerializedSet.java,v $ 
 * $Date: 2002/03/12 17:49:15 $ 
 * $Revision: 1.1 $
 *
 *****************************************************************************************
*/

package com.ibm.icu.impl;
/**
 * @version 	1.0
 * @author     Markus W. Scherer
 */

/**
 * Simple class for handling serialized USet/UnicodeSet structures
 * without object creation. See ICU4C icu/source/common/uset.c.
 *
 * @internal
 */
public final class USerializedSet {
	
    public final boolean getSet(char src[], int srcStart) {
        // leave most argument checking up to Java exceptions
        array=null;
        arrayOffset=bmpLength=length=0;

        length=src[srcStart++];
        if((length&0x8000) >0) {
            /* there are supplementary values */
            length&=0x7fff;
            if(src.length<(srcStart+1+length)) {
                length=0;
               throw new IndexOutOfBoundsException();
            }
            bmpLength=src[srcStart++];
        } else {
            /* only BMP values */
            if(src.length<(srcStart+length)) {
                length=0;
                throw new IndexOutOfBoundsException();
            }
            bmpLength=length;
        }
        array=src;
        arrayOffset=srcStart;
        return true;
    }

    public final  boolean  contains(int c) {
        if(c<0 || 0x10ffff<c) {
            return false;
        }

        if(c<=0xffff) {
            /* find c in the BMP part */
            int i, bmpLimit=arrayOffset+bmpLength;
            for(i=arrayOffset; i<bmpLimit && c>=array[i]; ++i) {}
            return (((i-arrayOffset)&1)>0);
        } else {
            /* find c in the supplementary part */
            int i, limit=arrayOffset+length;
            char high=(char)(c>>16), low=(char)c;
            for(i=arrayOffset+bmpLength;
                i<limit && (high>array[i] || (high==array[i] && low>=array[i+1]));
                i+=2) {}

            /* count pairs of 16-bit units even per BMP and check if the number of pairs is odd */
            return ((i+bmpLength-arrayOffset)&2)!=0;
        }
    }

    public final  boolean countRanges() {
        return ((bmpLength+(length-bmpLength)/2+1)/2)>0;
    }

    public final  boolean getRange(int rangeIndex, int range[]) {
        if(rangeIndex<0) {
            return false;
        }

        rangeIndex*=2; /* address start/limit pairs */
        if(rangeIndex<bmpLength) {
            range[0]=array[arrayOffset+rangeIndex++];
            if(rangeIndex<bmpLength) {
                range[1]=array[arrayOffset+rangeIndex];
            } else if(rangeIndex<length) {
                range[1]=(((int)array[arrayOffset+rangeIndex])<<16)|array[arrayOffset+rangeIndex+1];
            } else {
                range[1]=0x110000;
            }
            return true;
        } else {
            rangeIndex-=bmpLength;
            rangeIndex*=2; /* address pairs of pairs of units */
            int suppLength=length-bmpLength;
            if(rangeIndex<suppLength) {
                int offset=arrayOffset+bmpLength;
                range[0]=(((int)array[offset+rangeIndex])<<16)|array[offset+rangeIndex+1];
                rangeIndex+=2;
                if(rangeIndex<suppLength) {
                range[1]=(((int)array[offset+rangeIndex])<<16)|array[offset+rangeIndex+1];
                } else {
                    range[1]=0x110000;
                }
                return true;
            } else {
                return false;
            }
        }
    }

    private char array[];
    private int arrayOffset, bmpLength, length;
}