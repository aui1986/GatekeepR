package com.gatekeepr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Repräsentiert zugelassene Zugriffsrechte auf Feldebene für ein Objekt.
 * 
 * Wird typischerweise von der Policy Machine geliefert und enthält
 * getrennte Rechte für Lesen, Schreiben und Teilen von Daten.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObjectProperties {

    /** Felder, die von der Identität gelesen werden dürfen */
    private List<String> readProperties;

    /** Felder, die von der Identität beschrieben (verändert) werden dürfen */
    private List<String> writeProperties;

    /** Felder, die mit anderen geteilt (read-only) weitergegeben werden dürfen */
    private List<String> shareReadProperties;

    /** Felder, die mit Schreibrechten an Dritte weitergegeben werden dürfen */
    private List<String> shareWriteProperties;
}
