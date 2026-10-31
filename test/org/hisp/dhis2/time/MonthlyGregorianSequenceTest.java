package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.GregorianSequences.Monthly;
import static org.hisp.dhis2.time.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MonthlyGregorianSequenceTest {

  @Test
  void intervalIsFullMonth() {
    assertInterval(Monthly.interval(Epoch2kDay.ofYMD(2024, 6, 15)), 2024, 6, 1, 2024, 6, 30);
  }

  @Test
  void leapYearFebruaryHas29Days() {
    assertInterval(Monthly.interval(Epoch2kDay.ofYMD(2024, 2, 15)), 2024, 2, 1, 2024, 2, 29);
  }

  @Test
  void nonLeapYearFebruaryHas28Days() {
    assertInterval(Monthly.interval(Epoch2kDay.ofYMD(2023, 2, 15)), 2023, 2, 1, 2023, 2, 28);
  }

  @Test
  void firstAndLastDayOfMonthMapToSameInterval() {
    var expected = Monthly.interval(Epoch2kDay.ofYMD(2024, 6, 1));
    assertEquals(expected, Monthly.interval(Epoch2kDay.ofYMD(2024, 6, 30)));
  }

  @Test
  void nextIsFollowingMonth() {
    assertInterval(
        Monthly.next(Monthly.interval(Epoch2kDay.ofYMD(2024, 6, 15))), 2024, 7, 1, 2024, 7, 31);
  }

  @Test
  void nextFromDecemberCrossesYear() {
    assertInterval(
        Monthly.next(Monthly.interval(Epoch2kDay.ofYMD(2024, 12, 15))), 2025, 1, 1, 2025, 1, 31);
  }

  @Test
  void previousIsPrecedingMonth() {
    assertInterval(
        Monthly.previous(Monthly.interval(Epoch2kDay.ofYMD(2024, 6, 15))), 2024, 5, 1, 2024, 5, 31);
  }

  @Test
  void nextAndPreviousAreInverses() {
    for (var d :
        new Epoch2kDay[] {
          Epoch2kDay.ofYMD(2024, 1, 15),
          Epoch2kDay.ofYMD(2024, 2, 15),
          Epoch2kDay.ofYMD(2024, 12, 15),
          Epoch2kDay.ofYMD(2023, 2, 15)
        }) {
      var i = Monthly.interval(d);
      assertEquals(i, Monthly.previous(Monthly.next(i)));
      assertEquals(i, Monthly.next(Monthly.previous(i)));
    }
  }

  @Test
  void partitionQuarterIntoThreeMonths() {
    var q1 = interval(2024, 1, 1, 2024, 3, 31);
    var parts = Monthly.partition(q1);
    assertEquals(3, parts.size());
    assertInterval(parts.get(0), 2024, 1, 1, 2024, 1, 31);
    assertInterval(parts.get(2), 2024, 3, 1, 2024, 3, 31);
  }

  @Test
  void partitionYearIntoTwelveMonths() {
    var y = Epoch2kInterval.ofYear(2024);
    assertEquals(12, Monthly.partition(y).size());
  }

  @Test
  void partitionReflexive() {
    var month = Monthly.interval(Epoch2kDay.ofYMD(2024, 6, 15));
    var parts = Monthly.partition(month);
    assertEquals(1, parts.size());
    assertEquals(month, parts.get(0));
  }

  @Test
  void partitionMisalignedParentThrows() {
    var misaligned = interval(2024, 1, 15, 2024, 3, 14);
    assertThrows(IllegalArgumentException.class, () -> Monthly.partition(misaligned));
  }

  @Test
  void membersOfQuarterEqualsPartition() {
    var q1 = interval(2024, 1, 1, 2024, 3, 31);
    assertEquals(Monthly.partition(q1), Monthly.membersOf(q1));
  }
}
