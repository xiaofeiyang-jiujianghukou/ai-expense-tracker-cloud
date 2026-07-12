# expense-category (:8082)
FROM expense-base-builder:latest AS builder
COPY expense-category/expense-category-api/src ./expense-category/expense-category-api/src/
COPY expense-category/expense-category-api/pom.xml ./expense-category/expense-category-api/
COPY expense-category/expense-category-common/src ./expense-category/expense-category-common/src/
COPY expense-category/expense-category-common/pom.xml ./expense-category/expense-category-common/
COPY expense-category/expense-category-application/src ./expense-category/expense-category-application/src/
COPY expense-category/expense-category-application/pom.xml ./expense-category/expense-category-application/
RUN mvn install -f expense-category/expense-category-api/pom.xml -B -q -DskipTests
RUN mvn install -f expense-category/expense-category-common/pom.xml -B -q -DskipTests
RUN mvn package -f expense-category/expense-category-application/pom.xml -B -q -DskipTests
FROM crpi-27zlqugq2208c0pz.cn-hangzhou.personal.cr.aliyuncs.com/xiaofeiyang930112/eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /build/expense-category/expense-category-application/target/*.jar app.jar
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-jar", "app.jar"]
