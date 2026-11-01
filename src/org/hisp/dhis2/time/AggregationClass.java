package org.hisp.dhis2.time;

/**
 * How the executor chains an {@link AggregationFunction} across levels of
 * aggregation.
 *
 * <p>The plan produced by {@link AggregationStepPlanner} is aggregate-function
 * agnostic: it plans period-to-period steps. The executor maps each step onto a
 * concrete SQL aggregation based on the functions in the target set. This
 * classification tells the executor what shape that SQL takes.
 */
public enum AggregationClass {

  /**
   * Associative — chains by aggregating the already-materialised column at each
   * level. {@code SUM of SUMs = SUM}, {@code COUNT of COUNTs = COUNT},
   * {@code MIN of MINs = MIN}, {@code MAX of MAXs = MAX},
   * {@code SUM_SQ of SUM_SQs = SUM_SQ}.
   *
   * <p>Requires only the value itself in the row. The leaf computation (raw
   * source → first materialised level) may apply a per-value transform (e.g.
   * {@code SUM_SQ} squares each value before summing); higher levels aggregate
   * the stored column without a transform.
   */
  ASSOCIATIVE,

  /**
   * Not chainable — must be computed from source at each level. The value in a
   * coarser period is not a function of the finer-level values: {@code FIRST
   * of FIRSTs} is the first value of the first sub-period, which is not
   * generally the first value of the whole period. Same for {@code LAST} and
   * {@code LAST_IN_PERIOD}.
   *
   * <p>Currently deferred — the executor does not yet support computing flat
   * aggregations. Requesting one should fail with a clear "not yet supported"
   * rather than silently produce wrong values.
   */
  FLAT,

  /**
   * Not stored. Computed as a row-local generated-column expression from the
   * {@link #ASSOCIATIVE} columns listed in
   * {@link AggregationFunction#dependsOn()}. The generated value is correct
   * because the dependency columns are chained exactly and the derivation is a
   * pure function of those totals.
   *
   * <p>Examples: {@code AVERAGE = sum / count},
   * {@code VARIANCE = (sum_sq − sum²/count) / (count − 1)}.
   */
  DERIVED,

  /**
   * Reserved — requires full-value access (median, percentile). Not supported
   * by the algebraic-executor design; these functions cannot be expressed as
   * fixed-size sufficient statistics.
   */
  FULL_VALUE
}