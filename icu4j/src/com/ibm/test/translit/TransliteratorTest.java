import com.ibm.text.*;
import java.text.*;
import java.util.*;

/**
 * @test
 * @summary General test of Transliterator
 */
public class TransliteratorTest extends IntlTest {

    public static void main(String[] args) throws Exception {
        new TransliteratorTest().run(args);
    }

    /**
     * A CommonPoint legacy round-trip test for the Kana transliterator.
     */
//    public void TestKanaRoundTrip() {
//        Transliterator t = Transliterator.getInstance("Kana");
//        StringTokenizer tok = new StringTokenizer(KANA_RT_DATA);
//        while (tok.hasMoreTokens()) {
//            String str = tok.nextToken();
//            ReplaceableString tmp = new ReplaceableString(str);
//            t.transliterate(tmp, Transliterator.FORWARD);
//
//            str = tmp.toString();
//            tmp = new ReplaceableString(str);
//            t.transliterate(tmp, Transliterator.REVERSE);
//            t.transliterate(tmp, Transliterator.FORWARD);
//            if (!tmp.toString().equals(str)) {
//                tmp = new ReplaceableString(str);
//                t.transliterate(tmp, Transliterator.REVERSE);
//                String a = tmp.toString();
//                t.transliterate(tmp, Transliterator.FORWARD);
//                errln("FAIL: " + escape(str) + " -> " +
//                      escape(a) + " -> " + escape(tmp.toString()));
//            }
//        }
//    }

    public void TestInstantiation() {
        long ms = System.currentTimeMillis();
        String ID;
        for (Enumeration e = Transliterator.getAvailableIDs(); e.hasMoreElements(); ) {
            ID = (String) e.nextElement();
            try {
                Transliterator t = Transliterator.getInstance(ID);
                // We should get a new instance if we try again
                Transliterator t2 = Transliterator.getInstance(ID);
                if (t != t2) {
                    logln(ID + ":" + t);
                } else {
                    errln("FAIL: " + ID + " returned identical instances");
                }
            } catch (IllegalArgumentException ex) {
                errln("FAIL: " + ID);
                throw ex;
            }
        }

        // Now test the failure path
        try {
            ID = "<Not a valid Transliterator ID>";
            Transliterator t = Transliterator.getInstance(ID);
            errln("FAIL: " + ID + " returned " + t);
        } catch (IllegalArgumentException ex) {
            logln("OK: Bogus ID handled properly");
        }
        
        ms = System.currentTimeMillis() - ms;
        logln("Elapsed time: " + ms + " ms");
    }

    public void TestSimpleRules() {
        /* Example: rules 1. ab>x|y
         *                2. yc>z
         *
         * []|eabcd  start - no match, copy e to tranlated buffer
         * [e]|abcd  match rule 1 - copy output & adjust cursor
         * [ex|y]cd  match rule 2 - copy output & adjust cursor
         * [exz]|d   no match, copy d to transliterated buffer
         * [exzd]|   done
         */
        expect("ab>x|y\n" +
               "yc>z",
               "eabcd", "exzd");

        /* Another set of rules:
         *    1. ab>x|yzacw
         *    2. za>q
         *    3. qc>r
         *    4. cw>n
         *
         * []|ab       Rule 1
         * [x|yzacw]   No match
         * [xy|zacw]   Rule 2
         * [xyq|cw]    Rule 4
         * [xyqn]|     Done
         */
        expect("ab>x|yzacw\n" +
               "za>q\n" +
               "qc>r\n" +
               "cw>n",
               "ab", "xyqn");

        /* Test categories
         */
        Transliterator t = new RuleBasedTransliterator("<ID>",
                                                       "dummy=\uE100\n" +
                                                       "vowel=[aeiouAEIOU]\n" +
                                                       "lu=[:Lu:]\n" +
                                                       "{vowel}[{lu}>!\n" +
                                                       "{vowel}>&\n" +
                                                       "!]{lu}>^\n" +
                                                       "{lu}>*\n" +
                                                       "a>ERROR");
        expect(t, "abcdefgABCDEFGU", "&bcd&fg!^**!^*&");
    }

