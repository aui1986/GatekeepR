package com.gatekeepr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Repräsentiert ein zugreifbares Objekt, das durch eine bestimmte Identität abrufbar ist.
 * 
 * Enthält sowohl die zugrundeliegenden Zugriffsrechte (objectProperties) als auch die
 * nach Anwendung der Regeln gefilterten Daten (filteredData).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessibleObject {

    /** ID des Objekts, auf das zugegriffen werden kann (z. B. Fahrzeug-ID) */
    private String objectId;

    /** Klasse bzw. Typ des Objekts (z. B. "Vehicle", "Person") */
    private String objectEntityClass;

    /** Identität, die Zugriff auf das Objekt hat */
    private String identityId;

    /** Von der Policy Machine ermittelte Zugriffsrechte auf Attributebene */
    private ObjectProperties objectProperties;

    /** Tatsächlich freigegebene/zugeschnittene Daten nach Anwendung der Filterregeln */
    private Map<String, Object> filteredData;
}
