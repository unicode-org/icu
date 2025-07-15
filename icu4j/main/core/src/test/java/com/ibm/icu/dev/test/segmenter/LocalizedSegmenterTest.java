// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.segmenter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.segmenter.LocalizedSegmenter;
import com.ibm.icu.segmenter.LocalizedSegmenter.SegmentationType;
import com.ibm.icu.segmenter.Segmenter;
import com.ibm.icu.segmenter.Segments;
import com.ibm.icu.util.ULocale;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LocalizedSegmenterTest extends CoreTestFmwk {

  @Test
  public void testLocaleInLocalizedSegmenter() {

    Object[][] casesData = {
        {"de",
            "Das 21ste Jahrh. ist das beste.",
            Arrays.asList("Das", " ", "21ste", " ", "Jahrh", ".", " ", "ist", " ", "das", " ", "beste", ".")},
    };

    for (Object[] caseDatum : casesData) {
      String localeTag = (String) caseDatum[0];
      String source = (String) caseDatum[1];
      List<CharSequence> expWords = (List<CharSequence>) caseDatum[2];

      // create functions that abstract over the different types of locales so that, later on,
      // we can test using both in the same way
      Function<LocalizedSegmenter.Builder, LocalizedSegmenter.Builder> setJLocaleFn =
          builder -> builder.setLocale(Locale.forLanguageTag(localeTag));
      Function<LocalizedSegmenter.Builder, LocalizedSegmenter.Builder> setULocaleFn =
          builder -> builder.setLocale(ULocale.forLanguageTag(localeTag));

      // Loop/iterate over both Java Locale and ICU ULocale such that we always test with both
      // types of locale objects.
      for (Function<LocalizedSegmenter.Builder, LocalizedSegmenter.Builder> setLocIntoBuilder : Arrays.asList(setJLocaleFn, setULocaleFn)) {

        LocalizedSegmenter.Builder builder = LocalizedSegmenter.builder()
            .setSegmentationType(SegmentationType.WORD);
        LocalizedSegmenter.Builder builderWithLocale = setLocIntoBuilder.apply(builder);
        Segmenter wordSeg = builderWithLocale.build();
        Segments segments = wordSeg.segment(source);

        List<CharSequence> actWords = segments.subSequences().collect(Collectors.toList());

        assertThat(actWords, is(expWords));
      }
    }
  }
}
