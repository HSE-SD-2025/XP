FROM eclipse-temurin:21-jre

WORKDIR /app

COPY app/build/libs/*-fat.jar /app/app.jar

CMD ["java", "-jar", "/app/app.jar"]
