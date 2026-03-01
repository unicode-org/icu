// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.base.Ascii.toLowerCase;
import static com.google.common.base.Preconditions.checkState;
import static org.unicode.cldr.api.AttributeKey.keyOf;
import static org.unicode.cldr.api.CldrData.PathOrder.DTD;
import static org.unicode.cldr.api.CldrDataType.BCP47;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.RbPath;
import org.unicode.icu.tool.cldrtoicu.CldrDataProcessor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * A mapper to collect BCP-47 data from {@link CldrDataType#BCP47 BCP47} data under paths
 * matching:
 * <pre>{@code
 *   //ldmlBCP47/keyword/key[@name=*]/type[@name=*]
 * }</pre>
 */
public final class Bcp47Mapper {
    // Other attributes (e.g. "alias") are value attributes and don't need to be matched here.
    private static final AttributeKey KEY_NAME = keyOf("key", "name");
    private static final AttributeKey KEY_ALIAS = keyOf("key", "alias");
    private static final AttributeKey KEY_VALUE_TYPE = keyOf("key", "valueType");

    private static final AttributeKey TYPE_NAME = keyOf("type", "name");
    private static final AttributeKey TYPE_ALIASES = keyOf("type", "alias");
    private static final AttributeKey PREFERRED_TYPE_NAME = keyOf("type", "preferred");

    // Deprecation of the data is not the same as deprecation of attributes themselves. This
    // deprecation relates to identifying data which exists, but is not longer the right way to
    // represent things (which means it can be important for clients to know about).
    private static final AttributeKey KEY_DEPRECATED = keyOf("key", "deprecated");
    private static final AttributeKey TYPE_DEPRECATED = keyOf("type", "deprecated");

    // Attributes that can be emitted under the /keyInfo or /typeInfo paths for auxiliary
    // information in the ICU data. If the value is equal to the declared default, it is ignored.
    // NOTE: The need for hard-coded default values is a hack because there's not nice way (yet)
    // to determine the default for implicit values via the DTD. Ideally this would be automatic
    // and the AttributeKey class would be able to have a method like "isDefault(String value)".
    private static final ImmutableMap<AttributeKey, String> INFO_ATTRIBUTES =
        ImmutableMap.of(KEY_VALUE_TYPE, "", KEY_DEPRECATED, "false", TYPE_DEPRECATED, "false");

    private static final RbPath RB_KEYMAP = RbPath.of("keyMap");
    private static final RbPath RB_TYPE_ALIAS = RbPath.of("typeAlias", "timezone:alias");
    private static final RbPath RB_MAP_ALIAS = RbPath.of("typeMap", "timezone:alias");
    private static final RbPath RB_BCP_ALIAS = RbPath.of("bcpTypeAlias", "tz:alias");

    private static final CldrDataProcessor<Bcp47Mapper> BCP47_PROCESSOR;
    static {
        CldrDataProcessor.Builder<Bcp47Mapper> processor = CldrDataProcessor.builder();
        processor
            .addAction("//ldmlBCP47/keyword/key[@name=*]", (m, p) -> m.new ValueCollector(p))
            .addValueAction("type[@name=*]", ValueCollector::collect);
        BCP47_PROCESSOR = processor.build();
    }

    /**
     * Processes data from the given supplier to generate Timezone and BCP-47 ICU data.
     *
     * @param src the CLDR data supplier to process.
     * @return A list of IcuData instances containing BCP-47 data to be written to files.
     */
    public static ImmutableList<IcuData> process(CldrDataSupplier src) {
        return process(src.getDataForType(BCP47));
    }

    @VisibleForTesting // It's easier to supply a fake data instance than a fake supplier.
    static ImmutableList<IcuData> process(CldrData cldrData) {
        Bcp47Mapper mapper = BCP47_PROCESSOR.process(cldrData, new Bcp47Mapper(), DTD);
        mapper.addKeyMapValues();
        return ImmutableList.of(mapper.keyTypeData, mapper.tzData);
    }

    // Outer visitor which handles "key" paths by installing sub-visitor methods to process
    // each child "type" element. Depending on the key name, values are stored in different
    // IcuData instances.
    private final IcuData tzData = new IcuData("timezoneTypes", false);
    private final IcuData keyTypeData = new IcuData("keyTypeData", false);
    // A map collecting each key and values as they are visited.
    // TODO: Convert this to a Map<RbPath, String> which involves removing the '@' prefix hack.
    private Map<String, String> keyMap = new LinkedHashMap<>();

    private Bcp47Mapper() { }

    // Post processing to add additional captured attribute values and some special cases.
    private void addKeyMapValues() {
        IcuData keyData = keyTypeData;
        // Add all the keyMap values into the IcuData file.
        for (Entry<String, String> kmData : keyMap.entrySet()) {
            String bcpKey = kmData.getKey();
            String key = kmData.getValue();
            if (bcpKey.startsWith("@")) {
                // Undoing the weird hack in addInfoAttributes(). This can be done better.
                // We use "parse()" because these are full paths, and not single elements.
                keyData.add(RbPath.parse(bcpKey.substring(1)), key);
                continue;
            }
            if (bcpKey.equals(key)) {
                // An empty value indicates that the BCP47 key is same as the legacy key.
                bcpKey = "";
            }
            keyData.add(RB_KEYMAP.extendBy(key), bcpKey);
        }
        // Add aliases for timezone data.
        keyData.add(RB_TYPE_ALIAS, "/ICUDATA/timezoneTypes/typeAlias/timezone");
        keyData.add(RB_MAP_ALIAS, "/ICUDATA/timezoneTypes/typeMap/timezone");
        keyData.add(RB_BCP_ALIAS, "/ICUDATA/timezoneTypes/bcpTypeAlias/tz");
    }

    private final class ValueCollector {
        private final String keyName;
        // Mutable data to be written into (differs depending on the key name).
        private final IcuData icuData;

        ValueCollector(CldrPath prefix) {
            this.keyName = Ascii.toLowerCase(KEY_NAME.valueFrom(prefix));
            this.icuData = keyName.equals("tz") ? tzData : keyTypeData;
        }

        private void collect(CldrValue value) {
            String typeName = TYPE_NAME.valueFrom(value);
            // Note that if a "preferred" type exists, we treat the value specially and add
            // it only as an alias. We expected values with a preferred replacement to
            // always be explicitly deprecated.
            Optional<String> prefName = PREFERRED_TYPE_NAME.optionalValueFrom(value);
            if (prefName.isPresent()) {
                checkState(KEY_DEPRECATED.booleanValueFrom(value, false)
                        || TYPE_DEPRECATED.booleanValueFrom(value, false),
                    "unexpected 'preferred' attribute for non-deprecated value: %s", value);
                icuData.add(RbPath.of("bcpTypeAlias", keyName, typeName), prefName.get());
                return;
            }
            // Note: There are some deprecated values which don't have a preferred
            // replacement and these will be processed below (in particular we need to emit
            // the fact that they are deprecated).

            // Not all key elements have an alias. E.g. in calendar.xml:
            //     <key name="fw" description="First day of week" since="28">
            // But we still add it as a alias to itself (which is later turned into a path with
            // an empty value).
            String keyAlias = toLowerCase(KEY_ALIAS.valueFrom(value, keyName));

            keyMap.put(keyName, keyAlias);
            RbPath typeMapPrefix = RbPath.of("typeMap", keyAlias);

            List<String> typeAliases = TYPE_ALIASES.listOfValuesFrom(value);
            if (typeAliases.isEmpty()) {
                // Generate type map entry using empty value (an empty value indicates same
                // type name is used for both BCP47 and legacy type).
                icuData.add(typeMapPrefix.extendBy(typeName), "");
            } else {
                String mainAlias = typeAliases.get(0);
                icuData.add(typeMapPrefix.extendBy(quoteAlias(mainAlias)), typeName);
                // Put additional aliases as secondary aliases referencing the main alias.
                RbPath typeAliasPrefix = RbPath.of("typeAlias", keyAlias);
                typeAliases.stream()
                    .skip(1)
                    .map(Bcp47Mapper::quoteAlias)
                    .forEach(a -> icuData.add(typeAliasPrefix.extendBy(a), mainAlias));
            }
            addInfoAttributes(keyName, typeName, value.getValueAttributes());
        }

        // Add any additional attributes present to the attribute map. Note that this code was
        // copied from largely undocumented code, and the precise reasoning for why this is
        // needed or why it's done this way is not completely clear. It is very likely that it
        // can be simplified.
        //
        // The '@' symbol added here is just a magic token that gets stripped off again in the
        // addKeyMapValues() method, it appears to just be a way to distinguish keys added via
        // this method vs during the collect method. A better approach might just be to have two
        // maps.
        // TODO: Remove the use of '@' and simplify the logic for "info" attributes (infoMap?).
        private void addInfoAttributes(
            String keyName, String typeName, ImmutableMap<AttributeKey, String> attributes) {
            // Only emit deprecation for the "key" level, even if all types below that are also
            // marked as deprecated. Only do this for a subset of attributes (INFO_ATTRIBUTES).
            Set<AttributeKey> keys =
                Sets.intersection(attributes.keySet(), INFO_ATTRIBUTES.keySet());
            for (AttributeKey a : keys) {
                String value = attributes.get(a);
                // Skip empty or default values in attributes.
                if (value.isEmpty() || INFO_ATTRIBUTES.get(a).equals(value)) {
                    continue;
                }
                // The ID for the xxxInfo paths in ICU is the path fragment at which the
                // attribute exists. Since we only process complete paths here, we must do a
                // bit of reconstruction based on the element name of the attribute we are
                // processing. This relies on explicit knowledge that the paths are "<key>" or
                // "<key>/<type>". This all gets less messy if we switch to RbPath.
                String id =
                    a.getElementName().equals("key") ? keyName : keyName + "/" + typeName;
                keyMap.put(
                    "@" + a.getElementName() + "Info/" + a.getAttributeName() + "/" + id,
                    value);
            }
        }
    }

    /**
     * Escapes alias values containing '/' so they can appear in resource bundle paths. This
     * function replaces '/' with ':' and quotes the result (e.g. foo/bar -> "foo:bar").
     *
     * <p>This is needed for timezone "metazone" ID strings which are of the form 'Foo/Bar'
     * in the CLDR data.
     */
    // TODO: Switch to RbPath and do quoting automatically when ICU data is written out.
    private static String quoteAlias(String str) {
        return str.indexOf('/') == -1 ? str : '"' + str.replace('/', ':') + '"';
    }
}
