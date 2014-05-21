
package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author skraffmiller
 */
public class DatasetVersionDifference {
    private DatasetVersion newVersion;
    private DatasetVersion originalVersion;
    private List<List>addedDataAll = new ArrayList();
    private List<FileMetadata> addedFiles = new ArrayList();
    private List<FileMetadata> removedFiles = new ArrayList();
    private List<FileMetadata> changedFileMetadata = new ArrayList();
    private List<DatasetField> addedData = new ArrayList();
    private List<DatasetField> removedData = new ArrayList();
    private List<DatasetField> changedData = new ArrayList();
    private List<DatasetField> addedSummaryData = new ArrayList();
    private List<DatasetField> removedSummaryData = new ArrayList();
    private List<DatasetField> changedSummaryData = new ArrayList();

    public DatasetVersionDifference(DatasetVersion newVersion, DatasetVersion originalVersion) {
        
        setOriginalVersion(originalVersion);
        setNewVersion(newVersion);
        //Compare Data
        for (DatasetField dsfo : originalVersion.getDatasetFields()) {
            boolean deleted = true;
            for (DatasetField dsfn : newVersion.getDatasetFields()) {
                if (dsfo.getDatasetFieldType().equals(dsfn.getDatasetFieldType())) {
                    deleted = false;
                    if (dsfo.getDatasetFieldType().isPrimitive()) {
                        if (!dsfo.getDatasetFieldType().getFieldType().equals("email") && !compareValuesPrimitive(dsfo, dsfn)) {
                            if (!dsfo.getDatasetFieldType().isDisplayOnCreate()) {
                                changedData.add(dsfo);
                                changedData.add(dsfn);
                            } else {
                                changedSummaryData.add(dsfo);
                                changedSummaryData.add(dsfn);
                            }
                        }
                    } else {
                        if (!compareValuesCompound(dsfo, dsfn)) {
                            if (!dsfo.getDatasetFieldType().isDisplayOnCreate()) {
                                changedData.add(dsfo);
                                changedData.add(dsfn);
                            } else {
                                changedSummaryData.add(dsfo);
                                changedSummaryData.add(dsfn);
                            }
                        }
                    }
                    break;
                }
            }
            if (deleted) {
                if (dsfo.getDatasetFieldType().isDisplayOnCreate()) {
                    removedSummaryData.add(dsfo);
                } else {
                    removedData.add(dsfo);
                }
            }
        }
        for (DatasetField dsfn : newVersion.getDatasetFields()) {
            boolean added = true;
            for (DatasetField dsfo : originalVersion.getDatasetFields()) {
                if (dsfo.getDatasetFieldType().equals(dsfn.getDatasetFieldType())) {
                    added = false;
                    break;
                }
            }
            if (added) {
                if (dsfn.getDatasetFieldType().isDisplayOnCreate()) {
                    addedSummaryData.add(dsfn);
                } else {
                    addedData.add(dsfn);
                }
            }
        }

        for (FileMetadata fmdo : originalVersion.getFileMetadatas()) {
            boolean deleted = true;
            for (FileMetadata fmdn : newVersion.getFileMetadatas()) {
                if (fmdo.getDataFile().equals(fmdn.getDataFile())) {
                    deleted = false;
                    if (!compareFileMetadatas(fmdo, fmdn)) {
                        changedFileMetadata.add(fmdo);
                        changedFileMetadata.add(fmdn);
                    }
                    break;
                }
            }
            if (deleted) {
                removedFiles.add(fmdo);
            }
        }

        for (FileMetadata fmdn : newVersion.getFileMetadatas()) {
            boolean added = true;
            for (FileMetadata fmdo : originalVersion.getFileMetadatas()) {
                if (fmdo.getDataFile().equals(fmdn.getDataFile())) {
                    added = false;
                    break;
                }
            }
            if (added) {
                addedFiles.add(fmdn);
            }
        }

    }



    private boolean compareFileMetadatas(FileMetadata fmdo, FileMetadata fmdn) {

        if (!(fmdo.getDescription().equals(fmdn.getDescription()))) {
            return false;
        }
        if (!(fmdo.getCategory().equals(fmdn.getCategory()))) {
            return false;
        }
        if (!(fmdo.getLabel().equals(fmdn.getLabel()))) {
            return false;
        }

        return true;
    }

