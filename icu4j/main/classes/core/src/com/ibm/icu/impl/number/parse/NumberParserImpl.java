// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.icu.impl.StringSegment;
import com.ibm.icu.impl.number.AffixPatternProvider;
import com.ibm.icu.impl.number.CustomSymbolCurrency;
import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.Grouper;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.impl.number.PatternStringParser.ParsedPatternInfo;
import com.ibm.icu.impl.number.PropertiesAffixPatternProvider;
import com.ibm.icu.impl.number.RoundingUtils;
import com.ibm.icu.number.NumberFormatter.GroupingStrategy;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.ULocale;

/**
 * Primary number parsing implementation class.
 *
 * @author sffc
 *
 */
public class NumberParserImpl {

    // TODO: Find a better place for this enum.
    /** Controls the set of rules for parsing a string. */
    public static enum ParseMode {
        /**
         * Lenient mode should be used if you want to accept malformed user input. It will use heuristics
         * to attempt to parse through typographical errors in the string.
         */
        LENIENT,

        /**
         * Strict mode should be used if you want to require that the input is well-formed. More
         * specifically, it differs from lenient mode in the following ways:
         *
         * <ul>
         * <li>Grouping widths must match the grouping settings. For example, "12,3,45" will fail if the
         * grouping width is 3, as in the pattern "#,##0".
         * <li>The string must contain a complete prefix and suffix. For example, if the pattern is
         * "{#};(#)", then "{123}" or "(123)" would match, but "{123", "123}", and "123" would all fail.
         * (The latter strings would be accepted in lenient mode.)
         * <li>Whitespace may not appear at arbitrary places in the string. In lenient mode, whitespace
         * is allowed to occur arbitrarily before and after prefixes and exponent separators.
         * <li>Leading grouping separators are not allowed, as in ",123".
         * <li>Minus and plus signs can only appear if specified in the pattern. In lenient mode, a plus
         * or minus sign can always precede a number.
         * <li>The set of characters that can be interpreted as a decimal or grouping separator is
         * smaller.
         * <li><strong>If currency parsing is enabled,</strong> currencies must only appear where
         * specified in either the current pattern string or in a valid pattern string for the current
         * locale. For example, if the pattern is "¤0.00", then "$1.23" would match, but "1.23$" would
         * fail to match.
         * </ul>
         */
        STRICT,
    }

    public static NumberParserImpl createSimpleParser(
            ULocale locale,
            String pattern,
            int parseFlags) {

        NumberParserImpl parser = new NumberParserImpl(parseFlags);
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        IgnorablesMatcher ignorables = IgnorablesMatcher.DEFAULT;

        AffixTokenMatcherFactory factory = new AffixTokenMatcherFactory();
        factory.currency = Currency.getInstance("USD");
        factory.symbols = symbols;
        factory.ignorables = ignorables;
        factory.locale = locale;

        ParsedPatternInfo patternInfo = PatternStringParser.parseToPatternInfo(pattern);
        AffixMatcher.createMatchers(patternInfo, parser, factory, ignorables, parseFlags);

        Grouper grouper = Grouper.forStrategy(GroupingStrategy.AUTO).withLocaleData(locale, patternInfo);

        parser.addMatcher(ignorables);
        parser.addMatcher(DecimalMatcher.getInstance(symbols, grouper, parseFlags));
        parser.addMatcher(MinusSignMatcher.getInstance(symbols, false));
        parser.addMatcher(PlusSignMatcher.getInstance(symbols, false));
        parser.addMatcher(PercentMatcher.getInstance(symbols));
        parser.addMatcher(PermilleMatcher.getInstance(symbols));
        parser.addMatcher(NanMatcher.getInstance(symbols, parseFlags));
        parser.addMatcher(InfinityMatcher.getInstance(symbols));
        parser.addMatcher(PaddingMatcher.getInstance("@"));
        parser.addMatcher(ScientificMatcher.getInstance(symbols, grouper));
        parser.addMatcher(CurrencyNamesMatcher.getInstance(locale));
        parser.addMatcher(new RequireNumberMatcher());

        parser.freeze();
        return parser;
    }

