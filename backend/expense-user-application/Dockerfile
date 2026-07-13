# expense-user (:8081)
FROM expense-base-builder:latest AS builder
COPY expense-category/expense-category-api/src ./expense-category/expense-category-api/src/
COPY expense-category/expense-category-api/pom.xml ./expense-category/expense-category-api/
COPY expense-category/expense-category-common/src ./expense-category/expense-category-common/src/
COPY expense-category/expense-category-common/pom.xml ./expense-category/expense-category-common/
COPY expense-user-api/src ./expense-user-api/src/
COPY expense-user-api/pom.xml ./expense-user-api/
COPY expense-user-common/src ./expense-user-common/src/
COPY expense-user-common/pom.xml ./expense-user-common/
COPY expense-user-application/src ./expense-user-application/src/
COPY expense-user-application/pom.xml ./expense-user-application/
RUN mvn install -f expense-category/expense-category-api/pom.xml -B -q -DskipTests && mvn install -f expense-category/expense-category-common/pom.xml -B -q -DskipTests
RUN mvn install -f expense-user-api/pom.xml -B -q -DskipTests && mvn install -f expense-user-common/pom.xml -B -q -DskipTests
RUN mvn package -f expense-user-application/pom.xml -B -q -DskipTests
FROM crpi-27zlqugq2208c0pz.cn-hangzhou.personal.cr.aliyuncs.com/xiaofeiyang930112/eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /build/expense-user-application/target/*.jar app.jar
USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-jar", "app.jar"]
