package org.hisp.dhis2.time;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Plans an ordered set of {@link AggregationStep} steps that transform values
 * from a source sequence into values in one or more target sequences.
 *
 * <p>An aggregation step's {@link AggregationExactness} reflects whether the source
 * contributes to the target by {@link TimeSequence#partitions() partition}
 * ({@link AggregationExactness#EXACT}) or by {@link TimeSequence#contributesTo()
 * majority attribution} ({@link AggregationExactness#APPROXIMATE}).
 *
 * <h2>Chaining through intermediates</h2>
 *
 * <p>When multiple targets are requested and one target can feed another,
 * the plan chains through the finer target rather than re-aggregating from
 * the source. For example, {@code expandFind(GD_, {GM, GQ, GY})} produces
 * {@code GD → GM → GQ → GY}, not three independent edges from {@code GD}.
 * Chained plans do far less work: each step aggregates from a small set of
 * intervals rather than re-summing raw day-level data.
 *
 * <p>Chains are only formed when the intermediate is itself a target (or
 * the source). Intermediates that aren't targets are introduced only when
 * required — i.e. when no already-reached tag can reach the target, in
 * which case the shortest BFS path is used. This keeps the plan from
 * computing values nobody asked for.
 *
 * <h2>Cost ordering</h2>
 *
 * <p>Targets are processed finest to coarsest, and each target's source is
 * the coarsest already-reached tag that can reach it — which minimises the
 * target's own aggregation work. EXACT edges are preferred over APPROXIMATION
 * edges; if {@code allowApproximations} is {@code false}, only exact paths
 * are considered.
 */
public final class AggregationStepPlanner {

  /**
   * Sample day used to measure each sequence's typical interval length for
   * cost comparisons. Any day within the sequences' supported range works;
   * 2000-01-01 is inside every calendar sequence and before every reasonable
   * custom-sequence range.
   */
  private static final Epoch2kDay SAMPLE_DAY = new Epoch2kDay(0);

  private final Map<TimeSequenceTag, TimeSequence> byTag;
  private final Map<TimeSequenceTag, Integer> intervalLengths;

  /**
   * @param sequences all sequences the resolver may use, both as sources and
   *     as intermediates; duplicate tags are rejected so lookups are unambiguous
   */
  public AggregationStepPlanner(Collection<? extends TimeSequence> sequences) {
    var map = new HashMap<TimeSequenceTag, TimeSequence>();
    for (var seq : sequences) {
      var existing = map.putIfAbsent(seq.tag(), seq);
      if (existing != null)
        throw new IllegalArgumentException(
            "duplicate sequence tag " + seq.tag() + ": " + existing + " and " + seq);
    }
    this.byTag = Map.copyOf(map);

    var lengths = new HashMap<TimeSequenceTag, Integer>();
    for (var seq : sequences) {
      try {
        lengths.put(seq.tag(), seq.intervalOrNext(SAMPLE_DAY).lengthInDays());
      } catch (Exception e) {
        lengths.put(seq.tag(), 1);
      }
    }
    this.intervalLengths = Map.copyOf(lengths);
  }

  // ── Public API ─────────────────────────────────────────────────────

  /** Equivalent to {@code planFindOffsets(source, targets, true)}. */
  public List<AggregationStep> planFindOffsets(TimeSequenceTag source, Set<TimeFamilyTag> targets) {
    return planFindOffsets(source, targets, true);
  }

  /**
   * Resolve a plan from {@code source} to each target family. The concrete
   * target tags for each family is chosen from the reachable tags: the source
   * offset propagates when the target shares its offset domain, otherwise the
   * family's no-offset variant is used.
   *
   * @throws IllegalArgumentException if {@code source} is not registered, or
   *     any target family is not reachable under the given constraints
   */
  public List<AggregationStep> planFindOffsets(
      TimeSequenceTag source, Set<TimeFamilyTag> targets, boolean allowApproximations) {
    var reachable = reachableFrom(source, allowApproximations);
    var concreteTargets = new LinkedHashSet<TimeSequenceTag>();
    for (var family : targets) {
      concreteTargets.add(pickTagForFamily(reachable, source, family));
    }
    return chainAggregations(source, concreteTargets, allowApproximations, reachable);
  }

  /** Equivalent to {@code resolveExact(source, targets, true)}. */
  public List<AggregationStep> plan(TimeSequenceTag source, Set<TimeSequenceTag> targets) {
    return plan(source, targets, true);
  }

  /**
   * Resolve a plan from {@code source} to each concrete target tag. Every tag
   * in {@code targets} must be registered with this resolver and reachable
   * from {@code source} under the given constraints.
   *
   * @throws IllegalArgumentException if any target tag is not registered or
   *     not reachable from {@code source}
   */
  public List<AggregationStep> plan(
      TimeSequenceTag source, Set<TimeSequenceTag> targets, boolean allowApproximations) {
    for (var target : targets) {
      if (!byTag.containsKey(target))
        throw new IllegalArgumentException("no sequence registered for target tag " + target);
    }
    return chainAggregations(source, targets, allowApproximations, null);
  }

  // ── Chained planning ───────────────────────────────────────────────

  /**
   * Build the plan by processing targets finest-to-coarsest and picking the
   * coarsest already-reached source that can reach each. Targets that aren't
   * reachable from any already-reached tag fall back to the shortest BFS path.
   */
  private List<AggregationStep> chainAggregations(
      TimeSequenceTag source,
      Set<TimeSequenceTag> targets,
      boolean allowApproximations,
      Map<TimeSequenceTag, Reach> reachableHint) {
    if (targets.isEmpty()) return List.of();

    var reached = new LinkedHashSet<TimeSequenceTag>();
    reached.add(source);

    var edges = new LinkedHashSet<AggregationStep>();

    var sortedTargets = sortTargetsFinerFirst(targets);

    Map<TimeSequenceTag, Reach> reachable = reachableHint;

    for (var target : sortedTargets) {
      if (reached.contains(target)) continue; // source already provides it

      var bestSource = findBestSource(target, reached, allowApproximations);
      if (bestSource != null) {
        var type =
            byTag.get(bestSource).partitions().contains(target)
                ? AggregationExactness.EXACT
                : AggregationExactness.APPROXIMATE;
        edges.add(new AggregationStep(bestSource, target, type));
        reached.add(target);
      } else {
        if (reachable == null) reachable = reachableFrom(source, allowApproximations);
        if (!reachable.containsKey(target)) {
          throw new IllegalArgumentException(
              "target "
                  + target
                  + " is not reachable from "
                  + source
                  + (allowApproximations ? "" : " without approximations"));
        }
        for (var edge : reachable.get(target).path()) {
          edges.add(edge);
          reached.add(edge.target());
        }
      }
    }
    return topoSortEdges(edges);
  }

  /**
   * Sorts targets so that each target is processed after any target that can
   * reach it via {@code partitions()}, transitively. This is the order that
   * makes chaining possible: when a coarser target is processed, all finer
   * targets that could feed into it have already been computed.
   *
   * <p>Finer-first is the outcome; the primary key is partition depth (a
   * target's longest path from any other target that partitions it). Length is
   * a stable tiebreaker among targets at the same depth — it gives a
   * deterministic order without affecting plan correctness, because same-depth
   * targets are by definition incomparable via partitions and therefore cannot
   * chain from each other. Length also expresses a mild preference: among
   * equally-deep targets, process the longer one first, so that it is available
   * as a source when deeper targets are processed.
   *
   * <p>Interval length alone is not used as the primary key because a
   * sequence's typical interval length need not reflect its partition depth.
   * The 53-week fortnight stub is one real case: its interval length equals a
   * week's, which would tie it with the week in a length-only sort. Custom
   * sequences could in principle have any length/depth relationship.
   */
  private List<TimeSequenceTag> sortTargetsFinerFirst(Set<TimeSequenceTag> targets) {
    var depth = new HashMap<TimeSequenceTag, Integer>();
    for (var t : targets) depth.put(t, 0);

    // depth(t) = 1 + max(depth(p) : t ∈ p.partitions(), p ∈ targets)
    // Fixpoint iteration; bounded by |targets| so cycles (which would be a
    // configuration error elsewhere) don't loop forever.
    int maxIterations = targets.size();
    for (int i = 0; i < maxIterations; i++) {
      boolean changed = false;
      for (var t : targets) {
        for (var p : targets) {
          if (p.equals(t)) continue;
          if (byTag.get(p).partitions().contains(t)) {
            int candidate = depth.get(p) + 1;
            if (candidate > depth.get(t)) {
              depth.put(t, candidate);
              changed = true;
            }
          }
        }
      }
      if (!changed) break;
    }

    var sorted = new ArrayList<>(targets);
    sorted.sort(
        Comparator
            .comparingInt((TimeSequenceTag k) -> depth.get(k))
            .thenComparingInt(this::intervalLength));
    return sorted;
  }

  /**
   * Among {@code reached}, the coarsest tag that can reach {@code target} —
   * preferring EXACT edges over APPROXIMATION. Coarsest is measured by
   * interval length, which approximates the per-step work saved by using it
   * as the source: a coarser source means fewer intervals summed into the
   * target.
   */
  private TimeSequenceTag findBestSource(
      TimeSequenceTag target, Set<TimeSequenceTag> reached, boolean allowApproximations) {
    TimeSequenceTag best = null;
    int bestLength = -1;

    for (var candidate : reached) {
      var seq = byTag.get(candidate);
      if (seq.partitions().contains(target)) {
        int len = intervalLength(candidate);
        if (len > bestLength) {
          best = candidate;
          bestLength = len;
        }
      }
    }
    if (best != null) return best;

    if (!allowApproximations) return null;

    for (var candidate : reached) {
      var seq = byTag.get(candidate);
      if (seq.partitions().contains(target)) continue;
      if (seq.contributesTo().contains(target)) {
        int len = intervalLength(candidate);
        if (len > bestLength) {
          best = candidate;
          bestLength = len;
        }
      }
    }
    return best;
  }

  private int intervalLength(TimeSequenceTag tag) {
    return intervalLengths.getOrDefault(tag, 1);
  }

  // ── BFS over the declared aggregation graph (fallback path) ────────

  private record Reach(TimeSequenceTag node, List<AggregationStep> path, int hops, int approximations) {
    Reach(TimeSequenceTag node, List<AggregationStep> path) {
      this(
          node,
          path,
          path.size(),
          (int) path.stream().filter(a -> a.exactness() == AggregationExactness.APPROXIMATE).count());
    }
  }

  private static final Comparator<Reach> COST_ORDER =
      Comparator.comparingInt(Reach::approximations).thenComparingInt(Reach::hops);

  private Map<TimeSequenceTag, Reach> reachableFrom(
      TimeSequenceTag source, boolean allowApproximations) {
    if (!byTag.containsKey(source))
      throw new IllegalArgumentException("no sequence registered for source tag " + source);

    var best = new HashMap<TimeSequenceTag, Reach>();
    var pq = new PriorityQueue<Reach>(COST_ORDER);
    var seed = new Reach(source, List.of());
    best.put(source, seed);
    pq.add(seed);

    while (!pq.isEmpty()) {
      var current = pq.poll();
      if (COST_ORDER.compare(current, best.get(current.node())) > 0) continue;

      var seq = byTag.get(current.node());
      if (seq == null) continue;

      for (var target : seq.partitions()) {
        if (!byTag.containsKey(target)) continue;
        tryOffer(
            best,
            pq,
            new Reach(
                target,
                append(current.path(), new AggregationStep(
                    current.node(), target, AggregationExactness.EXACT))));
      }

      if (allowApproximations) {
        for (var target : seq.contributesTo()) {
          if (seq.partitions().contains(target)) continue;
          if (!byTag.containsKey(target)) continue;
          tryOffer(
              best,
              pq,
              new Reach(
                  target,
                  append(current.path(), new AggregationStep(
                      current.node(), target, AggregationExactness.APPROXIMATE))));
        }
      }
    }
    return best;
  }

  private static void tryOffer(
      Map<TimeSequenceTag, Reach> best, PriorityQueue<Reach> pq, Reach candidate) {
    var existing = best.get(candidate.node());
    if (existing == null || COST_ORDER.compare(candidate, existing) < 0) {
      best.put(candidate.node(), candidate);
      pq.add(candidate);
    }
  }

  // ── Target selection for expandFind ───────────────────────────────

  private TimeSequenceTag pickTagForFamily(
      Map<TimeSequenceTag, Reach> reachable, TimeSequenceTag source, TimeFamilyTag family) {
    var candidates =
        reachable.keySet().stream().filter(k -> k.family().equals(family)).toList();
    if (candidates.isEmpty()) {
      throw new IllegalArgumentException(
          "target family "
              + family
              + " is not reachable from "
              + source
              + "; reachable families: "
              + reachable.keySet().stream().map(TimeSequenceTag::family).distinct().toList());
    }
    if (candidates.size() == 1) return candidates.get(0);

    if (source.isOffset()) {
      var matching =
          candidates.stream().filter(c -> c.offset() == source.offset()).findFirst();
      if (matching.isPresent()) return matching.get();
    }
    var defaultTag = candidates.stream().filter(c -> !c.isOffset()).findFirst();
    return defaultTag.orElse(candidates.get(0));
  }

  // ── Path and edge utilities ────────────────────────────────────────

  private static List<AggregationStep> append(List<AggregationStep> path, AggregationStep edge) {
    var next = new ArrayList<AggregationStep>(path.size() + 1);
    next.addAll(path);
    next.add(edge);
    return List.copyOf(next);
  }

  private static List<AggregationStep> topoSortEdges(Collection<AggregationStep> edges) {
    if (edges.isEmpty()) return List.of();

    var producing = new HashMap<TimeSequenceTag, List<AggregationStep>>();
    var consuming = new HashMap<TimeSequenceTag, List<AggregationStep>>();
    for (var edge : edges) {
      producing.computeIfAbsent(edge.target(), k -> new ArrayList<>()).add(edge);
      consuming.computeIfAbsent(edge.source(), k -> new ArrayList<>()).add(edge);
    }

    var indegree = new HashMap<AggregationStep, Integer>(edges.size());
    for (var edge : edges) {
      indegree.put(edge, producing.getOrDefault(edge.source(), List.of()).size());
    }

    var queue = new ArrayDeque<AggregationStep>();
    for (var edge : edges) if (indegree.get(edge) == 0) queue.add(edge);

    var result = new ArrayList<AggregationStep>(edges.size());
    while (!queue.isEmpty()) {
      var current = queue.poll();
      result.add(current);
      for (var downstream : consuming.getOrDefault(current.target(), List.of())) {
        if (indegree.merge(downstream, -1, Integer::sum) == 0) queue.add(downstream);
      }
    }

    if (result.size() != edges.size())
      throw new IllegalStateException(
          "cycle detected in aggregation edges: emitted "
              + result.size()
              + " of "
              + edges.size());
    return result;
  }
}