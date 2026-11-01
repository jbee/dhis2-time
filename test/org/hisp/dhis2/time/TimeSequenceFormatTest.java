package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.April;
import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.hisp.dhis2.time.GregorianSequences.FM7;
import static org.hisp.dhis2.time.GregorianSequences.FQ7;
import static org.hisp.dhis2.time.GregorianSequences.FH4;
import static org.hisp.dhis2.time.GregorianSequences.FHb;
import static org.hisp.dhis2.time.GregorianSequences.FY7;
import static org.hisp.dhis2.time.GregorianSequences.GF_;
import static org.hisp.dhis2.time.GregorianSequences.GD_;
import static org.hisp.dhis2.time.GregorianSequences.GM_;
import static org.hisp.dhis2.time.GregorianSequences.GQ_;
import static org.hisp.dhis2.time.GregorianSequences.GS_;
import static org.hisp.dhis2.time.GregorianSequences.GB_;
import static org.hisp.dhis2.time.GregorianSequences.GW_;
import static org.hisp.dhis2.time.GregorianSequences.GY_;
import static org.hisp.dhis2.time.GregorianSequences.quarterly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TimeSequenceFormatTest {

  private static final List<TimeSequence> ALL_GREGORIAN =
      List.of(GD_, GW_, GF_, GM_, GB_, GS_, GQ_, GY_);

  // ── isMemberOf: member cases ────────────────────────────────

  @Test
  void isMemberOfAcceptsEverySequencesOwnIntervals() {
    for (var seq : ALL_GREGORIAN) {
      var interval = seq.interval(ofYMD(2024, 3, 15));
      assertTrue(TimeSequence.isMemberOf(seq, interval), seq.key().toString());
    }
  }

  @Test
  void isMemberOfAcceptsOffsetSequenceIntervals() {
    var gq4 = quarterly(April);
    var q1 = gq4.interval(ofYMD(2024, 5, 15));
    assertTrue(TimeSequence.isMemberOf(gq4, q1));
  }

  @Test
  void isMemberOfAcceptsCustomSequenceIntervals() {
    var spring = new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24));
    var autumn = new Epoch2kInterval(ofYMD(2024, 9, 2), ofYMD(2024, 12, 20));
    var terms =
        CustomSequences.of(
            new TimeSequenceKey('S', 'T'), List.of(spring, autumn));

    assertTrue(TimeSequence.isMemberOf(terms, spring));
    assertTrue(TimeSequence.isMemberOf(terms, autumn));
  }

  // ── isMemberOf: rejection cases ─────────────────────────────

  @Test
  void isMemberOfRejectsSubInterval() {
    // A ten-day slice of March is not March itself.
    var tenDays = new Epoch2kInterval(ofYMD(2024, 3, 10), ofYMD(2024, 3, 20));
    assertFalse(TimeSequence.isMemberOf(GM_, tenDays));
  }

  @Test
  void isMemberOfRejectsSuperInterval() {
    // A full quarter is not a month, even though it contains months.
    var q1 = GQ_.interval(ofYMD(2024, 2, 15));
    assertFalse(TimeSequence.isMemberOf(GM_, q1));
  }

  @Test
  void isMemberOfRejectsSameLengthButShifted() {
    // March 2 - April 1 has the same length as March but different boundaries.
    var shifted = new Epoch2kInterval(ofYMD(2024, 3, 2), ofYMD(2024, 4, 1));
    assertFalse(TimeSequence.isMemberOf(GM_, shifted));
  }

  @Test
  void isMemberOfRejectsEqualStartDifferentEnd() {
    var tooLong = new Epoch2kInterval(ofYMD(2024, 3, 1), ofYMD(2024, 4, 1));
    assertFalse(TimeSequence.isMemberOf(GM_, tooLong));
  }

  @Test
  void isMemberOfRejectsEqualEndDifferentStart() {
    var tooLong = new Epoch2kInterval(ofYMD(2024, 2, 28), ofYMD(2024, 3, 31));
    assertFalse(TimeSequence.isMemberOf(GM_, tooLong));
  }

  @Test
  void isMemberOfRejectsCustomSequenceGapDay() {
    var terms =
        CustomSequences.of(
            new TimeSequenceKey('S', 'T'),
            List.of(new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24))));

    var gap = new Epoch2kInterval(ofYMD(2024, 7, 1));
    assertFalse(TimeSequence.isMemberOf(terms, gap));
  }

  @Test
  void isMemberOfRejectsCustomSequenceOutOfRange() {
    var terms =
        CustomSequences.of(
            new TimeSequenceKey('S', 'T'),
            List.of(new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24))));

    assertFalse(TimeSequence.isMemberOf(terms, new Epoch2kInterval(ofYMD(2023, 12, 1))));
    assertFalse(TimeSequence.isMemberOf(terms, new Epoch2kInterval(ofYMD(2025, 1, 1))));
  }

  @Test
  void isMemberOfRejectsCustomSequenceSubInterval() {
    var terms =
        CustomSequences.of(
            new TimeSequenceKey('S', 'T'),
            List.of(new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24))));

    var partOfSpring = new Epoch2kInterval(ofYMD(2024, 3, 1), ofYMD(2024, 4, 1));
    assertFalse(TimeSequence.isMemberOf(terms, partOfSpring));
  }

  // ── format: preferred forms per sequence ────────────────────

  @Test
  void dailyFormatsAsSingleDay() {
    var d = GD_.interval(ofYMD(2024, 3, 15));
    assertEquals("GD/2024-03-15", GD_.format(d));
  }

  @Test
  void weeklyFormatsAsFirstDay() {
    // 2024-03-15 is a Friday; Monday-start week begins 2024-03-11.
    var w = GW_.interval(ofYMD(2024, 3, 15));
    assertEquals("GW/2024-03-11", GW_.format(w));
  }

  @Test
  void weeklyWithOffsetFormatsAsFirstDay() {
    var gw3 = GregorianSequences.weekly(Epoch2kDay.DayOfWeek.Wednesday);
    var w = gw3.interval(ofYMD(2024, 3, 15));
    assertEquals("GW3/2024-03-13", gw3.format(w));
  }

  @Test
  void fortnightlyStubFormatsAndRoundTrips() {
    // 2020 has 53 ISO weeks; BW27 is the 1-week stub.
    var stub = GF_.interval(ofYMD(2020, 12, 28));
    assertEquals(7, stub.lengthInDays());
    assertEquals("GF/2020-12-28", GF_.format(stub));

    var parser = new TimeSequenceParser(List.of(GF_));
    assertEquals(stub, parser.parse(GF_.format(stub)).interval());
  }

  @Test
  void monthlyFormatsAsYearMonth() {
    var m = GM_.interval(ofYMD(2024, 3, 15));
    assertEquals("GM/2024-03", GM_.format(m));
  }

  @Test
  void bimesterJanuaryFormatsAsPosition() {
    assertEquals("GB/2024:1", GB_.format(GB_.interval(ofYMD(2024, 1, 15))));
    assertEquals("GB/2024:2", GB_.format(GB_.interval(ofYMD(2024, 3, 15))));
    assertEquals("GB/2024:6", GB_.format(GB_.interval(ofYMD(2024, 11, 15))));
  }

  @Test
  void quarterlyJanuaryFormatsAsPosition() {
    assertEquals("GQ/2024:1", GQ_.format(GQ_.interval(ofYMD(2024, 2, 15))));
    assertEquals("GQ/2024:2", GQ_.format(GQ_.interval(ofYMD(2024, 5, 15))));
    assertEquals("GQ/2024:3", GQ_.format(GQ_.interval(ofYMD(2024, 8, 15))));
    assertEquals("GQ/2024:4", GQ_.format(GQ_.interval(ofYMD(2024, 11, 15))));
  }

  @Test
  void semesterJanuaryFormatsAsPosition() {
    assertEquals("GS/2024:1", GS_.format(GS_.interval(ofYMD(2024, 3, 15))));
    assertEquals("GS/2024:2", GS_.format(GS_.interval(ofYMD(2024, 9, 15))));
  }

  @Test
  void yearlyJanuaryFormatsAsYear() {
    assertEquals("GY/2024", GY_.format(GY_.interval(ofYMD(2024, 6, 15))));
  }

  // ── format: offset sequences use first-day form ────────────

  @Test
  void quarterlyAprilFormatsAsFirstDay() {
    var gq4 = quarterly(April);
    assertEquals("GQ4/2024-04-01", gq4.format(gq4.interval(ofYMD(2024, 5, 15))));
    assertEquals("GQ4/2024-01-01", gq4.format(gq4.interval(ofYMD(2024, 2, 15))));
  }

  @Test
  void bimesterAprilFormatsAsFirstDay() {
    var gb4 = GregorianSequences.bimester(April);
    assertEquals("GB4/2024-04-01", gb4.format(gb4.interval(ofYMD(2024, 4, 15))));
    assertEquals("GB4/2024-06-01", gb4.format(gb4.interval(ofYMD(2024, 7, 15))));
  }

  @Test
  void fiscalSemesterAprilFormatsAsFirstDay() {
    assertEquals("FH4/2024-04-01", FH4.format(FH4.interval(ofYMD(2024, 5, 15))));
    assertEquals("FH4/2024-10-01", FH4.format(FH4.interval(ofYMD(2024, 11, 15))));
  }

  @Test
  void fiscalSemesterNovemberFormatsAsFirstDay() {
    assertEquals("FHb/2024-11-01", FHb.format(FHb.interval(ofYMD(2024, 12, 15))));
  }

  @Test
  void fiscalMonthlyJulyFormatsAsFirstDay() {
    assertEquals("FM7/2024-07", FM7.format(FM7.interval(ofYMD(2024, 7, 15))));
  }

  @Test
  void fiscalQuarterlyJulyFormatsAsFirstDay() {
    assertEquals("FQ7/2024-07-01", FQ7.format(FQ7.interval(ofYMD(2024, 8, 15))));
  }

  @Test
  void fiscalYearlyJulyFormatsAsFirstDay() {
    assertEquals("FY7/2024-07-01", FY7.format(FY7.interval(ofYMD(2024, 8, 15))));
    assertEquals("FY7/2023-07-01", FY7.format(FY7.interval(ofYMD(2024, 3, 15))));
  }

  // ── format: custom sequences fall back to canonical ────────

  @Test
  void customSequenceFallsBackToCanonicalForm() {
    var terms =
        CustomSequences.of(
            new TimeSequenceKey('S', 'T'),
            List.of(new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24))));

    var spring = terms.interval(ofYMD(2024, 3, 15));
    assertEquals("ST/2024-01-08+138", terms.format(spring));
  }

  // ── format: fallback for non-members ────────────────────────

  @Test
  void formatFallbackForNonMemberShortInterval() {
    var tenDays = new Epoch2kInterval(ofYMD(2024, 3, 5), ofYMD(2024, 3, 14));
    assertEquals("GM/2024-03-05+10", GM_.format(tenDays));
    assertEquals("GQ/2024-03-05+10", GQ_.format(tenDays));
    assertEquals("GY/2024-03-05+10", GY_.format(tenDays));
  }

  @Test
  void formatFallbackForNonMemberLongInterval() {
    var twoMonths = new Epoch2kInterval(ofYMD(2024, 3, 1), ofYMD(2024, 4, 30));
    assertEquals("GM/2024-03-01+61", GM_.format(twoMonths));
  }

  @Test
  void formatFallbackForShiftedSameLengthInterval() {
    var shiftedMarch = new Epoch2kInterval(ofYMD(2024, 3, 2), ofYMD(2024, 4, 1));
    assertEquals("GM/2024-03-02+31", GM_.format(shiftedMarch));
  }

  @Test
  void formatFallbackIncludesKeyForEveryGregorianSequence() {
    var tenDays = new Epoch2kInterval(ofYMD(2024, 3, 5), ofYMD(2024, 3, 14));

    assertEquals("GD/2024-03-05+10", GD_.format(tenDays));
    assertEquals("GW/2024-03-05+10", GW_.format(tenDays));
    assertEquals("GF/2024-03-05+10", GF_.format(tenDays));
    assertEquals("GM/2024-03-05+10", GM_.format(tenDays));
    assertEquals("GB/2024-03-05+10", GB_.format(tenDays));
    assertEquals("GS/2024-03-05+10", GS_.format(tenDays));
    assertEquals("GQ/2024-03-05+10", GQ_.format(tenDays));
    assertEquals("GY/2024-03-05+10", GY_.format(tenDays));
  }

  @Test
  void formatFallbackForCustomSequenceGap() {
    var terms =
        CustomSequences.of(
            new TimeSequenceKey('S', 'T'),
            List.of(new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24))));

    var gapDay = new Epoch2kInterval(ofYMD(2024, 7, 15));
    assertEquals("ST/2024-07-15+1", terms.format(gapDay));
  }

  // ── format: shorter than canonical form ────────────────────

  @Test
  void formatProducesShorterStringThanCanonicalIntervalToString() {
    var q1 = GQ_.interval(ofYMD(2024, 2, 15));
    assertEquals("2024-01-01+91", q1.toString());
    assertEquals("GQ/2024:1", GQ_.format(q1));
  }

  @Test
  void formatOfMonthlyIsShorterThanCanonical() {
    var march = GM_.interval(ofYMD(2024, 3, 15));
    assertEquals("2024-03-01+31", march.toString());
    assertEquals("GM/2024-03", GM_.format(march));
  }

  // ── Round-trip through parser ───────────────────────────────

  @Test
  void formattedStringParsesBackToSameInterval() {
    var parser = new TimeSequenceParser(ALL_GREGORIAN);

    for (var seq : ALL_GREGORIAN) {
      var interval = seq.interval(ofYMD(2024, 3, 15));
      var formatted = seq.format(interval);
      assertEquals(
          interval,
          parser.parse(formatted).interval(),
          () -> seq.key() + " formatted " + interval + " as " + formatted);
    }
  }

  @Test
  void formattedStringRoundTripsAcrossBoundaries() {
    var parser = new TimeSequenceParser(ALL_GREGORIAN);
    var days =
        List.of(
            ofYMD(2024, 1, 1), ofYMD(2024, 2, 29), ofYMD(2024, 12, 31), ofYMD(2023, 6, 15));

    for (var seq : ALL_GREGORIAN) {
      for (var d : days) {
        var interval = seq.interval(d);
        var formatted = seq.format(interval);
        assertEquals(
            interval,
            parser.parse(formatted).interval(),
            () -> seq.key() + " day " + d + " formatted as " + formatted);
      }
    }
  }

  @Test
  void formattedOffsetSequencesRoundTripThroughParser() {
    var gq4 = quarterly(April);
    var gt4 = GregorianSequences.bimester(April);
    var parser = new TimeSequenceParser(List.of(gq4, gt4, FH4, FHb, FM7, FQ7, FY7));

    assertEquals(
        gq4.interval(ofYMD(2024, 5, 15)),
        parser.parse(gq4.format(gq4.interval(ofYMD(2024, 5, 15)))).interval());
    assertEquals(
        gt4.interval(ofYMD(2024, 5, 15)),
        parser.parse(gt4.format(gt4.interval(ofYMD(2024, 5, 15)))).interval());
    assertEquals(
        FY7.interval(ofYMD(2024, 8, 15)),
        parser.parse(FY7.format(FY7.interval(ofYMD(2024, 8, 15)))).interval());
    assertEquals(
        FH4.interval(ofYMD(2024, 11, 15)),
        parser.parse(FH4.format(FH4.interval(ofYMD(2024, 11, 15)))).interval());
  }

  // ── Fortnightly, including the 53-week stub ─────────────────────

  @Test
  void fortnightlyFormatsAsFirstDay() {
    // 2024-03-15 (Friday) falls in the fortnight starting Monday 2024-03-11.
    var f = GF_.interval(ofYMD(2024, 3, 15));
    assertEquals("GF/2024-03-11", GF_.format(f));
  }

  @Test
  void fortnightlyEndOfYearBoundaryIsNormalTwoWeeks() {
    // 2023 has 52 ISO weeks; fortnight 26 runs 2023-12-18 .. 2023-12-31.
    var f = GF_.interval(ofYMD(2023, 12, 25));
    assertEquals(14, f.lengthInDays());
    assertEquals("GF/2023-12-18", GF_.format(f));
  }

  @Test
  void fortnightlyStubIn53WeekYear() {
    // 2020 has 53 ISO weeks. Week 1 starts 2019-12-30. Week 53 is
    // 2020-12-28 .. 2021-01-03, and the 27th fortnight is that single week.
    var stub = GF_.interval(ofYMD(2020, 12, 30));
    assertEquals(7, stub.lengthInDays());
    assertEquals(ofYMD(2020, 12, 28), stub.firstDay());
    assertEquals(ofYMD(2021, 1, 3), stub.lastDay());
    assertEquals("GF/2020-12-28", GF_.format(stub));
  }

  @Test
  void fortnightlyStubRoundTripsThroughParser() {
    var stub = GF_.interval(ofYMD(2020, 12, 30));
    var formatted = GF_.format(stub);

    var parser = new TimeSequenceParser(List.of(GF_));
    assertEquals(stub, parser.parse(formatted).interval());
  }

  @Test
  void fortnightlyStubIsPrecededByNormalFortnight() {
    // Fortnight 26 of 2020 = 2020-12-14 .. 2020-12-27.
    var bw26 = GF_.interval(ofYMD(2020, 12, 20));
    assertEquals(14, bw26.lengthInDays());

    var stub = GF_.next(bw26);
    assertEquals(7, stub.lengthInDays());
    assertEquals(ofYMD(2020, 12, 28), stub.firstDay());

    var bw1of2021 = GF_.next(stub);
    assertEquals(14, bw1of2021.lengthInDays());
    assertEquals(ofYMD(2021, 1, 4), bw1of2021.firstDay());
  }

