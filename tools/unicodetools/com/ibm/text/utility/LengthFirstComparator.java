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