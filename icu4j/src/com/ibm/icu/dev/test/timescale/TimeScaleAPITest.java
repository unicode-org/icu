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
    
    public void testBigDecimalFromBigDecimal()
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
    
    public void testBigDecimalFromDouble()
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
    
    public void testBigDecimalFromLong()
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
    
    public void testFromDouble()
    {
        long result;
        
        try {
            result = UniversalTimeScale.from(0.0, -1);
            errln("from(0.0, -1) did not throw IllegalArgumentException.");
        } catch (IllegalArgumentException iae) {
        }
        
        for (int scale = 0; scale < UniversalTimeScale.MAX_SCALE; scale += 1) {
            UniversalTimeScale.TimeScaleData data = UniversalTimeScale.getTimeScaleData(scale);
            
            try {
                result = UniversalTimeScale.from(0.0, scale);
            } catch (IllegalArgumentException iae) {
                errln("from(0.0, " + scale + ") threw IllegalArgumentException.");
            }
            
            try {
                result = UniversalTimeScale.from(data.fromMin, scale);
            } catch (IllegalArgumentException iae) {
                errln("from(fromMin, " + scale + ") threw IllegalArgumentException.");
            }
             
            if (data.fromMin > Long.MIN_VALUE) {
                try {
                    result = UniversalTimeScale.from(data.fromMin - 1, scale);
                    errln("from(fromMin - 1, " + scale + ") did not throw IllegalArgumentException.");
                } catch (IllegalArgumentException iae) {
                }
            }
             
            try {
                result = UniversalTimeScale.from(data.fromMax, scale);
            } catch (IllegalArgumentException iae) {
                errln("from(fromMax, " + scale + ") threw IllegalArgumentException.");
            }
              
            if (data.fromMax < Long.MAX_VALUE) {
                try {
                    result = UniversalTimeScale.from(data.fromMax + 1, scale);
                    errln("from(fromMax + 1, " + scale + ") did not throw IllegalArgumentException.");
               } catch (IllegalArgumentException iae) {
               }
            }
       }
        
        try {
            result = UniversalTimeScale.from(0.0, UniversalTimeScale.MAX_SCALE);
            errln("from(0.0, MAX_SCALE) did not throw IllegalArgumetException.");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void testFromLong()
    {
        long result;
        
        try {
            result = UniversalTimeScale.from(0L, -1);
            errln("from(0L, -1) did not throw IllegalArgumentException.");
        } catch (IllegalArgumentException iae) {
        }
        
        for (int scale = 0; scale < UniversalTimeScale.MAX_SCALE; scale += 1) {
            UniversalTimeScale.TimeScaleData data = UniversalTimeScale.getTimeScaleData(scale);
            
            try {
                result = UniversalTimeScale.from(0L, scale);
            } catch (IllegalArgumentException iae) {
                errln("from(0L, " + scale + ") threw IllegalArgumentException.");
            }
            
            try {
                result = UniversalTimeScale.from(data.fromMin, scale);
            } catch (IllegalArgumentException iae) {
                errln("from(fromMin, " + scale + ") threw IllegalArgumentException.");
            }
             
            if (data.fromMin > Long.MIN_VALUE) {
                try {
                    result = UniversalTimeScale.from(data.fromMin - 1, scale);
                    errln("from(fromMin - 1, " + scale + ") did not throw IllegalArgumentException.");
                } catch (IllegalArgumentException iae) {
                }
            }
             
            try {
                result = UniversalTimeScale.from(data.fromMax, scale);
            } catch (IllegalArgumentException iae) {
                errln("from(fromMax, " + scale + ") threw IllegalArgumentException.");
            }
              
            if (data.fromMax < Long.MAX_VALUE) {
                try {
                    result = UniversalTimeScale.from(data.fromMax + 1, scale);
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
    
    public void testGetTimeScale()
    {
        UniversalTimeScale.TimeScaleData data;
        
        try {
            data = UniversalTimeScale.getTimeScaleData(-1);
            errln("getTimeScaleData(-1) did not throw IllegalArgumentException.");
        } catch (IllegalArgumentException iae) {
        }
        
        for (int scale = 0; scale < UniversalTimeScale.MAX_SCALE; scale += 1) {
            try {
                data = UniversalTimeScale.getTimeScaleData(scale);
            } catch (IllegalArgumentException iae) {
                errln("getTimeScaleData(" + scale + ") threw IllegalArgumentException.");
            }
        }
        
        try {
            data = UniversalTimeScale.getTimeScaleData(UniversalTimeScale.MAX_SCALE);
            errln("getTimeScaleData(" + UniversalTimeScale.MAX_SCALE + ") did not throw IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void testToBigDecimalFromBigDecimal()
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
    
    public void testToBigDecimalFromLong()
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
    
    public void testToDouble()
    {
        double result;
        
        try {
            result = UniversalTimeScale.toDouble(0L, -1);
            errln("toDouble(0L, -1) did not throw IllegalArgumentException.");
        } catch (IllegalArgumentException iae) {
        }
        
        for (int scale = 0; scale < UniversalTimeScale.MAX_SCALE; scale += 1) {
            UniversalTimeScale.TimeScaleData data = UniversalTimeScale.getTimeScaleData(scale);
            
            try {
                result = UniversalTimeScale.toDouble(0L, scale);
            } catch (IllegalArgumentException iae) {
                errln("toDouble(0L, " + scale + ") threw IllegalArgumentException.");
            }
            
            try {
                result = UniversalTimeScale.toDouble(data.toMin, scale);
            } catch (IllegalArgumentException iae) {
                errln("toDouble(toMin, " + scale + ") threw IllegalArgumentException.");
            }
             
            if (data.toMin > Long.MIN_VALUE) {
                try {
                    result = UniversalTimeScale.toDouble(data.toMin - 1, scale);
                    errln("toDouble(toMin - 1, " + scale + ") did not throw IllegalArgumentException.");
                } catch (IllegalArgumentException iae) {
                }
            }
             
            try {
                result = UniversalTimeScale.toDouble(data.toMax, scale);
            } catch (IllegalArgumentException iae) {
                errln("toDouble(toMax, " + scale + ") threw IllegalArgumentException.");
            }
              
            if (data.toMax < Long.MAX_VALUE) {
                try {
                    result = UniversalTimeScale.toDouble(data.toMax + 1, scale);
                    errln("toDouble(toMax + 1, " + scale + ") did not throw IllegalArgumentException.");
               } catch (IllegalArgumentException iae) {
               }
            }
       }
        
        try {
            result = UniversalTimeScale.toDouble(0L, UniversalTimeScale.MAX_SCALE);
            errln("toDouble(0L, MAX_SCALE) did not throw IllegalArgumetException.");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void testToLong()
    {
        long result;
        
        try {
            result = UniversalTimeScale.toLong(0L, -1);
            errln("toLong(0L, -1) did not throw IllegalArgumentException.");
        } catch (IllegalArgumentException iae) {
        }
        
        for (int scale = 0; scale < UniversalTimeScale.MAX_SCALE; scale += 1) {
            UniversalTimeScale.TimeScaleData data = UniversalTimeScale.getTimeScaleData(scale);
            
            try {
                result = UniversalTimeScale.toLong(0L, scale);
            } catch (IllegalArgumentException iae) {
                errln("toLong(0L, " + scale + ") threw IllegalArgumentException.");
            }
            
            try {
                result = UniversalTimeScale.toLong(data.toMin, scale);
            } catch (IllegalArgumentException iae) {
                errln("toLong(toMin, " + scale + ") threw IllegalArgumentException.");
            }
             
            if (data.toMin > Long.MIN_VALUE) {
                try {
                    result = UniversalTimeScale.toLong(data.toMin - 1, scale);
                    errln("toLong(toMin - 1, " + scale + ") did not throw IllegalArgumentException.");
                } catch (IllegalArgumentException iae) {
                }
            }
             
            try {
                result = UniversalTimeScale.toLong(data.toMax, scale);
            } catch (IllegalArgumentException iae) {
                errln("toLong(toMax, " + scale + ") threw IllegalArgumentException.");
            }
              
            if (data.toMax < Long.MAX_VALUE) {
                try {
                    result = UniversalTimeScale.toLong(data.toMax + 1, scale);
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
