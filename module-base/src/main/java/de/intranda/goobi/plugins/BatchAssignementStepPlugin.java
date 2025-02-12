package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.Date;

/**
 * This file is part of a plugin for Goobi - a Workflow tool for the support of mass digitization.
 *
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.goobi.beans.Batch;
import org.goobi.beans.JournalEntry;
import org.goobi.beans.JournalEntry.EntryType;
import org.goobi.beans.Step;
import org.goobi.managedbeans.LoginBean;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.forms.NavigationForm;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.persistence.managers.JournalManager;
import de.sub.goobi.persistence.managers.ProcessManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class BatchAssignementStepPlugin implements IStepPluginVersion2 {

    @Getter
    private String title = "intranda_step_batch_assignement";
    @Getter
    private Step step;
    private String returnPath;
    @Getter
    private List<Batch> batches;

    @Getter
    @Setter
    private Batch batch;

    @Getter
    @Setter
    private String batchNewTitle;

    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;

        // read parameters from correct block in configuration file
        SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig(title, step);
        //        value = myconfig.getString("value", "default value");
        //        allowTaskFinishButtons = myconfig.getBoolean("allowTaskFinishButtons", false);

        // get a list of all open batches
        collectAvailableBatches();

        log.info("BatchAssignement step plugin initialized");
    }

    /**
     * request all Batches which are currently available for the assignement
     */
    private void collectAvailableBatches() {
        List<Batch> allBatches = ProcessManager.getBatches(ConfigurationHelper.getInstance().getBatchMaxSize());
        batches = new ArrayList<>();
        for (Batch b : allBatches) {
            batches.add(b);
        }
    }

    /**
     * assign a selected batch
     */
    public void assignToExistingBatch() {
        System.out.println("Batch title: " + batch.getBatchName());
        step.getProzess().setBatch(batch);
        ProcessManager.saveProcessInformation(step.getProzess());

        // add a log entry
        LoginBean loginForm = Helper.getLoginBean();
        JournalEntry logEntry = new JournalEntry(step.getProzess().getId(), new Date(), loginForm.getMyBenutzer().getNachVorname(), LogType.DEBUG,
                "added process to batch " + batch.getBatchId(), EntryType.PROCESS);
        JournalManager.saveJournalEntry(logEntry);
    }

    /**
     * create a new batch and assign the current process to it
     */
    public void assignToNewBatch() {
        System.out.println("Batch title: " + batchNewTitle);

        // create a new batch
        Batch batch = new Batch();
        batch.setBatchName(batchNewTitle);
        step.getProzess().setBatch(batch);
        ProcessManager.saveProcessInformation(step.getProzess());

        // add a log entry
        LoginBean loginForm = Helper.getLoginBean();
        JournalEntry logEntry = new JournalEntry(step.getProzess().getId(), new Date(), loginForm.getMyBenutzer().getNachVorname(), LogType.DEBUG,
                "added process to batch " + batch.getBatchId(), EntryType.PROCESS);
        JournalManager.saveJournalEntry(logEntry);
        collectAvailableBatches();

        // switch ui back to first tab
        NavigationForm nf = Helper.getBeanByClass(NavigationForm.class);
        nf.getUiStatus().put("batchassign", "tab1");

    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.PART;
    }

    @Override
    public String getPagePath() {
        return "/uii/plugin_step_batch_assignement.xhtml";
    }

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    @Override
    public String cancel() {
        return "/uii" + returnPath;
    }

    @Override
    public String finish() {
        return "/uii" + returnPath;
    }

    @Override
    public int getInterfaceVersion() {
        return 0;
    }

    @Override
    public HashMap<String, StepReturnValue> validate() {
        return null;
    }

    @Override
    public boolean execute() {
        PluginReturnValue ret = run();
        return ret != PluginReturnValue.ERROR;
    }

    @Override
    public PluginReturnValue run() {
        boolean successful = true;
        // your logic goes here
        log.info("BatchAssignement step plugin executed");
        if (!successful) {
            return PluginReturnValue.ERROR;
        }
        return PluginReturnValue.FINISH;
    }
}
