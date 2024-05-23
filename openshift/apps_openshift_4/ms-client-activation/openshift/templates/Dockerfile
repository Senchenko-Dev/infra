FROM %NEXUS_BASE_URL%/pport/%DOCKER_FROM%
WORKDIR /home/%APP_NAME%
ARG REVISION
RUN chmod g+rwx /home/%APP_NAME%
RUN useradd -u 700 %DOCKER_USER%
RUN wget --no-check-certificate "https://nexus.gts.rus.socgen/repository/debian-bullseye/pool/main/g/google-perftools/libtcmalloc-minimal4_2.8.1-1_amd64.deb" -O /tmp/libtcmalloc-minimal4_2.8.1-1_amd64.deb && dpkg -i /tmp/libtcmalloc-minimal4_2.8.1-1_amd64.deb && rm /tmp/libtcmalloc-minimal4_2.8.1-1_amd64.deb
ENV LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libtcmalloc_minimal.so.4
ENV MALLOC_ARENA_MAX=4
USER %DOCKER_USER%
COPY  /build/libs/%APP_NAME%-%APP_VER%.jar %APP_NAME%.jar
EXPOSE 8080
