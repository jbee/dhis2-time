package org.hisp.dhis2.time;

import static java.lang.Integer.parseInt;
import static org.hisp.dhis2.time.Epoch2kDay.DayOfWeek.*;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.*;
import static org.hisp.dhis2.time.Epoch2kDay.ofJanuary1stInYear;
import static org.hisp.dhis2.time.Epoch2kDay.ofWeek1InYear;
import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TestEpoch2kDay {

  @Test
  void testYear() {
    assertDayEquals("2000-01-01", new Epoch2kDay(0));
    assertDayEquals("2000-01-02", new Epoch2kDay(1));
    assertDayEquals("2000-01-31", new Epoch2kDay(30));
    assertDayEquals("2000-02-29", new Epoch2kDay(30 + 29));
    assertDayEquals("2000-02-01", new Epoch2kDay(31));
    assertDayEquals("2000-12-31", new Epoch2kDay(365));
    assertDayEquals("2001-01-01", new Epoch2kDay(366));
    assertDayEquals("2001-12-31", new Epoch2kDay(365 + 365));
    assertDayEquals("2002-01-01", new Epoch2kDay(366 + 365));
  }

  @Test
  void testEpoch() {
    assertDayEquals("1970-01-02", Epoch2kDay.today(() -> 1));
  }

  // ── today(LongSupplier) ─────────────────────────────────────────────

  @Test
  void todayAt2kMidnightIsDayZero() {
    long millis2k = new Epoch2kDay(0).toUTCMillis();
    assertDayEquals("2000-01-01", Epoch2kDay.today(() -> millis2k));
  }

  @Test
  void todayOneDayAfter2kMidnightIsDayOne() {
    long millis2k = new Epoch2kDay(1).toUTCMillis();
    assertDayEquals("2000-01-02", Epoch2kDay.today(() -> millis2k));
  }

  @Test
  void todayIgnoresSubDayRemainder() {
    // 12 hours past 2k midnight is still 2000-01-01
    long noon2k = new Epoch2kDay(0).toUTCMillis() + 12L * 60 * 60 * 1000;
    assertDayEquals("2000-01-01", Epoch2kDay.today(() -> noon2k));
  }

  // ── Construction ────────────────────────────────────────────────────

  @Test
  void ofJanuary1stInYearRoundTrips() {
    for (int y : new int[] {1970, 2000, 2024, 2099}) {
      assertDayEquals("%04d-01-01".formatted(y), ofJanuary1stInYear(y));
    }
  }

  @Test
  void ofYMDForFirstOfMonth() {
    assertDayEquals("2024-01-01", ofYMD(2024, 1, 1));
    assertDayEquals("2024-03-01", ofYMD(2024, 3, 1));
    assertDayEquals("2024-12-01", ofYMD(2024, 12, 1));
  }

  @Test
  void ofYMDForLastOfMonth() {
    assertDayEquals("2024-01-31", ofYMD(2024, 1, 31));
    assertDayEquals("2024-02-29", ofYMD(2024, 2, 29));
    assertDayEquals("2023-02-28", ofYMD(2023, 2, 28));
    assertDayEquals("2024-12-31", ofYMD(2024, 12, 31));
  }

  @Test
  void ofWeek1InYearForYearStartingMonday() {
    // 2024-01-01 is a Monday and contains Jan 4 → week 1 starts 2024-01-01.
    assertDayEquals("2024-01-01", ofWeek1InYear(2024, Monday));
  }

  @Test
  void ofWeek1InYearForYearStartingSunday() {
    // 2023-01-01 is Sunday; week containing Jan 4 starts Mon 2023-01-02.
    assertDayEquals("2023-01-02", ofWeek1InYear(2023, Monday));
  }

  @Test
  void ofWeek1InYearIsSameDayOfWeekAsDay1() {
    for (int y : new int[] {2000, 2023, 2024, 2025, 2099}) {
      for (Epoch2kDay.DayOfWeek d : Epoch2kDay.DayOfWeek.values()) {
        assertEquals(0, ofWeek1InYear(y, d).dayOfWeekOffset(d),
            () -> "year " + y + " day1 " + d);
      }
    }
  }

  @Test
  void ofWeek1InYearContainingJan4() {
    for (int y : new int[] {2020, 2021, 2022, 2023, 2024, 2025, 2026}) {
      var jan4 = ofYMD(y, 1, 4);
      var week1 = ofWeek1InYear(y, Monday);
      assertFalse(week1.after(jan4), "week1 must start on or before Jan 4");
      assertTrue(week1.plusDays(6).notBefore(jan4), "week1 must extend through Jan 4");
    }
  }

  // ── Comparison ──────────────────────────────────────────────────────

  @Test
  void comparisonFollowsDayOrder() {
    Epoch2kDay a = new Epoch2kDay(5), b = new Epoch2kDay(5), c = new Epoch2kDay(6);
    assertFalse(a.before(b));
    assertTrue(a.notBefore(b));
    assertFalse(a.after(b));
    assertTrue(a.notAfter(b));
    assertTrue(a.before(c));
    assertFalse(a.after(c));
    assertFalse(c.before(a));
    assertTrue(c.after(a));
  }

  // ── toUTCMillis ─────────────────────────────────────────────────────

  @Test
  void utcMillisIsOffsetFromEpoch2k() {
    long millis2k = 946_684_800_000L;
    assertEquals(millis2k, new Epoch2kDay(0).toUTCMillis());
    assertEquals(millis2k + 86_400_000L, new Epoch2kDay(1).toUTCMillis());
    assertEquals(millis2k - 86_400_000L, new Epoch2kDay(-1).toUTCMillis());
  }

  // ── Arithmetic ──────────────────────────────────────────────────────

  @Test
  void plusAndMinusDays() {
    var d = ofYMD(2024, 3, 15);
    assertDayEquals("2024-03-16", d.plusDays(1));
    assertDayEquals("2024-03-22", d.plusDays(7));
    assertDayEquals("2024-03-14", d.minusDays(1));
    assertDayEquals("2024-03-08", d.minusDays(7));
    assertEquals(d, d.plusDays(0));
    assertEquals(d, d.plusDays(5).minusDays(5));
  }

  @Test
  void yesterdayAndTomorrowCrossYearBoundary() {
    assertDayEquals("2024-12-31", ofYMD(2025, 1, 1).yesterday());
    assertDayEquals("2025-01-01", ofYMD(2024, 12, 31).tomorrow());
    assertDayEquals("2025-01-01", ofYMD(2024, 12, 31).tomorrow());
  }

  @Test
  void plusMonthsPreservesDayOfMonth() {
    var d = ofYMD(2024, 3, 15);
    assertDayEquals("2024-04-15", d.plusMonths(1));
    assertDayEquals("2024-06-15", d.plusMonths(3));
    assertDayEquals("2025-03-15", d.plusMonths(12));
    assertDayEquals("2024-02-15", d.plusMonths(-1));
    assertDayEquals("2023-03-15", d.plusMonths(-12));
    assertEquals(d, d.plusMonths(0));
  }

  @Test
  void plusMonthsCrossesYearBoundaryInBothDirections() {
    assertDayEquals("2025-02-15", ofYMD(2024, 11, 15).plusMonths(3));
    assertDayEquals("2023-11-15", ofYMD(2024, 1, 15).plusMonths(-2));
  }

  @Test
  void plusMonthsIsReversibleForMidMonthDays() {
    var d = ofYMD(2024, 3, 15);
    assertEquals(d, d.plusMonths(5).minusMonths(5));
    assertEquals(d, d.minusMonths(7).plusMonths(7));
  }

  @Test
  void plusYears() {
    var d = ofYMD(2024, 3, 15);
    assertDayEquals("2025-03-15", d.plusYears(1));
    assertDayEquals("2023-03-15", d.plusYears(-1));
    assertDayEquals("2034-03-15", d.plusYears(10));
    assertEquals(d, d.plusYears(0));
  }

  @Test
  void plusYearsClampsLeapDayToFeb28() {
    // 2024 is leap, 2025 is not — Feb 29 + 12 months lands on Feb 28, 2025.
    assertDayEquals("2025-02-28", ofYMD(2024, 2, 29).plusYears(1));
  }

  @Test
  void plusMonthsClampsDayToShorterTargetMonth() {
    assertDayEquals("2024-04-30", ofYMD(2024, 3, 31).plusMonths(1));
    assertDayEquals("2024-02-29", ofYMD(2024, 3, 31).minusMonths(1));
    assertDayEquals("2023-02-28", ofYMD(2023, 3, 31).minusMonths(1));
    assertDayEquals("2024-06-30", ofYMD(2024, 5, 31).plusMonths(1));
  }

  @Test
  void plusMonthsIsReversibleForSafeDays() {
    for (var d : new Epoch2kDay[] {ofYMD(2024, 1, 15), ofYMD(2024, 6, 28), ofYMD(2023, 12, 1)}) {
      for (int n = -24; n <= 24; n++) {
        assertEquals(d, d.plusMonths(n).minusMonths(n), d + " plus " + n + " then minus " + n);
      }
    }
  }

  // ── Field accessors ─────────────────────────────────────────────────

  @Test
  void yearMonthDayAccessorsAreZeroBasedExceptYear() {
    var d = ofYMD(2024, 3, 15);
    assertEquals(2024, d.year());
    assertEquals(2, d.monthOffset());
    assertEquals(14, d.dayOfMonthOffset());
    assertEquals(74, d.dayOfYearOffset());
  }

  @Test
  void dayOfYearBoundaries() {
    assertEquals(0, ofYMD(2024, 1, 1).dayOfYearOffset());
    assertEquals(364, ofYMD(2023, 12, 31).dayOfYearOffset());
    assertEquals(365, ofYMD(2024, 12, 31).dayOfYearOffset());
  }

  @Test
  void day1InYearIsJanuary1st() {
    assertDayEquals("2024-01-01", ofYMD(2024, 3, 15).day1InYear());
    assertDayEquals("2024-01-01", ofYMD(2024, 1, 1).day1InYear());
    assertDayEquals("2024-01-01", ofYMD(2024, 12, 31).day1InYear());
  }

  @Test
  void day1InMonthUsesDayYear() {
    var d = ofYMD(2024, 3, 15);
    assertDayEquals("2024-01-01", d.day1InMonth(January));
    assertDayEquals("2024-03-01", d.day1InMonth(March));
    assertDayEquals("2024-07-01", d.day1InMonth(July));
    assertDayEquals("2024-12-01", d.day1InMonth(December));
  }

  // ── Day of week ─────────────────────────────────────────────────────

  @Test
  void dayOfWeekFor2000_01_01WhichIsSaturday() {
    assertEquals(0, new Epoch2kDay(0).dayOfWeekOffset(Saturday));
    assertEquals(6, new Epoch2kDay(0).dayOfWeekOffset(Sunday));
    assertEquals(5, new Epoch2kDay(0).dayOfWeekOffset(Monday));
  }

  @Test
  void dayOfWeekForKnownMondayIn2024() {
    var monday = ofYMD(2024, 3, 11); // known Monday
    assertEquals(0, monday.dayOfWeekOffset(Monday));
    assertEquals(6, monday.dayOfWeekOffset(Tuesday));
    assertEquals(5, monday.dayOfWeekOffset(Wednesday));
    assertEquals(4, monday.dayOfWeekOffset(Thursday));
    assertEquals(3, monday.dayOfWeekOffset(Friday));
    assertEquals(2, monday.dayOfWeekOffset(Saturday));
    assertEquals(1, monday.dayOfWeekOffset(Sunday));
  }

  @Test
  void dayOfWeekForKnownSundayIn2024() {
    var sunday = ofYMD(2024, 3, 17); // known Sunday
    assertEquals(0, sunday.dayOfWeekOffset(Sunday));
    assertEquals(6, sunday.dayOfWeekOffset(Monday));
  }

  // ── nextOrSame / previousOrSame ─────────────────────────────────────

  @Test
  void nextOrSameOnSameDayReturnsSelf() {
    var monday = ofYMD(2024, 3, 11);
    assertEquals(monday, monday.nextOrSame(Monday));
    assertEquals(monday, monday.previousOrSame(Monday));
  }

  @Test
  void nextOrSameMovesForward() {
    var wed = ofYMD(2024, 3, 13);
    assertDayEquals("2024-03-18", wed.nextOrSame(Monday));
    assertDayEquals("2024-03-16", wed.nextOrSame(Saturday));
  }

  @Test
  void previousOrSameMovesBackward() {
    var wed = ofYMD(2024, 3, 13);
    assertDayEquals("2024-03-11", wed.previousOrSame(Monday));
    assertDayEquals("2024-03-09", wed.previousOrSame(Saturday));
  }

  // ── Containing intervals ────────────────────────────────────────────

  @Test
  void containingWeekMondayAnchored() {
    var i = ofYMD(2024, 3, 15).containingWeek(); // Friday
    assertDayEquals("2024-03-11", i.firstDay());
    assertDayEquals("2024-03-17", i.lastDay());
    assertEquals(7, i.lengthInDays());
  }

  @Test
  void containingWeekSundayAnchored() {
    var i = ofYMD(2024, 3, 15).containingWeek(Sunday);
    assertDayEquals("2024-03-10", i.firstDay());
    assertDayEquals("2024-03-16", i.lastDay());
  }

  @Test
  void containingMonthLeapFebruary() {
    var i = ofYMD(2024, 2, 15).containingMonth();
    assertDayEquals("2024-02-01", i.firstDay());
    assertDayEquals("2024-02-29", i.lastDay());
    assertEquals(29, i.lengthInDays());
  }

  @Test
  void containingMonthNonLeapFebruary() {
    var i = ofYMD(2023, 2, 15).containingMonth();
    assertEquals(28, i.lengthInDays());
  }

  @Test
  void containingYearCalendar() {
    var i = ofYMD(2024, 3, 15).containingYear();
    assertDayEquals("2024-01-01", i.firstDay());
    assertDayEquals("2024-12-31", i.lastDay());
    assertEquals(366, i.lengthInDays());
  }

  @Test
  void containingYearStartingJulyBeforeJuly() {
    var i = ofYMD(2024, 3, 15).containingYear(July);
    assertDayEquals("2023-07-01", i.firstDay());
    assertDayEquals("2024-06-30", i.lastDay());
  }

  @Test
  void containingYearStartingJulyAfterJuly() {
    var i = ofYMD(2024, 8, 15).containingYear(July);
    assertDayEquals("2024-07-01", i.firstDay());
    assertDayEquals("2025-06-30", i.lastDay());
  }

  @Test
  void containingYearStartingJanuaryEqualsCalendar() {
    var d = ofYMD(2024, 3, 15);
    assertEquals(d.containingYear(), d.containingYear(January));
  }

  // ── yearCycleStart ──────────────────────────────────────────────────

  @Test
  void yearCycleStartForJanuaryStart() {
    assertDayEquals("2024-01-01", ofYMD(2024, 3, 15).yearCycleStart(January));
  }

  @Test
  void yearCycleStartForJulyStartRollsBack() {
    assertDayEquals("2023-07-01", ofYMD(2024, 3, 15).yearCycleStart(July));
  }

  @Test
  void yearCycleStartForJulyStartDoesNotRollBackAfterJuly() {
    assertDayEquals("2024-07-01", ofYMD(2024, 8, 15).yearCycleStart(July));
  }

  // ── Quarters ────────────────────────────────────────────────────────

  @Test
  void containingQuarterCalendar() {
    assertDayEquals("2024-01-01", ofYMD(2024, 2, 15).containingQuarter(January).firstDay());
    assertDayEquals("2024-03-31", ofYMD(2024, 2, 15).containingQuarter(January).lastDay());
    assertDayEquals("2024-10-01", ofYMD(2024, 11, 15).containingQuarter(January).firstDay());
    assertDayEquals("2024-12-31", ofYMD(2024, 11, 15).containingQuarter(January).lastDay());
  }

  @Test
  void containingQuarterFiscalJulyForAugust() {
    // Q1 of FY 2024/25 = Jul-Sep 2024
    var i = ofYMD(2024, 8, 15).containingQuarter(July);
    assertDayEquals("2024-07-01", i.firstDay());
    assertDayEquals("2024-09-30", i.lastDay());
  }

  @Test
  void containingQuarterFiscalJulyForJanuary() {
    // Q3 of FY 2023/24 = Jan-Mar 2024
    var i = ofYMD(2024, 1, 15).containingQuarter(July);
    assertDayEquals("2024-01-01", i.firstDay());
    assertDayEquals("2024-03-31", i.lastDay());
  }

  @Test
  void ofYMDRejectsAllInvalidMonthDayCombinations() {
    assertThrows(IllegalArgumentException.class, () -> Epoch2kDay.ofYMD(2024, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> Epoch2kDay.ofYMD(2024, 13, 1));
    assertThrows(IllegalArgumentException.class, () -> Epoch2kDay.ofYMD(2024, 4, 31));
    assertThrows(IllegalArgumentException.class, () -> Epoch2kDay.ofYMD(2023, 2, 29));
    assertThrows(IllegalArgumentException.class, () -> Epoch2kDay.ofYMD(2024, 6, 0));
    assertThrows(IllegalArgumentException.class, () -> Epoch2kDay.ofYMD(2024, 6, 31));
  }

  @Test
  void ofYMDAcceptsAllValidMonthDayCombinations() {
    // Every month's boundary days must be accepted.
    for (int m = 1; m <= 12; m++) {
      Epoch2kDay.ofYMD(2024, m, 1);                    // 1st always valid
      Epoch2kDay.ofYMD(2024, m, Epoch2kDay.daysInMonth(2024, m - 1)); // last always valid
    }
    // Leap day specifically.
    Epoch2kDay.ofYMD(2024, 2, 29);
    // Year-end / year-start boundary.
    Epoch2kDay.ofYMD(2024, 12, 31);
  }

  @Test
  void containingBiMonthJanuaryStart() {
    var janFeb = ofYMD(2024, 1, 15).containingBiMonth(January);
    assertEquals(ofYMD(2024, 1, 1), janFeb.firstDay());
    assertEquals(ofYMD(2024, 2, 29), janFeb.lastDay());

    var marApr = ofYMD(2024, 3, 15).containingBiMonth(January);
    assertEquals(ofYMD(2024, 3, 1), marApr.firstDay());
    assertEquals(ofYMD(2024, 4, 30), marApr.lastDay());
  }

  @Test
  void containingSixMonthJanuaryStart() {
    var s1 = ofYMD(2024, 3, 15).containingSixMonth(January);
    assertEquals(ofYMD(2024, 1, 1), s1.firstDay());
    assertEquals(ofYMD(2024, 6, 30), s1.lastDay());

    var s2 = ofYMD(2024, 9, 15).containingSixMonth(January);
    assertEquals(ofYMD(2024, 7, 1), s2.firstDay());
    assertEquals(ofYMD(2024, 12, 31), s2.lastDay());
  }

  @Test
  void containingSixMonthAprilStart() {
    // S1 of April-start cycle: Apr-Sep
    var s1 = ofYMD(2024, 5, 15).containingSixMonth(April);
    assertEquals(ofYMD(2024, 4, 1), s1.firstDay());
    assertEquals(ofYMD(2024, 9, 30), s1.lastDay());

    // S2 of April-start cycle: Oct-Mar, spans year boundary
    var s2 = ofYMD(2024, 11, 15).containingSixMonth(April);
    assertEquals(ofYMD(2024, 10, 1), s2.firstDay());
    assertEquals(ofYMD(2025, 3, 31), s2.lastDay());
  }

  @Test
  void containingBiMonthAprilStart() {
    // April-start bi-months: Apr-May, Jun-Jul, Aug-Sep, Oct-Nov, Dec-Jan, Feb-Mar
    var aprMay = ofYMD(2024, 5, 15).containingBiMonth(April);
    assertEquals(ofYMD(2024, 4, 1), aprMay.firstDay());
    assertEquals(ofYMD(2024, 5, 31), aprMay.lastDay());
  }

  // ── Helper ──────────────────────────────────────────────────────────

  private static void assertDayEquals(String expected, Epoch2kDay actual) {
    Epoch2kDay.OffsetYMD offset = actual.offset();
    assertEquals(expected, offset.toString());
    String[] yearMonthDay = expected.split("-");
    assertEquals(parseInt(yearMonthDay[0]), offset.years());
    assertEquals(parseInt(yearMonthDay[1]) - 1, offset.months());
    assertEquals(parseInt(yearMonthDay[2]) - 1, offset.days());
  }
}