// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.units;

import static java.math.MathContext.DECIMAL128;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import com.ibm.icu.util.MeasureUnit;

public class UnitConverter {
    private BigDecimal conversionRate;
    private BigDecimal offset;

    /**
     * Constructor of `UnitConverter`.
     * NOTE:
     * - source and target must be under the same category
     * - e.g. meter to mile --> both of them are length units.
     *
     * @param source          represents the source unit.
     * @param target          represents the target unit.
     * @param conversionRates contains all the needed conversion rates.
     */
    public UnitConverter(MeasureUnitImpl source, MeasureUnitImpl target, ConversionRates conversionRates) {
        Convertibility convertibility = extractConvertibility(source, target, conversionRates);
        assert (convertibility == Convertibility.CONVERTIBLE || convertibility == Convertibility.RECIPROCAL);

        Factor sourceToBase = conversionRates.getFactorToBase(source);
        Factor targetToBase = conversionRates.getFactorToBase(target);

        if (convertibility == Convertibility.CONVERTIBLE) {
            this.conversionRate = sourceToBase.divide(targetToBase).getConversionRate();
        } else {
            this.conversionRate = sourceToBase.multiply(targetToBase).getConversionRate();
        }

        // calculate the offset
        this.offset = conversionRates.getOffset(source, target, sourceToBase, targetToBase, convertibility);
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
            if (dimensionMap.containsKey(singleUnit.getSimpleUnit())) {
                dimensionMap.put(singleUnit.getSimpleUnit(), dimensionMap.get(singleUnit.getSimpleUnit()) + singleUnit.getDimensionality() * multiplier);
            } else {
                dimensionMap.put(singleUnit.getSimpleUnit(), singleUnit.getDimensionality() * multiplier);
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

    public BigDecimal convert(BigDecimal inputValue) {
        return inputValue.multiply(this.conversionRate).add(offset);
    }

    public BigDecimal convertInverse(BigDecimal inputValue) {
        return inputValue.subtract(offset).divide(this.conversionRate, DECIMAL128);
    }

    public enum Convertibility {
        CONVERTIBLE,
        RECIPROCAL,
        UNCONVERTIBLE,
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

            return result;
        }

        /**
         * Returns a single `BigDecimal` that represent the conversion rate after substituting all the constants.
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

        public Factor applySiPrefix(MeasureUnit.SIPrefix siPrefix) {
            Factor result = this.copy();
            if (siPrefix == MeasureUnit.SIPrefix.ONE) {
                return result;
            }

            BigDecimal siApplied = BigDecimal.valueOf(Math.pow(10.0, Math.abs(siPrefix.getPower())));

            if (siPrefix.getPower() < 0) {
                result.factorDen = this.factorDen.multiply(siApplied);
                return result;
            }

            result.factorNum = this.factorNum.multiply(siApplied);
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

            return result;
        }

        /**
         * Adds Entity with power or not. For example, `12 ^ 3` or `12`.
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
            } else if ("PI".equals(entity)) {
                this.exponentPi += power;
            } else {
                BigDecimal decimalEntity = new BigDecimal(entity).pow(power, DECIMAL128);
                this.factorNum = this.factorNum.multiply(decimalEntity);
            }
        }
    }

    @Override
    public String toString() {
        return "UnitConverter [conversionRate=" + conversionRate + ", offset=" + offset + "]";
    }
}