    public static Number parseStatic(
            String input,
            ParsePosition ppos,
            DecimalFormatProperties properties,
            DecimalFormatSymbols symbols) {
        NumberParserImpl parser = createParserFromProperties(properties, symbols, false, false);
        ParsedNumber result = new ParsedNumber();
        parser.parse(input, true, result);
        if (result.success()) {
            ppos.setIndex(result.charEnd);
            return result.getNumber();
        } else {
            ppos.setErrorIndex(result.charEnd);
            return null;
        }
    }

    public static CurrencyAmount parseStaticCurrency(
            String input,
            ParsePosition ppos,
            DecimalFormatProperties properties,
            DecimalFormatSymbols symbols) {
        NumberParserImpl parser = createParserFromProperties(properties, symbols, true, false);
        ParsedNumber result = new ParsedNumber();
        parser.parse(input, true, result);
        if (result.success()) {
            ppos.setIndex(result.charEnd);
            // TODO: Clean this up
            Currency currency;
            if (result.currencyCode != null) {
                currency = Currency.getInstance(result.currencyCode);
            } else {
                assert 0 != (result.flags & ParsedNumber.FLAG_HAS_DEFAULT_CURRENCY);
                currency = CustomSymbolCurrency
                        .resolve(properties.getCurrency(), symbols.getULocale(), symbols);
            }
            return new CurrencyAmount(result.getNumber(), currency);
        } else {
            ppos.setErrorIndex(result.charEnd);
            return null;
        }
    }

