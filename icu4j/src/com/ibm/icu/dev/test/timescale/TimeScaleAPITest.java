/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.test.timescale;

import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.util.UniversalTimeScale;
import com.ibm.icu.dev.test.TestFmwk;

/**
 * @author Owner
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TimeScaleAPITest extends TestFmwk
{

    /**
     * 
     */
    public TimeScaleAPITest()
    {
    }
    
    public void TestBigDecimalFromBigDecimal()
    {
        BigDecimal bigZero = new BigDecimal(0);
        BigDecimal result;
        
        try {
            result = UniversalTimeScale.bigDecimalFrom(bigZero, -1);
            errln("bigDecimalFrom(bigZero, -1) did not throw IllegalArgumentException.");
        } catch (IllegalArgumentException iae) {
        }
        
        for (int scale = 0; scale < UniversalTimeScale.MAX_SCALE; scale += 1) {
            try {
                result = UniversalTimeScale.bigDecimalFrom(bigZero, scale);
            } catch (IllegalArgumentException iae) {
                errln("bigDecimalFrom(bigZero, " + scale + ") threw IllegalArgumentException.");
            }
       }
        
        try {
            result = UniversalTimeScale.bigDecimalFrom(bigZero, UniversalTimeScale.MAX_SCALE);
            errln("from(bigZero, MAX_SCALE) did not throw IllegalArgumetException.");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void TestBigDecimalFromDouble()
    {
        BigDecimal result;
        
        try {
            result = UniversalTimeScale.bigDecimalFrom(0.0, -1);
            errln("bigDecimalFrom(0.0, -1) did not throw IllegalArgumentException.");
        } catch (IllegalArgumentException iae) {
        }
        
        for (int scale = 0; scale < UniversalTimeScale.MAX_SCALE; scale += 1) {
            try {
                result = UniversalTimeScale.bigDecimalFrom(0.0, scale);
            } catch (IllegalArgumentException iae) {
                errln("bigDecimalFrom(0.0, " + scale + ") threw IllegalArgumentException.");
            }
       }
        
        try {
            result = UniversalTimeScale.bigDecimalFrom(0.0, UniversalTimeScale.MAX_SCALE);
            errln("from(0.0, MAX_SCALE) did not throw IllegalArgumetException.");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void TestBigDecimalFromLong()
    {
        BigDecimal result;
        
        try {
            result = UniversalTimeScale.bigDecimalFrom(0L, -1);
            errln("bigDecimalFrom(0L, -1) did not throw IllegalArgumentException.");
        } catch (IllegalArgumentException iae) {
        }
        
        for (int scale = 0; scale < UniversalTimeScale.MAX_SCALE; scale += 1) {
            try {
                result = UniversalTimeScale.bigDecimalFrom(0L, scale);
            } catch (IllegalArgumentException iae) {
                errln("bigDecimalFrom(0L, " + scale + ") threw IllegalArgumentException.");
            }
       }
        
        try {
            result = UniversalTimeScale.bigDecimalFrom(0L, UniversalTimeScale.MAX_SCALE);
            errln("from(0L, MAX_SCALE) did not throw IllegalArgumetException.");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void TestFromLong()
    {
        long result;
        
        try {
            result = UniversalTimeScale.from(0L, -1);
            errln("from(0L, -1) did not throw IllegalArgumentException.");
        } catch (IllegalArgumentException iae) {
        }
        
        for (int scale = 0; scale < UniversalTimeScale.MAX_SCALE; scale += 1) {
            long fromMin = UniversalTimeScale.getTimeScaleValue(scale, UniversalTimeScale.FROM_MIN_VALUE);
            long fromMax = UniversalTimeScale.getTimeScaleValue(scale, UniversalTimeScale.FROM_MAX_VALUE);
            
            try {
                result = UniversalTimeScale.from(0L, scale);
            } catch (IllegalArgumentException iae) {
                errln("from(0L, " + scale + ") threw IllegalArgumentException.");
            }
            
            try {
                result = UniversalTimeScale.from(fromMin, scale);
            } catch (IllegalArgumentException iae) {
                errln("from(fromMin, " + scale + ") threw IllegalArgumentException.");
            }
             
            if (fromMin > Long.MIN_VALUE) {
                try {
                    result = UniversalTimeScale.from(fromMin - 1, scale);
                    errln("from(fromMin - 1, " + scale + ") did not throw IllegalArgumentException.");
                } catch (IllegalArgumentException iae) {
                }
            }
             
            try {
                result = UniversalTimeScale.from(fromMax, scale);
            } catch (IllegalArgumentException iae) {
                errln("from(fromMax, " + scale + ") threw IllegalArgumentException.");
            }
              
            if (fromMax < Long.MAX_VALUE) {
                try {
                    result = UniversalTimeScale.from(fromMax + 1, scale);
                    errln("from(fromMax + 1, " + scale + ") did not throw IllegalArgumentException.");
               } catch (IllegalArgumentException iae) {
               }
            }
       }
        
        try {
            result = UniversalTimeScale.from(0L, UniversalTimeScale.MAX_SCALE);
            errln("from(0L, MAX_SCALE) did not throw IllegalArgumetException.");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void TestGetTimeScale()
    {
        long value;
        
        try {
            value = UniversalTimeScale.getTimeScaleValue(-1, 0);
            errln("getTimeScaleValue(-1, 0) did not throw IllegalArgumentException.");
        } catch (IllegalArgumentException iae) {
        }
        
        try {
            value = UniversalTimeScale.getTimeScaleValue(0, -1);
            errln("getTimeScaleValue(0, -1) did not throw IllegalArgumentException.");
        } catch (IllegalArgumentException iae) {
        }
        
        for (int scale = 0; scale < UniversalTimeScale.MAX_SCALE; scale += 1) {
            try {
                value = UniversalTimeScale.getTimeScaleValue(scale, 0);
            } catch (IllegalArgumentException iae) {
                errln("getTimeScaleValue(" + scale + ", 0) threw IllegalArgumentException.");
            }
        }
        
        try {
            value = UniversalTimeScale.getTimeScaleValue(UniversalTimeScale.MAX_SCALE, 0);
            errln("getTimeScaleValue(MAX_SCALE, 0) did not throw IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
        }
        
        try {
            value = UniversalTimeScale.getTimeScaleValue(0, UniversalTimeScale.MAX_SCALE_VALUE);
            errln("getTimeScaleValue(0, MAX_SCALE_VALUE) did not throw IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void TestToBigDecimalFromBigDecimal()
    {
        BigDecimal bigZero = new BigDecimal(0);
        BigDecimal result;
        
        try {
            result = UniversalTimeScale.toBigDecimal(bigZero, -1);
            errln("toBigDecimal(bigZero, -1) did not throw IllegalArgumentException.");
        } catch (IllegalArgumentException iae) {
        }
        
        for (int scale = 0; scale < UniversalTimeScale.MAX_SCALE; scale += 1) {
            try {
                result = UniversalTimeScale.toBigDecimal(bigZero, scale);
            } catch (IllegalArgumentException iae) {
                errln("toBigDecimal(bigZero, " + scale + ") threw IllegalArgumentException.");
            }
        }
        
        try {
            result = UniversalTimeScale.toBigDecimal(bigZero, UniversalTimeScale.MAX_SCALE);
            errln("toBigDecimal(bigZero, MAX_SCALE) did not throw IllegalArgumetException.");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void TestToBigDecimalFromLong()
    {
        BigDecimal result;
        
        try {
            result = UniversalTimeScale.toBigDecimal(0L, -1);
            errln("toBigDecimal(0L, -1) did not throw IllegalArgumentException.");
        } catch (IllegalArgumentException iae) {
        }
        
        for (int scale = 0; scale < UniversalTimeScale.MAX_SCALE; scale += 1) {
            try {
                result = UniversalTimeScale.toBigDecimal(0L, scale);
            } catch (IllegalArgumentException iae) {
                errln("toBigDecimal(0L, " + scale + ") threw IllegalArgumentException.");
            }
        }
        
        try {
            result = UniversalTimeScale.toBigDecimal(0L, UniversalTimeScale.MAX_SCALE);
            errln("toBigDecimal(0L, MAX_SCALE) did not throw IllegalArgumetException.");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void TestToLong()
    {
        long result;
        
        try {
            result = UniversalTimeScale.toLong(0L, -1);
            errln("toLong(0L, -1) did not throw IllegalArgumentException.");
        } catch (IllegalArgumentException iae) {
        }
        
        for (int scale = 0; scale < UniversalTimeScale.MAX_SCALE; scale += 1) {
            long toMin = UniversalTimeScale.getTimeScaleValue(scale, UniversalTimeScale.TO_MIN_VALUE);
            long toMax = UniversalTimeScale.getTimeScaleValue(scale, UniversalTimeScale.TO_MAX_VALUE);
            
            try {
                result = UniversalTimeScale.toLong(0L, scale);
            } catch (IllegalArgumentException iae) {
                errln("toLong(0L, " + scale + ") threw IllegalArgumentException.");
            }
            
            try {
                result = UniversalTimeScale.toLong(toMin, scale);
            } catch (IllegalArgumentException iae) {
                errln("toLong(toMin, " + scale + ") threw IllegalArgumentException.");
            }
             
            if (toMin > Long.MIN_VALUE) {
                try {
                    result = UniversalTimeScale.toLong(toMin - 1, scale);
                    errln("toLong(toMin - 1, " + scale + ") did not throw IllegalArgumentException.");
                } catch (IllegalArgumentException iae) {
                }
            }
             
            try {
                result = UniversalTimeScale.toLong(toMax, scale);
            } catch (IllegalArgumentException iae) {
                errln("toLong(toMax, " + scale + ") threw IllegalArgumentException.");
            }
              
            if (toMax < Long.MAX_VALUE) {
                try {
                    result = UniversalTimeScale.toLong(toMax + 1, scale);
                    errln("toLong(toMax + 1, " + scale + ") did not throw IllegalArgumentException.");
               } catch (IllegalArgumentException iae) {
               }
            }
       }
        
        try {
            result = UniversalTimeScale.toLong(0L, UniversalTimeScale.MAX_SCALE);
            errln("toLong(0L, MAX_SCALE) did not throw IllegalArgumetException.");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public static void main(String[] args)
    {
        new TimeScaleAPITest().run(args);
    }
}
