// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.rbbi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.impl.UCharacterName;
import com.ibm.icu.impl.UCharacterNameChoice;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

/**
 * RBBI Monkey Test. Ported from ICU4C test/intltest/rbbimonkeytest.cpp.
 * This is the newer, data driven monkey test. It is completely separate from the
 * older class RBBITestMonkey.
 */

@RunWith(JUnit4.class)
public class RBBIMonkeyTest extends CoreTestFmwk {


    //  class CharClass    Represents a single character class from the source break rules.
    //                     Inherits from UObject because instances are adopted by UHashtable, which ultimately
    //                     deletes them using hash's object deleter function.

    static class CharClass  {
        String         fName;
        String         fOriginalDef;    // set definition as it appeared in user supplied rules.
        String         fExpandedDef;    // set definition with any embedded named sets replaced by their defs, recursively.
        UnicodeSet     fSet;
        CharClass(String name, String originalDef, String expandedDef, UnicodeSet set) {
            fName = name;
            fOriginalDef = originalDef;
            fExpandedDef = expandedDef;
            fSet = set;
        };
    }


    // class BreakRule    Struct-like class represents a single rule from a set of break rules.
    //                    Each rule has the set definitions expanded, and
    //                    is compiled to a regular expression.

    static class BreakRule {
        String    fName;                      // Name of the rule.
        String    fRule;                      // Rule expression, excluding the name, as written in user source.
        String    fExpandedRule;              // Rule expression after expanding the set definitions.
        Matcher   fRuleMatcher;               // Regular expression that matches the rule.
        boolean   fInitialMatchOnly = false;  // True if rule begins with '^', meaning no chaining.
    };


    // class BreakRules    represents a complete set of break rules, possibly tailored,
    //                     compiled from testdata break rules.

    static class BreakRules {
        BreakRules(RBBIMonkeyImpl monkeyImpl) {
            fMonkeyImpl = monkeyImpl;
            fBreakRules = new ArrayList<>();
            fType = BreakIterator.KIND_TITLE;
            fCharClasses = new HashMap<>();
            fCharClassList = new ArrayList<>();
            fDictionarySet = new UnicodeSet();

            // Match an alpha-numeric identifier in a rule. Will be a set name.
            // Use negative look-behind to exclude non-identifiers, mostly property names or values.
            fSetRefsMatcher = Pattern.compile(
                    "(?<!\\{[ \\t]{0,4})" +
                    "(?<!=[ \\t]{0,4})" +
                    "(?<!\\[:[ \\t]{0,4})" +
                    "(?<!\\\\)" +
                    "(?<![A-Za-z0-9_])" +
                    "([A-Za-z_][A-Za-z0-9_]*)").     // The char class name
                    matcher("");

            // Match comments and blank lines. Matches will be replaced with "", stripping the comments from the rules.
            fCommentsMatcher = Pattern.compile("" +
                    "(^|(?<=;))"   +                // Start either at start of line, or just after a ';' (look-behind for ';')
                    "[ \\t]*+"     +                //   Match white space.
                    "(#.*)?+"      +                //   Optional # plus whatever follows
                    "$").                           //   new-line at end of line.
                    matcher("");

            // Match (initial parse) of a character class definition line.
            fClassDefMatcher = Pattern.compile("" +
                    "[ \\t]*"           +                    // leading white space
                    "([A-Za-z_][A-Za-z0-9_]*)" +             // The char class name
                    "[ \\t]*=[ \\t]*"   +                    //   =
                    "(.*?)"  +                               // The char class UnicodeSet expression
                    "[ \\t]*;$").                            // ; <end of line>
                    matcher("");

            // Match (initial parse) of a break rule line.
            fRuleDefMatcher = Pattern.compile("" +
                    "[ \\t]*"           +                     // leading white space
                    "([A-Za-z_][A-Za-z0-9_.]*)" +             // The rule name
                    "[ \\t]*:[ \\t]*"   +                     //   :
                    "(.*?)"   +                               // The rule definition
                    "[ \\t]*;$").                             // ; <end of line>
                    matcher("");

            // Match a property expression, either [:xxx:] or \p{...}
            fPropertyMatcher = Pattern.compile("" +
                    "\\[:.*?:]|\\\\(?:p|P)\\{.*?\\}").
                    matcher("");


        }

