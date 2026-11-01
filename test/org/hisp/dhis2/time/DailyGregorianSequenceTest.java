package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.GregorianSequences.GD_;
import static org.hisp.dhis2.time.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DailyGregorianSequenceTest {

  @Test
  void intervalIsSingleDay() {
    var i = GD_.interval(Epoch2kDay.ofYMD(2024, 6, 15));
    assertEquals(1, i.lengthInDays());
    assertInterval(i, 2024, 6, 15, 2024, 6, 15);
  }

  @Test
  void intervalContainsDayForVariedInputs() {
    for (var d : List.of(Epoch2kDay.ofYMD(2000, 1, 1), Epoch2kDay.ofYMD(2024, 2, 29), Epoch2kDay.ofYMD(2099, 12, 31))) {
      assertContains(GD_.interval(d), d);
    }
  }

  @Test
  void nextIsFollowingDay() {
    assertInterval(GD_.next(GD_.interval(Epoch2kDay.ofYMD(2024, 6, 15))), 2024, 6, 16, 2024, 6, 16);
  }

  @Test
  void previousIsPrecedingDay() {
    assertInterval(GD_.previous(GD_.interval(Epoch2kDay.ofYMD(2024, 6, 15))), 2024, 6, 14, 2024, 6, 14);
  }

  @Test
  void nextAndPreviousAreInverses() {
    var i = GD_.interval(Epoch2kDay.ofYMD(2024, 6, 15));
    assertEquals(i, GD_.previous(GD_.next(i)));
    assertEquals(i, GD_.next(GD_.previous(i)));
  }

  @Test
  void todayUsesSupplier() {
    long fakeNow = Epoch2kDay.ofYMD(2024, 6, 15).toUTCMillis();
    assertInterval(GD_.today(() -> fakeNow), 2024, 6, 15, 2024, 6, 15);
  }

  @Test
  void partitionOfRangeYieldsOneIntervalPerDay() {
    var parent = interval(2024, 1, 1, 2024, 1, 10);
    var parts = GD_.partition(parent);
    assertEquals(10, parts.size());
    assertEquals(Epoch2kDay.ofYMD(2024, 1, 1), parts.get(0).firstDay());
    assertEquals(Epoch2kDay.ofYMD(2024, 1, 10), parts.get(9).lastDay());
  }

  @Test
  void partitionsEqualsAggregatesTo() {
    assertEquals(GD_.partitions(), GD_.contributesTo());
  }

  @Test
  void compactConstructorRejectsPartitionsOutsideAggregatesTo() {
    var key = new TimeSequenceKey('G', 'D');
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new GregorianSequences.DailyGregorianSequence(
                key, Set.of(GregorianSequences.GW), Set.of()));
  }
}
