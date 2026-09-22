package cz.wz.marysidy.world.util;

import cz.wz.marysidy.world.domain.City;
import cz.wz.marysidy.world.domain.Country;
import cz.wz.marysidy.world.domain.CountryLanguage;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import java.util.Properties;

public class MySessionFactory {
    private static MySessionFactory instance;
    private final SessionFactory sessionFactory;

    private MySessionFactory() {
        Properties properties = new Properties();
        properties.put(Environment.DRIVER, "com.p6spy.engine.spy.P6SpyDriver");
        properties.put(Environment.URL, "jdbc:p6spy:mysql://localhost:3308/world");
        properties.put(Environment.DIALECT, "org.hibernate.dialect.MySQLDialect");
        properties.put(Environment.USER, "root");
        properties.put(Environment.PASS, "root");
        properties.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        properties.put(Environment.HBM2DDL_AUTO, "validate");
        properties.put(Environment.STATEMENT_BATCH_SIZE, "100");

        sessionFactory = new Configuration()
                .addAnnotatedClass(City.class)
                .addAnnotatedClass(Country.class)
                .addAnnotatedClass(CountryLanguage.class)
                .addProperties(properties)
                .buildSessionFactory();
    }
    public static synchronized SessionFactory getSessionFactory() {
        if (instance == null) {
            instance = new MySessionFactory();
        }
        return instance.sessionFactory;
    }

    public static void shutdown() {
        if (instance != null) {
            instance.sessionFactory.close();
        }
    }
}