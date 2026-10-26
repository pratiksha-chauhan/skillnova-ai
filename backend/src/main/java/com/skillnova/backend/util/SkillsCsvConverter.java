package com.skillnova.backend.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Converts between the comma-separated {@code current_skills} TEXT column
 * on {@code users} and the {@code List<String>} shape the profile API
 * exposes to clients. Kept as a small static utility (rather than a JPA
 * {@code AttributeConverter}) so it stays usable from both the entity layer
 * and ad-hoc call sites (e.g. building AI prompts in Phase 8) without an
 * EntityManager in play.
 *
 * Skills are trimmed and empty entries dropped on both read and write, so
 * round-tripping never produces blank/whitespace-only entries.
 */
public final class SkillsCsvConverter {

    private static final String DELIMITER = ",";

    private SkillsCsvConverter() {
    }

    /**
     * @param csv raw {@code current_skills} column value; may be {@code null}
     * @return immutable list of trimmed, non-blank skills (empty if csv is
     *         null/blank)
     */
    public static List<String> toList(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }
        List<String> skills = new ArrayList<>();
        for (String skill : csv.split(DELIMITER)) {
            String trimmed = skill.trim();
            if (!trimmed.isEmpty()) {
                skills.add(trimmed);
            }
        }
        return Collections.unmodifiableList(skills);
    }

    /**
     * @param skills list of skills; may be {@code null}
     * @return comma-joined string of trimmed, non-blank skills, or
     *         {@code null} if the result would be empty (so the column
     *         stores NULL rather than an empty string)
     */
    public static String toCsv(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return null;
        }
        List<String> cleaned = skills.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .toList();
        return cleaned.isEmpty() ? null : String.join(DELIMITER, cleaned);
    }
}
