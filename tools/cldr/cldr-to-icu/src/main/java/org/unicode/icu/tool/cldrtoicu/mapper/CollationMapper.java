// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.base.Preconditions.checkArgument;
import static org.unicode.cldr.api.AttributeKey.keyOf;
import static org.unicode.cldr.api.CldrData.PathOrder.ARBITRARY;
import static org.unicode.cldr.api.CldrDataSupplier.CldrResolution.UNRESOLVED;

import java.util.Optional;

import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrData.PrefixVisitor;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.PathMatcher;
import org.unicode.icu.tool.cldrtoicu.RbPath;
import org.unicode.icu.tool.cldrtoicu.RbValue;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

/**
 * A mapper to collect collation data from {@link CldrDataType#LDML LDML} data via the paths:
 * <pre>{@code
 *   //ldml/collations/*
 *   //ldml/special/icu:UCARules
 *   //ldml/special/icu:depends
 * }</pre>
 */
public final class CollationMapper {
    private static final PathMatcher COLLATIONS = PathMatcher.of("ldml/collations");

    // Note that the 'type' attribute is optional, so cannot be in the path matcher.
    // However since the CLDR data never actually omits the value, it would be easy to change the
    // attribute metadata to stop it being an implicit attribute and then it could appear.
    private static final PathMatcher COLLATION_RULE = PathMatcher.of("collation/cr");
    private static final AttributeKey COLLATION_TYPE = keyOf("collation", "type");
    private static final AttributeKey COLLATION_RULE_ALT = keyOf("cr", "alt");

    private static final PathMatcher DEFAULT_COLLATION = PathMatcher.of("defaultCollation");

    private static final PathMatcher SPECIAL = PathMatcher.of("ldml/special");
    private static final AttributeKey SPECIAL_RULES = keyOf("icu:UCARules", "icu:uca_rules");
    private static final AttributeKey SPECIAL_DEP = keyOf("icu:depends", "icu:dependency");

    private static final RbPath RB_COLLATIONS_DEFAULT = RbPath.of("collations", "default");
    private static final RbPath RB_STANDARD_SEQUENCE =
        RbPath.of("collations", "standard", "Sequence");
    private static final RbPath RB_STANDARD_VERSION =
        RbPath.of("collations", "standard", "Version");

    private static final Splitter LINE_SPLITTER =
        Splitter.on('\n').trimResults().omitEmptyStrings();

    /**
     * Processes data from the given supplier to generate collation data for a set of locale IDs.
     *
     * @param localeId the locale ID to generate data for.
     * @param src the CLDR data supplier to process.
     * @param icuSpecialData additional ICU data (in the "icu:" namespace)
     * @return IcuData containing RBNF data for the given locale ID.
     */
    public static IcuData process(
        String localeId, CldrDataSupplier src, Optional<CldrData> icuSpecialData) {

        CollationVisitor visitor = new CollationVisitor(localeId);
        icuSpecialData.ifPresent(s -> s.accept(ARBITRARY, visitor));
        src.getDataForLocale(localeId, UNRESOLVED).accept(ARBITRARY, visitor);
        return visitor.icuData;
    }

    final static class CollationVisitor implements PrefixVisitor {
        private final IcuData icuData;

        CollationVisitor(String localeId) {
            this.icuData = new IcuData(localeId, true);
            // Super special hack case because the XML data is a bit broken for the root collation
            // data (there's an empty <collation> element that's a non-leaf element and thus not
            // visited, but we should add an empty sequence to the output data.
            if (localeId.equals("root")) {
                icuData.replace(RB_STANDARD_SEQUENCE, "");
                // TODO: Collation versioning probably needs to be improved.
                icuData.replace(RB_STANDARD_VERSION, CldrDataSupplier.getCldrVersionString());
            }
        }

        @Override
        public void visitPrefixStart(CldrPath prefix, Context ctx) {
            if (COLLATIONS.matchesPrefixOf(prefix)) {
                ctx.install(this::collectRules);
            } else if (SPECIAL.matchesPrefixOf(prefix)) {
                ctx.install(this::maybeAddSpecial);
            }
        }

        private void collectRules(CldrValue v) {
            CldrPath p = v.getPath();
            if (COLLATION_RULE.matchesSuffixOf(p)) {
                String type = COLLATION_TYPE.valueFrom(v);
                RbPath rbPath = RbPath.of("collations", type, "Sequence");

                // WARNING: This is almost certainly a bug, since while @type can have the value
                // "short" it can also have other values. This code was copied from CollationMapper
                // which has the line;
                //   isShort = attr.getValue("alt") != null;
                boolean isShort = COLLATION_RULE_ALT.optionalValueFrom(v).isPresent();

                // Note that it's not clear why there's a check for "contains()" here. The code
                // from which this was derived is largely undocumented and this check could have
                // been overly defensive (perhaps a duplicate key should be an error?).
                if (isShort || !icuData.contains(rbPath)) {
                    RbValue rules = RbValue.of(
                        LINE_SPLITTER.splitToList(v.getValue()).stream()
                            .map(CollationMapper::removeComment)
                            .filter(s -> !s.isEmpty())::iterator);
                    icuData.replace(rbPath, rules);
                    icuData.replace(
                        RbPath.of("collations", type, "Version"),
                        CldrDataSupplier.getCldrVersionString());
                }
            } else if (DEFAULT_COLLATION.matchesSuffixOf(p)) {
                icuData.add(RB_COLLATIONS_DEFAULT, v.getValue());
            }
        }

        // This is a bit special since the attribute we want to add depends on the element we are
        // visiting (which is somewhat unusual in the transformation classes).
        private void maybeAddSpecial(CldrValue value) {
            AttributeKey key;
            switch (value.getPath().getName()) {
            case "icu:UCARules":
                key = SPECIAL_RULES;
                break;
            case "icu:depends":
                key = SPECIAL_DEP;
                break;
            default:
                return;
            }
            // substring(4) just removes the "icu:" prefix (which we know is present in the key).
            RbPath rbPath = RbPath.of(
                String.format("%s:process(%s)",
                    key.getElementName().substring(4), key.getAttributeName().substring(4)));
            icuData.add(rbPath, key.valueFrom(value));
        }
    }

    // Collation data can contain # to mark an end-of-line comment, but it can also contain data
    // with # in it. In the latter case it must be in a single-quoted string (e.g. 'x#y'). However
    // the precise semantics of the quoting rules are not particularly clear, so this method
    // assumes that:
    // * single quote (apostrophe) begins and ends quoting.
    // * outside a quoted section, all characters are literal.
    // * inside a quoted section, backslash '\' escapes any single character (e.g \a, \', \\)
    private static String removeComment(String s) {
        int i = findCommentStart(s);
        if (i >= 0) {
            s = CharMatcher.whitespace().trimTrailingFrom(s.substring(0, i));
        }
        return s;
    }

    // Returns the index of the first unquoted '#' in the string.
    private static int findCommentStart(String s) {
        boolean quoted = false;
        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
            case '\'':
                quoted = !quoted;
                break;

            case '\\':
                if (quoted) {
                    i++;
                }
                break;

            case '#':
                if (!quoted) {
                    return i;
                }
                break;

            default:
                // Do nothing and consume the character
            }
        }
        checkArgument(!quoted, "mismatched quotes in: %s", s);
        return -1;
    }

    private CollationMapper() {}
}