    // Restore this test if/when it's been deciphered.  In general,
    // tests that depend on a specific tranliterator are subject
    // to the same fragility as tests that depend on resource data.

//    public void TestKana() {
//        String DATA[] = {
//            "a", "\u3042",
//            "A", "\u30A2",
//            "aA", "\u3042\u30A2",
//            "aaaa", "\u3042\u3042\u3042\u3042",
//            "akasata", "\u3042\u304B\u3055\u305F",
//        };
//
//        Transliterator t = Transliterator.getInstance("Latin-Kana");
//        Transliterator rt = Transliterator.getInstance("Kana-Latin");
//        for (int i=0; i<DATA.length; i+=2) {
//            expect(t, DATA[i], DATA[i+1], rt);
//        }
//    }


    /**
     * Create some inverses and confirm that they work.  We have to be
     * careful how we do this, since the inverses will not be true
     * inverses -- we can't throw any random string at the composition
     * of the transliterators and expect the identity function.  F x
     * F' != I.  However, if we are careful about the input, we will
     * get the expected results.
     */
    public void TestRuleBasedInverse() {
        String RULES =
            "abc>zyx\n" +
            "ab>yz\n" +
            "bc>zx\n" +
            "ca>xy\n" +
            "a>x\n" +
            "b>y\n" +
            "c>z\n" +

            "abc<zyx\n" +
            "ab<yz\n" +
            "bc<zx\n" +
            "ca<xy\n" +
            "a<x\n" +
            "b<y\n" +
            "c<z\n" +

            "";

        String[] DATA = {
            // Careful here -- random strings will not work.  If we keep
            // the left side to the domain and the right side to the range
            // we will be okay though (left, abc; right xyz).
            "a", "x",
            "abcacab", "zyxxxyy",
            "caccb", "xyzzy",
        };

        Transliterator fwd = new RuleBasedTransliterator("<ID>", RULES);
        Transliterator rev = new RuleBasedTransliterator("<ID>", RULES,
                                     RuleBasedTransliterator.REVERSE, null);
        for (int i=0; i<DATA.length; i+=2) {
            expect(fwd, DATA[i], DATA[i+1]);
            expect(rev, DATA[i+1], DATA[i]);
        }
    }

    /**
     * Basic test of keyboard.
     */
    public void TestKeyboard() {
        Transliterator t = new RuleBasedTransliterator("<ID>", 
                                                       "psch>Y\n"
                                                       +"ps>y\n"
                                                       +"ch>x\n"
                                                       +"a>A\n");
        String DATA[] = {
            // insertion, buffer
            "a", "A",
            "p", "Ap",
            "s", "Aps",
            "c", "Apsc",
            "a", "AycA",
            "psch", "AycAY",
            null, "AycAY", // null means finishKeyboardTransliteration
        };

        keyboardAux(t, DATA);
    }

    /**
     * Basic test of keyboard with cursor.
     */
    public void TestKeyboard2() {
        Transliterator t = new RuleBasedTransliterator("<ID>", 
                                                       "ych>Y\n"
                                                       +"ps>|y\n"
                                                       +"ch>x\n"
                                                       +"a>A\n");
        String DATA[] = {
            // insertion, buffer
            "a", "A",
            "p", "Ap",
            "s", "Ay",
            "c", "Ayc",
            "a", "AycA",
            "p", "AycAp",
            "s", "AycAy",
            "c", "AycAyc",
            "h", "AycAY",
            null, "AycAY", // null means finishKeyboardTransliteration
        };

        keyboardAux(t, DATA);
    }

