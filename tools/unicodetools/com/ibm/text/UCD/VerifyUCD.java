/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/VerifyUCD.java,v $
* $Date: 2002/03/20 00:21:42 $
* $Revision: 1.11 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.io.IOException;
import java.math.BigDecimal;

//import com.ibm.text.unicode.UInfo;
import java.util.*;
import java.io.*;
//import java.text.Un;
import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UTF16;
import com.ibm.text.utility.*;

public class VerifyUCD implements UCD_Types {
    
    public static void verify() throws IOException {
        Main.setUCD();
        
        checkIdentical("ea=h", "dt=nar");
        checkIdentical("ea=f", "dt=wide");
        checkIdentical("gc=ps", "lb=op");
        checkIdentical("lb=sg", "gc=cs");

/*
For LB we now have:

GC:Ps == LB:OP
GC:Nd && !(EA:F)

Try these on for size, and report any discrepancies

>GC:L& && EA:W -> LB:ID
>GC:L& && EA:A -> LB:AI
>GC:L& && EA:N -> LB:AL
>GC:L& && EA:Na -> LB:AL

plus

>LB:ID contains Ideo:T

Also, try these rules

GC:S# && EA:W -> LB:ID
GC:S# && EA:A -> LB:AI
GC:S# && EA:N -> LB:AL
GC:S# && EA:Na -> LB:AL

where S# is Sm | Sk | So

these will generate exceptions, but I need to see the list to them before I
can help you narrow these down.

>The trivial ones that I could glean from reading the TR are
>LB:SG == GC:Cs
>GC:Pi -> LB:QU
>GC:Pf -> LB:QU
>GC:Mc -> LB:CM
>GC:Me -> LB:CM
>GC:Mn -> LB:CM
>GC:Pe -> LB:CL
*/
    }
    
