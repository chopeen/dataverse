/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 * @author skraffmiller
 */
@FacesConverter("dataverseConverter")
public class DataverseConverter implements Converter {


    @EJB
    DataverseDao dataverseDao;

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String submittedValue) {
        return dataverseDao.find(new Long(submittedValue));
        //return dataverseService.findByAlias(submittedValue);
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        if (value == null || value.equals("")) {
            return "";
        } else {
            return ((Dataverse) value).getId().toString();
            //return ((Dataverse) value).getAlias();
        }
    }
}
