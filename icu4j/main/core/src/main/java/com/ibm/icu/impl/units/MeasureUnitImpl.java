// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.units;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.ibm.icu.util.BytesTrie;
import com.ibm.icu.util.CharsTrie;
import com.ibm.icu.util.CharsTrieBuilder;
import com.ibm.icu.util.ICUCloneNotSupportedException;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.StringTrieBuilder;


public class MeasureUnitImpl {

    /**
     * The full unit identifier. Null if not computed.
     */
    private String identifier = null;
    /**
     * The complexity, either SINGLE, COMPOUND, or MIXED.
     */
    private MeasureUnit.Complexity complexity = MeasureUnit.Complexity.SINGLE;
    /**
     * The constant denominator.
     * 
     * NOTE: when it is 0, it means there is no constant denominator.
     */
    private long constantDenominator = 0;
    /**
     * The list of single units. These may be summed or multiplied, based on the
     * value of the complexity field.
     * <p>
     * The "dimensionless" unit (SingleUnitImpl default constructor) must not be added to this list.
     * <p>
     * The "dimensionless" <code>MeasureUnitImpl</code> has an empty <code>singleUnits</code>.
     */
    private final ArrayList<SingleUnitImpl> singleUnits;

    public MeasureUnitImpl() {
        singleUnits = new ArrayList<>();
    }

    public MeasureUnitImpl(SingleUnitImpl singleUnit) {
        this();
        this.appendSingleUnit(singleUnit);
    }

    /**
     * Parse a unit identifier into a MeasureUnitImpl.
     *
     * @param identifier The unit identifier string.
     * @return A newly parsed object.
     * @throws IllegalArgumentException in case of incorrect/non-parsed identifier.
     */
    public static MeasureUnitImpl forIdentifier(String identifier) {
        return UnitsParser.parseForIdentifier(identifier);
    }

    /**
     * Used for currency units.
     */
    public static MeasureUnitImpl forCurrencyCode(String currencyCode) {
        MeasureUnitImpl result = new MeasureUnitImpl();
        result.identifier = currencyCode;
        return result;
    }

    public MeasureUnitImpl copy() {
        MeasureUnitImpl result = new MeasureUnitImpl();
        result.complexity = this.complexity;
        result.identifier = this.identifier;
        result.constantDenominator = this.constantDenominator;
        for (SingleUnitImpl singleUnit : this.singleUnits) {
            result.singleUnits.add(singleUnit.copy());
        }
        return result;
    }

    /**
     * Returns a simplified version of the unit.
     * NOTE: the simplification happen when there are two units equals in their base unit and their
     * prefixes.
     *
     * Example 1: "square-meter-per-meter" --> "meter"
     * Example 2: "square-millimeter-per-meter" --> "square-millimeter-per-meter"
     */
    public MeasureUnitImpl copyAndSimplify() {
        MeasureUnitImpl result = new MeasureUnitImpl();
        for (SingleUnitImpl singleUnit : this.getSingleUnits()) {
            // This `for` loop will cause time complexity to be O(n^2).
            // However, n is very small (number of units, generally, at maximum equal to 10)
            boolean unitExist = false;
            for (SingleUnitImpl resultSingleUnit : result.getSingleUnits()) {
                if(resultSingleUnit.getSimpleUnitID().compareTo(singleUnit.getSimpleUnitID()) == 0
                &&
                        resultSingleUnit.getPrefix().getIdentifier().compareTo(singleUnit.getPrefix().getIdentifier()) == 0
                ) {
                    unitExist = true;
                    resultSingleUnit.setDimensionality(resultSingleUnit.getDimensionality() + singleUnit.getDimensionality());
                    break;
                }
            }

            if(!unitExist) {
                result.appendSingleUnit(singleUnit);
            }
        }

        return result;
    }

    /**
     * Returns the list of simple units.
     */
    public ArrayList<SingleUnitImpl> getSingleUnits() {
        return singleUnits;
    }

    /**
     * Mutates this MeasureUnitImpl to take the reciprocal.
     */
    public void takeReciprocal() {
        this.identifier = null;
        for (SingleUnitImpl singleUnit :
                this.singleUnits) {
            singleUnit.setDimensionality(singleUnit.getDimensionality() * -1);
        }
    }

