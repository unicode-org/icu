// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.units;

import static java.math.MathContext.DECIMAL128;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

import com.ibm.icu.impl.IllegalIcuArgumentException;
import com.ibm.icu.util.MeasureUnit;

// TODO ICU-22683: Consider splitting handling of special mappings into separate (possibly internal) class
public class UnitsConverter {
    private BigDecimal conversionRate;
    private boolean reciprocal;
    private BigDecimal offset;
    private String specialSource;
    private String specialTarget;

    /**
     * Constructor of <code>UnitsConverter</code>.
     * NOTE:
     * - source and target must be under the same category
     * - e.g. meter to mile --> both of them are length units.
     * <p>
     * NOTE:
     * This constructor creates an instance of <code>UnitsConverter</code> internally.
     *
     * @param sourceIdentifier represents the source unit identifier.
     * @param targetIdentifier represents the target unit identifier.
     */
    public UnitsConverter(String sourceIdentifier, String targetIdentifier) {
        this(
                MeasureUnitImpl.forIdentifier(sourceIdentifier),
                MeasureUnitImpl.forIdentifier(targetIdentifier),
                new ConversionRates()
        );
    }

    /**
     * Constructor of <code>UnitsConverter</code>.
     * NOTE:
     * - source and target must be under the same category
     * - e.g. meter to mile --> both of them are length units.
     * This converts from source to base to target (one of those may be a no-op).
     *
     * @param source          represents the source unit.
     * @param target          represents the target unit.
     * @param conversionRates contains all the needed conversion rates.
     */
    public UnitsConverter(MeasureUnitImpl source, MeasureUnitImpl target, ConversionRates conversionRates) {
        Convertibility convertibility = extractConvertibility(source, target, conversionRates);
        if (convertibility != Convertibility.CONVERTIBLE && convertibility != Convertibility.RECIPROCAL) {
            throw new IllegalIcuArgumentException("input units must be convertible or reciprocal");
        }

        this.specialSource = conversionRates.getSpecialMappingName(source);
        this.specialTarget = conversionRates.getSpecialMappingName(target);

        if (this.specialSource == null && this.specialTarget == null) {
            Factor sourceToBase = conversionRates.getFactorToBase(source);
            Factor targetToBase = conversionRates.getFactorToBase(target);

            if (convertibility == Convertibility.CONVERTIBLE) {
                this.conversionRate = sourceToBase.divide(targetToBase).getConversionRate();
            } else {
                assert convertibility == Convertibility.RECIPROCAL;
                this.conversionRate = sourceToBase.multiply(targetToBase).getConversionRate();
            }
            this.reciprocal = convertibility == Convertibility.RECIPROCAL;

            // calculate the offset
            this.offset = conversionRates.getOffset(source, target, sourceToBase, targetToBase, convertibility);
            // We should see no offsets for reciprocal conversions - they don't make sense:
            assert convertibility != Convertibility.RECIPROCAL || this.offset == BigDecimal.ZERO;
        } else {
            this.reciprocal = false;
            this.offset = BigDecimal.ZERO;
            if (this.specialSource == null) {
                // conversionRate is for source to base only
                this.conversionRate = conversionRates.getFactorToBase(source).getConversionRate();
            } else if (this.specialTarget == null) {
                // conversionRate is for base to target only
                this.conversionRate = conversionRates.getFactorToBase(target).getConversionRate();
            } else {
                this.conversionRate = BigDecimal.ONE;
            }
        }
    }

    static public Convertibility extractConvertibility(MeasureUnitImpl source, MeasureUnitImpl target, ConversionRates conversionRates) {
        ArrayList<SingleUnitImpl> sourceSingleUnits = conversionRates.extractBaseUnits(source);
        ArrayList<SingleUnitImpl> targetSingleUnits = conversionRates.extractBaseUnits(target);

        HashMap<String, Integer> dimensionMap = new HashMap<>();

        insertInMap(dimensionMap, sourceSingleUnits, 1);
        insertInMap(dimensionMap, targetSingleUnits, -1);

        if (areDimensionsZeroes(dimensionMap)) return Convertibility.CONVERTIBLE;

        insertInMap(dimensionMap, targetSingleUnits, 2);
        if (areDimensionsZeroes(dimensionMap)) return Convertibility.RECIPROCAL;

        return Convertibility.UNCONVERTIBLE;
    }

