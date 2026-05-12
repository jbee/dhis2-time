package org.hisp.dhis2.time;

import java.util.function.LongSupplier;

import static java.lang.Math.abs;

@TimeOffset
public record IsoDay(@TimeOffset int daysSince2k) {

  private static final long MILLIS_SINCE_2K_MIDNIGHT = 946_684_800_000L;
  private static final long MILLIS_IN_A_DAY = 86_400_000L;

  public static IsoDay today() {
    return today(System::currentTimeMillis);
  }

  public static IsoDay today(@TimeOffset LongSupplier nowUTC) {
    long millisSince2k = nowUTC.getAsLong() - MILLIS_SINCE_2K_MIDNIGHT;
    int daysSince2k = (int) (millisSince2k / MILLIS_IN_A_DAY);
    return new IsoDay(daysSince2k);
  }

  @TimeOffset
  public enum Weekday {
    // In ISO order with Monday being the ISO start of the week
    Monday,
    Tuesday,
    Wednesday,
    Thursday,
    Friday,
    Saturday,
    Sunday
  }

  @TimeOffset
  public enum Month {
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
   * @param years relative to fixpoint
   * @param months relative to start of the year, 0-11, 0=January
   * @param days relative to start of the month, 0-30, 0=1st of month
   */
  public record OffsetYMD(
      @TimeOffset int years, @TimeOffset int months, @TimeOffset int days) {

    int daysInMonth() {
      return IsoDay.daysInMonth(years, months);
    }

    @Override
    public String toString() {
      return "%04d-%02d-%02d".formatted(years, months+1, days+1);
    }
  }

  public boolean before(IsoDay other) {
    return daysSince2k < other.daysSince2k;
  }

  public boolean notBefore(IsoDay other) {
    return daysSince2k >= other.daysSince2k;
  }

  public boolean after(IsoDay other) {
    return daysSince2k > other.daysSince2k;
  }

  public boolean notAfter(IsoDay other) {
    return daysSince2k <= other.daysSince2k;
  }

  @Override
  public String toString() {
    return offset().toString();
  }

  public IsoDay plusDays(int n) {
    return new IsoDay(daysSince2k + n);
  }

  public IsoDay minusDays(int n) {
    return plusDays(-n);
  }

  public IsoDay yesterday() {
    return minusDays(1);
  }

  public IsoDay tomorrow() {
    return plusDays(1);
  }

  /**
   * @param n number of months to add starting with the month of this day
   * @return the day n month later (which can effectively be n+1 month later of this day of the
   *     month exceeds the days in the month n month from this day's month)
   */
  public IsoDay plusMonths(int n) {
    int year = year();
    int startMonth = month();
    int days = daysSince2k;
    for (int i = 0; i < n; i++) {
      int monthOffset = startMonth + i;
      int month = monthOffset % 12;
      int yearOffset = monthOffset / 12;
      days += daysInMonth(year + yearOffset, month);
    }
    return new IsoDay(days);
  }

  public IsoDay day1InYear() {
    return minusDays(dayOfYear());
  }

  /**
   * @param month the month to find the 1st day of
   * @return the 1st day in the given month in the same year as this day's year
   */
  public IsoDay day1InMonth(Month month) {
    int year = year();
    int daysToDay1 = 0;
    for (int m = 0; m < month.ordinal(); m++) daysToDay1 += daysInMonth(year, m);
    return new IsoDay(daysSince2k - dayOfYear() + daysToDay1);
  }

  /*
   * ISO based calculations assuming dates between 1901-2099
   */

  public OffsetYMD offset() {
    int year = year();
    return new OffsetYMD(year, month(year), dayOfMonth(year));
  }

  private static final int DAYS_IN_4_YEARS = 366 + 3 * 365;

  @TimeOffset
  public int year() {
    int daysRem = daysSince2k % DAYS_IN_4_YEARS;
    int years0to3 = daysRem < 0
        ? daysRem % 365 == 0 ? (daysRem / 365) : (daysRem / 365) -1
        : daysRem < 366 ? 0 : 1 + ((daysRem - 366) / 365);
    return 2000 + ((daysSince2k / DAYS_IN_4_YEARS) * 4) + years0to3;
  }

  @TimeOffset
  public int dayOfYear() {
    int daysRem = daysSince2k % DAYS_IN_4_YEARS;
    return daysRem < 0
        ? daysRem % 365 == 0 ? 0 : 365 - abs(daysRem % 365)
        : daysRem < 366 ? daysRem : (daysRem - 366) % 365;
  }

  @TimeOffset
  public int month() {
    return month(year());
  }

  @TimeOffset
  public int month(@TimeOffset int year) {
    int dayOfYear = dayOfYear();
    int month = 0;
    int daysInMonth = daysInMonth(year, month);
    int daysRem = dayOfYear;
    while (daysRem >= daysInMonth) {
      daysRem -= daysInMonth;
      daysInMonth = daysInMonth(year, ++month);
    }
    return month;
  }

  @TimeOffset
  public int dayOfMonth() {
    return dayOfMonth(year());
  }

  @TimeOffset
  private int dayOfMonth(@TimeOffset int year) {
    int dayOfYear = dayOfYear();
    int month = 0;
    int daysInMonth = daysInMonth(year, month);
    int daysRem = dayOfYear;
    while (daysRem >= daysInMonth) {
      daysRem -= daysInMonth;
      daysInMonth = daysInMonth(year, ++month);
    }
    return daysRem;
  }

  public int dayOfWeek(Weekday day1) {
    int weekday2k = Weekday.Saturday.ordinal(); // 2000-01-01 is a Saturday
    int weekday = (weekday2k + daysSince2k) % 7;
    return (weekday - day1.ordinal()) % 7;
  }

  private static final int[] NON_LEAP_MONTH_LENGTHS = {
    31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
  };

  private static int daysInMonth(@TimeOffset int year, @TimeOffset int month) {
    int daysInMonth = NON_LEAP_MONTH_LENGTHS[month];
    if (month != 1) return daysInMonth;
    boolean leap = (year % 4 == 0); // works for 2000-2099
    return leap ? daysInMonth + 1 : daysInMonth;
  }

  /*
  Intervals
   */

  /**
   * @return the interval of the week (starting Monday) containing this day
   */
  public IsoInterval inWeek() {
    return inWeek(Weekday.Monday);
  }

  public IsoInterval inWeek(Weekday day1) {
    IsoDay start = minusDays(dayOfWeek(day1));
    //TODO two cases as well?
    return new IsoInterval(start, start.plusDays(6));
  }

  /**
   * @return the interval of the month containing this day
   */
  public IsoInterval inMonth() {
    OffsetYMD offset = offset();
    IsoDay firstDay = new IsoDay(daysSince2k - offset.days());
    return new IsoInterval(firstDay, firstDay.plusDays(offset.daysInMonth() - 1));
  }

  /**
   * @return the interval of the year containing this day
   */
  public IsoInterval inYear() {
    return inYear(Month.January);
  }

  public IsoInterval inYear(Month month1) {
    IsoDay yearDay1 = day1InMonth(month1);
    //TODO this day can be before or after year day1 - 2 cases
    IsoDay yearLastDay = yearDay1.plusMonths(12).minusDays(1);
    return new IsoInterval(yearDay1, yearLastDay);
  }

  public IsoInterval inQuarter(Month month1) {
    IsoDay yearDay1 = day1InMonth(month1);
    if (yearDay1.after(this)) {

    } else {

    }
    IsoDay qX = yearDay1.plusMonths(3).minusDays(1);

    return null;
  }
}
