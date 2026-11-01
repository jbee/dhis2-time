package org.hisp.dhis2.time;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.hisp.dhis2.time.GregorianSequences.GB_;
import static org.hisp.dhis2.time.GregorianSequences.GD_;
import static org.hisp.dhis2.time.GregorianSequences.GF_;
import static org.hisp.dhis2.time.GregorianSequences.GM;
import static org.hisp.dhis2.time.GregorianSequences.GM_;
import static org.hisp.dhis2.time.GregorianSequences.GQ;
import static org.hisp.dhis2.time.GregorianSequences.GQ_;
import static org.hisp.dhis2.time.GregorianSequences.GS_;
import static org.hisp.dhis2.time.GregorianSequences.GW_;
import static org.hisp.dhis2.time.GregorianSequences.GY;
import static org.hisp.dhis2.time.GregorianSequences.GY_;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AggregationJobPlannerTest {

  /** All eight Gregorian sequences, keyed by their default-offset variants. */
  private static final List<TimeSequence> GREGORIAN =
      List.of(GD_, GW_, GF_, GM_, GB_, GS_, GQ_, GY_);

  @Test
  void expandsSingleEdge() {
    var stepPlanner = new AggregationStepPlanner(GREGORIAN);
    var jobPlanner = new AggregationJobPlanner(GREGORIAN);
    var plan = stepPlanner.planFindOffsets(GD_.key(), Set.of(GM));
    var jobs = jobPlanner.plan(GD_.key(), Epoch2kInterval.ofYear(2024), plan);

    assertEquals(12, jobs.size());
    for (var job : jobs) {
      assertEquals(AggregationExactness.EXACT, job.exactness());
      assertEquals(GD_.key(), job.sourceKey());
      assertEquals(GM_.key(), job.targetKey());
    }
    // First job is January: 31 daily intervals
    assertEquals(31, jobs.get(0).sources().size());
    // Second job is February: 29 (leap)
    assertEquals(29, jobs.get(1).sources().size());
  }

  @Test
  void expandsChainedPlan() {
    var stepPlanner = new AggregationStepPlanner(GREGORIAN);
    var jobPlanner = new AggregationJobPlanner(GREGORIAN);
    var plan = stepPlanner.planFindOffsets(GD_.key(), Set.of(GM, GQ, GY));
    var jobs = jobPlanner.plan(GD_.key(), Epoch2kInterval.ofYear(2024), plan);

    assertEquals(17, jobs.size());  // 12 + 4 + 1
    var yearJob = jobs.stream().filter(j -> j.targetKey().equals(GY_.key())).findFirst().orElseThrow();
    assertEquals(4, yearJob.sources().size());
    assertEquals(AggregationExactness.EXACT, yearJob.exactness());
  }

  @Test
  void expansionRespectsApproximationAttribution() {
    var stepPlanner = new AggregationStepPlanner(GREGORIAN);
    var jobPlanner = new AggregationJobPlanner(GREGORIAN);
    var plan = stepPlanner.planFindOffsets(GW_.key(), Set.of(GY));
    var jobs = jobPlanner.plan(GW_.key(), Epoch2kInterval.ofYear(2024), plan);

    // Frame 2024 produces exactly one job: year 2024. The boundary week
    // starting 2024-12-30 is majority-in-2025 and belongs to frame 2025's
    // expansion; it is excluded from this one by the frame bound.
    assertEquals(1, jobs.size());

    var job = jobs.get(0);
    assertEquals(GY_.key(), job.targetKey());
    assertEquals(Epoch2kInterval.ofYear(2024), job.target());
    assertEquals(AggregationExactness.APPROXIMATE, job.exactness());

    // 52 weeks are majority-in-2024
    assertEquals(52, job.sources().size());

    // Every attributed week is majority-in the target interval
    for (var week : job.sources()) {
      assertTrue(
          week.overlapInDays(job.target()) * 2 > week.lengthInDays(),
          () -> week + " not majority in " + job.target());
    }

    // The excluded boundary week is not among the source intervals
    var boundaryWeekStart = ofYMD(2024, 12, 30);
    assertTrue(
        job.sources().stream()
            .noneMatch(w -> w.firstDay().equals(boundaryWeekStart)),
        "boundary week should belong to frame 2025's expansion, not 2024's");
  }

  @Test
  void adjacentFramesDoNotDuplicateBoundaryWeek() {
    var stepPlanner = new AggregationStepPlanner(GREGORIAN);
    var jobPlanner = new AggregationJobPlanner(GREGORIAN);
    var plan = stepPlanner.planFindOffsets(GW_.key(), Set.of(GY));

    var year2024 = jobPlanner.plan(GW_.key(), Epoch2kInterval.ofYear(2024), plan);
    var year2025 = jobPlanner.plan(GW_.key(), Epoch2kInterval.ofYear(2025), plan);

    // Count how many times each week is attributed to a target across both expansions
    var attributions = new java.util.HashMap<Epoch2kInterval, Integer>();
    for (var job : java.util.stream.Stream.concat(year2024.stream(), year2025.stream()).toList()) {
      for (var week : job.sources()) {
        attributions.merge(week, 1, Integer::sum);
      }
    }

    // Every week appears exactly once across all jobs
    for (var e : attributions.entrySet()) {
      assertEquals(1, e.getValue(),
          () -> "week " + e.getKey() + " attributed " + e.getValue() + " times");
    }
  }
}
