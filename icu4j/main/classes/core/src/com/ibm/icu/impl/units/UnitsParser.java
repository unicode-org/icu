package com.ibm.icu.impl.units;


import com.ibm.icu.impl.Assert;
import com.ibm.icu.util.BytesTrie.Result;
import com.ibm.icu.util.CharsTrie;
import com.ibm.icu.util.CharsTrieBuilder;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.StringTrieBuilder;

public class UnitsParser {
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
                throw new java.lang.InternalError("Two similar units are not allowed in a mixed unit.");
            }

            if ((++unitNum) >= 2) {
                // nextSingleUnit fails appropriately for "per" and "and" in the
                // same identifier. It doesn't fail for other compound units
                // (COMPOUND_PART_TIMES). Consequently we take care of that
                // here.
                MeasureUnit.Complexity complexity =
                        sawAnd ? MeasureUnit.Complexity.MIXED : MeasureUnit.Complexity.COMPOUND;
                if (unitNum == 2) {
                    // After appending two singleUnits, the complexity will be `UMEASURE_UNIT_COMPOUND`
                    Assert.assrt(result.getComplexity() == MeasureUnit.Complexity.COMPOUND);
                    result.setComplexity(complexity);
                } else if (result.getComplexity() != complexity) {
                    throw new java.lang.InternalError("Can't have mixed compound units");
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
     * Returns an error if we parse both compound units and "-and-", since mixed
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
                    throw new java.lang.InternalError();
                }

            }
        } else {
            // All other SingleUnit's are separated from previous SingleUnit's
            // via a compound part:
            if (token.getType() != Token.Type.TYPE_COMPOUND_PART) {
                throw new java.lang.InternalError("token type must be TYPE_COMPOUND_PART");
            }

            CompoundPart compoundPart = CompoundPart.getCompoundPartFromTrieIndex(token.getMatch());
            switch (compoundPart) {
                case COMPOUND_PART_PER:
                    if (sawAnd) {
                        throw new java.lang.InternalError("Mixed compound units not yet supported");
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
                        throw new java.lang.InternalError("Can't start with \"-and-\", and mixed compound units");
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
                        throw new java.lang.InternalError();
                    }

                    result.setDimensionality(result.getDimensionality() * token.getPower());
                    state = 1;
                    break;

                case TYPE_SI_PREFIX:
                    if (state > 1) {
                        throw new java.lang.InternalError();
                    }

                    result.setSiPrefix(token.getSIPrefix());
                    state = 2;
                    break;

                case TYPE_SIMPLE_UNIT:
                    result.setSimpleUnit(token.getSimpleUnitIndex(), simpleUnits);
                    return result;

                default:
                    throw new java.lang.InternalError();
            }

            if (!hasNext()) {
                throw new java.lang.InternalError("We ran out of tokens before finding a complete single unit.");
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
            Result result = trie.next(fSource.charAt(fIndex++));
            if (result == Result.NO_MATCH) {
                break;
            } else if (result == Result.NO_VALUE) {
                continue;
            }

            if (!result.hasValue()) {
                throw new java.lang.InternalError("result must has a value");
            }

            match = trie.getValue();
            previ = fIndex;

            if (result == Result.FINAL_VALUE) {
                break;
            }

            if (result != Result.INTERMEDIATE_VALUE) {
                throw new java.lang.InternalError("result must has an intermediate value");
            }

            // continue;
        }


        if (match < 0) {
            throw new java.lang.InternalError("match must be bigger than zero");
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
        for (SIPrefixString siPrefixInfo :
                gSIPrefixStrings) {
            trieBuilder.add(siPrefixInfo.siPrefixString, siPrefixInfo.value.getTrieIndex());
        }

        // Add simple units
        for (int i = 0; i < simpleUnits.length; i++) {
            trieBuilder.add(simpleUnits[i], i + Constants.kSimpleUnitOffset);

        }

        // TODO: Use SLOW or FAST here?
        trie = trieBuilder.build(StringTrieBuilder.Option.FAST);
    }

    private static CharsTrie trie = null;

    /**
     * Data
     */

    private static class SIPrefixString {
        public final String siPrefixString;
        public final MeasureUnit.SIPrefix value;

        SIPrefixString(String siPrefixString, MeasureUnit.SIPrefix value) {
            this.siPrefixString = siPrefixString;
            this.value = value;
        }
    }

    private static final SIPrefixString[] gSIPrefixStrings = {
            new SIPrefixString("yotta", MeasureUnit.SIPrefix.YOTTA),
            new SIPrefixString("zetta", MeasureUnit.SIPrefix.ZETTA),
            new SIPrefixString("exa", MeasureUnit.SIPrefix.EXA),
            new SIPrefixString("peta", MeasureUnit.SIPrefix.PETA),
            new SIPrefixString("tera", MeasureUnit.SIPrefix.TERA),
            new SIPrefixString("giga", MeasureUnit.SIPrefix.GIGA),
            new SIPrefixString("mega", MeasureUnit.SIPrefix.MEGA),
            new SIPrefixString("kilo", MeasureUnit.SIPrefix.KILO),
            new SIPrefixString("hecto", MeasureUnit.SIPrefix.HECTO),
            new SIPrefixString("deka", MeasureUnit.SIPrefix.DEKA),
            new SIPrefixString("deci", MeasureUnit.SIPrefix.DECI),
            new SIPrefixString("centi", MeasureUnit.SIPrefix.CENTI),
            new SIPrefixString("milli", MeasureUnit.SIPrefix.MILLI),
            new SIPrefixString("micro", MeasureUnit.SIPrefix.MICRO),
            new SIPrefixString("nano", MeasureUnit.SIPrefix.NANO),
            new SIPrefixString("pico", MeasureUnit.SIPrefix.PICO),
            new SIPrefixString("femto", MeasureUnit.SIPrefix.FEMTO),
            new SIPrefixString("atto", MeasureUnit.SIPrefix.ATTO),
            new SIPrefixString("zepto", MeasureUnit.SIPrefix.ZEPTO),
            new SIPrefixString("yocto", MeasureUnit.SIPrefix.YOCTO),
    };


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
}

