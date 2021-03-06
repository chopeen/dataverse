/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.persistence.dataset;


/**
 * @author skraffmiller
 */

public class DatasetRelPublication {

    /**
     * The "text" is the citation of the related publication.
     */
    private String text;
    private String idType;
    private String idNumber;
    private String url;

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isEmpty() {
        return ((text == null || text.trim().equals(""))
                && (idType == null || idType.trim().equals(""))
                && (idNumber == null || idNumber.trim().equals(""))
                && (url == null || url.trim().equals("")));
    }

}
