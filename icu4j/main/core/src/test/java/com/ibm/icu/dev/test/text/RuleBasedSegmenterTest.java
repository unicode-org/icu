package com.ibm.icu.dev.test.text;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.text.RuleBasedSegmenter;
import com.ibm.icu.text.Segmenter;
import com.ibm.icu.text.Segments;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RuleBasedSegmenterTest extends CoreTestFmwk {

  @Test
  public void testRules() {
    String source = "hejsan k:a tack";

    Object[][] casesData = {
        {"default",   ".*;",       Arrays.asList("hejsan k:a tack")},
        // TODO: add more cases once RBBI rule syntax is understood
    };

    for (Object[] caseDatum : casesData) {
      String desc = (String) caseDatum[0];
      String subrule = (String) caseDatum[1];
      List<CharSequence> expWords = (List<CharSequence>) caseDatum[2];

      // the following rule substring was taken as a subset from BreakIteratorRules_en_US_TEST.java:
      String rules = subrule;

      Segmenter seg = RuleBasedSegmenter.builder()
          .setRules(rules)
          .build();
      Segments segments = seg.segment(source);

      List<CharSequence> actWords = segments.subSequences().collect(Collectors.toList());

      assertThat(desc, actWords, is(expWords));
    }

  }

}
