package com.ibm.test.translit;
import com.ibm.text.*;
import com.ibm.test.*;
import com.ibm.util.Utility;
import java.text.*;
import java.util.*;

/**
 * @test
 * @summary Test the Latin-Jamo transliterator
 */
public class JamoTest extends TransliteratorTest {

    public static void main(String[] args) throws Exception {
        new JamoTest().run(args);
    }

    public void TestJamo() {
        Transliterator latinJamo = Transliterator.getInstance("Latin-Jamo");
        Transliterator jamoLatin = latinJamo.getInverse();

        String[] CASE = {
            // Column 1 is the latin text L1 to be fed to Latin-Jamo
            // to yield output J.

            // Column 2 is expected value of J.  J is fed to
            // Jamo-Latin to yield output L2.

            // Column 3 is expected value of L2.  If the expected
            // value of L2 is L1, then L2 is null.
            "bab", "(Bi)(A)(Bf)", null,
            "babb", "(Bi)(A)(Bf)(Bi)(EU)", "babbeu",
            "babbba", "(Bi)(A)(Bf)(BB)(A)", null,
            "bagg", "(Bi)(A)(GGf)", null,
            "baggga", "(Bi)(A)(GGf)(Gi)(A)", null,
            "bag'gga", "(Bi)(A)(Gf)(GGi)(A)", null,
            "kabsa", "(Ki)(A)(Bf)(Si)(A)", null,
            "kabska", "(Ki)(A)(BS)(Ki)(A)", null,
            "gabsbka", "(Gi)(A)(BS)(Bi)(EU)(Ki)(A)", "gabsbeuka", // not (Kf)
            "gga", "(GGi)(A)", null,
            "bsa", "(Bi)(EU)(Si)(A)", "beusa",
            "agg", "(IEUNG)(A)(GGf)", null,
            "agga", "(IEUNG)(A)(Gf)(Gi)(A)", null,
            "la", "(R)(A)", "ra",
            "bs", "(Bi)(EU)(Sf)", "beus",
        };

        for (int i=0; i<CASE.length; i+=3) {
            String jamo = nameToJamo(CASE[i+1]);
            if (CASE[i+2] == null) {
                expect(latinJamo, CASE[i], jamo, jamoLatin);
            } else {
                // Handle case where round-trip is expected to fail
                expect(latinJamo, CASE[i], jamo);
                expect(jamoLatin, jamo, CASE[i+2]);
            }
        }
    }

    // TransliteratorTest override
    void expectAux(String tag, String summary, boolean pass,
                   String expectedResult) {
        super.expectAux(tag, jamoToName(summary),
                        pass, jamoToName(expectedResult));
    }

    // UTILITIES

    static final String[] JAMO_NAMES = {
        "(Gi)", "\u1100",
        "(GGi)", "\u1101",
        "(Ni)", "\u1102",
        "(Di)", "\u1103",
        "(DD)", "\u1104",
        "(R)", "\u1105",
        "(Mi)", "\u1106",
        "(Bi)", "\u1107",
        "(BB)", "\u1108",
        "(Si)", "\u1109",
        "(SSi)", "\u110A",
        "(IEUNG)", "\u110B",
        "(Ji)", "\u110C",
        "(JJ)", "\u110D",
        "(Ci)", "\u110E",
        "(Ki)", "\u110F",
        "(Ti)", "\u1110",
        "(Pi)", "\u1111",
        "(Hi)", "\u1112",
        
        "(A)", "\u1161",
        "(AE)", "\u1162",
        "(YA)", "\u1163",
        "(YAE)", "\u1164",
        "(EO)", "\u1165",
        "(E)", "\u1166",
        "(YEO)", "\u1167",
        "(YE)", "\u1168",
        "(O)", "\u1169",
        "(WA)", "\u116A",
        "(WAE)", "\u116B",
        "(OE)", "\u116C",
        "(YO)", "\u116D",
        "(U)", "\u116E",
        "(WEO)", "\u116F",
        "(WE)", "\u1170",
        "(WI)", "\u1171",
        "(YU)", "\u1172",
        "(EU)", "\u1173",
        "(YI)", "\u1174",
        "(I)", "\u1175",

        "(Gf)", "\u11A8",
        "(GGf)", "\u11A9",
        "(GS)", "\u11AA",
        "(Nf)", "\u11AB",
        "(NJ)", "\u11AC",
        "(NH)", "\u11AD",
        "(Df)", "\u11AE",
        "(L)", "\u11AF",
        "(LG)", "\u11B0",
        "(LM)", "\u11B1",
        "(LB)", "\u11B2",
        "(LS)", "\u11B3",
        "(LT)", "\u11B4",
        "(LP)", "\u11B5",
        "(LH)", "\u11B6",
        "(Mf)", "\u11B7",
        "(Bf)", "\u11B8",
        "(BS)", "\u11B9",
        "(Sf)", "\u11BA",
        "(SSf)", "\u11BB",
        "(NG)", "\u11BC",
        "(Jf)", "\u11BD",
        "(Cf)", "\u11BE",
        "(Kf)", "\u11BF",
        "(Tf)", "\u11C0",
        "(Pf)", "\u11C1",
        "(Hf)", "\u11C2",
    };

    static Hashtable JAMO_TO_NAME;
    static Hashtable NAME_TO_JAMO;

    static {
        JAMO_TO_NAME = new Hashtable();
        NAME_TO_JAMO = new Hashtable();
        for (int i=0; i<JAMO_NAMES.length; i+=2) {
            JAMO_TO_NAME.put(JAMO_NAMES[i+1], JAMO_NAMES[i]);
            NAME_TO_JAMO.put(JAMO_NAMES[i], JAMO_NAMES[i+1]);
        }
    }

    /**
     * Convert short names to actual jamo.  E.g., "x(LG)y" returns
     * "x\u11B0y".  See JAMO_NAMES for table of names.
     */
    static String nameToJamo(String input) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<input.length(); ++i) {
            char c = input.charAt(i);
            if (c == '(') {
                int j = input.indexOf(')', i+1);
                if ((j-i) >= 2 && (j-i) <= 6) { // "(A)", "(IEUNG)"
                    String jamo = (String) NAME_TO_JAMO.get(input.substring(i, j+1));
                    if (jamo != null) {
                        buf.append(jamo);
                        i = j;
                        continue;
                    }
                }
            }
            buf.append(c);
        }
        return buf.toString();
    }

    /**
     * Convert jamo to short names.  E.g., "x\u11B0y" returns
     * "x(LG)y".  See JAMO_NAMES for table of names.
     */
    static String jamoToName(String input) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<input.length(); ++i) {
            char c = input.charAt(i);
            if (c >= 0x1100 && c <= 0x11C2) {
                String name = (String) JAMO_TO_NAME.get(input.substring(i, i+1));
                if (name != null) {
                    buf.append(name);
                    continue;
                }
            }
            buf.append(c);
        }
        return buf.toString();
    }
}
