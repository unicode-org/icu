 /*
  *******************************************************************************
  * Copyright (C) 2005-2007, International Business Machines Corporation and         *
  * others. All Rights Reserved.                                                *
  *******************************************************************************
  */
package com.ibm.icu.impl;

import java.util.Arrays;
import java.util.Date;

import com.ibm.icu.util.BasicTimeZone;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.DateTimeRule;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.InitialTimeZoneRule;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.TimeArrayTimeZoneRule;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZoneRule;
import com.ibm.icu.util.TimeZoneTransition;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * A time zone based on the Olson database.  Olson time zones change
 * behavior over time.  The raw offset, rules, presence or absence of
 * daylight savings time, and even the daylight savings amount can all
 * vary.
 *
 * This class uses a resource bundle named "zoneinfo".  Zoneinfo is a
 * table containing different kinds of resources.  In several places,
 * zones are referred to using integers.  A zone's integer is a number
 * from 0..n-1, where n is the number of zones, with the zones sorted
 * in lexicographic order.
 *
 * 1. Zones.  These have keys corresponding to the Olson IDs, e.g.,
 * "Asia/Shanghai".  Each resource describes the behavior of the given
 * zone.  Zones come in several formats, which are differentiated
 * based on length.
 *
 *  a. Alias (int, length 1).  An alias zone is an int resource.  The
 *  integer is the zone number of the target zone.  The key of this
 *  resource is an alternate name for the target zone.  Aliases
 *  represent Olson links and ICU compatibility IDs.
 *
 *  b. Simple zone (array, length 3).  The three subelements are:
 *
 *   i. An intvector of transitions.  These are given in epoch
 *   seconds.  This may be an empty invector (length 0).  If the
 *   transtions list is empty, then the zone's behavior is fixed and
 *   given by the offset list, which will contain exactly one pair.
 *   Otherwise each transtion indicates a time after which (inclusive)
 *   the associated offset pair is in effect.
 *
 *   ii. An intvector of offsets.  These are in pairs of raw offset /
 *   DST offset, in units of seconds.  There will be at least one pair
 *   (length >= 2 && length % 2 == 0).
 *
 *   iii. A binary resource.  This is of the same length as the
 *   transitions vector, so length may be zero.  Each unsigned byte
 *   corresponds to one transition, and has a value of 0..n-1, where n
 *   is the number of pairs in the offset vector.  This forms a map
 *   between transitions and offset pairs.
 *
 *  c. Simple zone with aliases (array, length 4).  This is like a
 *  simple zone, but also contains a fourth element:
 *
 *   iv. An intvector of aliases.  This list includes this zone
 *   itself, and lists all aliases of this zone.
 *
 *  d. Complex zone (array, length 5).  This is like a simple zone,
 *  but contains two more elements:
 *
 *   iv. A string, giving the name of a rule.  This is the "final
 *   rule", which governs the zone's behavior beginning in the "final
 *   year".  The rule ID is given without leading underscore, e.g.,
 *   "EU".
 *
 *   v. An intvector of length 2, containing the raw offset for the
 *   final rule (in seconds), and the final year.  The final rule
 *   takes effect for years >= the final year.
 *
 *  e. Complex zone with aliases (array, length 6).  This is like a
 *  complex zone, but also contains a sixth element:
 * 
 *   vi. An intvector of aliases.  This list includes this zone
 *   itself, and lists all aliases of this zone.
 *
 * 2. Rules.  These have keys corresponding to the Olson rule IDs,
 * with an underscore prepended, e.g., "_EU".  Each resource describes
 * the behavior of the given rule using an intvector, containing the
 * onset list, the cessation list, and the DST savings.  The onset and
 * cessation lists consist of the month, dowim, dow, time, and time
 * mode.  The end result is that the 11 integers describing the rule
 * can be passed directly into the SimpleTimeZone 13-argument
 * constructor (the other two arguments will be the raw offset, taken
 * from the complex zone element 5, and the ID string, which is not
 * used), with the times and the DST savings multiplied by 1000 to
 * scale from seconds to milliseconds.
 *
 * 3. Countries.  These have keys corresponding to the 2-letter ISO
 * country codes, with a percent sign prepended, e.g., "%US".  Each
 * resource is an intvector listing the zones associated with the
 * given country.  The special entry "%" corresponds to "no country",
 * that is, the category of zones assigned to no country in the Olson
 * DB.
 *
 * 4. Metadata.  Metadata is stored under the key "_".  It is an
 * intvector of length three containing the number of zones resources,
 * rule resources, and country resources.  For the purposes of this
 * count, the metadata entry itself is considered a rule resource,
 * since its key begins with an underscore.
 */
public class OlsonTimeZone extends BasicTimeZone {

