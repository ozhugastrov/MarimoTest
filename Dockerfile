FROM sbtscala/scala-sbt:eclipse-temurin-17.0.4_1.7.1_3.2.0

WORKDIR /app

COPY build.sbt .
COPY project ./project

RUN sbt update

COPY src ./src

EXPOSE 8080

# Default command to run your app
CMD ["sbt", "runMain inc.zhugastrov.marimo.Main"]