package org.hisp.dhis2.time;

import org.junit.jupiter.api.Test;

import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.hisp.dhis2.time.GregorianSequences.BiMonthly;
import static org.hisp.dhis2.time.GregorianSequences.Quarterly;
import static org.hisp.dhis2.time.GregorianSequences.SixMonth;
import static org.hisp.dhis2.time.GregorianSequences.SixMonthly;
import static org.hisp.dhis2.time.GregorianSequences.sixMonthly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiMonthlyGregorianSequenceTest {

  @Test
  void biMonthlyTilesSixMonthlyAndYear() {
    var year = Epoch2kInterval.ofYear(2024);
    assertEquals(6, BiMonthly.partition(year).size());

    var s1 = SixMonthly.interval(ofYMD(2024, 3, 15));
    assertEquals(3, BiMonthly.partition(s1).size());
  }

  @Test
  void biMonthlyContributesToQuarterViaTieBreak() {
    // Mar-Apr bi-month is exactly 50/50 in Q1/Q2; tie-break attributes to Q1.
    var marApr = BiMonthly.interval(ofYMD(2024, 4, 15));
    var q1 = Quarterly.interval(ofYMD(2024, 2, 15));
    var q2 = Quarterly.interval(ofYMD(2024, 5, 15));

    assertTrue(BiMonthly.contributorsTo(q1).contains(marApr));
    assertFalse(BiMonthly.contributorsTo(q2).contains(marApr));
  }

  @Test
  void biMonthlyContainsItsDay() {
    for (var d : new Epoch2kDay[] {
        ofYMD(2024, 1, 15), ofYMD(2024, 4, 15), ofYMD(2024, 12, 31)}) {
      assertTrue(BiMonthly.interval(d).contains(d));
    }
  }

  @Test
  void nextAndPreviousInverseForBiMonthly() {
    var i = BiMonthly.interval(ofYMD(2024, 3, 15));
    assertEquals(i, BiMonthly.next(BiMonthly.previous(i)));
    assertEquals(i, BiMonthly.previous(BiMonthly.next(i)));
  }

  @Test
  void biMonthlyDeclaresNoSixMonthPartition() {
    assertFalse(BiMonthly.partitions(SixMonth));
  }

  @Test
  void biMonthlyPartitionIsStructuralNotFamilyGated() {
    var janJun = SixMonthly.interval(ofYMD(2024, 3, 15));    // Jan 1 .. Jun 30
    var parts = BiMonthly.partition(janJun);
    assertEquals(3, parts.size());

    var febJul = sixMonthly(Epoch2kDay.MonthOfYear.February).interval(ofYMD(2024, 5, 15));
    assertThrows(IllegalArgumentException.class, () -> BiMonthly.partition(febJul));
  }

  @Test
  void biMonthlyContributesToSixMonthly() {
    var s1 = SixMonthly.interval(ofYMD(2024, 3, 15));   // Jan-Jun 2024
    var biMonths = BiMonthly.contributorsTo(s1);
    assertEquals(3, biMonths.size());
    assertEquals(ofYMD(2024, 1, 1), biMonths.get(0).firstDay());
    assertEquals(ofYMD(2024, 5, 1), biMonths.get(2).firstDay());
  }

  @Test
  void biMonthlyPartitionsYear() {
    var year = Epoch2kInterval.ofYear(2024);
    assertEquals(6, BiMonthly.partition(year).size());
  }
}
