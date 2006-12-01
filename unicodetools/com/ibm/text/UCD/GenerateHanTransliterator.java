/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateHanTransliterator.java,v $
* $Date: 2004/06/26 00:26:16 $
* $Revision: 1.16 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import java.io.*;

import com.ibm.text.utility.*;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.Replaceable;
import com.ibm.icu.text.ReplaceableString;
import com.ibm.icu.text.UnicodeMatcher;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;


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
        log.println("<head>");
        log.println("<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        log.println("<title>Unihan check</title>");
        log.println("</head>");

        BufferedReader in = Utility.openUnicodeFile("Unihan", Default.ucdVersion(), true, Utility.UTF8); 
        
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
            if (propertyValue.indexOf("U+") >= 0) propertyValue = fromHexUnicode.transliterate(propertyValue);
            
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
    
    static Transliterator fromHexUnicode = Transliterator.getInstance("hex-any/unicode");
    
    static Transliterator toHexUnicode = Transliterator.getInstance("any-hex/unicode");
    
    /*
    static String convertUPlus(String other) {
        int pos1 = other.indexOf("U+");
        if (pos1 < 0) return other;
        return fromHexUnicode(
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
    static final boolean SKIP_OVERRIDES = true;
    
    static PrintWriter out2;
    
    public static void fixedMandarin() throws IOException {
        UnicodeMap kMandarin = Default.ucd().getHanValue("kMandarin");
        UnicodeMap kHanyuPinlu = Default.ucd().getHanValue("kHanyuPinlu");
        UnicodeSet gotMandarin = kMandarin.getSet(null).complement();
        UnicodeSet gotHanyu = kHanyuPinlu.getSet(null).complement();
        UnicodeSet gotAtLeastOne = new UnicodeSet(gotMandarin).addAll(gotHanyu);
        Map outmap = new TreeMap(Collator.getInstance(new ULocale("zh")));
        for (UnicodeSetIterator it = new UnicodeSetIterator(gotAtLeastOne); it.next(); ) {
            //String code = UTF16.valueOf(it.codepoint);
            String hanyu = (String) kHanyuPinlu.getValue(it.codepoint);
            String mandarin = (String) kMandarin.getValue(it.codepoint);
            String hPinyin = hanyu == null ? null : digitPinyin_accentPinyin.transliterate(getUpTo(hanyu,'('));
            String mPinyin = mandarin == null ? null : digitPinyin_accentPinyin.transliterate(getUpTo(mandarin.toLowerCase(),' '));
            String uPinyin = hPinyin != null ? hPinyin : mPinyin;
            UnicodeSet s = (UnicodeSet) outmap.get(uPinyin);
            if (s == null) {
                s = new UnicodeSet();
                outmap.put(uPinyin, s); 
            }
            s.add(it.codepoint);
        }
        String filename = "Raw_Transliterator_Han_Latin.txt";
        PrintWriter out = BagFormatter.openUTF8Writer(UCD_Types.GEN_DIR, filename);
        for (Iterator it = outmap.keySet().iterator(); it.hasNext();) {
            String pinyin = (String) it.next();
            UnicodeSet uset = (UnicodeSet) outmap.get(pinyin);
            if (uset.size() == 1) {
                UnicodeSetIterator usi = new UnicodeSetIterator(uset);
                usi.next();
                out.println(UTF16.valueOf(usi.codepoint) + ">" + pinyin + ";");
            } else {
                out.println(uset.toPattern(false) + ">" + pinyin + ";");
            }
        }
        out.close();
    }
    
    public static class PairComparator implements Comparator {
        Comparator first;
        Comparator second;
        PairComparator(Comparator first, Comparator second) {
            this.first = first;
            this.second = second;
        }
        public int compare(Object o1, Object o2) {
            Pair p1 = (Pair)o1;
            Pair p2 = (Pair)o2;
            int result = first.compare(p1.first, p2.first);
            if (result != 0) return result;
            return second.compare(p1.second, p2.second);
        }
    }
    
    public static void quickMandarin() throws Exception {
        UnicodeMap gcl = new UnicodeMap();
        addField("C:\\DATA\\dict\\", "gcl_icu.txt", 2, 3, gcl);
        addField("C:\\DATA\\dict\\", "gcl_other.txt", 2, 5, gcl);        
        Transliterator icuPinyin = Transliterator.getInstance("han-latin");
        UnicodeMap kMandarin = Default.ucd().getHanValue("kMandarin");
        UnicodeMap kHanyuPinlu = Default.ucd().getHanValue("kHanyuPinlu");
        UnicodeSet gotMandarin = kMandarin.getSet(null).complement();
        UnicodeSet gotHanyu = kHanyuPinlu.getSet(null).complement();
        UnicodeSet gotAtLeastOne = new UnicodeSet(gotMandarin).addAll(gotHanyu);
        int counter = 0;
        int hCount = 0;
        log = Utility.openPrintWriter("Mandarin_First.txt", Utility.UTF8_WINDOWS);
        log.println("N\tCode\tChar\tUnihan\tICU\tGCL\tkHanyuPinlu / kMandarin");
        UnicodeMap reformed = new UnicodeMap();
        for (UnicodeSetIterator it = new UnicodeSetIterator(gotAtLeastOne); it.next(); ) {
            String code = UTF16.valueOf(it.codepoint);
            String hanyu = (String) kHanyuPinlu.getValue(it.codepoint);
            String mandarin = (String) kMandarin.getValue(it.codepoint);
            String hPinyin = hanyu == null ? null : digitPinyin_accentPinyin.transliterate(getUpTo(hanyu,'('));
            String mPinyin = mandarin == null ? null : digitPinyin_accentPinyin.transliterate(getUpTo(mandarin.toLowerCase(),' '));
            String uPinyin = hPinyin != null ? hPinyin : mPinyin;

            String iPinyin = icuPinyin.transliterate(code).trim();
            if (iPinyin.equals(code)) iPinyin = null;
            String gPinyin = (String) gcl.getValue(it.codepoint);
            
            if (hPinyin != null) reformed.put(it.codepoint, hPinyin);
            else if (gPinyin != null) reformed.put(it.codepoint, gPinyin);
            else if (mPinyin != null) reformed.put(it.codepoint, mPinyin);
            else if (iPinyin != null) reformed.put(it.codepoint, iPinyin);
            
            if (gPinyin != null && !gPinyin.equals(uPinyin)) {
                log.println((++counter) + "\t" + Utility.hex(it.codepoint) + "\t" + code
                    + "\t" + (uPinyin == null ? "" : uPinyin)
                    + "\t" + (iPinyin == null ? "" : iPinyin.equals(gPinyin) ? "" : iPinyin)
                    + "\t" + (gPinyin == null ? "" : gPinyin)
                    + "\t" + (hanyu == null ? "" : hanyu + " / ")
                    + (mandarin == null ? "" : mandarin)
                     );
                if (hanyu != null) hCount++;
                continue;
            }
            if (true) continue;
            if (isEqualOrNull(uPinyin, iPinyin)) continue;
            log.println((++counter) + "\t" + Utility.hex(it.codepoint) + "\t" + code
                + "\t" + (uPinyin == null ? "" : uPinyin)
                + "\t" + (iPinyin == null ? "" : iPinyin)
                + "\t" + (gPinyin == null ? "" : gPinyin)
                + "\t" + (hanyu == null ? "" : hanyu + " / ")
                + (mandarin == null ? "" : mandarin)
                 );
        }
        log.println("kHanyuPinlu count: " + hCount);
        
        Collator col = Collator.getInstance(new Locale("zh","","PINYIN"));
        UnicodeSet tailored = col.getTailoredSet().addAll(gotAtLeastOne);
        Collator pinyinCollator = new RuleBasedCollator(
            "&[before 1] a < \u0101 <<< \u0100 << \u00E1 <<< \u00C1 << \u01CE <<< \u01CD << \u00E0 <<< \u00C0 << a <<< A" +
            "&[before 1] e < \u0113 <<< \u0112 << \u00E9 <<< \u00C9 << \u011B <<< \u011A << \u00E8 <<< \u00C8 << e <<< A" +
            "&[before 1] i < \u012B <<< \u012A << \u00ED <<< \u00CD << \u01D0 <<< \u01CF << \u00EC <<< \u00CC << i <<< I" +
            "&[before 1] o < \u014D <<< \u014C << \u00F3 <<< \u00D3 << \u01D2 <<< \u01D1 << \u00F2 <<< \u00D2 << o <<< O" +
            "&[before 1] u < \u016B <<< \u016A << \u00FA <<< \u00DA << \u01D4 <<< \u01D3 << \u00F9 <<< \u00D9 << u <<< U" +
            " << \u01D6 <<< \u01D5 << \u01D8 <<< \u01D7 << \u01DA <<< \u01D9 << \u01DC <<< \u01DB << \u00FC");
        printSortedChars("ICU_Pinyin_Sort.txt", col, tailored, reformed, kHanyuPinlu, kMandarin, pinyinCollator);
        /*
        MultiComparator mcol = new MultiComparator(new Comparator[] {
                new UnicodeMapComparator(reformed, pinyinCollator), col});
        printSortedChars("ICU_Pinyin_Sort2.txt", mcol, tailored);
        */
        log.close();
    }
    
    static class UnicodeMapComparator implements Comparator {
        UnicodeMap map;
        Comparator comp;
        UnicodeMapComparator(UnicodeMap map, Comparator comp) {
            this.map = map;
            this.comp = comp;
        }
        public int compare(Object o1, Object o2) {
            int c1 = UTF16.charAt((String) o1,0);
            int c2 = UTF16.charAt((String) o2,0);
            Object v1 = map.getValue(c1);
            Object v2 = map.getValue(c2);
            if (v1 == null) {
                if (v2 == null) return 0;
                return -1;
            } else if (v2 == null) return 1;
            return comp.compare(v1, v2);
        }
    }
    
    static class MultiComparator implements Comparator {
        private Comparator[] comparators;
    
        public MultiComparator (Comparator[] comparators) {
            this.comparators = comparators;
        }
    
        /* Lexigraphic compare. Returns the first difference
         * @return zero if equal. Otherwise +/- (i+1) 
         * where i is the index of the first comparator finding a difference
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object arg0, Object arg1) {
            for (int i = 0; i < comparators.length; ++i) {
                int result = comparators[i].compare(arg0, arg1);
                if (result == 0) continue;
                if (result > 0) return i+1;
                return -(i+1);
            }
            return 0;
        }
    }

    private static void printSortedChars(String file, Comparator col, UnicodeSet tailored,
         UnicodeMap map, UnicodeMap hanyu, UnicodeMap mand, Comparator p2)
        throws IOException {
        Set set = new TreeSet(col);
        PrintWriter pw = Utility.openPrintWriter(file, Utility.UTF8_WINDOWS);
        for (UnicodeSetIterator it = new UnicodeSetIterator(tailored); it.next(); ) {
            set.add(UTF16.valueOf(it.codepoint));
        }
        String lastm = "";
        String lasts = "";
        for (Iterator it2 = set.iterator(); it2.hasNext(); ) {
            String s = (String)it2.next();
            String m = map == null ? null : (String) map.getValue(UTF16.charAt(s,0));
            if (m == null) m = "";
            String info = m;
            if (p2.compare(lastm,m) > 0) {
                info = info + "\t" + lastm + " > " + m + "\t";
                Object temp;
                temp = hanyu.getValue(UTF16.charAt(lasts,0));
                if (temp != null) info += "[" + temp + "]";
                temp = mand.getValue(UTF16.charAt(lasts,0));
                if (temp != null) info += "[" + temp + "]";
                info += " > ";
                temp = hanyu.getValue(UTF16.charAt(s,0));
                if (temp != null) info += "[" + temp + "]";
                temp = mand.getValue(UTF16.charAt(s,0));
                if (temp != null) info += "[" + temp + "]";                
            } 
            pw.println(Utility.hex(s) + "\t" + s + "\t" + info);
            lastm = m;
            lasts = s;
        }
        pw.close();
    }
    
    static void addField(String dir, String file, int hexCodeFieldNumber, int valueNumber, UnicodeMap result) throws IOException {
        BufferedReader br = BagFormatter.openUTF8Reader(dir, file);
        while (true) {
            String line = br.readLine();
            if (line == null) break;
            line = line.trim();
            if (line.length() == 0) continue;
            if (line.startsWith("\uFEFF")) line = line.substring(1);
            if (line.startsWith("#") || line.length() == 0) continue;
            String[] pieces = Utility.split(line,'\t');
            result.put(Integer.parseInt(pieces[hexCodeFieldNumber], 16), pieces[valueNumber]);
        }
        br.close();
    }
    
    static boolean isEqualOrNull(String a, String b) {
        if (a == null || b == null) return true;
        return a.equals(b);
    }
    public static String getUpTo(String s, char ch) {
        int pos = s.indexOf(ch);
        if (pos < 0) return s;
        return s.substring(0,pos);   
    }
    
    public static void main(int typeIn) throws IOException {
        if (typeIn == CHINESE) {
            fixedMandarin();
            return;
        }
    	type = typeIn;
    	
        try {
            System.out.println("Starting");
            System.out.println("Quoting: " + quoteNonLetters.toRules(true));
            System.out.println("Quoting: " + quoteNonLetters.toRules(true));
            
            
            String key; // kMandarin, kKorean, kJapaneseKun, kJapaneseOn
            String filename;
            
            switch (type) {
                case DEFINITION:
                    key = "kDefinition"; // kMandarin, kKorean, kJapaneseKun, kJapaneseOn
                    filename = "Raw_Transliterator_Han_Latin_Definition";
                    break;
                case JAPANESE: 
                    key = "kJapaneseOn";
                    filename = "Raw_Transliterator_ja_Latin";
                    break;
                case CHINESE:
                    key = "kMandarin";
                    filename = "Raw_Transliterator_Han_Latin";
                    break;
                default: throw new IllegalArgumentException("Unexpected option: must be 0..2");
            }
            filename += Default.ucd().getVersion() + ".txt";
                
            err = Utility.openPrintWriter("Transliterate_err.txt", Utility.UTF8_WINDOWS);
            log = Utility.openPrintWriter("Transliterate_log.txt", Utility.UTF8_WINDOWS);
            log.print('\uFEFF');
            
            if (false /*!SKIP_OVERRIDES*/) {
                log.println();
                log.println("@*Override Data");
                log.println();
                readOverrides(type);
    
                log.println();
                log.println("@*DICT Data");
                log.println();
                readCDICTDefinitions(type);
            }
          
            log.println();
            log.println("@Unihan Data");
            log.println();
            out2 = BagFormatter.openUTF8Writer(GEN_DIR, "unihan_kmandarinDump.txt");
            
            readUnihanData(key);
            
            out2.close();

            if (false) {
                readCDICT();
                compareUnihanWithCEDICT();
            }
            
            readFrequencyData(type);
            
            Iterator it = fullPinyin.iterator();
            while (it.hasNext()) {
                String s = (String) it.next();
                if (!isValidPinyin2(s)) {
                    err.println("?Valid Pinyin: " + s);
                }
            }
            
            
            it = unihanMap.keySet().iterator();
            Map badPinyin = new TreeMap();
            PrintWriter out2 = Utility.openPrintWriter("Raw_mapping.txt", Utility.UTF8_WINDOWS);
            try {
                while (it.hasNext()) {
                    String keyChar = (String) it.next();
                    String def = (String) unihanMap.get(keyChar);
                    if (!isValidPinyin(def)) {
                        String fixedDef = fixPinyin(def);
                        err.println(Default.ucd().getCode(keyChar) + "\t" + keyChar + "\t" + fixedDef + "\t#" + def
                            + (fixedDef.equals(def) ? " FAIL" : ""));
                        Utility.addToSet(badPinyin, def, keyChar);
                    }
                    // check both ways
                    String digitDef = accentPinyin_digitPinyin.transliterate(def);
                    String accentDef = digitPinyin_accentPinyin.transliterate(digitDef);
                    if (!accentDef.equals(def)) {
                        err.println("Failed Digit Pinyin: " 
                            + Default.ucd().getCode(keyChar) + "\t" + keyChar + "\t" 
                            + def + " => " + digitDef + " => " + accentDef);
                    }
                    
                    out2.println(toHexUnicode.transliterate(keyChar) 
                        + "\tkMandarin\t" + digitDef.toUpperCase() + "\t# " + keyChar + ";\t" + def);
                }
                err.println();
                err.println("Summary of Bad syllables");
                Utility.printMapOfCollection(err, badPinyin, "\r\n", ":\t", ", ");
            } finally {
                out2.close();
            }
            
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
            Set lenSet = new TreeSet();
            Set backSet = new TreeSet();
            int rank = 0;
            Map definitionCount = new HashMap();
            
            it = rankList.iterator();
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
            log.println("@Duplicates (Frequency Order");
            log.println();
            it = rankList.iterator();
            while (it.hasNext()) {
                String word = (String) it.next();
                Collection dups = (Collection) duplicates.get(word);
                if (dups == null) continue;
                log.print(hex.transliterate(word) + "\t" + word + "\t");
                Iterator it2 = dups.iterator();
                boolean gotFirst = false;
                while (it2.hasNext()) {
                    if (!gotFirst) gotFirst = true;
                    else log.print(", ");
                    log.print(it2.next());
                }
                if (overrideSet.contains(word)) log.print(" *override*");
                log.println();
            }
            
            log.println();
            log.println("@Duplicates (Character Order)");
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
                if (overrideSet.contains(word)) log.print(" *override*");
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
    
    //http://fog.ccsf.cc.ca.us/~jliou/phonetic.htm
    // longer ones must be AFTER!
    // longer ones must be AFTER!
    static final String[] initialPinyin = {
        "",
        "b", "p", "m", "f", 
        "d", "t", "n", "l", 
        "z", "c", "s", 
        "zh", "ch", "sh", "r",
        "j", "q", "x", 
        "g", "k", "h", 
        "y", "w"}; // added to make checking simpler
        
    static final String[] finalPinyin = {
        "a", "ai", "ao", "an", "ang",
        "o", "ou", "ong",
        "e", "ei", "er", "en", "eng",
        "i", "ia", "iao", "ie", "iu", "ian", "in", "iang", "ing", "iong",
        "u", "ua", "uo", "uai", "ui", "uan", "un", "uang", "ueng",
        "ü", "üe", "üan", "ün"
    };
    // Don't bother with the following rules; just add w,y to initials
    // When “i” stands alone, a “y” will be added before it as “yi”. 
    //      If “i” is the first letter of the syllable it will be changed to “y”. 
    // When “u” stands alone, a “w” will be added before it as “wu”. 
    //      If “u” is the first letter of the syllable it will be changed to “w”. e.g. “uang -> wang”. 
    // When “ü” stands alone, a “y” will be added before it and “ü” will be changed to “u” as “yu”. 
    //      If “ü” is the first letter of the syllable, then the spelling will be changed to “yu”. e.g. “üan -> yuan”. 
    //Note: The nasal final “ueng” never occurs after an initial but always form a syllable by itself.
    // The “o” in “iou” is hidden, so it will be wrote as “iu”. But, don’t forget to pronounce it. 
    // The “e” in “uei” is hidden, so it will be wrote as “ui”. But, don’t forget to pronounce it. 
    
    
    public static final String[] pinyin_bopomofo = {
	"a", "\u311a",
	"ai", "\u311e",
	"an", "\u3122",
	"ang", "\u3124",
	"ao", "\u3120",
	"ba", "\u3105\u311a",
	"bai", "\u3105\u311e",
	"ban", "\u3105\u3122",
	"bang", "\u3105\u3124",
	"bao", "\u3105\u3120",
	"bei", "\u3105\u311f",
	"ben", "\u3105\u3123",
	"beng", "\u3105\u3125",
	"bi", "\u3105\u3127",
	"bian", "\u3105\u3127\u3122",
	"biao", "\u3105\u3127\u3120",
	"bie", "\u3105\u3127\u311d",
	"bin", "\u3105\u3127\u3123",
	"bing", "\u3105\u3127\u3125",
	"bo", "\u3105\u311b",
	"bu", "\u3105\u3128",
	"ca", "\u3118\u311a",
	"cai", "\u3118\u311e",
	"can", "\u3118\u3122",
	"cang", "\u3118\u3124",
	"cao", "\u3118\u3120",
	"ce", "\u3118",
	"cen", "\u3118\u3123",
	"ceng", "\u3118\u3125",
	"cha", "\u3114\u311a",
	"chai", "\u3114\u311e",
	"chan", "\u3114\u3122",
	"chang", "\u3114\u3124",
	"chao", "\u3114\u3120",
	"che", "\u3114\u311c",
	"chen", "\u3114\u3123",
	"cheng", "\u3114\u3125",
	"chi", "\u3114",
	"chong", "\u3114\u3121\u3125",
	"chou", "\u3114\u3121",
	"chu", "\u3114\u3128",
	//"chua", "XXX",
	"chuai", "\u3114\u3128\u311e",
	"chuan", "\u3114\u3128\u3122",
	"chuang", "\u3114\u3128\u3124",
	"chui", "\u3114\u3128\u311f",
	"chun", "\u3114\u3128\u3123",
	"chuo", "\u3114\u3128\u311b",
	"ci", "\u3118",
	"cong", "\u3118\u3128\u3125",
	"cou", "\u3118\u3121",
	"cu", "\u3118\u3128",
	"cuan", "\u3118\u3128\u3122",
	"cui", "\u3118\u3128\u311f",
	"cun", "\u3118\u3128\u3123",
	"cuo", "\u3118\u3128\u311b",
	"da", "\u3109\u311a",
	"dai", "\u3109\u311e",
	"dan", "\u3109\u3122",
	"dang", "\u3109\u3124",
	"dao", "\u3109\u3120",
	"de", "\u3109\u311c",
	"dei", "\u3109\u311f",
        "den", "\u3109\u3123",
	"deng", "\u3109\u3125",
	"di", "\u3109\u3127",
	"dia", "\u3109\u3127\u311a",
	"dian", "\u3109\u3127\u3122",
	"diao", "\u3109\u3127\u3120",
	"die", "\u3109\u3127\u311d",
	"ding", "\u3109\u3127\u3125",
	"diu", "\u3109\u3127\u3121",
	"dong", "\u3109\u3128\u3125",
	"dou", "\u3109\u3121",
	"du", "\u3109\u3128",
	"duan", "\u3109\u3128\u3122",
	"dui", "\u3109\u3128\u311f",
	"dun", "\u3109\u3128\u3123",
	"duo", "\u3109\u3128\u311b",
	"e", "\u311c",
	"ei", "\u311f",
	"en", "\u3123",
	"eng", "\u3125",
	"er", "\u3126",
	"fa", "\u3108\u311a",
	"fan", "\u3108\u3122",
	"fang", "\u3108\u3124",
	"fei", "\u3108\u311f",
	"fen", "\u3108\u3123",
	"feng", "\u3108\u3125",
	"fo", "\u3108\u311b",
	"fou", "\u3108\u3121",
	"fu", "\u3108\u3128",
	"ga", "\u310d\u311a",
	"gai", "\u310d\u311e",
	"gan", "\u310d\u3122",
	"gang", "\u310d\u3124",
	"gao", "\u310d\u3120",
	"ge", "\u310d\u311c",
	"gei", "\u310d\u311f",
	"gen", "\u310d\u3123",
	"geng", "\u310d\u3125",
	"gong", "\u310d\u3128\u3125",
	"gou", "\u310d\u3121",
	"gu", "\u310d\u3128",
	"gua", "\u310d\u3128\u311a",
	"guai", "\u310d\u3128\u311e",
	"guan", "\u310d\u3128\u3122",
	"guang", "\u310d\u3128\u3124",
	"gui", "\u310d\u3128\u311f",
	"gun", "\u310d\u3128\u3123",
	"guo", "\u310d\u3128\u311b",
	"ha", "\u310f\u311a",
	"hai", "\u310f\u311e",
	"han", "\u310f\u3122",
	"hang", "\u310f\u3124",
	"hao", "\u310f\u3120",
	"he", "\u310f\u311c",
	"hei", "\u310f\u311f",
	"hen", "\u310f\u3123",
	"heng", "\u310f\u3125",
                "hm", "\u310f\u3107",
	"hng", "\u310f\u312b", // 'dialect of n'
	"hong", "\u310f\u3128\u3125",
	"hou", "\u310f\u3121",
	"hu", "\u310f\u3128",
	"hua", "\u310f\u3128\u311a",
	"huai", "\u310f\u3128\u311e",
	"huan", "\u310f\u3128\u3122",
	"huang", "\u310f\u3128\u3124",
	"hui", "\u310f\u3128\u311f",
	"hun", "\u310f\u3128\u3123",
	"huo", "\u310f\u3128\u311b",
	"ji", "\u3110\u3127",
	"jia", "\u3110\u3127\u311a",
	"jian", "\u3110\u3127\u3122",
	"jiang", "\u3110\u3127\u3124",
	"jiao", "\u3110\u3127\u3120",
	"jie", "\u3110\u3127\u311d",
	"jin", "\u3110\u3127\u3123",
	"jing", "\u3110\u3127\u3125",
	"jiong", "\u3110\u3129\u3125",
	"jiu", "\u3110\u3127\u3121",
	"ju", "\u3110\u3129",
	"juan", "\u3110\u3129\u3122",
	"jue", "\u3110\u3129\u311d",
	"jun", "\u3110\u3129\u3123",
	"ka", "\u310e\u311a",
	"kai", "\u310e\u311e",
	"kan", "\u310e\u3122",
	"kang", "\u310e\u3124",
	"kao", "\u310e\u3120",
	"ke", "\u310e\u311c",
                "kei", "\u310e\u311f",
	"ken", "\u310e\u3123",
	"keng", "\u310e\u3125",
	"kong", "\u310e\u3128\u3125",
	"kou", "\u310e\u3121",
	"ku", "\u310e\u3128",
	"kua", "\u310e\u3128\u311a",
	"kuai", "\u310e\u3128\u311e",
	"kuan", "\u310e\u3128\u3122",
	"kuang", "\u310e\u3128\u3124",
	"kui", "\u310e\u3128\u311f",
	"kun", "\u310e\u3128\u3123",
	"kuo", "\u310e\u3128\u311b",
	"la", "\u310c\u311a",
	"lai", "\u310c\u311e",
	"lan", "\u310c\u3122",
	"lang", "\u310c\u3124",
	"lao", "\u310c\u3120",
	"le", "\u310c\u311c",
	"lei", "\u310c\u311f",
	"leng", "\u310c\u3125",
	"li", "\u310c\u3127",
	"lia", "\u310c\u3127\u311a",
	"lian", "\u310c\u3127\u3122",
	"liang", "\u310c\u3127\u3124",
	"liao", "\u310c\u3127\u3120",
	"lie", "\u310c\u3127\u311d",
	"lin", "\u310c\u3127\u3123",
	"ling", "\u310c\u3127\u3125",
	"liu", "\u310c\u3127\u3121",
	"lo", "\u310c\u311b",
	"long", "\u310c\u3128\u3125",
	"lou", "\u310c\u3121",
	"lu", "\u310c\u3128",
	"lü", "\u310c\u3129",
	"luan", "\u310c\u3128\u3122",
	"lüe", "\u310c\u3129\u311d",
	"lun", "\u310c\u3128\u3123",
	"luo", "\u310c\u3128\u311b",
	"m", "\u3107",
	"ma", "\u3107\u311a",
	"mai", "\u3107\u311e",
	"man", "\u3107\u3122",
	"mang", "\u3107\u3124",
	"mao", "\u3107\u3120",
	"me", "\u3107\u311c",
	"mei", "\u3107\u311f",
	"men", "\u3107\u3123",
	"meng", "\u3107\u3125",
	"mi", "\u3107\u3127",
	"mian", "\u3107\u3127\u3122",
	"miao", "\u3107\u3127\u3120",
	"mie", "\u3107\u3127\u311d",
	"min", "\u3107\u3127\u3123",
	"ming", "\u3107\u3127\u3125",
	"miu", "\u3107\u3127\u3121",
	"mo", "\u3107\u311b",
	"mou", "\u3107\u3121",
	"mu", "\u3107\u3128",
	"n", "\u310b",
	"na", "\u310b\u311a",
	"nai", "\u310b\u311e",
	"nan", "\u310b\u3122",
	"nang", "\u310b\u3124",
	"nao", "\u310b\u3120",
	"ne", "\u310b\u311c",
	"nei", "\u310b\u311f",
	"nen", "\u310b\u3123",
	"neng", "\u310b\u3125",
	"ng", "\u312b",
	"ni", "\u310b\u3127",
	"nian", "\u310b\u3127\u3122",
	"niang", "\u310b\u3127\u3124",
	"niao", "\u310b\u3127\u3120",
	"nie", "\u310b\u3127\u311d",
	"nin", "\u310b\u3127\u3123",
	"ning", "\u310b\u3127\u3125",
	"niu", "\u310b\u3127\u3121",
	"nong", "\u310b\u3128\u3125",
	"nou", "\u310b\u3121",
	"nu", "\u310b\u3128",
	"nü", "\u310b\u3129",
	"nuan", "\u310b\u3128\u3122",
	"nüe", "\u310b\u3129\u311d",
	"nuo", "\u310b\u3128\u311b",
	"o", "\u311b",
	"ou", "\u3121",
	"pa", "\u3106\u311a",
	"pai", "\u3106\u311e",
	"pan", "\u3106\u3122",
	"pang", "\u3106\u3124",
	"pao", "\u3106\u3120",
	"pei", "\u3106\u311f",
	"pen", "\u3106\u3123",
	"peng", "\u3106\u3125",
	"pi", "\u3106\u3127",
	"pian", "\u3106\u3127\u3122",
	"piao", "\u3106\u3127\u3120",
	"pie", "\u3106\u3127\u311d",
	"pin", "\u3106\u3127\u3123",
	"ping", "\u3106\u3127\u3125",
	"po", "\u3106\u311b",
	"pou", "\u3106\u3121",
	"pu", "\u3106\u3128",
	"qi", "\u3111",
	"qia", "\u3111\u3127\u311a",
	"qian", "\u3111\u3127\u3122",
	"qiang", "\u3111\u3127\u3124",
	"qiao", "\u3111\u3127\u3120",
	"qie", "\u3111\u3127\u311d",
	"qin", "\u3111\u3127\u3123",
	"qing", "\u3111\u3127\u3125",
	"qiong", "\u3111\u3129\u3125",
	"qiu", "\u3111\u3129\u3121",
	"qu", "\u3111\u3129",
	"quan", "\u3111\u3129\u3122",
	"que", "\u3111\u3129\u311d",
	"qun", "\u3111\u3129\u3123",
	"ran", "\u3116\u3122",
	"rang", "\u3116\u3124",
	"rao", "\u3116\u3120",
	"re", "\u3116\u311c",
	"ren", "\u3116\u3123",
	"reng", "\u3116\u3125",
	"ri", "\u3116",
	"rong", "\u3116\u3128\u3125",
	"rou", "\u3116\u3121",
	"ru", "\u3116\u3128",
	"ruan", "\u3116\u3128\u3122",
	"rui", "\u3116\u3128\u311f",
	"run", "\u3116\u3128\u3123",
	"ruo", "\u3116\u3128\u311b",
	"sa", "\u3119\u311a",
	"sai", "\u3119\u311e",
	"san", "\u3119\u3122",
	"sang", "\u3119\u3124",
	"sao", "\u3119\u3120",
	"se", "\u3119\u311c",
	"sen", "\u3119\u3123",
	"seng", "\u3119\u3125",
	"sha", "\u3115\u311a",
	"shai", "\u3115\u311e",
	"shan", "\u3115\u3122",
	"shang", "\u3115\u3124",
	"shao", "\u3115\u3120",
	"she", "\u3115\u311c",
	"shei", "\u3115\u311f",
	"shen", "\u3115\u3123",
	"sheng", "\u3115\u3125",
	"shi", "\u3115",
	"shou", "\u3115\u3121",
	"shu", "\u3115\u3128",
	"shua", "\u3115\u3128\u311a",
	"shuai", "\u3115\u3128\u311e",
	"shuan", "\u3115\u3128\u3122",
	"shuang", "\u3115\u3128\u3124",
	"shui", "\u3115\u3128\u311f",
	"shun", "\u3115\u3128\u3123",
	"shuo", "\u3115\u3128\u311b",
	"si", "\u3119",
	"song", "\u3119\u3128\u3125",
	"sou", "\u3119\u3121",
	"su", "\u3119\u3128",
	"suan", "\u3119\u3128\u3122",
	"sui", "\u3119\u3128\u311f",
	"sun", "\u3119\u3128\u3123",
	"suo", "\u3119\u3128\u311b",
	"ta", "\u310a\u311a",
	"tai", "\u310a\u311e",
	"tan", "\u310a\u3122",
	"tang", "\u310a\u3124",
	"tao", "\u310a\u3120",
	"te", "\u310a\u311c",
	"teng", "\u310a\u3125",
	"ti", "\u310a\u3127",
	"tian", "\u310a\u3127\u3122",
	"tiao", "\u310a\u3127\u3120",
	"tie", "\u310a\u3127\u311d",
	"ting", "\u310a\u3127\u3125",
	"tong", "\u310a\u3128\u3125",
	"tou", "\u310a\u3121",
	"tu", "\u310a\u3128",
	"tuan", "\u310a\u3128\u3122",
	"tui", "\u310a\u3128\u311f",
	"tun", "\u310a\u3128\u3123",
	"tuo", "\u310a\u3128\u311b",
	"wa", "\u3128\u311a",
	"wai", "\u3128\u311e",
	"wan", "\u3128\u3122",
	"wang", "\u3128\u3124",
	"wei", "\u3128\u311f",
	"wen", "\u3128\u3123",
	"weng", "\u3128\u3125",
	"wo", "\u3128\u311b",
	"wu", "\u3128",
	"xi", "\u3112\u3127",
	"xia", "\u3112\u3127\u311a",
	"xian", "\u3112\u3127\u3122",
	"xiang", "\u3112\u3127\u3124",
	"xiao", "\u3112\u3127\u3120",
	"xie", "\u3112\u3127\u311d",
	"xin", "\u3112\u3127\u3123",
	"xing", "\u3112\u3127\u3125",
	"xiong", "\u3112\u3129\u3125",
	"xiu", "\u3112\u3127\u3121",
	"xu", "\u3112\u3129",
	"xuan", "\u3112\u3129\u3122",
	"xue", "\u3112\u3129\u311d",
	"xun", "\u3112\u3129\u3123",
	"ya", "\u3127\u311a",
	"yai", "\u3127\u311e", // not in xinhua zidian index, but listed as alternate pronunciation
	"yan", "\u3127\u3122",
	"yang", "\u3127\u3124",
	"yao", "\u3127\u3120",
	"ye", "\u3127\u311d",
	"yi", "\u3127",
	"yin", "\u3127\u3123",
	"ying", "\u3127\u3125",
	"yo", "\u3127\u311b",
	"yong", "\u3129\u3125",
	"you", "\u3127\u3121",
	"yu", "\u3129",
	"yuan", "\u3129\u3122",
	"yue", "\u3129\u311d",
	"yun", "\u3129\u3123",
	"za", "\u3117\u311a",
	"zai", "\u3117\u311e",
	"zan", "\u3117\u3122",
	"zang", "\u3117\u3124",
	"zao", "\u3117\u3120",
	"ze", "\u3117",
	"zei", "\u3117\u311f",
	"zen", "\u3117\u3123",
	"zeng", "\u3117\u3125",
	"zha", "\u3113\u311a",
	"zhai", "\u3113\u311e",
	"zhan", "\u3113\u3122",
	"zhang", "\u3113\u3124",
	"zhao", "\u3113\u3120",
	"zhe", "\u3113\u311d",
	"zhei", "\u3113\u311f",
	"zhen", "\u3113\u3123",
	"zheng", "\u3113\u3125",
	"zhi", "\u3113",
	"zhong", "\u3113\u3128\u3125",
	"zhou", "\u3113\u3121",
	"zhu", "\u3113\u3128",
	"zhua", "\u3113\u3128\u311a",
	"zhuai", "\u3113\u3128\u311e",
	"zhuan", "\u3113\u3128\u3122",
	"zhuang", "\u3113\u3128\u3124",
	"zhui", "\u3113\u3128\u311f",
	"zhun", "\u3113\u3128\u3123",
	"zhuo", "\u3113\u3128\u311b",
	"zi", "\u3117",
	"zong", "\u3117\u3128\u3125",
	"zou", "\u3117\u3121",
	"zu", "\u3117\u3128",
	"zuan", "\u3117\u3128\u3122",
	"zui", "\u3117\u3128\u311f",
	"zun", "\u3117\u3128\u3123",
	"zuo", "\u3117\u3128\u311b",
    };
    
    static final Set fullPinyin = new TreeSet();
    static {
        for (int i = 0; i < pinyin_bopomofo.length; i+= 2) {
            fullPinyin.add(pinyin_bopomofo[i]);
        }
    }
    
    static boolean isValidPinyin(String s) {
        s = dropTones.transliterate(s);
        if (fullPinyin.contains(s)) return true;
        return false;
    }
    
    static boolean isValidPinyin2(String s) {
        s = dropTones.transliterate(s);
        for (int i = initialPinyin.length-1; i >= 0; --i) {
            if (s.startsWith(initialPinyin[i])) {
                String end = s.substring(initialPinyin[i].length());
                for (int j = finalPinyin.length-1; j >= 0; --j) {
                    if (end.equals(finalPinyin[j])) return true;
                }
                return false;
            }
        }
        return false;
    }
    
    /*
    U+347C	·	liù	#lyuè  
U+3500	·	lüè	#lvè
U+3527	·	liù	#lyù
U+3729	·	ào	#àu
U+380E	·	jí	#jjí
U+3825	·	l·	#lv·
U+3A3C	·	lüè	#luè
U+3B5A	·	li·	#ly· *** lü?
U+3CB6	·	l·	#lv·
U+3D56	·	niù	#nyù *** nü?
U+3D88	·	li·ng	#li·ng
U+3EF2	·	li·	#ly·*** lü?
U+3F94	·	li·	#ly·*** lü?
U+4071	·	ào	#àu
U+40AE	·	liù	#lyuè *** lüe?
U+430E	·	liù	#lyuè *** lüe?
U+451E	·	liù	#lyù *** lü?
U+4588	·	nüè	#nuè
U+458B	·	nüè	#nuè
U+45A1	·	niù	#nyù *** nü?
U+4610	·	niù	#nyù *** nü?
U+46BC	·	niù	#nyù *** nü?
U+46DA	·	liù	#lyuè *** lüe?
U+4896	·	liù	#lyù *** lü?
U+4923	·	liù	#lyuè *** lüe?
U+4968	·	liù	#lyù *** lü?
U+4A0B	·	niù	#nyuè *** nüe?
U+4AC4	·	chuò	#chuà
U+4D08	·	·o	#·u
U+4D8A	·	niù	#nyù *** nü?
U+51CA	·	qíng	#qýng
U+51D6	·	zhu·n	#zhu·n *** this is probably zh·n 
U+5481	·	gàn	#gèm
U+5838	·	féng	#fúng
U+639F	·	lü·	#lu· *** this pronunciation surprises me, but I don't know...
U+66D5	·	yàn	#yiàn
U+6B3B	·	chu·	#chu· *** chua _is_ ok after all, my table missed an entry
U+6B56	·	chu·	#chu· *** chua 
U+6C7C	·	ni·	#ni·u
U+6E6D	·	qiú	#qióu
U+6F71	·	y·	#yi·
U+7493	·	xiù	#xiòu
U+7607	·	zh·ng	#zh·ng *** I suspect zh·ng
U+7674	·	luán	#lüán
U+7867	·	y·ng	#i·ng
U+7878	·	nüè	#nuè
*/
    
    static Transliterator fixTypos = Transliterator.createFromRules("fix_typos", 
        "$cons=[bcdfghjklmnpqrstvwxyz];"
        +"$nlet=[^[:Letter:][:Mark:]];"
        +"$cons{iou}$nlet   > iu;"
        +"$cons{em}$nlet    > an;"
        +"$cons{uen}$nlet   > ueng;"
        +"$cons{ve}$nlet    > üe;"
        +"$cons{v}$nlet     > ü;"
        +"$cons{yue}$nlet   > iu;"
        +"$cons{yng}$nlet   > ing;"
        +"$cons{yu}$nlet    > iu;"
        //+"$cons{ue}       > üe;"
        +"jj                > j;"
        //+"$nlet{ng}$nlet  > eng;"
        //+"$nlet{n}$nlet   > en;"
        //+"$nlet{m}$nlet   > en;"
        +"$nlet{au}$nlet    > ao;"
        
        // new fixes        
        +"zhueng}$nlet       > zhong;"
        +"zhuen}$nlet       > zhuan;"
        +"lue > lüe;"
        +"liong > liang;"
        +"nue > nüe;"
        +"chua > chuo;"
        +"yian > yan;"
        +"yie > ye;"
        +"lüan > luan;"
        +"iong > yong;"
        , Transliterator.FORWARD);
    
    
    static String fixPinyin(String s) {
        String original = s;
        //err.println("Source: " + s);
        s = accentPinyin_digitPinyin.transliterate(s);
        //err.println("Digit: " + s);
        s = fixTypos.transliterate(s);
        //err.println("fixed: " + s);
        s = digitPinyin_accentPinyin.transliterate(s);
        //err.println("Result: " + s);
        if (isValidPinyin(s)) return s;
        return original;
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
                br = Utility.openReadFile(BASE_DIR + "dict\\chinese_frequency.txt", Utility.UTF8);
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
         
                br = Utility.openReadFile( BASE_DIR + "dict\\japanese_frequency.txt", Utility.UTF8);
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
                        int script = Default.ucd().getScript(cp);
                        if (script != HAN_SCRIPT) {
                            if (script != HIRAGANA_SCRIPT && script != KATAKANA_SCRIPT 
                                && cp != 0x30FB && cp != 0x30FC) {
                                System.out.println("Huh: " + Default.ucd().getCodeAndName(cp));
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
            
            boolean showFrequency = false;
            
            if (showFrequency) {
                log.println();
                log.println("@Frequency data: Rank of Character");
                log.println();
            }
            
            // make up rankMap, rankList
            
            while(it.hasNext()) {
                Pair p = (Pair) it.next();
                if (showFrequency) log.println(p.first + ", " + p.second);
                Object rank = rankMap.get(p.second);
                if (rank == null) {
                    rankMap.put(p.second, new Integer(++overallRank));
                    rankList.add(p.second);
                }
            }

            if (showFrequency) {
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
            if (!Default.ucd().isAllocated(i)) continue;
            if (Default.ucd().getScript(i) != HAN_SCRIPT) continue;
            Utility.dot(i);
            
            String ch = UTF16.valueOf(i);
            
            String pinyin = (String) unihanMap.get(ch);
            if (pinyin == null) {
                String ch2 = Default.nfkd().normalize(ch);
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
        BufferedReader br = Utility.openReadFile(BASE_DIR + "dict\\" + fname, Utility.UTF8);
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
                        definition = digitToPinyin(definition, line);
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
    
    static void readOverrides(int type) throws IOException {
        if (type != CHINESE) return;
        String fname = "Chinese_override.txt";
        
        System.out.println("Reading " + fname);
        BufferedReader br = Utility.openReadFile(BASE_DIR + "dict\\" + fname, Utility.UTF8);
        int counter = 0;
        String[] pieces = new String[50];
        String line = "";
        boolean noOverrideFailure = true;
        try {
            while (true) {
                line = Utility.readDataLine(br);
                if (line == null) break;
                if (line.length() == 0) continue;
                Utility.dot(counter++);
                //System.out.println(line);
                
                // skip code
                line=line.toLowerCase();
                
                int wordStart = line.indexOf('\t') + 1;
                int wordEnd = line.indexOf('\t', wordStart);
                String word = line.substring(wordStart, wordEnd);
                String definition = fixPinyin(line.substring(wordEnd+1));
                String old = (String) unihanMap.get(word);
                if (old != null) {
                    if (!old.equals(definition)) {
                        if (noOverrideFailure) {
                            System.out.println("Overriding Failure");
                            noOverrideFailure = false;
                        }
                        err.println("Overriding Failure: " + word 
                            + "\t" + old + " " + toHexUnicode.transliterate(old)
                            + "\t" + definition + " " + toHexUnicode.transliterate(definition));
                    }
                } else {
                    addCheck(word, definition, line);
                    overrideSet.add(word);
                }
            }
            br.close();
        } catch (Exception e) {
            throw new ChainException("{0} Failed at {1}" , new Object []{new Integer(counter), line}, e);
        }
    }    
    
    
/*
    @Unihan Data

Bad pinyin data: \u4E7F	?	LE
\u7684	?	de, de, dí, dì
*/

    static void fixChineseOverrides() throws IOException {
        
        log = Utility.openPrintWriter("Transliterate_log.txt", Utility.UTF8_WINDOWS);
        out = Utility.openPrintWriter("new_Chinese_override.txt", Utility.UTF8_WINDOWS);
        try {
            
            String fname = "fixed_Chinese_transliterate_log.txt";
            
            int counter = 0;
            String line = "";
            String pinyinPrefix = "Bad pinyin data: ";
            
            System.out.println("Reading " + fname);
            BufferedReader br = Utility.openReadFile(BASE_DIR + "dict\\" + fname, Utility.UTF8);
            try {
                while (true) {
                    line = Utility.readDataLine(br);
                    if (line == null) break;
                    if (line.length() == 0) continue;
                    if (line.charAt(0) == 0xFEFF) {
                        line = line.substring(1); // remove BOM
                        if (line.length() == 0) continue;
                    }
                    Utility.dot(counter++);
                    
                    
                    if (line.charAt(0) == '@') continue;
                    if (line.startsWith(pinyinPrefix)) {
                        line = line.substring(pinyinPrefix.length());
                    }
                    line = line.toLowerCase();
                    
                    //System.out.println(Default.ucd.getCode(line));
                    // skip code
                    int wordStart = line.indexOf('\t') + 1;
                    int wordEnd = line.indexOf('\t', wordStart);
                    String word = line.substring(wordStart, wordEnd).trim();
                    
                    int defStart = wordEnd+1;
                    int defEnd = line.indexOf(',', defStart);
                    if (defEnd < 0) defEnd = line.length();
                    
                    String definition = fixCircumflex.transliterate(line.substring(defStart, defEnd).trim());
                    
                    String notones = dropTones.transliterate(definition);
                    if (definition.equals(notones)) {
                        definition = digitPinyin_accentPinyin.transliterate(definition + "1");
                        if (definition == null) {
                            System.out.println("Huh? " + notones);
                        }
                        log.println("Fixing: " + notones + " => " + definition + "; " + line);
                    }
                    
                    out.println(hex.transliterate(word) + "\t" + word + "\t" + definition);
                }
            } catch (Exception e) {
                throw new ChainException("{0} Failed at {1}" , new Object []{new Integer(counter), line}, e);
            } finally {
                br.close();
            }
        } finally {
            out.close();
        }
    }    


    
    static Set overrideSet = new HashSet();
    
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
        definition = Default.nfc().normalize(definition);
        word = Default.nfc().normalize(word);
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
        String fname = "cdict.txt";
        
        BufferedReader br = Utility.openReadFile(BASE_DIR + "dict\\" + fname, Utility.UTF8);
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
                    
                    String piece = digitToPinyin(pieces[i], line);
                    
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
    
    static String digitToPinyin(String source, String line) {
        if (source.indexOf('5') >= 0) log.println("Pinyin Tone5 at: " + line);
        return digitPinyin_accentPinyin.transliterate(source);
    }
    
    static Map cdict = new TreeMap();
    static Map simplifiedToTraditional = new HashMap();
    static Map traditionalToSimplified = new HashMap();
    
    static UnicodeMap kHanyuPinlu = new UnicodeMap();
  
    static void readUnihanData(String key) throws java.io.IOException {

        BufferedReader in = Utility.openUnicodeFile("Unihan", Default.ucdVersion(), true, Utility.UTF8); 

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
            if (propertyValue.indexOf("U+") >= 0) propertyValue = fromHexUnicode.transliterate(propertyValue);
            
            // gather traditional mapping
            if (property.equals("kTraditionalVariant")) {
                simplifiedToTraditional.put(UTF16.valueOf(code), propertyValue);
            }
            
            if (property.equals("kSimplifiedVariant")) {
                traditionalToSimplified.put(UTF16.valueOf(code), propertyValue);
            }
            
            if (key.equals("kMandarin") && property.equals("kHanyuPinlu")) {
                // U+64D4   kHanyuPinlu dan1(297), dan4(61), dan5(36)
                String[] piece = Utility.split(propertyValue,'(');
                String pinyin = digitToPinyin(piece[0], line);
                log.println(scode + "\t" + pinyin + "\t" + line);
                kHanyuPinlu.put(Integer.parseInt(scode,16), pinyin);
            }
            if (property.equals(key) 
                || key.equals("kJapaneseOn") && property.equals("kJapaneseKun")
                ) {
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
            
            definition = digitToPinyin(definition, line);
            out2.println(Utility.hex(cp) + '\t' + UTF16.valueOf(cp) + "\t" + definition.toLowerCase());
        }
        if (type == DEFINITION) {
            definition = removeMatched(definition,'(', ')', line);
            definition = removeMatched(definition,'[', ']', line);
            definition = fixDefinition(definition, line);
        }
        definition = definition.trim();
        definition = Default.ucd().getCase(definition, FULL, LOWER);

        if (definition.length() == 0) {
            Utility.fixDot();
            err.println("Zero value for " + Default.ucd().getCode(cp) + " on: " + hex.transliterate(line));
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
        definition = Default.ucd().getCase(definition, FULL, LOWER);
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
        
    static Map unihanMap = new TreeMap(); // could be hashmap
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
    
    static Transliterator digitPinyin_accentPinyin;
    
    static Transliterator accentPinyin_digitPinyin = Transliterator.createFromRules("accentPinyin_digitPinyin", 
        "::NFD; "
        + " ([\u0304\u0301\u030C\u0300\u0306]) ([[:Mark:][:Letter:]]+) > $2 | $1;"
        + "\u0304 > '1'; \u0301 > '2'; \u030C > '3'; \u0300 > '4'; \u0306 > '3';" 
        + " ::NFC;", Transliterator.FORWARD);
    
    static Transliterator fixCircumflex = Transliterator.createFromRules("fix_circumflex", 
        "::NFD; \u0306 > \u030C; ::NFC;", Transliterator.FORWARD);
        
    static Transliterator dropTones = Transliterator.createFromRules("drop_tones", 
        "::NFD; \u0304 > ; \u0301 > ; \u030C > ; \u0300 > ; \u0306 > ; ::NFC;", Transliterator.FORWARD);
    
    static {
        String dt = "1 > \u0304;\n"
                    + "2 <> \u0301;\n"
                    + "3 <> \u030C;\n"
                    + "4 <> \u0300;\n"
                    + "5 <> ;";
        
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
    	
    	digitPinyin_accentPinyin = Transliterator.createFromRules("digit-pinyin", dp, Transliterator.FORWARD);
    	System.out.println(digitPinyin_accentPinyin.transliterate("an2 aon2 oan2 ion2 oin2 uin2 iun2"));
    
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