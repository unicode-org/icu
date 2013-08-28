/*
 *******************************************************************************
 * Copyright (C) 2004-2013, Google Inc, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.text.UnicodeSet;

/**
 * A unit such as length, mass, volume, currency, etc.  A unit is
 * coupled with a numeric amount to produce a Measure.
 *
 * @see com.ibm.icu.util.Measure
 * @author Alan Liu
 * @stable ICU 3.0
 */
public class MeasureUnit implements Comparable<MeasureUnit>, Serializable {
    private static final long serialVersionUID = -1839973855554750484L;

    private static final Map<String, Map<String,MeasureUnit>> cache 
    = new HashMap<String, Map<String,MeasureUnit>>();

    protected final String type;
    protected final String code;
    /**
     * @param code
     */
    protected MeasureUnit(String type, String code) {
        this.type = type;
        this.code = code;
    }

    /**
     * Create an instance of a measurement unit.
     * @param type the type, such as "length"
     * @param code the code, such as "meter"
     * @return the unit.
     * @draft ICU 52
     * @provisional This API might change or be removed in a future release.
     */
    public static MeasureUnit getInstance(String type, String code) {
        Map<String, MeasureUnit> tmp = cache.get(type);
        if (tmp != null) {
            MeasureUnit result = tmp.get(code);
            if (result != null) {
                return result;
            }
        }
        if (type == null || !ASCII.containsAll(type) || code == null || ASCII_HYPHEN.containsAll(code)) {
            throw new NullPointerException("The type or code are invalid.");
        }
        synchronized (MeasureUnit.class) {
            return (Currency) MeasureUnit.addUnit(type, code, UNIT_FACTORY);
        }
    }

    static final UnicodeSet ASCII = new UnicodeSet('a', 'z').freeze();
    static final UnicodeSet ASCII_HYPHEN = new UnicodeSet('-', '-', 'a', 'z').freeze();

    protected interface Factory {
        MeasureUnit create(String type, String code);
    }

    private static Factory UNIT_FACTORY = new Factory() {
        public MeasureUnit create(String type, String code) {
            return new MeasureUnit(type, code);
        }
    };

