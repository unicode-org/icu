package com.ibm.text.UCD;

import java.io.IOException;
import java.math.BigDecimal;

//import com.ibm.text.unicode.UInfo;
import java.util.*;
import java.io.*;
//import java.text.*;

import com.ibm.text.utility.*;

public class VerifyUCD implements UCD_Types {
    
    public static final String IDN_DIR = DATA_DIR + "\\IDN\\";
    static String ucdVersion = "";
    
    public static void main (String[] args) throws Exception {
                
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.charAt(0) == '#') return; // skip rest of line
            
            Utility.fixDot();
            System.out.println("Argument: " + args[i]);
            
            if      (arg.equalsIgnoreCase("all")) {
                //checkCase();
                checkCanonicalProperties();
                CheckCaseFold();
                checkAgainstUInfo();
                
            } else if (arg.equalsIgnoreCase("build")) {
                ConvertUCD.main(new String[]{ucdVersion});
            } else if (arg.equalsIgnoreCase("version")) ucdVersion = args[++i];
            else if (arg.equalsIgnoreCase("generateXML")) generateXML();
            else if (arg.equalsIgnoreCase("checkCase")) checkCase();
            else if (arg.equalsIgnoreCase("checkCase2")) checkCase2();
            else if (arg.equalsIgnoreCase("checkCanonicalProperties")) checkCanonicalProperties();
            else if (arg.equalsIgnoreCase("CheckCaseFold")) CheckCaseFold();
            else if (arg.equalsIgnoreCase("idn")) VerifyIDN();
            else if (arg.equalsIgnoreCase("NFTest")) NFTest();
            else if (arg.equalsIgnoreCase("test1")) test1();
            //else if (arg.equalsIgnoreCase("checkAgainstUInfo")) checkAgainstUInfo();
            else if (arg.equalsIgnoreCase("checkScripts")) checkScripts();
            else if (arg.equalsIgnoreCase("IdentifierTest")) IdentifierTest();
            else if (arg.equalsIgnoreCase("GenerateData")) GenerateData.main(Utility.split(args[++i],','));
            else if (arg.equalsIgnoreCase("BuildNames")) BuildNames.main(null);
            else if (arg.equalsIgnoreCase("writeNormalizerTestSuite")) 
                GenerateData.writeNormalizerTestSuite("NormalizationTest-3.1.1d1.txt");
            
