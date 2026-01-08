# ---------- BUILD STAGE ----------
    FROM maven:3.9-eclipse-temurin-17 AS build

    WORKDIR /app
    
    # Copy pom.xml first (IMPORTANT for Maven)
    COPY pom.xml .
    
    # Download dependencies
    RUN mvn dependency:go-offline -B
    
    # Copy source code
    COPY src ./src
    
    # Build JAR
    RUN mvn clean package -DskipTests
    
    # ---------- RUNTIME STAGE ----------
    FROM eclipse-temurin:17-jre-alpine
    
    WORKDIR /app
    
    # Copy JAR from build stage
    COPY --from=build /app/target/*.jar app.jar
    
    EXPOSE 5001
    
    ENTRYPOINT ["java", "-jar", "app.jar"]