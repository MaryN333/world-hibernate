package cz.wz.marysidy.world.dao;

import cz.wz.marysidy.world.domain.Country;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import javax.persistence.NoResultException;
import java.util.List;
import java.util.Optional;

public class CountryDAO {
    private final SessionFactory sessionFactory;

    public CountryDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<Country> getAll() {
        Query<Country> query = sessionFactory.getCurrentSession().createQuery("select c from Country c", Country.class);
        return query.list();
    }

    public List<Country> getAllWithDetails() {
        Query<Country> query = sessionFactory.getCurrentSession().createQuery("select distinct c from Country c" +
                " left join fetch c.city left join fetch c.languages", Country.class);
        return query.list();
    }

    public Optional<Country> getById(Integer id) {
        Country country = sessionFactory.getCurrentSession().get(Country.class, id);
        return Optional.ofNullable(country);
    }

    public long getTotalCount(){
        Query<Long> query = sessionFactory.getCurrentSession().createQuery("select count(*) from Country", Long.class);
        return query.getSingleResult();
    }

    public Country save(Country country) {
        sessionFactory.getCurrentSession().saveOrUpdate(country);
        return country;     //returns object with id, if it is new object
    }

    public Country update(Country country) {
        return (Country) sessionFactory.getCurrentSession().merge(country);
    }

    public boolean deleteById(Integer id) {
        Country country = sessionFactory.getCurrentSession().get(Country.class, id);
        if (country != null) {
            sessionFactory.getCurrentSession().remove(country);
            return true;
        }
        return false;
    }

    public void delete(Country country) {
        sessionFactory.getCurrentSession().remove(country);
    }

    public Optional<Country> getByName(String name) {
        try {
            Country country = sessionFactory.getCurrentSession()
                    .createQuery("from Country where name = :name", Country.class)
                    .setParameter("name", name).getSingleResult();
            return Optional.of(country);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