    /**
     * Helpers
     */
    private static void insertInMap(HashMap<String, Integer> dimensionMap, ArrayList<SingleUnitImpl> singleUnits, int multiplier) {
        for (SingleUnitImpl singleUnit :
                singleUnits) {
            if (dimensionMap.containsKey(singleUnit.getSimpleUnitID())) {
                dimensionMap.put(singleUnit.getSimpleUnitID(), dimensionMap.get(singleUnit.getSimpleUnitID()) + singleUnit.getDimensionality() * multiplier);
            } else {
                dimensionMap.put(singleUnit.getSimpleUnitID(), singleUnit.getDimensionality() * multiplier);
            }
        }
    }

    private static boolean areDimensionsZeroes(HashMap<String, Integer> dimensionMap) {
        for (Integer value :
                dimensionMap.values()) {
            if (!value.equals(0)) return false;
        }

        return true;
    }

    // Convert inputValue (source) to base then to target
    public BigDecimal convert(BigDecimal inputValue) {
        BigDecimal result = inputValue;
        if (this.specialSource != null || this.specialTarget != null) {
            BigDecimal base = inputValue;
            // convert input (=source) to base
            if (this.specialSource != null) {
                // We  have a special mapping from source to base (not using factor, offset).
                // Currently the only supported mapping is a scale-based mapping for beaufort.
                base = (this.specialSource.equals("beaufort"))?
                    scaleToBase(inputValue, minMetersPerSecForBeaufort): inputValue;
            } else {
                // Standard mapping (using factor, offset) from source to base.
                base = inputValue.multiply(this.conversionRate);
            }
            // convert base to result (=target)
            if (this.specialTarget != null) {
                // We  have a special mapping from base to target (not using factor, offset).
                // Currently the only supported mapping is a scale-based mapping for beaufort.
                result = (this.specialTarget.equals("beaufort"))?
                    baseToScale(base, minMetersPerSecForBeaufort): base;
            } else {
                // Standard mapping (using factor, offset) from base to target.
                result = base.divide(this.conversionRate, DECIMAL128);
            }
            return result;
        }
        result = inputValue.multiply(this.conversionRate).add(offset);
        if (this.reciprocal) {
            // We should see no offsets for reciprocal conversions - they don't make sense:
            assert offset == BigDecimal.ZERO;
            if (result.compareTo(BigDecimal.ZERO) == 0) {
                // TODO(ICU-21988): determine desirable behaviour
                return BigDecimal.ZERO;
            }
            result = BigDecimal.ONE.divide(result, DECIMAL128);
        }
        return result;
    }

    // Convert inputValue (target) to base then to source
    public BigDecimal convertInverse(BigDecimal inputValue) {
        BigDecimal result = inputValue;
        if (this.specialSource != null || this.specialTarget != null) {
            BigDecimal base = inputValue;
            // convert input (=target) to base
            if (this.specialTarget != null) {
                // We  have a special mapping from target to base (not using factor, offset).
                // Currently the only supported mapping is a scale-based mapping for beaufort.
                base = (this.specialTarget.equals("beaufort"))?
                    scaleToBase(inputValue, minMetersPerSecForBeaufort): inputValue;
            } else {
                // Standard mapping (using factor, offset) from target to base.
                base = inputValue.multiply(this.conversionRate);
            }
            // convert base to result (=source)
            if (this.specialSource != null) {
                // We  have a special mapping from base to source (not using factor, offset).
                // Currently the only supported mapping is a scale-based mapping for beaufort.
                result = (this.specialSource.equals("beaufort"))?
                    baseToScale(base, minMetersPerSecForBeaufort): base;
            } else {
                // Standard mapping (using factor, offset) from base to source.
                result = base.divide(this.conversionRate, DECIMAL128);
            }
            return result;
        }
        if (this.reciprocal) {
            // We should see no offsets for reciprocal conversions - they don't make sense:
            assert offset == BigDecimal.ZERO;
            if (result.compareTo(BigDecimal.ZERO) == 0) {
                // TODO(ICU-21988): determine desirable behaviour
                return BigDecimal.ZERO;
            }
            result = BigDecimal.ONE.divide(result, DECIMAL128);
        }
        result = result.subtract(offset).divide(this.conversionRate, DECIMAL128);
        return result;
    }

