/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/utility/Utility.java,v $
* $Date: 2002/06/02 05:07:08 $
* $Revision: 1.17 $
*
*******************************************************************************
*/

package com.ibm.text.utility;

import java.util.*;
import java.text.*;
import java.io.*;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.text.UCD.*;

public final class Utility {    // COMMON UTILITIES

    static final boolean UTF8 = true; // TODO -- make argument
    
    public static String[] append(String[] array1, String[] array2) {
        String[] temp = new String[array1.length + array2.length];
        System.arraycopy(array1, 0, temp, 0, array1.length);
        System.arraycopy(array2, 0, temp, array1.length, array2.length);
        return temp;
    }

    public static String getName(int i, String[] names) {
        try {
            return names[i];
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private static boolean needCRLF = false;
    
    public static int DOTMASK = 0x7FF;

    public static void dot(int i) {
        if ((i % DOTMASK) == 0) {
            needCRLF = true;
            System.out.print('.');
        }
    }

    public static void fixDot() {
        if (needCRLF) {
            System.out.println();
            needCRLF = false;
        }
    }

    public static long setBits(long source, int start, int end) {
        if (start < end) {
            int temp = start;
            start = end;
            end = temp;
        }
        long bmstart = (1L << (start+1)) - 1;
        long bmend = (1L << end) - 1;
        bmstart &= ~bmend;
        return source |= bmstart;
    }

    public static long setBit(long source, int start) {
        return setBits(source, start, start);
    }

    public static long clearBits(long source, int start, int end) {
        if (start < end) {
            int temp = start;
            start = end;
            end = temp;
        }
        int bmstart = (1 << (start+1)) - 1;
        int bmend = (1 << end) - 1;
        bmstart &= ~bmend;
        return source &= ~bmstart;
    }

    public static long clearBit(long source, int start) {
        return clearBits(source, start, start);
    }

    public static int find(String source, String[] target, boolean skeletonize) {
        if (skeletonize) source = getSkeleton(source);
        for (int i = 0; i < target.length; ++i) {
            if (source.equals(getSkeleton(target[i]))) return i;
        }
        return -1;
    }
    
    /**
     * These routines use the Java functions, because they only need to act on ASCII.
     * Removes space, _, and lowercases.
     */
    
    public static String getSkeleton(String source) {
        skeletonBuffer.setLength(0);
        boolean gotOne = false;
        // remove spaces, '_', '-'
        // we can do this with char, since no surrogates are involved
        for (int i = 0; i < source.length(); ++i) {
            char ch = source.charAt(i);
            if (ch == '_' || ch == ' ' || ch == '-') {
                gotOne = true;
            } else {
                char ch2 = Character.toLowerCase(ch);
                if (ch2 != ch) {
                    gotOne = true;
                    skeletonBuffer.append(ch2);
                } else {
                    skeletonBuffer.append(ch);
                }
            }
        }
        if (!gotOne) return source; // avoid string creation
        return skeletonBuffer.toString();
    }
    
    private static StringBuffer skeletonBuffer = new StringBuffer();
    
    /**
     * These routines use the Java functions, because they only need to act on ASCII
     * Changes space, - into _, inserts _ between lower and UPPER.
     */
    
    public static String getUnskeleton(String source, boolean titlecaseStart) {
        if (source.equals("noBreak")) return source; // HACK
        StringBuffer result = new StringBuffer();
        int lastCat = -1;
        boolean haveFirstCased = true;
        for (int i = 0; i < source.length(); ++i) {
            char c = source.charAt(i);
            if (c == ' ' || c == '-') c = '_';
            int cat = Character.getType(c);
            if (lastCat == Character.LOWERCASE_LETTER && cat == Character.UPPERCASE_LETTER) {
                result.append('_');
            }
            if (haveFirstCased && (cat == Character.LOWERCASE_LETTER 
                    || cat == Character.TITLECASE_LETTER || cat == Character.UPPERCASE_LETTER)) {
                if (titlecaseStart) {
                    c = Character.toUpperCase(c);
                }
                haveFirstCased = false;
            }
            result.append(c);
            lastCat = cat;
        }
        return result.toString();
    }
    
    public static String findSubstring(String source, Set target, boolean invert) {
        Iterator it = target.iterator();
        while (it.hasNext()) {
            String other = it.next().toString();
            if ((other.indexOf(source) >= 0) == invert) return other;
        }
        return null;
    }

    public static byte lookup(String source, String[] target, boolean skeletonize) {
        int result = Utility.find(source, target, skeletonize);
        if (result != -1) return (byte)result;
        throw new ChainException("Could not find \"{0}\" in table [{1}]", new Object [] {source, target});
    }

    /**
     * Supplies a zero-padded hex representation of an integer (without 0x)
     */
    static public String hex(long i, int places) {
        if (i == Long.MIN_VALUE) return "-8000000000000000";
        boolean negative = i < 0;
        if (negative) {
            i = -i;
        }
        String result = Long.toString(i, 16).toUpperCase();
        if (result.length() < places) {
            result = "0000000000000000".substring(result.length(),places) + result;
        }
        if (negative) {
            return '-' + result;
        }
        return result;
    }

	public static String hex(long ch) {
	    return hex(ch,4);
	}

	public static String hex(byte ch) {
	    return hex(ch & 0xFF,2);
	}

	public static String hex(char ch) {
	    return hex(ch & 0xFFFF,4);
	}

	public static String hex(Object s) {
	    return hex(s, 4, " ");
	}

	public static String hex(Object s, int places) {
	    return hex(s, places, " ");
	}

	public static String hex(Object s, String separator) {
	    return hex(s, 4, separator);
	}

	public static String hex(Object o, int places, String separator) {
	    if (o == null) return "";
	    if (o instanceof Number) return hex(((Number)o).longValue(), places);

	    String s = o.toString();
	    StringBuffer result = new StringBuffer();
	    int ch;
	    for (int i = 0; i < s.length(); i += UTF32.count16(ch)) {
	        if (i != 0) result.append(separator);
	        ch = UTF32.char32At(s, i);
	        result.append(hex(ch));
	    }
	    return result.toString();
	}

	public static String hex(byte[] o, int start, int end, String separator) {
	    StringBuffer result = new StringBuffer();
	    //int ch;
	    for (int i = start; i < end; ++i) {
	        if (i != 0) result.append(separator);
	        result.append(hex(o[i]));
	    }
	    return result.toString();
	}

	public static String hex(char[] o, int start, int end, String separator) {
	    StringBuffer result = new StringBuffer();
	    for (int i = start; i < end; ++i) {
	        if (i != 0) result.append(separator);
	        result.append(hex(o[i]));
	    }
	    return result.toString();
	}

    /**
     * Returns a string containing count copies of s.
     * If count <= 0, returns "".
     */
	public static String repeat(String s, int count) {
	    if (count <= 0) return "";
	    if (count == 1) return s;
	    StringBuffer result = new StringBuffer(count*s.length());
	    for (int i = 0; i < count; ++i) {
	        result.append(s);
	    }
	    return result.toString();
	}

    public static int intFrom(String p) {
        if (p.length() == 0) return Short.MIN_VALUE;
        return Integer.parseInt(p);
    }

    public static float floatFrom(String p) {
        if (p.length() == 0) return Float.NaN;
        int fract = p.indexOf('/');
        if (fract == -1) return Float.valueOf(p).floatValue();
        String q = p.substring(0,fract);
        float num = 0;
        if (q.length() != 0) num = Integer.parseInt(q);
        p = p.substring(fract+1,p.length());
        float den = 0;
        if (p.length() != 0) den = Integer.parseInt(p);
        return num/den;
    }

    public static int codePointFromHex(String p) {
        String temp = Utility.fromHex(p);
        if (UTF32.length32(temp) != 1) throw new ChainException("String is not single (UTF32) character: " + p, null);
        return UTF32.char32At(temp, 0);
    }

    public static String fromHex(String p) {
        StringBuffer output = new StringBuffer();
        int value = 0;
        int count = 0;
        main:
        for (int i = 0; i < p.length(); ++i) {
            char ch = p.charAt(i);
            int digit = 0;
            switch (ch) {
                case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                    digit = ch - 'a' + 10;
                    break;
                case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                    digit = ch - 'A' + 10;
                    break;
                case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7':
                case '8': case '9':
                    digit = ch - '0';
                    break;
                default:
                    int type = Character.getType(ch);
                    if (type != Character.SPACE_SEPARATOR) {
                        throw new ChainException("bad hex value: '{0}' at position {1} in \"{2}\"",
                            new Object[] {String.valueOf(ch), new Integer(i), p});
                    }
                    // fall through!!
                case ' ': case ',': case ';': // do SPACE here, just for speed
                    if (count != 0) {
                        UTF32.append32(output, value);
                    }
                    count = 0;
                    value = 0;
                    continue main;
            }
            value <<= 4;
            value += digit;
            if (value > 0x10FFFF) {
                throw new ChainException("Character code too large: '{0}' at position {1} in \"{2}\"",
                    new Object[] {String.valueOf(ch), new Integer(i), p});
            }
            count++;
        }
        if (count != 0) {
            UTF32.append32(output, value);
        }
        return output.toString();
    }

    /**
     * Splits a string containing divider into pieces, storing in output
     * and returns the number of pieces.
     */
	public static int split(String s, char divider, String[] output) {
	    int last = 0;
	    int current = 0;
	    int i;
	    for (i = 0; i < s.length(); ++i) {
	        if (s.charAt(i) == divider) {
	            output[current++] = s.substring(last,i);
	            last = i+1;
	        }
	    }
	    output[current++] = s.substring(last,i);
	    int result = current;
	    while (current < output.length) {
	        output[current++] = "";
	    }
	    return result;
	}

	public static String[] split(String s, char divider) {
	    String[] result = new String[100];
	    int count = split(s, divider, result);
	    return extract(result, 0, count);
	}

	public static String[] extract(String[] source, int start, int end) {
	    String[] result = new String[end-start];
	    System.arraycopy(source, start, result, 0, end - start);
	    return result;
	}

	/*
	public static String quoteJava(String s) {
	    StringBuffer result = new StringBuffer();
	    for (int i = 0; i < s.length(); ++i) {
	        result.append(quoteJava(s.charAt(i)));
	    }
	    return result.toString();
	}
	*/
	public static String quoteJavaString(String s) {
	    if (s == null) return "null";
	    StringBuffer result = new StringBuffer();
	    result.append('"');
	    for (int i = 0; i < s.length(); ++i) {
	        result.append(quoteJava(s.charAt(i)));
	    }
	    result.append('"');
	    return result.toString();
	}

	public static String quoteJava(int c) {
	    switch (c) {
	      case '\\':
	        return "\\\\";
	      case '"':
	        return "\\\"";
	      case '\r':
	        return "\\r";
	      case '\n':
	        return "\\n";
	      default:
            if (c >= 0x20 && c <= 0x7E) {
                return String.valueOf((char)c);
            } else if (UTF32.isSupplementary(c)) {
                return "\\u" + hex((char)UTF32.getLead(c),4) + "\\u" + hex((char)UTF32.getTrail(c),4);
            } else {
                return "\\u" + hex((char)c,4);
            }
        }
	}

    public static String quoteXML(int c, boolean HTML) {
        switch (c) {
            case '<': return "&lt;";
            case '>': return "&gt;";
            case '&': return "&amp;";
            case '\'': if (!HTML) return "&apos;";
            	break;
            case '"': return "&quot;";

            // fix controls, since XML can't handle

            // also do this for 09, 0A, and 0D, so we can see them.
            case 0x00: case 0x01: case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07:
            case 0x08: case 0x09: case 0x0A: case 0x0B: case 0x0C: case 0x0D: case 0x0E: case 0x0F:
            case 0x10: case 0x11: case 0x12: case 0x13: case 0x14: case 0x15: case 0x16: case 0x17:
            case 0x18: case 0x19: case 0x1A: case 0x1B: case 0x1C: case 0x1D: case 0x1E: case 0x1F:
            case 0x7F:

             // fix noncharacters, since XML can't handle
            case 0xFFFE: case 0xFFFF:

                return "#" + hex(c,1);
        }

        // fix surrogates, since XML can't handle
        if (UTF32.isSurrogate(c)) {
            return "#" + hex(c,1);
        }

        if (c <= 0x7E || UTF8) {
            return UTF32.valueOf32(c);
        }

        // fix supplementaries & high characters, because of IE bug
        /*if (UTF32.isSupplementary(c) || 0xFFF9 <= c && c <= 0xFFFD) {
            return "#x" + hex(c,1) + ";";
        }
        */

        return "&#x" + hex(c,1) + ";";
    }

    public static String quoteXML(String source, boolean HTML) {
        if (source == null) return "null";
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < source.length(); ++i) {
            int c = UTF32.char32At(source, i);
            if (UTF32.isSupplementary(c)) ++i;
            result.append(quoteXML(c, HTML));
        }
        return result.toString();
    }
    
    public static String quoteXML(String source) {
    	return quoteXML(source, false);
    }
    
    public static String quoteXML(int source) {
    	return quoteXML(source, false);
    }
    

    public static int compare(char[] a, int aStart, int aEnd, char[] b, int bStart, int bEnd) {
        while (aStart < aEnd && bStart < bEnd) {
            int diff = a[aStart++] - b[bStart++];
            if (diff != 0) return diff;
        }
        return (aEnd - aStart) - (bEnd - bStart);
    }

    public static int compare(byte[] a, int aStart, int aEnd, byte[] b, int bStart, int bEnd) {
        while (aStart < aEnd && bStart < bEnd) {
            int diff = a[aStart++] - b[bStart++];
            if (diff != 0) return diff;
        }
        return (aEnd - aStart) - (bEnd - bStart);
    }

    public static int compareUnsigned(byte[] a, int aStart, int aEnd, byte[] b, int bStart, int bEnd) {
        while (aStart < aEnd && bStart < bEnd) {
            int diff = (a[aStart++] & 0xFF) - (b[bStart++] & 0xFF);
            if (diff != 0) return diff;
        }
        return (aEnd - aStart) - (bEnd - bStart);
    }

    /**
     * Joins an array together, using divider between the pieces
     */
    public static String join(int[] array, String divider) {
        String result = "{";
        for (int i = 0; i < array.length; ++i) {
            if (i != 0) result += divider;
            result += array[i];
        }
        return result + "}";
    }

    public static String join(long[] array, String divider) {
        String result = "{";
        for (int i = 0; i < array.length; ++i) {
            if (i != 0) result += divider;
            result += array[i];
        }
        return result + "}";
    }

    private static final String[] searchPath = {
        "EXTRAS",
        "3.2.0",
        "3.1.1",
        "3.1.0",
        "3.0.1",
        "3.0.0",
        "2.1.9",
        "2.1.8",
        "2.1.5",
        "2.1.2",
        "2.0.0",
        "1.1.0",
    };

    public static PrintWriter openPrintWriter(String filename) throws IOException {
        return openPrintWriter(filename, true, true);
    }
    
    // Normally use false, false.
    // But for UCD files use true, true
    // Or if they are UTF8, use true, false
    public static PrintWriter openPrintWriter(String filename, boolean removeCR, boolean latin1) throws IOException {
        File file = new File(getOutputName(filename));
        System.out.println("Creating File: " + file);
        File parent = new File(file.getParent());
        //System.out.println("Creating File: "+ parent);
        parent.mkdirs();
        return new PrintWriter(
                    new UTF8StreamWriter(
                        new FileOutputStream(file),
                        32*1024,
                        removeCR, latin1));
    }
    
    public static String getOutputName(String filename) {
        return UCD_Types.GEN_DIR + filename;
    }
    
    public static void print(PrintWriter pw, Collection c, String separator) {
        print(pw, c, separator, null);
    }
    
    public interface Breaker {
        public String get(Object current, Object old);
        public boolean filter(Object current); // true is keep
    }
    
    public static void print(PrintWriter pw, Collection c, String separator, Breaker b) {
        Iterator it = c.iterator();
        boolean first = true;
        Object last = null;
        while (it.hasNext()) {
            Object obj = it.next();
            if (b != null && !b.filter(obj)) continue;
            if (first) {
                first = false;
            } else {
                pw.print(separator);
            }
            if (b != null) {
                pw.print(b.get(obj, last));
            } else {
                pw.print(obj);
            }
            last = obj;
        }
    }
    
    public static void appendFile(String filename, boolean utf8, PrintWriter output) throws IOException {
    	appendFile(filename, utf8, output, null);
    }

    public static void appendFile(String filename, boolean utf8, PrintWriter output, String[] replacementList) throws IOException {
        FileInputStream fis = new FileInputStream(filename);
        InputStreamReader isr = utf8 ? new InputStreamReader(fis, "UTF8") :  new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr, 32*1024);
        while (true) {
            String line = br.readLine();
            if (line == null) break;
            if (replacementList != null) {
            	for (int i = 0; i < replacementList.length; i += 2) {
            		line = replace(line, replacementList[i], replacementList[i+1]);
            	}
            }
            output.println(line);
        }
    }
    
