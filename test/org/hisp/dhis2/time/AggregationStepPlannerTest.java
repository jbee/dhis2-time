package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.AggregationExactness.APPROXIMATE;
import static org.hisp.dhis2.time.AggregationExactness.EXACT;
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
import static org.hisp.dhis2.time.GregorianSequences.GW_;
import static org.hisp.dhis2.time.GregorianSequences.GY;
import static org.hisp.dhis2.time.GregorianSequences.GY_;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis2.time.Epoch2kDay.MonthOfYear;
import org.junit.jupiter.api.Test;

class AggregationStepPlannerTest {

  /** All eight Gregorian sequences, keyed by their default-offset variants. */
  private static final List<TimeSequence> GREGORIAN =
      List.of(GD_, GW_, GF_, GM_, GB_, GS_, GQ_, GY_);

  // ── Basic resolution ─────────────────────────────────────────────

  @Test
  void singleHopExact() {
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var plan = resolver.planFindOffsets(GD_.key(), Set.of(GY));
    assertEquals(List.of(new AggregationStep(GD_.key(), GY_.key(), EXACT)), plan);
  }

  @Test
  void explicitKeyResolutionWorks() {
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var plan = resolver.plan(GD_.key(), Set.of(GY_.key()));
    assertEquals(List.of(new AggregationStep(GD_.key(), GY_.key(), EXACT)), plan);
  }

  // ── Offset propagation ───────────────────────────────────────────

  @Test
  void offsetPropagatesFromSourceToTarget() {
    var gq4 = GregorianSequences.quarterly(MonthOfYear.April);
    var gy4 = GregorianSequences.yearly(MonthOfYear.April);
    var resolver = new AggregationStepPlanner(List.of(gq4, gy4));

    var plan = resolver.planFindOffsets(gq4.key(), Set.of(GY));
    assertEquals(List.of(new AggregationStep(gq4.key(), gy4.key(), EXACT)), plan);
  }

  @Test
  void defaultOffsetWhenSourceHasNone() {
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var plan = resolver.planFindOffsets(GD_.key(), Set.of(GQ));
    assertEquals(List.of(new AggregationStep(GD_.key(), GQ_.key(), EXACT)), plan);
    assertFalse(plan.get(0).target().isOffset());
  }

  // ── Approximation ────────────────────────────────────────────────

  @Test
  void approximationWhenOnlyContributesToExists() {
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var plan = resolver.planFindOffsets(GW_.key(), Set.of(GY));
    assertEquals(List.of(new AggregationStep(GW_.key(), GY_.key(), APPROXIMATE)), plan);
  }

  @Test
  void exactOnlyRejectsWeeklyToYear() {
    var resolver = new AggregationStepPlanner(GREGORIAN);
    assertThrows(
        IllegalArgumentException.class, () -> resolver.planFindOffsets(GW_.key(), Set.of(GY), false));
  }

  @Test
  void fortnightlyReachesYearViaApproximation() {
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var plan = resolver.planFindOffsets(GF_.key(), Set.of(GY));
    assertEquals(List.of(new AggregationStep(GF_.key(), GY_.key(), APPROXIMATE)), plan);
  }

  @Test
  void approximationEdgeIsInContributesToButNotPartitions() {
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var plan = resolver.planFindOffsets(GW_.key(), Set.of(GY));
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
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var plan = resolver.planFindOffsets(GD_.key(), Set.of(GM, GQ, GY));

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
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var plan = resolver.planFindOffsets(GD_.key(), Set.of(GM, GY));

    assertEquals(
        List.of(
            new AggregationStep(GD_.key(), GM_.key(), EXACT),
            new AggregationStep(GM_.key(), GY_.key(), EXACT)),
        plan);
  }

  @Test
  void singleTargetIsDirectNotChained() {
    // No intermediate targets, so no intermediates are introduced.
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var plan = resolver.planFindOffsets(GD_.key(), Set.of(GY));

    assertEquals(List.of(new AggregationStep(GD_.key(), GY_.key(), EXACT)), plan);
  }

  @Test
  void chainThroughExactIntermediateThenApproximate() {
    // GB is a target; GY is reached via GB (approximation) rather than GW directly.
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var plan = resolver.planFindOffsets(GW_.key(), Set.of(GregorianSequences.GF, GY));

    assertEquals(
        List.of(
            new AggregationStep(GW_.key(), GF_.key(), EXACT),
            new AggregationStep(GF_.key(), GY_.key(), APPROXIMATE)),
        plan);
  }