        /**
         * Create the expanded definition for this char class,
         * replacing any set references with the corresponding definition.
         */
        CharClass  addCharClass(String name, String definition) {
            StringBuffer expandedDef = new StringBuffer();
            fSetRefsMatcher.reset(definition);
            while (fSetRefsMatcher.find()) {
                String sname = fSetRefsMatcher.group(/*"ClassName"*/ 1);
                CharClass snameClass = fCharClasses.get(sname);
                String expansionForName = snameClass != null ? snameClass.fExpandedDef : sname;

                fSetRefsMatcher.appendReplacement(expandedDef, "");
                expandedDef.append(expansionForName);
            }
            fSetRefsMatcher.appendTail(expandedDef);
            String expandedDefString = expandedDef.toString();

            if (fMonkeyImpl.fDumpExpansions) {
                System.out.printf("addCharClass(\"%s\"\n", name);
                System.out.printf("             %s\n", definition);
                System.out.printf("expandedDef: %s\n", expandedDefString);
            }

            // Verify that the expanded set definition is valid.

            UnicodeSet s;
            try {
                s = new UnicodeSet(expandedDefString, UnicodeSet.IGNORE_SPACE);
            } catch (java.lang.IllegalArgumentException e) {
                System.err.printf("%s: error %s creating UnicodeSet %s", fMonkeyImpl.fRuleFileName, e.toString(), name);
                throw e;
            }

            // Get an expanded equivalent pattern from the UnicodeSet.
            // This removes set difference operators, which would fail if passed through to Java regex.

            StringBuffer expandedPattern = new StringBuffer();
            s._generatePattern(expandedPattern, true);
            expandedDefString = expandedPattern.toString();
            if (fMonkeyImpl.fDumpExpansions) {
                System.out.printf("expandedDef2: %s\n", expandedDefString);
            }

            CharClass cclass = new CharClass(name, definition, expandedDefString, s);
            CharClass previousClass = fCharClasses.put(name, cclass);

            if (previousClass != null) {
                // TODO: decide whether or not to allow redefinitions.
                //       Can be convenient in some cases.
                // String msg = String.format("%s: Redefinition of character class %s\n",
                //         fMonkeyImpl.fRuleFileName, cclass.fName);
                // System.err.println(msg);
                // throw new IllegalArgumentException(msg);
            }
            return cclass;

        };


