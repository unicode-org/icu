package com.ibm.text.UCD;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import java.util.BitSet;
import com.ibm.text.utility.*;
import java.io.PrintWriter;


public final class NFSkippable extends UnicodeProperty {
    
    static final boolean DEBUG = false;
    
    private Normalizer nf;
    private Normalizer nfd;
    private boolean composes;
    private int[] realTrailers = new int[100];
    private int realTrailerCount = 0;
    
    public NFSkippable(byte normalizerMode, String unicodeVersion) {
        isStandard = false;
        ucd = UCD.make(unicodeVersion);
        nf = new Normalizer(normalizerMode, unicodeVersion);
        name = nf.getName() + "_Skippable";
        shortName = nf.getName() + "_Skip";
        header = "# Derived Property: " + name
            + "\r\n#   Generated according to UAX #15."
            + "\r\n#   Characters that don't interact with any others in this normalization form."
            + "\r\n#   WARNING: Normalization of STRINGS must use the algorithm in UAX #15 because characters may interact."
            + "\r\n#            The length of a normalized string is not necessarily the sum of the lengths of the normalized characters!";

        nfd = new Normalizer(Normalizer.NFD, unicodeVersion);
        composes = normalizerMode == Normalizer.NFC || normalizerMode == Normalizer.NFKC;
        
        // preprocess to find possible trailers
        
        if (composes) for (int cp2 = 0; cp2 <= 0x10FFFF; ++cp2) {
            if (nf.isTrailing(cp2)) {
                //System.out.println("Trailing: " + ucd.getCodeAndName(cp2));
                if (ucd.isTrailingJamo(cp2)) {
                    //System.out.println("Jamo: " + ucd.getCodeAndName(cp2));
                    continue;
                }
                realTrailers[realTrailerCount++] = cp2;
            }
        }
        Utility.fixDot();
        //System.out.println("trailer count: " + realTrailerCount);
    }
    
    /** A skippable character is<br>
    * a) unassigned, or ALL of the following:<br>
    * b) of combining class 0.<br>
    * c) not decomposed by this normalization form.<br>
    * AND if NKC or NFKC, <br>
    * d) can never compose with a previous character.<br>
    * e) can never compose with a following character.<br>
    * f) can never change if another character is added.
    *    Example: a-breve might satisfy all but f, but if you
    *    add an ogonek it changes to a-ogonek + breve
    */
    
    String cause = "";
    
    public boolean hasValue(int cp) {
        // quick check on some special classes
        if (DEBUG) cause = "\t\tunassigned";
        if (!ucd.isAssigned(cp)) return true;
        
        if (DEBUG) cause = "\t\tnf differs";
        if (!nf.isNormalized(cp)) return false;
        
        if (DEBUG) cause = "\t\tnon-zero cc";
        if (ucd.getCombiningClass(cp) != 0) return false;
        
        if (DEBUG) cause = "";
        if (!composes) return true;
        
        // now special checks for composing normalizers
        if (DEBUG) cause = "\t\tleading";
        if (nf.isLeading(cp)) return false;

        if (DEBUG) cause = "\t\ttrailing";
        if (nf.isTrailing(cp)) return false;
        
        // OPTIMIZATION -- careful
        // If there is no NFD decomposition, then this character's accents can't be
        // "displaced", so we don't have to test further
        
        if (DEBUG) cause = "\t\tno decomp";
        if (nfd.isNormalized(cp)) return true;
        
        // OPTIMIZATION -- careful
        // Hangul syllables are skippable IFF they are isLeadingJamoComposition
        if (ucd.isHangulSyllable(cp)) return !ucd.isLeadingJamoComposition(cp);
        
        // We now see if adding another character causes a problem. 
        // brute force for now!!
        // We do skip the trailing Jamo, since those never displace!
        
        StringBuffer base = new StringBuffer(UTF16.valueOf(cp));
        int baseLen = base.length();
        for (int i = 0; i < realTrailerCount; ++i) {
            base.setLength(baseLen); // shorten if needed
            base.append(UTF16.valueOf(realTrailers[i]));
            String probe = base.toString();
            String result = nf.normalize(probe);
            if (!result.equals(probe)) {
                if (DEBUG) cause = "\t\tinteracts with " + ucd.getCodeAndName(realTrailers[i]);
                return false;
            }
        }
        
        // passed the sieve, so we are ok
        if (DEBUG) cause = "";
        return true;
    }
    
    // both the following should go into UTF16
    
    public static String replace(String source, int toReplace, int replacement) {
        if (0 <= toReplace && toReplace <= 0xFFFF
            && 0 <= replacement && replacement <= 0xFFFF) {
            return source.replace((char)toReplace, (char)replacement);
        }
        return replace(source, UTF16.valueOf(toReplace), UTF16.valueOf(replacement));
    }
    
    public static String replace(String source, String toReplace, String replacement) {
        int pos = 0;
        StringBuffer result = new StringBuffer(source.length());
        while (true) {
            int newPos = source.indexOf(toReplace, pos);
            if (newPos >= 0) {
                result.append(source.substring(pos, newPos));
                result.append(replacement);
                pos = newPos + toReplace.length();
            } else if (pos != 0) {
                result.append(source.substring(pos));
                return result.toString();
            } else {
                return source; // no change necessary
            }
        }
    }
    
