/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/utility/Utility.java,v $
* $Date: 2001/08/31 00:19:16 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.utility;

import java.util.*;
import java.text.*;
import java.io.*;

public final class Utility {    // COMMON UTILITIES

    static final boolean UTF8 = true; // TODO -- make argument

    public static String getName(int i, String[] names) {
        try {
            return names[i];
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private static boolean needCRLF = false;

    public static void dot(int i) {
        if ((i % 0x7FF) == 0) {
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

    public static int setBits(int source, int start, int end) {
        if (start < end) {
            int temp = start;
            start = end;
            end = temp;
        }
        int bmstart = (1 << (start+1)) - 1;
        int bmend = (1 << end) - 1;
        bmstart &= ~bmend;
        return source |= bmstart;
    }

    public static int setBit(int source, int start) {
        return setBits(source, start, start);
    }

    public static int clearBits(int source, int start, int end) {
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

    public static int clearBit(int source, int start) {
        return clearBits(source, start, start);
    }

    public static int find(String source, String[] target) {
        for (int i = 0; i < target.length; ++i) {
            if (source.equalsIgnoreCase(target[i])) return i;
        }
        return -1;
    }

    public static byte lookup(String source, String[] target) {
        int result = Utility.find(source, target);
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

	public static String hex(byte[] o, int start, int end) {
	    StringBuffer result = new StringBuffer();
	    //int ch;
	    for (int i = start; i < end; ++i) {
	        if (i != 0) result.append(' ');
	        result.append(hex(o[i] & 0xFF, 2));
	    }
	    return result.toString();
	}

	public static String hex(char[] o, int start, int end) {
	    StringBuffer result = new StringBuffer();
	    for (int i = start; i < end; ++i) {
	        if (i != 0) result.append(' ');
	        result.append(hex(o[i], 4));
	    }
	    return result.toString();
	}

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

    public static String quoteXML(int c) {
        switch (c) {
            case '<': return "&lt;";
            case '>': return "&gt;";
            case '&': return "&amp;";
            case '\'': return "&apos;";
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

                return "#x" + hex(c,1) + ";";
        }

        // fix surrogates, since XML can't handle
        if (UTF32.isSurrogate(c)) {
            return "#x" + hex(c,1) + ";";
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

    public static String quoteXML(String source) {
        if (source == null) return "null";
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < source.length(); ++i) {
            int c = UTF32.char32At(source, i);
            if (UTF32.isSupplementary(c)) ++i;
            result.append(quoteXML(c));
        }
        return result.toString();
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

    public static String join(int[] array, String sep) {
        String result = "{";
        for (int i = 0; i < array.length; ++i) {
            if (i != 0) result += sep;
            result += array[i];
        }
        return result + "}";
    }

    public static String join(long[] array, String sep) {
        String result = "{";
        for (int i = 0; i < array.length; ++i) {
            if (i != 0) result += sep;
            result += array[i];
        }
        return result + "}";
    }

    private static final String[] searchPath = {
        "EXTRAS",
        "3.1.1",
        "3.1.0",
        "3.0.1",
        "3.0.0",
        "2.1.9",
        "2.0.0",
        "1.1.0",
    };

    private static final String DATA_DIR = "C:\\DATA";

    public static PrintWriter openPrintWriter(String filename) throws IOException {
        return new PrintWriter(
                    new UTF8StreamWriter(new FileOutputStream(DATA_DIR + File.separator + "GEN" + File.separator + filename),
                    32*1024));
    }

    public static BufferedReader openUnicodeFile(String filename, String version) throws IOException {
        // get all the files in the directory

        for (int i = 0; i < searchPath.length; ++i) {
            if (version.length() != 0 && version.compareTo(searchPath[i]) < 0) continue;

            String directoryName = DATA_DIR + File.separator + searchPath[i] + "-Update" + File.separator;
            System.out.println("Trying: '" + directoryName + "'");
            File directory = new File(directoryName);
            String[] list = directory.list();
            for (int j = 0; j < list.length; ++j) {
                String fn = list[j];
                if (!fn.endsWith(".txt")) continue;
                //System.out.print("\t'" + fn + "'");
                if (!fn.startsWith(filename)) {
                    //System.out.println(" -- MISS: '" + filename + "'");
                    continue;
                }
                //System.out.println(" -- HIT");
                System.out.println("\tFound: '" + fn + "'");
                return new BufferedReader(new FileReader(directoryName + fn),32*1024);
            }
        }
        return null;
    }



}