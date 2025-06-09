package de.intranda.goobi.plugins;

import java.io.Serializable;
import java.util.List;

import org.goobi.production.properties.DisplayProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MiniBatch implements Serializable {
    private static final long serialVersionUID = -7474189753500106239L;
    private Integer batchId;
    private String batchName;
    private int numberOfProcesses;
    private List<DisplayProperty> properties;
}
