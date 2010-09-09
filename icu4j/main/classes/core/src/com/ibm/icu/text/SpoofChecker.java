/*
 ***************************************************************************
 * Copyright (C) 2008-2010, International Business Machines Corporation
 * and others. All Rights Reserved.
 ***************************************************************************
 *
 * Unicode Spoof Detection
 */
package com.ibm.icu.text;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.impl.Trie2;
import com.ibm.icu.impl.Trie2Writable;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.util.ULocale;

/**
 *
 * \brief for Unicode Security and Spoofing Detection.
 *
 * These functions are intended to check strings, typically
 * identifiers of some type, such as URLs, for the presence of
 * characters that are likely to be visually confusing - 
 * for cases where the displayed form of an identifier may
 * not be what it appears to be.
 *
 * Unicode Technical Report #36, http://unicode.org/reports/tr36, and
 * Unicode Technical Standard #39, http://unicode.org/reports/tr39
 * "Unicode security considerations", give more background on 
 * security and spoofing issues with Unicode identifiers.
 * The tests and checks provided by this module implement the recommendations
 * from these Unicode documents.
 *
 * The tests available on identifiers fall into two general categories:
 *   -#  Single identifier tests.  Check whether an identifier is
 *       potentially confusable with any other string, or is suspicious
 *       for other reasons.
 *   -#  Two identifier tests.  Check whether two specific identifiers are confusable.
 *       This does not consider whether either of strings is potentially
 *       confusable with any string other than the exact one specified.
 *
 * The steps to perform confusability testing are
 *   -#  Create a SpoofChecker.Builder
 *   -#  Configure the Builder for the desired set of tests.  The tests that will
 *       be performed are specified by a set of SpoofCheck flags.
 *   -#  Build a SpoofChecker from the Builder.
 *   -#  Perform the checks using the pre-configured SpoofChecker.  The results indicate
 *       which (if any) of the selected tests have identified possible problems with the identifier.
 *       Results are reported as a set of SpoofCheck flags;  this mirrors the form in which
 *       the set of tests to perform was originally specified to the SpoofChecker.
 *
 * A SpoofChecker may be used repeatedly to perform checks on any number of identifiers.
 *
 * Thread Safety: The methods on SpoofChecker objects are thread safe.  
 * The test functions for checking a single identifier, or for testing 
 * whether two identifiers are potentially confusable,  may called concurrently 
 * from multiple threads using the same SpoofChecker instance.
 *
 *
 * Descriptions of the available checks.
 *
 * When testing whether pairs of identifiers are confusable, with the areConfusable()
 * family of functions, the relevant tests are
 *
 *   -# SINGLE_SCRIPT_CONFUSABLE:  All of the characters from the two identifiers are
 *      from a single script, and the two identifiers are visually confusable.
 *   -# MIXED_SCRIPT_CONFUSABLE:  At least one of the identifiers contains characters
 *      from more than one script, and the two identifiers are visually confusable.
 *   -# WHOLE_SCRIPT_CONFUSABLE: Each of the two identifiers is of a single script, but
 *      the the two identifiers are from different scripts, and they are visually confusable.
 *
 * The safest approach is to enable all three of these checks as a group.
 *
 * ANY_CASE is a modifier for the above tests.  If the identifiers being checked can
 * be of mixed case and are used in a case-sensitive manner, this option should be specified.
 *
 * If the identifiers being checked are used in a case-insensitive manner, and if they are
 * displayed to users in lower-case form only, the ANY_CASE option should not be
 * specified.  Confusabality issues involving upper case letters will not be reported.
 *
 * When performing tests on a single identifier, with the check() family of functions,
 * the relevant tests are:
 *
 *    -# MIXED_SCRIPT_CONFUSABLE: the identifier contains characters from multiple
 *       scripts, and there exists an identifier of a single script that is visually confusable.
 *    -# WHOLE_SCRIPT_CONFUSABLE: the identifier consists of characters from a single
 *       script, and there exists a visually confusable identifier.
 *       The visually confusable identifier also consists of characters from a single script.
 *       but not the same script as the identifier being checked.
 *    -# ANY_CASE: modifies the mixed script and whole script confusables tests.  If
 *       specified, the checks will find confusable characters of any case.  
 *       If this flag is not set, the test is performed assuming case folded identifiers.
 *    -# SINGLE_SCRIPT: check that the identifier contains only characters from a
 *       single script.  (Characters from the 'common' and 'inherited' scripts are ignored.)
 *       This is not a test for confusable identifiers
 *    -# INVISIBLE: check an identifier for the presence of invisible characters,
 *       such as zero-width spaces, or character sequences that are
 *       likely not to display, such as multiple occurrences of the same
 *       non-spacing mark.  This check does not test the input string as a whole
 *       for conformance to any particular syntax for identifiers.
 *    -# CHAR_LIMIT: check that an identifier contains only characters from a specified set
 *       of acceptable characters.  See Builder.setAllowedChars() and
 *       Builder.setAllowedLocales().
 *
 *  Note on Scripts:
 *     Characters from the Unicode Scripts "Common" and "Inherited" are ignored when considering
 *     the script of an identifier. Common characters include digits and symbols that
 *     are normally used with text from many different scripts.
 *     
 */

/**
 * The main SpoofChecker class.
 */
public class SpoofChecker {

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
     * @draft ICU 4.6
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
     * @draft ICU 4.6
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
     * @draft ICU 4.6
     */
    public static final int WHOLE_SCRIPT_CONFUSABLE = 4;

    /**
     * Any Case Modifier for confusable identifier tests.
     * 
     * When specified, consider all characters, of any case, when looking for confusables. If ANY_CASE is not specified,
     * identifiers being checked are assumed to have been case folded, and upper case conusable characters will not be
     * checked.
     * 
     * @draft ICU 4.6
     */
    public static final int ANY_CASE = 8;

    /**
     * Check that an identifer contains only characters from a single script (plus chars from the common and inherited
     * scripts.) Applies to checks of a single identifier check only.
     * 
     * @draft ICU 4.6
     */
    public static final int SINGLE_SCRIPT = 16;

    /**
     * Check an identifier for the presence of invisible characters, such as zero-width spaces, or character sequences
     * that are likely not to display, such as multiple occurrences of the same non-spacing mark. This check does not
     * test the input string as a whole for conformance to any particular syntax for identifiers.
     * 
     * @draft ICU 4.6
     */
    public static final int INVISIBLE = 32;

    /**
     * Check that an identifier contains only characters from a specified set of acceptable characters. See
     * Builder.setAllowedChars() and Builder.setAllowedLocales().
     * 
     * @draft ICU 4.6
     */
    public static final int CHAR_LIMIT = 64;

