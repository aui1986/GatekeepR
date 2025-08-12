package com.gatekeepr.client;

import com.gatekeepr.dto.ObjectProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Client zur Kommunikation mit dem externen Policy Machine.
 * Dient der Abfrage zugriffsbezogener Metadaten und der Suche nach freigegebenen Objekten.
 */
@Slf4j
@Component
public class TransitAccessClient {

    private static final String TRANSIT_BASE_URL = "http://192.168.71.102:8085/v1"; //IP unter der die Polcy Machine errechbar ist
    private static final String API_KEY = "614D5358726EC07655BF4A38CA751E5055FEF920ECCAC2624C";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Abfrage von Zugriffsrechten auf ein einzelnes Objekt.
     *
     * @param applicationId  ID der Application
     * @param objectId       Die ID des Objekts
     * @param identityId     Die ID der anfragenden Identität
     * @param requestedById  Die ID des ursprünglichen Anfragenden (z. B. System oder Benutzer)
     * @return Objekt mit Lese-/Schreibrechten oder leeres Rechteobjekt
     */
    public AccessRights getAccessRights(String applicationId, String objectId, String identityId, String requestedById) {
        String url = String.format("%s/application/%s/access/%s?identityId=%s&requestedById=%s",
                TRANSIT_BASE_URL, applicationId, objectId, identityId, requestedById);


        log.info(url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", API_KEY);

        try {
            ResponseEntity<AccessRights> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), AccessRights.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Keine Zugriffsrechte in TRANSIT gefunden (404) fuer applicationId='{}', objectId='{}', identityId='{}'",
                    applicationId, objectId, identityId);
        } catch (Exception e) {
            log.error("Fehler beim Aufruf des TRANSIT-Systems", e);
        }

        return AccessRights.empty();
    }

    /**
     * Abfrage aller Objekte, auf die eine bestimmte Identität Zugriff hat.
     *
     * @param applicationId    ID der Application
     * @param identityId       Optional in manchen Zugriffsszenarios: Die Identität, für die Zugriffsrechte geprüft werden
     * @param requestedById    Die ID des aufrufenden Systems/Nutzers
     * @param entityClass      Der Entitätstyp des Zielobjekts (z. B. "Vehicle")
     * @param createdByMyOwn   Optional: Nur eigene erstellte Objekte betrachten
     * @param pageSize         Optional: Seitengröße der Ergebnisse
     * @return Liste der zugänglichen Objekte
     */
    public List<ObjectAccess> searchAccessibleObjects(
            String applicationId, 
            String identityId,
            String requestedById,
            String entityClass,
            Boolean createdByMyOwn,
            Integer pageSize
    ) {
        StringBuilder url = new StringBuilder(TRANSIT_BASE_URL + "/application/" + applicationId + "/access/search/?");

        if (identityId != null && !identityId.isBlank()) {
            url.append("identityId=").append(identityId).append("&");
        }

        url.append("requestedById=").append(requestedById)
           .append("&objectEntityClass=").append(entityClass)
           .append("&createdByMyOwn=").append(createdByMyOwn != null ? createdByMyOwn : true);

        if (pageSize != null) {
            url.append("&pagesize=").append(pageSize);
        }

        log.info("TRANSIT-Objektsuche: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", API_KEY);

        try {
            ResponseEntity<SearchResponse> response = restTemplate.exchange(
                    url.toString(), HttpMethod.GET, new HttpEntity<>(headers), SearchResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getObjects();
            }
        } catch (Exception e) {
            log.error("Fehler beim Abrufen der zugänglichen Objekte von TRANSIT", e);
        }

        return Collections.emptyList();
    }

    /**
     * Wrapper für Zugriffsrechte auf ein Objekt (Lesen, Schreiben, etc.).
     */
    @Data
    public static class AccessRights {
        private String objectId;
        private String objectEntityClass;
        private String identityId;
        private ObjectProperties objectProperties;

        public boolean isEmpty() {
            return objectProperties == null || objectProperties.getReadProperties().isEmpty();
        }

        public Set<String> getRead() {
            return new HashSet<>(Optional.ofNullable(objectProperties.getReadProperties()).orElse(List.of()));
        }

        public Set<String> getWrite() {
            return new HashSet<>(Optional.ofNullable(objectProperties.getWriteProperties()).orElse(List.of()));
        }

        public Set<String> getSharedRead() {
            return new HashSet<>(Optional.ofNullable(objectProperties.getShareReadProperties()).orElse(List.of()));
        }

        public Set<String> getSharedWrite() {
            return new HashSet<>(Optional.ofNullable(objectProperties.getShareWriteProperties()).orElse(List.of()));
        }

        public Set<ObjectProperties.DigitAccess> getDigitsAccess() {
            return new HashSet<>(Optional.ofNullable(objectProperties.getDigitsAccess()).orElse(List.of()));
        }

        public static AccessRights empty() {
            AccessRights ar = new AccessRights();
            ar.setObjectProperties(new ObjectProperties(
                    List.of(), List.of(), List.of(), List.of(), List.of()
            ));
            return ar;
        }
    }

    /**
     * Interne Hilfsklasse zur Entgegennahme von Suchergebnissen aus PM.
     */
    @Data
    public static class SearchResponse {
        private List<ObjectAccess> objects = List.of();
    }

    /**
     * Repräsentiert ein Objekt mit Zugriffsdaten.
     */
    @Data
    public static class ObjectAccess {
        private String applicationId; 
        private String objectId;
        private String objectEntityClass;
        private String identityId;
        private ObjectProperties objectProperties;
    }
}