    private boolean compareValuesPrimitive(DatasetField originalField, DatasetField newField) {

        String originalValue = originalField.getDisplayValue();
        String newValue = newField.getDisplayValue();
        if (originalValue == null || newValue == null) {
            return true;
        }
        return originalValue.equals(newValue);

    }

    private boolean compareValuesCompound(DatasetField originalField, DatasetField newField) {
        String originalValue = "";
        String newValue = "";
        for (DatasetFieldCompoundValue datasetFieldCompoundValueNew : newField.getDatasetFieldCompoundValues()) {
            for (DatasetField dsfn : datasetFieldCompoundValueNew.getChildDatasetFields()) {
                newValue += dsfn.getDisplayValue() + ", ";
            }
            newValue += ";";
        }
        for (DatasetFieldCompoundValue datasetFieldCompoundValueOriginal : originalField.getDatasetFieldCompoundValues()) {
            for (DatasetField dsfn : datasetFieldCompoundValueOriginal.getChildDatasetFields()) {
                originalValue += dsfn.getDisplayValue() + ", ";
            }
            originalValue += ";";
        }

        if (originalValue == null || newValue == null) {
            return true;
        }
        return originalValue.equals(newValue);

    }
    
    public List<String> getNotes(){
       String retString = "";
       List retList = new ArrayList();
        if (addedSummaryData.size() > 0) {
            retString = " Summary data added ";
            int count = 0;
            for (DatasetField dsf : addedSummaryData) {
                if (count == 0) {
                    retString += ":";
                } else {
                    retString += ";";
                }
                retString += " " + dsf.getDatasetFieldType().getDisplayName();
                count++;
            }
            retList.add(retString);
        }
        if (removedSummaryData.size() > 0) {
            retString = " Summary data deleted ";
            int count = 0;
            for (DatasetField dsf : removedSummaryData) {
                if (count == 0) {
                    retString += ":";
                } else {
                    retString += ";";
                }
                retString += " " + dsf.getDatasetFieldType().getDisplayName();
                count++;
            }
            retList.add(retString);
        }
        if (changedSummaryData.size() > 0) {
            retString = " Summary data changed: ";
            for (Iterator<DatasetField> iter = changedSummaryData.iterator(); iter.hasNext();) {
                DatasetField dsf = iter.next();
                retString += " " + dsf.getDatasetFieldType().getDisplayName() + "; ";
                DatasetField dsfn = iter.next();
            }
            retList.add(retString);
        }
        /*
             for (Iterator<DatasetField> iter = changedSummaryData.iterator(); iter.hasNext();) {
                DatasetField dsf = iter.next();
                if (dsf.getDatasetFieldType().isPrimitive()) {
                    retString += " " + dsf.getDatasetFieldType().getDisplayName() + " from " + dsf.getDisplayValue();
                    DatasetField dsfn = iter.next();
                    retString += " to " + dsfn.getDisplayValue() + "; ";
                } else {
                    retString += " " + dsf.getDatasetFieldType().getDisplayName() + " from " + dsf.getCompoundDisplayValue();
                    DatasetField dsfn = iter.next();
                    retString += " to " + dsfn.getCompoundDisplayValue() + "; ";
                }

            }*/
       
        if (addedData.size() > 0) {
            retString = " Number of Additional Data added: " + addedData.size();
            retList.add(retString);
        }
        if (removedData.size() > 0) {
            retString = " Number of Additional Data deleted: " + removedData.size() ;
            retList.add(retString);
        }
        if (changedData.size() > 0) {
            retString = " Number of Additional Data changed: " + changedData.size()/2 ;
            retList.add(retString);
        }
        if (addedFiles.size() > 0) {
            retString = " Number of Files added: " + addedFiles.size() ;
            retList.add(retString);
        }
        if (removedFiles.size() > 0) {
            retString = " Number of Files deleted: " + removedFiles.size() ;
            retList.add(retString);
        }
        if (changedFileMetadata.size() > 0) {
            retString = " Number of File Metadata changed: " + changedFileMetadata.size()/2 ;
            retList.add(retString);
        }

       return retList;
    }

