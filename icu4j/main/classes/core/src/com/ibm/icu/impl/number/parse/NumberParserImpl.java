// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number.parse;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.icu.impl.StringSegment;
import com.ibm.icu.impl.number.AffixPatternProvider;
import com.ibm.icu.impl.number.AffixUtils;
import com.ibm.icu.impl.number.CustomSymbolCurrency;
import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.DecimalFormatProperties.ParseMode;
import com.ibm.icu.impl.number.Grouper;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.impl.number.PatternStringParser.ParsedPatternInfo;
import com.ibm.icu.impl.number.PropertiesAffixPatternProvider;
import com.ibm.icu.impl.number.RoundingUtils;
import com.ibm.icu.number.NumberFormatter.GroupingStrategy;
import com.ibm.icu.number.Scale;
import com.ibm.icu.text.DecimalFormatSymbols;
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

    /**
     * Creates a parser with most default options. Used for testing, not production.
     */
    public static NumberParserImpl createSimpleParser(ULocale locale, String pattern, int parseFlags) {

        NumberParserImpl parser = new NumberParserImpl(parseFlags);
        Currency currency = Currency.getInstance("USD");
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        IgnorablesMatcher ignorables = IgnorablesMatcher.getInstance(parseFlags);

        AffixTokenMatcherFactory factory = new AffixTokenMatcherFactory();
        factory.currency = currency;
        factory.symbols = symbols;
        factory.ignorables = ignorables;
        factory.locale = locale;
        factory.parseFlags = parseFlags;

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
        parser.addMatcher(CombinedCurrencyMatcher.getInstance(currency, symbols, parseFlags));
        parser.addMatcher(new RequireNumberValidator());

        parser.freeze();
        return parser;
    }

    /**
     * Parses the string without returning a NumberParserImpl. Used for testing, not production.
     */
    public static Number parseStatic(
            String input,
            ParsePosition ppos,
            DecimalFormatProperties properties,
            DecimalFormatSymbols symbols) {
        NumberParserImpl parser = createParserFromProperties(properties, symbols, false);
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

    /**
     * Parses the string without returning a NumberParserImpl. Used for testing, not production.
     */
    public static CurrencyAmount parseStaticCurrency(
            String input,
            ParsePosition ppos,
            DecimalFormatProperties properties,
            DecimalFormatSymbols symbols) {
        NumberParserImpl parser = createParserFromProperties(properties, symbols, true);
        ParsedNumber result = new ParsedNumber();
        parser.parse(input, true, result);
        if (result.success()) {
            ppos.setIndex(result.charEnd);
            assert result.currencyCode != null;
            return new CurrencyAmount(result.getNumber(), Currency.getInstance(result.currencyCode));
        } else {
            ppos.setErrorIndex(result.charEnd);
            return null;
        }
    }

    public static NumberParserImpl createDefaultParserForLocale(ULocale loc) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(loc);
        DecimalFormatProperties properties = PatternStringParser.parseToProperties("0");
        return createParserFromProperties(properties, symbols, false);
    }

    /**
     * Creates a parser from the given DecimalFormatProperties. This is the endpoint used by
     * DecimalFormat in production code.
     *
     * @param properties
     *            The property bag.
     * @param symbols
     *            The locale's symbols.
     * @param parseCurrency
     *            True to force a currency match and use monetary separators; false otherwise.
     * @return An immutable parser object.
     */
    public static NumberParserImpl createParserFromProperties(
            DecimalFormatProperties properties,
            DecimalFormatSymbols symbols,
            boolean parseCurrency) {

        ULocale locale = symbols.getULocale();
        AffixPatternProvider affixProvider = PropertiesAffixPatternProvider.forProperties(properties);
        Currency currency = CustomSymbolCurrency.resolve(properties.getCurrency(), locale, symbols);
        ParseMode parseMode = properties.getParseMode();
        if (parseMode == null) {
            parseMode = ParseMode.LENIENT;
        }
        Grouper grouper = Grouper.forProperties(properties);
        int parseFlags = 0;
        if (!properties.getParseCaseSensitive()) {
            parseFlags |= ParsingUtils.PARSE_FLAG_IGNORE_CASE;
        }
        if (properties.getParseIntegerOnly()) {
            parseFlags |= ParsingUtils.PARSE_FLAG_INTEGER_ONLY;
        }
        if (properties.getParseToBigDecimal()) {
            parseFlags |= ParsingUtils.PARSE_FLAG_FORCE_BIG_DECIMAL;
        }
        if (properties.getSignAlwaysShown()) {
            parseFlags |= ParsingUtils.PARSE_FLAG_PLUS_SIGN_ALLOWED;
        }
        if (parseMode == ParseMode.JAVA_COMPATIBILITY) {
            parseFlags |= ParsingUtils.PARSE_FLAG_STRICT_SEPARATORS;
            parseFlags |= ParsingUtils.PARSE_FLAG_USE_FULL_AFFIXES;
            parseFlags |= ParsingUtils.PARSE_FLAG_EXACT_AFFIX;
            parseFlags |= ParsingUtils.PARSE_FLAG_JAVA_COMPATIBILITY_IGNORABLES;
        } else if (parseMode == ParseMode.STRICT) {
            parseFlags |= ParsingUtils.PARSE_FLAG_STRICT_GROUPING_SIZE;
            parseFlags |= ParsingUtils.PARSE_FLAG_STRICT_SEPARATORS;
            parseFlags |= ParsingUtils.PARSE_FLAG_USE_FULL_AFFIXES;
            parseFlags |= ParsingUtils.PARSE_FLAG_EXACT_AFFIX;
            parseFlags |= ParsingUtils.PARSE_FLAG_STRICT_IGNORABLES;
        } else {
            parseFlags |= ParsingUtils.PARSE_FLAG_INCLUDE_UNPAIRED_AFFIXES;
        }
        if (grouper.getPrimary() <= 0) {
            parseFlags |= ParsingUtils.PARSE_FLAG_GROUPING_DISABLED;
        }
        if (parseCurrency || affixProvider.hasCurrencySign()) {
            parseFlags |= ParsingUtils.PARSE_FLAG_MONETARY_SEPARATORS;
        }
        if (!parseCurrency) {
            parseFlags |= ParsingUtils.PARSE_FLAG_NO_FOREIGN_CURRENCIES;
        }

        NumberParserImpl parser = new NumberParserImpl(parseFlags);
        IgnorablesMatcher ignorables = IgnorablesMatcher.getInstance(parseFlags);

        AffixTokenMatcherFactory factory = new AffixTokenMatcherFactory();
        factory.currency = currency;
        factory.symbols = symbols;
        factory.ignorables = ignorables;
        factory.locale = locale;
        factory.parseFlags = parseFlags;

        //////////////////////
        /// AFFIX MATCHERS ///
        //////////////////////

        // Set up a pattern modifier with mostly defaults to generate AffixMatchers.
        AffixMatcher.createMatchers(affixProvider, parser, factory, ignorables, parseFlags);

        ////////////////////////
        /// CURRENCY MATCHER ///
        ////////////////////////

        if (parseCurrency || affixProvider.hasCurrencySign()) {
            parser.addMatcher(CombinedCurrencyMatcher.getInstance(currency, symbols, parseFlags));
        }

        ///////////////
        /// PERCENT ///
        ///////////////

        // ICU-TC meeting, April 11, 2018: accept percent/permille only if it is in the pattern,
        // and to maintain regressive behavior, divide by 100 even if no percent sign is present.
        if (parseMode == ParseMode.LENIENT && affixProvider.containsSymbolType(AffixUtils.TYPE_PERCENT)) {
            parser.addMatcher(PercentMatcher.getInstance(symbols));
        }
        if (parseMode == ParseMode.LENIENT && affixProvider.containsSymbolType(AffixUtils.TYPE_PERMILLE)) {
            parser.addMatcher(PermilleMatcher.getInstance(symbols));
        }

        ///////////////////////////////
        /// OTHER STANDARD MATCHERS ///
        ///////////////////////////////

        if (parseMode == ParseMode.LENIENT) {
            parser.addMatcher(PlusSignMatcher.getInstance(symbols, false));
            parser.addMatcher(MinusSignMatcher.getInstance(symbols, false));
        }
        parser.addMatcher(NanMatcher.getInstance(symbols, parseFlags));
        parser.addMatcher(InfinityMatcher.getInstance(symbols));
        String padString = properties.getPadString();
        if (padString != null && !ignorables.getSet().contains(padString)) {
            parser.addMatcher(PaddingMatcher.getInstance(padString));
        }
        parser.addMatcher(ignorables);
        parser.addMatcher(DecimalMatcher.getInstance(symbols, grouper, parseFlags));
        // NOTE: parseNoExponent doesn't disable scientific parsing if we have a scientific formatter
        if (!properties.getParseNoExponent() || properties.getMinimumExponentDigits() > 0) {
            parser.addMatcher(ScientificMatcher.getInstance(symbols, grouper));
        }

        //////////////////
        /// VALIDATORS ///
        //////////////////

        parser.addMatcher(new RequireNumberValidator());
        if (parseMode != ParseMode.LENIENT) {
            parser.addMatcher(new RequireAffixValidator());
        }
        if (parseCurrency) {
            parser.addMatcher(new RequireCurrencyValidator());
        }
        if (properties.getDecimalPatternMatchRequired()) {
            boolean patternHasDecimalSeparator = properties.getDecimalSeparatorAlwaysShown()
                    || properties.getMaximumFractionDigits() != 0;
            parser.addMatcher(RequireDecimalSeparatorValidator.getInstance(patternHasDecimalSeparator));
        }
        // The multiplier takes care of scaling percentages.
        Scale multiplier = RoundingUtils.scaleFromProperties(properties);
        if (multiplier != null) {
            parser.addMatcher(new MultiplierParseHandler(multiplier));
        }

        parser.freeze();
        return parser;
    }

    private final int parseFlags;
    private final List<NumberParseMatcher> matchers;
    private boolean frozen;

    /**
     * Creates a new, empty parser.
     *
     * @param parseFlags
     *            The parser settings defined in the PARSE_FLAG_* fields.
     */
    public NumberParserImpl(int parseFlags) {
        matchers = new ArrayList<>();
        this.parseFlags = parseFlags;
        frozen = false;
    }

    public void addMatcher(NumberParseMatcher matcher) {
        assert !frozen;
        this.matchers.add(matcher);
    }

    public void addMatchers(Collection<? extends NumberParseMatcher> matchers) {
        assert !frozen;
        this.matchers.addAll(matchers);
    }

    public void freeze() {
        frozen = true;
    }

    public int getParseFlags() {
        return parseFlags;
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
            parseGreedy(segment, result);
        } else if (0 != (parseFlags & ParsingUtils.PARSE_FLAG_ALLOW_INFINITE_RECURSION)) {
            // Start at 1 so that recursionLevels never gets to 0
            parseLongestRecursive(segment, result, 1);
        } else {
            // Arbitrary recursion safety limit: 100 levels.
            parseLongestRecursive(segment, result, -100);
        }
        for (NumberParseMatcher matcher : matchers) {
            matcher.postProcess(result);
        }
        result.postProcess();
    }

    private void parseGreedy(StringSegment segment, ParsedNumber result) {
        // Note: this method is not recursive in order to avoid stack overflow.
        for (int i = 0; i < matchers.size();) {
            // Base Case
            if (segment.length() == 0) {
                return;
            }
            NumberParseMatcher matcher = matchers.get(i);
            if (!matcher.smokeTest(segment)) {
                // Matcher failed smoke test: try the next one
                i++;
                continue;
            }
            int initialOffset = segment.getOffset();
            matcher.match(segment, result);
            if (segment.getOffset() != initialOffset) {
                // Greedy heuristic: accept the match and loop back
                i = 0;
                continue;
            } else {
                // Matcher did not match: try the next one
                i++;
                continue;
            }
        }

        // NOTE: If we get here, the greedy parse completed without consuming the entire string.
    }

    private void parseLongestRecursive(StringSegment segment, ParsedNumber result, int recursionLevels) {
        // Base Case
        if (segment.length() == 0) {
            return;
        }

        // Safety against stack overflow
        if (recursionLevels == 0) {
            return;
        }

        // TODO: Give a nice way for the matcher to reset the ParsedNumber?
        ParsedNumber initial = new ParsedNumber();
        initial.copyFrom(result);
        ParsedNumber candidate = new ParsedNumber();

        int initialOffset = segment.getOffset();
        for (int i = 0; i < matchers.size(); i++) {
            NumberParseMatcher matcher = matchers.get(i);
            if (!matcher.smokeTest(segment)) {
                continue;
            }

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
                    parseLongestRecursive(segment, candidate, recursionLevels + 1);
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
