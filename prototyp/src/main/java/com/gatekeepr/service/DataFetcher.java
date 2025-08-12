package com.gatekeepr.service;

import com.gatekeepr.client.SourceDataClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Verantwortlich für den Abruf der Rohdaten eines Objekts aus dem Quellsystem.
 * 
 * Diese Daten werden nach erfolgreicher Rechteprüfung durch GatekeepR
 * gefiltert und ggf. transformiert zurückgegeben.
 */
@Service
@RequiredArgsConstructor
public class DataFetcher {

    private final SourceDataClient source;

    /**
     * Lädt die vollständigen Rohdaten eines Objekts anhand seiner ID und Entitätsklasse.
     *
     * @param objectId     Eindeutige ID des Objekts
     * @param entityClass  Klassentyp des Objekts (z. B. "Vehicle")
     * @return Map mit den Rohdatenattributen (z. B. { "licensePlate": "XYZ 123" })
     */
    public Map<String, Object> fetchRawData(String objectId, String entityClass) {
        return source.loadObjectData(objectId, entityClass);
    }
}