    public ArrayList<MeasureUnitImplWithIndex> extractIndividualUnitsWithIndices() {
        ArrayList<MeasureUnitImplWithIndex> result = new ArrayList<>();
        if (this.getComplexity() == MeasureUnit.Complexity.MIXED) {
            // In case of mixed units, each single unit can be considered as a stand alone MeasureUnitImpl.
            int i = 0;
            for (SingleUnitImpl singleUnit :
                    this.getSingleUnits()) {
                result.add(new MeasureUnitImplWithIndex(i++, new MeasureUnitImpl(singleUnit)));
            }

            return result;
        }

        result.add(new MeasureUnitImplWithIndex(0, this.copy()));
        return result;
    }

    /**
     * Applies dimensionality to all the internal single units.
     * For example: <b>square-meter-per-second</b>, when we apply dimensionality -2, it will be <b>square-second-per-p4-meter</b>
     */
    public void applyDimensionality(int dimensionality) {
        for (SingleUnitImpl singleUnit :
                singleUnits) {
            singleUnit.setDimensionality(singleUnit.getDimensionality() * dimensionality);
        }
    }

    /**
     * Mutates this MeasureUnitImpl to append a single unit.
     *
     * @return true if a new item was added. If unit is the dimensionless unit,
     * it is never added: the return value will always be false.
     */
    public boolean appendSingleUnit(SingleUnitImpl singleUnit) {
        identifier = null;

        if (singleUnit == null) {
            // Do not append dimensionless units.
            return false;
        }

        // Find a similar unit that already exists, to attempt to coalesce
        SingleUnitImpl oldUnit = null;
        for (SingleUnitImpl candidate : this.singleUnits) {
            if (candidate.isCompatibleWith(singleUnit)) {
                oldUnit = candidate;
                break;
            }
        }

        if (oldUnit != null) {
            // Both dimensionalities will be positive, or both will be negative, by
            // virtue of isCompatibleWith().
            oldUnit.setDimensionality(oldUnit.getDimensionality() + singleUnit.getDimensionality());

            return false;
        }

        // Add a copy of singleUnit
        this.singleUnits.add(singleUnit.copy());

        // If the MeasureUnitImpl is `UMEASURE_UNIT_SINGLE` and after the appending a unit, the singleUnits contains
        // more than one. thus means the complexity should be `UMEASURE_UNIT_COMPOUND`
        if (this.singleUnits.size() > 1 && this.complexity == MeasureUnit.Complexity.SINGLE) {
            this.setComplexity(MeasureUnit.Complexity.COMPOUND);
        }

        return true;
    }

    /**
     * Transform this MeasureUnitImpl into a MeasureUnit, simplifying if possible.
     * <p>
     * NOTE: this function must be called from a thread-safe class
     */
    public MeasureUnit build() {
        return MeasureUnit.fromMeasureUnitImpl(this);
    }

    /**
     * @return SingleUnitImpl
     * @throws UnsupportedOperationException if the object could not be converted to SingleUnitImpl.
     */
    public SingleUnitImpl getSingleUnitImpl() {
        if (this.singleUnits.size() == 0) {
            return new SingleUnitImpl();
        }
        if (this.singleUnits.size() == 1) {
            return this.singleUnits.get(0).copy();
        }

        throw new UnsupportedOperationException();
    }

    /**
     * Returns the CLDR unit identifier and null if not computed.
     */
    public String getIdentifier() {
        return identifier;
    }

    public MeasureUnit.Complexity getComplexity() {
        return complexity;
    }

    public void setComplexity(MeasureUnit.Complexity complexity) {
        this.complexity = complexity;
    }

    /**
     * Get the constant denominator.
     */
    public long getConstantDenominator() {
        return constantDenominator;
    }

    /**
     * Set the constant denominator.
     */
    public void setConstantDenominator(long constantDenominator) {
        this.constantDenominator = constantDenominator;
    }

    private int countCharacter(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }

