FROM eclipse-temurin:25-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle gradle
COPY app-api/build.gradle app-api/build.gradle
COPY application/build.gradle application/build.gradle
COPY common/build.gradle common/build.gradle
COPY domain/build.gradle domain/build.gradle
COPY infrastructure/build.gradle infrastructure/build.gradle

RUN chmod +x ./gradlew
RUN ./gradlew :app-api:dependencies --no-daemon > /tmp/gradle-dependencies.log

COPY app-api/src app-api/src
COPY application/src application/src
COPY common/src common/src
COPY domain/src domain/src
COPY infrastructure/src infrastructure/src

RUN ./gradlew :app-api:bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre-jammy AS runtime

ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=UTC
ENV JAVA_OPTS=""

WORKDIR /app

RUN groupadd --system meetbowl \
    && useradd --system --gid meetbowl --create-home --home-dir /app meetbowl

COPY --from=build /workspace/app-api/build/libs/*.jar /app/app.jar

USER meetbowl

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