    /**
     * Test keyboard transliteration with back-replacement.
     */
    public void TestKeyboard3() {
        // We want th>z but t>y.  Furthermore, during keyboard
        // transliteration we want t>y then yh>z if t, then h are
        // typed.
        String RULES =
            "t>|y\n" +
            "yh>z\n" +
            "";

        String[] DATA = {
            // Column 1: characters to add to buffer (as if typed)
            // Column 2: expected appearance of buffer after
            //           keyboard xliteration.
            "a", "a",
            "b", "ab",
            "t", "aby",
            "c", "abyc",
            "t", "abycy",
            "h", "abycz",
            null, "abycz", // null means finishKeyboardTransliteration
        };

        Transliterator t = new RuleBasedTransliterator("<ID>", RULES);
        keyboardAux(t, DATA);
    }

    private void keyboardAux(Transliterator t, String[] DATA) {
        int[] index = {0, 0, 0};
        ReplaceableString s = new ReplaceableString();
        for (int i=0; i<DATA.length; i+=2) {
            StringBuffer log;
            if (DATA[i] != null) {
                log = new StringBuffer(s.toString() + " + "
                                       + DATA[i]
                                       + " -> ");
                t.keyboardTransliterate(s, index, DATA[i]);
            } else {
                log = new StringBuffer(s.toString() + " => ");
                t.finishKeyboardTransliteration(s, index);
            }
            String str = s.toString();
            // Show the start index '{' and the cursor '|'
            log.append(str.substring(0, index[Transliterator.START])).
                append('{').
                append(str.substring(index[Transliterator.START],
                                     index[Transliterator.CURSOR])).
                append('|').
                append(str.substring(index[Transliterator.CURSOR]));
            if (str.equals(DATA[i+1])) {
                logln(log.toString());
            } else {
                errln("FAIL: " + log.toString() + ", expected " + DATA[i+1]);
            }
        }
    }

    public void TestArabic() {
        String DATA[] = {
            "Arabic", "\u062a\u062a\u0645\u062a\u0639\u0020"+
                      "\u0627\u0644\u0644\u063a\u0629\u0020"+
                      "\u0627\u0644\u0639\u0631\u0628\u0628\u064a\u0629\u0020"+
                      "\u0628\u0628\u0646\u0638\u0645\u0020"+
                      "\u0643\u062a\u0627\u0628\u0628\u064a\u0629\u0020"+
                      "\u062c\u0645\u064a\u0644\u0629",
        };

        Transliterator t = Transliterator.getInstance("Latin-Arabic");
        for (int i=0; i<DATA.length; i+=2) {
            expect(t, DATA[i], DATA[i+1]);
        }
    }

    /**
     * Compose the Kana transliterator forward and reverse and try
     * some strings that should come out unchanged.
     */
    public void TestCompoundKana() {
        Transliterator kana = Transliterator.getInstance("Latin-Kana");
        Transliterator rkana = Transliterator.getInstance("Kana-Latin");
        Transliterator[] trans = { kana, rkana };
        Transliterator t = new CompoundTransliterator("<ID>", trans);

        expect(t, "aaaaa", "aaaaa");
    }

    /**
     * Compose the hex transliterators forward and reverse.
     */
    public void TestCompoundHex() {
        Transliterator a = Transliterator.getInstance("Unicode-Hex");
        Transliterator b = Transliterator.getInstance("Hex-Unicode");
        Transliterator[] trans = { a, b };
        Transliterator ab = new CompoundTransliterator("ab", trans);
        String s = "abcde";
        expect(ab, s, s);

        trans = new Transliterator[] { b, a };
        Transliterator ba = new CompoundTransliterator("ba", trans);
        ReplaceableString str = new ReplaceableString(s);
        a.transliterate(str);
        expect(ba, str.toString(), str.toString());
    }

    /**
     * Do some basic tests of filtering.
     */
    public void TestFiltering() {
        Transliterator hex = Transliterator.getInstance("Unicode-Hex");
        hex.setFilter(new UnicodeFilter() {
            public boolean isIn(char c) {
                return c != 'c';
            }
        });
        String s = "abcde";
        String out = hex.transliterate(s);
        String exp = "\\u0061\\u0062c\\u0064\\u0065";
        if (out.equals(exp)) {
            logln("Ok:   \"" + exp + "\"");
        } else {
            logln("FAIL: \"" + out + "\", wanted \"" + exp + "\"");
        }
    }

