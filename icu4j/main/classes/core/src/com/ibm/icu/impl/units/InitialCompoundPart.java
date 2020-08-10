package com.ibm.icu.impl.units;

public enum InitialCompoundPart {

    // Represents "per-", the only compound part that can appear at the start of
    // an identifier.
    INITIAL_COMPOUND_PART_PER(0);

    private final int index;

    InitialCompoundPart(int powerIndex) {
        this.index = powerIndex ;
    }

    public int getTrieIndex() {
        return this.index + Constants.kInitialCompoundPartOffset;
    }

    public static InitialCompoundPart getInitialCompoundPartFromTrieIndex(int trieIndex) {
        int index = trieIndex - Constants.kInitialCompoundPartOffset;
        if (index == 0) {
            return  INITIAL_COMPOUND_PART_PER;
        }

        throw new java.lang.InternalError("Incorrect trieIndex");
    }

    public int getValue() {
        return index;
    }

}
