package com.gatekeepr.response;

import com.gatekeepr.dto.AccessRequestDto;
import com.gatekeepr.dto.AccessibleObject;
import com.gatekeepr.dto.ObjectProperties;
import com.gatekeepr.policy.RuleUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Erzeugt ein {@link AccessibleObject} mit gefilterten Daten auf Basis
 * der zugehörigen Zugriffsrechte und anwendbaren Filterregeln.
 */
@Component
@RequiredArgsConstructor
public class AccessResponseBuilder {

    private final ResponseEngine responseEngine;

    /**
     * Baut ein Objekt mit bereinigten (gefilterten) Datenattributen.
     *
     * @param objectId     ID des Zielobjekts
     * @param entityClass  Typ des Objekts (z. B. "Vehicle")
     * @param identityId   Identität, für die Rechte gelten
     * @param rights       Zugriffsrechte auf Attributebene
     * @param rawData      Ursprüngliche Rohdaten des Objekts
     * @param request      Ursprüngliche Anfrage (für Kontextprüfung)
     * @param ruleSummary  Zähler zur Analyse angewendeter Regeln
     * @return Neues {@link AccessibleObject} mit Filterergebnis
     */
    public AccessibleObject build(
            String applicationId,
            String objectId,
            String entityClass,
            String identityId,
            ObjectProperties rights,
            Map<String, Object> rawData,
            AccessRequestDto request,
            Map<String, RuleUsage> ruleSummary
    ) {
        Map<String, Object> filtered = responseEngine.filterAndTransform(
                rawData,
                rights.getReadProperties(),
                rights.getDigitsAccess() != null ? rights.getDigitsAccess() : List.of(),
                request,
                ruleSummary
        );

        return new AccessibleObject(
                applicationId,
                objectId,
                entityClass,
                identityId,
                rights,
                filtered
        );
    }
}