// ── Fiscal half-year after the S→H rename ──────────────────────

  @Test
  void fiscalHalfYearAprilFormatsAsFirstDay() {
    // FH4/ = fiscal half-year starting April; first period Apr-Sep 2024.
    assertEquals("FH4/2024-04-01", FH4.format(FH4.interval(ofYMD(2024, 5, 15))));
    assertEquals("FH4/2024-10-01", FH4.format(FH4.interval(ofYMD(2024, 11, 15))));
  }

  @Test
  void fiscalHalfYearNovemberFormatsAsFirstDay() {
    // FHb/ = fiscal half-year starting November; first period Nov 2024 - Apr 2025.
    assertEquals("FHb/2024-11-01", FHb.format(FHb.interval(ofYMD(2024, 12, 15))));
    assertEquals("FHb/2025-05-01", FHb.format(FHb.interval(ofYMD(2025, 6, 15))));
  }

  @Test
  void fiscalHalfYearRoundTripsThroughParser() {
    var parser = new TimeSequenceParser(List.of(FH4, FHb));

    for (var seq : List.of(FH4, FHb)) {
      for (var d : List.of(
          ofYMD(2024, 5, 15), ofYMD(2024, 12, 15), ofYMD(2025, 3, 15))) {
        var interval = seq.interval(d);
        var formatted = seq.format(interval);
        assertEquals(
            interval,
            parser.parse(formatted).interval(),
            () -> seq.key() + " day " + d + " formatted as " + formatted);
      }
    }
  }

  @Test
  void fiscalHalfYearKeyUsesHIntervalChar() {
    assertEquals('H', FH4.key().interval());
    assertEquals('H', FHb.key().interval());
    assertTrue(FH4.key().toString().startsWith("FH4/"));
    assertTrue(FHb.key().toString().startsWith("FHb/"));
  }

