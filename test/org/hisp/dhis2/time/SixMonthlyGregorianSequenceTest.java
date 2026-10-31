package org.hisp.dhis2.time;

import org.junit.jupiter.api.Test;

import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.hisp.dhis2.time.GregorianSequences.FiscalSixMonthlyApril;
import static org.hisp.dhis2.time.GregorianSequences.FiscalYear;
import static org.hisp.dhis2.time.GregorianSequences.SixMonthly;
import static org.hisp.dhis2.time.GregorianSequences.Year;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SixMonthlyGregorianSequenceTest {

  @Test
  void sixMonthlyTilesYear() {
    var year = Epoch2kInterval.ofYear(2024);
    var parts = SixMonthly.partition(year);
    assertEquals(2, parts.size());
    assertEquals(ofYMD(2024, 1, 1), parts.get(0).firstDay());
    assertEquals(ofYMD(2024, 7, 1), parts.get(1).firstDay());
  }

  @Test
  void fiscalSixMonthlyAggregatesToFiscalYear() {
    assertTrue(FiscalSixMonthlyApril.contributesTo(FiscalYear));
    assertTrue(FiscalSixMonthlyApril.partitions(FiscalYear));
    assertFalse(FiscalSixMonthlyApril.contributesTo(Year));
  }

  @Test
  void sixMonthlyAprilStartsApril() {
    var s1 = FiscalSixMonthlyApril.interval(ofYMD(2024, 5, 15));
    assertEquals(ofYMD(2024, 4, 1), s1.firstDay());
    assertEquals(ofYMD(2024, 9, 30), s1.lastDay());
  }

  @Test
  void nextAndPreviousInverseForSixMonthly() {
    var i = SixMonthly.interval(ofYMD(2024, 8, 15));
    assertEquals(i, SixMonthly.next(SixMonthly.previous(i)));
    assertEquals(i, SixMonthly.previous(SixMonthly.next(i)));
  }
}
