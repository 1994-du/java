FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests
CMD ["java","-jar","target/springBootProject-0.0.1-SNAPSHOT.jar"]
