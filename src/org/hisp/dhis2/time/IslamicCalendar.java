package org.hisp.dhis2.time;

import java.util.List;

/**
 * Umm al-Qura (Saudi Hijri) calendar arithmetic on top of {@link Epoch2kDay}.
 *
 * <p>Unlike the Gregorian or Ethiopian calendars, the Umm al-Qura calendar has no
 * arithmetic leap rule. Month lengths are fixed by the KACST tables, based on
 * astronomical conditions evaluated for Mecca. This class therefore encodes the
 * tabulated month lengths directly.
 *
 * <p>Supported range: AH {@link #MIN_YEAR_IC} - {@link #MAX_YEAR_IC} inclusive. Methods
 * that take a year throw {@link IllegalArgumentException} for unsupported years;
 * {@link #yearInIC(Epoch2kDay)} throws for days outside the supported range. The range
 * is limited by the availability of the official KACST table; extending it means
 * inserting rows into {@link #TABLE} and moving {@link #SENTINEL}.
 */
public final class IslamicCalendar {

  private IslamicCalendar() {}

  /**
   * One row of the Umm al-Qura tabulation.
   *
   * @param year AH year
   * @param monthMask 12-bit mask: bit {@code i} (0-indexed) = length of month
   *     {@code i+1}; set = 30 days, clear = 29 days
   * @param start 1 Muharram of {@code year}
   */
  private record YearRow(int year, int monthMask, Epoch2kDay start) {

    int daysInYear() {
      return 12 * 29 + Integer.bitCount(monthMask);
    }
  }

  /**
   * The tabulated Umm al-Qura data, in chronological order. Extend the supported
   * range by inserting additional rows before {@link #SENTINEL}.
   *
   * <p>Source: KACST official tabulation, as mirrored by R. H. van Gent
   * (https://webspace.science.uu.nl/~gent0113/islam/ummalqura.htm) and aligned
   * with the .NET {@code UmAlQuraCalendar} table.
   */
  private static final List<YearRow> TABLE = List.of(
      new YearRow(1440, 0x2BA, Epoch2kDay.ofYMD(2018,  9, 11)),
      new YearRow(1441, 0x5B5, Epoch2kDay.ofYMD(2019,  8, 31)),
      new YearRow(1442, 0x5AA, Epoch2kDay.ofYMD(2020,  8, 20)),
      new YearRow(1443, 0xD55, Epoch2kDay.ofYMD(2021,  8,  9)),
      new YearRow(1444, 0xA9A, Epoch2kDay.ofYMD(2022,  7, 30)),
      new YearRow(1445, 0x92E, Epoch2kDay.ofYMD(2023,  7, 19)),
      new YearRow(1446, 0x25E, Epoch2kDay.ofYMD(2024,  7,  7)),
      new YearRow(1447, 0x55D, Epoch2kDay.ofYMD(2025,  6, 26)),
      new YearRow(1448, 0xADA, Epoch2kDay.ofYMD(2026,  6, 16)),
      new YearRow(1449, 0x6D4, Epoch2kDay.ofYMD(2027,  6,  6)),
      new YearRow(1450, 0x6A5, Epoch2kDay.ofYMD(2028,  5, 25)));

  /**
   * Sentinel row marking the day just past the last supported year. Its
   * {@code monthMask} is unused; only its {@code start()} is referenced.
   * When extending {@link #TABLE}, advance this row accordingly.
   */
  private static final YearRow SENTINEL =
      new YearRow(1451, -1, Epoch2kDay.ofYMD(2029, 5, 14));

  public static final int MIN_YEAR_IC = TABLE.get(0).year();
  public static final int MAX_YEAR_IC = TABLE.get(TABLE.size() - 1).year();

  /** {@code YEAR_START_2K[i]} = daysSince2k of 1 Muharram of {@code TABLE.get(i).year()}. */
  private static final int[] YEAR_START_2K;

  /** daysSince2k of 1 Muharram of {@link #SENTINEL} — one past the last supported day. */
  private static final int SENTINEL_START_2K;

  static {
    int n = TABLE.size();
    YEAR_START_2K = new int[n];
    for (int i = 0; i < n; i++) YEAR_START_2K[i] = TABLE.get(i).start().daysSince2k();
    SENTINEL_START_2K = SENTINEL.start().daysSince2k();
    validateTable();
  }

  /**
   * Fail fast at class-init if {@link #TABLE} or {@link #SENTINEL} is internally
   * inconsistent — a mask typo, a missing year, or a sentinel that doesn't line up
   * with the last row. Errors here mean the source data was transcribed wrong;
   * catching them at startup is far cheaper than a silent wrong-date bug.
   */
  private static void validateTable() {
    int n = TABLE.size();
    for (int i = 0; i < n - 1; i++) {
      YearRow curr = TABLE.get(i);
      YearRow next = TABLE.get(i + 1);
      if (next.year() != curr.year() + 1)
        throw new IllegalStateException(
            "Table year gap: " + curr.year() + " -> " + next.year());
      int expectedNext = YEAR_START_2K[i] + curr.daysInYear();
      if (YEAR_START_2K[i + 1] != expectedNext)
        throw new IllegalStateException(
            "Year " + curr.year() + ": mask " + Integer.toHexString(curr.monthMask())
                + " implies next start at " + expectedNext
                + " but table has " + YEAR_START_2K[i + 1]);
    }
    int expectedSentinel = YEAR_START_2K[n - 1] + TABLE.get(n - 1).daysInYear();
    if (SENTINEL_START_2K != expectedSentinel)
      throw new IllegalStateException(
          "Sentinel at " + SENTINEL_START_2K + " does not match computed " + expectedSentinel);
  }

