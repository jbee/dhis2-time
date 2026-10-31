package org.hisp.dhis2.time;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;

import static java.util.Objects.requireNonNull;

/**
 * A named way of slicing the timeline into intervals.
 *
 * <h2>Mental model</h2>
 *
 * <p>A {@code TimeSequence} maps days to intervals. The primitive operation is
 * {@link #interval(Epoch2kDay)}: given a day, return the interval of this sequence
 * that contains it. Everything else in this interface is derived from that:
 *
 * <ul>
 *   <li><b>Navigation</b> — {@link #today(LongSupplier)}, {@link #next(Epoch2kInterval)},
 *       {@link #previous(Epoch2kInterval)} move one interval at a time.</li>
 *   <li><b>Strict decomposition</b> — {@link #partitions()} declares which families
 *       this sequence strictly tiles; {@link #partition(Epoch2kInterval)} performs
 *       the decomposition.</li>
 *   <li><b>Majority attribution</b> — {@link #contributesTo()} declares which
 *       families this sequence contributes to by majority-of-days;
 *       {@link #contributorsTo(Epoch2kInterval)} performs the selection.</li>
 * </ul>
 *
 * <h2>Calendar sequences vs. custom sequences</h2>
 *
 * <p>Calendar sequences (Gregorian, Ethiopian, Umm al-Qura, …) tile the whole
 * timeline: every day belongs to exactly one interval, so {@code interval(day)}
 * is total within the supported range. Custom sequences are backed by an explicit
 * table of intervals (e.g. school terms) and may leave gaps. For a day in a gap,
 * {@link #interval(Epoch2kDay)} throws; {@code next}/{@code previous} mean
 * "the next/previous interval in the table" (which may be months away); and
 * {@link #contributorsTo(Epoch2kInterval)} is overridden to iterate the table
 * rather than walk day-by-day.
 *
 * <h2>Strict vs. majority attribution</h2>
 *
 * <p>For most Gregorian families the two relations coincide: months nest into
 * quarters, quarters into years, days into everything. The distinction matters
 * exactly where a boundary does not align:
 *
 * <pre>{@code
 * Weekly.partitions()          →  {BiWeek}               // weeks tile into bi-weeks
 * Weekly.contributesTo()       →  {BiWeek, Month, Quarter, Year}
 *
 * Weekly.partition(month)      →  throws                 // a week can straddle a month
 * Weekly.contributorsTo(month) →  [weeks by majority]    // each week assigned once
 * }</pre>
 *
 * <p>For sequences whose intervals always nest cleanly into the target family
 * (Gregorian Monthly, Quarterly, Yearly) {@code partitions() == contributesTo()},
 * and {@code partition()} and {@code contributorsTo()} return the same result on
 * any well-formed parent of the right family.
 *
 * <h2>Identity and the parser</h2>
 *
 * <p>Each sequence has a {@link #key()} that identifies it as a value: two
 * sequences with equal keys are indistinguishable to the string parser and to
 * lookup registries. The key encodes (calendar system, interval type, optional
 * offset) and is what appears as the prefix of a period's string form
 * ({@code GQ/2022-01-01+90}). {@link TimeSequenceParser} resolves a parsed key
 * to a concrete sequence, then uses {@code interval} / {@code next} to materialise
 * the interval.
 *
 * <p>The key is <b>coarser than equality</b>: two {@code TimeSequence} instances
 * may share a key yet differ in {@code contributesTo()} or {@code partitions()}
 * (a configuration error that {@link TimeSequenceParser} rejects at construction
 * time).
 *
 * <h2>Contract</h2>
 *
 * <p>Implementations must satisfy the following invariants within their supported
 * range of days:
 *
 * <ul>
 *   <li><b>Determinism</b> — {@code interval(d)} returns the same interval on every
 *       call, for the same {@code d}. No clock, no randomness.</li>
 *   <li><b>Containment</b> (calendar sequences) — when {@code interval(d)} returns
 *       normally, {@code d} is inside the result.</li>
 *   <li><b>Non-overlap</b> — for any two days {@code d1}, {@code d2} where
 *       {@code interval} returns normally, the results are either equal or
 *       disjoint.</li>
 *   <li><b>Subset</b> — {@link #partitions()} must be a subset of
 *       {@link #contributesTo()}. Enforced by
 *       {@link #checkFamilies(Set, Set)}.</li>
 *   <li><b>Same calendar</b> — the {@link TimeFamilyKey}s in {@code partitions()} and
 *       {@code contributesTo()} must be from the same calendar system as this
 *       sequence. A Gregorian day aggregates into Gregorian months, never into
 *       Islamic or Ethiopian ones.</li>
 * </ul>
 *
 * <p>Implementations are <b>immutable</b> and safe to share across threads. The
 * interface is sealed by convention: extend the set of calendars via a dedicated
 * {@code *Sequences} factory class rather than creating one-off subtypes.
 *
 * <h2>Supported day range</h2>
 *
 * <p>Day arithmetic on {@link Epoch2kDay} is guaranteed for 1901-01-01 through
 * 2099-12-31 Gregorian. Calendar sequences may have narrower ranges (the tabulated
 * Umm al-Qura sequence covers AH 1440-1450, i.e. 2018-2029). {@code interval} throws
 * {@link IllegalArgumentException} for days outside an implementation's range.
 *
 * @see TimeSequenceKey
 * @see Epoch2kDay
 * @see Epoch2kInterval
 * @see TimeSequenceParser
 * @see GregorianSequences
 * @see EthiopianSequences
 * @see IslamicSequences
 * @see CustomSequences
 */
