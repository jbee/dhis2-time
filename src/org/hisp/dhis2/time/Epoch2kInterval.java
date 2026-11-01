package org.hisp.dhis2.time;

/**
 * A closed interval in the ISO calendar.
 *
 * @param firstDay day 1 of the interval (inclusive)
 * @param lastDay last day of the interval (inclusive)
 */
public record Epoch2kInterval(Epoch2kDay firstDay, Epoch2kDay lastDay) {

  /**
   * The calendar year {@code [year-01-01, year-12-31]} — the whole of the given
   * Gregorian year, regardless of any sequence's fiscal start.
   */
  public static Epoch2kInterval ofYear(int year) {
    return ofYearRange(year, year);
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
    if (lastDay.before(firstDay))
      throw new IllegalArgumentException(
          "Negative length interval between %s and %s".formatted(firstDay, lastDay));
  }

  public int lengthInDays() {
    return lastDay.daysSince2k() - firstDay.daysSince2k() + 1;
  }

  @Override
  public String toString() {
    // yields the canonical format
    return firstDay.toString() + "+" + lengthInDays();
  }

  public boolean contains(Epoch2kDay day) {
    return firstDay.daysSince2k() <= day.daysSince2k() && lastDay.daysSince2k() >= day.daysSince2k();
  }

  public boolean overlaps(Epoch2kInterval other) {
    return firstDay.daysSince2k() <= other.lastDay.daysSince2k()
        && other.firstDay.daysSince2k() <= lastDay.daysSince2k();
  }

  /** @return the number of days shared by this interval and {@code other}; 0 if disjoint. */
  public int overlapInDays(Epoch2kInterval other) {
    int first = Math.max(this.firstDay.daysSince2k(), other.firstDay.daysSince2k());
    int last  = Math.min(this.lastDay.daysSince2k(),  other.lastDay.daysSince2k());
    return Math.max(0, last - first + 1);
  }

  /**
   * Whether this interval contributes to {@code parent} under the attribution rule:
   * a majority of its days lie inside {@code parent}, with an exact 50/50 split
   * going to the earlier parent — the one containing this interval's
   * {@link #firstDay()}. This makes attribution total: any interval is attributed
   * to exactly one parent across a family, with no day silently dropped.
   *
   * <p>Used by {@link TimeSequence#contributorsTo(Epoch2kInterval)} and by
   * {@link CustomSequences.CustomSequence#contributorsTo(Epoch2kInterval)}.
   *
   * @param parent the candidate parent interval
   * @return {@code true} if this interval is attributed to {@code parent}
   */
  public boolean contributesTo(Epoch2kInterval parent) {
    int overlap = overlapInDays(parent);
    int childLength = lengthInDays();
    if (overlap * 2 != childLength) return overlap * 2 > childLength;
    // Exact 50/50: attribute to the earlier parent — the one containing firstDay().
    return parent.contains(firstDay());
  }
}
