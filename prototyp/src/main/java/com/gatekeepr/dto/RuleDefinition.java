package com.gatekeepr.dto;

import lombok.Data;

import java.util.Map;

/**
 * Repräsentiert eine einzelne Filterregel für ein Feld.
 * 
 * Wird typischerweise aus der Datei rules.json geladen und beschreibt,
 * unter welchen Bedingungen eine bestimmte Aktion (z. B. Maskierung) auf ein Feld angewendet wird.
 */
@Data
public class RuleDefinition {

    /** Pfad des zu verarbeitenden Felds (z. B. "vehicle.licensePlate") */
    private String field;

    /** Aktion, die ausgeführt werden soll (z. B. "mask", "remove", "pseudonymize") */
    private String action;

    /** Bedingung, unter der die Regel greift (z. B. Zeit, Kontext, Zugriffshäufigkeit) */
    private Map<String, Object> condition;

    /** Optionale Zusatzparameter, z. B. ein benutzerdefiniertes Maskierungsmuster */
    private Map<String, Object> parameters;
}
