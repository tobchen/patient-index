set -x

maven_version=3
java_version=21
pix_version=0.0.1-SNAPSHOT

docker build -f patient-index-commons/Dockerfile \
    -t "patient-index-commons:$pix_version-mvn-$maven_version-java-$java_version" patient-index-commons

docker build -f patient-index-main/Dockerfile \
    -t "patient-index-main:$pix_version-mvn-$maven_version-java-$java_version" patient-index-main

docker build -f patient-index-feed/Dockerfile \
    -t "patient-index-feed:$pix_version-mvn-$maven_version-java-$java_version" patient-index-feed

docker build -f patient-index-ws/Dockerfile \
    -t "patient-index-ws:$pix_version-mvn-$maven_version-java-$java_version" patient-index-ws
