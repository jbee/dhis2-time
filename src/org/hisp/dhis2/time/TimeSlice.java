package org.hisp.dhis2.time;

/**
 * The result of parsing a period string: the sequence key and the interval that
 * the key's sequence should contain. {@link #toString()} produces the canonical
 * {@code <key>/<first>+<length>} form.
 */
public record TimeSlice(TimeSequenceKey key, Epoch2kInterval interval) {
  /**
   * @return the canonical form
   */
  @Override
  public String toString() {
    return key + interval.toString();
  }

  public String formatYYYY_MM_DD() {
    Epoch2kDay d = interval.firstDay();
    return key+ "%04d-%02d-%02d".formatted(
        d.year(), d.monthOffset() + 1, d.dayOfMonthOffset() + 1);
  }

  public String formatYYYY_MM() {
    Epoch2kDay d = interval.firstDay();
    return key + "%04d-%02d".formatted(d.year(), d.monthOffset() + 1);
  }

  public String formatYYYY() {
    Epoch2kDay d = interval.firstDay();
    return key + "%04d".formatted(d.year());
  }

  public String formatYYYYxN(int position) {
    Epoch2kDay d = interval.firstDay();
    return key + "%04d:%d".formatted(d.year(), position);
  }
}