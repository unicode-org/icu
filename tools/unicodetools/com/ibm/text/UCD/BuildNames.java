/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/BuildNames.java,v $
* $Date: 2002/06/13 21:14:05 $
* $Revision: 1.6 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.io.IOException;
import com.ibm.icu.text.UTF16;

//import com.ibm.text.unicode.UInfo;
import java.util.*;
import java.io.*;
//import java.text.*;

import com.ibm.text.utility.*;


public class BuildNames implements UCD_Types {

    static final boolean DEBUG = true;

    public static void main(String[] args) throws IOException {

        Default.setUCD();
        collectWords();
    }

    static Map words = new TreeMap(new LengthFirstComparator());
    static Map doubleWords = new TreeMap(new LengthFirstComparator());
    static Map tripleWords = new TreeMap(new LengthFirstComparator());
    static Map quadWords = new TreeMap(new LengthFirstComparator());
    static Set lines = new TreeSet(new LengthFirstComparator());
    static int[] letters = new int[128];
    
    static class Count {
    	Count(int count) {this.count = count;}
    	int count;
    }
    
    static String lastWord = "";
    static String preLastWord = "";
    static String prePreLastWord = "";
    
    static void addWord(String word, Map words) {
    	Count count = (Count) words.get(word);
    	if (count == null) {
    		count = new Count(0);
    		words.put(word, count);
    	}
        count.count++;
    }

    static void stash(String word, int position) {
    	addWord(word, words);
        
        // doubles
        
        if (position > 0) {
        	addWord(lastWord + "/" + word, doubleWords);
        }
        
        if (position > 1) {
        	addWord(preLastWord + "/" + lastWord + "/" + word, tripleWords);
        }
        
        if (position > 2) {
        	addWord(prePreLastWord + "/" + preLastWord + "/" + lastWord + "/" + word, quadWords);
        }
        
        prePreLastWord = preLastWord;
        preLastWord = lastWord;
        lastWord = word;
        
        for (int i = 0; i < word.length(); ++i) {
            letters[word.charAt(i)]++;
        }
    }

    static String transform(String line) {
        StringBuffer result = new StringBuffer();
        boolean changed = false;
        for (int i = 0; i < line.length(); ++i) {
            char c = line.charAt(i);

            if (c == '-' || c == '<' || c == '>') {
                if (result.length() > 0 && result.charAt(result.length()-1) != ' ') result.append(' ');
                result.append(c);
                if (i + 1 < line.length() && line.charAt(i+1) != ' ') result.append(' ');
                changed = true;
                continue;
            }

            if ('a' <= c && c <= 'z') {
                result.append((char)(c - 'a' + 'A'));
                changed = true;
                continue;
            }
            if ('0' <= c && c <= '9') {
                result.append('*').append((char)(c - '0' + 'A'));
                changed = true;
                continue;
            }
            result.append(c);
        }
        if (!changed) return line;
        return result.toString().trim();
    }
    
    static void printWords(Map words) {
        System.out.println();
        System.out.println("Finding largest");
        System.out.println();
        
        Map biggest = new TreeMap();
        Iterator it = words.keySet().iterator();
        while (it.hasNext()) {
        	String word = (String) it.next();
        	Count count = (Count) words.get(word);
        	biggest.put(new Integer(-count.count * word.length()), word); // make it negative just to reverse the sort
        }
        
        it = biggest.keySet().iterator();
        int counter = 0;
        while (it.hasNext()) {
        	if (counter++ > 50) break;
        	Integer key = (Integer) it.next();
        	String word = (String) biggest.get(key);
        	System.out.println(word + ":\t" + (-key.intValue()));
        }
    }  

