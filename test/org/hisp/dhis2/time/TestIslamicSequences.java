package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.IslamicCalendar.day;
import static org.hisp.dhis2.time.IslamicSequences.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class TestIslamicSequences {

  @Test
  void dailyIntervalIsSingleDay() {
    var i = Daily.interval(day(1445, 9, 1));
    assertEquals(1, i.lengthInDays());
    assertEquals(day(1445, 9, 1), i.first());
  }

  @Test
  void monthlyIntervalContainsItsDay() {
    for (Epoch2kDay d : new Epoch2kDay[] {
        day(1440, 1, 1), day(1445, 9, 15), day(1450, 12, 29)}) {
      assertTrue(Monthly.interval(d).contains(d));
    }
  }

  @Test
  void yearlyIntervalContainsItsDay() {
    for (Epoch2kDay d : new Epoch2kDay[] {
        day(1440, 1, 1), day(1445, 9, 15), day(1450, 12, 29)}) {
      assertTrue(Yearly.interval(d).contains(d));
    }
  }

  @Test
  void monthlyNextIsFollowingIslamicMonth() {
    var ramadan = Monthly.interval(day(1445, 9, 1));
    var shawwal = Monthly.next(ramadan);
    assertEquals(day(1445, 10, 1), shawwal.first());
    assertEquals(day(1445, 10, 29), shawwal.last());
  }

  @Test
  void monthlyNextFromDhuAlHijjahCrossesIslamicYear() {
    var last = Monthly.interval(day(1445, 12, 1));
    var next = Monthly.next(last);
    assertEquals(day(1446, 1, 1), next.first());
  }

  @Test
  void yearlyNextIsFollowingIslamicYear() {
    var y1445 = Yearly.interval(day(1445, 6, 15));
    var y1446 = Yearly.next(y1445);
    assertEquals(day(1446, 1, 1), y1446.first());
  }

  @Test
  void nextAndPreviousAreInverses() {
    var month = Monthly.interval(day(1447, 9, 1));
    assertEquals(month, Monthly.previous(Monthly.next(month)));
    assertEquals(month, Monthly.next(Monthly.previous(month)));

    var year = Yearly.interval(day(1447, 6, 15));
    assertEquals(year, Yearly.previous(Yearly.next(year)));
    assertEquals(year, Yearly.next(Yearly.previous(year)));
  }

  @Test
  void dailyPartitionOfIslamicMonthYields29Or30Days() {
    var ramadan1446 = Monthly.interval(day(1446, 9, 1));
    assertEquals(29, Daily.partition(ramadan1446).size());

    var ramadan1447 = Monthly.interval(day(1447, 9, 1));
    assertEquals(30, Daily.partition(ramadan1447).size());
  }

  @Test
  void monthlyPartitionOfIslamicYearYields12Months() {
    var year = Yearly.interval(day(1447, 6, 15));
    var months = Monthly.partition(year);
    assertEquals(12, months.size());
    assertEquals(day(1447, 1, 1), months.get(0).first());
    assertEquals(day(1447, 12, IslamicCalendar.daysInMonth(1447, 12)), months.get(11).last());
  }

  @Test
  void aggregatesToOnlyIslamicFamilies() {
    assertEquals(Set.of(IslamicMonth, IslamicYear), Daily.contributesTo());
    assertFalse(Daily.contributesTo(GregorianSequences.Month));
    assertFalse(Daily.contributesTo(EthiopianSequences.Month));
  }

  @Test
  void partitionAcrossCalendarBoundaryThrows() {
    // A Gregorian calendar year is not aligned to Islamic month boundaries.
    var gregYear = Epoch2kInterval.ofYear(2024);
    assertThrows(IllegalArgumentException.class, () -> Monthly.partition(gregYear));
  }
}