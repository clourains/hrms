# 多阶段构建：Maven 构建 → JRE 17 运行（M2-6）
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# 先下依赖，利用 Docker 层缓存
RUN mvn dependency:go-offline -B -q
COPY src ./src
RUN mvn package -B -DskipTests -q

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
