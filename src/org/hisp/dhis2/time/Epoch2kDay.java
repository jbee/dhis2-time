package org.hisp.dhis2.time;

import java.util.function.LongSupplier;

import static java.lang.Math.abs;

@TimeOffset
public record Epoch2kDay(@TimeOffset int daysSince2k) {

  private static final long MILLIS_UTC_T0_UNTIL_2K_MIDNIGHT = 946_684_800_000L;
  private static final long MILLIS_IN_A_DAY = 86_400_000L;

  public static Epoch2kDay today() {
    return today(System::currentTimeMillis);
  }

  public static Epoch2kDay today(@TimeOffset LongSupplier nowUTC) {
    long millisSince2k = nowUTC.getAsLong() - MILLIS_UTC_T0_UNTIL_2K_MIDNIGHT;
    int daysSince2k = (int) (millisSince2k / MILLIS_IN_A_DAY);
    return new Epoch2kDay(daysSince2k);
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

  public long toUTCMillis() {
    return MILLIS_UTC_T0_UNTIL_2K_MIDNIGHT + daysSince2k * MILLIS_IN_A_DAY;
  }

  public Epoch2kDay plusDays(int n) {
    return new Epoch2kDay(daysSince2k + n);
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

  /**
   * @param n number of months to add starting with the month of this day
   * @return the day n month later (which can effectively be n+1 month later of this day of the
   *     month exceeds the days in the month n month from this day's month)
   */
  public Epoch2kDay plusMonths(int n) {
    int year = year();
    int startMonth = month();
    int days = daysSince2k;
    for (int i = 0; i < n; i++) {
      int monthOffset = startMonth + i;
      int month = monthOffset % 12;
      int yearOffset = monthOffset / 12;
      days += daysInMonth(year + yearOffset, month);
    }
    return new Epoch2kDay(days);
  }

  public Epoch2kDay day1InYear() {
    return minusDays(dayOfYear());
  }

  /**
   * @param month the month to find the 1st day of
   * @return the 1st day in the given month in the same year as this day's year
   */
  public Epoch2kDay day1InMonth(Month month) {
    int year = year();
    int daysToDay1 = 0;
    for (int m = 0; m < month.ordinal(); m++) daysToDay1 += daysInMonth(year, m);
    return new Epoch2kDay(daysSince2k - dayOfYear() + daysToDay1);
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
  public Epoch2kInterval containingWeek() {
    return containingWeek(Weekday.Monday);
  }

  public Epoch2kInterval containingWeek(Weekday day1) {
    Epoch2kDay start = minusDays(dayOfWeek(day1));
    //TODO two cases as well?
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
    return containingYear(Month.January);
  }

  public Epoch2kInterval containingYear(Month month1) {
    Epoch2kDay yearDay1 = day1InMonth(month1);
    //TODO this day can be before or after year day1 - 2 cases
    Epoch2kDay yearLastDay = yearDay1.plusMonths(12).minusDays(1);
    return new Epoch2kInterval(yearDay1, yearLastDay);
  }

  public Epoch2kInterval containingQuarter(Month month1) {
    Epoch2kDay yearDay1 = day1InMonth(month1);
    if (yearDay1.after(this)) {

    } else {

    }
    Epoch2kDay qX = yearDay1.plusMonths(3).minusDays(1);

    return null;
  }
}
