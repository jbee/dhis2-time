package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.AggregationExactness.APPROXIMATE;
import static org.hisp.dhis2.time.AggregationExactness.EXACT;
import static org.hisp.dhis2.time.GregorianSequences.FH;
import static org.hisp.dhis2.time.GregorianSequences.FH4;
import static org.hisp.dhis2.time.GregorianSequences.FHb;
import static org.hisp.dhis2.time.GregorianSequences.FM7;
import static org.hisp.dhis2.time.GregorianSequences.FQ;
import static org.hisp.dhis2.time.GregorianSequences.FQ7;
import static org.hisp.dhis2.time.GregorianSequences.FY7;
import static org.hisp.dhis2.time.GregorianSequences.GF_;
import static org.hisp.dhis2.time.GregorianSequences.GD_;
import static org.hisp.dhis2.time.GregorianSequences.GM;
import static org.hisp.dhis2.time.GregorianSequences.GM_;
import static org.hisp.dhis2.time.GregorianSequences.GQ;
import static org.hisp.dhis2.time.GregorianSequences.GQ_;
import static org.hisp.dhis2.time.GregorianSequences.GS;
import static org.hisp.dhis2.time.GregorianSequences.GS_;
import static org.hisp.dhis2.time.GregorianSequences.GB;
import static org.hisp.dhis2.time.GregorianSequences.GB_;
import static org.hisp.dhis2.time.GregorianSequences.GW;
import static org.hisp.dhis2.time.GregorianSequences.GW_;
import static org.hisp.dhis2.time.GregorianSequences.GY;
import static org.hisp.dhis2.time.GregorianSequences.GY_;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis2.time.Epoch2kDay.MonthOfYear;
import org.junit.jupiter.api.Test;

class AggregationStepPlannerTest {

  /** All eight Gregorian sequences, keyed by their default-offset variants. */
  private static final List<TimeSequence> GREGORIAN =
      List.of(GD_, GW_, GF_, GM_, GB_, GS_, GQ_, GY_);

  private static final TimeSequence FH7 =
      GregorianSequences.fiscalHalfYear(Epoch2kDay.MonthOfYear.July);

  /** All common Gregorian fiscal sequences, keyed by their default-offset variants. */
  private static final List<TimeSequence> GREGORIAN_FISCAL =
      List.of(FM7, FH4, FHb, FQ7, FY7, FH7);


  // ── Basic resolution ─────────────────────────────────────────────

