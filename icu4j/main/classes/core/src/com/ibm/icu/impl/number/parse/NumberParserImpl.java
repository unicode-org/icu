// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.ibm.icu.impl.number.AffixPatternProvider;
import com.ibm.icu.impl.number.AffixUtils;
import com.ibm.icu.impl.number.CustomSymbolCurrency;
import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.Parse.ParseMode;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.impl.number.PropertiesAffixPatternProvider;
import com.ibm.icu.impl.number.RoundingUtils;
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
    @Deprecated
    public static NumberParserImpl createParserFromPattern(String pattern, boolean strictGrouping) {
        // Temporary frontend for testing.

        NumberParserImpl parser = new NumberParserImpl(true, true);
        ULocale locale = new ULocale("en_IN");
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        IgnorablesMatcher ignorables = IgnorablesMatcher.DEFAULT;

        AffixPatternProvider patternInfo = PatternStringParser.parseToPatternInfo(pattern);
        AffixMatcher.generateFromAffixPatternProvider(patternInfo, parser, ignorables, true);

        parser.addMatcher(ignorables);
        DecimalMatcher decimalMatcher = new DecimalMatcher();
        decimalMatcher.requireGroupingMatch = strictGrouping;
        decimalMatcher.grouping1 = 3;
        decimalMatcher.grouping2 = 2;
        decimalMatcher.freeze(symbols, false, false);
        parser.addMatcher(decimalMatcher);
        parser.addMatcher(MinusSignMatcher.getInstance(symbols));
        parser.addMatcher(new ScientificMatcher(symbols));
        parser.addMatcher(CurrencyTrieMatcher.getInstance(locale));
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
            ppos.setIndex(result.charsConsumed);
            return result.getNumber();
        } else {
            ppos.setErrorIndex(result.charsConsumed);
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
            ppos.setIndex(result.charsConsumed);
            // TODO: Clean this up
            Currency currency;
            if (result.currencyCode != null) {
                currency = Currency.getInstance(result.currencyCode);
            } else {
                assert 0 != (result.flags & ParsedNumber.FLAG_HAS_DEFAULT_CURRENCY);
                currency = CustomSymbolCurrency.resolve(properties.getCurrency(), symbols.getULocale(), symbols);
            }
            return new CurrencyAmount(result.getNumber(), currency);
        } else {
            ppos.setErrorIndex(result.charsConsumed);
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
        NumberParserImpl parser = new NumberParserImpl(!properties.getParseCaseSensitive(), optimize);
        ULocale locale = symbols.getULocale();
        Currency currency = CustomSymbolCurrency.resolve(properties.getCurrency(), locale, symbols);
        boolean isStrict = properties.getParseMode() == ParseMode.STRICT;
        IgnorablesMatcher ignorables = isStrict ? IgnorablesMatcher.STRICT : IgnorablesMatcher.DEFAULT;

        boolean decimalSeparatorRequired = properties.getDecimalPatternMatchRequired()
                ? (properties.getDecimalSeparatorAlwaysShown() || properties.getMaximumFractionDigits() != 0)
                : false;

        //////////////////////
        /// AFFIX MATCHERS ///
        //////////////////////

        // Set up a pattern modifier with mostly defaults to generate AffixMatchers.
        AffixPatternProvider patternInfo = new PropertiesAffixPatternProvider(properties);
        AffixMatcher.generateFromAffixPatternProvider(patternInfo, parser, ignorables, !isStrict);

        ////////////////////////
        /// CURRENCY MATCHER ///
        ////////////////////////

        if (parseCurrency || patternInfo.hasCurrencySign()) {
            parser.addMatcher(CurrencyTrieMatcher.getInstance(locale));
            parser.addMatcher(CurrencyMatcher.getInstance(currency, locale));
        }

        ///////////////////////////////
        /// OTHER STANDARD MATCHERS ///
        ///////////////////////////////

        if (!isStrict || patternInfo.containsSymbolType(AffixUtils.TYPE_PLUS_SIGN) || properties.getSignAlwaysShown()) {
            parser.addMatcher(PlusSignMatcher.getInstance(symbols));
        }
        parser.addMatcher(MinusSignMatcher.getInstance(symbols));
        parser.addMatcher(NanMatcher.getInstance(symbols));
        parser.addMatcher(PercentMatcher.getInstance(symbols));
        parser.addMatcher(PermilleMatcher.getInstance(symbols));
        parser.addMatcher(InfinityMatcher.getInstance(symbols));
        String padString = properties.getPadString();
        if (padString != null && !ignorables.getSet().contains(padString)) {
            parser.addMatcher(new PaddingMatcher(padString));
        }
        parser.addMatcher(ignorables);
        DecimalMatcher decimalMatcher = new DecimalMatcher();
        decimalMatcher.requireGroupingMatch = isStrict;
        decimalMatcher.groupingEnabled = properties.getGroupingSize() > 0;
        decimalMatcher.decimalEnabled = properties.getDecimalPatternMatchRequired() ? decimalSeparatorRequired : true;
        decimalMatcher.grouping1 = properties.getGroupingSize();
        decimalMatcher.grouping2 = properties.getSecondaryGroupingSize();
        decimalMatcher.integerOnly = properties.getParseIntegerOnly();
        decimalMatcher.freeze(symbols, parseCurrency || patternInfo.hasCurrencySign(), isStrict);
        parser.addMatcher(decimalMatcher);
        if (!properties.getParseNoExponent()) {
            parser.addMatcher(new ScientificMatcher(symbols));
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
        if (decimalSeparatorRequired) {
            parser.addMatcher(new RequireDecimalSeparatorMatcher());
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

    private final boolean ignoreCase;
    private final List<NumberParseMatcher> matchers;
    private final List<UnicodeSet> leadCharses;
    private Comparator<ParsedNumber> comparator;
    private boolean frozen;

    /**
     * Creates a new, empty parser.
     *
     * @param ignoreCase
     *            If true, perform case-folding. This parameter needs to go into the constructor because its value is
     *            used during the construction of the matcher chain.
     * @param optimize
     *            If true, compute "lead chars" UnicodeSets for the matchers. This reduces parsing runtime but increases
     *            construction runtime. If the parser is going to be used only once or twice, set this to false; if it
     *            is going to be used hundreds of times, set it to true.
     */
    public NumberParserImpl(boolean ignoreCase, boolean optimize) {
        matchers = new ArrayList<NumberParseMatcher>();
        if (optimize) {
            leadCharses = new ArrayList<UnicodeSet>();
        } else {
            leadCharses = null;
        }
        comparator = ParsedNumber.COMPARATOR; // default value
        this.ignoreCase = ignoreCase;
        frozen = false;
    }

    public void addMatcher(NumberParseMatcher matcher) {
        assert !frozen;
        this.matchers.add(matcher);
        if (leadCharses != null) {
            UnicodeSet leadChars = matcher.getLeadChars(ignoreCase);
            assert leadChars.isFrozen();
            this.leadCharses.add(leadChars);
        }
    }

    public void addMatchers(Collection<? extends NumberParseMatcher> matchers) {
        assert !frozen;
        this.matchers.addAll(matchers);
        if (leadCharses != null) {
            for (NumberParseMatcher matcher : matchers) {
                UnicodeSet leadChars = matcher.getLeadChars(ignoreCase);
                assert leadChars.isFrozen();
                this.leadCharses.add(leadChars);
            }
        }
    }

    public void setComparator(Comparator<ParsedNumber> comparator) {
        assert !frozen;
        this.comparator = comparator;
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
     *            The string to parse. This is a String, not CharSequence, to enforce assumptions about immutability
     *            (CharSequences are not guaranteed to be immutable).
     * @param start
     *            The index into the string at which to start parsing.
     * @param greedy
     *            Whether to use the faster but potentially less accurate greedy code path.
     * @param result
     *            Output variable to store results.
     */
    public void parse(String input, int start, boolean greedy, ParsedNumber result) {
        assert frozen;
        StringSegment segment = new StringSegment(input, ignoreCase);
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
        char leadChar = leadCharses == null ? 0
                : ignoreCase ? ParsingUtils.getCaseFoldedLeadingChar(segment) : segment.charAt(0);
        for (int i = 0; i < matchers.size(); i++) {
            if (leadCharses != null && !leadCharses.get(i).contains(leadChar)) {
                continue;
            }
            NumberParseMatcher matcher = matchers.get(i);
            matcher.match(segment, result);
            if (segment.getOffset() != initialOffset) {
                // In a greedy parse, recurse on only the first match.
                parseGreedyRecursive(segment, result);
                // The following line resets the offset so that the StringSegment says the same across the function
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
            NumberParseMatcher matcher = matchers.get(i);
            // In a non-greedy parse, we attempt all possible matches and pick the best.
            for (int charsToConsume = 1; charsToConsume <= segment.length(); charsToConsume++) {
                candidate.copyFrom(initial);

                // Run the matcher on a segment of the current length.
                segment.setLength(charsToConsume);
                boolean maybeMore = matcher.match(segment, candidate);
                segment.resetLength();

                // If the entire segment was consumed, recurse.
                if (segment.getOffset() - initialOffset == charsToConsume) {
                    parseLongestRecursive(segment, candidate);
                    if (comparator.compare(candidate, result) > 0) {
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
