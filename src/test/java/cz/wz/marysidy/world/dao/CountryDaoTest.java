package cz.wz.marysidy.world.dao;

import cz.wz.marysidy.world.domain.Continent;
import cz.wz.marysidy.world.domain.Country;
import cz.wz.marysidy.world.util.MySessionFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CountryDaoTest {
    private static SessionFactory sessionFactory;
    private static CountryDAO countryDAO;

    @BeforeAll
    static void setup() {
        sessionFactory = MySessionFactory.getSessionFactory();
        countryDAO = new CountryDAO(sessionFactory);
    }

    @AfterAll
    static void tearDown() {
        if (sessionFactory != null) {
            MySessionFactory.shutdown();
        }
    }

    // -*-*-*-*-*-*-*-*-*-*-*-* 1. getAll() -*-*-*-*-*-*-*-*-*-*-*-*
    @Test
    @Order(1)
    void testGetAll() {
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();

        List<Country> countries = countryDAO.getAll();

        transaction.commit();
        assertNotNull(countries);
        assertTrue(countries.size() > 0);
        System.out.println("\ntestGetAll: found " + countries.size() + " countries");
    }

    // -*-*-*-*-*-*-*-*-*-*-*-* 2. getAllWithDetails() -*-*-*-*-*-*-*-*-*-*-*-*
    @Test
    @Order(2)
    void testGetAllWithDetails() {
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();

        List<Country> countries = countryDAO.getAllWithDetails();

        transaction.commit();
        assertNotNull(countries);
        assertTrue(countries.size() > 0);
        Country first = countries.get(0);
        assertNotNull(first.getLanguages());
        System.out.println("\ntestGetAllWithDetails: loaded " + countries.size() + " countries with details");
    }

    // -*-*-*-*-*-*-*-*-*-*-*-* 3. getById() -*-*-*-*-*-*-*-*-*-*-*-*
    @Test
    @Order(3)
    void testGetById() {
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();

        Optional<Country> countryOpt = countryDAO.getById(1);

        transaction.commit();
        assertTrue(countryOpt.isPresent());
        assertEquals("Aruba", countryOpt.get().getName());
        System.out.println("\ntestGetById: found " + countryOpt.get().getName());
    }

    // -*-*-*-*-*-*-*-*-*-*-*-* 4. getTotalCount() -*-*-*-*-*-*-*-*-*-*-*-*
    @Test
    @Order(4)
    void testGetTotalCount() {
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();

        long count = countryDAO.getTotalCount();

        transaction.commit();
        assertTrue(count > 0);
        System.out.println("testGetTotalCount: " + count);
    }

    // -*-*-*-*-*-*-*-*-*-*-*-* 5. save() -*-*-*-*-*-*-*-*-*-*-*-*
    @Test
    @Order(5)
    void testSave() {
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();

        Country newCountry = new Country();
        newCountry.setName("Testland");
        newCountry.setCode("TST");
        newCountry.setAlternativeCode("TS");
        newCountry.setContinent(Continent.EUROPE);
        newCountry.setRegion("Test Region");
        newCountry.setSurfaceArea(BigDecimal.valueOf(1000));
        newCountry.setPopulation(100000);
        newCountry.setLocalName("Testland");
        newCountry.setGovernmentForm("Test Government");

        Integer savedId = null;
        try {
            countryDAO.save(newCountry);
            savedId = newCountry.getId();
            assertNotNull(savedId, "ID should be generated after save");
            System.out.println("\ntestSave: saved country with ID: " + savedId);

            Optional<Country> saved = countryDAO.getById(savedId);
            assertTrue(saved.isPresent());
            assertEquals("Testland", saved.get().getName());

        } catch (Exception e) {
            System.err.println("testSave failed: " + e.getMessage());
            throw e;
        } finally {
            if (savedId != null) {
                try {
                    countryDAO.deleteById(savedId);
                    System.out.println("testSave: cleaned up country with ID: " + savedId);
                } catch (Exception cleanupEx) {
                    System.err.println("Cleanup failed for ID: " + savedId + " - " + cleanupEx.getMessage());
                }
            }
            transaction.commit();
        }
    }

    // -*-*-*-*-*-*-*-*-*-*-*-* 6. update() -*-*-*-*-*-*-*-*-*-*-*-*
    @Test
    @Order(6)
    void testUpdate() {
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();

        Country newCountry = new Country();
        newCountry.setName("UpdateTest");
        newCountry.setCode("UPD");
        newCountry.setAlternativeCode("UP");
        newCountry.setContinent(Continent.EUROPE);
        newCountry.setRegion("Test Region");
        newCountry.setSurfaceArea(BigDecimal.valueOf(1000));
        newCountry.setPopulation(100000);
        newCountry.setLocalName("UpdateTest");
        newCountry.setGovernmentForm("Test Government");

        Integer savedId = null;
        try {
            countryDAO.save(newCountry);
            savedId = newCountry.getId();

            newCountry.setName("UpdatedName");
            countryDAO.update(newCountry);

            Optional<Country> updated = countryDAO.getById(savedId);
            assertTrue(updated.isPresent());
            assertEquals("UpdatedName", updated.get().getName());
            System.out.println("\ntestUpdate: updated country name to: " + updated.get().getName());

        } catch (Exception e) {
            System.err.println("testUpdate failed: " + e.getMessage());
            throw e;
        } finally {
            if (savedId != null) {
                try {
                    countryDAO.deleteById(savedId);
                    System.out.println("testUpdate: cleaned up country with ID: " + savedId);
                } catch (Exception cleanupEx) {
                    System.err.println("Cleanup failed for ID: " + savedId + " - " + cleanupEx.getMessage());
                }
            }
            transaction.commit();
        }
    }

    // -*-*-*-*-*-*-*-*-*-*-*-* 7. deleteById() -*-*-*-*-*-*-*-*-*-*-*-*
    @Test
    @Order(7)
    void testDeleteById() {
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();

        Country newCountry = new Country();
        newCountry.setName("DeleteTest");
        newCountry.setCode("DEL");
        newCountry.setAlternativeCode("DE");
        newCountry.setContinent(Continent.EUROPE);
        newCountry.setRegion("Test Region");
        newCountry.setSurfaceArea(BigDecimal.valueOf(1000));
        newCountry.setPopulation(100000);
        newCountry.setLocalName("DeleteTest");
        newCountry.setGovernmentForm("Test Government");

        Integer savedId = null;
        try {
            countryDAO.save(newCountry);
            savedId = newCountry.getId();

            boolean deleted = countryDAO.deleteById(savedId);
            assertTrue(deleted);
            System.out.println("\ntestDeleteById: deleted country with ID: " + savedId);

            Optional<Country> check = countryDAO.getById(savedId);
            assertFalse(check.isPresent());
            savedId = null;

        } catch (Exception e) {
            System.err.println("testDeleteById failed: " + e.getMessage());
            throw e;
        } finally {
            if (savedId != null) {
                try {
                    countryDAO.deleteById(savedId);
                    System.out.println("testDeleteById: cleaned up country with ID: " + savedId);
                } catch (Exception cleanupEx) {
                    System.err.println("Cleanup failed for ID: " + savedId + " - " + cleanupEx.getMessage());
                }
            }
            transaction.commit();
        }
    }

    // -*-*-*-*-*-*-*-*-*-*-*-* 8. delete(Country) -*-*-*-*-*-*-*-*-*-*-*-*
    @Test
    @Order(8)
    void testDelete() {
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();

        Country newCountry = new Country();
        newCountry.setName("DeleteObjTest");
        newCountry.setCode("DEL");
        newCountry.setAlternativeCode("DO");
        newCountry.setContinent(Continent.EUROPE);
        newCountry.setRegion("Test Region");
        newCountry.setSurfaceArea(BigDecimal.valueOf(1000));
        newCountry.setPopulation(100000);
        newCountry.setLocalName("DeleteObjTest");
        newCountry.setGovernmentForm("Test Government");

        Integer savedId = null;
        try {
            countryDAO.save(newCountry);
            savedId = newCountry.getId();

            countryDAO.delete(newCountry);
            System.out.println("\ntestDelete: deleted country object with ID: " + savedId);

            Optional<Country> check = countryDAO.getById(savedId);
            assertFalse(check.isPresent());
            savedId = null;

        } catch (Exception e) {
            System.err.println("testDelete failed: " + e.getMessage());
            throw e;
        } finally {
            if (savedId != null) {
                try {
                    countryDAO.deleteById(savedId);
                    System.out.println("testDelete: cleaned up country with ID: " + savedId);
                } catch (Exception cleanupEx) {
                    System.err.println("Cleanup failed for ID: " + savedId + " - " + cleanupEx.getMessage());
                }
            }
            transaction.commit();
        }
    }

    // -*-*-*-*-*-*-*-*-*-*-*-* 9. getByName() -*-*-*-*-*-*-*-*-*-*-*-*
    @Test
    @Order(9)
    void testGetByName() {
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();

        Optional<Country> countryOpt = countryDAO.getByName("France");

        transaction.commit();
        assertTrue(countryOpt.isPresent());
        assertEquals("FRA", countryOpt.get().getCode());
        System.out.println("\ntestGetByName: found " + countryOpt.get().getName());
    }
}