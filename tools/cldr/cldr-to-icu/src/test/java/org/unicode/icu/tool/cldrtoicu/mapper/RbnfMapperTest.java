// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static org.unicode.icu.tool.cldrtoicu.mapper.RbnfMapperTest.Access.PRIVATE;
import static org.unicode.icu.tool.cldrtoicu.mapper.RbnfMapperTest.Access.PUBLIC;
import static org.unicode.icu.tool.cldrtoicu.mapper.RbnfMapperTest.Group.DURATION_RULES;
import static org.unicode.icu.tool.cldrtoicu.mapper.RbnfMapperTest.Group.ORDINAL_RULES;
import static org.unicode.icu.tool.cldrtoicu.mapper.RbnfMapperTest.Group.SPELLOUT_RULES;
import static org.unicode.icu.tool.cldrtoicu.testing.IcuDataSubjectFactory.assertThat;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.RbValue;

import com.google.common.base.Ascii;
import com.google.common.base.CaseFormat;

@RunWith(JUnit4.class)
public class RbnfMapperTest {
    enum Access {
        PUBLIC, PRIVATE;

        @Override public String toString() {
            return Ascii.toLowerCase(name());
        }
    }

    // IMPORTANT: The ldml.dtd only defines 3 groups:
    //     NumberingSystemRules, OrdinalRules, SpelloutRules
    // but the "specials" files used by ICU introduce additional group names (e.g. DurationRules)
    // which are strictly speaking invalid according to the DTD.
    enum Group {
        NUMBERING_SYSTEM_RULES, ORDINAL_RULES, SPELLOUT_RULES, DURATION_RULES;

        @Override public String toString() {
            // It's "NumberingSystemRules" not "numberingSystemRules"
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
        }
    }

    @Test
    public void testSingleRuleset() {
        int idx = 0;
        CldrData cldrData = cldrData(
            rbnfRule(SPELLOUT_RULES, "2d-year", PRIVATE, "0", "hundred;", ++idx),
            rbnfRule(SPELLOUT_RULES, "2d-year", PRIVATE, "1", "oh-=%first-set=;", ++idx),
            rbnfRule(SPELLOUT_RULES, "2d-year", PRIVATE, "10", "=%first-set=;", ++idx));

        IcuData icuData = new IcuData("xx", true);
        RbnfMapper.process(icuData, cldrData, Optional.empty());

        assertThat(icuData).hasValuesFor("/RBNFRules/SpelloutRules",
            // Double-% prefix for "private" access.
            RbValue.of("%%2d-year:"),
            RbValue.of("0: hundred;"),
            RbValue.of("1: oh-=%first-set=;"),
            RbValue.of("10: =%first-set=;"));
    }


    @Test
    public void testMultipleRulesets() {
        // Note that input order of these paths shouldn't matter since they are ordered (and thus
        // grouped) by DTD order (relative order matters for values in the same set, but values
        // do not have to grouped together).
        int idx = 0;
        CldrData cldrData = cldrData(
            rbnfRule(SPELLOUT_RULES, "first-set", PUBLIC, "-x", "one;", ++idx),
            rbnfRule(SPELLOUT_RULES, "first-set", PUBLIC, "Inf", "two;", ++idx),
            rbnfRule(SPELLOUT_RULES, "second-set", PUBLIC, "-x", "five;", ++idx),
            rbnfRule(SPELLOUT_RULES, "second-set", PUBLIC, "Inf", "six;", ++idx),
            rbnfRule(SPELLOUT_RULES, "first-set", PUBLIC, "NaN", "three;", ++idx),
            rbnfRule(SPELLOUT_RULES, "first-set", PUBLIC, "0", "four;", ++idx),
            rbnfRule(SPELLOUT_RULES, "second-set", PUBLIC, "NaN", "seven;", ++idx),
            rbnfRule(SPELLOUT_RULES, "second-set", PUBLIC, "0", "eight;", ++idx));

        IcuData icuData = new IcuData("xx", true);
        RbnfMapper.process(icuData, cldrData, Optional.empty());

        assertThat(icuData).hasValuesFor("/RBNFRules/SpelloutRules",
            // Single-% prefix for "public" access.
            RbValue.of("%first-set:"),
            RbValue.of("-x: one;"),
            RbValue.of("Inf: two;"),
            RbValue.of("NaN: three;"),
            RbValue.of("0: four;"),
            // Each "heading" appears once at the start of the section.
            RbValue.of("%second-set:"),
            RbValue.of("-x: five;"),
            RbValue.of("Inf: six;"),
            RbValue.of("NaN: seven;"),
            RbValue.of("0: eight;"));
    }

