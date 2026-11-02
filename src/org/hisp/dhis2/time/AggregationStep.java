package org.hisp.dhis2.time;

/**
 * One step in an aggregation plan: compute {@code target} from {@code source}. Both endpoints are
 * concrete keys, so the caller can look up the sequences and execute the step.
 */
public record AggregationStep(
    TimeSequenceTag source, TimeSequenceTag target, AggregationExactness exactness) {}
