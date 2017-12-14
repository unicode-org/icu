// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.ibm.icu.impl.number.AffixPatternProvider;
import com.ibm.icu.impl.number.AffixUtils;
import com.ibm.icu.impl.number.CustomSymbolCurrency;
import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.MutablePatternModifier;
import com.ibm.icu.impl.number.Parse.ParseMode;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.impl.number.PropertiesAffixPatternProvider;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
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
    public static NumberParserImpl createParserFromPattern(String pattern, boolean strictGrouping) {
        // Temporary frontend for testing.

        NumberParserImpl parser = new NumberParserImpl();
        ULocale locale = new ULocale("en_IN");
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);

        MutablePatternModifier mod = new MutablePatternModifier(false);
        AffixPatternProvider provider = PatternStringParser.parseToPatternInfo(pattern);
        mod.setPatternInfo(provider);
        mod.setPatternAttributes(SignDisplay.AUTO, false);
        mod.setSymbols(symbols, Currency.getInstance("USD"), UnitWidth.FULL_NAME, null);
        int flags = 0;
        if (provider.containsSymbolType(AffixUtils.TYPE_PERCENT)) {
            flags |= ParsedNumber.FLAG_PERCENT;
        }
        if (provider.containsSymbolType(AffixUtils.TYPE_PERMILLE)) {
            flags |= ParsedNumber.FLAG_PERMILLE;
        }
        AffixMatcher.generateFromPatternModifier(mod, flags, true, parser);

        parser.addMatcher(WhitespaceMatcher.getInstance());
        DecimalMatcher decimalMatcher = DecimalMatcher.getInstance(symbols);
        decimalMatcher.requireGroupingMatch = strictGrouping;
        decimalMatcher.grouping1 = 3;
        decimalMatcher.grouping2 = 2;
        parser.addMatcher(decimalMatcher);
        parser.addMatcher(new MinusSignMatcher());
        parser.addMatcher(new ScientificMatcher(symbols));
        parser.addMatcher(new CurrencyMatcher(locale));
        parser.addMatcher(new RequireNumberMatcher());

        parser.freeze();
        return parser;
    }

    public static Number parseStatic(String input,
      ParsePosition ppos,
      DecimalFormatProperties properties,
      DecimalFormatSymbols symbols) {
        NumberParserImpl parser = createParserFromProperties(properties, symbols, false);
        ParsedNumber result = new ParsedNumber();
        parser.parse(input, true, result);
        ppos.setIndex(result.charsConsumed);
        if (result.charsConsumed > 0) {
            return result.getDouble();
        } else {
            return null;
        }
    }

    public static CurrencyAmount parseStaticCurrency(String input,
      ParsePosition ppos,
      DecimalFormatProperties properties,
      DecimalFormatSymbols symbols) {
        NumberParserImpl parser = createParserFromProperties(properties, symbols, true);
        ParsedNumber result = new ParsedNumber();
        parser.parse(input, true, result);
        ppos.setIndex(result.charsConsumed);
        if (result.charsConsumed > 0) {
            // TODO: Clean this up
            Currency currency;
            if (result.currencyCode != null) {
                currency = Currency.getInstance(result.currencyCode);
            } else {
                assert 0 != (result.flags & ParsedNumber.FLAG_HAS_DEFAULT_CURRENCY);
                currency = CustomSymbolCurrency.resolve(properties.getCurrency(), symbols.getULocale(), symbols);
            }
            return new CurrencyAmount(result.getDouble(), currency);
        } else {
            return null;
        }
    }

    public static NumberParserImpl createParserFromProperties(
            DecimalFormatProperties properties,
            DecimalFormatSymbols symbols,
            boolean parseCurrency) {
        NumberParserImpl parser = new NumberParserImpl();
        ULocale locale = symbols.getULocale();
        Currency currency = CustomSymbolCurrency.resolve(properties.getCurrency(), locale, symbols);
        boolean isStrict = properties.getParseMode() == ParseMode.STRICT;

        ////////////////////////
        /// CURRENCY MATCHER ///
        ////////////////////////

        if (parseCurrency) {
            parser.addMatcher(new CurrencyMatcher(locale));
        }

        //////////////////////
        /// AFFIX MATCHERS ///
        //////////////////////

        // Set up a pattern modifier with mostly defaults to generate AffixMatchers.
        MutablePatternModifier mod = new MutablePatternModifier(false);
        AffixPatternProvider patternInfo = new PropertiesAffixPatternProvider(properties);
        mod.setPatternInfo(patternInfo);
        mod.setPatternAttributes(SignDisplay.AUTO, false);
        mod.setSymbols(symbols, currency, UnitWidth.SHORT, null);

        // Figure out which flags correspond to this pattern modifier. Note: negatives are taken care of in the
        // generateFromPatternModifier function.
        int flags = 0;
        if (patternInfo.containsSymbolType(AffixUtils.TYPE_PERCENT)) {
            flags |= ParsedNumber.FLAG_PERCENT;
        }
        if (patternInfo.containsSymbolType(AffixUtils.TYPE_PERMILLE)) {
            flags |= ParsedNumber.FLAG_PERMILLE;
        }
        if (patternInfo.hasCurrencySign()) {
            flags |= ParsedNumber.FLAG_HAS_DEFAULT_CURRENCY;
        }

        parseCurrency = parseCurrency || patternInfo.hasCurrencySign();

        AffixMatcher.generateFromPatternModifier(mod, flags, !isStrict && !parseCurrency, parser);

        ///////////////////////////////
        /// OTHER STANDARD MATCHERS ///
        ///////////////////////////////

        if (!isStrict) {
            parser.addMatcher(WhitespaceMatcher.getInstance());
            parser.addMatcher(new PlusSignMatcher());
        }
        parser.addMatcher(new MinusSignMatcher());
        DecimalMatcher decimalMatcher = DecimalMatcher.getInstance(symbols);
        decimalMatcher.groupingEnabled = properties.getGroupingSize() > 0;
        decimalMatcher.requireGroupingMatch = isStrict;
        decimalMatcher.grouping1 = properties.getGroupingSize();
        decimalMatcher.grouping2 = properties.getSecondaryGroupingSize();
        decimalMatcher.integerOnly = properties.getParseIntegerOnly();
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

        ////////////////////////
        /// OTHER ATTRIBUTES ///
        ////////////////////////

        parser.setIgnoreCase(!properties.getParseCaseSensitive());

        parser.freeze();
        return parser;
    }

    private final List<NumberParseMatcher> matchers;
    private Comparator<ParsedNumber> comparator;
    private boolean ignoreCase;
    private boolean frozen;

    public NumberParserImpl() {
        matchers = new ArrayList<NumberParseMatcher>();
        comparator = ParsedNumber.COMPARATOR; // default value
        ignoreCase = true;
        frozen = false;
    }

    public void addMatcher(NumberParseMatcher matcher) {
        matchers.add(matcher);
    }

    public void setComparator(Comparator<ParsedNumber> comparator) {
        this.comparator = comparator;
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public void freeze() {
        frozen = true;
    }

    public void parse(String input, boolean greedy, ParsedNumber result) {
        assert frozen;
        StringSegment segment = new StringSegment(input, ignoreCase);
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
        for (int i = 0; i < matchers.size(); i++) {
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
