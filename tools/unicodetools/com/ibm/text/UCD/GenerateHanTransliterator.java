/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateHanTransliterator.java,v $
* $Date: 2002/07/30 09:56:41 $
* $Revision: 1.8 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import java.io.*;
import com.ibm.text.utility.*;

import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.Replaceable;
import com.ibm.icu.text.ReplaceableString;
import com.ibm.icu.text.UnicodeMatcher;


import java.util.*;


public final class GenerateHanTransliterator implements UCD_Types {
    
    static final boolean DISAMBIG = false;
    static final boolean DEBUG = false;
    
    static class HanInfo {
        int count = 0;
        int minLen = Integer.MAX_VALUE;
        int maxLen = Integer.MIN_VALUE;
        int sampleLen = 0;
        Set samples = new TreeSet();
        Map map = new TreeMap();
    }
    
    public static void readUnihan() throws java.io.IOException {

        log = Utility.openPrintWriter("Unihan_log.html", Utility.UTF8_WINDOWS);
        log.println("<body>");

        BufferedReader in = Utility.openUnicodeFile("Unihan", Default.ucdVersion, true, true); 
        
        Map properties = new TreeMap();
        
        Integer integerCode = new Integer(0);
        int lineCounter = 0;
        
        while (true) {
            Utility.dot(++lineCounter);
            
            String line = in.readLine();
            if (line == null) break;
            if (line.length() < 6) continue;
            if (line.charAt(0) == '#') continue;
            line = line.trim();
            
            int tabPos = line.indexOf('\t');
            String scode = line.substring(2, tabPos).trim();
            
            int code = Integer.parseInt(scode, 16);
            if (code != integerCode.intValue()) {
                integerCode = new Integer(code);
            }
            
            int tabPos2 = line.indexOf('\t', tabPos+1);
            String property = line.substring(tabPos+1, tabPos2).trim();
            
            String propertyValue = line.substring(tabPos2+1).trim();
            if (propertyValue.indexOf("U+") >= 0) propertyValue = fixHex.transliterate(propertyValue);
            
            HanInfo values = (HanInfo) properties.get(property);
            if (values == null) {
                values = new HanInfo();
                properties.put(property, values);
                Utility.fixDot();
                System.out.println("Property: " + property);
            }
            ++values.count;
            if (values.minLen > propertyValue.length()) values.minLen = propertyValue.length();
            if (values.maxLen < propertyValue.length()) values.maxLen = propertyValue.length();
            if (values.sampleLen < 150) {
                String temp = scode + ":" + propertyValue;
                values.sampleLen += temp.length() + 2;
                values.samples.add(temp);
            }
            if (property.endsWith("Variant")
                || property.endsWith("Numeric")
                || property.startsWith("kRS")
                || property.equals("kTotalStrokes")) {
                values.map.put(integerCode, propertyValue);
            }
        }
 
        Set props = properties.keySet();
        /*
        log.println("Properties");
        log.print(" ");
        Utility.print(log, props, "\r\n ");
        log.println();
        log.println();
        
        log.println("Sample Values");
        */
        Iterator it = props.iterator();
        log.println("<ol>");
        while (it.hasNext()) {
            String property = (String)it.next();
            HanInfo values = (HanInfo) properties.get(property);
            log.println("<li><b>" + property + "</b><ul><li>");
            log.println("count: " + values.count 
                + ", min length: " + values.minLen 
                + ", max length: " + values.maxLen);
            log.println("</li><li>samples:");
            Utility.print(log, values.samples, "; ");
            log.println("</li></ul></li>");
        }
        log.println("</ol>");
        
        String[] list = {"kRSJapanese", "kRSKanWa", "kRSKangXi", "kRSKorean"};
        Map kRSUnicodeMap = ((HanInfo) properties.get("kRSUnicode")).map;
        Set redundants = new HashSet();
        int unequalCount = 0;
        for (int j = 0; j < list.length; ++j) {
            unequalCount = 0;
            log.println("<p><b>Checking Redundants for " + list[j] + "</b></p><blockquote>");
            redundants.clear();
            Map otherInfo = ((HanInfo) properties.get(list[j])).map;
            it = otherInfo.keySet().iterator();
            while (it.hasNext()) {
                Integer key = (Integer) it.next();
                Object ovalue = otherInfo.get(key);
                Object uvalue = kRSUnicodeMap.get(key);
                if (ovalue.equals(uvalue)) {
                    redundants.add(key);
                } else if (++unequalCount < 5) {
                    log.println("<p>" + Integer.toString(key.intValue(),16)
                        + ": <b>" + ovalue + "</b>, " + uvalue + "</p>");
                }
            }
            log.println("</p>Total Unique: " + (otherInfo.size() - redundants.size())
                + "(out of" + otherInfo.size() + ")</p></blockquote>");
        }
        
        log.println("<p><b>Checking Redundants for kTotalStrokes</b></p><blockquote>");
        
        // pass through first to get a count for the radicals
        Map kTotalStrokesMap = ((HanInfo) properties.get("kTotalStrokes")).map;
        int[] radCount = new int[512];
        it = kRSUnicodeMap.keySet().iterator();
        while(it.hasNext()) {
            Integer key = (Integer) it.next();
            String uvalue = (String) kRSUnicodeMap.get(key);
            if (uvalue.endsWith(".0")) {
                String tvalue = (String) kTotalStrokesMap.get(key);
                if (tvalue == null) continue;
                int rs = getRadicalStroke(uvalue);
                radCount[rs>>8] = Integer.parseInt(tvalue);
            }
        }
        
        // now compare the computed value against the real value
        it = kTotalStrokesMap.keySet().iterator();
        unequalCount = 0;
        redundants.clear();
        while(it.hasNext()) {
            Integer key = (Integer) it.next();
            String uvalue = (String) kRSUnicodeMap.get(key);
            int rs = getRadicalStroke(uvalue);
            String tvalue = (String) kTotalStrokesMap.get(key);
            int t = Integer.parseInt(tvalue);
            int projected = radCount[rs>>8] + (rs & 0xFF);
            if (t == projected) {
                redundants.add(key);
            } else if (++unequalCount < 5) {
                log.println("<p>" + Integer.toString(key.intValue(),16)
                    + ": <b>" + t + "</b>, " + projected + "</p>");
            }
        }
        log.println("</p>Total Unique: " + (kTotalStrokesMap.size() - redundants.size())
                + "(out of" + kTotalStrokesMap.size() + ")</p></blockquote>");

        log.println("</body>");
        in.close();
        log.close();
    }
    
