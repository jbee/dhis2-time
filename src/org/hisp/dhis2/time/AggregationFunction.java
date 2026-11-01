package org.hisp.dhis2.time;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Aggregation functions DHIS2 supports at period level, classified for the
 * plan-based executor.
 *
 * <p>For a target set {@code T}, the executor materialises
 * {@code withDependencies(T)} as physical columns (for
 * {@link AggregationClass#ASSOCIATIVE} members) and generated-column
 * expressions (for {@link AggregationClass#DERIVED} members). The
 * <b>stored</b> columns are exactly the {@code ASSOCIATIVE} functions in the
 * closure; the {@code DERIVED} functions become SQL
 * {@code GENERATED ALWAYS AS (...)} expressions over those columns.
 *
 * <h2>Example</h2>
 *
 * <p>Target set {@code {AVERAGE, STDDEV}} expands to
 * {@code {AVERAGE, STDDEV, SUM, COUNT, SUM_SQ}}. The stored columns are
 * {@code value_sum}, {@code value_count}, {@code value_sumsq}. The generated
 * columns are {@code value_avg} and {@code value_stddev}, computed from the
 * stored columns by row-local SQL expressions.
 *
 * <h2>Out of scope</h2>
 *
 * <p>{@code NONE}, {@code CUSTOM}, and {@code DEFAULT} are not modelled here —
 * they are data-element configuration concerns resolved before the executor
 * sees the function set. {@code MEDIAN} and {@code PERCENTILE} are not
 * modelled because they require {@link AggregationClass#FULL_VALUE}, which the
 * current design does not support.
 *
 * <p>{@link #SUM_SQ} is a <b>synthetic</b> function: not user-selectable, but
 * added as a dependency of {@link #STDDEV} and {@link #VARIANCE} whose
 * algebraic forms require the sum of squared source values. Users cannot
 * request it directly; the closure expansion introduces it when needed.
 */
public enum AggregationFunction {

  // ── Associative first-order ────────────────────────────────────────

  SUM(AggregationClass.ASSOCIATIVE),
  COUNT(AggregationClass.ASSOCIATIVE),
  MIN(AggregationClass.ASSOCIATIVE),
  MAX(AggregationClass.ASSOCIATIVE),

  /** Synthetic: SUM of squared values. Dependency of STDDEV and VARIANCE. */
  SUM_SQ(AggregationClass.ASSOCIATIVE),

  // ── Derived second-order ───────────────────────────────────────────

  AVERAGE(AggregationClass.DERIVED, Set.of(SUM, COUNT)),

  /**
   * AVERAGE on period axis, SUM on org-unit axis. The period axis is what the
   * plan chains, and on that axis this is identical to {@link #AVERAGE} — same
   * dependencies, same generated expression. The org-unit difference is
   * resolved by the executor's org-unit aggregation step, not by this enum.
   */
  AVERAGE_SUM_ORG_UNIT(AggregationClass.DERIVED, Set.of(SUM, COUNT)),

  /**
   * Population standard deviation (the form DHIS2 documents for its
   * {@code AggregationType}). Sample standard deviation, which would divide by
   * {@code count − 1} instead of {@code count}, is not a DHIS2
   * {@code AggregationType} and is not modelled here.
   */
  STDDEV(AggregationClass.DERIVED, Set.of(SUM, COUNT, SUM_SQ)),

  VARIANCE(AggregationClass.DERIVED, Set.of(SUM, COUNT, SUM_SQ)),

  // ── Flat ───────────────────────────────────────────────────────────

  FIRST(AggregationClass.FLAT),
  LAST(AggregationClass.FLAT),
  FIRST_AVERAGE_ORG_UNIT(AggregationClass.FLAT),
  FIRST_FIRST_ORG_UNIT(AggregationClass.FLAT),
  LAST_AVERAGE_ORG_UNIT(AggregationClass.FLAT),
  LAST_LAST_ORG_UNIT(AggregationClass.FLAT),
  LAST_IN_PERIOD(AggregationClass.FLAT),
  LAST_IN_PERIOD_AVERAGE_ORG_UNIT(AggregationClass.FLAT);

  private final AggregationClass aggregationClass;
  private final Set<AggregationFunction> dependsOn;

  AggregationFunction(AggregationClass aggregationClass) {
    this(aggregationClass, Set.of());
  }

  AggregationFunction(
      AggregationClass aggregationClass, Set<AggregationFunction> dependsOn) {
    this.aggregationClass = aggregationClass;
    this.dependsOn = dependsOn;
  }

  public AggregationClass aggregationClass() {
    return aggregationClass;
  }

  /**
   * The first-order functions this function requires as materialised columns.
   * Empty for {@link AggregationClass#ASSOCIATIVE} and {@link AggregationClass#FLAT};
   * non-empty for {@link AggregationClass#DERIVED}. The dependency graph is
   * acyclic (DERIVED functions never depend on other DERIVED functions).
   */
  public Set<AggregationFunction> dependsOn() {
    return dependsOn;
  }

  /**
   * The given target set plus the transitive closure of its dependencies.
   * The result contains, for each requested function, every
   * {@link AggregationClass#ASSOCIATIVE} column that must be materialised and
   * every intermediate {@link AggregationClass#DERIVED} function that must be
   * added as a generated column.
   *
   * <p>Example: {@code withDependencies(Set.of(AVERAGE, STDDEV))} returns
   * {@code {AVERAGE, STDDEV, SUM, COUNT, SUM_SQ}}.
   */
  public static Set<AggregationFunction> withDependencies(
      Set<AggregationFunction> targets) {
    var expanded = new HashSet<AggregationFunction>();
    var queue = new ArrayDeque<>(targets);
    while (!queue.isEmpty()) {
      var f = queue.poll();
      if (expanded.add(f)) queue.addAll(f.dependsOn());
    }
    return Set.copyOf(expanded);
  }
}