    static Factory CURRENCY_FACTORY = new Factory() {
        public MeasureUnit create(String type, String code) {
            return new Currency(code);
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
        for (FormatWidth key : FormatWidth.values()) {
            try {
                ICUResourceBundle unitsTypeRes = resource.getWithFallback(key.resourceKey);
                int size = unitsTypeRes.getSize();
                for ( int index = 0; index < size; ++index) {
                    UResourceBundle unitsRes = unitsTypeRes.get(index);
                    String type = unitsRes.getKey();
                    int unitsSize = unitsRes.getSize();
                    for ( int index2 = 0; index2 < unitsSize; ++index2) {
                        String unitName = unitsRes.get(index2).getKey();
                        addUnit(type, unitName, UNIT_FACTORY);
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
                MeasureUnit.addUnit("currency", it.nextElement(), Currency.CURRENCY_FACTORY);
            }
        } catch (MissingResourceException e) {
            // fall through
        }
    }

    // Must only be called at static initialization, or inside synchronized block.
    protected static MeasureUnit addUnit(String type, String unitName, Factory factory) {
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
     * Get all of the available general units' types.
     * @return available units
     */
    public static Set<String> getAvailableTypes() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    /**
     * Get all of the available general units for a given type.
     * @return available units
     */
    public static Collection<MeasureUnit> getAvailable(String type) {
        Map<String, MeasureUnit> units = cache.get(type);
        return units == null ? null : Collections.unmodifiableCollection(units.values());
    }

    /**
     * Get all of the available general units.
     * @return available units
     */
    public static Set<MeasureUnit> getAvailable() {
        Set<MeasureUnit> result = new TreeSet<MeasureUnit>();
        for (String type : new TreeSet<String>(MeasureUnit.getAvailableTypes())) {
            for (MeasureUnit unit : MeasureUnit.getAvailable(type)) {
                result.add(unit);
            }
        }
        return result;
    }

    /**
     * Return a hashcode for this currency.
     * @stable ICU 2.2
     */
    @Override
    public int hashCode() {
        return code.hashCode() ^ type.hashCode();
    }

    /**
     * Return true if rhs is a Currency instance,
     * is non-null, and has the same currency code.
     * @stable ICU 2.2
     */
    @Override
    public boolean equals(Object rhs) {
        if (rhs == null) return false;
        if (rhs == this) return true;
        try {
            MeasureUnit c = (MeasureUnit) rhs;
            return type.equals(c.type) && code.equals(c.code);
        }
        catch (ClassCastException e) {
            return false;
        }
    }
    /**
     */
    public int compareTo(MeasureUnit other) {
        int diff;
        return this == other ? 0
                : (diff = type.compareTo(other.type)) != 0 ? diff
                        : code.compareTo(other.code);
    }

    @Override
    public String toString() {
        return type + "-" + code;
    }

    /**
     * @return the type for this unit
     */
    public String getType() {
        return type;
    }

    /**
     * @return the code for this unit.
     */
    public String getCode() {
        return code;
    }

    /** 
     * Useful constants. Not necessarily complete: see {@link #getAvailable()}.
     * @draft ICU 52
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit
    /** Constant for unit of acceleration: g-force */
    G_FORCE = MeasureUnit.getInstance("acceleration", "g-force"),
    /** Constant for unit of angle: degree */
    DEGREE = MeasureUnit.getInstance("angle", "degree"),
    /** Constant for unit of angle: minute */
    ARC_MINUTE = MeasureUnit.getInstance("angle", "minute"),
    /** Constant for unit of angle: second */
    ARC_SECOND = MeasureUnit.getInstance("angle", "second"),
    /** Constant for unit of area: acre */
    ACRE = MeasureUnit.getInstance("area", "acre"),
    /** Constant for unit of area: hectare */
    HECTARE = MeasureUnit.getInstance("area", "hectare"),
    /** Constant for unit of area: square-foot */
    SQUARE_FOOT = MeasureUnit.getInstance("area", "square-foot"),
    /** Constant for unit of area: square-kilometer */
    SQUARE_KILOMETER = MeasureUnit.getInstance("area", "square-kilometer"),
    /** Constant for unit of area: square-meter */
    SQUARE_METER = MeasureUnit.getInstance("area", "square-meter"),
    /** Constant for unit of area: square-mile */
    SQUARE_MILE = MeasureUnit.getInstance("area", "square-mile"),
    /** Constant for unit of duration: day */
    DAY = MeasureUnit.getInstance("duration", "day"),
    /** Constant for unit of duration: hour */
    HOUR = MeasureUnit.getInstance("duration", "hour"),
    /** Constant for unit of duration: millisecond */
    MILLISECOND = MeasureUnit.getInstance("duration", "millisecond"),
    /** Constant for unit of duration: minute */
    MINUTE = MeasureUnit.getInstance("duration", "minute"),
    /** Constant for unit of duration: month */
    MONTH = MeasureUnit.getInstance("duration", "month"),
    /** Constant for unit of duration: second */
    SECOND = MeasureUnit.getInstance("duration", "second"),
    /** Constant for unit of duration: week */
    WEEK = MeasureUnit.getInstance("duration", "week"),
    /** Constant for unit of duration: year */
    YEAR = MeasureUnit.getInstance("duration", "year"),
    /** Constant for unit of length: centimeter */
    CENTIMETER = MeasureUnit.getInstance("length", "centimeter"),
    /** Constant for unit of length: foot */
    FOOT = MeasureUnit.getInstance("length", "foot"),
    /** Constant for unit of length: inch */
    INCH = MeasureUnit.getInstance("length", "inch"),
    /** Constant for unit of length: kilometer */
    KILOMETER = MeasureUnit.getInstance("length", "kilometer"),
    /** Constant for unit of length: light-year */
    LIGHT_YEAR = MeasureUnit.getInstance("length", "light-year"),
    /** Constant for unit of length: meter */
    METER = MeasureUnit.getInstance("length", "meter"),
    /** Constant for unit of length: mile */
    MILE = MeasureUnit.getInstance("length", "mile"),
    /** Constant for unit of length: millimeter */
    MILLIMETER = MeasureUnit.getInstance("length", "millimeter"),
    /** Constant for unit of length: picometer */
    PICOMETER = MeasureUnit.getInstance("length", "picometer"),
    /** Constant for unit of length: yard */
    YARD = MeasureUnit.getInstance("length", "yard"),
    /** Constant for unit of mass: gram */
    GRAM = MeasureUnit.getInstance("mass", "gram"),
    /** Constant for unit of mass: kilogram */
    KILOGRAM = MeasureUnit.getInstance("mass", "kilogram"),
    /** Constant for unit of mass: ounce */
    OUNCE = MeasureUnit.getInstance("mass", "ounce"),
    /** Constant for unit of mass: pound */
    POUND = MeasureUnit.getInstance("mass", "pound"),
    /** Constant for unit of power: horsepower */
    HORSEPOWER = MeasureUnit.getInstance("power", "horsepower"),
    /** Constant for unit of power: kilowatt */
    KILOWATT = MeasureUnit.getInstance("power", "kilowatt"),
    /** Constant for unit of power: watt */
    WATT = MeasureUnit.getInstance("power", "watt"),
    /** Constant for unit of pressure: hectopascal */
    HECTOPASCAL = MeasureUnit.getInstance("pressure", "hectopascal"),
    /** Constant for unit of pressure: inch-hg */
    INCH_HG = MeasureUnit.getInstance("pressure", "inch-hg"),
    /** Constant for unit of pressure: millibar */
    MILLIBAR = MeasureUnit.getInstance("pressure", "millibar"),
    /** Constant for unit of speed: kilometer-per-hour */
    KILOMETER_PER_HOUR = MeasureUnit.getInstance("speed", "kilometer-per-hour"),
    /** Constant for unit of speed: meter-per-second */
    METER_PER_SECOND = MeasureUnit.getInstance("speed", "meter-per-second"),
    /** Constant for unit of speed: mile-per-hour */
    MILE_PER_HOUR = MeasureUnit.getInstance("speed", "mile-per-hour"),
    /** Constant for unit of temperature: celsius */
    CELSIUS = MeasureUnit.getInstance("temperature", "celsius"),
    /** Constant for unit of temperature: fahrenheit */
    FAHRENHEIT = MeasureUnit.getInstance("temperature", "fahrenheit"),
    /** Constant for unit of volume: cubic-kilometer */
    CUBIC_KILOMETER = MeasureUnit.getInstance("volume", "cubic-kilometer"),
    /** Constant for unit of volume: cubic-mile */
    CUBIC_MILE = MeasureUnit.getInstance("volume", "cubic-mile"),
    /** Constant for unit of volume: liter */
    LITER = MeasureUnit.getInstance("volume", "liter");

    /** Private **/

    private Object writeReplace() throws ObjectStreamException {
        return new MeasureUnitProxy(type, code);
    }

    static final class MeasureUnitProxy implements Externalizable {
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
            byte version = in.readByte(); // version
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
            return "currency".equals(type) 
                    ? Currency.getInstance(code) 
                            : MeasureUnit.getInstance(type, code);
        }
    }
}