    public static void renameIdentical(String file1, String file2) throws IOException {
        if (file1 == null) {
            System.out.println("Null file");
            return;
        }
        
        boolean identical = false;
        BufferedReader br1 = new BufferedReader(new FileReader(file1), 32*1024);
        BufferedReader br2 = new BufferedReader(new FileReader(file2), 32*1024);
        String line1 = "";
        String line2 = "";
        try {
            for (int lineCount = 0; ; ++lineCount) {
                line1 = getLineWithoutFluff(br1, lineCount == 0);
                line2 = getLineWithoutFluff(br2, lineCount == 0);
                if (line1 == null) {
                    if (line2 == null) identical = true;
                    break;
                }
                if (!line1.equals(line2)) {
                    break;
                }
            }
        } finally {
            br1.close();
            br2.close();
        }
        if (identical) {
            File foo = new File(file2);
            File newName = new File(foo.getParent(), "UNCHANGED-" + foo.getName());
            if (newName.exists()) {
                for (int i = 1; newName.exists(); ++i) {
                    newName = new File(foo.getParent(), "UNCHANGED" + i + "-" + foo.getName());
                }
            }
            System.out.println("IDENTICAL TO PREVIOUS, RENAMING : " + foo);
            System.out.println("TO : " + newName);
            boolean renameResult = foo.renameTo(newName);
            if (!renameResult) System.out.println("Couldn't rename!");
        } else {
            System.out.println("Found difference in : " + file1 + ", " + file2);
            int diff = compare(line1, line2);
            System.out.println(" Line1: '" + line1.substring(0,diff) + "', '" + line1.substring(diff));
            System.out.println(" Line2: '" + line2.substring(0,diff) + "', '" + line2.substring(diff));
        }
    }
    
