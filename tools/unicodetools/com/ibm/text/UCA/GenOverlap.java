/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCA/GenOverlap.java,v $ 
* $Date: 2001/08/31 00:20:40 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.UCA;

import java.util.*;
import java.io.*;
import com.ibm.text.UCD.*;
import com.ibm.text.utility.*;
import com.ibm.text.UTF16;

public class GenOverlap {
  
    static Map completes = new TreeMap();
    static Map back = new HashMap();
    static Map initials = new HashMap();
    static int[] ces = new int[50];
    static UCA collator;
    static UCD ucd;
    static Normalizer nfd;
    static Normalizer nfkd;
    
    public static void test(UCA collatorIn) throws Exception {
        collator = collatorIn;
            
        CEList.main(null);
            
        System.out.println("# Overlap");
        System.out.println("# Generated " + new Date());
            
        ucd = UCD.make();

        nfd = new Normalizer(Normalizer.NFD);
        nfkd = new Normalizer(Normalizer.NFKD);
            
        UCA.CollationContents cc = collator.getCollationContents(UCA.FIXED_CE, nfd);
            
        // store data for faster lookup
            
        System.out.println("# Gathering Data");
        int counter = 0;
            
        int[] lenArray = new int[1];
            
        while (true) {
                
            Utility.dot(counter++);
            String s = cc.next(ces, lenArray);
            if (s == null) break;
            int len = lenArray[0];
                  
            CEList currCEList = new CEList(ces, 0, len);
            addString(s, currCEList);
        }
        
        for (int cp = 0x10000; cp <= 0x10FFFF; ++cp) {
            if (!ucd.isRepresented(cp)) continue;
            byte decompType = ucd.getDecompositionType(cp);
            if (decompType >= UCD.COMPATIBILITY) {
                String decomp = nfkd.normalize(cp);
                CEList celist = getCEList(cp, decomp, decompType);
                addString(decomp, celist);
                System.out.println("Adding: " + ucd.getCodeAndName(cp) + "\t" + celist);
            }
        }
            
        Utility.fixDot();
        System.out.println("# Completes Count: " + completes.size());
        System.out.println("# Initials Count: " + initials.size());
        System.out.println("# Writing Overlaps");
            
        // simpleList();
        fullCheck();
    }
    
    public static void addString(String s, CEList currCEList) {
        back.put(s, currCEList);
        completes.put(currCEList, s);
              
        for (int i = 1; i < currCEList.length(); ++i) {
            CEList start = currCEList.start(i);
            Set bag = (Set) initials.get(start);
            if (bag == null) {
                bag = new TreeSet();
                initials.put(start, bag);
            }
            bag.add(s);
        }
    }
    
  
    static void simpleList() {
        Iterator it = completes.keySet().iterator();
        int counter = 0;
        int foundCount = 0;
            
        while (it.hasNext()) {
            Utility.dot(counter++);
                
            // see if the ces for the current element are the start of something else
            CEList key = (CEList) it.next();
            String val = (String) completes.get(key);
            Set probe = (Set) initials.get(key);
              
            if (probe != null) {
            Utility.fixDot();
            foundCount++;
            System.out.println("Possible Overlap: ");
            System.out.println("  " + ucd.getCodeAndName(val));
            System.out.println("\t" + key);
                
            Iterator it2 = probe.iterator();
            int count2 = 0;
            while (it2.hasNext()) {
                String match = (String) it2.next();
                CEList ceList = (CEList) back.get(match);
                System.out.println((count2++) + ".  " + ucd.getCodeAndName(match));
                System.out.println("\t" + ceList);
            }
            }
        }
        System.out.println("# Found Count: " + foundCount);
    }
    
    static boolean PROGRESS = false;
      
