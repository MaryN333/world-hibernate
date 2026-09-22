package cz.wz.marysidy.world.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.wz.marysidy.world.dao.CityDAO;
import cz.wz.marysidy.world.domain.City;
import cz.wz.marysidy.world.redis.CityCountry;
import cz.wz.marysidy.world.domain.Country;
import cz.wz.marysidy.world.domain.CountryLanguage;
import cz.wz.marysidy.world.redis.Language;
import cz.wz.marysidy.world.util.MySessionFactory;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisStringCommands;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PerformanceTest {
    private static SessionFactory sessionFactory;
    private static CityDAO cityDAO;
    private static RedisClient redisClient;
    private static ObjectMapper mapper;
    private static List<Integer> testIds;

    @BeforeAll
    static void setup() {
        sessionFactory = MySessionFactory.getSessionFactory();
        cityDAO = new CityDAO(sessionFactory);

        redisClient = RedisClient.create(RedisURI.create("localhost", 6379));
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            System.out.println("Connected to Redis");
        }

        mapper = new ObjectMapper();
        testIds = List.of(3, 2545, 123, 4, 189, 89, 3458, 1189, 10, 102);

        // Load cities and push to Redis once before tests
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();

        int total = cityDAO.getTotalCount();
        List<City> cities = cityDAO.getItems(0, total);
        List<CityCountry> cityCountries = transformData(cities);

        transaction.commit();

        pushToRedis(cityCountries);
        System.out.println("Redis preloaded with " + cityCountries.size() + " records");
    }

    @AfterAll
    static void tearDown() {
        if (redisClient != null) {
            redisClient.shutdown();
        }
        MySessionFactory.shutdown();
    }

    @Test
    @Order(1)
    void testRedisPerformance() {
        long start = System.currentTimeMillis();

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (Integer id : testIds) {
                String key = "city:" + id;
                String json = sync.get(key);
                assertNotNull(json, "Data for city " + id + " not found in Redis");
                mapper.readValue(json, CityCountry.class);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        long duration = System.currentTimeMillis() - start;
        System.out.println("\n\n\nRedis read 10 cities: " + duration + " ms");
    }

    @Test
    @Order(2)
    void testMySqlPerformance() {
        long start = System.currentTimeMillis();

        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        for (Integer id : testIds) {
            Optional<City> optionalCity = cityDAO.getById(id);
            assertNotNull(optionalCity, "City with id " + id + " not found in MySQL");
            optionalCity.ifPresent(city -> {
                if (city.getCountry() != null) {
                    Set<CountryLanguage> languages = city.getCountry().getLanguages();
                }
            });
        }
        session.getTransaction().commit();

        long duration = System.currentTimeMillis() - start;
        System.out.println("\n\n\nMySQL read 10 cities: " + duration + " ms");
    }

    private static List<CityCountry> transformData(List<City> cities) {
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

    private static void pushToRedis(List<CityCountry> data) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (CityCountry cityCountry : data) {
                try {
                    String key = "city:" + cityCountry.getId();
                    String value = mapper.writeValueAsString(cityCountry);
                    sync.set(key, value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