            else {
                System.out.println("Unknown option -- must be one of the following (case-insensitive)");
                System.out.println("generateXML, checkCase, checkCanonicalProperties, CheckCaseFold,");
                System.out.println("VerifyIDN, NFTest, test1, ");
                // System.out.println(checkAgainstUInfo,");
                System.out.println("checkScripts, IdentifierTest, writeNormalizerTestSuite");
            }
        }
    }
    
        /*
        System.out.println(ucd.toString(0x0387));
        System.out.println(ucd.toString(0x00B7));
        System.out.println(ucd.toString(0x03a3));
        System.out.println(ucd.toString(0x03c2));
        System.out.println(ucd.toString(0x03c3));
        System.out.println(ucd.toString(0x0069));
        System.out.println(ucd.toString(0x0130));
        System.out.println(ucd.toString(0x0131));
        System.out.println(ucd.toString(0x0345));
        */
    
    static void checkAgainstOtherVersion(String otherVersion) {
        ucd = UCD.make(ucdVersion);
        UCD ucd2 = UCD.make(otherVersion);
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            UData curr = ucd.get(cp, true);
            UData other = ucd2.get(cp, true);
            if (!curr.equals(other)) {
                System.out.println("Difference at " + ucd.getCodeAndName(cp));
                System.out.println(curr);
                System.out.println(curr);
                System.out.println();
            }
        }
    }
    
    static void generateXML() throws IOException {
        ucd = UCD.make(ucdVersion);
        String filename = "UCD.xml";
        PrintWriter log = Utility.openPrintWriter(filename);
                
         //log.println('\uFEFF');
        log.println("<ucd>");
        
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!ucd.isRepresented(cp)) continue;
            if (cp == 0xE0026 || cp == 0x20000) {
                System.out.println("debug");
            }
            log.println(ucd.toString(cp));
        }
        
        log.println("</ucd>");
        log.close();
    }
    
    static final byte MIXED = (byte)(UNCASED + 1);
    
    public static void checkCase() throws IOException {
        Utility.fixDot();
        System.out.println("checkCase");
        ucd = UCD.make(ucdVersion);
        initNormalizers();
        System.out.println(ucd.getCase("ABC,DE'F G\u0308H", FULL, TITLE));
        String fileName = "CaseDifferences.txt";
        PrintWriter log = Utility.openPrintWriter(fileName);
        
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!ucd.isRepresented(cp) || ucd.isPUA(cp)) continue;
            if (cp == '\u3371') {
               System.out.println("debug");
            }
            String x = nfkd.normalize(cp);
            String xu = ucd.getCase(x, FULL, UPPER);
            String xl = ucd.getCase(x, FULL, LOWER);
            String xt = ucd.getCase(x, FULL, TITLE);
            
            byte caseCat = MIXED;
            if (xu.equals(xl)) caseCat = UNCASED;
            else if (x.equals(xl)) caseCat = LOWER;
            else if (x.equals(xu)) caseCat = UPPER;
            else if (x.equals(xt)) caseCat = TITLE;
            
            byte cat = ucd.getCategory(cp);
            boolean otherLower = ucd.getBinaryProperty(cp, Other_Lowercase);
            boolean otherUpper = ucd.getBinaryProperty(cp, Other_Uppercase);
            byte oldCaseCat = (cat == Lu || otherUpper) ? UPPER
                : (cat == Ll || otherLower) ? LOWER
                : (cat == Lt) ? TITLE
                : UNCASED;
                
            if (caseCat != oldCaseCat) {
                log.println(UTF32.valueOf32(cp)
                    + "\t" + names[caseCat] 
                    + "\t" + names[oldCaseCat]
                    + "\t" + ucd.getCategoryID_fromIndex(cat) 
                    + "\t" + lowerNames[otherLower ? 1 : 0] 
                    + "\t" + upperNames[otherUpper ? 1 : 0] 
                    + "\t" + ucd.getCodeAndName(cp) 
                    + "\t" + ucd.getCodeAndName(x)
                    + "\t" + ucd.getCodeAndName(xu)
                    + "\t" + ucd.getCodeAndName(xl)
                    + "\t" + ucd.getCodeAndName(xt)
                );
            }
        }
        
        log.close();
    }
    
    public static void checkCase2() throws IOException {
        Utility.fixDot();
        System.out.println("checkCase");
        ucd = UCD.make(ucdVersion);
        initNormalizers();
        System.out.println(ucd.getCase("ABC,DE'F G\u0308H", FULL, TITLE));
        String fileName = "CaseNormalizationDifferences.txt";
        PrintWriter log = Utility.openPrintWriter(fileName);
                
        log.println("Differences between case(normalize(cp)) and normalize(case(cp))");
        log.println("u, l, t - upper, lower, title");
        log.println("c, d - nfc, nfd");
        
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!ucd.isRepresented(cp) || ucd.isPUA(cp)) continue;
            if (cp == '\u3371') {
               System.out.println("debug");
            }
            
            String x = UTF32.valueOf32(cp);
            
            String ux = ucd.getCase(x, FULL, UPPER);
            String lx = ucd.getCase(x, FULL, LOWER);
            String tx = ucd.getCase(x, FULL, TITLE);
            
            String dux = nfd.normalize(ux);
            String dlx = nfd.normalize(lx);
            String dtx = nfd.normalize(tx);
            
            String cux = nfc.normalize(ux);
            String clx = nfc.normalize(lx);
            String ctx = nfc.normalize(tx);
            
            String dx = nfd.normalize(cp);
            String cx = nfc.normalize(cp);
            
            String udx = ucd.getCase(dx, FULL, UPPER);
            String ldx = ucd.getCase(dx, FULL, LOWER);
            String tdx = ucd.getCase(dx, FULL, TITLE);
            
            String ucx = ucd.getCase(cx, FULL, UPPER);
            String lcx = ucd.getCase(cx, FULL, LOWER);
            String tcx = ucd.getCase(cx, FULL, TITLE);
                   
            String dudx = nfd.normalize(udx);
            String dldx = nfd.normalize(ldx);
            String dtdx = nfd.normalize(tdx);
            
            String cucx = nfc.normalize(ucx);
            String clcx = nfc.normalize(lcx);
            String ctcx = nfc.normalize(tcx);
            
                   
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
                    log.println("Difference at " + ucd.getCodeAndName(cp));
                    if (!x.equals(ux)) log.println("\tu(cp):\t" + ucd.getCodeAndName(ux));
                    if (!x.equals(lx)) log.println("\tl(cp):\t" + ucd.getCodeAndName(lx));
                    if (!tx.equals(ux)) log.println("\tt(cp):\t" + ucd.getCodeAndName(tx));
                    if (!x.equals(dx)) log.println("\td(cp):\t" + ucd.getCodeAndName(dx));
                    if (!x.equals(cx)) log.println("\tc(cp):\t" + ucd.getCodeAndName(cx));
                    
                if (!dux.equals(udx)) {
                    log.println();
                    log.println("\td(u(cp)):\t" + ucd.getCodeAndName(dux));
                    log.println("\tu(d(cp)):\t" + ucd.getCodeAndName(udx));
                }
                if (!dlx.equals(ldx)) {
                    log.println();
                    log.println("\td(l(cp)):\t" + ucd.getCodeAndName(dlx));
                    log.println("\tl(d(cp)):\t" + ucd.getCodeAndName(ldx));
                }
                if (!dtx.equals(tdx)) {
                    log.println();
                    log.println("\td(t(cp)):\t" + ucd.getCodeAndName(dtx));
                    log.println("\tt(d(cp)):\t" + ucd.getCodeAndName(tdx));
                }
                    
                if (!cux.equals(ucx)) {
                    log.println();
                    log.println("\tc(u(cp)):\t" + ucd.getCodeAndName(cux));
                    log.println("\tu(c(cp)):\t" + ucd.getCodeAndName(ucx));
                }
                if (!clx.equals(lcx)) {
                    log.println();
                    log.println("\tc(l(cp)):\t" + ucd.getCodeAndName(clx));
                    log.println("\tl(c(cp)):\t" + ucd.getCodeAndName(lcx));
                }
                if (!ctx.equals(tcx)) {
                    log.println();
                    log.println("\tc(t(cp)):\t" + ucd.getCodeAndName(ctx));
                    log.println("\tt(c(cp)):\t" + ucd.getCodeAndName(tcx));
                }
                
                // ...........
 
                if (!udx.equals(dudx)) {
                    log.println();
                    log.println("\tu(d(cp)):\t" + ucd.getCodeAndName(udx));
                    log.println("\td(u(d(cp))):\t" + ucd.getCodeAndName(dudx));
                }
                if (!ldx.equals(dldx)) {
                    log.println();
                    log.println("\tl(d(cp)):\t" + ucd.getCodeAndName(ldx));
                    log.println("\td(l(d(cp))):\t" + ucd.getCodeAndName(dldx));
                }
                if (!tdx.equals(dtdx)) {
                    log.println();
                    log.println("\tt(d(cp)):\t" + ucd.getCodeAndName(tdx));
                    log.println("\td(t(d(cp))):\t" + ucd.getCodeAndName(dtdx));
                }
                    
                if (!ucx.equals(cucx)) {
                    log.println();
                    log.println("\tu(c(cp)):\t" + ucd.getCodeAndName(ucx));
                    log.println("\tc(u(c(cp))):\t" + ucd.getCodeAndName(cucx));
                }
                if (!lcx.equals(clcx)) {
                    log.println();
                    log.println("\tl(c(cp)):\t" + ucd.getCodeAndName(lcx));
                    log.println("\tc(l(c(cp))):\t" + ucd.getCodeAndName(clcx));
                }
                if (!tcx.equals(ctcx)) {
                    log.println();
                    log.println("\tt(c(cp)):\t" + ucd.getCodeAndName(tcx));
                    log.println("\tc(t(c(cp))):\t" + ucd.getCodeAndName(ctcx));
                }
            }
        }
        
        log.close();
    }
    
    static final String names[] = {"LOWER", "TITLE", "UPPER", "(UNC)", "MIXED"};
    static final String lowerNames[] = {"", "Other_Lower"};
    static final String upperNames[] = {"", "Other_Upper"};
    
    public static void CheckCaseFold() {
        ucd = UCD.make(ucdVersion);
        System.out.println("Checking Case Fold");
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!ucd.isAssigned(cp) || ucd.isPUA(cp)) continue;
            String fullTest = ucd.getCase(ucd.getCase(cp, FULL, UPPER), FULL, LOWER);
            String simpleTest = ucd.getCase(ucd.getCase(cp, SIMPLE, UPPER), SIMPLE, LOWER);
            
            String full = ucd.getCase(cp, FULL, FOLD);
            String simple = ucd.getCase(cp, SIMPLE, FOLD);
            
            boolean failed = false;
            if (!full.equals(fullTest)) {
                Utility.fixDot();
                System.out.println("Case fold fails at " + ucd.getCodeAndName(cp));
                System.out.println("  fullFold(ch):             " + ucd.getCodeAndName(full));
                System.out.println("  fullUpper(fullLower(ch)): " + ucd.getCodeAndName(fullTest));
                failed = true;
            }
            if (!simple.equals(simpleTest)) {
                Utility.fixDot();
                if (!failed) System.out.println("Case fold fails at " + ucd.getCodeAndName(cp));
                System.out.println("  simpleFold(ch):               " + ucd.getCodeAndName(simple));
                System.out.println("  simpleUpper(simpleLower(ch)): " + ucd.getCodeAndName(simpleTest));
                failed = true;
            }
            if (failed) System.out.println();
        }
    }
    
    public static void VerifyIDN() throws IOException {
        System.out.println("VerifyIDN");
        ucd = UCD.make(ucdVersion);
        initNormalizers();
        
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
            if (mappedOut.get(cp)) continue;
            
            boolean ucdUnassigned = !ucd.isAllocated(cp);
            boolean idnUnassigned = unassigned.get(cp);
            boolean guess = guessSet.get(cp);
            boolean idnProhibited = prohibited.get(cp);

            if (ucdUnassigned && !idnUnassigned) {
                showError("UCD Unassigned but not IDN Unassigned: ", cp);
                ++errorCount;
            } else if (!ucdUnassigned && idnUnassigned) {
                showError("Not UCD Unassigned but IDN Unassigned: ", cp);
                ++errorCount;
            }
            
            if (idnProhibited && unassigned.get(cp)) {
                showError("Both IDN Unassigned AND IDN Prohibited: ", cp);
                ++errorCount;
            }
            
            if (guess && !idnProhibited) {
                showError("UCD ?prohibited? but not IDN Prohibited: ", cp);
                ++errorCount;
            } else if (!guess && idnProhibited) {
                showError("Not UCD ?prohibited? but IDN Prohibited: ", cp);
                ++errorCount;
            }
            
        }
        System.out.println();
        System.out.println("Total Errors: " + errorCount);
    }
    
    static void showError(String description, int cp) {
        System.out.println(description + ucd.getCodeAndName(cp) + " (" + ucd.getCategoryID(cp) + ")");
    }
    
    
    public static BitSet guessIDN() {
        BitSet result = new BitSet();
        for (int cp = 0; cp < 0x10FFFF; ++cp) {
            int cat = ucd.getCategory(cp);
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
            if (ucd.getBinaryProperty(cp, Noncharacter_Code_Point)) result.set(cp);
            
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
                if (!ucd.isAssigned(cp) || ucd.isPUA(cp)) throw new ChainException("IDN character unassigned or PUA: " + line, null);
                String value = Utility.fromHex(parts[1]);
                String reason = parts[2].trim();

                if (reason.equals("Map out")) {
                    value = Utility.fromHex(parts[1]);
                    Utility.fixDot();
                    System.out.println("Note, Mapping Out: " + ucd.getCodeAndName(cp)
                        + ", " + ucd.getCodeAndName(value) + ", " + ucd.getCategoryID(cp));
                    mappedOut.set(cp);
                }
                idnFold.put(key, value);
                idnWhy.put(key, reason);
            }
            
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                Utility.dot(cp);
                if (!ucd.isAssigned(cp) || ucd.isPUA(cp)) continue;
                if (mappedOut.get(cp)) continue;
                
                String key = UTF32.valueOf32(cp);
                String value = (String)idnFold.get(key);
                if (value == null) value = key;
                String reason = (String)idnWhy.get(key);
                String ucdFold = ucd.getCase(cp, FULL, FOLD, "I");
                if (!ucdFold.equals(value)) {
                    String b = nfkc.normalize(ucd.getCase(cp, FULL, FOLD, "I"));
                    String c = nfkc.normalize(ucd.getCase(b, FULL, FOLD, "I"));

                    if (c.equals(value)) continue;
                    Utility.fixDot();
                    
                    System.out.println("Mismatch: " + ucd.getCodeAndName(cp));
                    System.out.println("  UCD Case Fold: <" + ucd.getCodeAndName(ucdFold) + ">");
                    System.out.println("  IDN Map [" + reason + "]: <" + ucd.getCodeAndName(value) + ">");
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
    
    
    private static void IdentifierTest() {
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
                if (!ucd.isAssigned(cp)) continue;
                if (ucd.isPUA(cp)) continue;
                if (!normalizationDiffers(cp, j)) continue;
                
                if (cp == 0xFDFB || cp == 0x0140) {
                    System.out.println("debug point");
                }
                
                boolean norm;
                boolean plain;

                String x_cp = 'x' + UTF32.valueOf32(cp);
                String nfx_x_cp = normalize(x_cp, j);
                plain = ucd.isIdentifier(x_cp, true);
                norm = ucd.isIdentifier(nfx_x_cp, true);
                if (plain & !norm) {
                    Utility.fixDot();
                    System.out.println("*Not Identifier: " + ucd.getCodeAndName(cp));
                    System.out.println("    nfx_x_cp: " + ucd.getCodeAndName(nfx_x_cp));
                    
                    System.out.println("  isIdentifier(nfx_x_cp, true): " + norm);
                    System.out.println("    cat(nfx_x_cp): " + getCategoryID(nfx_x_cp));
                    
                    System.out.println("  isIdentifier(x_cp, true): " + plain);
                    System.out.println("    cat(x_cp): " + getCategoryID(x_cp));
                    continue;
                }
                
                String nfx_cp = normalize(UTF32.valueOf32(cp), j);
                plain = ucd.isIdentifierStart(cp, true);
                norm = ucd.isIdentifier(nfx_cp, true);
                if (plain & !norm) {
                    Utility.fixDot();
                    System.out.println(" Changes Category: " + ucd.getCodeAndName(cp));
                    System.out.println("    nfx_cp: " + ucd.getCodeAndName(nfx_cp));
                    
                    System.out.println("  isIdentifier(nfx_cp, true): " + norm);
                    System.out.println("    cat(nfx_cp): " + getCategoryID(nfx_cp));
                    
                    System.out.println("  isIdentifierStart(cp, true): " + plain);
                    System.out.println("    cat(cp): " + ucd.getCategoryID(cp));
                    System.out.println();
                    continue;
                }
            }
        }
    }
    
    static String getCategoryID(String s) {
        if (UTF32.length32(s) == 1) return ucd.getCategoryID(UTF32.char32At(s, 0));
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            if (i != 0) result.append(' ');
            result.append(ucd.getCategoryID(cp));
        }
        return result.toString();
    }
    
    static String normalize(String s, int j) {
        if (j < 4) return nf[j].normalize(s);
        return ucd.getCase(s, FULL, FOLD);
    }
    
    static boolean normalizationDiffers(int cp, int j) {
        if (j < 4) return nf[j].normalizationDiffers(cp);
        return true;
    }
    
    private static Normalizer[] nf = new Normalizer[4];
    private static Normalizer nfd, nfc, nfkd, nfkc;
    
    static void initNormalizers() {
        nfd = nf[0] = new Normalizer(Normalizer.NFD);
        nfc = nf[1] = new Normalizer(Normalizer.NFC);
        nfkd = nf[2] = new Normalizer(Normalizer.NFKD);
        nfkc = nf[3] = new Normalizer(Normalizer.NFKC);
    }
    
    private static UCD ucd;
    private static final String[] NAMES = {"NFD", "NFC", "NFKD", "NFKC", "Fold"};
    
    private static void NFTest() {
        initNormalizers();
        for (int j = 0; j < 4; ++j) {
            Normalizer nfx = nf[j];
            System.out.println();
            System.out.println("Testing normalizationDiffers for " + NAMES[j]);
            System.out.println();
            for (int i = 0; i < 0x10FFFF; ++i) {
                Utility.dot(i);
                if (!ucd.isAssigned(i)) continue;
                if (ucd.isPUA(i)) continue;
                String s = nfx.normalize(i);
                boolean differs = !s.equals(UTF32.valueOf32(i));
                boolean call = nfx.normalizationDiffers(i);
                if (differs != call) {
                    Utility.fixDot();
                    System.out.println("Problem: differs: " + differs
                     + ", call: " + call + " " + ucd.getCodeAndName(i));
                }
            }
                
        }
    }
    
    public static void checkScripts() {
        ucd = UCD.make(ucdVersion);
        for (int i = 0; i < 0x10FFFF; ++i) {
            //byte script = ucd.getScript(i);
            if (true) { // script != COMMON_SCRIPT) {
                System.out.println(Utility.hex(i) + "; " + ucd.getScriptID(i) + " # " + ucd.getName(i));
            }
        }
    }
    
    public static void checkAgainstUInfo() {
    /*
        ucd = UCD.make(ucdVersion);
        UData x = new UData();
        x.fleshOut();
        
        System.out.println(ucd.toString(0x1E0A));
        
        UInfo.init();
        System.out.println("Cross-checking against old implementation");
        System.out.println("Version: " + ucd.getVersion() + ", " + new Date(ucd.getDate()));
        for (int i = 0; i <= 0xFFFF; ++i) {
            Utility.dot(i);
            
            if ((i & 0x0FFF) == 0) System.out.println("#" + Utility.hex(i));
            try {
                check(i, ucd.getName(i), UInfo.getName((char)i), "Name");
                check(i, ucd.getCategory(i), UInfo.getCategory((char)i), UCD_Names.GC, "GeneralCategory");
                check(i, ucd.getCombiningClass(i), UInfo.getCanonicalClass((char)i), "CanonicalClass");
                check(i, ucd.getBidiClass(i), UInfo.getBidiClass((char)i), UCD_Names.BC, "BidiClass");
                check(i, ucd.getDecompositionMapping(i), UInfo.getDecomposition((char)i), "Decomposition");
                check(i, ucd.getDecompositionType(i), UInfo.getDecompositionType((char)i), UCD_Names.DT, "DecompositionType");
                check(i, ucd.getNumericValue(i), UInfo.getNumeric((char)i), "NumericValue");
                check(i, ucd.getNumericType(i), UInfo.getNumericType((char)i), UCD_Names.NT, "NumericType");
                
                check(i, ucd.getCase(i, SIMPLE, LOWER), UInfo.getLowercase((char)i), "SimpleLowercase");
                check(i, ucd.getCase(i, SIMPLE, UPPER), UInfo.getUppercase((char)i), "SimpleUppercase");
                check(i, ucd.getCase(i, SIMPLE, TITLE), UInfo.getTitlecase((char)i), "SimpleTitlecase");
                //check(i, ucd.getSimpleCaseFolding(i), UInfo.getSimpleCaseFolding((char)i));
                
                if (ucd.getSpecialCase(i).length() == 0) {  // NORMAL
                    check(i, ucd.getCase(i, FULL, LOWER), UInfo.toLowercase((char)i, ""), "FullLowercase");
                    check(i, ucd.getCase(i, FULL, UPPER), UInfo.toUppercase((char)i, ""), "FullUppercase");
                    check(i, ucd.getCase(i, FULL, TITLE), UInfo.toTitlecase((char)i, ""), "FullTitlecase");
                } else {                                    // SPECIAL
                    check(i, ucd.getCase(i, SIMPLE, LOWER), UInfo.toLowercase((char)i, ""), "FullLowercase");
                    check(i, ucd.getCase(i, SIMPLE, UPPER), UInfo.toUppercase((char)i, ""), "FullUppercase");
                    check(i, ucd.getCase(i, SIMPLE, TITLE), UInfo.toTitlecase((char)i, ""), "FullTitlecase");
                }
                // check(i, ucd.getFullCaseFolding(i), UInfo.getFullCaseFolding((char)i));
                
                check(i, ucd.getSpecialCase(i).toUpperCase(), UInfo.getCaseCondition((char)i).toUpperCase(), "SpecialCase");
                check(i, ucd.getLineBreak(i), UInfo.getLineBreakType((char)i), UCD_Names.LB, "LineBreak");
                check(i, ucd.getEastAsianWidth(i), UInfo.getEastAsianWidthType((char)i), UCD_Names.EA, "EastAsian");
                
                int props = ucd.getBinaryProperties(i);
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
            String s = ucd.getDecompositionMapping(cp);
            System.out.print(ucd.getCodeAndName(cp));
            if (showCanonicalDecomposition && !s.equals(UTF32.valueOf32(cp))) {
                System.out.print(" => " + ucd.getCodeAndName(s));
            }
            System.out.println();
            lastShowed = cp;
        }
    }
    
    public static void test1() {
        ucd = UCD.make(ucdVersion);
        
        for (int i = 0x19; i < 0x10FFFF; ++i) {
            
            System.out.println(Utility.hex(i) + " " + Utility.quoteJavaString(ucd.getName(i)));
                
            System.out.print("    "
                + ", gc=" + ucd.getCategoryID(i)
                + ", bc=" + ucd.getBidiClassID(i)
                + ", cc=" + ucd.getCombiningClassID(i)
                + ", ea=" + ucd.getEastAsianWidthID(i)
                + ", lb=" + ucd.getLineBreakID(i)
                + ", dt=" + ucd.getDecompositionTypeID(i)
                + ", nt=" + ucd.getNumericTypeID(i)
                + ", nv=" + ucd.getNumericValue(i)
            );
            for (int j = 0; j < UCD_Types.LIMIT_BINARY_PROPERTIES; ++j) {
                if (ucd.getBinaryProperty(i,j)) System.out.print(", " + UCD_Names.BP[j]);
            }
            System.out.println();
            
            System.out.println("    "
                + ", dm=" + Utility.quoteJavaString(ucd.getDecompositionMapping(i))
                + ", slc=" + Utility.quoteJavaString(ucd.getCase(i, SIMPLE, LOWER))
                + ", stc=" + Utility.quoteJavaString(ucd.getCase(i, SIMPLE, TITLE))
                + ", suc=" + Utility.quoteJavaString(ucd.getCase(i, SIMPLE, UPPER))
                + ", flc=" + Utility.quoteJavaString(ucd.getCase(i, FULL, LOWER))
                + ", ftc=" + Utility.quoteJavaString(ucd.getCase(i, FULL, TITLE))
                + ", fuc=" + Utility.quoteJavaString(ucd.getCase(i, FULL, UPPER))
                + ", sc=" + Utility.quoteJavaString(ucd.getSpecialCase(i))
            );
            
            if (i > 0x180) i = 3 * i / 2;
        }
    }
    
    static void checkCanonicalProperties() {
        ucd = UCD.make(ucdVersion);
        System.out.println(ucd.toString(0x1E0A));
        
        System.out.println("Cross-checking canonical equivalence");
        System.out.println("Version: " + ucd.getVersion() + ", " + new Date(ucd.getDate()));
        showCanonicalDecomposition = true;
        for (int q = 1; q < 2; ++q)
        for (int i = 0; i <= 0x10FFFF; ++i) {
            Utility.dot(i);
            if (i == 0x0387) {
                System.out.println("debug?");
            }
            byte type = ucd.getDecompositionType(i);
            if (type != CANONICAL) continue;
            
            String s = ucd.getDecompositionMapping(i);
            int slen = UTF32.length32(s);
            int j = UTF32.char32At(s, 0);
            try {
                if (q == 0) {
                check(i, ucd.getCategory(i), ucd.getCategory(j), UCD_Names.GC, "GeneralCategory");
                check(i, ucd.getCombiningClass(i), ucd.getCombiningClass(j), "CanonicalClass");
                check(i, ucd.getBidiClass(i), ucd.getBidiClass(j), UCD_Names.BC, "BidiClass");
                check(i, ucd.getNumericValue(i), ucd.getNumericValue(j), "NumericValue");
                check(i, ucd.getNumericType(i), ucd.getNumericType(j), UCD_Names.NT, "NumericType");
                
                if (false) {
                    for (byte k = LOWER; k <= FOLD; ++k) {
                        check(i, ucd.getCase(i, SIMPLE, k), ucd.getCase(j, SIMPLE, k), "Simple("+k+")");
                        check(i, ucd.getCase(i, FULL, k), ucd.getCase(j, FULL, k), "Full("+k+")");
                    }
                }
                                
                if (slen == 1) check(i, ucd.getSpecialCase(i), ucd.getSpecialCase(j), "SpecialCase");
                
                for (byte k = 0; k < LIMIT_BINARY_PROPERTIES; ++k) {
                    if (k == Hex_Digit) continue;
                    if (k == Radical) continue;
                    if (k == UnifiedIdeograph) continue;
                    if (k == CompositionExclusion) continue;
                    check(i, ucd.getBinaryProperty(i, k), ucd.getBinaryProperty(j, k), UCD_Names.YN_TABLE, ucd.getBinaryPropertiesID_fromIndex(k));
                }
                } else {
                    //check(i, ucd.getLineBreak(i), ucd.getLineBreak(j), UCD_Names.LB, "LineBreak");
                    //check(i, ucd.getEastAsianWidth(i), ucd.getEastAsianWidth(j), UCD_Names.EA, "EastAsian");
                }
                
            } catch (Exception e) {
                System.out.println("Error: " + Utility.hex(i) + " " + e.getClass().getName() + e.getMessage());
                e.printStackTrace();
            }
        }
  }
        
}
