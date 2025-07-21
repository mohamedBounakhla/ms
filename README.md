# Trading System - Order Domain Setup

A Spring Boot trading system implementing Hexagonal Architecture with focus on the Order domain.

## 🏗️ Architecture

The project follows **Hexagonal Architecture** (Ports and Adapters) with clear separation between:

- **Domain Layer**: Core business logic, entities, and value objects
- **Application Layer**: Use cases and application services
- **Infrastructure Layer**: Database persistence and external integrations
- **Web Layer**: REST controllers and DTOs

## 🛠️ Prerequisites

- **Java 17+**
- **Maven 3.8+**
- **Docker & Docker Compose**
- **PostgreSQL** (via Docker)
- **Redis** (via Docker)

## 🚀 Quick Start

### 1. Clone and Setup

```bash
git clone <your-repository>
cd trading-system
chmod +x start-dev.sh
```

### 2. Start Development Environment

```bash
./start-dev.sh
```

This script will:
- Start PostgreSQL and Redis containers
- Run database migrations with Flyway
- Compile the project
- Display service information

### 3. Start the Application

```bash
mvn spring-boot:run
```

The application will be available at: `http://localhost:8080`

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn test -Dtest="*IntegrationTest"
```

### Run Unit Tests
```bash
mvn test -Dtest="*UnitTest"
```

## 📊 Services

| Service | URL | Credentials |
|---------|-----|-------------|
| PostgreSQL | `localhost:5432` | `postgres/postgres` (DB: `msdb`) |
| Redis | `localhost:6379` | No auth |
| Application | `http://localhost:8080` | See Security section |

## 🔐 Security & Authentication

### Default Users

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | ADMIN |
| `testuser` | `customer123` | CUSTOMER |

### Authentication Flow

1. **Register** (POST `/register`)
```json
{
  "username": "newuser",
  "password": "password123"
}
```

2. **Login** (POST `/login`)
```json
{
  "username": "testuser",
  "password": "customer123"
}
```

3. **Use JWT Token** in Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

## 📈 Order Management API

### Create Buy Order
```http
POST /api/v1/orders/buy
Authorization: Bearer <token>
Content-Type: application/json

{
  "userId": "testuser",
  "symbolCode": "BTC",
  "price": 45000.00,
  "currency": "USD",
  "quantity": 0.1
}
```

### Create Sell Order
```http
POST /api/v1/orders/sell
Authorization: Bearer <token>
Content-Type: application/json

{
  "userId": "testuser",
  "symbolCode": "ETH",
  "price": 3000.00,
  "currency": "USD",
  "quantity": 1.5
}
```

### Get Order by ID
```http
GET /api/v1/orders/{orderId}
Authorization: Bearer <token>
```

### Get Orders by Symbol
```http
GET /api/v1/orders/symbol/BTC
Authorization: Bearer <token>
```

### Get Orders by Status
```http
GET /api/v1/orders/status/PENDING
Authorization: Bearer <token>
```

## 🗄️ Database

### Flyway Migrations

Migrations are located in `src/main/resources/db/migration/`:

- `V1__Create_order_tables.sql` - Order domain tables
- `V2__Create_user_tables.sql` - User management tables

### Manual Migration Commands

```bash
# Run migrations
mvn flyway:migrate

# Check migration status
mvn flyway:info

# Validate migrations
mvn flyway:validate
```

### Database Schema

**Order Tables:**
- `buy_orders` - Buy order entities
- `sell_orders` - Sell order entities
- `transactions` - Executed transactions

**User Tables:**
- `msuser` - User accounts and authentication

## 🧰 Development Commands

### Docker Management
```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f postgres
docker-compose logs -f redis

# Access PostgreSQL CLI
docker exec -it ms-postgres psql -U postgres -d msdb
```

### Maven Commands
```bash
# Clean build
mvn clean compile

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Package application
mvn clean package

# Skip tests during build
mvn clean package -DskipTests
```

## 📁 Project Structure

```
src/
├── main/
│   ├── java/core/ms/
│   │   ├── config/           # Configuration classes
│   │   ├── order/            # Order domain (complete)
│   │   │   ├── domain/       # Core domain logic
│   │   │   ├── application/  # Application services
│   │   │   ├── infrastructure/ # Persistence layer
│   │   │   └── web/          # REST controllers
│   │   ├── security/         # Security domain
│   │   ├── shared/           # Shared domain objects
│   │   └── MsApplication.java
│   └── resources/
│       ├── db/migration/     # Flyway migrations
│       ├── application.properties
│       └── application-test.properties
└── test/                     # Test classes
```

## 🐛 Troubleshooting

### Database Connection Issues
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Check PostgreSQL logs
docker logs ms-postgres

# Restart PostgreSQL
docker-compose restart postgres
```

### Application Startup Issues
```bash
# Check application logs
mvn spring-boot:run -X

# Verify database migrations
mvn flyway:info
```

### Port Conflicts
If ports 5432 or 6379 are already in use:
```bash
# Check what's using the ports
lsof -i :5432
lsof -i :6379

# Modify docker-compose.yml to use different ports
```

## 🔄 Next Steps

1. **Complete Other Domains**: Implement hexagonal architecture for:
    - Portfolio domain
    - Order Book domain
    - Market Engine domain
    - OHLC domain

2. **Add More Tests**:
    - Unit tests for domain entities
    - Integration tests for repositories
    - End-to-end API tests

3. **Enhance Security**:
    - Role-based access control
    - API rate limiting
    - Input validation

4. **Add Monitoring**:
    - Health checks
    - Metrics collection
    - Logging improvements

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design](https://domainlanguage.com/ddd/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)