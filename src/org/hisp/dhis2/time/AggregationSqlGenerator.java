package org.hisp.dhis2.time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Emits SQL for the two DDL and DML operations the executor needs:
 *
 * <ul>
 *   <li>{@link #generateCreateTableSQL(AggregationTable)} — additive schema
 *       management for a target analytics table. The base table and
 *       {@code exactness} column are created once; function columns are added
 *       idempotently so a table can grow without being rebuilt.</li>
 *   <li>{@link #generateAggregationSQL(AggregationJob, AggregationExecution)} —
 *       one {@code INSERT ... SELECT} per job, aggregating the stored columns
 *       of the source table over the job's source intervals.</li>
 * </ul>
 *
 * <h2>Exactness</h2>
 *
 * <p>The period table's identity includes exactness
 * ({@code (system, interval, offset, exactness)}), so each period ID already
 * distinguishes exact from approximate. The analytics table additionally
 * stores an {@code exactness} column — a denormalization for query convenience,
 * so consumers can filter on exactness without joining to the period table.
 *
 * <p>The value written is {@code job.exactness()}, the last edge's attribution
 * type. If a deployment ever needs to tag a value with the accumulated
 * exactness of its chain (e.g. a step that is EXACT but inherits APPROXIMATE
 * from upstream), that accumulation is the executor's responsibility — the
 * generator writes whatever exactness the job carries.
 *
 * <h2>Nullability</h2>
 *
 * <p>Stored function columns are nullable. A populated table cannot accept a
 * {@code NOT NULL} column without values for existing rows, and analytics data
 * is naturally sparse — NULL represents "no data for this period and org
 * unit", a state the analytics consumer already has to handle.
 */
public final class AggregationSqlGenerator {

  /**
   * Resolves a {@link TimeSlice} to the numeric primary key of the period row
   * in the period table. Because the period table's PK includes exactness,
   * the same calendar interval resolves to different IDs for exact and
   * approximate variants.
   *
   * <p>Implementations are expected to be backed by a cache or a preloaded
   * {@code Map<TimeSlice, Long>}; a per-call database lookup for every source
   * and target period is too expensive for a bulk aggregation run.
   */
  @FunctionalInterface
  public interface PeriodIdCache {
    long get(TimeSlice slice);
  }

  /** Whether the source table is raw data or a previous analytics table. */
  public enum SourceKind { LEAF, INTERMEDIATE }

  /** SQL with bound-parameter placeholders and the ordered parameter list. */
  public record SqlWithParams(String sql, List<Object> parameters) {}

  /**
   * One materialised analytics table and its target function set. The
   * function set is expanded to its dependency closure in the compact
   * constructor, so callers pass the requested target set and the record
   * stores the full expansion.
   */
  public record AggregationTable(String name, Set<AggregationFunction> functions) {
    public AggregationTable {
      if (name == null || name.isEmpty())
        throw new IllegalArgumentException("table name must not be empty");
      if (functions == null || functions.isEmpty())
        throw new IllegalArgumentException("function set must not be empty for " + name);
      functions = AggregationFunction.withDependencies(functions);
    }
  }

  /**
   * The execution context for one plan step: where to read from, how to read
   * it, and which target table and function set to write to. Fixed per step
   * across all its jobs.
   */
  public record AggregationExecution(
      String sourceTable,
      SourceKind sourceKind,
      AggregationTable target) {
    public AggregationExecution {
      if (sourceTable == null || sourceTable.isEmpty())
        throw new IllegalArgumentException("source table must not be empty");
      if (sourceKind == null)
        throw new IllegalArgumentException("source kind must not be null");
      if (target == null)
        throw new IllegalArgumentException("target must not be null");
    }
  }

  private final PeriodIdCache periodIds;

  public AggregationSqlGenerator(PeriodIdCache periodIds) {
    if (periodIds == null)
      throw new IllegalArgumentException("periodId cache must not be null");
    this.periodIds = periodIds;
  }

  // ── DDL ────────────────────────────────────────────────────────────

  /**
   * Additive DDL for the target analytics table: a {@code CREATE TABLE IF NOT
   * EXISTS} for the base schema, followed by an {@code ALTER TABLE ... ADD
   * COLUMN IF NOT EXISTS} for every function column.
   *
   * <p>Idempotent. On a fresh database the CREATE TABLE makes the base schema
   * and the ALTERs add all function columns; on a populated table the ALTERs
   * add only columns not yet present, so different
   * {@link AggregationExecution}s can contribute different function sets to
   * the same table over time without rebuilding it.
   *
   * <p>Generated columns for {@link AggregationClass#DERIVED} functions use
   * {@code STORED}, which pays storage in exchange for fast reads — the right
   * trade-off for values dashboards hit repeatedly.
   */
  public String generateCreateTableSQL(AggregationTable table) {
    var additions = new ArrayList<String>();
    // Defensive: no-op for fresh tables, adds the column for tables created
    // before exactness became part of the base schema.
    additions.add("ADD COLUMN IF NOT EXISTS exactness text");
    for (var fn : table.functions()) {
      additions.add("ADD COLUMN IF NOT EXISTS " + columnDefinition(fn));
    }

    return """
        CREATE TABLE IF NOT EXISTS %s (
          periodid   bigint NOT NULL,
          orgunitid  text   NOT NULL,
          exactness  text,
          PRIMARY KEY (periodid, orgunitid)
        );

        ALTER TABLE %s
          %s;
        """.formatted(
        table.name(),
        table.name(),
        String.join(",\n  ", additions));
  }

  // ── DML ────────────────────────────────────────────────────────────

  /**
   * {@code INSERT ... SELECT} SQL for one {@link AggregationJob}: aggregate
   * the stored columns of the source table over the job's source intervals,
   * grouping by org unit, into the target table under the job's target period
   * and exactness.
   *
   * <p>Generated columns are not written by the INSERT — the database
   * recomputes them from the stored columns the INSERT populates.
   */
  public SqlWithParams generateAggregationSQL(AggregationJob job, AggregationExecution exec) {
    var storedFunctions = storedFunctions(exec.target().functions());
    if (storedFunctions.isEmpty())
      throw new IllegalStateException(
          "target " + exec.target().name() + " has no stored functions — "
              + "cannot aggregate into an all-generated table");

    var insertColumns = new ArrayList<String>();
    var selectExprs = new ArrayList<String>();
    var params = new ArrayList<Object>();

    // Fixed columns: period id, org unit, exactness
    insertColumns.add("periodid");
    insertColumns.add("orgunitid");
    insertColumns.add("exactness");
    selectExprs.add("?");
    selectExprs.add("orgunitid");
    selectExprs.add("?");
    params.add(periodIds.get(new TimeSlice(job.target(), job.targetInterval())));
    params.add(job.exactness().name());

    // Stored function columns
    for (var fn : storedFunctions) {
      insertColumns.add(columnName(fn));
      selectExprs.add(
          exec.sourceKind() == SourceKind.LEAF
              ? leafExpression(fn)
              : chainedExpression(fn));
    }

    // Source period ids, in the same order as job.sourceIntervals()
    var sourcePeriodIds = new ArrayList<Long>();
    for (var si : job.sources()) {
      sourcePeriodIds.add(periodIds.get(new TimeSlice(job.source(), si)));
    }
    params.addAll(sourcePeriodIds);

    var inPlaceholders = String.join(", ",
        Collections.nCopies(sourcePeriodIds.size(), "?"));

    var sql = """
        INSERT INTO %s (%s)
        SELECT %s
        FROM %s
        WHERE periodid IN (%s)
        GROUP BY orgunitid
        """.formatted(
        exec.target().name(),
        String.join(", ", insertColumns),
        String.join(", ", selectExprs),
        exec.sourceTable(),
        inPlaceholders);

    return new SqlWithParams(sql, List.copyOf(params));
  }

  // ── Column definitions ─────────────────────────────────────────────

  private static String columnDefinition(AggregationFunction fn) {
    return switch (fn.aggregationClass()) {
      case ASSOCIATIVE ->
          columnName(fn) + " " + columnType(fn);
      case DERIVED ->
          columnName(fn) + " numeric GENERATED ALWAYS AS ("
              + generatedExpression(fn) + ") STORED";
      default -> throw new UnsupportedOperationException(
          "cannot add column for " + fn + " (" + fn.aggregationClass() + ")");
    };
  }

  private static Set<AggregationFunction> storedFunctions(Set<AggregationFunction> functions) {
    var result = new LinkedHashSet<AggregationFunction>();
    for (var fn : functions) {
      if (fn.aggregationClass() == AggregationClass.ASSOCIATIVE) result.add(fn);
    }
    return result;
  }

  private static String columnName(AggregationFunction fn) {
    return switch (fn) {
      case SUM -> "value_sum";
      case COUNT -> "value_count";
      case MIN -> "value_min";
      case MAX -> "value_max";
      case SUM_SQ -> "value_sumsq";
      case AVERAGE, AVERAGE_SUM_ORG_UNIT -> "value_avg";
      case STDDEV -> "value_stddev";
      case VARIANCE -> "value_var";
      default -> throw new UnsupportedOperationException(
          "no column for " + fn + " (" + fn.aggregationClass() + ")");
    };
  }

  private static String columnType(AggregationFunction fn) {
    return switch (fn) {
      case COUNT -> "bigint";
      default -> "numeric";
    };
  }

  /** SQL expression to compute the column from raw source data. */
  private static String leafExpression(AggregationFunction fn) {
    return switch (fn) {
      case SUM -> "SUM(value)";
      case COUNT -> "COUNT(value)";
      case MIN -> "MIN(value)";
      case MAX -> "MAX(value)";
      case SUM_SQ -> "SUM(value * value)";
      default -> throw new UnsupportedOperationException(
          "no leaf expression for " + fn);
    };
  }

  /** SQL expression to compute the column from a previous analytics table. */
  private static String chainedExpression(AggregationFunction fn) {
    return switch (fn) {
      case SUM -> "SUM(value_sum)";
      case COUNT -> "SUM(value_count)";
      case MIN -> "MIN(value_min)";
      case MAX -> "MAX(value_max)";
      case SUM_SQ -> "SUM(value_sumsq)";
      default -> throw new UnsupportedOperationException(
          "no chained expression for " + fn);
    };
  }

  /**
   * SQL expression for a generated column, computed from the stored columns
   * of the same row. Uses numerically stable forms of the STDDEV and VARIANCE
   * formulas — computing {@code E[x²] − E[x]²} term-by-term rather than as a
   * difference of two large numbers, which loses precision catastrophically
   * for large means with small variance.
   */
  private static String generatedExpression(AggregationFunction fn) {
    return switch (fn) {
      case AVERAGE, AVERAGE_SUM_ORG_UNIT ->
          "CASE WHEN value_count = 0 THEN NULL"
              + " ELSE value_sum / value_count END";
      case STDDEV ->
          "CASE WHEN value_count = 0 THEN NULL"
              + " ELSE sqrt(value_sumsq / value_count"
              + " - (value_sum / value_count) * (value_sum / value_count)) END";
      case VARIANCE ->
          "CASE WHEN value_count = 0 THEN NULL"
              + " ELSE value_sumsq / value_count"
              + " - (value_sum / value_count) * (value_sum / value_count) END";
      default -> throw new UnsupportedOperationException(
          "no generated expression for " + fn + " (" + fn.aggregationClass() + ")");
    };
  }
}