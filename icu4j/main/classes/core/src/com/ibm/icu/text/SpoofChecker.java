/*
 ***************************************************************************
 * Copyright (C) 2008-2015 International Business Machines Corporation
 * and others. All Rights Reserved.
 ***************************************************************************
 *
 * Unicode Spoof Detection
 */

package com.ibm.icu.text;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.impl.ICUBinary;
import com.ibm.icu.impl.ICUBinary.Authenticate;
import com.ibm.icu.impl.Trie2;
import com.ibm.icu.impl.Trie2Writable;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.util.ULocale;

/**
 *
 * <b>Unicode Security and Spoofing Detection.</b>
 *
 * <p>This class is intended to check strings, typically
 * identifiers of some type, such as URLs, for the presence of
 * characters that are likely to be visually confusing -
 * for cases where the displayed form of an identifier may
 * not be what it appears to be.
 *
 * <p>Unicode Technical Report #36,
 * <a href="http://unicode.org/reports/tr36">http://unicode.org/reports/tr36</a> and
 * Unicode Technical Standard #39,
 * <a href="http://unicode.org/reports/tr39">http://unicode.org/reports/tr39</a>
 * "Unicode security considerations", give more background on
 * security and spoofing issues with Unicode identifiers.
 * The tests and checks provided by this module implement the recommendations
 * from these Unicode documents.
 *
 * <p>The tests available on identifiers fall into two general categories:
 *   <ul>
 *     <li>  Single identifier tests.  Check whether an identifier is
 *       potentially confusable with any other string, or is suspicious
 *       for other reasons. </li>
 *     <li> Two identifier tests.  Check whether two specific identifiers are confusable.
 *       This does not consider whether either of strings is potentially
 *       confusable with any string other than the exact one specified. </li>
 *   </ul>
 *
 * <p>The steps to perform confusability testing are
 *   <ul>
 *     <li>  Create a <code>SpoofChecker.Builder</code> </li>
 *     <li>  Configure the Builder for the desired set of tests.  The tests that will
 *           be performed are specified by a set of SpoofCheck flags. </li>
 *     <li>  Build a <code>SpoofChecker</code> from the Builder. </li>
 *     <li>  Perform the checks using the pre-configured <code>SpoofChecker</code>.  The results indicate
 *           which (if any) of the selected tests have identified possible problems with the identifier.
 *           Results are reported as a set of SpoofCheck flags;  this mirrors the form in which
 *           the set of tests to perform was originally specified to the SpoofChecker. </li>
 *    </ul>
 *
 * <p>A <code>SpoofChecker</code> instance may be used repeatedly to perform checks on any number
 *    of identifiers.
 *
 * <p>Thread Safety: The methods on SpoofChecker objects are thread safe.
 * The test functions for checking a single identifier, or for testing
 * whether two identifiers are potentially confusable,  may called concurrently
 * from multiple threads using the same SpoofChecker instance.
 *
 *
 * <p>Descriptions of the available checks.
 *
 * <p>When testing whether pairs of identifiers are confusable, with <code>areConfusable()</code>
 * the relevant tests are
 *
 *  <ul>
 *   <li> <code>SINGLE_SCRIPT_CONFUSABLE</code>:  All of the characters from the two identifiers are
 *      from a single script, and the two identifiers are visually confusable.</li>
 *   <li> <code>MIXED_SCRIPT_CONFUSABLE</code>:  At least one of the identifiers contains characters
 *      from more than one script, and the two identifiers are visually confusable.</li>
 *   <li> <code>WHOLE_SCRIPT_CONFUSABLE</code>: Each of the two identifiers is of a single script, but
 *      the the two identifiers are from different scripts, and they are visually confusable.</li>
 *  </ul>
 *
 * <p>The safest approach is to enable all three of these checks as a group.
 *
 * <p><code>ANY_CASE</code> is a modifier for the above tests.  If the identifiers being checked can
 * be of mixed case and are used in a case-sensitive manner, this option should be specified.
 *
 * <p>If the identifiers being checked are used in a case-insensitive manner, and if they are
 * displayed to users in lower-case form only, the <code>ANY_CASE</code> option should not be
 * specified.  Confusabality issues involving upper case letters will not be reported.
 *
 * <p>When performing tests on a single identifier, with the check() family of functions,
 * the relevant tests are:
 *
 *  <ul>
 *    <li><code>MIXED_SCRIPT_CONFUSABLE</code>: the identifier contains characters from multiple
 *       scripts, and there exists an identifier of a single script that is visually confusable.</li>
 *    <li><code>WHOLE_SCRIPT_CONFUSABLE</code>: the identifier consists of characters from a single
 *       script, and there exists a visually confusable identifier.
 *       The visually confusable identifier also consists of characters from a single script.
 *       but not the same script as the identifier being checked.</li>
 *    <li><code>ANY_CASE</code>: modifies the mixed script and whole script confusables tests.  If
 *       specified, the checks will find confusable characters of any case.
 *       If this flag is not set, the test is performed assuming case folded identifiers.</li>
 *    <li><code>SINGLE_SCRIPT</code>: check that the identifier contains only characters from a
 *       single script.  (Characters from the <em>common</em> and <em>inherited</em> scripts are ignored.)
 *       This is not a test for confusable identifiers</li>
 *    <li><code>INVISIBLE</code>: check an identifier for the presence of invisible characters,
 *       such as zero-width spaces, or character sequences that are
 *       likely not to display, such as multiple occurrences of the same
 *       non-spacing mark.  This check does not test the input string as a whole
 *       for conformance to any particular syntax for identifiers.</li>
 *    <li><code>CHAR_LIMIT</code>: check that an identifier contains only characters from a specified set
 *       of acceptable characters.  See <code>Builder.setAllowedChars()</code> and
 *       <code>Builder.setAllowedLocales()</code>.</li>
 *  </ul>
 *
 *  <p>Note on Scripts:
 *     <blockquote>Characters from the Unicode Scripts "Common" and "Inherited" are ignored when considering
 *     the script of an identifier. Common characters include digits and symbols that
 *     are normally used with text from many different scripts. </blockquote>
 *
 * @stable ICU 4.6
 */
public class SpoofChecker {

    /**
     * Constants from UAX 31 for use in setRestrictionLevel.
     * @stable ICU 53
     */
    public enum RestrictionLevel {
        /**
         * Only ASCII characters: U+0000..U+007F
         *
         * @stable ICU 53
         */
        ASCII,
        /**
         * All characters in each identifier must be from a single script.
         * 
         * @stable ICU 53
         */
        SINGLE_SCRIPT_RESTRICTIVE,
         /**
         * All characters in each identifier must be from a single script, or from the combinations: Latin + Han +
         * Hiragana + Katakana; Latin + Han + Bopomofo; or Latin + Han + Hangul. Note that this level will satisfy the
         * vast majority of Latin-script users; also that TR36 has ASCII instead of Latin.
         *
         * @stable ICU 53
         */
        HIGHLY_RESTRICTIVE,
        /**
         * Allow Latin with other scripts except Cyrillic, Greek, Cherokee Otherwise, the same as Highly Restrictive
         *
         * @stable ICU 53
         */
        MODERATELY_RESTRICTIVE,
        /**
         * Allow arbitrary mixtures of scripts, such as Ωmega, Teχ, HλLF-LIFE, Toys-Я-Us. Otherwise, the same as
         * Moderately Restrictive
         *
         * @stable ICU 53
         */
        MINIMALLY_RESTRICTIVE,
        /**
         * Any valid identifiers, including characters outside of the Identifier Profile, such as I♥NY.org
         *
         * @stable ICU 53
         */
        UNRESTRICTIVE
    }


