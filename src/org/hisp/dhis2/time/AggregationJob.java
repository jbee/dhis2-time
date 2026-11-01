package org.hisp.dhis2.time;

import java.util.List;

/**
 * One concrete unit of work produced by {@link AggregationJobPlanner}: aggregate
 * {@code sourceIntervals} (all of sequence {@code source}) into
 * {@code targetInterval} (of sequence {@code target}), under {@code type}'s
 * attribution rule.
 */
public record AggregationJob(
    TimeSequenceKey source,
    TimeSequenceKey target,
    Epoch2kInterval targetInterval,
    List<Epoch2kInterval> sourceIntervals,
    AggregationExactness exactness) {}