// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.ibm.icu.dev.test.CoreTestFmwk;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
// TODO: enable in Java 8: import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.ibm.icu.dev.test.serializable.SerializableTestUtility;
import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.DecimalFormatProperties.ParseMode;
import com.ibm.icu.impl.number.Padder.PadPosition;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.CurrencyPluralInfo;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

public class PropertiesTest extends CoreTestFmwk {

    @Test
    public void testBasicEquals() {
        DecimalFormatProperties p1 = new DecimalFormatProperties();
        DecimalFormatProperties p2 = new DecimalFormatProperties();
        assertEquals("DecimalFormatProperties.equals()", p1, p2);

        p1.setPositivePrefix("abc");
        assertNotEquals("DecimalFormatProperties.equals()", p1, p2);
        p2.setPositivePrefix("xyz");
        assertNotEquals("DecimalFormatProperties.equals()", p1, p2);
        p1.setPositivePrefix("xyz");
        assertEquals("DecimalFormatProperties.equals()", p1, p2);
    }

    @Test
    public void testFieldCoverage() {
        DecimalFormatProperties p0 = new DecimalFormatProperties();
        DecimalFormatProperties p1 = new DecimalFormatProperties();
        DecimalFormatProperties p2 = new DecimalFormatProperties();
        DecimalFormatProperties p3 = new DecimalFormatProperties();
        DecimalFormatProperties p4 = new DecimalFormatProperties();

        Set<Integer> hashCodes = new HashSet<Integer>();
        Field[] fields = DecimalFormatProperties.class.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            // Check for getters and setters
            String fieldNamePascalCase = Character.toUpperCase(field.getName().charAt(0))
                    + field.getName().substring(1);
            String getterName = "get" + fieldNamePascalCase;
            String setterName = "set" + fieldNamePascalCase;
            Method getter, setter;
            try {
                getter = DecimalFormatProperties.class.getMethod(getterName);
                assertEquals("Getter does not return correct type",
                        field.getType(),
                        getter.getReturnType());
            } catch (NoSuchMethodException e) {
                fail("Could not find method " + getterName + " for field " + field);
                continue;
            } catch (SecurityException e) {
                fail("Could not access method " + getterName + " for field " + field);
                continue;
            }
            try {
                setter = DecimalFormatProperties.class.getMethod(setterName, field.getType());
                assertEquals("Method " + setterName + " does not return correct type",
                        DecimalFormatProperties.class,
                        setter.getReturnType());
            } catch (NoSuchMethodException e) {
                fail("Could not find method " + setterName + " for field " + field);
                continue;
            } catch (SecurityException e) {
                fail("Could not access method " + setterName + " for field " + field);
                continue;
            }

            // Check for parameter name equality.
            // The parameter name is not always available, depending on compiler settings.
            // TODO: Enable in Java 8
            /*
             * Parameter param = setter.getParameters()[0]; if (!param.getName().subSequence(0,
             * 3).equals("arg")) { assertEquals("Parameter name should equal field name",
             * field.getName(), param.getName()); }
             */

            try {
                // Check for default value (should be null for objects)
                if (field.getType() != Integer.TYPE && field.getType() != Boolean.TYPE) {
                    Object default0 = getter.invoke(p0);
                    assertEquals("Field " + field + " has non-null default value:", null, default0);
                }

                // Check for getter, equals, and hash code behavior
                Object val0 = getSampleValueForType(field.getType(), 0);
                Object val1 = getSampleValueForType(field.getType(), 1);
                Object val2 = getSampleValueForType(field.getType(), 2);
                assertNotEquals("Test setup values should be different", val0, val1);
                setter.invoke(p1, val0);
                setter.invoke(p2, val0);
                assertEquals("Equal outputs for equal DecimalFormatProperties inputs", p1, p2);
                assertEquals("Equal outputs for equal DecimalFormatProperties inputs", p1.hashCode(), p2.hashCode());
                assertEquals("Equal outputs for equal DecimalFormatProperties inputs", getter.invoke(p1), getter.invoke(p2));
                assertEquals("Getter returns equal val set by setter for DecimalFormatProperties", getter.invoke(p1), val0);
                assertNotEquals("Getter returns equal vals for equal inputs", getter.invoke(p1), val1);
                hashCodes.add(p1.hashCode());
                setter.invoke(p1, val1);
                assertNotEquals("Field " + field + " is missing from equals()", p1, p2);
                assertNotEquals("Getter returns equal vals for equal inputs", getter.invoke(p1), getter.invoke(p2));
                assertNotEquals("Getter returns equal vals for equal inputs", getter.invoke(p1), val0);
                assertEquals("Getter returns equal vals for equal inputs", getter.invoke(p1), val1);
                setter.invoke(p1, val0);
                assertEquals("Field " + field + " setter might have side effects", p1, p2);
                assertEquals("Getter returns equal vals for equal inputs", p1.hashCode(), p2.hashCode());
                assertEquals("Getter returns equal vals for equal inputs", getter.invoke(p1), getter.invoke(p2));
                setter.invoke(p1, val1);
                setter.invoke(p2, val1);
                assertEquals("Getter returns equal vals for equal inputs", p1, p2);
                assertEquals("Getter returns equal vals for equal inputs", p1.hashCode(), p2.hashCode());
                assertEquals("Getter returns equal vals for equal inputs", getter.invoke(p1), getter.invoke(p2));
                setter.invoke(p1, val2);
                setter.invoke(p1, val1);
                assertEquals("Field " + field + " setter might have side effects", p1, p2);
                assertEquals("Getter returns equal vals for equal inputs", p1.hashCode(), p2.hashCode());
                assertEquals("Getter returns equal vals for equal inputs", getter.invoke(p1), getter.invoke(p2));
                hashCodes.add(p1.hashCode());

                // Check for clone behavior
                DecimalFormatProperties copy = p1.clone();
                assertEquals("Field " + field + " did not get copied in clone", p1, copy);
                assertEquals("Getter returns equal vals for equal inputs", p1.hashCode(), copy.hashCode());
                assertEquals("Getter returns equal vals for equal inputs", getter.invoke(p1), getter.invoke(copy));

                // Check for copyFrom behavior
                setter.invoke(p1, val0);
                assertNotEquals("Getter returns equal vals for equal inputs", p1, p2);
                assertNotEquals("Getter returns equal vals for equal inputs", getter.invoke(p1), getter.invoke(p2));
                p2.copyFrom(p1);
                assertEquals("Field " + field + " is missing from copyFrom()", p1, p2);
                assertEquals("Getter returns equal vals for equal inputs", p1.hashCode(), p2.hashCode());
                assertEquals("Getter returns equal vals for equal inputs", getter.invoke(p1), getter.invoke(p2));

                // Load values into p3 and p4 for clear() behavior test
                setter.invoke(p3, getSampleValueForType(field.getType(), 3));
                hashCodes.add(p3.hashCode());
                setter.invoke(p4, getSampleValueForType(field.getType(), 4));
                hashCodes.add(p4.hashCode());
            } catch (IllegalAccessException e) {
                fail("Could not access method for field " + field);
            } catch (IllegalArgumentException e) {
                fail("Could call method for field " + field);
            } catch (InvocationTargetException e) {
                fail("Could invoke method on target for field " + field);
            }
        }