    // Generated by serialver from JDK 1.4.1_01
    static final long serialVersionUID = -6281977362477515376L;

    private static final boolean ASSERT = false;

    /* (non-Javadoc)
     * @see com.ibm.icu.util.TimeZone#getOffset(int, int, int, int, int, int)
     */
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        if (month < Calendar.JANUARY || month > Calendar.DECEMBER) {
            throw new IllegalArgumentException("Month is not in the legal range: " +month);
        } else {
            return getOffset(era, year, month, day, dayOfWeek, milliseconds, Grego.monthLength(year, month));
        }
    }

    /**
     * TimeZone API.
     */
    public int getOffset(int era, int year, int month,int dom, int dow, int millis, int monthLength){

        if ((era != GregorianCalendar.AD && era != GregorianCalendar.BC)
            || month < Calendar.JANUARY
            || month > Calendar.DECEMBER
            || dom < 1
            || dom > monthLength
            || dow < Calendar.SUNDAY
            || dow > Calendar.SATURDAY
            || millis < 0
            || millis >= Grego.MILLIS_PER_DAY
            || monthLength < 28
            || monthLength > 31) {
            throw new IllegalArgumentException();
        }

        if (era == GregorianCalendar.BC) {
            year = -year;
        }

        if (year > finalYear) { // [sic] >, not >=; see above
            if (ASSERT) Assert.assrt("(finalZone != null)", finalZone != null);
            return finalZone.getOffset(era, year, month, dom, dow, millis);
        }

        // Compute local epoch millis from input fields
        long time = Grego.fieldsToDay(year, month, dom) * Grego.MILLIS_PER_DAY + millis;

        int[] offsets = new int[2];
        getHistoricalOffset(time, true, LOCAL_DST, LOCAL_STD, offsets);
        return offsets[0] + offsets[1];
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.TimeZone#setRawOffset(int)
     */
    public void setRawOffset(int offsetMillis) {
        if (getRawOffset() == offsetMillis) {
            return;
        }
        GregorianCalendar cal = new GregorianCalendar(ULocale.ROOT);
        cal.setTimeZone(this);
        int tmpFinalYear = cal.get(Calendar.YEAR) - 1;

        // Apply the raw offset starting current year and beyond
        if (finalYear > tmpFinalYear) {
            finalYear = tmpFinalYear;
            finalMillis = Grego.fieldsToDay(tmpFinalYear, 0, 1) * Grego.MILLIS_PER_DAY;
        }
        if (finalZone == null) {
            // Create SimpleTimeZone instance to store the offset
            finalZone = new SimpleTimeZone(offsetMillis, getID());
        } else {
            finalZone.setRawOffset(offsetMillis);
            finalZone.setStartYear(finalYear);
        }

        transitionRulesInitialized = false;
    }

    public Object clone() {
        OlsonTimeZone other = (OlsonTimeZone) super.clone();
        if(finalZone!=null){
            finalZone.setID(getID());
            other.finalZone = (SimpleTimeZone)finalZone.clone();
        }
        other.transitionTimes = (int[])transitionTimes.clone();
        other.typeData = (byte[])typeData.clone();
        other.typeOffsets = (int[])typeOffsets.clone();
        return other;
    }

    /**
     * TimeZone API.
     */
    public void getOffset(long date, boolean local, int[] offsets)  {
        // The check against finalMillis will suffice most of the time, except
        // for the case in which finalMillis == DBL_MAX, date == DBL_MAX,
        // and finalZone == 0.  For this case we add "&& finalZone != 0".
        if (date >= finalMillis && finalZone != null) {
            finalZone.getOffset(date, local, offsets);
        } else {
            getHistoricalOffset(date, local,
                    LOCAL_FORMER, LOCAL_LATTER, offsets);
        }
    }

    /**
     * {@inheritDoc}
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public void getOffsetFromLocal(long date,
            int nonExistingTimeOpt, int duplicatedTimeOpt, int[] offsets) {
        if (date >= finalMillis && finalZone != null) {
            finalZone.getOffsetFromLocal(date, nonExistingTimeOpt, duplicatedTimeOpt, offsets);
        } else {
            getHistoricalOffset(date, true, nonExistingTimeOpt, duplicatedTimeOpt, offsets);
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.TimeZone#getRawOffset()
     */
    public int getRawOffset() {
        int[] ret = new int[2];
        getOffset( System.currentTimeMillis(), false, ret);
        return ret[0];
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.TimeZone#useDaylightTime()
     */
    public boolean useDaylightTime() {
        // If DST was observed in 1942 (for example) but has never been
        // observed from 1943 to the present, most clients will expect
        // this method to return FALSE.  This method determines whether
        // DST is in use in the current year (at any point in the year)
        // and returns TRUE if so.
        int[] fields = Grego.timeToFields(System.currentTimeMillis(), null);
        int year = fields[0];

        if (year > finalYear) { // [sic] >, not >=; see above
            return (finalZone != null && finalZone.useDaylightTime());
        }

        // Find start of this year, and start of next year
        long start = Grego.fieldsToDay(year, 0, 1) * SECONDS_PER_DAY;    
        long limit = Grego.fieldsToDay(year+1, 0, 1) * SECONDS_PER_DAY;    

        // Return TRUE if DST is observed at any time during the current
        // year.
        for (int i = 0; i < transitionCount; ++i) {
            if (transitionTimes[i] >= limit) {
                break;
            }
            if (transitionTimes[i] >= start &&
                dstOffset(typeData[i]) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * TimeZone API
     * Returns the amount of time to be added to local standard time
     * to get local wall clock time.
     */
    public int getDSTSavings() {
        if(finalZone!=null){
            return finalZone.getDSTSavings();
        }
        return super.getDSTSavings();
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.TimeZone#inDaylightTime(java.util.Date)
     */
    public boolean inDaylightTime(Date date) {
        int[] temp = new int[2];
        getOffset(date.getTime(), false, temp);
        return temp[1] != 0;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.TimeZone#hasSameRules(com.ibm.icu.util.TimeZone)
     */
    public boolean hasSameRules(TimeZone other) {
        // The super class implementation only check raw offset and
        // use of daylight saving time.
        if (!super.hasSameRules(other)) {
            return false;
        }

        if (!(other instanceof OlsonTimeZone)) {
            // We cannot reasonably compare rules in different types
            return false;
        }

        // Check final zone
        OlsonTimeZone o = (OlsonTimeZone)other;
        if (finalZone == null) {
            if (o.finalZone != null && finalYear != Integer.MAX_VALUE) {
                return false;
            }
        } else {
            if (o.finalZone == null
                    || finalYear != o.finalYear
                    || !(finalZone.hasSameRules(o.finalZone))) {
                return false;
            }
        }
        // Check transitions
        // Note: The code below actually fails to compare two equivalent rules in
        // different representation properly.
        if (transitionCount != o.transitionCount ||
                !Arrays.equals(transitionTimes, o.transitionTimes) ||
                typeCount != o.typeCount ||
                !Arrays.equals(typeData, o.typeData) ||
                !Arrays.equals(typeOffsets, o.typeOffsets)){
            return false;
        }
        return true;
    }

    /**
     * Construct a GMT+0 zone with no transitions.  This is done when a
     * constructor fails so the resultant object is well-behaved.
     */
    private void constructEmpty(){
        transitionCount = 0;
        typeCount = 1;
        transitionTimes = typeOffsets = new int[]{0,0};
        typeData =  new byte[2];
        
    }

    /**
     * Construct from a resource bundle
     * @param top the top-level zoneinfo resource bundle.  This is used
     * to lookup the rule that `res' may refer to, if there is one.
     * @param res the resource bundle of the zone to be constructed
     */
    public OlsonTimeZone(UResourceBundle top, UResourceBundle res){
        construct(top, res);
    }

    private void construct(UResourceBundle top, UResourceBundle res){
        
        if ((top == null || res == null)) {
            throw new IllegalArgumentException();
        }
        if(DEBUG) System.out.println("OlsonTimeZone(" + res.getKey() +")");


        // TODO -- clean up -- Doesn't work if res points to an alias
        //        // TODO remove nonconst casts below when ures_* API is fixed
        //        setID(ures_getKey((UResourceBundle*) res)); // cast away const

        // Size 1 is an alias TO another zone (int)
        // HOWEVER, the caller should dereference this and never pass it in to us
        // Size 3 is a purely historical zone (no final rules)
        // Size 4 is like size 3, but with an alias list at the end
        // Size 5 is a hybrid zone, with historical and final elements
        // Size 6 is like size 5, but with an alias list at the end
        int size = res.getSize();
        if (size < 3 || size > 6) {
           // ec = U_INVALID_FORMAT_ERROR;
            throw new IllegalArgumentException("Invalid Format");
        }

        // Transitions list may be empty
        UResourceBundle r = res.get(0);
        transitionTimes = r.getIntVector();
        
        if ((transitionTimes.length<0 || transitionTimes.length>0x7FFF) ) {
            throw new IllegalArgumentException("Invalid Format");
        }
        transitionCount = (int) transitionTimes.length;
        
        // Type offsets list must be of even size, with size >= 2
        r = res.get( 1);
        typeOffsets = r.getIntVector();
        if ((typeOffsets.length<2 || typeOffsets.length>0x7FFE || ((typeOffsets.length&1)!=0))) {
            throw new IllegalArgumentException("Invalid Format");
        }
        typeCount = (int) typeOffsets.length >> 1;

        // Type data must be of the same size as the transitions list        
        r = res.get(2);
        typeData = r.getBinary().array();
        if (typeData.length != transitionCount) {
            throw new IllegalArgumentException("Invalid Format");
        }

        // Process final rule and data, if any
        if (size >= 5) {
            String ruleid = res.getString(3);
            r = res.get(4);
            int[] data = r.getIntVector();

            if (data != null && data.length == 2) {
                int rawOffset = data[0] * Grego.MILLIS_PER_SECOND;
                // Subtract one from the actual final year; we
                // actually store final year - 1, and compare
                // using > rather than >=.  This allows us to use
                // INT32_MAX as an exclusive upper limit for all
                // years, including INT32_MAX.
                if (ASSERT) Assert.assrt("data[1] > Integer.MIN_VALUE", data[1] > Integer.MIN_VALUE);
                finalYear = data[1] - 1;
                // Also compute the millis for Jan 1, 0:00 GMT of the
                // finalYear.  This reduces runtime computations.
                finalMillis = Grego.fieldsToDay(data[1], 0, 1) * Grego.MILLIS_PER_DAY;
                //U_DEBUG_TZ_MSG(("zone%s|%s: {%d,%d}, finalYear%d, finalMillis%.1lf\n",
                  //              zKey,rKey, data[0], data[1], finalYear, finalMillis));
                r = loadRule(top, ruleid);

                // 3, 1, -1, 7200, 0, 9, -31, -1, 7200, 0, 3600
                data = r.getIntVector();
                if ( data.length == 11) {
                    //U_DEBUG_TZ_MSG(("zone%s, rule%s: {%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d}", zKey, ures_getKey(r), 
                      //            data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8], data[9], data[10]));
                    finalZone = new SimpleTimeZone(rawOffset, "",
                        data[0], data[1], data[2],
                        data[3] * Grego.MILLIS_PER_SECOND,
                        data[4],
                        data[5], data[6], data[7],
                        data[8] * Grego.MILLIS_PER_SECOND,
                        data[9],
                        data[10] * Grego.MILLIS_PER_SECOND);
                } else {
                    throw new IllegalArgumentException("Invalid Format");
                }                
            } else {
                throw new IllegalArgumentException("Invalid Format");
            }
        }       
    }

    public OlsonTimeZone(){
       /*
        * 
        finalYear = Integer.MAX_VALUE;
        finalMillis = Double.MAX_VALUE;
        finalZone = null;
        */
        constructEmpty();
    }

    public OlsonTimeZone(String id){
        UResourceBundle top = (UResourceBundle) UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "zoneinfo", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        UResourceBundle res = ZoneMeta.openOlsonResource(id);
        construct(top, res);
        if(finalZone!=null){
            finalZone.setID(id);
        }
        super.setID(id);
    }

    public void setID(String id){
        if(finalZone!= null){
            finalZone.setID(id);
        }
        super.setID(id);
        transitionRulesInitialized = false;
    }

    private static final int UNSIGNED_BYTE_MASK =0xFF;

    private int getInt(byte val){
        return (int)(UNSIGNED_BYTE_MASK & val); 
    }

    private void getHistoricalOffset(long date, boolean local,
            int NonExistingTimeOpt, int DuplicatedTimeOpt, int[] offsets) {
        if (transitionCount != 0) {
            long sec = myFloorDivide(date, Grego.MILLIS_PER_SECOND);
            // Linear search from the end is the fastest approach, since
            // most lookups will happen at/near the end.
            int i = 0;
            for (i = transitionCount - 1; i > 0; --i) {
                int transition = transitionTimes[i];
                if (local) {
                    int offsetBefore = zoneOffset(getInt(typeData[i-1]));
                    boolean dstBefore = dstOffset(getInt(typeData[i-1])) != 0;

                    int offsetAfter = zoneOffset(getInt(typeData[i]));
                    boolean dstAfter = dstOffset(getInt(typeData[i])) != 0;

                    boolean dstToStd = dstBefore && !dstAfter;
                    boolean stdToDst = !dstBefore && dstAfter;
                    
                    if (offsetAfter - offsetBefore >= 0) {
                        // Positive transition, which makes a non-existing local time range
                        if (((NonExistingTimeOpt & STD_DST_MASK) == LOCAL_STD && dstToStd)
                                || ((NonExistingTimeOpt & STD_DST_MASK) == LOCAL_DST && stdToDst)) {
                            transition += offsetBefore;
                        } else if (((NonExistingTimeOpt & STD_DST_MASK) == LOCAL_STD && stdToDst)
                                || ((NonExistingTimeOpt & STD_DST_MASK) == LOCAL_DST && dstToStd)) {
                            transition += offsetAfter;
                        } else if ((NonExistingTimeOpt & FORMER_LATTER_MASK) == LOCAL_LATTER) {
                            transition += offsetBefore;
                        } else {
                            // Interprets the time with rule before the transition,
                            // default for non-existing time range
                            transition += offsetAfter;
                        }
                    } else {
                        // Negative transition, which makes a duplicated local time range
                        if (((DuplicatedTimeOpt & STD_DST_MASK) == LOCAL_STD && dstToStd)
                                || ((DuplicatedTimeOpt & STD_DST_MASK) == LOCAL_DST && stdToDst)) {
                            transition += offsetAfter;
                        } else if (((DuplicatedTimeOpt & STD_DST_MASK) == LOCAL_STD && stdToDst)
                                || ((DuplicatedTimeOpt & STD_DST_MASK) == LOCAL_DST && dstToStd)) {
                            transition += offsetBefore;
                        } else if ((DuplicatedTimeOpt & FORMER_LATTER_MASK) == LOCAL_FORMER) {
                            transition += offsetBefore;
                        } else {
                            // Interprets the time with rule after the transition,
                            // default for duplicated local time range
                            transition += offsetAfter;
                        }
                    }
                }
                
                if (sec >= transition) {
                    break;
                }
            }

            if (ASSERT) Assert.assrt("i>=0 && i<transitionCount", i>=0 && i<transitionCount);

            // Check invariants for GMT times; if these pass for GMT times
            // the local logic should be working too.
            if (ASSERT) {
                Assert.assrt("local || sec < transitionTimes[0] || sec >= transitionTimes[i]", 
                        local || sec < transitionTimes[0] || sec >= transitionTimes[i]);
                Assert.assrt("local || i == transitionCount-1 || sec < transitionTimes[i+1]", 
                        local || i == transitionCount-1 || sec < transitionTimes[i+1]);
            }
            // Since ICU tzdata 2007c, the first transition data is actually not a
            // transition, but used for representing the initial offset.  So the code
            // below works even if i == 0.
            int index = getInt(typeData[i]);
            offsets[0] = rawOffset(index) * Grego.MILLIS_PER_SECOND;
            offsets[1] = dstOffset(index) * Grego.MILLIS_PER_SECOND;
        } else {
            // No transitions, single pair of offsets only
            offsets[0] = rawOffset(0) * Grego.MILLIS_PER_SECOND;
            offsets[1] = dstOffset(0) * Grego.MILLIS_PER_SECOND;
        }
    }

    private int zoneOffset(int index){
        index=index << 1;
        return typeOffsets[index] + typeOffsets[index+1];
    }

    private int rawOffset(int index){
        return typeOffsets[(int)(index << 1)];
    }

    private int dstOffset(int index){
        return typeOffsets[(int)((index << 1) + 1)];
    }
    
    // temp
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(super.toString());
        buf.append('[');
        buf.append("transitionCount=" + transitionCount);
        buf.append(",typeCount=" + typeCount);
        buf.append(",transitionTimes=");
        if (transitionTimes != null) {
            buf.append('[');
            for (int i = 0; i < transitionTimes.length; ++i) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(Integer.toString(transitionTimes[i]));
            }
            buf.append(']');
        } else {
            buf.append("null");
        }
        buf.append(",typeOffsets=");
        if (typeOffsets != null) {
            buf.append('[');
            for (int i = 0; i < typeOffsets.length; ++i) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(Integer.toString(typeOffsets[i]));
            }
            buf.append(']');
        } else {
            buf.append("null");
        }
        buf.append(",finalYear=" + finalYear);
        buf.append(",finalMillis=" + finalMillis);
        buf.append(",finalZone=" + finalZone);
        buf.append(']');
        
        return buf.toString();
    }

    /**
     * Number of transitions, 0..~370
     */
    private int transitionCount;

    /**
     * Number of types, 1..255
     */
    private int typeCount;

    /**
     * Time of each transition in seconds from 1970 epoch.
     * Length is transitionCount int32_t's.
     */
    private int[] transitionTimes; // alias into res; do not delete

    /**
     * Offset from GMT in seconds for each type.
     * Length is typeCount int32_t's.
     */
    private int[] typeOffsets; // alias into res; do not delete

    /**
     * Type description data, consisting of transitionCount uint8_t
     * type indices (from 0..typeCount-1).
     * Length is transitionCount int8_t's.
     */
    private byte[] typeData; // alias into res; do not delete

    /**
     * The last year for which the transitions data are to be used
     * rather than the finalZone.  If there is no finalZone, then this
     * is set to INT32_MAX.  NOTE: This corresponds to the year _before_
     * the one indicated by finalMillis.
     */
    private int finalYear = Integer.MAX_VALUE;

    /**
     * The millis for the start of the first year for which finalZone
     * is to be used, or DBL_MAX if finalZone is 0.  NOTE: This is
     * 0:00 GMT Jan 1, <finalYear + 1> (not <finalMillis>).
     */
    private double finalMillis = Double.MAX_VALUE;

    /**
     * A SimpleTimeZone that governs the behavior for years > finalYear.
     * If and only if finalYear == INT32_MAX then finalZone == 0.
     */
    private SimpleTimeZone finalZone = null; // owned, may be NULL
 
    private static final boolean DEBUG = ICUDebug.enabled("olson");
    private static final int SECONDS_PER_DAY = 24*60*60;
    
    private static UResourceBundle loadRule(UResourceBundle top, String ruleid) {
        UResourceBundle r = top.get("Rules");
        r = r.get(ruleid);
        return r;
    }

    /**
     * Divide two long integers, returning the floor of the quotient.
     * <p>
     * Unlike the built-in division, this is mathematically well-behaved.
     * E.g., <code>-1/4</code> => 0
     * but <code>floorDivide(-1,4)</code> => -1.
     * @param numerator the numerator
     * @param denominator a divisor which must be > 0
     * @return the floor of the quotient.
     * @stable ICU 2.0
     */
    private static final long myFloorDivide(long numerator, long denominator) {
        // We do this computation in order to handle
        // a numerator of Long.MIN_VALUE correctly
        return (numerator >= 0) ?
            numerator / denominator :
            ((numerator + 1) / denominator) - 1;
    }

    public boolean equals(Object obj){
        if (!super.equals(obj)) return false; // super does class check
        
        OlsonTimeZone z = (OlsonTimeZone) obj;

        return (Utility.arrayEquals(typeData, z.typeData) ||
                 // If the pointers are not equal, the zones may still
                 // be equal if their rules and transitions are equal
                 (finalYear == z.finalYear &&
                  // Don't compare finalMillis; if finalYear is ==, so is finalMillis
                  ((finalZone == null && z.finalZone == null) ||
                   (finalZone != null && z.finalZone != null &&
                    finalZone.equals(z.finalZone)) &&
                  transitionCount == z.transitionCount &&
                  typeCount == z.typeCount &&
                  Utility.arrayEquals(transitionTimes, z.transitionTimes) &&
                  Utility.arrayEquals(typeOffsets, z.typeOffsets) &&
                  Utility.arrayEquals(typeData, z.typeData)
                  )));

    }

    public int hashCode(){
        int ret =   (int)  (finalYear ^ (finalYear>>>4) +
                   transitionCount ^ (transitionCount>>>6) +
                   typeCount ^ (typeCount>>>8) + 
                   Double.doubleToLongBits(finalMillis)+
                   (finalZone == null ? 0 : finalZone.hashCode()) + 
                   super.hashCode());
        for(int i=0; i<transitionTimes.length; i++){
            ret+=transitionTimes[i]^(transitionTimes[i]>>>8);
        }
        for(int i=0; i<typeOffsets.length; i++){
            ret+=typeOffsets[i]^(typeOffsets[i]>>>8);
        }
        for(int i=0; i<typeData.length; i++){
            ret+=typeData[i] & UNSIGNED_BYTE_MASK;
        } 
        return ret;
    }
    /*
    private void readObject(ObjectInputStream s) throws IOException  {
        s.defaultReadObject();
        // customized deserialization code
       
        // followed by code to update the object, if necessary
    }
    */

 
    //
    // BasicTimeZone methods
    //

    /* (non-Javadoc)
     * @see com.ibm.icu.util.BasicTimeZone#getNextTransition(long, boolean)
     */
    public TimeZoneTransition getNextTransition(long base, boolean inclusive) {
        initTransitionRules();

        if (finalZone != null) {
            if (inclusive && base == firstFinalTZTransition.getTime()) {
                return firstFinalTZTransition;
            } else if (base >= firstFinalTZTransition.getTime()) {
                if (finalZone.useDaylightTime()) {
                    //return finalZone.getNextTransition(base, inclusive);
                    return finalZoneWithStartYear.getNextTransition(base, inclusive);
                } else {
                    // No more transitions
                    return null;
                }
            }
        }
        if (historicRules != null) {
            // Find a historical transition
            int ttidx = transitionCount - 1;
            for (; ttidx >= firstTZTransitionIdx; ttidx--) {
                long t = ((long)transitionTimes[ttidx]) * Grego.MILLIS_PER_SECOND;
                if (base > t || (!inclusive && base == t)) {
                    break;
                }
            }
            if (ttidx == transitionCount - 1)  {
                return firstFinalTZTransition;
            } else if (ttidx < firstTZTransitionIdx) {
                return firstTZTransition;
            } else {
                // Create a TimeZoneTransition
                TimeZoneRule to = historicRules[getInt(typeData[ttidx + 1])];
                TimeZoneRule from = historicRules[getInt(typeData[ttidx])];
                long startTime = ((long)transitionTimes[ttidx+1])*Grego.MILLIS_PER_SECOND;

                // The transitions loaded from zoneinfo.res may contain non-transition data
                if (from.getName().equals(to.getName()) && from.getRawOffset() == to.getRawOffset()
                        && from.getDSTSavings() == to.getDSTSavings()) {
                    return getNextTransition(startTime, false);
                }

                return new TimeZoneTransition(startTime, from, to);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.BasicTimeZone#getPreviousTransition(long, boolean)
     */
    public TimeZoneTransition getPreviousTransition(long base, boolean inclusive) {
        initTransitionRules();

        if (finalZone != null) {
            if (inclusive && base == firstFinalTZTransition.getTime()) {
                return firstFinalTZTransition;
            } else if (base > firstFinalTZTransition.getTime()) {
                if (finalZone.useDaylightTime()) {
                    //return finalZone.getPreviousTransition(base, inclusive);
                    return finalZoneWithStartYear.getPreviousTransition(base, inclusive);
                } else {
                    return firstFinalTZTransition;
                }                
            }
        }

        if (historicRules != null) {
            // Find a historical transition
            int ttidx = transitionCount - 1;
            for (; ttidx >= firstTZTransitionIdx; ttidx--) {
                long t = ((long)transitionTimes[ttidx]) * Grego.MILLIS_PER_SECOND;
                if (base > t || (inclusive && base == t)) {
                    break;
                }
            }
            if (ttidx < firstTZTransitionIdx) {
                // No more transitions
                return null;
            } else if (ttidx == firstTZTransitionIdx) {
                return firstTZTransition;
            } else {
                // Create a TimeZoneTransition
                TimeZoneRule to = historicRules[getInt(typeData[ttidx])];
                TimeZoneRule from = historicRules[getInt(typeData[ttidx-1])];
                long startTime = ((long)transitionTimes[ttidx])*Grego.MILLIS_PER_SECOND;

                // The transitions loaded from zoneinfo.res may contain non-transition data
                if (from.getName().equals(to.getName()) && from.getRawOffset() == to.getRawOffset()
                        && from.getDSTSavings() == to.getDSTSavings()) {
                    return getPreviousTransition(startTime, false);
                }

                return new TimeZoneTransition(startTime, from, to);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.BasicTimeZone#getTimeZoneRules()
     */
    public TimeZoneRule[] getTimeZoneRules() {
        initTransitionRules();
        int size = 1;
        if (historicRules != null) {
            // historicRules may contain null entries when original zoneinfo data
            // includes non transition data.
            for (int i = 0; i < historicRules.length; i++) {
                if (historicRules[i] != null) {
                    size++;
                }
            }
        }
        if (finalZone != null) {
            if (finalZone.useDaylightTime()) {
                size += 2;
            } else {
                size++;
            }
        }

        TimeZoneRule[] rules = new TimeZoneRule[size];
        int idx = 0;
        rules[idx++] = initialRule;

        if (historicRules != null) {
            for (int i = 0; i < historicRules.length; i++) {
                if (historicRules[i] != null) {
                    rules[idx++] = historicRules[i];
                }
            }
         }

        if (finalZone != null) {
            if (finalZone.useDaylightTime()) {
                TimeZoneRule[] stzr = finalZoneWithStartYear.getTimeZoneRules();
                // Adding only transition rules
                rules[idx++] = stzr[1];
                rules[idx++] = stzr[2];
            } else {
                // Create a TimeArrayTimeZoneRule at finalMillis
                rules[idx++] = new TimeArrayTimeZoneRule(getID() + "(STD)", finalZone.getRawOffset(), 0,
                        new long[] {(long)finalMillis}, DateTimeRule.UTC_TIME);                
            }
        }
        return rules;
    }

    private transient InitialTimeZoneRule initialRule;
    private transient TimeZoneTransition firstTZTransition;
    private transient int firstTZTransitionIdx;
    private transient TimeZoneTransition firstFinalTZTransition;
    private transient TimeArrayTimeZoneRule[] historicRules;
    private transient SimpleTimeZone finalZoneWithStartYear; // hack

    private transient boolean transitionRulesInitialized;

    private synchronized void initTransitionRules() {
        if (transitionRulesInitialized) {
            return;
        }

        initialRule = null;
        firstTZTransition = null;
        firstFinalTZTransition = null;
        historicRules = null;
        firstTZTransitionIdx = 0;
        finalZoneWithStartYear = null;

        String stdName = getID() + "(STD)";
        String dstName = getID() + "(DST)";

        int raw, dst;
        if (transitionCount > 0) {
            int transitionIdx, typeIdx;

            // Note: Since 2007c, the very first transition data is a dummy entry
            //       added for resolving a offset calculation problem.

            // Create initial rule
            typeIdx = getInt(typeData[0]); // initial type
            raw = typeOffsets[typeIdx*2]*Grego.MILLIS_PER_SECOND;
            dst = typeOffsets[typeIdx*2 + 1]*Grego.MILLIS_PER_SECOND;
            initialRule = new InitialTimeZoneRule((dst == 0 ? stdName : dstName), raw, dst);

            for (transitionIdx = 1; transitionIdx < transitionCount; transitionIdx++) {
                firstTZTransitionIdx++;
                if (typeIdx != getInt(typeData[transitionIdx])) {
                    break;
                }
            }
            if (transitionIdx == transitionCount) {
                // Actually no transitions...
            } else {
                // Build historic rule array
                long[] times = new long[transitionCount];
                for (typeIdx = 0; typeIdx < typeCount; typeIdx++) {
                    // Gather all start times for each pair of offsets
                    int nTimes = 0;
                    for (transitionIdx = firstTZTransitionIdx; transitionIdx < transitionCount; transitionIdx++) {
                        if (typeIdx == getInt(typeData[transitionIdx])) {
                            long tt = ((long)transitionTimes[transitionIdx])*Grego.MILLIS_PER_SECOND;
                            if (tt < finalMillis) {
                                // Exclude transitions after finalMillis
                                times[nTimes++] = tt;
                            }
                        }
                    }
                    if (nTimes > 0) {
                        long[] startTimes = new long[nTimes];
                        System.arraycopy(times, 0, startTimes, 0, nTimes);
                        // Create a TimeArrayTimeZoneRule
                        raw = typeOffsets[typeIdx*2]*Grego.MILLIS_PER_SECOND;
                        dst = typeOffsets[typeIdx*2 + 1]*Grego.MILLIS_PER_SECOND;
                        if (historicRules == null) {
                            historicRules = new TimeArrayTimeZoneRule[typeCount];
                        }
                        historicRules[typeIdx] = new TimeArrayTimeZoneRule((dst == 0 ? stdName : dstName),
                                raw, dst, startTimes, DateTimeRule.UTC_TIME);
                    }
                }

                // Create initial transition
                typeIdx = getInt(typeData[firstTZTransitionIdx]);
                firstTZTransition = new TimeZoneTransition(((long)transitionTimes[firstTZTransitionIdx])*Grego.MILLIS_PER_SECOND,
                        initialRule, historicRules[typeIdx]);
                
            }
        }

        if (initialRule == null) {
            // No historic transitions
            raw = typeOffsets[0]*Grego.MILLIS_PER_SECOND;
            dst = typeOffsets[1]*Grego.MILLIS_PER_SECOND;
            initialRule = new InitialTimeZoneRule((dst == 0 ? stdName : dstName), raw, dst);
        }

        if (finalZone != null) {
            // Get the first occurrence of final rule starts
            long startTime = (long)finalMillis;
            TimeZoneRule firstFinalRule;
            if (finalZone.useDaylightTime()) {
                /*
                 * Note: When an OlsonTimeZone is constructed, we should set the final year
                 * as the start year of finalZone.  However, the boundary condition used for
                 * getting offset from finalZone has some problems.  So setting the start year
                 * in the finalZone will cause a problem.  For now, we do not set the valid
                 * start year when the construction time and create a clone and set the
                 * start year when extracting rules.
                 */
                finalZoneWithStartYear = (SimpleTimeZone)finalZone.clone();
                // finalYear is 1 year before the actual final year.
                // See the comment in the construction method.
                finalZoneWithStartYear.setStartYear(finalYear + 1);

                TimeZoneTransition tzt = finalZoneWithStartYear.getNextTransition(startTime, false);
                firstFinalRule  = tzt.getTo();
                startTime = tzt.getTime();
            } else {
                finalZoneWithStartYear = finalZone;
                firstFinalRule = new TimeArrayTimeZoneRule(finalZone.getID(),
                        finalZone.getRawOffset(), 0, new long[] {startTime}, DateTimeRule.UTC_TIME);
            }
            TimeZoneRule prevRule = null;
            if (transitionCount > 0) {
                prevRule = historicRules[getInt(typeData[transitionCount - 1])];
            }
            if (prevRule == null) {
                // No historic transitions, but only finalZone available
                prevRule = initialRule;
            }
            firstFinalTZTransition = new TimeZoneTransition(startTime, prevRule, firstFinalRule);
        }

        transitionRulesInitialized = true;
    }
}
