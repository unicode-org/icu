package com.ibm.icu.impl.units;

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
        return  this.power +  Constants.kPowerPartOffset ;
    }

    public static int getPowerFromTrieIndex(int trieIndex) {
        return trieIndex -  Constants.kPowerPartOffset ;
    }

    public int getValue() {
        return power;
    }
}
