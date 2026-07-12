# Stage 1: Build with Maven + JDK 21
FROM crpi-27zlqugq2208c0pz.cn-hangzhou.personal.cr.aliyuncs.com/xiaofeiyang930112/maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Copy parent POM + all module POMs (Maven reactor needs all declared modules)
COPY pom.xml ./
COPY docker-settings.xml /root/.m2/settings.xml
COPY expense-framework/pom.xml ./expense-framework/
COPY expense-security/pom.xml ./expense-security/
COPY expense-category-api/pom.xml ./expense-category-api/
COPY expense-category-common/pom.xml ./expense-category-common/
COPY expense-bill-api/pom.xml ./expense-bill-api/
COPY expense-bill-common/pom.xml ./expense-bill-common/
COPY expense-statistics-api/pom.xml ./expense-statistics-api/
COPY expense-statistics-common/pom.xml ./expense-statistics-common/
COPY expense-user/pom.xml ./expense-user/
COPY expense-category-application/pom.xml ./expense-category-application/
COPY expense-bill-application/pom.xml ./expense-bill-application/
COPY expense-statistics-application/pom.xml ./expense-statistics-application/
COPY expense-ai/pom.xml ./expense-ai/
COPY expense-budget/pom.xml ./expense-budget/
COPY expense-server/pom.xml ./expense-server/
COPY expense-gateway/pom.xml ./expense-gateway/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B -pl expense-category-application -am -q || true

# Copy source code (only needed modules)
COPY expense-framework/src ./expense-framework/src/
COPY expense-category-api/src ./expense-category-api/src/
COPY expense-category-common/src ./expense-category-common/src/
COPY expense-category-application/src ./expense-category-application/src/

# Build only category-service
RUN mvn package -B -pl expense-category-application -am -DskipTests -q

# Stage 2: Runtime with minimal JRE
FROM crpi-27zlqugq2208c0pz.cn-hangzhou.personal.cr.aliyuncs.com/xiaofeiyang930112/eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /build/expense-category-application/target/*.jar app.jar

USER appuser
EXPOSE 8082

ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-jar", "app.jar"]
