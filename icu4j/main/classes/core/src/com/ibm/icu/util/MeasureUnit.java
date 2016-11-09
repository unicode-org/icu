/*
 *******************************************************************************
 * Copyright (C) 2004-2014, Google Inc, International Business Machines        *
 * Corporation and others. All Rights Reserved.                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.text.UnicodeSet;

/**
 * A unit such as length, mass, volume, currency, etc.  A unit is
 * coupled with a numeric amount to produce a Measure. MeasureUnit objects are immutable.
 * All subclasses must guarantee that. (However, subclassing is discouraged.)

 *
 * @see com.ibm.icu.util.Measure
 * @author Alan Liu
 * @stable ICU 3.0
 */
public class MeasureUnit implements Serializable {
    private static final long serialVersionUID = -1839973855554750484L;
    
    // Used to pre-fill the cache. These same constants appear in MeasureFormat too.
    private static final String[] unitKeys = new String[]{"units", "unitsShort", "unitsNarrow"};
    
    private static final Map<String, Map<String,MeasureUnit>> cache 
    = new HashMap<String, Map<String,MeasureUnit>>();

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    protected final String type;
    
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    protected final String subType;
    
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    protected MeasureUnit(String type, String subType) {
        this.type = type;
        this.subType = subType;
    }
    
