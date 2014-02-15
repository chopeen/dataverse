/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta.DTAFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta.DTAFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.metadataextraction.FileMetadataExtractor;
import edu.harvard.iq.dataverse.ingest.metadataextraction.impl.plugins.fits.FITSFileMetadataExtractor;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named("DatasetPage")
public class DatasetPage implements java.io.Serializable {

    public enum EditMode {CREATE, INFO, FILE, METADATA};
    
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataFileServiceBean datafileService;
    @EJB
    DataverseServiceBean dataverseService;    
   @EJB
    TemplateServiceBean templateService;

    private Dataset dataset = new Dataset();
    private EditMode editMode;
    private Long ownerId;
    private int selectedTabIndex;
    private Map<UploadedFile,DataFile> newFiles = new HashMap();
    private Map<MetadataBlock, Object> metadataBlockValues = new HashMap();
    private DatasetVersion editVersion = new DatasetVersion();   
    private List<DatasetField> citationFields = new ArrayList();
    private List<DatasetField> otherMetadataFields = new ArrayList();
    private List<DatasetFieldValue> citationValues = new ArrayList();

    public List<DatasetFieldValue> getCitationValues() {
        return citationValues;
    }

    public void setCitationValues(List<DatasetFieldValue> citationValues) {
        this.citationValues = citationValues;
    }
    private List<DatasetFieldValue> otherMetadataValues = new ArrayList();    
    private List<DatasetFieldValue> editValues = new ArrayList();
    
    
    public List<DatasetFieldValue> getOtherMetadataValues() {
        return otherMetadataValues;
    }
    /*
    public Map<MetadataBlock, Object> getMetadataBlockValues() {
        return metadataBlockValues;
    }*/
    
    public List<Map.Entry<MetadataBlock, Object>> getMetadataBlockValues() {
    Set<Map.Entry<MetadataBlock, Object>> metadataSet = 
                     metadataBlockValues.entrySet();
    return new ArrayList<>(metadataSet);
}


    public void setOtherMetadataValues(List<DatasetFieldValue> otherMetadataValues) {
        this.otherMetadataValues = otherMetadataValues;
    }
    
    public Dataset getDataset() {
        return dataset;
    }
    
    public List<DatasetFieldValue> getEditValues() {
        return editValues;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
    }

    public DatasetVersion getEditVersion() {
        return editVersion;
    }

    public void setEditVersion(DatasetVersion editVersion) {
        this.editVersion = editVersion;
    }
    
    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public void setSelectedTabIndex(int selectedTabIndex) {
        this.selectedTabIndex = selectedTabIndex;
    }
    
    public void init() {
        if (dataset.getId() != null) { // view mode for a dataset           
            dataset = datasetService.find(dataset.getId());
            editVersion = dataset.getLatestVersion();
            editVersion.setDatasetFieldValues(editVersion.initDatasetFieldValues()); 
            editValues = editVersion.getDatasetFieldValues();
            citationValues = extractValues(editValues, true);
            otherMetadataValues = extractValues(editValues, false);
            getMetadataValueBlocks(editValues);
            ownerId = dataset.getOwner().getId();
        } else if (ownerId != null) { // create mode for a new child dataset
            editMode = EditMode.CREATE;
            dataset.setOwner(dataverseService.find(ownerId));
            dataset.setVersions(new ArrayList());
            editVersion.setDataset(dataset);           
            editVersion.setFileMetadatas(new ArrayList());
            editVersion.setDatasetFieldValues(null);            
            editVersion.setVersionState(VersionState.DRAFT);
            editVersion.setDatasetFieldValues(editVersion.initDatasetFieldValues()); 
            editVersion.setVersionNumber(new Long(1));  
            editValues = editVersion.getDatasetFieldValues();
            citationValues = extractValues(editValues, true);
            otherMetadataValues = extractValues(editValues, false);
            getMetadataValueBlocks(editValues);
            dataset.getVersions().add(editVersion);
        } else {
            throw new RuntimeException("On Dataset page without id or ownerid."); // improve error handling
        }        
        setCitationFields(dataverseService.findCitationDatasetFieldsByDataverseId(ownerId));
        setOtherMetadataFields(dataverseService.findOtherMetadataDatasetFieldsByDataverseId(ownerId));       
    }
    
    public void edit(EditMode editMode) {
        this.editMode = editMode;
        if (editMode == EditMode.INFO) {
            editVersion = dataset.getEditVersion();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Edit Dataset Info", " - Edit your dataset info."));
        } else if (editMode == EditMode.FILE) {
            editVersion = dataset.getEditVersion();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Edit Dataset Files", " - Edit your dataset files. Tip: You can drag and drop your files from your desktop, directly into the upload widget."));
        } else if (editMode == EditMode.METADATA) {
            editVersion = dataset.getEditVersion();
            editVersion.setDatasetFieldValues(editVersion.initDatasetFieldValues()); 
            editVersion.setVersionNumber(new Long(1));  
            editValues = editVersion.getDatasetFieldValues();
            citationValues = extractValues(editValues, true);
            otherMetadataValues = extractValues(editValues, false);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Edit Dataset Metadata", " - Edit your dataset metadata."));
        }
    }
       