    /**
     * Security Profile constant from UAX 31 for use in setAllowedChars.
     * Will probably be replaced by UnicodeSet property.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static final UnicodeSet INCLUSION = new UnicodeSet("[" +
            "\\u0027\\u002D-\\u002E\\u003A\\u00B7\\u0375\\u058A\\u05F3-\\u05F4"+
            "\\u06FD-\\u06FE\\u0F0B\\u200C-\\u200D\\u2010\\u2019\\u2027\\u30A0\\u30FB]").freeze();
        // Note: data from http://unicode.org/Public/security/latest/xidmodifications.txt version 6.3.0

    /**
     * Security Profile constant from UAX 31 for use in setAllowedChars.
     * Will probably be replaced by UnicodeSet property.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static final UnicodeSet RECOMMENDED = new UnicodeSet(
            "[\\u0030-\\u0039\\u0041-\\u005A\\u005F\\u0061-\\u007A\\u00C0-\\u00D6\\u00D8-\\u00F6" +
            "\\u00F8-\\u0131\\u0134-\\u013E\\u0141-\\u0148\\u014A-\\u017E\\u01A0-\\u01A1" +
            "\\u01AF-\\u01B0\\u01CD-\\u01DC\\u01DE-\\u01E3\\u01E6-\\u01F0\\u01F4-\\u01F5" +
            "\\u01F8-\\u021B\\u021E-\\u021F\\u0226-\\u0233\\u0259\\u02BB-\\u02BC\\u02EC" +
            "\\u0300-\\u0304\\u0306-\\u030C\\u030F-\\u0311\\u0313-\\u0314\\u031B\\u0323-\\u0328" +
            "\\u032D-\\u032E\\u0330-\\u0331\\u0335\\u0338-\\u0339\\u0342\\u0345\\u037B-\\u037D" +
            "\\u0386\\u0388-\\u038A\\u038C\\u038E-\\u03A1\\u03A3-\\u03CE\\u03FC-\\u045F" +
            "\\u048A-\\u0529\\u052E-\\u052F\\u0531-\\u0556\\u0559\\u0561-\\u0586\\u05B4" +
            "\\u05D0-\\u05EA\\u05F0-\\u05F2\\u0620-\\u063F\\u0641-\\u0655\\u0660-\\u0669" +
            "\\u0670-\\u0672\\u0674\\u0679-\\u068D\\u068F-\\u06D3\\u06D5\\u06E5-\\u06E6" +
            "\\u06EE-\\u06FC\\u06FF\\u0750-\\u07B1\\u08A0-\\u08AC\\u08B2\\u0901-\\u094D" +
            "\\u094F-\\u0950\\u0956-\\u0957\\u0960-\\u0963\\u0966-\\u096F\\u0971-\\u0977" +
            "\\u0979-\\u097F\\u0981-\\u0983\\u0985-\\u098C\\u098F-\\u0990\\u0993-\\u09A8" +
            "\\u09AA-\\u09B0\\u09B2\\u09B6-\\u09B9\\u09BC-\\u09C4\\u09C7-\\u09C8\\u09CB-\\u09CE" +
            "\\u09D7\\u09E0-\\u09E3\\u09E6-\\u09F1\\u0A01-\\u0A03\\u0A05-\\u0A0A\\u0A0F-\\u0A10" +
            "\\u0A13-\\u0A28\\u0A2A-\\u0A30\\u0A32\\u0A35\\u0A38-\\u0A39\\u0A3C\\u0A3E-\\u0A42" +
            "\\u0A47-\\u0A48\\u0A4B-\\u0A4D\\u0A5C\\u0A66-\\u0A74\\u0A81-\\u0A83\\u0A85-\\u0A8D" +
            "\\u0A8F-\\u0A91\\u0A93-\\u0AA8\\u0AAA-\\u0AB0\\u0AB2-\\u0AB3\\u0AB5-\\u0AB9" +
            "\\u0ABC-\\u0AC5\\u0AC7-\\u0AC9\\u0ACB-\\u0ACD\\u0AD0\\u0AE0-\\u0AE3\\u0AE6-\\u0AEF" +
            "\\u0B01-\\u0B03\\u0B05-\\u0B0C\\u0B0F-\\u0B10\\u0B13-\\u0B28\\u0B2A-\\u0B30" +
            "\\u0B32-\\u0B33\\u0B35-\\u0B39\\u0B3C-\\u0B43\\u0B47-\\u0B48\\u0B4B-\\u0B4D" +
            "\\u0B56-\\u0B57\\u0B5F-\\u0B61\\u0B66-\\u0B6F\\u0B71\\u0B82-\\u0B83\\u0B85-\\u0B8A" +
            "\\u0B8E-\\u0B90\\u0B92-\\u0B95\\u0B99-\\u0B9A\\u0B9C\\u0B9E-\\u0B9F\\u0BA3-\\u0BA4" +
            "\\u0BA8-\\u0BAA\\u0BAE-\\u0BB9\\u0BBE-\\u0BC2\\u0BC6-\\u0BC8\\u0BCA-\\u0BCD" +
            "\\u0BD0\\u0BD7\\u0BE6-\\u0BEF\\u0C01-\\u0C03\\u0C05-\\u0C0C\\u0C0E-\\u0C10" +
            "\\u0C12-\\u0C28\\u0C2A-\\u0C33\\u0C35-\\u0C39\\u0C3D-\\u0C44\\u0C46-\\u0C48" +
            "\\u0C4A-\\u0C4D\\u0C55-\\u0C56\\u0C60-\\u0C61\\u0C66-\\u0C6F\\u0C82-\\u0C83" +
            "\\u0C85-\\u0C8C\\u0C8E-\\u0C90\\u0C92-\\u0CA8\\u0CAA-\\u0CB3\\u0CB5-\\u0CB9" +
            "\\u0CBC-\\u0CC4\\u0CC6-\\u0CC8\\u0CCA-\\u0CCD\\u0CD5-\\u0CD6\\u0CE0-\\u0CE3" +
            "\\u0CE6-\\u0CEF\\u0CF1-\\u0CF2\\u0D02-\\u0D03\\u0D05-\\u0D0C\\u0D0E-\\u0D10" +
            "\\u0D12-\\u0D3A\\u0D3D-\\u0D43\\u0D46-\\u0D48\\u0D4A-\\u0D4E\\u0D57\\u0D60-\\u0D61" +
            "\\u0D66-\\u0D6F\\u0D7A-\\u0D7F\\u0D82-\\u0D83\\u0D85-\\u0D8E\\u0D91-\\u0D96" +
            "\\u0D9A-\\u0DA5\\u0DA7-\\u0DB1\\u0DB3-\\u0DBB\\u0DBD\\u0DC0-\\u0DC6\\u0DCA" +
            "\\u0DCF-\\u0DD4\\u0DD6\\u0DD8-\\u0DDE\\u0DF2\\u0E01-\\u0E32\\u0E34-\\u0E3A" +
            "\\u0E40-\\u0E4E\\u0E50-\\u0E59\\u0E81-\\u0E82\\u0E84\\u0E87-\\u0E88\\u0E8A" +
            "\\u0E8D\\u0E94-\\u0E97\\u0E99-\\u0E9F\\u0EA1-\\u0EA3\\u0EA5\\u0EA7\\u0EAA-\\u0EAB" +
            "\\u0EAD-\\u0EB2\\u0EB4-\\u0EB9\\u0EBB-\\u0EBD\\u0EC0-\\u0EC4\\u0EC6\\u0EC8-\\u0ECD" +
            "\\u0ED0-\\u0ED9\\u0EDE-\\u0EDF\\u0F00\\u0F20-\\u0F29\\u0F35\\u0F37\\u0F3E-\\u0F42" +
            "\\u0F44-\\u0F47\\u0F49-\\u0F4C\\u0F4E-\\u0F51\\u0F53-\\u0F56\\u0F58-\\u0F5B" +
            "\\u0F5D-\\u0F68\\u0F6A-\\u0F6C\\u0F71-\\u0F72\\u0F74\\u0F7A-\\u0F80\\u0F82-\\u0F84" +
            "\\u0F86-\\u0F92\\u0F94-\\u0F97\\u0F99-\\u0F9C\\u0F9E-\\u0FA1\\u0FA3-\\u0FA6" +
            "\\u0FA8-\\u0FAB\\u0FAD-\\u0FB8\\u0FBA-\\u0FBC\\u0FC6\\u1000-\\u1049\\u1050-\\u109D" +
            "\\u10C7\\u10CD\\u10D0-\\u10F0\\u10F7-\\u10FA\\u10FD-\\u10FF\\u1200-\\u1248" +
            "\\u124A-\\u124D\\u1250-\\u1256\\u1258\\u125A-\\u125D\\u1260-\\u1288\\u128A-\\u128D" +
            "\\u1290-\\u12B0\\u12B2-\\u12B5\\u12B8-\\u12BE\\u12C0\\u12C2-\\u12C5\\u12C8-\\u12D6" +
            "\\u12D8-\\u1310\\u1312-\\u1315\\u1318-\\u135A\\u135D-\\u135F\\u1380-\\u138F" +
            "\\u1780-\\u17A2\\u17A5-\\u17A7\\u17A9-\\u17B3\\u17B6-\\u17CA\\u17D2\\u17D7" +
            "\\u17DC\\u17E0-\\u17E9\\u1E00-\\u1E99\\u1EBF\\u1F00-\\u1F15\\u1F18-\\u1F1D" +
            "\\u1F20-\\u1F45\\u1F48-\\u1F4D\\u1F50-\\u1F57\\u1F59\\u1F5B\\u1F5D\\u1F5F-\\u1F70" +
            "\\u1F72\\u1F74\\u1F76\\u1F78\\u1F7A\\u1F7C\\u1F80-\\u1FB4\\u1FB6-\\u1FBA" +
            "\\u1FBC\\u1FC2-\\u1FC4\\u1FC6-\\u1FC8\\u1FCA\\u1FCC\\u1FD0-\\u1FD2\\u1FD6-\\u1FDA" +
            "\\u1FE0-\\u1FE2\\u1FE4-\\u1FEA\\u1FEC\\u1FF2-\\u1FF4\\u1FF6-\\u1FF8\\u1FFA" +
            "\\u1FFC\\u2D27\\u2D2D\\u2D80-\\u2D96\\u2DA0-\\u2DA6\\u2DA8-\\u2DAE\\u2DB0-\\u2DB6" +
            "\\u2DB8-\\u2DBE\\u2DC0-\\u2DC6\\u2DC8-\\u2DCE\\u2DD0-\\u2DD6\\u2DD8-\\u2DDE" +
            "\\u3005-\\u3007\\u3041-\\u3096\\u3099-\\u309A\\u309D-\\u309E\\u30A1-\\u30FA" +
            "\\u30FC-\\u30FE\\u3105-\\u312D\\u31A0-\\u31BA\\u3400-\\u4DB5\\u4E00-\\u9FCC" +
            "\\uA660-\\uA661\\uA674-\\uA67B\\uA67F\\uA69F\\uA717-\\uA71F\\uA788\\uA78D-\\uA78E" +
            "\\uA790-\\uA793\\uA7A0-\\uA7AA\\uA7FA\\uA9E7-\\uA9FE\\uAA60-\\uAA76\\uAA7A-\\uAA7F" +
            "\\uAB01-\\uAB06\\uAB09-\\uAB0E\\uAB11-\\uAB16\\uAB20-\\uAB26\\uAB28-\\uAB2E" +
            "\\uAC00-\\uD7A3\\uFA0E-\\uFA0F\\uFA11\\uFA13-\\uFA14\\uFA1F\\uFA21\\uFA23-\\uFA24" +
            "\\uFA27-\\uFA29\\U0001B000-\\U0001B001\\U00020000-\\U0002A6D6\\U0002A700-\\U0002B734" +
            "\\U0002B740-\\U0002B81D]").freeze();
            // Note: data from http://unicode.org/Public/security/latest/xidmodifications.txt version 7.0.0

    /**
     * Constants for the kinds of checks that USpoofChecker can perform. These values are used both to select the set of
     * checks that will be performed, and to report results from the check function.
     *
     */

    /**
     * Single script confusable test. When testing whether two identifiers are confusable, report that they are if both
     * are from the same script and they are visually confusable. Note: this test is not applicable to a check of a
     * single identifier.
     *
     * @stable ICU 4.6
     */
    public static final int SINGLE_SCRIPT_CONFUSABLE = 1;

    /**
     * Mixed script confusable test.
     *
     * When checking a single identifier, report a problem if the identifier contains multiple scripts, and is also
     * confusable with some other identifier in a single script.
     *
     * When testing whether two identifiers are confusable, report that they are if the two IDs are visually confusable,
     * and and at least one contains characters from more than one script.
     *
     * @stable ICU 4.6
     */
    public static final int MIXED_SCRIPT_CONFUSABLE = 2;

    /**
     * Whole script confusable test.
     *
     * When checking a single identifier, report a problem if The identifier is of a single script, and there exists a
     * confusable identifier in another script.
     *
     * When testing whether two Identifiers are confusable, report that they are if each is of a single script, the
     * scripts of the two identifiers are different, and the identifiers are visually confusable.
     *
     * @stable ICU 4.6
     */
    public static final int WHOLE_SCRIPT_CONFUSABLE = 4;

    /**
     * Any Case Modifier for confusable identifier tests.
     *
     * When specified, consider all characters, of any case, when looking for confusables. If ANY_CASE is not specified,
     * identifiers being checked are assumed to have been case folded, and upper case conusable characters will not be
     * checked.
     *
     * @stable ICU 4.6
     */
    public static final int ANY_CASE = 8;

    /**
     * Check that an identifier is no looser than the specified RestrictionLevel.
     * The default if this is not called is HIGHLY_RESTRICTIVE.
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static final int RESTRICTION_LEVEL = 16;

    /**
     * Check that an identifer contains only characters from a single script (plus chars from the common and inherited
     * scripts.) Applies to checks of a single identifier check only.
     *
     * @deprecated ICU 51 Use RESTRICTION_LEVEL
     */
    @Deprecated
    public static final int SINGLE_SCRIPT = RESTRICTION_LEVEL;

    /**
     * Check an identifier for the presence of invisible characters, such as zero-width spaces, or character sequences
     * that are likely not to display, such as multiple occurrences of the same non-spacing mark. This check does not
     * test the input string as a whole for conformance to any particular syntax for identifiers.
     *
     * @stable ICU 4.6
     */
    public static final int INVISIBLE = 32;

    /**
     * Check that an identifier contains only characters from a specified set of acceptable characters. See
     * Builder.setAllowedChars() and Builder.setAllowedLocales().
     *
     * @stable ICU 4.6
     */
    public static final int CHAR_LIMIT = 64;

    /**
     * Check that an identifier does not mix numbers.
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static final int MIXED_NUMBERS = 128;

    /**
     * Enable all spoof checks.
     *
     * @stable ICU 4.6
     */
    public static final int ALL_CHECKS = 0xFFFFFFFF;


    // Magic number for sanity checking spoof binary resource data.
    static final int MAGIC = 0x3845fdef;

    /**
     * private constructor: a SpoofChecker has to be built by the builder
     */
    private SpoofChecker() {
    }

    /**
     * SpoofChecker Builder. To create a SpoofChecker, first instantiate a SpoofChecker.Builder, set the desired
     * checking options on the builder, then call the build() function to create a SpoofChecker instance.
     *
     * @stable ICU 4.6
     */
    public static class Builder {
        int fChecks; // Bit vector of checks to perform.
        SpoofData fSpoofData;
        final UnicodeSet fAllowedCharsSet = new UnicodeSet(0, 0x10ffff); // The UnicodeSet of allowed characters.
        // for this Spoof Checker. Defaults to all chars.
        final Set<ULocale> fAllowedLocales = new LinkedHashSet<ULocale>(); // The list of allowed locales.
        private RestrictionLevel fRestrictionLevel;

        /**
         * Constructor: Create a default Unicode Spoof Checker Builder, configured to perform all checks except for
         * LOCALE_LIMIT and CHAR_LIMIT. Note that additional checks may be added in the future, resulting in the changes
         * to the default checking behavior.
         *
         * @stable ICU 4.6
         */
        public Builder() {
            fChecks = ALL_CHECKS;
            fSpoofData = null;
            fRestrictionLevel = RestrictionLevel.HIGHLY_RESTRICTIVE;
        }

        /**
         * Constructor: Create a Spoof Checker Builder, and set the configuration from an existing SpoofChecker.
         *
         * @param src
         *            The existing checker.
         * @stable ICU 4.6
         */
        public Builder(SpoofChecker src) {
            fChecks = src.fChecks;
            fSpoofData = src.fSpoofData;      // For the data, we will either use the source data
                                              //   as-is, or drop the builder's reference to it
                                              //   and generate new data, depending on what our
                                              //   caller does with the builder.
            fAllowedCharsSet.set(src.fAllowedCharsSet);
            fAllowedLocales.addAll(src.fAllowedLocales);
            fRestrictionLevel = src.fRestrictionLevel;
        }