public interface TimeSequence {

  /**
   * The identity of this sequence: which calendar, which interval type, and any
   * offset (fiscal start month, week's first day, …).
   *
   * <p>The key is what appears as the prefix of a period's string form
   * ({@code GQ/2022-01-01+90} — the key is {@code GQ/}) and what
   * {@link TimeSequenceParser} uses to resolve string input to a concrete
   * sequence. Two sequences with equal keys cannot coexist in one parser's
   * registry.
   *
   * <p>The key is coarser than object equality: two instances may share a key yet
   * differ in {@link #contributesTo()}, which is treated as a configuration error
   * by the parser. {@code key()} identifies the sequence's <em>behaviour
   * family</em>, not a specific instance.
   */
  TimeSequenceKey key();

  /**
   * The interval of this sequence that contains {@code day}.
   *
   * <p>For calendar sequences, this is total within the supported day range:
   * any day maps to exactly one interval, and {@code interval(d).contains(d)}
   * holds.
   *
   * <p>For custom sequences, this may throw {@link IllegalArgumentException} when
   * {@code day} falls in a gap. Callers that want the "or nothing" variant should
   * use {@link CustomSequences.CustomSequence#intervalOrEmpty(Epoch2kDay)}.
   *
   * <p>This is the primitive from which every other operation derives:
   * {@link #next}, {@link #previous}, {@link #partition}, and
   * {@link #contributorsTo} are all defined in terms of it (custom sequences
   * override the navigation and contributor methods to accommodate gaps).
   *
   * @param day the day whose interval is requested
   * @return the interval of this sequence containing {@code day}
   * @throws IllegalArgumentException if {@code day} is not covered by this
   *     sequence (out of range, or in a gap)
   */
  Epoch2kInterval interval(Epoch2kDay day);

  /**
   * The first interval of this sequence whose last day is on or after {@code day}.
   *
   * <p>For sequences without gaps this is identical to {@link #interval(Epoch2kDay)}.
   * Sequences with gaps (custom sequences) override this to walk forward to the next
   * interval, so a day in a gap resolves to the following interval rather than throwing.
   *
   * @throws IllegalArgumentException if no interval of this sequence ends at or after {@code day}
   */
  default Epoch2kInterval intervalOrAfter(Epoch2kDay day) {
    return interval(day);
  }

  /**
   * The interval of this sequence containing today, in UTC.
   *
   * @param nowUTC current time in milliseconds since 1970-01-01T00:00Z, as given
   *     by {@link System#currentTimeMillis()}. The supplier is invoked once.
   *     Testing use cases pass a fixed supplier; production passes
   *     {@code System::currentTimeMillis}.
   * @return the interval containing today in UTC. Callers interested in a
   *     different time zone should construct their "today" day themselves and
   *     call {@link #interval(Epoch2kDay)}; this method is deliberately
   *     zone-agnostic and treats UTC as the canonical clock.
   */
  default Epoch2kInterval today(LongSupplier nowUTC) {
    return interval(Epoch2kDay.today(nowUTC));
  }