    public void save() {
        
        dataset.setOwner(dataverseService.find(ownerId));
        //TODO get real application-wide protocol/authority
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("5555");

        if (!(dataset.getVersions().get(0).getFileMetadatas() == null) && !dataset.getVersions().get(0).getFileMetadatas().isEmpty()) {
            int fmdIndex = 0;
            for (FileMetadata fmd : dataset.getVersions().get(0).getFileMetadatas()) {
                for (FileMetadata fmdTest : editVersion.getFileMetadatas()) {
                    if (fmd.equals(fmdTest)) {
                        dataset.getVersions().get(0).getFileMetadatas().get(fmdIndex).setDataFile(fmdTest.getDataFile());
                    }
                }
                fmdIndex++;
            }
        }        

        // save any new files
        for (UploadedFile uFile : newFiles.keySet()) {
            DataFile dFile = newFiles.get(uFile);
            try {
                boolean ingestedAsTabular = false;
                boolean metadataExtracted = false; 
                
                /* Make sure the dataset directory exists: */
                if (!Files.exists(dataset.getFileSystemDirectory())) {
                    /* Note that "createDirectories()" must be used - not 
                     * "createDirectory()", to make sure all the parent 
                     * directories that may not yet exist are created as well. 
                     */
                    Files.createDirectories(dataset.getFileSystemDirectory());
                }

                if (ingestableAsTabular(dFile)) {
                    
                    try {          
                        ingestedAsTabular = ingestAsTabular(uFile, dFile);
                    } catch (IOException iex) {
                        Logger.getLogger(DatasetPage.class.getName()).log(Level.SEVERE, null, iex);
                        ingestedAsTabular = false; 
                    }
                } else if (fileMetadataExtractable(dFile)) {
                    
                    try {
                        metadataExtracted = extractIndexableMetadata(uFile, dFile);
                    } catch(IOException mex) {
                        Logger.getLogger(DatasetPage.class.getName()).log(Level.SEVERE, "Caught exception trying to extract indexable metadata from file "+dFile.getName(), mex);
                    }
                    if (metadataExtracted) {
                        Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Successfully extracted indexable metadata from file " + dFile.getName());
                    } else {
                        Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Failed to extract indexable metadata from file " + dFile.getName());
                    }
                }

                if (!ingestedAsTabular) {
                    Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Will attempt to save the file as: " + dFile.getFileSystemLocation().toString());
                    Files.copy(uFile.getInputstream(), dFile.getFileSystemLocation(), StandardCopyOption.REPLACE_EXISTING);
                }
                
            } catch (IOException ex) {
                Logger.getLogger(DatasetPage.class.getName()).log(Level.SEVERE, null, ex);
                // TODO: 
                // discard the DataFile and disconnect it from the dataset object!
            }

        }
        
        dataset = datasetService.save(dataset);

        newFiles.clear();
        editMode = null;
    }

    public void cancel() {
        // reset values
        dataset = datasetService.find(dataset.getId());
        ownerId = dataset.getOwner().getId();
        newFiles.clear();
        editMode = null;
    }

    public void handleFileUpload(FileUploadEvent event) {
        UploadedFile uFile = event.getFile();
        DataFile dFile = new DataFile(uFile.getFileName(), uFile.getContentType());
        FileMetadata fmd = new FileMetadata();
        dFile.setOwner(dataset);
        fmd.setDataFile(dFile);
        dFile.getFileMetadatas().add(fmd);
        fmd.setLabel(dFile.getName());
        fmd.setCategory(dFile.getContentType());
        fmd.setDescription("add description");
        if (editVersion.getFileMetadatas() == null) {
            editVersion.setFileMetadatas(new ArrayList());
        }
        editVersion.getFileMetadatas().add(fmd);
        fmd.setDatasetVersion(editVersion);
        dataset.getFiles().add( dFile );
        newFiles.put(uFile, dFile);
    }
    
    public void addRow(ActionEvent ae) {       
        //      UIComponent dataTable = ae.getComponent().getParent().getParent().getParent();
        HtmlDataTable dataTable = (HtmlDataTable)ae.getComponent().getParent().getParent();
            DatasetAuthor newElem = new DatasetAuthor();
           // newElem.setMetadata(editVersion.getMetadata());
           // editVersion.getMetadata().getDatasetAuthors().add(dataTable.getRowIndex()+1,newElem);
            //JavascriptContext.addJavascriptCall(getFacesContext(),"initAddAuthorSync();");
                         
    }

                
    public DataModel getDatasetFieldsDataModel() {
        List values = new ArrayList();  
        int i = 0;
        for (DatasetFieldValue dsfv : editVersion.getDatasetFieldValues()){
            DatasetField datasetField = dsfv.getDatasetField();                      
                Object[] row = new Object[4];
                row[0] = datasetField;
                row[1] = getValuesDataModel(dsfv);
                row[2] = new Integer(i);
                row[3] = datasetField;                
                values.add(row);
                i++;
        }         
        return new ListDataModel(values);
    }
    
