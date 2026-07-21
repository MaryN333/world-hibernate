package cz.wz.marysidy.world;

import cz.wz.marysidy.world.domain.Country;
import cz.wz.marysidy.world.util.MySessionFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class Main {
    public static void main(String[] args) {
//        System.out.println("Hello world!");
        SessionFactory sessionFactory = MySessionFactory.getSessionFactory();
        try (Session session = sessionFactory.openSession()) {
            System.out.println("Connected to DB!!!");

            List<Country> countries = session.createQuery("from Country", Country.class)
                    .setMaxResults(5)
                    .getResultList();

            System.out.println("Found countries (first 5):");
            for (Country country : countries) {
                System.out.println(" - " + country.getName() + " (" + country.getCode() + ")");
            }
        } catch (Exception e) {
            System.err.println("Something wrong!!!");
            e.printStackTrace();
        } finally {
            MySessionFactory.shutdown();
        }
    }

}