# Piovra development environment.
#
# Java 25 is an architectural choice, not a preference: since JEP 491 a synchronized block no
# longer pins a virtual thread to its carrier thread. On 21, every library holding a
# synchronized monitor across a blocking call cancels out the benefit of virtual threads -
# which is exactly what a connector does (docs/10-stack-and-repo.md).
FROM eclipse-temurin:25-jdk

ARG DEBIAN_FRONTEND=noninteractive
ARG USERNAME=piovra
ARG USER_UID=1000
ARG USER_GID=1000

RUN apt-get update && apt-get install -y --no-install-recommends \
        git curl unzip zip jq ca-certificates \
        postgresql-client redis-tools \
        docker.io docker-compose-plugin \
    && rm -rf /var/lib/apt/lists/*

# Non-root user sharing the host uid: files created in the workspace stay editable outside.
RUN groupadd --gid ${USER_GID} ${USERNAME} 2>/dev/null || true \
    && useradd --uid ${USER_UID} --gid ${USER_GID} -m -s /bin/bash ${USERNAME} 2>/dev/null || true \
    && usermod -aG docker ${USERNAME} || true

USER ${USERNAME}
WORKDIR /workspace

# The Maven cache lives on a volume: see devcontainer.json
ENV MAVEN_OPTS="-Dmaven.repo.local=/home/${USERNAME}/.m2/repository"

# Sanity check: if the image does not actually carry Java 25, better to find out here.
RUN java -version 2>&1 | grep -q '"25' || (echo "expected JDK 25" && exit 1)
