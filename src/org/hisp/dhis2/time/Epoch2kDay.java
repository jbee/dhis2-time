package org.hisp.dhis2.time;

import static java.lang.Math.abs;

import java.util.function.LongSupplier;

public record Epoch2kDay(@ZeroBased int daysSince2k) implements Comparable<Epoch2kDay> {

  private static final long MILLIS_UTC_0_UNTIL_2K_MIDNIGHT = 946_684_800_000L;
  private static final long MILLIS_IN_A_DAY = 86_400_000L;

  public static Epoch2kDay today() {
    return today(System::currentTimeMillis);
  }

  public static Epoch2kDay today(LongSupplier nowUTC) {
    long millisSince2k = nowUTC.getAsLong() - MILLIS_UTC_0_UNTIL_2K_MIDNIGHT;
    int daysSince2k = (int) (millisSince2k / MILLIS_IN_A_DAY);
    return new Epoch2kDay(daysSince2k);
  }

  static Epoch2kDay ofYMD(int year,@OneBased int month, @OneBased int dayOfMonth) {
    if (month < 1 || month > 12)
      throw new IllegalArgumentException("month must be 1-12, got: " + month);
    int max = daysInMonth(year, month - 1);   // ← 1-based → 0-based at the boundary
    if (dayOfMonth < 1 || dayOfMonth > max)
      throw new IllegalArgumentException(
          "day must be 1-" + max + " for " + year + "-" + month + ", got: " + dayOfMonth);
    return ofJanuary1stInYear(year).plusMonths(month - 1).plusDays(dayOfMonth - 1);
  }

  /** @return January 1st of the given year */
  public static Epoch2kDay ofJanuary1stInYear(int year) {
    return new Epoch2kDay(0).plusYears(year - 2000); // 0 == 2000-01-01
  }

  /**
   * Start of the first week of the given year.
   *
   * <p>A week belongs to the year containing the majority of its days. A 7-day
   * week can straddle at most one year boundary and cannot split evenly, so
   * every week has a unique owning year. Week 1 of year Y is therefore the
   * earliest week whose majority of days lies in Y.
   *
   * <p>Equivalently for any {@code day1}: week 1 is the earliest week whose
   * 4th day — the day 3 days after {@code day1} — falls in Y. For a Monday-start
   * week this is the week containing January 4th, which is the phrasing usually
   * encountered in ISO 8601; the majority rule is the underlying reason, and
   * it is what generalises to {@code day1} choices other than Monday.
   *
   * @param year the year whose first week's start is returned
   * @param day1 the day on which each week starts (Monday for ISO weeks)
   * @return the first {@code day1} of week 1 of {@code year}
   */
  public static Epoch2kDay ofWeek1InYear(int year, DayOfWeek day1) {
    return ofJanuary1stInYear(year).plusDays(3).previousOrSame(day1);
  }

  /**
   * Days of the week in ISO order: {@link #Monday} has ordinal 0, {@link #Sunday}
   * has ordinal 6. Used both as a label and — via
   * {@link Epoch2kDay#dayOfWeekOffset(DayOfWeek)} and the week factories in
   * {@link GregorianSequences} — as the base for day-offset arithmetic.
   */
  @ZeroBased
  public enum DayOfWeek {
    // In ISO order with Monday being the ISO start of the week
    Monday,
    Tuesday,
    Wednesday,
    Thursday,
    Friday,
    Saturday,
    Sunday
  }

  @ZeroBased
  public enum MonthOfYear {
    January,
    February,
    March,
    April,
    May,
    June,
    July,
    August,
    September,
    October,
    November,
    December
  }

  /**
   * Calendar coordinates. {@code years} is the absolute Gregorian year (e.g. 2024);
   * {@code months} is 0-based (0 = January); {@code days} is 0-based within the
   * month (0 = 1st). Use {@link #toString} for the human-readable YYYY-MM-DD form,
   * which applies the +1 conversions.
   */
  public record OffsetYMD(int years, @ZeroBased int months, @ZeroBased int days) {

    int daysInMonth() {
      return Epoch2kDay.daysInMonth(years, months);
    }

    @Override
    public String toString() {
      return "%04d-%02d-%02d".formatted(years, months+1, days+1);
    }
  }

  public boolean before(Epoch2kDay other) {
    return daysSince2k < other.daysSince2k;
  }

  public boolean notBefore(Epoch2kDay other) {
    return daysSince2k >= other.daysSince2k;
  }

  public boolean after(Epoch2kDay other) {
    return daysSince2k > other.daysSince2k;
  }

  public boolean notAfter(Epoch2kDay other) {
    return daysSince2k <= other.daysSince2k;
  }

  @Override
  public String toString() {
    return offset().toString();
  }

