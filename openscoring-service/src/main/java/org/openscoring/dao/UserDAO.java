package org.openscoring.dao;

import org.hibernate.Session;
import org.openscoring.dao.model.User;

/**
 * Created by nhannd on 3/16/17.
 */
public class UserDAO {
    public static User getUserByUsername(String username) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        User user = (User) session.createQuery("from User where username = :username")
                .setParameter("username", username)
                .uniqueResult();
        session.getTransaction().commit();
        return user;
    }
}
