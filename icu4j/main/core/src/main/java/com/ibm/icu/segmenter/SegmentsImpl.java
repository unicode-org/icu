// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.segmenter;

import com.ibm.icu.text.BreakIterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class SegmentsImpl implements Segments {

  private CharSequence source;

  private BreakIterator breakIterPrototype;

  SegmentsImpl(BreakIterator breakIter, CharSequence source) {
    this.source = source;

    // We are creating a clone of the Segmenter's prototype BreakIterator field so that this
    // concrete Segments object can avoid sharing state with the other Segments object instances
    // that get spawned from the Segmenter. This allows difference source CharSequences to be used
    // in each Segments object.
    //
    // In turn, the cloned BreakIterator becomes a prototype to be stored in the Segments object,
    // which then gets cloned and used in each of the Segments APIs' implementations. The second
    // level of cloning that happens when the Segments object's local BreakIterator prototype
    // gets cloned allows the iteration state to be separate whenever an Segments API is called.
    // Otherwise, there is a chance that multiple API calls on the same Segments object might
    // mutate the same position/index, if done concurrently.
    breakIterPrototype = breakIter.clone();
    // It's okay to perform .setText on the object that we want to clone later because we should
    // then not have to call .setText on the clones.
    breakIterPrototype.setText(source);
  }

  @Override
  public Segment segmentAt(int i) {
    BreakIterator breakIter = breakIterPrototype.clone();
    int start;
    int limit;

    if (i < 0 || i >= source.length()) {
      throw new IndexOutOfBoundsException(i);
    }

    boolean isBoundary = breakIter.isBoundary(i);

    if (isBoundary) {
      start = i;
      limit = breakIter.next();
    } else {
      // BreakIterator.isBoundary(i) will advance forwards to the next boundary if the argument
      // is not a boundary.
      limit = breakIter.current();
      start = breakIter.previous();
    }

    assert start != BreakIterator.DONE && limit != BreakIterator.DONE;

    return new Segment(start, limit, source);
  }

  @Override
  public boolean isBoundary(int i) {
    return breakIterPrototype.clone().isBoundary(i);
  }

  @Override
  public Stream<Segment> segmentsFrom(int i) {
    BreakIterator breakIter = breakIterPrototype.clone();

    // create a Stream from a Spliterator of an Iterable so that the Stream can be lazy, not eager
    SegmentIterable iterable = new SegmentIterable(breakIter, IterationDirection.FORWARDS, i,
        source);
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  @Override
  public Stream<Segment> segmentsBefore(int i) {
    BreakIterator breakIter = breakIterPrototype.clone();

    // create a Stream from a Spliterator of an Iterable so that the Stream can be lazy, not eager
    SegmentIterable iterable = new SegmentIterable(breakIter, IterationDirection.BACKWARDS, i,
        source);
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  @Override
  public IntStream boundariesAfter(int i) {
    BreakIterator breakIter = breakIterPrototype.clone();

    // create a Stream from a Spliterator of an Iterable so that the Stream can be lazy, not eager
    return StreamSupport.intStream(new BoundarySpliterator(breakIter, source, IterationDirection.FORWARDS,
        i), false);
  }

  @Override
  public IntStream boundariesBackFrom(int i) {
    BreakIterator breakIter = breakIterPrototype.clone();
    // create a Stream from a Spliterator of an Iterable so that the Stream can be lazy, not eager
    return StreamSupport.intStream(new BoundarySpliterator(breakIter, source, IterationDirection.BACKWARDS,
        i), false);
  }

}
