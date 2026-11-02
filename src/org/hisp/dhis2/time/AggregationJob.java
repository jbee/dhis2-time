package org.hisp.dhis2.time;

import java.util.List;

/**
 * One concrete unit of work produced by {@link AggregationJobPlanner}: aggregate
 * {@code sources} (all of sequence {@code source}) into
 * {@code targetInterval} (of sequence {@code target}), under {@code exactness}'s
 * attribution rule.
 */
public record AggregationJob(
    TimeSequenceTag source,
    TimeSequenceTag target,
    Epoch2kInterval targetInterval,
    List<Epoch2kInterval> sources,
    AggregationExactness exactness) {}