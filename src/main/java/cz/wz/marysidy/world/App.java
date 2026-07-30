package cz.wz.marysidy.world;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.wz.marysidy.world.dao.CityDAO;
import cz.wz.marysidy.world.dao.CountryDAO;
import cz.wz.marysidy.world.domain.City;
import cz.wz.marysidy.world.domain.Continent;
import cz.wz.marysidy.world.domain.Country;
import cz.wz.marysidy.world.domain.CountryLanguage;
import cz.wz.marysidy.world.redis.CityCountry;
import cz.wz.marysidy.world.redis.Language;
import cz.wz.marysidy.world.util.MySessionFactory;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisStringCommands;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class App {
    private final SessionFactory sessionFactory;
    private final CountryDAO countryDAO;
    private final CityDAO cityDAO;
    private final RedisClient redisClient;
    private final ObjectMapper mapper;


    // Test raw Hibernate query to verify connection
    private void testConnection(Session session) {
        System.out.println("Connected to DB!!!");
        List<Country> countries = session.createQuery("from Country", Country.class)
                .setMaxResults(5).getResultList();
        System.out.println("Found countries (first 5):");
        for (Country country : countries) {
            System.out.println(" - " + country.getName() + " (" + country.getCode() + ")");
        }
    }

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
        List<Country> allCountries = countryDAO.getAllWithDetails();
        System.out.println("Loaded " + allCountries.size() + " countries with details");

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

    private List<CityCountry> transformData(List<City> cities) {
        return cities.stream().map(city -> {
            CityCountry res = new CityCountry();
            res.setId(city.getId());
            res.setName(city.getName());
            res.setPopulation(city.getPopulation());
            res.setDistrict(city.getDistrict());

            Country country = city.getCountry();
            res.setAlternativeCountryCode(country.getAlternativeCode());
            res.setContinent(country.getContinent());
            res.setCountryCode(country.getCode());
            res.setCountryName(country.getName());
            res.setCountryPopulation(country.getPopulation());
            res.setCountryRegion(country.getRegion());
            res.setCountrySurfaceArea(country.getSurfaceArea());
            Set<CountryLanguage> countryLanguages = country.getLanguages();
            Set<Language> languages = countryLanguages.stream().map(cl -> {
                Language language = new Language();
                language.setLanguage(cl.getLanguage());
                language.setOfficial(cl.getOfficial());
                language.setPercentage(cl.getPercentage());
                return language;
            }).collect(Collectors.toSet());
            res.setLanguages(languages);

            return res;
        }).collect(Collectors.toList());
    }

    private RedisClient prepareRedisClient() {
        RedisClient redisClient = RedisClient.create(RedisURI.create("localhost", 6379));
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            System.out.println("\nConnected to Redis\n");
        }
        return redisClient;
    }

    private void pushToRedis(List<CityCountry> data) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (CityCountry cityCountry : data) {
                try {
                    String key = "city:" + cityCountry.getId();
                    String value = mapper.writeValueAsString(cityCountry);
                    sync.set( key, value);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Long getCountFromRedis() {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            return connection.sync().dbsize();
        }
    }

    private void testRedisData(List<Integer> ids) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (Integer id : ids) {
                String key = "city:" + id;
                String value = sync.get(key);

                if (value == null) {
                    System.out.println("No data found for key: city:" + id);
                    continue;
                }
                try {
                    mapper.readValue(value, CityCountry.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void testMysqlData(List<Integer> ids) {
        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();
            for (Integer id : ids) {
                Optional<City> optionalCity = cityDAO.getById(id);
                // Force Hibernate to fully load the City to avoid proxy objects
                optionalCity.ifPresent(city -> {
                    if (city.getCountry() != null) {
                        Set<CountryLanguage> languages = city.getCountry().getLanguages();
                    }
                });
            }
            session.getTransaction().commit();
        }
    }

    public App() {
        this.sessionFactory = MySessionFactory.getSessionFactory();
        this.countryDAO = new CountryDAO(sessionFactory);
        this.cityDAO = new CityDAO(sessionFactory);
        this.redisClient = prepareRedisClient();
        this.mapper = new ObjectMapper();
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

//            testConnection(session);

//            testCountryDAO();

            System.out.println("\nLoading all cities from DB...");
            List<City> cities = fetchCities();
            System.out.println("Total cities loaded: " + cities.size());

            System.out.println("\nTransforming data for Redis...");
            List<CityCountry> cityCountries = transformData(cities);
            System.out.println("Transformed " + cityCountries.size() + " records.");

            transaction.commit();

            System.out.println("\nPushing data to Redis...");
            pushToRedis(cityCountries);
            System.out.println("Data successfully pushed to Redis!");

            Long count = getCountFromRedis();
            System.out.println("\nTotal keys in Redis: " + count);

            // Явно закрываем сессию, чтобы очистить кэш перед тестами, вручную
            session.close();

            List<Integer> ids = List.of(3, 2545, 123, 4, 189, 89, 3458, 1189, 10, 102);

            long startRedis = System.currentTimeMillis();
            testRedisData(ids);
            long stopRedis = System.currentTimeMillis();

            long startMysql = System.currentTimeMillis();
            testMysqlData(ids);
            long stopMysql = System.currentTimeMillis();

            System.out.printf("Redis: %d ms\n", (stopRedis - startRedis));
            System.out.printf("MySQL: %d ms\n", (stopMysql - startMysql));

        } catch (Exception e) {
            System.err.println("Something wrong!!!");
            e.printStackTrace();
        } finally {
            if (redisClient != null) {
                redisClient.shutdown();
            }
            MySessionFactory.shutdown();
        }
    }
}