        /**
         * Create a SpoofChecker with current configuration.
         *
         * @return SpoofChecker
         * @stable ICU 4.6
         */
        public SpoofChecker build() {
            if (fSpoofData == null) { // read binary file
                fSpoofData = SpoofData.getDefault();
            }

            // Copy all state from the builder to the new SpoofChecker.
            //  Make sure that everything is either cloned or copied, so
            //  that subsequent re-use of the builder won't modify the built
            //  SpoofChecker.
            //
            //  One exception to this: the SpoofData is just assigned.
            //  If the builder subsequently needs to modify fSpoofData
            //  it will create a new SpoofData object first.


            SpoofChecker result = new SpoofChecker();
            result.fChecks = this.fChecks;
            result.fSpoofData = this.fSpoofData;
            result.fAllowedCharsSet = (UnicodeSet) (this.fAllowedCharsSet.clone());
            result.fAllowedCharsSet.freeze();
            result.fAllowedLocales = new HashSet<ULocale>(this.fAllowedLocales);
            result.fRestrictionLevel = this.fRestrictionLevel;
            return result;
        }

        /**
         * Specify the source form of the spoof data Spoof Checker. The inputs correspond to the Unicode data
         * files confusables.txt and confusablesWholeScript.txt as described in Unicode UAX 39. The syntax of the source
         * data is as described in UAX 39 for these files, and the content of these files is acceptable input.
         *
         * @param confusables
         *            the Reader of confusable characters definitions, as found in file confusables.txt from
         *            unicode.org.
         * @param confusablesWholeScript
         *            the Reader of whole script confusables definitions, as found in the file
         *            xonfusablesWholeScript.txt from unicode.org.
         * @throws ParseException
         *             To report syntax errors in the input.
         * @stable ICU 4.6
         */
        public Builder setData(Reader confusables, Reader confusablesWholeScript) throws ParseException,
        java.io.IOException {

            // Compile the binary data from the source (text) format.
            //   Drop the builder's reference to any pre-existing data, which may
            //   be in use in an already-built checker.

            fSpoofData = new SpoofData();
            ConfusabledataBuilder.buildConfusableData(confusables, fSpoofData);
            WSConfusableDataBuilder.buildWSConfusableData(confusablesWholeScript, fSpoofData);
            return this;
        }

        /**
         * Specify the set of checks that will be performed by the check functions of this Spoof Checker.
         *
         * @param checks
         *            The set of checks that this spoof checker will perform. The value is an 'or' of the desired
         *            checks.
         * @return self
         * @stable ICU 4.6
         */
        public Builder setChecks(int checks) {
            // Verify that the requested checks are all ones (bits) that
            // are acceptable, known values.
            if (0 != (checks & ~SpoofChecker.ALL_CHECKS)) {
                throw new IllegalArgumentException("Bad Spoof Checks value.");
            }
            this.fChecks = (checks & SpoofChecker.ALL_CHECKS);
            return this;
        }

        /**
         * Limit characters that are acceptable in identifiers being checked to those normally used with the languages
         * associated with the specified locales. Any previously specified list of locales is replaced by the new
         * settings.
         *
         * A set of languages is determined from the locale(s), and from those a set of acceptable Unicode scripts is
         * determined. Characters from this set of scripts, along with characters from the "common" and "inherited"
         * Unicode Script categories will be permitted.
         *
         * Supplying an empty string removes all restrictions; characters from any script will be allowed.
         *
         * The CHAR_LIMIT test is automatically enabled for this SpoofChecker when calling this function with a
         * non-empty list of locales.
         *
         * The Unicode Set of characters that will be allowed is accessible via the getAllowedChars() function.
         * setAllowedLocales() will <i>replace</i> any previously applied set of allowed characters.
         *
         * Adjustments, such as additions or deletions of certain classes of characters, can be made to the result of
         * setAllowedLocales() by fetching the resulting set with getAllowedChars(), manipulating it with the Unicode
         * Set API, then resetting the spoof detectors limits with setAllowedChars()
         *
         * @param locales
         *            A Set of ULocales, from which the language and associated script are extracted. If the locales Set
         *            is null, no restrictions will be placed on the allowed characters.
         *
         * @return self
         * @stable ICU 4.6
         */
        public Builder setAllowedLocales(Set<ULocale> locales) {
            fAllowedCharsSet.clear();

            for (ULocale locale : locales) {
                // Add the script chars for this locale to the accumulating set
                // of allowed chars.
                addScriptChars(locale, fAllowedCharsSet);
            }

            // If our caller provided an empty list of locales, we disable the
            // allowed characters checking
            fAllowedLocales.clear();
            if (locales.size() == 0) {
                fAllowedCharsSet.add(0, 0x10ffff);
                fChecks &= ~CHAR_LIMIT;
                return this;
            }

            // Add all common and inherited characters to the set of allowed
            // chars.
            UnicodeSet tempSet = new UnicodeSet();
            tempSet.applyIntPropertyValue(UProperty.SCRIPT, UScript.COMMON);
            fAllowedCharsSet.addAll(tempSet);
            tempSet.applyIntPropertyValue(UProperty.SCRIPT, UScript.INHERITED);
            fAllowedCharsSet.addAll(tempSet);

            // Store the updated spoof checker state.
            fAllowedLocales.clear();
            fAllowedLocales.addAll(locales);
            fChecks |= CHAR_LIMIT;
            return this;
        }

        /**
         * Limit characters that are acceptable in identifiers being checked to those normally used with the languages
         * associated with the specified locales. Any previously specified list of locales is replaced by the new
         * settings.
         * @param locales
         *            A Set of Locales, from which the language and associated script are extracted. If the locales Set
         *            is null, no restrictions will be placed on the allowed characters.
         *
         * @return self
         * @draft ICU 54
         * @provisional This API might change or be removed in a future release.
         */
        public Builder setAllowedJavaLocales(Set<Locale> locales) {
            HashSet<ULocale> ulocales = new HashSet<ULocale>(locales.size());
            for (Locale locale : locales) {
                ulocales.add(ULocale.forLocale(locale));
            }
            return setAllowedLocales(ulocales);
        }

        // Add (union) to the UnicodeSet all of the characters for the scripts
        // used for the specified locale. Part of the implementation of
        // setAllowedLocales.
        private void addScriptChars(ULocale locale, UnicodeSet allowedChars) {
            int scripts[] = UScript.getCode(locale);
            UnicodeSet tmpSet = new UnicodeSet();
            int i;
            for (i = 0; i < scripts.length; i++) {
                tmpSet.applyIntPropertyValue(UProperty.SCRIPT, scripts[i]);
                allowedChars.addAll(tmpSet);
            }
        }

        /**
         * Limit the acceptable characters to those specified by a Unicode Set. Any previously specified character limit
         * is is replaced by the new settings. This includes limits on characters that were set with the
         * setAllowedLocales() function. Note that the RESTRICTED set is useful;
         *
         * The CHAR_LIMIT test is automatically enabled for this SpoofChecker by this function.
         *
         * @param chars
         *            A Unicode Set containing the list of characters that are permitted. The incoming set is cloned by
         *            this function, so there are no restrictions on modifying or deleting the UnicodeSet after calling
         *            this function. Note that this clears the allowedLocales set.
         * @return self
         * @stable ICU 4.6
         */
        public Builder setAllowedChars(UnicodeSet chars) {
            fAllowedCharsSet.set(chars);
            fAllowedLocales.clear();
            fChecks |= CHAR_LIMIT;
            return this;
        }


        /**
         * Set the loosest restriction level allowed. The default if this is not called is HIGHLY_RESTRICTIVE.
         * This method also sets RESTRICTION_LEVEL.
         * @param restrictionLevel The loosest restriction level allowed.
         * @return self
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        public Builder setRestrictionLevel(RestrictionLevel restrictionLevel) {
            fRestrictionLevel = restrictionLevel;
            fChecks |= RESTRICTION_LEVEL;
            return this;
        }

        // Structure for the Whole Script Confusable Data
        // See Unicode UAX-39, Unicode Security Mechanisms, for a description of the
        // Whole Script confusable data
        //
        // The data provides mappings from code points to a set of scripts
        // that contain characters that might be confused with the code point.
        // There are two mappings, one for lower case only, and one for characters
        // of any case.
        //
        // The actual data consists of a utrie2 to map from a code point to an offset,
        // and an array of UScriptSets (essentially bit maps) that is indexed
        // by the offsets obtained from the Trie.
        //
        //

        /*
         * Internal functions for compiling Whole Script confusable source data into its binary (runtime) form. The
         * binary data format is described in uspoof_impl.h
         */
        private static class WSConfusableDataBuilder {

            // Regular expression for parsing a line from the Unicode file confusablesWholeScript.txt
            // Example Lines:
            //   006F           ; Latn; Deva; A #      (o) LATIN SMALL LETTER O
            //   0048..0049     ; Latn; Grek; A #  [2] (H..I) LATIN CAPITAL LETTER H..LATIN CAPITAL LETTER I
            //     |               |     |    |
            //     |               |     |    |---- Which table, Any Case or Lower Case (A or L)
            //     |               |     |----------Target script. We need this.
            //     |               |----------------Src script. Should match the script of the source
            //     |                                code points. Beyond checking that, we don't keep it.
            //     |--------------------------------Source code points or range.
            //
            // The expression will match _all_ lines, including erroneous lines.
            // The result of the parse is returned via the contents of the (match) groups.
            static String parseExp =
                "(?m)" +                        // Multi-line mode
                "^([ \\t]*(?:#.*?)?)$" +        // A blank or comment line. Matches Group 1.
                "|^(?:" +                       // OR
                "\\s*([0-9A-F]{4,})(?:..([0-9A-F]{4,}))?\\s*;" + // Code point range. Groups 2 and 3.
                "\\s*([A-Za-z]+)\\s*;" +        // The source script. Group 4.
                "\\s*([A-Za-z]+)\\s*;" +        // The target script. Group 5.
                "\\s*(?:(A)|(L))" +             // The table A or L. Group 6 or 7
                "[ \\t]*(?:#.*?)?" +            // Trailing commment
                ")$|" +                         // OR
                "^(.*?)$";                      // An error line. Group 8.
                                                // Any line not matching the preceding
                                                // parts of the expression will match
                                                // this, and thus be flagged as an error


            static void readWholeFileToString(Reader reader, StringBuffer buffer) throws java.io.IOException {
                // Convert the user input data from UTF-8 to char (UTF-16)
                LineNumberReader lnr = new LineNumberReader(reader);
                do {
                    String line = lnr.readLine();
                    if (line == null) {
                        break;
                    }
                    buffer.append(line);
                    buffer.append('\n');
                } while (true);
            }

