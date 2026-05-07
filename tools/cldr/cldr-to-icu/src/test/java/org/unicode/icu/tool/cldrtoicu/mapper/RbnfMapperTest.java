// © 2019-2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static org.unicode.icu.tool.cldrtoicu.mapper.RbnfMapperTest.Group.DURATION_RULES;
import static org.unicode.icu.tool.cldrtoicu.mapper.RbnfMapperTest.Group.ORDINAL_RULES;
import static org.unicode.icu.tool.cldrtoicu.mapper.RbnfMapperTest.Group.SPELLOUT_RULES;
import static org.unicode.icu.tool.cldrtoicu.testing.IcuDataSubjectFactory.assertThat;

import com.google.common.base.CaseFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.util.ULocale;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.RbValue;

@RunWith(JUnit4.class)
public class RbnfMapperTest {
    // IMPORTANT: The ldml.dtd only defines 3 groups:
    //     NumberingSystemRules, OrdinalRules, SpelloutRules
    // but the "specials" files used by ICU introduce additional group names (e.g. DurationRules)
    // which are strictly speaking invalid according to the DTD.
    enum Group {
        NUMBERING_SYSTEM_RULES,
        ORDINAL_RULES,
        SPELLOUT_RULES,
        DURATION_RULES;

        @Override
        public String toString() {
            // It's "NumberingSystemRules" not "numberingSystemRules"
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
        }
    }

    @Test
    public void testSingleRuleset() {
        CldrData cldrData =
                cldrData(
                        rbnfRules(
                                SPELLOUT_RULES,
                                "\n"
                                        + "%%2d-year:\n"
                                        + "0: hundred;\n"
                                        + "1: oh-=%first-set=;\n"
                                        + "10: =%first-set=;\n"));

        IcuData icuData = new IcuData("xx", true);
        RbnfMapper.process(icuData, cldrData, Optional.empty());

        assertThat(icuData)
                .hasValuesFor(
                        "/RBNFRules/SpelloutRules",
                        RbValue.of(
                                "%%2d-year:",
                                "0: hundred;",
                                "1: oh-=%first-set=;",
                                "10: =%first-set=;"));
    }

    @Test
    public void testMultipleRulesets() {
        CldrData cldrData =
                cldrData(
                        rbnfRules(
                                SPELLOUT_RULES,
                                "\n"
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

        assertThat(icuData)
                .hasValuesFor(
                        "/RBNFRules/SpelloutRules",
                        RbValue.of(
                                "%first-set:",
                                "-x: one;",
                                "Inf: two;",
                                "NaN: three;",
                                "0: four;",
                                "%second-set:",
                                "-x: five;",
                                "Inf: six;",
                                "NaN: seven;",
                                "0: eight;"));
    }

    @Test
    public void testSpecials() {
        CldrData specials =
                cldrData(
                        rbnfRules(
                                DURATION_RULES,
                                "\n"
                                        + "%%hr:\n"
                                        + "0: 0 hours; 1 hour; =0= hours;\n"
                                        + "%in-numerals:\n"
                                        + "0: =0= sec.;\n"
                                        + "60: =%%min-sec=;\n"
                                        + "3600: =%%hr-min-sec=;\n"
                                        + "%%min:\n"
                                        + "0: 0 minutes; 1 minute; =0= minutes;\n"));

        CldrData cldrData =
                cldrData(
                        rbnfRules(
                                ORDINAL_RULES,
                                "\n"
                                        + "%digits-ordinal:\n"
                                        + "-x: \\u2212>>;\n"
                                        + "0: =#,##0=$(ordinal,one{st}two{nd}few{rd}other{th})$;\n"));

        IcuData icuData = new IcuData("xx", true);
        RbnfMapper.process(icuData, cldrData, Optional.of(specials));

        assertThat(icuData)
                .hasValuesFor(
                        "/RBNFRules/OrdinalRules",
                        RbValue.of(
                                "%digits-ordinal:",
                                "-x: \\u2212>>;",
                                "0: =#,##0=$(ordinal,one{st}two{nd}few{rd}other{th})$;"));

        // The headings are sorted in the output ("hr" < "in-numerals" < min").
        assertThat(icuData)
                .hasValuesFor(
                        "/RBNFRules/DurationRules",
                        RbValue.of(
                                "%%hr:",
                                "0: 0 hours; 1 hour; =0= hours;",
                                "%in-numerals:",
                                "0: =0= sec.;",
                                "60: =%%min-sec=;",
                                "3600: =%%hr-min-sec=;",
                                "%%min:",
                                "0: 0 minutes; 1 minute; =0= minutes;"));
    }

    // Note that while this is testing the escaping behaviour, the implementation was largely
    // derived from a mostly undocumented method in the previous converter, and while it behaves
    // the same, it's not entirely obviously why some of the special cases really exist.
    @Test
    public void testEscaping() {
        CldrData cldrData =
                cldrData(
                        rbnfRules(
                                SPELLOUT_RULES,
                                "\n"
                                        + "%escaping:\n"
                                        + "k1: \\\\ Backslash\n"
                                        + "k2: << Arrows >>\n"
                                        + "k3: \\u00DC Umlaut\n"
                                        + "k4: \\U0001F603 Smiley\n"));

        IcuData icuData = new IcuData("xx", true);
        RbnfMapper.process(icuData, cldrData, Optional.empty());

        assertThat(icuData)
                .hasValuesFor(
                        "/RBNFRules/SpelloutRules",
                        RbValue.of(
                                "%escaping:",
                                "k1: \\\\ Backslash",
                                "k2: << Arrows >>",
                                "k3: \\u00DC Umlaut",
                                "k4: \\U0001F603 Smiley"));
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

    @SuppressWarnings("unused")
    @Ignore("Disabled. It should be run manually.")
    @Test
    public void testCoverage() {
        var ruleMatchingPatterns = List.of(
                Pattern.compile("^%spellout-numbering$"),
                Pattern.compile("^%spellout-numbering-year$"),
                Pattern.compile("^%spellout-cardinal.*"),
                Pattern.compile("^%spellout-ordinal.*"));
        System.out.println("| Locale | Numbering | Year | Cardinal | Ordinal |");
        System.out.println("| ------ | --------- | ---- | -------- | ------- |");
        for (ULocale loc : NumberFormat.getAvailableULocales()) {
            RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(loc, RuleBasedNumberFormat.SPELLOUT);
            if (!rbnf.getLocale(ULocale.ACTUAL_LOCALE).equals(loc)) {
                // Uninteresting duplicate data. Show only the minimal set of information.
                continue;
            }
            System.out.print("| [" + loc + "](https://st.unicode.org/cldr-apps/numbers.jsp?locale="+loc+")");
            for (var desiredRule : ruleMatchingPatterns) {
                int count = 0;
                for (String name : rbnf.getRuleSetNames()) {
                    if (desiredRule.matcher(name).find()) {
                        count++;
                    }
                }
                if (count > 0) {
                    System.out.print(" | " + count + "✅");
                }
                else {
                    System.out.print(" | ❌");
                }
            }
            System.out.println(" |");
        }
    }
}