    static int getRadicalStroke(String s) {
        int dotPos = s.indexOf('.');
        int strokes = Integer.parseInt(s.substring(dotPos+1));
        int radical = 0;
        if (s.charAt(dotPos - 1) == '\'') {
            radical = 256;
            --dotPos;
        }
        radical += Integer.parseInt(s.substring(0,dotPos));
        return (radical << 8) + strokes;
    }
    
    static Transliterator fixHex = Transliterator.getInstance("hex-any/unicode");
    
    /*
    static String convertUPlus(String other) {
        int pos1 = other.indexOf("U+");
        if (pos1 < 0) return other;
        return fixHex(
        pos1 += 2;
        
        StringBuffer result = new StringBuffer();
        while (pos1 < other.length()) {
            int end = getHexEnd(s, pos1);
            result.append(UTF16.valueOf(Integer.parseInt(other.substring(pos1, end), 16)));
            pos1 = other.indexOf("U+", pos1);
            if (pos2 < 0) pos2 = other.length();
            pos1 = pos2;
        }
        return result.toString();
    }
    
    static int getHexEnd(String s, int start) {
        int i= start;
        for (; i < s.length; ++i) {
            char c = s.charAt(i);
            if ('0' <= c && c <= '9') continue;
            if ('A' <= c && c <= 'F') continue;
            if ('a' <= c && c <= 'f') continue;
            break;
        }
        return i;
    }
    */
    
    static final boolean TESTING = false;
    static int type;
    
    static final int CHINESE = 2, JAPANESE = 1, DEFINITION = 0;
    
    static final boolean DO_SIMPLE = true;
    
    public static void main(int typeIn) {
    	type = typeIn;
    	Default.setUCD();
        try {
            System.out.println("Starting");
            System.out.println("Quoting: " + quoteNonLetters.toRules(true));
            System.out.println("Quoting: " + quoteNonLetters.toRules(true));
            
            
            String key; // kMandarin, kKorean, kJapaneseKun, kJapaneseOn
            String filename;
            
            switch (type) {
                case DEFINITION:
                    key = "kDefinition"; // kMandarin, kKorean, kJapaneseKun, kJapaneseOn
                    filename = "Raw_Transliterator_Han_Latin_Definition.txt";
                    break;
                case JAPANESE: 
                    key = "kJapaneseOn";
                    filename = "Raw_Transliterator_ja_Latin.txt";
                    break;
                case CHINESE:
                    key = "kMandarin";
                    filename = "Raw_Transliterator_Han_Latin.txt";
                    break;
                default: throw new IllegalArgumentException("Unexpected option: must be 0..2");
            }
                
            err = Utility.openPrintWriter("Transliterate_err.txt", Utility.UTF8_WINDOWS);
            log = Utility.openPrintWriter("Transliterate_log.txt", Utility.UTF8_WINDOWS);
            log.print('\uFEFF');
            
            log.println();
            log.println("@*DICT Data");
            log.println();
            readCDICTDefinitions(type);
            
            log.println();
            log.println("@Unihan Data");
            log.println();
            readUnihanData(key);

            if (false) {
                readCDICT();
                compareUnihanWithCEDICT();
            }
            
            readFrequencyData(type);
            
            out = Utility.openPrintWriter(filename, Utility.UTF8_WINDOWS);
            out.println("# Start RAW data for converting CJK characters");
            /*
            out.println("# Note: adds space between them and letters.");
            out.println("{ ([:Han:]) } [:L:] > | $1 ' ';");
            out.println("[\\.\\,\\?\\!\uFF0E\uFF0C\uFF1F\uFF01\u3001\u3002[:Pe:][:Pf:]] { } [:L:] > ' ';");
            out.println("[:L:] { } [[:Han:][:Ps:][:Pi:]]> ' ';");
            
            if (type == JAPANESE) {
                out.println("$kata = [[\uFF9E\uFF9F\uFF70\u30FC][:katakana:]];");
                out.println("$kata { } [[:L:]-$kata]> ' ';");
                out.println("[[:L:]-$kata] { } $kata > ' ';");
                out.println("[:hiragana:] { } [[:L:]-[:hiragana:]] > ' ';");
                out.println("[[:L:]-[:hiragana:]] { } [:hiragana:]> ' ';");
            }
            */
            
            Set gotAlready = new HashSet();
            Iterator it = rankList.iterator();
            Set lenSet = new TreeSet();
            Set backSet = new TreeSet();
            int rank = 0;
            Map definitionCount = new HashMap();
            
            
            while (it.hasNext()) {
                String keyChar = (String) it.next();
                String def = (String) unihanMap.get(keyChar);
                if (def == null) continue; // skipping
                // sort longer definitions first!
                
                Integer countInteger = (Integer) definitionCount.get(def);
                int defCount = (countInteger == null) ? 0 : countInteger.intValue();
                String oldDef = def;
                if (DISAMBIG && (defCount != 0 || def.indexOf(' ') >= 0)) {
                    def += " " + toSub.transliterate(String.valueOf(defCount));
                }
                
                lenSet.add(new Pair(
                    new Pair(new Integer(-UTF16.countCodePoint(keyChar)), 
                        new Pair(new Integer(-def.length()), new Integer(rank++))),
                    new Pair(keyChar, def)));
                backSet.add(new Pair(
                    new Pair(new Integer(-def.toString().length()), new Integer(rank++)),
                    new Pair(keyChar, def)));
                    
                definitionCount.put(oldDef, new Integer(defCount+1));
                gotAlready.add(keyChar);
            }
            
            // add the ones that are not ranked!
            it = unihanMap.keySet().iterator();
            while (it.hasNext()) {
                String keyChar = (String) it.next();
                if (gotAlready.contains(keyChar)) continue;
                
                String def = (String) unihanMap.get(keyChar);

                Integer countInteger = (Integer) definitionCount.get(def);
                int defCount = (countInteger == null) ? 0 : countInteger.intValue();
                String oldDef = def;
                if (DISAMBIG && (defCount != 0 || def.indexOf(' ') >= 0)) {
                    def += " " + toSub.transliterate(String.valueOf(defCount));
                }
                
                lenSet.add(new Pair(
                    new Pair(new Integer(-UTF16.countCodePoint(keyChar)), 
                        new Pair(new Integer(-def.toString().length()), new Integer(rank++))),
                    new Pair(keyChar, def)));
                backSet.add(new Pair(
                    new Pair(new Integer(-def.toString().length()), new Integer(rank++)),
                    new Pair(keyChar, def)));

                definitionCount.put(oldDef, new Integer(defCount+1));
            }
            
            // First, find the ones that we want a definition for, based on the ranking
            // We might have a situation where the definitions are masked.
            // In that case, write forwards and backwards separately
            
            Set doReverse = new HashSet();
            Set gotIt = new HashSet();
            
            if (!DO_SIMPLE) {
                it = backSet.iterator();
                while (it.hasNext()) {
                    Pair p = (Pair) it.next();
                    p = (Pair) p.second;
                    
                    String keyChar = (String) p.first; 
                    String def = (String) p.second;
                    if (!gotIt.contains(def)) {
                        if (unihanNonSingular) {
                            out.println(quoteNonLetters.transliterate(keyChar)
                                + " < " + quoteNonLetters.transliterate(def) + ";");
                        } else {
                            doReverse.add(keyChar);
                        }
                    }
                    gotIt.add(def);
                }
            }
            
           
            it = lenSet.iterator();
            while (it.hasNext()) {
                Pair p = (Pair) it.next();
                p = (Pair) p.second;
                
                String keyChar = (String) p.first; 
                String def = (String) p.second;
                String rel = !DO_SIMPLE && doReverse.contains(keyChar) ? "<>" : ">";
                
                out.println(quoteNonLetters.transliterate(keyChar) + rel
                    + quoteNonLetters.transliterate(def) + "|\\ ;");
                    //if (TESTING) System.out.println("# " + code + " > " + definition);
            }
            
            out.println("\u3002 <> '.';");
            out.println("# End RAW data for converting CJK characters");
            
            /*
            if (type == JAPANESE) {
                out.println(":: katakana-latin;");
                out.println(":: hiragana-latin;");
            }
            out.println(":: fullwidth-halfwidth ();");
            */
            
            
            System.out.println("Total: " + totalCount);
            System.out.println("Defined Count: " + count);
            
            log.println();
            log.println("@Duplicates");
            log.println();
            it = duplicates.keySet().iterator();
            while (it.hasNext()) {
                String word = (String) it.next();
                log.print(hex.transliterate(word) + "\t" + word + "\t");
                Collection dups = (Collection) duplicates.get(word);
                Iterator it2 = dups.iterator();
                boolean gotFirst = false;
                while (it2.hasNext()) {
                    if (!gotFirst) gotFirst = true;
                    else log.print(", ");
                    log.print(it2.next());
                }
                log.println();
            }
            
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            if (log != null) log.close();
            if (err != null) err.close();
            if (out != null) out.close();
        }
    }
    
