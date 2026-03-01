// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.unicode.cldr.api.AttributeKey.keyOf;
import static org.unicode.cldr.api.CldrData.PathOrder.DTD;

import java.util.Optional;

import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.RbPath;
import org.unicode.icu.tool.cldrtoicu.RbValue;
import org.unicode.icu.tool.cldrtoicu.CldrDataProcessor;
import org.unicode.icu.tool.cldrtoicu.CldrDataProcessor.SubProcessor;

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

    private static final CldrDataProcessor<CollationMapper> CLDR_PROCESSOR;
    static {
        CldrDataProcessor.Builder<CollationMapper> processor = CldrDataProcessor.builder();
        SubProcessor<CollationMapper> collations = processor.addSubprocessor("//ldml/collations");
        collations.addValueAction("collation/cr", CollationMapper::collectRule);
        collations.addValueAction("defaultCollation", CollationMapper::collectDefault);
        // This could be a separate processor, since the specials data only contains these paths,
        // but it's not clear if in future it could also contain any collation rules.
        processor.addValueAction("//ldml/special/*", CollationMapper::maybeAddSpecial);
        CLDR_PROCESSOR = processor.build();
    }

    private static final AttributeKey COLLATION_TYPE = keyOf("collation", "type");
    private static final AttributeKey COLLATION_RULE_ALT = keyOf("cr", "alt");

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
     * @param icuData the ICU data to be filled.
     * @param cldrData the unresolved CLDR data to process.
     * @param icuSpecialData additional ICU data (in the "icu:" namespace).
     * @param cldrVersion version string to add to ICU data.
     * @return IcuData containing RBNF data for the given locale ID.
     */
    public static IcuData process(
        IcuData icuData, CldrData cldrData, Optional<CldrData> icuSpecialData, String cldrVersion) {

        CollationMapper mapper = new CollationMapper(icuData, cldrVersion);
        icuSpecialData.ifPresent(specialData -> CLDR_PROCESSOR.process(specialData, mapper, DTD));
        CLDR_PROCESSOR.process(cldrData, mapper, DTD);
        return icuData;
    }

    private final IcuData icuData;
    private final String cldrVersion;

    private CollationMapper(IcuData icuData, String cldrVersion) {
        this.icuData = checkNotNull(icuData);
        this.cldrVersion = checkNotNull(cldrVersion);
        // Super special hack case because the XML data is a bit broken for the root collation
        // data (there's an empty <collation> element that's a non-leaf element and thus not
        // visited, but we should add an empty sequence to the output data.
        // TODO: Fix CLDR (https://unicode-org.atlassian.net/projects/CLDR/issues/CLDR-13131)
        if (icuData.getName().equals("root")) {
            icuData.replace(RB_STANDARD_SEQUENCE, "");
            // TODO: Collation versioning probably needs to be improved.
            icuData.replace(RB_STANDARD_VERSION, cldrVersion);
        }
    }

    private void collectRule(CldrValue v) {
        String type = COLLATION_TYPE.valueFrom(v);
        RbPath rbPath = RbPath.of("collations", type, "Sequence");

        // WARNING: This is almost certainly a bug, since while @type can have the value
        // "short" it can also have other values. This code was copied from CollationMapper
        // which has the line;
        //   isShort = attr.getValue("alt") != null;
        // TODO: Raise a ticket to examine this.
        boolean isShort = COLLATION_RULE_ALT.optionalValueFrom(v).isPresent();

        // Note that it's not clear why there's a check for "contains()" here. The code
        // from which this was derived is largely undocumented and this check could have
        // been overly defensive (perhaps a duplicate key should be an error?).
        if (isShort || !icuData.getPaths().contains(rbPath)) {
            RbValue rules = RbValue.of(
                LINE_SPLITTER.splitToList(v.getValue()).stream()
                    .map(CollationMapper::removeComment)
                    .filter(s -> !s.isEmpty())::iterator);
            icuData.replace(rbPath, rules);
            icuData.replace(RbPath.of("collations", type, "Version"), cldrVersion);
        }
    }

    private void collectDefault(CldrValue v) {
        icuData.add(RB_COLLATIONS_DEFAULT, v.getValue());
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
