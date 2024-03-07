// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
*******************************************************************************
* Copyright (C) 1996-2016, International Business Machines Corporation and
* others. All Rights Reserved.
*******************************************************************************
*/

package com.ibm.icu.dev.test.lang;

import java.util.BitSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.lang.UScript.ScriptUsage;
import com.ibm.icu.text.UnicodeSet;

@RunWith(JUnit4.class)
public class TestUScript extends CoreTestFmwk {

    /**
    * Constructor
    */
    public TestUScript()
    {
    }

    @Test
    public void TestGetScriptOfCharsWithScriptExtensions() {
        /* test characters which have Script_Extensions */
        if(!(
            UScript.COMMON==UScript.getScript(0x0640) &&
            UScript.INHERITED==UScript.getScript(0x0650) &&
            UScript.ARABIC==UScript.getScript(0xfdf2))
        ) {
            errln("UScript.getScript(character with Script_Extensions) failed");
        }
    }

    @Test
    public void TestHasScript() {
        if(!(
            !UScript.hasScript(0x063f, UScript.COMMON) &&
            UScript.hasScript(0x063f, UScript.ARABIC) &&  /* main Script value */
            !UScript.hasScript(0x063f, UScript.SYRIAC) &&
            !UScript.hasScript(0x063f, UScript.THAANA))
        ) {
            errln("UScript.hasScript(U+063F, ...) is wrong");
        }
        if(!(
            !UScript.hasScript(0x0640, UScript.COMMON) &&  /* main Script value */
            UScript.hasScript(0x0640, UScript.ARABIC) &&
            UScript.hasScript(0x0640, UScript.SYRIAC) &&
            !UScript.hasScript(0x0640, UScript.THAANA))
        ) {
            errln("UScript.hasScript(U+0640, ...) is wrong");
        }
        if(!(
            !UScript.hasScript(0x0650, UScript.INHERITED) &&  /* main Script value */
            UScript.hasScript(0x0650, UScript.ARABIC) &&
            UScript.hasScript(0x0650, UScript.SYRIAC) &&
            !UScript.hasScript(0x0650, UScript.THAANA))
        ) {
            errln("UScript.hasScript(U+0650, ...) is wrong");
        }
        if(!(
            !UScript.hasScript(0x0660, UScript.COMMON) &&  /* main Script value */
            UScript.hasScript(0x0660, UScript.ARABIC) &&
            !UScript.hasScript(0x0660, UScript.SYRIAC) &&
            UScript.hasScript(0x0660, UScript.THAANA))
        ) {
            errln("UScript.hasScript(U+0660, ...) is wrong");
        }
        if(!(
            !UScript.hasScript(0xfdf2, UScript.COMMON) &&
            UScript.hasScript(0xfdf2, UScript.ARABIC) &&  /* main Script value */
            !UScript.hasScript(0xfdf2, UScript.SYRIAC) &&
            UScript.hasScript(0xfdf2, UScript.THAANA))
        ) {
            errln("UScript.hasScript(U+FDF2, ...) is wrong");
        }
        if(UScript.hasScript(0x0640, 0xaffe)) {
            // An unguarded implementation might go into an infinite loop.
            errln("UScript.hasScript(U+0640, bogus 0xaffe) is wrong");
        }
    }

