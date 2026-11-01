package org.hisp.dhis2.time;

/** Whether a step preserves the value exactly, or approximates it. */
public enum AggregationExactness {
  /** Sum of source intervals equals the target interval — no boundary loss. */
  EXACT,
  /** Source intervals are attributed to the target by majority; sums approximate. */
  APPROXIMATION
}
