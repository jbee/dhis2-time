package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.Epoch2kDay.DayOfWeek.Monday;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.January;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.November;

/**
 * Formats a {@link TimeSlice} into the legacy DHIS2 period identifier string —
 * the compact forms such as {@code 2024}, {@code 202403}, {@code 2024Q1},
 * {@code 2024W11}, {@code 2024NovQ1}, {@code 202401B}, {@code 2024BiW1},
 * {@code 20240315}.
 *
 * <p>The bridge is selective: not every slice has a bridge form. Where the
 * legacy format cannot express the slice — e.g. calendar yearly starting in
 * July, weekly starting on Tuesday, fiscal quarterly starting in any month
 * other than November — {@link #format(TimeSlice)} returns {@code null}.
 *
 * <p>Round-trip invariant: for any slice {@code s} where
 * {@code format(s) != null}, parsing the returned string with a
 * {@link BridgeParser} constructed over a compatible {@link BridgeFormat}
 * mapping produces {@code s} back. The system setting of the parser
 * determines whether {@code 2024Q1} means a calendar quarter ({@code GQ}) or
 * a fiscal quarter ({@code FQ}); the formatter captures the shape, the parser's
 * mapping resolves the system.
 *
 * <p>Validity check: each format case first confirms that the interval is
 * exactly what the tag's sequence would produce for its first day, via
 * {@code firstDay().containingX(...)}. A slice whose interval doesn't match
 * its tag produces {@code null} rather than a misleading string.
 *
 * <p>Canonical choices for the forms with alternatives:
 * <ul>
 *   <li>Daily: compact {@code 20240315} (not {@code 2024-03-15})</li>
 *   <li>Monthly: compact {@code 202403} (not {@code 2024-03})</li>
 *   <li>Weekly / fortnightly: no leading zero ({@code W1}, {@code BiW1})</li>
 * </ul>
 */
public final class BridgeFormatter {

  /**
   * @return the bridge-format string for {@code slice}, or {@code null} if this
   *     slice has no bridge form (unsupported tag, or interval not matching the
   *     tag's sequence)
   */
  public String format(TimeSlice slice) {
    var tag = slice.tag();
    var interval = slice.interval();
    return switch (tag.interval()) {
      case 'D' -> daily(interval);
      case 'W' -> weekly(tag, interval);
      case 'F' -> fortnightly(tag, interval);
      case 'M' -> monthly(interval);
      case 'B' -> bimester(interval);
      case 'S' -> calendarSemester(interval);
      case 'H' -> fiscalHalfYear(tag, interval);
      case 'Q' -> quarterly(tag, interval);
      case 'Y' -> yearly(tag, interval);
      default -> null;
    };
  }

  // ── Daily ─────────────────────────────────────────────────────────

  private static String daily(Epoch2kInterval interval) {
    if (interval.lengthInDays() != 1) return null;
    var d = interval.firstDay();
    return "%04d%02d%02d".formatted(
        d.year(), d.monthOffset() + 1, d.dayOfMonthOffset() + 1);
  }

  // ── Weekly ────────────────────────────────────────────────────────

  private static String weekly(TimeSequenceTag tag, Epoch2kInterval interval) {
    var day1 = tag.offsetAsDay();
    String dayToken = switch (day1) {
      case Monday -> "";
      case Wednesday -> "Wed";
      case Thursday -> "Thu";
      case Saturday -> "Sat";
      case Sunday -> "Sun";
      default -> null;
    };
    if (dayToken == null) return null;

    var firstDay = interval.firstDay();
    if (!firstDay.containingWeek(day1).equals(interval)) return null;

    int weekYear = firstDay.yearOfWeekMajority(day1);
    int weekNumber = firstDay.weekOfYearNumber(day1);
    return "%04d%sW%d".formatted(weekYear, dayToken, weekNumber);
  }

  // ── Fortnightly ───────────────────────────────────────────────────

  private static String fortnightly(TimeSequenceTag tag, Epoch2kInterval interval) {
    if (tag.offsetAsDay() != Monday) return null;
    var firstDay = interval.firstDay();
    if (!firstDay.containingFortnight(Monday).equals(interval)) return null;
    int weekYear = firstDay.yearOfWeekMajority(Monday);
    int weekNumber = firstDay.weekOfYearNumber(Monday);
    int fortnightNumber = (weekNumber - 1) / 2 + 1;
    return "%04dBiW%d".formatted(weekYear, fortnightNumber);
  }

  // ── Monthly ───────────────────────────────────────────────────────