    @Test
    public void TestGetScriptExtensions() {
        BitSet scripts=new BitSet(UScript.CODE_LIMIT);

        /* invalid code points */
        if(UScript.getScriptExtensions(-1, scripts)!=UScript.UNKNOWN || scripts.cardinality()!=1 ||
                !scripts.get(UScript.UNKNOWN)) {
            errln("UScript.getScriptExtensions(-1) is not {UNKNOWN}");
        }
        if(UScript.getScriptExtensions(0x110000, scripts)!=UScript.UNKNOWN || scripts.cardinality()!=1 ||
                !scripts.get(UScript.UNKNOWN)) {
            errln("UScript.getScriptExtensions(0x110000) is not {UNKNOWN}");
        }

        /* normal usage */
        if(UScript.getScriptExtensions(0x063f, scripts)!=UScript.ARABIC || scripts.cardinality()!=1 ||
                !scripts.get(UScript.ARABIC)) {
            errln("UScript.getScriptExtensions(U+063F) is not {ARABIC}");
        }
        if(UScript.getScriptExtensions(0x0640, scripts)>-3 || scripts.cardinality()<3 ||
           !scripts.get(UScript.ARABIC) || !scripts.get(UScript.SYRIAC) || !scripts.get(UScript.MANDAIC)
        ) {
            errln("UScript.getScriptExtensions(U+0640) failed");
        }
        if(UScript.getScriptExtensions(0xfdf2, scripts)!=-2 || scripts.cardinality()!=2 ||
                !scripts.get(UScript.ARABIC) || !scripts.get(UScript.THAANA)) {
            errln("UScript.getScriptExtensions(U+FDF2) failed");
        }
        if(UScript.getScriptExtensions(0xff65, scripts)!=-6 || scripts.cardinality()!=6 ||
                !scripts.get(UScript.BOPOMOFO) || !scripts.get(UScript.YI)) {
            errln("UScript.getScriptExtensions(U+FF65) failed");
        }
    }

    @Test
    public void TestDefaultScriptExtensions() {
        // Block 3000..303F CJK Symbols and Punctuation defaults to scx=Bopo Hang Hani Hira Kana Yiii
        // but some of its characters revert to scx=<script> which is usually Common.
        BitSet scx = new BitSet();
        assertEquals("U+3000 num scx",  // IDEOGRAPHIC SPACE
                UScript.COMMON,
                UScript.getScriptExtensions(0x3000, scx));
        scx.clear();
        assertEquals("U+3012 num scx",  // POSTAL MARK
                UScript.COMMON,
                UScript.getScriptExtensions(0x3012, scx));
    }

    @Test
    public void TestScriptMetadataAPI() {
        /* API & code coverage. */
        String sample = UScript.getSampleString(UScript.LATIN);
        if(sample.length()!=1 || UScript.getScript(sample.charAt(0))!=UScript.LATIN) {
            errln("UScript.getSampleString(Latn) failed");
        }
        sample = UScript.getSampleString(UScript.INVALID_CODE);
        if(sample.length()!=0) {
            errln("UScript.getSampleString(invalid) failed");
        }

        if(UScript.getUsage(UScript.LATIN)!=ScriptUsage.RECOMMENDED ||
                // Unicode 10 gives up on "aspirational".
                UScript.getUsage(UScript.YI)!=ScriptUsage.LIMITED_USE ||
                UScript.getUsage(UScript.CHEROKEE)!=ScriptUsage.LIMITED_USE ||
                UScript.getUsage(UScript.COPTIC)!=ScriptUsage.EXCLUDED ||
                UScript.getUsage(UScript.CIRTH)!=ScriptUsage.NOT_ENCODED ||
                UScript.getUsage(UScript.INVALID_CODE)!=ScriptUsage.NOT_ENCODED ||
                UScript.getUsage(UScript.CODE_LIMIT)!=ScriptUsage.NOT_ENCODED) {
            errln("UScript.getUsage() failed");
        }

        if(UScript.isRightToLeft(UScript.LATIN) ||
                UScript.isRightToLeft(UScript.CIRTH) ||
                !UScript.isRightToLeft(UScript.ARABIC) ||
                !UScript.isRightToLeft(UScript.HEBREW)) {
            errln("UScript.isRightToLeft() failed");
        }

        if(UScript.breaksBetweenLetters(UScript.LATIN) ||
                UScript.breaksBetweenLetters(UScript.CIRTH) ||
                !UScript.breaksBetweenLetters(UScript.HAN) ||
                !UScript.breaksBetweenLetters(UScript.THAI)) {
            errln("UScript.breaksBetweenLetters() failed");
        }

        if(UScript.isCased(UScript.CIRTH) ||
                UScript.isCased(UScript.HAN) ||
                !UScript.isCased(UScript.LATIN) ||
                !UScript.isCased(UScript.GREEK)) {
            errln("UScript.isCased() failed");
        }
    }

