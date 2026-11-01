package org.hisp.dhis2.time;

import org.junit.jupiter.api.Test;

import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.hisp.dhis2.time.GregorianSequences.GB_;
import static org.hisp.dhis2.time.GregorianSequences.GQ_;
import static org.hisp.dhis2.time.GregorianSequences.GS;
import static org.hisp.dhis2.time.GregorianSequences.GS_;
import static org.hisp.dhis2.time.GregorianSequences.sixMonthly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BimesterGregorianSequenceTest {

  @Test
  void bimesterTilesSixMonthlyAndYear() {
    var year = Epoch2kInterval.ofYear(2024);
    assertEquals(6, GB_.partition(year).size());

    var s1 = GS_.interval(ofYMD(2024, 3, 15));
    assertEquals(3, GB_.partition(s1).size());
  }

  @Test
  void bimesterContributesToQuarterViaTieBreak() {
    // Mar-Apr bi-month is exactly 50/50 in Q1/Q2; tie-break attributes to Q1.
    var marApr = GB_.interval(ofYMD(2024, 4, 15));
    var q1 = GQ_.interval(ofYMD(2024, 2, 15));
    var q2 = GQ_.interval(ofYMD(2024, 5, 15));

    assertTrue(GB_.contributorsTo(q1).contains(marApr));
    assertFalse(GB_.contributorsTo(q2).contains(marApr));
  }

  @Test
  void bimesterContainsItsDay() {
    for (var d : new Epoch2kDay[] {
        ofYMD(2024, 1, 15), ofYMD(2024, 4, 15), ofYMD(2024, 12, 31)}) {
      assertTrue(GB_.interval(d).contains(d));
    }
  }

  @Test
  void nextAndPreviousInverseForBimester() {
    var i = GB_.interval(ofYMD(2024, 3, 15));
    assertEquals(i, GB_.next(GB_.previous(i)));
    assertEquals(i, GB_.previous(GB_.next(i)));
  }

  @Test
  void bimesterDeclaresNoSixMonthPartition() {
    assertFalse(GB_.partitions(GS));
  }

  @Test
  void bimesterPartitionIsStructuralNotFamilyGated() {
    var janJun = GS_.interval(ofYMD(2024, 3, 15));    // Jan 1 .. Jun 30
    var parts = GB_.partition(janJun);
    assertEquals(3, parts.size());

    var febJul = sixMonthly(Epoch2kDay.MonthOfYear.February).interval(ofYMD(2024, 5, 15));
    assertThrows(IllegalArgumentException.class, () -> GB_.partition(febJul));
  }

  @Test
  void bimesterContributesToSixMonthly() {
    var s1 = GS_.interval(ofYMD(2024, 3, 15));   // Jan-Jun 2024
    var biMonths = GB_.contributorsTo(s1);
    assertEquals(3, biMonths.size());
    assertEquals(ofYMD(2024, 1, 1), biMonths.get(0).firstDay());
    assertEquals(ofYMD(2024, 5, 1), biMonths.get(2).firstDay());
  }

  @Test
  void bimesterPartitionsYear() {
    var year = Epoch2kInterval.ofYear(2024);
    assertEquals(6, GB_.partition(year).size());
  }
}
