# Piovra development environment.
#
# Java 25 is an architectural choice, not a preference: since JEP 491 a synchronized block no
# longer pins a virtual thread to its carrier thread. On 21, every library holding a
# synchronized monitor across a blocking call cancels out the benefit of virtual threads -
# which is exactly what a connector does (docs/10-stack-and-repo.md).
#
# The base is pinned to -noble (Ubuntu 24.04 LTS) rather than the floating :25-jdk tag, which
# currently resolves to Ubuntu 26.04. The devcontainer feature ecosystem does not publish
# packages for a distribution that recent, and a floating base makes the build a moving target.
FROM eclipse-temurin:25-jdk-noble

ARG DEBIAN_FRONTEND=noninteractive
ARG USERNAME=piovra
ARG USER_UID=1000
ARG USER_GID=1000

# docker.io provides the CLI that talks to the host daemon (Testcontainers); on Ubuntu the
# compose plugin is packaged as docker-compose-v2. Access to the host socket is a matter of
# group id, handled at run time by scripts/devshell - see the comment there.
RUN apt-get update && apt-get install -y --no-install-recommends \
        git curl unzip zip jq ca-certificates sudo \
        postgresql-client redis-tools \
        docker.io docker-compose-v2 \
    && rm -rf /var/lib/apt/lists/*

# The user must share the host uid, so files created in the workspace stay editable from
# outside. Ubuntu base images since 24.04 already ship a user at uid 1000 ("ubuntu"), so we
# rename that one instead of creating a second: two users cannot share a uid, and useradd
# would fail here rather than at the point where it matters.
RUN set -eux; \
    if getent passwd "${USER_UID}" >/dev/null; then \
        existing_user="$(getent passwd "${USER_UID}" | cut -d: -f1)"; \
        existing_group="$(getent group "${USER_GID}" | cut -d: -f1 || true)"; \
        if [ "${existing_user}" != "${USERNAME}" ]; then \
            [ -n "${existing_group}" ] && groupmod -n "${USERNAME}" "${existing_group}"; \
            usermod -l "${USERNAME}" -d "/home/${USERNAME}" -m "${existing_user}"; \
        fi; \
    else \
        groupadd --gid "${USER_GID}" "${USERNAME}"; \
        useradd --uid "${USER_UID}" --gid "${USER_GID}" -m -s /bin/bash "${USERNAME}"; \
    fi; \
    usermod -aG docker "${USERNAME}"; \
    echo "${USERNAME} ALL=(root) NOPASSWD:ALL" > "/etc/sudoers.d/${USERNAME}"; \
    chmod 0440 "/etc/sudoers.d/${USERNAME}"; \
    mkdir -p "/home/${USERNAME}/.m2"; \
    chown -R "${USER_UID}:${USER_GID}" "/home/${USERNAME}"

USER ${USERNAME}
WORKDIR /workspaces/piovra

# The Maven cache lives on a volume: see devcontainer.json
ENV MAVEN_OPTS="-Dmaven.repo.local=/home/${USERNAME}/.m2/repository"

# Sanity check: if the image does not actually carry Java 25, better to find out here.
RUN java -version 2>&1 | grep -q '"25' || (echo "expected JDK 25" && exit 1)