    static void collectWords() throws IOException {

        String fname = "ShortNames.txt";
        System.out.println("Writing " + fname);
        PrintWriter log = Utility.openPrintWriter(fname, false, true);
        
        System.out.println("Gathering data");
        //Counter counter = new Counter();
        String[] parts = new String[100];
        //int total = 0;
        int used = 0;
        int sum = 0;
        int longSum = 0;
        
        for (int cp = 0; cp < 0x10FFFF; ++cp) {
            if (!Default.ucd.isAllocated(cp)) continue;
            if (Default.ucd.hasComputableName(cp)) continue;
            Utility.dot(cp);
            String name;
            
            if (Default.ucd.isRepresented(cp)) {
                name = Default.ucd.getName(cp, SHORT);
                log.println(Utility.hex(cp) + " " + name);
                String backName = Utility.replace(name, UCD_Names.NAME_ABBREVIATIONS, false);
                if (!name.equals(backName)) {
                    System.out.println("Failed to recreate: " + name + ", " + backName);
                }
            }
            
            // check the string, and its decomposition. This is just to get a good count.
            
            String str = UTF16.valueOf(cp);
            if (false && !Default.nfkd.isNormalized(cp)) {
                str += Default.nfkd.normalize(cp);
            }
                
            int cp2;
            for (int i = 0; i < str.length(); i += UTF16.getCharCount(cp2)) {
                cp2 = UTF16.charAt(str, i);
                name = Default.ucd.getName(cp2, SHORT);
                if (name == null) continue;
                //name = transform(name);

                sum += name.length();
                longSum += Default.ucd.getName(cp2).length();
                used++;

                // replace numbers & letters

                int len = Utility.split(name, ' ', parts);
                for (int j = 0; j < len; ++j) {
                    stash(parts[j], j);
                }

                lines.add(name);
            }
        }
        log.close();
        Utility.fixDot();
        //System.out.println("Overhead: " + (lastLink - used) + ", " + ((lastLink - used) * 100 / used) + "%");
        //System.out.println("Strings: " + sum + ", " + (lastLink*4));
        System.out.println("Short Names sum: " + sum + ", average: " + (sum + 0.0)/used);
        System.out.println("Long Names sum: " + longSum + ", average: " + (longSum + 0.0)/used);
        System.out.println("Savings: " + (1 - (sum+0.0)/longSum));
        
        
        printWords(words);
        printWords(doubleWords);
        printWords(tripleWords);
        printWords(quadWords);
        
        if (true) return;
        
        System.out.println();
        System.out.println("Compacting Words");
        System.out.println();
        Iterator it = words.keySet().iterator();
        
        int i = 0;
        while (it.hasNext()) {
            String s = (String) it.next();
            int test = CompactName.addWord(s);
            String round = CompactName.stringFromToken(test);
            boolean goesRound = round.equals(s);
            if (false || !goesRound) System.out.println("Compacting: '" + s + "': " + i++ + "(" + CompactName.lastToken + ")"
                + (goesRound ? ": NO RT: '" + round + "'" : ""));
        }

        System.out.println();
        System.out.println("Compacting Lines");
        System.out.println();
        CompactName.startLines();
        it = lines.iterator();
        i = 0;
        while (it.hasNext()) {
            String s = (String) it.next();
            if (s.equals("< BELL >")) {
                System.out.println("DEBUG");
            }
            int test = CompactName.addLine(s);
            String round = CompactName.stringFromToken(test);
            boolean goesRound = round.equals(s);
            if (false || !goesRound) System.out.println("Compacting: '" + s + "': " + i++ + "(" + CompactName.lastToken + ")"
                + (!goesRound ? ": NO RT: '" + round + "'" : ""));
        }

        /*System.out.println("Printing Compact Forms");
        for (int i = 0; i < CompactName.lastToken; ++i) {
            String s = CompactName.stringFromToken(i);
            System.out.println(i + ": '" + s + "'");
        }*/

        System.out.println("Strings: " + sum
            + ", " + (CompactName.spacedMinimum*4)
            + ", " + (CompactName.lastToken*4)
        );

    }
    /*
        Set stuff = new TreeSet();
        for (int i = 0; i < letters.length; ++i) {
            if (letters[i] != 0) {
                stuff.add(new Integer((letters[i] << 8) + i));
            }
        }

        it = stuff.iterator();
        while (it.hasNext()) {
            int in = ((Integer) it.next()).intValue();
            System.out.println((char)(in & 0xFF) + ":\t" + String.valueOf(in >> 8));
        }
            int r = addString(name);
            if (!DEBUG && !rname.equals(name)) {
                System.out.println("\tNo Round Trip: '" + rname + "'");
            }
    */

