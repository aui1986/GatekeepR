# GatekeepR

GatekeepR ist eine **Middleware** zur kontextsensitiven Zugriffskontrolle für API-Responses.  
Das System verarbeitet Antworten aus Quellsystemen, filtert sie basierend auf den von einer **Policy Machine** gelieferten Attributrechten und wendet zusätzlich lokal definierte Regeln an.

## Funktionsweise

1. **Anfrage**  
   GatekeepR empfängt eine API-Anfrage mit Kontextinformationen (z. B. Identität, Objekt, gewünschte Felder).

2. **Abfrage der Policy Machine**  
   Die Zugriffsrechte werden über die [Finest-Grained Attribute-Based Policy Machine](https://github.com/KoberSascha/Finest-Grained-Attribute-Based-Policy-Machine?tab=readme-ov-file) ermittelt.

3. **Level 1 – Policy-Machine-Filterung**  
   - Erlaubte Felder werden gefiltert (`readProperties`, `writeProperties`, `shareReadProperties`, `shareWriteProperties`, `digitAccess`).
   - **DigitAccess** wird vor der eigentlichen Maskierung angewendet, um Teilfreigaben (z. B. nur bestimmte Zeichen eines Wertes) umzusetzen.

4. **Level 2 – Lokale GatekeepR-Regeln**  
   - Zusätzliche Maskierung, Generalisierung, Pseudonymisierung oder Entfernung von Feldern.
   - Regeln werden in einer `rules.json` definiert.

5. **Antwort**  
   GatekeepR gibt die gefilterten Daten an den anfragenden Client zurück.

## Hauptfunktionen

- **Integration mit der Finest-Grained ABAC Policy Machine**
- **Feldgenaue Zugriffskontrolle** basierend auf PM-Attributrechten
- **DigitAccess-Unterstützung** für selektive Sichtbarkeit von Datenfragmenten
- **Flexible lokale Regeln** über konfigurierbare JSON-Datei
- **Protokollierung** aller angewendeten Regeln und Berechtigungen

## Voraussetzungen

- Java 17+
- Maven
- Zugriff auf eine laufende Policy Machine Instanz  
  (siehe: [Finest-Grained Attribute-Based Policy Machine – GitHub](https://github.com/KoberSascha/Finest-Grained-Attribute-Based-Policy-Machine?tab=readme-ov-file))

## Installation & Start

```bash
# Projekt klonen
git clone https://github.com/aui1986/GatekeepR.git
cd GatekeepR/prototyp

# Build
docker compose -d --build

```
## Konfiguration
/config/rules.json definiert die lokalen Filter- und Transformationsregeln.

## Verbindung zur Policy Machine
GatekeepR ist auf die Zusammenarbeit mit der
Finest-Grained Attribute-Based Policy Machine ausgelegt.
Die Policy Machine liefert die primären Zugriffsrechte, GatekeepR verfeinert diese anschließend.

## Test mittels Postman
Um einen ersten Eindruck zu erhalten, liegt im Ordner /postman-testdata eine json welche bei einer lokaler Dockerinstanz von
der Policy Machine die Datenbank mit Testdaten eines fiktiven Fahrzeugpools fühlt sowie geeignete Abfragen von un- und gefilterten Request an
GatekeepR.