    static PrintWriter log;
    static PrintWriter out;
    static PrintWriter err;
    
    static int count;
    static int totalCount;
    static int oldLine;
    
    static void readFrequencyData(int type) throws java.io.IOException {
        String line = "";
        try {
            
            // chinese_frequency.txt
            // 1	çš„	1588561	1588561	3.5008%
            // japanese_frequency.txt
            // 1 ? 17176
            
            Set combinedRank = new TreeSet();
            BufferedReader br;
            int counter = 0;
            Iterator it;
            
            if (type == CHINESE) {
                System.out.println("Reading chinese_frequency.txt");
                br = Utility.openReadFile(BASE_DIR + "dict\\chinese_frequency.txt", true);
                counter = 0;
                while (true) {
                    line = Utility.readDataLine(br);
                    if (line == null) break;
                    if (line.length() == 0) continue;
                    Utility.dot(counter++);
                    int tabPos = line.indexOf('\t');
                    int rank = Integer.parseInt(line.substring(0,tabPos));
                    int cp = line.charAt(tabPos+1);
                    //if ((rank % 100) == 0) System.out.println(rank + ", " + Utility.hex(cp));
                    combinedRank.add(new Pair(new Integer(rank), UTF16.valueOf(cp)));
                }
                br.close();
            }
            
            if (type == JAPANESE) {
                System.out.println("Reading japanese_frequency.txt");
         
                br = Utility.openReadFile( BASE_DIR + "dict\\japanese_frequency.txt", true);
                Map japaneseMap = new HashMap();
                while (true) {
                    line = Utility.readDataLine(br);
                    if (line == null) break;
                    if (line.length() == 0) continue;
                    Utility.dot(counter++);
                    int tabPos = line.indexOf(' ');
                    
                    int tabPos2 = line.indexOf(' ', tabPos+1);
                    int freq = Integer.parseInt(line.substring(tabPos2+1));
                    
                    for (int i = tabPos+1; i < tabPos2; ++i) {
                        int cp = line.charAt(i);
                        int script = Default.ucd.getScript(cp);
                        if (script != HAN_SCRIPT) {
                            if (script != HIRAGANA_SCRIPT && script != KATAKANA_SCRIPT) {
                                System.out.println("Huh: " + Default.ucd.getCodeAndName(cp));
                            }
                            continue;
                        }
                        // if ((rank % 100) == 0) System.out.println(rank + ", " + Utility.hex(cp));
                        Utility.addCount(japaneseMap, UTF16.valueOf(cp), -freq);
                    }
                }
                br.close();
                // get rank order japanese
                it = japaneseMap.keySet().iterator();
                int countJapanese = 0;
                while (it.hasNext()) {
                    Comparable key = (Comparable) it.next();
                    Comparable val = (Comparable) japaneseMap.get(key);
                    combinedRank.add(new Pair(new Integer(++countJapanese), key));
                }
     
            }
            
            
            int overallRank = 0;
            it = combinedRank.iterator();
            
            log.println();
            log.println("@Frequency data: Rank of Character");
            log.println();
            
            while(it.hasNext()) {
                Pair p = (Pair) it.next();
                log.println(p.first + ", " + p.second);
                Object rank = rankMap.get(p.second);
                if (rank == null) {
                    rankMap.put(p.second, new Integer(++overallRank));
                    rankList.add(p.second);
                }
            }

            log.println();
            log.println("@Frequency data: Character to Rank");
            log.println();
            
            // get full order
            it = rankList.iterator();
            while (it.hasNext()) {
                Comparable key = (Comparable) it.next();
                Comparable val = (Comparable) rankMap.get(key);
                log.println(key + ", " + val);
            }
            
        } catch (Exception e) {
            throw new ChainException("Line \"{0}\"", new String[] {line}, e);
        }
    }
    
