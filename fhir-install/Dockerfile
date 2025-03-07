# ----------------------------------------------------------------------------
# (C) Copyright IBM Corp. 2016, 2021
#
# SPDX-License-Identifier: Apache-2.0
# ----------------------------------------------------------------------------
# Stage: Base

FROM openliberty/open-liberty:21.0.0.1-full-java8-openj9-ubi as base

ENV LICENSE accept

USER root
RUN yum install -y unzip

RUN mkdir -p /opt/ibm-fhir-server

COPY target/fhir-server-distribution.zip /tmp/
RUN unzip -qq /tmp/fhir-server-distribution.zip -d /tmp && \
    /tmp/fhir-server-dist/install-fhir.sh /opt/ol/wlp && \
    mv /tmp/fhir-server-dist/tools /opt/ibm-fhir-server/tools
COPY ./src/main/docker/ibm-fhir-server/bootstrap.sh /opt/ibm-fhir-server/

USER 1001

# ----------------------------------------------------------------------------
# Stage: Runnable

FROM openliberty/open-liberty:21.0.0.1-full-java8-openj9-ubi

ARG FHIR_VERSION=4.6.0

# The following labels are required: 
LABEL name='IBM FHIR Server'
LABEL vendor='IBM'
LABEL version="$FHIR_VERSION"
LABEL summary="Image for IBM FHIR Server with OpenJ9 and UBI 8"
LABEL description="The IBM FHIR Server is a modular Java implementation of version 4 of the HL7 FHIR specification with a focus on performance and configurability."

COPY --chown=1001:0 --from=base /opt/ol/wlp/usr /opt/ol/wlp/usr
COPY --chown=1001:0 --from=base /opt/ibm-fhir-server /opt/ibm-fhir-server

COPY target/LICENSE /licenses/

ENV FHIR /opt/ol/wlp/usr/servers/fhir-server

# Set the working directory to the fhir-server liberty server
WORKDIR ${FHIR}

# Set the output directory so that output will appear under the working directory
ENV WLP_OUTPUT_DIR=/opt/ol/wlp/usr/servers

# Tell liberty not to worry about a keystore since we provide our own at a different path
ENV KEYSTORE_REQUIRED "false"

# Replace links to defaultServer with links to fhir-server and 
# create the configDropins directories for the fhir-server
USER root
RUN chmod -R 755 /opt/ibm-fhir-server && \
    ln -sfn ${FHIR} /output && \
    ln -sfn ${FHIR} /config && \
    rm -rf /opt/ol/wlp/output && \
    rm -rf /opt/ol/wlp/usr/servers/defaultServer && \
    mkdir -p /config/configDropins/defaults /config/configDropins/overrides && \
    chown -R 1001:1001 /config/configDropins/ && \
    yum update -y && \
    yum clean all && \
    rm -rf /var/cache/yum

USER 1001

ENTRYPOINT ["/opt/ibm-fhir-server/bootstrap.sh"]
CMD ["/opt/ol/wlp/bin/server", "run", "fhir-server"]
