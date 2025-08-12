package com.gatekeepr.policy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatekeepr.dto.RuleDefinition;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Lädt die Filter- und Transformationsregeln (rule.json) zur Laufzeit.
 * 
 * Die Datei enthält eine Liste von {@link RuleDefinition}-Einträgen, die beim Start und auf Wunsch neu eingelesen werden.
 */
@Slf4j
@Component
public class RuleLoader {

    /** Pfad zur Regeldatei, konfiguriert über application.properties/yml */
    @Value("${gatekeepr.rules.path}")
    private String rulesPathProp;

    /** Liste aller aktuell geladenen Regeln */
    @Getter
    private List<RuleDefinition> rules = Collections.emptyList();

    /**
     * Initialer Ladevorgang beim Systemstart.
     */
    @PostConstruct
    public void loadRules() {
        this.rules = readRulesFromFile();
        log.info("Loaded {} policy rules from '{}'", rules.size(), rulesPathProp);
    }

    /**
     * Ermöglicht das manuelle Nachladen von Regeln zur Laufzeit.
     * Wird z. B. im Testkontext oder bei externer Änderung aufgerufen.
     */
    public void reload() {
        this.rules = readRulesFromFile();
        log.info("Rules reloaded ({} entries)", rules.size());
    }

    /**
     * Interner Lesevorgang der JSON-basierten Regeldatei.
     *
     * @return Liste der gültigen Regeln oder leere Liste bei Fehlern
     */
    private List<RuleDefinition> readRulesFromFile() {
        Path rulesFile = Paths.get(rulesPathProp);

        try (InputStream in = Files.newInputStream(rulesFile)) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(in, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Fehler beim Laden der Regeldatei '{}'", rulesFile.toAbsolutePath(), e);
            return Collections.emptyList();
        }
    }
}