    static Map stringToInt = new HashMap();
    static Map intToString = new HashMap();

    static final int[] remap = new int['Z'+1];
    static final int maxToken;

    static {
        int counter = 1;
        remap[' '] = counter++;
        remap['-'] = counter++;
        remap['>'] = counter++;
        remap['<'] = counter++;
        for (int i = 'A'; i <= 'Z'; ++i) {
            remap[i] = counter++;
        }
        for (int i = '0'; i <= '9'; ++i) {
            remap[i] = counter++;
        }
        maxToken = counter;
    }

    static final String[] unmap = new String[maxToken];
    static {
        unmap[0] = "";
        for (int i = 0; i < remap.length; ++i) {
            int x = remap[i];
            if (x != 0) unmap[x] = String.valueOf((char)i);
        }
    }

    static int[] links = new int[40000];
    static final int linkStart = 0;
    static int lastLink = 0;
    static final int LITERAL_BOUND = 0x7FFF - maxToken * maxToken;

    static boolean isLiteral(int i) {
        return (i & 0x7FFF) > LITERAL_BOUND;
    }

    static String lookup(int i) {
        String result;
        boolean trailingSpace = false;
        if ((i & 0x8000) != 0) {
            i ^= 0x8000;
            trailingSpace = true;
        }
        if (i > LITERAL_BOUND) {
            i = i - LITERAL_BOUND;
            int first = i / maxToken;
            int second = i % maxToken;
            result = unmap[first] + unmap[second];
        } else {
            int value = links[i];
            int lead = value >>> 16;
            int trail = value & 0xFFFF;
            //if (DEBUG) System.out.println("lead: " + lead + ", trail: " + trail);
            result = lookup(lead) + lookup(trail);
        }
        if (trailingSpace) result += ' ';
        if (DEBUG) System.out.println("token: " + i + " => '" + result + "'");
        return result;
    }

    static int getInt(String s) {
        if (s.length() < 3) {
            if (s.length() == 0) return 0;
            int first = s.charAt(0);
            int second = s.length() > 1 ? s.charAt(1) : 0;
            return LITERAL_BOUND + (remap[first] * maxToken + remap[second]);
        }
        Object in = stringToInt.get(s);
        if (in == null) return -1;
        return ((Integer)in).intValue();
    }

    static int putString(String s, int lead, int trail) {
        Object in = stringToInt.get(s);
        if (in != null) throw new IllegalArgumentException();
        int value = (lead << 16) + (trail & 0xFFFF);
        int result = lastLink;
        links[lastLink++] = value;

        if (DEBUG) {
            System.out.println("'" + s + "', link[" + result + "] = lead: " + lead + ", trail: " + trail);
            String roundTrip = lookup(result);
            if (!roundTrip.equals(s)) {
                System.out.println("\t*** No Round Trip: '" + roundTrip + "'");
            }
        }
        stringToInt.put(s, new Integer(result));
        return result;
    }

