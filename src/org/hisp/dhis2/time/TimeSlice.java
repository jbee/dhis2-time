package org.hisp.dhis2.time;

/**
 * A concrete ID or name for an interval in a specific {@link TimeSequence} in its object data model.
 * <p>
 * {@link #toString()} produces the canonical {@code <tag>/<first>+<length>} form.
 *
 * @see TimeSliceParser
 * @see BridgeParser
 */
public record TimeSlice(TimeSequenceTag tag, Epoch2kInterval interval) {
  /**
   * @return the canonical form
   */
  @Override
  public String toString() {
    return formatCanonical();
  }

  private String formatCanonical() {
    return tag + interval.toString();
  }

  public String formatYYYY_MM_DD() {
    Epoch2kDay d = interval.firstDay();
    return tag + "%04d-%02d-%02d".formatted(
        d.year(), d.monthOffset() + 1, d.dayOfMonthOffset() + 1);
  }

  public String formatYYYY_MM() {
    Epoch2kDay d = interval.firstDay();
    return tag + "%04d-%02d".formatted(d.year(), d.monthOffset() + 1);
  }

  public String formatYYYY() {
    Epoch2kDay d = interval.firstDay();
    return tag + "%04d".formatted(d.year());
  }

  public String formatYYYYxN(int position) {
    Epoch2kDay d = interval.firstDay();
    return tag + "%04d:%d".formatted(d.year(), position);
  }
}