package com.ibm.icu.impl.units;

import com.ibm.icu.impl.UResource;

import java.util.ArrayList;

public class SimpleUnitsSink  extends UResource.Sink {
    @Override
    public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
        ArrayList<String> simpleUnits = new ArrayList<>();
        simpleUnits.add(key.toString());

        this.simpleUnits = simpleUnits.toArray(new String[0]);
    }

    String[] simpleUnits = null;
}
