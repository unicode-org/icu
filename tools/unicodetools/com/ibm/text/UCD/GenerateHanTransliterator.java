/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateHanTransliterator.java,v $
* $Date: 2002/06/13 21:14:05 $
* $Revision: 1.5 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import java.io.*;
import com.ibm.text.utility.*;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UTF16;
import java.util.*;


public final class GenerateHanTransliterator implements UCD_Types {
    
    static final boolean TESTING = false;
    static int type;
    
    static final int CHINESE = 2, JAPANESE = 1, DEFINITION = 0;
    
    public static void main(int typeIn) {
    	type = typeIn;
    	Default.setUCD();
        try {
            System.out.println("Starting");
            log = Utility.openPrintWriter("Transliterate_log.txt", false, false);
            err = Utility.openPrintWriter("Transliterate_err.txt", false, false);
            log.print('\uFEFF');
            
            String key; // kMandarin, kKorean, kJapaneseKun, kJapaneseOn
            String filter; // "kJis0";
            String filename;
            
            switch (type) {
                case DEFINITION:
                    key = "kDefinition"; // kMandarin, kKorean, kJapaneseKun, kJapaneseOn
                    filter = null; // "kJis0";
                    filename = "Transliterator_Han_Latin_Definition.txt";
                    break;
                case JAPANESE: 
                    key = "kJapaneseOn";
                    filter = null; // "kJis0";
                    filename = "Transliterator_ja_Latin.txt";
                    break;
                case CHINESE:
                    key = "kMandarin";
                    filename = "Transliterator_Han_Latin.txt";
                    filter = null;
                    break;
                default: throw new IllegalArgumentException("Unexpected option: must be 0..2");
            }
                
            readUnihanData(key, filter);
            
            if (false) {
                readCDICT();
                compareUnihanWithCEDICT();
            }
            
            readFrequencyData();
            
            out = Utility.openPrintWriter(filename, false, false);
            out.println("# Convert CJK characters");
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
                        
            Set gotAlready = new HashSet();
            Iterator it = rankList.iterator();
            Set lenSet = new TreeSet();
            int rank = 0;
            while (it.hasNext()) {
                Comparable keyChar = (Comparable) it.next();
                Comparable def = (Comparable) unihanMap.get(keyChar);
                if (def == null) continue; // skipping
                // sort longer definitions first!
                lenSet.add(new Pair(
                    new Pair(new Integer(-def.toString().length()), new Integer(rank++)),
                    new Pair(keyChar, def)));
                gotAlready.add(keyChar);
            }
            
            // add the ones that are not ranked!
            it = unihanMap.keySet().iterator();
            while (it.hasNext()) {
                Comparable keyChar = (Comparable) it.next();
                Comparable def = (Comparable) unihanMap.get(keyChar);
                if (!gotAlready.contains(keyChar)) {
                    lenSet.add(new Pair(
                        new Pair(new Integer(-def.toString().length()), new Integer(rank++)),
                        new Pair(keyChar, def)));
                }
            }
                    
            Set gotIt = new HashSet();
            it = lenSet.iterator();
            while (it.hasNext()) {
                Pair p = (Pair) it.next();
                p = (Pair) p.second;
                
                Comparable keyChar = p.first; 
                Comparable def = p.second;
                String rel = gotIt.contains(def) ? " > " : " <> ";
                out.println(keyChar + rel + def + ";");
                //if (TESTING) System.out.println("# " + code + " > " + definition);
                gotIt.add(def);
            }
            
            out.println("\u3002 <> '.';");
            if (type == JAPANESE) {
                out.println(":: katakana-latin;");
                out.println(":: hiragana-latin;");
            }
            out.println(":: fullwidth-halfwidth;");

            
            
            System.out.println("Total: " + totalCount);
            System.out.println("Defined Count: " + count);
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            if (log != null) log.close();
            if (out != null) out.close();
            if (err != null) err.close();
        }
    }
    
    static PrintWriter log;
    static PrintWriter out;
    static PrintWriter err;
    
    static int count;
    static int totalCount;
    static int oldLine;
    