    @Test
    public void testSpecials() {
        int idx = 0;
        CldrData specials = cldrData(
            rbnfRule(DURATION_RULES, "min", PRIVATE, "0", "0 minutes; 1 minute; =0= minutes;", ++idx),
            rbnfRule(DURATION_RULES, "hr", PRIVATE, "0", "0 hours; 1 hour; =0= hours;", ++idx),
            rbnfRule(DURATION_RULES, "in-numerals", PUBLIC, "0", "=0= sec.;", ++idx),
            rbnfRule(DURATION_RULES, "in-numerals", PUBLIC, "60", "=%%min-sec=;", ++idx),
            rbnfRule(DURATION_RULES, "in-numerals", PUBLIC, "3600", "=%%hr-min-sec=;", ++idx));

        idx = 0;
        CldrData cldrData = cldrData(
            rbnfRule(ORDINAL_RULES, "digits-ordinal", PUBLIC, "-x", "−→→;", ++idx),
            rbnfRule(ORDINAL_RULES, "digits-ordinal", PUBLIC, "0",
                "=#,##0=$(ordinal,one{st}two{nd}few{rd}other{th})$;", ++idx));

        IcuData icuData = new IcuData("xx", true);
        RbnfMapper.process(icuData, cldrData, Optional.of(specials));

        assertThat(icuData).hasValuesFor("/RBNFRules/OrdinalRules",
            RbValue.of("%digits-ordinal:"),
            RbValue.of("-x: \\u2212>>;"),
            RbValue.of("0: =#,##0=$(ordinal,one{st}two{nd}few{rd}other{th})$;"));

        // The headings are sorted in the output ("hr" < "in-numerals" < min").
        assertThat(icuData).hasValuesFor("/RBNFRules/DurationRules",
            RbValue.of("%%hr:"),
            RbValue.of("0: 0 hours; 1 hour; =0= hours;"),
            RbValue.of("%in-numerals:"),
            RbValue.of("0: =0= sec.;"),
            RbValue.of("60: =%%min-sec=;"),
            RbValue.of("3600: =%%hr-min-sec=;"),
            RbValue.of("%%min:"),
            RbValue.of("0: 0 minutes; 1 minute; =0= minutes;"));
    }

    // Note that while this is testing the escaping behaviour, the implementation was largely
    // derived from a mostly undocumented method in the previous converter, and while it behaves
    // the same, it's not entirely obviously why some of the special cases really exist.
    @Test
    public void testEscaping() {
        int idx = 0;
        CldrData cldrData = cldrData(
            rbnfRule(SPELLOUT_RULES, "escaping", PUBLIC, "k1", "\\ Backslash", ++idx),
            rbnfRule(SPELLOUT_RULES, "escaping", PUBLIC, "k2", "←← Arrows →→", ++idx),
            rbnfRule(SPELLOUT_RULES, "escaping", PUBLIC, "k3", "Ü Umlaut", ++idx),
            rbnfRule(SPELLOUT_RULES, "escaping", PUBLIC, "k4", "\uD83D\uDE03 Smiley", ++idx));

        IcuData icuData = new IcuData("xx", true);
        RbnfMapper.process(icuData, cldrData, Optional.empty());

        assertThat(icuData).hasValuesFor("/RBNFRules/SpelloutRules",
            RbValue.of("%escaping:"),
            RbValue.of("k1: \\\\ Backslash"),
            RbValue.of("k2: << Arrows >>"),
            RbValue.of("k3: \\u00DC Umlaut"),
            RbValue.of("k4: \\U0001F603 Smiley"));
    }

    private static CldrData cldrData(CldrValue... values) {
        return CldrDataSupplier.forValues(Arrays.asList(values));
    }

    // Both ruleset and rbnfrule are "ordered" elements, but to mimic the XML below, it's the
    // rbnfrule which needs to have an incrementing sort index:
    //
    // <ruleset type="<set-type>" access="<access>">
    //     <rbnfrule value="<key-1>">value-1</rbnfrule>
    //     <rbnfrule value="<key-2>">value-2</rbnfrule>
    //     <rbnfrule value="<key-3>">value-3</rbnfrule>
    // </ruleset>
    private static CldrValue rbnfRule(
        Group group, String setType, Access access, String key, String value, int ruleIndex) {

        StringBuilder cldrPath = new StringBuilder("//ldml/rbnf");
        appendAttribute(cldrPath.append("/rulesetGrouping"), "type", group);
        // We aren't testing sort index (#N) here, but still need to set it to something.
        cldrPath.append("/ruleset#0");
        appendAttribute(cldrPath, "type", setType);
        appendAttribute(cldrPath, "access", access);
        cldrPath.append("/rbnfrule#").append(ruleIndex);
        appendAttribute(cldrPath, "value", key);
        return CldrValue.parseValue(cldrPath.toString(), value);
    }

    private static void appendAttribute(StringBuilder out, String k, Object v) {
        out.append(String.format("[@%s=\"%s\"]", k, v));
    }
}