    //======================================================================
    // Support methods
    //======================================================================

    void expect(String rules, String source, String expectedResult) {
        expect(new RuleBasedTransliterator("<ID>", rules), source, expectedResult);
    }

    void expect(Transliterator t, String source, String expectedResult,
                Transliterator reverseTransliterator) {
        expect(t, source, expectedResult);
        if (reverseTransliterator != null) {
            expect(reverseTransliterator, expectedResult, source);
        }
    }

    void expect(Transliterator t, String source, String expectedResult) {
        String result = t.transliterate(source);
        expectAux(t.getID() + ":String", source, result, expectedResult);

        ReplaceableString rsource = new ReplaceableString(source);
        t.transliterate(rsource);
        result = rsource.toString();
        expectAux(t.getID() + ":Replaceable", source, result, expectedResult);

        // Test keyboard (incremental) transliteration -- this result
        // must be the same after we finalize (see below).
        rsource.getStringBuffer().setLength(0);
        int[] index = { 0, 0, 0 };
        StringBuffer log = new StringBuffer();

        for (int i=0; i<source.length(); ++i) {
            if (i != 0) {
                log.append(" + ");
            }
            log.append(source.charAt(i)).append(" -> ");
            t.keyboardTransliterate(rsource, index,
                                    String.valueOf(source.charAt(i)));
            // Append the string buffer with a vertical bar '|' where
            // the committed index is.
            String s = rsource.toString();
            log.append(s.substring(0, index[Transliterator.CURSOR])).
                append('|').
                append(s.substring(index[Transliterator.CURSOR]));
        }
        
        // As a final step in keyboard transliteration, we must call
        // transliterate to finish off any pending partial matches that
        // were waiting for more input.
        t.finishKeyboardTransliteration(rsource, index);
        result = rsource.toString();
        log.append(" => ").append(rsource.toString());

        expectAux(t.getID() + ":Keyboard", log.toString(),
                  result.equals(expectedResult),
                  expectedResult);
    }

    void expectAux(String tag, String source,
                   String result, String expectedResult) {
        expectAux(tag, source + " -> " + result,
                  result.equals(expectedResult),
                  expectedResult);
    }
    
    void expectAux(String tag, String summary, boolean pass,
                   String expectedResult) {
        if (pass) {
            logln("("+tag+") " + escape(summary));
        } else {
            errln("FAIL: ("+tag+") "
                  + escape(summary)
                  + ", expected " + escape(expectedResult));
        }
    }
    
