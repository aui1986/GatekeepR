package com.gatekeepr.service;

import com.gatekeepr.client.TransitAccessClient.AccessRights;
import com.gatekeepr.client.TransitAccessClient.ObjectAccess;
import com.gatekeepr.dto.*;
import com.gatekeepr.policy.RuleUsage;
import com.gatekeepr.response.AccessResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zentrale Verarbeitungseinheit für Zugriffsanfragen.
 *
 * Koordiniert die Berechtigungsprüfung, Datenbeschaffung, Filterung und Regelanwendung.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectRequestHandler {

    private final AccessEvaluator accessEvaluator;
    private final DataFetcher dataFetcher;
    private final AccessResponseBuilder responseBuilder;

    /** Zugriffszähler für (Identität + Objekt) – wird für accessCount-Regeln verwendet */
    private final Map<String, Integer> accessCounter = new ConcurrentHashMap<>();

    /** Letzter Zugriff pro (Identität + Objekt) – zur Zurücksetzung der Zähler nach Intervall */
    private final Map<String, Long> lastAccessTime = new ConcurrentHashMap<>();

    /** Zeitraum nach dem ein Zugriffscounter zurückgesetzt wird (in ms) */
    private static final long RESET_INTERVAL_MS = 1 * 60 * 1000; // 1 Minute(n)

    /**
     * Haupteinstiegspunkt für alle Zugriffsanfragen mit Regeltracking.
     *
     * @param req         Die ursprüngliche Anfrage
     * @param ruleSummary Optionales Regelprotokoll für spätere Analyse
     * @return Komplette Zugriffsergebnisantwort
     */
    public AccessResponseDto handleRequest(AccessRequestDto req, Map<String, RuleUsage> ruleSummary) {
        String applicationId = req.getApplicationId();
        String identityId = req.getIdentityId();
        String requestedById = Optional.ofNullable(req.getRequestedById()).filter(s -> !s.isBlank()).orElse(identityId);

        List<AccessibleObject> accessibleObjects = new ArrayList<>();

        // Entscheidungspfad je nach Anfrageart
        if (req.getObjectIds() != null && !req.getObjectIds().isEmpty()) { //Mehrfachanfrage von Objekten
            for (String objectId : req.getObjectIds()) {
                AccessibleObject obj = handleDirectAccess(applicationId, objectId, req.getObjectEntityClass(), identityId, requestedById, req, ruleSummary);
                if (obj != null) accessibleObjects.add(obj);
            }
        } else if (req.getObjectId() != null && !req.getObjectId().isBlank()) { //Einzelnanfrage von Objetk
            AccessibleObject obj = handleDirectAccess(applicationId, req.getObjectId(), req.getObjectEntityClass(), identityId, requestedById, req, ruleSummary);
            if (obj != null) accessibleObjects.add(obj);
        } else if (req.getObjectEntityClass() != null && !req.getObjectEntityClass().isBlank()) { //Suche nach allen verfügbaren Objekten
            accessibleObjects.addAll(handleSearchAccess(applicationId, identityId, requestedById, req, ruleSummary));
        }

        logAccessCounter(); // optional für Audit, Debug

        return new AccessResponseDto(accessibleObjects, "success", null, Instant.now().toString());
    }

    /**
     * Filteranfragen landen hier und werden weitergereicht, am Ende hier ausgegeben mit Info über angewendete Regeln
     */
    public FilteredAccessResponseDto handleFilteredResponse(AccessRequestDto req) {
        Map<String, RuleUsage> ruleSummary = new HashMap<>();
        AccessResponseDto full = handleRequest(req, ruleSummary);

        List<Map<String, Object>> filtered = full.getObjects().stream()
                .map(AccessibleObject::getFilteredData).toList();

        Object data = (filtered.size() == 1) ? filtered.get(0) : filtered;

        if (!ruleSummary.isEmpty()) {
            log.info("-------------------------------------------------------");
            log.info("Angewendete Regeln ({}):", ruleSummary.size());
            ruleSummary.values().forEach(usage ->
                log.info("field='{}', action='{}', condition={}, count={}",
                        usage.getField(), usage.getAction(), usage.getCondition(), usage.getCount())
            );
            log.info("-------------------------------------------------------");
        }

        return new FilteredAccessResponseDto(
                data,
                full.getStatus(),
                full.getMessage(),
                full.getTimestamp()
        );
    }

    /**
     * Verarbeitet den direkten Zugriff auf ein Objekt.
     */
    private AccessibleObject handleDirectAccess(String applicationId, String objectId, String entityClass, String identityId, String requestedById,
                                                AccessRequestDto req, Map<String, RuleUsage> ruleSummary) {

        updateAccessCount(identityId, requestedById, objectId, req);

        AccessRights rights = accessEvaluator.evaluateDirectAccess(applicationId, objectId, identityId, requestedById);
        if (rights == null || rights.isEmpty()) {
            log.info("No access rights for object '{}', identity '{}'", objectId, identityId);
            return null;
        }

        Map<String, Object> rawData = dataFetcher.fetchRawData(objectId, entityClass);

        return responseBuilder.build(
                applicationId,
                objectId,
                entityClass,
                identityId,
                toProperties(rights),
                rawData,
                req,
                ruleSummary
        );
    }

    /**
     * Verarbeitet Zugriff per Suchanfrage (z. B. auf alle Fahrzeuge einer Klasse).
     */
    private List<AccessibleObject> handleSearchAccess(String applicationId, String identityId, String requestedById,
                                                      AccessRequestDto req, Map<String, RuleUsage> ruleSummary) {

        List<ObjectAccess> accessList = accessEvaluator.evaluateSearchAccess(
                applicationId,
                identityId,
                requestedById,
                req.getObjectEntityClass(),
                req.getCreatedByMyOwn(),
                req.getPageSize()
        );

        // Anzahl der zurückgegebenen Objekte in den Kontext schreiben (für Regeln)
        if (req.getContext() == null) {
            req.setContext(new HashMap<>());
        }
        req.getContext().put("objectCount", accessList.size());

        List<AccessibleObject> results = new ArrayList<>();
        for (ObjectAccess o : accessList) {
            updateAccessCount(identityId, requestedById, o.getObjectId(), req);
            Map<String, Object> raw = dataFetcher.fetchRawData(o.getObjectId(), req.getObjectEntityClass());

            results.add(responseBuilder.build(
                    applicationId,
                    o.getObjectId(),
                    req.getObjectEntityClass(),
                    identityId,
                    o.getObjectProperties(),
                    raw,
                    req,
                    ruleSummary
            ));
        }

        return results;
    }

    /**
     * Konvertiert AccessRights (von der Policy Machine) in interne ObjectProperties.
     */
    private ObjectProperties toProperties(AccessRights rights) {
        return new ObjectProperties(
                new ArrayList<>(rights.getRead()),
                new ArrayList<>(rights.getWrite()),
                new ArrayList<>(rights.getSharedRead()),
                new ArrayList<>(rights.getSharedWrite()),
                rights.getDigitsAccess() != null
                    ? new ArrayList<>(rights.getDigitsAccess())
                    : List.of()
        );
    }

    /**
     * Convenience-Variante ohne Regeltracking.
     */
    public AccessResponseDto handleRequest(AccessRequestDto req) {
        return handleRequest(req, new HashMap<>());
    }

    /**
     * Hält Zugriffszählung pro Identität und Objekt aktuell und schreibt sie in den Kontext.
     */
    private void updateAccessCount(String identityId, String requestedById, String objectId, AccessRequestDto req) {
        if (identityId == null || identityId.isBlank()) {
            identityId = requestedById;
        }

        String key = identityId + ":" + requestedById + "::" + objectId;
        long now = System.currentTimeMillis();

        Long lastAccess = lastAccessTime.get(key);
        if (lastAccess != null && (now - lastAccess) > RESET_INTERVAL_MS) {
            accessCounter.put(key, 1); // zurücksetzen
        } else {
            accessCounter.merge(key, 1, Integer::sum);
        }

        lastAccessTime.put(key, now);
        int count = accessCounter.getOrDefault(key, 1);

        if (req.getContext() == null) {
            req.setContext(new HashMap<>());
        }
        req.getContext().put("accessCount", count);
    }

    /**
     * Gibt den aktuellen Zustand des Zugriffszählers aus.
     */
    private void logAccessCounter() {
        log.info("---- Aktueller Zugriffszaehler (accessCounter) ----");
        accessCounter.forEach((key, value) ->
                log.info("Key='{}' → Count={}", key, value)
        );
        log.info("-----------------------------------------------");
    }
}