    /**
     * Maps a special script code to the most common script of its encoded characters.
     */
    private static final int getCharScript(int script) {
        switch(script) {
        case UScript.HAN_WITH_BOPOMOFO:
        case UScript.SIMPLIFIED_HAN:
        case UScript.TRADITIONAL_HAN:
            return UScript.HAN;
        case UScript.JAPANESE:
            return UScript.HIRAGANA;
        case UScript.JAMO:
        case UScript.KOREAN:
            return UScript.HANGUL;
        case UScript.SYMBOLS_EMOJI:
            return UScript.SYMBOLS;
        default:
            return script;
        }
    }

    @Test
    public void TestScriptMetadata() {
        UnicodeSet rtl = new UnicodeSet("[[:bc=R:][:bc=AL:]-[:Cn:]-[:sc=Common:]]");
        // So far, sample characters are uppercase.
        // Georgian is special.
        UnicodeSet cased = new UnicodeSet("[[:Lu:]-[:sc=Common:]-[:sc=Geor:]]");
        for(int sc = 0; sc < UScript.CODE_LIMIT; ++sc) {
            String sn = UScript.getShortName(sc);
            ScriptUsage usage = UScript.getUsage(sc);
            String sample = UScript.getSampleString(sc);
            UnicodeSet scriptSet = new UnicodeSet();
            scriptSet.applyIntPropertyValue(UProperty.SCRIPT, sc);
            if(usage == ScriptUsage.NOT_ENCODED) {
                assertTrue(sn + " not encoded, no sample", sample.isEmpty());
                assertFalse(sn + " not encoded, not RTL", UScript.isRightToLeft(sc));
                assertFalse(sn + " not encoded, not LB letters", UScript.breaksBetweenLetters(sc));
                assertFalse(sn + " not encoded, not cased", UScript.isCased(sc));
                assertTrue(sn + " not encoded, no characters", scriptSet.isEmpty());
            } else {
                assertFalse(sn + " encoded, has a sample character", sample.isEmpty());
                int firstChar = sample.codePointAt(0);
                int charScript = getCharScript(sc);
                assertEquals(sn + " script(sample(script))",
                             charScript, UScript.getScript(firstChar));
                assertEquals(sn + " RTL vs. set", rtl.contains(firstChar), UScript.isRightToLeft(sc));
                assertEquals(sn + " cased vs. set", cased.contains(firstChar), UScript.isCased(sc));
                assertEquals(sn + " encoded, has characters", sc == charScript, !scriptSet.isEmpty());
                if(UScript.isRightToLeft(sc)) {
                    rtl.removeAll(scriptSet);
                }
                if(UScript.isCased(sc)) {
                    cased.removeAll(scriptSet);
                }
            }
        }
        assertEquals("no remaining RTL characters", "[]", rtl.toPattern(true));
        assertEquals("no remaining cased characters", "[]", cased.toPattern(true));

        assertTrue("Hani breaks between letters", UScript.breaksBetweenLetters(UScript.HAN));
        assertTrue("Thai breaks between letters", UScript.breaksBetweenLetters(UScript.THAI));
        assertFalse("Latn does not break between letters", UScript.breaksBetweenLetters(UScript.LATIN));
    }