    static String getLineWithoutFluff(BufferedReader br1, boolean first) throws IOException {
        while (true) {
            String line1 = br1.readLine();
            if (line1 == null) return line1;
            line1 = line1.trim();
            if (line1.length() == 0) continue;
            if (line1.equals("#")) continue;
            if (line1.startsWith("# Generated")) continue;
            if (line1.startsWith("# Date")) continue;
            if (first && line1.startsWith("#")) {
                first = false;
                continue;
            }
            return line1;
        }
    }    
    
    /** Returns -1 if strings are equal; otherwise the position they are different at
    */
    public static int compare(String a, String b) {
        int len = a.length();
        if (len > b.length()) len = b.length();
        for (int i = 0; i < len; ++i) {
            if (a.charAt(i) != b.charAt(i)) return i;
        }
        if (a.length() != b.length()) return len;
        return -1;
    }
    
    public static void copyTextFile(String filename, boolean utf8, String newName, String[] replacementList) throws IOException {
        PrintWriter out = Utility.openPrintWriter(newName, false, false);
        appendFile(filename, utf8, out, replacementList);
        out.close();
    }

    public static void copyTextFile(String filename, boolean utf8, String newName) throws IOException {
        copyTextFile(filename, utf8, newName, null);
    }

    public static BufferedReader openUnicodeFile(String filename, String version, boolean show) throws IOException {
        String name = getMostRecentUnicodeDataFile(filename, version, true, show);
        if (name == null) return null;
        return new BufferedReader(new FileReader(name),32*1024);
    }

