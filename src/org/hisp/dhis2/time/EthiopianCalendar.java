package org.hisp.dhis2.time;

/**
 * Ethiopian (Ge'ez) calendar arithmetic on top of the day-count representation
 * used by {@link Epoch2kDay}.
 *
 * <p>The Ethiopian calendar has 12 months of 30 days plus a 13th intercalary
 * month (Pagume) of 5 days (6 in a leap year). Leap years occur every 4 years
 * without exception, when {@code year % 4 == 3}.
 *
 * <p>Anchor: 2000-01-01 Gregorian = 22 Tahsas 1992 Ethiopian.
 */
public final class EthiopianCalendar {

  private EthiopianCalendar() {}

  /** Offset of 1 Meskerem 1992 = 111 days before 2000-01-01. */
  private static final int ANCHOR_YEAR = 1992;
  private static final int ANCHOR_OFFSET = -111;
  private static final int CONSTANT =
      365 * ANCHOR_YEAR + Math.floorDiv(ANCHOR_YEAR, 4) - ANCHOR_OFFSET;

  /** @return the day offset (days since 2000-01-01) of 1 Meskerem of the given Ethiopian year */
  public static int newYearOffset(int year) {
    return 365 * year + Math.floorDiv(year, 4) - CONSTANT;
  }

  /** Ethiopian leap year: {@code year % 4 == 3} (every 4 years without exception). */
  public static boolean isLeapYear(int year) {
    return year % 4 == 3;
  }

  /** @param month 1..13 */
  public static int daysInMonth(int year, int month) {
    if (month < 13) return 30;
    return isLeapYear(year) ? 6 : 5;
  }

  public static int daysInYear(int year) {
    return isLeapYear(year) ? 366 : 365;
  }

  /** @param month 1..13, {@code dayOfMonth} 1..30 (or 1..5/6 for month 13) */
  public static Epoch2kDay day(int year, int month, int dayOfMonth) {
    return new Epoch2kDay(
        newYearOffset(year) + (month - 1) * 30 + (dayOfMonth - 1));
  }

  /** @return the Ethiopian year containing the given day */
  public static int year(Epoch2kDay day) {
    int d = day.daysSince2k();
    int estimate = Math.floorDiv((d + CONSTANT) * 4, 1461);
    while (newYearOffset(estimate + 1) <= d) estimate++;
    while (newYearOffset(estimate) > d) estimate--;
    return estimate;
  }

  /** @return 1..13 */
  public static int month(Epoch2kDay day) {
    return dayOfYear(day) / 30 + 1;
  }

  /** @return 1..30, or 1..5/6 for month 13 */
  public static int dayOfMonth(Epoch2kDay day) {
    return dayOfYear(day) % 30 + 1;
  }

  /** @return 0-based day within the Ethiopian year */
  public static int dayOfYear(Epoch2kDay day) {
    return day.daysSince2k() - newYearOffset(year(day));
  }

  public static Epoch2kInterval monthInterval(Epoch2kDay day) {
    int y = year(day);
    int m = month(day);
    Epoch2kDay first = day(y, m, 1);
    return new Epoch2kInterval(first, first.plusDays(daysInMonth(y, m) - 1));
  }

  public static Epoch2kInterval yearInterval(Epoch2kDay day) {
    int y = year(day);
    Epoch2kDay first = new Epoch2kDay(newYearOffset(y));
    return new Epoch2kInterval(first, first.plusDays(daysInYear(y) - 1));
  }
}