    /**
     * Internal function that returns a string of the constants in the correct
     * format.
     * 
     * Example:
     * 1000 --> "-per-1000"
     * 1000000 --> "-per-1e6"
     * 
     * NOTE: this function is only used when the constant denominator is greater
     * than 0.
     */
    private String getConstantsString(long constantDenominator) {
        assert constantDenominator > 0;
        StringBuilder constantString = new StringBuilder();
        constantString.append(constantDenominator);
        String result = constantString.toString();

        if (constantDenominator <= 1000) {
            return result;
        }

        // Check if the constant denominator is a power of 10
        int zeroCount = countCharacter(result, '0');
        if (zeroCount == result.length() - 1 && result.charAt(0) == '1') {
            return "1e" + zeroCount;
        }

        return result;
    }

    /**
     * Normalizes the MeasureUnitImpl and generates the identifier string in place.
     */
    public void serialize() {
        if (this.getSingleUnits().size() == 0) {
            // Dimensionless, constructed by the default constructor: no appending
            // to this.result, we wish it to contain the zero-length string.
            return;
        }

        if (this.complexity == MeasureUnit.Complexity.COMPOUND) {
            // Note: don't sort a MIXED unit
            Collections.sort(this.getSingleUnits(), new SingleUnitComparator());
        }

        StringBuilder result = new StringBuilder();
        boolean beforePer = true;
        boolean firstTimeNegativeDimension = false;
        boolean isConstantDenominatorAdded = false;
        for (SingleUnitImpl singleUnit : this.getSingleUnits()) {
            if (beforePer && singleUnit.getDimensionality() < 0) {
                beforePer = false;
                firstTimeNegativeDimension = true;
            } else if (singleUnit.getDimensionality() < 0) {
                firstTimeNegativeDimension = false;
            }

            if (firstTimeNegativeDimension && this.constantDenominator > 0) {
                result.append("-per-");
                result.append(getConstantsString(this.constantDenominator));
                firstTimeNegativeDimension = false;
                isConstantDenominatorAdded = true;
            }

            if (this.getComplexity() == MeasureUnit.Complexity.MIXED) {
                if (result.length() != 0) {
                    result.append("-and-");
                }
            } else {
                if (firstTimeNegativeDimension) {
                    if (result.length() == 0) {
                        result.append("per-");
                    } else {
                        result.append("-per-");
                    }
                } else {
                    if (result.length() != 0) {
                        result.append("-");
                    }
                }
            }

            result.append(singleUnit.getNeutralIdentifier());
        }

        if (this.constantDenominator > 0 && !isConstantDenominatorAdded) {
            result.append("-per-");
            result.append(getConstantsString(this.constantDenominator));
        }

        this.identifier = result.toString();
    }

    @Override
    public String toString() {
        return "MeasureUnitImpl [" + build().getIdentifier() + "]";
    }

    public enum CompoundPart {
        // Represents "-per-"
        PER(0),
        // Represents "-"
        TIMES(1),
        // Represents "-and-"
        AND(2);

        private final int index;

        CompoundPart(int index) {
            this.index = index;
        }

        public static CompoundPart getCompoundPartFromTrieIndex(int trieIndex) {
            int index = trieIndex - UnitsData.Constants.kCompoundPartOffset;
            switch (index) {
                case 0:
                    return CompoundPart.PER;
                case 1:
                    return CompoundPart.TIMES;
                case 2:
                    return CompoundPart.AND;
                default:
                    throw new AssertionError("CompoundPart index must be 0, 1 or 2");
            }
        }

        public int getTrieIndex() {
            return this.index + UnitsData.Constants.kCompoundPartOffset;
        }

        public int getValue() {
            return index;
        }
    }

    public enum PowerPart {
        P2(2),
        P3(3),
        P4(4),
        P5(5),
        P6(6),
        P7(7),
        P8(8),
        P9(9),
        P10(10),
        P11(11),
        P12(12),
        P13(13),
        P14(14),
        P15(15);

        private final int power;

        PowerPart(int power) {
            this.power = power;
        }

        public static int getPowerFromTrieIndex(int trieIndex) {
            return trieIndex - UnitsData.Constants.kPowerPartOffset;
        }

