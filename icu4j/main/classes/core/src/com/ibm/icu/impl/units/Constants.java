package com.ibm.icu.impl.units;

public class Constants {
    // Trie value offset for simple units, e.g. "gram", "nautical-mile",
    // "fluid-ounce-imperial".
    public static final int kSimpleUnitOffset = 512;

    // Trie value offset for powers like "square-", "cubic-", "pow2-" etc.
    public static final int kPowerPartOffset = 256;


    // Trie value offset for "per-".
    public final static int kInitialCompoundPartOffset = 192;

    // Trie value offset for compound parts, e.g. "-per-", "-", "-and-".
    public final static int kCompoundPartOffset = 128;

    // Trie value offset for SI Prefixes. This is big enough to ensure we only
    // insert positive integers into the trie.
    public static final int kSIPrefixOffset = 64;


    /* Tables Names*/
    public static final String CONVERSION_UNIT_TABLE_NAME = "convertUnits";
    public static final String UNIT_PREFERENCE_TABLE_NAME = "unitPreferenceData";
    public static final String CATEGORY_TABLE_NAME = "unitQuantities";
    public static final String DEFAULT_REGION = "001";
    public static final String DEFAULT_USAGE = "default";
}
