package com.ibm.icu.impl.units;

import com.ibm.icu.impl.Assert;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.regex.Pattern;

import static java.math.MathContext.DECIMAL128;

class Factor {
    public static Factor precessFactor(String factor) {
        Assert.assrt(!factor.isEmpty());

        // Remove all spaces in the factor
        factor.replaceAll("\\s+", "");

        String[] fractions = factor.split("/");
        Assert.assrt(fractions.length == 1 || fractions.length == 2);

        if (fractions.length == 1) {
            return processFactorWithoutDivision(fractions[0]);
        }

        Factor num = processFactorWithoutDivision(fractions[0]);
        Factor den = processFactorWithoutDivision(fractions[1]);
        return num.divide(den);
    }

    /**
     * Returns a single `BigDecimal` that represent the conversion rate after substituting all the constants.
     *
     * @return
     */
    public BigDecimal getConversionRate() {
        Factor resultCollector = new Factor(this);

        resultCollector.substitute(BigDecimal.valueOf(0.3048), this.CONSTANT_FT2M);
        resultCollector.substitute(BigDecimal.valueOf(0.45359237).divide(BigDecimal.valueOf(131002976.0), DECIMAL128), this.CONSTANT_PI);
        resultCollector.substitute(BigDecimal.valueOf(9.80665), this.CONSTANT_GRAVITY);
        resultCollector.substitute(new BigDecimal("6.67408E-11"), this.CONSTANT_G);
        resultCollector.substitute(BigDecimal.valueOf(0.00454609), this.CONSTANT_GAL_IMP2M3);
        resultCollector.substitute(BigDecimal.valueOf(0.45359237), this.CONSTANT_LB2KG);

        return resultCollector.factorNum.divide(resultCollector.factorDen, DECIMAL128);
    }

    private void substitute(BigDecimal value, int power) {
        if (power == 0) return;

        BigDecimal absPoweredValue = value.pow(Math.abs(power), DECIMAL128);
        if (power > 0) {
            this.factorNum = this.factorNum.multiply(absPoweredValue);
        } else {
            this.factorDen = this.factorDen.multiply(absPoweredValue);
        }
    }