        public int getTrieIndex() {
            return this.power + UnitsData.Constants.kPowerPartOffset;
        }

        public int getValue() {
            return power;
        }
    }

    public enum InitialCompoundPart {

        // Represents "per-", the only compound part that can appear at the start of
        // an identifier.
        INITIAL_COMPOUND_PART_PER(0);

        private final int index;

        InitialCompoundPart(int powerIndex) {
            this.index = powerIndex;
        }

        public static InitialCompoundPart getInitialCompoundPartFromTrieIndex(int trieIndex) {
            int index = trieIndex - UnitsData.Constants.kInitialCompoundPartOffset;
            if (index == 0) {
                return INITIAL_COMPOUND_PART_PER;
            }

            throw new IllegalArgumentException("Incorrect trieIndex");
        }

        public int getTrieIndex() {
            return this.index + UnitsData.Constants.kInitialCompoundPartOffset;
        }

        public int getValue() {
            return index;
        }

    }

    public static class MeasureUnitImplWithIndex {
        int index;
        MeasureUnitImpl unitImpl;

        MeasureUnitImplWithIndex(int index, MeasureUnitImpl unitImpl) {
            this.index = index;
            this.unitImpl = unitImpl;
        }
    }

    public static class UnitsParser {

        /**
         * Contains a single unit or a constant.
         * 
         * @throws IllegalArgumentException when both singleUnit and constant are
         *                                  existing.
         * @param singleUnit the single unit
         * @param constant   the constant
         */
        private class SingleUnitOrConstant {
            SingleUnitImpl singleUnit;
            Long constant;

            SingleUnitOrConstant(SingleUnitImpl singleUnit, Long constant) {
                if (singleUnit != null && constant != null) {
                    throw new IllegalArgumentException("It is a SingleUnit Or a Constant, not both");
                }
                this.singleUnit = singleUnit;
                this.constant = constant;
            }
        }

        // This used only to not build the trie each time we use the parser
        private volatile static CharsTrie savedTrie = null;

        // This trie used in the parsing operation.
        private final CharsTrie trie;
        private final String fSource;
        // Tracks parser progress: the offset into fSource.
        private int fIndex = 0;
        // Set to true when we've seen a "-per-" or a "per-", after which all units
        // are in the denominator. Until we find an "-and-", at which point the
        // identifier is invalid pending TODO(CLDR-13701).
        private boolean fAfterPer = false;

        // Set to true when we just parsed a "-per-" or a "per-".
        // This is used to ensure that the unit constant (such as "per-100-kilometer")
        // can be parsed when it occurs after a "-per-" or a "per-".
        private boolean fJustAfterPer = false;

        // If an "-and-" was parsed prior to finding the "single
        // * unit", sawAnd is set to true. If not, it is left as is.
        private boolean fSawAnd = false;

        // Cache the MeasurePrefix values array to make getPrefixFromTrieIndex()
        // more efficient
        private static MeasureUnit.MeasurePrefix[] measurePrefixValues = MeasureUnit.MeasurePrefix.values();

        private UnitsParser(String identifier) {
            this.fSource = identifier;

            try {
                this.trie = UnitsParser.savedTrie.clone();
            } catch (CloneNotSupportedException e) {
                throw new ICUCloneNotSupportedException();
            }
        }

