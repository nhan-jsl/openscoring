package org.openscoring.service;

import java.util.List;
import java.util.Date;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import org.openscoring.dao.model.*;

/**
 * Created by nhannd on 3/16/17.
 */
public class TestHibernate {
    private static SessionFactory factory;

    public static void main(String[] args) {
        try {
            factory = new Configuration().configure().buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Failed to create sessionFactory object." + ex);
            throw new ExceptionInInitializerError(ex);
        }
        TestHibernate ME = new TestHibernate();

        /* Add few users records in database */
        Integer userId1 = ME.addUser("Zara", "Ali", "abc1@xyz.com");
        Integer userId2 = ME.addUser("Daisy", "Das", "abc2@xyz.com");
        Integer userId3 = ME.addUser("John", "Paul", "abc3@xyz.com");

        /* List down all the users */
        ME.listUsers();

        /* Update user's records */
        ME.updateUser(userId1, "updatedmail@gmail.com");

        /* Delete an user from the database */
        ME.deleteUser(userId2);

        /* List down new list of the users */
        ME.listUsers();
    }

    /* Method to CREATE an employee in the database */
    public Integer addUser(String username, String password, String email) {
        Session session = factory.openSession();
        Transaction tx = null;
        Integer userId = null;
        try {
            tx = session.beginTransaction();
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setEmail(email);

            userId = (Integer) session.save(user);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return userId;
    }

    /* Method to  READ all the users */
    public void listUsers() {
        Session session = factory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            List users = session.createQuery("FROM User").list();
            for (Iterator iterator =
                 users.iterator(); iterator.hasNext(); ) {
                User user = (User) iterator.next();
                System.out.print("First Name: " + user.getUsername());
                System.out.print("  Last Name: " + user.getPassword());
                System.out.println("  Salary: " + user.getEmail());
            }
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    /* Method to UPDATE email for an User */
    public void updateUser(Integer EmployeeID, String newEmail) {
        Session session = factory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            User user =
                    (User) session.get(User.class, EmployeeID);
            user.setEmail(newEmail);
            session.update(user);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    /* Method to DELETE an user from the records */
    public void deleteUser(Integer EmployeeID) {
        Session session = factory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            User user =
                    (User) session.get(User.class, EmployeeID);
            session.delete(user);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }
}
