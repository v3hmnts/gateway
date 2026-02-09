FROM eclipse-temurin:24-jdk-ubi10-minimal as build
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:24-jdk-ubi10-minimal
WORKDIR application
COPY --from=build application/dependencies/ ./
COPY --from=build application/spring-boot-loader/ ./
COPY --from=build application/snapshot-dependencies/ ./
COPY --from=build application/application/ ./
ENV SPRING_PROFILES_ACTIVE="docker"
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher","--spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]