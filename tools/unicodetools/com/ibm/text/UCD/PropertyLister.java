/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/PropertyLister.java,v $
* $Date: 2001/08/31 00:30:17 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.io.*;
import com.ibm.text.utility.*;


abstract public class PropertyLister implements UCD_Types {

    static final boolean COMPRESS_NAMES = false;
    static final boolean DROP_INDICATORS = true;


    protected UCD ucdData;
    protected PrintStream output;
    protected boolean showOnConsole;
    protected boolean usePropertyComment = true;
    protected int firstRealCp = -2;
    protected int lastRealCp = -2;
    protected boolean alwaysBreaks = false; // set to true if property only breaks

    public static final byte INCLUDE = 0, BREAK = 1, CONTINUE = 2, EXCLUDE = 3;

    /**
     * @return status. Also have access to firstRealCp, lastRealCp
     */
    abstract public byte status(int cp);

    public String headerString() {
        return "";
    }

    public String propertyName(int cp) {
        return "";
    }

    public String optionalName(int cp) {
        return "";
    }

    public String optionalComment(int cp) {
        if (!usePropertyComment) return "";
        int cat = ucdData.getCategory(cp);
        if (cat == Lt || cat == Ll || cat == Lu) return "L&";
        return ucdData.getCategoryID(cp);
    }

    public int minPropertyWidth() {
        return 1;
    }

    public void format(int startCp, int endCp, int realCount) {
        try {
            String prop = propertyName(startCp);
            if (prop.length() > 0) prop = "; " + prop;
            String opt = optionalName(startCp);
            if (opt.length() > 0) opt = "; " + opt;
            String optCom = optionalComment(startCp);
            if (optCom.length() > 0) optCom += " ";
            String startName = getKenName(startCp);
            String line;
            String pgap = Utility.repeat(" ", minPropertyWidth() - prop.length() - opt.length());
            if (startCp != endCp) {
                String endName = getKenName(endCp);
                int bridge = endCp - startCp + 1 - realCount;
                String count = (bridge == 0) ? "" + realCount : realCount + "/" + bridge;
                String countStr = Utility.repeat(" ", 3-count.length()) + "[" + count + "] ";
                String gap = Utility.repeat(" ", 12 - width(startCp) - width(endCp));

                line = Utility.hex(startCp,4) + ".." + Utility.hex(endCp,4) + gap
                        + prop + opt + pgap + " # " + optCom
                        + countStr;
                if (startName.length() != 0 || endName.length() != 0) {
                    int com = 0;
                    if (COMPRESS_NAMES) com = commonInitialWords(startName, endName);
                    if (com == 0) {
                        line += startName + ".." + endName;
                    } else {
                        line += startName.substring(0,com)
                            + "(" + startName.substring(com) + ".." + endName.substring(com) + ")";
                    }
                }
            } else {
                String gap = alwaysBreaks
                    ? Utility.repeat(" ", 6 - width(startCp))
                    : Utility.repeat(" ", 14 - width(startCp));
                String gap2 = alwaysBreaks
                    ? " "
                    : "      ";
                line = Utility.hex(startCp,4) + gap
                        + prop + opt + pgap + " # " + optCom + gap2
                        + startName;
            }
            output.println(line);
            if (showOnConsole) System.out.println(line);
        } catch (Exception e) {
            throw new ChainException("Format error {0}, {1}",
                new Object[]{new Integer(startCp), new Integer(endCp)}, e);
        }
    }

    int width(int cp) {
        return cp <= 0xFFFF ? 4
             : cp <= 0xFFFFF ? 5
             : 6;
    }

    String getKenName(int cp) {
        String result = ucdData.getName(cp);
        if (result == null) return "";
        if (DROP_INDICATORS && result.charAt(0) == '<') {
            if (cp < 0xFF) return "<control>";
            return "";
        }
        return result;
    }


    /**
     * @return common initial substring length ending with SPACE or HYPHEN-MINUS. 0 if there is none
     */
    public static int commonInitialWords(String a, String b) {
        if (a.length() > b.length()) {
            String temp = a;
            a = b;
            b = temp;
        }
        int lastSpace = 0;
        for (int i = 0; i < a.length(); ++i) {
            char ca = a.charAt(i);
            char cb = b.charAt(i);
            if (ca != cb) return lastSpace;
            if (ca == ' ' || ca == '-') lastSpace = i + 1;
        }
        if (b.length() == a.length() || b.charAt(a.length()) == ' ' || b.charAt(a.length()) == '-') {
            lastSpace = a.length();
        }
        return lastSpace;
    }

    public int print() {
        int count = 0;
        firstRealCp = -1;
        byte firstRealCpCat = -1;
        lastRealCp = -1;
        int realRangeCount = 0;

        String header = headerString();
        if (header.length() != 0) {
            output.println(header);
            output.println();
        }
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            byte s = status(cp);
            if (s == INCLUDE && firstRealCp != -1) {
                byte cat = ucdData.getCategory(cp);
                if (cat == Lt || cat == Ll) cat = Lu;
                if (cat != firstRealCpCat) s = BREAK;
            }

            switch(s) {
              case CONTINUE:
                break; // do nothing
              case INCLUDE:
                if (firstRealCp == -1) {
                    firstRealCp = cp;
                    firstRealCpCat = ucdData.getCategory(firstRealCp);
                    if (firstRealCpCat == Lt || firstRealCpCat == Ll) firstRealCpCat = Lu;
                }
                lastRealCp = cp;
                count++;
                realRangeCount++;
                break;
              case BREAK:
                if (firstRealCp != -1) {
                    format(firstRealCp, lastRealCp, realRangeCount);
                }
                lastRealCp = firstRealCp = cp;
                firstRealCpCat = ucdData.getCategory(firstRealCp);
                if (firstRealCpCat == Lt || firstRealCpCat == Ll) firstRealCpCat = Lu;

                realRangeCount = 1;
                count++;
                break;
              case EXCLUDE:
                if (firstRealCp != -1) {
                    format(firstRealCp, lastRealCp, realRangeCount);
                    firstRealCp = -1;
                    realRangeCount = 0;
                }
                break;
            }
        }
        if (firstRealCp != -1) {
            format(firstRealCp, lastRealCp, realRangeCount);
        }

        if (count == 0) System.out.println("WARNING -- ZERO COUNT FOR " + header);
        output.println();
        output.println("# Total code points: " + count);
        output.println();
        return count;
    }
}