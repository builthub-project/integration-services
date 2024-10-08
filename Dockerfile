FROM openjdk:17-jdk-slim
RUN apt-get update && apt-get install -y curl
COPY ./integration-services-1.12.0.jar app.jar
EXPOSE 9091
ENTRYPOINT ["java", "-Xms512m", "-Xmx4g", "-jar","./app.jar"]
HEALTHCHECK CMD curl -f -G http://localhost:9091/api/health || exit 1
