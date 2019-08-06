/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.notification;

import edu.harvard.iq.dataverse.mail.MailService;
import edu.harvard.iq.dataverse.notification.dto.EmailNotificationMapper;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;
import io.vavr.Tuple2;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author xyang
 */
@Stateless
public class UserNotificationServiceBean {

    private static final Logger logger = Logger.getLogger(UserNotificationServiceBean.class.getCanonicalName());

    @EJB
    MailService mailService;
    @Inject
    private EmailNotificationMapper mailMapper;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public List<UserNotification> findByUser(Long userId) {
        TypedQuery<UserNotification> query = em.createQuery("select un from UserNotification un where un.user.id =:userId order by un.sendDate desc", UserNotification.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }

    public List<UserNotification> findByDvObject(Long dvObjId) {
        TypedQuery<UserNotification> query = em.createQuery("select object(o) from UserNotification as o where o.objectId =:dvObjId order by o.sendDate desc", UserNotification.class);
        query.setParameter("dvObjId", dvObjId);
        return query.getResultList();
    }

    public List<UserNotification> findUnreadByUser(Long userId) {
        TypedQuery<UserNotification> query = em.createQuery("select object(o) from UserNotification as o where o.user.id =:userId and o.readNotification = 'false' order by o.sendDate desc", UserNotification.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }

    public Long getUnreadNotificationCountByUser(Long userId) {
        if (userId == null) {
            return new Long("0");
        }
        Query query = em.createNativeQuery("select count(id) from usernotification as o where o.user_id = " + userId + " and o.readnotification = 'false';");
        return (Long) query.getSingleResult();
    }

    public List<UserNotification> findUnemailed() {
        TypedQuery<UserNotification> query = em.createQuery("select object(o) from UserNotification as o where o.readNotification = 'false' and o.emailed = 'false'", UserNotification.class);
        return query.getResultList();
    }

    public UserNotification find(Object pk) {
        return em.find(UserNotification.class, pk);
    }

    public UserNotification save(UserNotification userNotification) {
        return em.merge(userNotification);
    }

    public void delete(UserNotification userNotification) {
        em.remove(em.merge(userNotification));
    }

    public void sendNotificationWithoutEmail(AuthenticatedUser dataverseUser, Timestamp sendDate, NotificationType type) {
        UserNotification userNotification = new UserNotification();
        userNotification.setUser(dataverseUser);
        userNotification.setSendDate(sendDate);
        userNotification.setType(type);

        save(userNotification);
    }

    public void sendNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, NotificationType type, Tuple2<Long, NotificationObjectType> dvObjectIdAndType) {
        sendNotification(dataverseUser, sendDate, type, dvObjectIdAndType, "");
    }

    public void sendNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, NotificationType type, Tuple2<Long, NotificationObjectType> dvObjectIdAndType, String comment) {
        sendNotification(dataverseUser, sendDate, type, dvObjectIdAndType, comment, null);
    }

    public void sendNotification(AuthenticatedUser dataverseUser,
                                 Timestamp sendDate,
                                 NotificationType type,
                                 Tuple2<Long, NotificationObjectType> dvObjectIdAndType,
                                 String comment,
                                 AuthenticatedUser requestor) {

        UserNotification userNotification = new UserNotification();
        userNotification.setUser(dataverseUser);
        userNotification.setSendDate(sendDate);
        userNotification.setType(type);
        userNotification.setObjectId(dvObjectIdAndType._1());
        userNotification.setRequestor(requestor);

        if (mailService.sendNotificationEmail(mailMapper.toDto(userNotification, dvObjectIdAndType), requestor)) {
            logger.fine("email was sent");
            userNotification.setEmailed(true);
            save(userNotification);
        } else {
            logger.fine("email was not sent");
            save(userNotification);
        }
    }
}