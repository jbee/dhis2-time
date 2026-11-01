package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.Epoch2kDay.DayOfWeek.Friday;
import static org.hisp.dhis2.time.Epoch2kDay.DayOfWeek.Monday;
import static org.hisp.dhis2.time.Epoch2kDay.DayOfWeek.Saturday;
import static org.hisp.dhis2.time.Epoch2kDay.DayOfWeek.Sunday;
import static org.hisp.dhis2.time.Epoch2kDay.DayOfWeek.Thursday;
import static org.hisp.dhis2.time.Epoch2kDay.DayOfWeek.Wednesday;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.April;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.August;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.February;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.January;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.July;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.November;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.October;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.September;
import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;

import java.util.List;
import java.util.Map;

/**
 * A {@link TimeSlice} parser that parses legacy DHIS2 period identifiers — the compact string forms
 * such as {@code 2024}, {@code 202403}, {@code 2024Q1}, {@code 2024W11}, {@code 2024NovQ1}, {@code
 * 202401B}, {@code 2024BiW1}, {@code 20240315} — into a {@link TimeSlice}.
 *
 * <p>The parser is decoupled from {@link TimeSequence}: it only needs a {@code Map<BridgeFormat,
 * TimeFamilyKey>} that says which concrete sequence family each legacy form belongs to. Offsets
 * (week start day, fiscal start month) are resolved from the parsed content; the parser applies the
 * offset before producing the {@code TimeSlice}.
 *
 * <p>The mapping is expected to already encode the caller's system setting: for calendar-mode
 * parsing, pass {@code GY} for {@code YEARLY}; for fiscal-mode, pass {@code FY.noOffset()}. Formats
 * that name a fiscal start explicitly ({@code 2024NovQ1}, {@code 2024AprilS1}, {@code 2024July})
 * override the base key's offset from the parsed content and are always fiscal in intent — the
 * caller's mapping is what makes that true, by mapping those kinds to fiscal family keys.
 *
 * <p>Intervals are computed using {@link Epoch2kDay}'s containing-* methods, the same primitives
 * the {@link TimeSequence} implementations use. This keeps the parser free of duplicated period
 * arithmetic while remaining independent of any sequence instance.
 */
public final class BridgeParser {

  private final Map<BridgeFormat, TimeFamilyKey> mapping;

  public static BridgeParser calendar() {
    return new BridgeParser(BridgeFormat.calendarMapping());
  }

  /**
   * @param mapping which sequence family each legacy form belongs to; keys are
   *     the no-offset variants of the target families (offsets are applied by
   *     the parser from the parsed content)
   * @throws IllegalArgumentException if the mapping is missing any {@link BridgeFormat}
   */
  public BridgeParser(Map<BridgeFormat, TimeFamilyKey> mapping) {
    for (var kind : BridgeFormat.values()) {
      if (!mapping.containsKey(kind))
        throw new IllegalArgumentException("mapping missing BridgeFormat." + kind);
    }
    this.mapping = Map.copyOf(mapping);
  }

