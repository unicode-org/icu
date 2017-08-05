// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.util;

public class Dimensionless extends MeasureUnit {

    public static final Dimensionless BASE =
        (Dimensionless) MeasureUnit.internalGetInstance("dimensionless", "base");

    public static final Dimensionless PERCENT =
        (Dimensionless) MeasureUnit.internalGetInstance("dimensionless", "percent");

    public static final Dimensionless PERMILLE =
        (Dimensionless) MeasureUnit.internalGetInstance("dimensionless", "permille");

    protected Dimensionless(String subType) {
        super("dimensionless", subType);
    }
}
