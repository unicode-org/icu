/*
 * $RCSfile: ChineseDateSymbols.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:49 $
 */

package com.ibm.text;


import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

class ChineseDateSymbols extends DateFormatSymbols {
    
    public ChineseDateSymbols() {
        this(Locale.getDefault());
    }
    
    public ChineseDateSymbols(Locale loc) {
        super(loc);
        
        ResourceBundle bundle = ResourceBundle.getBundle("com.ibm.util.resources.ChineseCalendarSymbols", loc);
        
        try {
            String[] temp = bundle.getStringArray("DayNames");
            setWeekdays(temp);
            setShortWeekdays(temp);

            temp = bundle.getStringArray("DayAbbreviations");
            setShortWeekdays( temp );
        }
        catch (MissingResourceException e) {
        }

        try {
            String[] temp = bundle.getStringArray("MonthNames");
            setMonths( temp );
            setShortMonths( temp );

            temp = bundle.getStringArray("MonthAbbreviations");
            setShortMonths( temp );
        }
        catch (MissingResourceException e) {
        }

        setEras( bundle.getStringArray("Eras") );
        
        stemNames   = bundle.getStringArray("StemNames");
        branchNames = bundle.getStringArray("BranchNames");
        termNames   = bundle.getStringArray("TermNames");
        monthTypes  = bundle.getStringArray("MonthTypes");
        
    }

    public String[] getStemNames() {
        return duplicate(stemNames);
    }

    public String[] getBranchNames() {
        return duplicate(branchNames);
    }

    public String[] getTermNames() {
        return duplicate(termNames);
    }
    
    public String[] getMonthTypes() {
        return duplicate(monthTypes);
    }

    /**
     * Clones an array of Strings.
     * @param srcArray the source array to be cloned.
     * @param count the number of elements in the given source array.
     * @return a cloned array.
     */
    private final String[] duplicate(String[] srcArray)
    {
        String[] dstArray = new String[srcArray.length];
        System.arraycopy(srcArray, 0, dstArray, 0, srcArray.length);
        return dstArray;
    }
    
    private String[] stemNames;
    private String[] branchNames;
    private String[] termNames;
    private String[] monthTypes;
};
