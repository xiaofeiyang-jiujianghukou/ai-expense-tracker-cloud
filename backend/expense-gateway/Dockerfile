# expense-gateway (:8080)
FROM expense-base-builder:latest AS builder
COPY expense-gateway/src ./expense-gateway/src/
COPY expense-gateway/pom.xml ./expense-gateway/
RUN mvn package -f expense-gateway/pom.xml -B -q -DskipTests
FROM crpi-27zlqugq2208c0pz.cn-hangzhou.personal.cr.aliyuncs.com/xiaofeiyang930112/eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /build/expense-gateway/target/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-jar", "app.jar"]
