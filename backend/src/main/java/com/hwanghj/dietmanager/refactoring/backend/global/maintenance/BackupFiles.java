package com.hwanghj.dietmanager.refactoring.backend.global.maintenance;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import tools.jackson.databind.ObjectMapper;
import com.hwanghj.dietmanager.refactoring.backend.global.maintenance.BackupModel.Backup;
final class BackupFiles {
    private BackupFiles() {}
    private static Path folder() { return Path.of("..", ".local-backups").toAbsolutePath().normalize(); }
    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    static Path write(Backup backup) throws Exception {
        Path directory = folder();
        Files.createDirectories(directory);
        Path file = directory.resolve("mysql-" + System.currentTimeMillis() + ".json");
        byte[] bytes = new ObjectMapper().writeValueAsBytes(backup);
        Files.write(file, bytes, StandardOpenOption.CREATE_NEW);
        Files.writeString(Path.of(file + ".sha256"), hash(bytes), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        return file;
    }
    static Backup read(Path requested) throws Exception {
        Path file = requested.toAbsolutePath().normalize().toRealPath();
        if (!file.startsWith(folder().toRealPath()) || !file.getFileName().toString().matches("mysql-[0-9]+\\.json")) {
            throw new IllegalArgumentException("프로젝트 백업 파일만 복원할 수 있습니다.");
        }
        byte[] bytes = Files.readAllBytes(file);
        if (!hash(bytes).equals(Files.readString(Path.of(file + ".sha256")).trim())) {
            throw new IllegalArgumentException("백업 체크섬 불일치");
        }
        return new ObjectMapper().readValue(bytes, Backup.class);
    }
}
