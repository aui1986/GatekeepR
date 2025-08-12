package com.gatekeepr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Antwortobjekt für gefilterte Datenzugriffe via GatekeepR.
 * 
 * Liefert ausschließlich die autorisierten Dateninhalte nach Anwendung der Filterlogik.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilteredAccessResponseDto {

    /** Die gefilterten (reduzierten) Daten, typischerweise als Map oder Objektstruktur */
    private Object data;

    /** Status der Antwort, z. B. "success" oder "error" */
    private String status;

    /** Nachricht oder Fehlerbeschreibung */
    private String message;

    /** Zeitstempel im ISO-Format */
    private String timestamp;

    /**
     * Erzeugt eine Fehlerantwort mit Standardstruktur.
     *
     * @param msg Fehlermeldung
     * @return Fehlerhafte Filterantwort mit aktuellem Zeitstempel
     */
    public static FilteredAccessResponseDto error(String msg) {
        return new FilteredAccessResponseDto(
                null,
                "error",
                msg,
                java.time.ZonedDateTime.now().toString()
        );
    }
}
