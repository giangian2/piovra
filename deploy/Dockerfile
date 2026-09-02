# Runtime image of a Piovra deployable.
#
#   docker build -f deploy/Dockerfile --build-arg APP=piovra-core -t piovra/core .
#
# Same Java major as the build and the devcontainer: a project that compiles on one JVM and
# runs on another discovers the differences in production.

# ---------- build ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
# POMs first, so the dependency layer is invalidated only when they actually change.
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY libs/pom.xml libs/
COPY drivers/pom.xml drivers/
COPY services/pom.xml services/
COPY apps/pom.xml apps/
COPY . .
ARG APP
RUN ./mvnw -B -pl apps/${APP} -am -DskipTests package

# ---------- runtime ----------
FROM eclipse-temurin:25-jre
ARG APP
RUN useradd -r -u 10001 piovra
USER piovra
WORKDIR /app
COPY --from=build /src/apps/${APP}/target/${APP}-*.jar app.jar

# Virtual threads need no pool tuning, but the JVM must see the container's real limits.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseZGC -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
