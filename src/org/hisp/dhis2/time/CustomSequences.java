package org.hisp.dhis2.time;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Table-driven sequences whose intervals come from an external source (typically
 * entered by a user and stored in a database). Unlike the calendar sequences,
 * custom sequences do not follow an algorithm and may leave gaps — a given day
 * is either covered by one of the table intervals or by none.
 *
 * <p>Consequences of gaps:
 * <ul>
 *   <li>{@link TimeSequence#interval(Epoch2kDay)} throws for days in a gap.</li>
 *   <li>{@code next} / {@code previous} are table lookups (the next / previous
 *       interval in the table, regardless of gaps) rather than
 *       {@code interval(day ± 1)}.</li>
 *   <li>{@code contributesTo} is overridden to iterate the table directly, so it works
 *       for parents that contain gaps.</li>
 *   <li>{@code partition} is left as the default: it succeeds only for parents
 *       that are exactly one of the table intervals, or a run of adjacent ones.</li>
 * </ul>
 */
public final class CustomSequences {

  private CustomSequences() {}

  /** Custom sequence with no declared aggregation targets (terminal in the graph). */
  public static TimeSequence of(TimeSequenceKey key, List<Epoch2kInterval> intervals) {
    return of(key, intervals, Set.of(), Set.of());
  }

  public static TimeSequence of(
      TimeSequenceKey key,
      List<Epoch2kInterval> intervals,
      Set<TimeSequenceKey> partitions,
      Set<TimeSequenceKey> contributesTo) {
    return new CustomSequence(key, intervals, partitions, contributesTo);
  }

  /**
   * A custom sequence defined by an ordered, non-overlapping list of intervals.
   * The list is sorted defensively on construction (matching the usual "loaded
   * from a DB, might not be ordered" case) and validated for non-overlap.
   */
  public record CustomSequence(
      TimeSequenceKey key,
      List<Epoch2kInterval> intervals,
      Set<TimeSequenceKey> partitions,
      Set<TimeSequenceKey> contributesTo) implements TimeSequence {

    public CustomSequence {
      if (intervals.isEmpty())
        throw new IllegalArgumentException(
            "custom sequence " + key + " requires at least one interval");
      intervals = intervals.stream()
          .sorted(Comparator.comparing(Epoch2kInterval::firstDay))
          .toList();
      for (int i = 0; i < intervals.size() - 1; i++) {
        Epoch2kInterval curr = intervals.get(i);
        Epoch2kInterval next = intervals.get(i + 1);
        if (curr.lastDay().notBefore(next.firstDay()))
          throw new IllegalArgumentException(
              "custom sequence " + key + ": interval " + curr
                  + " overlaps or is not before " + next);
      }
      TimeSequence.checkKeys(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      int lo = 0, hi = intervals.size() - 1;
      while (lo <= hi) {
        int mid = (lo + hi) >>> 1;
        Epoch2kInterval candidate = intervals.get(mid);
        if (day.before(candidate.firstDay())) hi = mid - 1;
        else if (day.after(candidate.lastDay())) lo = mid + 1;
        else return candidate;
      }
      throw new IllegalArgumentException(
          "day " + day + " is not covered by any interval in custom sequence " + key);
    }

    @Override
    public Epoch2kInterval intervalOrNext(Epoch2kDay day) {
      int lo = 0, hi = intervals.size() - 1, result = -1;
      while (lo <= hi) {
        int mid = (lo + hi) >>> 1;
        if (intervals.get(mid).lastDay().notBefore(day)) { result = mid; hi = mid - 1; }
        else { lo = mid + 1; }
      }
      if (result < 0)
        throw new IllegalArgumentException(
            "no interval in " + key + " ends at or after " + day);
      return intervals.get(result);
    }

    /**
     * The next interval in the table after {@code ref}, regardless of gaps.
     *
     * @throws NoSuchElementException if {@code ref} is at or after the last interval
     */
    @Override
    public Epoch2kInterval next(Epoch2kInterval ref) {
      Epoch2kDay after = ref.lastDay().tomorrow();
      int lo = 0, hi = intervals.size() - 1, result = -1;
      while (lo <= hi) {
        int mid = (lo + hi) >>> 1;
        if (intervals.get(mid).firstDay().notBefore(after)) {
          result = mid;
          hi = mid - 1;
        } else {
          lo = mid + 1;
        }
      }
      if (result < 0)
        throw new NoSuchElementException(
            "no interval in " + key + " starts after " + ref);
      return intervals.get(result);
    }

    /**
     * The previous interval in the table before {@code ref}, regardless of gaps.
     *
     * @throws NoSuchElementException if {@code ref} is at or before the first interval
     */
    @Override
    public Epoch2kInterval previous(Epoch2kInterval ref) {
      Epoch2kDay before = ref.firstDay().yesterday();
      int lo = 0, hi = intervals.size() - 1, result = -1;
      while (lo <= hi) {
        int mid = (lo + hi) >>> 1;
        if (intervals.get(mid).lastDay().notAfter(before)) {
          result = mid;
          lo = mid + 1;
        } else {
          hi = mid - 1;
        }
      }
      if (result < 0)
        throw new NoSuchElementException(
            "no interval in " + key + " ends before " + ref);
      return intervals.get(result);
    }

    /**
     * The table intervals whose majority of days fall inside {@code parent}.
     * Iterates the table directly, so this works for parents that span gaps —
     * which the default day-by-day walk does not.
     */
    @Override
    public List<Epoch2kInterval> contributorsTo(Epoch2kInterval parent) {
      return intervals.stream().filter(i -> i.contributesTo(parent)).toList();
    }
  }
}