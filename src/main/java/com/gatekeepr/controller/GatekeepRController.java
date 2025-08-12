package com.gatekeepr.controller;

import com.gatekeepr.dto.AccessRequestDto;
import com.gatekeepr.dto.AccessResponseDto;
import com.gatekeepr.dto.FilteredAccessResponseDto;
import com.gatekeepr.service.ObjectRequestHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Zentrale Controller-Klasse für Zugriffsanfragen an GatekeepR.
 * Steuert eingehende API-Requests und übergibt sie zur Weiterverarbeitung an den ObjectRequestHandler.
 */
@RestController
@RequestMapping("/gatekeepr")
@RequiredArgsConstructor
@Slf4j
public class GatekeepRController {

    private final ObjectRequestHandler objectRequestHandler;

    /**
     * Validiert eingehende Zugriffsanfragen basierend auf minimal erforderlichen Feldern.
     *
     * @param dto Anfrageobjekt
     * @return Fehlermeldung oder null bei gültiger Anfrage
     */
    private String validateRequest(AccessRequestDto dto) {
        boolean hasSingle = dto.getObjectId() != null && !dto.getObjectId().isBlank();
        boolean hasMultiple = dto.getObjectIds() != null && !dto.getObjectIds().isEmpty();
        boolean hasEntityClass = dto.getObjectEntityClass() != null && !dto.getObjectEntityClass().isBlank();

        if (!hasSingle && !hasMultiple && !hasEntityClass) {
            return "Entweder objectId, objectIds oder objectEntityClass muss angegeben sein.";
        }

        if (hasSingle && (dto.getIdentityId() == null || dto.getIdentityId().isBlank())) {
            return "identityId ist erforderlich beim Zugriff auf ein einzelnes Objekt.";
        }

        return null;
    }

    /**
     * Einstiegspunkt für Anfragen, bei denen ein vollständiges AccessResponseDto zurückgegeben wird.
     *
     * @param requestDto Die Zugriffsanfrage
     * @return Vollständige Response inkl. Rohdaten und Metainformationen
     */
    @PostMapping("/request")
    public ResponseEntity<AccessResponseDto> handleAccessRequest(@RequestBody AccessRequestDto requestDto) {
        log.info("Zugriffsanfrage erhalten: identityId='{}', requestedById='{}', objectId='{}', entityClass='{}'",
                requestDto.getIdentityId(),
                requestDto.getRequestedById(),
                requestDto.getObjectId(),
                requestDto.getObjectEntityClass());

        String validationError = validateRequest(requestDto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(AccessResponseDto.error(validationError));
        }

        AccessResponseDto response = objectRequestHandler.handleRequest(requestDto);
        return ResponseEntity.ok(response);
    }

    /**
     * Einstiegspunkt für Anfragen, bei denen nur das gefilterte Ergebnis zurückgegeben wird.
     *
     * @param requestDto Die Zugriffsanfrage
     * @return Nur die zulässigen Datenfelder als FilteredAccessResponseDto
     */
    @PostMapping("/filtered")
    public ResponseEntity<FilteredAccessResponseDto> getFilteredDataOnly(@RequestBody AccessRequestDto requestDto) {
        log.info("Zugriffsanfrage erhalten: identityId='{}', objectId='{}', entityClass='{}'",
                requestDto.getIdentityId(),
                requestDto.getObjectId(),
                requestDto.getObjectEntityClass());

        String validationError = validateRequest(requestDto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(
                    new FilteredAccessResponseDto(
                            null,
                            "error",
                            validationError,
                            java.time.ZonedDateTime.now().toString()
                    )
            );
        }

        FilteredAccessResponseDto response = objectRequestHandler.handleFilteredResponse(requestDto);
        return ResponseEntity.ok(response);
    }
}