        static {
            // Build Units trie.
            CharsTrieBuilder trieBuilder;
            trieBuilder = new CharsTrieBuilder();

            // Add SI and binary prefixes
            for (MeasureUnit.MeasurePrefix unitPrefix : measurePrefixValues) {
                trieBuilder.add(unitPrefix.getIdentifier(), getTrieIndexForPrefix(unitPrefix));
            }

            // Add syntax parts (compound, power prefixes)
            trieBuilder.add("-per-", CompoundPart.PER.getTrieIndex());
            trieBuilder.add("-", CompoundPart.TIMES.getTrieIndex());
            trieBuilder.add("-and-", CompoundPart.AND.getTrieIndex());
            trieBuilder.add("per-", InitialCompoundPart.INITIAL_COMPOUND_PART_PER.getTrieIndex());
            trieBuilder.add("square-", PowerPart.P2.getTrieIndex());
            trieBuilder.add("cubic-", PowerPart.P3.getTrieIndex());
            trieBuilder.add("pow2-", PowerPart.P2.getTrieIndex());
            trieBuilder.add("pow3-", PowerPart.P3.getTrieIndex());
            trieBuilder.add("pow4-", PowerPart.P4.getTrieIndex());
            trieBuilder.add("pow5-", PowerPart.P5.getTrieIndex());
            trieBuilder.add("pow6-", PowerPart.P6.getTrieIndex());
            trieBuilder.add("pow7-", PowerPart.P7.getTrieIndex());
            trieBuilder.add("pow8-", PowerPart.P8.getTrieIndex());
            trieBuilder.add("pow9-", PowerPart.P9.getTrieIndex());
            trieBuilder.add("pow10-", PowerPart.P10.getTrieIndex());
            trieBuilder.add("pow11-", PowerPart.P11.getTrieIndex());
            trieBuilder.add("pow12-", PowerPart.P12.getTrieIndex());
            trieBuilder.add("pow13-", PowerPart.P13.getTrieIndex());
            trieBuilder.add("pow14-", PowerPart.P14.getTrieIndex());
            trieBuilder.add("pow15-", PowerPart.P15.getTrieIndex());

            // Add simple units
            String[] simpleUnits = UnitsData.getSimpleUnits();
            for (int i = 0; i < simpleUnits.length; i++) {
                trieBuilder.add(simpleUnits[i], i + UnitsData.Constants.kSimpleUnitOffset);

            }

            // TODO: Use SLOW or FAST here?
            UnitsParser.savedTrie = trieBuilder.build(StringTrieBuilder.Option.FAST);
        }

        /**
         * Construct a MeasureUnit from a CLDR Unit Identifier, defined in UTS 35.
         * Validates and canonicalizes the identifier.
         *
         * @return MeasureUnitImpl object or null if the identifier is empty.
         * @throws IllegalArgumentException in case of invalid identifier.
         */
        public static MeasureUnitImpl parseForIdentifier(String identifier) {
            if (identifier == null || identifier.isEmpty()) {
                return null;
            }

            UnitsParser parser = new UnitsParser(identifier);
            return parser.parse();

        }

        private static MeasureUnit.MeasurePrefix getPrefixFromTrieIndex(int trieIndex) {
            return measurePrefixValues[trieIndex - UnitsData.Constants.kPrefixOffset];
        }

        private static int getTrieIndexForPrefix(MeasureUnit.MeasurePrefix prefix) {
            return prefix.ordinal() + UnitsData.Constants.kPrefixOffset;
        }

        private MeasureUnitImpl parse() {
            MeasureUnitImpl result = new MeasureUnitImpl();

            if (fSource.isEmpty()) {
                // The dimensionless unit: nothing to parse. return null.
                return null;
            }

            while (hasNext()) {
                fSawAnd = false;
                SingleUnitOrConstant nextSingleUnitPair = nextSingleUnit();

                if (nextSingleUnitPair.singleUnit == null) {
                    result.setConstantDenominator(nextSingleUnitPair.constant);
                    result.setComplexity(MeasureUnit.Complexity.COMPOUND);
                    continue;
                }

                SingleUnitImpl singleUnit = nextSingleUnitPair.singleUnit;

                boolean added = result.appendSingleUnit(singleUnit);
                if (fSawAnd && !added) {
                    throw new IllegalArgumentException("Two similar units are not allowed in a mixed unit.");
                }

                if ((result.singleUnits.size()) >= 2) {
                    // nextSingleUnit fails appropriately for "per" and "and" in the
                    // same identifier. It doesn't fail for other compound units
                    // (COMPOUND_PART_TIMES). Consequently we take care of that
                    // here.
                    MeasureUnit.Complexity complexity = fSawAnd ? MeasureUnit.Complexity.MIXED
                            : MeasureUnit.Complexity.COMPOUND;
                    if (result.getSingleUnits().size() == 2) {
                        // After appending two singleUnits, the complexity will be
                        // MeasureUnit.Complexity.COMPOUND
                        assert result.getComplexity() == MeasureUnit.Complexity.COMPOUND;
                        result.setComplexity(complexity);
                    } else if (result.getComplexity() != complexity) {
                        throw new IllegalArgumentException("Can't have mixed compound units");
                    }
                }
            }

            if (result.getSingleUnits().size() == 0) {
                throw new IllegalArgumentException("Error in parsing a unit identifier.");
            }

            return result;
        }