    // TODO per CLDR-17421 and ICU-22683: consider getting the data below from CLDR
    private static final BigDecimal[] minMetersPerSecForBeaufort = {
        // Minimum m/s (base) values for each Bft value, plus an extra artificial value;
        // when converting from Bft to m/s, the middle of the range will be used
        // (Values from table in Wikipedia, except for artificial value).
        // Since this is 0 based, max Beaufort value is thus array dimension minus 2.
        BigDecimal.valueOf(0.0), // 0 Bft
        BigDecimal.valueOf(0.3), // 1
        BigDecimal.valueOf(1.6), // 2
        BigDecimal.valueOf(3.4), // 3
        BigDecimal.valueOf(5.5), // 4
        BigDecimal.valueOf(8.0), // 5
        BigDecimal.valueOf(10.8), // 6
        BigDecimal.valueOf(13.9), // 7
        BigDecimal.valueOf(17.2), // 8
        BigDecimal.valueOf(20.8), // 9
        BigDecimal.valueOf(24.5), // 10
        BigDecimal.valueOf(28.5), // 11
        BigDecimal.valueOf(32.7), // 12
        BigDecimal.valueOf(36.9), // 13
        BigDecimal.valueOf(41.4), // 14
        BigDecimal.valueOf(46.1), // 15
        BigDecimal.valueOf(51.1), // 16
        BigDecimal.valueOf(55.8), // 17
        BigDecimal.valueOf(61.4), // artificial end of range 17 to give reasonable midpoint
    };

    // Convert from what should be discrete scale values for a particular unit like beaufort
    // to a corresponding value in the base unit (which can have any decimal value, like meters/sec).
    // First we round the scale value to the nearest integer (in case it is specified with a fractional value),
    // then we map that to a value in middle of the range of corresponding base values.
    // This can handle different scales, specified by minBaseForScaleValues[].
    private BigDecimal scaleToBase(BigDecimal scaleValue, BigDecimal[] minBaseForScaleValues) {
        BigDecimal pointFive = BigDecimal.valueOf(0.5);
        BigDecimal scaleAdjust = scaleValue.abs().add(pointFive); // adjust up for later truncation
        BigDecimal scaleAdjustCapped = scaleAdjust.min(BigDecimal.valueOf(minBaseForScaleValues.length - 2));
        int scaleIndex = scaleAdjustCapped.intValue();
        // Return midpont of range (the final range uses an articial end to produce reasonable midpoint)
        return minBaseForScaleValues[scaleIndex].add(minBaseForScaleValues[scaleIndex + 1]).multiply(pointFive);
    }

    // Convert from a value in the base unit (which can have any decimal value, like meters/sec) to a corresponding
    // discrete value in a scale (like beaufort), where each scale value represents a range of base values.
    // We binary-search the ranges to find the one that contains the specified base value, and return its index.
    // This can handle different scales, specified by minBaseForScaleValues[].
    private BigDecimal baseToScale(BigDecimal baseValue, BigDecimal[] minBaseForScaleValues) {
        int scaleIndex = Arrays.binarySearch(minBaseForScaleValues, baseValue.abs());
        if (scaleIndex < 0) {
            // since our first array entry is 0, this value will always be -2 or less
            scaleIndex = -scaleIndex - 2;
        }
        int scaleMax = minBaseForScaleValues.length - 2;
        if (scaleIndex > scaleMax) {
            scaleIndex = scaleMax;
        }
        return BigDecimal.valueOf(scaleIndex);
    }

