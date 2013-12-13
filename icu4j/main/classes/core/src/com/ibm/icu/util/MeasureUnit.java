/*
 *******************************************************************************
 * Copyright (C) 2004-2013, Google Inc, International Business Machines        *
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
import java.util.ArrayList;
import java.util.Collection;
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
    protected final String type;
    
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected final String code;
    
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected MeasureUnit(String type, String code) {
        this.type = type;
        this.code = code;
    }
    
    /**
     * Get the type, such as "length"
     * 
     * @draft ICU 53
     * @provisional
     */
    public String getType() {
        return type;
    }
    

    /**
     * Get the subType, such as “foot”.
     *
     * @draft ICU 53
     * @provisional
     */
    public String getSubtype() {
        return code;
    }
    
    

    /**
     * @draft ICU 53
     * @provisional
     */
    @Override
    public int hashCode() {
        return 31 * type.hashCode() + code.hashCode();
    }
    
    /**
     * @draft ICU 53
     * @provisional
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
        return type.equals(c.type) && code.equals(c.code);
    }
    
    /**
     * @draft ICU 53
     * @provisional
     */
    @Override
    public String toString() {
        return type + "-" + code;
    }
    
    /**
     * Get all of the available units' types.
     * 
     * @draft ICU 53
     * @provisional
     */
    public synchronized static Set<String> getAvailableTypes() {
        return new HashSet<String>(cache.keySet());
    }

    /**
     * For the given type, return the available units.
     * @param type the type
     * @return the available units for type
     * @draft ICU 53
     * @provisional
     */
    public synchronized static Collection<MeasureUnit> getAvailable(String type) {
        Map<String, MeasureUnit> units = cache.get(type);
        return units == null ? null : new ArrayList<MeasureUnit>(units.values());
    }

    /**
     * Get all of the available units.
     *
     * @draft ICU 53
     * @provisional
     */
    public synchronized static Set<MeasureUnit> getAvailable() {
        Set<MeasureUnit> result = new HashSet<MeasureUnit>();
        for (String type : new HashSet<String>(MeasureUnit.getAvailableTypes())) {
            for (MeasureUnit unit : MeasureUnit.getAvailable(type)) {
                result.add(unit);
            }
        }
        return result;
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
    public static MeasureUnit internalGetInstance(String type, String code) {
        if (type == null || code == null) {
            throw new NullPointerException("Type and code must be non-null");
        }
        if (!"currency".equals(type)) {
            if (!ASCII.containsAll(type) || !ASCII_HYPHEN.containsAll(code)) {
                throw new IllegalArgumentException("The type or code are invalid.");
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
        return MeasureUnit.addUnit(type, code, factory);
    }

    static final UnicodeSet ASCII = new UnicodeSet('a', 'z').freeze();
    static final UnicodeSet ASCII_HYPHEN = new UnicodeSet('-', '-', 'a', 'z').freeze();

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected interface Factory {
        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        MeasureUnit create(String type, String code);
    }

    private static Factory UNIT_FACTORY = new Factory() {
        public MeasureUnit create(String type, String code) {
            return new MeasureUnit(type, code);
        }
    };

    static Factory CURRENCY_FACTORY = new Factory() {
        public MeasureUnit create(String unusedType, String code) {
            return new Currency(code);
        }
    };
    
    static Factory TIMEUNIT_FACTORY = new Factory() {
        public MeasureUnit create(String type, String code) {
           return new TimeUnit(type, code);
        }
    };


    //    /**
    //     * Register a unit for later use
    //     * @param type the type, such as "length"
    //     * @param code the code, such as "meter"
    //     * @return the unit.
    //     * @draft ICU 52
    //     * @provisional This API might change or be removed in a future release.
    //     */
    //    public static synchronized MeasureUnit registerUnit(String type, String code) {
    //        MeasureUnit result = getInstance(type, code);
    //        if (result == null) {
    //            result = addUnit(type, code, MY_FACTORY);
    //        }
    //        return result;
    //    }
    //
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

   


    /** 
     * Useful constants. Not necessarily complete: see {@link #getAvailable()}.
     * @draft ICU 53
     * @provisional
     */
    public static final MeasureUnit
    /** Constant for unit of acceleration: g-force */
    G_FORCE = MeasureUnit.internalGetInstance("acceleration", "g-force"),
    /** Constant for unit of angle: degree */
    DEGREE = MeasureUnit.internalGetInstance("angle", "degree"),
    /** Constant for unit of angle: minute */
    ARC_MINUTE = MeasureUnit.internalGetInstance("angle", "arc-minute"),
    /** Constant for unit of angle: second */
    ARC_SECOND = MeasureUnit.internalGetInstance("angle", "arc-second"),
    /** Constant for unit of area: acre */
    ACRE = MeasureUnit.internalGetInstance("area", "acre"),
    /** Constant for unit of area: hectare */
    HECTARE = MeasureUnit.internalGetInstance("area", "hectare"),
    /** Constant for unit of area: square-foot */
    SQUARE_FOOT = MeasureUnit.internalGetInstance("area", "square-foot"),
    /** Constant for unit of area: square-kilometer */
    SQUARE_KILOMETER = MeasureUnit.internalGetInstance("area", "square-kilometer"),
    /** Constant for unit of area: square-meter */
    SQUARE_METER = MeasureUnit.internalGetInstance("area", "square-meter"),
    /** Constant for unit of area: square-mile */
    SQUARE_MILE = MeasureUnit.internalGetInstance("area", "square-mile"),
    /** Constant for unit of duration: millisecond */
    MILLISECOND = MeasureUnit.internalGetInstance("duration", "millisecond"),
    /** Constant for unit of length: centimeter */
    CENTIMETER = MeasureUnit.internalGetInstance("length", "centimeter"),
    /** Constant for unit of length: foot */
    FOOT = MeasureUnit.internalGetInstance("length", "foot"),
    /** Constant for unit of length: inch */
    INCH = MeasureUnit.internalGetInstance("length", "inch"),
    /** Constant for unit of length: kilometer */
    KILOMETER = MeasureUnit.internalGetInstance("length", "kilometer"),
    /** Constant for unit of length: light-year */
    LIGHT_YEAR = MeasureUnit.internalGetInstance("length", "light-year"),
    /** Constant for unit of length: meter */
    METER = MeasureUnit.internalGetInstance("length", "meter"),
    /** Constant for unit of length: mile */
    MILE = MeasureUnit.internalGetInstance("length", "mile"),
    /** Constant for unit of length: millimeter */
    MILLIMETER = MeasureUnit.internalGetInstance("length", "millimeter"),
    /** Constant for unit of length: picometer */
    PICOMETER = MeasureUnit.internalGetInstance("length", "picometer"),
    /** Constant for unit of length: yard */
    YARD = MeasureUnit.internalGetInstance("length", "yard"),
    /** Constant for unit of mass: gram */
    GRAM = MeasureUnit.internalGetInstance("mass", "gram"),
    /** Constant for unit of mass: kilogram */
    KILOGRAM = MeasureUnit.internalGetInstance("mass", "kilogram"),
    /** Constant for unit of mass: ounce */
    OUNCE = MeasureUnit.internalGetInstance("mass", "ounce"),
    /** Constant for unit of mass: pound */
    POUND = MeasureUnit.internalGetInstance("mass", "pound"),
    /** Constant for unit of power: horsepower */
    HORSEPOWER = MeasureUnit.internalGetInstance("power", "horsepower"),
    /** Constant for unit of power: kilowatt */
    KILOWATT = MeasureUnit.internalGetInstance("power", "kilowatt"),
    /** Constant for unit of power: watt */
    WATT = MeasureUnit.internalGetInstance("power", "watt"),
    /** Constant for unit of pressure: hectopascal */
    HECTOPASCAL = MeasureUnit.internalGetInstance("pressure", "hectopascal"),
    /** Constant for unit of pressure: inch-hg */
    INCH_HG = MeasureUnit.internalGetInstance("pressure", "inch-hg"),
    /** Constant for unit of pressure: millibar */
    MILLIBAR = MeasureUnit.internalGetInstance("pressure", "millibar"),
    /** Constant for unit of speed: kilometer-per-hour */
    KILOMETER_PER_HOUR = MeasureUnit.internalGetInstance("speed", "kilometer-per-hour"),
    /** Constant for unit of speed: meter-per-second */
    METER_PER_SECOND = MeasureUnit.internalGetInstance("speed", "meter-per-second"),
    /** Constant for unit of speed: mile-per-hour */
    MILE_PER_HOUR = MeasureUnit.internalGetInstance("speed", "mile-per-hour"),
    /** Constant for unit of temperature: celsius */
    CELSIUS = MeasureUnit.internalGetInstance("temperature", "celsius"),
    /** Constant for unit of temperature: fahrenheit */
    FAHRENHEIT = MeasureUnit.internalGetInstance("temperature", "fahrenheit"),
    /** Constant for unit of volume: cubic-kilometer */
    CUBIC_KILOMETER = MeasureUnit.internalGetInstance("volume", "cubic-kilometer"),
    /** Constant for unit of volume: cubic-mile */
    CUBIC_MILE = MeasureUnit.internalGetInstance("volume", "cubic-mile"),
    /** Constant for unit of volume: liter */
    LITER = MeasureUnit.internalGetInstance("volume", "liter");
    
    public static TimeUnit
    /** Constant for unit of duration: year */
    YEAR = (TimeUnit) MeasureUnit.internalGetInstance("duration", "year"),
    /** Constant for unit of duration: month */
    MONTH = (TimeUnit) MeasureUnit.internalGetInstance("duration", "month"),
    /** Constant for unit of duration: week */
    WEEK = (TimeUnit) MeasureUnit.internalGetInstance("duration", "week"),
    /** Constant for unit of duration: day */
    DAY = (TimeUnit) MeasureUnit.internalGetInstance("duration", "day"),
    /** Constant for unit of duration: hour */
    HOUR = (TimeUnit) MeasureUnit.internalGetInstance("duration", "hour"),
    /** Constant for unit of duration: minute */
    MINUTE = (TimeUnit) MeasureUnit.internalGetInstance("duration", "minute"),
    /** Constant for unit of duration: second */
    SECOND = (TimeUnit) MeasureUnit.internalGetInstance("duration", "second");

    /** Private **/

    private Object writeReplace() throws ObjectStreamException {
        return new MeasureUnitProxy(type, code);
    }

    static final class MeasureUnitProxy implements Externalizable {
        private static final long serialVersionUID = -3910681415330989598L;

        private String type;
        private String code;

        public MeasureUnitProxy(String type, String code) {
            this.type = type;
            this.code = code;
        }

        // Must have public constructor, to enable Externalizable
        public MeasureUnitProxy() {
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeByte(0); // version
            out.writeUTF(type);
            out.writeUTF(code);
            out.writeShort(0); // allow for more data.
        }

        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            /* byte version = */ in.readByte(); // version
            type = in.readUTF();
            code = in.readUTF();
            // allow for more data from future version
            int extra = in.readShort();
            if (extra > 0) {
                byte[] extraBytes = new byte[extra];
                in.read(extraBytes, 0, extra);
            }
        }

        private Object readResolve() throws ObjectStreamException {
            return MeasureUnit.internalGetInstance(type, code);
        }
    }
}
