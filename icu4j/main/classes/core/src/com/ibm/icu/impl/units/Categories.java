package com.ibm.icu.impl.units;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.util.UResourceBundle;

import java.util.TreeMap;

public class Categories {

    public Categories() {
        // Read unit Categories
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "units");
        CategoriesSink sink = new CategoriesSink();
        resource.getAllItemsWithFallback(Constants.CATEGORY_TABLE_NAME, sink);
        this.mapFromUnitToCategory = sink.getMapFromUnitToCategory();
    }


    /**
     * Contains the map between units in their base units into their category.
     * For example:  meter-per-second --> "speed"
     */
    TreeMap<String, String> mapFromUnitToCategory;
}
