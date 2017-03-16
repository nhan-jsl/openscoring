package org.openscoring.dao;

import org.hibernate.Session;
import org.openscoring.dao.model.UserRole;

import java.util.List;

/**
 * Created by nhannd on 3/16/17.
 */
public class UserRoleDAO {
    public static List<UserRole> getUserRolesByUsername(String username) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        @SuppressWarnings("unchecked")
        List<UserRole> roles = session.createQuery("from UserRole where username = :username")
                .setParameter("username", username)
                .list();
        session.getTransaction().commit();
        return roles;
    }

    public static void insert(UserRole r) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        session.persist(r);
        session.getTransaction().commit();
    }
}
