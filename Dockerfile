FROM openjdk:17-jdk-alpine

WORKDIR /app

RUN apk add --no-cache python3 py3-pip
RUN pip3 install requests

# Copy the jar file from the target directory of your Maven build into the container
COPY maven_arx-1.0-SNAPSHOT.jar app.jar
EXPOSE 8070

COPY entrypoint.py /app/entrypoint.py
RUN chmod +x /app/entrypoint.py

CMD ["python3", "/app/entrypoint.py"]