    public enum Convertibility {
        CONVERTIBLE,
        RECIPROCAL,
        UNCONVERTIBLE,
    }

    public ConversionInfo getConversionInfo() {
        ConversionInfo result = new ConversionInfo();
        result.conversionRate = this.conversionRate;
        result.offset = this.offset;
        result.reciprocal = this.reciprocal;

        return result;
    }

    public static class ConversionInfo {
        public BigDecimal conversionRate;
        public BigDecimal offset;
        public boolean reciprocal;
    }

    /**
     * Responsible for all the Factor operation
     * NOTE:
     * This class is immutable
     */
    static class Factor {
        private BigDecimal factorNum;
        private BigDecimal factorDen;

        // The exponents below correspond to ICU4C's Factor::exponents[].

        /** Exponent for the ft_to_m constant */
        private int exponentFtToM = 0;
        /** Exponent for PI */
        private int exponentPi = 0;
        /** Exponent for gravity (gravity-of-earth, "g") */
        private int exponentGravity = 0;
        /** Exponent for Newtonian constant of gravitation "G". */
        private int exponentG = 0;
        /** Exponent for the imperial-gallon to cubic-meter conversion rate constant */
        private int exponentGalImpToM3 = 0;
        /** Exponent for the pound to kilogram conversion rate constant */
        private int exponentLbToKg = 0;
        /** Exponent for the glucose molar mass conversion rate constant */
        private int exponentGlucoseMolarMass = 0;
        /** Exponent for the item per mole conversion rate constant */
        private int exponentItemPerMole = 0;
        /** Exponent for the meters per AU conversion rate constant */
        private int exponentMetersPerAU = 0;
        /** Exponent for the sec per julian year conversion rate constant */
        private int exponentSecPerJulianYear = 0;
        /** Exponent for the speed of light meters per second" conversion rate constant */
        private int exponentSpeedOfLightMetersPerSecond = 0;
        /** Exponent for https://en.wikipedia.org/wiki/Japanese_units_of_measurement */
        private int exponentShoToM3 = 0;
        /** Exponent for https://en.wikipedia.org/wiki/Japanese_units_of_measurement */
        private int exponentTsuboToM2 = 0;
        /** Exponent for https://en.wikipedia.org/wiki/Japanese_units_of_measurement */
        private int exponentShakuToM = 0;
        /** Exponent for Atomic Mass Unit */
        private int exponentAMU = 0;

        /**
         * Creates Empty Factor
         */
        public Factor() {
            this.factorNum = BigDecimal.valueOf(1);
            this.factorDen = BigDecimal.valueOf(1);
        }

        public static Factor processFactor(String factor) {
            assert (!factor.isEmpty());

            // Remove all spaces in the factor
            factor = factor.replaceAll("\\s+", "");

            String[] fractions = factor.split("/");
            assert (fractions.length == 1 || fractions.length == 2);

            if (fractions.length == 1) {
                return processFactorWithoutDivision(fractions[0]);
            }

            Factor num = processFactorWithoutDivision(fractions[0]);
            Factor den = processFactorWithoutDivision(fractions[1]);
            return num.divide(den);
        }

        private static Factor processFactorWithoutDivision(String factorWithoutDivision) {
            Factor result = new Factor();
            for (String poweredEntity :
                    factorWithoutDivision.split(Pattern.quote("*"))) {
                result.addPoweredEntity(poweredEntity);
            }

            return result;
        }

