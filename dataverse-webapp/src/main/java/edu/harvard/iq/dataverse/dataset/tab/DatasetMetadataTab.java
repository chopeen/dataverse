package edu.harvard.iq.dataverse.dataset.tab;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.dataset.DatasetFieldsInitializer;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ViewScoped
@Named("DatasetMetadataTab")
public class DatasetMetadataTab implements Serializable {

    private PermissionsWrapper permissionsWrapper;
    private DataverseRequestServiceBean dvRequestService;
    private ExportService exportService;
    private SystemConfig systemConfig;
    private DatasetFieldsInitializer datasetFieldsInitializer;

    private Dataset dataset;
    private boolean isDatasetLocked;
    private Map<MetadataBlock, List<DatasetFieldsByType>> metadataBlocks;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated /*JEE requirement */
    DatasetMetadataTab() {
    }

    @Inject
    public DatasetMetadataTab(PermissionsWrapper permissionsWrapper,
                              DataverseRequestServiceBean dvRequestService,
                              ExportService exportService,
                              SystemConfig systemConfig,
                              DatasetFieldsInitializer datasetVersionUI) {
        this.permissionsWrapper = permissionsWrapper;
        this.dvRequestService = dvRequestService;
        this.exportService = exportService;
        this.systemConfig = systemConfig;
        this.datasetFieldsInitializer = datasetVersionUI;
    }

    // -------------------- GETTERS --------------------

    public Dataset getDataset() {
        return dataset;
    }

    public boolean isDatasetLocked() {
        return isDatasetLocked;
    }

    /**
     * Metadata blocks meant for view.
     */
    public Map<MetadataBlock, List<DatasetFieldsByType>> getMetadataBlocks() {
        return metadataBlocks;
    }

    // -------------------- LOGIC --------------------

    public void init(DatasetVersion datasetVersion,
                     boolean isDatasetLocked) {
        this.dataset = datasetVersion.getDataset();
        this.isDatasetLocked = isDatasetLocked;
        
        List<DatasetField> datasetFields = datasetFieldsInitializer.prepareDatasetFieldsForView(datasetVersion.getDatasetFields());
        this.metadataBlocks = DatasetFieldUtil.groupByBlockAndType(datasetFields);
    }

    public boolean canUpdateDataset() {
        return permissionsWrapper.canUpdateDataset(dvRequestService.getDataverseRequest(), this.dataset);
    }

    /**
     * Updates the dataset lock state.
     */
    public boolean updateDatasetLockState(boolean isDatasetLocked) {
        this.isDatasetLocked = isDatasetLocked;
        return isDatasetLocked;
    }

    /**
     * Extracts exporters display name and redirect url.
     */
    public List<Tuple2<String, String>> getExportersDisplayNameAndURL() {
        List<Tuple2<String, String>> exportersInfo = new ArrayList<>();

        exportService.getAllExporters().values().stream()
                .filter(Exporter::isAvailableToUsers)
                .forEach(exporter -> exportersInfo.add(Tuple.of(exporter.getDisplayName(), createExporterURL(exporter, systemConfig.getDataverseSiteUrl()))));

        return exportersInfo;
    }

    // -------------------- PRIVATE --------------------

    private String createExporterURL(Exporter exporter, String myHostURL) {
        return myHostURL + "/api/datasets/export?exporter=" + exporter.getProviderName() + "&persistentId=" + dataset.getGlobalIdString();
    }
}