    public static String getMostRecentUnicodeDataFile(String filename, String version, 
      boolean acceptLatest, boolean show) throws IOException {
        // get all the files in the directory

        int compValue = acceptLatest ? 0 : 1;
        for (int i = 0; i < searchPath.length; ++i) {
            if (version.length() != 0 && version.compareTo(searchPath[i]) < compValue) continue;

            String directoryName = UCD_Types.UCD_DIR + File.separator + searchPath[i] + "-Update" + File.separator;
            if (show) System.out.println("Trying: '" + directoryName + "', '" + filename + "'");
            String result = searchDirectory(new File(directoryName), filename, show);
            if (result != null) return result;
            
        }
        return null;
    }
    
    public static Set getDirectoryContentsLastFirst(File directory) {
        Set result = new TreeSet(new Comparator() {
                public int compare(Object a, Object b) {
                    return ((Comparable) b).compareTo(a);
                }
            });
        result.addAll(java.util.Arrays.asList(directory.list()));
        return result;
    }
    
    public static String searchDirectory(File directory, String filename, boolean show) throws IOException {
        Iterator it = getDirectoryContentsLastFirst(directory).iterator();
        while (it.hasNext()) {
            String fn = (String) it.next();
            File foo = new File(directory + File.separator + fn);
            // System.out.println("\tChecking: '" + foo.getCanonicalPath() + "'");
            if (foo.isDirectory()) {
                String attempt = searchDirectory(foo, filename, show);
                if (attempt != null) return attempt;
            }
            if (fn.endsWith(".txt") && fn.startsWith(filename)) {
                if (show) System.out.println("\tFound: '" + fn + "'");
                return foo.getCanonicalPath();
            }
        }
        return null;
    }

