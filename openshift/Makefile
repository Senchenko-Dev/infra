
njdkSHELL:=/bin/bash

.EXPORT_ALL_VARIABLES:
.ONESHELL:

quay_login:
	@echo "*** $(shell date +"%F %T (%Z)") [Makefile] login to docker registry $${QUAY_BASE_URL}"
	docker login -u $${CI_CD_USER_QUAY} -p \"$${CI_CD_PASS_QUAY}\" $${QUAY_BASE_URL}

nexus_login:
	@echo "*** $(shell date +"%F %T (%Z)") [Makefile] login to docker registry $${NEXUS_BASE_URL}"
	docker login -u $${CI_CD_TEST_USER} -p $${CI_CD_TEST_PASS} $${NEXUS_BASE_URL}

build_jar:
	@echo "*** $(shell date +"%F %T (%Z)") [Makefile] build all - prepare for docker build"
	docker pull $(JDK_IMAGE)
	docker volume create gradle-cache
	docker run -i --rm \
		--name "${GROUP_ID}-${APP_REPO_NAME}-${BUILD_NUMBER}" \
		-v /var/run/docker.sock:/var/run/docker.sock \
		-v $(shell pwd):/build \
		-v gradle-cache:/tmp \
		-w /build \
        -e GRADLE_OPTS='-Dorg.gradle.jvmargs="-Xms512m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError"' \
	    -e GRADLE_USER_HOME=/tmp/.gradle \
		-e LANG=$$LANG \
		-e LOCAL_USER=$$USER \
		-e LOCAL_USER_ID=$(shell id -u) \
		-e CI_CD_USER=$${CI_CD_USER} \
		-e CI_CD_PASS=$${CI_CD_PASS} \
		$(JDK_IMAGE) -c "\
		chmod 755 gradlew  && \
		./gradlew -g .gradle -stop
		./gradlew -g .gradle build $${GRADLE_OPTIONS}"    

build_php:
	docker build . -f docker/Dockerfile -t $${NEXUS_BASE_URL}/$${NEXUS_FOLDER}/$${APP_REPO_NAME}/$${IMAGE_DIR}:$${ARTIFACT_VERSION}

nexus_make_image: 
	@echo "*** $(shell date +"%F %T (%Z)") [Makefile] build docker image for $${APP_REPO_NAME}"
	sed -i 's|%OPENSHIFT_SCRIPTS_DIR%|'$${OPENSHIFT_SCRIPTS_DIR}'|g' Dockerfile
	sed -i 's|%ENV%|'$${ENVIRONMENT}'|g' Dockerfile
	sed -i 's|%APP_NAME%|'$${APP_REPO_NAME}'|g' Dockerfile
	sed -i 's|%DOCKER_FROM%|'$${DOCKER_FROM}'|g' Dockerfile
	sed -i 's|%NEXUS_BASE_URL%|'$${NEXUS_BASE_URL}'|g' Dockerfile
	sed -i 's|%NEXUS_GRADLE_URL%|'$${NEXUS_GRADLE_URL}'|g' Dockerfile
	sed -i 's|%APP_VER%|'$${APP_VER}'|g' Dockerfile
	sed -i 's|%DOCKER_USER%|'$${DOCKER_USER}'|g' Dockerfile
	sed -i 's|%GROUP_ID_TEMPLATE%|'$${GROUP_ID_TEMPLATE}'|g' Dockerfile
	docker build --rm \
		--build-arg APP_NAME=$${APP_REPO_NAME} \
		--build-arg TAG=$${ARTIFACT_VERSION} \
		-t $${NEXUS_BASE_URL}/$${NEXUS_FOLDER}/$${APP_REPO_NAME}/$${IMAGE_DIR}:$${ARTIFACT_VERSION} \
		-f Dockerfile .

nexus_push_image: 
	@echo "*** $(shell date +"%F %T (%Z)") [Makefile] push docker image for $${APP_REPO_NAME}"
	docker push $${NEXUS_BASE_URL}/$${NEXUS_FOLDER}/$${APP_REPO_NAME}/$${IMAGE_DIR}:$${ARTIFACT_VERSION}

deploy:
	@echo "*** $(shell date +"%F %T (%Z)") [Makefile] deploy to target $${OPENSHIFT_URL} as user $${CI_CD_USER_USERNAME} and -n=$${ENVIRONMENT}-pro-$${GROUP_ID_TEMPLATE}"
	@echo "oc login $${OPENSHIFT_URL} --username=$${CI_CD_USER_USERNAME}@$${DOMAIN} --password=$${CI_CD_USER_PASSWORD}  -n=$${ENVIRONMENT}-pro-$${GROUP_ID_TEMPLATE} --insecure-skip-tls-verify=true"
	oc login $${OPENSHIFT_URL} --username=$${CI_CD_USER_USERNAME}@$${DOMAIN} --password=$${CI_CD_USER_PASSWORD}  -n=$${ENVIRONMENT}-pro-$${GROUP_ID_TEMPLATE} --insecure-skip-tls-verify=true
	oc apply -f $${TEMPLATES_DIR}/$${APP_REPO_NAME}-$${TEMPLATE_FILE_MAIN_POSTFIX}.yaml
	oc rollout status deployment $${APP_REPO_NAME}$${MAJOR_VER} --request-timeout=$${DEPLOYMENT_TIMEOUT_SEC}

