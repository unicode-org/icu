/*
 *******************************************************************************
 *   Copyright (C) 2002, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/USerializedSet.java,v $ 
 * $Date: 2002/06/20 01:18:09 $ 
 * $Revision: 1.3 $
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
        array = new char[length];
        System.arraycopy(src,srcStart,array,0,length);
        //arrayOffset=srcStart;
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

		range=new int[2];
		
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
	public final void setSerializedToOne(int c) {
	    if( 0x10ffff<c) {
	        return;
	    }

	    if(c<0xffff) {
	        bmpLength=length=2;
	        array[0]=(char)c;
	        array[1]=(char)(c+1);
	    } else if(c==0xffff) {
	        bmpLength=1;
	        length=3;
	        array[0]=0xffff;
	        array[1]=1;
	        array[2]=0;
	    } else if(c<0x10ffff) {
	        bmpLength=0;
	        length=4;
	        array[0]=(char)(c>>16);
	        array[1]=(char)c;
	        ++c;
	        array[2]=(char)(c>>16);
	        array[3]=(char)c;
	    } else /* c==0x10ffff */ {
	        bmpLength=0;
	        length=2;
	        array[0]=0x10;
	        array[1]=0xffff;
	    }
	}
	
	
	public final boolean getSerializedRange( int rangeIndex,int[] range) {
	    if( rangeIndex<0) {
	        return false;
	    }
	    if(array==null){
			array = new char[8];
		}
        if(range==null || range.length <2){
            throw new IllegalArgumentException();
        }
        rangeIndex*=2; /* address start/limit pairs */
	    if(rangeIndex<bmpLength) {
	        range[0]=array[rangeIndex++];
	        if(rangeIndex<bmpLength) {
	            range[1]=array[rangeIndex];
	        } else if(rangeIndex<length) {
	            range[1]=(((int)array[rangeIndex])<<16)|array[rangeIndex+1];
	        } else {
	            range[1]=0x110000;
	        }
            range[1]-=1;
	        return true;
	    } else {
	        rangeIndex-=bmpLength;
	        rangeIndex*=2; /* address pairs of pairs of units */
	        length-=bmpLength;
	        if(rangeIndex<length) {
	            int offset=arrayOffset+bmpLength;
	            range[0]=(((int)array[offset+rangeIndex])<<16)|array[offset+rangeIndex+1];
	            rangeIndex+=2;
	            if(rangeIndex<length) {
	                range[1]=(((int)array[offset+rangeIndex])<<16)|array[offset+rangeIndex+1];
	            } else {
	                range[1]=0x110000;
	            }
                range[1]-=1;
	            return true;
	        } else {
	            return false;
	        }
	    }
	}
	public final boolean serializedContains(int c) {
	
	    if(c>0x10ffff) {
	        return false;
	    }
	    
	    if(c<=0xffff) {
	    	int i;
	        /* find c in the BMP part */
	        for(i=0; i<bmpLength && (char)c>=array[i]; ++i) {}
	        return (boolean)((i&1) != 0);
	    } else {
	    	int i;
	        /* find c in the supplementary part */
	        char high=(char)(c>>16), low=(char)c;
	        for(i=bmpLength;
	            i<length && (high>array[i] || (high==array[i] && low>=array[i+1]));
	            i+=2) {}
	
	        /* count pairs of 16-bit units even per BMP and check if the number of pairs is odd */
	        return (boolean)(((i+bmpLength)&2)!=0);
	    }
	}
	
	public final int countSerializedRanges() {
	    return (bmpLength+(length-bmpLength)/2+1)/2;
	}

    private char array[] = new char[8];
    private int arrayOffset, bmpLength, length;
}