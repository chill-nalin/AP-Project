package edu.univ.erp.api.types;

import java.util.Comparator;

// Record to display Section information nicely in JComboBoxes.
public record SectionDisplay(int sectionId, String displayText) {
    @Override
    public String toString() {
        return displayText;
    }

    // Comparator for sorting sections by course code found in displayText
    public static Comparator<SectionDisplay> CODE_COMPARATOR = Comparator.comparing(sd -> {
        String text = sd.displayText();
        int firstSpace = text.indexOf(' ');
        return (firstSpace > 0) ? text.substring(0, firstSpace) : text;
    });
}