    static void fullCheck() throws IOException {
        PrintWriter log = Utility.openPrintWriter("Overlap.html");
        PrintWriter simpleList = Utility.openPrintWriter("Overlap.txt");
        
        Iterator it = completes.keySet().iterator();
        int counter = 0;
        int foundCount = 0;
            
        String [] goalChars = new String[1];
        String [] matchChars = new String[1];
        
        // CEList show = getCEList("\u2034");
        log.println("<html><head>");
        log.println("<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        log.println("<title>New Page 1</title>");
        log.println("<style><!--");
        log.println("table        { border-style: solid; border-width: 1 }");
        log.println("td           { border-style: solid; border-width: 1 }");
        log.println("--></style>");
        log.println("</head><body><table>");

            
        while (it.hasNext()) {
            Utility.dot(counter++);
            CEList key = (CEList) it.next();
            if (key.length() < 2) continue;
            
            String val = (String) completes.get(key);
            goalChars[0] = "";
            matchChars[0] = "";
            if (matchWhole(val, key, 0, goalChars, matchChars)) {
                
                simpleList.println(ucd.getCodeAndName(val));
                
                goalChars[0] = val + goalChars[0]; // fix first char
                
                if (!getCEList(goalChars[0]).equals(getCEList(matchChars[0]))) {
                    log.println("<tr><td colspan='6'>WARNING:" + getCEList(matchChars[0]) + "</td></tr>");
                }
                foundCount++;
                log.println("<tr><td>" + val + "</td>");
                log.println("<td>" + goalChars[0] + "</td>");
                log.println("<td>" + matchChars[0] + "</td>");
                log.println("<td>" + ucd.getCodeAndName(goalChars[0]) + "</td>");
                log.println("<td>" + ucd.getCodeAndName(matchChars[0]) + "</td>");
                log.println("<td>" + getCEList(goalChars[0]) + "</td></tr>");
                //log.println("\t" + );
            }
        }
        log.println("</tr></table>Number of Overlapping characters: " + foundCount + "</body>");
        log.close();
        simpleList.close();
    }
  
    static private CEList getCEList(String s) {
        int len = collator.getCEs(s, true, ces);
        return new CEList(ces, 0, len);
    }
  
    static private CEList getCEList(int originalChar, String s, byte type) {
        int len = collator.getCEs(s, true, ces);
        for (int i = 0; i < len; ++i) {
            ces[i] = UCA.makeKey(UCA.getPrimary(ces[i]), 
                UCA.getSecondary(ces[i]),
                CEList.remap(originalChar, type, UCA.getTertiary(ces[i])));
        }
        return new CEList(ces, 0, len);
    }
  
    static boolean matchWhole(String goalStr, CEList goal, int depth, String[] goalChars, String[] otherChars) {
        
        if (PROGRESS) System.out.println(Utility.repeat(". ", depth) + "Trying: " + ucd.getCodeAndName(goalStr) + ", " + goal);
        
        // to stop infinite loops, we limit the depth to 5
        if (depth > 5) {
            if (PROGRESS) System.out.println(Utility.repeat(". ", depth) + "stack exhausted");
            return false;
        }
        
        String match;
        
        // There are 3 possible conditions. Any of which work.
        
        // To eliminate double matches at the top level, we test depth > 0
        
        if (depth > 0) {
        
            // Condition 1.
            // we have an exact match
            
            match = (String) completes.get(goal);
            if (match != null) {
                if (PROGRESS) System.out.println(Utility.repeat(". ", depth) + "Matches Exactly: " + ucd.getCodeAndName(match));
                otherChars[0] = match + otherChars[0];
                if (PROGRESS) System.out.println(Utility.repeat(". ", depth)
                    + ucd.getCode(goalChars[0])
                    + " / " + ucd.getCode(otherChars[0])
                );
                return true;
            }
            
            
            // Condition 2
            // this whole string matches some initial portion of another string
            // AND the remainder of that other string also does a matchWhole.
            // Example: if we get the following, we search for a match to "de"
            // abc...
            // abcde
            // If we find a match, we append to the strings, the string for abc
            // and the one for abcde
            
            Set probe = (Set) initials.get(goal);
            if (probe != null) {
                Iterator it2 = probe.iterator();
                while (it2.hasNext()) {
                    match = (String) it2.next();
                    if (PROGRESS) System.out.println(Utility.repeat(". ", depth) + "Matches Longer: " + ucd.getCodeAndName(match)
                        + "\t\tswitching");
                    CEList trail = ((CEList) back.get(match)).end(goal.length());
                    boolean doesMatch = matchWhole(match, trail, depth+1, otherChars, goalChars);
                    if (doesMatch) {
                        otherChars[0] = match + otherChars[0];
                        if (PROGRESS) System.out.println(Utility.repeat(". ", depth)
                            + ucd.getCode(goalChars[0])
                            + " / " + ucd.getCode(otherChars[0])
                        );
                        return true;
                    }
                }
            }
        }
        
        // Condition 3
        // the first part of this string matches a whole other string
        // and the remainder of this string also does a matchWhole
        // Example: if we get the following, we search for a match to "de"
        // abcde..
        // abc..
        // if we find a match

        for (int i = goal.length() - 1; i > 0; --i) {
            CEList first = goal.start(i);
            match = (String) completes.get(first);
            if (match != null) {
                if (PROGRESS) System.out.println(Utility.repeat(". ", depth) + "Matches Shorter: " + ucd.getCodeAndName(match));
                boolean doesMatch = matchWhole("", goal.end(i), depth+1, goalChars, otherChars);
                if (doesMatch) {
                    otherChars[0] = match + otherChars[0];
                    if (PROGRESS) System.out.println(Utility.repeat(". ", depth)
                        + ucd.getCode(goalChars[0])
                        + " / " + ucd.getCode(otherChars[0])
                    );
                    return true;
                }
            }
        }
        
        // if we get this far, we failed.
        
        return false;
    }
    
    public static void generateRevision (UCA collatorIn) throws Exception {
        generateRevision(collatorIn, false);
        generateRevision(collatorIn, true);
    }
        
    public static void generateRevision (UCA collatorIn, boolean doMax) throws Exception {
        collator = collatorIn;
            
        CEList.main(null);
            
        System.out.println("# Generate");
        System.out.println("# Generated " + new Date());
            
        ucd = UCD.make();

        nfd = new Normalizer(Normalizer.NFD);
        nfkd = new Normalizer(Normalizer.NFKD);
            
        UCA.CollationContents cc = collator.getCollationContents(UCA.FIXED_CE, nfd);
            
        // store data for faster lookup
            
        System.out.println("# Gathering Data");
        int counter = 0;
            
        int[] lenArray = new int[1];
        
        Set list = new TreeSet();
        Map newCollisions = new HashMap();
        Map oldCollisions = new HashMap();
        Map newProblems = new TreeMap();
        Map oldProblems = new TreeMap();
        
        CEList nullCEList = new CEList(new int[1]);
            
        while (true) {
            Utility.dot(counter++);
            String str = cc.next(ces, lenArray);
            if (str == null) break;
            int len = lenArray[0];
                  
            CEList oldList = new CEList(ces, 0, len);
            
            CEList newList = new CEList(ces,0,0);
            int cp;
            for (int i = 0; i < str.length(); i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(str, i);
                if (0xFF67 <= cp && cp <= 0xFF6F) {
                    System.out.println("debug");
                }
                boolean mashLast = false;
                if (nfkd.normalizationDiffers(cp)) {
                    String decomp = nfkd.normalize(cp);
                    String canon = nfd.normalize(cp);
                    len = collator.getCEs(decomp, true, ces);
                    if (!decomp.equals(canon)) {
                        byte type = ucd.getDecompositionType(cp);
                        for (int j = 0; j < len; ++j) {
                            int p = (i == 0 && decomp.length() > 1 && decomp.charAt(0) == ' ' ? 0x20A : UCA.getPrimary(ces[j]));
                            int s = UCA.getSecondary(ces[j]);
                            boolean needsFix = (s != 0x20 && p != 0);
                            if (needsFix) ++len;
                            int t = (doMax && len > 1 && j == len-1 ? 0x1F : CEList.remap(cp, type, UCA.getTertiary(ces[j])));
                            if (needsFix) {
                                ces[j++] = UCA.makeKey(p, 0x20, t);             // Set Extra
                                System.arraycopy(ces, j, ces, j+1, len - j);    // Insert HOLE!
                                p = 0;
                            }
                            ces[j] = UCA.makeKey(p, s, t);
                        }
                    }
                } else {
                    len = collator.getCEs(UTF16.valueOf(cp), true, ces);
                }
                CEList inc = new CEList(ces, 0, len);
                
                if (cp == 0xFF71 || cp == 0xFF67) {
                    System.out.println("  String: " + ucd.getCodeAndName(cp));
                    System.out.println("  Type: " + ucd.getDecompositionTypeID(cp));
                    System.out.println("  xxx: " + inc);
                }
                
                newList = newList.append(inc);
                
            }
            if (newList.length() == 0) newList = nullCEList;
            if (oldList.length() == 0) oldList = nullCEList;
            
            if (!newList.equals(oldList)) {
                /*
                System.out.println("String: " + ucd.getCodeAndName(str));
                System.out.println("\tOld: " + oldList);
                System.out.println("\tNew: " + newList);
                */
                list.add(new Pair(newList, new Pair(str, oldList)));
            }
            
            // check for collisions
            if (str.equals("\u206F")) {
                System.out.println("debug");
            }
            Object probe = newCollisions.get(newList);
            if (probe == null) {
                newCollisions.put(newList, str);
            } else {
                newProblems.put(str, new Pair((String)probe, newList));
            }
  
            probe = oldCollisions.get(oldList);
            if (probe == null) {
                oldCollisions.put(oldList, str);
            } else {
                oldProblems.put(str, new Pair((String)probe, oldList));
            }
            
        }
        
        Set newKeys = new TreeSet(newProblems.keySet());
        Set oldKeys = new TreeSet(oldProblems.keySet());
        Set joint = new TreeSet(newKeys);
        joint.retainAll(oldKeys);
        newKeys.removeAll(joint);
        oldKeys.removeAll(joint);
        
        PrintWriter log = Utility.openPrintWriter("UCA-old-vs-new" + (doMax ? "-MAX.txt" : ".txt"));
        Iterator it = list.iterator();
        int last = -1;
        while (it.hasNext()) {
            Utility.dot(counter++);
            Pair value = (Pair) it.next();
            CEList newList = (CEList)value.first;
            int cur = UCA.getPrimary(newList.at(0));
            if (cur != last) {
                log.println();
                last = cur;
            }
            Pair v2 = (Pair) value.second;
            String ss = (String)v2.first;
            log.println(ucd.getCodeAndName(ss) + "\t\t" + ucd.getDecompositionTypeID(ss.charAt(0)));
            log.println("\tnew:\t" + value.first);
            log.println("\told:\t" + v2.second);
        }
        
        /*
        log.println();
        log.println("New Collisions: " + newKeys.size());
        it = newKeys.iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            CEList cel = (CEList) newProblems.get(key);
            String other = (String) newCollisions.get(cel);
            log.println(ucd.getCodeAndName(key) + " collides with " + ucd.getCodeAndName(other));
            log.println("\t" + cel);
        }
        
        log.println("Removed Collisions: " + oldKeys.size());
        it = oldKeys.iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            CEList cel = (CEList) oldProblems.get(key);
            String other = (String) oldCollisions.get(cel);
            log.println(ucd.getCodeAndName(key) + " collides with " + ucd.getCodeAndName(other));
            log.println("\t" + cel);
        }
        */
        
        showCollisions(log, "New Collisions:", newKeys, newProblems);
        showCollisions(log, "Old Collisions:", oldKeys, oldProblems);
        showCollisions(log, "In Both:", joint, oldProblems);
        log.close();
    }
    
    static void showCollisions(PrintWriter log, String title, Set bad, Map probs) {
        log.println();
        log.println(title + bad.size());
        Iterator it = bad.iterator();
        Set lister = new TreeSet();
        
        while (it.hasNext()) {
            String key = (String) it.next();
            Pair pair = (Pair) probs.get(key);
            String other = (String) pair.first;
            CEList cel = (CEList) pair.second;
            if (key.equals("\u0001")) {
                System.out.println("debug");
            }
            lister.add(new Pair(cel, ucd.getCodeAndName(key) + ",\t" + ucd.getCodeAndName(other)));
        }
        
        it = lister.iterator();
        int last = -1;
        while (it.hasNext()) {
            Pair pair = (Pair) it.next();
            CEList cel = (CEList) pair.first;
            int curr = UCA.getPrimary(cel.at(0));
            if (curr != last) {
                last = curr;
                log.println();
            }
            log.println("Collision between: " + pair.second);
            log.println("\t" + pair.first);
        }
        log.flush();
    }
}