    // s cannot have a trailing space. Must be <,>,-,SPACE,0-9,A-Z
    static int addString(String s) {
        int result = getInt(s);
        if (result != -1) return result;
        int limit = s.length() - 1;
        int bestLen = 0;
        int best_i = 0;
        int bestSpaceLen = 0;
        int bestSpace_i = 0;
        int lastSpace = -1;
        int spaceBits;
        int endOfFirst;

        // invariant. We break after a space if there is one.

        for (int i = 1; i < limit; ++i) {
            char c = s.charAt(i-1);
            spaceBits = 0;
            endOfFirst = i;
            if (c == ' ') {
                lastSpace = i;
                endOfFirst--;
                spaceBits = 0x8000;
            }

            String firstPart = s.substring(0, endOfFirst);
            String lastPart = s.substring(i);
            if (firstPart.equals("<START OF ")) {
                System.out.println("HUH");
            }
            int lead = getInt(firstPart);
            int trail = getInt(lastPart);
            if (lead >= 0 && trail >= 0) { // if both match, return immediately with pair
                if (DEBUG) System.out.println(s + " => '" + firstPart + (spaceBits != 0 ? "*" : "")
                    + "' # '" + lastPart + "' MATCH BOTH");
                return putString(s, spaceBits | lead, trail);
            }
            if (!isLiteral(lead)) {
                if (i > bestLen) {
                    bestLen = i;
                    best_i = i;
                }
                if (i > bestSpaceLen && c == ' ') {
                    bestSpaceLen = i;
                    bestSpace_i = i + 1;
                }
            }
            int end_i = s.length() - i;
            if (!isLiteral(trail)) {
                if (end_i > bestLen) {
                    bestLen = end_i;
                    best_i = i;
                }
                if (end_i > bestSpaceLen && c == ' ') {
                    bestSpaceLen = end_i;
                    bestSpace_i = i + 1;
                }
            }
        }
        if (lastSpace >= 0) {
            bestLen = bestSpaceLen;
            best_i = bestSpace_i;
        }

        spaceBits = 0;

        if (bestLen > 0) { // if one matches, recurse -- and return pair
            endOfFirst = best_i;
            if (lastSpace > 0) {
                --endOfFirst;
                spaceBits = 0x8000;
            }
            String firstPart = s.substring(0, endOfFirst);
            String lastPart = s.substring(best_i);
            int lead = getInt(firstPart);
            int trail = getInt(lastPart);
            if (lead >= 0) {
                if (DEBUG) System.out.println(s + " => '" + firstPart + (spaceBits != 0 ? "*" : "")
                    + "' # '" + lastPart + "' MATCH FIRST");
                return putString(s, spaceBits | lead, addString(lastPart));
            } else {
                if (DEBUG) System.out.println(s + " => '" + firstPart + (spaceBits != 0 ? "*" : "")
                    + "' # '" + lastPart + "' MATCH SECOND");
                return putString(s, spaceBits | addString(firstPart), trail);
            }
        }
        // otherwise, we failed to find anything. Then break before the last word, if there is one
        // otherwise break in the middle (but at even value)


        if (lastSpace >= 0) {
            best_i = lastSpace;
            endOfFirst = lastSpace - 1;
            spaceBits = 0x8000;
        } else {
            endOfFirst = best_i = ((s.length() + 1) / 4) * 2;
        }
        String firstPart = s.substring(0, endOfFirst);
        String lastPart = s.substring(best_i);
        if (DEBUG) System.out.println(s + " => '" + firstPart + (spaceBits != 0 ? "*" : "")
            + "' # '" + lastPart + "' FALLBACK");
        return putString(s, spaceBits | addString(firstPart), addString(lastPart));
    }

