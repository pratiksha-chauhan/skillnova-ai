package com.skillnova.backend.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsCsvConverterTest {

    @Test
    void toListReturnsEmptyForNull() {
        assertThat(SkillsCsvConverter.toList(null)).isEmpty();
    }

    @Test
    void toListReturnsEmptyForBlank() {
        assertThat(SkillsCsvConverter.toList("   ")).isEmpty();
    }

    @Test
    void toListSplitsTrimsAndDropsBlankEntries() {
        List<String> skills = SkillsCsvConverter.toList("Java, Spring Boot ,,  , PostgreSQL");
        assertThat(skills).containsExactly("Java", "Spring Boot", "PostgreSQL");
    }

    @Test
    void toCsvReturnsNullForNullOrEmptyList() {
        assertThat(SkillsCsvConverter.toCsv(null)).isNull();
        assertThat(SkillsCsvConverter.toCsv(List.of())).isNull();
    }

    @Test
    void toCsvReturnsNullWhenAllEntriesAreBlank() {
        assertThat(SkillsCsvConverter.toCsv(List.of("  ", ""))).isNull();
    }

    @Test
    void toCsvJoinsTrimmedNonBlankEntries() {
        String csv = SkillsCsvConverter.toCsv(List.of(" Java ", "Spring Boot", "  "));
        assertThat(csv).isEqualTo("Java,Spring Boot");
    }

    @Test
    void roundTripsThroughCsvAndBackToList() {
        List<String> original = List.of("Java", "Spring Boot", "PostgreSQL");
        String csv = SkillsCsvConverter.toCsv(original);
        List<String> roundTripped = SkillsCsvConverter.toList(csv);
        assertThat(roundTripped).isEqualTo(original);
    }
}
