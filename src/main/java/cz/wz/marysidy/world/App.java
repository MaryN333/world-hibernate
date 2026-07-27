package cz.wz.marysidy.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.wz.marysidy.world.dao.CityDAO;
import cz.wz.marysidy.world.dao.CountryDAO;
import cz.wz.marysidy.world.domain.City;
import cz.wz.marysidy.world.domain.Continent;
import cz.wz.marysidy.world.domain.Country;
import cz.wz.marysidy.world.util.MySessionFactory;
import io.lettuce.core.RedisClient;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class App {
    private final SessionFactory sessionFactory;
    private final CountryDAO countryDAO;
    private final CityDAO cityDAO;

    private void testCountryDAO() {
        System.out.println("=== Testing CountryDAO ===");

        // 1. getAll()
        List<Country> countries1 = countryDAO.getAll();
        System.out.println("Found countries (first 5):");
        countries1.stream().limit(5).forEach(c ->
                System.out.println(" - " + c.getName() + " (" + c.getCode() + ")"));

        // 2. getById()
        Optional<Country> countryOpt = countryDAO.getById(1);
        countryOpt.ifPresentOrElse(
                c -> System.out.println("Country with ID 1: " + c.getName()),
                () -> System.out.println("Country with ID 1 not found"));

        // 3. save()
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
        countryDAO.save(newCountry);
        System.out.println("Saved new country: " + newCountry.getName() + " (ID: " + newCountry.getId() + ")");

        // 4. update()
        newCountry.setName("Testland Updated");
        countryDAO.update(newCountry);
        System.out.println("Updated country name: " + newCountry.getName());

        // 5. deleteById()
        boolean deleted = countryDAO.deleteById(newCountry.getId());
        System.out.println(deleted ? "Deleted country by ID: " + newCountry.getId() : "Failed to delete");

        // 6. getByName()
        Optional<Country> countryByName = countryDAO.getByName("France");
        countryByName.ifPresentOrElse(
                c -> System.out.println("Country with name 'France': " + c.getCode()),
                () -> System.out.println("Country with name 'France' not found"));
    }

    private List<City> fetchCities() {
        List<City> allCities = new ArrayList<>();

            int totalCount = cityDAO.getTotalCount();
            int step = 500;

            for (int i = 0; i < totalCount; i += step) {
                List<City> batch = cityDAO.getItems(i, step);
                allCities.addAll(batch);
                System.out.println("Fetched " + batch.size() + " cities (offset " + i + ")");
            }
        return allCities;
    }

    public App() {
        this.sessionFactory = MySessionFactory.getSessionFactory();
        this.countryDAO = new CountryDAO(sessionFactory);
        this.cityDAO = new CityDAO(sessionFactory);
    }

    public static void main(String[] args) {
//        SessionFactory sessionFactory = MySessionFactory.getSessionFactory();
//        CountryDAO countryDAO = new CountryDAO(sessionFactory);
        App app = new App();
        app.run();
    }

    public void run() {
        try (Session session = sessionFactory.getCurrentSession()) {
            Transaction transaction = session.beginTransaction();
            System.out.println("Connected to DB!!!");

            // Test raw Hibernate query to verify connection
            List<Country> countries = session.createQuery("from Country", Country.class)
                    .setMaxResults(5).getResultList();
            System.out.println("Found countries (first 5):");
            for (Country country : countries) {
                System.out.println(" - " + country.getName() + " (" + country.getCode() + ")");
            }

            testCountryDAO();

            System.out.println("Loading all cities from DB...");
            List<City> cities = fetchCities();
            System.out.println("Total cities loaded: " + cities.size());

            transaction.commit();
        } catch (Exception e) {
            System.err.println("Something wrong!!!");
            e.printStackTrace();
        } finally {
            MySessionFactory.shutdown();
        }

    }
}