    /**
     * Get the type, such as "length"
     * 
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public String getType() {
        return type;
    }
    

    /**
     * Get the subType, such as “foot”.
     *
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public String getSubtype() {
        return subType;
    }
    
    

    /**
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public int hashCode() {
        return 31 * type.hashCode() + subType.hashCode();
    }
    
    /**
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public boolean equals(Object rhs) {
        if (rhs == this) {
            return true;
        }
        if (!(rhs instanceof MeasureUnit)) {
            return false;
        }
        MeasureUnit c = (MeasureUnit) rhs;
        return type.equals(c.type) && subType.equals(c.subType);
    }
    
    /**
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public String toString() {
        return type + "-" + subType;
    }
    
    /**
     * Get all of the available units' types. Returned set is unmodifiable.
     * 
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public synchronized static Set<String> getAvailableTypes() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    /**
     * For the given type, return the available units.
     * @param type the type
     * @return the available units for type. Returned set is unmodifiable.
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public synchronized static Set<MeasureUnit> getAvailable(String type) {
        Map<String, MeasureUnit> units = cache.get(type);
        // Train users not to modify returned set from the start giving us more
        // flexibility for implementation.
        return units == null ? Collections.<MeasureUnit>emptySet()
                : Collections.unmodifiableSet(new HashSet<MeasureUnit>(units.values()));
    }

    /**
     * Get all of the available units. Returned set is unmodifiable.
     *
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public synchronized static Set<MeasureUnit> getAvailable() {
        Set<MeasureUnit> result = new HashSet<MeasureUnit>();
        for (String type : new HashSet<String>(MeasureUnit.getAvailableTypes())) {
            for (MeasureUnit unit : MeasureUnit.getAvailable(type)) {
                result.add(unit);
            }
        }
        // Train users not to modify returned set from the start giving us more
        // flexibility for implementation.
        return Collections.unmodifiableSet(result);
    }

    /**
     * Create a MeasureUnit instance (creates a singleton instance).
     * <p>
     * Normally this method should not be used, since there will be no formatting data
     * available for it, and it may not be returned by getAvailable().
     * However, for special purposes (such as CLDR tooling), it is available.
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static MeasureUnit internalGetInstance(String type, String subType) {
        if (type == null || subType == null) {
            throw new NullPointerException("Type and subType must be non-null");
        }
        if (!"currency".equals(type)) {
            if (!ASCII.containsAll(type) || !ASCII_HYPHEN.containsAll(subType)) {
                throw new IllegalArgumentException("The type or subType are invalid.");
            }
        }
        Factory factory;
        if ("currency".equals(type)) {
            factory = CURRENCY_FACTORY;
        } else if ("duration".equals(type)) {
            factory = TIMEUNIT_FACTORY;
        } else {
            factory = UNIT_FACTORY;
        }
        return MeasureUnit.addUnit(type, subType, factory);
    }

    static final UnicodeSet ASCII = new UnicodeSet('a', 'z').freeze();
    static final UnicodeSet ASCII_HYPHEN = new UnicodeSet('-', '-', 'a', 'z').freeze();

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    protected interface Factory {
        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        MeasureUnit create(String type, String subType);
    }

    private static Factory UNIT_FACTORY = new Factory() {
        public MeasureUnit create(String type, String subType) {
            return new MeasureUnit(type, subType);
        }
    };

    static Factory CURRENCY_FACTORY = new Factory() {
        public MeasureUnit create(String unusedType, String subType) {
            return new Currency(subType);
        }
    };
    
    static Factory TIMEUNIT_FACTORY = new Factory() {
        public MeasureUnit create(String type, String subType) {
           return new TimeUnit(type, subType);
        }
    };

    static {
        // load all of the units for English, since we know that that is a superset.
        /**
         *     units{
         *            duration{
         *                day{
         *                    one{"{0} ден"}
         *                    other{"{0} дена"}
         *                }
         */
        ICUResourceBundle resource = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "en");
        for (String key : unitKeys) {
            try {
                ICUResourceBundle unitsTypeRes = resource.getWithFallback(key);
                int size = unitsTypeRes.getSize();
                for ( int index = 0; index < size; ++index) {
                    UResourceBundle unitsRes = unitsTypeRes.get(index);
                    String type = unitsRes.getKey();
                    int unitsSize = unitsRes.getSize();
                    for ( int index2 = 0; index2 < unitsSize; ++index2) {
                        ICUResourceBundle unitNameRes = (ICUResourceBundle)unitsRes.get(index2);
                        if (unitNameRes.get("other") != null) {
                            internalGetInstance(type, unitNameRes.getKey());
                        }
                    }
                }
            } catch ( MissingResourceException e ) {
                continue;
            }
        }
        // preallocate currencies
        try {
            UResourceBundle bundle = UResourceBundle.getBundleInstance(
                    ICUResourceBundle.ICU_BASE_NAME,
                    "currencyNumericCodes",
                    ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            UResourceBundle codeMap = bundle.get("codeMap");
            for (Enumeration<String> it = codeMap.getKeys(); it.hasMoreElements();) {
                MeasureUnit.internalGetInstance("currency", it.nextElement());
            }
        } catch (MissingResourceException e) {
            // fall through
        }
    }

    // Must only be called at static initialization, or inside synchronized block.
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    protected synchronized static MeasureUnit addUnit(String type, String unitName, Factory factory) {
        Map<String, MeasureUnit> tmp = cache.get(type);
        if (tmp == null) {
            cache.put(type, tmp = new HashMap<String, MeasureUnit>());
        } else {
            // "intern" the type by setting to first item's type.
            type = tmp.entrySet().iterator().next().getValue().type; 
        }
        MeasureUnit unit = tmp.get(unitName);
        if (unit == null) {
            tmp.put(unitName, unit = factory.create(type, unitName));
        }
        return unit;
    }


    /*
     * Useful constants. Not necessarily complete: see {@link #getAvailable()}.
     */

    /**
     * Constant for unit of acceleration: g-force
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit G_FORCE = MeasureUnit.internalGetInstance("acceleration", "g-force");

    /**
     * Constant for unit of angle: degree
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit DEGREE = MeasureUnit.internalGetInstance("angle", "degree");

    /**
     * Constant for unit of angle: minute
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit ARC_MINUTE = MeasureUnit.internalGetInstance("angle", "arc-minute");

    /**
     * Constant for unit of angle: second
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit ARC_SECOND = MeasureUnit.internalGetInstance("angle", "arc-second");

    /**
     * Constant for unit of area: acre
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit ACRE = MeasureUnit.internalGetInstance("area", "acre");

    /**
     * Constant for unit of area: hectare
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit HECTARE = MeasureUnit.internalGetInstance("area", "hectare");

    /**
     * Constant for unit of area: square-foot
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit SQUARE_FOOT = MeasureUnit.internalGetInstance("area", "square-foot");

    /**
     * Constant for unit of area: square-kilometer
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit SQUARE_KILOMETER = MeasureUnit.internalGetInstance("area", "square-kilometer");

    /**
     * Constant for unit of area: square-meter
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit SQUARE_METER = MeasureUnit.internalGetInstance("area", "square-meter");

    /**
     * Constant for unit of area: square-mile
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit SQUARE_MILE = MeasureUnit.internalGetInstance("area", "square-mile");

    /**
     * Constant for unit of duration: millisecond
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit MILLISECOND = MeasureUnit.internalGetInstance("duration", "millisecond");

    /**
     * Constant for unit of length: centimeter
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit CENTIMETER = MeasureUnit.internalGetInstance("length", "centimeter");

    /**
     * Constant for unit of length: foot
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit FOOT = MeasureUnit.internalGetInstance("length", "foot");

    /**
     * Constant for unit of length: inch
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit INCH = MeasureUnit.internalGetInstance("length", "inch");

    /**
     * Constant for unit of length: kilometer
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit KILOMETER = MeasureUnit.internalGetInstance("length", "kilometer");

    /**
     * Constant for unit of length: light-year
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit LIGHT_YEAR = MeasureUnit.internalGetInstance("length", "light-year");

    /**
     * Constant for unit of length: meter
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit METER = MeasureUnit.internalGetInstance("length", "meter");

    /**
     * Constant for unit of length: mile
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit MILE = MeasureUnit.internalGetInstance("length", "mile");

    /**
     * Constant for unit of length: millimeter
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit MILLIMETER = MeasureUnit.internalGetInstance("length", "millimeter");

    /**
     * Constant for unit of length: picometer
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit PICOMETER = MeasureUnit.internalGetInstance("length", "picometer");

    /**
     * Constant for unit of length: yard
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit YARD = MeasureUnit.internalGetInstance("length", "yard");

    /**
     * Constant for unit of mass: gram
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit GRAM = MeasureUnit.internalGetInstance("mass", "gram");

    /**
     * Constant for unit of mass: kilogram
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit KILOGRAM = MeasureUnit.internalGetInstance("mass", "kilogram");

    /**
     * Constant for unit of mass: ounce
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit OUNCE = MeasureUnit.internalGetInstance("mass", "ounce");

    /**
     * Constant for unit of mass: pound
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit POUND = MeasureUnit.internalGetInstance("mass", "pound");

    /**
     * Constant for unit of power: horsepower
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit HORSEPOWER = MeasureUnit.internalGetInstance("power", "horsepower");

    /**
     * Constant for unit of power: kilowatt
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit KILOWATT = MeasureUnit.internalGetInstance("power", "kilowatt");

    /**
     * Constant for unit of power: watt
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit WATT = MeasureUnit.internalGetInstance("power", "watt");

    /**
     * Constant for unit of pressure: hectopascal
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit HECTOPASCAL = MeasureUnit.internalGetInstance("pressure", "hectopascal");

    /**
     * Constant for unit of pressure: inch-hg
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit INCH_HG = MeasureUnit.internalGetInstance("pressure", "inch-hg");

    /**
     * Constant for unit of pressure: millibar
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit MILLIBAR = MeasureUnit.internalGetInstance("pressure", "millibar");

    /**
     * Constant for unit of speed: kilometer-per-hour
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit KILOMETER_PER_HOUR = MeasureUnit.internalGetInstance("speed", "kilometer-per-hour");

    /**
     * Constant for unit of speed: meter-per-second
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit METER_PER_SECOND = MeasureUnit.internalGetInstance("speed", "meter-per-second");

    /**
     * Constant for unit of speed: mile-per-hour
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit MILE_PER_HOUR = MeasureUnit.internalGetInstance("speed", "mile-per-hour");

    /**
     * Constant for unit of temperature: celsius
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit CELSIUS = MeasureUnit.internalGetInstance("temperature", "celsius");

    /**
     * Constant for unit of temperature: fahrenheit
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit FAHRENHEIT = MeasureUnit.internalGetInstance("temperature", "fahrenheit");

    /**
     * Constant for unit of volume: cubic-kilometer
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit CUBIC_KILOMETER = MeasureUnit.internalGetInstance("volume", "cubic-kilometer");

    /**
     * Constant for unit of volume: cubic-mile
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit CUBIC_MILE = MeasureUnit.internalGetInstance("volume", "cubic-mile");

    /**
     * Constant for unit of volume: liter
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit LITER = MeasureUnit.internalGetInstance("volume", "liter");    

    /**
     * Constant for unit of duration: year
     * @stable ICU 4.0
     */
    public static TimeUnit YEAR = (TimeUnit) MeasureUnit.internalGetInstance("duration", "year");

    /**
     * Constant for unit of duration: month
     * @stable ICU 4.0
     */
    public static TimeUnit MONTH = (TimeUnit) MeasureUnit.internalGetInstance("duration", "month");

    /**
     * Constant for unit of duration: week
     * @stable ICU 4.0
     */
    public static TimeUnit WEEK = (TimeUnit) MeasureUnit.internalGetInstance("duration", "week");

    /**
     * Constant for unit of duration: day
     * @stable ICU 4.0
     */
    public static TimeUnit DAY = (TimeUnit) MeasureUnit.internalGetInstance("duration", "day");

    /**
     * Constant for unit of duration: hour
     * @stable ICU 4.0
     */
    public static TimeUnit HOUR = (TimeUnit) MeasureUnit.internalGetInstance("duration", "hour");

    /**
     * Constant for unit of duration: minute
     * @stable ICU 4.0
     */
    public static TimeUnit MINUTE = (TimeUnit) MeasureUnit.internalGetInstance("duration", "minute");

    /**
     * Constant for unit of duration: second
     * @stable ICU 4.0
     */
    public static TimeUnit SECOND = (TimeUnit) MeasureUnit.internalGetInstance("duration", "second");

    /* Private */

    private Object writeReplace() throws ObjectStreamException {
        return new MeasureUnitProxy(type, subType);
    }

    static final class MeasureUnitProxy implements Externalizable {
        private static final long serialVersionUID = -3910681415330989598L;

        private String type;
        private String subType;

        public MeasureUnitProxy(String type, String subType) {
            this.type = type;
            this.subType = subType;
        }

        // Must have public constructor, to enable Externalizable
        public MeasureUnitProxy() {
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeByte(0); // version
            out.writeUTF(type);
            out.writeUTF(subType);
            out.writeShort(0); // allow for more data.
        }

        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            /* byte version = */ in.readByte(); // version
            type = in.readUTF();
            subType = in.readUTF();
            // allow for more data from future version
            int extra = in.readShort();
            if (extra > 0) {
                byte[] extraBytes = new byte[extra];
                in.read(extraBytes, 0, extra);
            }
        }

        private Object readResolve() throws ObjectStreamException {
            return MeasureUnit.internalGetInstance(type, subType);
        }
    }
}
