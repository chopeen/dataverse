package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.PermissionServiceBean.StaticPermissionQuery;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.omnifaces.util.Faces;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author gdurand
 */
@Named
@SessionScoped
public class DataverseSession implements Serializable {

    @EJB
    PermissionServiceBean permissionsService;

    @EJB
    ActionLogServiceBean logSvc;

    @Inject
    SystemConfig systemConfig;

    private static final Logger logger = Logger.getLogger(DataverseSession.class.getCanonicalName());

    /* Note that on logout, variables must be cleared manually in DataverseHeaderFragment*/
    private User user;
    private boolean statusDismissed = false;
    private String localeCode;

    // -------------------- GETTERS --------------------

    public User getUser() {
        if (user == null) {
            user = GuestUser.get();
        }

        return user;
    }

    public boolean isStatusDismissed() {
        return statusDismissed;
    }

    public String getLocaleCode() {
        if (localeCode == null) {
            localeCode = initLocale();

            logger.fine("init: locale set to " + localeCode);
        }
        return localeCode;
    }

    public Locale getLocale() {
        String localeCode = getLocaleCode();
        return Locale.forLanguageTag(localeCode);
    }

    public String getLocaleTitle() {
        if (localeCode == null) {
            localeCode = initLocale();

            logger.fine("init: locale set to " + localeCode);
        }
        return systemConfig.getConfiguredLocales().get(localeCode);
    }

    // -------------------- LOGIC --------------------

    public String initLocale() {
        Set<String> dataverseLanguages = systemConfig.getConfiguredLocales().keySet();

        return dataverseLanguages.contains(getBrowserLanguage()) ? getBrowserLanguage() : "en";
    }

    public void updateLocaleInViewRootAndRedirect(String code) {

        localeCode = code;
        FacesContext.getCurrentInstance().getViewRoot().setLocale(new Locale(code));
        try {
            String url = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).getHeader("referer");
            FacesContext.getCurrentInstance().getExternalContext().redirect(url);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void updateLocaleInViewRoot() {
        if (localeCode != null
                && FacesContext.getCurrentInstance() != null
                && FacesContext.getCurrentInstance().getViewRoot() != null
                && !localeCode.equals(FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage())) {
            FacesContext.getCurrentInstance().getViewRoot().setLocale(new Locale(localeCode));
        }
    }

    public StaticPermissionQuery on(Dataverse d) {
        return permissionsService.userOn(user, d);
    }

    // -------------------- PRIVATE --------------------

    /**
     * @return Top browser locale which is taken from 'Accept-Language header'.
     */
    private String getBrowserLanguage() {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestLocale().getLanguage();
    }

    // -------------------- SETTERS --------------------

    public void setUser(User aUser) {
        logSvc.log(
                new ActionLogRecord(ActionLogRecord.ActionType.SessionManagement, (aUser == null) ? "logout" : "login")
                        .setUserIdentifier((aUser != null) ? aUser.getIdentifier() : (user != null ? user.getIdentifier() : "")));
        if (aUser == null) {
            Faces.invalidateSession();
        }
        this.user = aUser;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    public void setStatusDismissed(boolean status) {
        statusDismissed = status; //MAD: Set to true to enable code!
    }
}
