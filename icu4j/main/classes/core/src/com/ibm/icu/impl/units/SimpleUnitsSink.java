package com.ibm.icu.impl.units;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.util.UResourceBundle;

import java.util.ArrayList;

public class SimpleUnitsSink extends UResource.Sink {
    @Override
    public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
        Assert.assrt(key.toString().equals(Constants.conversionUnitTableName));
        Assert.assrt(value.getType() == UResourceBundle.TABLE);

        UResource.Table simpleUnitsTable = value.getTable();
        ArrayList<String> simpleUnits = new ArrayList<>();
        for (int i = 0; simpleUnitsTable.getKeyAndValue(i, key, value); i++) {
            simpleUnits.add(key.toString());
        }

        this.simpleUnits = simpleUnits.toArray(new String[0]);
    }

    String[] simpleUnits = null;
}