    static void compareUnihanWithCEDICT() {
        System.out.println("@Comparing CEDICT to Unihan");
        log.println("@Comparing CEDICT to Unihan");
        Iterator it = unihanMap.keySet().iterator();
        List inCEDICT = new ArrayList();
        List inUnihan = new ArrayList();
        List inBoth = new ArrayList();
        UnicodeSet noPinyin = new UnicodeSet();
        UnicodeSet kPinyin = new UnicodeSet();
        UnicodeSet tPinyin = new UnicodeSet();
        UnicodeSet sPinyin = new UnicodeSet();
        
        for (int i = 0; i < 0x10FFFF; ++i) {
            if (!Default.ucd.isAllocated(i)) continue;
            if (Default.ucd.getScript(i) != HAN_SCRIPT) continue;
            Utility.dot(i);
            
            String ch = UTF16.valueOf(i);
            
            String pinyin = (String) unihanMap.get(ch);
            if (pinyin == null) {
                String ch2 = Default.nfkd.normalize(ch);
                pinyin = (String) unihanMap.get(ch2);
                if (pinyin != null) {
                    addCheck(ch, pinyin, "n/a");
                    kPinyin.add(i);
                } else {
                    String trial = (String) simplifiedToTraditional.get(ch2);
                    if (trial != null) {
                        pinyin = (String) unihanMap.get(trial);
                        if (pinyin != null) {
                            addCheck(ch, pinyin, "n/a");
                            tPinyin.add(i);
                        } else {
                            trial = (String) traditionalToSimplified.get(ch2);
                            if (trial != null) {
                                pinyin = (String) unihanMap.get(trial);
                                if (pinyin != null) {
                                    addCheck(ch, pinyin, "n/a");
                                    sPinyin.add(i);
                                }
                            }
                        }
                    }
                }
            }
            Map pinyinSet = (Map) cdict.get(ch);
            if (pinyin == null) {
                if (pinyinSet != null) inCEDICT.add(ch + " => " + pinyinSet);
                noPinyin.add(i);
            } else if (pinyinSet == null) {
                inUnihan.add(ch + " => " + pinyin);
            } else {
                Object temp = pinyinSet.get(pinyin);
                if (temp == null) {
                    inBoth.add(ch + " => " + pinyin + "; " + pinyinSet);
                }
            }
        }
        
        log.println("@In CEDICT but not Unihan: ");
        printCollection(log, inCEDICT);
        
        log.println("@In Unihan but not CEDICT: ");
        printCollection(log, inUnihan);
        
        log.println("@In Unihan and CEDICT, but different: ");
        printCollection(log, inBoth);
        
        log.println("@Missing from Unihan: ");
        log.println(noPinyin.toPattern(true));
        
        log.println("@Has mapping if we NFKD it: ");
        log.println(kPinyin.toPattern(true));
        
        log.println("@Has mapping if we NFKC & simp-trad it: ");
        log.println(tPinyin.toPattern(true));
        
        log.println("@Has mapping if we NFKC & trad-simp it: ");
        log.println(sPinyin.toPattern(true));
        
        log.println("@Done comparison");
    }
    
    static void printCollection(PrintWriter p, Collection c) {
        Iterator it = c.iterator();
        int count = 0;
        while (it.hasNext()) {
            p.println((++count) + "\t" + it.next());
        }
    }
        
    
    static Map rankMap = new TreeMap(); // maps from single char strings to overall rank
    static List rankList = new ArrayList(10000);
    
    // form: ???? [ai4 wu1 ji2 wu1] /love me/love my dog/
    
    static void readCDICTDefinitions(int type) throws IOException {
        String fname = "cdict.txt";
        if (type == JAPANESE) fname = "edict.txt";
        
        System.out.println("Reading " + fname);
        BufferedReader br = Utility.openReadFile(BASE_DIR + "dict\\" + fname, true);
        int counter = 0;
        String[] pieces = new String[50];
        String line = "";
        String definition;
        try {
            while (true) {
                line = Utility.readDataLine(br);
                if (line == null) break;
                if (line.length() == 0) continue;
                Utility.dot(counter++);
                
                
                int pinyinStart = line.indexOf('[');
                int pinyinEnd = line.indexOf(']', pinyinStart+1);
                int defStart = line.indexOf('/', pinyinEnd+1);
                int defEnd = line.indexOf('/', defStart+1);
                
                int firstData = pinyinStart >= 0 ? pinyinStart : defStart;
                
                String word = line.substring(0,firstData).trim();
                
                if (type == DEFINITION) {
                    definition = fixDefinition(line.substring(defStart+1, defEnd), line);
                    addCheck(word, definition, line);
                } else if (pinyinStart >= 0) {
                    definition = line.substring(pinyinStart+1, pinyinEnd).trim();
                    if (type == JAPANESE) {
                        processEdict(word, definition, line);
                    } else {
                        definition = convertPinyin.transliterate(definition);
                        //definition = Utility.replace(definition, " ", "\\ ");
                        addCheck(word, definition, line);
                    }
                }
            }
            br.close();
        } catch (Exception e) {
            throw new ChainException("{0} Failed at {1}" , new Object []{new Integer(counter), line}, e);
        }
    }
    
