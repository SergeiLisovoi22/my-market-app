FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/my-market-app-0.0.1-SNAPSHOT.jar app.jar
VOLUME /app/data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