  public TimeSlice parse(CharSequence p) {
    int len = p.length();
    if (len < 4 || len > 11)
      throw new IllegalArgumentException(
          "Period must have between 4 and 11 characters but got: `" + p + "`");

    int year = digits4(p, 0);

    if (len == 4) return yearlySlice(year);

    if (len == 6) {
      char c4 = p.charAt(4);
      if (c4 == 'W') return weeklySlice(year, digits1(p, 5), Monday);
      if (isDigit(c4)) return monthlySlice(year, digits2(p, 4));
      if (c4 == 'Q') return quarterlySlice(year, digits1(p, 5));
      if (c4 == 'S') return semesterSlice(year, digits1(p, 5));
      throw illegalInfix(p, "W", "digit", "Q", "S");
    }

    if (len == 7) {
      if (endsWith(p, "Feb")) return yearlyFiscalSlice(year, February);
      if (endsWith(p, "Aug")) return yearlyFiscalSlice(year, August);
      if (endsWith(p, "Sep")) return yearlyFiscalSlice(year, September);
      if (endsWith(p, "Oct")) return yearlyFiscalSlice(year, October);
      if (endsWith(p, "Nov")) return yearlyFiscalSlice(year, November);
      if (endsWith(p, "B")) return bimesterSlice(year, digits2(p, 4));
      if (p.charAt(4) == '-') return monthlySlice(year, digits2(p, 5));
      if (p.charAt(4) == 'W') return weeklySlice(year, digits2(p, 5), Monday);
      throw illegalInfix(p, "Feb", "Aug", "Sep", "Oct", "Nov", "B", "-", "W");
    }

    if (len == 8) {
      if (endsWith(p, "July")) return yearlyFiscalSlice(year, July);
      if (matchesAt(p, 4, "BiW")) return fortnightlySlice(year, digits1(p, 7));
      return dailySlice(year, digits2(p, 4), digits2(p, 6));
    }

    if (len == 9) {
      if (endsWith(p, "April")) return yearlyFiscalSlice(year, April);
      if (matchesAt(p, 4, "NovQ")) return quarterlyFiscalSlice(year, digits1(p, 8), November);
      if (matchesAt(p, 4, "BiW")) return fortnightlySlice(year, digits2(p, 7));
      if (matchesAt(p, 4, "WedW")) return weeklySlice(year, digits1(p, 8), Wednesday);
      if (matchesAt(p, 4, "ThuW")) return weeklySlice(year, digits1(p, 8), Thursday);
      if (matchesAt(p, 4, "FriW")) return weeklySlice(year, digits1(p, 8), Friday);
      if (matchesAt(p, 4, "SatW")) return weeklySlice(year, digits1(p, 8), Saturday);
      if (matchesAt(p, 4, "SunW")) return weeklySlice(year, digits1(p, 8), Sunday);
      if (matchesAt(p, 4, "NovS")) return semesterFiscalSlice(year, digits1(p, 8), November);
      throw illegalInfix(p, "April", "NovQ", "BiW", "WedW", "ThuW", "FriW",
          "SatW", "SunW", "NovS");
    }

    if (len == 10) {
      if (p.charAt(4) == '-') {
        if (p.charAt(7) != '-') throw illegalChar(p, "dash", 7);
        return dailySlice(year, digits2(p, 5), digits2(p, 8));
      }
      if (matchesAt(p, 4, "WedW")) return weeklySlice(year, digits2(p, 8), Wednesday);
      if (matchesAt(p, 4, "ThuW")) return weeklySlice(year, digits2(p, 8), Thursday);
      if (matchesAt(p, 4, "FriW")) return weeklySlice(year, digits2(p, 8), Friday);
      if (matchesAt(p, 4, "SatW")) return weeklySlice(year, digits2(p, 8), Saturday);
      if (matchesAt(p, 4, "SunW")) return weeklySlice(year, digits2(p, 8), Sunday);
      throw illegalInfix(p, "WedW", "ThuW", "FriW", "SatW", "SunW");
    }

    if (len == 11) {
      if (matchesAt(p, 4, "AprilS")) return semesterFiscalSlice(year, digits1(p, 10), April);
    }
    throw illegalInfix(p, "AprilS");
  }

  // ── Per-kind TimeSlice construction ──────────────────────────────

  private TimeSlice yearlySlice(int year) {
    var key = mapping.get(BridgeFormat.YEARLY);
    var anchor = ofYMD(year, 1, 1);
    return new TimeSlice(key.noOffset(), anchor.containingYear(January));
  }

  private TimeSlice yearlyFiscalSlice(int year, Epoch2kDay.MonthOfYear month1) {
    var key = mapping.get(BridgeFormat.YEARLY_FISCAL).offsetBy(month1);
    var anchor = ofYMD(year, month1.ordinal() + 1, 1);
    return new TimeSlice(key, anchor.containingYear(month1));
  }

  private TimeSlice monthlySlice(int year, int month) {
    var key = mapping.get(BridgeFormat.MONTHLY);
    var anchor = ofYMD(year, month, 1);
    return new TimeSlice(key.noOffset(), anchor.containingMonth());
  }

  private TimeSlice quarterlySlice(int year, int quarter) {
    var key = mapping.get(BridgeFormat.QUARTERLY);
    var anchor = ofYMD(year, (quarter - 1) * 3 + 1, 1);
    return new TimeSlice(key.noOffset(), anchor.containingQuarter(January));
  }