    static void readFrequencyData() throws java.io.IOException {
        String line = "";
        try {
            
            // chinese_frequency.txt
            // 1	çš„	1588561	1588561	3.5008%
            // japanese_frequency.txt
            // 1 ? 17176
            
            Set combinedRank = new TreeSet();
            
            System.out.println("Reading chinese_frequency.txt");
            BufferedReader br = Utility.openReadFile(BASE_DIR + "dict\\chinese_frequency.txt", true);
            int counter = 0;
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
            Iterator it = japaneseMap.keySet().iterator();
            int countJapanese = 0;
            while (it.hasNext()) {
                Comparable key = (Comparable) it.next();
                Comparable val = (Comparable) japaneseMap.get(key);
                combinedRank.add(new Pair(new Integer(++countJapanese), key));
            }
 
            
            int overallRank = 0;
            it = combinedRank.iterator();
            
            while(it.hasNext()) {
                Pair p = (Pair) it.next();
                log.println(p.first + ", " + p.second);
                Object rank = rankMap.get(p.second);
                if (rank == null) {
                    rankMap.put(p.second, new Integer(++overallRank));
                    rankList.add(p.second);
                }
            }

            log.println("@character to rank");
            
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
                    unihanMap.put(ch, pinyin);
                    kPinyin.add(i);
                } else {
                    String trial = (String) simplifiedToTraditional.get(ch2);
                    if (trial != null) {
                        pinyin = (String) unihanMap.get(trial);
                        if (pinyin != null) {
                            unihanMap.put(ch, pinyin);
                            tPinyin.add(i);
                        } else {
                            trial = (String) traditionalToSimplified.get(ch2);
                            if (trial != null) {
                                pinyin = (String) unihanMap.get(trial);
                                if (pinyin != null) {
                                    unihanMap.put(ch, pinyin);
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
  
    static void readUnihanData(String key, String filter) throws java.io.IOException {

        BufferedReader in = Utility.openUnicodeFile("Unihan", Default.ucdVersion, true, true); 

        int count = 0;
        String oldCode = "";
        String oldLine = "";
        int oldStart = 0;
        boolean foundFilter = (filter == null);
        boolean foundKey = false;
        
        int lineCounter = 0;
        
        while (true) {
            Utility.dot(++lineCounter);
            
            String line = in.readLine();
            if (line == null) break;
            if (line.length() < 6) continue;
            if (line.charAt(0) == '#') continue;
            line = line.trim();
            
            int tabPos = line.indexOf('\t');
            String code = line.substring(2, tabPos);
            
            // gather traditional mapping
            if (line.indexOf("kTraditionalVariant") >= 0) {
                int tabPos2 = line.indexOf('\t', tabPos+1);
                int tabPos3 = line.indexOf(' ', tabPos2+1);
                if (tabPos3 < 0) tabPos3 = line.length();
                
                String code2 = line.substring(tabPos2+3, tabPos3);
                simplifiedToTraditional.put(UTF16.valueOf(Integer.parseInt(code, 16)), 
                    UTF16.valueOf(Integer.parseInt(code2, 16)));
            }
            
            if (line.indexOf("kSimplifiedVariant") >= 0) {
                int tabPos2 = line.indexOf('\t', tabPos+1);
                int tabPos3 = line.indexOf(' ', tabPos2+1);
                if (tabPos3 < 0) tabPos3 = line.length();
                
                String code2 = line.substring(tabPos2+3, tabPos3);
                traditionalToSimplified.put(UTF16.valueOf(Integer.parseInt(code, 16)), 
                    UTF16.valueOf(Integer.parseInt(code2, 16)));
            }
            
            
            
            /* if (code.compareTo("9FA0") >= 0) {
                System.out.println("? " + line);
            }*/
            if (!code.equals(oldCode)) {
            	totalCount++;
            	
                if (foundKey && foundFilter) {
                    count++;
                    /*if (true) { //*/
                    if (TESTING && (count == 1 || (count % 100) == 0)) {
                        System.out.println(count + ": " + oldLine);
                    }
                    storeDef(out, oldCode, oldLine, oldStart);
                }
                if (TESTING) if (count > 1000) {
                    System.out.println("ABORTING at 1000 for testing");
                    break;
                }
                oldCode = code;
                foundKey = false;
                foundFilter = (filter == null);
            }
            
            // detect key, filter. Must be on different lines
            if (!foundFilter && line.indexOf(filter) >= 0) {
                foundFilter = true;
            } else if (!foundKey && (oldStart = line.indexOf(key)) >= 0) {
                foundKey = true;
                oldLine = line;
                oldStart += key.length();
            }
        }
        if (foundKey && foundFilter) storeDef(out, oldCode, oldLine, oldStart);
        
        in.close();
    }
    
    static void storeDef(PrintWriter out, String code, String line, int start) {
        if (code.length() == 0) return;
        
        // skip spaces & numbers at start
        for (;start < line.length(); ++start) {
            char ch = line.charAt(start);
            if (ch != ' ' && ch != '\t' && (ch < '0' || ch > '9')) break;
        }

        // go up to comma or semicolon, whichever is earlier
        int end = line.indexOf(";", start);
        if (end < 0) end = line.length();
        
        int end2 = line.indexOf(",", start);
        if (end2 < 0) end2 = line.length();
        if (end > end2) end = end2;
  
        if (type != DEFINITION) {
            end2 = line.indexOf(" ", start);
            if (end2 < 0) end2 = line.length();
            if (end > end2) end = end2;
        }
        
        String definition = line.substring(start,end);
        if (type == CHINESE) {
            // since data are messed up, terminate after first digit
            int end3 = findInString(definition, "12345")+1;
            if (end3 == 0) {
                log.println("Bad pinyin data: " + line);
                end3 = definition.length();
            }
            definition = definition.substring(0, end3);
            
            definition = convertPinyin.transliterate(definition);
        }
        if (type == DEFINITION) {
            definition = removeMatched(definition,'(', ')', line);
            definition = removeMatched(definition,'[', ']', line);
            definition = definition.trim();
            definition = Utility.replace(definition, "  ", " ");
            definition = "'[" + quoteNonLetters.transliterate(definition) + "]'";
        }
        definition.trim();
        definition = Default.ucd.getCase(definition, FULL, LOWER);
        String cp = UTF16.valueOf(Integer.parseInt(code, 16));
        unihanMap.put(cp, definition);
        /*
        String key = (String) unihanMap.get(definition);
        if (key == null) {
            unihanMap.put(definition, cp);
        }
        out.println(cp + (key == null ? " <> " : " > ") + Default.ucd.getCase(definition, FULL, TITLE) + ";");
        if (TESTING) System.out.println("# " + code + " > " + definition);
        */
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
    
    static StringBuffer handlePinyinTemp = new StringBuffer();
    
    static Transliterator hex = Transliterator.getInstance("[^\\u0020-\\u007F] hex");
    static Transliterator quoteNonLetters = Transliterator.createFromRules("any-quotenonletters", 
        "([[\\u0021-\\u007E]-[:L:]-[\\']]) > \\u005C $1; \\' > \\'\\';", Transliterator.FORWARD);
    
    
    
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