        void addRule(String  name, String  definition) {
            BreakRule  thisRule = new BreakRule();
            StringBuffer expandedDefsRule = new StringBuffer();
            thisRule.fName = name;
            thisRule.fRule = definition;

            // Expand the char class definitions within the rule.
            fSetRefsMatcher.reset(definition);
            while (fSetRefsMatcher.find()) {
                String sname = fSetRefsMatcher.group(/*"ClassName"*/ 1);
                CharClass nameClass = fCharClasses.get(sname);
                if (nameClass == null) {
                    System.err.printf("char class \"%s\" unrecognized in rule \"%s\"\n", sname, definition);
                }
                String expansionForName = nameClass != null ? nameClass.fExpandedDef : sname;
                fSetRefsMatcher.appendReplacement(expandedDefsRule, "");
                expandedDefsRule.append(expansionForName);
            }
            fSetRefsMatcher.appendTail(expandedDefsRule);

            // Replace any property expressions, \p{...} or [:...:] with an equivalent expansion,
            // obtained from ICU UnicodeSet. Need to do this substitution because Java regex
            // does not recognize all properties, and because Java's definitions are likely
            // older than ICU's.

            StringBuffer expandedRule = new StringBuffer();
            fPropertyMatcher.reset(expandedDefsRule);
            while (fPropertyMatcher.find()) {
                String prop = fPropertyMatcher.group();
                UnicodeSet propSet = new UnicodeSet("[" + prop + "]");
                StringBuffer propExpansion = new StringBuffer();
                propSet._generatePattern(propExpansion, true);
                fPropertyMatcher.appendReplacement(expandedRule, propExpansion.toString());
            }
            fPropertyMatcher.appendTail(expandedRule);

            // If rule begins with a '^' rule chaining is disallowed.
            // Strip off the '^' from the rule expression, and set the flag.
            if (expandedRule.charAt(0) == '^') {
                thisRule.fInitialMatchOnly = true;
                expandedRule.deleteCharAt(0);
                expandedRule = new StringBuffer(expandedRule.toString().trim());
            }

            //   Replace any [^negated sets] with equivalent flattened sets generated by
            //   ICU UnicodeSet. [^ ...] in Java Regex character classes does not apply
            //   to any nested classes. Variable substitution in rules produces
            //   nested sets that [^negation] needs to apply to.

            StringBuffer ruleWithFlattenedSets = new StringBuffer();
            int idx = 0;
            while (idx<expandedRule.length()) {
                int setOpenPos = expandedRule.indexOf("[^", idx);
                if (setOpenPos < 0) {
                    break;
                }
                if (setOpenPos > idx) {
                    // Move anything from the source rule preceding the [^ into the processed rule, unchanged.
                    ruleWithFlattenedSets.append(expandedRule.substring(idx,  setOpenPos));
                }
                int nestingLevel = 1;
                boolean haveNesting = false;
                int setClosePos;
                for (setClosePos = setOpenPos + 2; nestingLevel > 0 && setClosePos<expandedRule.length(); ++setClosePos) {
                    char c = expandedRule.charAt(setClosePos);
                    if (c == '\\') {
                        ++setClosePos;
                    } else if (c == '[') {
                        ++nestingLevel;
                        haveNesting = true;
                    } else if (c == ']') {
                        --nestingLevel;
                    }
                }
                if (haveNesting && nestingLevel == 0) {
                    // Found one, a negated set that includes interior nested sets.
                    // Create an ICU UnicodeSet from the source pattern, and obtain an
                    // equivalent flattened pattern from that.
                    UnicodeSet uset = new UnicodeSet(expandedRule.substring(setOpenPos, setClosePos), true);
                    uset._generatePattern(ruleWithFlattenedSets, true);
                } else {
                    // The [^ set definition did not include any nested sets.
                    // Copy the original definition without change.
                    // Java regular expressions will handle it without needing to recast it.
                    if (nestingLevel > 0) {
                        // Error case of an unclosed character class expression.
                        // Java regex will also eventually flag the error.
                        System.err.printf("No closing ] found in rule %s\n", name);
                    }
                    ruleWithFlattenedSets.append(expandedRule.substring(setOpenPos, setClosePos));
                }
                idx = setClosePos;
            }

            if (idx < expandedRule.length()) {
                ruleWithFlattenedSets.append(expandedRule.substring(idx, expandedRule.length()));
            }

            thisRule.fExpandedRule = ruleWithFlattenedSets.toString();

            // Replace the divide sign (\u00f7) with a regular expression named capture.
            // When running the rules, a match that includes this group means we found a break position.

            // thisRule.fExpandedRule = thisRule.fExpandedRule.replace("÷", "(?<BreakPosition>)");
            thisRule.fExpandedRule = thisRule.fExpandedRule.replace("÷", "()");
            if (thisRule.fExpandedRule.indexOf("÷") != -1) {
                String msg = String.format("%s Rule %s contains multiple ÷ signs", fMonkeyImpl.fRuleFileName, name);
                System.err.println(msg);
                throw new IllegalArgumentException(msg);
            }

            // UAX break rule set definitions can be empty, just [].
            // Regular expression set expressions don't accept this. Substitute with [a&&[^a]], which
            // also matches nothing.

            thisRule.fExpandedRule = thisRule.fExpandedRule.replace("[]", "[a&&[^a]]");

            // Change Unicode escape syntax for compatibility with Java regular expressions
            //    \udddd     => \x{dddd}
            //    \U00hhhhhh => \x{hhhhhh}

             thisRule.fExpandedRule = thisRule.fExpandedRule.replaceAll("\\\\u([0-9A-Fa-f]{4})", "\\\\x{$1}");
             thisRule.fExpandedRule = thisRule.fExpandedRule.replaceAll("\\\\U00([0-9A-Fa-f]{6})", "\\\\x{$1}");

            // Escape any literal '#' in the rule expression. Without escaping, these introduce a comment.
            // UnicodeSet._generatePattern() inserts un-escaped "#"s

            thisRule.fExpandedRule = thisRule.fExpandedRule.replace("#", "\\#");
            if (fMonkeyImpl.fDumpExpansions) {
                System.out.printf("fExpandedRule: %s\n", thisRule.fExpandedRule);
            }

            // Compile a regular expression for this rule.

            try {
                thisRule.fRuleMatcher = Pattern.compile(thisRule.fExpandedRule, Pattern.COMMENTS | Pattern.DOTALL).matcher("");
            } catch (PatternSyntaxException e) {
                System.err.printf("%s: Error creating regular expression for rule %s. Expansion is \n\"%s\"",
                        fMonkeyImpl.fRuleFileName, name, thisRule.fExpandedRule);
                throw e;
            }

            // Put this new rule into the vector of all Rules.

            fBreakRules.add(thisRule);
        };

        @SuppressWarnings("unused")
        private static String hexToCodePoint(String hex) {
            int cp = Integer.parseInt(hex, 16);
            return new StringBuilder().appendCodePoint(cp).toString();
        }


        boolean setKeywordParameter(String keyword, String value) {
            if (keyword.equals("locale")) {
                fLocale = new ULocale(value);
                return true;
            }
            if (keyword.equals("type")) {
                if (value.equals("grapheme")) {
                    fType = BreakIterator.KIND_CHARACTER;
                } else if (value.equals("word")) {
                    fType = BreakIterator.KIND_WORD;
                } else if (value.equals("line")) {
                    fType = BreakIterator.KIND_LINE;
                } else if (value.equals("sentence")) {
                    fType = BreakIterator.KIND_SENTENCE;
                } else {
                    String msg = String.format("%s: Unrecognized break type %s", fMonkeyImpl.fRuleFileName, value);
                    System.err.println(msg);
                    throw new IllegalArgumentException(msg);
                }
                return true;
            }
            return false;
        }


