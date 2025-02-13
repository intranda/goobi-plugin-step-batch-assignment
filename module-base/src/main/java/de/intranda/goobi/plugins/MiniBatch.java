package de.intranda.goobi.plugins;

import java.util.List;

import org.goobi.beans.Processproperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MiniBatch {
    private Integer batchId;
    private String batchName;
    private int numberOfProcesses;
    private List<Processproperty> properties;
}
