package com.ibm.icu.impl.units;

/**
 * Enumeration for SI prefixes, such as "kilo".
 *
 * @draft ICU 68
 */
public enum SIPrefix {

    /**
     * SI prefix: yotta, 10^24.
     *
     * @draft ICU 68
     */
    SI_PREFIX_YOTTA(24, "yotta"),

    /**
     * SI prefix: zetta, 10^21.
     *
     * @draft ICU 68
     */
    SI_PREFIX_ZETTA(21, "zetta"),

    /**
     * SI prefix: exa, 10^18.
     *
     * @draft ICU 68
     */
    SI_PREFIX_EXA(18, "exa"),

    /**
     * SI prefix: peta, 10^15.
     *
     * @draft ICU 68
     */
    SI_PREFIX_PETA(15, "peta"),

    /**
     * SI prefix: tera, 10^12.
     *
     * @draft ICU 68
     */
    SI_PREFIX_TERA(12, "tera"),

    /**
     * SI prefix: giga, 10^9.
     *
     * @draft ICU 68
     */
    SI_PREFIX_GIGA(9, "giga"),

    /**
     * SI prefix: mega, 10^6.
     *
     * @draft ICU 68
     */
    SI_PREFIX_MEGA(6, "mega"),

    /**
     * SI prefix: kilo, 10^3.
     *
     * @draft ICU 68
     */
    SI_PREFIX_KILO(3, "kilo"),

    /**
     * SI prefix: hecto, 10^2.
     *
     * @draft ICU 68
     */
    SI_PREFIX_HECTO(2, "hecto"),

    /**
     * SI prefix: deka, 10^1.
     *
     * @draft ICU 68
     */
    SI_PREFIX_DEKA(1, "deka"),

    /**
     * The absence of an SI prefix.
     *
     * @draft ICU 68
     */
    SI_PREFIX_ONE(0, ""),

    /**
     * SI prefix: deci, 10^-1.
     *
     * @draft ICU 68
     */
    SI_PREFIX_DECI(-1, "deci"),

    /**
     * SI prefix: centi, 10^-2.
     *
     * @draft ICU 68
     */
    SI_PREFIX_CENTI(-2, "centi"),

    /**
     * SI prefix: milli, 10^-3.
     *
     * @draft ICU 68
     */
    SI_PREFIX_MILLI(-3, "milli"),

    /**
     * SI prefix: micro, 10^-6.
     *
     * @draft ICU 68
     */
    SI_PREFIX_MICRO(-6, "micro"),

    /**
     * SI prefix: nano, 10^-9.
     *
     * @draft ICU 68
     */
    SI_PREFIX_NANO(-9, "nano"),

    /**
     * SI prefix: pico, 10^-12.
     *
     * @draft ICU 68
     */
    SI_PREFIX_PICO(-12, "pico"),

    /**
     * SI prefix: femto, 10^-15.
     *
     * @draft ICU 68
     */
    SI_PREFIX_FEMTO(-15, "femto"),

    /**
     * SI prefix: atto, 10^-18.
     *
     * @draft ICU 68
     */
    SI_PREFIX_ATTO(-18, "atto"),

    /**
     * SI prefix: zepto, 10^-21.
     *
     * @draft ICU 68
     */
    SI_PREFIX_ZEPTO(-21, "zepto"),

    /**
     * SI prefix: yocto, 10^-24.
     *
     * @draft ICU 68
     */
    SI_PREFIX_YOCTO(-24, "yocto");

    public String getSiRepresentation() {
        return siRepresentation;
    }

    public int getSiPrefixPower() {
        return siPrefixPower;
    }


    private final int siPrefixPower;


    private final String siRepresentation;

    SIPrefix(int siPrefixPower, String siRepresentation) {
        this.siPrefixPower = siPrefixPower;
        this.siRepresentation = siRepresentation;
    }


    public int getTrieIndex() {
        return siPrefixPower + Constants.kSIPrefixOffset;
    }

    public static SIPrefix getSiPrefixFromTrieIndex(int trieIndex) {
        for (SIPrefix element :
                values()) {
            if (element.getTrieIndex() == trieIndex)
                return element;
        }

        throw new java.lang.InternalError("Incorrect trieIndex");
    }
}