  @Test
  void multiTargetWithMixedGranularity() {
    // Month, bi-month, year: each step prefers the coarsest already-reached source.
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var plan = resolver.planFindOffsets(GD_.key(), Set.of(GM, GB, GY));

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
  void multipleIndependentTargetFamilies() {
    // Quarterly and six-monthly both reachable from daily, but neither
    // partitions the other, so no chain forms.
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var plan = resolver.planFindOffsets(GD_.key(), Set.of(GS, GQ));

    assertEquals(2, plan.size());
    assertEquals(
        Set.of(
            new AggregationStep(GD_.key(), GQ_.key(), EXACT),
            new AggregationStep(GD_.key(), GS_.key(), EXACT)),
        new HashSet<>(plan));
  }

  @Test
  void chainedPlanWithSharedMonthIntermediate() {
    // Adding GM to the previous case changes the plan shape entirely:
    //   - GM is processed first (finest target), so GD → GM is emitted.
    //   - GQ and GS are both processed after; both are reachable EXACTLY from GM.
    //   - GQ contributesTo GS only via APPROXIMATION, so GM is preferred as GS's
    //     source even though GQ has a shorter interval length. Exactness wins
    //     before coarseness.
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var plan = resolver.planFindOffsets(GD_.key(), Set.of(GM, GQ, GS));

    assertEquals(
        List.of(
            new AggregationStep(GD_.key(), GM_.key(), EXACT),
            new AggregationStep(GM_.key(), GQ_.key(), EXACT),
            new AggregationStep(GM_.key(), GS_.key(), EXACT)),
        plan);
  }

  @Test
  void addingMonthToIndependentTargetsCollapsesToChain() {
    // Without GM: GD → GQ, GD → GS (two flat edges from source).
    // With GM:    GD → GM → {GQ, GS} (one prefix, two consumers).
    // The total edge count increases by one, but the raw-day reads drop from
    // (365 + 365) to 365 (for GM) + 3 + 2 (for GQ, GS from GM's 12 intervals).
    var resolver = new AggregationStepPlanner(GREGORIAN);

    var withoutMonth = resolver.planFindOffsets(GD_.key(), Set.of(GQ, GS));
    assertEquals(2, withoutMonth.size());
    assertTrue(withoutMonth.stream().allMatch(e -> e.source().equals(GD_.key())));

    var withMonth = resolver.planFindOffsets(GD_.key(), Set.of(GM, GQ, GS));
    assertEquals(3, withMonth.size());
    assertEquals(new AggregationStep(GD_.key(), GM_.key(), EXACT), withMonth.get(0));
    assertEquals(
        Set.of(GM_.key()),
        withMonth.stream()
            .skip(1)
            .map(AggregationStep::source)
            .collect(java.util.stream.Collectors.toSet()));
  }

  // ── Degenerate cases ─────────────────────────────────────────────

  @Test
  void sourceInTargetsGivesEmptyPlan() {
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var plan = resolver.planFindOffsets(GD_.key(), Set.of(GregorianSequences.GD));
    assertTrue(plan.isEmpty());
  }

  @Test
  void emptyTargetSetGivesEmptyPlan() {
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var plan = resolver.planFindOffsets(GD_.key(), Set.of());
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
    var resolver = new AggregationStepPlanner(List.of(gq_, gy4));

    assertThrows(
        IllegalArgumentException.class, () -> resolver.plan(gq_.key(), Set.of(gy4.key())));
  }

  @Test
  void unregisteredSourceThrows() {
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var unknown = new TimeSequenceKey('Z', 'D');
    assertThrows(IllegalArgumentException.class, () -> resolver.planFindOffsets(unknown, Set.of(GY)));
  }

  @Test
  void unregisteredTargetKeyThrows() {
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var unknown = new TimeSequenceKey('Z', 'Y');
    assertThrows(
        IllegalArgumentException.class, () -> resolver.plan(GD_.key(), Set.of(unknown)));
  }

  @Test
  void crossCalendarUnreachable() {
    var resolver =
        new AggregationStepPlanner(
            List.of(
                IslamicSequences.ID_, IslamicSequences.IM_, IslamicSequences.IY_, GD_, GM_, GY_));

    assertThrows(
        IllegalArgumentException.class,
        () -> resolver.planFindOffsets(IslamicSequences.ID_.key(), Set.of(GY)));
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

    var resolver =
        new AggregationStepPlanner(
            List.of(
                fakeSequence(st, Set.of(gm), Set.of(gm)),
                fakeSequence(gm, Set.of(gq), Set.of(gq, gy)),
                fakeSequence(gq, Set.of(gy), Set.of(gy)),
                fakeSequence(gy, Set.of(), Set.of())));

    var plan = resolver.planFindOffsets(st, Set.of(GY));

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

    var resolver =
        new AggregationStepPlanner(
            List.of(
                fakeSequence(st, Set.of(gm), Set.of(gm)),
                fakeSequence(gm, Set.of(gq, gy), Set.of(gq, gy)),
                fakeSequence(gq, Set.of(), Set.of()),
                fakeSequence(gy, Set.of(), Set.of())));

    var plan = resolver.planFindOffsets(st, Set.of(GQ, GY));

    assertEquals(3, plan.size());
    assertEquals(
        Set.of(
            new AggregationStep(st, gm, EXACT),
            new AggregationStep(gm, gq, EXACT),
            new AggregationStep(gm, gy, EXACT)),
        new HashSet<>(plan));
    assertEquals(new AggregationStep(st, gm, EXACT), plan.get(0));
  }

  @Test
  void sharedIntermediatePrecedesAllConsumers() {
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var plan = resolver.planFindOffsets(GD_.key(), Set.of(GM, GQ, GS));

    var gmIndex = plan.indexOf(new AggregationStep(GD_.key(), GM_.key(), EXACT));
    var gqIndex = plan.indexOf(new AggregationStep(GM_.key(), GQ_.key(), EXACT));
    var gsIndex = plan.indexOf(new AggregationStep(GM_.key(), GS_.key(), EXACT));

    assertTrue(gmIndex >= 0);
    assertTrue(gqIndex > gmIndex, () -> "GQ must be computed after GM; plan: " + plan);
    assertTrue(gsIndex > gmIndex, () -> "GS must be computed after GM; plan: " + plan);
  }

  // ── Invariants ───────────────────────────────────────────────────

  @Test
  void edgeTypeMatchesSequenceDeclaration() {
    var resolver = new AggregationStepPlanner(GREGORIAN);
    var plan = resolver.planFindOffsets(GW_.key(), Set.of(GM, GQ, GY), true);

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
