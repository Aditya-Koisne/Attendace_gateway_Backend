FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 7890
ENTRYPOINT ["java","-jar","/app/app.jar"]
