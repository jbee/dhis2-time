package org.hisp.dhis2.time;

/**
 * A closed interval in the ISO calendar.
 *
 * @param first day 1 of the interval (inclusive)
 * @param last last day of the interval (inclusive)
 */
public record Epoch2kInterval(Epoch2kDay first, Epoch2kDay last) {

  /**
   * The calendar year {@code [year-01-01, year-12-31]} — the whole of the given
   * Gregorian year, regardless of any sequence's fiscal start.
   */
  public static Epoch2kInterval ofYear(int year) {
    return new Epoch2kInterval(
        Epoch2kDay.ofJanuary1stInYear(year),
        Epoch2kDay.ofJanuary1stInYear(year + 1).minusDays(1));
  }

  /**
   * The calendar-year span {@code [year1-01-01, year2-12-31]} — from the start of
   * {@code year1} to the end of {@code year2}, inclusive.
   *
   * @throws IllegalArgumentException if {@code year2 < year1}
   */
  public static Epoch2kInterval ofYearRange(int year1, int year2) {
    if (year2 < year1)
      throw new IllegalArgumentException(
          "year2 (" + year2 + ") must not be before year1 (" + year1 + ")");
    return new Epoch2kInterval(
        Epoch2kDay.ofJanuary1stInYear(year1),
        Epoch2kDay.ofJanuary1stInYear(year2 + 1).minusDays(1));
  }

  Epoch2kInterval(Epoch2kDay day) {
    this(day, day);
  }

  public Epoch2kInterval {
    if (last.before(first))
      throw new IllegalArgumentException(
          "Negative length interval between %s and %s".formatted(first, last));
  }

  public int lengthInDays() {
    return last.daysSince2k() - first.daysSince2k() + 1;
  }

  @Override
  public String toString() {
    return first.toString() + "+" + lengthInDays();
  }

  public boolean contains(Epoch2kDay day) {
    return first.daysSince2k() <= day.daysSince2k() && last.daysSince2k() >= day.daysSince2k();
  }

  public boolean overlaps(Epoch2kInterval other) {
    return first.daysSince2k() <= other.last.daysSince2k()
        && other.first.daysSince2k() <= last.daysSince2k();
  }

  /** @return the number of days shared by this interval and {@code other}; 0 if disjoint. */
  public int overlapLength(Epoch2kInterval other) {
    int first = Math.max(this.first.daysSince2k(), other.first.daysSince2k());
    int last  = Math.min(this.last.daysSince2k(),  other.last.daysSince2k());
    return Math.max(0, last - first + 1);
  }
}