    /**
     * Escape non-ASCII characters as Unicode.
     */
    public static final String escape(String s) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<s.length(); ++i) {
            char c = s.charAt(i);
            if (c >= ' ' && c <= 0x007F) {
                buf.append(c);
            } else {
                buf.append("\\u");
                if (c < 0x1000) {
                    buf.append('0');
                    if (c < 0x100) {
                        buf.append('0');
                        if (c < 0x10) {
                            buf.append('0');
                        }
                    }
                }
                buf.append(Integer.toHexString(c));
            }
        }
        return buf.toString();
    }

    /*
    static final String KANA_RT_DATA =
"a "+

"ba bi bu be bo "+
"bya byi byu bye byo "+
"bba "+

"da di du de do "+
"dya dyi dyu dye dyo "+
"dha dhi dhu dhe dho "+
"dda "+

"e "+

"fa fi fe fo "+
"fya fyu fyo "+
"ffa "+

"ga gi gu ge go "+
"gya gyi gyu gye gyo "+
"gwa gwi gwu gwe gwo "+
"gga "+

"ha hi hu he ho "+
"hya hyi hyu hye hyo "+
"hha "+

"i "+

"ka ki ku ke ko "+
"kwa kwi kwu kwe kwo "+
"kya kyi kyu kye kyo "+
"kka "+

"ma mi mu me mo "+
"mya myi myu mye myo "+
"mba mfa mma mpa mva "+
"m'' "+

"na ni nu ne no "+
"nya nyi nyu nye nyo "+
"nn n'' n "+

"o "+

"pa pi pu pe po "+
"pya pyi pyu pye pyo "+
"ppa "+

"qa qi qu qe qo "+
"qya qyi qyu qye qyo "+
"qqa "+

"ra ri ru re ro "+
"rya ryi ryu rye ryo "+
"rra "+

"sa si su se so "+
"sya syi syu sye syo "+
"ssya ssa "+

"ta ti tu te to "+
"tha thi thu the tho "+
"tsa tsi tse tso "+
"tya tyi tyu tye tyo "+
"ttsa "+
"tta "+

"u "+

"va vi vu ve vo "+
"vya vyi vyu vye vyo "+
"vva "+

"wa wi we wo "+
"wwa "+

"ya yu ye yo "+
"yya "+

"za zi zu ze zo "+
"zya zyi zyu zye zyo "+
"zza "+

"xa xi xu xe xo "+
"xka xke "+
"xtu "+
"xwa "+
"xya xyu xyo "+

        "akka akki akku akke akko "+
        "akkya akkyu akkyo "+

        "atta atti attu atte atto "+
        "attya attyu attyo "+
        "adda addi addu adde addo "+

        "atcha atchi atchu atche atcho "+

        "assa assi assu asse asso "+
        "assya assyu assyo "+

        "ahha ahhi ahhu ahhe ahho "+
        "appa appi appu appe appo "+

        "an "+
        "ana ani anu ane ano "+
        "anna anni annu anne anno "+
        "an'a an'i an'u an'e an'o "+

        "annna annni annnu annne annno "+
        "an'na an'ni an'nu an'ne an'no "+

        "anka anki anku anke anko "+
        "anga angi angu ange ango "+

        "ansa ansi ansu anse anso "+
        "anza anzi anzu anze anzo "+
        "anzya anzyu anzyo "+

        "anta anti antu ante anto "+
        "antya antyu antyo "+
        "anda andi andu ande ando "+

        "ancha anchi anchu anche ancho "+
        "anja anji anju anje anjo "+
        "antsa antsu antso "+

        "anpa anpi anpu anpe anpo "+
        "ampa ampi ampu ampe ampo "+

        "anba anbi anbu anbe anbo "+
        "amba ambi ambu ambe ambo "+

        "anma anmi anmu anme anmo "+
        "amma ammi ammu amme ammo "+

        "anwa anwi anwu anwe anwo "+

        "anha anhi anhu anhe anho "+

        "anya anyi anyu anye anyo "+
        "annya annyi annyu annye annyo "+
        "an'ya an'yi an'yu an'ye an'yo "+

        "kkk "+
        "ggg "+
        "sss "+
        "zzz "+
        "ttt "+
        "ddd "+
        "nnn "+
        "hhh "+
        "bbb "+
        "ppp "+
        "mmm "+
        "yyy "+
        "rrr "+
        "www ";
*/

        /*+

        "A I U E O "+
        "XA XI XU XE XO "+

        "KA KI KU KE KO "+
        "KYA KYI KYU KYE KYO "+
        "KWA KWI KWU KWE KWO "+
        "QA QI QU QE QO "+
        "QYA QYI QYU QYE QYO "+
        "XKA XKE "+

        "GA GI GU GE GO "+
        "GYA GYI GYU GYE GYO "+
        "GWA GWI GWU GWE GWO "+

        "SA SI SU SE SO  "+
        "SHA SHI SHU SHE SHO "+
        "SYA SYI SYU SYE SYO "+

        "ZA ZI ZU ZE ZO "+
        "ZYA ZYI ZYU ZYE ZYO "+
        "JA JI JU JE JO "+
        "JYA JYU JYO "+

        "TA TI TU TE TO "+
        "XTU XTSU "+
        "TYA TYU TYO "+
        "CYA CYU CYO "+
        "CHA CHI CHU CHE CHO "+
        "TSA TSI TSU TSE TSO "+
        "DA DI DU DE DO "+
        "DYA DYU DYO "+
        "THA THI THU THE THO "+
        "DHA DHI DHU DHE DHO "+

        "NA NI NU NE NO "+
        "NYA NYU NYO "+

        "HA HI HU HE HO "+
        "HYA HYU HYO "+
        "FA FI FU FE FO "+
        "FYA FYU FYO "+
        "BA BI BU BE BO "+
        "BYA BYU BYO "+
        "PA PI PU PE PO "+
        "PYA PYU PYO "+

        "MA MI MU ME MO "+
        "MYA MYU MYO "+
        "YA YI YU YE YO "+
        "XYA XYI XYU XYE XYO "+

        "RA RI RU RE RO "+
        "LA LI LU LE LO "+
        "RYA RYI RYU RYE RYO "+
        "LYA LYI LYU LYE LYO "+

        "WA WI WU WE WO "+
        "VA VI VU VE VO "+
        "VYA VYU VYO "+

        "CYA CYI CYU CYE CYO "+

        "NN "+
        "N' "+
        "N "+

        "AKKA AKKI AKKU AKKE AKKO "+
        "AKKYA AKKYU AKKYO "+

        "ATTA ATTI ATTU ATTE ATTO "+
        "ATTYA ATTYU ATTYO "+
        "ADDA ADDI ADDU ADDE ADDO "+

        "ATCHA ATCHI ATCHU ATCHE ATCHO "+

        "ASSA ASSI ASSU ASSE ASSO "+
        "ASSYA ASSYU ASSYO "+

        "AHHA AHHI AHHU AHHE AHHO "+
        "APPA APPI APPU APPE APPO "+

        "AN "+
        "ANA ANI ANU ANE ANO "+
        "ANNA ANNI ANNU ANNE ANNO "+
        "AN'A AN'I AN'U AN'E AN'O "+

        "ANNNA ANNNI ANNNU ANNNE ANNNO "+
        "AN'NA AN'NI AN'NU AN'NE AN'NO "+

        "ANKA ANKI ANKU ANKE ANKO "+
        "ANGA ANGI ANGU ANGE ANGO "+

        "ANSA ANSI ANSU ANSE ANSO "+
        "ANZA ANZI ANZU ANZE ANZO "+
        "ANZYA ANZYU ANZYO "+

        "ANTA ANTI ANTU ANTE ANTO "+
        "ANTYA ANTYU ANTYO "+
        "ANDA ANDI ANDU ANDE ANDO "+

        "ANCHA ANCHI ANCHU ANCHE ANCHO "+
        "ANJA ANJI ANJU ANJE ANJO "+
        "ANTSA ANTSU ANTSO "+

        "ANPA ANPI ANPU ANPE ANPO "+
        "AMPA AMPI AMPU AMPE AMPO "+

        "ANBA ANBI ANBU ANBE ANBO "+
        "AMBA AMBI AMBU AMBE AMBO "+

        "ANMA ANMI ANMU ANME ANMO "+
        "AMMA AMMI AMMU AMME AMMO "+

        "ANWA ANWI ANWU ANWE ANWO "+

        "ANHA ANHI ANHU ANHE ANHO "+

        "ANYA ANYI ANYU ANYE ANYO "+
        "ANNYA ANNYI ANNYU ANNYE ANNYO "+
        "AN'YA AN'YI AN'YU AN'YE AN'YO "+

        "KKK "+
        "GGG "+
        "SSS "+
        "ZZZ "+
        "TTT "+
        "DDD "+
        "NNN "+
        "HHH "+
        "BBB "+
        "PPP "+
        "MMM "+
        "YYY "+
        "RRR "+
        "WWW";*/
}
