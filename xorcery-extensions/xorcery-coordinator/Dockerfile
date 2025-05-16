ARG JDK_IMAGE=eclipse-temurin:21-ubi9-minimal
FROM $JDK_IMAGE
ENV JAVA_OPTS="-Xmx1G -Xms1G"
ARG MAIN_JAR
ENV ENV_MAIN_JAR=$MAIN_JAR
ARG MAIN_CLASS
ENV ENV_MAIN_CLASS=$MAIN_CLASS

COPY ./target/app/. /home/app

COPY --chmod=755 <<-EOT /home/app/entrypoint.sh
#!/bin/sh
#
# Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

exec java $JAVA_OPTS -jar lib/${ENV_MAIN_JAR}
EOT

RUN echo "$(</home/app/entrypoint.sh)"
EXPOSE 8080
WORKDIR /home/app
ENTRYPOINT ["./entrypoint.sh"]