deploy_java: 
	oc login $${OPENSHIFT_URL} --username=$${CI_CD_USER_USERNAME}@$${DOMAIN} --password=$${CI_CD_USER_PASSWORD}  -n=$${ENVIRONMENT}-pro-$${GROUP_ID_L} --insecure-skip-tls-verify=true
	oc project $${ENVIRONMENT}-pro-$${GROUP_ID_L}
	oc process -f $${TEMPLATES_DIR}/$${APP_REPO_NAME}-$${TEMPLATE_FILE_MAIN_POSTFIX}.yaml | oc apply -f -
	oc rollout status deploymentconfig $${APP_REPO_NAME}$${MAJOR_VER} --request-timeout=$${DEPLOYMENT_TIMEOUT_SEC}
 

prepare_and_deploy_serviceMonitor_yml: 
	sed -i 's|%APP_REPO_NAME%|'$${APP_REPO_NAME}'|g' serviceMonitor.yaml
	sed -i 's|%ENVIRONMENT%|'$${ENVIRONMENT}'|g' serviceMonitor.yaml
	sed -i 's|%GROUP_ID%|'$${GROUP_ID_TEMPLATE}'|g' serviceMonitor.yaml
	oc apply -f serviceMonitor.yaml

prepare_and_deploy_service_yml:
	sed -i 's|%APP_REPO_NAME%|'$${APP_REPO_NAME}'|g' Services.yaml
	sed -i 's|%ENVIRONMENT%|'$${ENVIRONMENT}'|g' Services.yaml
	sed -i 's|%GROUP_ID%|'$${GROUP_ID_TEMPLATE}'|g' Services.yaml
	oc apply -f Services.yaml

  
   
prepare_yml:
	@echo "*** $(shell date +"%F %T (%Z)") [Makefile] prepare deployment yaml for app:$${APP_REPO_NAME} version:$${ARTIFACT_VERSION} env:$${ENVIRONMENT}"
	sed -i 's|%APP_REPO_NAME%|'$${APP_REPO_NAME}'|g' $${TEMPLATES_DIR}/$${APP_REPO_NAME}-$${TEMPLATE_FILE_MAIN_POSTFIX}.yaml
	sed -i 's|%ENVIRONMENT%|'$${ENVIRONMENT}'|g'  $${TEMPLATES_DIR}/$${APP_REPO_NAME}-$${TEMPLATE_FILE_MAIN_POSTFIX}.yaml
	sed -i 's|%GROUP_ID%|'$${GROUP_ID_TEMPLATE}'|g'  $${TEMPLATES_DIR}/$${APP_REPO_NAME}-$${TEMPLATE_FILE_MAIN_POSTFIX}.yaml
	sed -i 's|%TAG%|'$${ARTIFACT_VERSION}'|g'  $${TEMPLATES_DIR}/$${APP_REPO_NAME}-$${TEMPLATE_FILE_MAIN_POSTFIX}.yaml
	sed -i 's|%DOCKER_REGISTRY_URL%|'$${DOCKER_REGISTRY_URL}'|g'  $${TEMPLATES_DIR}/$${APP_REPO_NAME}-$${TEMPLATE_FILE_MAIN_POSTFIX}.yaml
	sed -i 's|%DOMAIN%|'$${DOMAIN}'|g'  $${TEMPLATES_DIR}/$${APP_REPO_NAME}-$${TEMPLATE_FILE_MAIN_POSTFIX}.yaml
	sed -i 's|%SUB_DOMAIN%|'$${SUB_DOMAIN}'|g'  $${TEMPLATES_DIR}/$${APP_REPO_NAME}-$${TEMPLATE_FILE_MAIN_POSTFIX}.yaml
	sed -i 's|%PODS_COUNT%|'$${PODS_COUNT}'|g'  $${TEMPLATES_DIR}/$${APP_REPO_NAME}-$${TEMPLATE_FILE_MAIN_POSTFIX}.yaml
	sed -i 's|%DEPLOYMENT_TIMEOUT_SEC%|'$${DEPLOYMENT_TIMEOUT_SEC}'|g'  $${TEMPLATES_DIR}/$${APP_REPO_NAME}-$${TEMPLATE_FILE_MAIN_POSTFIX}.yaml
	sed -i 's|%APP_VER%|'$${APP_VER_YAML}'|g' $${TEMPLATES_DIR}/$${APP_REPO_NAME}-$${TEMPLATE_FILE_MAIN_POSTFIX}.yaml
	sed -i 's|%MAJOR_VER%|'$${MAJOR_VER_APP}'|g' $${TEMPLATES_DIR}/$${APP_REPO_NAME}-$${TEMPLATE_FILE_MAIN_POSTFIX}.yaml
	sed -i 's|%MAJOR_VER_IS%|'$${MAJOR_VER_IS}'|g' $${TEMPLATES_DIR}/$${APP_REPO_NAME}-$${TEMPLATE_FILE_MAIN_POSTFIX}.yaml
	sed -i 's|%IMAGE_DIR%|'"$${IMAGE_DIR_TEMPLATE}"'|g' $${TEMPLATES_DIR}/$${APP_REPO_NAME}-$${TEMPLATE_FILE_MAIN_POSTFIX}.yaml
	sed -i 's|%ENV_GATEWAY_IPS%|'"$${ENV_GATEWAY_IPS}"'|g' $${TEMPLATES_DIR}/$${APP_REPO_NAME}-$${TEMPLATE_FILE_MAIN_POSTFIX}.yaml
	sed -i 's|%NEXUS_FOLDER%|'"$${NEXUS_FOLDER}"'|g' $${TEMPLATES_DIR}/$${APP_REPO_NAME}-$${TEMPLATE_FILE_MAIN_POSTFIX}.yaml

local_image_delete:
	docker rmi -f $${NEXUS_BASE_URL}/$${NEXUS_FOLDER}/$${APP_REPO_NAME}/$${IMAGE_DIR}:$${ARTIFACT_VERSION}
