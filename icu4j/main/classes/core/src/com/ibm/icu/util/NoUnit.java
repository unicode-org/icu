// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.util;

public class NoUnit extends MeasureUnit {
    private static final long serialVersionUID = 2467174286237024095L;

    public static final NoUnit BASE =
        (NoUnit) MeasureUnit.internalGetInstance("none", "base");

    public static final NoUnit PERCENT =
        (NoUnit) MeasureUnit.internalGetInstance("none", "percent");

    public static final NoUnit PERMILLE =
        (NoUnit) MeasureUnit.internalGetInstance("none", "permille");

    protected NoUnit(String subType) {
        super("none", subType);
    }
}
