package gr.hua.dit.dras.dto;

public record ExternalCleanupResult(
        int matched,
        int deleted,
        boolean dryRun,
        int graceDays,
        long durationMillis,
        boolean lockAcquired
) {
}
