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

    public void run() {
        try (Session session = sessionFactory.getCurrentSession()) {
            Transaction transaction = session.beginTransaction();

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

    public App() {
        this.sessionFactory = MySessionFactory.getSessionFactory();
        this.countryDAO = new CountryDAO(sessionFactory);
        this.cityDAO = new CityDAO(sessionFactory);
        this.redisClient = prepareRedisClient();
        this.mapper = new ObjectMapper();
    }

    public static void main(String[] args) {
        App app = new App();
        app.run();
    }
}