    static void processEdict(String word, String definition, String line) {
        // We have a situation where we have words of the form CCCHHHKKKCCHHCCH > HHHHHHKKKHHHHHHHH
        // C = CJK, H = Hiragana, K = katakana
        
        // We want to break those up into the following rules.
        // { CCC } HHHKKKCCCHH => HHH
        // CCCHHHKKK { CC } HHCCH => HH
        // CCCHHHKKKCCHH { CC } H => HH
        
        int[] offset = {0};
        int[] offset2 = {0};        
        int[][] pairList = new int[50][2];
        int pairCount = 0;
        
        // first gather the information as to where the CJK blocks are
        // do this all at once, so we can refer to stuff ahead of us
        while (true) {
            // find next CJK block
            // where CJK really means anything but kana
            int type = find(word, kana, offset, offset2, word.length(), false, false);
            if (type == UnicodeMatcher.U_MISMATCH) break; // we are done.
            pairList[pairCount][0] = offset[0];
            pairList[pairCount++][1] = offset2[0];
            offset[0] = offset2[0]; // get ready for the next one
        }
        
        // IF we only got one CJK block, and it goes from the start to the end, then just do it.
        
        if (pairCount == 1 && pairList[0][0] == 0 && pairList[0][1] == word.length()) {
            addCheck(word, kanaToLatin.transliterate(definition), line);
            return;
        }
        
        // IF we didn't find any Kanji, bail.
        
        if (pairCount < 1) {
            System.out.println("No Kanji on line, skipping");
            System.out.println(hex.transliterate(word) + " > " + hex.transliterate(definition)
                + ", " + kanaToLatin.transliterate(definition));
            return;
        }
            
        // Now generate the rules
        
        
        if (DEBUG && pairCount > 1) {
            System.out.println("Paircount: " + pairCount);
            System.out.println("\t" + hex.transliterate(word) + " > " + hex.transliterate(definition) + ", " + kanaToLatin.transliterate(definition));
        }
        
        pairList[pairCount][0] = word.length(); // to make the algorithm easier, we add a termination
        int delta = 0; // the current difference in positions between the definition and the word
        
        for (int i = 0; i < pairCount; ++i) {
            int start = pairList[i][0];
            int limit = pairList[i][1];
            if (DEBUG && pairCount > 1) System.out.println(start + ", " + limit + ", " + delta);
            
            // that part was easy. the hard part is figuring out where this corresponds to in the definition.
            // For now, we use a simple mechanism.
            
            // The word and the definition should match to this point, so we just use the start (offset by delta)
            // We'll check just to be sure.
            
            int lastLimit = i == 0 ? 0 : pairList[i-1][1];
            
            int defStart = start + delta;
            
            String defPrefix = definition.substring(0, defStart);
            String wordInfix = word.substring(lastLimit, start);
            
            boolean firstGood = defPrefix.endsWith(wordInfix);
            if (!firstGood) {
                String wordInfix2 = katakanatoHiragana.transliterate(wordInfix);
                firstGood = defPrefix.endsWith(wordInfix2);
            }
            if (!firstGood) {
                // Houston, we have a problem.
                Utility.fixDot();
                System.out.println("Suspect line: " + hex.transliterate(word) + " > " + hex.transliterate(definition)
                    + ", " + kanaToLatin.transliterate(definition));
                System.out.println("\tNo match for " + hex.transliterate(word.substring(lastLimit, start)) 
                    + " at end of " + hex.transliterate(definition.substring(0, defStart)));
                break; // BAIL
            }
            
            // For the limit of the defintion, we get the intermediate portion of the word
            // then search for it in the definition.
            // We could get tripped up if the end of the transliteration of the Kanji matched the start.
            // If so, we should find out on the next pass.
            
            int defLimit;
            if (limit == word.length()) {
                defLimit = definition.length();
            } else {
                String afterPart = word.substring(limit, pairList[i+1][0]);
                defLimit = definition.indexOf(afterPart, defStart+1); // we assume the CJK is at least one!
                if (defLimit < 0) {
                    String afterPart2 = katakanatoHiragana.transliterate(afterPart);
                    defLimit = definition.indexOf(afterPart2, defStart+1); // we assume the CJK is at least one!
                }
                
                if (defLimit < 0) {
                    // Houston, we have a problem.
                    Utility.fixDot();
                    System.out.println("Suspect line: " + hex.transliterate(word) + " > " + hex.transliterate(definition)
                        + ", " + kanaToLatin.transliterate(definition));
                    System.out.println("\tNo match for " + hex.transliterate(afterPart) 
                        + " in " + hex.transliterate(definition.substring(0, defStart+1)));
                }
                break;
            }
            
            String defPart = definition.substring(defStart, defLimit);
            defPart = kanaToLatin.transliterate(defPart);
            
            // FOR NOW, JUNK the context before!!
            // String contextWord = word.substring(0, start) + "{" + word.substring(start, limit) + "}" + word.substring(limit);
            String contextWord = word.substring(start, limit);
            if (limit != word.length()) contextWord += "}" + word.substring(limit);
            
            addCheck(contextWord, defPart, line);
            if (DEBUG && pairCount > 1) System.out.println("\t" + hex.transliterate(contextWord) + " > " + hex.transliterate(defPart));
            
            delta = defLimit - limit;
        }
        
    }
    
    // Useful Utilities?
    
    /** 
     * Returns the start of the first substring that matches m.
     * Most arguments are the same as UnicodeMatcher.matches, except for offset[]
     * @positive Use true if you want the first point that matches, and false if you want the first point that doesn't match.
     * @offset On input, the starting position. On output, the start of the match position (not the end!!)
     */
    static int find(Replaceable s, UnicodeMatcher m, int[] offset, int limit, boolean incremental, boolean positive) {
        int direction = offset[0] <= limit ? 1 : -1;

        
        while (offset[0] != limit) {
            int original = offset[0];
            int type = m.matches(s, offset, limit, incremental); // if successful, changes offset.
            if (type == UnicodeMatcher.U_MISMATCH) {
                if (!positive) {
                    return UnicodeMatcher.U_MATCH;
                }
                offset[0] += direction;  // used to skip to next code unit, in the positive case
                // !! This should be safe, and saves checking the length of the code point
            } else if (positive) {
                offset[0] = original; // reset to the start position!!!
                return type;
            }
        }
        return UnicodeMatcher.U_MISMATCH;
    }
    