        RuleBasedBreakIterator createICUBreakIterator() {
            BreakIterator bi;
            switch(fType) {
                case BreakIterator.KIND_CHARACTER:
                    bi = (BreakIterator.getCharacterInstance(fLocale));
                    break;
                case BreakIterator.KIND_WORD:
                    bi = (BreakIterator.getWordInstance(fLocale));
                    break;
                case BreakIterator.KIND_LINE:
                    bi = (BreakIterator.getLineInstance(fLocale));
                    break;
                case BreakIterator.KIND_SENTENCE:
                    bi = (BreakIterator.getSentenceInstance(fLocale));
                    break;
                default:
                    String msg = String.format("%s: Bad break iterator type of %d", fMonkeyImpl.fRuleFileName, fType);
                    System.err.println(msg);
                    throw new IllegalArgumentException(msg);
            }
            return (RuleBasedBreakIterator)bi;

        };



        void compileRules(String rules) {
            int lineNumber = 0;
            for (String line: rules.split("\\r?\\n")) {
                ++lineNumber;
                // Strip comment lines.
                fCommentsMatcher.reset(line);
                line = fCommentsMatcher.replaceFirst("");
                if (line.isEmpty()) {
                    continue;
                }

                // Recognize character class definition and keyword lines
                fClassDefMatcher.reset(line);
                if (fClassDefMatcher.matches()) {
                    String className = fClassDefMatcher.group(/*"ClassName"*/ 1);
                    String classDef  = fClassDefMatcher.group(/*"ClassDef"*/ 2);
                    if (fMonkeyImpl.fDumpExpansions) {
                        System.out.printf("scanned class: %s = %s\n", className, classDef);
                    }
                    if (setKeywordParameter(className, classDef)) {
                        // The scanned item was "type = ..." or "locale = ...", etc.
                        //   which are not actual character classes.
                        continue;
                    }
                    addCharClass(className, classDef);
                    continue;
                }

                // Recognize rule lines.
                fRuleDefMatcher.reset(line);
                if (fRuleDefMatcher.matches()) {
                    String ruleName = fRuleDefMatcher.group(/*"RuleName"*/ 1);
                    String ruleDef  = fRuleDefMatcher.group(/*"RuleDef"*/ 2);
                    if (fMonkeyImpl.fDumpExpansions) {
                        System.out.printf("scanned rule: %s : %s\n", ruleName, ruleDef);
                    }
                    addRule(ruleName, ruleDef);
                    continue;
                }

                String msg = String.format("Unrecognized line in rule file %s:%d \"%s\"",
                        fMonkeyImpl.fRuleFileName, lineNumber, line);
                System.err.println(msg);
                throw new IllegalArgumentException(msg);
            }

            // Build the vector of char classes, omitting the dictionary class if there is one.
            // This will be used when constructing the random text to be tested.

            // Also compute the "other" set, consisting of any characters not included in
            // one or more of the user defined sets.

            UnicodeSet otherSet = new UnicodeSet(0, 0x10ffff);

            for (Map.Entry<String, CharClass> el: fCharClasses.entrySet()) {
                String ccName = el.getKey();
                CharClass cclass = el.getValue();

                // System.out.printf("    Adding %s\n", ccName);
                if (!ccName.equals(cclass.fName)) {
                    throw new IllegalArgumentException(
                            String.format("%s: internal error, set names (%s, %s) inconsistent.\n",
                                    fMonkeyImpl.fRuleFileName, ccName, cclass.fName));
                }
                otherSet.removeAll(cclass.fSet);
                if (ccName.equals("dictionary")) {
                    fDictionarySet = cclass.fSet;
                } else {
                    fCharClassList.add(cclass);
                }
            }

            if (!otherSet.isEmpty()) {
                // System.out.printf("have an other set.\n");
                CharClass cclass = addCharClass("__Others", otherSet.toPattern(true));
                fCharClassList.add(cclass);
            }

        };

        CharClass getClassForChar(int c) {
            for (CharClass cc: fCharClassList) {
                if (cc.fSet.contains(c)) {
                    return cc;
                }
            }
            return null;
        };


        RBBIMonkeyImpl          fMonkeyImpl;        // Pointer back to the owning MonkeyImpl instance.
        List<BreakRule>         fBreakRules;        // Contents are of type (BreakRule *).

        Map<String, CharClass>  fCharClasses;       // Key is the set name.
        //                                          // Value is the corresponding CharClass
        List<CharClass>         fCharClassList;     // Char Classes, same contents as fCharClasses values,

        UnicodeSet              fDictionarySet;     // Dictionary set, empty if none is defined.
        ULocale                 fLocale;
        int                     fType;              // BreakItererator.KIND_WORD, etc.


        Matcher fSetRefsMatcher;
        Matcher fCommentsMatcher;
        Matcher fClassDefMatcher;
        Matcher fRuleDefMatcher;
        Matcher fPropertyMatcher;
    };




    // class MonkeyTestData    represents a randomly synthesized test data string together
    //                         with the expected break positions obtained by applying
    //                         the test break rules.

    static class MonkeyTestData{

