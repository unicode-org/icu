/*
 * $RCSfile: ChineseCalendarSymbols.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:55 $
 */

package com.ibm.util.resources;

import java.util.ListResourceBundle;

public class ChineseCalendarSymbols extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1999 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {

        { "DateTimePatterns",
            new String[] {
                "h:mm:ss a z",      // full time pattern
                "h:mm:ss a z",      // long time pattern
                "h:mm:ss a",        // medium time pattern
                "h:mm a",           // short time pattern
                "EEEE, MMMM d, yy", // full date pattern
                "MMMM d, yy",       // long date pattern
                "MMM d, yy",        // medium date pattern
                "M/d/yy",           // short date pattern
                "{1} {0}"           // date-time pattern
            }
        },
        
        { "DisplayName",    "Chinese lunar" },
        
        { "Eras", new String[] {} },
        
        { "MonthTypes", new String[] {
                "", "leap"
            }
        },
        
        { "StemNames",
            new String[] {
                "Jia",
                "Yi",
                "Bing",
                "Ding",
                "Wu",
                "Ji",
                "Geng",
                "Xin",
                "Ren",
                "Gui",
            }
        },
        
        { "BranchNames",
            new String[] {
                "Rat",
                "Ox",
                "Tiger",
                "Hare",
                "Dragon",
                "Snake",
                "Horse",
                "Sheep",
                "Monkey",
                "Fowl",
                "Dog",
                "Pig",
            }
        },
        
        { "TermNames",
            new String[] {
                "Beginning of Spring",
                "Rain Water",
                "Waking of Insects",
                "Spring Equinox",
                "Pure Brightness",
                "Grain Rain",
                "Beginning of Summer",
                "Grain Full",
                "Grain in Ear",
                "Summer Solstice",
                "Slight Heat",
                "Great Heat",
                "Beginning of Autumn",
                "Limit of Heat",
                "White Dew",
                "Autumnal Equinox",
                "Cold Dew",
                "Descent of Frost",
                "Beginning of Winter",
                "Slight Snow",
                "Great Snow",
                "Winter Solstice",
                "Slight Cold",
                "Great Cold",
            }
        },
        
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
}
