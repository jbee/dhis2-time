package org.hisp.dhis2.time;

/**
 * What is the exactness of the outcomes produced by the aggrgeation
 */
public enum AggregationExactness {
  /** Sum of source intervals equals the target interval — no boundary loss. */
  EXACT,
  /** Source intervals are attributed to the target by majority; sums approximate. */
  APPROXIMATE
}
