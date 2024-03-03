openssl req -newkey rsa:2048 -nodes -keyout key.pem -x509 -days 365 -out certificate.pem \
    -subj "/C=DE/ST=Berlin/L=Berlin/O=Tobchen.de/OU=Tobchen.de/CN=localhost/" \
    -addext "subjectAltName=DNS:localhost,DNS:patient-index-main,DNS:patient-index-ws"