  @Override
  public int compareTo(Epoch2kDay other) {
    return Integer.compare(daysSince2k, other.daysSince2k);
  }

  public long toUTCMillis() {
    return MILLIS_UTC_0_UNTIL_2K_MIDNIGHT + daysSince2k * MILLIS_IN_A_DAY;
  }

  public Epoch2kDay plusDays(int n) {
    return n == 0 ? this : new Epoch2kDay(daysSince2k + n);
  }

  public Epoch2kDay minusDays(int n) {
    return plusDays(-n);
  }

  public Epoch2kDay yesterday() {
    return minusDays(1);
  }

  public Epoch2kDay tomorrow() {
    return plusDays(1);
  }

  public Epoch2kDay day1InYear() {
    return minusDays(dayOfYearOffset());
  }

  /**
   * @param month the month to find the 1st day of
   * @return the 1st day in the given month in the same year as this day's year
   */
  public Epoch2kDay day1InMonth(MonthOfYear month) {
    int year = year();
    int daysToDay1 = 0;
    for (int m = 0; m < month.ordinal(); m++) daysToDay1 += daysInMonth(year, m);
    return new Epoch2kDay(daysSince2k - dayOfYearOffset() + daysToDay1);
  }

  /*
   * ISO based calculations assuming dates between 1901-2099
   */

  public OffsetYMD offset() {
    int year = year();
    return new OffsetYMD(year, monthOffset(year), dayOfMonthOffset(year));
  }

  private static final int DAYS_IN_4_YEARS = 366 + 3 * 365;

  public int year() {
    int daysRem = daysSince2k % DAYS_IN_4_YEARS;
    int years0to3 = daysRem < 0
        ? daysRem % 365 == 0 ? (daysRem / 365) : (daysRem / 365) -1
        : daysRem < 366 ? 0 : 1 + ((daysRem - 366) / 365);
    return 2000 + ((daysSince2k / DAYS_IN_4_YEARS) * 4) + years0to3;
  }

  /**
   * @return 0-based offset within the year: 0 for January 1st, 364 for
   *     December 31st in a non-leap year. Note this differs from
   *     {@link java.time.LocalDate#getDayOfYear()} which is 1-based.
   */
  @ZeroBased
  public int dayOfYearOffset() {
    int daysRem = daysSince2k % DAYS_IN_4_YEARS;
    return daysRem < 0
        ? daysRem % 365 == 0 ? 0 : 365 - abs(daysRem % 365)
        : daysRem < 366 ? daysRem : (daysRem - 366) % 365;
  }

  @ZeroBased
  public int monthOffset() {
    return monthOffset(year());
  }

  @ZeroBased
  public int monthOffset(int year) {
    int dayOfYear = dayOfYearOffset();
    int month = 0;
    int daysInMonth = daysInMonth(year, month);
    int daysRem = dayOfYear;
    while (daysRem >= daysInMonth) {
      daysRem -= daysInMonth;
      daysInMonth = daysInMonth(year, ++month);
    }
    return month;
  }

  @ZeroBased
  public int dayOfMonthOffset() {
    return dayOfMonthOffset(year());
  }

  @ZeroBased
  private int dayOfMonthOffset(int year) {
    int dayOfYear = dayOfYearOffset();
    int month = 0;
    int daysInMonth = daysInMonth(year, month);
    int daysRem = dayOfYear;
    while (daysRem >= daysInMonth) {
      daysRem -= daysInMonth;
      daysInMonth = daysInMonth(year, ++month);
    }
    return daysRem;
  }

  /**
   * The offset of this day from the previous (or same) occurrence of
   * {@code day1}: 0 for {@code day1} itself, up to 6.
   *
   * <p>Note this differs from {@link java.time.LocalDate#getDayOfWeek()} which
   * returns a {@link java.time.DayOfWeek} enum, and from
   * {@link java.util.Calendar#DAY_OF_WEEK} which is 1-based. This method is
   * intended for offset arithmetic; use {@code DayOfWeek} directly if a label
   * is what you need.
   *
   * @return 0..6
   */
  @ZeroBased
  public int dayOfWeekOffset(DayOfWeek day1) {
    int weekday2k = DayOfWeek.Saturday.ordinal(); // 2000-01-01 is a Saturday
    int weekday = (weekday2k + daysSince2k) % 7;
    return Math.floorMod(weekday - day1.ordinal(), 7);
  }

  private static final int[] NON_LEAP_MONTH_LENGTHS = {
    31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
  };

  static int daysInMonth(int year, @ZeroBased int month0) {
    int days = NON_LEAP_MONTH_LENGTHS[month0];
    if (month0 == 1 && isLeapYear(year)) days++;
    return days;
  }

  /*
  Intervals
   */

  /**
   * @return the interval of the week (starting Monday) containing this day
   */
  public Epoch2kInterval containingWeek() {
    return containingWeek(DayOfWeek.Monday);
  }

