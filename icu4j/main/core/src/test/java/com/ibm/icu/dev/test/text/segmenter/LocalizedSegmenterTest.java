package com.ibm.icu.dev.test.text.segmenter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.text.segmenter.LocalizedSegmenter;
import com.ibm.icu.text.segmenter.LocalizedSegmenter.SegmentationType;
import com.ibm.icu.text.segmenter.Segmenter;
import com.ibm.icu.text.segmenter.Segments;
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
    String source = "Die 21en Jahrh. ist die Beste.";

    Object[][] casesData = {
        {"de", Arrays.asList("Die 21en Jahrh. ist die Beste.")},
    };

    for (Object[] caseDatum : casesData) {
      String localeTag = (String) caseDatum[0];
      ULocale locale = ULocale.forLanguageTag(localeTag);
      List<CharSequence> expWords = (List<CharSequence>) caseDatum[1];

      Segmenter wordSeg =
          LocalizedSegmenter.builder()
              .setLocale(locale)
              .setSegmentationType(SegmentationType.SENTENCE)
              .build();
      Segments segments = wordSeg.segment(source);

      List<CharSequence> actWords = segments.subSequences().collect(Collectors.toList());

      assertThat(actWords, is(expWords));
    }
  }
}
