// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.impl;

import static org.junit.Assert.*;

import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.MissingResourceException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ResourceBundleWrapperCachingTest {
    @Test
    public void testCacheOnDifferentClassloaders() {
        // Loading first bundle
        try (var firstCL =
                new URLClassLoader(
                        new URL[] {getClass().getResource("/com/ibm/icu/dev/test/locale/first/")},
                        null)) {
            // Making sure that resources are available
            assertNotNull(firstCL.getResource("localization.properties"));
            assertNotNull(firstCL.getResource("localization_de.properties"));

            // Getting the bundle. Since RootType here will be JAVA,
            // ResourceBundleWrapper is chosen by UResourceBundle#instantiateBundle as
            // implementation
            // Passed locale here should not matter
            var bundle = UResourceBundle.getBundleInstance("localization", ULocale.GERMAN, firstCL);

            // Only 'First' should be present
            assertEquals("Dies ist eine erste Zeile", bundle.getString("First"));
            // 'Second' is not in the first bundle
            assertThrows(MissingResourceException.class, () -> bundle.getString("Second"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Loading second bundle
        try (var secondFL =
                new URLClassLoader(
                        new URL[] {getClass().getResource("/com/ibm/icu/dev/test/locale/second/")},
                        null)) {
            // Making sure that resources are available
            assertNotNull(secondFL.getResource("localization.properties"));
            assertNotNull(secondFL.getResource("localization_de.properties"));

            // Making sure that second bundle has `Second` in the localization file (unlike the
            // first one)
            try (var is = secondFL.getResourceAsStream("localization.properties")) {
                assertTrue(
                        new String(is.readAllBytes(), StandardCharsets.UTF_8)
                                .contains("Second=This is a second line"));
            }

            // Getting the bundle, same as the first one
            var bundle =
                    UResourceBundle.getBundleInstance("localization", ULocale.GERMAN, secondFL);

            assertEquals("Dies ist eine erste Zeile", bundle.getString("First"));
            // Must contain 'Second' and not throw MissingResourceException if cached properly (no
            // clash between first and second)
            assertEquals("Dies ist eine zweite Zeile", bundle.getString("Second"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
