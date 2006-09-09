/**
*******************************************************************************
* Copyright (C) 2006, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/ 
package com.ibm.icu.charset;

import java.util.Enumeration;


/**
 * Enumeration for Converter Aliases
 */

final class UConverterAliasesEnumeration implements Enumeration {

	private UAliasContext context;
	
	/* Set alias context
	 */	
	void setContext(UAliasContext context){
		this.context = context;		
	}
	
    int count() {
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

    void reset() {
		context.listIdx = 0;
	}

	/**
	 * Class to store context for alias
	 */
	static class UAliasContext{
		private long listOffset;
		private long listIdx;
		
		UAliasContext(long listOffset, long listIdx){
			this.listOffset = listOffset;
			this.listIdx = listIdx;
		}
		
		long getListOffset(){
			return listOffset;
		}
		
		long getListIdx(){
			return listIdx;
		}
	}

    public boolean hasMoreElements() {
        long listCount = UConverterAlias.gTaggedAliasLists[(int)context.listOffset];
        return (context.getListIdx() < listCount);
    }
}
