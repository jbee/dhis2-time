package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.EthiopianCalendar.toEpochDayInEC;
import static org.hisp.dhis2.time.EthiopianSequences.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class TestEthiopianSequences {

  @Test
  void dailyIntervalIsSingleDay() {
    var i = Daily.interval(toEpochDayInEC(1992, 4, 22));
    assertEquals(1, i.lengthInDays());
    assertEquals(toEpochDayInEC(1992, 4, 22), i.first());
    assertEquals(toEpochDayInEC(1992, 4, 22), i.last());
  }

  @Test
  void monthlyIntervalContainsItsDay() {
    for (Epoch2kDay d : new Epoch2kDay[] {
        toEpochDayInEC(1992, 4, 22), toEpochDayInEC(1995, 13, 3), toEpochDayInEC(2017, 1, 1)}) {
      assertTrue(Monthly.interval(d).contains(d));
    }
  }

  @Test
  void yearlyIntervalContainsItsDay() {
    for (Epoch2kDay d : new Epoch2kDay[] {
        toEpochDayInEC(1992, 4, 22), toEpochDayInEC(1995, 13, 3), toEpochDayInEC(2017, 1, 1)}) {
      assertTrue(Yearly.interval(d).contains(d));
    }
  }

  @Test
  void monthlyNextIsFollowingEthiopianMonth() {
    var tahsas = Monthly.interval(new Epoch2kDay(0));  // Tahsas 1992
    var tir = Monthly.next(tahsas);                    // Tir 1992
    assertEquals(toEpochDayInEC(1992, 5, 1), tir.first());
    assertEquals(toEpochDayInEC(1992, 5, 30), tir.last());
  }

  @Test
  void monthlyNextFromPagumeCrossesEthiopianYear() {
    // Pagume 1995 is the last month; next is Meskerem 1996.
    var pagume = Monthly.interval(toEpochDayInEC(1995, 13, 1));
    var meskerem = Monthly.next(pagume);
    assertEquals(toEpochDayInEC(1996, 1, 1), meskerem.first());
  }

  @Test
  void yearlyNextIsFollowingEthiopianYear() {
    var y1992 = Yearly.interval(new Epoch2kDay(0));
    var y1993 = Yearly.next(y1992);
    assertEquals(toEpochDayInEC(1993, 1, 1), y1993.first());
    assertEquals(toEpochDayInEC(1993, 13, 5), y1993.last());
  }

  @Test
  void nextAndPreviousAreInverses() {
    var month = Monthly.interval(toEpochDayInEC(1995, 13, 3));
    assertEquals(month, Monthly.previous(Monthly.next(month)));
    assertEquals(month, Monthly.next(Monthly.previous(month)));

    var year = Yearly.interval(toEpochDayInEC(1995, 6, 15));
    assertEquals(year, Yearly.previous(Yearly.next(year)));
    assertEquals(year, Yearly.next(Yearly.previous(year)));
  }

  @Test
  void dailyPartitionOfEthiopianMonthYields30Days() {
    var tahsas = Monthly.interval(new Epoch2kDay(0));
    assertEquals(30, Daily.partition(tahsas).size());
  }

  @Test
  void dailyPartitionOfLeapPagumeYields6Days() {
    var pagume = Monthly.interval(toEpochDayInEC(1995, 13, 1));
    var days = Daily.partition(pagume);
    assertEquals(6, days.size());
    assertEquals(toEpochDayInEC(1995, 13, 1), days.get(0).first());
    assertEquals(toEpochDayInEC(1995, 13, 6), days.get(5).last());
  }

  @Test
  void monthlyPartitionOfEthiopianYearYields13Months() {
    var year = Yearly.interval(toEpochDayInEC(1995, 6, 15));
    var months = Monthly.partition(year);
    assertEquals(13, months.size());
    assertEquals(toEpochDayInEC(1995, 1, 1), months.get(0).first());
    assertEquals(toEpochDayInEC(1995, 13, 6), months.get(12).last());
  }

  @Test
  void dailyAggregatesToEthiopianMonthAndYearOnly() {
    assertEquals(Set.of(Month, Year), Daily.contributesTo());
    assertFalse(Daily.contributesTo(GregorianSequences.Month));
    assertFalse(Daily.contributesTo(GregorianSequences.Year));
  }

  @Test
  void monthlyAggregatesToEthiopianYearOnly() {
    assertEquals(Set.of(Year), Monthly.contributesTo());
  }

  @Test
  void yearlyAggregatesToNothing() {
    assertTrue(Yearly.contributesTo().isEmpty());
  }

  @Test
  void ethiopianAndGregorianYearlyDoNotInteract() {
    var ethYear = Yearly.interval(toEpochDayInEC(1995, 6, 15));
    var gregYear = GregorianSequences.Yearly.interval(Epoch2kDay.ofYMD(2002, 1, 1));
    assertTrue(ethYear.overlaps(gregYear));
    // But the Ethiopian sequence does not claim to aggregate to Gregorian years.
    assertFalse(Yearly.contributesTo(GregorianSequences.Year));
    assertFalse(GregorianSequences.Yearly.contributesTo(Year));
  }

  @Test
  void partitionAcrossCalendarBoundaryThrows() {
    // A Gregorian calendar year is not aligned to Ethiopian month boundaries.
    var gregYear = Epoch2kInterval.ofYear(2000);
    assertThrows(IllegalArgumentException.class, () -> Monthly.partition(gregYear));
  }

  @Test
  void membersOfGregorianYearSelectsEthiopianMonthsByMajority() {
    // The Ethiopian month containing the most days of Gregorian 2000
    // is included; boundary months are filtered by majority.
    var gregYear = Epoch2kInterval.ofYear(2000);
    var members = Monthly.contributorsTo(gregYear);
    for (var m : members) {
      int inside = Math.min(m.last().daysSince2k(), gregYear.last().daysSince2k())
                 - Math.max(m.first().daysSince2k(), gregYear.first().daysSince2k()) + 1;
      assertTrue(inside * 2 > m.lengthInDays());
    }
  }
}