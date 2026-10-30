package org.hisp.dhis2.time;

/**
 * A closed interval in the ISO calendar.
 *
 * @param first day 1 of the interval (inclusive)
 * @param last last day of the interval (inclusive)
 */
public record Epoch2kInterval(Epoch2kDay first, Epoch2kDay last) {

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
}