// ── Custom sequence default formatting ─────────────────────────

  @Test
  void customSequenceMemberFormatsAsCanonical() {
    // Custom sequences don't override format, so even a member interval
    // renders as the canonical <key>/<first>+<length> form.
    var terms = CustomSequences.of(
        new TimeSequenceKey('S', 'T'),
        List.of(new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24))));

    var spring = terms.interval(ofYMD(2024, 3, 15));
    assertEquals("ST/2024-01-08+138", terms.format(spring));
  }

  @Test
  void customSequenceGapDayFallsBackToCanonicalWithKey() {
    // A single day in the gap between terms is not a member, so format
    // must fall through to <key>/<interval.toString()> — including the key
    // prefix, not just the bare interval body.
    var terms = CustomSequences.of(
        new TimeSequenceKey('S', 'T'),
        List.of(
            new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24)),
            new Epoch2kInterval(ofYMD(2024, 9, 2), ofYMD(2024, 12, 20))));

    var gapDay = new Epoch2kInterval(ofYMD(2024, 7, 15));
    assertEquals("ST/2024-07-15+1", terms.format(gapDay));
  }

  @Test
  void customSequenceOutOfRangeFallsBackToCanonicalWithKey() {
    var terms = CustomSequences.of(
        new TimeSequenceKey('S', 'T'),
        List.of(new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24))));

    var beforeRange = new Epoch2kInterval(ofYMD(2023, 12, 1));
    assertEquals("ST/2023-12-01+1", terms.format(beforeRange));

    var afterRange = new Epoch2kInterval(ofYMD(2025, 1, 15));
    assertEquals("ST/2025-01-15+1", terms.format(afterRange));
  }

  // ── Default format ──────────────────────────────────────────

  @Test
  void defaultFormatEqualsKeyPlusIntervalToString() {
    // Yearly-January overrides format to "GY/2024"; the default is
    // reachable by any sequence that doesn't override, and produces
    // key + interval.toString().
    var terms =
        CustomSequences.of(
            new TimeSequenceKey('S', 'T'),
            List.of(new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24))));

    var interval = terms.interval(ofYMD(2024, 3, 15));
    assertEquals(terms.key().toString() + interval.toString(), terms.format(interval));
  }
}