    public static NumberParserImpl createDefaultParserForLocale(ULocale loc, boolean optimize) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(loc);
        DecimalFormatProperties properties = PatternStringParser.parseToProperties("0");
        return createParserFromProperties(properties, symbols, false, optimize);
    }

    public static NumberParserImpl createParserFromProperties(
            DecimalFormatProperties properties,
            DecimalFormatSymbols symbols,
            boolean parseCurrency,
            boolean optimize) {

        ULocale locale = symbols.getULocale();
        AffixPatternProvider patternInfo = new PropertiesAffixPatternProvider(properties);
        Currency currency = CustomSymbolCurrency.resolve(properties.getCurrency(), locale, symbols);
        boolean isStrict = properties.getParseMode() == ParseMode.STRICT;
        Grouper grouper = Grouper.forProperties(properties);
        int parseFlags = 0;
        // Fraction grouping is disabled by default because it has never been supported in DecimalFormat
        parseFlags |= ParsingUtils.PARSE_FLAG_FRACTION_GROUPING_DISABLED;
        if (!properties.getParseCaseSensitive()) {
            parseFlags |= ParsingUtils.PARSE_FLAG_IGNORE_CASE;
        }
        if (properties.getParseIntegerOnly()) {
            parseFlags |= ParsingUtils.PARSE_FLAG_INTEGER_ONLY;
        }
        if (properties.getSignAlwaysShown()) {
            parseFlags |= ParsingUtils.PARSE_FLAG_PLUS_SIGN_ALLOWED;
        }
        if (isStrict) {
            parseFlags |= ParsingUtils.PARSE_FLAG_STRICT_GROUPING_SIZE;
            parseFlags |= ParsingUtils.PARSE_FLAG_STRICT_SEPARATORS;
            parseFlags |= ParsingUtils.PARSE_FLAG_USE_FULL_AFFIXES;
            parseFlags |= ParsingUtils.PARSE_FLAG_EXACT_AFFIX;
        } else {
            parseFlags |= ParsingUtils.PARSE_FLAG_INCLUDE_UNPAIRED_AFFIXES;
        }
        if (grouper.getPrimary() <= 0) {
            parseFlags |= ParsingUtils.PARSE_FLAG_GROUPING_DISABLED;
        }
        if (parseCurrency || patternInfo.hasCurrencySign()) {
            parseFlags |= ParsingUtils.PARSE_FLAG_MONETARY_SEPARATORS;
        }
        if (optimize) {
            parseFlags |= ParsingUtils.PARSE_FLAG_OPTIMIZE;
        }
        IgnorablesMatcher ignorables = isStrict ? IgnorablesMatcher.STRICT : IgnorablesMatcher.DEFAULT;

        NumberParserImpl parser = new NumberParserImpl(parseFlags);

        AffixTokenMatcherFactory factory = new AffixTokenMatcherFactory();
        factory.currency = currency;
        factory.symbols = symbols;
        factory.ignorables = ignorables;
        factory.locale = locale;

        //////////////////////
        /// AFFIX MATCHERS ///
        //////////////////////

        // Set up a pattern modifier with mostly defaults to generate AffixMatchers.
        AffixMatcher.createMatchers(patternInfo, parser, factory, ignorables, parseFlags);

        ////////////////////////
        /// CURRENCY MATCHER ///
        ////////////////////////

        if (parseCurrency || patternInfo.hasCurrencySign()) {
            parser.addMatcher(CurrencyCustomMatcher.getInstance(currency, locale));
            parser.addMatcher(CurrencyNamesMatcher.getInstance(locale));
        }

        ///////////////////////////////
        /// OTHER STANDARD MATCHERS ///
        ///////////////////////////////

        if (!isStrict) {
            parser.addMatcher(PlusSignMatcher.getInstance(symbols, false));
            parser.addMatcher(MinusSignMatcher.getInstance(symbols, false));
            parser.addMatcher(NanMatcher.getInstance(symbols, parseFlags));
            parser.addMatcher(PercentMatcher.getInstance(symbols));
            parser.addMatcher(PermilleMatcher.getInstance(symbols));
        }
        parser.addMatcher(InfinityMatcher.getInstance(symbols));
        String padString = properties.getPadString();
        if (padString != null && !ignorables.getSet().contains(padString)) {
            parser.addMatcher(PaddingMatcher.getInstance(padString));
        }
        parser.addMatcher(ignorables);
        parser.addMatcher(DecimalMatcher.getInstance(symbols, grouper, parseFlags));
        if (!properties.getParseNoExponent()) {
            parser.addMatcher(ScientificMatcher.getInstance(symbols, grouper));
        }

        //////////////////
        /// VALIDATORS ///
        //////////////////

        parser.addMatcher(new RequireNumberMatcher());
        if (isStrict) {
            parser.addMatcher(new RequireAffixMatcher());
        }
        if (isStrict && properties.getMinimumExponentDigits() > 0) {
            parser.addMatcher(new RequireExponentMatcher());
        }
        if (parseCurrency) {
            parser.addMatcher(new RequireCurrencyMatcher());
        }
        if (properties.getDecimalPatternMatchRequired()) {
            boolean patternHasDecimalSeparator = properties.getDecimalSeparatorAlwaysShown()
                    || properties.getMaximumFractionDigits() != 0;
            parser.addMatcher(RequireDecimalSeparatorMatcher.getInstance(patternHasDecimalSeparator));
        }
        if (properties.getMultiplier() != null) {
            // We need to use a math context in order to prevent non-terminating decimal expansions.
            // This is only used when dividing by the multiplier.
            parser.addMatcher(new MultiplierHandler(properties.getMultiplier(),
                    RoundingUtils.getMathContextOr34Digits(properties)));
        }

        parser.freeze();
        return parser;
    }

    private final int parseFlags;
    private final List<NumberParseMatcher> matchers;
    private final List<UnicodeSet> leads;
    private boolean frozen;

    /**
     * Creates a new, empty parser.
     *
     * @param parseFlags
     *            The parser settings defined in the PARSE_FLAG_* fields.
     */
    public NumberParserImpl(int parseFlags) {
        matchers = new ArrayList<NumberParseMatcher>();
        if (0 != (parseFlags & ParsingUtils.PARSE_FLAG_OPTIMIZE)) {
            leads = new ArrayList<UnicodeSet>();
        } else {
            leads = null;
        }
        this.parseFlags = parseFlags;
        frozen = false;
    }

    public void addMatcher(NumberParseMatcher matcher) {
        assert !frozen;
        this.matchers.add(matcher);
        if (leads != null) {
            addLeadCodePointsForMatcher(matcher);
        }
    }

    public void addMatchers(Collection<? extends NumberParseMatcher> matchers) {
        assert !frozen;
        this.matchers.addAll(matchers);
        if (leads != null) {
            for (NumberParseMatcher matcher : matchers) {
                addLeadCodePointsForMatcher(matcher);
            }
        }
    }

    private void addLeadCodePointsForMatcher(NumberParseMatcher matcher) {
        UnicodeSet leadCodePoints = matcher.getLeadCodePoints();
        assert leadCodePoints.isFrozen();
        // TODO: Avoid the clone operation here.
        if (0 != (parseFlags & ParsingUtils.PARSE_FLAG_IGNORE_CASE)) {
            leadCodePoints = leadCodePoints.cloneAsThawed().closeOver(UnicodeSet.ADD_CASE_MAPPINGS)
                    .freeze();
        }
        this.leads.add(leadCodePoints);
    }

    public void freeze() {
        frozen = true;
    }

    public void parse(String input, boolean greedy, ParsedNumber result) {
        parse(input, 0, greedy, result);
    }

    /**
     * Primary entrypoint to parsing code path.
     *
     * @param input
     *            The string to parse. This is a String, not CharSequence, to enforce assumptions about
     *            immutability (CharSequences are not guaranteed to be immutable).
     * @param start
     *            The index into the string at which to start parsing.
     * @param greedy
     *            Whether to use the faster but potentially less accurate greedy code path.
     * @param result
     *            Output variable to store results.
     */
    public void parse(String input, int start, boolean greedy, ParsedNumber result) {
        assert frozen;
        assert start >= 0 && start < input.length();
        StringSegment segment = new StringSegment(input,
                0 != (parseFlags & ParsingUtils.PARSE_FLAG_IGNORE_CASE));
        segment.adjustOffset(start);
        if (greedy) {
            parseGreedyRecursive(segment, result);
        } else {
            parseLongestRecursive(segment, result);
        }
        for (NumberParseMatcher matcher : matchers) {
            matcher.postProcess(result);
        }
    }

    private void parseGreedyRecursive(StringSegment segment, ParsedNumber result) {
        // Base Case
        if (segment.length() == 0) {
            return;
        }

        int initialOffset = segment.getOffset();
        int leadCp = segment.getCodePoint();
        for (int i = 0; i < matchers.size(); i++) {
            if (leads != null && !leads.get(i).contains(leadCp)) {
                continue;
            }
            NumberParseMatcher matcher = matchers.get(i);
            matcher.match(segment, result);
            if (segment.getOffset() != initialOffset) {
                // In a greedy parse, recurse on only the first match.
                parseGreedyRecursive(segment, result);
                // The following line resets the offset so that the StringSegment says the same across
                // the function
                // call boundary. Since we recurse only once, this line is not strictly necessary.
                segment.setOffset(initialOffset);
                return;
            }
        }

        // NOTE: If we get here, the greedy parse completed without consuming the entire string.
    }

    private void parseLongestRecursive(StringSegment segment, ParsedNumber result) {
        // Base Case
        if (segment.length() == 0) {
            return;
        }

        // TODO: Give a nice way for the matcher to reset the ParsedNumber?
        ParsedNumber initial = new ParsedNumber();
        initial.copyFrom(result);
        ParsedNumber candidate = new ParsedNumber();

        int initialOffset = segment.getOffset();
        for (int i = 0; i < matchers.size(); i++) {
            // TODO: Check leadChars here?
            NumberParseMatcher matcher = matchers.get(i);

            // In a non-greedy parse, we attempt all possible matches and pick the best.
            for (int charsToConsume = 0; charsToConsume < segment.length();) {
                charsToConsume += Character.charCount(segment.codePointAt(charsToConsume));

                // Run the matcher on a segment of the current length.
                candidate.copyFrom(initial);
                segment.setLength(charsToConsume);
                boolean maybeMore = matcher.match(segment, candidate);
                segment.resetLength();

                // If the entire segment was consumed, recurse.
                if (segment.getOffset() - initialOffset == charsToConsume) {
                    parseLongestRecursive(segment, candidate);
                    if (candidate.isBetterThan(result)) {
                        result.copyFrom(candidate);
                    }
                }

                // Since the segment can be re-used, reset the offset.
                // This does not have an effect if the matcher did not consume any chars.
                segment.setOffset(initialOffset);

                // Unless the matcher wants to see the next char, continue to the next matcher.
                if (!maybeMore) {
                    break;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "<NumberParserImpl matchers=" + matchers.toString() + ">";
    }
}
