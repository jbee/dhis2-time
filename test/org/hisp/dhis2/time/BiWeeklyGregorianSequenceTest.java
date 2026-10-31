package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.GregorianSequences.*;
import static org.hisp.dhis2.time.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class BiWeeklyGregorianSequenceTest {

  @Test
  void intervalIsTwoWeeks() {
    // 2024-01-01 is Monday. BW1 of ISO-week-year 2024 is 2024-01-01 to 2024-01-14.
    var i = BiWeekly.interval(Epoch2kDay.of(2024, 1, 1));
    assertEquals(14, i.lengthInDays());
    assertInterval(i, 2024, 1, 1, 2024, 1, 14);
  }

  @Test
  void secondWeekOfBiWeekMapsToSameBiWeek() {
    assertInterval(BiWeekly.interval(Epoch2kDay.of(2024, 1, 8)), 2024, 1, 1, 2024, 1, 14);
  }

  @Test
  void thirdWeekStartsNewBiWeek() {
    assertInterval(BiWeekly.interval(Epoch2kDay.of(2024, 1, 15)), 2024, 1, 15, 2024, 1, 28);
  }

  @Test
  void biWeekYearBoundaryFollowsIsoWeekYear() {
    // 2019-12-30 is Monday of ISO week 1 of 2020, so it belongs to BW1 of 2020.
    assertInterval(BiWeekly.interval(Epoch2kDay.of(2019, 12, 30)), 2019, 12, 30, 2020, 1, 12);
  }

  @Test
  void week53In53WeekYearIsSingleWeekStub() {
    // 2020 has 53 ISO weeks. Week 53 runs 2020-12-28 to 2021-01-03.
    // BW27 is the 1-week stub.
    var i = BiWeekly.interval(Epoch2kDay.of(2020, 12, 28));
    assertEquals(7, i.lengthInDays());
    assertInterval(i, 2020, 12, 28, 2021, 1, 3);
  }

  @Test
  void allDaysOfWeek53MapToSameStub() {
    var expected = BiWeekly.interval(Epoch2kDay.of(2020, 12, 28));
    assertEquals(expected, BiWeekly.interval(Epoch2kDay.of(2020, 12, 30)));
    assertEquals(expected, BiWeekly.interval(Epoch2kDay.of(2021, 1, 1)));
    assertEquals(expected, BiWeekly.interval(Epoch2kDay.of(2021, 1, 3)));
  }

  @Test
  void nextAfterStubJumpsToNextWeekYear() {
    var stub = BiWeekly.interval(Epoch2kDay.of(2020, 12, 28));
    assertInterval(BiWeekly.next(stub), 2021, 1, 4, 2021, 1, 17);
  }

  @Test
  void previousBeforeStubIsFullBiWeek() {
    var stub = BiWeekly.interval(Epoch2kDay.of(2020, 12, 28));
    var prev = BiWeekly.previous(stub);
    assertEquals(14, prev.lengthInDays());
    assertInterval(prev, 2020, 12, 14, 2020, 12, 27);
  }

  @Test
  void nextAndPreviousInvertAcrossStub() {
    var bw26 = BiWeekly.interval(Epoch2kDay.of(2020, 12, 14));
    var stub = BiWeekly.next(bw26);
    assertEquals(stub, BiWeekly.previous(BiWeekly.next(stub)));
    assertEquals(bw26, BiWeekly.previous(stub));
  }

  @Test
  void biWeekContainsItsDay() {
    Epoch2kDay[] days = {
      Epoch2kDay.of(2020, 12, 21), Epoch2kDay.of(2020, 12, 28), Epoch2kDay.of(2021, 1, 3),
      Epoch2kDay.of(2021, 1, 4), Epoch2kDay.of(2024, 6, 15), Epoch2kDay.of(2025, 1, 1)
    };
    for (var d : days) {
      assertContains(BiWeekly.interval(d), d);
    }
  }

  @Test
  void partitionsIsEmpty() {
    assertTrue(BiWeekly.partitions().isEmpty());
  }

  @Test
  void aggregatesToContainsMonthQuarterYear() {
    assertEquals(Set.of(Month, Quarter, Year), BiWeekly.aggregatesTo());
  }

  @Test
  void partitionAlignedTwoBiWeekParentSucceeds() {
    var parent = interval(2024, 1, 1, 2024, 1, 28);
    var parts = BiWeekly.partition(parent);
    assertEquals(2, parts.size());
  }

  @Test
  void partitionCalendarYearThrowsBecauseLastBiWeekStraddles() {
    var calendarYear = Epoch2kInterval.ofYear(2024);
    assertThrows(IllegalArgumentException.class, () -> BiWeekly.partition(calendarYear));
  }

  @Test
  void membersOfCalendarYearAssignsBoundaryBiWeeksByMajority() {
    var calendarYear = Epoch2kInterval.ofYear(2024);
    var members = BiWeekly.membersOf(calendarYear);
    for (var m : members) {
      int inside =
          Math.min(m.last().daysSince2k(), calendarYear.last().daysSince2k())
              - Math.max(m.first().daysSince2k(), calendarYear.first().daysSince2k())
              + 1;
      assertTrue(
          inside * 2 > m.lengthInDays(),
          () -> "member " + m + " has only " + inside + " of " + m.lengthInDays() + " days inside");
    }
  }
}
