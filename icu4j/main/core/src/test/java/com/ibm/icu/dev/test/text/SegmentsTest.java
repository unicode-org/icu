package com.ibm.icu.dev.test.text;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.text.LocalizedSegmenter;
import com.ibm.icu.text.LocalizedSegmenter.SegmentationType;
import com.ibm.icu.text.Segments;
import com.ibm.icu.util.ULocale;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SegmentsTest extends CoreTestFmwk {

  @Test
  public void testMultipleSegmentsFromSegmenter() {
    LocalizedSegmenter enWordSegmenter =
        LocalizedSegmenter.builder()
            .setLocale(ULocale.ENGLISH)
            .setSegmentationType(SegmentationType.WORD)
            .build();

    String source1 = "The quick brown fox jumped over the lazy dog.";
    String source2 = "Sphinx of black quartz, judge my vow.";
    String source3 = "How vexingly quick daft zebras jump!";

    List<CharSequence> exp1 = Arrays.asList("The", " ", "quick", " ", "brown", " ", "fox", " ",
        "jumped", " ", "over", " ", "the", " ", "lazy", " ", "dog", ".");
    List<CharSequence> exp2 = Arrays.asList("Sphinx", " ", "of", " ", "black", " ", "quartz", ",",
        " ", "judge", " ", "my", " ", "vow", ".");
    List<CharSequence> exp3 = Arrays.asList("How", " ", "vexingly", " ", "quick", " ", "daft", " ",
        "zebras", " ", "jump", "!");

    // Create new Segments for source1
    Segments segments1 = enWordSegmenter.segment(source1);
    List<CharSequence> act1 = segments1.subSequences().collect(Collectors.toList());
    assertThat(act1, is(exp1));

    // Create new Segments for source2
    Segments segments2 = enWordSegmenter.segment(source2);
    List<CharSequence> act2 = segments2.subSequences().collect(Collectors.toList());
    assertThat(act2, is(exp2));

    // Check that Segments for source1 is unaffected
    act1 = segments1.subSequences().collect(Collectors.toList());
    assertThat(act1, is(exp1));

    // Create new Segments for source3
    Segments segments3 = enWordSegmenter.segment(source3);
    List<CharSequence> act3 = segments3.subSequences().collect(Collectors.toList());
    assertThat(act3, is(exp3));

    // Check that Segments for source1 is unaffected
    act1 = segments1.subSequences().collect(Collectors.toList());
    assertThat(act1, is(exp1));

    // Check that Segments for source2 is unaffected
    act2 = segments2.subSequences().collect(Collectors.toList());
    assertThat(act2, is(exp2));
  }

}
