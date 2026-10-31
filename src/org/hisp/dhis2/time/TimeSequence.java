package org.hisp.dhis2.time;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;

import static java.util.Objects.requireNonNull;

public interface TimeSequence {

  TimeSequenceKey key();

  Epoch2kInterval interval(Epoch2kDay day);

  /**
   * @param nowUTC supplier for current time in millis since 1970 UTC as given by {@link
   *     System#currentTimeMillis()}
   * @return the interval of this sequence containing the current day (now/today)
   */
  default Epoch2kInterval today(LongSupplier nowUTC) {
    return interval(Epoch2kDay.today(nowUTC));
  }

  /**
   * @param ref a reference interval, usually of the same sequence
   * @return the interval of this sequence starting or containing the day after the last day of the
   *     given interval
   */
  default Epoch2kInterval next(Epoch2kInterval ref) {
    return interval(ref.last().tomorrow());
  }

  /**
   * @param ref a reference interval, usually of the same sequence
   * @return the interval of this sequence ending or containing the day before the first day of the
   *     given interval
   */
  default Epoch2kInterval previous(Epoch2kInterval ref) {
    return interval(ref.first().yesterday());
  }

  /**
   * Families that {@link #partition} can strictly tile. Subset of
   * {@link #contributesTo()}.
   */
  Set<TimeFamilyKey> partitions();
  default boolean partitions(TimeFamilyKey family) { return partitions().contains(family); }

  /**
   * The families this sequence can contribute to — a strict superset of
   * {@link #partitions()}. For each family here, calling
   * {@link #contributorsTo(Epoch2kInterval)} with a well-formed parent yields
   * the intervals whose majority of days fall inside that parent.
   *
   * <p>Strict containment governs {@link #partitions()}; majority-of-days
   * governs this set. The gap between them is where a calendar's boundaries
   * do not align (e.g. weeks to months).
   *
   * <p>Same-calendar families only — a Gregorian day lists Gregorian families,
   * never Islamic or Ethiopian.
   */
  Set<TimeFamilyKey> contributesTo();
  default boolean contributesTo(TimeFamilyKey family) { return contributesTo().contains(family); }

  default List<Epoch2kInterval> partition(Epoch2kInterval parent) {
    Epoch2kInterval child = interval(parent.first());
    if (!child.first().equals(parent.first()))
      throw new IllegalArgumentException(
          key() + " does not align with " + parent.first()
              + " (child starts at " + child.first() + ")");

    List<Epoch2kInterval> parts = new ArrayList<>();
    while (true) {
      parts.add(child);
      if (child.last().equals(parent.last())) return List.copyOf(parts);
      if (child.last().after(parent.last()))
        throw new IllegalArgumentException(
            key() + " overshoots " + parent.last() + " (child ends at " + child.last() + ")");
      child = interval(child.last().tomorrow());
    }
  }

  /**
   * The intervals of this sequence that contribute to {@code parent} — those
   * with a strict majority of their days inside it. {@code parent} is not
   * required to be an interval of a sequence listed in {@link #contributesTo()};
   * the method computes regardless, and the set documents which families the
   * result is meaningful for.
   *
   * <p>Unlike {@link #partition}, this tolerates parents the child sequence does
   * not strictly tile (weeks into months, bi-weeks into calendar years, custom
   * sequences into Gregorian years containing gaps).
   */
  default List<Epoch2kInterval> contributorsTo(Epoch2kInterval parent) {
    Epoch2kInterval child = interval(parent.first());
    List<Epoch2kInterval> result = new ArrayList<>();
    while (!child.first().after(parent.last())) {
      if (majorityInside(child, parent)) result.add(child);
      child = interval(child.last().tomorrow());
    }
    return List.copyOf(result);
  }

  static boolean majorityInside(Epoch2kInterval child, Epoch2kInterval parent) {
    return child.overlapLength(parent) * 2 > child.lengthInDays();
  }

  /** Invariant check used by every implementation's compact constructor. */
  static void checkFamilies(Set<TimeFamilyKey> partitions, Set<TimeFamilyKey> contributesTo) {
    requireNonNull(partitions, "partitions");
    requireNonNull(contributesTo, "contributesTo");
    if (!contributesTo.containsAll(partitions))
      throw new IllegalArgumentException(
          "partitions() must be a subset of contributesTo(); missing: "
              + partitions.stream().filter(p -> !contributesTo.contains(p)).toList());
  }
}