        /**
         * Copy this <code>Factor</code>.
         */
        protected Factor copy() {
            Factor result = new Factor();
            result.factorNum = this.factorNum;
            result.factorDen = this.factorDen;

            result.exponentFtToM = this.exponentFtToM;
            result.exponentPi = this.exponentPi;
            result.exponentGravity = this.exponentGravity;
            result.exponentG = this.exponentG;
            result.exponentGalImpToM3 = this.exponentGalImpToM3;
            result.exponentLbToKg = this.exponentLbToKg;
            result.exponentGlucoseMolarMass = this.exponentGlucoseMolarMass;
            result.exponentItemPerMole = this.exponentItemPerMole;
            result.exponentMetersPerAU = this.exponentMetersPerAU;
            result.exponentSecPerJulianYear = this.exponentSecPerJulianYear;
            result.exponentSpeedOfLightMetersPerSecond = this.exponentSpeedOfLightMetersPerSecond;
            result.exponentShoToM3 = this.exponentShoToM3;
            result.exponentTsuboToM2 = this.exponentTsuboToM2;
            result.exponentShakuToM = this.exponentShakuToM;
            result.exponentAMU = this.exponentAMU;

            return result;
        }

        /**
         * Returns a single {@code BigDecimal} that represent the conversion rate after substituting all the constants.
         *
         * In ICU4C, see Factor::substituteConstants().
         */
        public BigDecimal getConversionRate() {
            // TODO: this copies all the exponents then doesn't use them at all.
            Factor resultCollector = this.copy();

            // TODO(icu-units#92): port C++ unit tests to Java.
            // These values are a hard-coded subset of unitConstants in the
            // units resources file. A unit test should check that all constants
            // in the resource file are at least recognised by the code.
            // In ICU4C, these constants live in constantsValues[].
            resultCollector.multiply(new BigDecimal("0.3048"), this.exponentFtToM);
            // TODO: this recalculates this division every time this is called.
            resultCollector.multiply(new BigDecimal("411557987.0").divide(new BigDecimal("131002976.0"), DECIMAL128), this.exponentPi);
            resultCollector.multiply(new BigDecimal("9.80665"), this.exponentGravity);
            resultCollector.multiply(new BigDecimal("6.67408E-11"), this.exponentG);
            resultCollector.multiply(new BigDecimal("0.00454609"), this.exponentGalImpToM3);
            resultCollector.multiply(new BigDecimal("0.45359237"), this.exponentLbToKg);
            resultCollector.multiply(new BigDecimal("180.1557"), this.exponentGlucoseMolarMass);
            resultCollector.multiply(new BigDecimal("6.02214076E+23"), this.exponentItemPerMole);
            resultCollector.multiply(new BigDecimal("149597870700"), this.exponentMetersPerAU);
            resultCollector.multiply(new BigDecimal("31557600"), this.exponentSecPerJulianYear);
            resultCollector.multiply(new BigDecimal("299792458"), this.exponentSpeedOfLightMetersPerSecond);
            resultCollector.multiply(new BigDecimal("0.001803906836964688204"), this.exponentShoToM3);   // 2401/(1331*1000)
            resultCollector.multiply(new BigDecimal("3.305785123966942"), this.exponentTsuboToM2);    // 400/121
            resultCollector.multiply(new BigDecimal("0.033057851239669"), this.exponentShakuToM);     // 4/121
            resultCollector.multiply(new BigDecimal("1.66053878283E-27"), this.exponentAMU);

            return resultCollector.factorNum.divide(resultCollector.factorDen, DECIMAL128);
        }

        /** Multiplies the Factor instance by value^power. */
        private void multiply(BigDecimal value, int power) {
            if (power == 0) return;

            BigDecimal absPoweredValue = value.pow(Math.abs(power), DECIMAL128);
            if (power > 0) {
                this.factorNum = this.factorNum.multiply(absPoweredValue);
            } else {
                this.factorDen = this.factorDen.multiply(absPoweredValue);
            }
        }

        /** Apply SI or binary prefix to the Factor. */
        public Factor applyPrefix(MeasureUnit.MeasurePrefix unitPrefix) {
            Factor result = this.copy();
            if (unitPrefix == MeasureUnit.MeasurePrefix.ONE) {
                return result;
            }

            int base = unitPrefix.getBase();
            int power = unitPrefix.getPower();
            BigDecimal absFactor =
                BigDecimal.valueOf(base).pow(Math.abs(power), DECIMAL128);

            if (power < 0) {
                result.factorDen = this.factorDen.multiply(absFactor);
                return result;
            }

            result.factorNum = this.factorNum.multiply(absFactor);
            return result;
        }

