package com.ibm.icu.impl.units;

class Token {

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

    ;

    public Type getType() {
        return this.type;
    }

    public SIPrefix getSIPrefix() {
        if (this.type == Type.TYPE_SI_PREFIX) {
            return SIPrefix.getSiPrefixFromTrieIndex(this.fMatch);
        }

        throw new java.lang.InternalError("type must be TYPE_SI_PREFIX");
    }

    // Valid only for tokens with type TYPE_COMPOUND_PART.
    public int getMatch() {
        if (getType() == Type.TYPE_COMPOUND_PART) {
            return fMatch;
        }

        throw new java.lang.InternalError("getType() must return Type.TYPE_COMPOUND_PART");
    }

    // Even if there is only one InitialCompoundPart value, we have this
    // function for the simplicity of code consistency.
    public InitialCompoundPart getInitialCompoundPart() {

        if (this.type == Type.TYPE_INITIAL_COMPOUND_PART && fMatch == InitialCompoundPart.INITIAL_COMPOUND_PART_PER.getTrieIndex()) {
            return InitialCompoundPart.getInitialCompoundPartFromTrieIndex(fMatch);
        }

        throw new java.lang.InternalError("type must be initial and fMatch must equal `InitialCompoundPart.INITIAL_COMPOUND_PART_PER.getTrieIndex()`");
    }

    public int getPower() {
        if (this.type == Type.TYPE_POWER_PART) {
            return PowerPart.getPowerFromTrieIndex(this.fMatch);
        }

        throw new java.lang.InternalError("type must be `TYPE_POWER_PART`");
    }

    public int getSimpleUnitIndex() {
        return this.fMatch - Constants.kSimpleUnitOffset;
    }

    // Calling calculateType() is invalid, resulting in an assertion failure, if Token
    // value isn't positive.
    private Type calculateType(int fMatch) {
        if (fMatch <= 0) {
            throw new java.lang.InternalError("fMatch must have a positive value");
        }

        if (fMatch < Constants.kCompoundPartOffset) {
            return Type.TYPE_SI_PREFIX;
        }
        if (fMatch < Constants.kInitialCompoundPartOffset) {
            return Type.TYPE_COMPOUND_PART;
        }
        if (fMatch < Constants.kPowerPartOffset) {
            return Type.TYPE_INITIAL_COMPOUND_PART;
        }
        if (fMatch < Constants.kSimpleUnitOffset) {
            return Type.TYPE_POWER_PART;
        }

        return Type.TYPE_SIMPLE_UNIT;
    }

    private final int fMatch;
    private final Type type;
};