        void set(BreakRules rules, ICU_Rand rand) {
            int dataLength = 1000;   // length of test data to generate, in code points.

            // Fill the test string with random characters.
            // First randomly pick a char class, then randomly pick a character from that class.
            // Exclude any characters from the dictionary set.

            // System.out.println("Populating Test Data");
            fRandomSeed = rand.getSeed();         // Save initial seed for use in error messages,
                                                  // allowing recreation of failing data.
            fBkRules = rules;
            StringBuilder newString = new StringBuilder();
            for (int n=0; n<dataLength;) {
                int charClassIndex = rand.next() % rules.fCharClassList.size();
                CharClass cclass = rules.fCharClassList.get(charClassIndex);
                if (cclass.fSet.size() == 0) {
                    // Some rules or tailorings do end up with empty char classes.
                    continue;
                }
                int charIndex = rand.next() % cclass.fSet.size();
                int c = cclass.fSet.charAt(charIndex);
                if (/*Character.isBmpCodePoint(c)*/ c<=0x0ffff && Character.isLowSurrogate((char)c) &&
                        newString.length() > 0 && Character.isHighSurrogate(newString.charAt(newString.length()-1))) {
                    // Character classes may contain unpaired surrogates, e.g. Grapheme_Cluster_Break = Control.
                    // Don't let random unpaired surrogates combine in the test data because they might
                    // produce an unwanted dictionary character.
                    continue;
                }

                if (!rules.fDictionarySet.contains(c)) {
                    newString.appendCodePoint(c);
                    ++n;
                }
            }
            fString = newString.toString();

            // Init the expectedBreaks, actualBreaks and ruleForPosition.
            // Expected and Actual breaks are one longer than the input string; a true value
            // will indicate a boundary preceding that position.

            fActualBreaks    = new boolean[fString.length()+1];
            fExpectedBreaks  = new boolean[fString.length()+1];
            fRuleForPosition = new int[fString.length()+1];
            f2ndRuleForPos   = new int[fString.length()+1];

            int expectedBreakCount = 0;

            // Apply reference rules to find the expected breaks.

            fExpectedBreaks[0] = true;       // Force an expected break before the start of the text.
                                             // ICU always reports a break there.
                                             // The reference rules do not have a means to do so.
            int strIdx = 0;
            boolean initialMatch = true;     // True at start of text, and immediately after each boundary,
            //                               // for control over rule chaining.

            while (strIdx < fString.length()) {
                BreakRule matchingRule = null;
                boolean hasBreak = false;
                int ruleNum = 0;
                int matchStart = 0;
                int matchEnd = 0;
                for (ruleNum=0; ruleNum<rules.fBreakRules.size(); ruleNum++) {
                    BreakRule rule = rules.fBreakRules.get(ruleNum);
                    if (rule.fInitialMatchOnly && !initialMatch) {
                        // Skip checking this '^' rule. (No rule chaining)
                        continue;
                    }
                    rule.fRuleMatcher.reset(fString.substring(strIdx));
                    if (rule.fRuleMatcher.lookingAt()) {
                        // A candidate rule match, check further to see if we take it or continue to check other rules.
                        // Matches of zero or one code point count only if they also specify a break.
                        matchStart = strIdx;
                        matchEnd = strIdx + rule.fRuleMatcher.end();
                        hasBreak = BreakGroupStart(rule.fRuleMatcher) >= 0;
                        if (hasBreak ||
                                (matchStart < fString.length() && fString.offsetByCodePoints(matchStart, 1) < matchEnd)) {
                            matchingRule = rule;
                            break;
                        }
                    }
                }
                if (matchingRule == null) {
                    // No reference rule matched. This is an error in the rules that should never happen.
                    String msg = String.format("%s: No reference rules matched at position %d. ",
                            rules.fMonkeyImpl.fRuleFileName, strIdx);
                    System.err.println(msg);
                    dump(strIdx);
                    throw new IllegalArgumentException(msg);
                }
                if (matchingRule.fRuleMatcher.group().length() == 0) {
                    // Zero length rule match. This is also an error in the rule expressions.
                    String msg = String.format("%s:%s: Zero length rule match at %d.",
                            rules.fMonkeyImpl.fRuleFileName, matchingRule.fName, strIdx);
                    System.err.println(msg);
                    dump(strIdx);
                    throw new IllegalArgumentException(msg);
                }

                // Record which rule matched over the length of the match.
                for (int i = matchStart; i < matchEnd; i++) {
                    if (fRuleForPosition[i] == 0) {
                        fRuleForPosition[i] = ruleNum;
                    } else {
                        f2ndRuleForPos[i] = ruleNum;
                    }
                }

                // Break positions appear in rules as a matching named capture of zero length at the break position,
                //   the adjusted pattern contains (?<BreakPosition>)
                if (hasBreak) {
                    int breakPos = strIdx + BreakGroupStart(matchingRule.fRuleMatcher);
                    fExpectedBreaks[breakPos] = true;
                    expectedBreakCount++;
                    // System.out.printf("recording break at %d\n", breakPos);
                    // For the next iteration, pick up applying rules immediately after the break,
                    // which may differ from end of the match. The matching rule may have included
                    // context following the boundary that needs to be looked at again.
                    strIdx = breakPos;
                    initialMatch = true;
                } else {
                    // Original rule didn't specify a break.
                    // Continue applying rules starting on the last code point of this match.
                    int updatedStrIdx = fString.offsetByCodePoints(matchEnd, -1);
                    if (updatedStrIdx == matchStart) {
                        // Match was only one code point, no progress if we continue.
                        // Shouldn't get here, case is filtered out at top of loop.
                        throw new IllegalArgumentException(String.format("%s: Rule %s internal error.",
                                rules.fMonkeyImpl.fRuleFileName, matchingRule.fName));
                    }
                    strIdx = updatedStrIdx;
                    initialMatch = false;
                }
            }
            if (expectedBreakCount >= fString.length()) {
                throw new IllegalArgumentException(String.format("expectedBreakCount (%d) should be less than the test string length (%d).",
                        expectedBreakCount, fString.length()));
            }
        };