  private static String monthly(Epoch2kInterval interval) {
    var d = interval.firstDay();
    if (!d.containingMonth().equals(interval)) return null;
    return "%04d%02d".formatted(d.year(), d.monthOffset() + 1);
  }

  // ── Bimester ──────────────────────────────────────────────────────

  private static String bimester(Epoch2kInterval interval) {
    var d = interval.firstDay();
    if (!d.containingBimester(January).equals(interval)) return null;
    int bimesterNumber = d.monthOffset() / 2 + 1;
    return "%04d%02dB".formatted(d.year(), bimesterNumber);
  }

  // ── Calendar semester ─────────────────────────────────────────────

  private static String calendarSemester(Epoch2kInterval interval) {
    var d = interval.firstDay();
    if (!d.containingSemester(January).equals(interval)) return null;
    int semesterNumber = d.monthOffset() / 6 + 1;
    return "%04dS%d".formatted(d.year(), semesterNumber);
  }

  // ── Fiscal half-year ──────────────────────────────────────────────

  private static String fiscalHalfYear(TimeSequenceTag tag, Epoch2kInterval interval) {
    var month1 = tag.offsetAsMonth();
    String monthToken = switch (month1) {
      case April -> "April";
      case November -> "Nov";
      default -> null;
    };
    if (monthToken == null) return null;

    var firstDay = interval.firstDay();
    if (!firstDay.containingSemester(month1).equals(interval)) return null;

    int fiscalYear = firstDay.yearCycleStart(month1).year();
    int monthsSinceStart = Math.floorMod(firstDay.monthOffset() - month1.ordinal(), 12);
    int semesterNumber = monthsSinceStart / 6 + 1;
    return "%04d%sS%d".formatted(fiscalYear, monthToken, semesterNumber);
  }

  // ── Quarterly ─────────────────────────────────────────────────────

  private static String quarterly(TimeSequenceTag tag, Epoch2kInterval interval) {
    return switch (tag.system()) {
      case 'G' -> calendarQuarterly(tag, interval);
      case 'F' -> fiscalQuarterly(tag, interval);
      default -> null;
    };
  }

  private static String calendarQuarterly(TimeSequenceTag tag, Epoch2kInterval interval) {
    var month1 = tag.offsetAsMonth();
    if (month1 != January) return null;
    var firstDay = interval.firstDay();
    if (!firstDay.containingQuarter(month1).equals(interval)) return null;
    int quarterNumber = firstDay.monthOffset() / 3 + 1;
    return "%04dQ%d".formatted(firstDay.year(), quarterNumber);
  }

  private static String fiscalQuarterly(TimeSequenceTag tag, Epoch2kInterval interval) {
    var month1 = tag.offsetAsMonth();
    if (month1 != November) return null;
    var firstDay = interval.firstDay();
    if (!firstDay.containingQuarter(month1).equals(interval)) return null;
    int fiscalYear = firstDay.yearCycleStart(month1).year();
    int monthsSinceStart = Math.floorMod(firstDay.monthOffset() - month1.ordinal(), 12);
    int quarterNumber = monthsSinceStart / 3 + 1;
    return "%04dNovQ%d".formatted(fiscalYear, quarterNumber);
  }

  // ── Yearly ────────────────────────────────────────────────────────

  private static String yearly(TimeSequenceTag tag, Epoch2kInterval interval) {
    return switch (tag.system()) {
      case 'G' -> calendarYearly(tag, interval);
      case 'F' -> fiscalYearly(tag, interval);
      default -> null;
    };
  }

  private static String calendarYearly(TimeSequenceTag tag, Epoch2kInterval interval) {
    var month1 = tag.offsetAsMonth();
    if (month1 != January) return null;
    var firstDay = interval.firstDay();
    if (!firstDay.containingYear(month1).equals(interval)) return null;
    return "%04d".formatted(firstDay.year());
  }

  private static String fiscalYearly(TimeSequenceTag tag, Epoch2kInterval interval) {
    var month1 = tag.offsetAsMonth();
    String monthToken = switch (month1) {
      case February -> "Feb";
      case April -> "April";
      case July -> "July";
      case August -> "Aug";
      case September -> "Sep";
      case October -> "Oct";
      case November -> "Nov";
      default -> null;
    };
    if (monthToken == null) return null;
    var firstDay = interval.firstDay();
    if (!firstDay.containingYear(month1).equals(interval)) return null;
    int fiscalYear = firstDay.yearCycleStart(month1).year();
    return "%04d%s".formatted(fiscalYear, monthToken);
  }
}