            // Build the Whole Script Confusable data
            //
            static void buildWSConfusableData(Reader confusablesWS, SpoofData dest)
                    throws ParseException, java.io.IOException {
                Pattern parseRegexp = null;
                StringBuffer input = new StringBuffer();
                int lineNum = 0;

                ArrayList<BuilderScriptSet> scriptSets = null;
                int rtScriptSetsCount = 2;

                Trie2Writable anyCaseTrie = new Trie2Writable(0, 0);
                Trie2Writable lowerCaseTrie = new Trie2Writable(0, 0);

                // The scriptSets vector provides a mapping from TRIE values to the set
                // of scripts.
                //
                // Reserved TRIE values:
                //   0: Code point has no whole script confusables.
                //   1: Code point is of script Common or Inherited.
                //
                // These code points do not participate in whole script confusable detection.
                // (This is logically equivalent to saying that they contain confusables
                // in all scripts)
                //
                // Because Trie values are indexes into the ScriptSets vector, pre-fill
                // vector positions 0 and 1 to avoid conflicts with the reserved values.

                scriptSets = new ArrayList<BuilderScriptSet>();
                scriptSets.add(null);
                scriptSets.add(null);

                readWholeFileToString(confusablesWS, input);

                parseRegexp = Pattern.compile(parseExp);

                // Zap any Byte Order Mark at the start of input. Changing it to a space
                // is benign
                // given the syntax of the input.
                if (input.charAt(0) == 0xfeff) {
                    input.setCharAt(0, (char) 0x20);
                }

                // Parse the input, one line per iteration of this loop.
                Matcher matcher = parseRegexp.matcher(input);
                while (matcher.find()) {
                    lineNum++;
                    if (matcher.start(1) >= 0) {
                        // this was a blank or comment line.
                        continue;
                    }
                    if (matcher.start(8) >= 0) {
                        // input file syntax error.
                        throw new ParseException("ConfusablesWholeScript, line " + lineNum + ": Unrecognized input: "
                                + matcher.group(), matcher.start());
                    }

                    // Pick up the start and optional range end code points from the
                    // parsed line.
                    int startCodePoint = Integer.parseInt(matcher.group(2), 16);
                    if (startCodePoint > 0x10ffff) {
                        throw new ParseException("ConfusablesWholeScript, line " + lineNum
                                + ": out of range code point: " + matcher.group(2), matcher.start(2));
                    }
                    int endCodePoint = startCodePoint;
                    if (matcher.start(3) >= 0) {
                        endCodePoint = Integer.parseInt(matcher.group(3), 16);
                    }
                    if (endCodePoint > 0x10ffff) {
                        throw new ParseException("ConfusablesWholeScript, line " + lineNum
                                + ": out of range code point: " + matcher.group(3), matcher.start(3));
                    }

                    // Extract the two script names from the source line.
                    String srcScriptName = matcher.group(4);
                    String targScriptName = matcher.group(5);
                    int srcScript = UCharacter.getPropertyValueEnum(UProperty.SCRIPT, srcScriptName);
                    int targScript = UCharacter.getPropertyValueEnum(UProperty.SCRIPT, targScriptName);
                    if (srcScript == UScript.INVALID_CODE) {
                        throw new ParseException("ConfusablesWholeScript, line " + lineNum
                                + ": Invalid script code t: " + matcher.group(4), matcher.start(4));
                    }
                    if (targScript == UScript.INVALID_CODE) {
                        throw new ParseException("ConfusablesWholeScript, line " + lineNum
                                + ": Invalid script code t: " + matcher.group(5), matcher.start(5));
                    }

                    // select the table - (A) any case or (L) lower case only
                    Trie2Writable table = anyCaseTrie;
                    if (matcher.start(7) >= 0) {
                        table = lowerCaseTrie;
                    }

                    // Build the set of scripts containing confusable characters for
                    // the code point(s) specified in this input line.
                    // Sanity check that the script of the source code point is the same
                    // as the source script indicated in the input file. Failure of this
                    // check is an error in the input file.
                    //
                    // Include the source script in the set (needed for Mixed Script
                    // Confusable detection).
                    //
                    int cp;
                    for (cp = startCodePoint; cp <= endCodePoint; cp++) {
                        int setIndex = table.get(cp);
                        BuilderScriptSet bsset = null;
                        if (setIndex > 0) {
                            assert (setIndex < scriptSets.size());
                            bsset = scriptSets.get(setIndex);
                        } else {
                            bsset = new BuilderScriptSet();
                            bsset.codePoint = cp;
                            bsset.trie = table;
                            bsset.sset = new ScriptSet();
                            setIndex = scriptSets.size();
                            bsset.index = setIndex;
                            bsset.rindex = 0;
                            scriptSets.add(bsset);
                            table.set(cp, setIndex);
                        }
                        bsset.sset.Union(targScript);
                        bsset.sset.Union(srcScript);

                        int cpScript = UScript.getScript(cp);
                        if (cpScript != srcScript) {
                            // status = U_INVALID_FORMAT_ERROR;
                            throw new ParseException("ConfusablesWholeScript, line " + lineNum
                                    + ": Mismatch between source script and code point " + Integer.toString(cp, 16),
                                    matcher.start(5));
                        }
                    }
                }

                // Eliminate duplicate script sets. At this point we have a separate
                // script set for every code point that had data in the input file.
                //
                // We eliminate underlying ScriptSet objects, not the BuildScriptSets
                // that wrap them
                //
                // printf("Number of scriptSets: %d\n", scriptSets.size());
                //int duplicateCount = 0;
                rtScriptSetsCount = 2;
                for (int outeri = 2; outeri < scriptSets.size(); outeri++) {
                    BuilderScriptSet outerSet = scriptSets.get(outeri);
                    if (outerSet.index != outeri) {
                        // This set was already identified as a duplicate.
                        // It will not be allocated a position in the runtime array
                        // of ScriptSets.
                        continue;
                    }
                    outerSet.rindex = rtScriptSetsCount++;
                    for (int inneri = outeri + 1; inneri < scriptSets.size(); inneri++) {
                        BuilderScriptSet innerSet = scriptSets.get(inneri);
                        if (outerSet.sset.equals(innerSet.sset) && outerSet.sset != innerSet.sset) {
                            innerSet.sset = outerSet.sset;
                            innerSet.index = outeri;
                            innerSet.rindex = outerSet.rindex;
                            //duplicateCount++;
                        }
                        // But this doesn't get all. We need to fix the TRIE.
                    }
                }
                // printf("Number of distinct script sets: %d\n",
                // rtScriptSetsCount);

                // Update the Trie values to be reflect the run time script indexes (after duplicate merging).
                // (Trie Values 0 and 1 are reserved, and the corresponding slots in scriptSets
                // are unused, which is why the loop index starts at 2.)
                for (int i = 2; i < scriptSets.size(); i++) {
                    BuilderScriptSet bSet = scriptSets.get(i);
                    if (bSet.rindex != i) {
                        bSet.trie.set(bSet.codePoint, bSet.rindex);
                    }
                }

                // For code points with script==Common or script==Inherited,
                // Set the reserved value of 1 into both Tries. These characters do not participate
                // in Whole Script Confusable detection; this reserved value is the means
                // by which they are detected.
                UnicodeSet ignoreSet = new UnicodeSet();
                ignoreSet.applyIntPropertyValue(UProperty.SCRIPT, UScript.COMMON);
                UnicodeSet inheritedSet = new UnicodeSet();
                inheritedSet.applyIntPropertyValue(UProperty.SCRIPT, UScript.INHERITED);
                ignoreSet.addAll(inheritedSet);
                for (int rn = 0; rn < ignoreSet.getRangeCount(); rn++) {
                    int rangeStart = ignoreSet.getRangeStart(rn);
                    int rangeEnd = ignoreSet.getRangeEnd(rn);
                    anyCaseTrie.setRange(rangeStart, rangeEnd, 1, true);
                    lowerCaseTrie.setRange(rangeStart, rangeEnd, 1, true);
                }

                // Put the compiled data to the destination SpoofData
                dest.fAnyCaseTrie   = anyCaseTrie.toTrie2_16();
                dest.fLowerCaseTrie = lowerCaseTrie.toTrie2_16();
                dest.fScriptSets = new ScriptSet[rtScriptSetsCount];
                dest.fScriptSets[0] = new ScriptSet();
                dest.fScriptSets[1] = new ScriptSet();

                int rindex = 2;
                for (int i = 2; i < scriptSets.size(); i++) {
                    BuilderScriptSet bSet = scriptSets.get(i);
                    if (bSet.rindex < rindex) {
                        // We have already put this script set to the output data.
                        continue;
                    }
                    assert (rindex == bSet.rindex);
                    dest.fScriptSets[rindex] = bSet.sset;
                    rindex++;
                }
            }

            // class BuilderScriptSet. Represents the set of scripts (Script Codes)
            // containing characters that are confusable with one specific
            // code point.
            static class BuilderScriptSet {
                int codePoint;           // The source code point.
                Trie2Writable trie;      // Any-case or Lower-case Trie.
                                         // These Trie tables are the final result of the
                                         // build. This flag indicates which of the two
                                         // this set of data is for.

                ScriptSet sset;          // The set of scripts itself.

                int index;               // Index of this set in the Build Time vector
                                         // of script sets.

                int rindex;              // Index of this set in the final (runtime)
                                         // array of sets.

                // its underlying sset.

                BuilderScriptSet() {
                    codePoint = -1;
                    trie = null;
                    sset = null;
                    index = 0;
                    rindex = 0;
                }
            }

        }

        /*
         * *****************************************************************************
         * Internal classes for compililing confusable data into its binary (runtime) form.
         * *****************************************************************************
         */
        // ---------------------------------------------------------------------
        //
        // buildConfusableData Compile the source confusable data, as defined by
        // the Unicode data file confusables.txt, into the binary
        // structures used by the confusable detector.
        //
        // The binary structures are described in uspoof_impl.h
        //
        // 1. parse the data, building 4 hash tables, one each for the SL, SA, ML and MA
        // tables. Each maps from a int to a String.
        //
        // 2. Sort all of the strings encountered by length, since they will need to
        // be stored in that order in the final string table.
        //
        // 3. Build a list of keys (UChar32s) from the four mapping tables. Sort the
        // list because that will be the ordering of our runtime table.
        //
        // 4. Generate the run time string table. This is generated before the key & value
        // tables because we need the string indexes when building those tables.
        //
        // 5. Build the run-time key and value tables. These are parallel tables, and
        // are built at the same time

        // class ConfusabledataBuilder
        //     An instance of this class exists while the confusable data is being built from source.
        //     It encapsulates the intermediate data structures that are used for building.
        //     It exports one static function, to do a confusable data build.
        private static class ConfusabledataBuilder {

            private Hashtable<Integer, SPUString> fSLTable;
            private Hashtable<Integer, SPUString> fSATable;
            private Hashtable<Integer, SPUString> fMLTable;
            private Hashtable<Integer, SPUString> fMATable;
            private UnicodeSet fKeySet; // A set of all keys (UChar32s) that go into the
                                        // four mapping tables.

            // The compiled data is first assembled into the following four collections,
            // then output to the builder's SpoofData object.
            private StringBuffer fStringTable;
            private ArrayList<Integer> fKeyVec;
            private ArrayList<Integer> fValueVec;
            private ArrayList<Integer> fStringLengthsTable;
            private SPUStringPool stringPool;
            private Pattern fParseLine;
            private Pattern fParseHexNum;
            private int fLineNum;

            ConfusabledataBuilder() {
                fSLTable  = new Hashtable<Integer, SPUString>();
                fSATable  = new Hashtable<Integer, SPUString>();
                fMLTable  = new Hashtable<Integer, SPUString>();
                fMATable  = new Hashtable<Integer, SPUString>();
                fKeySet   = new UnicodeSet();
                fKeyVec   = new ArrayList<Integer>();
                fValueVec = new ArrayList<Integer>();
                stringPool = new SPUStringPool();
            }

