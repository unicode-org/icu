/*
 *******************************************************************************
 * Copyright (C) 2004-2020, Google Inc, International Business Machines
 * Corporation and others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.impl.units;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.util.UResourceBundle;

import java.util.TreeMap;

public class CategoriesSink  extends UResource.Sink{
    public CategoriesSink() {
        mapFromUnitToCategory = new TreeMap<>();
    }

    @Override
    public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
        Assert.assrt(key.toString() == UnitsData.Constants.CATEGORY_TABLE_NAME);
        Assert.assrt(value.getType() == UResourceBundle.TABLE);

        UResource.Table categoryTable = value.getTable();
        for (int i = 0; categoryTable.getKeyAndValue(i, key, value); i++) {
            Assert.assrt(value.getType() == UResourceBundle.STRING);
            mapFromUnitToCategory.put(key.toString(), value.toString());
        }
    }

    public TreeMap<String, String> getMapFromUnitToCategory() {
        return mapFromUnitToCategory;
    }

    /**
     * Contains the map between units in their base units into their category.
     * For example:  meter-per-second --> "speed"
     */
    TreeMap<String , String> mapFromUnitToCategory;
}
