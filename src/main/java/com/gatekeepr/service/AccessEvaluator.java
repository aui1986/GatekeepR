package com.gatekeepr.service;

import com.gatekeepr.client.TransitAccessClient;
import com.gatekeepr.client.TransitAccessClient.AccessRights;
import com.gatekeepr.client.TransitAccessClient.ObjectAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Dient der Abfrage von Zugriffsrechten über die externe Policy Machine (TRANSIT).
 * 
 * Diese Komponente stellt Methoden bereit, um sowohl Einzelobjektzugriffe als auch
 * Zugriffssuchen über Entitätsklassen zu bewerten.
 */
@Service
@RequiredArgsConstructor
public class AccessEvaluator {

    private final TransitAccessClient transit;

    /**
     * Prüft, welche Rechte eine Identität auf ein bestimmtes Objekt besitzt.
     *
     * @param objectId       ID des Objekts
     * @param identityId     Die anfragende Identität
     * @param requestedById  Die ursprüngliche Quelle der Anfrage (z. B. API-Client)
     * @return Zugriffsrechte auf Attributebene (READ, WRITE, etc.)
     */
    public AccessRights evaluateDirectAccess(String objectId, String identityId, String requestedById) {
        return transit.getAccessRights(objectId, identityId, requestedById);
    }

    /**
     * Ermittelt alle Objekte einer Entitätsklasse, auf die eine Identität Zugriff hat.
     *
     * @param identityId      Die anfragende Identität (optional)
     * @param requestedById   Ursprung der Anfrage
     * @param entityClass     Entitätstyp (z. B. "Vehicle")
     * @param createdByMyOwn  Nur eigene Objekte? (optional)
     * @param pageSize        Begrenzung der Rückgabemenge (optional)
     * @return Liste der zugreifbaren Objekte
     */
    public List<ObjectAccess> evaluateSearchAccess(
            String identityId,
            String requestedById,
            String entityClass,
            Boolean createdByMyOwn,
            Integer pageSize
    ) {
        return transit.searchAccessibleObjects(identityId, requestedById, entityClass, createdByMyOwn, pageSize);
    }
}