    /** 
     * Returns the start/limit of the first substring that matches m. Most arguments are the same as find().<br>
     * <b>Warning:</b> if the search is backwards, then substringEnd will contain the <i>start</i> of the substring
     * and offset will contain the </i>limit</i> of the substring.
     */
    static int find(Replaceable s, UnicodeMatcher m, int[] offset, int[] offset2, int limit, boolean incremental, boolean positive) {
        int type = find(s, m, offset, limit, incremental, positive);
        if (type == UnicodeMatcher.U_MISMATCH) return type;
        offset2[0] = offset[0];
        int type2 = find(s, m, offset2, limit, incremental, !positive);
        return type;
    }
    
    static int find(String ss, UnicodeMatcher m, int[] offset, int limit, boolean incremental, boolean positive) {
        // UGLY that we have to create a wrapper!
        return find(new ReplaceableString(ss), m, offset, limit, incremental, positive);
    }
    
    static int find(String ss, UnicodeMatcher m, int[] offset, int[] offset2, int limit, boolean incremental, boolean positive) {
        // UGLY that we have to create a wrapper!
        return find(new ReplaceableString(ss), m, offset, offset2, limit, incremental, positive);
    }
    
    static UnicodeSet pua = new UnicodeSet("[:private use:]");
    static UnicodeSet numbers = new UnicodeSet("[0-9]");
    
    static void addCheck(String word, String definition, String line) {
        int lastSlash = 0;
        while (lastSlash < word.length()) {
            int wordSlash = word.indexOf('/', lastSlash);
            if (wordSlash < 0) wordSlash = word.length();
            addCheck2(word.substring(lastSlash, wordSlash), definition, line);
            lastSlash = wordSlash + 1;
        }
    }
    
    static void addCheck2(String word, String definition, String line) {
        definition = Default.nfc.normalize(definition);
        word = Default.nfc.normalize(word);
        if (DO_SIMPLE && UTF16.countCodePoint(word) > 1) return;
        
        if (pua.containsSome(word) ) {
            Utility.fixDot();
            System.out.println("PUA on: " + line);
        } else if (numbers.containsAll(definition) ) {
            Utility.fixDot();
            System.out.println("Only numbers on: " + line);
        } else {
            Object alreadyThere = unihanMap.get(word);
            if (alreadyThere == null) {
                unihanMap.put(word, definition);
            } else if (!definition.equals(alreadyThere)) {
                Utility.addToList(duplicates, word, alreadyThere, true);
                Utility.addToList(duplicates, word, definition, true);
            }
        }
        if (UTF16.countCodePoint(word) > 1) unihanNonSingular = true;
    }
    
    static void readCDICT() throws IOException {
        System.out.println("Reading cdict.txt");
        BufferedReader br = Utility.openReadFile(BASE_DIR + "dict\\cdict.txt", true);
        int counter = 0;
        String[] pieces = new String[50];
        String line = "";
        
        try {
            while (true) {
                line = Utility.readDataLine(br);
                if (line == null) break;
                if (line.length() == 0) continue;
                Utility.dot(counter++);
                int tabPos = line.indexOf('[');
                String word = line.substring(0,tabPos).trim();
                word = Utility.replace(word, "\uFE4D", "");
                word = Utility.replace(word, ".", "");
                word = Utility.replace(word, "/", "");
                word = Utility.replace(word, "(", "");
                word = Utility.replace(word, ")", "");
               
                
                int tab2Pos = line.indexOf(']', tabPos+1);
                String pinyins = line.substring(tabPos+1, tab2Pos);
                int len = Utility.split(pinyins, ' ', pieces);
                if (word.length() != len) {
                    log.println("Len mismatch: " + line);
                    continue;
                }
                for (int i = 0; i < len; ++i) {
                    String chr = word.substring(i, i+1);
                    String piece = convertPinyin.transliterate(pieces[i]);
                    Map oldMap = (Map) cdict.get(chr);
                    if (oldMap == null) {
                        oldMap = new TreeMap();
                        cdict.put(chr, oldMap);
                    }
                    /*&& !oldMap.equals(piece)) {
                        log.println("Variant for '" + chr + "', new: '" + piece + "', old: '" + oldMap + "'");
                    }
                    */
                    Utility.addCount(oldMap, piece, 1);
                }
            }
            br.close();
            
            Iterator it = cdict.keySet().iterator();
            Set tempSet = new TreeSet();
            while (it.hasNext()) {
                Object key = it.next();
                Map val = (Map) cdict.get(key);
                log.print(key + ": ");
                Iterator it2 = val.keySet().iterator();
                tempSet.clear();
                while (it2.hasNext()) {
                    Comparable key2 = (Comparable) it2.next();
                    Comparable count = (Comparable) val.get(key2);
                    Pair p = new Pair(count, key2);
                    tempSet.add(p); // reverse the order
                }
                it2 = tempSet.iterator();
                int counter2 = 0;
                while (it2.hasNext()) {
                    if (counter2++ != 0) log.print("/");
                    log.print(it2.next());
                }
                log.println();
            }
            
        } catch (Exception e) {
            throw new ChainException("{0} Failed at {1}" , new Object []{new Integer(counter), line}, e);
        }
    }
    
    static Map cdict = new TreeMap();
    static Map simplifiedToTraditional = new HashMap();
    static Map traditionalToSimplified = new HashMap();
  
