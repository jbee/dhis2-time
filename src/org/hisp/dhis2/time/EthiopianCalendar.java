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
  public static int newYearOffsetInEC(int year) {
    return 365 * year + Math.floorDiv(year, 4) - CONSTANT;
  }

  /** Ethiopian leap year: {@code year % 4 == 3} (every 4 years without exception). */
  public static boolean isLeapYearInEC(int year) {
    return year % 4 == 3;
  }

  /** @param month 1..13 */
  public static int daysInMonthInEC(int year, int month) {
    if (month < 13) return 30;
    return isLeapYearInEC(year) ? 6 : 5;
  }

  public static int daysInYearInEC(int year) {
    return isLeapYearInEC(year) ? 366 : 365;
  }

  /** @param month 1..13, {@code dayOfMonth} 1..30 (or 1..5/6 for month 13) */
  public static Epoch2kDay toEpochDayInEC(int year, int month, int dayOfMonth) {
    return new Epoch2kDay(
        newYearOffsetInEC(year) + (month - 1) * 30 + (dayOfMonth - 1));
  }

  /** @return the Ethiopian year containing the given day */
  public static int yearInEC(Epoch2kDay day) {
    int d = day.daysSince2k();
    int estimate = Math.floorDiv((d + CONSTANT) * 4, 1461);
    while (newYearOffsetInEC(estimate + 1) <= d) estimate++;
    while (newYearOffsetInEC(estimate) > d) estimate--;
    return estimate;
  }

  /** @return 1..13 */
  public static int monthInEC(Epoch2kDay day) {
    return dayOfYearInEC(day) / 30 + 1;
  }

  /** @return 1..30, or 1..5/6 for month 13 */
  public static int dayOfMonthInEC(Epoch2kDay day) {
    return dayOfYearInEC(day) % 30 + 1;
  }

  /** @return 0-based day within the Ethiopian year */
  public static int dayOfYearInEC(Epoch2kDay day) {
    return day.daysSince2k() - newYearOffsetInEC(yearInEC(day));
  }

  public static Epoch2kInterval toEpochIntervalOfEthiopianMonthContaining(Epoch2kDay day) {
    int y = yearInEC(day);
    int m = monthInEC(day);
    Epoch2kDay first = toEpochDayInEC(y, m, 1);
    return new Epoch2kInterval(first, first.plusDays(daysInMonthInEC(y, m) - 1));
  }

  public static Epoch2kInterval toEpochIntervalOfEthiopianYearContaining(Epoch2kDay day) {
    int y = yearInEC(day);
    Epoch2kDay first = new Epoch2kDay(newYearOffsetInEC(y));
    return new Epoch2kInterval(first, first.plusDays(daysInYearInEC(y) - 1));
  }
}