    static final void checkCase3 () {
        Main.setUCD();
        
        checkNF_AndCase("\u0130", true);
        checkNF_AndCase("\u0131", true);
        
        UnicodeProperty softdot = null;
        CanonicalIterator cit = new CanonicalIterator("a");
        UnicodeSet badChars = new UnicodeSet();
        
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!Main.ucd.isAllocated(cp)) continue;
            byte cat = Main.ucd.getCategory(cp);
        // check if canonical equivalents are case-mapped to canonical equivalents
            if (cat != PRIVATE_USE && cat != SURROGATE) {
                String str = UTF16.valueOf(cp);
                if (!checkNF_AndCase(str, false)) badChars.add(cp);
                //if (Main.ucd.getScript(cp) != GREEK_SCRIPT) continue;
                str += "\u0334";
                try {
                    //System.out.println("Check " + Main.ucd.getCodeAndName(str));
                    cit.setSource(str);
                    while (true) {
                        String s = cit.next();
                        if (s == null) break;
                        if (s.equals(str)) continue; // don't check twice
                        
                        //System.out.println("  Checking " + Main.ucd.getCodeAndName(s));
                        if (!checkNF_AndCase(s, false)) badChars.add(cp);
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    System.out.println("Problem with " + Main.ucd.getCodeAndName(str));
                    throw e;
                }
                
            }
            
            if (false) {
                if (softdot == null) softdot = DerivedProperty.make(Type_i, Main.ucd);
                if (Main.ucd.getBinaryProperty(cp, Soft_Dotted) !=
                    softdot.hasValue(cp)) {
                    System.out.println("FAIL: " + Main.ucd.getCodeAndName(cp));
                    System.out.println("Soft_Dotted='" + Main.ucd.getBinaryPropertiesID(cp, Soft_Dotted)
                        + "', DerivedSD=" + softdot.getValue(cp) + "'");
                }
            }
                        
        }
        System.out.println();
        Utility.showSetNames("", badChars, false, Main.ucd);
    }
    
    static void checkIdentical(String ubpName1, String ubpName2) {
        UnicodeProperty prop1 = UnifiedBinaryProperty.make(ubpName1, Main.ucd);
        UnicodeSet set1 = prop1.getSet();
        UnicodeProperty prop2 = UnifiedBinaryProperty.make(ubpName2, Main.ucd);
        UnicodeSet set2 = prop2.getSet();
        UnicodeSet set1minus2 = new UnicodeSet(set1);
        set1minus2.removeAll(set2);
        UnicodeSet set2minus1 = new UnicodeSet(set2);
        set2minus1.removeAll(set1);
        
        if (set1minus2.isEmpty() && set2minus1.isEmpty()) {
            System.out.println("PASS: " + prop1.getFullName(LONG) + " == " + prop2.getFullName(LONG));
            System.out.println();
            return;
        }
        System.out.println("FAIL: " + prop1.getFullName(LONG) + " != " + prop2.getFullName(LONG));
        if (!set1minus2.isEmpty()) {
            System.out.println(" In " + prop1.getFullName(LONG) + " but not " + prop2.getFullName(LONG));
            Utility.showSetNames("  " + prop1.getFullName(SHORT) + ": ", set1minus2, false, Main.ucd);
        }
        if (!set2minus1.isEmpty()) {
            System.out.println(" In " + prop2.getFullName(LONG) + " but not " + prop1.getFullName(LONG));
            Utility.showSetNames("  " + prop2.getFullName(SHORT) + ": ", set2minus1, false, Main.ucd);
        }
        System.out.println();
    }
    
    static boolean checkNF_AndCase(String source, boolean both) {
        boolean result = true;
        String decomp = Main.nfd.normalize(source);
        if (!decomp.equals(source)) {
            
            result &= checkNFC("Lower", source, decomp, Main.ucd.getCase(source, FULL, LOWER), Main.ucd.getCase(decomp, FULL, LOWER));
            result &= checkNFC("Upper", source, decomp, Main.ucd.getCase(source, FULL, UPPER), Main.ucd.getCase(decomp, FULL, UPPER));
            result &= checkNFC("Title", source, decomp, Main.ucd.getCase(source, FULL, TITLE), Main.ucd.getCase(decomp, FULL, TITLE));
            result &= checkNFC("Fold", source, decomp, Main.ucd.getCase(source, FULL, FOLD), Main.ucd.getCase(decomp, FULL, FOLD));
            
            if (!both) return result;
            
            result &= checkNFC("SLower", source, decomp, Main.ucd.getCase(source, SIMPLE, LOWER), Main.ucd.getCase(decomp, SIMPLE, LOWER));
            result &= checkNFC("SUpper", source, decomp, Main.ucd.getCase(source, SIMPLE, UPPER), Main.ucd.getCase(decomp, SIMPLE, UPPER));
            result &= checkNFC("STitle", source, decomp, Main.ucd.getCase(source, SIMPLE, TITLE), Main.ucd.getCase(decomp, SIMPLE, TITLE));
            result &= checkNFC("SFold", source, decomp, Main.ucd.getCase(source, SIMPLE, TITLE), Main.ucd.getCase(decomp, SIMPLE, TITLE));
        }
        return result;
    }
    
    static final boolean SHOW_NFC_DIFFERENCE = false;
    
    static boolean checkNFC(String label, String source, String decomp, String casedCp, String casedDecomp) {
        if (!Main.nfd.normalize(casedCp).equals(Main.nfd.normalize(casedDecomp))) {
            if (SHOW_NFC_DIFFERENCE) {
                Utility.fixDot();
                System.out.println("FAIL CASE CE: " + label + " (" + Main.ucd.getCodeAndName(source) + ")");
                System.out.println("\t" + Main.ucd.getCode(source) + " => " + Main.ucd.getCode(casedCp));
                System.out.println("\t" + Main.ucd.getCode(decomp) + " => " + Main.ucd.getCode(casedDecomp));
            }
            return false;
        }
        return true;
    }

    public static final String IDN_DIR = BASE_DIR + "\\IDN\\";

        /*
        System.out.println(Main.ucd.toString(0x0387));
        System.out.println(Main.ucd.toString(0x00B7));
        System.out.println(Main.ucd.toString(0x03a3));
        System.out.println(Main.ucd.toString(0x03c2));
        System.out.println(Main.ucd.toString(0x03c3));
        System.out.println(Main.ucd.toString(0x0069));
        System.out.println(Main.ucd.toString(0x0130));
        System.out.println(Main.ucd.toString(0x0131));
        System.out.println(Main.ucd.toString(0x0345));
        */

    static void checkAgainstOtherVersion(String otherVersion) {
        Main.setUCD();
        UCD ucd2 = UCD.make(otherVersion);
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            UData curr = Main.ucd.get(cp, true);
            UData other = ucd2.get(cp, true);
            if (!curr.equals(other)) {
                System.out.println("Difference at " + Main.ucd.getCodeAndName(cp));
                System.out.println(curr);
                System.out.println(curr);
                System.out.println();
            }
        }
    }

    static void generateXML() throws IOException {
        Main.setUCD();
        String filename = "UCD.xml";
        PrintWriter log = Utility.openPrintWriter(filename);

         //log.println('\uFEFF');
        log.println("<ucd>");

        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!Main.ucd.isRepresented(cp)) continue;
            if (cp == 0xE0026 || cp == 0x20000) {
                System.out.println("debug");
            }
            log.println(Main.ucd.toString(cp));
        }

        log.println("</ucd>");
        log.close();
    }

    static final byte MIXED = (byte)(UNCASED + 1);

    public static void checkCase() throws IOException {
        Main.setUCD();
        Utility.fixDot();
        System.out.println("checkCase");
        
        String test = "The qui'ck br\u2019own 'fox jum\u00ADped ov\u200Ber th\u200Ce lazy dog.";
        
        String ttest = Main.ucd.getCase(test, FULL, TITLE);
        
        PrintWriter titleTest = Utility.openPrintWriter("TestTitle.txt");
        titleTest.println(test);
        titleTest.println(ttest);
        titleTest.close();
        
        System.out.println(Main.ucd.getCase("ABC,DE'F G\u0308H", FULL, TITLE));
        String fileName = "CaseDifferences.txt";
        PrintWriter log = Utility.openPrintWriter(fileName);

        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!Main.ucd.isRepresented(cp) || Main.ucd.isPUA(cp)) continue;
            if (cp == '\u3371') {
               System.out.println("debug");
            }
            String x = Main.nfkd.normalize(cp);
            String xu = Main.ucd.getCase(x, FULL, UPPER);
            String xl = Main.ucd.getCase(x, FULL, LOWER);
            String xt = Main.ucd.getCase(x, FULL, TITLE);

            byte caseCat = MIXED;
            if (xu.equals(xl)) caseCat = UNCASED;
            else if (x.equals(xl)) caseCat = LOWER;
            else if (x.equals(xu)) caseCat = UPPER;
            else if (x.equals(xt)) caseCat = TITLE;

            byte cat = Main.ucd.getCategory(cp);
            boolean otherLower = Main.ucd.getBinaryProperty(cp, Other_Lowercase);
            boolean otherUpper = Main.ucd.getBinaryProperty(cp, Other_Uppercase);
            byte oldCaseCat = (cat == Lu || otherUpper) ? UPPER
                : (cat == Ll || otherLower) ? LOWER
                : (cat == Lt) ? TITLE
                : UNCASED;

            if (caseCat != oldCaseCat) {
                log.println(UTF32.valueOf32(cp)
                    + "\t" + names[caseCat]
                    + "\t" + names[oldCaseCat]
                    + "\t" + Main.ucd.getCategoryID_fromIndex(cat)
                    + "\t" + lowerNames[otherLower ? 1 : 0]
                    + "\t" + upperNames[otherUpper ? 1 : 0]
                    + "\t" + Main.ucd.getCodeAndName(cp)
                    + "\t" + Main.ucd.getCodeAndName(x)
                    + "\t" + Main.ucd.getCodeAndName(xu)
                    + "\t" + Main.ucd.getCodeAndName(xl)
                    + "\t" + Main.ucd.getCodeAndName(xt)
                );
            }
        }

        log.close();
    }

    public static void checkCase2(boolean longForm) throws IOException {
        Main.setUCD();
        Utility.fixDot();
        System.out.println("checkCase");
        
        /*String tx1 = "\u0391\u0342\u0345";
        String ux1 = "\u0391\u0342\u0399";
        String ctx1 = nfc.normalize(tx1);
        String ctx2 = nfc.normalize(ux1); // wrong??

        //System.out.println(Main.ucd.getCase("ABC,DE'F G\u0308H", FULL, TITLE));
        */
        
        
        String fileName = "CaseNormalizationDifferences.txt";
        PrintWriter log = Utility.openPrintWriter(fileName);

        log.println("Differences between case(normalize(cp)) and normalize(case(cp))");
        log.println("u, l, t - upper, lower, title");
        log.println("c, d - nfc, nfd");
        
        //Utility.DOTMASK = 0x7F;

        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!Main.ucd.isRepresented(cp) || Main.ucd.isPUA(cp)) continue;
            if (cp == '\u0130') {
               System.out.println("debug");
            }

            String x = UTF32.valueOf32(cp);
            String dx = Main.nfd.normalize(cp);
            String cx = Main.nfc.normalize(cp);

            String ux = Main.ucd.getCase(x, FULL, UPPER);
            String lx = Main.ucd.getCase(x, FULL, LOWER);
            String tx = Main.ucd.getCase(x, FULL, TITLE);
            
            if (x.equals(dx) && dx.equals(cx) && cx.equals(ux) && ux.equals(lx) && lx.equals(tx)) continue;

            String cux = Main.nfc.normalize(ux);
            String clx = Main.nfc.normalize(lx);
            String ctx = Main.nfc.normalize(tx);
            
            if (x.equals(cx)) {
                boolean needBreak = false;
                if (!clx.equals(lx)) needBreak = true;
                if (!ctx.equals(tx)) needBreak = true;
                if (!cux.equals(ux)) needBreak = true;
                
                if (needBreak) {
                    log.println("# Was not NFC:");
                    log.println(
                        "## " + Utility.hex(x) + "; "
                        + Utility.hex(lx) + "; "
                        + Utility.hex(tx) + "; "
                        + Utility.hex(ux) + "; # "
                        + Main.ucd.getName(x));
                    log.println("#   should be:");
                    log.println(
                        Utility.hex(x) + "; "
                        + Utility.hex(clx) + "; "
                        + Utility.hex(ctx) + "; "
                        + Utility.hex(cux) + "; # "
                        + Main.ucd.getName(x));
                    log.println();
                }
            }
                       
            String dux = Main.nfd.normalize(ux);
            String dlx = Main.nfd.normalize(lx);
            String dtx = Main.nfd.normalize(tx);
            
            
            
            String startdx = getMarks(dx, false);
            String enddx = getMarks(dx, true);

            String startdux = getMarks(dux, false);
            String enddux = getMarks(dux, true);

            String startdtx = getMarks(dtx, false);
            String enddtx = getMarks(dtx, true);

            String startdlx = getMarks(dlx, false);
            String enddlx = getMarks(dlx, true);
            
            // If the new marks don't occur in the old decomposition, we got a problem!
            
            if (!startdx.startsWith(startdux) || !startdx.startsWith(startdtx) || !startdx.startsWith(startdlx)
              || !enddx.endsWith(enddux) || !enddx.endsWith(enddtx) || !enddx.endsWith(enddlx)) {
                log.println("Combining Class Difference for " + Main.ucd.getCodeAndName(x));
                log.println("x:  " + Main.ucd.getCodeAndName(dx) + ", " + Utility.hex(startdx) + ", " + Utility.hex(enddx));
                log.println("ux: " + Main.ucd.getCodeAndName(dux) + ", " + Utility.hex(startdux) + ", " + Utility.hex(enddux));
                log.println("tx: " + Main.ucd.getCodeAndName(dtx) + ", " + Utility.hex(startdtx) + ", " + Utility.hex(enddtx));
                log.println("lx: " + Main.ucd.getCodeAndName(dlx) + ", " + Utility.hex(startdlx) + ", " + Utility.hex(enddlx));
                log.println();
            }
            

            if (!longForm) continue;
                        
            String udx = Main.ucd.getCase(dx, FULL, UPPER);
            String ldx = Main.ucd.getCase(dx, FULL, LOWER);
            String tdx = Main.ucd.getCase(dx, FULL, TITLE);

            String ucx = Main.ucd.getCase(cx, FULL, UPPER);
            String lcx = Main.ucd.getCase(cx, FULL, LOWER);
            String tcx = Main.ucd.getCase(cx, FULL, TITLE);

            String dudx = Main.nfd.normalize(udx);
            String dldx = Main.nfd.normalize(ldx);
            String dtdx = Main.nfd.normalize(tdx);

            String cucx = Main.nfc.normalize(ucx);
            String clcx = Main.nfc.normalize(lcx);
            String ctcx = Main.nfc.normalize(tcx);


            if (!dux.equals(udx)
                || !dlx.equals(ldx)
                || !dtx.equals(tdx)
                || !cux.equals(ucx)
                || !clx.equals(lcx)
                || !ctx.equals(tcx)
                || !dux.equals(dudx)
                || !dlx.equals(dldx)
                || !dtx.equals(dtdx)
                || !cux.equals(cucx)
                || !clx.equals(clcx)
                || !ctx.equals(ctcx)
                ) {
                    log.println();
                    log.println("Difference at " + Main.ucd.getCodeAndName(cp));
                    if (!x.equals(ux)) log.println("\tu(cp):\t" + Main.ucd.getCodeAndName(ux));
                    if (!x.equals(lx)) log.println("\tl(cp):\t" + Main.ucd.getCodeAndName(lx));
                    if (!tx.equals(ux)) log.println("\tt(cp):\t" + Main.ucd.getCodeAndName(tx));
                    if (!x.equals(dx)) log.println("\td(cp):\t" + Main.ucd.getCodeAndName(dx));
                    if (!x.equals(cx)) log.println("\tc(cp):\t" + Main.ucd.getCodeAndName(cx));

                if (!dux.equals(udx)) {
                    log.println();
                    log.println("\td(u(cp)):\t" + Main.ucd.getCodeAndName(dux));
                    log.println("\tu(d(cp)):\t" + Main.ucd.getCodeAndName(udx));
                }
                if (!dlx.equals(ldx)) {
                    log.println();
                    log.println("\td(l(cp)):\t" + Main.ucd.getCodeAndName(dlx));
                    log.println("\tl(d(cp)):\t" + Main.ucd.getCodeAndName(ldx));
                }
                if (!dtx.equals(tdx)) {
                    log.println();
                    log.println("\td(t(cp)):\t" + Main.ucd.getCodeAndName(dtx));
                    log.println("\tt(d(cp)):\t" + Main.ucd.getCodeAndName(tdx));
                }

                if (!cux.equals(ucx)) {
                    log.println();
                    log.println("\tc(u(cp)):\t" + Main.ucd.getCodeAndName(cux));
                    log.println("\tu(c(cp)):\t" + Main.ucd.getCodeAndName(ucx));
                }
                if (!clx.equals(lcx)) {
                    log.println();
                    log.println("\tc(l(cp)):\t" + Main.ucd.getCodeAndName(clx));
                    log.println("\tl(c(cp)):\t" + Main.ucd.getCodeAndName(lcx));
                }
                if (!ctx.equals(tcx)) {
                    log.println();
                    log.println("\tc(t(cp)):\t" + Main.ucd.getCodeAndName(ctx));
                    log.println("\tt(c(cp)):\t" + Main.ucd.getCodeAndName(tcx));
                }

                // ...........

                if (!udx.equals(dudx)) {
                    log.println();
                    log.println("\tu(d(cp)):\t" + Main.ucd.getCodeAndName(udx));
                    log.println("\td(u(d(cp))):\t" + Main.ucd.getCodeAndName(dudx));
                }
                if (!ldx.equals(dldx)) {
                    log.println();
                    log.println("\tl(d(cp)):\t" + Main.ucd.getCodeAndName(ldx));
                    log.println("\td(l(d(cp))):\t" + Main.ucd.getCodeAndName(dldx));
                }
                if (!tdx.equals(dtdx)) {
                    log.println();
                    log.println("\tt(d(cp)):\t" + Main.ucd.getCodeAndName(tdx));
                    log.println("\td(t(d(cp))):\t" + Main.ucd.getCodeAndName(dtdx));
                }

                if (!ucx.equals(cucx)) {
                    log.println();
                    log.println("\tu(c(cp)):\t" + Main.ucd.getCodeAndName(ucx));
                    log.println("\tc(u(c(cp))):\t" + Main.ucd.getCodeAndName(cucx));
                }
                if (!lcx.equals(clcx)) {
                    log.println();
                    log.println("\tl(c(cp)):\t" + Main.ucd.getCodeAndName(lcx));
                    log.println("\tc(l(c(cp))):\t" + Main.ucd.getCodeAndName(clcx));
                }
                if (!tcx.equals(ctcx)) {
                    log.println();
                    log.println("\tt(c(cp)):\t" + Main.ucd.getCodeAndName(tcx));
                    log.println("\tc(t(c(cp))):\t" + Main.ucd.getCodeAndName(ctcx));
                }
            }
        }

        log.close();
    }
    
    public static String getMarks(String s, boolean doEnd) {
        int cp;
        if (!doEnd) {
            for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(s, i);
                int cc = Main.ucd.getCombiningClass(cp);
                if (cc == 0) {
                    return s.substring(0, i);
                }
            }
        } else {
            for (int i = s.length(); i > 0; i -= UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(s, i-1); // will go 2 before if necessary
                int cc = Main.ucd.getCombiningClass(cp);
                if (cc == 0) {
                    return s.substring(i);
                }
            }
        }
        return s;
    }

    static final String names[] = {"LOWER", "TITLE", "UPPER", "(UNC)", "MIXED"};
    static final String lowerNames[] = {"", "Other_Lower"};
    static final String upperNames[] = {"", "Other_Upper"};

    public static void CheckCaseFold() {
        Main.setUCD();
        System.out.println("Checking Case Fold");
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!Main.ucd.isAssigned(cp) || Main.ucd.isPUA(cp)) continue;
            String fullTest = Main.ucd.getCase(Main.ucd.getCase(cp, FULL, UPPER), FULL, LOWER);
            String simpleTest = Main.ucd.getCase(Main.ucd.getCase(cp, SIMPLE, UPPER), SIMPLE, LOWER);

            String full = Main.ucd.getCase(cp, FULL, FOLD);
            String simple = Main.ucd.getCase(cp, SIMPLE, FOLD);

            boolean failed = false;
            if (!full.equals(fullTest)) {
                Utility.fixDot();
                System.out.println("Case fold fails at " + Main.ucd.getCodeAndName(cp));
                System.out.println("  fullFold(ch):             " + Main.ucd.getCodeAndName(full));
                System.out.println("  fullUpper(fullLower(ch)): " + Main.ucd.getCodeAndName(fullTest));
                failed = true;
            }
            if (!simple.equals(simpleTest)) {
                Utility.fixDot();
                if (!failed) System.out.println("Case fold fails at " + Main.ucd.getCodeAndName(cp));
                System.out.println("  simpleFold(ch):               " + Main.ucd.getCodeAndName(simple));
                System.out.println("  simpleUpper(simpleLower(ch)): " + Main.ucd.getCodeAndName(simpleTest));
                failed = true;
            }
            if (failed) System.out.println();
        }
    }
    
    public static void compareBlueberry() {
        Main.setUCD();
        
        UnicodeSet NameStartChar = new UnicodeSet("[A-Z:_a-z\\u00C0-\\u02FF"
        + "\\u0370-\\u037D\\u037F-\\u2027\\u202A-\\u218F\\u2800-\\uD7FF"
        + "\\uE000-\\uFDCF\\uFDE0-\\uFFEF\\U00010000-\\U0010FFFF]");
        System.out.println("NameStartChar:");
        System.out.println("\t" + NameStartChar.toPattern(true));
        
        UnicodeSet NameChar = new UnicodeSet("[-.0-9\\u00b7\\u0300-\\u036F]");
        System.out.println("NameChar-:");
        System.out.println("\t" + NameChar.toPattern(true));
        NameChar.addAll(NameStartChar);
        System.out.println("NameChar:");
        System.out.println("\t" + NameChar.toPattern(true));
        
        UnicodeProperty IDstart = DerivedProperty.make(Mod_ID_Start, Main.ucd);
        UnicodeProperty IDcontinue = DerivedProperty.make(Mod_ID_Continue_NO_Cf, Main.ucd);
        
        UnicodeSet IDContinueMinusNameChar = new UnicodeSet();
        UnicodeSet IDStartMinusNameChar = new UnicodeSet();
        UnicodeSet IDStartMinusNameStartChar = new UnicodeSet();
        UnicodeSet UnassignedMinusNameChar = new UnicodeSet();
        
        for (int cp = 0; cp < 0x10FFFF; ++cp) {
            Utility.dot(cp);
            
            if (Main.ucd.isPUA(cp)) continue;
            if (!Main.ucd.isAssigned(cp) && !NameChar.contains(cp)) {
                UnassignedMinusNameChar.add(cp);
            } else if (IDcontinue.hasValue(cp) && !NameChar.contains(cp)) {
                IDContinueMinusNameChar.add(cp);
            } else if (IDstart.hasValue(cp)) {
                if (!NameChar.contains(cp)) {
                    IDStartMinusNameChar.add(cp);
                } else if (!NameStartChar.contains(cp)) {
                    IDStartMinusNameStartChar.add(cp);
                }
            }
        }
        System.out.println("IDContinueMinusNameChar: ");
        System.out.println("\t" + IDContinueMinusNameChar.toPattern(true));
        Utility.showSetNames("\t", IDContinueMinusNameChar, false, Main.ucd);
        System.out.println("IDStartMinusNameChar: ");
        System.out.println("\t" + IDStartMinusNameChar.toPattern(true));
        System.out.println("IDStartMinusNameStartChar: ");
        System.out.println("\t" + IDStartMinusNameStartChar.toPattern(true));
        System.out.println("UnassignedMinusNameChar: ");
        System.out.println("\t" + UnassignedMinusNameChar.toPattern(true));
    }
    
    public static void VerifyIDN() throws IOException {
        Main.setUCD();
        System.out.println("VerifyIDN");

        System.out.println();
        System.out.println("Checking Map");
        System.out.println();

        BitSet mappedOut = new BitSet();
        int errorCount = verifyUTFMap(mappedOut);

        BitSet unassigned = getIDNList("IDN-Unassigned.txt");
        BitSet prohibited = getIDNList("IDN-Prohibited.txt");
        BitSet guessSet = guessIDN();

        System.out.println();
        System.out.println("Checking Prohibited and Unassigned");
        System.out.println();
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (mappedOut.get(cp)) continue;

            boolean ucdUnassigned = !Main.ucd.isAllocated(cp);
            boolean idnUnassigned = unassigned.get(cp);
            boolean guess = guessSet.get(cp);
            boolean idnProhibited = prohibited.get(cp);

            if (ucdUnassigned && !idnUnassigned) {
                showError("?UCD Unassigned but not IDN Unassigned", cp, "");
                ++errorCount;
            } else if (!ucdUnassigned && idnUnassigned) {
                showError("?Not UCD Unassigned but IDN Unassigned", cp, "");
                ++errorCount;
            }

            if (idnProhibited && unassigned.get(cp)) {
                showError("?Both IDN Unassigned AND IDN Prohibited", cp, "");
                ++errorCount;
            }

            if (guess && !idnProhibited) {
                showError("?UCD ?prohibited? but not IDN Prohibited ", cp, "");
                ++errorCount;
            } else if (!guess && idnProhibited) {
                showError("?Not UCD ?prohibited? but IDN Prohibited ", cp, "");
                ++errorCount;
            }
            
            if (cp == 0x3131) {
                System.out.println("Debug: " + idnProhibited
                    + ", " + idnUnassigned
                    + ", " + Main.nfkd.normalizationDiffers(cp)
                    + ", " + Main.ucd.getCodeAndName(Main.nfkc.normalize(cp))
                    + ", " + Main.ucd.getCodeAndName(Main.nfc.normalize(cp)));
            } 
            
            if (!idnProhibited && ! idnUnassigned && Main.nfkd.normalizationDiffers(cp)) {
                String kc = Main.nfkc.normalize(cp);
                String c = Main.nfc.normalize(cp);
                if (kc.equals(c)) continue;
                int cp2;
                boolean excluded = false;
                for (int j = 0; j < kc.length(); j += UTF16.getCharCount(cp2)) {
                    cp2 = UTF16.charAt(kc, j);
                    if (prohibited.get(cp2)) {
                        showError("Prohibited with NFKC, but output with NFC", cp, "");
                        excluded = true;
                        break;
                    }
                }
                if (!excluded) {
                    showError("Remapped to core abstract character with NFKC (but not NFC)", cp, ""); // , "\t=> " + Main.ucd.getCodeAndName(kc));
                }
            }

        }
        System.out.println("Writing IDNCheck.txt");
        
        
        PrintWriter log = Utility.openPrintWriter("IDNCheck.txt");
        log.println("IDN Check");
        log.println("Total Errors: " + errorCount);
       
        Iterator it = idnMap.keySet().iterator();
        while (it.hasNext()) {
            String description = (String) it.next();
            Map map = (Map) idnMap.get(description);
            log.println();
            log.println(description);
            log.println("Total: " + map.size());
            log.println();
            
            Iterator it2 = map.keySet().iterator();
            while (it2.hasNext()) {
                Object key = it2.next();
                String line = (String) map.get(key);
                log.println("  " + line);
            }
        }
        log.close();
    }
    
    static Map idnMap = new java.util.HashMap();

    static void showError(String description, int cp, String option) {
        Map probe = (Map) idnMap.get(description);
        if (probe == null) {
            probe = new TreeMap();
            idnMap.put(description, probe);
        }
        probe.put(new Integer(cp), Main.ucd.getCodeAndName(cp) + " (" + Main.ucd.getCategoryID(cp) + ")" + option);
    }


    public static BitSet guessIDN() {
        BitSet result = new BitSet();
        for (int cp = 0; cp < 0x10FFFF; ++cp) {
            int cat = Main.ucd.getCategory(cp);
            // 5.1 Currently-prohibited ASCII characters

            if (cp < 0x80 && cp != '-' && !(cat == Lu || cat == Ll || cat == Nd)) result.set(cp);

            // 5.2 Space characters

            if (cat == Zs) result.set(cp);

            // 5.3 Control characters
            if (cat == Cc || cat == Zp || cat == Zl) result.set(cp);

            // exclude those reserved for Cf
            /*if (0x2060 <= cp && cp <= 0x206F) result.set(cp);
            if (0xFFF0 <= cp && cp <= 0xFFFC) result.set(cp);
            if (0xE0000 <= cp && cp <= 0xE0FFF) result.set(cp);
            */

            // 5.4 Private use and replacement characters

            if (cat == Co) result.set(cp);
            if (cp == 0xFFFD) result.set(cp);

            // 5.5 Non-character code points
            if (Main.ucd.getBinaryProperty(cp, Noncharacter_Code_Point)) result.set(cp);

            // 5.6 Surrogate codes
            if (cat == Cs) result.set(cp);

            // 5.7 Inappropriate for plain text

            if (cat == Cf) result.set(cp);
            if (cp == 0xFFFC) result.set(cp);

            // 5.8 Inappropriate for domain names

            if (isIDS(cp)) result.set(cp);

           // 5.9 Change display properties
           // Cf, checked above

           // 5.10 Inappropriate characters from common input mechanisms
            if (cp == 0x3002) result.set(cp);

           // 5.11 Tagging characters
           // Cf, checked above
        }
        return result;
    }

    static boolean isIDS(int cp) { return 0x2FF0 <= cp && cp <= 0x2FFB; }


