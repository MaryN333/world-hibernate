package cz.wz.marysidy.world.dao;

import cz.wz.marysidy.world.domain.City;
import cz.wz.marysidy.world.util.MySessionFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CityDaoTest {
    private static SessionFactory sessionFactory;
    private static CityDAO cityDAO;

    @BeforeAll
    static void setup() {
        sessionFactory = MySessionFactory.getSessionFactory();
        cityDAO = new CityDAO(sessionFactory);
    }

    @AfterAll
    static void tearDown() {
        MySessionFactory.shutdown();
    }

    // -*-*-*-*-*-*-*-*-*-*-*-* 1. getItems() -*-*-*-*-*-*-*-*-*-*-*-*
    @Test
    @Order(1)
    void testGetItems() {
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();

        int limit = 10;

        List<City> firstPage = cityDAO.getItems(0, limit);
        assertNotNull(firstPage);
        assertEquals(limit, firstPage.size(), "Should return exactly " + limit + " cities");
        assertNotNull(firstPage.get(0).getCountry(), "Country should be loaded via JOIN FETCH");

        List<City> secondPage = cityDAO.getItems(10, limit);
        assertNotEquals(firstPage.get(0).getId(), secondPage.get(0).getId(), "Pages should contain different cities");

        transaction.commit();

        System.out.println("\ntestGetItems: fetched " + firstPage.size() + " cities (first page)");
    }

    // -*-*-*-*-*-*-*-*-*-*-*-* 2. getById() -*-*-*-*-*-*-*-*-*-*-*-*
    @Test
    @Order(2)
    void testGetById() {
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();

        Optional<City> cityOpt = cityDAO.getById(1);

        transaction.commit();

        assertTrue(cityOpt.isPresent(), "City with ID 1 should exist");
        City city = cityOpt.get();
        assertNotNull(city.getName(), "City name should not be null");
        assertNotNull(city.getCountry(), "Country should be loaded via JOIN FETCH");
        System.out.println("\ntestGetById: found city: " + city.getName() + " (country: " + city.getCountry().getName() + ")");
    }

    // -*-*-*-*-*-*-*-*-*-*-*-* 3. getTotalCount() -*-*-*-*-*-*-*-*-*-*-*-*
    @Test
    @Order(3)
    void testGetTotalCount() {
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();

        int count = cityDAO.getTotalCount();

        transaction.commit();

        assertTrue(count > 0, "Total count should be greater than 0");
        System.out.println("testGetTotalCount: " + count + " cities in database");
    }
}

