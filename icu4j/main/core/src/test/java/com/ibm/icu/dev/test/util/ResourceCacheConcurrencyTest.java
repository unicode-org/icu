// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.util;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Concurrency regression test for ResourceCache. */
@RunWith(JUnit4.class)
public class ResourceCacheConcurrencyTest extends ConcurrencyTest {

    @Test
    public void testResourceCacheConcurrentLookups() throws Exception {
        String[] localeNames = {"en", "de", "ja", "zh", "fr", "es", "ko", "pt"};

        for (String loc : localeNames) {
            UResourceBundle rb =
                    UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, new ULocale(loc));
            if (rb == null) {
                return;
            }
        }

        runConcurrent(
                "ResourceCacheLookups",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        String loc = localeNames[(tid + i) % localeNames.length];
                        UResourceBundle rb =
                                UResourceBundle.getBundleInstance(
                                        ICUData.ICU_BASE_NAME, new ULocale(loc));
                        assertNotNull("ResourceBundle should not be null", rb);
                        try {
                            rb.get("Version");
                        } catch (Exception e) {
                            // Some bundles may not have this key
                        }
                    }
                });
    }
}
