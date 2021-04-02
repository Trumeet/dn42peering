package moe.yuuta.dn42peering.database;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.RepairResult;

import javax.annotation.Nonnull;
import java.util.stream.Collectors;

public class DatabaseMigration {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigration.class.getSimpleName());
    public static Future<Void> autoMigrate(@Nonnull Vertx vertx,
                                           @Nonnull DatabaseConfiguration configuration) {
        return vertx.executeBlocking(f -> {
            if(configuration.migrateAction == DatabaseConfiguration.MigrateAction.DISABLED) {
                logger.warn("Database migration is disabled. Use with care.");
                f.complete();
                return;
            }
            final FluentConfiguration flywayConfig = Flyway.configure()
                    .dataSource(String.format("jdbc:mariadb://%s:%d/%s",
                            configuration.host,
                            configuration.port,
                            configuration.database),
                            configuration.user,
                            configuration.password);
            if(configuration.migrateAction == DatabaseConfiguration.MigrateAction.AUTO_NO_BASELINE) {
                logger.warn("Baseline migration is disabled. The migration will fail if the " +
                        "database is dirty.");
            } else {
                flywayConfig
                        .baselineOnMigrate(true)
                        .baselineVersion("3");
            }
            final Flyway flyway = flywayConfig.load();

            switch (configuration.migrateAction) {
                case AUTO:
                case AUTO_NO_BASELINE:
                    logger.info("Starting database migration");
                    final MigrateResult result = flyway.migrate();

                    String warningPrompt;
                    if(result.warnings == null || result.warnings.isEmpty()) {
                        warningPrompt = "No warnings produced.";
                    } else {
                        warningPrompt = result.warnings.toString();
                    }

                    logger.info(String.format("Migration completed. " +
                                    "Processed %d migrations.\n" +
                                    "From %s to %s.\n" +
                                    "%s",
                            result.migrationsExecuted,
                            result.initialSchemaVersion,
                            result.targetSchemaVersion,
                            warningPrompt));
                    f.complete();
                    return;
                case REPAIR:
                    logger.warn("Repair mode enabled. Make sure " +
                            "you had already followed the instructions to undo failed migrations.\n" +
                            "Also remember to turn repair mode off after you finished.\n" +
                            "Repair mode will only cleanup upgrade history. All leftover migrations (half-completed " +
                            "changes) must be cleaned yourself. Good luck!");
                    final RepairResult repairResult = flyway.repair();
                    logger.warn(String.format("Repair completed.\n" +
                            "Repair actions: %s\n" +
                            "Removed migrations: %s\n" +
                            "Deleted migrations: %s\n" +
                            "Aligned migrations: %s",
                            repairResult.repairActions,
                            repairResult.migrationsRemoved == null ? "Empty" :
                                repairResult.migrationsRemoved.stream().map(output -> output.version).collect(Collectors.toList()),
                            repairResult.migrationsDeleted == null ? "Empty" :
                                    repairResult.migrationsDeleted.stream().map(output -> output.version).collect(Collectors.toList()),
                            repairResult.migrationsAligned == null ? "Empty" :
                                    repairResult.migrationsAligned.stream().map(output -> output.version).collect(Collectors.toList())));
                    f.fail(new ShutdownException());
                    return;
            }
        });
    }

    public static class ShutdownException extends Exception {}
}