    static void writeStringInPieces(PrintWriter pw, String s, String term) {
        int start;
        int end;
        int lineLen = 64;
        for (start = 0; ; start = end) {
            if (start == 0) pw.print("\t  \"");
            else pw.print("\t+ \"");
            end = s.length();
            if (end > start + lineLen) end = start + lineLen;
            
            // if we have a slash in the last 5 characters, backup
            
            int lastSlash = s.lastIndexOf('\\', end);
            if (lastSlash >= end-5) end = lastSlash;
            
            // backup if we broke on a \
            
            while (end > start && s.charAt(end-1) == '\\') --end;
            
            pw.print(s.substring(start, end));
            if (end == s.length()) {
                pw.println('"' + term);
                break;
            } else {
                pw.println('"');
            }
        }
    }
    
    static void testWriteStringInPieces() {
        String test =
	  "[^\\u00C0-\\u00C5\\u00C7-\\u00CF\\u00D1-\\u00D6\\u00D9-\\u00DD"
	+ "\\u00E0-\\u00E5\\u00E7-\\u00EF\\u00F1-\\u00F6\\u00F9-\\u00FD\\u00F"
	+ "F-\\u010F\\u0112-\\u0125\\u0128-\\u0130\\u0134-\\u0137\\u0139-"
	+ "\\u013E\\u0143-\\u0148\\u014C-\\u0151\\u0154-\\u0165\\u0168-\\u017"
	+ "E\\u01A0-\\u01A1\\u01AF-\\u01B0\\u01CD-\\u01DC\\u01DE-\\u01E3\\u"
	+ "01E6-\\u01F0\\u01F4-\\u01F5\\u01F8-\\u021B\\u021E-\\u021F\\u0226";
	    PrintWriter pw = new PrintWriter(System.out);
	    writeStringInPieces(pw,test,"");
	    writeStringInPieces(pw,replace(test, "\\", "\\\\"),"");
	    
	    pw.flush();
	}
        
    static int limit = 0x10FFFF; // full version = 10ffff, for testing may use smaller
    
    public static void main (String[] args) throws java.io.IOException {
        
        String version = ""; // Unicode version, "" = latest released
        
        PrintWriter out = Utility.openPrintWriter("NFSafeSets.txt");
        
        for (int mode = NFD_UnsafeStart; mode <= NFKC_UnsafeStart; ++mode) {
            UnicodeProperty up = DerivedProperty.make(mode, UCD.make(version));
            generateSet(out, "UNSAFE[" + Normalizer.getName((byte)(mode-NFD_UnsafeStart)) + "]", up);
        }
        
        for (byte mode = NFD; mode <= NFKC; ++mode) {
            NFSkippable skipper = new NFSkippable(mode,version);
            generateSet(out, "SKIPPABLE[" + Normalizer.getName(mode) + "]", skipper);
        }
        
        out.close();
    }
    
    static void generateSet(PrintWriter out, String label, UnicodeProperty up) {
        System.out.println("Generating: " + up.getName(NORMAL));
        UnicodeSet result = new UnicodeSet();
        for (int cp = 0; cp <= limit; ++cp) {
            Utility.dot(cp);
            if (up.hasValue(cp)) result.add(cp);
        }
        Utility.fixDot();
            
        String rSet = result.toPattern(true);          
        rSet = replace(rSet, "\\U", "\\\\U");
        out.println(label + " = new UnicodeSet(");
        writeStringInPieces(out, rSet, ", false);");
        out.println();
            
        rSet = result.toPattern(false);
        out.println("/*Unicode: ");
        writeStringInPieces(out, rSet, "*/");
        out.println();
        out.flush();
    }
    
            /*
       // DerivedProperty dp = new DerivedProperty(UCD.make(version));
        
            System.out.println(skipper.getName(NORMAL));
            
            UnicodeSet result = new UnicodeSet();
            for (int cp = 0; cp <= limit; ++cp) {
                Utility.dot(cp);
                if (skipper.hasProperty(cp)) result.add(cp);
            }
            Utility.fixDot();
            
            String rSet = result.toPattern(true);
            rSet = replace(rSet, "\\U", "\\\\U");
            out.println("\tSKIPPABLE[" + skipper.getName(NORMAL)
                + "] = new UnicodeSet(");
            writeStringInPieces(out, rSet, ", false);");
            out.println();
            
            rSet = result.toPattern(false);
            out.println("/*Unicode: ");
            */
            //writeStringInPieces(out, rSet, "*/");
            /*out.println();
            out.flush();
        
        if (false) {
            NFSkippable skipper = new NFSkippable(Normalizer.NFC,"");
            NFSkippable skipper2 = new NFSkippable(Normalizer.NFKC,"");
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                if (cp > 0xFF) {
                    if (!skipper.ucd.isAssigned(cp)) continue;
                    byte cat = skipper.ucd.getCategory(cp);
                    if (cat == PRIVATE_USE || cat == SURROGATE) continue;
                    if (skipper.ucd.getCombiningClass(cp) != 0) continue;
                    if (!skipper.nf.isNormalized(cp)) continue;
                    if ((cp < 0xAC00 || cp > 0xAE00)
                        && cp != skipper.ucd.mapToRepresentative(cp, false)) continue;
                }
                
                if (skipper2.hasProperty(cp) == skipper.hasProperty(cp)) continue;
                
                String status = (skipper.hasProperty(cp) ? "  SKIPc " : "NOSKIPc ")
                    + (skipper2.hasProperty(cp) ? "  SKIPkc " : "NOSKIPkc ");
                System.out.println(status
                    + skipper.ucd.getCodeAndName(cp)
                    + skipper.cause);
            }
        }
        */
    
}