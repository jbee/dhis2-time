package org.hisp.dhis2.time;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hisp.dhis2.time.Epoch2kDay.DayOfWeek.Tuesday;
import static org.hisp.dhis2.time.Epoch2kDay.DayOfWeek.Wednesday;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.April;
import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.hisp.dhis2.time.GregorianSequences.GB;
import static org.hisp.dhis2.time.GregorianSequences.GB_;
import static org.hisp.dhis2.time.GregorianSequences.GD;
import static org.hisp.dhis2.time.GregorianSequences.GF;
import static org.hisp.dhis2.time.GregorianSequences.GF_;
import static org.hisp.dhis2.time.GregorianSequences.GM;
import static org.hisp.dhis2.time.GregorianSequences.GM_;
import static org.hisp.dhis2.time.GregorianSequences.GQ;
import static org.hisp.dhis2.time.GregorianSequences.GQ_;
import static org.hisp.dhis2.time.GregorianSequences.GS;
import static org.hisp.dhis2.time.GregorianSequences.GS_;
import static org.hisp.dhis2.time.GregorianSequences.GW;
import static org.hisp.dhis2.time.GregorianSequences.GW_;
import static org.hisp.dhis2.time.GregorianSequences.GY;
import static org.hisp.dhis2.time.GregorianSequences.GY_;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class BridgeFormatterTest {

  private static final BridgeFormatter FORMATTER = new BridgeFormatter();

  @Test
  void dailyCompact() {
    var slice = new TimeSlice(GD.noOffset(), new Epoch2kInterval(ofYMD(2024, 3, 15)));
    assertEquals("20240315", FORMATTER.format(slice));
  }

  @Test
  void monthlyCompact() {
    var interval = new Epoch2kInterval(ofYMD(2024, 3, 1), ofYMD(2024, 3, 31));
    var slice = new TimeSlice(GM.noOffset(), interval);
    assertEquals("202403", FORMATTER.format(slice));
  }

  @Test
  void weeklyMondayAndWednesday() {
    var monday = new TimeSlice(
        GW.noOffset(),
        GW_.interval(ofYMD(2024, 3, 15)));
    assertEquals("2024W11", FORMATTER.format(monday));

    var wednesdayKey = GW.offsetBy(Wednesday);
    var wednesday = new TimeSlice(
        wednesdayKey,
        GregorianSequences.weekly(Wednesday).interval(ofYMD(2024, 3, 15)));
    assertEquals("2024WedW11", FORMATTER.format(wednesday));
  }

  @Test
  void weeklyTuesdayHasNoBridgeForm() {
    var tuesday = GregorianSequences.weekly(Tuesday);
    var slice = new TimeSlice(tuesday.key(), tuesday.interval(ofYMD(2024, 3, 15)));
    assertNull(FORMATTER.format(slice));
  }

  @Test
  void fortnightlyStubFormats() {
    var stub = new TimeSlice(
        GF.noOffset(),
        GF_.interval(ofYMD(2020, 12, 30)));
    assertEquals("2020BiW27", FORMATTER.format(stub));
  }

  @Test
  void mismatchedFortnightlySliceReturnsNull() {
    // Fortnightly key with a 10-day interval — not a full 14-day fortnight.
    var badSlice = new TimeSlice(
        GF.noOffset(),
        new Epoch2kInterval(ofYMD(2024, 3, 11), ofYMD(2024, 3, 20)));
    assertNull(FORMATTER.format(badSlice));
  }

  @Test
  void fortnightlyStubIsBridgeableButNormalFortnightsAreToo() {
    // Both the 14-day and the 7-day (53-week-year stub) forms are valid;
    // the strictness check must accept both.
    var normal = new TimeSlice(GF.noOffset(), GF_.interval(ofYMD(2024, 3, 15)));
    var stub = new TimeSlice(GF.noOffset(), GF_.interval(ofYMD(2020, 12, 30)));
    assertNotNull(FORMATTER.format(normal));
    assertNotNull(FORMATTER.format(stub));
  }

  @Test
  void quarterlyCalendar() {
    var slice = new TimeSlice(
        GQ.noOffset(),
        GQ_.interval(ofYMD(2024, 5, 15)));
    assertEquals("2024Q2", FORMATTER.format(slice));
  }

  @Test
  void quarterlyFiscalNovember() {
    var slice = new TimeSlice(
        GregorianSequences.FQb.key(),
        GregorianSequences.FQb.interval(ofYMD(2024, 12, 15)));
    assertEquals("2024NovQ1", FORMATTER.format(slice));
  }

  @Test
  void fiscalHalfYearApril() {
    var slice = new TimeSlice(
        GregorianSequences.FH4.key(),
        GregorianSequences.FH4.interval(ofYMD(2024, 5, 15)));
    assertEquals("2024AprilS1", FORMATTER.format(slice));
  }

  @Test
  void yearlyCalendar() {
    var slice = new TimeSlice(
        GY.noOffset(),
        Epoch2kInterval.ofYear(2024));
    assertEquals("2024", FORMATTER.format(slice));
  }

  @Test
  void yearlyFiscalJuly() {
    var slice = new TimeSlice(
        GregorianSequences.FY7.key(),
        GregorianSequences.FY7.interval(ofYMD(2024, 8, 15)));
    assertEquals("2024July", FORMATTER.format(slice));
  }

  @Test
  void calendarYearlyWithOffsetHasNoBridgeForm() {
    var gy4 = GregorianSequences.yearly(April);
    var slice = new TimeSlice(gy4.key(), gy4.interval(ofYMD(2024, 5, 15)));
    assertNull(FORMATTER.format(slice));
  }

  @Test
  void mismatchedSliceReturnsNull() {
    // Monthly key with a 30-day interval for March (which is 31 days).
    var badSlice = new TimeSlice(
        GM.noOffset(),
        new Epoch2kInterval(ofYMD(2024, 3, 1), ofYMD(2024, 3, 30)));
    assertNull(FORMATTER.format(badSlice));
  }

  @Test
  void everyBridgeableSliceRoundTripsThroughParser() {
    var parser = new BridgeParser(BridgeFormat.calendarMapping());
    var fiscalParser = new BridgeParser(BridgeFormat.fiscalMapping());

    for (var example : List.of(
        new TimeSlice(GD.noOffset(), new Epoch2kInterval(ofYMD(2024, 3, 15))),
        new TimeSlice(GM.noOffset(), GM_.interval(ofYMD(2024, 3, 15))),
        new TimeSlice(GW.noOffset(), GW_.interval(ofYMD(2024, 3, 15))),
        new TimeSlice(GF.noOffset(), GF_.interval(ofYMD(2024, 3, 15))),
        new TimeSlice(GB.noOffset(), GB_.interval(ofYMD(2024, 3, 15))),
        new TimeSlice(GS.noOffset(), GS_.interval(ofYMD(2024, 3, 15))),
        new TimeSlice(GQ.noOffset(), GQ_.interval(ofYMD(2024, 5, 15))),
        new TimeSlice(GY.noOffset(), GY_.interval(ofYMD(2024, 6, 15))))) {
      var bridge = FORMATTER.format(example);
      assertNotNull(bridge, () -> "expected bridge form for " + example);
      assertEquals(example, parser.parse(bridge), () -> "round-trip failed for " + example);
    }

    for (var example : List.of(
        new TimeSlice(GregorianSequences.FHb.key(), GregorianSequences.FHb.interval(ofYMD(2024, 12, 15))),
        new TimeSlice(GregorianSequences.FH4.key(), GregorianSequences.FH4.interval(ofYMD(2024, 5, 15))),
        new TimeSlice(GregorianSequences.FY7.key(), GregorianSequences.FY7.interval(ofYMD(2024, 8, 15))))) {
      var bridge = FORMATTER.format(example);
      assertNotNull(bridge, () -> "expected fiscal bridge form for " + example);
      assertEquals(example, fiscalParser.parse(bridge), () -> "fiscal round-trip failed for " + example);
    }
  }
}