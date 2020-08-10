package com.ibm.icu.impl.units;

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
        return  this.index +  Constants.kCompoundPartOffset ;
    }

    public static CompoundPart getCompoundPartFromTrieIndex(int trieIndex) {
        int index = trieIndex -  Constants.kCompoundPartOffset ;
        switch (index) {
            case 0:
                return CompoundPart.COMPOUND_PART_PER;
            case 1:
                return CompoundPart.COMPOUND_PART_TIMES;
            case 2:
                return CompoundPart.COMPOUND_PART_AND;
            default:
               throw new java.lang.InternalError("CompoundPart index must be 0, 1 or 2");
        }
    }

    public int getValue() {
        return index;
    }
}