    public Factor applySiPrefix(UMeasureSIPrefix siPrefix) {
        Factor result = new Factor(this);
        if (siPrefix == UMeasureSIPrefix.UMEASURE_SI_PREFIX_ONE) {
            return result;
        }

        BigDecimal siApplied = BigDecimal.valueOf(Math.pow(10.0, Math.abs(siPrefix.getSiPrefix())));

        if (siPrefix.getSiPrefix() < 0) {
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

        result.CONSTANT_FT2M = this.CONSTANT_FT2M * power;
        result.CONSTANT_PI = this.CONSTANT_PI * power;
        result.CONSTANT_GRAVITY = this.CONSTANT_GRAVITY * power;
        result.CONSTANT_G = this.CONSTANT_G * power;
        result.CONSTANT_GAL_IMP2M3 = this.CONSTANT_GAL_IMP2M3 * power;
        result.CONSTANT_LB2KG = this.CONSTANT_LB2KG * power;

        return result;
    }

    public Factor divide(Factor other) {
        Factor result = new Factor();
        result.factorNum = this.factorNum.multiply(other.factorDen);
        result.factorDen = this.factorDen.multiply(other.factorNum);

        result.CONSTANT_FT2M = this.CONSTANT_FT2M - other.CONSTANT_FT2M;
        result.CONSTANT_PI = this.CONSTANT_PI - other.CONSTANT_PI;
        result.CONSTANT_GRAVITY = this.CONSTANT_GRAVITY - other.CONSTANT_GRAVITY;
        result.CONSTANT_G = this.CONSTANT_G - other.CONSTANT_G;
        result.CONSTANT_GAL_IMP2M3 = this.CONSTANT_GAL_IMP2M3 - other.CONSTANT_GAL_IMP2M3;
        result.CONSTANT_LB2KG = this.CONSTANT_LB2KG - other.CONSTANT_LB2KG;

        return result;
    }

    public Factor multiply(Factor other) {
        Factor result = new Factor();
        result.factorNum = this.factorNum.multiply(other.factorNum);
        result.factorDen = this.factorDen.multiply(other.factorDen);

        result.CONSTANT_FT2M = this.CONSTANT_FT2M + other.CONSTANT_FT2M;
        result.CONSTANT_PI = this.CONSTANT_PI + other.CONSTANT_PI;
        result.CONSTANT_GRAVITY = this.CONSTANT_GRAVITY + other.CONSTANT_GRAVITY;
        result.CONSTANT_G = this.CONSTANT_G + other.CONSTANT_G;
        result.CONSTANT_GAL_IMP2M3 = this.CONSTANT_GAL_IMP2M3 + other.CONSTANT_GAL_IMP2M3;
        result.CONSTANT_LB2KG = this.CONSTANT_LB2KG + other.CONSTANT_LB2KG;

        return result;
    }

    /**
     * Creates Empty Factor
     */
    public Factor() {
        this.factorNum = BigDecimal.valueOf(1);
        this.factorDen = BigDecimal.valueOf(1);
    }

    /**
     * Copy another factor.
     *
     * @param other
     */
    private Factor(Factor other) {

        this.factorNum = other.factorNum;
        this.factorDen = other.factorDen;

        this.CONSTANT_FT2M = other.CONSTANT_FT2M;
        this.CONSTANT_PI = other.CONSTANT_PI;
        this.CONSTANT_GRAVITY = other.CONSTANT_GRAVITY;
        this.CONSTANT_G = other.CONSTANT_G;
        this.CONSTANT_GAL_IMP2M3 = other.CONSTANT_GAL_IMP2M3;
        this.CONSTANT_LB2KG = other.CONSTANT_LB2KG;
    }


    private BigDecimal factorNum;
    private BigDecimal factorDen;

    /* FACTOR CONSTANTS */
    private int
            CONSTANT_FT2M = 0,    // ft2m stands for foot to meter.
            CONSTANT_PI = 0,      // PI
            CONSTANT_GRAVITY = 0, // Gravity
            CONSTANT_G = 0,
            CONSTANT_GAL_IMP2M3 = 0, // Gallon imp to m3
            CONSTANT_LB2KG = 0;      // Pound to Kilogram


    private static Factor processFactorWithoutDivision(String factorWithoutDivision) {
        Factor result = new Factor();
        for (String poweredEntity :
                factorWithoutDivision.split(Pattern.quote("*"))) {
            result.addPoweredEntity(poweredEntity);
        }

        return result;
    }

    /**
     * Adds Entity with power or not. For example, `12 ^ 3` or `12`.
     *
     * @param poweredEntity
     */
    private void addPoweredEntity(String poweredEntity) {
        String[] entities = poweredEntity.split(Pattern.quote("^"));
        Assert.assrt(entities.length == 1 || entities.length == 2);

        int power = entities.length == 2 ? Integer.parseInt(entities[1]) : 1;
        this.addEntity(entities[0], power);
    }

    private void addEntity(String entity, int power) {
        if ("ft_to_m".equals(entity)) {
            this.CONSTANT_FT2M += power;
        } else if ("ft2_to_m2".equals(entity)) {
            this.CONSTANT_FT2M += 2 * power;
        } else if ("ft3_to_m3".equals(entity)) {
            this.CONSTANT_FT2M += 3 * power;
        } else if ("in3_to_m3".equals(entity)) {
            this.CONSTANT_FT2M += 3 * power;
            this.factorDen = this.factorDen.multiply(BigDecimal.valueOf(Math.pow(12, 3)));
        } else if ("gal_to_m3".equals(entity)) {
            this.factorNum = this.factorNum.multiply(BigDecimal.valueOf(231));
            this.CONSTANT_FT2M += 3 * power;
            this.factorDen = this.factorDen.multiply(BigDecimal.valueOf(12 * 12 * 12));
        } else if ("gal_imp_to_m3".equals(entity)) {
            this.CONSTANT_GAL_IMP2M3 += power;
        } else if ("G".equals(entity)) {
            this.CONSTANT_G += power;
        } else if ("gravity".equals(entity)) {
            this.CONSTANT_GRAVITY += power;
        } else if ("lb_to_kg".equals(entity)) {
            this.CONSTANT_LB2KG += power;
        } else if ("PI".equals(entity)) {
            this.CONSTANT_PI += power;
        } else {
            BigDecimal decimalEntity = new BigDecimal(entity).pow(power, DECIMAL128);
            this.factorNum = this.factorNum.multiply(decimalEntity);
        }
    }
}
