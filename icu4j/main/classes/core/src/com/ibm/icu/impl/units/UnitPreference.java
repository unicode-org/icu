/*
 *******************************************************************************
 * Copyright (C) 2004-2020, Google Inc, International Business Machines
 * Corporation and others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.impl.units;

import java.math.BigDecimal;

public class UnitPreference {
    public UnitPreference(String unit, String geq, String skeleton ) {
        this.unit = unit;
        this.geq = new BigDecimal(geq);
        this.skeleton = skeleton;
    }

    public String getUnit() {
        return this.unit;
    }

    public BigDecimal getGeq() {
        return geq;
    }


    public String getSkeleton() {
        return skeleton;
    }

    // TODO: make them final.
    private String unit;
    private BigDecimal geq;
    private String skeleton;
}