/*
5.1 Currently-prohibited ASCII characters

Some of the ASCII characters that are currently prohibited in host names
by [STD13] are also used in protocol elements such as URIs [URI]. The other
characters in the range U+0000 to U+007F that are not currently allowed
are also prohibited in host name parts to reserve them for future use in
protocol elements.

0000-002C; [ASCII CONTROL CHARACTERS and SPACE through ,]
002E-002F; [ASCII . through /]
003A-0040; [ASCII : through @]
005B-0060; [ASCII [ through `]
007B-007F; [ASCII { through DEL]

5.2 Space characters

Space characters would make visual transcription of URLs nearly
impossible and could lead to user entry errors in many ways.

0020; SPACE
00A0; NO-BREAK SPACE
1680; OGHAM SPACE MARK
2000; EN QUAD
2001; EM QUAD
2002; EN SPACE
2003; EM SPACE
2004; THREE-PER-EM SPACE
2005; FOUR-PER-EM SPACE
2006; SIX-PER-EM SPACE
2007; FIGURE SPACE
2008; PUNCTUATION SPACE
2009; THIN SPACE
200A; HAIR SPACE
202F; NARROW NO-BREAK SPACE
3000; IDEOGRAPHIC SPACE

5.3 Control characters

Control characters cannot be seen and can cause unpredictable results
when displayed.

0000-001F; [CONTROL CHARACTERS]
007F; DELETE
0080-009F; [CONTROL CHARACTERS]
2028; LINE SEPARATOR
2029; PARAGRAPH SEPARATOR
206A-206F; [CONTROL CHARACTERS]
FFF9-FFFC; [CONTROL CHARACTERS]
1D173-1D17A; [MUSICAL CONTROL CHARACTERS]

5.4 Private use and replacement characters

Because private-use characters do not have defined meanings, they are
prohibited. The private-use characters are:

E000-F8FF; [PRIVATE USE, PLANE 0]
F0000-FFFFD; [PRIVATE USE, PLANE 15]
100000-10FFFD; [PRIVATE USE, PLANE 16]

The replacement character (U+FFFD) has no known semantic definition in a
name, and is often displayed by renderers to indicate "there would be
some character here, but it cannot be rendered". For example, on a
computer with no Asian fonts, a name with three ideographs might be
rendered with three replacement characters.

FFFD; REPLACEMENT CHARACTER

5.5 Non-character code points

Non-character code points are code points that have been allocated in
ISO/IEC 10646 but are not characters. Because they are already assigned,
they are guaranteed not to later change into characters.

FDD0-FDEF; [NONCHARACTER CODE POINTS]
FFFE-FFFF; [NONCHARACTER CODE POINTS]
1FFFE-1FFFF; [NONCHARACTER CODE POINTS]
2FFFE-2FFFF; [NONCHARACTER CODE POINTS]
3FFFE-3FFFF; [NONCHARACTER CODE POINTS]
4FFFE-4FFFF; [NONCHARACTER CODE POINTS]
5FFFE-5FFFF; [NONCHARACTER CODE POINTS]
6FFFE-6FFFF; [NONCHARACTER CODE POINTS]
7FFFE-7FFFF; [NONCHARACTER CODE POINTS]
8FFFE-8FFFF; [NONCHARACTER CODE POINTS]
9FFFE-9FFFF; [NONCHARACTER CODE POINTS]
AFFFE-AFFFF; [NONCHARACTER CODE POINTS]
BFFFE-BFFFF; [NONCHARACTER CODE POINTS]
CFFFE-CFFFF; [NONCHARACTER CODE POINTS]
DFFFE-DFFFF; [NONCHARACTER CODE POINTS]
EFFFE-EFFFF; [NONCHARACTER CODE POINTS]
FFFFE-FFFFF; [NONCHARACTER CODE POINTS]

5.6 Surrogate codes

The following code points are permanently reserved for use as surrogate
code values in the UTF-16 encoding, will never be assigned to
characters, and are therefore prohibited:

D800-DFFF; [SURROGATE CODES]

5.7 Inappropriate for plain text

The following characters should not appear in regular text.

FFF9; INTERLINEAR ANNOTATION ANCHOR
FFFA; INTERLINEAR ANNOTATION SEPARATOR
FFFB; INTERLINEAR ANNOTATION TERMINATOR
FFFC; OBJECT REPLACEMENT CHARACTER

5.8 Inappropriate for domain names

The ideographic description characters allow different sequences of
characters to be rendered the same way, which makes them inappropriate
for host names that must have a single canonical representation.

2FF0-2FFB; [IDEOGRAPHIC DESCRIPTION CHARACTERS]

5.9 Change display properties

The following characters, some of which are deprecated in ISO/IEC 10646,
can cause changes in display or the order in which characters appear
when rendered.

200E; LEFT-TO-RIGHT MARK
200F; RIGHT-TO-LEFT MARK
202A; LEFT-TO-RIGHT EMBEDDING
202B; RIGHT-TO-LEFT EMBEDDING
202C; POP DIRECTIONAL FORMATTING
202D; LEFT-TO-RIGHT OVERRIDE
202E; RIGHT-TO-LEFT OVERRIDE
206A; INHIBIT SYMMETRIC SWAPPING
206B; ACTIVATE SYMMETRIC SWAPPING
206C; INHIBIT ARABIC FORM SHAPING
206D; ACTIVATE ARABIC FORM SHAPING
206E; NATIONAL DIGIT SHAPES
206F; NOMINAL DIGIT SHAPES

5.10 Inappropriate characters from common input mechanisms

U+3002 is used as if it were U+002E in many input mechanisms,
particularly in Asia. This prohibition allows input mechanisms to safely
map U+3002 to U+002E before doing nameprep without worrying about
preventing users from accessing legitimate host name parts.

3002; IDEOGRAPHIC FULL STOP

5.11 Tagging characters

The following characters are used for tagging text and are invisible.

E0001; LANGUAGE TAG
E0020-E007F; [TAGGING CHARACTERS]
*/


    public static int verifyUTFMap(BitSet mappedOut) throws IOException {
        int errorCount = 0;
        BufferedReader input = new BufferedReader(new FileReader(IDN_DIR + "IDN-Mapping.txt"),32*1024);
        String line = "";
        Map idnFold = new TreeMap();
        Map idnWhy = new HashMap();
        try {
    	    String[] parts = new String[20];
            for (int lineNumber = 1; ; ++lineNumber) {
                line = input.readLine();
			    if (line == null) break;
			    if ((lineNumber % 500) == 0) {
                    Utility.fixDot();
			        System.out.println("//" + lineNumber + ": '" + line + "'");
			    }

			    if (line.length() == 0) continue;

                int count = Utility.split(line,';',parts);
                if (count != 3) throw new ChainException("Incorrect # of fields in IDN folding", null);

                String key = Utility.fromHex(parts[0]);
                if (UTF32.length32(key) != 1) throw new ChainException("First IDN field not single character: " + line, null);
                int cp = UTF32.char32At(key, 0);
                if (!Main.ucd.isAssigned(cp) || Main.ucd.isPUA(cp)) throw new ChainException("IDN character unassigned or PUA: " + line, null);
                String value = Utility.fromHex(parts[1]);
                String reason = parts[2].trim();

                if (reason.equals("Map out")) {
                    value = Utility.fromHex(parts[1]);
                    Utility.fixDot();
                    showError("Mapping Out: ", cp, "");
                    mappedOut.set(cp);
                }
                idnFold.put(key, value);
                idnWhy.put(key, reason);
            }

            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                Utility.dot(cp);
                if (!Main.ucd.isAssigned(cp) || Main.ucd.isPUA(cp)) continue;
                if (mappedOut.get(cp)) continue;

                String key = UTF32.valueOf32(cp);
                String value = (String)idnFold.get(key);
                if (value == null) value = key;
                String reason = (String)idnWhy.get(key);
                String ucdFold = Main.ucd.getCase(cp, FULL, FOLD, "I");
                if (!ucdFold.equals(value)) {
                    String b = Main.nfkc.normalize(Main.ucd.getCase(cp, FULL, FOLD, "I"));
                    String c = Main.nfkc.normalize(Main.ucd.getCase(b, FULL, FOLD, "I"));

                    if (c.equals(value)) continue;
                    Utility.fixDot();

                    System.out.println("Mismatch: " + Main.ucd.getCodeAndName(cp));
                    System.out.println("  UCD Case Fold: <" + Main.ucd.getCodeAndName(ucdFold) + ">");
                    System.out.println("  IDN Map [" + reason + "]: <" + Main.ucd.getCodeAndName(value) + ">");
                    errorCount++;
                }
            }
        } finally {
            input.close();
        }
        return errorCount;
    }

    static BitSet getIDNList(String file) throws IOException {
        BufferedReader input = new BufferedReader(new FileReader(IDN_DIR + file),32*1024);
        BitSet result = new BitSet();
        String line;
        try {
    	    String[] parts = new String[20];
            for (int lineNumber = 1; ; ++lineNumber) {
                line = input.readLine();
			    if (line == null) break;
			    if ((lineNumber % 500) == 0) {
                    Utility.fixDot();
			        System.out.println("//" + lineNumber + ": '" + line + "'");
			    }

			    if (line.length() == 0) continue;

                int count = Utility.split(line,'-',parts);
                if (count > 2) throw new ChainException("Incorrect # of fields in IDN list", null);
                int start = Utility.codePointFromHex(parts[0]);
                int end = count == 1 ? start : Utility.codePointFromHex(parts[1]);

                for (int i = start; i <= end; ++i) {
                    result.set(i);
                }
            }
        } finally {
            input.close();
        }
        return result;
    }
    
    /*
                    + "\r\n#  Generated from <2060..206F, FFF0..FFFB, E0000..E0FFF>"
                    + "\r\n#    + Other_Default_Ignorable_Code_Point + (Cf + Cc + Cs - White_Space)";
    */
    
    public static void diffIgnorable () {
        Main.setUCD();
    	
    	UnicodeSet control = UnifiedBinaryProperty.make(CATEGORY + Cf, Main.ucd).getSet();
    	
    	System.out.println("Cf");
    	Utility.showSetNames("", control, false, Main.ucd);
    	
    	control.addAll(UnifiedBinaryProperty.make(CATEGORY + Cc, Main.ucd).getSet());

    	System.out.println("Cf + Cc");
    	Utility.showSetNames("", control, false, Main.ucd);
    	
    	control.addAll(UnifiedBinaryProperty.make(CATEGORY + Cs, Main.ucd).getSet());

    	System.out.println("Cf + Cc + Cs");
    	Utility.showSetNames("", control, false, Main.ucd);
    	
    	control.removeAll(UnifiedBinaryProperty.make(BINARY_PROPERTIES + White_space, Main.ucd).getSet());
    	
    	System.out.println("Cf + Cc + Cs - WhiteSpace");
    	Utility.showSetNames("", control, false, Main.ucd);

    	control.add(0x2060,0x206f).add(0xFFF0,0xFFFB).add(0xE0000,0xE0FFF);
    	
    	System.out.println("(Cf + Cc + Cs - WhiteSpace) + ranges");
    	Utility.showSetNames("", control, false, Main.ucd);

    	UnicodeSet odicp = UnifiedBinaryProperty.make(BINARY_PROPERTIES + Other_Default_Ignorable_Code_Point, Main.ucd).getSet();
    	
    	odicp.removeAll(control);
    	
    	System.out.println("Minimal Default Ignorable Code Points");
    	Utility.showSetNames("", odicp, true, Main.ucd);
    }


    public static void IdentifierTest() {
        String x = normalize(UTF32.valueOf32(0x10300), 4) ;
        getCategoryID(x);

        /*
        Changes Category: U+10300 OLD ITALIC LETTER A
   nfx_cp: U+D800 <surrogate-D800>
 isIdentifier(nfx_cp, true): false
   cat(nfx_cp): Cs
 isIdentifierStart(cp, true): true
   cat(cp): Lo
   */

        for (int j = 0; j < 5; ++j) {
            System.out.println();
            System.out.println("Testing Identifier Closure for " + NAMES[j]);
            System.out.println();
            for (int cp = 0; cp < 0x10FFFF; ++cp) {
                Utility.dot(cp);
                if (!Main.ucd.isAssigned(cp)) continue;
                if (Main.ucd.isPUA(cp)) continue;
                if (!normalizationDiffers(cp, j)) continue;

                if (cp == 0xFDFB || cp == 0x0140) {
                    System.out.println("debug point");
                }

                boolean norm;
                boolean plain;

                String x_cp = 'x' + UTF32.valueOf32(cp);
                String nfx_x_cp = normalize(x_cp, j);
                plain = Main.ucd.isIdentifier(x_cp, true);
                norm = Main.ucd.isIdentifier(nfx_x_cp, true);
                if (plain & !norm) {
                    Utility.fixDot();
                    System.out.println("*Not Identifier: " + Main.ucd.getCodeAndName(cp));
                    System.out.println("    nfx_x_cp: " + Main.ucd.getCodeAndName(nfx_x_cp));

                    System.out.println("  isIdentifier(nfx_x_cp, true): " + norm);
                    System.out.println("    cat(nfx_x_cp): " + getCategoryID(nfx_x_cp));

                    System.out.println("  isIdentifier(x_cp, true): " + plain);
                    System.out.println("    cat(x_cp): " + getCategoryID(x_cp));
                    continue;
                }

                String nfx_cp = normalize(UTF32.valueOf32(cp), j);
                plain = Main.ucd.isIdentifierStart(cp, true);
                norm = Main.ucd.isIdentifier(nfx_cp, true);
                if (plain & !norm) {
                    Utility.fixDot();
                    System.out.println(" Changes Category: " + Main.ucd.getCodeAndName(cp));
                    System.out.println("    nfx_cp: " + Main.ucd.getCodeAndName(nfx_cp));

                    System.out.println("  isIdentifier(nfx_cp, true): " + norm);
                    System.out.println("    cat(nfx_cp): " + getCategoryID(nfx_cp));

                    System.out.println("  isIdentifierStart(cp, true): " + plain);
                    System.out.println("    cat(cp): " + Main.ucd.getCategoryID(cp));
                    System.out.println();
                    continue;
                }
            }
        }
    }

    static String getCategoryID(String s) {
        if (UTF32.length32(s) == 1) return Main.ucd.getCategoryID(UTF32.char32At(s, 0));
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            if (i != 0) result.append(' ');
            result.append(Main.ucd.getCategoryID(cp));
        }
        return result.toString();
    }

    static String normalize(String s, int j) {
        if (j < 4) return Main.nf[j].normalize(s);
        return Main.ucd.getCase(s, FULL, FOLD);
    }

    static boolean normalizationDiffers(int cp, int j) {
        if (j < 4) return Main.nf[j].normalizationDiffers(cp);
        return true;
    }

    private static final String[] NAMES = {"Main.nfd", "NFC", "NFKD", "NFKC", "Fold"};

    public static void NFTest() {
        for (int j = 0; j < 4; ++j) {
            Normalizer nfx = Main.nf[j];
            System.out.println();
            System.out.println("Testing normalizationDiffers for " + NAMES[j]);
            System.out.println();
            for (int i = 0; i < 0x10FFFF; ++i) {
                Utility.dot(i);
                if (!Main.ucd.isAssigned(i)) continue;
                if (Main.ucd.isPUA(i)) continue;
                String s = nfx.normalize(i);
                boolean differs = !s.equals(UTF32.valueOf32(i));
                boolean call = nfx.normalizationDiffers(i);
                if (differs != call) {
                    Utility.fixDot();
                    System.out.println("Problem: differs: " + differs
                     + ", call: " + call + " " + Main.ucd.getCodeAndName(i));
                }
            }

        }
    }
    
    static final int EXCEPTION_FLAG = 0x8000000;

    public static void checkScripts() throws IOException {
        Main.setUCD();
        boolean ok;
        Map m = new TreeMap();
        UnicodeSet exceptions = ScriptExceptions.getExceptions();
        int maxScriptLen = 0;
        UnicodeSet show = new UnicodeSet();
        show.add(0x2071);
        show.add(0x207F);
        
        for (int i = 0; i < 0x10FFFF; ++i) {
            if (!Main.ucd.isAssigned(i)) continue;
            byte cat = Main.ucd.getCategory(i);
            byte script = Main.ucd.getScript(i);
            switch (cat) {
              case Lo: case Lt: case Ll: case Lu: case Lm: case Mc: case Sk:
                ok = script != INHERITED_SCRIPT && script != COMMON_SCRIPT;
                break;
              case Mn: case Me:
                ok = script == INHERITED_SCRIPT;
                break;
              default:
                ok = script == COMMON_SCRIPT;
                break;
            }
            if (show.contains(i)) {
                System.out.println(Main.ucd.getCodeAndName(i)
                    + "; " + Main.ucd.getScriptID(i)
                    + "; " + Main.ucd.getCategoryID(i)
                );
            }
            if (!ok) {
                if (cat == Ll || cat == Lt) cat = Lu;
                int intKey = (cat << 8) + script;
                if (exceptions.contains(i)) intKey |= EXCEPTION_FLAG;
                Integer key = new Integer(intKey);
                UnicodeSet us = (UnicodeSet) m.get(key);
                if (us == null) {
                    us = new UnicodeSet();
                    m.put(key, us);
                }
                us.add(i);
                int len = Main.ucd.getScriptID(i).length();
                if (maxScriptLen < len) maxScriptLen = len;
            }
        }
        
        PrintWriter log = Utility.openPrintWriter("CheckScriptsLog.txt");
        
        Iterator it = m.keySet().iterator();
        while (it.hasNext()) {
            Integer key = (Integer) it.next();
            int intKey = key.intValue();
            UnicodeSet badChars = (UnicodeSet) m.get(key);
            int ranges = badChars.getRangeCount();
            for (int kk = 0; kk < ranges; ++kk) {
                int start = badChars.getRangeStart(kk);
                int end = badChars.getRangeEnd(kk);
                String code = Utility.hex(start) + (start != end ? ".." + Utility.hex(end) : "");
                String scriptName = Main.ucd.getScriptID(start);
                String title = "FAIL";
                if ((intKey & EXCEPTION_FLAG) != 0) title = "EXCEPTION";
                log.println(title + ": " + code + "; " + Utility.repeat(" ", 14 - code.length())
                    + scriptName + Utility.repeat(" ", maxScriptLen-scriptName.length())
                    + " # (" + LCgetCategoryID(start) + ") " + Main.ucd.getName(start)
                    + (start != end ? ".." + Main.ucd.getName(end) : "")
                    );
            }
            log.println();
        }
        log.close();
    }
    
    static public String LCgetCategoryID(int cp) {
        byte cat = Main.ucd.getCategory(cp);
        if (cat == Lu || cat == Lt || cat == Ll) return "LC";
        return Main.ucd.getCategoryID(cp);
    }
    
    static public void verifyNormalizationStability() {
        Main.setUCD();
		verifyNormalizationStability2("3.1.0");
		verifyNormalizationStability2("3.0.0");
    }
    
    static public void verifyNormalizationStability2(String version) {
        
        Main.nfd.normalizationDiffers(0x10300);
        
        UCD older = UCD.make(version); // Main.ucd.getPreviousVersion();
        
        Normalizer oldNFC = new Normalizer(Normalizer.NFC, older.getVersion());
        Normalizer oldNFD = new Normalizer(Normalizer.NFD, older.getVersion());
        Normalizer oldNFKC = new Normalizer(Normalizer.NFKC, older.getVersion());
        Normalizer oldNFKD = new Normalizer(Normalizer.NFKD, older.getVersion());
        
        System.out.println("Testing " + Main.nfd.getUCDVersion() + " against " + oldNFD.getUCDVersion());
        
        for (int i = 0; i <= 0x10FFFF; ++i) {
        	Utility.dot(i);
            if (!Main.ucd.isAssigned(i)) continue;
            byte cat = Main.ucd.getCategory(i);
            if (cat == Cs || cat == PRIVATE_USE) continue;
            
            if (i == 0x5e) {
            	System.out.println("debug");
            	String test1 = Main.nfkd.normalize(i);
            	String test2 = oldNFKD.normalize(i);
        		System.out.println("Testing (new/old)" + Main.ucd.getCodeAndName(i));
    			System.out.println("\t" + Main.ucd.getCodeAndName(test1));
    			System.out.println("\t" + Main.ucd.getCodeAndName(test2));
            }
            	
            if (older.isAssigned(i)) {
            	
            	int newCan = Main.ucd.getCombiningClass(i);
            	int oldCan = older.getCombiningClass(i);
            	if (newCan != oldCan) {
            		System.out.println("FAILS CCC STABILITY: " + newCan + " != " + oldCan
            			+ "; " + Main.ucd.getCodeAndName(i));
            	}
            	
            	verifyEquals(i, "NFD STABILITY (new/old)", Main.nfd.normalize(i), oldNFD.normalize(i));
            	verifyEquals(i, "NFC STABILITY (new/old)", Main.nfc.normalize(i), oldNFC.normalize(i));
            	verifyEquals(i, "NFKD STABILITY (new/old)", Main.nfkd.normalize(i), oldNFKD.normalize(i));
            	verifyEquals(i, "NFKC STABILITY (new/old)", Main.nfkc.normalize(i), oldNFKC.normalize(i));
            	
            } else {
            	// not in older version. 
            	// (1) If there is a decomp, and it is composed of all OLD characters, then it must NOT compose
            	if (Main.nfd.normalizationDiffers(i)) {
            		String decomp = Main.nfd.normalize(i);
            		if (noneHaveCategory(decomp, Cn, older)) {
            			String recomp = Main.nfc.normalize(decomp);
            			if (recomp.equals(UTF16.valueOf(i))) {
        					Utility.fixDot();
            				System.out.println("FAILS COMP STABILITY: " + Main.ucd.getCodeAndName(i));
    						System.out.println("\t" + Main.ucd.getCodeAndName(decomp));
    						System.out.println("\t" + Main.ucd.getCodeAndName(recomp));
    						System.out.println();
    						throw new IllegalArgumentException("Comp stability");
            			}
            		}
            	}
            }
        }
    }
    
    public static boolean noneHaveCategory(String s, byte cat, UCD ucd) {
    	int cp;
    	for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
    		cp = UTF16.charAt(s, i);
    		byte cat2 = ucd.getCategory(i);
    		if (cat == cat2) return false;
    	}
    	return true;
    }
    
    public static void verifyEquals(int cp, String message, String a, String b) {
    	if (!a.equals(b)) {
        	Utility.fixDot();
    		System.out.println("FAILS " + message + ": " + Main.ucd.getCodeAndName(cp));
    		System.out.println("\t" + Main.ucd.getCodeAndName(a));
    		System.out.println("\t" + Main.ucd.getCodeAndName(b));
    		System.out.println();
    	}
    }

    public static void checkAgainstUInfo() {
    /*
        Main.ucd = UCD.make(Main.Main.ucdVersion);
        UData x = new UData();
        x.fleshOut();

        System.out.println(Main.ucd.toString(0x1E0A));

        UInfo.init();
        System.out.println("Cross-checking against old implementation");
        System.out.println("Version: " + Main.ucd.getVersion() + ", " + new Date(Main.ucd.getDate()));
        for (int i = 0; i <= 0xFFFF; ++i) {
            Utility.dot(i);

            if ((i & 0x0FFF) == 0) System.out.println("#" + Utility.hex(i));
            try {
                check(i, Main.ucd.getName(i), UInfo.getName((char)i), "Name");
                check(i, Main.ucd.getCategory(i), UInfo.getCategory((char)i), UCD_Names.GC, "GeneralCategory");
                check(i, Main.ucd.getCombiningClass(i), UInfo.getCanonicalClass((char)i), "CanonicalClass");
                check(i, Main.ucd.getBidiClass(i), UInfo.getBidiClass((char)i), UCD_Names.BC, "BidiClass");
                check(i, Main.ucd.getDecompositionMapping(i), UInfo.getDecomposition((char)i), "Decomposition");
                check(i, Main.ucd.getDecompositionType(i), UInfo.getDecompositionType((char)i), UCD_Names.DT, "DecompositionType");
                check(i, Main.ucd.getNumericValue(i), UInfo.getNumeric((char)i), "NumericValue");
                check(i, Main.ucd.getNumericType(i), UInfo.getNumericType((char)i), UCD_Names.NT, "NumericType");

                check(i, Main.ucd.getCase(i, SIMPLE, LOWER), UInfo.getLowercase((char)i), "SimpleLowercase");
                check(i, Main.ucd.getCase(i, SIMPLE, UPPER), UInfo.getUppercase((char)i), "SimpleUppercase");
                check(i, Main.ucd.getCase(i, SIMPLE, TITLE), UInfo.getTitlecase((char)i), "SimpleTitlecase");
                //check(i, Main.ucd.getSimpleCaseFolding(i), UInfo.getSimpleCaseFolding((char)i));

                if (Main.ucd.getSpecialCase(i).length() == 0) {  // NORMAL
                    check(i, Main.ucd.getCase(i, FULL, LOWER), UInfo.toLowercase((char)i, ""), "FullLowercase");
                    check(i, Main.ucd.getCase(i, FULL, UPPER), UInfo.toUppercase((char)i, ""), "FullUppercase");
                    check(i, Main.ucd.getCase(i, FULL, TITLE), UInfo.toTitlecase((char)i, ""), "FullTitlecase");
                } else {                                    // SPECIAL
                    check(i, Main.ucd.getCase(i, SIMPLE, LOWER), UInfo.toLowercase((char)i, ""), "FullLowercase");
                    check(i, Main.ucd.getCase(i, SIMPLE, UPPER), UInfo.toUppercase((char)i, ""), "FullUppercase");
                    check(i, Main.ucd.getCase(i, SIMPLE, TITLE), UInfo.toTitlecase((char)i, ""), "FullTitlecase");
                }
                // check(i, Main.ucd.getFullCaseFolding(i), UInfo.getFullCaseFolding((char)i));

                check(i, Main.ucd.getSpecialCase(i).toUpperCase(), UInfo.getCaseCondition((char)i).toUpperCase(), "SpecialCase");
                check(i, Main.ucd.getLineBreak(i), UInfo.getLineBreakType((char)i), UCD_Names.LB, "LineBreak");
                check(i, Main.ucd.getEastAsianWidth(i), UInfo.getEastAsianWidthType((char)i), UCD_Names.EA, "EastAsian");

                int props = Main.ucd.getBinaryProperties(i);
                check(i, (props>>BidiMirrored) & 1, UInfo.getMirrored((char)i), UCD_Names.YN_TABLE, "BidiMirroring");
                check(i, (props>>CompositionExclusion) & 1, UInfo.isCompositionExcluded((char)i)?1:0, UCD_Names.YN_TABLE, "Comp-Exclusion");

            } catch (Exception e) {
                Utility.fixDot();

                System.out.println("Error: " + Utility.hex(i) + " " + e.getClass().getName() + e.getMessage());
                e.printStackTrace();
            }
        }
    */
    }


    public static void check(int cp, boolean x, boolean y, String[] names, String type) {
        check(cp, x ? 1 : 0, y ? 1 : 0, names, type);
    }

    public static void check(int cp, int x, int y, String[] names, String type) {
        if (x == y) return;
        showLast(cp);
        Utility.fixDot();
        System.out.println("  " + type + ": "
            + Utility.getName(x, names) + " (" + x  + ") " + " != "
            + Utility.getName(y, names) + " (" + y  + ") ") ;
    }

    public static void check(int cp, int x, int y, String type) {
        if (x == y) return;
        showLast(cp);
        Utility.fixDot();
        System.out.println("  " + type + ": " + x + " != " + y) ;
    }

    public static void check(int cp, float x, float y, String type) {
        if (!(x > y) && !(x < y)) return;   // funny syntax to catch NaN
        showLast(cp);
        Utility.fixDot();
        System.out.println("  " + type + ": " + x + " != " + y) ;
    }

    public static void check(int cp, String x, String y, String type) {
        if (x != null && x.equals(y)) return;
        if (x != null && y != null
         && x.length() > 0 && y.length() > 0
         && x.charAt(0) == '<' && y.charAt(0) == '<') {
            if (x.startsWith("<unassigned") && y.equals("<reserved>")) return;
            if (y.equals("<control>")) return;
            if (x.startsWith("<surrogate") && y.indexOf("Surrogate") != -1) return;
            if (x.startsWith("<private use") && y.startsWith("<Private Use")) return;
        }
        showLast(cp);
        Utility.fixDot();
        System.out.println("  " + type + ": " + Utility.quoteJavaString(x) + " != " + Utility.quoteJavaString(y));
    }


    static int lastShowed = -1;
    static boolean showCanonicalDecomposition = false;

    static void showLast(int cp) {
        if (lastShowed != cp) {
            Utility.fixDot();
            System.out.println();
            String s = Main.ucd.getDecompositionMapping(cp);
            System.out.print(Main.ucd.getCodeAndName(cp));
            if (showCanonicalDecomposition && !s.equals(UTF32.valueOf32(cp))) {
                System.out.print(" => " + Main.ucd.getCodeAndName(s));
            }
            System.out.println();
            lastShowed = cp;
        }
    }

    public static void test1() {
        Main.setUCD();

        for (int i = 0x19; i < 0x10FFFF; ++i) {

            System.out.println(Utility.hex(i) + " " + Utility.quoteJavaString(Main.ucd.getName(i)));

            System.out.print("    "
                + ", gc=" + Main.ucd.getCategoryID(i)
                + ", bc=" + Main.ucd.getBidiClassID(i)
                + ", cc=" + Main.ucd.getCombiningClassID(i)
                + ", ea=" + Main.ucd.getEastAsianWidthID(i)
                + ", lb=" + Main.ucd.getLineBreakID(i)
                + ", dt=" + Main.ucd.getDecompositionTypeID(i)
                + ", nt=" + Main.ucd.getNumericTypeID(i)
                + ", nv=" + Main.ucd.getNumericValue(i)
            );
            for (int j = 0; j < UCD_Types.LIMIT_BINARY_PROPERTIES; ++j) {
                if (Main.ucd.getBinaryProperty(i,j)) System.out.print(", " + UCD_Names.BP[j]);
            }
            System.out.println();

            System.out.println("    "
                + ", dm=" + Utility.quoteJavaString(Main.ucd.getDecompositionMapping(i))
                + ", slc=" + Utility.quoteJavaString(Main.ucd.getCase(i, SIMPLE, LOWER))
                + ", stc=" + Utility.quoteJavaString(Main.ucd.getCase(i, SIMPLE, TITLE))
                + ", suc=" + Utility.quoteJavaString(Main.ucd.getCase(i, SIMPLE, UPPER))
                + ", flc=" + Utility.quoteJavaString(Main.ucd.getCase(i, FULL, LOWER))
                + ", ftc=" + Utility.quoteJavaString(Main.ucd.getCase(i, FULL, TITLE))
                + ", fuc=" + Utility.quoteJavaString(Main.ucd.getCase(i, FULL, UPPER))
                + ", sc=" + Utility.quoteJavaString(Main.ucd.getSpecialCase(i))
            );

            if (i > 0x180) i = 3 * i / 2;
        }
    }

    static void checkCanonicalProperties() {
        Main.setUCD();
        System.out.println(Main.ucd.toString(0x1E0A));

        System.out.println("Cross-checking canonical equivalence");
        System.out.println("Version: " + Main.ucd.getVersion() + ", " + new Date(Main.ucd.getDate()));
        showCanonicalDecomposition = true;
        for (int q = 1; q < 2; ++q)
        for (int i = 0; i <= 0x10FFFF; ++i) {
            Utility.dot(i);
            if (i == 0x0387) {
                System.out.println("debug?");
            }
            byte type = Main.ucd.getDecompositionType(i);
            if (type != CANONICAL) continue;

            String s = Main.ucd.getDecompositionMapping(i);
            int slen = UTF32.length32(s);
            int j = UTF32.char32At(s, 0);
            try {
                if (q == 0) {
                check(i, Main.ucd.getCategory(i), Main.ucd.getCategory(j), UCD_Names.GC, "GeneralCategory");
                check(i, Main.ucd.getCombiningClass(i), Main.ucd.getCombiningClass(j), "CanonicalClass");
                check(i, Main.ucd.getBidiClass(i), Main.ucd.getBidiClass(j), UCD_Names.BC, "BidiClass");
                check(i, Main.ucd.getNumericValue(i), Main.ucd.getNumericValue(j), "NumericValue");
                check(i, Main.ucd.getNumericType(i), Main.ucd.getNumericType(j), UCD_Names.NT, "NumericType");

                if (false) {
                    for (byte k = LOWER; k <= FOLD; ++k) {
                        check(i, Main.ucd.getCase(i, SIMPLE, k), Main.ucd.getCase(j, SIMPLE, k), "Simple("+k+")");
                        check(i, Main.ucd.getCase(i, FULL, k), Main.ucd.getCase(j, FULL, k), "Full("+k+")");
                    }
                }

                if (slen == 1) check(i, Main.ucd.getSpecialCase(i), Main.ucd.getSpecialCase(j), "SpecialCase");

                for (byte k = 0; k < LIMIT_BINARY_PROPERTIES; ++k) {
                    if (k == Hex_Digit) continue;
                    if (k == Radical) continue;
                    if (k == UnifiedIdeograph) continue;
                    if (k == CompositionExclusion) continue;
                    check(i, Main.ucd.getBinaryProperty(i, k), Main.ucd.getBinaryProperty(j, k), UCD_Names.YN_TABLE, Main.ucd.getBinaryPropertiesID_fromIndex(k));
                }
                } else {
                    //check(i, Main.ucd.getLineBreak(i), Main.ucd.getLineBreak(j), UCD_Names.LB, "LineBreak");
                    //check(i, Main.ucd.getEastAsianWidth(i), Main.ucd.getEastAsianWidth(j), UCD_Names.EA, "EastAsian");
                }

            } catch (Exception e) {
                System.out.println("Error: " + Utility.hex(i) + " " + e.getClass().getName() + e.getMessage());
                e.printStackTrace();
            }
        }
  }
  
  static void checkSpeed() {
    int count = 1000000;
    int sum = 0;
    long start, end;
    
    java.text.NumberFormat nf = java.text.NumberFormat.getPercentInstance();
    
    start = System.currentTimeMillis();
    for (int i = count; i >= 0; --i) {
        sum += dummy0(i).length();
    }
    end = System.currentTimeMillis();
    double base = end - start;
    
    System.out.println("unsynchronized static char[]: " + nf.format((end - start)/base));

    start = System.currentTimeMillis();
    for (int i = count; i >= 0; --i) {
        sum += dummy2(i).length();
    }
    end = System.currentTimeMillis();
    System.out.println("synchronized static char[]: " + nf.format((end - start)/base));

    start = System.currentTimeMillis();
    for (int i = count; i >= 0; --i) {
        sum += dummy1(i).length();
    }
    end = System.currentTimeMillis();
    System.out.println("char[] each time: " + nf.format((end - start)/base));
    
    start = System.currentTimeMillis();
    for (int i = count; i >= 0; --i) {
        sum += dummy3(i).length();
    }
    end = System.currentTimeMillis();
    System.out.println("two valueofs: " + nf.format((end - start)/base));
    
    System.out.println(sum);
  }
  
  static String dummy1(int a) {
    char[] temp = new char[2];
    temp[0] = (char)(a >>> 16);
    temp[1] = (char)a;
    return new String(temp);
  }
  
  static char[] temp2 = new char[2];
  
  static String dummy2(int a) {
    synchronized (temp2) {
        temp2[0] = (char)(a >>> 16);
        temp2[1] = (char)a;
        return new String(temp2);
    }
  }
  
  static String dummy0(int a) {
        temp2[0] = (char)(a >>> 16);
        temp2[1] = (char)a;
        return new String(temp2);
  }
  
  static String dummy3(int a) {
    return String.valueOf((char)(a >>> 16)) + (char)a;
  }
  

}
