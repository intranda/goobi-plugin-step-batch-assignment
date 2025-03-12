package de.intranda.goobi.plugins;

import lombok.Data;

@Data
public class PropertyDefinition {
    private String name;
    private String vocabularyFilterField;
    private String vocabularyFilterValue;
}
