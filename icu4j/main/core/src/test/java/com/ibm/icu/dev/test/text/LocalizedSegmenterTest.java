package com.ibm.icu.dev.test.text;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.text.LocalizedSegmenter;
import com.ibm.icu.text.LocalizedSegmenter.SegmentationType;
import com.ibm.icu.text.Segmenter;
import com.ibm.icu.text.Segments;
import com.ibm.icu.util.ULocale;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LocalizedSegmenterTest extends CoreTestFmwk {

  @Test
  public void testLocaleInLocalizedSegmenter() {
    String source = "k:a";

    Object[][] casesData = {
        {"en", Arrays.asList("k", ":", "a")},
        {"sv", Arrays.asList("k:a")}
    };

    for (Object[] caseDatum : casesData) {
      String localeTag = (String) caseDatum[0];
      ULocale locale = ULocale.forLanguageTag(localeTag);
      List<CharSequence> expWords = (List<CharSequence>) caseDatum[1];

      Segmenter wordSeg =
          LocalizedSegmenter.builder()
              .setLocale(locale)
              .setSegmentationType(SegmentationType.WORD)
              .build();
      Segments segments = wordSeg.segment(source);

      List<CharSequence> actWords = segments.subSequences().collect(Collectors.toList());

      assertThat(actWords, is(expWords));
    }
  }
}