    /**
     * Enable all spoof checks.
     * 
     * @draft ICU 4.6
     */
    public static final int ALL_CHECKS = 0x7f;

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
     * @draft ICU 4.6
     */
    public static class Builder {
        int fMagic; // Internal sanity check.
        int fChecks; // Bit vector of checks to perform.
        SpoofData fSpoofData;
        UnicodeSet fAllowedCharsSet; // The UnicodeSet of allowed characters.
                                     // for this Spoof Checker. Defaults to all chars.
        Set<ULocale> fAllowedLocales; // The list of allowed locales.

        /**
         * Constructor: Create a default Unicode Spoof Checker Builder, configured to perform all checks except for
         * LOCALE_LIMIT and CHAR_LIMIT. Note that additional checks may be added in the future, resulting in the changes
         * to the default checking behavior.
         * 
         * @draft ICU 4.6
         */
        public Builder() {
            fMagic = MAGIC;
            fChecks = ALL_CHECKS;
            fSpoofData = null;
            fAllowedCharsSet = new UnicodeSet(0, 0x10ffff);
            fAllowedLocales = new LinkedHashSet<ULocale>();
        }

        /**
         * Constructor: Create a Spoof Checker Builder, and set the configuration from an existing SpoofChecker.
         * 
         * @param src
         *            The existing checker.
         * @draft ICU 4.6
         */
        public Builder(SpoofChecker src) {
            fMagic = src.fMagic;
            fChecks = src.fChecks;
            fSpoofData = null;
            fAllowedCharsSet = src.fAllowedCharsSet.cloneAsThawed();
            fAllowedLocales = new LinkedHashSet<ULocale>();
            fAllowedLocales.addAll(src.fAllowedLocales);
        }

        /**
         * Create a SpoofChecker with current configuration.
         * 
         * @return SpoofChecker
         * @draft ICU 4.6
         */
        public SpoofChecker build() {
            if (fSpoofData == null) { // read binary file
                try {
                    fSpoofData = SpoofData.getDefault();
                } catch (java.io.IOException e) {
                    return null;
                }
            }
            if (!SpoofData.validateDataVersion(fSpoofData.fRawData)) {
                return null;
            }
            SpoofChecker result = new SpoofChecker();
            result.fMagic = this.fMagic;
            result.fChecks = this.fChecks;
            result.fSpoofData = this.fSpoofData;
            result.fAllowedCharsSet = (UnicodeSet) (this.fAllowedCharsSet.clone());
            result.fAllowedCharsSet.freeze();
            result.fAllowedLocales = this.fAllowedLocales;
            return result;
        }

        /**
         * Specify the source form of the spoof data Spoof Checker. The Three inputs correspond to the Unicode data
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
         * @draft ICU 4.6
         */
        public Builder setData(Reader confusables, Reader confusablesWholeScript) throws ParseException,
                java.io.IOException {
            // Set up a shell of a spoof detector, with empty data.
            fSpoofData = new SpoofData();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream os = new DataOutputStream(bos);
            // Compile the binary data from the source (text) format.
            ConfusabledataBuilder.buildConfusableData(fSpoofData, confusables);
            WSConfusableDataBuilder.buildWSConfusableData(fSpoofData, os, confusablesWholeScript);
            return this;
        }