        // Helper function to find the starting index of a match of the "BreakPosition" named capture group.
        // @param m: a Java regex Matcher that has completed a matching operation.
        // @return m.start("BreakPosition),
        //         or -1 if there is no such group, or the group did not participate in the match.
        //
        // TODO: this becomes m.start("BreakPosition") with Java 8.
        //       In the mean time, assume that the only zero-length capturing group in
        //       a reference rule expression is the "BreakPosition" that corresponds to a "÷".

        static int BreakGroupStart(Matcher m) {
            for (int groupNum=1; groupNum <= m.groupCount(); ++groupNum) {
                String group = m.group(groupNum);
                if (group == null) {
                    continue;
                }
                if (group.equals("")) {
                    // assert(m.end(groupNum) == m.end("BreakPosition"));
                    return m.start(groupNum);
                }
            }
            return -1;
        }

        void dump(int around) {
            System.out.print("\n"
                    +        "         char                        break  Rule                     Character\n"
                    +        "   pos   code   class                 R I   name                     name\n"
                    +        "---------------------------------------------------------------------------------------------\n");

            int start;
            int end;

            if (around == -1) {
                start = 0;
                end = fString.length();
            } else {
                // Display context around a failure.
                try {
                    start = fString.offsetByCodePoints(around, -30);
                } catch (Exception e) {
                    start = 0;
                }
                try {
                    end = fString.offsetByCodePoints(around, +30);
                } catch (Exception e) {
                    end = fString.length();
                }
            }

            for (int charIdx = start; charIdx < end; charIdx=fString.offsetByCodePoints(charIdx, 1)) {
                int c = fString.codePointAt(charIdx);
                CharClass cc = fBkRules.getClassForChar(c);

                BreakRule rule = fBkRules.fBreakRules.get(fRuleForPosition[charIdx]);
                String secondRuleName = "";
                if (f2ndRuleForPos[charIdx] > 0) {
                    secondRuleName = fBkRules.fBreakRules.get(f2ndRuleForPos[charIdx]).fName;
                }
                String cName = UCharacterName.INSTANCE.getName(c, UCharacterNameChoice.EXTENDED_CHAR_NAME);

                System.out.printf("  %4d %6x   %-20s  %c %c   %-10s %-10s    %s\n",
                        charIdx, c, cc.fName,
                        fExpectedBreaks[charIdx] ? '*' : '.',
                        fActualBreaks[charIdx] ? '*' : '.',
                        rule.fName, secondRuleName, cName
                        );
                }

        };

        void clearActualBreaks() {
            Arrays.fill(fActualBreaks, false);
        }


        int               fRandomSeed;        // The initial seed value from the random number generator.
        BreakRules        fBkRules;           // The break rules used to generate this data.
        String            fString;            // The text.
        boolean           fExpectedBreaks[];  // Breaks as found by the reference rules.
                                              //     Parallel to fString. true if break preceding.
        boolean           fActualBreaks[];    // Breaks as found by ICU break iterator.
        int               fRuleForPosition[]; // Index into BreakRules.fBreakRules of rule that applied at each position.
                                              // Also parallel to fString.
        int               f2ndRuleForPos[];   // As above. A 2nd rule applies when the preceding rule
                                              //   didn't cause a break, and a subsequent rule match starts
                                              //   on the last code point of the preceding match.

    }


    // class RBBIMonkeyImpl     holds (some indirectly) everything associated with running a monkey
    //                          test for one set of break rules.
    //

    static class RBBIMonkeyImpl extends Thread {

        void setup(String ruleFile) {
            fRuleFileName = ruleFile;
            openBreakRules(ruleFile);
            fRuleSet = new BreakRules(this);
            fRuleSet.compileRules(fRuleCharBuffer);
            fBI = fRuleSet.createICUBreakIterator();
            fTestData = new MonkeyTestData();
        };

