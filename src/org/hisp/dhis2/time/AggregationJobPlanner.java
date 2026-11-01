package org.hisp.dhis2.time;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Expands an abstract aggregation plan (produced by {@link AggregationStepPlanner})
 * against a source time-frame into a concrete list of {@link AggregationJob}s.
 *
 * <p>A job pairs one target interval with the set of source intervals that feed
 * into it, under the step's attribution rule:
 *
 * <ul>
 *   <li>{@link AggregationExactness#EXACT} — the source intervals
 *       are {@code source.partition(targetInterval)}: they tile the target
 *       interval exactly, so the sum of their values is the target value with
 *       no boundary loss.</li>
 *   <li>{@link AggregationExactness#APPROXIMATE} — the source
 *       intervals are {@code source.contributorsTo(targetInterval)}: each is
 *       attributed to {@code targetInterval} by majority of days, with ties
 *       broken toward the earlier parent (the one containing the source
 *       interval's first day). Every source interval is attributed to exactly
 *       one target across the family; sums may not match the true total at
 *       boundaries.</li>
 * </ul>
 *
 * <p>The source frame bounds which source intervals are <em>considered</em>
 * — those whose first day is on or before {@code frame.lastDay()} become
 * seeds for the walk. It does <em>not</em> bound which target intervals are
 * <em>produced</em>. A source interval that straddles out of the frame into
 * a target interval outside it still produces a job for that target, because
 * the interval's value must be attributed to exactly one target and the
 * majority rule may put it outside the frame.
 *
 * <p>For example, expanding weekly → yearly against frame 2024 produces
 * jobs for both 2024 (52 weeks, all majority-in-2024) and 2025 (the single
 * week starting 2024-12-30, which is majority-in-2025). Running the same
 * expansion against frame 2025 produces the same 2025 job plus 2025's own
 * weeks. Across both expansions, every week is attributed to exactly one
 * target exactly once — the frame does not cause duplicate work at
 * boundaries.
 *
 * <p>Target intervals that overlap the frame but have only partial source
 * coverage (because the frame is narrower than the target) still produce jobs;
 * the partial coverage is visible in {@link AggregationJob#sourceIntervals()}.
 * Skipping partial targets is an executor policy, not an expansion concern.
 */
public final class AggregationJobPlanner {

  private final Map<TimeSequenceKey, TimeSequence> byKey;

  public AggregationJobPlanner(Collection<? extends TimeSequence> sequences) {
    var map = new HashMap<TimeSequenceKey, TimeSequence>();
    for (var seq : sequences) {
      var existing = map.putIfAbsent(seq.key(), seq);
      if (existing != null)
        throw new IllegalArgumentException(
            "duplicate sequence key " + seq.key() + ": " + existing + " and " + seq);
    }
    this.byKey = Map.copyOf(map);
  }

  /**
   * Expands an abstract aggregation plan (produced by {@link AggregationStepPlanner})
   * against a source time-frame into a concrete list of {@link AggregationJob}s.
   *
   * <p>A job pairs one target interval with the set of source intervals that feed
   * into it, under the step's attribution rule:
   *
   * <ul>
   *   <li>{@link AggregationExactness#EXACT} — the source intervals
   *       are {@code source.partition(targetInterval)}: they tile the target
   *       interval exactly, so the sum of their values is the target value with
   *       no boundary loss.</li>
   *   <li>{@link AggregationExactness#APPROXIMATE} — the source
   *       intervals are {@code source.contributorsTo(targetInterval)}: each is
   *       attributed to {@code targetInterval} by majority of days, with ties
   *       broken toward the earlier parent (the one containing the source
   *       interval's first day). Every source interval is attributed to exactly
   *       one target across the family; sums may not match the true total at
   *       boundaries.</li>
   * </ul>
   *
   * <h2>Frame bound</h2>
   *
   * <p>Both ends of the expansion are bounded by the frame. <b>Source
   * intervals</b> are seeded by overlap with the frame: any source interval
   * whose first day is on or before {@code frame.lastDay()}, starting from the
   * interval containing {@code frame.firstDay()}, is considered. <b>Target
   * intervals</b> are produced only when they overlap the frame: a source
   * interval whose majority attribution would fall into a target interval
   * entirely outside the frame is dropped from this expansion, and is picked up
   * by the expansion for the frame containing that target.
   *
   * <p>The consequence is composability: expanding a set of adjacent frames
   * that partition a larger range attributes every source interval to exactly
   * one target exactly once — no duplication across boundaries, no loss. Without
   * the target bound, a boundary week starting 2024-12-30 (majority-in-2025)
   * would be attributed to year 2025 in <em>both</em> the expansion for frame
   * 2024 (via the straddling week producing year 2025 as a candidate) and the
   * expansion for frame 2025 (via the week being a seed).
   *
   * <p>Targets that overlap the frame but only partially cover it still
   * produce jobs; the partial coverage is visible in
   * {@link AggregationJob#sourceIntervals()}. Skipping partial targets is an
   * executor policy, not an expansion concern.
   *
   * @param sourceKey the key of the sequence raw data arrives in
   * @param frame the source time-frame; source intervals overlapping this range
   *     are the seeds for the plan
   * @param plan the abstract plan from {@link AggregationStepPlanner}, in
   *     topological order (sources before targets)
   * @return the concrete jobs, in the same step order as the plan, with jobs
   *     for each step grouped consecutively
   * @throws IllegalArgumentException if {@code sourceKey} is not registered, or
   *     if {@code plan} references a key not registered with this expander
   */
  public List<AggregationJob> plan(
      TimeSequenceKey sourceKey, Epoch2kInterval frame, List<AggregationStep> plan) {

    var sourceSeq = byKey.get(sourceKey);
    if (sourceSeq == null)
      throw new IllegalArgumentException("no sequence registered for source key " + sourceKey);
    for (var step : plan) {
      if (!byKey.containsKey(step.source()))
        throw new IllegalArgumentException(
            "no sequence registered for " + step.source() + " (step " + step + ")");
      if (!byKey.containsKey(step.target()))
        throw new IllegalArgumentException(
            "no sequence registered for " + step.target() + " (step " + step + ")");
    }

    var active = new LinkedHashMap<TimeSequenceKey, Set<Epoch2kInterval>>();
    active.put(sourceKey, sourceIntervalsOverlapping(sourceSeq, frame));

    var jobs = new ArrayList<AggregationJob>();

    for (var step : plan) {
      var sourceIntervals = active.getOrDefault(step.source(), Set.of());
      if (sourceIntervals.isEmpty()) continue;

      var targetSeq = byKey.get(step.target());
      var sourceSeqForEdge = byKey.get(step.source());

      // Collect every target interval that any active source interval touches.
      // A straddling source interval contributes to two (or more) consecutive
      // target intervals in this set.
      var targetIntervals = new LinkedHashSet<Epoch2kInterval>();
      for (var si : sourceIntervals) {
        var ti = targetSeq.interval(si.firstDay());
        while (true) {
          targetIntervals.add(ti);
          if (ti.lastDay().notBefore(si.lastDay())) break;
          ti = targetSeq.next(ti);
        }
      }

      // Bound target production to the frame. A source interval that straddles
      // the frame boundary can produce a target interval outside the frame;
      // that target's attribution belongs to the frame containing it, not this
      // one. Without this, adjacent-frame expansions duplicate boundary-week
      // attributions.
      targetIntervals.removeIf(ti -> !ti.overlaps(frame));

      for (var ti : targetIntervals) {
        var feeding =
            switch (step.exactness()) {
              case EXACT -> sourceSeqForEdge.partition(ti);
              case APPROXIMATE -> sourceSeqForEdge.contributorsTo(ti);
            };
        var activeFeeding = feeding.stream().filter(sourceIntervals::contains).toList();
        if (activeFeeding.isEmpty()) continue;

        jobs.add(new AggregationJob(step.source(), step.target(), ti, activeFeeding, step.exactness()));
        active.computeIfAbsent(step.target(), k -> new LinkedHashSet<>()).add(ti);
      }
    }
    return List.copyOf(jobs);
  }

  /**
   * All source intervals that overlap {@code frame}, walked from the interval
   * containing {@code frame.firstDay()} forward. Uses {@link TimeSequence#intervalOrNext}
   * so gap sequences resolve to their next interval rather than throwing, and
   * stops at the end of the sequence rather than looping.
   */
  private static Set<Epoch2kInterval> sourceIntervalsOverlapping(
      TimeSequence seq, Epoch2kInterval frame) {
    var result = new LinkedHashSet<Epoch2kInterval>();
    Epoch2kInterval si;
    try {
      si = seq.intervalOrNext(frame.firstDay());
    } catch (IllegalArgumentException e) {
      return result;
    }
    while (si.firstDay().notAfter(frame.lastDay())) {
      result.add(si);
      try {
        si = seq.next(si);
      } catch (RuntimeException e) {
        break;
      }
    }
    return result;
  }
}