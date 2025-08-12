package com.gatekeepr.policy;

import com.gatekeepr.dto.AccessRequestDto;
import com.gatekeepr.dto.RuleDefinition;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.*;

/**
 * Kernkomponente zur Bewertung kontextabhängiger Regeln.
 * 
 * Verarbeitet Regeln aus der Konfigurationsdatei und entscheidet,
 * ob und wie bestimmte Felder maskiert, entfernt o. ä. werden sollen.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyEngine {

    private final RuleLoader ruleLoader;

    /**
     * Bewertet alle Regeln für einen Request und gibt die Feld-Aktionen zurück.
     *
     * @param request Zugriffsanfrage
     * @return Map von Feldname → Aktion (z. B. "mask", "remove")
     */
    public Map<String, String> evaluate(AccessRequestDto request) {
        Map<String, String> decisions = new HashMap<>();

        for (RuleDefinition rule : ruleLoader.getRules()) {
            log.debug("Prüfe Regel für Feld '{}', Bedingung: {}", rule.getField(), rule.getCondition());

            if (matchesCondition(rule.getCondition(), request)) {
                decisions.put(rule.getField(), rule.getAction());
            }
        }

        return decisions;
    }

    /**
     * Gibt alle passenden Regeln für einen Request zurück.
     * 
     * @param request Zugriffsanfrage
     * @return Liste der zutreffenden Regeln
     */
    public List<RuleDefinition> getMatchingRules(AccessRequestDto request) {
        return ruleLoader.getRules().stream()
                .filter(rule -> matchesCondition(rule.getCondition(), request))
                .toList();
    }

    /**
     * Prüft, ob eine Bedingung (aus der JSON-Konfiguration) auf die Anfrage zutrifft.
     *
     * Unterstützt:
     * - Zeitfenster (before/after)
     * - Kontextbasierte Werte
     * - Zugriffshäufigkeit (accessCount)
     * - Objektanzahl (objectCount)
     * - Always-Flag
     *
     * @param condition Regelbedingung
     * @param request   Zugriffsanfrage
     * @return true, wenn die Bedingung erfüllt ist
     */
    private boolean matchesCondition(Map<String, Object> condition, AccessRequestDto request) {
        if (condition == null || condition.isEmpty()) return false;
        boolean matched = true;

        // Zeitbedingung (z. B. nur nach 08:00 und vor 18:00)
        if (condition.containsKey("time")) {
            Map<String, String> timeCond = castMap(condition.get("time"), String.class);
            LocalTime now = LocalTime.now();

            boolean afterOk = !timeCond.containsKey("after") || now.isAfter(LocalTime.parse(timeCond.get("after")));
            boolean beforeOk = !timeCond.containsKey("before") || now.isBefore(LocalTime.parse(timeCond.get("before")));

            matched &= (afterOk && beforeOk);
        }

        // Kontextbasierte Bedingungen (z. B. { "context": { "region": "EU" } })
        if (condition.containsKey("context")) {
            Map<String, String> requiredContext = castMap(condition.get("context"), String.class);
            if (request.getContext() == null) return false;

            for (Map.Entry<String, String> entry : requiredContext.entrySet()) {
                Object actual = request.getContext().get(entry.getKey());
                if (actual == null || !entry.getValue().equals(actual.toString())) {
                    return false;
                }
            }
        }

        // Zugriffshäufigkeit (z. B. "accessCount" > 5)
        if (condition.containsKey("accessCount")) {
            Map<String, Integer> countCond = castMap(condition.get("accessCount"), Integer.class);
            if (!evaluateNumericCondition(request, "accessCount", countCond)) return false;
        }

        // Anzahl an zurückgelieferten Objekten (z. B. zur Limitierung von Detailtiefe)
        if (condition.containsKey("objectCount")) {
            Map<String, Integer> countCond = castMap(condition.get("objectCount"), Integer.class);
            if (!evaluateNumericCondition(request, "objectCount", countCond)) return false;
        }

        // Immer ausführen, wenn "always": true
        if (condition.containsKey("always")) {
            Object val = condition.get("always");
            matched &= val instanceof Boolean && (Boolean) val;
        }

        return matched;
    }

    /**
     * Hilfsmethode zur Prüfung numerischer Kontextwerte wie accessCount oder objectCount.
     */
    private boolean evaluateNumericCondition(AccessRequestDto request, String ctxKey, Map<String, Integer> cond) {
        if (request.getContext() == null || !request.getContext().containsKey(ctxKey)) return false;

        try {
            int actual = Integer.parseInt(request.getContext().get(ctxKey).toString());

            if (cond.containsKey("greaterThan") && actual <= cond.get("greaterThan")) return false;
            if (cond.containsKey("lessThan") && actual >= cond.get("lessThan")) return false;
            if (cond.containsKey("equals") && actual != cond.get("equals")) return false;

        } catch (NumberFormatException e) {
            log.warn("Ungültiger numerischer Wert für '{}': {}", ctxKey, request.getContext().get(ctxKey));
            return false;
        }

        return true;
    }

    /**
     * Kapselt eine unsichere Cast-Operation mit unterdrückter Warnung.
     */
    @SuppressWarnings("unchecked")
    private <T> Map<String, T> castMap(Object obj, Class<T> clazz) {
        return (Map<String, T>) obj;
    }
}
