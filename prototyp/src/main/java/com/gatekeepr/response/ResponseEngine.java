package com.gatekeepr.response;

import com.gatekeepr.dto.AccessRequestDto;
import com.gatekeepr.dto.ObjectProperties;
import com.gatekeepr.dto.RuleDefinition;
import com.gatekeepr.policy.PolicyEngine;
import com.gatekeepr.policy.RuleUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Führt kontextabhängige Filter- und Transformationsregeln auf Daten durch.
 * 
 * Diese Komponente ist für die dynamische Anpassung von Objektdaten
 * gemäß definierter Regeln verantwortlich (vgl. Kapitel 3.3 der Arbeit).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseEngine {

    private final PolicyEngine policyEngine;

    /**
     * Führt Filter- und Umwandlungslogik auf Basis erlaubter Felder und zutreffender Regeln aus.
     *
     * @param rawData          Ursprüngliche Objektdaten
     * @param allowedProperties Durch die Policy Machine erlaubte Felder
     * @param request          Kontext der aktuellen Anfrage
     * @param ruleSummary      Protokollierung der verwendeten Regeln
     * @return Gefilterte und ggf. transformierte Daten
     */
    public Map<String, Object> filterAndTransform(
            Map<String, Object> rawData,
            Collection<String> allowedProperties,
            Collection<ObjectProperties.DigitAccess> digitsAccess,
            AccessRequestDto request,
            Map<String, RuleUsage> ruleSummary
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<RuleDefinition> matchedRules = policyEngine.getMatchingRules(request);
        Map<String, List<ObjectProperties.ReadableDigitsRange>> mergedRanges = mergeDigitRanges(digitsAccess);

        String objectClass = Optional.ofNullable(rawData.get("objectEntityClass"))
                .map(Object::toString)
                .map(String::toLowerCase)
                .orElse("");

        // Gruppiere Regeln pro Feld
        Map<String, List<RuleDefinition>> rulesPerField = new HashMap<>();
        for (RuleDefinition rule : matchedRules) {
            rulesPerField.computeIfAbsent(rule.getField(), k -> new ArrayList<>()).add(rule);
        }

        for (Map.Entry<String, Object> entry : rawData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Immer durchlassen: Pflichtfelder (z. B. objectId)
            boolean isMandatory = "objectId".equals(key); // ggf. erweiterbar
            if (!allowedProperties.contains(key) && !isMandatory) continue;

            String qualifiedKey = objectClass + "." + key;

            // DigitAccess-Anwendung
            if (mergedRanges.containsKey(key) && value instanceof String s && !s.isEmpty()) {
                value = applyDigitSlicing(s, mergedRanges.get(key));
                trackRule(ruleSummary, qualifiedKey, "digitSlice", Map.of("source", "PM"));

            }

            // Suche passende Regeln (erst qualified, dann unqualified)
            List<RuleDefinition> fieldRules = Optional.ofNullable(rulesPerField.get(qualifiedKey))
                    .or(() -> Optional.ofNullable(rulesPerField.get(key)))
                    .orElse(List.of());

            // Sonderfall: Regel mit "none" überschreibt alles
            boolean hasNoneRule = fieldRules.stream().anyMatch(r -> "none".equals(r.getAction()));
            if (hasNoneRule) {
                trackRule(ruleSummary, qualifiedKey, "none", Map.of("override", true));
                result.put(key, value);
                continue;
            }

            // Transformation anwenden
            boolean transformed = false;
            for (RuleDefinition rule : fieldRules) {
                String action = rule.getAction();
                Map<String, Object> params = rule.getParameters();

                trackRule(ruleSummary, qualifiedKey, action, rule.getCondition());

                switch (action) {
                    case "remove" -> {
                        transformed = true;
                        break;
                    }
                    case "mask" -> value = applyMasking(value);
                    case "pseudonymize" -> value = applyPseudonymization(value, params);
                    case "generalize" -> value = applyGeneralization(value, params);
                }
            }

            if (!transformed) {
                result.put(key, value);
            }
        }

        return result;
    }

    // --- Transformationsmethoden ---
    private Object applyMasking(Object value) {
        if (value instanceof String s && s.length() > 3) {
            return s.substring(0, 2) + "*".repeat(Math.min(6, s.length() - 2));
        }
        return "***";
    }

    private Object applyPseudonymization(Object value, Map<String, Object> params) {
        String prefix = Optional.ofNullable(params != null ? params.get("prefix") : null)
                .map(Object::toString).orElse("pseu");
        String suffix = Optional.ofNullable(params != null ? params.get("suffix") : null)
                .map(Object::toString).orElse("");

        return prefix + new Random().nextInt(10000) + suffix;
    }

    private Object applyGeneralization(Object value, Map<String, Object> params) {
        if (!(value instanceof Number)) return "***";

        int roundTo = Optional.ofNullable(params != null ? params.get("roundTo") : null)
                .map(Object::toString)
                .map(Integer::parseInt)
                .orElse(1000);

        int original = ((Number) value).intValue();
        int rounded = (original / roundTo) * roundTo;
        return rounded + "+";
    }

    // Hilfsmethoden für DigitAccess - Merge
    private Map<String, List<ObjectProperties.ReadableDigitsRange>> mergeDigitRanges(Collection<ObjectProperties.DigitAccess> digitsAccessList) {
        Map<String, List<ObjectProperties.ReadableDigitsRange>> mergedMap = new HashMap<>();
        
        if (digitsAccessList == null) return Collections.emptyMap();        

        for (ObjectProperties.DigitAccess da : digitsAccessList) {
            if (da.getReadableDigits() != null && !da.getReadableDigits().isEmpty()) {
                mergedMap.computeIfAbsent(da.getProperty(), k -> new ArrayList<>())
                        .addAll(da.getReadableDigits());
            }
        }

        for (Map.Entry<String, List<ObjectProperties.ReadableDigitsRange>> e : mergedMap.entrySet()) {
            e.setValue(mergeRanges(e.getValue()));
        }

        return mergedMap;
    }

    private List<ObjectProperties.ReadableDigitsRange> mergeRanges(List<ObjectProperties.ReadableDigitsRange> ranges) {
        ranges.sort(Comparator.comparingInt(ObjectProperties.ReadableDigitsRange::getReadableDigitsFrom));
        List<ObjectProperties.ReadableDigitsRange> merged = new ArrayList<>();
        for (ObjectProperties.ReadableDigitsRange current : ranges) {
            if (merged.isEmpty()) {
                merged.add(current);
            } else {
                ObjectProperties.ReadableDigitsRange last = merged.get(merged.size() - 1);
                if (current.getReadableDigitsFrom() <= last.getReadableDigitsTo() + 1) {
                    last.setReadableDigitsTo(Math.max(last.getReadableDigitsTo(), current.getReadableDigitsTo()));
                } else {
                    merged.add(current);
                }
            }
        }
        return merged;
    }

    // Hilfsmethoden für DigitAccess - Slicing
    private String applyDigitSlicing(Object value, List<ObjectProperties.ReadableDigitsRange> ranges) {

        if (value == null) return null;        

        String strValue = String.valueOf(value);
        if (strValue.isEmpty() || ranges == null || ranges.isEmpty()) {
            return strValue;
        }

        char[] chars = strValue.toCharArray();
        Arrays.fill(chars, '*');

        for (ObjectProperties.ReadableDigitsRange r : ranges) {
            int from = Math.max(0, Math.min(r.getReadableDigitsFrom(), r.getReadableDigitsTo()) - 1); // 1-basiert -> 0-basiert
            int to = Math.min(chars.length, Math.max(r.getReadableDigitsFrom(), r.getReadableDigitsTo()));
            for (int i = from; i < to; i++) {
                chars[i] = strValue.charAt(i);
            }
        }

        return new String(chars);
    }



    // --- Tracking ---
    private void trackRule(Map<String, RuleUsage> summary, String fieldKey, String action, Map<String, Object> condition) {
        String countKey = fieldKey + "::" + action;
        summary.computeIfAbsent(countKey, k -> new RuleUsage(fieldKey, action, condition)).increment();
    }
}
