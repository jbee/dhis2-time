package org.hisp.dhis2.time;

import java.util.function.LongSupplier;

interface TimeSequence {

  record Code(char system, char interval, char offset) {
    public Code(char system, char interval) {
      this(system, interval, '-');
    }
  }

  Code code();

  /**
   * @return when positive: the exact length in days (regular sequence), when negative: the average
   *     length in days (irregular sequence)
   */
  int lengthInDays();

  /**
   * @param nowUTC supplier for current time in millis since 1970 UTC as given by {@link
   *     System#currentTimeMillis()}
   * @return the interval of this sequence containing the current day (now/today)
   */
  Epoch2kInterval today(LongSupplier nowUTC);

  /**
   * @param ref a reference interval, usually of the same sequence
   * @return the interval of this sequence starting or containing the day after the last day of the
   *     given interval
   */
  Epoch2kInterval next(Epoch2kInterval ref);

  /**
   * @param ref a reference interval, usually of the same sequence
   * @return the interval of this sequence ending or containing the day before the first day of the
   *     given interval
   */
  Epoch2kInterval previous(Epoch2kInterval ref);
}
