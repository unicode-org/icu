package com.ibm.icu.impl.units;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.util.UResourceBundle;

import java.util.ArrayList;

public class UnitPreferencesSink extends UResource.Sink {

    public UnitPreferencesSink(UnitPreferences unitPreferences) {
        this.unitPreferences = unitPreferences;
    }

    /**
     * The unitPreferenceData structure (see icu4c/source/data/misc/units.txt) contains a
     * hierarchy of category/usage/region, within which are a set of
     * preferences. Hence three for-loops and another loop for the
     * preferences themselves.
     */
    @Override
    public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
        Assert.assrt(Constants.UNIT_PREFERENCE_TABLE_NAME.equals(key.toString()));

        UResource.Table categoryTable = value.getTable();
        for (int i = 0; categoryTable.getKeyAndValue(i, key, value); i++) {
            Assert.assrt(value.getType() == UResourceBundle.TABLE);

            String category = key.toString();
            UResource.Table usageTable = value.getTable();
            for (int j = 0; usageTable.getKeyAndValue(j, key, value); j++) {
                Assert.assrt(value.getType() == UResourceBundle.TABLE);

                String usage = key.toString();
                UResource.Table regionTable = value.getTable();
                for (int k = 0; regionTable.getKeyAndValue(k, key, value); k++) {
                    int test = value.getType();
                    Assert.assrt(value.getType() == UResourceBundle.ARRAY);

                    String region = key.toString();
                    UResource.Array preferencesTable = value.getArray();
                    ArrayList<UnitPreference> unitPreferences = new ArrayList<>();
                    for (int l = 0; preferencesTable.getValue(l, value); l++) {
                        Assert.assrt(value.getType() == UResourceBundle.TABLE);

                        UResource.Table singlePrefTable = value.getTable();
                        // TODO collect the data
                        String unit = null;
                        String geq = "1";
                        String skeleton = "";
                        for (int m = 0; singlePrefTable.getKeyAndValue(m, key, value); m++) {
                            Assert.assrt(value.getType() == UResourceBundle.STRING);
                            String keyString = key.toString();
                            if ("unit".equals(keyString)) {
                                unit = value.getString();
                            } else if ("geq".equals(keyString)) {
                                geq = value.getString();
                            } else if ("skeleton".equals(keyString)) {
                                skeleton = value.getString();
                            } else {
                                Assert.fail("key must be unit, geq or skeleton");
                            }
                        }
                        Assert.assrt(unit != null);
                        unitPreferences.add(new UnitPreference(unit, geq, skeleton));
                    }

                    Assert.assrt(!unitPreferences.isEmpty());
                    this.unitPreferences.insertUnitPreferences(
                            category,
                            usage,
                            region,
                            unitPreferences.toArray(new UnitPreference[0])
                    );
                }
            }
        }
    }


    public UnitPreferences getUnitPreferences() {
        return unitPreferences;
    }

    private UnitPreferences unitPreferences;
}
