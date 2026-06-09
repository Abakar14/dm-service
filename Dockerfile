FROM eclipse-temurin:21-jdk

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY build/libs/dm-*.jar dm-service.jar

ENTRYPOINT ["java", "-jar", "dm-service.jar"]
