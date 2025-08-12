package com.gatekeepr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Datenobjekt für eine Zugriffsanfrage an GatekeepR.
 * 
 * Dient der Übermittlung aller relevanten Parameter für die Rechteermittlung
 * und die dynamische Filterung von Objektdaten.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessRequestDto {

    /** Die Identität, für die der Zugriff geprüft werden soll (Pflicht bei Einzelobjektabfrage) */
    private String identityId;

    /** Die Identität, die die Anfrage auslöst (z. B. Systemakteur, Dienst, Frontend-User) */
    private String requestedById;

    /** Objekt-ID eines einzelnen Zielobjekts (z. B. Fahrzeug, Person) */
    private String objectId;

    /** Liste mehrerer Objekt-IDs bei Mehrfachanfragen */
    private List<String> objectIds;

    /** Objektklasse (z. B. "Vehicle", "Sensor"), erforderlich bei Suchanfragen */
    private String objectEntityClass;

    /** Optionaler Kontext für die Anfrage (z. B. Uhrzeit, IP-Adresse, Geoposition) */
    private Map<String, Object> context;

    /** Gibt an, ob nur selbst erstellte Objekte betrachtet werden sollen (Standard: true) */
    private Boolean createdByMyOwn;

    /** Anzahl der zurückzugebenden Objekte bei Mehrfachabfragen (optional, z. B. 50) */
    private Integer pageSize;
}