            void build(Reader confusables, SpoofData dest) throws ParseException, java.io.IOException {
                StringBuffer fInput = new StringBuffer();
                WSConfusableDataBuilder.readWholeFileToString(confusables, fInput);

                // Regular Expression to parse a line from Confusables.txt. The expression will match
                // any line. What was matched is determined by examining which capture groups have a match.
                //   Capture Group 1: the source char
                //   Capture Group 2: the replacement chars
                //   Capture Group 3-6 the table type, SL, SA, ML, or MA
                //   Capture Group 7: A blank or comment only line.
                //   Capture Group 8: A syntactically invalid line. Anything that didn't match before.
                // Example Line from the confusables.txt source file:
                //   "1D702 ; 006E 0329 ; SL # MATHEMATICAL ITALIC SMALL ETA ... "
                fParseLine = Pattern.compile("(?m)^[ \\t]*([0-9A-Fa-f]+)[ \\t]+;" + // Match the source char
                        "[ \\t]*([0-9A-Fa-f]+" +                     // Match the replacement char(s)
                        "(?:[ \\t]+[0-9A-Fa-f]+)*)[ \\t]*;" +        //     (continued)
                        "\\s*(?:(SL)|(SA)|(ML)|(MA))" +              // Match the table type
                        "[ \\t]*(?:#.*?)?$" +                        // Match any trailing #comment
                        "|^([ \\t]*(?:#.*?)?)$" +                    // OR match empty lines or lines with only a #comment
                        "|^(.*?)$");                                 // OR match any line, which catches illegal lines.

                // Regular expression for parsing a hex number out of a space-separated list of them.
                // Capture group 1 gets the number, with spaces removed.
                fParseHexNum = Pattern.compile("\\s*([0-9A-F]+)");

                // Zap any Byte Order Mark at the start of input. Changing it to a space
                // is benign given the syntax of the input.
                if (fInput.charAt(0) == 0xfeff) {
                    fInput.setCharAt(0, (char) 0x20);
                }

                // Parse the input, one line per iteration of this loop.
                Matcher matcher = fParseLine.matcher(fInput);
                while (matcher.find()) {
                    fLineNum++;
                    if (matcher.start(7) >= 0) {
                        // this was a blank or comment line.
                        continue;
                    }
                    if (matcher.start(8) >= 0) {
                        // input file syntax error.
                        // status = U_PARSE_ERROR;
                        throw new ParseException("Confusables, line " + fLineNum + ": Unrecognized Line: "
                                + matcher.group(8), matcher.start(8));
                    }

                    // We have a good input line. Extract the key character and mapping
                    // string, and
                    // put them into the appropriate mapping table.
                    int keyChar = Integer.parseInt(matcher.group(1), 16);
                    if (keyChar > 0x10ffff) {
                        throw new ParseException("Confusables, line " + fLineNum + ": Bad code point: "
                                + matcher.group(1), matcher.start(1));
                    }
                    Matcher m = fParseHexNum.matcher(matcher.group(2));

                    StringBuilder mapString = new StringBuilder();
                    while (m.find()) {
                        int c = Integer.parseInt(m.group(1), 16);
                        if (keyChar > 0x10ffff) {
                            throw new ParseException("Confusables, line " + fLineNum + ": Bad code point: "
                                    + Integer.toString(c, 16), matcher.start(2));
                        }
                        mapString.appendCodePoint(c);
                    }
                    assert (mapString.length() >= 1);

                    // Put the map (value) string into the string pool
                    // This a little like a Java intern() - any duplicates will be
                    // eliminated.
                    SPUString smapString = stringPool.addString(mapString.toString());

                    // Add the char . string mapping to the appropriate table.
                    Hashtable<Integer, SPUString> table = matcher.start(3) >= 0 ? fSLTable
                            : matcher.start(4) >= 0 ? fSATable : matcher.start(5) >= 0 ? fMLTable
                                    : matcher.start(6) >= 0 ? fMATable : null;
                                    assert (table != null);
                                    table.put(keyChar, smapString);
                                    fKeySet.add(keyChar);
                }

                // Input data is now all parsed and collected.
                // Now create the run-time binary form of the data.
                //
                // This is done in two steps. First the data is assembled into vectors and strings,
                // for ease of construction, then the contents of these collections are copied
                // into the actual SpoofData object.

                // Build up the string array, and record the index of each string therein
                // in the (build time only) string pool.
                // Strings of length one are not entered into the strings array.
                // At the same time, build up the string lengths table, which records the
                // position in the string table of the first string of each length >= 4.
                // (Strings in the table are sorted by length)

                stringPool.sort();
                fStringTable = new StringBuffer();
                fStringLengthsTable = new ArrayList<Integer>();
                int previousStringLength = 0;
                int previousStringIndex = 0;
                int poolSize = stringPool.size();
                int i;
                for (i = 0; i < poolSize; i++) {
                    SPUString s = stringPool.getByIndex(i);
                    int strLen = s.fStr.length();
                    int strIndex = fStringTable.length();
                    assert (strLen >= previousStringLength);
                    if (strLen == 1) {
                        // strings of length one do not get an entry in the string table.
                        // Keep the single string character itself here, which is the same
                        // convention that is used in the final run-time string table index.
                        s.fStrTableIndex = s.fStr.charAt(0);
                    } else {
                        if ((strLen > previousStringLength) && (previousStringLength >= 4)) {
                            fStringLengthsTable.add(previousStringIndex);
                            fStringLengthsTable.add(previousStringLength);
                        }
                        s.fStrTableIndex = strIndex;
                        fStringTable.append(s.fStr);
                    }
                    previousStringLength = strLen;
                    previousStringIndex = strIndex;
                }
                // Make the final entry to the string lengths table.
                // (it holds an entry for the _last_ string of each length, so adding
                // the
                // final one doesn't happen in the main loop because no longer string
                // was encountered.)
                if (previousStringLength >= 4) {
                    fStringLengthsTable.add(previousStringIndex);
                    fStringLengthsTable.add(previousStringLength);
                }

                // Construct the compile-time Key and Value tables
                //
                // For each key code point, check which mapping tables it applies to,
                // and create the final data for the key & value structures.
                //
                // The four logical mapping tables are conflated into one combined
                // table.
                // If multiple logical tables have the same mapping for some key, they
                // share a single entry in the combined table.
                // If more than one mapping exists for the same key code point, multiple
                // entries will be created in the table

                for (String keyCharStr: fKeySet) {
                    int keyChar = keyCharStr.codePointAt(0);
                    addKeyEntry(keyChar, fSLTable, SpoofChecker.SL_TABLE_FLAG);
                    addKeyEntry(keyChar, fSATable, SpoofChecker.SA_TABLE_FLAG);
                    addKeyEntry(keyChar, fMLTable, SpoofChecker.ML_TABLE_FLAG);
                    addKeyEntry(keyChar, fMATable, SpoofChecker.MA_TABLE_FLAG);
                }

                // Put the assembled data into the destination SpoofData object.

                // The Key Table
                //     While copying the keys to the output array,
                //     also sanity check that the keys are sorted.

                int numKeys = fKeyVec.size();
                dest.fCFUKeys = new int[numKeys];
                int previousKey = 0;
                for (i=0; i<numKeys; i++) {
                    int key = fKeyVec.get(i);
                    assert ((key & 0x00ffffff) >= (previousKey & 0x00ffffff));
                    assert ((key & 0xff000000) != 0);
                    dest.fCFUKeys[i] = key;
                    previousKey = key;
                }

                // The Value Table, parallels the key table
                int numValues = fValueVec.size();
                assert (numKeys == numValues);
                dest.fCFUValues = new short[numValues];
                i = 0;
                for (int value:fValueVec) {
                    assert (value < 0xffff);
                    dest.fCFUValues[i++] = (short)value;
                }

                // The Strings Table.

                dest.fCFUStrings = fStringTable.toString();


                // The String Lengths Table.

                // While copying into the runtime array do some sanity checks on the values
                // Each complete entry contains two fields, an index and an offset.
                // Lengths should increase with each entry.
                // Offsets should be less than the size of the string table.

                int lengthTableLength = fStringLengthsTable.size();
                int previousLength = 0;

                // Note: StringLengthsSize in the raw data is the number of complete entries,
                //       each consisting of a pair of 16 bit values, hence the divide by 2.

                int stringLengthsSize = lengthTableLength / 2;
                dest.fCFUStringLengths = new SpoofData.SpoofStringLengthsElement[stringLengthsSize];
                for (i = 0; i < stringLengthsSize; i += 1) {
                    int offset = fStringLengthsTable.get(i*2);
                    int length = fStringLengthsTable.get(i*2 + 1);
                    assert (offset < dest.fCFUStrings.length());
                    assert (length < 40);
                    assert (length > previousLength);
                    dest.fCFUStringLengths[i] = new SpoofData.SpoofStringLengthsElement();
                    dest.fCFUStringLengths[i].fLastString = offset;
                    dest.fCFUStringLengths[i].fStrLength  = length;
                    previousLength = length;
                }
             }

            // Add an entry to the key and value tables being built
            // input: data from SLTable, MATable, etc.
            // outut: entry added to fKeyVec and fValueVec
            // addKeyEntry Construction of the confusable Key and Mapping Values tables.
            // This is an intermediate point in the building process.
            // We already have the mappings in the hash tables fSLTable, etc.
            // This function builds corresponding run-time style table entries into
            // fKeyVec and fValueVec
            void addKeyEntry(int keyChar, // The key character
                    Hashtable<Integer, SPUString> table, // The table, one of SATable,
                    // MATable, etc.
                    int tableFlag) { // One of SA_TABLE_FLAG, etc.
                SPUString targetMapping = table.get(keyChar);
                if (targetMapping == null) {
                    // No mapping for this key character.
                    // (This function is called for all four tables for each key char
                    // that
                    // is seen anywhere, so this no entry cases are very much expected.)
                    return;
                }

                // Check whether there is already an entry with the correct mapping.
                // If so, simply set the flag in the keyTable saying that the existing
                // entry
                // applies to the table that we're doing now.
                boolean keyHasMultipleValues = false;
                int i;
                for (i = fKeyVec.size() - 1; i >= 0; i--) {
                    int key = fKeyVec.get(i);
                    if ((key & 0x0ffffff) != keyChar) {
                        // We have now checked all existing key entries for this key
                        // char (if any)
                        // without finding one with the same mapping.
                        break;
                    }
                    String mapping = getMapping(i);
                    if (mapping.equals(targetMapping.fStr)) {
                        // The run time entry we are currently testing has the correct
                        // mapping.
                        // Set the flag in it indicating that it applies to the new
                        // table also.
                        key |= tableFlag;
                        fKeyVec.set(i, key);
                        return;
                    }
                    keyHasMultipleValues = true;
                }

                // Need to add a new entry to the binary data being built for this
                // mapping.
                // Includes adding entries to both the key table and the parallel values
                // table.
                int newKey = keyChar | tableFlag;
                if (keyHasMultipleValues) {
                    newKey |= SpoofChecker.KEY_MULTIPLE_VALUES;
                }
                int adjustedMappingLength = targetMapping.fStr.length() - 1;
                if (adjustedMappingLength > 3) {
                    adjustedMappingLength = 3;
                }
                newKey |= adjustedMappingLength << SpoofChecker.KEY_LENGTH_SHIFT;

                int newData = targetMapping.fStrTableIndex;

                fKeyVec.add(newKey);
                fValueVec.add(newData);

                // If the preceding key entry is for the same key character (but with a
                // different mapping)
                // set the multiple-values flag on it.
                if (keyHasMultipleValues) {
                    int previousKeyIndex = fKeyVec.size() - 2;
                    int previousKey = fKeyVec.get(previousKeyIndex);
                    previousKey |= SpoofChecker.KEY_MULTIPLE_VALUES;
                    fKeyVec.set(previousKeyIndex, previousKey);
                }
            }

            // From an index into fKeyVec & fValueVec
            // get a String with the corresponding mapping.
            String getMapping(int index) {
                int key = fKeyVec.get(index);
                int value = fValueVec.get(index);
                int length = SpoofChecker.getKeyLength(key);
                int lastIndexWithLen;
                switch (length) {
                case 0:
                    char[] cs = { (char) value };
                    return new String(cs);
                case 1:
                case 2:
                    return fStringTable.substring(value, value + length + 1); // Note: +1 as optimization
                case 3:
                    length = 0;
                    int i;
                    for (i = 0; i < fStringLengthsTable.size(); i += 2) {
                        lastIndexWithLen = fStringLengthsTable.get(i);
                        if (value <= lastIndexWithLen) {
                            length = fStringLengthsTable.get(i + 1);
                            break;
                        }
                    }
                    assert (length >= 3);
                    return fStringTable.substring(value, value + length);
                default:
                    assert (false);
                }
                return "";
            }





            public static void buildConfusableData(Reader confusables, SpoofData dest) throws java.io.IOException,
            ParseException {
                ConfusabledataBuilder builder = new ConfusabledataBuilder();
                builder.build(confusables, dest);
            }

            /*
             * *****************************************************************************
             * Internal classes for compiling confusable data into its binary (runtime) form.
             * *****************************************************************************
             */
            // SPUString
            // Holds a string that is the result of one of the mappings defined
            // by the confusable mapping data (confusables.txt from Unicode.org)
            // Instances of SPUString exist during the compilation process only.

            private static class SPUString {
                String fStr; // The actual string.
                int fStrTableIndex; // Index into the final runtime data for this string.
                                    // (or, for length 1, the single string char itself,
                                    // there being no string table entry for it.)

                SPUString(String s) {
                    fStr = s;
                    fStrTableIndex = 0;
                }
            }

            // Comparison function for ordering strings in the string pool.
            // Compare by length first, then, within a group of the same length,
            // by code point order.

            private static class SPUStringComparator implements Comparator<SPUString> {
                public int compare(SPUString sL, SPUString sR) {
                    int lenL = sL.fStr.length();
                    int lenR = sR.fStr.length();
                    if (lenL < lenR) {
                        return -1;
                    } else if (lenL > lenR) {
                        return 1;
                    } else {
                        return sL.fStr.compareTo(sR.fStr);
                    }
                }
            }

            // String Pool A utility class for holding the strings that are the result of
            // the spoof mappings. These strings will utimately end up in the
            // run-time String Table.
            // This is sort of like a sorted set of strings, except that ICU's anemic
            // built-in collections don't support those, so it is implemented with a
            // combination of a uhash and a Vector.
            private static class SPUStringPool {
                public SPUStringPool() {
                    fVec = new Vector<SPUString>();
                    fHash = new Hashtable<String, SPUString>();
                }