        /**
         * Specify the set of checks that will be performed by the check functions of this Spoof Checker.
         * 
         * @param checks
         *            The set of checks that this spoof checker will perform. The value is an 'or' of the desired
         *            checks..
         * @return self
         * @draft ICU 4.6
         * 
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
         * @draft ICU 4.6
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
            fAllowedLocales = new LinkedHashSet<ULocale>();
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
            fAllowedLocales.addAll(locales);
            fChecks |= CHAR_LIMIT;
            return this;
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
         * setAllowedLocales() function.
         * 
         * The CHAR_LIMIT test is automatically enabled for this SpoofChecker by this function.
         * 
         * @param chars
         *            A Unicode Set containing the list of characters that are permitted. The incoming set is cloned by
         *            this function, so there are no restrictions on modifying or deleting the UnicodeSet after calling
         *            this function. Note that this clears the allowedLocales set.
         * @return self
         * @draft ICU 4.6
         */
        public Builder setAllowedChars(UnicodeSet chars) {
            fAllowedCharsSet = chars.cloneAsThawed();
            fAllowedLocales = new LinkedHashSet<ULocale>();
            fChecks |= CHAR_LIMIT;
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
         * Internal functions for compililing Whole Script confusable source data into its binary (runtime) form. The
         * binary data format is described in uspoof_impl.h
         */
        private static class WSConfusableDataBuilder {

            // Regular expression for parsing a line from the Unicode file
            // confusablesWholeScript.txt
            // Example Lines:
            // 006F ; Latn; Deva; A # (o) LATIN SMALL LETTER O
            // 0048..0049 ; Latn; Grek; A # [2] (H..I) LATIN CAPITAL LETTER H..LATIN
            // CAPITAL LETTER I
            // | | | |
            // | | | |---- Which table, Any Case or Lower Case (A or L)
            // | | |----------Target script. We need this.
            // | |----------------Src script. Should match the script of the source
            // | code points. Beyond checking that, we don't keep it.
            // |--------------------------------Source code points or range.
            //
            // The expression will match _all_ lines, including erroneous lines.
            // The result of the parse is returned via the contents of the (match)
            // groups.
            static String parseExp =

            "(?m)" + // Multi-line mode
                    "^([ \\t]*(?:#.*?)?)$" + // A blank or comment line. Matches Group
                    // 1.
                    "|^(?:" + // OR
                    "\\s*([0-9A-F]{4,})(?:..([0-9A-F]{4,}))?\\s*;" + // Code point
                    // range. Groups
                    // 2 and 3.
                    "\\s*([A-Za-z]+)\\s*;" + // The source script. Group 4.
                    "\\s*([A-Za-z]+)\\s*;" + // The target script. Group 5.
                    "\\s*(?:(A)|(L))" + // The table A or L. Group 6 or 7
                    "[ \\t]*(?:#.*?)?" + // Trailing commment
                    ")$|" + // OR
                    "^(.*?)$"; // An error line. Group 8.

            // Any line not matching the preceding
            // parts of the expression.will match
            // this, and thus be flagged as an error

            // Extract a regular expression match group into a char * string.
            // The group must contain only invariant characters.
            // Used for script names
            //

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
            static void buildWSConfusableData(SpoofData fSpoofData, DataOutputStream os, Reader confusablesWS)
                    throws ParseException, java.io.IOException {
                Pattern parseRegexp = null;
                StringBuffer input = new StringBuffer();
                int lineNum = 0;

                Vector<BuilderScriptSet> scriptSets = null;
                int rtScriptSetsCount = 2;

                Trie2Writable anyCaseTrie = new Trie2Writable(0, 0);
                Trie2Writable lowerCaseTrie = new Trie2Writable(0, 0);

                // The scriptSets vector provides a mapping from TRIE values to the set
                // of scripts.
                //
                // Reserved TRIE values:
                // 0: Code point has no whole script confusables.
                // 1: Code point is of script Common or Inherited.
                // These code points do not participate in whole script confusable
                // detection.
                // (This is logically equivalent to saying that they contain confusables
                // in all scripts)
                //
                // Because Trie values are indexes into the ScriptSets vector, pre-fill
                // vector positions 0 and 1 to avoid conflicts with the reserved values.
                scriptSets = new Vector<BuilderScriptSet>();
                scriptSets.addElement(null);
                scriptSets.addElement(null);

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
                            bsset = scriptSets.elementAt(setIndex);
                        } else {
                            bsset = new BuilderScriptSet();
                            bsset.codePoint = cp;
                            bsset.trie = table;
                            bsset.sset = new ScriptSet();
                            setIndex = scriptSets.size();
                            bsset.index = setIndex;
                            bsset.rindex = 0;
                            scriptSets.addElement(bsset);
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
                {
                    int duplicateCount = 0;
                    rtScriptSetsCount = 2;
                    for (int outeri = 2; outeri < scriptSets.size(); outeri++) {
                        BuilderScriptSet outerSet = scriptSets.elementAt(outeri);
                        if (outerSet.index != outeri) {
                            // This set was already identified as a duplicate.
                            // It will not be allocated a position in the runtime array
                            // of ScriptSets.
                            continue;
                        }
                        outerSet.rindex = rtScriptSetsCount++;
                        for (int inneri = outeri + 1; inneri < scriptSets.size(); inneri++) {
                            BuilderScriptSet innerSet = scriptSets.elementAt(inneri);
                            if (outerSet.sset.equals(innerSet.sset) && outerSet.sset != innerSet.sset) {
                                innerSet.sset = outerSet.sset;
                                innerSet.index = outeri;
                                innerSet.rindex = outerSet.rindex;
                                duplicateCount++;
                            }
                            // But this doesn't get all. We need to fix the TRIE.
                        }
                    }
                    // printf("Number of distinct script sets: %d\n",
                    // rtScriptSetsCount);
                }

                // Update the Trie values to be reflect the run time script indexes
                // (after duplicate merging).
                // (Trie Values 0 and 1 are reserved, and the corresponding slots in
                // scriptSets
                // are unused, which is why the loop index starts at 2.)
                {
                    for (int i = 2; i < scriptSets.size(); i++) {
                        BuilderScriptSet bSet = scriptSets.elementAt(i);
                        if (bSet.rindex != i) {
                            bSet.trie.set(bSet.codePoint, bSet.rindex);
                        }
                    }
                }

                // For code points with script==Common or script==Inherited,
                // Set the reserved value of 1 into both Tries. These characters do not
                // participate
                // in Whole Script Confusable detection; this reserved value is the
                // means
                // by which they are detected.
                {
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
                }

                // Serialize the data to the Spoof Detector
                {
                    anyCaseTrie.toTrie2_16().serialize(os);
                    lowerCaseTrie.toTrie2_16().serialize(os);

                    fSpoofData.fRawData.fScriptSetsLength = rtScriptSetsCount;
                    int rindex = 2;
                    for (int i = 2; i < scriptSets.size(); i++) {
                        BuilderScriptSet bSet = scriptSets.elementAt(i);
                        if (bSet.rindex < rindex) {
                            // We have already copied this script set to the serialized
                            // data.
                            continue;
                        }
                        assert (rindex == bSet.rindex);
                        bSet.sset.output(os);
                        rindex++;
                    }
                }
            }

            // class BuilderScriptSet. Represents the set of scripts (Script Codes)
            // containing characters that are confusable with one specific
            // code point.
            private static class BuilderScriptSet {
                int codePoint; // The source code point.
                Trie2Writable trie; // Any-case or Lower-case Trie.
                // These Trie tables are the final result of the
                // build. This flag indicates which of the two
                // this set of data is for.
                ScriptSet sset; // The set of scripts itself.

                // Vectors of all B
                int index; // Index of this set in the Build Time vector
                // of script sets.
                int rindex; // Index of this set in the final (runtime)

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
        // An instance of this class exists while the confusable data is being built
        // from source.
        // It encapsulates the intermediate data structures that are used for building.
        // It exports one static function, to do a confusable data build.
        private static class ConfusabledataBuilder {
            private SpoofData fSpoofData;
            private ByteArrayOutputStream bos;
            private DataOutputStream os;
            private Hashtable<Integer, SPUString> fSLTable;
            private Hashtable<Integer, SPUString> fSATable;
            private Hashtable<Integer, SPUString> fMLTable;
            private Hashtable<Integer, SPUString> fMATable;
            private UnicodeSet fKeySet; // A set of all keys (UChar32s) that go into the
            // four mapping tables.

            // The binary data is first assembled into the following four collections,
            // then output to the DataOutputStream os.
            private StringBuffer fStringTable;
            private Vector<Integer> fKeyVec;
            private Vector<Integer> fValueVec;
            private Vector<Integer> fStringLengthsTable;
            private SPUStringPool stringPool;
            private Pattern fParseLine;
            private Pattern fParseHexNum;
            private int fLineNum;

            ConfusabledataBuilder(SpoofData spData, ByteArrayOutputStream bos) {
                this.bos = bos;
                this.os = new DataOutputStream(bos);
                fSpoofData = spData;
                fSLTable = new Hashtable<Integer, SPUString>();
                fSATable = new Hashtable<Integer, SPUString>();
                fMLTable = new Hashtable<Integer, SPUString>();
                fMATable = new Hashtable<Integer, SPUString>();
                fKeySet = new UnicodeSet();
                fKeyVec = new Vector<Integer>();
                fValueVec = new Vector<Integer>();
                stringPool = new SPUStringPool();
            }

            void build(Reader confusables) throws ParseException, java.io.IOException {
                StringBuffer fInput = new StringBuffer();
                WSConfusableDataBuilder.readWholeFileToString(confusables, fInput);

                // Regular Expression to parse a line from Confusables.txt. The
                // expression will match
                // any line. What was matched is determined by examining which capture
                // groups have a match.
                // Capture Group 1: the source char
                // Capture Group 2: the replacement chars
                // Capture Group 3-6 the table type, SL, SA, ML, or MA
                // Capture Group 7: A blank or comment only line.
                // Capture Group 8: A syntactically invalid line. Anything that didn't
                // match before.
                // Example Line from the confusables.txt source file:
                // "1D702 ;	006E 0329 ;	SL	# MATHEMATICAL ITALIC SMALL ETA ... "
                fParseLine = Pattern.compile("(?m)^[ \\t]*([0-9A-Fa-f]+)[ \\t]+;" + // Match
                        // the
                        // source
                        // char
                        "[ \\t]*([0-9A-Fa-f]+" + // Match the replacement char(s)
                        "(?:[ \\t]+[0-9A-Fa-f]+)*)[ \\t]*;" + // (continued)
                        "\\s*(?:(SL)|(SA)|(ML)|(MA))" + // Match the table type
                        "[ \\t]*(?:#.*?)?$" + // Match any trailing #comment
                        "|^([ \\t]*(?:#.*?)?)$" + // OR match empty lines or lines with
                        // only a #comment
                        "|^(.*?)$"); // OR match any line, which catches illegal lines.

                // Regular expression for parsing a hex number out of a space-separated
                // list of them.
                // Capture group 1 gets the number, with spaces removed.
                fParseHexNum = Pattern.compile("\\s*([0-9A-F]+)");

                // Zap any Byte Order Mark at the start of input. Changing it to a space
                // is benign
                // given the syntax of the input.
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

                    StringBuffer mapString = new StringBuffer();
                    while (m.find()) {
                        int c = Integer.parseInt(m.group(1), 16);
                        if (keyChar > 0x10ffff) {
                            throw new ParseException("Confusables, line " + fLineNum + ": Bad code point: "
                                    + Integer.toString(c, 16), matcher.start(2));
                        }
                        mapString.append(c);
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
                // This is done in two steps. First the data is assembled into vectors
                // and strings,
                // for ease of construction, then the contents of these collections are
                // dumped
                // into the actual raw-bytes data storage.

                // Build up the string array, and record the index of each string
                // therein
                // in the (build time only) string pool.
                // Strings of length one are not entered into the strings array.
                // At the same time, build up the string lengths table, which records
                // the
                // position in the string table of the first string of each length >= 4.
                // (Strings in the table are sorted by length)
                stringPool.sort();
                fStringTable = new StringBuffer();
                fStringLengthsTable = new Vector<Integer>();
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
                        // strings of length one do not get an entry in the string
                        // table.
                        // Keep the single string character itself here, which is the
                        // same
                        // convention that is used in the final run-time string table
                        // index.
                        s.fStrTableIndex = s.fStr.charAt(0);
                    } else {
                        if ((strLen > previousStringLength) && (previousStringLength >= 4)) {
                            fStringLengthsTable.addElement(previousStringIndex);
                            fStringLengthsTable.addElement(previousStringLength);
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
                    fStringLengthsTable.addElement(previousStringIndex);
                    fStringLengthsTable.addElement(previousStringLength);
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

                for (int range = 0; range < fKeySet.getRangeCount(); range++) {
                    // It is an oddity of the UnicodeSet API that simply enumerating the
                    // contained
                    // code points requires a nested loop.
                    for (int keyChar = fKeySet.getRangeStart(range); keyChar <= fKeySet.getRangeEnd(range); keyChar++) {
                        addKeyEntry(keyChar, fSLTable, SpoofChecker.SL_TABLE_FLAG);
                        addKeyEntry(keyChar, fSATable, SpoofChecker.SA_TABLE_FLAG);
                        addKeyEntry(keyChar, fMLTable, SpoofChecker.ML_TABLE_FLAG);
                        addKeyEntry(keyChar, fMATable, SpoofChecker.MA_TABLE_FLAG);
                    }
                }

                // Put the assembled data into the flat runtime array
                outputData();

                // All of the intermediate allocated data belongs to the
                // ConfusabledataBuilder object (this), and is deleted by Java GC.
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
                    int key = fKeyVec.elementAt(i);
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
                        fKeyVec.setElementAt(key, i);
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

                fKeyVec.addElement(newKey);
                fValueVec.addElement(newData);

                // If the preceding key entry is for the same key character (but with a
                // different mapping)
                // set the multiple-values flag on it.
                if (keyHasMultipleValues) {
                    int previousKeyIndex = fKeyVec.size() - 2;
                    int previousKey = fKeyVec.elementAt(previousKeyIndex);
                    previousKey |= SpoofChecker.KEY_MULTIPLE_VALUES;
                    fKeyVec.setElementAt(previousKey, previousKeyIndex);
                }
            }

            // From an index into fKeyVec & fValueVec
            // get a String with the corresponding mapping.
            String getMapping(int index) {
                int key = fKeyVec.elementAt(index);
                int value = fValueVec.elementAt(index);
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
                        lastIndexWithLen = fStringLengthsTable.elementAt(i);
                        if (value <= lastIndexWithLen) {
                            length = fStringLengthsTable.elementAt(i + 1);
                            break;
                        }
                    }
                    assert (length >= 3);
                    return fStringTable.substring(value, value + length);
                default:
                    assert (false);
                }
                return new String();
            }

            // Populate the final binary output data array with the compiled data.
            // The confusable data has been compiled and stored in intermediate
            // collections and strings. Copy it from there to the final flat
            // binary array.
            void outputData() throws java.io.IOException {

                SpoofDataHeader rawData = fSpoofData.fRawData;
                // The Key Table
                // While copying the keys to the runtime array,
                // also sanity check that they are sorted.
                int numKeys = fKeyVec.size();
                int i;
                int previousKey = 0;
                rawData.output(os);
                rawData.fCFUKeys = os.size();
                assert (rawData.fCFUKeys == 128);
                rawData.fCFUKeysSize = numKeys;
                for (i = 0; i < numKeys; i++) {
                    int key = fKeyVec.elementAt(i);
                    assert ((key & 0x00ffffff) >= (previousKey & 0x00ffffff));
                    assert ((key & 0xff000000) != 0);
                    os.writeInt(key);
                    previousKey = key;
                }

                // The Value Table, parallels the key table
                int numValues = fValueVec.size();
                assert (numKeys == numValues);
                rawData.fCFUStringIndex = os.size();
                rawData.fCFUStringIndexSize = numValues;
                for (i = 0; i < numValues; i++) {
                    int value = fValueVec.elementAt(i);
                    assert (value < 0xffff);
                    os.writeShort((short) value);
                }

                // The Strings Table.

                int stringsLength = fStringTable.length();
                // Reserve an extra space so the string will be nul-terminated. This is
                // only a convenience, for when debugging; it is not needed otherwise.
                String strings = fStringTable.toString();
                rawData.fCFUStringTable = os.size();
                rawData.fCFUStringTableLen = stringsLength;
                for (i = 0; i < stringsLength; i++) {
                    os.writeChar(strings.charAt(i));
                }

                // The String Lengths Table
                // While copying into the runtime array do some sanity checks on the
                // values
                // Each complete entry contains two fields, an index and an offset.
                // Lengths should increase with each entry.
                // Offsets should be less than the size of the string table.
                int lengthTableLength = fStringLengthsTable.size();
                int previousLength = 0;
                // Note: StringLengthsSize in the raw data is the number of complete
                // entries,
                // each consisting of a pair of 16 bit values, hence the divide by 2.
                rawData.fCFUStringLengthsSize = lengthTableLength / 2;
                rawData.fCFUStringLengths = os.size();
                for (i = 0; i < lengthTableLength; i += 2) {
                    int offset = fStringLengthsTable.elementAt(i);
                    int length = fStringLengthsTable.elementAt(i + 1);
                    assert (offset < stringsLength);
                    assert (length < 40);
                    assert (length > previousLength);
                    os.writeShort((short) offset);
                    os.writeShort((short) length);
                    previousLength = length;
                }

                os.flush();
                DataInputStream is = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()));
                is.mark(Integer.MAX_VALUE);
                fSpoofData.initPtrs(is);
            }

            public static void buildConfusableData(SpoofData spData, Reader confusables) throws java.io.IOException,
                    ParseException {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ConfusabledataBuilder builder = new ConfusabledataBuilder(spData, bos);
                builder.build(confusables);
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
            // Conforms to the type signature for a USortComparator in uvector.h
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
     * Get the set of checks that this Spoof Checker has been configured to perform.
     * 
     * @return The set of checks that this spoof checker will perform.
     * @draft ICU 4.6
     * 
     */
    public int getChecks() {
        return fChecks;
    }

    /**
     * Get a list of locales for the scripts that are acceptable in strings to be checked. If no limitations on scripts
     * have been specified, an empty set will be returned.
     * 
     * setAllowedChars() will reset the list of allowed locales to be empty.
     * 
     * The returned set may not be identical to the originally specified set that is supplied to setAllowedLocales();
     * the information other than languages from the originally specified locales may be omitted.
     * 
     * @return A set of locales corresponding to the acceptable scripts.
     * 
     * @draft ICU 4.6
     */
    public Set<ULocale> getAllowedLocales() {
        return fAllowedLocales;
    }

    /**
     * Get a UnicodeSet for the characters permitted in an identifier. This corresponds to the limits imposed by the Set
     * Allowed Characters functions. Limitations imposed by other checks will not be reflected in the set returned by
     * this function.
     * 
     * The returned set will be frozen, meaning that it cannot be modified by the caller.
     * 
     * @return A UnicodeSet containing the characters that are permitted by the CHAR_LIMIT test.
     * @draft ICU 4.6
     */
    public UnicodeSet getAllowedChars() {
        return fAllowedCharsSet;
    }

    /**
     * Represent the results of a Spoof Check operation. Encapsulates which check(s) have failed and the index in the
     * string where the failure was found.
     * 
     * @draft ICU 4.6
     */
    public static class CheckResult {
        /**
         * Indicate which of the spoof check(s) has failed.
         * 
         * @draft ICU 4.6
         */
        public int checks;
        /**
         * The index of the first string position that failed a check.
         * 
         * @draft ICU 4.6
         */
        public int position;
    }

    /**
     * Check the specified string for possible security issues. The text to be checked will typically be an identifier
     * of some sort. The set of checks to be performed was specified when building the SpoofChecker.
     * 
     * @param text
     *            A String to be checked for possible security issues.
     * @param checkResult
     *            Optional caller provided fill-in parameter. If not null, on return it will be filled.
     * @return True there any issue is found with the input string.
     * @draft ICU 4.6
     */
    public boolean check(String text, CheckResult checkResult) {
        int length = text.length();

        int result = 0;
        int failPos = Integer.MAX_VALUE;

        // A count of the number of non-Common or inherited scripts.
        // Needed for both the SINGLE_SCRIPT and the
        // WHOLE/MIXED_SCIRPT_CONFUSABLE tests.
        // Share the computation when possible. scriptCount == -1 means that we
        // haven't done it yet.
        int scriptCount = -1;

        if (0 != ((this.fChecks) & SINGLE_SCRIPT)) {
            scriptCount = this.scriptScan(text, checkResult);
            // no need to set failPos, it will be set to checkResult.position inside this.scriptScan
            // printf("scriptCount (clipped to 2) = %d\n", scriptCount);
            if (scriptCount >= 2) {
                // Note: scriptCount == 2 covers all cases of the number of
                // scripts >= 2
                result |= SINGLE_SCRIPT;
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
                    if (i < failPos) {
                        failPos = i;
                    }
                    break;
                }
            }
        }

        if (0 != (this.fChecks & (WHOLE_SCRIPT_CONFUSABLE | MIXED_SCRIPT_CONFUSABLE | INVISIBLE))) {
            // These are the checks that need to be done on NFKD input
            String nfkdText = Normalizer.normalize(text, Normalizer.NFKD, 0);

            if (0 != (this.fChecks & INVISIBLE)) {

                // scan for more than one occurence of the same non-spacing mark
                // in a sequence of non-spacing marks.
                int i;
                int c;
                int firstNonspacingMark = 0;
                boolean haveMultipleMarks = false;
                UnicodeSet marksSeenSoFar = new UnicodeSet(); // Set of
                // combining
                // marks in a
                // single
                // combining
                // sequence.

                for (i = 0; i < length;) {
                    // U16_NEXT(nfkdText, i, nfkdLength, c);
                    c = Character.codePointAt(nfkdText, i);
                    i = Character.offsetByCodePoints(nfkdText, i, 1);
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
                        failPos = i;
                        break;
                    }
                    marksSeenSoFar.add(c);
                }
            }

            if (0 != (this.fChecks & (WHOLE_SCRIPT_CONFUSABLE | MIXED_SCRIPT_CONFUSABLE))) {
                // The basic test is the same for both whole and mixed script
                // confusables.
                // Compute the set of scripts that every input character has a
                // confusable in.
                // For this computation an input character is always considered
                // to be
                // confusable with itself in its own script.
                // If the number of such scripts is two or more, and the input
                // consisted of
                // characters all from a single script, we have a whole script
                // confusable.
                // (The two scripts will be the original script and the one that
                // is confusable)
                // If the number of such scripts >= one, and the original input
                // contained characters from
                // more than one script, we have a mixed script confusable. (We
                // can transform
                // some of the characters, and end up with a visually similar
                // string all in
                // one script.)

                if (scriptCount == -1) {
                    scriptCount = this.scriptScan(text, null);
                }

                ScriptSet scripts = new ScriptSet();
                this.wholeScriptCheck(nfkdText, scripts);
                int confusableScriptCount = scripts.countMembers();
                // printf("confusableScriptCount = %d\n",
                // confusableScriptCount);

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
            if (failPos != Integer.MAX_VALUE) {
                checkResult.position = failPos;
            }
        }
        return (0 != result);
    }

    /**
     * Check the specified string for possible security issues. The text to be checked will typically be an identifier
     * of some sort. The set of checks to be performed was specified when building the SpoofChecker.
     * 
     * @param text
     *            A String to be checked for possible security issues.
     * @return True there any issue is found with the input string.
     * @draft ICU 4.6
     */
    public boolean check(String text) {
        return check(text, null);
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
     * @draft ICU 4.6
     */
    public int areConfusable(String s1, String s2) {
        //
        // See section 4 of UAX 39 for the algorithm for checking whether two
        // strings are confusable,
        // and for definitions of the types (single, whole, mixed-script) of
        // confusables.

        // We only care about a few of the check flags. Ignore the others.
        // If no tests relavant to this function have been specified, signal an
        // error.
        // TODO: is this really the right thing to do? It's probably an error on
        // the caller's part, but logically we would just return 0 (no error).
        if ((this.fChecks & (SINGLE_SCRIPT_CONFUSABLE | MIXED_SCRIPT_CONFUSABLE | WHOLE_SCRIPT_CONFUSABLE)) == 0) {
            throw new IllegalArgumentException("No confusable checks are enabled.");
        }
        int flagsForSkeleton = this.fChecks & ANY_CASE;
        String s1Skeleton;
        String s2Skeleton;

        int result = 0;
        int s1ScriptCount = this.scriptScan(s1, null);
        int s2ScriptCount = this.scriptScan(s2, null);

        if (0 != (this.fChecks & SINGLE_SCRIPT_CONFUSABLE)) {
            // Do the Single Script compare.
            if (s1ScriptCount <= 1 && s2ScriptCount <= 1) {
                flagsForSkeleton |= SINGLE_SCRIPT_CONFUSABLE;
                s1Skeleton = getSkeleton(flagsForSkeleton, s1);
                s2Skeleton = getSkeleton(flagsForSkeleton, s2);
                if (s1Skeleton.length() == s2Skeleton.length() && s1Skeleton.equals(s2Skeleton)) {
                    result |= SINGLE_SCRIPT_CONFUSABLE;
                }
            }
        }

        if (0 != (result & SINGLE_SCRIPT_CONFUSABLE)) {
            // If the two inputs are single script confusable they cannot also
            // be
            // mixed or whole script confusable, according to the UAX39
            // definitions.
            // So we can skip those tests.
            return result;
        }

        // Optimization for whole script confusables test: two identifiers are
        // whole script confusable if
        // each is of a single script and they are mixed script confusable.
        boolean possiblyWholeScriptConfusables = s1ScriptCount <= 1 && s2ScriptCount <= 1
                && (0 != (this.fChecks & WHOLE_SCRIPT_CONFUSABLE));

        // Mixed Script Check
        if ((0 != (this.fChecks & MIXED_SCRIPT_CONFUSABLE)) || possiblyWholeScriptConfusables) {
            // For getSkeleton(), resetting the SINGLE_SCRIPT_CONFUSABLE flag
            // will get us
            // the mixed script table skeleton, which is what we want.
            // The Any Case / Lower Case bit in the skelton flags was set at the
            // top of the function.
            flagsForSkeleton &= ~SINGLE_SCRIPT_CONFUSABLE;
            s1Skeleton = getSkeleton(flagsForSkeleton, s1);
            s2Skeleton = getSkeleton(flagsForSkeleton, s2);
            if (s1Skeleton.length() == s2Skeleton.length() && s1Skeleton.equals(s2Skeleton)) {
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
     * @param type
     *            The type of skeleton, corresponding to which of the Unicode confusable data tables to use. The default
     *            is Mixed-Script, Lowercase. Allowed options are SINGLE_SCRIPT_CONFUSABLE and ANY_CASE_CONFUSABLE. The
     *            two flags may be ORed.
     * @param s
     *            The input string whose skeleton will be genereated.
     * @return The output skeleton string.
     * 
     * @draft ICU 4.6
     */
    public String getSkeleton(int type, String s) {
        // TODO: this function could be sped up a bit
        // Skip the input normalization when not needed, work from callers data.
        // It probably won't need normalization.
        if ((type & ~(SINGLE_SCRIPT_CONFUSABLE | ANY_CASE)) != 0) {
            // *status = U_ILLEGAL_ARGUMENT_ERROR;
            return null;
        }

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
            return null;
        }

        // NFKD transform of the user supplied input
        String nfkdInput = Normalizer.normalize(s, Normalizer.NFKD, 0);
        int normalizedLen = nfkdInput.length();

        // Apply the skeleton mapping to the NFKD normalized input string
        // Accumulate the skeleton, possibly unnormalized, in a String.
        int inputIndex = 0;
        StringBuilder skelStr = new StringBuilder();
        while (inputIndex < normalizedLen) {
            int c;
            c = Character.codePointAt(nfkdInput, inputIndex);
            inputIndex = Character.offsetByCodePoints(nfkdInput, inputIndex, 1);
            this.confusableLookup(c, tableMask, skelStr);
        }

        String result = skelStr.toString();
        String normedResult;

        // Check the skeleton for NFKD, normalize it if needed.
        // Unnormalized results should be very rare.
        if (!Normalizer.isNormalized(result, Normalizer.NFKD, 0)) {
            normedResult = Normalizer.normalize(result, Normalizer.NFKD, 0);
            result = normedResult;
        }
        return result;
    }

    /*
     * Append the confusable skeleton transform for a single code point to a StringBuilder. The string to be appended
     * will between 1 and 18 characters.
     * 
     * This is the heart of the confusable skeleton generation implementation.
     * 
     * @param tableMask bit flag specifying which confusable table to use. One of SL_TABLE_FLAG, MA_TABLE_FLAG, etc.
     */
    private void confusableLookup(int inChar, int tableMask, StringBuilder dest) {
        // Binary search the spoof data key table for the inChar
        int low = 0;
        int mid = 0;
        int limit = fSpoofData.fRawData.fCFUKeysSize;
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
                // we have checked mid is not the char we looking for, the next
                // char
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
        // All strings of the same length are stored contiguously in the string
        // table.
        // 'value' from the lookup above is the starting index for the desired
        // string.

        int ix;
        if (stringLen == 4) {
            int stringLengthsLimit = fSpoofData.fRawData.fCFUStringLengthsSize;
            for (ix = 0; ix < stringLengthsLimit; ix++) {
                if (fSpoofData.fCFUStringLengths[ix].fLastString >= value) {
                    stringLen = fSpoofData.fCFUStringLengths[ix].fStrLength;
                    break;
                }
            }
            assert (ix < stringLengthsLimit);
        }

        assert (value + stringLen < fSpoofData.fRawData.fCFUStringTableLen);
        dest.append(fSpoofData.fCFUStrings, value, stringLen);
        return;
    }

    // WholeScript and MixedScript check implementation.
    // Implementation for Whole Script tests.
    // Return the test bit flag to be ORed into the eventual user return value
    // if a Spoof opportunity is detected.
    // Input text is already normalized to NFKD
    // Return the set of scripts, each of which can represent something that is
    // confusable with the input text. The script of the input text
    // is included; input consisting of characters from a single script will
    // always produce a result consisting of a set containing that script.
    void wholeScriptCheck(CharSequence text, ScriptSet result) {
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

    /**
     * Scan a string to determine how many scripts it includes. Ignore characters with script=Common and
     * scirpt=Inherited.
     * 
     * @param text
     *            The char text to be scanned
     * @param checkResult
     *            Optional caller provided fill-in parameter. If not null, on return it will be filled. set to the first
     *            input postion at which a second script was encountered, ignoring Common and Inherited.
     * @return the number of (non-common,inherited) scripts encountered, clipped to a max of two.
     * @internal
     */
    int scriptScan(CharSequence text, CheckResult checkResult) {
        int inputIdx = 0;
        int c;
        int scriptCount = 0;
        int lastScript = UScript.INVALID_CODE;
        int sc = UScript.INVALID_CODE;
        while ((inputIdx < text.length()) && scriptCount < 2) {
            c = Character.codePointAt(text, inputIdx);
            inputIdx = Character.offsetByCodePoints(text, inputIdx, 1);
            sc = UScript.getScript(c);
            if (sc == UScript.COMMON || sc == UScript.INHERITED || sc == UScript.UNKNOWN) {
                continue;
            }
            if (sc != lastScript) {
                scriptCount++;
                lastScript = sc;
            }
        }
        if (scriptCount == 2 && checkResult != null) {
            checkResult.position = inputIdx;
        }
        return scriptCount;
    }

    // Data Members
    private int fMagic; // Internal sanity check.
    private int fChecks; // Bit vector of checks to perform.
    private SpoofData fSpoofData;
    private Set<ULocale> fAllowedLocales; // The Set of allowed locales.
    private UnicodeSet fAllowedCharsSet; // The UnicodeSet of allowed characters.

    // for this Spoof Checker. Defaults to all chars.
    //
    // Confusable Mappings Data Structures
    //
    // For the confusable data, we are essentially implementing a map,
    // key: a code point
    // value: a string. Most commonly one char in length, but can be more.
    //
    // The keys are stored as a sorted array of 32 bit ints.
    // bits 0-23 a code point value
    // bits 24-31 flags
    // 24: 1 if entry applies to SL table
    // 25: 1 if entry applies to SA table
    // 26: 1 if entry applies to ML table
    // 27: 1 if entry applies to MA table
    // 28: 1 if there are multiple entries for this code point.
    // 29-30: length of value string, in UChars.
    // values are (1, 2, 3, other)
    // The key table is sorted in ascending code point order. (not on the
    // 32 bit int value, the flag bits do not participate in the sorting.)
    //
    // Lookup is done by means of a binary search in the key table.
    //
    // The corresponding values are kept in a parallel array of 16 bit ints.
    // If the value string is of length 1, it is literally in the value array.
    // For longer strings, the value array contains an index into the strings
    // table.
    //
    // String Table:
    // The strings table contains all of the value strings (those of length two
    // or greater)
    // concatentated together into one long char (UTF-16) array.
    //
    // The array is arranged by length of the strings - all strings of the same
    // length
    // are stored together. The sections are ordered by length of the strings -
    // all two char strings first, followed by all of the three Char strings,
    // etc.
    //
    // There is no nul character or other mark between adjacent strings.
    //
    // String Lengths table
    // The length of strings from 1 to 3 is flagged in the key table.
    // For strings of length 4 or longer, the string length table provides a
    // mapping between an index into the string table and the corresponding
    // length.
    // Strings of these lengths are rare, so lookup time is not an issue.
    // Each entry consists of
    // short index of the _last_ string with this length
    // short the length

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

    // ---------------------------------------------------------------------------------------
    //
    // Raw Binary Data Formats, as loaded from the ICU data file,
    // or as built by the builder.
    //
    // ---------------------------------------------------------------------------------------
    private static class SpoofDataHeader {
        int fMagic; // (0x8345fdef)
        byte[] fFormatVersion = new byte[4]; // Data Format. Same as the value in
        // class UDataInfo
        // if there is one associated with this data.
        int fLength; // Total lenght in bytes of this spoof data,
        // including all sections, not just the header.

        // The following four sections refer to data representing the confusable
        // data
        // from the Unicode.org data from "confusables.txt"

        int fCFUKeys; // byte offset to Keys table (from SpoofDataHeader *)
        int fCFUKeysSize; // number of entries in keys table (32 bits each)

        // TODO: change name to fCFUValues, for consistency.
        int fCFUStringIndex; // byte offset to String Indexes table
        int fCFUStringIndexSize; // number of entries in String Indexes table (16 bits each)
                                 // (number of entries must be same as in Keys table

        int fCFUStringTable; // byte offset of String table
        int fCFUStringTableLen; // length of string table (in 16 bit UChars)

        int fCFUStringLengths; // byte offset to String Lengths table
        int fCFUStringLengthsSize; // number of entries in lengths table. (2 x 16 bits each)

        // The following sections are for data from confusablesWholeScript.txt
        int fAnyCaseTrie; // byte offset to the serialized Any Case Trie
        int fAnyCaseTrieLength; // Length (bytes) of the serialized Any Case Trie

        int fLowerCaseTrie; // byte offset to the serialized Lower Case Trie
        int fLowerCaseTrieLength; // Length (bytes) of the serialized Lower Case Trie

        int fScriptSets; // byte offset to array of ScriptSets
        int fScriptSetsLength; // Number of ScriptSets (24 bytes each)

        // The following sections are for data from xidmodifications.txt
        int[] unused = new int[15]; // Padding, Room for Expansion

        public SpoofDataHeader() {
        }

        public SpoofDataHeader(DataInputStream dis) throws IOException {
            int i;
            fMagic = dis.readInt();
            for (i = 0; i < fFormatVersion.length; i++) {
                fFormatVersion[i] = dis.readByte();
            }
            fLength = dis.readInt();
            fCFUKeys = dis.readInt();
            fCFUKeysSize = dis.readInt();
            fCFUStringIndex = dis.readInt();
            fCFUStringIndexSize = dis.readInt();
            fCFUStringTable = dis.readInt();
            fCFUStringTableLen = dis.readInt();
            fCFUStringLengths = dis.readInt();
            fCFUStringLengthsSize = dis.readInt();
            fAnyCaseTrie = dis.readInt();
            fAnyCaseTrieLength = dis.readInt();
            fLowerCaseTrie = dis.readInt();
            fLowerCaseTrieLength = dis.readInt();
            fScriptSets = dis.readInt();
            fScriptSetsLength = dis.readInt();
            for (i = 0; i < unused.length; i++) {
                unused[i] = dis.readInt();
            }
        }

        public void output(DataOutputStream os) throws java.io.IOException {
            int i;
            os.writeInt(fMagic);
            for (i = 0; i < fFormatVersion.length; i++) {
                os.writeByte(fFormatVersion[i]);
            }
            os.writeInt(fLength);
            os.writeInt(fCFUKeys);
            os.writeInt(fCFUKeysSize);
            os.writeInt(fCFUStringIndex);
            os.writeInt(fCFUStringIndexSize);
            os.writeInt(fCFUStringTable);
            os.writeInt(fCFUStringTableLen);
            os.writeInt(fCFUStringLengths);
            os.writeInt(fCFUStringLengthsSize);
            os.writeInt(fAnyCaseTrie);
            os.writeInt(fAnyCaseTrieLength);
            os.writeInt(fLowerCaseTrie);
            os.writeInt(fLowerCaseTrieLength);
            os.writeInt(fScriptSets);
            os.writeInt(fScriptSetsLength);
            for (i = 0; i < unused.length; i++) {
                os.writeInt(unused[i]);
            }
        }
    }

    // -------------------------------------------------------------------------------------
    // SpoofData
    //
    // A small class that wraps the raw (was memory mapped in the C world) spoof data.
    // Nothing in this class includes state that is specific to any particular
    // SpoofDetector object.
    // ---------------------------------------------------------------------------------------
    private static class SpoofData {
        // getDefault() - return a wrapper around the spoof data that is
        // baked into the default ICU data.
        // Load standard ICU spoof data.
        public static SpoofData getDefault() throws java.io.IOException {
            // TODO: Cache it. Lazy create, keep until cleanup.
            InputStream is = com.ibm.icu.impl.ICUData.getRequiredStream(com.ibm.icu.impl.ICUResourceBundle.ICU_BUNDLE
                    + "/confusables.cfu");
            SpoofData This = new SpoofData(is);
            return This;
        }

        // SpoofChecker Data constructor for use from data builder.
        // Initializes a new, empty data area that will be populated later.
        public SpoofData() {
            // The spoof header should already be sized to be a multiple of 16
            // bytes.
            // Just in case it's not, round it up.

            fRawData = new SpoofDataHeader();

            fRawData.fMagic = SpoofChecker.MAGIC;
            fRawData.fFormatVersion[0] = 1;
            fRawData.fFormatVersion[1] = 0;
            fRawData.fFormatVersion[2] = 0;
            fRawData.fFormatVersion[3] = 0;
        }

        // Constructor for use when creating from prebuilt default data.
        // A InputStream is what the ICU internal data loading functions provide.
        public SpoofData(InputStream is) throws java.io.IOException {
            // Seek past the ICU data header.
            // TODO: verify that the header looks good.
            DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
            dis.skip(0x80);
            assert (dis.markSupported());
            dis.mark(Integer.MAX_VALUE);

            fRawData = new SpoofDataHeader(dis);
            initPtrs(dis);
        }

        // Check raw SpoofChecker Data Version compatibility.
        // Return true it looks good.
        static boolean validateDataVersion(SpoofDataHeader rawData) {
            if (rawData == null || rawData.fMagic != SpoofChecker.MAGIC || rawData.fFormatVersion[0] > 1
                    || rawData.fFormatVersion[1] > 0) {
                return false;
            }
            return true;
        }

        // build SpoofChecker from DataInputStream
        // read from binay data input stream
        // initialize the pointers from this object to the raw data.
        // Initialize the pointers to the various sections of the raw data.
        //
        // This function is used both during the Trie building process (multiple
        // times, as the individual data sections are added), and
        // during the opening of a SpoofChecker Checker from prebuilt data.
        //
        // The pointers for non-existent data sections (identified by an offset of
        // 0) are set to null.
        void initPtrs(DataInputStream dis) throws java.io.IOException {
            int i;
            fCFUKeys = null;
            fCFUValues = null;
            fCFUStringLengths = null;
            fCFUStrings = null;

            // the binary file from C world is memory-mapped, each section of data
            // is align-ed to 16-bytes boundary, to make the code more robust we call
            // reset()/skip() which essensially seek() to the correct offset.
            dis.reset();
            dis.skip(fRawData.fCFUKeys);
            if (fRawData.fCFUKeys != 0) {
                fCFUKeys = new int[fRawData.fCFUKeysSize];
                for (i = 0; i < fRawData.fCFUKeysSize; i++) {
                    fCFUKeys[i] = dis.readInt();
                }
            }

            dis.reset();
            dis.skip(fRawData.fCFUStringIndex);
            if (fRawData.fCFUStringIndex != 0) {
                fCFUValues = new short[fRawData.fCFUStringIndexSize];
                for (i = 0; i < fRawData.fCFUStringIndexSize; i++) {
                    fCFUValues[i] = dis.readShort();
                }
            }

            dis.reset();
            dis.skip(fRawData.fCFUStringTable);
            if (fRawData.fCFUStringTable != 0) {
                fCFUStrings = new char[fRawData.fCFUStringTableLen];
                for (i = 0; i < fRawData.fCFUStringTableLen; i++) {
                    fCFUStrings[i] = dis.readChar();
                }
            }

            dis.reset();
            dis.skip(fRawData.fCFUStringLengths);
            if (fRawData.fCFUStringLengths != 0) {
                fCFUStringLengths = new SpoofStringLengthsElement[fRawData.fCFUStringLengthsSize];
                for (i = 0; i < fRawData.fCFUStringLengthsSize; i++) {
                    fCFUStringLengths[i] = new SpoofStringLengthsElement();
                    fCFUStringLengths[i].fLastString = dis.readShort();
                    fCFUStringLengths[i].fStrLength = dis.readShort();
                }
            }

            dis.reset();
            dis.skip(fRawData.fAnyCaseTrie);
            if (fAnyCaseTrie == null && fRawData.fAnyCaseTrie != 0) {
                fAnyCaseTrie = Trie2.createFromSerialized(dis);
            }
            dis.reset();
            dis.skip(fRawData.fLowerCaseTrie);
            if (fLowerCaseTrie == null && fRawData.fLowerCaseTrie != 0) {
                fLowerCaseTrie = Trie2.createFromSerialized(dis);
            }

            dis.reset();
            dis.skip(fRawData.fScriptSets);
            if (fRawData.fScriptSets != 0) {
                fScriptSets = new ScriptSet[fRawData.fScriptSetsLength];
                for (i = 0; i < fRawData.fScriptSetsLength; i++) {
                    fScriptSets[i] = new ScriptSet(dis);
                }
            }
        }

        SpoofDataHeader fRawData;

        // Confusable data
        int[] fCFUKeys;
        short[] fCFUValues;
        SpoofStringLengthsElement[] fCFUStringLengths;
        char[] fCFUStrings;

        // Whole Script Confusable Data
        Trie2 fAnyCaseTrie;
        Trie2 fLowerCaseTrie;
        ScriptSet[] fScriptSets;

        private static class SpoofStringLengthsElement {
            short fLastString; // index in string table of last string with this length
            short fStrLength; // Length of strings
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
    private static class ScriptSet {
        public ScriptSet() {
        }

        public ScriptSet(DataInputStream dis) throws java.io.IOException {
            for (int j = 0; j < bits.length; j++) {
                bits[j] = dis.readInt();
            }
        }

        public void output(DataOutputStream os) throws java.io.IOException {
            for (int i = 0; i < bits.length; i++) {
                os.writeInt(bits[i]);
            }
        }

        public boolean equals(ScriptSet other) {
            for (int i = 0; i < bits.length; i++) {
                if (bits[i] != other.bits[i]) {
                    return false;
                }
            }
            return true;
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
                while (x > 0) {
                    count++;
                    x &= (x - 1); // and off the least significant one bit.
                }
            }
            return count;
        }

        private int[] bits = new int[6];
    }
}
