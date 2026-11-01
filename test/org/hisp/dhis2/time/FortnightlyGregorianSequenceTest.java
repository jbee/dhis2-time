package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.GregorianSequences.*;
import static org.hisp.dhis2.time.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class FortnightlyGregorianSequenceTest {

  @Test
  void intervalIsTwoWeeks() {
    // 2024-01-01 is Monday. BW1 of ISO-week-year 2024 is 2024-01-01 to 2024-01-14.
    var i = GT_.interval(Epoch2kDay.ofYMD(2024, 1, 1));
    assertEquals(14, i.lengthInDays());
    assertInterval(i, 2024, 1, 1, 2024, 1, 14);
  }

  @Test
  void secondWeekOfBiWeekMapsToSameBiWeek() {
    assertInterval(GT_.interval(Epoch2kDay.ofYMD(2024, 1, 8)), 2024, 1, 1, 2024, 1, 14);
  }

  @Test
  void thirdWeekStartsNewBiWeek() {
    assertInterval(GT_.interval(Epoch2kDay.ofYMD(2024, 1, 15)), 2024, 1, 15, 2024, 1, 28);
  }

  @Test
  void biWeekYearBoundaryFollowsIsoWeekYear() {
    // 2019-12-30 is Monday of ISO week 1 of 2020, so it belongs to BW1 of 2020.
    assertInterval(GT_.interval(Epoch2kDay.ofYMD(2019, 12, 30)), 2019, 12, 30, 2020, 1, 12);
  }

  @Test
  void week53In53WeekYearIsSingleWeekStub() {
    // 2020 has 53 ISO weeks. Week 53 runs 2020-12-28 to 2021-01-03.
    // BW27 is the 1-week stub.
    var i = GT_.interval(Epoch2kDay.ofYMD(2020, 12, 28));
    assertEquals(7, i.lengthInDays());
    assertInterval(i, 2020, 12, 28, 2021, 1, 3);
  }

  @Test
  void allDaysOfWeek53MapToSameStub() {
    var expected = GT_.interval(Epoch2kDay.ofYMD(2020, 12, 28));
    assertEquals(expected, GT_.interval(Epoch2kDay.ofYMD(2020, 12, 30)));
    assertEquals(expected, GT_.interval(Epoch2kDay.ofYMD(2021, 1, 1)));
    assertEquals(expected, GT_.interval(Epoch2kDay.ofYMD(2021, 1, 3)));
  }

  @Test
  void nextAfterStubJumpsToNextWeekYear() {
    var stub = GT_.interval(Epoch2kDay.ofYMD(2020, 12, 28));
    assertInterval(GT_.next(stub), 2021, 1, 4, 2021, 1, 17);
  }

  @Test
  void previousBeforeStubIsFullBiWeek() {
    var stub = GT_.interval(Epoch2kDay.ofYMD(2020, 12, 28));
    var prev = GT_.previous(stub);
    assertEquals(14, prev.lengthInDays());
    assertInterval(prev, 2020, 12, 14, 2020, 12, 27);
  }

  @Test
  void nextAndPreviousInvertAcrossStub() {
    var bw26 = GT_.interval(Epoch2kDay.ofYMD(2020, 12, 14));
    var stub = GT_.next(bw26);
    assertEquals(stub, GT_.previous(GT_.next(stub)));
    assertEquals(bw26, GT_.previous(stub));
  }

  @Test
  void biWeekContainsItsDay() {
    Epoch2kDay[] days = {
      Epoch2kDay.ofYMD(2020, 12, 21), Epoch2kDay.ofYMD(2020, 12, 28), Epoch2kDay.ofYMD(2021, 1, 3),
      Epoch2kDay.ofYMD(2021, 1, 4), Epoch2kDay.ofYMD(2024, 6, 15), Epoch2kDay.ofYMD(2025, 1, 1)
    };
    for (var d : days) {
      assertContains(GT_.interval(d), d);
    }
  }

  @Test
  void partitionsIsEmpty() {
    assertTrue(GT_.partitions().isEmpty());
  }

  @Test
  void aggregatesToContainsMonthQuarterYear() {
    assertEquals(Set.of(GM.noOffset(), GB.noOffset(), GQ.noOffset(), GS.noOffset(), GY.noOffset()), GT_.contributesTo());
  }

  @Test
  void partitionAlignedTwoBiWeekParentSucceeds() {
    var parent = interval(2024, 1, 1, 2024, 1, 28);
    var parts = GT_.partition(parent);
    assertEquals(2, parts.size());
  }

  @Test
  void partitionCalendarYearThrowsBecauseLastBiWeekStraddles() {
    var calendarYear = Epoch2kInterval.ofYear(2024);
    assertThrows(IllegalArgumentException.class, () -> GT_.partition(calendarYear));
  }

  @Test
  void membersOfCalendarYearAssignsBoundaryBiWeeksByMajority() {
    var calendarYear = Epoch2kInterval.ofYear(2024);
    var members = GT_.contributorsTo(calendarYear);
    for (var m : members) {
      int inside =
          Math.min(m.lastDay().daysSince2k(), calendarYear.lastDay().daysSince2k())
              - Math.max(m.firstDay().daysSince2k(), calendarYear.firstDay().daysSince2k())
              + 1;
      assertTrue(
          inside * 2 > m.lengthInDays(),
          () -> "member " + m + " has only " + inside + " of " + m.lengthInDays() + " days inside");
    }
  }

  @Test
  void fortnightlyStraddlingMonth50_50GoesToEarlierMonth() {
    // Bi-week starting Monday 2024-03-25 runs Mar 25 – Apr 7: 7 days in each month.
    var straddler = GT_.interval(Epoch2kDay.ofYMD(2024, 3, 25));
    assertEquals(14, straddler.lengthInDays());
    assertEquals(Epoch2kDay.ofYMD(2024, 3, 25), straddler.firstDay());
    assertEquals(Epoch2kDay.ofYMD(2024, 4, 7), straddler.lastDay());

    var march = GM_.interval(Epoch2kDay.ofYMD(2024, 3, 15));
    var april = GM_.interval(Epoch2kDay.ofYMD(2024, 4, 15));

    assertTrue(GT_.contributorsTo(march).contains(straddler),
        "tie should go to the earlier parent (March)");
    assertFalse(GT_.contributorsTo(april).contains(straddler),
        "tie should not also appear in April");
  }

  @Test
  void everyBiWeekIsAttributedToExactlyOneMonthAcross2024() {
    var year = Epoch2kInterval.ofYear(2024);
    var months = GM_.partition(year);
    var count = new java.util.HashMap<Epoch2kInterval, Integer>();

    for (var month : months) {
      for (var bw : GT_.contributorsTo(month)) {
        count.merge(bw, 1, Integer::sum);
      }
    }

    for (var e : count.entrySet()) {
      assertEquals(1, e.getValue(),
          () -> "bi-week " + e.getKey() + " attributed to " + e.getValue() + " months");
    }
  }
}