  /**
   * The interval of this sequence starting or containing the day after
   * {@code ref.last()}.
   *
   * <p>For calendar sequences this is {@code interval(ref.last().tomorrow())}:
   * the next interval in the walk regardless of what {@code ref} was, provided
   * {@code ref} lies within the sequence's range.
   *
   * <p>Custom sequences override this to mean "the next interval in the table",
   * which may be a gap away from {@code ref} and may be at a very different scale.
   * See {@link CustomSequences.CustomSequence#next(Epoch2kInterval)}.
   *
   * <p>{@code next} and {@link #previous} are inverses when the sequence has no
   * gaps and no irregular step sizes: {@code previous(next(ref)) == ref} for any
   * {@code ref} of this sequence. Custom sequences weaken this to "within their
   * own table"; at the endpoints they throw {@link java.util.NoSuchElementException}.
   *
   * @param ref a reference interval, usually of this sequence
   * @return the interval immediately following {@code ref}
   * @throws IllegalArgumentException if the day after {@code ref.last()} is out
   *     of this sequence's supported range
   */
  default Epoch2kInterval next(Epoch2kInterval ref) {
    return interval(ref.last().tomorrow());
  }

  /**
   * The interval of this sequence ending or containing the day before
   * {@code ref.first()}.
   *
   * <p>Symmetric to {@link #next(Epoch2kInterval)}: for calendar sequences this
   * is {@code interval(ref.first().yesterday())}; custom sequences override to
   * return the previous interval in their table.
   *
   * @param ref a reference interval, usually of this sequence
   * @return the interval immediately preceding {@code ref}
   * @throws IllegalArgumentException if the day before {@code ref.first()} is out
   *     of this sequence's supported range
   */
  default Epoch2kInterval previous(Epoch2kInterval ref) {
    return interval(ref.first().yesterday());
  }

  /**
   * The families this sequence strictly tiles: calling {@link #partition} with a
   * parent of one of these families will succeed for well-formed input.
   *
   * <p>Strict tiling means every interval of this sequence is fully contained in
   * exactly one interval of the parent family, so the parent is exactly the
   * concatenation of child intervals with no gaps and no overlaps. Equivalently:
   * no child interval straddles a parent boundary.
   *
   * <p>Always a subset of {@link #contributesTo()}. The two sets coincide for
   * calendar sequences whose boundaries nest cleanly; they differ for sequences
   * with boundary-straddling children (e.g. ISO weeks into Gregorian months).
   *
   * @see #partition(Epoch2kInterval)
   */
  Set<TimeFamilyKey> partitions();

  /**
   * Convenience test: does {@code family} belong to {@link #partitions()}?
   *
   * @see #partitions()
   */
  default boolean partitions(TimeFamilyKey family) { return partitions().contains(family); }

  /**
   * The families this sequence contributes to by majority-of-days: for each family
   * here, {@link #contributorsTo(Epoch2kInterval)} with a well-formed parent yields
   * a meaningful set of child intervals (those with a strict majority of their
   * days inside the parent).
   *
   * <p>Always a superset of {@link #partitions()}. Includes families whose
   * boundaries do not align with this sequence's (weeks to months, bi-weeks to
   * calendar years) as long as majority attribution is the intended behaviour.
   *
   * <p>Same-calendar families only. The aggregation framework uses this set to
   * reject, up front, requests that would cross calendar families (a Gregorian day
   * contributing to Islamic months, for example).
   *
   * @see #contributorsTo(Epoch2kInterval)
   */
  Set<TimeFamilyKey> contributesTo();

  /**
   * Convenience test: does {@code family} belong to {@link #contributesTo()}?
   *
   * @see #contributesTo()
   */
  default boolean contributesTo(TimeFamilyKey family) { return contributesTo().contains(family); }