        void openBreakRules(String fileName) {
            StringBuilder testFileBuf = new StringBuilder();
            InputStream is = null;
            String filePath = "break_rules/" + fileName;
            try {
                is = RBBIMonkeyImpl.class.getResourceAsStream(filePath);
                if (is == null) {
                    errln("Could not open test data file " + fileName);
                    return;
                }
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                try {
                    int c;
                    int count = 0;
                    for (;;) {
                        c = isr.read();
                        if (c < 0) {
                            break;
                        }
                        count++;
                        if (c == 0xFEFF && count == 1) {
                            // BOM in the test data file. Discard it.
                            continue;
                        }
                       testFileBuf.appendCodePoint(c);
                    }
                } finally {
                    isr.close();
                }
                } catch (IOException e) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
                errln(e.toString());
            }
            fRuleCharBuffer =  testFileBuf.toString();  /* the file as a String */
        }

        class MonkeyException extends RuntimeException  {
            private static final long serialVersionUID = 1L;
            public int fPosition;    // Position of the failure in the test data.
            MonkeyException(String description, int pos) {
                super(description);
                fPosition = pos;
            }
        }

        @Override
        public void run() {
            int errorCount = 0;
            if (fBI == null) {
                fErrorMsgs.append("Unable to run test because fBI is null.\n");
                return;
            }
            for (long loopCount = 0; fLoopCount < 0 || loopCount < fLoopCount; loopCount++) {
                try {
                    fTestData.set(fRuleSet, fRandomGenerator);
                    // fTestData.dump(-1);
                    testForwards();
                    testPrevious();
                    testFollowing();
                    testPreceding();
                    testIsBoundary();
                } catch (MonkeyException e) {
                    String formattedMsg = String.format(
                            "%s at index %d. VM Arguments to reproduce: -Drules=%s -Dseed=%d -Dloop=1 -Dverbose=1 \"\n",
                            e.getMessage(), e.fPosition, fRuleFileName, fTestData.fRandomSeed);
                    System.err.print(formattedMsg);
                    if (fVerbose) {
                        fTestData.dump(e.fPosition);
                    }
                    fErrorMsgs.append(formattedMsg);
                    if (++errorCount > 10) {
                        return;
                    }
                }
                if (fLoopCount < 0 && loopCount % 100 == 0) {
                    System.err.print(".");
                }
            }
        }

        enum CheckDirection {
            FORWARD,
            REVERSE
        };

        void testForwards() {
            fTestData.clearActualBreaks();
            fBI.setText(fTestData.fString);
            int previousBreak = -2;
            for (int bk=fBI.first(); bk != BreakIterator.DONE; bk=fBI.next()) {
                if (bk <= previousBreak) {
                    throw new MonkeyException("Break Iterator Stall", bk);
                }
                if (bk < 0 || bk > fTestData.fString.length()) {
                    throw new MonkeyException("Boundary out of bounds", bk);
                }
                fTestData.fActualBreaks[bk] = true;
            }
            checkResults("testForwards", CheckDirection.FORWARD);
        };


       void testFollowing() {
           fTestData.clearActualBreaks();
           fBI.setText(fTestData.fString);
           int nextBreak = -1;
           for (int i=-1 ; i<fTestData.fString.length(); ++i) {
               int bk = fBI.following(i);
               if (bk == BreakIterator.DONE && i == fTestData.fString.length()) {
                   continue;
               }
               if (bk == nextBreak && bk > i) {
                   // i is in the gap between two breaks.
                   continue;
               }
               if (i == nextBreak && bk > nextBreak) {
                   fTestData.fActualBreaks[bk] = true;
                   nextBreak = bk;
                   continue;
               }
               throw new MonkeyException("following(i)", i);
           }
           checkResults("testFollowing", CheckDirection.FORWARD);
        };


        void testPrevious() {
            fTestData.clearActualBreaks();
            fBI.setText(fTestData.fString);
            int previousBreak = Integer.MAX_VALUE;
            for (int bk=fBI.last(); bk != BreakIterator.DONE; bk=fBI.previous()) {
                 if (bk >= previousBreak) {
                     throw new MonkeyException("Break Iterator Stall", bk);
                }
                if (bk < 0 || bk > fTestData.fString.length()) {
                    throw new MonkeyException("Boundary out of bounds", bk);
                }
                fTestData.fActualBreaks[bk] = true;
            }
            checkResults("testPrevius", CheckDirection.REVERSE);
        };


        /**
         * Given an index into a string, if it refers to the trail surrogate of a surrogate pair,
         * adjust it to point to the lead surrogate, which is the start of the code point.
         * @param s the String.
         * @param i the initial index
         * @return the adjusted index
         */
        private int getChar32Start(String s, int i) {
            if (i > 0 && i < s.length() &&
                    Character.isLowSurrogate(s.charAt(i)) && Character.isHighSurrogate(s.charAt(i-1))) {
                --i;
            }
            return i;
        }


