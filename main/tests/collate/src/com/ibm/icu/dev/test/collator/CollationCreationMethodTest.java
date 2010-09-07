/*
 *******************************************************************************
 * Copyright (C) 2002-2010, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
 
package com.ibm.icu.dev.test.collator;

import java.util.Locale;
import java.util.Random;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.CollationKey;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;


/**
 * 
 * CollationCreationMethodTest checks to ensure that the collators act the same whether they are created by choosing a 
 * locale and loading the data from file, or by using rules.
 * 
 * @author Brian Rower - IBM - August 2008
 *
 */
public class CollationCreationMethodTest extends TestFmwk 
{
    
    public static void main(String[] args) throws Exception 
    {
        new CollationCreationMethodTest().run(args);
    }

    public void TestRuleVsLocaleCreationMonkey()
    {
        //create a RBC from a collator reader by reading in a locale collation file
        //also create one simply from a rules string (which should be 
        //pulled from the locale collation file)
        //and then do crazy monkey testing on it to make sure they are the same.
        int x,y,z;
        Random r = createRandom();
        String randString1;
        CollationKey key1;
        CollationKey key2;


        Locale[] locales = Collator.getAvailableLocales();

        RuleBasedCollator localeCollator;
        RuleBasedCollator ruleCollator;

        for(z = 0; z < 60; z++)
        {
            x = r.nextInt(locales.length);

            try
            {
                //this is making the assumption that the only type of collator that will be made is RBC
                localeCollator = (RuleBasedCollator)Collator.getInstance(locales[x]);
                ruleCollator = new RuleBasedCollator(localeCollator.getRules());
                logln("Rules are: " + localeCollator.getRules());
            } 
            catch (Exception e) 
            {
                warnln("ERROR: in creation of collator of " + locales[x].getDisplayName() + " locale");
                return;
            }

            //do it several times for each collator
            int n = 3;
            for(y = 0; y < n; y++)
            {

                randString1 = generateNewString(r);

                key1 = localeCollator.getCollationKey(randString1);
                key2 = ruleCollator.getCollationKey(randString1);
               
                report(locales[x].getDisplayName(), randString1, key1, key2);
            }
        }
    }

    private String generateNewString(Random r)
    {
        int maxCodePoints = 40;
        byte[] c = new byte[r.nextInt(maxCodePoints)*2]; //two bytes for each code point
        int x;
        int z;
        String s = "";

        for(x = 0; x < c.length/2; x = x + 2) //once around for each UTF-16 character
        {
            z = r.nextInt(0x7fff); //the code point...

            c[x + 1] = (byte)z;
            c[x] = (byte)(z >>> 4);
        }
        try
        {
            s = new String(c, "UTF-16BE");
        }
        catch(Exception e)
        {
            warnln("Error creating random strings");
        }
        return s;
    }

    private void report(String localeName, String string1, CollationKey k1, CollationKey k2) 
    {
        if (!k1.equals(k2)) 
        {
            String msg = "";
            msg += "With " + localeName + "Collator: ";
            msg += string1; 
            msg += " failed to produce identical keys on both collators";
            errln(msg);
        }
    }
}
