package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.EthiopianCalendar.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EthiopianCalendarTest {

  @Test
  void anchor2000_01_01IsTahsas22_1992() {
    Epoch2kDay d = new Epoch2kDay(0);
    assertEquals(1992, yearInEC(d));
    assertEquals(4, monthInEC(d));       // Tahsas
    assertEquals(22, dayOfMonthInEC(d));
  }

  @Test
  void newYear1992Is1999_09_12() {
    assertEquals(-111, newYearOffsetInEC(1992));
  }

  @Test
  void newYear1993Is2000_09_11() {
    // 1992 is not leap (1992 % 4 == 0), so New Year 1993 is the 11th.
    assertEquals(254, newYearOffsetInEC(1993));
  }

  @Test
  void newYear1995Is2002_09_11() {
    // 1995 is leap (1995 % 4 == 3), but the leap day is in Pagume 1995,
    // so Meskerem 1995 itself is still the 11th.
    assertEquals(984, newYearOffsetInEC(1995));
  }

  @Test
  void newYear1996Is2003_09_12() {
    // 1995 was leap, so New Year 1996 shifts to the 12th.
    assertEquals(1350, newYearOffsetInEC(1996));
  }

  @Test
  void leapYearsFollowMod4Equals3() {
    assertTrue(isLeapYearInEC(1995));
    assertTrue(isLeapYearInEC(1999));
    assertFalse(isLeapYearInEC(1992));
    assertFalse(isLeapYearInEC(1993));
    assertFalse(isLeapYearInEC(1994));
    assertFalse(isLeapYearInEC(1996));
  }

  @Test
  void pagumeHas5Or6Days() {
    assertEquals(5, daysInMonthInEC(1992, 13));
    assertEquals(6, daysInMonthInEC(1995, 13));
    assertEquals(30, daysInMonthInEC(1995, 1));
    assertEquals(30, daysInMonthInEC(1995, 12));
  }

  @Test
  void roundTripKnownAnchors() {
    assertRoundTrip(1992, 4, 22, new Epoch2kDay(0));
    assertRoundTrip(2017, 1, 1, Epoch2kDay.ofYMD(2024, 9, 11));
    assertRoundTrip(2018, 11, 5, Epoch2kDay.ofYMD(2026, 7, 12));
    assertRoundTrip(1992, 1, 1, Epoch2kDay.ofYMD(1999, 9, 12));
  }

  @Test
  void everyDayOf1995RoundTrips() {
    int start = newYearOffsetInEC(1995);
    int end = newYearOffsetInEC(1996);
    assertEquals(366, end - start);
    for (int i = start; i < end; i++) {
      Epoch2kDay d = new Epoch2kDay(i);
      int y = yearInEC(d), m = monthInEC(d), dom = dayOfMonthInEC(d);
      assertEquals(d, toEpochDayInEC(y, m, dom), "round-trip failed for day " + i);
    }
  }

  @Test
  void monthIntervalForTahsas1992() {
    var i = toEpochIntervalOfEthiopianMonthContaining(new Epoch2kDay(0));
    assertEquals(toEpochDayInEC(1992, 4, 1), i.firstDay());
    assertEquals(toEpochDayInEC(1992, 4, 30), i.lastDay());
    assertEquals(30, i.lengthInDays());
  }

  @Test
  void monthIntervalForPagumeInLeapYear() {
    // Pagume 1995 (month 13) has 6 days.
    var i = toEpochIntervalOfEthiopianMonthContaining(toEpochDayInEC(1995, 13, 3));
    assertEquals(toEpochDayInEC(1995, 13, 1), i.firstDay());
    assertEquals(toEpochDayInEC(1995, 13, 6), i.lastDay());
    assertEquals(6, i.lengthInDays());
  }

  @Test
  void yearIntervalFor1992Has365Days() {
    var i = toEpochIntervalOfEthiopianYearContaining(new Epoch2kDay(0));
    assertEquals(toEpochDayInEC(1992, 1, 1), i.firstDay());
    assertEquals(toEpochDayInEC(1992, 13, 5), i.lastDay());
    assertEquals(365, i.lengthInDays());
  }

  @Test
  void yearIntervalFor1995Has366Days() {
    var i = toEpochIntervalOfEthiopianYearContaining(toEpochDayInEC(1995, 6, 15));
    assertEquals(toEpochDayInEC(1995, 1, 1), i.firstDay());
    assertEquals(toEpochDayInEC(1995, 13, 6), i.lastDay());
    assertEquals(366, i.lengthInDays());
  }

  private static void assertRoundTrip(int y, int m, int d, Epoch2kDay expected) {
    Epoch2kDay actual = toEpochDayInEC(y, m, d);
    assertEquals(expected, actual, () -> y + "-" + m + "-" + d);
    assertEquals(y, yearInEC(actual));
    assertEquals(m, monthInEC(actual));
    assertEquals(d, dayOfMonthInEC(actual));
  }
}