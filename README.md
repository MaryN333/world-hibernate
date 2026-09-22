# World Hibernate

A Java learning project demonstrating Hibernate ORM, Redis caching, and performance comparison between MySQL and Redis.

## Technologies

- **Java 17**
- **Hibernate 5.6** — ORM for database interaction
- **MySQL 8.0** — relational database (via Docker)
- **Redis 6.2** — in-memory cache (via Docker)
- **Lettuce** — Redis Java client
- **Jackson** — JSON serialization/deserialization
- **JUnit 5** — unit and performance testing
- **P6Spy** — SQL logging with real parameter values
- **Maven** — build automation

---
## Getting Started

### Prerequisites

- **Docker** and **Docker Compose** installed
- **Java 17**
- **IntelliJ IDEA** (or any Java IDE)
- **DBeaver** (optional, for DB inspection)

---
### Quick Start

#### 1. Clone the repository

```bash
git clone https://github.com/MaryN333/world-hibernate.git
cd world-hibernate
```
#### 2. Start MySQL with the database dump
   The database dump is located in docker/dump-hibernate-final.sql.

Run the following command from the project root:
```bash
docker run --name mysql_world_hibernate \
  -e MYSQL_ROOT_PASSWORD=root \
  -p 3308:3306 \
  -v "$(pwd)/docker":/docker-entrypoint-initdb.d \
  -v mysql_world_data:/var/lib/mysql \
  --restart unless-stopped \
  -d mysql:8.0
```
This will:

Create a MySQL container with the database world

Load the initial data from the dump

Persist data in a named Docker volume (mysql_world_data)

#### 3. Start Redis
```bash
docker run -d --name redis -p 6379:6379 redis:6.2-alpine
```
#### 4. Run the application
   Open the project in IntelliJ IDEA and run the App.main() method.

You should see output similar to:
```text
Connected to Redis

Loading all cities from DB...
Loaded 239 countries with details
Fetched 500 cities (offset 0)
Fetched 500 cities (offset 500)
...
Total cities loaded: 4079

Transforming data for Redis...
Transformed 4079 records.

Pushing data to Redis...
Data successfully pushed to Redis!

Total keys in Redis: 4079
```
#### 5. Run performance tests
Run the PerformanceTest class (JUnit) to compare read performance between Redis and MySQL:
```text
Redis read 10 cities: 22 ms
MySQL read 10 cities: 34 ms
```
### Project Structure
```text
src/
├── main/java/cz/wz/marysidy/world/
│   ├── dao/              # Data Access Objects
│   │   ├── CityDAO.java
│   │   └── CountryDAO.java
│   ├── domain/           # JPA Entities
│   │   ├── City.java
│   │   ├── Country.java
│   │   ├── CountryLanguage.java
│   │   └── Continent.java
│   ├── redis/            # Redis DTOs
│   │   ├── CityCountry.java
│   │   └── Language.java
│   ├── util/             # Utilities
│   │   └── MySessionFactory.java
│   └── App.java          # Main application entry point
├── test/java/cz/wz/marysidy/world/
│   ├── dao/              # DAO tests
│   │   ├── CityDaoTest.java
│   │   └── CountryDaoTest.java
│   └── performance/      # Performance benchmarks
│       └── PerformanceTest.java
├── docker/
│   └── dump-hibernate-final.sql
├── src/main/resources/
│   └── spy.properties
└── pom.xml
```
### Key Features
- CRUD operations for Country and City entities
- JOIN FETCH optimization to avoid N+1 queries
- Pagination support for city data loading (500 records per batch)
- Redis caching — transformed data stored as JSON strings
- Performance comparison between Redis and MySQL (10 random reads)
- JUnit tests for DAO layer and Redis/MySQL benchmarks