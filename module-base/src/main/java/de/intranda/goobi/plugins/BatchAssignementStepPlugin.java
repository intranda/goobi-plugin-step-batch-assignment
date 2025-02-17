package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.goobi.beans.Batch;
import org.goobi.beans.Institution;
import org.goobi.beans.JournalEntry;
import org.goobi.beans.JournalEntry.EntryType;
import org.goobi.beans.Process;
import org.goobi.beans.Processproperty;
import org.goobi.beans.Step;
import org.goobi.beans.User;
import org.goobi.managedbeans.LoginBean;
import org.goobi.managedbeans.StepBean;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.flow.statistics.hibernate.FilterHelper;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;
import org.goobi.production.properties.ProcessProperty;
import org.goobi.production.properties.PropertyParser;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.forms.NavigationForm;
import de.sub.goobi.helper.CloseStepHelper;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.persistence.managers.JournalManager;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.PropertyManager;
import io.goobi.workflow.xslt.XsltToPdf;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class BatchAssignementStepPlugin implements IStepPluginVersion2 {

    private static final long serialVersionUID = -1178690277971117431L;
    @Getter
    private String title = "intranda_step_batch_assignement";
    @Getter
    private Step step;
    private String returnPath;
    @Getter
    private List<MiniBatch> batches;

    @Getter
    @Setter
    private MiniBatch batch;

    @Getter
    @Setter
    private String batchNewTitle;
    private String batchWaitStep;
    private List<String> propertyNames;
    @Getter
    private List<ProcessProperty> properties;

    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;

        // read parameters from correct block in configuration file
        SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig(title, step);
        batchWaitStep = myconfig.getString("batchWaitStep");
        properties = new ArrayList<>();
        propertyNames = Arrays.asList(myconfig.getStringArray("property"));

        // first load the property configuration
        List<ProcessProperty> plist = PropertyParser.getInstance().getPropertiesForProcess(step.getProzess());
        for (ProcessProperty pt : plist) {
            if (propertyNames.contains(pt.getName())) {
                properties.add(pt);
            }
        }

        // get a list of all open batches
        collectAvailableBatches();

        log.info("BatchAssignement step plugin initialized");
    }

    /**
     * request all Batches which are currently available for the assignement
     */
    private void collectAvailableBatches() {
        List<Batch> allBatches = ProcessManager
                .getBatchesWithFilter(FilterHelper.criteriaBuilder("\"-stepdone:" + batchWaitStep + "\"", false, null, null, null, true, false), 0,
                        20, null);
        batches = new ArrayList<>();
        for (Batch b : allBatches) {
            MiniBatch mb = new MiniBatch();
            mb.setBatchId(b.getBatchId());
            mb.setBatchName(b.getBatchName());
            mb.setProperties(new ArrayList<>());

            // request the currently assigned processes
            List<Process> processes = getProcessesOfBatch(b.getBatchId());
            mb.setNumberOfProcesses(processes.size());

            // get desired properties of the first process in existing batch
            if (!processes.isEmpty()) {
                Process p = processes.get(0);
                List<ProcessProperty> plist = PropertyParser.getInstance().getPropertiesForProcess(p);
                for (ProcessProperty prop : plist) {
                    if (propertyNames.contains(prop.getName())) {
                        mb.getProperties().add(prop);
                    }
                }

            }
            batches.add(mb);
        }
    }

    /**
     * get a list of all processes of a given batch
     *
     * @param id
     * @return
     */
    private List<Process> getProcessesOfBatch(int id) {
        StringBuilder filter = new StringBuilder().append(FilterHelper.criteriaBuilder("", false, null, null, null, true, false));
        filter.append(" AND istTemplate = false AND batchID = ").append(id);
        Institution inst = null;
        User user = Helper.getCurrentUser();
        if (user != null && !user.isSuperAdmin()) {
            inst = user.getInstitution();
        }
        return ProcessManager.getProcesses("prozesse.titel", filter.toString(), 0, ConfigurationHelper.getInstance().getBatchMaxSize(), inst);
    }

    /**
     * assign a selected batch
     */
    public void assignToExistingBatch() {
        Process p = step.getProzess();

        // copy properies from first process in batch
        List<Process> processes = getProcessesOfBatch(batch.getBatchId());
        if (!processes.isEmpty()) {
            Process firstProcess = processes.get(0);

            for (Processproperty prop : firstProcess.getEigenschaften()) {
                if (propertyNames.contains(prop.getTitel())) {

                    // try to set the existing property to the same value
                    boolean found = false;
                    for (Processproperty myprop : p.getEigenschaften()) {
                        if (myprop.getTitel().equals(prop.getTitel())) {
                            found = true;
                            myprop.setWert(prop.getWert());
                            PropertyManager.saveProcessProperty(myprop);
                            break;
                        }
                    }

                    // if property could not be found add it here
                    if (!found) {
                        Processproperty newProp = new Processproperty();
                        newProp.setTitel(prop.getTitel());
                        newProp.setWert(prop.getWert());
                        newProp.setProzess(p);
                        PropertyManager.saveProcessProperty(newProp);
                    }
                }
            }
        }

        // assign to the same batch
        Batch b = ProcessManager.getBatchById(batch.getBatchId());
        p.setBatch(b);
        ProcessManager.saveProcessInformation(p);

        // add a log entry
        LoginBean loginForm = Helper.getLoginBean();
        JournalEntry logEntry = new JournalEntry(step.getProzess().getId(), new Date(), loginForm.getMyBenutzer().getNachVorname(), LogType.DEBUG,
                "added process to batch " + batch.getBatchId(), EntryType.PROCESS);
        JournalManager.saveJournalEntry(logEntry);
        collectAvailableBatches();
    }

    /**
     * create a new batch and assign the current process to it
     */
    public void assignToNewBatch() {
        // create a new batch
        Batch newBatch = new Batch();
        newBatch.setBatchName(batchNewTitle);
        step.getProzess().setBatch(newBatch);
        for (ProcessProperty pp : properties) {
            if (pp.getProzesseigenschaft() == null) {
                Processproperty pe = new Processproperty();
                pe.setProzess(step.getProzess());
                pp.setProzesseigenschaft(pe);
                step.getProzess().getEigenschaften().add(pe);
                pp.transfer();
            } else {
                for (Processproperty pe : step.getProzess().getEigenschaften()) {
                    if (pe.getTitel().equals(pp.getName())) {
                        pe.setWert(pp.getValue());
                    }
                }
            }
        }
        ProcessManager.saveProcessInformation(step.getProzess());

        // add a log entry
        LoginBean loginForm = Helper.getLoginBean();
        JournalEntry logEntry = new JournalEntry(step.getProzess().getId(), new Date(), loginForm.getMyBenutzer().getNachVorname(), LogType.DEBUG,
                "added process to batch " + newBatch.getBatchId(), EntryType.PROCESS);
        JournalManager.saveJournalEntry(logEntry);
        collectAvailableBatches();

        // switch ui back to first tab
        NavigationForm nf = Helper.getBeanByClass(NavigationForm.class);
        nf.getUiStatus().put("batchassign", "tab1");

    }

    /**
     * lock this batcch
     */
    public String lockBatch() {
        LoginBean loginForm = Helper.getLoginBean();
        // run through all processes of current batch
        List<Process> processes = getProcessesOfBatch(step.getProzess().getBatch().getBatchId());
        for (Process process : processes) {
            // run through all steps of process to finish the batch-wait-step
            for (Step other : process.getSchritteList()) {
                if (other.getTitel().equals(batchWaitStep)) {
                    CloseStepHelper.closeStep(other, loginForm.getMyBenutzer());
                    break;
                }
            }
        }

        // finish current step
        StepBean sb = Helper.getBeanByClass(StepBean.class);
        return sb.SchrittDurchBenutzerAbschliessen();
    }

    /**
     * generate Batch Docket
     */
    public void generateBatchDocket() {
        Institution inst = null;
        User user = Helper.getCurrentUser();
        if (user != null && !user.isSuperAdmin()) {
            // limit result to institution of current user
            inst = user.getInstitution();
        }

        if (log.isDebugEnabled()) {
            log.debug("generate docket for process list");
        }
        String rootpath = ConfigurationHelper.getInstance().getXsltFolder();
        Path xsltfile = Paths.get(rootpath, "docket_multipage.xsl");
        FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
        List<Process> docket = ProcessManager.getProcesses(null, " istTemplate = false AND batchID = " + step.getProzess().getBatch().getBatchId(), 0,
                ConfigurationHelper.getInstance().getBatchMaxSize(), inst);

        if (!docket.isEmpty() && !facesContext.getResponseComplete()) {
            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
            String fileName = "batch_" + step.getProzess().getBatch().getBatchId() + ".pdf";
            ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
            String contentType = servletContext.getMimeType(fileName);
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");

            try {
                ServletOutputStream out = response.getOutputStream();
                XsltToPdf ern = new XsltToPdf();
                ern.startExport(docket, out, xsltfile.toString());
                out.flush();
            } catch (IOException e) {
                log.error("IOException while exporting run note", e);
            }

            facesContext.responseComplete();
        }
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
        return null; //NOSONAR
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