  @Test
  void singleHopExact() {
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GD_.key(), Set.of(GY));
    assertEquals(List.of(new AggregationStep(GD_.key(), GY_.key(), EXACT)), plan);
  }

  @Test
  void explicitKeyResolutionWorks() {
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.plan(GD_.key(), Set.of(GY_.key()));
    assertEquals(List.of(new AggregationStep(GD_.key(), GY_.key(), EXACT)), plan);
  }

  // ── Offset propagation ───────────────────────────────────────────

  @Test
  void offsetPropagatesFromSourceToTarget() {
    var gq4 = GregorianSequences.quarterly(MonthOfYear.April);
    var gy4 = GregorianSequences.yearly(MonthOfYear.April);
    var planner = new AggregationStepPlanner(List.of(gq4, gy4));

    var plan = planner.planFindOffsets(gq4.key(), Set.of(GY));
    assertEquals(List.of(new AggregationStep(gq4.key(), gy4.key(), EXACT)), plan);
  }

  @Test
  void defaultOffsetWhenSourceHasNone() {
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GD_.key(), Set.of(GQ));
    assertEquals(List.of(new AggregationStep(GD_.key(), GQ_.key(), EXACT)), plan);
    assertFalse(plan.get(0).target().isOffset());
  }

  // ── Approximation ────────────────────────────────────────────────

  @Test
  void approximationWhenOnlyContributesToExists() {
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GW_.key(), Set.of(GY));
    assertEquals(List.of(new AggregationStep(GW_.key(), GY_.key(), APPROXIMATE)), plan);
  }

  @Test
  void exactOnlyRejectsWeeklyToYear() {
    var planner = new AggregationStepPlanner(GREGORIAN);
    assertThrows(
        IllegalArgumentException.class, () -> planner.planFindOffsets(GW_.key(), Set.of(GY), false));
  }

  @Test
  void fortnightlyReachesYearViaApproximation() {
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GF_.key(), Set.of(GY));
    assertEquals(List.of(new AggregationStep(GF_.key(), GY_.key(), APPROXIMATE)), plan);
  }

  @Test
  void approximationEdgeIsInContributesToButNotPartitions() {
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GW_.key(), Set.of(GY));
    var edge = plan.get(0);
    assertEquals(APPROXIMATE, edge.exactness());
    assertTrue(GW_.contributesTo().contains(edge.target()));
    assertFalse(GW_.partitions().contains(edge.target()));
  }

  // ── Chaining through intermediates ──────────────────────────────

  @Test
  void multiTargetChainsThroughIntermediates() {
    // The classic case: asking for month, quarter and year from daily data.
    // The plan folds each stage into the next rather than re-summing raw days.
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GD_.key(), Set.of(GM, GQ, GY));

    assertEquals(
        List.of(
            new AggregationStep(GD_.key(), GM_.key(), EXACT),
            new AggregationStep(GM_.key(), GQ_.key(), EXACT),
            new AggregationStep(GQ_.key(), GY_.key(), EXACT)),
        plan);
  }

  @Test
  void chainFormsWhenCoarserIntermediateIsTarget() {
    // GM is a target, so GY is reached via GM (fewer reads than GD → GY).
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GD_.key(), Set.of(GM, GY));

    assertEquals(
        List.of(
            new AggregationStep(GD_.key(), GM_.key(), EXACT),
            new AggregationStep(GM_.key(), GY_.key(), EXACT)),
        plan);
  }

  @Test
  void singleTargetIsDirectNotChained() {
    // No intermediate targets, so no intermediates are introduced.
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GD_.key(), Set.of(GY));

    assertEquals(List.of(new AggregationStep(GD_.key(), GY_.key(), EXACT)), plan);
  }

  @Test
  void chainThroughExactIntermediateThenApproximate() {
    // GB is a target; GY is reached via GB (approximation) rather than GW directly.
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GW_.key(), Set.of(GregorianSequences.GF, GY));

    assertEquals(
        List.of(
            new AggregationStep(GW_.key(), GF_.key(), EXACT),
            new AggregationStep(GF_.key(), GY_.key(), APPROXIMATE)),
        plan);
  }

  @Test
  void multiTargetWithMixedGranularity() {
    // Month, bi-month, year: each step prefers the coarsest already-reached source.
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GD_.key(), Set.of(GM, GB, GY));

    // Sorted by interval length: GM (~30), GT (~61), GY (~365)
    //   GD → GM  (GD is only candidate)
    //   GM → GT  (GM partitions GT; GD also partitions GT but GM is coarser)
    //   GT → GY  (GT partitions GY; GM and GD partition GY too, GT coarsest)
    assertEquals(
        List.of(
            new AggregationStep(GD_.key(), GM_.key(), EXACT),
            new AggregationStep(GM_.key(), GB_.key(), EXACT),
            new AggregationStep(GB_.key(), GY_.key(), EXACT)),
        plan);
  }

  // ── Multi-target (non-chained where no chain exists) ───────────

  @Test
  void addingFinerTargetDeepensChain() {
    var planner = new AggregationStepPlanner(GREGORIAN);

    // Without GM: GQ is reached from GD, then GS is reached from GQ.
    // Two hops.
    var withoutMonth = planner.planFindOffsets(GD_.key(), Set.of(GQ, GS));
    assertEquals(
        List.of(
            new AggregationStep(GD_.key(), GQ_.key(), EXACT),
            new AggregationStep(GQ_.key(), GS_.key(), EXACT)),
        withoutMonth);

    // With GM: the chain gains a step (GD → GM → GQ), and GQ is now
    // computed from 12 monthly values instead of 365 daily values.
    // GS is still computed from GQ — that edge is unchanged.
    var withMonth = planner.planFindOffsets(GD_.key(), Set.of(GM, GQ, GS));
    assertEquals(
        List.of(
            new AggregationStep(GD_.key(), GM_.key(), EXACT),
            new AggregationStep(GM_.key(), GQ_.key(), EXACT),
            new AggregationStep(GQ_.key(), GS_.key(), EXACT)),
        withMonth);
  }

  @Test
  void nestedTargetsFormLinearChain() {
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GD_.key(), Set.of(GM, GQ, GS));

    // Processed finest-to-coarsest: GM (31d), GQ (91d), GS (182d).
    // Each target is reached from the previous coarser source.
    assertEquals(
        List.of(
            new AggregationStep(GD_.key(), GM_.key(), EXACT),
            new AggregationStep(GM_.key(), GQ_.key(), EXACT),
            new AggregationStep(GQ_.key(), GS_.key(), EXACT)),
        plan);
  }

  @Test
  void planIsTopologicallyOrdered() {
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GD_.key(), Set.of(GM, GQ, GS));

    Map<AggregationStep, Integer> index = new HashMap<>();
    for (int i = 0; i < plan.size(); i++) index.put(plan.get(i), i);

    // Every step comes after the step that produces its source
    for (var step : plan) {
      if (step.source().equals(GD_.key())) continue;
      var producerIndex =
          plan.stream()
              .filter(e -> e.target().equals(step.source()))
              .findFirst()
              .map(index::get)
              .orElseThrow(() -> new AssertionError("no producer for " + step.source()));
      assertTrue(
          producerIndex < index.get(step),
          () -> "producer of " + step.source() + " must precede " + step);
    }
  }

  @Test
  void independentTargetFamiliesBranchFromSource() {
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GD_.key(), Set.of(GW, GM));

    // Weekly and Monthly: GW.partitions() = {GF}, GM.partitions() =
    // {GB, GQ, GS, GY}. Neither contains the other, so both branch
    // from the source rather than chaining.
    assertEquals(2, plan.size());
    assertEquals(
        Set.of(
            new AggregationStep(GD_.key(), GW_.key(), EXACT),
            new AggregationStep(GD_.key(), GM_.key(), EXACT)),
        new HashSet<>(plan));
  }

  // ── Degenerate cases ─────────────────────────────────────────────

  @Test
  void sourceInTargetsGivesEmptyPlan() {
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GD_.key(), Set.of(GregorianSequences.GD));
    assertTrue(plan.isEmpty());
  }

  @Test
  void emptyTargetSetGivesEmptyPlan() {
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GD_.key(), Set.of());
    assertTrue(plan.isEmpty());
  }

  // ── Error cases ──────────────────────────────────────────────────

  @Test
  void unreachableFamilyThrows() {
    var fq7 = GregorianSequences.fiscalQuarterly(MonthOfYear.July);
    var fiscalResolver = new AggregationStepPlanner(List.of(fq7));

    assertThrows(
        IllegalArgumentException.class, () -> fiscalResolver.planFindOffsets(fq7.key(), Set.of(GY)));
  }

  @Test
  void unreachableKeyThrows() {
    var gq_ = GregorianSequences.quarterly(MonthOfYear.January);
    var gy4 = GregorianSequences.yearly(MonthOfYear.April);
    var planner = new AggregationStepPlanner(List.of(gq_, gy4));

    assertThrows(
        IllegalArgumentException.class, () -> planner.plan(gq_.key(), Set.of(gy4.key())));
  }

  @Test
  void unregisteredSourceThrows() {
    var planner = new AggregationStepPlanner(GREGORIAN);
    var unknown = new TimeSequenceKey('Z', 'D');
    assertThrows(IllegalArgumentException.class, () -> planner.planFindOffsets(unknown, Set.of(GY)));
  }

  @Test
  void unregisteredTargetKeyThrows() {
    var planner = new AggregationStepPlanner(GREGORIAN);
    var unknown = new TimeSequenceKey('Z', 'Y');
    assertThrows(
        IllegalArgumentException.class, () -> planner.plan(GD_.key(), Set.of(unknown)));
  }

  @Test
  void crossCalendarUnreachable() {
    var planner =
        new AggregationStepPlanner(
            List.of(
                IslamicSequences.ID_, IslamicSequences.IM_, IslamicSequences.IY_, GD_, GM_, GY_));

    assertThrows(
        IllegalArgumentException.class,
        () -> planner.planFindOffsets(IslamicSequences.ID_.key(), Set.of(GY)));
  }

  @Test
  void duplicateSequenceRegistrationRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AggregationStepPlanner(
                List.of(GregorianSequences.daily(), GregorianSequences.daily())));
  }

  // ── Multi-hop via non-target intermediates ──────────────────────

  @Test
  void multiHopExactChainThroughNonTargetIntermediates() {
    // No target is a direct partition of ST, so BFS introduces GM and GQ as
    // intermediates. The plan must be a full chain, not isolated edges.
    var st = new TimeSequenceKey('S', 'T');
    var gm = GM.noOffset();
    var gq = GQ.noOffset();
    var gy = GY.noOffset();

    var planner =
        new AggregationStepPlanner(
            List.of(
                fakeSequence(st, Set.of(gm), Set.of(gm)),
                fakeSequence(gm, Set.of(gq), Set.of(gq, gy)),
                fakeSequence(gq, Set.of(gy), Set.of(gy)),
                fakeSequence(gy, Set.of(), Set.of())));

    var plan = planner.planFindOffsets(st, Set.of(GY));

    assertEquals(
        List.of(
            new AggregationStep(st, gm, EXACT),
            new AggregationStep(gm, gq, EXACT),
            new AggregationStep(gq, gy, EXACT)),
        plan);
  }

  @Test
  void sharedIntermediateIsDeduplicated() {
    var st = new TimeSequenceKey('S', 'T');
    var gm = GM.noOffset();
    var gq = GQ.noOffset();
    var gy = GY.noOffset();

    var planner =
        new AggregationStepPlanner(
            List.of(
                fakeSequence(st, Set.of(gm), Set.of(gm)),
                fakeSequence(gm, Set.of(gq, gy), Set.of(gq, gy)),
                fakeSequence(gq, Set.of(), Set.of()),
                fakeSequence(gy, Set.of(), Set.of())));

    var plan = planner.planFindOffsets(st, Set.of(GQ, GY));

    assertEquals(3, plan.size());
    assertEquals(
        Set.of(
            new AggregationStep(st, gm, EXACT),
            new AggregationStep(gm, gq, EXACT),
            new AggregationStep(gm, gy, EXACT)),
        new HashSet<>(plan));
    assertEquals(new AggregationStep(st, gm, EXACT), plan.get(0));
  }

  // ── Invariants ───────────────────────────────────────────────────

  @Test
  void edgeTypeMatchesSequenceDeclaration() {
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GW_.key(), Set.of(GM, GQ, GY), true);

    assertFalse(plan.isEmpty());
    for (var edge : plan) {
      var sourceSeq =
          GREGORIAN.stream()
              .filter(s -> s.key().equals(edge.source()))
              .findFirst()
              .orElseThrow(() -> new AssertionError("unknown source " + edge.source()));
      var expected = sourceSeq.partitions().contains(edge.target()) ? EXACT : APPROXIMATE;
      assertEquals(expected, edge.exactness(), () -> "edge " + edge);
    }
  }

  @Test
  void quarterlyChainsToSemester() {
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GD_.key(), Set.of(GQ, GS));

    // GQ.partitions() now includes GS (2 quarters = 1 semester).
    // Chaining through GQ is preferred over re-reading 182 days for GS.
    assertEquals(
        List.of(
            new AggregationStep(GD_.key(), GQ_.key(), EXACT),
            new AggregationStep(GQ_.key(), GS_.key(), EXACT)),
        plan);

    // GS is not reached directly from GD
    assertFalse(
        plan.stream()
            .anyMatch(s -> s.source().equals(GD_.key()) && s.target().equals(GS_.key())));
  }

  @Test
  void bimesterChainsToSemester() {
    var planner = new AggregationStepPlanner(GREGORIAN);
    var plan = planner.planFindOffsets(GD_.key(), Set.of(GB, GS));

    // GB.partitions() now includes GS (3 bimesters = 1 semester).
    // GB (61d) processed before GS (182d), so GB → GS.
    assertEquals(
        List.of(
            new AggregationStep(GD_.key(), GB_.key(), EXACT),
            new AggregationStep(GB_.key(), GS_.key(), EXACT)),
        plan);
  }

  @Test
  void fiscalQuarterlyChainsToFiscalHalfYear() {
    var resolver = new AggregationStepPlanner(GREGORIAN_FISCAL);
    var plan = resolver.planFindOffsets(FM7.key(), Set.of(FQ, FH));

    // FM7 → FQ7 → FH7. Each is the July-start fiscal variant.
    assertEquals(
        List.of(
            new AggregationStep(FM7.key(), FQ7.key(), EXACT),
            new AggregationStep(FQ7.key(), FH7.key(), EXACT)),
        plan);
  }

  @Test
  void partitionDepthWinsOverIntervalLengthInSorting() {
    var a1 = new TimeSequenceKey('A', 'X');
    var a2 = new TimeSequenceKey('A', 'Y');

    // Source partitions both. Depth: a2 has depth 0 (nothing partitions it),
    // a1 has depth 1 (a2 partitions it). Sort order: a2 first, then a1.
    var source = fakeSequence(new TimeSequenceKey('A', 'Q'), 1, Set.of(a1, a2));
    // Deliberately inverted: a2 is depth-finer but length-longer.
    // A length-only sort would process a1 first (5 < 10), then a2 from
    // source — missing the chain from a2 to a1 that depth sort enables.
    var a1Seq = fakeSequence(a1, 5, Set.of());
    var a2Seq = fakeSequence(a2, 10, Set.of(a1));

    var planner = new AggregationStepPlanner(List.of(source, a1Seq, a2Seq));
    var plan = planner.plan(source.key(), Set.of(a1, a2));

    // Depth sort produces the chain: a2 → a1.
    // Length-only sort would produce: source → a1, source → a2 (no chain).
    assertEquals(
        List.of(
            new AggregationStep(source.key(), a2, EXACT),
            new AggregationStep(a2, a1, EXACT)),
        plan);
  }

  private static TimeSequence fakeSequence(
      TimeSequenceKey key, int intervalLength, Set<TimeSequenceKey> partitions) {
    return new TimeSequence() {
      @Override public TimeSequenceKey key() { return key; }
      @Override public Set<TimeSequenceKey> partitions() { return partitions; }
      @Override public Set<TimeSequenceKey> contributesTo() { return partitions; }
      @Override public Epoch2kInterval interval(Epoch2kDay day) {
        return new Epoch2kInterval(day, day.plusDays(intervalLength - 1));
      }
    };
  }

  // ── Helpers ──────────────────────────────────────────────────────

  private static TimeSequence fakeSequence(
      TimeSequenceKey key, Set<TimeSequenceKey> partitions, Set<TimeSequenceKey> contributesTo) {
    return new TimeSequence() {
      @Override
      public TimeSequenceKey key() {
        return key;
      }

      @Override
      public Set<TimeSequenceKey> partitions() {
        return partitions;
      }

      @Override
      public Set<TimeSequenceKey> contributesTo() {
        return contributesTo;
      }

      @Override
      public Epoch2kInterval interval(Epoch2kDay day) {
        return new Epoch2kInterval(day);
      }
    };
  }
}
