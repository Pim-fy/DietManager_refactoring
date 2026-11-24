package com.hwanghj.dietmanager.refactoring.backend.global.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BackupFilesTest {
    @TempDir
    Path temporary;

    @Test
    void syntheticBackupRoundTripsAndRejectsTampering() throws Exception {
        var backup = new BackupModel.Backup(1, List.of());
        Path file = BackupFiles.write(backup);
        Path checksum = Path.of(file + ".sha256");
        try {
            assertThat(BackupFiles.read(file)).isEqualTo(backup);
            Files.writeString(file, "{}");
            assertThatThrownBy(() -> BackupFiles.read(file)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("체크섬");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(checksum);
        }
    }

    @Test
    void outsideFileIsRejectedBeforeReadingContent() throws Exception {
        Path file = temporary.resolve("mysql-123.json");
        Files.writeString(file, "synthetic");
        assertThatThrownBy(() -> BackupFiles.read(file)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("프로젝트 백업");
    }
}
