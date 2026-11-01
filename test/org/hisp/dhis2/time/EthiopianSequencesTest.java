package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.EthiopianCalendar.toEpochDayInEC;
import static org.hisp.dhis2.time.EthiopianSequences.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class EthiopianSequencesTest {

  @Test
  void dailyIntervalIsSingleDay() {
    var i = ED_.interval(toEpochDayInEC(1992, 4, 22));
    assertEquals(1, i.lengthInDays());
    assertEquals(toEpochDayInEC(1992, 4, 22), i.firstDay());
    assertEquals(toEpochDayInEC(1992, 4, 22), i.lastDay());
  }

  @Test
  void monthlyIntervalContainsItsDay() {
    for (Epoch2kDay d : new Epoch2kDay[] {
        toEpochDayInEC(1992, 4, 22), toEpochDayInEC(1995, 13, 3), toEpochDayInEC(2017, 1, 1)}) {
      assertTrue(EM_.interval(d).contains(d));
    }
  }

  @Test
  void yearlyIntervalContainsItsDay() {
    for (Epoch2kDay d : new Epoch2kDay[] {
        toEpochDayInEC(1992, 4, 22), toEpochDayInEC(1995, 13, 3), toEpochDayInEC(2017, 1, 1)}) {
      assertTrue(EY_.interval(d).contains(d));
    }
  }

  @Test
  void monthlyNextIsFollowingEthiopianMonth() {
    var tahsas = EM_.interval(new Epoch2kDay(0));  // Tahsas 1992
    var tir = EM_.next(tahsas);                    // Tir 1992
    assertEquals(toEpochDayInEC(1992, 5, 1), tir.firstDay());
    assertEquals(toEpochDayInEC(1992, 5, 30), tir.lastDay());
  }

  @Test
  void monthlyNextFromPagumeCrossesEthiopianYear() {
    // Pagume 1995 is the last month; next is Meskerem 1996.
    var pagume = EM_.interval(toEpochDayInEC(1995, 13, 1));
    var meskerem = EM_.next(pagume);
    assertEquals(toEpochDayInEC(1996, 1, 1), meskerem.firstDay());
  }

  @Test
  void yearlyNextIsFollowingEthiopianYear() {
    var y1992 = EY_.interval(new Epoch2kDay(0));
    var y1993 = EY_.next(y1992);
    assertEquals(toEpochDayInEC(1993, 1, 1), y1993.firstDay());
    assertEquals(toEpochDayInEC(1993, 13, 5), y1993.lastDay());
  }

  @Test
  void nextAndPreviousAreInverses() {
    var month = EM_.interval(toEpochDayInEC(1995, 13, 3));
    assertEquals(month, EM_.previous(EM_.next(month)));
    assertEquals(month, EM_.next(EM_.previous(month)));

    var year = EY_.interval(toEpochDayInEC(1995, 6, 15));
    assertEquals(year, EY_.previous(EY_.next(year)));
    assertEquals(year, EY_.next(EY_.previous(year)));
  }

  @Test
  void dailyPartitionOfEthiopianMonthYields30Days() {
    var tahsas = EM_.interval(new Epoch2kDay(0));
    assertEquals(30, ED_.partition(tahsas).size());
  }

  @Test
  void dailyPartitionOfLeapPagumeYields6Days() {
    var pagume = EM_.interval(toEpochDayInEC(1995, 13, 1));
    var days = ED_.partition(pagume);
    assertEquals(6, days.size());
    assertEquals(toEpochDayInEC(1995, 13, 1), days.get(0).firstDay());
    assertEquals(toEpochDayInEC(1995, 13, 6), days.get(5).lastDay());
  }

  @Test
  void monthlyPartitionOfEthiopianYearYields13Months() {
    var year = EY_.interval(toEpochDayInEC(1995, 6, 15));
    var months = EM_.partition(year);
    assertEquals(13, months.size());
    assertEquals(toEpochDayInEC(1995, 1, 1), months.get(0).firstDay());
    assertEquals(toEpochDayInEC(1995, 13, 6), months.get(12).lastDay());
  }

  @Test
  void dailyAggregatesToEthiopianMonthAndYearOnly() {
    assertEquals(Set.of(EM.noOffset(), EY.noOffset()), ED_.contributesTo());
    assertFalse(ED_.contributesTo(GregorianSequences.GM));
    assertFalse(ED_.contributesTo(GregorianSequences.GY));
  }

  @Test
  void monthlyAggregatesToEthiopianYearOnly() {
    assertEquals(Set.of(EY.noOffset()), EM_.contributesTo());
  }

  @Test
  void yearlyAggregatesToNothing() {
    assertTrue(EY_.contributesTo().isEmpty());
  }

  @Test
  void ethiopianAndGregorianYearlyDoNotInteract() {
    var ethYear = EY_.interval(toEpochDayInEC(1995, 6, 15));
    var gregYear = GregorianSequences.GY_.interval(Epoch2kDay.ofYMD(2002, 1, 1));
    assertTrue(ethYear.overlaps(gregYear));
    // But the Ethiopian sequence does not claim to aggregate to Gregorian years.
    assertFalse(EY_.contributesTo(GregorianSequences.GY));
    assertFalse(GregorianSequences.GY_.contributesTo(EY));
  }

  @Test
  void partitionAcrossCalendarBoundaryThrows() {
    // A Gregorian calendar year is not aligned to Ethiopian month boundaries.
    var gregYear = Epoch2kInterval.ofYear(2000);
    assertThrows(IllegalArgumentException.class, () -> EM_.partition(gregYear));
  }

  @Test
  void membersOfGregorianYearSelectsEthiopianMonthsByMajority() {
    // The Ethiopian month containing the most days of Gregorian 2000
    // is included; boundary months are filtered by majority.
    var gregYear = Epoch2kInterval.ofYear(2000);
    var members = EM_.contributorsTo(gregYear);
    for (var m : members) {
      int inside = Math.min(m.lastDay().daysSince2k(), gregYear.lastDay().daysSince2k())
                 - Math.max(m.firstDay().daysSince2k(), gregYear.firstDay().daysSince2k()) + 1;
      assertTrue(inside * 2 > m.lengthInDays());
    }
  }
}