    /*
    static int addCompression(String s) {
        Object in = stringToInt.get(s);
        if (in != null) return ((Integer) in).intValue();
        // find best match, recursively
        int bestBreak = -1;
        boolean pickFirst = false;
        for (int i = 1; i < s.length() - 1; ++i) {
            char c = s.charAt(i);
            if (c == ' ' || c == '-') {
                Object pos1 = stringToInt.get(s.substring(0,i+1));
                //Object pos23 = stringToInt.get(s..substring(i));


                    if (pos2 >= 0 && pos3 >= 0) {
                        fullToCompressed.put(value, new Integer(index + reserved));
                        continue main;
                    }
                    if (pos2 >= 0) {
                         if (k > bestBreak) {
                            bestBreak = k;
                            pickFirst = true;
                         }
                    } else if (pos3 >= 0) {
                        if (value.length() - k > bestBreak) {
                            bestBreak = k;
                            pickFirst = false;
                        }
                    }
                }

            }
        }
    }

    static void gatherData() throws IOException {
        System.out.println("Gathering data");
        Counter counter = new Counter();
        String[] parts = new String[100];
        String[] parts2 = new String[100];
        int total = 0;
        for (int i = 0; i < 0x10FFFF; ++i) {
            //if ((i & 0xFF) == 0) System.out.println(Utility.hex(i));
            if (!ucd.isRepresented(i)) continue;
            String s = ucd.getName(i);
            total += s.length();
            int len = Utility.split(s, ' ', parts);
            for (int j = 0; j < len; ++j) {
                if (parts[j].indexOf('-') >= 0) {
                    // hyphen stuff
                    int len2 = Utility.split(parts[j], '-', parts2);
                    for (int k = 0; k < len2; ++k) {
                        if (k == len2 - 1) {
                            counter.add(parts2[k] + '-');
                        } else {
                            counter.add(parts2[k] + " ");
                        }
                    }
                } else {
                   // normal
                    counter.add(parts[j] + " ");
                }
            }
        }

        System.out.println("Sorting data");
        Map m = counter.extract();

        System.out.println("Printing data");

        PrintWriter log = new PrintWriter(
            new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(GEN_DIR + "NameCompression.txt")),
            32*1024));

        log.println("total: " + total);

        Iterator it = m.keySet().iterator();

        String mondo = "";
        int i = 0;
        int strTotal = 0;

        int index = 0;
        Map fullToCompressed = new HashMap();

        String mondoIndex = "";

        main:
        while (it.hasNext()) {
            index++;
            if ((i & 255) == 0) System.out.println("#" + i);
            Counter.RWInteger key = (Counter.RWInteger) it.next();
            String value =  (String)m.get(key);
            log.println(i++ + ": " + key + ": \"" + value + "\"");
            strTotal += value.length();


            // first 128 are the highest frequency, inc. space

            if (index < 128 - SINGLES) {
                mondo += value;
                fullToCompressed.put(value, new String((char)(index + reserved)));
                continue;
            }

            int pos = mondo.indexOf(value);
            if (pos >= 0) {
                // try splitting!

                int bestBreak = -1;
                boolean pickFirst = false;
                if (value.length() > 2) for (int k = 1; k < value.length()-1; ++k) {
                    int pos2 = mondo.indexOf(value.substring(0,k) + " ");
                    int pos3 = mondo.indexOf(value.substring(k));
                    if (pos2 >= 0 && pos3 >= 0) {
                        fullToCompressed.put(value, new Integer(index + reserved));
                        continue main;
                    }
                    if (pos2 >= 0) {
                         if (k > bestBreak) {
                            bestBreak = k;
                            pickFirst = true;
                         }
                    } else if (pos3 >= 0) {
                        if (value.length() - k > bestBreak) {
                            bestBreak = k;
                            pickFirst = false;
                        }
                    }
                }
                if (bestBreak > 0) {
                    if (pickFirst) {
                        mondo += value.substring(bestBreak);
                    } else {
                        mondo += value.substring(0, bestBreak) + " ";
                    }
                } else {
                    mondo += value;
                }
            }

            // high bit on, means 2 bytes, look in array
        }

        log.println("strTotal: " + strTotal);
        log.println("mondo: " + mondo.length());

        int k = 80;
        for (; k < mondo.length(); k += 80) {
            log.println(mondo.substring(k-80, k));
        }
        log.println(mondo.substring(k-80)); // last line

        log.close();
    }

    static int indexOf(StringBuffer target, String source) {
        int targetLen = target.length() - source.length();
        main:
        for (int i = 0; i <= targetLen; ++i) {
            for (int j = 0; j < source.length(); ++j) {
                if (target.charAt(i) != source.charAt(j)) continue main;
            }
            return i;
        }
        return -1;
    }

    static final int SINGLES = 26 + 10 + 2;
    */

    /*
    static String decode(int x) {
        if (x < SINGLES) {
            if (x < 26) return String.valueOf(x + 'A');
            if (x < 36) return String.valueOf(x - 26 + '0');
            if (x == 36) return "-";
            return " ";
        }
        if (x < binaryLimit) {
            x =
    */
}
