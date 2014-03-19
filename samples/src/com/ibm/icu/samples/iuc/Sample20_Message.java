/*
 *******************************************************************************
 * Copyright (C) 2013, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.samples.iuc;

import java.util.HashMap;
import java.util.Map;

import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.LocaleDisplayNames.DialectHandling;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.ULocale;

/**
 * @author srl
 *
 */
public class Sample20_Message {
    public static void main(String... args) {
        ULocale defaultLocaleID = ULocale.getDefault();
        LocaleDisplayNames ldn = LocaleDisplayNames.getInstance(defaultLocaleID, DialectHandling.DIALECT_NAMES);
        String defaultLocaleName = ldn.localeDisplayName(defaultLocaleID);
        String world = ldn.regionDisplayName("001");
        MessageFormat fmt = new MessageFormat("A hello to {part, number, percent} of the {world}, in {mylocale}, on {today, date}!",
                defaultLocaleID);
        Map<String, Object> msgargs = new HashMap<String, Object>();
        msgargs.put("part", 1.00);
        msgargs.put("world", world);
        msgargs.put("mylocale", defaultLocaleName);
        msgargs.put("today", System.currentTimeMillis());
        System.out.println(fmt.format(msgargs, new StringBuffer(), null));
    }
}
