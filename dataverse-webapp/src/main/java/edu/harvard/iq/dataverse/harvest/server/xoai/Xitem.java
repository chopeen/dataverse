/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server.xoai;

import com.lyncode.xoai.dataprovider.model.Item;
import com.lyncode.xoai.dataprovider.model.Set;
import com.lyncode.xoai.model.oaipmh.About;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.harvest.OAIRecord;
import edu.harvard.iq.dataverse.util.StringUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * @author Leonid Andreev
 * <p>
 * This is an implemention of an Lyncode XOAI Item;
 * You can think of it as an XOAI Item wrapper around the
 * Dataverse OAIRecord entity.
 */
public class Xitem implements Item {

    public Xitem(OAIRecord oaiRecord) {
        super();
        this.oaiRecord = oaiRecord;
        oaisets = new ArrayList<>();
        if (!StringUtil.isEmpty(oaiRecord.getSetName())) {
            oaisets.add(new Set(oaiRecord.getSetName()));
        }
    }

    private OAIRecord oaiRecord;

    public OAIRecord getOaiRecord() {
        return oaiRecord;
    }

    public void setOaiRecord(OAIRecord oaiRecord) {
        this.oaiRecord = oaiRecord;
    }

    private Dataset dataset;

    public Dataset getDataset() {
        return dataset;
    }

    public Xitem withDataset(Dataset dataset) {
        this.dataset = dataset;
        return this;
    }

    @Override
    public List<About> getAbout() {
        return null;
    }

    @Override
    public Xmetadata getMetadata() {
        return new Xmetadata(null);
    }

    @Override
    public String getIdentifier() {
        return oaiRecord.getGlobalId();
    }

    @Override
    public Date getDatestamp() {
        return oaiRecord.getLastUpdateTime();
    }

    private List<Set> oaisets;

    @Override
    public List<Set> getSets() {

        return oaisets;

    }

    @Override
    public boolean isDeleted() {
        return oaiRecord.isRemoved();
    }

}
