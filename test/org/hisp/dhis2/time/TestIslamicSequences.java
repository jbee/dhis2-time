package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.IslamicCalendar.toEpochDayInIC;
import static org.hisp.dhis2.time.IslamicSequences.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class TestIslamicSequences {

  @Test
  void dailyIntervalIsSingleDay() {
    var i = ID_.interval(toEpochDayInIC(1445, 9, 1));
    assertEquals(1, i.lengthInDays());
    assertEquals(toEpochDayInIC(1445, 9, 1), i.firstDay());
  }

  @Test
  void monthlyIntervalContainsItsDay() {
    for (Epoch2kDay d : new Epoch2kDay[] {
        toEpochDayInIC(1440, 1, 1), toEpochDayInIC(1445, 9, 15), toEpochDayInIC(1450, 12, 29)}) {
      assertTrue(IM_.interval(d).contains(d));
    }
  }

  @Test
  void yearlyIntervalContainsItsDay() {
    for (Epoch2kDay d : new Epoch2kDay[] {
        toEpochDayInIC(1440, 1, 1), toEpochDayInIC(1445, 9, 15), toEpochDayInIC(1450, 12, 29)}) {
      assertTrue(IY_.interval(d).contains(d));
    }
  }

  @Test
  void monthlyNextIsFollowingIslamicMonth() {
    var ramadan = IM_.interval(toEpochDayInIC(1445, 9, 1));
    var shawwal = IM_.next(ramadan);
    assertEquals(toEpochDayInIC(1445, 10, 1), shawwal.firstDay());
    assertEquals(toEpochDayInIC(1445, 10, 29), shawwal.lastDay());
  }

  @Test
  void monthlyNextFromDhuAlHijjahCrossesIslamicYear() {
    var last = IM_.interval(toEpochDayInIC(1445, 12, 1));
    var next = IM_.next(last);
    assertEquals(toEpochDayInIC(1446, 1, 1), next.firstDay());
  }

  @Test
  void yearlyNextIsFollowingIslamicYear() {
    var y1445 = IY_.interval(toEpochDayInIC(1445, 6, 15));
    var y1446 = IY_.next(y1445);
    assertEquals(toEpochDayInIC(1446, 1, 1), y1446.firstDay());
  }

  @Test
  void nextAndPreviousAreInverses() {
    var month = IM_.interval(toEpochDayInIC(1447, 9, 1));
    assertEquals(month, IM_.previous(IM_.next(month)));
    assertEquals(month, IM_.next(IM_.previous(month)));

    var year = IY_.interval(toEpochDayInIC(1447, 6, 15));
    assertEquals(year, IY_.previous(IY_.next(year)));
    assertEquals(year, IY_.next(IY_.previous(year)));
  }

  @Test
  void dailyPartitionOfIslamicMonthYields29Or30Days() {
    var ramadan1446 = IM_.interval(toEpochDayInIC(1446, 9, 1));
    assertEquals(29, ID_.partition(ramadan1446).size());

    var ramadan1447 = IM_.interval(toEpochDayInIC(1447, 9, 1));
    assertEquals(30, ID_.partition(ramadan1447).size());
  }

  @Test
  void monthlyPartitionOfIslamicYearYields12Months() {
    var year = IY_.interval(toEpochDayInIC(1447, 6, 15));
    var months = IM_.partition(year);
    assertEquals(12, months.size());
    assertEquals(toEpochDayInIC(1447, 1, 1), months.get(0).firstDay());
    assertEquals(toEpochDayInIC(1447, 12, IslamicCalendar.daysInMonthInIC(1447, 12)), months.get(11).lastDay());
  }

  @Test
  void aggregatesToOnlyIslamicFamilies() {
    assertEquals(Set.of(IM.noOffset(), IY.noOffset()), ID_.contributesTo());
    assertFalse(ID_.contributesTo(GregorianSequences.GM));
    assertFalse(ID_.contributesTo(EthiopianSequences.EM));
  }

  @Test
  void partitionAcrossCalendarBoundaryThrows() {
    // A Gregorian calendar year is not aligned to Islamic month boundaries.
    var gregYear = Epoch2kInterval.ofYear(2024);
    assertThrows(IllegalArgumentException.class, () -> IM_.partition(gregYear));
  }
}