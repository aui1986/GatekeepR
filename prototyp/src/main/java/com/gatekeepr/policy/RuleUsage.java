package com.gatekeepr.policy;

import lombok.Getter;

import java.util.Map;

/**
 * Repräsentiert eine konkrete Regelinstanz und deren Verwendungshäufigkeit.
 * 
 * Dient der Analyse und Nachverfolgung, wie oft eine Regel angewendet wurde.
 */
@Getter
public class RuleUsage {

    /** Das betroffene Feld, auf das sich die Regel bezieht */
    private final String field;

    /** Die angewendete Aktion (z. B. "mask", "remove") */
    private final String action;

    /** Die Bedingung, unter der die Regel greift */
    private final Map<String, Object> condition;

    /** Anzahl der Anwendungen dieser Regel */
    private int count = 0;

    public RuleUsage(String field, String action, Map<String, Object> condition) {
        this.field = field;
        this.action = action;
        this.condition = condition;
    }

    /** Erhöht den internen Zähler um eins */
    public void increment() {
        count++;
    }
}