        /**
         * Token states definitions.
         */
        enum TokenState {
            // No tokens seen yet (will accept power, SI or binary prefix, or simple unit)
            NO_TOKENS_SEEN,
            // Power token seen (will not accept another power token)
            POWER_TOKEN_SEEN,
            // SI or binary prefix token seen (will not accept a power, or SI or binary prefix token)
            PREFIX_TOKEN_SEEN
        }

        /**
         * Returns the next "single unit" via result.
         * <p>
         * If a "-per-" was parsed, the result will have appropriate negative
         * dimensionality.
         * <p>
         *
         * @throws IllegalArgumentException if we parse both compound units and "-and-",
         *                                  since mixed
         *                                  compound units are not yet supported -
         *                                  TODO(CLDR-13701).
         */
        private SingleUnitOrConstant nextSingleUnit() {
            SingleUnitImpl result = new SingleUnitImpl();

            TokenState state = TokenState.NO_TOKENS_SEEN;

            boolean atStart = fIndex == 0;
            Token token = nextToken();
            fJustAfterPer = false;

            if (atStart) {
                if (token.getType() == Token.Type.TYPE_UNIT_CONSTANT) {
                    throw new IllegalArgumentException("Unit constant cannot be the first token");
                }
                // Identifiers optionally start with "per-".
                if (token.getType() == Token.Type.TYPE_INITIAL_COMPOUND_PART) {
                    assert token.getInitialCompoundPart() == InitialCompoundPart.INITIAL_COMPOUND_PART_PER;

                    fAfterPer = true;
                    fJustAfterPer = true;
                    result.setDimensionality(-1);

                    token = nextToken();
                }
            } else {
                // All other SingleUnit's are separated from previous SingleUnit's
                // via a compound part:
                if (token.getType() != Token.Type.TYPE_COMPOUND_PART) {
                    throw new IllegalArgumentException("token type must be TYPE_COMPOUND_PART");
                }

                CompoundPart compoundPart = CompoundPart.getCompoundPartFromTrieIndex(token.getMatch());
                switch (compoundPart) {
                    case PER:
                        if (fSawAnd) {
                            throw new IllegalArgumentException("Mixed compound units not yet supported");
                            // TODO(CLDR-13701).
                        }

                        fAfterPer = true;
                        fJustAfterPer = true;
                        result.setDimensionality(-1);
                        break;

                    case TIMES:
                        if (fAfterPer) {
                            result.setDimensionality(-1);
                        }
                        break;

                    case AND:
                        if (fAfterPer) {
                            // not yet supported, TODO(CLDR-13701).
                            throw new IllegalArgumentException("Can't start with \"-and-\", and mixed compound units");
                        }
                        fSawAnd = true;
                        break;
                }

                token = nextToken();
            }

            // Treat unit constant
            if (token.getType() == Token.Type.TYPE_UNIT_CONSTANT) {
                if (!fJustAfterPer) {
                    throw new IllegalArgumentException("Unit constant cannot be the first token");
                }

                return new SingleUnitOrConstant(null, token.getConstantDenominator());
            }

            // Read tokens until we have a complete SingleUnit or we reach the end.
            while (true) {
                switch (token.getType()) {
                    case TYPE_POWER_PART:
                        if (state != TokenState.NO_TOKENS_SEEN) {
                            throw new IllegalArgumentException();
                        }

                        result.setDimensionality(result.getDimensionality() * token.getPower());
                        state = TokenState.POWER_TOKEN_SEEN;
                        break;

                    case TYPE_PREFIX:
                        if (state == TokenState.PREFIX_TOKEN_SEEN) {
                            throw new IllegalArgumentException();
                        }

                        result.setPrefix(token.getPrefix());
                        state = TokenState.PREFIX_TOKEN_SEEN;
                        break;

                    case TYPE_SIMPLE_UNIT:
                        result.setSimpleUnit(token.getSimpleUnitIndex(), UnitsData.getSimpleUnits());
                        return new SingleUnitOrConstant(result, null);

                    default:
                        throw new IllegalArgumentException();
                }

                if (!hasNext()) {
                    throw new IllegalArgumentException("We ran out of tokens before finding a complete single unit.");
                }

                token = nextToken();
            }
        }

