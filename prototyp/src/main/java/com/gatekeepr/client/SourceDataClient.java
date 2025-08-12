package com.gatekeepr.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Simuliert den Datenabruf aus einem externen Quellsystem.
 * 
 * Liefert aktuell variierende Fahrzeugdaten (z. B. für Filter- und Testzwecke).
 */
@Slf4j
@Component
public class SourceDataClient {

    private static final String[] BRANDS = {"Opel", "VW", "BMW", "Mercedes", "Ford"};
    private static final String[] MODELS = {"Corsa", "Golf", "3er", "A-Klasse", "Focus"};
    private static final String[] FUEL_TYPES = {"Diesel", "Benzin", "Elektro", "Hybrid"};
    private static final String[] STATUSES = {"active", "inactive", "maintenance"};

    private final Random random = new Random();

    /**
     * Simuliert den Abruf eines Objekts aus einem Quellsystem.
     *
     * @param objectId     Die Objekt-ID (z. B. UUID)
     * @param entityClass  Die Entitätsklasse (z. B. "Vehicle")
     * @return Eine Map mit simulierten Fahrzeugdaten
     */
    public Map<String, Object> loadObjectData(String objectId, String entityClass) {
        log.info("Simulating data fetch for objectId='{}', entityClass='{}'", objectId, entityClass);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("objectId", objectId);
        data.put("objectEntityClass", entityClass != null ? entityClass : "vehicle");

        // Zufallsdaten für vehicle
        if ("vehicle".equalsIgnoreCase(entityClass)) {
            data.put("licensePlate", generateRandomPlate());
            data.put("brand", randomChoice(BRANDS));
            data.put("model", randomChoice(MODELS));
            data.put("location", "DE");
            data.put("fuelType", randomChoice(FUEL_TYPES));
            data.put("mileage", 5000 + random.nextInt(195_000));
            data.put("status", randomChoice(STATUSES));
        } else {
            // Optional: generisches Objekt
            data.put("name", "Generic Object");
            data.put("description", "Simulated data for unknown entity class");
            data.put("status", "active");
        }

        return data;
    }

    private String randomChoice(String[] options) {
        return options[random.nextInt(options.length)];
    }

    private String generateRandomPlate() {
        String letters = String.valueOf((char) (random.nextInt(26) + 'A')) +
                         (char) (random.nextInt(26) + 'A');
        int number = 100 + random.nextInt(900);
        return letters + "-" + number;
    }
}