        public Factor power(int power) {
            Factor result = new Factor();
            if (power == 0) return result;
            if (power > 0) {
                result.factorNum = this.factorNum.pow(power);
                result.factorDen = this.factorDen.pow(power);
            } else {
                result.factorNum = this.factorDen.pow(power * -1);
                result.factorDen = this.factorNum.pow(power * -1);
            }

            result.exponentFtToM = this.exponentFtToM * power;
            result.exponentPi = this.exponentPi * power;
            result.exponentGravity = this.exponentGravity * power;
            result.exponentG = this.exponentG * power;
            result.exponentGalImpToM3 = this.exponentGalImpToM3 * power;
            result.exponentLbToKg = this.exponentLbToKg * power;
            result.exponentGlucoseMolarMass = this.exponentGlucoseMolarMass * power;
            result.exponentItemPerMole = this.exponentItemPerMole * power;
            result.exponentMetersPerAU = this.exponentMetersPerAU * power;
            result.exponentSecPerJulianYear = this.exponentSecPerJulianYear * power;
            result.exponentSpeedOfLightMetersPerSecond =
                this.exponentSpeedOfLightMetersPerSecond * power;
            result.exponentShoToM3 = this.exponentShoToM3 * power;
            result.exponentTsuboToM2 = this.exponentTsuboToM2 * power;
            result.exponentShakuToM = this.exponentShakuToM * power;
            result.exponentAMU = this.exponentAMU * power;

            return result;
        }

        public Factor divide(Factor other) {
            Factor result = new Factor();
            result.factorNum = this.factorNum.multiply(other.factorDen);
            result.factorDen = this.factorDen.multiply(other.factorNum);

            result.exponentFtToM = this.exponentFtToM - other.exponentFtToM;
            result.exponentPi = this.exponentPi - other.exponentPi;
            result.exponentGravity = this.exponentGravity - other.exponentGravity;
            result.exponentG = this.exponentG - other.exponentG;
            result.exponentGalImpToM3 = this.exponentGalImpToM3 - other.exponentGalImpToM3;
            result.exponentLbToKg = this.exponentLbToKg - other.exponentLbToKg;
            result.exponentGlucoseMolarMass =
                this.exponentGlucoseMolarMass - other.exponentGlucoseMolarMass;
            result.exponentItemPerMole = this.exponentItemPerMole - other.exponentItemPerMole;
            result.exponentMetersPerAU = this.exponentMetersPerAU - other.exponentMetersPerAU;
            result.exponentSecPerJulianYear = this.exponentSecPerJulianYear - other.exponentSecPerJulianYear;
            result.exponentSpeedOfLightMetersPerSecond =
                this.exponentSpeedOfLightMetersPerSecond - other.exponentSpeedOfLightMetersPerSecond;
            result.exponentShoToM3 = this.exponentShoToM3 - other.exponentShoToM3;
            result.exponentTsuboToM2 = this.exponentTsuboToM2 - other.exponentTsuboToM2;
            result.exponentShakuToM = this.exponentShakuToM - other.exponentShakuToM;
            result.exponentAMU = this.exponentAMU - other.exponentAMU;

            return result;
        }

