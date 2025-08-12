package com.gatekeepr.service;

import com.gatekeepr.policy.RuleLoader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Überwacht die Regeldatei auf Änderungen und stößt bei Bedarf ein automatisches Neuladen an.
 *
 * Diese Komponente arbeitet zyklisch und erkennt Modifikationen an der Datei über den Zeitstempel.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleReloadService {

    private final RuleLoader ruleLoader;

    @Value("${gatekeepr.rules.path}")
    private String rulesPathProp;

    private Path rulesPath;
    private long lastModified = 0L;

    /**
     * Initialisiert den Dienst und speichert den Initialzustand der Regeldatei.
     */
    @PostConstruct
    public void init() {
        try {
            this.rulesPath = Paths.get(rulesPathProp).toAbsolutePath();
            if (Files.exists(rulesPath)) {
                this.lastModified = Files.getLastModifiedTime(rulesPath).toMillis();
                log.info("Initial rule file detected: {}", rulesPath);
            } else {
                log.warn("Rule file does not exist at startup: {}", rulesPath);
            }
        } catch (Exception e) {
            log.warn("Could not initialize RuleReloadService", e);
        }
    }

    /**
     * Prüft alle 5 Sekunden, ob sich die Regeldatei verändert hat.
     * Erkennt Änderungen anhand des "last modified"-Zeitstempels.
     */
    @Scheduled(fixedDelay = 5000)
    public void checkForUpdates() {
        if (rulesPath == null) return;

        try {
            if (!Files.exists(rulesPath)) return;

            long currentModified = Files.getLastModifiedTime(rulesPath).toMillis();
            if (currentModified > lastModified) {
                log.info("Detected change in '{}', reloading rules...", rulesPath);
                ruleLoader.reload();
                lastModified = currentModified;
            }
        } catch (Exception e) {
            log.error("Error while checking for rule updates", e);
        }
    }
}
