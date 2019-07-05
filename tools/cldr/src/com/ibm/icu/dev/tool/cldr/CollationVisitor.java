package com.ibm.icu.dev.tool.cldr;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrData.PrefixVisitor;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static org.unicode.cldr.api.AttributeKey.keyOf;
import static org.unicode.cldr.api.CldrDataSupplier.CldrResolution.UNRESOLVED;
import static org.unicode.cldr.api.CldrDataType.LDML;
import static org.unicode.cldr.api.CldrDraftStatus.CONTRIBUTED;
import static org.unicode.cldr.api.CldrDraftStatus.UNCONFIRMED;

/**
 * A visitor to collect collation data from {@code LDML} XML data via the paths:
 * <pre>{@code
 *   //ldml/collation/*
 * }</pre>
 *
 * <p>This mapper also supports blending additional "special" data into the results.
 */
final class CollationVisitor implements PrefixVisitor {
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

    private static final Splitter LINE_SPLITTER =
        Splitter.on('\n').trimResults().omitEmptyStrings();

    /** Processes data from the given supplier to generate day period data. */
    public static List<IcuData> process(CldrDataSupplier supplier, Path specialsDir) {
        List<IcuData> icuData = new ArrayList<>();
        // From CldrMapper, which checks the draft status of the collation element.
        supplier = supplier.withDraftStatus(CONTRIBUTED);
        for (String localeId : supplier.getAvailableLocaleIds()) {
            CollationVisitor v = new CollationVisitor(localeId);

            Path specialsXml = specialsDir.resolve(localeId + ".xml");
            if (Files.exists(specialsXml)) {
                CldrDataSupplier
                    .forCldrFile(LDML, specialsXml, UNCONFIRMED)
                    .accept(CldrData.PathOrder.ARBITRARY, v);
                v.icuData.setFileComment(
                    "ICU <specials> source: <path>/xml/collation/" + localeId + ".xml");
            }
            supplier.getDataForLocale(localeId, UNRESOLVED).accept(CldrData.PathOrder.ARBITRARY, v);
            if (v.hasData()) {
                v.icuData.add("/Version", CldrDataSupplier.getCldrVersionString());
                icuData.add(v.icuData);
            }
        }
        return icuData;
    }

    private final IcuData icuData;

    private CollationVisitor(String localeId) {
        this.icuData = new IcuData("common/collation/" + localeId + ".xml", localeId, true);
        // Super special hack case because the XML data is a bit broken for the root collation
        // data (there's an empty <collation> element that's a non-leaf element and thus not
        // visited, but we should add an empty sequence to the output data.
        if (localeId.equals("root")) {
            icuData.replace("/collations/standard/Sequence", "");
            icuData.replace(
                "/collations/standard/Version", CldrDataSupplier.getCldrVersionString());
        }
    }

    private boolean hasData() {
        return !icuData.keySet().isEmpty();
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
            String rbPath = "/collations/" + type + "/Sequence";

            // WARNING: This is almost certainly a bug, since while @type can have the value
            // "short" it can also have other values. This code was copied from CollationMapper
            // which has the line;
            //   isShort = attr.getValue("alt") != null;
            boolean isShort = COLLATION_RULE_ALT.optionalValueFrom(v).isPresent();

            // Note that it's not clear why there's a check for "containsKey()" here. The code from
            // which this was derived is largely undocumented and this check could have been overly
            // defensive (perhaps a duplicate key should be an error?).
            if (isShort || !icuData.containsKey(rbPath)) {
                // TODO: Perhaps (like AttributeKey) there could be getListValue() on CldrValue.
                String[] rules = LINE_SPLITTER.splitToList(v.getValue()).stream()
                    .map(CollationVisitor::removeComment)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
                if (rules.length > 0) {
                    icuData.replace(rbPath, rules);
                    icuData.replace(
                        "/collations/" + type + "/Version",
                        CldrDataSupplier.getCldrVersionString());
                }
            }
        } else if (DEFAULT_COLLATION.matchesSuffixOf(p)) {
            icuData.add("/collations/default", v.getValue());
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
        String rbPath = String.format("/%s:process(%s)",
            key.getElementName().substring(4), key.getAttributeName().substring(4));
        icuData.add(rbPath, key.valueFrom(value));
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
}