    static void readUnihanData(String key) throws java.io.IOException {

        BufferedReader in = Utility.openUnicodeFile("Unihan", Default.ucdVersion, true, true); 

        int count = 0;
        int lineCounter = 0;
        
        while (true) {
            Utility.dot(++lineCounter);
            
            String line = in.readLine();
            if (line == null) break;
            if (line.length() < 6) continue;
            if (line.charAt(0) == '#') continue;
            line = line.trim();
            
            int tabPos = line.indexOf('\t');
            int tabPos2 = line.indexOf('\t', tabPos+1);
            
            String scode = line.substring(2, tabPos).trim();
            
            int code = Integer.parseInt(scode, 16);            
            String property = line.substring(tabPos+1, tabPos2).trim();
            
            String propertyValue = line.substring(tabPos2+1).trim();
            if (propertyValue.indexOf("U+") >= 0) propertyValue = fixHex.transliterate(propertyValue);
            
            // gather traditional mapping
            if (property.equals("kTraditionalVariant")) {
                simplifiedToTraditional.put(UTF16.valueOf(code), propertyValue);
            }
            
            if (property.equals("kSimplifiedVariant")) {
                traditionalToSimplified.put(UTF16.valueOf(code), propertyValue);
            }
            
            if (property.equals(key) || key.equals("kJapaneseOn") && property.equals("kJapaneseKun")) {
                storeDef(out, code, propertyValue, line);
            }            
        }
        
        in.close();
    }
    
    static void storeDef(PrintWriter out, int cp, String rawDefinition, String line) {
        // skip spaces & numbers at start
        int start;
        for (start = 0;start < rawDefinition.length(); ++start) {
            char ch = rawDefinition.charAt(start);
            if (ch != ' ' && ch != '\t' && (ch < '0' || ch > '9')) break;
        }

        // go up to comma or semicolon, whichever is earlier
        int end = rawDefinition.indexOf(";", start);
        if (end < 0) end = rawDefinition.length();
        
        int end2 = rawDefinition.indexOf(",", start);
        if (end2 < 0) end2 = rawDefinition.length();
        if (end > end2) end = end2;
  
        // IF CHINESE or JAPANESE, stop at first space!!!
        rawDefinition = rawDefinition.substring(start,end);
        
        if (type == DEFINITION) {
            storeDef2(out, cp, rawDefinition, line);
        } else {
            if (rawDefinition.indexOf(' ') < 0) storeDef2(out, cp, rawDefinition, line);
            else {
                String [] pieces = Utility.split(rawDefinition, ' ');
                for (int i = 0; i < pieces.length; ++i) {
                    storeDef2(out, cp, pieces[i], line);
                }
            }
        }
    }
    
    static void storeDef2(PrintWriter out, int cp, String definition, String line) {
        if (type == CHINESE) {
            // since data are messed up, terminate after first digit
            int end3 = findInString(definition, "12345")+1;
            if (end3 == 0) {
                log.println("Bad pinyin data: " + hex.transliterate(UTF16.valueOf(cp))
                    + "\t" + UTF16.valueOf(cp) + "\t" + definition);
                end3 = definition.length();
            }
            definition = definition.substring(0, end3);
            
            definition = convertPinyin.transliterate(definition);
        }
        if (type == DEFINITION) {
            definition = removeMatched(definition,'(', ')', line);
            definition = removeMatched(definition,'[', ']', line);
            definition = fixDefinition(definition, line);
        }
        definition = definition.trim();
        definition = Default.ucd.getCase(definition, FULL, LOWER);

        if (definition.length() == 0) {
            Utility.fixDot();
            System.out.println("Zero value for " + Default.ucd.getCode(cp) + " on: " + hex.transliterate(line));
        } else {
            addCheck(UTF16.valueOf(cp), definition, line);
        }
        /*
        String key = (String) unihanMap.get(definition);
        if (key == null) {
            unihanMap.put(definition, cp);
        }
        out.println(cp + (key == null ? " <> " : " > ") + Default.ucd.getCase(definition, FULL, TITLE) + ";");
        if (TESTING) System.out.println("# " + code + " > " + definition);
        */
    }
    
    static String fixDefinition(String definition, String rawDefinition) {
        definition = definition.trim();
        definition = Utility.replace(definition, "  ", " ");
        definition = Utility.replace(definition, " ", "-");
        definition = Default.ucd.getCase(definition, FULL, LOWER);
        return definition;
    }
    
    
    // WARNING not supplemenatary-safe!
    
    static int findInString(String source, String chars) {
        for (int i = 0; i < source.length(); ++i) {
            if (chars.indexOf(source.charAt(i)) >= 0) return i;
        }
        return -1;
    }
        
    // WARNING not supplemenatary-safe!
    
    static String removeMatched(String source, char start, char end, String originalLine) {
        while (true) {
            int pos = source.indexOf(start);
            if (pos < 0) break;
            int epos = source.indexOf(end, pos+1);
            if (epos < 0) {
                epos = source.length()-1;
                log.println("Mismatches with " + start + ", " + end + ": " + originalLine);
            }
            source = source.substring(0,pos) + source.substring(epos+1);
        }
        return source;
    }
        
    static Map unihanMap = new HashMap();
    static Map duplicates = new TreeMap();
    
    static boolean unihanNonSingular = false;
    
    static StringBuffer handlePinyinTemp = new StringBuffer();
    
    static final Transliterator hex = Transliterator.getInstance("[^\\u0020-\\u007F] hex");
    static final Transliterator quoteNonLetters = Transliterator.createFromRules("any-quotenonletters", 
          "([[\\u0020-\\u007E]-[:L:]-[\\'\\{\\}]-[0-9]]) > \\u005C $1; "
        + "\\' > \\'\\';",
        Transliterator.FORWARD);
    static final Transliterator toSub = Transliterator.createFromRules("any-subscript", 
            " 0 > \u2080; "
          + " 1 > \u2081; "
          + " 2 > \u2082; "
          + " 3 > \u2084; "
          + " 4 > \u2084; "
          + " 5 > \u2085; "
          + " 6 > \u2086; "
          + " 7 > \u2087; "
          + " 8 > \u2088; "
          + " 9 > \u2089; ",
        Transliterator.FORWARD);
    
    static final Transliterator kanaToLatin = Transliterator.createFromRules("any-subscript", 
            " $kata = [[:katakana:]\u30FC]; "
          + "[:hiragana:] {} [:^hiragana:] > ' '; "
          + "$kata {} [^[:hiragana:]$kata] > ' '; "  
          + "::Katakana-Latin; "
          + "::Hiragana-Latin;",
        Transliterator.FORWARD);
        
    static final Transliterator katakanatoHiragana = Transliterator.getInstance("katakana-hiragana");        
    