        private boolean hasNext() {
            return fIndex < fSource.length();
        }

        private Token nextToken() {
            trie.reset();
            int matchingValue = -1;
            // Saves the position in the `fSource` string at the end of the most
            // recently matched token.
            int prevIndex = -1;

            int savedIndex = fIndex;

            // Find the longest token that matches a value in the trie:
            while (fIndex < fSource.length()) {
                BytesTrie.Result result = trie.next(fSource.charAt(fIndex++));
                if (result == BytesTrie.Result.NO_MATCH) {
                    break;
                }

                if (result == BytesTrie.Result.NO_VALUE) {
                    continue;
                }

                matchingValue = trie.getValue();
                prevIndex = fIndex;

                if (result == BytesTrie.Result.FINAL_VALUE) {
                    break;
                }

                if (result != BytesTrie.Result.INTERMEDIATE_VALUE) {
                    throw new IllegalArgumentException("Result must have an intermediate value");
                }
            }

            if (matchingValue < 0) {
                if (fJustAfterPer) {
                    // We've just parsed a "per-", so we can expect a unit constant.
                    int hyphenIndex = fSource.indexOf('-', savedIndex);

                    // extract the unit constant from the string
                    String unitConstant = (hyphenIndex == -1) ? fSource.substring(savedIndex)
                            : fSource.substring(savedIndex, hyphenIndex);
                    fIndex = (hyphenIndex == -1) ? fSource.length() : hyphenIndex;

                    return Token.tokenWithConstant(unitConstant);

                } else {
                    throw new IllegalArgumentException("Encountered unknown token starting at index " + prevIndex);
                }
            } else {
                fIndex = prevIndex;
            }

            return new Token(matchingValue);
        }

        static class Token {

            private final long fMatch;
            private final Type type;

            public Token(long fMatch) {
                this.fMatch = fMatch;
                type = calculateType(fMatch);
            }

            private Token(long fMatch, Type type) {
                this.fMatch = fMatch;
                this.type = type;
            }

            public static Token tokenWithConstant(String constantStr) {
                BigDecimal unitConstantValue = new BigDecimal(constantStr);
                if (unitConstantValue.scale() <= 0 && unitConstantValue.compareTo(BigDecimal.ZERO) >= 0
                        && unitConstantValue.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) <= 0) {
                    return new Token(unitConstantValue.longValueExact(), Type.TYPE_UNIT_CONSTANT);
                } else {
                    throw new IllegalArgumentException(
                            "The unit constant value is not a valid non-negative long integer.");
                }
            }

            public Type getType() {
                return this.type;
            }

            public MeasureUnit.MeasurePrefix getPrefix() {
                assert this.type == Type.TYPE_PREFIX;
                assert this.fMatch <= Integer.MAX_VALUE;

                int trieIndex = (int) this.fMatch;
                return getPrefixFromTrieIndex(trieIndex);
            }

            // Valid only for tokens with type TYPE_UNIT_CONSTANT.
            public long getConstantDenominator() {
                assert this.type == Type.TYPE_UNIT_CONSTANT;
                return this.fMatch;
            }

            // Valid only for tokens with type TYPE_COMPOUND_PART.
            public int getMatch() {
                assert getType() == Type.TYPE_COMPOUND_PART;
                assert this.fMatch <= Integer.MAX_VALUE;

                int matchIndex = (int) this.fMatch;
                return matchIndex;
            }

