package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.GregorianSequences.Daily;
import static org.hisp.dhis2.time.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DailyGregorianSequenceTest {

  @Test
  void intervalIsSingleDay() {
    var i = Daily.interval(day(2024, 6, 15));
    assertEquals(1, i.lengthInDays());
    assertInterval(i, 2024, 6, 15, 2024, 6, 15);
  }

  @Test
  void intervalContainsDayForVariedInputs() {
    for (var d : List.of(
        day(2000, 1, 1), day(2024, 2, 29), day(2099, 12, 31))) {
      assertContains(Daily.interval(d), d);
    }
  }

  @Test
  void nextIsFollowingDay() {
    assertInterval(Daily.next(Daily.interval(day(2024, 6, 15))),
        2024, 6, 16, 2024, 6, 16);
  }

  @Test
  void previousIsPrecedingDay() {
    assertInterval(Daily.previous(Daily.interval(day(2024, 6, 15))),
        2024, 6, 14, 2024, 6, 14);
  }

  @Test
  void nextAndPreviousAreInverses() {
    var i = Daily.interval(day(2024, 6, 15));
    assertEquals(i, Daily.previous(Daily.next(i)));
    assertEquals(i, Daily.next(Daily.previous(i)));
  }

  @Test
  void todayUsesSupplier() {
    long fakeNow = day(2024, 6, 15).toUTCMillis();
    assertInterval(Daily.today(() -> fakeNow), 2024, 6, 15, 2024, 6, 15);
  }

  @Test
  void partitionOfRangeYieldsOneIntervalPerDay() {
    var parent = interval(2024, 1, 1, 2024, 1, 10);
    var parts = Daily.partition(parent);
    assertEquals(10, parts.size());
    assertEquals(day(2024, 1, 1), parts.get(0).first());
    assertEquals(day(2024, 1, 10), parts.get(9).last());
  }

  @Test
  void partitionsEqualsAggregatesTo() {
    assertEquals(Daily.partitions(), Daily.aggregatesTo());
  }

  @Test
  void compactConstructorRejectsPartitionsOutsideAggregatesTo() {
    var key = new TimeSequenceKey('G', 'D');
    assertThrows(IllegalArgumentException.class,
        () -> new GregorianSequences.DailyGregorianSequence(
            key, Set.of(GregorianSequences.Week), Set.of()));
  }
}