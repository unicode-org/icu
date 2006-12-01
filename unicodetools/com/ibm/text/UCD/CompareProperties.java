/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/CompareProperties.java,v $
* $Date: 2004/02/12 08:23:15 $
* $Revision: 1.5 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.*;
import java.io.*;
import java.text.NumberFormat;

import com.ibm.text.utility.*;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class CompareProperties implements UCD_Types {
	
	static final boolean DO_DISJOINT = false;
	
	static CompareProperties me = null;
	
	static void partition() throws IOException {
		if (me == null) me = new CompareProperties();
		me.printPartition();
	}
	
	static void statistics() throws IOException {
		UnicodeSet a = new UnicodeSet("[abc]");
		UnicodeSet empty = new UnicodeSet();
		System.out.println(a.containsAll(empty));
		System.out.println(empty.containsAll(a));
		System.out.println(empty.containsAll(new UnicodeSet()));
		if (me == null) me = new CompareProperties();
		me.printStatistics();
	}
	
	public final class BitSetComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			BitSet bs1 = (BitSet) o1;
			BitSet bs2 = (BitSet) o2;
			int count2 = bs1.size() > bs2.size() ? bs1.size() : bs2.size();
			for (int i = 0; i < count2; ++i) {
				if (bs1.get(i)) {
					if (!bs2.get(i)) {
						return 1;
					}
				} else if (bs2.get(i)) {
					return -1;
				}
			}
			return 0;
		 }
	}
	
	/*
	 * 
	 * @author Davis
	 *
	 * Reverses the order of a comparison, for getting a list in reverse order
	 */
	public static class InverseComparator implements Comparator {
		private Comparator other;
		public InverseComparator(Comparator other) {
			this.other = other;
		}
		public int compare(Object a, Object b) {
			return other.compare(b, a);
		}
	}

	/*
	 * 
	 * @author Davis
	 *
	 * Reverses the order of a comparison, for getting a list in reverse order
	 */
	public static class MethodComparator implements Comparator {
		public int compare(Object a, Object b) {
			return ((Comparable)a).compareTo(b);
		}
	}

	public final static class UnicodeSetComparator implements Comparator {
		/**
		 * Compares two UnicodeSets, producing a transitive ordering.
         * The ordering is based on the first codepoint that differs between them.
		 * @return -1 if first set contains the first different code point 
		 * 1 if the second set does.
		 * 0 if there is no difference.
		 * If compareTo were added to UnicodeSet, this can be optimized to use list[i].
		 * @author Davis
		 *
		 */
		public int compare(Object o1, Object o2) {
			UnicodeSetIterator it1 = new UnicodeSetIterator((UnicodeSet) o1);
			UnicodeSetIterator it2 = new UnicodeSetIterator((UnicodeSet) o2);
			while (it1.nextRange()) {
                if (!it2.nextRange()) return -1; // first has range while second exhausted
				if (it1.codepoint < it2.codepoint) return -1; // first has code point not in second
				if (it1.codepoint > it2.codepoint) return 1;
				if (it1.codepointEnd < it2.codepointEnd) return 1; // second has codepoint not in first
				if (it1.codepointEnd > it2.codepointEnd) return -1;
			}
            if (it2.nextRange()) return 1; // second has range while first is exhausted
			return 0; // otherwise we ran out in both of them, so equal
		 }
	}

    boolean isPartitioned = false;
	
	UCDProperty[] props = new UCDProperty[500];
	UnicodeSet[] sets = new UnicodeSet[500];
	int count = 0;
	BitSet[] disjoints = new BitSet[500];
	BitSet[] contains = new BitSet[500];
	BitSet[] isin = new BitSet[500];
	BitSet[] equals = new BitSet[500];
	
	Map map = new TreeMap(new BitSetComparator());

    {
        getProperties();   
        fillPropertyValues();
		Utility.fixDot();
	}

	private void fillPropertyValues() {
	    BitSet probe = new BitSet();
	    int total = 0;
	    for (int cp = 0; cp <= 0x10FFFF; ++cp) {
	        Utility.dot(cp);
	        int cat = Default.ucd().getCategory(cp);
	        // if (cat == UNASSIGNED || cat == PRIVATE_USE || cat == SURROGATE) continue;
	        if (!Default.ucd().isAllocated(cp)) continue;
	    
	        for (int i = 0; i < count; ++i) {
	            UCDProperty up = props[i];
	            boolean iProp = up.hasValue(cp);
	            if (iProp) {
	            	probe.set(i);
	            	sets[i].add(cp);
	            } else {
	            	probe.clear(i);
	          	} 
	        }
	    
	        ++total;
	        UnicodeSet value = (UnicodeSet) map.get(probe);
	        if (value == null) {
	        	value = new UnicodeSet();
	            map.put(probe.clone(), value);
	            // Utility.fixDot();
	            // System.out.println("Set Size: " + map.size() + ", total: " + total + ", " + Default.ucd.getCodeAndName(cp));
	        }
	        value.add(cp);
	    }
	}

	private void getProperties() {
	    for (int i = 0; i < LIMIT_ENUM; ++i) { //   || iType == SCRIPT
	        int iType = i & 0xFF00;
	        if (iType == AGE || iType == JOINING_GROUP || iType == COMBINING_CLASS) continue;
	        if (i == 0x0900) {
	        	System.out.println("debug");
	        }
	        UCDProperty up = UnifiedBinaryProperty.make(i, Default.ucd());
	        if (up == null) continue;
			if (up.getValueType() < BINARY_PROP) {
				System.out.println("\tSkipping " + up.getName() + "; value varies");
				continue;
			}
	        if (!up.isStandard()) {
	            System.out.println("\tSkipping " + getPropName(up) + "; not standard");
	            continue;
	        }
	        if (up.getName(LONG).startsWith("Other_")) {
				System.out.println("\tSkipping " + getPropName(up) + "; contributory");
				continue;	        	
	        }
	        if (up.isDefaultValue() || up.skipInDerivedListing()) {
				System.out.println("\tSkipping " + getPropName(up) + "; default value");
				continue;	        	
	        }
	        // System.out.println(Utility.hex(i) + " " + up.getName(LONG) + "(" + up.getName(SHORT) + ")");
	        // System.out.println("\t" + up.getValue(LONG) + "(" + up.getValue(SHORT) + ")");
			sets[count] = new UnicodeSet();
			disjoints[count] = new BitSet();
			equals[count] = new BitSet();
			contains[count] = new BitSet();
			isin[count] = new BitSet();
	        props[count++] = up;
	        System.out.println(Utility.hex(i) + " " + (count - 1) + " " + getPropName(count - 1));	        
	    }
	    System.out.println("props: " + count);
	}
    
	public void printPartition() throws IOException {
		System.out.println("Set Size: " + map.size());
		PrintWriter output = Utility.openPrintWriter("Partition"
			 + UnicodeDataFile.getFileSuffix(true), Utility.LATIN1_WINDOWS);
        
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
			BitSet probe2 = (BitSet) it.next();
			UnicodeSet value = (UnicodeSet) map.get(probe2);
			output.println();
			output.println(value);
			output.println("Size: " + value.size());
			for (int i = 0; i < count; ++i) {
				if (!probe2.get(i)) continue;
				output.print(" " + getPropName(i));
			}
			output.println();
		}
		output.println("Count: " + map.keySet().size());
		output.close();
	}

	static final NumberFormat percent = NumberFormat.getPercentInstance(Locale.ENGLISH);
	
	public void printStatistics() throws IOException {
		System.out.println("Set Size: " + map.size());
		PrintWriter output = Utility.openPrintWriter("Statistics"
			 + UnicodeDataFile.getFileSuffix(true), Utility.LATIN1_WINDOWS);
        
        System.out.println("Finding disjoints/contains");
        for (int i = 0; i < count; ++i) {
			System.out.println(getPropName(i));
        	for (int j = 0; j < count; ++j) {
        		if (j == i) continue;
        		if (i == 1 && j == 2) {
        			System.out.println("debug");
        		}
        		if (sets[i].containsNone(sets[j])) {
        			disjoints[i].set(j);
        		} else if (sets[i].equals(sets[j])) {
					equals[i].set(j);
				} else if (sets[i].containsAll(sets[j])) {
					contains[i].set(j);
				} else if (sets[j].containsAll(sets[i])) {
					isin[i].set(j);
        		}
         	}
        }
        
		System.out.println("Removing non-maximal sets");
		// a set is non-maximal if it is contained in one of the other sets
		// so remove anything that is contained in one of the items
		if (false) {
			BitSet[] tempContains = new BitSet[count];
			for (int i = 0; i < count; ++i) {
				System.out.println(getPropName(i));
				tempContains[i] = (BitSet) contains[i]; // worry about collisions
				BitSet b = contains[i];
				for (int j = 0; j < b.size(); ++j) {
					if (b.get(j)) tempContains[i].andNot(contains[j]);
				}
				b = disjoints[i];	// don't worry
				for (int j = 0; j < b.size(); ++j) {
					if (b.get(j)) b.andNot(contains[j]);
				}
			}
			for (int i = 0; i < count; ++i) {
				contains[i] = tempContains[i];
			}
		}
		
		System.out.println("Printing disjoints & contains");
		// a set is non-maximal if it is contained in one of the other sets
		// so remove anything that is contained in one of the items
		List remainder = new ArrayList();
		Map m = new TreeMap(); // new UnicodeSetComparator()
		for (int i = 0; i < count; ++i) {
			m.put(getPropName(i), new Integer(i)); // sets[i]
		}
		Iterator it = m.keySet().iterator();
		while (it.hasNext()) {
			Object key = it.next();
			int index = ((Integer)m.get(key)).intValue();
			boolean haveName = printBitSet(output, index, "EQUALS: ", equals[index], false);
			haveName = printBitSet(output, index, "CONTAINS: ", contains[index], haveName);
			haveName = printBitSet(output, index, "IS CONTAINED IN: ", isin[index], haveName);
			if (DO_DISJOINT) {
				printBitSet(output, index, "IS DISJOINT WITH: ", disjoints[index], haveName);
			}
			if (!haveName) remainder.add(getPropName(index));
		}
		it = remainder.iterator();
		output.println();
		output.print("NONE OF THE ABOVE: ");
		boolean first = true;
		while (it.hasNext()) {
			Object key = it.next();
			if (!first) output.print(", ");
			first = false;
			output.print(key);
		}
		output.println();
		output.close();
	}

    private boolean printBitSet(PrintWriter output, int index, String title, BitSet b, boolean haveName) {
        if (!b.isEmpty()) {
        	if (!haveName) {
				output.println();
				output.println(getPropName(index));
		       	haveName = true;
			}
			output.print(title);
			Set ss = new TreeSet();
			for (int j = 0; j < b.size(); ++j) {      		
				if (b.get(j)) {
					ss.add(getPropName(j));
				}
			}
			Iterator it = ss.iterator();
	       	boolean first = true;
	       	while (it.hasNext()) {
    			if (!first) output.print(", ");
    			first = false;
    			output.print(it.next());
        	}
			output.println();
			output.flush();
       }
       return haveName;
   }

	/* 
			UnicodeSet a_b = new UnicodeSet();
			UnicodeSet ab = new UnicodeSet();
			UnicodeSet _ab = new UnicodeSet();
	 */
	/*
	a_b.set(sets[i]).removeAll(sets[j]);
	ab.set(sets[i]).retainAll(sets[j]);
	_ab.set(sets[j]).removeAll(sets[i]);
	// we are interested in cases where a contains b or is contained by b
	// contain = _ab = 0
	// is contained == a_b = 0
	// is disjoint == ab == 0
	// is equal == contains & iscontained
	double total = a_b.size() + ab.size() + _ab.size();
	double limit = total*0.03;
	boolean gotName = showDiff(output, "C", j, a_b, total, limit, false);
	gotName = showDiff(output, "D", j, ab, total, limit, gotName);
	gotName = showDiff(output, "S", j, _ab, total, limit, gotName);
	if (gotName) output.println();
	*/

	private boolean showDiff(PrintWriter output, String title, int propIndex, UnicodeSet a_b, 
    		double total, double limit, boolean gotName) {
        if (0 < a_b.size() && a_b.size() < limit) {
        	if (!gotName) {
        		gotName = true;
        		output.print("\t" + getPropName(propIndex));
        	}
        	output.print("\t" + title + percent.format(a_b.size()/total));
        }
        return gotName;
    }

	private String getPropName(int propertyIndex) {
		return getPropName(props[propertyIndex]);
	}

	private String getPropName(UCDProperty ubp) {
		return Utility.getUnskeleton(ubp.getFullName(LONG), true);
	}

    public static void listDifferences() throws IOException {
    
        PrintWriter output = Utility.openPrintWriter("PropertyDifferences" + UnicodeDataFile.getFileSuffix(true), Utility.LATIN1_UNIX);
        output.println("# Listing of relationships among properties, suitable for analysis by spreadsheet");
        output.println("# Generated for " + Default.ucd().getVersion());
        output.println(UnicodeDataFile.generateDateLine());
        output.println("# P1	P2	R(P1,P2)	C(P1&P2)	C(P1-P2)	C(P2-P1)");
        
    
        for (int i = 1; i < UCD_Types.LIMIT_ENUM; ++i) {
            int iType = i & 0xFF00;
            if (iType == UCD_Types.JOINING_GROUP || iType == UCD_Types.AGE || iType == UCD_Types.COMBINING_CLASS || iType == UCD_Types.SCRIPT) continue;
            UCDProperty upi = UnifiedBinaryProperty.make(i, Default.ucd());
            if (upi == null) continue;
            if (!upi.isStandard()) {
                System.out.println("Skipping " + upi.getName() + "; not standard");
                continue;
            }
            if (upi.getValueType() < UCD_Types.BINARY_PROP) {
                System.out.println("Skipping " + upi.getName() + "; value varies");
                continue;
            }
            
            String iNameShort = upi.getFullName(UCD_Types.SHORT);
            String iNameLong = upi.getFullName(UCD_Types.LONG);
    
            System.out.println();
            System.out.println();
            System.out.println(iNameLong);
            output.println("#" + iNameLong);
    
            int last = -1;
            for (int j = i+1; j < UCD_Types.LIMIT_ENUM; ++j) {
                int jType = j & 0xFF00;
                if (jType == UCD_Types.JOINING_GROUP || jType == UCD_Types.AGE || jType == UCD_Types.COMBINING_CLASS || jType == UCD_Types.SCRIPT
                    || (jType == iType && jType != UCD_Types.BINARY_PROPERTIES)) continue;
                UCDProperty upj = UnifiedBinaryProperty.make(j, Default.ucd());
                if (upj == null) continue;
                if (!upj.isStandard()) continue;
                if (upj.getValueType() < UCD_Types.BINARY_PROP) continue;
                
    
                if ((j >> 8) != last) {
                    last = j >> 8;
                    System.out.println();
                    System.out.print("\t" + UCD_Names.SHORT_UNIFIED_PROPERTIES[last]);
                    output.flush();
                    output.println("#\t" + UCD_Names.SHORT_UNIFIED_PROPERTIES[last]);
                } else {
                    System.out.print('.');
                }
                System.out.flush();
    
                int bothCount = 0, i_jPropCount = 0, j_iPropCount = 0, iCount = 0, jCount = 0;
    
                for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                    int cat = Default.ucd().getCategory(cp);
                    if (cat == UCD_Types.UNASSIGNED || cat == UCD_Types.PRIVATE_USE || cat == UCD_Types.SURROGATE) continue;
                    if (!Default.ucd().isAllocated(cp)) continue;
    
                    boolean iProp = upi.hasValue(cp);
                    boolean jProp = upj.hasValue(cp);
    
                    if (jProp) ++jCount;
                    if (iProp) {
                        ++iCount;
                        if (jProp) ++bothCount;
                        else ++i_jPropCount;
                    } else if (jProp) ++j_iPropCount;
                }
                if (iCount == 0 || jCount == 0) continue;
    
                String jNameShort = upj.getFullName(UCD_Types.SHORT);
                //String jNameLong = ubp.getFullID(j, LONG);
    
                String rel = bothCount == 0 ? "DISJOINT"
                    : i_jPropCount == 0 && j_iPropCount == 0 ? "EQUALS"
                    : i_jPropCount == 0 ? "CONTAINS" // depends on reverse output
                    : j_iPropCount == 0 ? "CONTAINS"
                    : "OVERLAPS";
    
                if (j_iPropCount > i_jPropCount) {
                    // reverse output
                    output.println(jNameShort + "\t" + iNameShort + "\t" + rel
                        + "\t" + bothCount + "\t" + j_iPropCount + "\t" + i_jPropCount);
                } else {
                    output.println(iNameShort + "\t" + jNameShort + "\t" + rel
                        + "\t" + bothCount + "\t" + i_jPropCount + "\t" + j_iPropCount);
                }
            }
        }
        output.close();
    }
}