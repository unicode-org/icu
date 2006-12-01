/*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/utility/SampleEnum.java,v $
* $Date: 2002/10/05 01:28:56 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text.utility;

import java.util.*;

/** Sample Poor-Man's Enum.
 * To use as a template, copy and 
 * <ul>
 * <li>replace all instances of "SampleEnum" by your enum's name</li>
 * <li>change the enum values to your values</li>
 * <li>set any aliases (or remove that section)</li>
 * </ul>
 */
public final class SampleEnum extends PoorMansEnum {
    private static PoorMansEnum.EnumStore store = new PoorMansEnum.EnumStore();
    
    public static final SampleEnum
        ALPHA = add("The"),
        BETA = add("Quick"),
        GAMMA = add("Brown"),
        
        FIRST = ALPHA;
    
    static {
        store.addAlias(ALPHA, "A");
    }
   
    /* Boilerplate */
    public SampleEnum next() { return (SampleEnum) next; }
    public void getAliases(Collection output) { store.getAliases(this, output); }
    public static SampleEnum get(String s) { return (SampleEnum) store.get(s); }
    public static SampleEnum get(int v) { return (SampleEnum) store.get(v); }
    public static int getMax() { return store.getMax(); }
    
    private SampleEnum() {}
    private static SampleEnum add(String name) { return (SampleEnum) store.add(new SampleEnum(), name);}



    /* just for testing */
    public static void test() {
        // int to string, collecting strings as we go
        Set s = new TreeSet();
        for (int i = 0; i < SampleEnum.getMax(); ++i) {
            String n = SampleEnum.get(i).toString();
            System.out.println(i + ", " + n);
            s.add(n);
        }
        // String to int
        Iterator it = s.iterator();
        while (it.hasNext()) {
            String n = (String)it.next();
            System.out.println(n + ", " + SampleEnum.get(n).toInt());
        }
        
        // iteration
        for (SampleEnum current = FIRST; current != null; current = current.next()) {
            s.clear();
            current.getAliases(s);
            System.out.println(current.toInt() + ", " + current + ", " + s);
        } 
    }    
        
    
}