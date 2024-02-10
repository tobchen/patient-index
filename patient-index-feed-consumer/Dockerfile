FROM python:3.9.18-alpine3.19
WORKDIR /app
COPY ./consumer.py .
ENTRYPOINT [ "python", "-u", "consumer.py", "6060" ]
EXPOSE 6060