  private static int indexOf(int year) {
    requireSupported(year);
    return year - MIN_YEAR_IC;
  }

  private static void requireSupported(int year) {
    if (year < MIN_YEAR_IC || year > MAX_YEAR_IC)
      throw new IllegalArgumentException(
          "Umm al-Qura year " + year + " outside supported range " + MIN_YEAR_IC + "-" + MAX_YEAR_IC);
  }

  public static boolean isSupportedYearInIC(int year) {
    return year >= MIN_YEAR_IC && year <= MAX_YEAR_IC;
  }

  /**
   * The full supported range as a closed interval. Useful for tests and for callers
   * that want to bound a query without inspecting {@link #MIN_YEAR_IC} / {@link #MAX_YEAR_IC}.
   */
  public static Epoch2kInterval supportedRange() {
    return new Epoch2kInterval(
        new Epoch2kDay(YEAR_START_2K[0]),
        new Epoch2kDay(SENTINEL_START_2K - 1));
  }

  /** daysSince2k of 1 Muharram of the given year. */
  public static int newYearOffsetInIC(int year) {
    return YEAR_START_2K[indexOf(year)];
  }

  /** @param month 1..12 */
  public static int daysInMonthInIC(int year, int month) {
    if (month < 1 || month > 12)
      throw new IllegalArgumentException("month must be 1-12, got: " + month);
    return ((TABLE.get(indexOf(year)).monthMask() >> (month - 1)) & 1) == 1 ? 30 : 29;
  }

  public static int daysInYearInIC(int year) {
    return TABLE.get(indexOf(year)).daysInYear();
  }

  /**
   * An Umm al-Qura leap year is simply a year with 355 days. Unlike the tabular
   * Islamic calendar there is no arithmetic rule; the result follows the table.
   */
  public static boolean isLeapYearInIC(int year) {
    return daysInYearInIC(year) == 355;
  }

  /** @param month 1..12, {@code dayOfMonth} 1..29/30 */
  public static Epoch2kDay toEpochDayInIC(int year, int month, int dayOfMonth) {
    int idx = indexOf(year);
    int days = YEAR_START_2K[idx];
    for (int m = 1; m < month; m++) days += daysInMonthInIC(year, m);
    return new Epoch2kDay(days + dayOfMonth - 1);
  }

  /** @return the Umm al-Qura year containing the given day */
  public static int yearInIC(Epoch2kDay day) {
    int d = day.daysSince2k();
    if (d < YEAR_START_2K[0] || d >= SENTINEL_START_2K)
      throw new IllegalArgumentException(
          "day " + day + " (" + d + ") outside supported Umm al-Qura range ["
              + YEAR_START_2K[0] + ", " + SENTINEL_START_2K + ")");
    int lo = 0, hi = YEAR_START_2K.length - 1, result = 0;
    while (lo <= hi) {
      int mid = (lo + hi) >>> 1;
      if (YEAR_START_2K[mid] <= d) {
        result = mid;
        lo = mid + 1;
      } else {
        hi = mid - 1;
      }
    }
    return MIN_YEAR_IC + result;
  }

  /** @return 1..12 */
  public static int monthInIC(Epoch2kDay day) {
    int y = yearInIC(day);
    int remaining = day.daysSince2k() - YEAR_START_2K[indexOf(y)];
    for (int m = 1; m <= 12; m++) {
      int len = daysInMonthInIC(y, m);
      if (remaining < len) return m;
      remaining -= len;
    }
    throw new IllegalStateException(
        "day of year " + (day.daysSince2k() - YEAR_START_2K[indexOf(y)])
            + " not covered by year " + y);
  }

  /** @return 1..30 */
  public static int dayOfMonthInIC(Epoch2kDay day) {
    int y = yearInIC(day);
    int m = monthInIC(day);
    int remaining = day.daysSince2k() - YEAR_START_2K[indexOf(y)];
    for (int i = 1; i < m; i++) remaining -= daysInMonthInIC(y, i);
    return remaining + 1;
  }

  public static Epoch2kInterval toEpochIntervalOfIslamicMonthContaining(Epoch2kDay day) {
    int y = yearInIC(day);
    int m = monthInIC(day);
    Epoch2kDay first = toEpochDayInIC(y, m, 1);
    return new Epoch2kInterval(first, first.plusDays(daysInMonthInIC(y, m) - 1));
  }

  public static Epoch2kInterval toEpochIntervalOfIslamicYearContaining(Epoch2kDay day) {
    int y = yearInIC(day);
    Epoch2kDay first = new Epoch2kDay(YEAR_START_2K[indexOf(y)]);
    return new Epoch2kInterval(first, first.plusDays(daysInYearInIC(y) - 1));
  }
}