    private DataModel getValuesDataModel(DatasetFieldValue datasetFieldValue) {
        List values = new ArrayList();
                Object[] row = new Object[2];
                row[0] = datasetFieldValue;
                row[1] = datasetFieldValue.getDatasetField().getDatasetFieldValues(); // used by the remove method
                values.add(row);
        return new ListDataModel(values);
    }
    
    public String getTitle() {
        return datasetService.getDatasetVersionTitle(dataset.getLatestVersion());
    }

    public List<DatasetField> getCitationFields() {
        return citationFields;
    }

    public void setCitationFields(List<DatasetField> citationFields) {
        this.citationFields = citationFields;
    }

    public List<DatasetField> getOtherMetadataFields() {
        return otherMetadataFields;
    }

    public void setOtherMetadataFields(List<DatasetField> otherMetadataFields) {
        this.otherMetadataFields = otherMetadataFields;
    }
    
    private List<DatasetFieldValue> extractValues(List<DatasetFieldValue> inList, boolean citation){
        
        List retList = new ArrayList();
        for (DatasetFieldValue dsfv : inList){
            if (citation && dsfv.getDatasetField().getMetadataBlock().isShowOnCreate()) { 
                  retList.add(dsfv);                
            } else if (!citation && !dsfv.getDatasetField().getMetadataBlock().isShowOnCreate()) {
                  retList.add(dsfv);
            }
        }
        return retList;
    }
    
    private void getMetadataValueBlocks(List<DatasetFieldValue> inList){
        metadataBlockValues.clear();
        for(MetadataBlock mdb: dataset.getOwner().getMetadataBlocks()){
            List addList = new ArrayList();
            if(!mdb.isShowOnCreate()){
                for (DatasetFieldValue dsfv: inList){
                     if (dsfv.getDatasetField().getMetadataBlock().equals(mdb)){
                         addList.add(dsfv);
                     }
                }
                metadataBlockValues.put(mdb, addList);
            }
        }
            
     int a = 1;    
    }

    private boolean ingestableAsTabular(DataFile dataFile) {
        /* 
         * Eventually we'll be using some complex technology of identifying 
         * potentially ingestable file formats here, similar to what we had in 
         * v.3.*; for now - just a hardcoded list of filename extensions:
         *  -- L.A. 4.0alpha1
         */
        if (dataFile.getName() != null && dataFile.getName().endsWith(".dta")) {
            return true;
        }
        return false;
    } 
    
    private boolean fileMetadataExtractable(DataFile dataFile) {
        /* 
         * Eventually we'll be consulting the Ingest Service Provider Registry
         * to see if there is a plugin for this type of file;
         * for now - just a hardcoded list of filename extensions:
         *  -- L.A. 4.0alpha1
         */
        if (dataFile.getName() != null && dataFile.getName().endsWith(".fits")) {
            return true;
        }
        return false;
    }

    private boolean ingestAsTabular(UploadedFile uFile, DataFile dataFile) throws IOException {
        boolean ingestSuccessful = false;

        // Locate ingest plugin for the file format by looking
        // it up with the Ingest Service Provider Registry:
        
        //TabularDataFileReader ingestPlugin = IngestSP.getTabDataReaderByMIMEType(dFile.getContentType());
        TabularDataFileReader ingestPlugin = new DTAFileReader(new DTAFileReaderSpi());

        TabularDataIngest tabDataIngest = ingestPlugin.read(new BufferedInputStream(uFile.getInputstream()), null);

        if (tabDataIngest != null) {
            File tabFile = tabDataIngest.getTabDelimitedFile();

            if (tabDataIngest.getDataTable() != null
                    && tabFile != null
                    && tabFile.exists()) {

                Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Tabular data successfully ingested; DataTable with "
                        + tabDataIngest.getDataTable().getVarQuantity() + " variables produced.");

                Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Tab-delimited file produced: " + tabFile.getAbsolutePath());

                dataFile.setName(dataFile.getName().replaceAll("\\.dta$", ".tab"));
                Files.copy(Paths.get(tabFile.getAbsolutePath()), dataFile.getFileSystemLocation(), StandardCopyOption.REPLACE_EXISTING);
                
                dataFile.setDataTable(tabDataIngest.getDataTable());
                tabDataIngest.getDataTable().setDataFile(dataFile);

                ingestSuccessful = true;
            }
        }
        return ingestSuccessful;
    }
    
    private boolean extractIndexableMetadata(UploadedFile uFile, DataFile dataFile) throws IOException {
        boolean ingestSuccessful = false; 
        
        // Locate metadata extraction plugin for the file format by looking
        // it up with the Ingest Service Provider Registry:
        
        //FileMetadataExtractor extractorPlugin = IngestSP.getMetadataExtractorByMIMEType(dfile.getContentType());
        FileMetadataExtractor extractorPlugin = new FITSFileMetadataExtractor();
        
        Map<String, Set<String>> extractedMetadata = extractorPlugin.ingest(new BufferedInputStream(uFile.getInputstream()));
        
        // Store the fields and values we've gathered for safe-keeping:
        
        // ...
        
        ingestSuccessful = true;
        
        return ingestSuccessful;
    }
}
