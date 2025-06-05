// © 2019-2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

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

import com.google.common.base.CaseFormat;

@RunWith(JUnit4.class)
public class RbnfMapperTest {
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
        CldrData cldrData = cldrData(rbnfRules(SPELLOUT_RULES, "\n"
                + "%%2d-year:\n"
                + "0: hundred;\n"
                + "1: oh-=%first-set=;\n"
                + "10: =%first-set=;\n"));

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
        CldrData cldrData = cldrData(rbnfRules(SPELLOUT_RULES, "\n"
                // Single-% prefix for "public" access.
                + "%first-set:\n"
                + "-x: one;\n"
                + "Inf: two;\n"
                + "NaN: three;\n"
                + "0: four;\n"
                // Each "heading" appears once at the start of the section.
                + "%second-set:\n"
                + "-x: five;\n"
                + "Inf: six;\n"
                + "NaN: seven;\n"
                + "0: eight;"));

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
        CldrData specials = cldrData(rbnfRules(DURATION_RULES, "\n"
                + "%%hr:\n"
                + "0: 0 hours; 1 hour; =0= hours;\n"
                + "%in-numerals:\n"
                + "0: =0= sec.;\n"
                + "60: =%%min-sec=;\n"
                + "3600: =%%hr-min-sec=;\n"
                + "%%min:\n"
                + "0: 0 minutes; 1 minute; =0= minutes;\n"));

        CldrData cldrData = cldrData(rbnfRules(ORDINAL_RULES, "\n"
                + "%digits-ordinal:\n"
                + "-x: \\u2212>>;\n"
                + "0: =#,##0=$(ordinal,one{st}two{nd}few{rd}other{th})$;\n"));

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
        CldrData cldrData = cldrData(rbnfRules(SPELLOUT_RULES, "\n"
                + "%escaping:\n"
                + "k1: \\\\ Backslash\n"
                + "k2: << Arrows >>\n"
                + "k3: \\u00DC Umlaut\n"
                + "k4: \\U0001F603 Smiley\n"));

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

    /*
     * This wraps a set of RBNF rules into an rbnfRules element.
     */
    private static CldrValue rbnfRules(Group group, String value) {

        StringBuilder cldrPath = new StringBuilder("//ldml/rbnf");
        appendAttribute(cldrPath.append("/rulesetGrouping"), "type", group);
        cldrPath.append("/rbnfRules");

        return CldrValue.parseValue(cldrPath.toString(), value);
    }

    private static void appendAttribute(StringBuilder out, String k, Object v) {
        out.append(String.format("[@%s=\"%s\"]", k, v));
    }
}