        void testPreceding() {
            fTestData.clearActualBreaks();
            fBI.setText(fTestData.fString);
            int nextBreak = fTestData.fString.length()+1;
            for (int i=fTestData.fString.length()+1 ; i>=0; --i) {
                int bk = fBI.preceding(i);
                // System.err.printf("testPreceding() i:%d  bk:%d  nextBreak:%d\n", i, bk, nextBreak);
                if (bk == BreakIterator.DONE && i == 0) {
                    continue;
                }
                if (bk == nextBreak && bk < i) {
                    // i is in the gap between two breaks.
                    continue;
                }
                if (i<fTestData.fString.length() && getChar32Start(fTestData.fString, i) < i) {
                    // i indexes to a trailing surrogate.
                    // Break Iterators treat an index to either half as referring to the supplemental code point,
                    // with preceding going to some preceding code point.
                    if (fBI.preceding(i) != fBI.preceding(getChar32Start(fTestData.fString, i))) {
                        throw new MonkeyException("preceding of trailing surrogate error", i);
                    }
                    continue;
                }
                if (i == nextBreak && bk < nextBreak) {
                    fTestData.fActualBreaks[bk] = true;
                    nextBreak = bk;
                    continue;
                }
                throw new MonkeyException("preceding(i)", i);
            }
            checkResults("testPreceding", CheckDirection.REVERSE);

        };


        void testIsBoundary() {
            fTestData.clearActualBreaks();
            fBI.setText(fTestData.fString);
            for (int i=fTestData.fString.length(); i>=0; --i) {
                if (fBI.isBoundary(i)) {
                    fTestData.fActualBreaks[i] = true;
                }
            }
            checkResults("testForwards", CheckDirection.FORWARD);
        };


        void checkResults(String msg, CheckDirection direction) {
            if (direction == CheckDirection.FORWARD) {
                for (int i=0; i<=fTestData.fString.length(); ++i) {
                    if (fTestData.fExpectedBreaks[i] != fTestData.fActualBreaks[i]) {
                        throw new MonkeyException(msg, i);
                    }
                }
            } else {
                for (int i=fTestData.fString.length(); i>=0; i--) {
                    if (fTestData.fExpectedBreaks[i] != fTestData.fActualBreaks[i]) {
                        throw new MonkeyException(msg, i);
                    }
                }
            }

        };

        String                 fRuleCharBuffer;         // source file contents of the reference rules.
        BreakRules             fRuleSet;
        RuleBasedBreakIterator fBI;
        MonkeyTestData         fTestData;
        ICU_Rand               fRandomGenerator;
        String                 fRuleFileName;
        boolean                fVerbose;                 // True to do long dump of failing data.
        int                    fLoopCount;
        int                    fErrorCount;

        boolean                fDumpExpansions;          // Debug flag to output expanded form of rules and sets.
        StringBuilder          fErrorMsgs = new StringBuilder();

    }

    //  Test parameters, specified via Java properties.
    //
    //  rules=file_name   Name of file containing the reference rules.
    //  seed=nnnnn        Random number starting seed.
    //                    Setting the seed allows errors to be reproduced.
    //  loop=nnn          Looping count.  Controls running time.
    //                    -1:  run forever.
    //                     0 or greater:  run length.
    //  expansions        debug option, show expansions of rules and sets.
    //  verbose           Display details of the failure.
    //
    // Parameters are passed to the JVM on the command line, or
    // via the Eclipse Run Configuration settings, arguments tab, VM parameters.
    // For example,
    //      -ea -Drules=line.txt -Dloop=-1
    //
    @Test
    public void TestMonkey() {
        String tests[] = {"grapheme.txt", "word.txt", "line.txt", "line_cj.txt", "sentence.txt", "line_normal.txt",
                "line_normal_cj.txt", "line_loose.txt", "line_loose_cj.txt", "word_POSIX.txt"
        };

        String testNameFromParams = getProperty("rules");

        if (testNameFromParams != null) {
            tests = new String[] {testNameFromParams};
        }

        int loopCount = getIntProperty("loop", isQuick() ? 100 : 5000);
        boolean dumpExpansions =  getBooleanProperty("expansions", false);
        boolean verbose = getBooleanProperty("verbose", false);
        int seed = getIntProperty("seed", 1);

        List<RBBIMonkeyImpl> startedTests = new ArrayList<>();

        // Monkey testing is multi-threaded.
        // Each set of break rules to be tested is run in a separate thread.
        // Each thread/set of rules gets a separate RBBIMonkeyImpl object.

        for (String testName: tests) {
            logln(String.format("beginning testing of %s", testName));

            RBBIMonkeyImpl test = new RBBIMonkeyImpl();

            test.fDumpExpansions = dumpExpansions;
            test.fVerbose = verbose;
            test.fRandomGenerator = new ICU_Rand(seed);
            test.fLoopCount = loopCount;
            test.setup(testName);

            test.start();
            startedTests.add(test);
        }

        StringBuilder errors = new StringBuilder();
        for (RBBIMonkeyImpl test: startedTests) {
            try {
                test.join();
                errors.append(test.fErrorMsgs);
            } catch (InterruptedException e) {
                errors.append(e + "\n");
            }
        }
        String errorMsgs = errors.toString();
        assertEquals(errorMsgs, "", errorMsgs);

    }


}