        // Check for clear() behavior
        assertNotEquals("Setup for check for clear() behavior", p3, p4);
        p3.clear();
        p4.clear();
        assertEquals("A field is missing from the clear() function", p3, p4);

        // A good hashCode() implementation should produce very few collisions. We added at most
        // 4*fields.length codes to the set. We'll say the implementation is good if we had at least
        // fields.length unique values.
        // TODO: Should the requirement be stronger than this?
        assertTrue(
                "Too many hash code collisions: " + hashCodes.size() + " out of " + (fields.length * 4),
                hashCodes.size() >= fields.length);
    }

    /**
     * Creates a valid sample instance of the given type. Used to simulate getters and setters.
     *
     * @param type
     *            The type to generate.
     * @param seed
     *            An integer seed, guaranteed to be positive. The same seed should generate two instances
     *            that are equal. A different seed should in general generate two instances that are not
     *            equal; this might not always be possible, such as with booleans or enums where there
     *            are limited possible values.
     * @return An instance of the specified type.
     */
    Object getSampleValueForType(Class<?> type, int seed) {
        if (type == Integer.TYPE) {
            return seed * 1000001;

        } else if (type == Boolean.TYPE) {
            return (seed % 2) == 0;

        } else if (type == BigDecimal.class) {
            if (seed == 0)
                return null;
            return new BigDecimal(seed * 1000002);

        } else if (type == String.class) {
            if (seed == 0)
                return null;
            return BigInteger.valueOf(seed * 1000003).toString(32);

        } else if (type == CompactStyle.class) {
            if (seed == 0)
                return null;
            CompactStyle[] values = CompactStyle.values();
            return values[seed % values.length];

        } else if (type == Currency.class) {
            if (seed == 0)
                return null;
            Object[] currencies = Currency.getAvailableCurrencies().toArray();
            return currencies[seed % currencies.length];

        } else if (type == CurrencyPluralInfo.class) {
            if (seed == 0)
                return null;
            ULocale[] locales = ULocale.getAvailableLocales();
            return CurrencyPluralInfo.getInstance(locales[seed % locales.length]);

        } else if (type == CurrencyUsage.class) {
            if (seed == 0)
                return null;
            CurrencyUsage[] values = CurrencyUsage.values();
            return values[seed % values.length];

        } else if (type == FormatWidth.class) {
            if (seed == 0)
                return null;
            FormatWidth[] values = FormatWidth.values();
            return values[seed % values.length];

        } else if (type == Map.class) {
            // Map<String,Map<String,String>> for compactCustomData property
            if (seed == 0)
                return null;
            Map<String, Map<String, String>> outer = new HashMap<String, Map<String, String>>();
            Map<String, String> inner = new HashMap<String, String>();
            inner.put("one", "0 thousand");
            StringBuilder magnitudeKey = new StringBuilder();
            magnitudeKey.append("1000");
            for (int i = 0; i < seed % 9; i++) {
                magnitudeKey.append("0");
            }
            outer.put(magnitudeKey.toString(), inner);
            return outer;

        } else if (type == MathContext.class) {
            if (seed == 0)
                return null;
            RoundingMode[] modes = RoundingMode.values();
            return new MathContext(seed, modes[seed % modes.length]);

        } else if (type == MeasureUnit.class) {
            if (seed == 0)
                return null;
            Object[] units = MeasureUnit.getAvailable().toArray();
            return units[seed % units.length];

        } else if (type == PadPosition.class) {
            if (seed == 0)
                return null;
            PadPosition[] values = PadPosition.values();
            return values[seed % values.length];

        } else if (type == ParseMode.class) {
            if (seed == 0)
                return null;
            ParseMode[] values = ParseMode.values();
            return values[seed % values.length];

        } else if (type == PluralRules.class) {
            if (seed == 0)
                return null;
            ULocale[] locales = PluralRules.getAvailableULocales();
            return PluralRules.forLocale(locales[seed % locales.length]);

        } else if (type == RoundingMode.class) {
            if (seed == 0)
                return null;
            RoundingMode[] values = RoundingMode.values();
            return values[seed % values.length];

        } else {
            fail("Don't know how to handle type "
                    + type
                    + ". Please add it to getSampleValueForType().");
            return null;
        }
    }

    @Test
    public void TestBasicSerializationRoundTrip() throws IOException, ClassNotFoundException {
        DecimalFormatProperties props0 = new DecimalFormatProperties();

        // Write values to some of the fields
        PatternStringParser.parseToExistingProperties("A-**####,#00.00#b¤", props0);

        // Write to byte stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(props0);
        oos.flush();
        baos.close();
        byte[] bytes = baos.toByteArray();

        // Read from byte stream
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object obj = ois.readObject();
        ois.close();
        DecimalFormatProperties props1 = (DecimalFormatProperties) obj;

        // Test equality
        assertEquals("Did not round-trip through serialization", props0, props1);
    }

    /** Handler for serialization compatibility test suite. */
    public static class PropertiesHandler implements SerializableTestUtility.Handler {

        @Override
        public Object[] getTestObjects() {
            return new Object[] {
                    new DecimalFormatProperties(),
                    PatternStringParser.parseToProperties("x#,##0.00%"),
                    new DecimalFormatProperties().setCompactStyle(CompactStyle.LONG)
                            .setMinimumExponentDigits(2) };
        }

        @Override
        public boolean hasSameBehavior(Object a, Object b) {
            return a.equals(b);
        }
    }

    /**
     * Handler for the ICU 59 class named "Properties" before it was renamed to
     * "DecimalFormatProperties".
     */
    public static class ICU59PropertiesHandler implements SerializableTestUtility.Handler {

        @Override
        public Object[] getTestObjects() {
            return new Object[] { new com.ibm.icu.impl.number.Properties() };
        }

        @Override
        public boolean hasSameBehavior(Object a, Object b) {
            return true;
        }
    }
}
