package org.hisp.dhis2.time;

import org.junit.jupiter.api.Test;

import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.hisp.dhis2.time.GregorianSequences.GT_;
import static org.hisp.dhis2.time.GregorianSequences.GQ_;
import static org.hisp.dhis2.time.GregorianSequences.GS;
import static org.hisp.dhis2.time.GregorianSequences.GS_;
import static org.hisp.dhis2.time.GregorianSequences.sixMonthly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiMonthlyGregorianSequenceTest {

  @Test
  void biMonthlyTilesSixMonthlyAndYear() {
    var year = Epoch2kInterval.ofYear(2024);
    assertEquals(6, GT_.partition(year).size());

    var s1 = GS_.interval(ofYMD(2024, 3, 15));
    assertEquals(3, GT_.partition(s1).size());
  }

  @Test
  void biMonthlyContributesToQuarterViaTieBreak() {
    // Mar-Apr bi-month is exactly 50/50 in Q1/Q2; tie-break attributes to Q1.
    var marApr = GT_.interval(ofYMD(2024, 4, 15));
    var q1 = GQ_.interval(ofYMD(2024, 2, 15));
    var q2 = GQ_.interval(ofYMD(2024, 5, 15));

    assertTrue(GT_.contributorsTo(q1).contains(marApr));
    assertFalse(GT_.contributorsTo(q2).contains(marApr));
  }

  @Test
  void biMonthlyContainsItsDay() {
    for (var d : new Epoch2kDay[] {
        ofYMD(2024, 1, 15), ofYMD(2024, 4, 15), ofYMD(2024, 12, 31)}) {
      assertTrue(GT_.interval(d).contains(d));
    }
  }

  @Test
  void nextAndPreviousInverseForBiMonthly() {
    var i = GT_.interval(ofYMD(2024, 3, 15));
    assertEquals(i, GT_.next(GT_.previous(i)));
    assertEquals(i, GT_.previous(GT_.next(i)));
  }

  @Test
  void biMonthlyDeclaresNoSixMonthPartition() {
    assertFalse(GT_.partitions(GS));
  }

  @Test
  void biMonthlyPartitionIsStructuralNotFamilyGated() {
    var janJun = GS_.interval(ofYMD(2024, 3, 15));    // Jan 1 .. Jun 30
    var parts = GT_.partition(janJun);
    assertEquals(3, parts.size());

    var febJul = sixMonthly(Epoch2kDay.MonthOfYear.February).interval(ofYMD(2024, 5, 15));
    assertThrows(IllegalArgumentException.class, () -> GT_.partition(febJul));
  }

  @Test
  void biMonthlyContributesToSixMonthly() {
    var s1 = GS_.interval(ofYMD(2024, 3, 15));   // Jan-Jun 2024
    var biMonths = GT_.contributorsTo(s1);
    assertEquals(3, biMonths.size());
    assertEquals(ofYMD(2024, 1, 1), biMonths.get(0).firstDay());
    assertEquals(ofYMD(2024, 5, 1), biMonths.get(2).firstDay());
  }

  @Test
  void biMonthlyPartitionsYear() {
    var year = Epoch2kInterval.ofYear(2024);
    assertEquals(6, GT_.partition(year).size());
  }
}
