package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.Epoch2kDay.DayOfWeek.Wednesday;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.April;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.July;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.November;
import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.hisp.dhis2.time.GregorianSequences.FH;
import static org.hisp.dhis2.time.GregorianSequences.FM;
import static org.hisp.dhis2.time.GregorianSequences.FQ;
import static org.hisp.dhis2.time.GregorianSequences.FY;
import static org.hisp.dhis2.time.GregorianSequences.GB;
import static org.hisp.dhis2.time.GregorianSequences.GD;
import static org.hisp.dhis2.time.GregorianSequences.GF;
import static org.hisp.dhis2.time.GregorianSequences.GM;
import static org.hisp.dhis2.time.GregorianSequences.GQ;
import static org.hisp.dhis2.time.GregorianSequences.GS;
import static org.hisp.dhis2.time.GregorianSequences.GW;
import static org.hisp.dhis2.time.GregorianSequences.GY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestLegacyPeriodParser {

  // Assumes LegacyMappings exposes calendarMapping() and fiscalMapping().
  // If the actual API differs, adjust these two helpers.

  private static LegacyPeriodParser calendar() {
    return new LegacyPeriodParser(LegacyMappings.calendarMapping());
  }

  private static LegacyPeriodParser fiscal() {
    return new LegacyPeriodParser(LegacyMappings.fiscalMapping());
  }

  // ── Yearly ────────────────────────────────────────────────────

  @Test
  void yearlyUsesCalendarSystemByDefault() {
    var slice = calendar().parse("2024");
    assertEquals(GY.noOffset(), slice.key());
    assertEquals(Epoch2kInterval.ofYear(2024), slice.interval());
  }

  @Test
  void yearlyUsesFiscalSystemWhenConfigured() {
    var slice = fiscal().parse("2024");
    assertEquals(FY.noOffset(), slice.key());
    assertEquals(Epoch2kInterval.ofYear(2024), slice.interval());
  }

  @Test
  void yearlyFiscalJuly() {
    var slice = calendar().parse("2024July");
    assertEquals(FY.offsetBy(July), slice.key());
    assertEquals(ofYMD(2024, 7, 1), slice.interval().firstDay());
    assertEquals(ofYMD(2025, 6, 30), slice.interval().lastDay());
  }

  @Test
  void yearlyFiscalApril() {
    var slice = calendar().parse("2024April");
    assertEquals(FY.offsetBy(April), slice.key());
    assertEquals(ofYMD(2024, 4, 1), slice.interval().firstDay());
    assertEquals(ofYMD(2025, 3, 31), slice.interval().lastDay());
  }

  // ── Monthly ───────────────────────────────────────────────────

  @Test
  void monthlyCompact() {
    var slice = calendar().parse("202403");
    assertEquals(GM.noOffset(), slice.key());
    assertEquals(ofYMD(2024, 3, 1), slice.interval().firstDay());
    assertEquals(ofYMD(2024, 3, 31), slice.interval().lastDay());
  }

  @Test
  void monthlyDashedIsEquivalent() {
    assertEquals(calendar().parse("202403"), calendar().parse("2024-03"));
  }

  @Test
  void monthlyUsesFiscalSystemWhenConfigured() {
    var slice = fiscal().parse("202403");
    assertEquals(FM.noOffset(), slice.key());
    assertEquals(ofYMD(2024, 3, 1), slice.interval().firstDay());
  }

  // ── Quarterly ─────────────────────────────────────────────────

  @Test
  void quarterlyFirstQuarter() {
    var slice = calendar().parse("2024Q1");
    assertEquals(GQ.noOffset(), slice.key());
    assertEquals(ofYMD(2024, 1, 1), slice.interval().firstDay());
    assertEquals(ofYMD(2024, 3, 31), slice.interval().lastDay());
  }

  @Test
  void quarterlySecondQuarter() {
    var slice = calendar().parse("2024Q2");
    assertEquals(ofYMD(2024, 4, 1), slice.interval().firstDay());
    assertEquals(ofYMD(2024, 6, 30), slice.interval().lastDay());
  }

  @Test
  void quarterlyFiscalNovemberFirst() {
    var slice = calendar().parse("2024NovQ1");
    assertEquals(FQ.offsetBy(November), slice.key());
    assertEquals(ofYMD(2024, 11, 1), slice.interval().firstDay());
    assertEquals(ofYMD(2025, 1, 31), slice.interval().lastDay());
  }

  @Test
  void quarterlyFiscalNovemberSecond() {
    var slice = calendar().parse("2024NovQ2");
    assertEquals(ofYMD(2025, 2, 1), slice.interval().firstDay());
    assertEquals(ofYMD(2025, 4, 30), slice.interval().lastDay());
  }

  // ── Semester ──────────────────────────────────────────────────

  @Test
  void semesterFirst() {
    var slice = calendar().parse("2024S1");
    assertEquals(GS.noOffset(), slice.key());
    assertEquals(ofYMD(2024, 1, 1), slice.interval().firstDay());
    assertEquals(ofYMD(2024, 6, 30), slice.interval().lastDay());
  }

  @Test
  void semesterSecond() {
    var slice = calendar().parse("2024S2");
    assertEquals(ofYMD(2024, 7, 1), slice.interval().firstDay());
    assertEquals(ofYMD(2024, 12, 31), slice.interval().lastDay());
  }

  @Test
  void semesterFiscalApril() {
    var slice = calendar().parse("2024AprilS1");
    assertEquals(FH.offsetBy(April), slice.key());
    assertEquals(ofYMD(2024, 4, 1), slice.interval().firstDay());
    assertEquals(ofYMD(2024, 9, 30), slice.interval().lastDay());
  }

  @Test
  void semesterFiscalNovember() {
    var slice = calendar().parse("2024NovS1");
    assertEquals(FH.offsetBy(November), slice.key());
    assertEquals(ofYMD(2024, 11, 1), slice.interval().firstDay());
    assertEquals(ofYMD(2025, 4, 30), slice.interval().lastDay());
  }

  // ── Bimester ──────────────────────────────────────────────────

  @Test
  void bimesterFirst() {
    var slice = calendar().parse("202401B");
    assertEquals(GB.noOffset(), slice.key());
    assertEquals(ofYMD(2024, 1, 1), slice.interval().firstDay());
    assertEquals(ofYMD(2024, 2, 29), slice.interval().lastDay());
  }

  @Test
  void bimesterSixth() {
    var slice = calendar().parse("202406B");
    assertEquals(ofYMD(2024, 11, 1), slice.interval().firstDay());
    assertEquals(ofYMD(2024, 12, 31), slice.interval().lastDay());
  }

  // ── Weekly ────────────────────────────────────────────────────

  @Test
  void weeklyMondaySingleDigit() {
    var slice = calendar().parse("2024W1");
    assertEquals(GW.noOffset(), slice.key());
    assertEquals(ofYMD(2024, 1, 1), slice.interval().firstDay());
    assertEquals(ofYMD(2024, 1, 7), slice.interval().lastDay());
  }

  @Test
  void weeklyMondayDoubleDigit() {
    var slice = calendar().parse("2024W11");
    assertEquals(ofYMD(2024, 3, 11), slice.interval().firstDay());
    assertEquals(ofYMD(2024, 3, 17), slice.interval().lastDay());
  }

  @Test
  void weeklyWednesdayUsesOffset() {
    var slice = calendar().parse("2024WedW11");
    assertEquals(GW.offsetBy(Wednesday), slice.key());
    assertEquals(ofYMD(2024, 3, 13), slice.interval().firstDay());
  }

  // ── Fortnightly ───────────────────────────────────────────────

  @Test
  void fortnightlySingleDigit() {
    var slice = calendar().parse("2024BiW1");
    assertEquals(GF.noOffset(), slice.key());
    assertEquals(ofYMD(2024, 1, 1), slice.interval().firstDay());
    assertEquals(ofYMD(2024, 1, 14), slice.interval().lastDay());
  }

  @Test
  void fortnightlyDoubleDigit() {
    var slice = calendar().parse("2024BiW11");
    assertEquals(GF.noOffset(), slice.key());
    assertEquals(14, slice.interval().lengthInDays());
  }

  // ── Daily ─────────────────────────────────────────────────────

  @Test
  void dailyCompact() {
    var slice = calendar().parse("20240315");
    assertEquals(GD.noOffset(), slice.key());
    assertEquals(ofYMD(2024, 3, 15), slice.interval().firstDay());
    assertEquals(ofYMD(2024, 3, 15), slice.interval().lastDay());
  }

  @Test
  void dailyDashed() {
    assertEquals(calendar().parse("20240315"), calendar().parse("2024-03-15"));
  }

  @Test
  void dailyLeapYearFebruary29() {
    var slice = calendar().parse("20240229");
    assertEquals(ofYMD(2024, 2, 29), slice.interval().firstDay());
  }

  // ── System-setting behaviour ──────────────────────────────────

  @Test
  void systemSettingChangesKeyNotInterval() {
    var cal = calendar().parse("2024");
    var fis = fiscal().parse("2024");
    assertEquals(cal.interval(), fis.interval());
    assertEquals(GY.noOffset(), cal.key());
    assertEquals(FY.noOffset(), fis.key());
  }

  @Test
  void explicitFiscalMonthOverridesSystemSetting() {
    // The month name in the legacy string makes it fiscal regardless
    // of which mapping the parser was constructed with.
    var cal = calendar().parse("2024July");
    var fis = fiscal().parse("2024July");
    assertEquals(cal.key(), fis.key());
    assertEquals(cal.interval(), fis.interval());
  }

  // ── CharSequence input ────────────────────────────────────────

  @Test
  void acceptsStringBuilder() {
    assertEquals(
        calendar().parse("2024Q2"),
        calendar().parse(new StringBuilder("2024Q2")));
  }

  @Test
  void acceptsCharSequenceWrapper() {
    CharSequence wrapped = new StringBuilder("2024W11");
    assertEquals(calendar().parse("2024W11"), calendar().parse(wrapped));
  }

  // ── Constructor validation ────────────────────────────────────

  @Test
  void rejectsEmptyMapping() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new LegacyPeriodParser(Map.of()));
  }

  @Test
  void rejectsPartiallyCompleteMapping() {
    var partial =
        Map.of(
            LegacyKind.DAILY, GD.noOffset(),
            LegacyKind.WEEKLY, GW.noOffset());
    assertThrows(
        IllegalArgumentException.class,
        () -> new LegacyPeriodParser(partial));
  }

  // ── Malformed inputs ──────────────────────────────────────────

  @Test
  void rejectsTooShort() {
    assertThrows(IllegalArgumentException.class, () -> calendar().parse("202"));
  }

  @Test
  void rejectsTooLong() {
    assertThrows(
        IllegalArgumentException.class, () -> calendar().parse("202403151234"));
  }

  @Test
  void rejectsNonDigitYear() {
    assertThrows(IllegalArgumentException.class, () -> calendar().parse("20X4"));
  }

  @Test
  void rejectsUnknownSuffix() {
    assertThrows(IllegalArgumentException.class, () -> calendar().parse("2024X1"));
  }

  @Test
  void rejectsMalformedDashedDaily() {
    // "2024-3-15" doesn't fit any legacy format (the dashed form requires
    // two-digit month and day).
    assertThrows(
        IllegalArgumentException.class, () -> calendar().parse("2024-3-15"));
  }

  // ── Cross-parser round-trip ───────────────────────────────────

  @Test
  void legacyParsedSliceFormatsAndReparsesToSameInterval() {
    var slice = calendar().parse("2024Q2");
    var sequence = GregorianSequences.GQ_;
    var formatter = new TimeSequenceParser(List.of(sequence));

    var formatted = sequence.format(slice.interval());
    var reparsed = formatter.parse(formatted);

    assertEquals(slice.interval(), reparsed.interval());
    assertEquals(slice.key(), reparsed.key());
  }
}