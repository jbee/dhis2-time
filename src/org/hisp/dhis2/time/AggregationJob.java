package org.hisp.dhis2.time;

import java.util.List;

/**
 * One concrete unit of work produced by {@link AggregationJobPlanner}: aggregate
 * {@code sources} (all of sequence {@code sourceKey}) into
 * {@code target} (of sequence {@code targetKey}), under {@code exactness}'s
 * attribution rule.
 */
public record AggregationJob(
    TimeSequenceKey sourceKey,
    TimeSequenceKey targetKey,
    Epoch2kInterval target,
    List<Epoch2kInterval> sources,
    AggregationExactness exactness) {}