        public Factor multiply(Factor other) {
            Factor result = new Factor();
            result.factorNum = this.factorNum.multiply(other.factorNum);
            result.factorDen = this.factorDen.multiply(other.factorDen);

            result.exponentFtToM = this.exponentFtToM + other.exponentFtToM;
            result.exponentPi = this.exponentPi + other.exponentPi;
            result.exponentGravity = this.exponentGravity + other.exponentGravity;
            result.exponentG = this.exponentG + other.exponentG;
            result.exponentGalImpToM3 = this.exponentGalImpToM3 + other.exponentGalImpToM3;
            result.exponentLbToKg = this.exponentLbToKg + other.exponentLbToKg;
            result.exponentGlucoseMolarMass =
                this.exponentGlucoseMolarMass + other.exponentGlucoseMolarMass;
            result.exponentItemPerMole = this.exponentItemPerMole + other.exponentItemPerMole;
            result.exponentMetersPerAU = this.exponentMetersPerAU + other.exponentMetersPerAU;
            result.exponentSecPerJulianYear = this.exponentSecPerJulianYear + other.exponentSecPerJulianYear;
            result.exponentSpeedOfLightMetersPerSecond =
                this.exponentSpeedOfLightMetersPerSecond + other.exponentSpeedOfLightMetersPerSecond;
            result.exponentShoToM3 = this.exponentShoToM3 + other.exponentShoToM3;
            result.exponentTsuboToM2 = this.exponentTsuboToM2 + other.exponentTsuboToM2;
            result.exponentShakuToM = this.exponentShakuToM  + other.exponentShakuToM;
            result.exponentAMU = this.exponentAMU  + other.exponentAMU;

            return result;
        }

        /**
         * Adds Entity with power or not. For example, {@code 12 ^ 3} or {@code 12}.
         *
         * @param poweredEntity
         */
        private void addPoweredEntity(String poweredEntity) {
            String[] entities = poweredEntity.split(Pattern.quote("^"));
            assert (entities.length == 1 || entities.length == 2);

            int power = entities.length == 2 ? Integer.parseInt(entities[1]) : 1;
            this.addEntity(entities[0], power);
        }

        private void addEntity(String entity, int power) {
            if ("ft_to_m".equals(entity)) {
                this.exponentFtToM += power;
            } else if ("ft2_to_m2".equals(entity)) {
                this.exponentFtToM += 2 * power;
            } else if ("ft3_to_m3".equals(entity)) {
                this.exponentFtToM += 3 * power;
            } else if ("in3_to_m3".equals(entity)) {
                this.exponentFtToM += 3 * power;
                this.factorDen = this.factorDen.multiply(BigDecimal.valueOf(Math.pow(12, 3)));
            } else if ("gal_to_m3".equals(entity)) {
                this.factorNum = this.factorNum.multiply(BigDecimal.valueOf(231));
                this.exponentFtToM += 3 * power;
                this.factorDen = this.factorDen.multiply(BigDecimal.valueOf(12 * 12 * 12));
            } else if ("gal_imp_to_m3".equals(entity)) {
                this.exponentGalImpToM3 += power;
            } else if ("G".equals(entity)) {
                this.exponentG += power;
            } else if ("gravity".equals(entity)) {
                this.exponentGravity += power;
            } else if ("lb_to_kg".equals(entity)) {
                this.exponentLbToKg += power;
            } else if ("glucose_molar_mass".equals(entity)) {
                this.exponentGlucoseMolarMass += power;
            } else if ("item_per_mole".equals(entity)) {
                this.exponentItemPerMole += power;
            } else if ("meters_per_AU".equals(entity)) {
                this.exponentMetersPerAU += power;
            } else if ("PI".equals(entity)) {
                this.exponentPi += power;
            } else if ("sec_per_julian_year".equals(entity)) {
                this.exponentSecPerJulianYear += power;
            } else if ("speed_of_light_meters_per_second".equals(entity)) {
                this.exponentSpeedOfLightMetersPerSecond += power;
            } else if ("sho_to_m3".equals(entity)) {
                this.exponentShoToM3 += power;
            } else if ("tsubo_to_m2".equals(entity)) {
                this.exponentTsuboToM2 += power;
            } else if ("shaku_to_m".equals(entity)) {
                this.exponentShakuToM += power;
            } else if ("AMU".equals(entity)) {
                this.exponentAMU += power;
            } else {
                BigDecimal decimalEntity = new BigDecimal(entity).pow(power, DECIMAL128);
                this.factorNum = this.factorNum.multiply(decimalEntity);
            }
        }
    }

    @Override
    public String toString() {
        return "UnitsConverter [conversionRate=" + conversionRate + ", offset=" + offset + "]";
    }
}