                public int size() {
                    return fVec.size();
                }

                // Get the n-th string in the collection.
                public SPUString getByIndex(int index) {
                    SPUString retString = fVec.elementAt(index);
                    return retString;
                }

                // Add a string. Return the string from the table.
                // If the input parameter string is already in the table, delete the
                // input parameter and return the existing string.
                public SPUString addString(String src) {
                    SPUString hashedString = fHash.get(src);
                    if (hashedString == null) {
                        hashedString = new SPUString(src);
                        fHash.put(src, hashedString);
                        fVec.addElement(hashedString);
                    }
                    return hashedString;
                }

                // Sort the contents; affects the ordering of getByIndex().
                public void sort() {
                    Collections.sort(fVec, new SPUStringComparator());
                }

                private Vector<SPUString> fVec; // Elements are SPUString *
                private Hashtable<String, SPUString> fHash; // Key: Value:
            }

        }
    }

    /**
     * Get the Restriction Level that is being tested.
     *
     * @return The restriction level
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public RestrictionLevel getRestrictionLevel() {
        return fRestrictionLevel;
    }

    /**
     * Get the set of checks that this Spoof Checker has been configured to perform.
     *
     * @return The set of checks that this spoof checker will perform.
     * @stable ICU 4.6
     */
    public int getChecks() {
        return fChecks;
    }

    /**
     * Get a read-only set of locales for the scripts that are acceptable in strings to be checked. If no limitations on scripts
     * have been specified, an empty set will be returned.
     *
     * setAllowedChars() will reset the list of allowed locales to be empty.
     *
     * The returned set may not be identical to the originally specified set that is supplied to setAllowedLocales();
     * the information other than languages from the originally specified locales may be omitted.
     *
     * @return A set of locales corresponding to the acceptable scripts.
     *
     * @stable ICU 4.6
     */
    public Set<ULocale> getAllowedLocales() {
        return Collections.unmodifiableSet(fAllowedLocales);
    }

    /**
     * Get a set of JDK locales for the scripts that are acceptable in strings to be checked. If no limitations on scripts
     * have been specified, an empty set will be returned.
     *
     * @return A set of locales corresponding to the acceptable scripts.
     * @draft ICU 54
     * @provisional This API might change or be removed in a future release.
     */
    public Set<Locale> getAllowedJavaLocales() {
        HashSet<Locale> locales = new HashSet<Locale>(fAllowedLocales.size());
        for (ULocale uloc : fAllowedLocales) {
            locales.add(uloc.toLocale());
        }
        return locales;
    }

    /**
     * Get a UnicodeSet for the characters permitted in an identifier. This corresponds to the limits imposed by the Set
     * Allowed Characters functions. Limitations imposed by other checks will not be reflected in the set returned by
     * this function.
     *
     * The returned set will be frozen, meaning that it cannot be modified by the caller.
     *
     * @return A UnicodeSet containing the characters that are permitted by the CHAR_LIMIT test.
     * @stable ICU 4.6
     */
    public UnicodeSet getAllowedChars() {
        return fAllowedCharsSet;
    }

    /**
     * A struct-like class to hold the results of a Spoof Check operation.
     * Tells which check(s) have failed.
     *
     * @stable ICU 4.6
     */
    public static class CheckResult {
        /**
         * Indicate which of the spoof check(s) has failed.  The value is a bitwise OR
         * of the constants for the tests in question, SINGLE_SCRIPT_CONFUSABLE,
         * MIXED_SCRIPT_CONFUSABLE, WHOLE_SCRIPT_CONFUSABLE, and so on.
         *
         * @stable ICU 4.6
         */
        public int checks;
        /**
         * The index of the first string position that failed a check.
         *
         * @deprecated ICU 51. No longer supported. Always set to zero.
         */
        @Deprecated
        public int position;
        /**
         * The numerics found in the string, if MIXED_NUMBERS was set; otherwise null;
         *
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        public UnicodeSet numerics;
        /**
         * The restriction level that the text meets, if RESTRICTION_LEVEL is set; otherwise null.
         *
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        public RestrictionLevel restrictionLevel;

        /**
         *  Default constructor
         *  @stable ICU 4.6
         */
        public CheckResult() {
            checks = 0;
            position = 0;
        }
    }

    /**
     * Check the specified string for possible security issues. The text to be checked will typically be an identifier
     * of some sort. The set of checks to be performed was specified when building the SpoofChecker.
     *
     * @param text
     *            A String to be checked for possible security issues.
     * @param checkResult
     *            Output parameter, indicates which specific tests failed.
     *            May be null if the information is not wanted.
     * @return True there any issue is found with the input string.
     * @stable ICU 4.8
     */
    public boolean failsChecks(String text, CheckResult checkResult) {
        int length = text.length();

        int result = 0;
        if (checkResult != null) {
            checkResult.position = 0;
            checkResult.numerics = null;
            checkResult.restrictionLevel = null;
        }

        // Allocate an identifier info if needed.

        IdentifierInfo identifierInfo = null;
        if (0 != ((this.fChecks) & (RESTRICTION_LEVEL | MIXED_NUMBERS))) {
            identifierInfo = getIdentifierInfo().setIdentifier(text).setIdentifierProfile(fAllowedCharsSet);
        }

        if (0 != ((this.fChecks) & RESTRICTION_LEVEL)) {
            RestrictionLevel textRestrictionLevel = identifierInfo.getRestrictionLevel();
            if (textRestrictionLevel.compareTo(fRestrictionLevel) > 0) {
                result |= RESTRICTION_LEVEL;
            }
            if (checkResult != null) {
                checkResult.restrictionLevel = textRestrictionLevel;
            }
        }

        if (0 != ((this.fChecks) & MIXED_NUMBERS)) {
            UnicodeSet numerics = identifierInfo.getNumerics();
            if (numerics.size() > 1) {
                result |= MIXED_NUMBERS;
            }
            if (checkResult != null) {
                checkResult.numerics = numerics;
            }
        }

        if (0 != (this.fChecks & CHAR_LIMIT)) {
            int i;
            int c;
            for (i = 0; i < length;) {
                // U16_NEXT(text, i, length, c);
                c = Character.codePointAt(text, i);
                i = Character.offsetByCodePoints(text, i, 1);
                if (!this.fAllowedCharsSet.contains(c)) {
                    result |= CHAR_LIMIT;
                    break;
                }
            }
        }

        if (0 != (this.fChecks & (WHOLE_SCRIPT_CONFUSABLE | MIXED_SCRIPT_CONFUSABLE | INVISIBLE))) {
            // These are the checks that need to be done on NFD input
            String nfdText = nfdNormalizer.normalize(text);

            if (0 != (this.fChecks & INVISIBLE)) {

                // scan for more than one occurence of the same non-spacing mark
                // in a sequence of non-spacing marks.
                int i;
                int c;
                int firstNonspacingMark = 0;
                boolean haveMultipleMarks = false;
                UnicodeSet marksSeenSoFar = new UnicodeSet(); // Set of combining marks in a
                                                              // single combining sequence.
                for (i = 0; i < length;) {
                    c = Character.codePointAt(nfdText, i);
                    i = Character.offsetByCodePoints(nfdText, i, 1);
                    if (Character.getType(c) != UCharacterCategory.NON_SPACING_MARK) {
                        firstNonspacingMark = 0;
                        if (haveMultipleMarks) {
                            marksSeenSoFar.clear();
                            haveMultipleMarks = false;
                        }
                        continue;
                    }
                    if (firstNonspacingMark == 0) {
                        firstNonspacingMark = c;
                        continue;
                    }
                    if (!haveMultipleMarks) {
                        marksSeenSoFar.add(firstNonspacingMark);
                        haveMultipleMarks = true;
                    }
                    if (marksSeenSoFar.contains(c)) {
                        // report the error, and stop scanning.
                        // No need to find more than the first failure.
                        result |= INVISIBLE;
                        break;
                    }
                    marksSeenSoFar.add(c);
                }
            }

            if (0 != (this.fChecks & (WHOLE_SCRIPT_CONFUSABLE | MIXED_SCRIPT_CONFUSABLE))) {
                // The basic test is the same for both whole and mixed script confusables.
                // Compute the set of scripts that every input character has a confusable in.
                // For this computation an input character is always considered to be
                // confusable with itself in its own script.
                //
                // If the number of such scripts is two or more, and the input consisted of
                // characters all from a single script, we have a whole script confusable.
                // (The two scripts will be the original script and the one that is confusable).

                // If the number of such scripts >= one, and the original input contained characters from
                // more than one script, we have a mixed script confusable. (We can transform
                // some of the characters, and end up with a visually similar string all in one script.)

                if (identifierInfo == null) {
                    identifierInfo = getIdentifierInfo();
                    identifierInfo.setIdentifier(text);
                }
                int scriptCount = identifierInfo.getScriptCount();

                ScriptSet scripts = new ScriptSet();
                this.wholeScriptCheck(nfdText, scripts);
                int confusableScriptCount = scripts.countMembers();

                if ((0 != (this.fChecks & WHOLE_SCRIPT_CONFUSABLE)) && confusableScriptCount >= 2 && scriptCount == 1) {
                    result |= WHOLE_SCRIPT_CONFUSABLE;
                }

                if ((0 != (this.fChecks & MIXED_SCRIPT_CONFUSABLE)) && confusableScriptCount >= 1 && scriptCount > 1) {
                    result |= MIXED_SCRIPT_CONFUSABLE;
                }
            }
        }
        if (checkResult != null) {
            checkResult.checks = result;
        }
        releaseIdentifierInfo(identifierInfo);
        return (0 != result);
    }

    /**
     * Check the specified string for possible security issues. The text to be checked will typically be an identifier
     * of some sort. The set of checks to be performed was specified when building the SpoofChecker.
     *
     * @param text
     *            A String to be checked for possible security issues.
     * @return True there any issue is found with the input string.
     * @stable ICU 4.8
     */
    public boolean failsChecks(String text) {
        return failsChecks(text, null);
    }

    /**
     * Check the whether two specified strings are visually confusable. The types of confusability to be tested - single
     * script, mixed script, or whole script - are determined by the check options set for the SpoofChecker.
     *
     * The tests to be performed are controlled by the flags SINGLE_SCRIPT_CONFUSABLE MIXED_SCRIPT_CONFUSABLE
     * WHOLE_SCRIPT_CONFUSABLE At least one of these tests must be selected.
     *
     * ANY_CASE is a modifier for the tests. Select it if the identifiers may be of mixed case. If identifiers are case
     * folded for comparison and display to the user, do not select the ANY_CASE option.
     *
     *
     * @param s1
     *            The first of the two strings to be compared for confusability.
     * @param s2
     *            The second of the two strings to be compared for confusability.
     * @return Non-zero if s1 and s1 are confusable. If not 0, the value will indicate the type(s) of confusability
     *         found, as defined by spoof check test constants.
     * @stable ICU 4.6
     */
    public int areConfusable(String s1, String s2) {
        //
        // See section 4 of UAX 39 for the algorithm for checking whether two strings are confusable,
        // and for definitions of the types (single, whole, mixed-script) of confusables.

        // We only care about a few of the check flags. Ignore the others.
        // If no tests relavant to this function have been specified, signal an error.
        // TODO: is this really the right thing to do? It's probably an error on
        // the caller's part, but logically we would just return 0 (no error).
        if ((this.fChecks & (SINGLE_SCRIPT_CONFUSABLE | MIXED_SCRIPT_CONFUSABLE | WHOLE_SCRIPT_CONFUSABLE)) == 0) {
            throw new IllegalArgumentException("No confusable checks are enabled.");
        }
        int flagsForSkeleton = this.fChecks & ANY_CASE;

        int result = 0;
        IdentifierInfo identifierInfo = getIdentifierInfo();
        identifierInfo.setIdentifier(s1);
        int s1ScriptCount = identifierInfo.getScriptCount();
        int s1FirstScript = identifierInfo.getScripts().nextSetBit(0);
        identifierInfo.setIdentifier(s2);
        int s2ScriptCount = identifierInfo.getScriptCount();
        int s2FirstScript = identifierInfo.getScripts().nextSetBit(0);
        releaseIdentifierInfo(identifierInfo);

        if (0 != (this.fChecks & SINGLE_SCRIPT_CONFUSABLE)) {
            // Do the Single Script compare.
            if (s1ScriptCount <= 1 && s2ScriptCount <= 1 && s1FirstScript == s2FirstScript) {
                flagsForSkeleton |= SINGLE_SCRIPT_CONFUSABLE;
                String s1Skeleton = getSkeleton(flagsForSkeleton, s1);
                String s2Skeleton = getSkeleton(flagsForSkeleton, s2);
                if (s1Skeleton.equals(s2Skeleton)) {
                    result |= SINGLE_SCRIPT_CONFUSABLE;
                }
            }
        }

        if (0 != (result & SINGLE_SCRIPT_CONFUSABLE)) {
            // If the two inputs are single script confusable they cannot also be
            // mixed or whole script confusable, according to the UAX39 definitions.
            // So we can skip those tests.
            return result;
        }

        // Two identifiers are whole script confusable if each is of a single script
        // and they are mixed script confusable.
        boolean possiblyWholeScriptConfusables = s1ScriptCount <= 1 && s2ScriptCount <= 1
                && (0 != (this.fChecks & WHOLE_SCRIPT_CONFUSABLE));

        // Mixed Script Check
        if ((0 != (this.fChecks & MIXED_SCRIPT_CONFUSABLE)) || possiblyWholeScriptConfusables) {
            // For getSkeleton(), resetting the SINGLE_SCRIPT_CONFUSABLE flag will get us
            // the mixed script table skeleton, which is what we want.
            // The Any Case / Lower Case bit in the skelton flags was set at the top of the function.
            flagsForSkeleton &= ~SINGLE_SCRIPT_CONFUSABLE;
            String s1Skeleton = getSkeleton(flagsForSkeleton, s1);
            String s2Skeleton = getSkeleton(flagsForSkeleton, s2);
            if (s1Skeleton.equals(s2Skeleton)) {
                result |= MIXED_SCRIPT_CONFUSABLE;
                if (possiblyWholeScriptConfusables) {
                    result |= WHOLE_SCRIPT_CONFUSABLE;
                }
            }
        }
        return result;
    }