    @Test
    public void TestScriptNames(){
        for(int i=0; i<UScript.CODE_LIMIT;i++){
            String name = UScript.getName(i);
            if(name.equals("") ){
                errln("FAILED: getName for code : "+i);
            }
            String shortName= UScript.getShortName(i);
            if(shortName.equals("")){
                errln("FAILED: getName for code : "+i);
            }
        }
    }
    @Test
    public void TestAllCodepoints(){
        int code;
        //String oldId="";
        //String oldAbbrId="";
        for( int i =0; i <= 0x10ffff; i++){
          code =UScript.INVALID_CODE;
          code = UScript.getScript(i);
          if(code==UScript.INVALID_CODE){
                errln("UScript.getScript for codepoint 0x"+ hex(i)+" failed");
          }
          String id =UScript.getName(code);
          if(id.indexOf("INVALID")>=0){
                 errln("UScript.getScript for codepoint 0x"+ hex(i)+" failed");
          }
          String abbr = UScript.getShortName(code);
          if(abbr.indexOf("INV")>=0){
                 errln("UScript.getScript for codepoint 0x"+ hex(i)+" failed");
          }
        }
    }
    @Test
    public void TestNewCode(){
        /*
         * These script codes were originally added to ICU pre-3.6, so that ICU would
         * have all ISO 15924 script codes. ICU was then based on Unicode 4.1.
         * These script codes were added with only short names because we don't
         * want to invent long names ourselves.
         * Unicode 5 and later encode some of these scripts and give them long names.
         * Whenever this happens, the long script names here need to be updated.
         */
        String[] expectedLong = new String[]{
            "Balinese", "Batak", "Blis", "Brahmi", "Cham", "Cirt", "Cyrs",
            "Egyd", "Egyh", "Egyptian_Hieroglyphs",
            "Geok", "Hans", "Hant", "Pahawh_Hmong", "Old_Hungarian", "Inds",
            "Javanese", "Kayah_Li", "Latf", "Latg",
            "Lepcha", "Linear_A", "Mandaic", "Maya", "Meroitic_Hieroglyphs",
            "Nko", "Old_Turkic", "Old_Permic", "Phags_Pa", "Phoenician",
            "Miao", "Roro", "Sara", "Syre", "Syrj", "Syrn", "Teng", "Vai", "Visp", "Cuneiform",
            "Zxxx", "Unknown",
            "Carian", "Jpan", "Tai_Tham", "Lycian", "Lydian", "Ol_Chiki", "Rejang", "Saurashtra", "SignWriting", "Sundanese",
            "Moon", "Meetei_Mayek",
            /* new in ICU 4.0 */
            "Imperial_Aramaic", "Avestan", "Chakma", "Kore",
            "Kaithi", "Manichaean", "Inscriptional_Pahlavi", "Psalter_Pahlavi", "Phlv",
            "Inscriptional_Parthian", "Samaritan", "Tai_Viet",
            "Zmth", "Zsym",
            /* new in ICU 4.4 */
            "Bamum", "Lisu", "Nkgb", "Old_South_Arabian",
            /* new in ICU 4.6 */
            "Bassa_Vah", "Duployan", "Elbasan", "Grantha", "Kpel",
            "Loma", "Mende_Kikakui", "Meroitic_Cursive",
            "Old_North_Arabian", "Nabataean", "Palmyrene", "Khudawadi", "Warang_Citi",
            /* new in ICU 4.8 */
            "Afak", "Jurc", "Mro", "Nushu", "Sharada", "Sora_Sompeng", "Takri", "Tangut", "Wole",
            /* new in ICU 49 */
            "Anatolian_Hieroglyphs", "Khojki", "Tirhuta",
            /* new in ICU 52 */
            "Caucasian_Albanian", "Mahajani",
            /* new in ICU 54 */
            "Ahom", "Hatran", "Modi", "Multani", "Pau_Cin_Hau", "Siddham",
            // new in ICU 58
            "Adlam", "Bhaiksuki", "Marchen", "Newa", "Osage", "Hanb", "Jamo", "Zsye",
            // new in ICU 60
            "Masaram_Gondi", "Soyombo", "Zanabazar_Square",
            // new in ICU 61
            "Dogra", "Gunjala_Gondi", "Makasar", "Medefaidrin",
            "Hanifi_Rohingya", "Sogdian", "Old_Sogdian",
            // new in ICU 64
            "Elymaic", "Nyiakeng_Puachue_Hmong", "Nandinagari", "Wancho",
            // new in ICU 66
            "Chorasmian", "Dives_Akuru", "Khitan_Small_Script", "Yezidi",
            // new in ICU 70
            "Cypro_Minoan", "Old_Uyghur", "Tangsa", "Toto", "Vithkuqi",
            // new in ICU 72
            "Kawi", "Nag_Mundari",
            // new in ICU 75
            "Aran",
        };
        String[] expectedShort = new String[]{
            "Bali", "Batk", "Blis", "Brah", "Cham", "Cirt", "Cyrs", "Egyd", "Egyh", "Egyp",
            "Geok", "Hans", "Hant", "Hmng", "Hung", "Inds", "Java", "Kali", "Latf", "Latg",
            "Lepc", "Lina", "Mand", "Maya", "Mero", "Nkoo", "Orkh", "Perm", "Phag", "Phnx",
            "Plrd", "Roro", "Sara", "Syre", "Syrj", "Syrn", "Teng", "Vaii", "Visp", "Xsux",
            "Zxxx", "Zzzz",
            "Cari", "Jpan", "Lana", "Lyci", "Lydi", "Olck", "Rjng", "Saur", "Sgnw", "Sund",
            "Moon", "Mtei",
            /* new in ICU 4.0 */
            "Armi", "Avst", "Cakm", "Kore",
            "Kthi", "Mani", "Phli", "Phlp", "Phlv", "Prti", "Samr", "Tavt",
            "Zmth", "Zsym",
            /* new in ICU 4.4 */
            "Bamu", "Lisu", "Nkgb", "Sarb",
            /* new in ICU 4.6 */
            "Bass", "Dupl", "Elba", "Gran", "Kpel", "Loma", "Mend", "Merc",
            "Narb", "Nbat", "Palm", "Sind", "Wara",
            /* new in ICU 4.8 */
            "Afak", "Jurc", "Mroo", "Nshu", "Shrd", "Sora", "Takr", "Tang", "Wole",
            /* new in ICU 49 */
            "Hluw", "Khoj", "Tirh",
            /* new in ICU 52 */
            "Aghb", "Mahj",
            /* new in ICU 54 */
            "Ahom", "Hatr", "Modi", "Mult", "Pauc", "Sidd",
            // new in ICU 58
            "Adlm", "Bhks", "Marc", "Newa", "Osge", "Hanb", "Jamo", "Zsye",
            // new in ICU 60
            "Gonm", "Soyo", "Zanb",
            // new in ICU 61
            "Dogr", "Gong", "Maka", "Medf", "Rohg", "Sogd", "Sogo",
            // new in ICU 64
            "Elym", "Hmnp", "Nand", "Wcho",
            // new in ICU 66
            "Chrs", "Diak", "Kits", "Yezi",
            // new in ICU 70
            "Cpmn", "Ougr", "Tnsa", "Toto", "Vith",
            // new in ICU 72
            "Kawi", "Nagm",
            // new in ICU 75
            "Aran",
        };
        if(expectedLong.length!=(UScript.CODE_LIMIT-UScript.BALINESE)) {
            errln("need to add new script codes in lang.TestUScript.java!");
            return;
        }
        int j = 0;
        int i = 0;
        for(i=UScript.BALINESE; i<UScript.CODE_LIMIT; i++, j++){
            String name = UScript.getName(i);
            if(name==null || !name.equals(expectedLong[j])){
                errln("UScript.getName failed for code"+ i + name +"!=" +expectedLong[j]);
            }
            name = UScript.getShortName(i);
            if(name==null || !name.equals(expectedShort[j])){
                errln("UScript.getShortName failed for code"+ i + name +"!=" +expectedShort[j]);
            }
        }
        for(i=0; i<expectedLong.length; i++){
            int[] ret = UScript.getCode(expectedShort[i]);
            if(ret.length>1){
                errln("UScript.getCode did not return expected number of codes for script"+ expectedShort[i]+". EXPECTED: 1 GOT: "+ ret.length);
            }
            if(ret[0]!= (UScript.BALINESE+i)){
                errln("UScript.getCode did not return expected code for script"+ expectedShort[i]+". EXPECTED: "+ (UScript.BALINESE+i)+" GOT: %i\n"+ ret[0] );
            }
        }
    }
}
