FROM crpi-27zlqugq2208c0pz.cn-hangzhou.personal.cr.aliyuncs.com/xiaofeiyang930112/eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY arthas-tunnel-server.jar /app/app.jar

EXPOSE 7777 8080

ENTRYPOINT ["java", "-jar", "-Darthas.enable-detail-pages=true", "/app/app.jar"]
