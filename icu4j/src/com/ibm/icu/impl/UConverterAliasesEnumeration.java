/**
*******************************************************************************
* Copyright (C) 2006, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/ 
package com.ibm.icu.impl;

import java.util.Enumeration;


/**
 * Enumeration for Converter Aliases
 */

public class UConverterAliasesEnumeration implements Enumeration {

	private UAliasContext context;
	
	/* Set alias context
	 */	
	public void setContext(UAliasContext context){
		this.context = context;		
	}
	
    public int count() {
		int value = 0;
	    
	    if (context.listOffset!=0) {
	        value = UConverterAlias.gTaggedAliasLists[(int)context.listOffset];
	    }
	    return value;
	}

	public Object nextElement() {
	
		if (context.listOffset!=0) {
	        long listCount = UConverterAlias.gTaggedAliasLists[(int)context.listOffset];
	        int[] currListArray = UConverterAlias.gTaggedAliasLists;
	        long currListArrayIndex = context.getListOffset() + 1; 

	        if (context.getListIdx() < listCount) {
	            String str = UConverterAlias.GET_STRING(currListArray[(int)(context.listIdx+currListArrayIndex)]);
	            context.listIdx++;
	            return str;
	        }
	    }
	    /* Either we accessed a zero length list, or we enumerated too far. */
	    throw new IndexOutOfBoundsException();
	}

    public void reset() {
		context.listIdx = 0;
	}

	/**
	 * Class to store context for alias
	 */
	public static class UAliasContext{
		private long listOffset;
		private long listIdx;
		
		public UAliasContext(long listOffset, long listIdx){
			this.listOffset = listOffset;
			this.listIdx = listIdx;
		}
		
		public long getListOffset(){
			return listOffset;
		}
		
		public long getListIdx(){
			return listIdx;
		}
	}

    public boolean hasMoreElements() {
        long listCount = UConverterAlias.gTaggedAliasLists[(int)context.listOffset];
        return (context.getListIdx() < listCount);
    }
}