    /**
     * Get the "skeleton" for an identifier string. Skeletons are a transformation of the input string; Two strings are
     * confusable if their skeletons are identical. See Unicode UAX 39 for additional information.
     *
     * Using skeletons directly makes it possible to quickly check whether an identifier is confusable with any of some
     * large set of existing identifiers, by creating an efficiently searchable collection of the skeletons.
     *
     * Skeletons are computed using the algorithm and data describe in Unicode UAX 39.
     * The latest proposed update, UAX 39 Version 8 draft 1, says "the tables SL, SA, and ML
     * were still problematic, and discouraged from use in [Uniocde] 7.0.
     * They were thus removed from version 8.0"
     * 
     * In light of this, the default mapping data included with ICU 55 uses the
     * Unicode 7 MA (Multi script Any case) table data for the other type options
     * (Single Script, Any Case), (Single Script, Lower Case) and (Multi Script, Lower Case).
     *
     * @param type
     *            The type of skeleton, corresponding to which of the Unicode confusable data tables to use. The default
     *            is Mixed-Script, Lowercase. Allowed options are SINGLE_SCRIPT_CONFUSABLE and ANY_CASE_CONFUSABLE. The
     *            two flags may be ORed.
     * @param id
     *            The input identifier whose skeleton will be genereated.
     * @return The output skeleton string.
     *
     * @stable ICU 4.6
     */
    public String getSkeleton(int type, String id) {
        int tableMask = 0;
        switch (type) {
        case 0:
            tableMask = ML_TABLE_FLAG;
            break;
        case SINGLE_SCRIPT_CONFUSABLE:
            tableMask = SL_TABLE_FLAG;
            break;
        case ANY_CASE:
            tableMask = MA_TABLE_FLAG;
            break;
        case SINGLE_SCRIPT_CONFUSABLE | ANY_CASE:
            tableMask = SA_TABLE_FLAG;
            break;
        default:
            // *status = U_ILLEGAL_ARGUMENT_ERROR;
            throw new IllegalArgumentException("SpoofChecker.getSkeleton(), bad type value.");
        }

        // Apply the skeleton mapping to the NFD normalized input string
        // Accumulate the skeleton, possibly unnormalized, in a String.

        String nfdId = nfdNormalizer.normalize(id);
        int normalizedLen = nfdId.length();
        StringBuilder skelSB = new StringBuilder();
        for (int inputIndex = 0; inputIndex < normalizedLen;) {
            int c = Character.codePointAt(nfdId, inputIndex);
            inputIndex += Character.charCount(c);
            this.confusableLookup(c, tableMask, skelSB);
        }
        String skelStr = skelSB.toString();
        skelStr = nfdNormalizer.normalize(skelStr);
        return skelStr;
    }