  private TimeSlice quarterlyFiscalSlice(int year, int quarter, Epoch2kDay.MonthOfYear month1) {
    var key = mapping.get(BridgeFormat.QUARTERLY_FISCAL).offsetBy(month1);
    var anchor = ofYMD(year, month1.ordinal() + 1, 1).plusMonths((quarter - 1) * 3);
    return new TimeSlice(key, anchor.containingQuarter(month1));
  }

  private TimeSlice semesterSlice(int year, int semester) {
    var key = mapping.get(BridgeFormat.SEMESTER);
    var anchor = ofYMD(year, (semester - 1) * 6 + 1, 1);
    return new TimeSlice(key.noOffset(), anchor.containingSemester(January));
  }

  private TimeSlice semesterFiscalSlice(
      int year, int semester, Epoch2kDay.MonthOfYear month1) {
    var key = mapping.get(BridgeFormat.SEMESTER_FISCAL).offsetBy(month1);
    var anchor = ofYMD(year, month1.ordinal() + 1, 1).plusMonths((semester - 1) * 6);
    return new TimeSlice(key, anchor.containingSemester(month1));
  }

  private TimeSlice bimesterSlice(int year, int bimester) {
    var key = mapping.get(BridgeFormat.BIMESTER);
    var anchor = ofYMD(year, (bimester - 1) * 2 + 1, 1);
    return new TimeSlice(key.noOffset(), anchor.containingBimester(January));
  }

  private TimeSlice weeklySlice(int year, int week, Epoch2kDay.DayOfWeek day1) {
    var key = mapping.get(BridgeFormat.WEEKLY).offsetBy(day1);
    var anchor = Epoch2kDay.ofWeek1InYear(year, day1).plusDays((week - 1) * 7);
    return new TimeSlice(key, new Epoch2kInterval(anchor, anchor.plusDays(6)));
  }

  private TimeSlice fortnightlySlice(int year, int fortnight) {
    var key = mapping.get(BridgeFormat.FORTNIGHTLY);
    var anchor = Epoch2kDay.ofWeek1InYear(year, Monday).plusDays((fortnight - 1) * 14);
    return new TimeSlice(key.noOffset(), new Epoch2kInterval(anchor, anchor.plusDays(13)));
  }

  private TimeSlice dailySlice(int year, int month, int day) {
    var key = mapping.get(BridgeFormat.DAILY);
    return new TimeSlice(key.noOffset(), new Epoch2kInterval(ofYMD(year, month, day)));
  }

  // ── CharSequence helpers ─────────────────────────────────────────

  private static boolean matchesAt(CharSequence s, int offset, String literal) {
    if (offset < 0 || offset + literal.length() > s.length()) return false;
    for (int i = 0; i < literal.length(); i++)
      if (s.charAt(offset + i) != literal.charAt(i)) return false;
    return true;
  }

  private static boolean endsWith(CharSequence s, String suffix) {
    return matchesAt(s, s.length() - suffix.length(), suffix);
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static int digits1(CharSequence p, int index) {
    char c = p.charAt(index);
    if (!isDigit(c)) throw illegalChar(p, "digit", index);
    return c - '0';
  }

  private static int digits2(CharSequence p, int index) {
    return digits1(p, index) * 10 + digits1(p, index + 1);
  }

  private static int digits4(CharSequence p, int index) {
    int result = 0;
    for (int i = 0; i < 4; i++) result = result * 10 + digits1(p, index + i);
    return result;
  }

  private static IllegalArgumentException illegalChar(
      CharSequence period, String expected, int index) {
    return new IllegalArgumentException(
        "Invalid Period `" + period + "`, expected a " + expected
            + " at position " + index + " but found: `" + period.charAt(index) + "`");
  }

  private static IllegalArgumentException illegalInfix(CharSequence period, String... oneOf) {
    return new IllegalArgumentException(
        "Invalid Period `" + period + "`, expected one of " + List.of(oneOf)
            + " at index 4 for an ISO period of length " + period.length()
            + " but found: `" + period.subSequence(4, period.length()) + "`");
  }
}