    public static void writeHtmlHeader(PrintWriter log, String title) {
        log.println("<html><head>");
        log.println("<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        log.println("<title>" + title + "</title>");
        log.println("<style><!--");
        log.println("table        { border-collapse: collapse; border: 1 solid blue }");
        log.println("td           { border: 1 solid blue; padding: 2 }");
        log.println("th           { border: 1 solid blue; padding: 2 }");
        log.println("--></style>");
        log.println("</head><body>");
    }
    
    /**
     * Replaces all occurances of piece with replacement, and returns new String
     */
    public static String replace(String source, String piece, String replacement) {
    	int pos = 0;
        while (true) {
            pos = source.indexOf(piece, pos);
            if (pos < 0) return source;
            source = source.substring(0,pos) + replacement + source.substring(pos + piece.length());
            if (replacement.length() > 0) ++pos;
        }
    }
    
    public static String getStack() {
        Exception e = new Exception();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        return "Showing Stack with fake " + sw.getBuffer().toString();
    }
    
    public static void showSetNames(String prefix, UnicodeSet set, boolean all, UCD ucd) {
        int count = set.getRangeCount();
        for (int i = 0; i < count; ++i) {
            int start = set.getRangeStart(i);
            int end = set.getRangeEnd(i);
            if (all) {
                for (int cp = start; cp <= end; ++cp) {
                    if (!set.contains(cp)) continue;
                    System.out.println(prefix + ucd.getCodeAndName(cp));
                }
            } else {
                System.out.println(prefix + ucd.getCode(start)
                    + ((start != end) ? (".." + ucd.getCode(end)) : "")
                    + "\t# " + ucd.getName(start)
                    + ((start != end) ? (".." + ucd.getName(end)) : "")
                );
            }
        }
    }
}