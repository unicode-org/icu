// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.units;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.IllegalIcuArgumentException;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.util.UResourceBundle;

/**
 * UnitAliases is a class that holds all the aliases and their replacements for
 * units.
 */
public class UnitAliases {
    /**
     * A class to hold the alias and replacement.
     */
    public static class Alias {
        public final String alias;
        public final String replacement;

        public Alias(String alias, String replacement) {
            this.alias = alias;
            this.replacement = replacement;
        }
    }

    private static final class AllAliasSink extends UResource.Sink {
        private final Map<String, String> mapAliasToReplacement = new HashMap<>();

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            String alias = key.toString();
            UResource.Table aliasEntryTable = value.getTable();
            
            if (aliasEntryTable.findValue("replacement", value)) {
                String replacement = value.toString();
                this.mapAliasToReplacement.put(alias, replacement);
            } else {
                throw new IllegalIcuArgumentException("No replacement found for alias: " + alias);
            }
        }

        public Map<String, String> getAliasMap() {
            return Collections.unmodifiableMap(mapAliasToReplacement);
        }
    }

    private final Map<String, String> mapAliasToReplacement;

    public UnitAliases() {
        // Read unit aliases
        ICUResourceBundle metadataResource = (ICUResourceBundle) UResourceBundle
                .getBundleInstance(ICUData.ICU_BASE_NAME, "metadata");
        AllAliasSink aliasSink = new AllAliasSink();
        metadataResource.getAllChildrenWithFallback("alias/unit", aliasSink);
        this.mapAliasToReplacement = aliasSink.getAliasMap();
    }

    /**
     * Returns a list of all the aliases.
     * 
     * @return an unmodifiable list of aliases
     */
    public List<Alias> getAliases() {
        List<Alias> aliasList = new ArrayList<>(mapAliasToReplacement.size());
        for (Map.Entry<String, String> entry : mapAliasToReplacement.entrySet()) {
            aliasList.add(new Alias(entry.getKey(), entry.getValue()));
        }
        return Collections.unmodifiableList(aliasList);
    }

    /**
     * Returns the replacement unit for a given alias, or null if no alias exists.
     * 
     * @param alias the alias unit to look up
     * @return the replacement unit, or null if not found
     */
    public String getReplacement(String alias) {
        return mapAliasToReplacement.get(alias);
    }
}