/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/utility/LengthFirstComparator.java,v $
* $Date: 2001/08/31 00:19:16 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.utility;

import java.util.*;

public final class LengthFirstComparator implements Comparator {
	public int compare(Object a, Object b) {
		String as = (String) a;
		String bs = (String) b;
		if (as.length() < bs.length()) return -1;
		if (as.length() > bs.length()) return 1;
		return as.compareTo(bs);
	}
}