package com.ibm.text;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.lang.UCharacter;
import java.util.BitSet;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Iterator;
import java.text.NumberFormat;
import com.ibm.text.utility.FastIntBinarySearch;

public class TestICU4J {
  public static void main(String[] args) {
        String a = UTF16.valueOf(0x10000);
        String b = Normalizer.normalize("a\u0308", Normalizer.NFC);
        System.out.println(b);
    /*
    System.out.println(UCharacter.getType(0x10FFFF));
    System.out.println(UCharacter.getName(0x61));
    */
        testUnicodeSetSpeed(Character.TITLECASE_LETTER, 100);
        testUnicodeSetSpeed(Character.UNASSIGNED, 1);
  }
  
  static final boolean SHOW_ERRORS = false;
  static boolean OPTIMIZATION = true;
  
  static void testUnicodeSetSpeed(int prop, int ITERATIONS) {
        NumberFormat numb = NumberFormat.getNumberInstance();
        NumberFormat percent = NumberFormat.getPercentInstance();
        double start, delta, oldDelta;
        int temp = 0;
        Set s;
        UnicodeSet us;
        Iterator it;
        UnicodeSetIterator uit;
        
        BitSet bs = new BitSet();
        System.out.println();
        System.out.println("Getting characters for property " + prop);
        int total = 0;
        for (int cp = 0; cp < 0x10FFFF; ++cp) {
            if (UCharacter.getType(cp) == prop) {
                bs.set(cp);
                ++total;
            }
        }
        System.out.println("Total characters: " + numb.format(total));
        System.out.println("Loop Iterations: " + numb.format(ITERATIONS));
        System.out.println();
        
        System.out.println("Testing Add speed");
        
        s = new TreeSet();
        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; ++i) {
            s.clear();
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                if (bs.get(cp)) {
                    s.add(new Integer(cp));
                }
            }
        }
        oldDelta = delta = (System.currentTimeMillis() - start)/ITERATIONS;
        System.out.println("Set add time: " + numb.format(delta));
        System.out.println("Total characters: " + numb.format(s.size()));
        
        us = new UnicodeSet();
        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; ++i) {
            us.clear();
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                if (bs.get(cp)) {
                    optimizedAdd(us,cp);
                }
            }
        }
        optimizedDone(us);
        delta = (System.currentTimeMillis() - start)/ITERATIONS;
        System.out.println("UnicodeSet add time: " + numb.format(delta) + ", " + percent.format(delta/oldDelta));
        System.out.println("Total characters: " + numb.format(us.size()) + ", ranges: " + us.getRangeCount());
        
        System.out.println();
        System.out.println("Testing Contains speed");
        
        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; ++i) {
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                if (s.contains(new Integer(cp)) != bs.get(cp)) {
                    if (SHOW_ERRORS) System.out.println("Error at: " + info(cp));
                }
            }
        }
        oldDelta = delta = (System.currentTimeMillis() - start)/ITERATIONS;
        System.out.println("Set contains time: " + numb.format(delta));
        
        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; ++i) {
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                if (us.contains(cp) != bs.get(cp)) {
                    if (SHOW_ERRORS) System.out.println("Error at: " + info(cp));
                }
            }
        }
        delta = (System.currentTimeMillis() - start)/ITERATIONS;
        System.out.println("UnicodeSet contains time: " + numb.format(delta) + ", " + percent.format(delta/oldDelta));
 
        setupBinary(us);
        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; ++i) {
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                if (binaryContains(cp) != bs.get(cp)) {
                    if (SHOW_ERRORS) System.out.println("Error at: " + info(cp));
                }
            }
        }
        delta = (System.currentTimeMillis() - start)/ITERATIONS;
        System.out.println("BINARY UnicodeSet contains time: " + numb.format(delta) + ", " + percent.format(delta/oldDelta));
 
        System.out.println("Testing Iteration speed");
        
        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; ++i) {
            it = s.iterator();
            while (it.hasNext()) {
                temp += ((Integer)it.next()).intValue();
            }
        }
        oldDelta = delta = (System.currentTimeMillis() - start)/ITERATIONS;
        System.out.println("Set iteration time: " + numb.format(delta));
        
        uit = new UnicodeSetIterator(us);
        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; ++i) {
            uit.reset();
            while (uit.next()) {
                temp += uit.codepoint;
            }
        }
        delta = (System.currentTimeMillis() - start)/ITERATIONS;
        System.out.println("UnicodeSet iteration time: " + numb.format(delta) + ", " + percent.format(delta/oldDelta));

        uit.reset();
        start = System.currentTimeMillis();
        while (uit.nextRange()) {
            System.out.println(info(uit.codepoint, uit.codepointEnd));
        }
  }
  
  static FastIntBinarySearch fibs;
  
  static void setupBinary(UnicodeSet us) {
    int[] dummySearch = new int[us.getRangeCount()*2];
    int dummyLimit = 0;
    UnicodeSetIterator uit = new UnicodeSetIterator(us);
    while (uit.nextRange()) {
        dummySearch[dummyLimit++] = uit.codepoint;
        dummySearch[dummyLimit++] = uit.codepointEnd+1;
    }
    fibs = new FastIntBinarySearch(dummySearch);
  }
  
  static boolean binaryContains(int cp) {
    return ((fibs.findIndex(cp) & 1) != 0); // return true if odd
  }
    
  
  static String info(int cp) {
    return Integer.toString(cp, 16).toUpperCase() + " " + UCharacter.getName(cp);
  }
  
  static String info(int cpStart, int cpEnd) {
    if (cpStart == cpEnd) {
        return Integer.toString(cpStart, 16).toUpperCase()
            + " " + UCharacter.getName(cpStart);
    }
    return Integer.toString(cpStart, 16).toUpperCase() + ".." + Integer.toString(cpEnd, 16).toUpperCase()
        + " " + UCharacter.getName(cpStart) + ".." + UCharacter.getName(cpEnd);
  }
  
  static int first;
  static int limit = -2;
  
  static void optimizedAdd(UnicodeSet us, int cp) {
    if (!OPTIMIZATION) {
        us.add(cp);
        return;
    }
    if (cp == limit) {
        ++limit;
    } else {
        if (limit > 0) {
            us.add(first, limit - 1);
            // System.out.println(info(first, limit-1));
        }
        first = cp;
        limit = cp + 1;
    }
  }
  
  static void optimizedDone(UnicodeSet us) {
    if (!OPTIMIZATION) return;
    if (limit > 0) {
        us.add(first, limit - 1);
        //System.out.println(info(first, limit-1));
    }
    limit = -2; // reset to invalid
  }

  
  public static class UXCharacter {
	/**
	* Provides interface for properties in 
	* http://www.unicode.org/Public/UNIDATA/PropertyAliases.txt
	* and their values in 
	* http://www.unicode.org/Public/UNIDATA/PropertyValueAliases.txt
	*/
	
	/**
	 * Tests a particular code point to see if the cited property has the given value.
	 *
	 * Sample: the following are equivalent
	 * <pre>
	 *		if (UCharacter.test("LB", "AL", cp)) ...
	 *		if (UCharacter.test("line break", "alphabetic", cp)) ...
	 * </pre>
	 *
	 */
	public static boolean test(String propertyName, String propertyValue, int codePoint) {
		return false;
	}

	/**
	 * Produces a UnicodeSet of code points that have the given propertyvalue for the given property.
	 * @param set the resulting value. The set is cleared, 
	 * then all the code points with the given <property, value> are added. 
	 *
	 * Sample: the following are equivalent
	 * <pre>
	 *		if (UCharacter.test("WSpace", cp)) ...
	 *		if (UCharacter.test("White_Space", cp)) ...
	 *		if (UCharacter.test("White_Space", "true", cp)) ...
	 *		if (!UCharacter.test("White_Space", "false", cp)) ...
	 * </pre>
	 *
	 */
	public static void getSet(String propertyName, String propertyValue, UnicodeSet set) {
		// logical implemenation. Real implementation would be way faster!
		set.clear();
		for (int cp = 0; cp <= 0x10FFFF; ++cp) {
			if (test(propertyName, propertyValue, cp)) set.add(cp);
		}
	}

	// ======================================================
	// POSSIBLE ADDITIONAL UTILITIES FOR CONVENIENCE OR SPEED
	// ======================================================
	
	/**
	 * Tests a particular code point to see if the cited boolean property is true.
	 * @param propertyName the cited property
	 * @param codePoint the particular code point
	 * @return true if the cited property has the given value for the specified code point.
	 *
	 * Sample: the following are equivalent
	 * <pre>
	 *		if (UCharacter.test("WSpace", cp)) ...
	 *		if (UCharacter.test("White_Space", cp)) ...
	 *		if (UCharacter.test("White_Space", "true", cp)) ...
	 *		if (!UCharacter.test("White_Space", "false", cp)) ...
	 * </pre>
	 *
	 */
	public static boolean test(String booleanPropertyName, int codePoint) {
		return test(booleanPropertyName, "true", codePoint);
	}

	// ===============================================
	// The following allow access to properties by number, saving a string lookup
	// on each call.
	// ===============================================
	

	/**
	 * Gets an index for higher-speed access to properties.
	 *
	 * Sample:
	 * <pre>
	 *		int prop = UCharacter.getPropertyIndexIndex("LB");
	 *		int value = UCharacter.getValueIndex("LB", "AL");
	 *		while (true) {
	 *		...
	 *		if (test(prop, value, codePoint)) ...
	 * </pre>
	 *
	 */
	public static int getPropertyIndex(String propertyName) {
		return 0;
	}
	
	/**
	 * Gets maximum property index, used for iterating through properties
	 *
	 */
	public static int getMaxPropertyIndex() {
		return 0;
	}
	

	static final byte // NAME_STYLE
		SHORT = 0,
		DEFAULT = 1,
		LONG = 2;

	/**
	 * Gets property name
	 *
	 */
	public static String getPropertyName(int propertyIndex, byte namestyle) {
		return "";
	}

	/*
	 * Tests a particular code point to see if the cited property has the given value.
	 */
	public static boolean test(int propertyIndex, String propertyValue, int codePoint) {
		return false;
	}

	/**
	 * Produces a UnicodeSet of code points that have the given propertyvalue for the given property.
	 */
	public static void getSet(int propertyIndex, String propertyValue, UnicodeSet set) {
	}

	// ===============================================
	// The following allow access to enumerated property values by number,
	// saving a string lookup on each call.
	// They are only valid for enumerated properties
	// including the combining character class (0..255).
	// ===============================================
	
	/**
	 * Gets an index for higher-speed access to property values.
	 * Only valid for enumerated properties.
	 */
	public static int getValueIndex(String propertyName, String propertyValue) {
		return 0;
	}

	/**
	 * Gets maximum value index for a given property, used for iterating through property values.
	 * Only valid for enumerated properties.
	 *
	 */
	public static int getMaxValueIndex(int propertyIndex) {
		return 0;
	}
	
	/**
	 * Gets property value, corresponding to one of the values passed in
	 *
	 */
	public static String getValueName(int propertyIndex, int valueIndex, byte namestyle) {
		return "";
	}
	
	/*
	 * Tests a particular code point to see if the cited property has the given value.
	 */
	public static boolean test(int propertyIndex, int valueIndex, int codePoint) {
		return false;
	}

	/**
	 * Produces a UnicodeSet of code points that have the given propertyvalue for the given property.
	 */
	public static void getSet(int propertyIndex, int valueIndex, UnicodeSet set) {
	}


/* OPEN ISSUES:
- Don't like the names of the functions. Any better options? test => hasValue? hasPropertyValue?
- Should getSet really ADD to the set (avoiding the clear?) and be called addProperties?
Maybe faster sometimes, but might also be more errorprone.
*/
  
  }
}