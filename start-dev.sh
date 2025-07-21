#!/bin/bash

echo "🔧 Fixing Trading System Setup Issues..."

# Check if we're in the right directory
if [ ! -f "build.gradle.kts" ]; then
    echo "❌ Please run this script from the project root directory (where build.gradle.kts is located)"
    exit 1
fi

# 1. Fix migration file naming
echo "📝 Fixing migration file names..."
MIGRATION_DIR="src/main/resources/db/migration"

if [ -f "$MIGRATION_DIR/v1__Create_order_tables.sql" ]; then
    echo "   Renaming v1__Create_order_tables.sql to V1__Create_order_tables.sql"
    mv "$MIGRATION_DIR/v1__Create_order_tables.sql" "$MIGRATION_DIR/V1__Create_order_tables.sql"
fi

# List migration files to verify
echo "   Current migration files:"
ls -la "$MIGRATION_DIR"

# 2. Ensure Docker containers are running
echo "🐳 Checking Docker containers..."
docker-compose up -d

# Wait a bit for containers to start
sleep 5

# 3. Check PostgreSQL connection
echo "🔍 Testing PostgreSQL connection..."
if docker exec ms-postgres pg_isready -U postgres -d msdb > /dev/null 2>&1; then
    echo "✅ PostgreSQL is ready"
else
    echo "❌ PostgreSQL is not ready. Let's wait a bit more..."
    sleep 10
    if docker exec ms-postgres pg_isready -U postgres -d msdb > /dev/null 2>&1; then
        echo "✅ PostgreSQL is now ready"
    else
        echo "❌ PostgreSQL failed to start. Checking logs..."
        docker logs ms-postgres --tail 20
        exit 1
    fi
fi

# 4. Test database connection manually
echo "🔍 Testing database connection manually..."
docker exec ms-postgres psql -U postgres -d msdb -c "SELECT version();" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Database connection successful"
else
    echo "❌ Database connection failed. Creating database..."
    docker exec ms-postgres psql -U postgres -c "CREATE DATABASE msdb;" 2>/dev/null || echo "   Database might already exist"
fi

# 5. Clean and rebuild Gradle dependencies
echo "🔨 Cleaning and rebuilding Gradle dependencies..."
./gradlew clean --refresh-dependencies

# 6. Try Flyway migration with Spring Boot instead of Gradle plugin
echo "🗃️  Attempting Flyway migration with Spring Boot..."
./gradlew flywayMigrate -Dspring.profiles.active=default

if [ $? -eq 0 ]; then
    echo "✅ Flyway migration successful!"
else
    echo "⚠️  Gradle Flyway plugin failed. Let's try with Spring Boot..."

    # Alternative: Use Spring Boot's Flyway integration
    echo "🔄 Building project first..."
    ./gradlew compileJava

    if [ $? -eq 0 ]; then
        echo "✅ Compilation successful!"
        echo ""
        echo "🎉 Setup completed! Now you can:"
        echo "   ./gradlew bootRun    # To start the application (Flyway will run automatically)"
        echo "   ./gradlew test       # To run tests"
        echo ""
        echo "Note: Spring Boot will run Flyway migrations automatically on startup."
    else
        echo "❌ Compilation failed. Please check the error messages above."
        exit 1
    fi
fi

echo ""
echo "📊 Service Status:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"