            // Even if there is only one InitialCompoundPart value, we have this
            // function for the simplicity of code consistency.
            public InitialCompoundPart getInitialCompoundPart() {
                assert this.type == Type.TYPE_INITIAL_COMPOUND_PART;
                assert fMatch == InitialCompoundPart.INITIAL_COMPOUND_PART_PER.getTrieIndex();
                assert fMatch <= Integer.MAX_VALUE;
                int trieIndex = (int) fMatch;
                return InitialCompoundPart.getInitialCompoundPartFromTrieIndex(trieIndex);
            }

            public int getPower() {
                assert this.type == Type.TYPE_POWER_PART;
                assert this.fMatch <= Integer.MAX_VALUE;
                int trieIndex = (int) this.fMatch;
                return PowerPart.getPowerFromTrieIndex(trieIndex);
            }

            public int getSimpleUnitIndex() {
                assert this.type == Type.TYPE_SIMPLE_UNIT;
                assert this.fMatch <= Integer.MAX_VALUE;
                return ((int) this.fMatch) - UnitsData.Constants.kSimpleUnitOffset;
            }

            // It is invalid to call calculateType() with a non-positive Token value,
            // as it will result in an assertion failure.
            private Type calculateType(long fMatch) {
                if (fMatch <= 0) {
                    throw new AssertionError("fMatch must have a positive value");
                }
                if (fMatch < UnitsData.Constants.kCompoundPartOffset) {
                    return Type.TYPE_PREFIX;
                }
                if (fMatch < UnitsData.Constants.kInitialCompoundPartOffset) {
                    return Type.TYPE_COMPOUND_PART;
                }
                if (fMatch < UnitsData.Constants.kPowerPartOffset) {
                    return Type.TYPE_INITIAL_COMPOUND_PART;
                }
                if (fMatch < UnitsData.Constants.kSimpleUnitOffset) {
                    return Type.TYPE_POWER_PART;
                }

                return Type.TYPE_SIMPLE_UNIT;
            }

            enum Type {
                TYPE_UNDEFINED,
                TYPE_PREFIX,
                // Token type for "-per-", "-", and "-and-".
                TYPE_COMPOUND_PART,
                // Token type for "per-".
                TYPE_INITIAL_COMPOUND_PART,
                TYPE_POWER_PART,
                TYPE_SIMPLE_UNIT,
                TYPE_UNIT_CONSTANT,
            }
        }
    }

    static class MeasureUnitImplComparator implements Comparator<MeasureUnitImpl> {
        private final ConversionRates conversionRates;

        public MeasureUnitImplComparator(ConversionRates conversionRates) {
            this.conversionRates = conversionRates;
        }

        @Override
        public int compare(MeasureUnitImpl o1, MeasureUnitImpl o2) {
            String special1 = this.conversionRates.getSpecialMappingName(o1);
            String special2 = this.conversionRates.getSpecialMappingName(o2);
            if (special1 != null || special2 != null) {
                if (special1 == null) {
                    // non-specials come first
                    return -1;
                }
                if (special2 == null) {
                    // non-specials come first
                    return 1;
                }
                // both are specials, compare lexicographically
                return special1.compareTo(special2);
            }
            BigDecimal factor1 = this.conversionRates.getFactorToBase(o1).getConversionRate();
            BigDecimal factor2 = this.conversionRates.getFactorToBase(o2).getConversionRate();

            return factor1.compareTo(factor2);
        }
    }

    static class MeasureUnitImplWithIndexComparator implements Comparator<MeasureUnitImplWithIndex> {
        private MeasureUnitImplComparator measureUnitImplComparator;

        public MeasureUnitImplWithIndexComparator(ConversionRates conversionRates) {
            this.measureUnitImplComparator = new MeasureUnitImplComparator(conversionRates);
        }

        @Override
        public int compare(MeasureUnitImplWithIndex o1, MeasureUnitImplWithIndex o2) {
            return this.measureUnitImplComparator.compare(o1.unitImpl, o2.unitImpl);
        }
    }

    static class SingleUnitComparator implements Comparator<SingleUnitImpl> {
        @Override
        public int compare(SingleUnitImpl o1, SingleUnitImpl o2) {
            return o1.compareTo(o2);
        }
    }
}