    static final UnicodeSet kana = new UnicodeSet("[[:hiragana:][:katakana:]\u30FC]");
    // since we are working in NFC, we don't worry about the combining marks.
            
    // ADD Factory since otherwise getInverse blows out
    static class DummyFactory implements Transliterator.Factory {
        static DummyFactory singleton = new DummyFactory();
        static HashMap m = new HashMap();

        // Since Transliterators are immutable, we don't have to clone on set & get
        static void add(String ID, Transliterator t) {
            m.put(ID, t);
            System.out.println("Registering: " + ID + ", " + t.toRules(true));
            Transliterator.registerFactory(ID, singleton);
        }
        public Transliterator getInstance(String ID) {
            return (Transliterator) m.get(ID);
        }
    }
    
    static Transliterator convertPinyin;
    
    static {
        String dt = "1 > ;\n"
                    + "2 <> \u0301;\n"
                    + "3 <> \u0306;\n"
                    + "4 <> \u0300;\n"
                    + "5 <> \u0304;";
        
        String dp = "# syllable is ...vowel+ consonant* number\n"
                    + "# 'a', 'e' are the preferred bases\n"
                    + "# otherwise 'o'\n"
                    + "# otherwise last vowel\n"
                    + "::NFC;\n"
                    + "$vowel = [aAeEiIoOuUüÜ];\n"
                    + "$consonant = [[a-z A-Z] - [$vowel]];\n"
                    + "$digit = [1-5];\n"
                    + "([aAeE]) ($vowel* $consonant*) ($digit) > $1 &digit-tone($3) $2;\n"
                    + "([oO]) ([$vowel-[aeAE]]* $consonant*) ($digit) > $1 &digit-tone($3) $2;\n"
                    + "($vowel) ($consonant*) ($digit) > $1 &digit-tone($3) $2;\n"
                    + "($digit) > &digit-tone($1);\n"
                    + "::NFC;\n";
 
    	Transliterator at = Transliterator.createFromRules("digit-tone", dt, Transliterator.FORWARD);
    	System.out.println(at.transliterate("a1a2a3a4a5"));
    	DummyFactory.add(at.getID(), at);
    	
    	convertPinyin = Transliterator.createFromRules("digit-pinyin", dp, Transliterator.FORWARD);
    	System.out.println(convertPinyin.transliterate("an2 aon2 oan2 ion2 oin2 uin2 iun2"));
    
    }
    /*
    
    static String convertTones(String source, String debugLine) {
        try {
            result = new StringBuffer();
            main:
            for (int i = 0; i < source.length(); ++i) {
                ch = source.charAt(i);
                switch (ch) {
                    case ':': 
                        if (i > 0) {
                            char last = result.charAt(result.length()-1);
                            if (last == 'u') {
                                result.setCharAt(result.length()-1, 'ü');
                                continue main;
                            } else if (last == 'U') {
                                result.setCharAt(result.length()-1, 'Ü');
                                continue main;
                            }
                        }
                        break;
                    case '1': break; // skip character
                    case '2': case '3': case '4': case '5':
                        applyToPrecedingBase(result, ch-'0');
                        break;
                    default:
                        result.append(ch);
                        break;
                }
            }
        }
            
                        
        source = source.trim();
            char ch = source.charAt(source.length()-1);
            int num = (int)(ch-'1');
            if (num < 0 || num > 5) throw new Exception("none");
            handlePinyinTemp.setLength(0);
            boolean gotIt = false;
            boolean messageIfNoGotIt = true;
            
            for (int i = source.length()-2; i >= 0; --i) {
                ch = source.charAt(i);
                if (ch == ':') {
                    ch = 'Ü';
                    --i;
                }
                if ('0' <= ch && ch <= '9') break;
                if (ch != 'Ü' && (ch < 'A' || ch > 'Z')) {
                    Utility.fixDot();
                    System.out.println("Warning: non-ASCII in " + hex.transliterate(source) + " (" + hex.transliterate(debugLine) + ")");
                    break;
                }
                if (!gotIt) switch (ch) {
                    case 'A': ch = "AÁ\u0102À\u0100".charAt(num); gotIt = true; break;
                    case 'E': ch = "EÉ\u0114È\u0112".charAt(num); gotIt = true; break;
                    case 'I': ch = "IÍ\u012CÌ\u012A".charAt(num); gotIt = true; break;
                    case 'O': ch = "OÓ\u014EÒ\u014C".charAt(num); gotIt = true; break;
                    case 'U': ch = "UÚ\u016CÙ\u016A".charAt(num); gotIt = true; break;
                    case 'Ü': ch = "Ü\u01D7\u01D9\u01DB\u01D5".charAt(num); gotIt = true; break;
                }
                handlePinyinTemp.insert(0,ch);
            }
            if (!gotIt && num > 0) {
                handlePinyinTemp.append(" \u0301\u0306\u0300\u0304".charAt(num));
                if (messageIfNoGotIt) {
                    err.println("Missing vowel?: " + debugLine + " -> " + handlePinyinTemp
                    .toString());
                }
            }
            source = handlePinyinTemp.toString().toLowerCase();
        } catch (Exception e) {
            log.println("Bad line: " + debugLine);
        }
        return source;
    }
    
/*
A and e trump all other vowels and always take the tone mark.
There are no Mandarin syllables that contain both a and e. 
In the combination ou, o takes the mark. 
In all other cases, the final vowel takes the mark. 
*/
/*
    static String applyToPrecedingBase(StringBuffer result, int tone) {
        for (int i = result.length()-1; i >= 0; --i) {
            char ch = result.charAt(i);
            switch (ch) {
                case 'a': case 'e': case 'A': case 'E':
                    result.setCharAt(i, mapTone(ch, tone));
                    return;
                case 'o': case 'O': bestSoFar = i; break;
                case 'i': case 'I': case 'u': case 'U': case '
        if (tone == 1) return String.valueOf(ch);
        return Default.nfc.normalize(ch + mapTone[tone]);
    }
    
    static final char[] MAP_TONE = {"\u0301", "\u0306", "\u0300", "\u0304"};
    */
}