package org.hisp.dhis2.time;

import org.hisp.dhis2.time.AggregationSqlGenerator.AggregationExecution;
import org.hisp.dhis2.time.AggregationSqlGenerator.AggregationTable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.hisp.dhis2.time.AggregationExactness.APPROXIMATE;
import static org.hisp.dhis2.time.AggregationExactness.EXACT;
import static org.hisp.dhis2.time.AggregationFunction.AVERAGE;
import static org.hisp.dhis2.time.AggregationFunction.COUNT;
import static org.hisp.dhis2.time.AggregationFunction.MAX;
import static org.hisp.dhis2.time.AggregationFunction.MIN;
import static org.hisp.dhis2.time.AggregationFunction.STDDEV;
import static org.hisp.dhis2.time.AggregationFunction.SUM;
import static org.hisp.dhis2.time.AggregationFunction.SUM_SQ;
import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.hisp.dhis2.time.GregorianSequences.GD;
import static org.hisp.dhis2.time.GregorianSequences.GM;
import static org.hisp.dhis2.time.GregorianSequences.GM_;
import static org.hisp.dhis2.time.GregorianSequences.GQ;
import static org.hisp.dhis2.time.GregorianSequences.GQ_;
import static org.hisp.dhis2.time.GregorianSequences.GY;
import static org.hisp.dhis2.time.GregorianSequences.GY_;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AggregationSqlGeneratorTest {

  /**
   * Deterministic period ID: YYYYMMDD of the interval's first day.
   * Collisions across period types are fine for these tests — the assertion
   * checks the sequence of parameters, not their uniqueness.
   */
  private static final AggregationSqlGenerator.PeriodIdCache IDS = slice -> {
    var d = slice.interval().firstDay();
    return (long) d.year() * 10000
        + (d.monthOffset() + 1) * 100
        + (d.dayOfMonthOffset() + 1);
  };

  private static final AggregationSqlGenerator GENERATOR = new AggregationSqlGenerator(IDS);

  // ── DDL ────────────────────────────────────────────────────────────

  @Test
  void createTableForSingleSum() {
    var table = new AggregationTable("analytics_gm", Set.of(SUM));
    assertEquals(
        """
        CREATE TABLE IF NOT EXISTS analytics_gm (
          periodid   bigint NOT NULL,
          orgunitid  text   NOT NULL,
          exactness  text,
          PRIMARY KEY (periodid, orgunitid)
        );

        ALTER TABLE analytics_gm
          ADD COLUMN IF NOT EXISTS exactness text,
          ADD COLUMN IF NOT EXISTS value_sum numeric;
        """,
        GENERATOR.generateCreateTableSQL(table));
  }

  @Test
  void createTableForSumAndCount() {
    var table = new AggregationTable("analytics_gm", Set.of(SUM, COUNT));
    assertEquals(
        """
        CREATE TABLE IF NOT EXISTS analytics_gm (
          periodid   bigint NOT NULL,
          orgunitid  text   NOT NULL,
          exactness  text,
          PRIMARY KEY (periodid, orgunitid)
        );

        ALTER TABLE analytics_gm
          ADD COLUMN IF NOT EXISTS exactness text,
          ADD COLUMN IF NOT EXISTS value_sum numeric,
          ADD COLUMN IF NOT EXISTS value_count bigint;
        """,
        GENERATOR.generateCreateTableSQL(table));
  }

  @Test
  void createTableForMinAndMax() {
    var table = new AggregationTable("analytics_gm", Set.of(MIN, MAX));
    assertEquals(
        """
        CREATE TABLE IF NOT EXISTS analytics_gm (
          periodid   bigint NOT NULL,
          orgunitid  text   NOT NULL,
          exactness  text,
          PRIMARY KEY (periodid, orgunitid)
        );

        ALTER TABLE analytics_gm
          ADD COLUMN IF NOT EXISTS exactness text,
          ADD COLUMN IF NOT EXISTS value_min numeric,
          ADD COLUMN IF NOT EXISTS value_max numeric;
        """,
        GENERATOR.generateCreateTableSQL(table));
  }

  @Test
  void createTableForAverageExpandsDependencies() {
    // AVERAGE depends on SUM and COUNT — the DDL adds all three, with the
    // stored columns before the generated column.
    var table = new AggregationTable("analytics_gm", Set.of(AVERAGE));
    assertEquals(
        """
        CREATE TABLE IF NOT EXISTS analytics_gm (
          periodid   bigint NOT NULL,
          orgunitid  text   NOT NULL,
          exactness  text,
          PRIMARY KEY (periodid, orgunitid)
        );

        ALTER TABLE analytics_gm
          ADD COLUMN IF NOT EXISTS exactness text,
          ADD COLUMN IF NOT EXISTS value_sum numeric,
          ADD COLUMN IF NOT EXISTS value_count bigint,
          ADD COLUMN IF NOT EXISTS value_avg numeric GENERATED ALWAYS AS (CASE WHEN value_count = 0 THEN NULL ELSE value_sum / value_count END) STORED;
        """,
        GENERATOR.generateCreateTableSQL(table));
  }

  @Test
  void createTableForStddevExpandsDependencies() {
    // STDDEV depends on SUM, COUNT, SUM_SQ — three stored columns plus one
    // generated, with the stable-form population stddev expression.
    var table = new AggregationTable("analytics_gy", Set.of(STDDEV));
    assertEquals(
        """
        CREATE TABLE IF NOT EXISTS analytics_gy (
          periodid   bigint NOT NULL,
          orgunitid  text   NOT NULL,
          exactness  text,
          PRIMARY KEY (periodid, orgunitid)
        );

        ALTER TABLE analytics_gy
          ADD COLUMN IF NOT EXISTS exactness text,
          ADD COLUMN IF NOT EXISTS value_sum numeric,
          ADD COLUMN IF NOT EXISTS value_count bigint,
          ADD COLUMN IF NOT EXISTS value_sumsq numeric,
          ADD COLUMN IF NOT EXISTS value_stddev numeric GENERATED ALWAYS AS (CASE WHEN value_count = 0 THEN NULL ELSE sqrt(value_sumsq / value_count - (value_sum / value_count) * (value_sum / value_count)) END) STORED;
        """,
        GENERATOR.generateCreateTableSQL(table));
  }

  // ── DML: leaf source ───────────────────────────────────────────────

  @Test
  void leafSumAggregation() {
    var job = new AggregationJob(
        GD.noOffset(),
        GM.noOffset(),
        GM_.interval(ofYMD(2024, 1, 15)),
        List.of(
            new Epoch2kInterval(ofYMD(2024, 1, 15)),
            new Epoch2kInterval(ofYMD(2024, 1, 16)),
            new Epoch2kInterval(ofYMD(2024, 1, 17))),
        EXACT);
    var exec = new AggregationExecution(
        "datavalue",
        AggregationSqlGenerator.SourceKind.LEAF,
        new AggregationTable("analytics_gm", Set.of(SUM)));

    var sql = GENERATOR.generateAggregationSQL(job, exec);
    assertEquals(
        """
        INSERT INTO analytics_gm (periodid, orgunitid, exactness, value_sum)
        SELECT ?, orgunitid, ?, SUM(value)
        FROM datavalue
        WHERE periodid IN (?, ?, ?)
        GROUP BY orgunitid
        """,
        sql.sql());
    assertEquals(
        List.of(20240101L, "EXACT", 20240115L, 20240116L, 20240117L),
        sql.parameters());
  }

  @Test
  void leafAverageUsesSumAndCount() {
    var job = new AggregationJob(
        GD.noOffset(),
        GM.noOffset(),
        GM_.interval(ofYMD(2024, 3, 15)),
        List.of(new Epoch2kInterval(ofYMD(2024, 3, 1))),
        EXACT);
    var exec = new AggregationExecution(
        "datavalue",
        AggregationSqlGenerator.SourceKind.LEAF,
        new AggregationTable("analytics_gm", Set.of(AVERAGE)));

    var sql = GENERATOR.generateAggregationSQL(job, exec);
    assertEquals(
        """
        INSERT INTO analytics_gm (periodid, orgunitid, exactness, value_sum, value_count)
        SELECT ?, orgunitid, ?, SUM(value), COUNT(value)
        FROM datavalue
        WHERE periodid IN (?)
        GROUP BY orgunitid
        """,
        sql.sql());
    assertEquals(
        List.of(20240301L, "EXACT", 20240301L),
        sql.parameters());
  }

  @Test
  void leafSumSqSquaresTheSourceValue() {
    // SUM_SQ at leaf level must square the value; the transform happens only here.
    var job = new AggregationJob(
        GD.noOffset(),
        GM.noOffset(),
        GM_.interval(ofYMD(2024, 1, 15)),
        List.of(
            new Epoch2kInterval(ofYMD(2024, 1, 15)),
            new Epoch2kInterval(ofYMD(2024, 1, 16))),
        EXACT);
    var exec = new AggregationExecution(
        "datavalue",
        AggregationSqlGenerator.SourceKind.LEAF,
        new AggregationTable("analytics_gm", Set.of(SUM_SQ)));

    var sql = GENERATOR.generateAggregationSQL(job, exec);
    assertEquals(
        """
        INSERT INTO analytics_gm (periodid, orgunitid, exactness, value_sumsq)
        SELECT ?, orgunitid, ?, SUM(value * value)
        FROM datavalue
        WHERE periodid IN (?, ?)
        GROUP BY orgunitid
        """,
        sql.sql());
    assertEquals(
        List.of(20240101L, "EXACT", 20240115L, 20240116L),
        sql.parameters());
  }

  // ── DML: intermediate source ───────────────────────────────────────

  @Test
  void intermediateSumAggregation() {
    var job = new AggregationJob(
        GM.noOffset(),
        GQ.noOffset(),
        GQ_.interval(ofYMD(2024, 5, 15)),                       // Q2 2024
        List.of(
            GM_.interval(ofYMD(2024, 1, 15)),                    // Jan
            GM_.interval(ofYMD(2024, 2, 15)),                    // Feb
            GM_.interval(ofYMD(2024, 3, 15))),                   // Mar
        EXACT);
    var exec = new AggregationExecution(
        "analytics_gm",
        AggregationSqlGenerator.SourceKind.INTERMEDIATE,
        new AggregationTable("analytics_gq", Set.of(SUM)));

    var sql = GENERATOR.generateAggregationSQL(job, exec);
    assertEquals(
        """
        INSERT INTO analytics_gq (periodid, orgunitid, exactness, value_sum)
        SELECT ?, orgunitid, ?, SUM(value_sum)
        FROM analytics_gm
        WHERE periodid IN (?, ?, ?)
        GROUP BY orgunitid
        """,
        sql.sql());
    assertEquals(
        List.of(20240401L, "EXACT", 20240101L, 20240201L, 20240301L),
        sql.parameters());
  }

  @Test
  void intermediateAverageUsesChainedSumAndCount() {
    var job = new AggregationJob(
        GM.noOffset(),
        GQ.noOffset(),
        GQ_.interval(ofYMD(2024, 5, 15)),
        List.of(
            GM_.interval(ofYMD(2024, 1, 15)),
            GM_.interval(ofYMD(2024, 2, 15)),
            GM_.interval(ofYMD(2024, 3, 15))),
        EXACT);
    var exec = new AggregationExecution(
        "analytics_gm",
        AggregationSqlGenerator.SourceKind.INTERMEDIATE,
        new AggregationTable("analytics_gq", Set.of(AVERAGE)));

    var sql = GENERATOR.generateAggregationSQL(job, exec);
    assertEquals(
        """
        INSERT INTO analytics_gq (periodid, orgunitid, exactness, value_sum, value_count)
        SELECT ?, orgunitid, ?, SUM(value_sum), SUM(value_count)
        FROM analytics_gm
        WHERE periodid IN (?, ?, ?)
        GROUP BY orgunitid
        """,
        sql.sql());
    assertEquals(
        List.of(20240401L, "EXACT", 20240101L, 20240201L, 20240301L),
        sql.parameters());
  }

  @Test
  void intermediateSumSqUsesStoredColumnWithoutTransform() {
    // The stored value_sumsq is already the sum of squares; chaining is a plain SUM.
    var job = new AggregationJob(
        GM.noOffset(),
        GQ.noOffset(),
        GQ_.interval(ofYMD(2024, 5, 15)),
        List.of(
            GM_.interval(ofYMD(2024, 1, 15)),
            GM_.interval(ofYMD(2024, 2, 15))),
        EXACT);
    var exec = new AggregationExecution(
        "analytics_gm",
        AggregationSqlGenerator.SourceKind.INTERMEDIATE,
        new AggregationTable("analytics_gq", Set.of(SUM_SQ)));

    var sql = GENERATOR.generateAggregationSQL(job, exec);
    assertEquals(
        """
        INSERT INTO analytics_gq (periodid, orgunitid, exactness, value_sumsq)
        SELECT ?, orgunitid, ?, SUM(value_sumsq)
        FROM analytics_gm
        WHERE periodid IN (?, ?)
        GROUP BY orgunitid
        """,
        sql.sql());
    assertEquals(
        List.of(20240401L, "EXACT", 20240101L, 20240201L),
        sql.parameters());
  }

  // ── Exactness handling ─────────────────────────────────────────────

  @Test
  void approximateExactnessWrittenToInsert() {
    var job = new AggregationJob(
        GD.noOffset(),
        GY.noOffset(),
        GY_.interval(ofYMD(2024, 6, 15)),
        List.of(new Epoch2kInterval(ofYMD(2024, 1, 1))),
        APPROXIMATE);
    var exec = new AggregationExecution(
        "datavalue",
        AggregationSqlGenerator.SourceKind.LEAF,
        new AggregationTable("analytics_gy", Set.of(SUM)));

    var sql = GENERATOR.generateAggregationSQL(job, exec);
    assertEquals(
        """
        INSERT INTO analytics_gy (periodid, orgunitid, exactness, value_sum)
        SELECT ?, orgunitid, ?, SUM(value)
        FROM datavalue
        WHERE periodid IN (?)
        GROUP BY orgunitid
        """,
        sql.sql());
    assertEquals(
        List.of(20240101L, "APPROXIMATE", 20240101L),
        sql.parameters());
  }

  // ── Parameter ordering ─────────────────────────────────────────────

  @Test
  void parametersFollowPlaceholderOrderInSql() {
    // Target period ID, then exactness, then source period IDs in order —
    // matching the left-to-right order of ? placeholders in the SQL string.
    var job = new AggregationJob(
        GM.noOffset(),
        GY.noOffset(),
        GY_.interval(ofYMD(2024, 6, 15)),
        List.of(
            GM_.interval(ofYMD(2024, 1, 15)),
            GM_.interval(ofYMD(2024, 2, 15)),
            GM_.interval(ofYMD(2024, 3, 15)),
            GM_.interval(ofYMD(2024, 4, 15))),
        EXACT);
    var exec = new AggregationExecution(
        "analytics_gm",
        AggregationSqlGenerator.SourceKind.INTERMEDIATE,
        new AggregationTable("analytics_gy", Set.of(COUNT)));

    var sql = GENERATOR.generateAggregationSQL(job, exec);

    // First placeholder = target period ID
    assertEquals(20240101L, sql.parameters().get(0));
    // Second placeholder = exactness
    assertEquals("EXACT", sql.parameters().get(1));
    // Remaining placeholders = source period IDs in the same order as sources()
    assertEquals(20240101L, sql.parameters().get(2));
    assertEquals(20240201L, sql.parameters().get(3));
    assertEquals(20240301L, sql.parameters().get(4));
    assertEquals(20240401L, sql.parameters().get(5));
    // Total: 2 fixed + 4 source = 6 parameters
    assertEquals(6, sql.parameters().size());
  }
}