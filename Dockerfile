# Stage 1: build jar bang Maven (co JDK + Maven, image nay khong duoc dung de chay app)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: runtime - chi chua JRE + file jar da build, image nho gon
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/ecommerce-mini-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