  public Epoch2kInterval containingWeek(DayOfWeek day1) {
    Epoch2kDay start = previousOrSame(day1);
    return new Epoch2kInterval(start, start.plusDays(6));
  }

  /**
   * @return the interval of the month containing this day
   */
  public Epoch2kInterval containingMonth() {
    OffsetYMD offset = offset();
    Epoch2kDay firstDay = new Epoch2kDay(daysSince2k - offset.days());
    return new Epoch2kInterval(firstDay, firstDay.plusDays(offset.daysInMonth() - 1));
  }

  /**
   * @return the interval of the year containing this day
   */
  public Epoch2kInterval containingYear() {
    return containingYear(MonthOfYear.January);
  }

  /** start of the year-cycle (starting in {@code month1}) that contains this day */
  public Epoch2kDay yearCycleStart(MonthOfYear month1) {
    Epoch2kDay day1 = day1InMonth(month1);
    return day1.after(this) ? day1.minusMonths(12) : day1;
  }

  public Epoch2kInterval containingYear(MonthOfYear month1) {
    Epoch2kDay yearDay1 = yearCycleStart(month1);
    return new Epoch2kInterval(yearDay1, yearDay1.plusMonths(12).minusDays(1));
  }

  public Epoch2kInterval containingQuarter(MonthOfYear month1) {
    return containingNMonthPeriod(month1, 3);
  }

  public Epoch2kInterval containingSixMonth(MonthOfYear month1) {
    return containingNMonthPeriod(month1, 6);
  }

  public Epoch2kInterval containingBiMonth(MonthOfYear month1) {
    return containingNMonthPeriod(month1, 2);
  }

  /**
   * The interval of the cycle starting in {@code month1} that contains this day,
   * where each interval is {@code periodLengthInMonths} calendar months long.
   * Requires {@code periodLengthInMonths} to divide 12 evenly.
   */
  private Epoch2kInterval containingNMonthPeriod(MonthOfYear month1, int periodLengthInMonths) {
    if (periodLengthInMonths <= 0 || 12 % periodLengthInMonths != 0)
      throw new IllegalArgumentException(
          "periodLengthInMonths must be a positive divisor of 12, got: " + periodLengthInMonths);
    Epoch2kDay yearDay1 = yearCycleStart(month1);
    int monthsSinceYearStart = Math.floorMod(monthOffset() - month1.ordinal(), 12);
    Epoch2kDay periodDay1 = yearDay1.plusMonths(
        (monthsSinceYearStart / periodLengthInMonths) * periodLengthInMonths);
    return new Epoch2kInterval(
        periodDay1, periodDay1.plusMonths(periodLengthInMonths).minusDays(1));
  }



  /**
   * @param n number of months; negative moves backwards, day-of-month clamped to target month length
   */
  public Epoch2kDay plusMonths(int n) {
    int y = year();
    int m = monthOffset();
    int d = dayOfMonthOffset();
    int totalMonths = y * 12 + m + n;
    int targetYear = Math.floorDiv(totalMonths, 12);
    int targetMonth = Math.floorMod(totalMonths, 12);
    int targetDay = Math.min(d, daysInMonth(targetYear, targetMonth) - 1);
    return fromYMD(targetYear, targetMonth, targetDay);
  }

  /**
   * Inverse of {@link #offset()}. {@code year} is the absolute Gregorian year;
   * {@code month} is 0-based (0 = January); {@code dayOfMonth} is 0-based (0 = 1st).
   */
  private static Epoch2kDay fromYMD(int year, @ZeroBased int month, @ZeroBased int dayOfMonth) {
    int days = 0;
    if (year >= 2000) {
      for (int y = 2000; y < year; y++) days += isLeapYear(y) ? 366 : 365;
    } else {
      for (int y = year; y < 2000; y++) days -= isLeapYear(y) ? 366 : 365;
    }
    for (int m = 0; m < month; m++) days += daysInMonth(year, m);
    return new Epoch2kDay(days + dayOfMonth);
  }

  private static boolean isLeapYear(int year) {
    return year % 4 == 0; // valid for 1901-2099
  }

  public Epoch2kDay minusMonths(int n) { return plusMonths(-n); }
  public Epoch2kDay plusYears(int n)   { return plusMonths(12 * n); }
  public Epoch2kDay minusYears(int n)  { return plusMonths(-12 * n); }

  /** @return the first {@code day} on or after this day */
  public Epoch2kDay nextOrSame(DayOfWeek day) {
    return plusDays((7 - dayOfWeekOffset(day)) % 7);
  }

  /** @return the last {@code day} on or before this day */
  public Epoch2kDay previousOrSame(DayOfWeek day) {
    return minusDays(dayOfWeekOffset(day));
  }

}