    /**
     *   Equality function. Return true if the two SpoofChecker objects
     *   incorporate the same confusable data and have enabled the same
     *   set of checks.
     *
     *   @param other the SpoofChecker being compared with.
     *   @return true if the two SpoofCheckers are equal.
     *   @internal
     *   @deprecated This API is ICU internal only.
     */
    @Deprecated
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SpoofChecker)) {return false; }
        SpoofChecker otherSC = (SpoofChecker)other;
        if (fSpoofData != otherSC.fSpoofData &&
                fSpoofData != null &&
                !fSpoofData.equals(otherSC.fSpoofData)) {
            return false;
        }
        if (fChecks != otherSC.fChecks) {return false; }
        if (fAllowedLocales != otherSC.fAllowedLocales &&
                fAllowedLocales != null &&
                !fAllowedLocales.equals(otherSC.fAllowedLocales)) {
            return false;
        }
        if (fAllowedCharsSet != otherSC.fAllowedCharsSet &&
                fAllowedCharsSet != null &&
                !fAllowedCharsSet.equals(otherSC.fAllowedCharsSet)) {
            return false;
        }
        if (fRestrictionLevel != otherSC.fRestrictionLevel) {
            return false;
        }
        return true;
     }

    /**
     * This is a stub implementation and not designed for generic use.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    @Override
    public int hashCode() {
        assert false;   // To make sure ICU implementation does not depend on this.
        return 1234;    // Any arbitrary value - for now, using 1234.
    }

    /*
     * Append the confusable skeleton transform for a single code point to a StringBuilder.
     * The string to be appended will between 1 and 18 characters.
     *
     * This is the heart of the confusable skeleton generation implementation.
     *
     * @param tableMask bit flag specifying which confusable table to use. One of SL_TABLE_FLAG, MA_TABLE_FLAG, etc.
     */
    private void confusableLookup(int inChar, int tableMask, StringBuilder dest) {
        // Binary search the spoof data key table for the inChar
        int low = 0;
        int mid = 0;
        int limit = fSpoofData.fCFUKeys.length;
        int midc;
        boolean foundChar = false;
        // [low, limit), i.e low is inclusive, limit is exclusive
        do {
            int delta = (limit - low) / 2;
            mid = low + delta;
            midc = fSpoofData.fCFUKeys[mid] & 0x1fffff;
            if (inChar == midc) {
                foundChar = true;
                break;
            } else if (inChar < midc) {
                limit = mid; // limit is exclusive
            } else {
                // we have checked mid is not the char we looking for, the next char
                // we want to check is (mid + 1)
                low = mid + 1; // low is inclusive
            }
        } while (low < limit);
        if (!foundChar) { // Char not found. It maps to itself.
            dest.appendCodePoint(inChar);
            return;
        }

        boolean foundKey = false;
        int keyFlags = fSpoofData.fCFUKeys[mid] & 0xff000000;
        if ((keyFlags & tableMask) == 0) {
            // We found the right key char, but the entry doesn't pertain to the
            // table we need. See if there is an adjacent key that does
            if (0 != (keyFlags & SpoofChecker.KEY_MULTIPLE_VALUES)) {
                int altMid;
                for (altMid = mid - 1; (fSpoofData.fCFUKeys[altMid] & 0x00ffffff) == inChar; altMid--) {
                    keyFlags = fSpoofData.fCFUKeys[altMid] & 0xff000000;
                    if (0 != (keyFlags & tableMask)) {
                        mid = altMid;
                        foundKey = true;
                        break;
                    }
                }
                if (!foundKey) {
                    for (altMid = mid + 1; (fSpoofData.fCFUKeys[altMid] & 0x00ffffff) == inChar; altMid++) {
                        keyFlags = fSpoofData.fCFUKeys[altMid] & 0xff000000;
                        if (0 != (keyFlags & tableMask)) {
                            mid = altMid;
                            foundKey = true;
                            break;
                        }
                    }
                }
            }
            if (!foundKey) {
                // No key entry for this char & table.
                // The input char maps to itself.
                dest.appendCodePoint(inChar);
                return;
            }
        }

        int stringLen = getKeyLength(keyFlags) + 1;
        int keyTableIndex = mid;

        // Value is either a char (for strings of length 1) or
        // an index into the string table (for longer strings)
        short value = fSpoofData.fCFUValues[keyTableIndex];
        if (stringLen == 1) {
            dest.append((char) value);
            return;
        }

        // String length of 4 from the above lookup is used for all strings of
        // length >= 4.
        // For these, get the real length from the string lengths table,
        // which maps string table indexes to lengths.
        // All strings of the same length are stored contiguously in the string table.
        // 'value' from the lookup above is the starting index for the desired string.

        if (stringLen == 4) {
            boolean dataOK = false;
            for (SpoofData.SpoofStringLengthsElement el: fSpoofData.fCFUStringLengths) {
                if (el.fLastString >= value) {
                    stringLen = el.fStrLength;
                    dataOK = true;
                    break;
                }
            }
            assert(dataOK);
        }

        dest.append(fSpoofData.fCFUStrings, value, value + stringLen);
        return;
    }

    // Implementation for Whole Script tests.
    // Input text is already normalized to NFD
    // Return the set of scripts, each of which can represent something that is
    // confusable with the input text. The script of the input text
    // is included; input consisting of characters from a single script will
    // always produce a result consisting of a set containing that script.
    private void wholeScriptCheck(CharSequence text, ScriptSet result) {
        int inputIdx = 0;
        int c;

        Trie2 table = (0 != (fChecks & ANY_CASE)) ? fSpoofData.fAnyCaseTrie : fSpoofData.fLowerCaseTrie;
        result.setAll();
        while (inputIdx < text.length()) {
            c = Character.codePointAt(text, inputIdx);
            inputIdx = Character.offsetByCodePoints(text, inputIdx, 1);
            int index = table.get(c);
            if (index == 0) {
                // No confusables in another script for this char.
                // TODO: we should change the data to have sets with just the single script
                // bit for the script of this char. Gets rid of this special case.
                // Until then, grab the script from the char and intersect it with the set.
                int cpScript = UScript.getScript(c);
                assert (cpScript > UScript.INHERITED);
                result.intersect(cpScript);
            } else if (index == 1) {
                // Script == Common or Inherited. Nothing to do.
            } else {
                result.intersect(fSpoofData.fScriptSets[index]);
            }
        }
    }

    // IdentifierInfo Cache. IdentifierInfo objects are somewhat expensive to create.
    //  Maintain a one-element cache, which is sufficient to avoid repeatedly
    //  creating new ones unless we get multi-thread concurrency collisions in spoof
    //  check operations, which should be statistically uncommon.

    private IdentifierInfo fCachedIdentifierInfo = null;  // Do not use this directly.

    private IdentifierInfo getIdentifierInfo() {
        IdentifierInfo returnIdInfo = null;
        synchronized (this) {
            returnIdInfo = fCachedIdentifierInfo;
            fCachedIdentifierInfo = null;
        }
        if (returnIdInfo == null) {
            returnIdInfo = new IdentifierInfo();
        }
        return returnIdInfo;
    }


    private void releaseIdentifierInfo(IdentifierInfo idInfo) {
        if (idInfo != null) {
            synchronized (this) {
                if (fCachedIdentifierInfo == null) {
                    fCachedIdentifierInfo = idInfo;
                }
            }
        }
    };

    // Data Members
    private int fChecks;                         // Bit vector of checks to perform.
    private SpoofData fSpoofData;
    private Set<ULocale> fAllowedLocales;        // The Set of allowed locales.
    private UnicodeSet fAllowedCharsSet;         // The UnicodeSet of allowed characters.
    private RestrictionLevel fRestrictionLevel;

    private static Normalizer2 nfdNormalizer = Normalizer2.getNFDInstance();


    // Confusable Mappings Data Structures
    //
    // For the confusable data, we are essentially implementing a map,
    //    key: a code point
    //    value: a string. Most commonly one char in length, but can be more.
    //
    // The keys are stored as a sorted array of 32 bit ints.
    //          bits 0-23    a code point value
    //          bits 24-31   flags
    //             24:    1 if entry applies to SL table
    //             25:    1 if entry applies to SA table
    //             26:    1 if entry applies to ML table
    //             27:    1 if entry applies to MA table
    //             28:    1 if there are multiple entries for this code point.
    //             29-30: length of value string, in UChars.
    //                    values are (1, 2, 3, other)
    //     The key table is sorted in ascending code point order. (not on the
    //     32 bit int value, the flag bits do not participate in the sorting.)
    //
    //     Lookup is done by means of a binary search in the key table.
    //
    // The corresponding values are kept in a parallel array of 16 bit ints.
    // If the value string is of length 1, it is literally in the value array.
    // For longer strings, the value array contains an index into the strings
    // table.
    //
    // String Table:
    //     The strings table contains all of the value strings (those of length two or greater)
    //     concatentated together into one long char (UTF-16) array.
    //
    //     The array is arranged by length of the strings - all strings of the same length
    //     are stored together. The sections are ordered by length of the strings -
    //     all two char strings first, followed by all of the three Char strings, etc.
    //
    //     There is no nul character or other mark between adjacent strings.
    //
    // String Lengths table
    //     The length of strings from 1 to 3 is flagged in the key table.
    //     For strings of length 4 or longer, the string length table provides a
    //     mapping between an index into the string table and the corresponding length.
    //     Strings of these lengths are rare, so lookup time is not an issue.
    //     Each entry consists of
    //        unsigned short      index of the _last_ string with this length
    //        unsigned short      the length

    // Flag bits in the Key entries
    static final int SL_TABLE_FLAG = (1 << 24);
    static final int SA_TABLE_FLAG = (1 << 25);
    static final int ML_TABLE_FLAG = (1 << 26);
    static final int MA_TABLE_FLAG = (1 << 27);
    static final int KEY_MULTIPLE_VALUES = (1 << 28);
    static final int KEY_LENGTH_SHIFT = 29;

    static final int getKeyLength(int x) {
        return (((x) >> 29) & 3);
    }


    // -------------------------------------------------------------------------------------
    //
    // SpoofData
    //
    //   This class corresonds to the ICU SpoofCheck data.
    //
    //   The data can originate with the Binary ICU data that is generated in ICU4C,
    //   or it can originate from source rules that are compiled in ICU4J.
    //
    //   This class does not include the set of checks to be performed, but only
    //     data that is serialized into the ICU binary data.
    //
    //   Because Java cannot easily wrap binaray data like ICU4C, the binary data is
    //     copied into Java structures that are convenient for use by the run time code.
    //
    // ---------------------------------------------------------------------------------------
    private static class SpoofData {

        // The Confusable data, Java data structures for.
        int[]                       fCFUKeys;
        short[]                     fCFUValues;
        SpoofStringLengthsElement[] fCFUStringLengths;
        String                      fCFUStrings;

        // Whole Script Confusable Data
        Trie2                       fAnyCaseTrie;
        Trie2                       fLowerCaseTrie;
        ScriptSet[]                 fScriptSets;

        static class SpoofStringLengthsElement {
            int fLastString;  // index in string table of last string with this length
            int fStrLength;   // Length of strings
            public boolean equals(Object other) {
                if (!(other instanceof SpoofStringLengthsElement)) {
                    return false;
                }
                SpoofStringLengthsElement otherEl = (SpoofStringLengthsElement)other;
                return fLastString == otherEl.fLastString &&
                       fStrLength  == otherEl.fStrLength;
            }
        }

        private static final int DATA_FORMAT = 0x43667520;  // "Cfu "

        private static final class IsAcceptable implements Authenticate {
            // @Override when we switch to Java 6
            public boolean isDataVersionAcceptable(byte version[]) {
                return version[0] == 1;
            }
        }
        private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable();

        private static final class DefaultData {
            private static SpoofData INSTANCE = null;

            static {
                try {
                    INSTANCE = new SpoofData(ICUBinary.getRequiredData("confusables.cfu"));
                } catch (IOException ignored) {
                }
            }
        }

        /**
         * @return instance for Unicode standard data
         */
        static SpoofData getDefault() {
            return DefaultData.INSTANCE;
        }

        // SpoofChecker Data constructor for use from data builder.
        // Initializes a new, empty data area that will be populated later.
        SpoofData() {
        }

        // Constructor for use when creating from prebuilt default data.
        // A ByteBuffer is what the ICU internal data loading functions provide.
        SpoofData(ByteBuffer bytes) throws java.io.IOException {
            ICUBinary.readHeader(bytes, DATA_FORMAT, IS_ACCEPTABLE);
            bytes.mark();
            readData(bytes);
        }

        public boolean equals(Object other) {
            if (!(other instanceof SpoofData)) {
                return false;
            }
            SpoofData otherData = (SpoofData)other;
            if (!Arrays.equals(fCFUKeys, otherData.fCFUKeys)) return false;
            if (!Arrays.equals(fCFUValues, otherData.fCFUValues)) return false;
            if (!Arrays.deepEquals(fCFUStringLengths, otherData.fCFUStringLengths)) return false;
            if (fCFUStrings != otherData.fCFUStrings &&
                    fCFUStrings != null &&
                    !fCFUStrings.equals(otherData.fCFUStrings)) return false;
            if (fAnyCaseTrie != otherData.fAnyCaseTrie &&
                    fAnyCaseTrie != null &&
                    !fAnyCaseTrie.equals(otherData.fAnyCaseTrie)) return false;
            if (fLowerCaseTrie != otherData.fLowerCaseTrie &&
                    fLowerCaseTrie != null &&
                    !fLowerCaseTrie.equals(otherData.fLowerCaseTrie)) return false;
            if (!Arrays.deepEquals(fScriptSets, otherData.fScriptSets)) return false;
            return true;
        }

        // Set the SpoofChecker data from pre-built binary data in a byte buffer.
        // The binary data format is as described for ICU4C spoof data.
        //
        void readData(ByteBuffer bytes) throws java.io.IOException {
            int magic = bytes.getInt();
            if (magic != 0x3845fdef) {
                throw new IllegalArgumentException("Bad Spoof Check Data.");
            }
            @SuppressWarnings("unused")
            int dataFormatVersion      = bytes.getInt();
            @SuppressWarnings("unused")
            int dataLength             = bytes.getInt();

            int CFUKeysOffset          = bytes.getInt();
            int CFUKeysSize            = bytes.getInt();

            int CFUValuesOffset        = bytes.getInt();
            int CFUValuesSize          = bytes.getInt();

            int CFUStringTableOffset   = bytes.getInt();
            int CFUStringTableSize     = bytes.getInt();

            int CFUStringLengthsOffset = bytes.getInt();
            int CFUStringLengthsSize   = bytes.getInt();

            int anyCaseTrieOffset      = bytes.getInt();
            @SuppressWarnings("unused")
            int anyCaseTrieSize        = bytes.getInt();

            int lowerCaseTrieOffset    = bytes.getInt();
            @SuppressWarnings("unused")
            int lowerCaseTrieLength    = bytes.getInt();

            int scriptSetsOffset       = bytes.getInt();
            int scriptSetslength       = bytes.getInt();

            int i;
            fCFUKeys = null;
            fCFUValues = null;
            fCFUStringLengths = null;
            fCFUStrings = null;

            // We have now read the file header, and obtained the position for each
            // of the data items. Now read each in turn, first seeking the
            // input stream to the position of the data item.

            bytes.reset();
            ICUBinary.skipBytes(bytes, CFUKeysOffset);
            fCFUKeys = new int[CFUKeysSize];
            for (i = 0; i < CFUKeysSize; i++) {
                fCFUKeys[i] = bytes.getInt();
            }

            bytes.reset();
            ICUBinary.skipBytes(bytes, CFUValuesOffset);
            fCFUValues = new short[CFUValuesSize];
            for (i = 0; i < CFUValuesSize; i++) {
                fCFUValues[i] = bytes.getShort();
            }

            bytes.reset();
            ICUBinary.skipBytes(bytes, CFUStringTableOffset);
            StringBuffer CFUStringB = new StringBuffer();
            for (i = 0; i < CFUStringTableSize; i++) {
                CFUStringB.append(bytes.getChar());
            }
            fCFUStrings = CFUStringB.toString();

            bytes.reset();
            ICUBinary.skipBytes(bytes, CFUStringLengthsOffset);
            fCFUStringLengths = new SpoofStringLengthsElement[CFUStringLengthsSize];
            for (i = 0; i < CFUStringLengthsSize; i++) {
                fCFUStringLengths[i] = new SpoofStringLengthsElement();
                fCFUStringLengths[i].fLastString = bytes.getShort();
                fCFUStringLengths[i].fStrLength = bytes.getShort();
            }

            bytes.reset();
            ICUBinary.skipBytes(bytes, anyCaseTrieOffset);
            fAnyCaseTrie = Trie2.createFromSerialized(bytes);

            bytes.reset();
            ICUBinary.skipBytes(bytes, lowerCaseTrieOffset);
            fLowerCaseTrie = Trie2.createFromSerialized(bytes);

            bytes.reset();
            ICUBinary.skipBytes(bytes, scriptSetsOffset);
            fScriptSets = new ScriptSet[scriptSetslength];
            for (i = 0; i < scriptSetslength; i++) {
                fScriptSets[i] = new ScriptSet(bytes);
            }
        }

    }

    // -------------------------------------------------------------------------------
    //
    // ScriptSet - Script code bit sets. Used with the whole script confusable data.
    // Used both at data build and at run time.
    // Could almost be a Java BitSet, except that the input and output would
    // be awkward.
    //
    // -------------------------------------------------------------------------------
    static class ScriptSet {
        public ScriptSet() {
        }

        public ScriptSet(ByteBuffer bytes) throws java.io.IOException {
            for (int j = 0; j < bits.length; j++) {
                bits[j] = bytes.getInt();
            }
        }

        public void output(DataOutputStream os) throws java.io.IOException {
            for (int i = 0; i < bits.length; i++) {
                os.writeInt(bits[i]);
            }
        }

        public boolean equals(Object other) {
            if (!(other instanceof ScriptSet)) {
                return false;
            }
            ScriptSet otherSet = (ScriptSet)other;
            return Arrays.equals(bits, otherSet.bits);
        }

        public void Union(int script) {
            int index = script / 32;
            int bit = 1 << (script & 31);
            assert (index < bits.length * 4 * 4);
            bits[index] |= bit;
        }

        @SuppressWarnings("unused")
        public void Union(ScriptSet other) {
            for (int i = 0; i < bits.length; i++) {
                bits[i] |= other.bits[i];
            }
        }

        public void intersect(ScriptSet other) {
            for (int i = 0; i < bits.length; i++) {
                bits[i] &= other.bits[i];
            }
        }

        public void intersect(int script) {
            int index = script / 32;
            int bit = 1 << (script & 31);
            assert (index < bits.length * 4 * 4);
            int i;
            for (i = 0; i < index; i++) {
                bits[i] = 0;
            }
            bits[index] &= bit;
            for (i = index + 1; i < bits.length; i++) {
                bits[i] = 0;
            }
        }

        public void setAll() {
            for (int i = 0; i < bits.length; i++) {
                bits[i] = 0xffffffff;
            }
        }

        @SuppressWarnings("unused")
        public void resetAll() {
            for (int i = 0; i < bits.length; i++) {
                bits[i] = 0;
            }
        }

        public int countMembers() {
            // This bit counter is good for sparse numbers of '1's, which is
            // very much the case that we will usually have.
            int count = 0;
            for (int i = 0; i < bits.length; i++) {
                int x = bits[i];
                while (x != 0) {
                    count++;
                    x &= (x - 1); // AND off the least significant one bit.
                                  // Note - Java integer over/underflow behavior is well defined.
                                  //        0x80000000 - 1 = 0x7fffffff
                }
            }
            return count;
        }

        private int[] bits = new int[6];
    }
}

