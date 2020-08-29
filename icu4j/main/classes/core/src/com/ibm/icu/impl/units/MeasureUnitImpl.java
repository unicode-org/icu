/*
 *******************************************************************************
 * Copyright (C) 2004-2020, Google Inc, International Business Machines
 * Corporation and others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.impl.units;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.util.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MeasureUnitImpl {

    /**
     * The full unit identifier. Owned by the MeasureUnitImpl.  Null if not computed.
     */
    private String identifier = null;
    /**
     * The complexity, either SINGLE, COMPOUND, or MIXED.
     */
    private MeasureUnit.Complexity complexity = MeasureUnit.Complexity.SINGLE;
    /**
     * the list of simple units.These may be summed or multiplied, based on the
     * value of the complexity field.
     * <p>
     * The "dimensionless" unit (SingleUnitImpl default constructor) must not be
     * added to this list.
     * <p>
     * The "dimensionless" <code>MeasureUnitImpl</code> has an empty <code>singleUnits</code>.
     */
    private ArrayList<SingleUnitImpl> singleUnits;

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
     * @return A newly parsed value object.
     * @throws <code>InternalError</code> in case of incorrect/non-parsed identifier.
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

    public MeasureUnitImpl clone() {
        MeasureUnitImpl result = new MeasureUnitImpl();
        result.complexity = this.complexity;
        result.identifier = this.identifier;
        result.singleUnits = (ArrayList<SingleUnitImpl>) this.singleUnits.clone();
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

    /**
     * Extracts the list of all the individual units inside the `MeasureUnitImpl`.
     * For example:
     * -   if the <code>MeasureUnitImpl</code> is <code>foot-per-hour</code>
     * it will return a list of 1 <code>{foot-per-hour}</code>
     * -   if the <code>MeasureUnitImpl</code> is <code>foot-and-inch</code>
     * it will return a list of 2 <code>{ foot, inch}</code>
     *
     * @return a list of <code>MeasureUnitImpl</code>
     */
    public ArrayList<MeasureUnitImpl> extractIndividualUnits() {
        ArrayList<MeasureUnitImpl> result = new ArrayList<MeasureUnitImpl>();
        if (this.getComplexity() == MeasureUnit.Complexity.MIXED) {
            // In case of mixed units, each single unit can be considered as a stand alone MeasureUnitImpl.
            for (SingleUnitImpl singleUnit :
                    this.getSingleUnits()) {
                result.add(new MeasureUnitImpl(singleUnit));
            }

            return result;
        }

        result.add(this.clone());
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

        if (singleUnit.isDimensionless()) {
            // We don't append dimensionless units.
            return false;
        }

        // Find a similar unit that already exists, to attempt to coalesce
        SingleUnitImpl oldUnit = null;
        for (int i = 0, n = this.singleUnits.size(); i < n; i++) {
            SingleUnitImpl candidate = this.singleUnits.get(i);
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

        // TODO: shall we just add singleUnit instead of creating a copy ??
        this.singleUnits.add(singleUnit.clone());

        // If the MeasureUnitImpl is `UMEASURE_UNIT_SINGLE` and after the appending a unit, the singleUnits are more
        // than one singleUnit. thus means the complexity should be `UMEASURE_UNIT_COMPOUND`
        if (this.singleUnits.size() > 1 && this.complexity == MeasureUnit.Complexity.SINGLE) {
            this.setComplexity(MeasureUnit.Complexity.COMPOUND);
        }

        return true;
    }

    /**
     * Transform this MeasureUnitImpl into a MeasureUnit, simplifying if possible.
     */
    public MeasureUnit build() {
        return new MeasureUnit(this);
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
            return this.singleUnits.get(0).clone();
        }

        throw new UnsupportedOperationException();
    }

    public String getIdentifier() {
        if (this.identifier != null) {
            return this.identifier;
        }

        this.serialize();
        return identifier;
    }

    public MeasureUnit.Complexity getComplexity() {
        return complexity;
    }

    public void setComplexity(MeasureUnit.Complexity complexity) {
        this.complexity = complexity;
    }

    /**
     * Normalizes the MeasureUnitImpl and generates the identifier string in place.
     */
    private void serialize() {
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
        boolean beforePre = true;
        boolean firstTimeNegativeDimension = false;
        for (SingleUnitImpl singleUnit :
                this.getSingleUnits()) {
            if (beforePre && singleUnit.getDimensionality() < 0) {
                beforePre = false;
                firstTimeNegativeDimension = true;
            } else if (singleUnit.getDimensionality() < 0) {
                firstTimeNegativeDimension = false;
            }

            String singleUnitIdentifier = singleUnit.getNeutralIdentifier();
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

            result.append(singleUnitIdentifier);
        }

        this.identifier = result.toString();
    }

    class SingleUnitComparator implements Comparator<SingleUnitImpl> {
        @Override
        public int compare(SingleUnitImpl o1, SingleUnitImpl o2) {
            return o1.compareTo(o2);
        }
    }

    public static class UnitsParser {
        public static MeasureUnitImpl parseForIdentifier(String identifier) {
            UnitsParser parser = new UnitsParser(identifier);
            return parser.parse();

        }

        private MeasureUnitImpl parse() {
            MeasureUnitImpl result = new MeasureUnitImpl();

            if (fSource.isEmpty()) {
                // The dimensionless unit: nothing to parse. return the empty result.
                return result;
            }


            int unitNum = 0;
            while (hasNext()) {
                sawAnd = false;
                SingleUnitImpl singleUnit = nextSingleUnit();
                Assert.assrt(!singleUnit.isDimensionless());

                boolean added = result.appendSingleUnit(singleUnit);
                if (sawAnd && !added) {
                    throw new InternalError("Two similar units are not allowed in a mixed unit.");
                }

                if ((result.singleUnits.size()) >= 2) {
                    // nextSingleUnit fails appropriately for "per" and "and" in the
                    // same identifier. It doesn't fail for other compound units
                    // (COMPOUND_PART_TIMES). Consequently we take care of that
                    // here.
                    MeasureUnit.Complexity complexity =
                            sawAnd ? MeasureUnit.Complexity.MIXED : MeasureUnit.Complexity.COMPOUND;
                    if (result.getSingleUnits().size() == 2) {
                        // After appending two singleUnits, the complexity will be `UMEASURE_UNIT_COMPOUND`
                        Assert.assrt(result.getComplexity() == MeasureUnit.Complexity.COMPOUND);
                        result.setComplexity(complexity);
                    } else if (result.getComplexity() != complexity) {
                        throw new InternalError("Can't have mixed compound units");
                    }
                }
            }

            return result;
        }

        /**
         * Returns the next "single unit" via result.
         * <p>
         * If a "-per-" was parsed, the result will have appropriate negative
         * dimensionality.
         * <p>
         * @throws InternalError if we parse both compound units and "-and-", since mixed
         * compound units are not yet supported - TODO(CLDR-13700).
         */
        private SingleUnitImpl nextSingleUnit() {
            SingleUnitImpl result = new SingleUnitImpl();

            // state:
            // 0 = no tokens seen yet (will accept power, SI prefix, or simple unit)
            // 1 = power token seen (will not accept another power token)
            // 2 = SI prefix token seen (will not accept a power or SI prefix token)
            int state = 0;

            boolean atStart = fIndex == 0;
            Token token = nextToken();

            if (atStart) {
                // Identifiers optionally start with "per-".
                if (token.getType() == Token.Type.TYPE_INITIAL_COMPOUND_PART) {
                    if (token.getInitialCompoundPart() == InitialCompoundPart.INITIAL_COMPOUND_PART_PER) {
                        fAfterPer = true;
                        result.setDimensionality(-1);

                        token = nextToken();

                    } else {
                        throw new InternalError();
                    }

                }
            } else {
                // All other SingleUnit's are separated from previous SingleUnit's
                // via a compound part:
                if (token.getType() != Token.Type.TYPE_COMPOUND_PART) {
                    throw new InternalError("token type must be TYPE_COMPOUND_PART");
                }

                CompoundPart compoundPart = CompoundPart.getCompoundPartFromTrieIndex(token.getMatch());
                switch (compoundPart) {
                    case COMPOUND_PART_PER:
                        if (sawAnd) {
                            throw new InternalError("Mixed compound units not yet supported");
                            // TODO(CLDR-13700).
                        }

                        fAfterPer = true;
                        result.setDimensionality(-1);
                        break;

                    case COMPOUND_PART_TIMES:
                        if (fAfterPer) {
                            result.setDimensionality(-1);
                        }
                        break;

                    case COMPOUND_PART_AND:
                        if (fAfterPer) {
                            // not yet supported, TODO(CLDR-13700).
                            throw new InternalError("Can't start with \"-and-\", and mixed compound units");
                        }
                        sawAnd = true;
                        break;
                }

                token = nextToken();
            }

            // Read tokens until we have a complete SingleUnit or we reach the end.
            while (true) {
                switch (token.getType()) {
                    case TYPE_POWER_PART:
                        if (state > 0) {
                            throw new InternalError();
                        }

                        result.setDimensionality(result.getDimensionality() * token.getPower());
                        state = 1;
                        break;

                    case TYPE_SI_PREFIX:
                        if (state > 1) {
                            throw new InternalError();
                        }

                        result.setSiPrefix(token.getSIPrefix());
                        state = 2;
                        break;

                    case TYPE_SIMPLE_UNIT:
                        result.setSimpleUnit(token.getSimpleUnitIndex(), simpleUnits);
                        return result;

                    default:
                        throw new InternalError();
                }

                if (!hasNext()) {
                    throw new InternalError("We ran out of tokens before finding a complete single unit.");
                }

                token = nextToken();
            }
        }

        private boolean hasNext() {
            return fIndex < fSource.length();
        }

        private Token nextToken() {
            trie.reset();
            int match = -1;
            // Saves the position in the fSource string for the end of the most
            // recent matching token.
            int previ = -1;

            // Find the longest token that matches a value in the trie:
            while (fIndex < fSource.length()) {
                BytesTrie.Result result = trie.next(fSource.charAt(fIndex++));
                if (result == BytesTrie.Result.NO_MATCH) {
                    break;
                } else if (result == BytesTrie.Result.NO_VALUE) {
                    continue;
                }

                if (!result.hasValue()) {
                    throw new InternalError("result must has a value");
                }

                match = trie.getValue();
                previ = fIndex;

                if (result == BytesTrie.Result.FINAL_VALUE) {
                    break;
                }

                if (result != BytesTrie.Result.INTERMEDIATE_VALUE) {
                    throw new InternalError("result must has an intermediate value");
                }

                // continue;
            }


            if (match < 0) {
                throw new InternalError("match must be bigger than zero");
            } else {
                fIndex = previ;
            }

            return new Token(match);
        }

        private UnitsParser(String identifier) {
            this.simpleUnits = UnitsData.getSimpleUnits();
            this.fSource = identifier;

            if (trie != null) {
                return;
            }



            // Building the trie.
            CharsTrieBuilder trieBuilder;
            trieBuilder = new CharsTrieBuilder();


            // Add syntax parts (compound, power prefixes)
            trieBuilder.add("-per-", CompoundPart.COMPOUND_PART_PER.getTrieIndex());
            trieBuilder.add("-", CompoundPart.COMPOUND_PART_TIMES.getTrieIndex());
            trieBuilder.add("-and-", CompoundPart.COMPOUND_PART_AND.getTrieIndex());
            trieBuilder.add("per-", InitialCompoundPart.INITIAL_COMPOUND_PART_PER.getTrieIndex());
            trieBuilder.add("square-", PowerPart.POWER_PART_P2.getTrieIndex());
            trieBuilder.add("cubic-", PowerPart.POWER_PART_P3.getTrieIndex());
            trieBuilder.add("pow2-", PowerPart.POWER_PART_P2.getTrieIndex());
            trieBuilder.add("pow3-", PowerPart.POWER_PART_P3.getTrieIndex());
            trieBuilder.add("pow4-", PowerPart.POWER_PART_P4.getTrieIndex());
            trieBuilder.add("pow5-", PowerPart.POWER_PART_P5.getTrieIndex());
            trieBuilder.add("pow6-", PowerPart.POWER_PART_P6.getTrieIndex());
            trieBuilder.add("pow7-", PowerPart.POWER_PART_P7.getTrieIndex());
            trieBuilder.add("pow8-", PowerPart.POWER_PART_P8.getTrieIndex());
            trieBuilder.add("pow9-", PowerPart.POWER_PART_P9.getTrieIndex());
            trieBuilder.add("pow10-", PowerPart.POWER_PART_P10.getTrieIndex());
            trieBuilder.add("pow11-", PowerPart.POWER_PART_P11.getTrieIndex());
            trieBuilder.add("pow12-", PowerPart.POWER_PART_P12.getTrieIndex());
            trieBuilder.add("pow13-", PowerPart.POWER_PART_P13.getTrieIndex());
            trieBuilder.add("pow14-", PowerPart.POWER_PART_P14.getTrieIndex());
            trieBuilder.add("pow15-", PowerPart.POWER_PART_P15.getTrieIndex());

            // Add SI prefixes
            for (MeasureUnit.SIPrefix siPrefix :
                    MeasureUnit.SIPrefix.values()) {
                trieBuilder.add(siPrefix.getIdentifier(), siPrefix.getTrieIndex());
            }

            // Add simple units
            for (int i = 0; i < simpleUnits.length; i++) {
                trieBuilder.add(simpleUnits[i], i + UnitsData.Constants.kSimpleUnitOffset);

            }

            // TODO: Use SLOW or FAST here?
            trie = trieBuilder.build(StringTrieBuilder.Option.FAST);
        }

        private static CharsTrie trie = null;


        // Set to true when we've seen a "-per-" or a "per-", after which all units
        // are in the denominator. Until we find an "-and-", at which point the
        // identifier is invalid pending TODO(CLDR-13700).
        private boolean fAfterPer = false;

        private String fSource;

        // Tracks parser progress: the offset into fSource.
        int fIndex = 0;

        // If an "-and-" was parsed prior to finding the "single
        //     * unit", sawAnd is set to true. If not, it is left as is.
        private boolean sawAnd = false;


        private final String[] simpleUnits;

        static class Token {

            public Token(int fMatch) {
                this.fMatch = fMatch;
                type = calculateType(fMatch);
            }

            enum Type {
                TYPE_UNDEFINED,
                TYPE_SI_PREFIX,
                // Token type for "-per-", "-", and "-and-".
                TYPE_COMPOUND_PART,
                // Token type for "per-".
                TYPE_INITIAL_COMPOUND_PART,
                TYPE_POWER_PART,
                TYPE_SIMPLE_UNIT,
            }

            public Type getType() {
                return this.type;
            }

            public MeasureUnit.SIPrefix getSIPrefix() {
                if (this.type == Type.TYPE_SI_PREFIX) {
                    return MeasureUnit.SIPrefix.getSiPrefixFromTrieIndex(this.fMatch);
                }

                throw new InternalError("type must be TYPE_SI_PREFIX");
            }

            // Valid only for tokens with type TYPE_COMPOUND_PART.
            public int getMatch() {
                if (getType() == Type.TYPE_COMPOUND_PART) {
                    return fMatch;
                }

                throw new InternalError("getType() must return Type.TYPE_COMPOUND_PART");
            }

            // Even if there is only one InitialCompoundPart value, we have this
            // function for the simplicity of code consistency.
            public InitialCompoundPart getInitialCompoundPart() {

                if (this.type == Type.TYPE_INITIAL_COMPOUND_PART && fMatch == InitialCompoundPart.INITIAL_COMPOUND_PART_PER.getTrieIndex()) {
                    return InitialCompoundPart.getInitialCompoundPartFromTrieIndex(fMatch);
                }

                throw new InternalError("type must be initial and fMatch must equal `InitialCompoundPart.INITIAL_COMPOUND_PART_PER.getTrieIndex()`");
            }

            public int getPower() {
                if (this.type == Type.TYPE_POWER_PART) {
                    return PowerPart.getPowerFromTrieIndex(this.fMatch);
                }

                throw new InternalError("type must be `TYPE_POWER_PART`");
            }

            public int getSimpleUnitIndex() {
                return this.fMatch - UnitsData.Constants.kSimpleUnitOffset;
            }

            // Calling calculateType() is invalid, resulting in an assertion failure, if Token
            // value isn't positive.
            private Type calculateType(int fMatch) {
                if (fMatch <= 0) {
                    throw new InternalError("fMatch must have a positive value");
                }

                if (fMatch < UnitsData.Constants.kCompoundPartOffset) {
                    return Type.TYPE_SI_PREFIX;
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

            private final int fMatch;
            private final Type type;
        }
    }

    public enum CompoundPart {
        // Represents "-per-"
        COMPOUND_PART_PER(0),
        // Represents "-"
        COMPOUND_PART_TIMES(1),
        // Represents "-and-"
        COMPOUND_PART_AND(2);


        private final int index;
        CompoundPart(int index) {
            this.index = index ;
        }

        public int getTrieIndex() {
            return  this.index +  UnitsData.Constants.kCompoundPartOffset ;
        }

        public static CompoundPart getCompoundPartFromTrieIndex(int trieIndex) {
            int index = trieIndex -  UnitsData.Constants.kCompoundPartOffset ;
            switch (index) {
                case 0:
                    return CompoundPart.COMPOUND_PART_PER;
                case 1:
                    return CompoundPart.COMPOUND_PART_TIMES;
                case 2:
                    return CompoundPart.COMPOUND_PART_AND;
                default:
                   throw new InternalError("CompoundPart index must be 0, 1 or 2");
            }
        }

        public int getValue() {
            return index;
        }
    }

    public enum PowerPart {
        POWER_PART_P2 (2),
        POWER_PART_P3(3),
        POWER_PART_P4(4),
        POWER_PART_P5(5),
        POWER_PART_P6(6),
        POWER_PART_P7(7),
        POWER_PART_P8(8),
        POWER_PART_P9(9),
        POWER_PART_P10(10),
        POWER_PART_P11(11),
        POWER_PART_P12(12),
        POWER_PART_P13(13),
        POWER_PART_P14(14),
        POWER_PART_P15(15);

        private final int power;
        PowerPart(int power) {
            this.power = power;
        }

        public int getTrieIndex() {
            return  this.power +  UnitsData.Constants.kPowerPartOffset ;
        }

        public static int getPowerFromTrieIndex(int trieIndex) {
            return trieIndex -  UnitsData.Constants.kPowerPartOffset ;
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
            this.index = powerIndex ;
        }

        public int getTrieIndex() {
            return this.index + UnitsData.Constants.kInitialCompoundPartOffset;
        }

        public static InitialCompoundPart getInitialCompoundPartFromTrieIndex(int trieIndex) {
            int index = trieIndex - UnitsData.Constants.kInitialCompoundPartOffset;
            if (index == 0) {
                return  INITIAL_COMPOUND_PART_PER;
            }

            throw new InternalError("Incorrect trieIndex");
        }

        public int getValue() {
            return index;
        }

    }

    static class MeasureUnitImplComparator implements Comparator<MeasureUnitImpl> {
        @Override
        public int compare(MeasureUnitImpl o1, MeasureUnitImpl o2) {
            Assert.assrt(conversionRates != null);

            UnitConverter fromO1toO2 = new UnitConverter(o1, o2, conversionRates);
            return fromO1toO2.convert(BigDecimal.valueOf(1)).compareTo(BigDecimal.valueOf(1));
        }

        public static void setConversionRates(ConversionRates conversionRates) {
            MeasureUnitImplComparator.conversionRates = conversionRates;
        }

        private static ConversionRates conversionRates = null;
    }
}