  /**
   * Decomposes {@code parent} into the intervals of this sequence that exactly
   * tile it: the first child starts at {@code parent.first()}, each subsequent
   * child starts the day after the previous child's last day, and the final child
   * ends at {@code parent.last()}.
   *
   * <p>Succeeds only when {@code parent} aligns with this sequence's boundaries.
   * For calendar sequences, misalignment means a parent of the wrong family or
   * with boundaries that do not match this sequence's configuration. For custom
   * sequences, any parent containing a gap fails at the gap.
   *
   * <p>This is the strict counterpart to {@link #contributorsTo(Epoch2kInterval)}:
   * use {@code partition} when the parent must be exactly reconstructed from its
   * children (e.g. totals that must sum to the parent exactly), and
   * {@code contributorsTo} when boundary intervals should be attributed to
   * whichever parent they spend the majority of their days in.
   *
   * @param parent the interval to decompose
   * @return the ordered list of child intervals, from {@code parent.first()} to
   *     {@code parent.last()}
   * @throws IllegalArgumentException if {@code parent} is not aligned with this
   *     sequence's boundaries, or if this sequence leaves a gap inside
   *     {@code parent}
   */
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
   * The intervals of this sequence that contribute to {@code parent} — those with
   * a strict majority of their days inside it.
   *
   * <p>Unlike {@link #partition(Epoch2kInterval)}, this tolerates parents the
   * sequence does not strictly tile: a boundary interval is assigned to exactly
   * one parent by majority, not split. Consequently contributors need not sum
   * exactly to the parent's day count.
   *
   * <p>{@code parent} is not required to be an interval of a family listed in
   * {@link #contributesTo()}; the method computes regardless. The set is a
   * declaration of which families the result is meaningful for. For example,
   * {@code Weekly.contributorsTo(monthInterval)} yields the weeks that fall in
   * the month by majority, even though {@code Month} is not in
   * {@code Weekly.partitions()}.
   *
   * <p>Custom sequences override this to iterate their table directly, so the
   * method works for parents that span gaps — which a day-by-day walk via
   * {@link #interval(Epoch2kDay)} cannot do.
   *
   * @param parent the interval to attribute child intervals to
   * @return the ordered list of contributing intervals
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

  /**
   * Whether {@code child} is attributed to {@code parent} under the
   * majority-of-days rule.
   *
   * <p>A child is attributed to a parent when more than half its days lie inside
   * it. When the split is exactly 50/50 — possible only when the child has even
   * length and straddles two adjacent parents — the tie is broken toward the
   * <b>earlier</b> parent, defined as the one containing the child's first day.
   * This makes attribution total: every child is attributed to exactly one parent
   * across a family, with no day silently dropped.
   *
   * <p>{@code BiWeekly} into a calendar month is the one current case that
   * exercises the tie: a 14-day bi-week straddling a month boundary has 7 days in
   * each month, and is attributed to the earlier month.
   *
   * <p>The tie-break is defined for any pair {@code (child, parent)} where the
   * overlap is exactly half the child, regardless of whether the parent is one of
   * a canonical family. If {@code parent} contains {@code child.first()}, its
   * overlap is a prefix of the child (the "earlier" side); otherwise it is a
   * suffix (the "later" side).
   *
   * @param child the candidate interval
   * @param parent the interval to attribute {@code child} to
   * @return {@code true} if {@code child} is attributed to {@code parent}
   */
  static boolean majorityInside(Epoch2kInterval child, Epoch2kInterval parent) {
    int overlap = child.overlapLength(parent);
    int childLength = child.lengthInDays();
    if (overlap * 2 != childLength) return overlap * 2 > childLength;
    // Exact 50/50: attribute to the earlier parent — the one containing child.first().
    return parent.contains(child.first());
  }

  /**
   * Invariant check used by every implementation's compact constructor: verifies
   * that {@code partitions} is a subset of {@code contributesTo}. Called at
   * construction time so the invariant cannot be broken by later mutation and
   * misconfiguration fails immediately rather than at query time.
   *
   * @param partitions the strict-tiling set
   * @param contributesTo the majority-attribution set
   * @throws IllegalArgumentException if {@code partitions} contains a family not
   *     in {@code contributesTo}
   * @throws NullPointerException if either argument is {@code null}
   */
  static void checkFamilies(Set<TimeFamilyKey> partitions, Set<TimeFamilyKey> contributesTo) {
    requireNonNull(partitions, "partitions");
    requireNonNull(contributesTo, "contributesTo");
    if (!contributesTo.containsAll(partitions))
      throw new IllegalArgumentException(
          "partitions() must be a subset of contributesTo(); missing: "
              + partitions.stream().filter(p -> !contributesTo.contains(p)).toList());
  }
}