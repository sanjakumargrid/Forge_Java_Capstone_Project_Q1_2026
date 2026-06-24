package com.talentgrid.demand.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Repairs {@code skills.embedding} when Hibernate created it as {@code real[]} instead of {@code vector(768)}.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class SkillEmbeddingSchemaMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        String columnType = jdbcTemplate.query("""
                        SELECT format_type(a.atttypid, a.atttypmod)
                        FROM pg_attribute a
                        JOIN pg_class c ON a.attrelid = c.oid
                        JOIN pg_namespace n ON c.relnamespace = n.oid
                        WHERE n.nspname = 'public'
                          AND c.relname = 'skills'
                          AND a.attname = 'embedding'
                          AND NOT a.attisdropped
                        """,
                rs -> rs.next() ? rs.getString(1) : null);

        if (columnType == null) {
            log.debug("skills.embedding column not present yet; Hibernate will create vector(768).");
            return;
        }

        if (columnType.startsWith("vector")) {
            log.debug("skills.embedding already uses pgvector type: {}", columnType);
            return;
        }

        log.warn("skills.embedding has incompatible type '{}'; migrating to vector(768)", columnType);
        jdbcTemplate.execute("DROP INDEX IF EXISTS idx_skills_embedding");
        jdbcTemplate.execute("ALTER TABLE skills DROP COLUMN embedding");
        jdbcTemplate.execute("ALTER TABLE skills ADD COLUMN embedding vector(768)");
        jdbcTemplate.execute("ALTER TABLE skills ADD COLUMN IF NOT EXISTS embedding_updated_at TIMESTAMP");
        jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_skills_embedding ON skills USING hnsw (embedding vector_cosine_ops)");
        log.info("skills.embedding migrated to vector(768). Re-run skill embedding backfill if needed.");
    }
}
