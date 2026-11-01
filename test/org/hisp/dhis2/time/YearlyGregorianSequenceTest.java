package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.GregorianSequences.*;
import static org.hisp.dhis2.time.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class YearlyGregorianSequenceTest {

  @Test
  void calendarYearIsFullYear() {
    assertInterval(GY_.interval(Epoch2kDay.ofYMD(2024, 6, 15)), 2024, 1, 1, 2024, 12, 31);
  }

  @Test
  void leapYearHas366Days() {
    assertEquals(366, GY_.interval(Epoch2kDay.ofYMD(2024, 1, 1)).lengthInDays());
  }

  @Test
  void nonLeapYearHas365Days() {
    assertEquals(365, GY_.interval(Epoch2kDay.ofYMD(2023, 1, 1)).lengthInDays());
  }

  @Test
  void julyAnchoredYearStartsJulyFirst() {
    var y = yearly(Epoch2kDay.MonthOfYear.July);
    assertInterval(y.interval(Epoch2kDay.ofYMD(2024, 8, 15)), 2024, 7, 1, 2025, 6, 30);
  }

  @Test
  void julyAnchoredYearForJanuaryBelongsToPriorCycle() {
    var y = yearly(Epoch2kDay.MonthOfYear.July);
    assertInterval(y.interval(Epoch2kDay.ofYMD(2024, 1, 15)), 2023, 7, 1, 2024, 6, 30);
  }

  @Test
  void nextIsFollowingYear() {
    assertInterval(
        GY_.next(GY_.interval(Epoch2kDay.ofYMD(2024, 6, 15))), 2025, 1, 1, 2025, 12, 31);
  }

  @Test
  void nextAndPreviousAreInverses() {
    var i = GY_.interval(Epoch2kDay.ofYMD(2024, 6, 15));
    assertEquals(i, GY_.previous(GY_.next(i)));
    assertEquals(i, GY_.next(GY_.previous(i)));
  }

  @Test
  void partitionReflexive() {
    var year = GY_.interval(Epoch2kDay.ofYMD(2024, 6, 15));
    var parts = GY_.partition(year);
    assertEquals(1, parts.size());
    assertEquals(year, parts.get(0));
  }

  @Test
  void partitionTwoYearRangeIntoTwo() {
    var twoYears = Epoch2kInterval.ofYearRange(2024, 2025);
    var parts = GY_.partition(twoYears);
    assertEquals(2, parts.size());
    assertEquals(Epoch2kDay.ofYMD(2024, 1, 1), parts.get(0).firstDay());
    assertEquals(Epoch2kDay.ofYMD(2025, 12, 31), parts.get(1).lastDay());
  }

  @Test
  void partitionMonthIntervalThrowsBecauseMisaligned() {
    var month = interval(2024, 6, 1, 2024, 6, 30);
    assertThrows(IllegalArgumentException.class, () -> GY_.partition(month));
  }

  @Test
  void partitionsAndAggregatesToAreEmpty() {
    assertTrue(GY_.partitions().isEmpty());
    assertTrue(GY_.contributesTo().isEmpty());
  }

  @Test
  void yearlyWithOffsetKeepsCalendarSystem() {
    var y = yearly(Epoch2kDay.MonthOfYear.July);
    assertEquals('G', y.key().system());
    assertEquals('7', y.key().offset());
  }

  @Test
  void fiscalYearlyUsesFiscalSystem() {
    var y = fiscalYearly(Epoch2kDay.MonthOfYear.July);
    assertEquals('F', y.key().system());
    assertEquals('Y', y.key().interval());
    assertEquals('7', y.key().offset());
  }
}
