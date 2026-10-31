package org.hisp.dhis2.time;

/**
 * The result of parsing a period string: the sequence key and the interval that
 * the key's sequence should contain. {@link #toString()} produces the canonical
 * {@code <key>/<first>+<length>} form.
 */
public record TimeSequenceInput(TimeSequenceKey key, Epoch2kInterval interval) {
  @Override
  public String toString() {
    return key + interval.toString();
  }
}