package com.gatekeepr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Antwortobjekt für eine Zugriffsanfrage an GatekeepR.
 * 
 * Enthält entweder eine Liste zugänglicher Objekte oder eine Fehlermeldung.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessResponseDto {

    /** Liste aller zugänglichen Objekte inkl. Rechteinformationen und ggf. Filterergebnis */
    private List<AccessibleObject> objects;

    /** Status der Antwort, z. B. "success" oder "error" */
    private String status;

    /** Optionaler Fehler- oder Hinweistext */
    private String message;

    /** Zeitstempel der Antwort im ISO-Format */
    private String timestamp;

    /**
     * Hilfsmethode zur schnellen Erstellung eines Fehlerobjekts mit Zeitstempel.
     *
     * @param msg Fehlernachricht
     * @return AccessResponseDto mit Status "error"
     */
    public static AccessResponseDto error(String msg) {
        return new AccessResponseDto(null, "error", msg, Instant.now().toString());
    }
}