    public String getNote() {
        String retString = "";
        if (addedSummaryData.size() > 0) {
            retString += " Summary data added ";
            int count = 0;
            for (DatasetField dsf : addedSummaryData) {
                if (count == 0) {
                    retString += ":";
                } else {
                    retString += ";";
                }
                retString += " " + dsf.getDatasetFieldType().getDisplayName();
                count++;
            }
        }
        if (removedSummaryData.size() > 0) {
            retString += " Summary data deleted ";
            int count = 0;
            for (DatasetField dsf : removedSummaryData) {
                if (count == 0) {
                    retString += ":";
                } else {
                    retString += ";";
                }
                retString += " " + dsf.getDatasetFieldType().getDisplayName();
                count++;
            }
        }
        if (changedSummaryData.size() > 0) {
            retString += " Summary data changed: ";
            for (Iterator<DatasetField> iter = changedSummaryData.iterator(); iter.hasNext();) {
                DatasetField dsf = iter.next();
                retString += " " + dsf.getDatasetFieldType().getDisplayName() + "; ";
                DatasetField dsfn = iter.next();
            }
        }
        /*
             for (Iterator<DatasetField> iter = changedSummaryData.iterator(); iter.hasNext();) {
                DatasetField dsf = iter.next();
                if (dsf.getDatasetFieldType().isPrimitive()) {
                    retString += " " + dsf.getDatasetFieldType().getDisplayName() + " from " + dsf.getDisplayValue();
                    DatasetField dsfn = iter.next();
                    retString += " to " + dsfn.getDisplayValue() + "; ";
                } else {
                    retString += " " + dsf.getDatasetFieldType().getDisplayName() + " from " + dsf.getCompoundDisplayValue();
                    DatasetField dsfn = iter.next();
                    retString += " to " + dsfn.getCompoundDisplayValue() + "; ";
                }

            }*/
       
        if (addedData.size() > 0) {
            retString += " Number of Additional Data added " + addedData.size()+ ";";
        }
        if (removedData.size() > 0) {
            retString += " Number of Additional Data deleted " + removedData.size() + ";";
        }
        if (changedData.size() > 0) {
            retString += " Number of Additional Data changed " + changedData.size()/2 + ";";
        }
        if (addedFiles.size() > 0) {
            retString += " Number of Files added " + addedFiles.size() +  ";";
        }
        if (removedFiles.size() > 0) {
            retString += " Number of Files deleted " + removedFiles.size()  +  ";";
        }
        if (changedFileMetadata.size() > 0) {
            retString += " Number of File Metadata changed " + changedFileMetadata.size()/2  +  ";";
        }

        return retString;
    }
        public List<FileMetadata> getAddedFiles() {
        return addedFiles;
    }

    public void setAddedFiles(List<FileMetadata> addedFiles) {
        this.addedFiles = addedFiles;
    }

    public List<FileMetadata> getRemovedFiles() {
        return removedFiles;
    }

    public void setRemovedFiles(List<FileMetadata> removedFiles) {
        this.removedFiles = removedFiles;
    }

    public List<DatasetField> getAddedData() {
        return addedData;
    }

    public void setAddedData(List<DatasetField> addedData) {
        this.addedData = addedData;
    }

    public List<DatasetField> getRemovedData() {
        return removedData;
    }

    public void setRemovedData(List<DatasetField> removedData) {
        this.removedData = removedData;
    }

    public List<DatasetField> getChangedData() {
        return changedData;
    }

    public void setChangedData(List<DatasetField> changedData) {
        this.changedData = changedData;
    }

    public List<DatasetField> getAddedSummaryData() {
        return addedSummaryData;
    }

    public void setAddedSummaryData(List<DatasetField> addedSummaryData) {
        this.addedSummaryData = addedSummaryData;
    }

    public List<DatasetField> getRemovedSummaryData() {
        return removedSummaryData;
    }

    public void setRemovedSummaryData(List<DatasetField> removedSummaryData) {
        this.removedSummaryData = removedSummaryData;
    }

    public List<DatasetField> getChangedSummaryData() {
        return changedSummaryData;
    }

    public void setChangedSummaryData(List<DatasetField> changedSummaryData) {
        this.changedSummaryData = changedSummaryData;
    }
    
    public DatasetVersion getNewVersion() {
        return newVersion;
    }

    public void setNewVersion(DatasetVersion newVersion) {
        this.newVersion = newVersion;
    }

    public DatasetVersion getOriginalVersion() {
        return originalVersion;
    }

    public void setOriginalVersion(DatasetVersion originalVersion) {
        this.originalVersion = originalVersion;
    }

    public List<FileMetadata> getChangedFileMetadata() {
        return changedFileMetadata;
    }

    public void setChangedFileMetadata(List<FileMetadata> changedFileMetadata) {
        this.changedFileMetadata = changedFileMetadata;
    }

}
