pipeline
{
  agent
  {
    node
    {
      label 'Proportal_agent-prod'
    }
  }

  options
  {
    buildDiscarder(logRotator(artifactDaysToKeepStr: '14', artifactNumToKeepStr: '14', daysToKeepStr: '14', numToKeepStr: '20'))
    timestamps()
    skipDefaultCheckout()
    //disableConcurrentBuilds()
  }

  parameters
  {
    choice(name: 'ENVIRONMENT', choices: ['test','cert','prod'], description: 'Среда для установки')
    choice(name: 'TARGET_PLATFORM', choices: ['main_and_dr', 'main', 'dr'], description: 'Только для PROD. Выбор плошадок для деплоя.')
    choice(name: 'STRATEGY', choices: ['build_and_deploy','build_only','deploy_only','check'], description: 'Стратегия запуска задания')
    string(name: 'IMAGE_TAG', defaultValue: '', description: 'Только для deploy_only. Имя тэга образа для установки (можно скопировать с NEXUS. Например 0.0.1-20200401-1)')
    //choice(name: 'HOTFIX', choices: ['no','yes'], description: 'Только для прода')
    //string(name: 'HOTFIX_ISSUES', defaultValue: '', description: 'Только для HOTFIX. Номер задачи в jira(например PRODBO-18048)')
    string(name: 'PODS_COUNT', defaultValue: '1', description: 'Только для deploy_only и build_and_deploy. Количество запущенных экземпляров приложения')
    string(name: 'DEPLOYMENT_TIMEOUT_SEC', defaultValue: '120', description: 'Только для deploy_only и build_and_deploy. Таймаут ожидания деплоя (в секундах)')
    string(name: 'INFRA_BACKUP_BRANCH', defaultValue: 'master', description: 'Ветка для сохранения удачных конфигураций')
  }

  environment
  {
    GIT_BASE_URL = "https://gitlab.rosbank.rus.socgen"
    GIT_BASE_URL_SHORT = "gitlab.rosbank.rus.socgen"
    DOCKER_FROM = "pport/core/feature:1.0.1"
    DOCKER_FROM_17 = "openjdk:17-jdk-slim-debian_v3"
    DOCKER_USER = "admin"
    FILE_DIR = "./openshift/jenkins/RFC"
    GROOVY_SCRIPTS_DIR="./openshift/groovy_files"
    GROUP_ID = "ProPortal"
    GROUP_ID_TEMPLATE = "${GROUP_ID.toLowerCase()}"
    GROUP_PATH = "proport"
    GITLAB_API_URL="https://gitlab.rosbank.rus.socgen/api/v4/"
    INFRA_APPS_SCRIPTS_DIR = "./openshift/apps_scripts"
    INSTALLATION_TYPE = "release"
    IS_PRISMA_CLOUD_ACTIVE = "false"
    IS_PROTOCOL_DIR = "./src/main/java/ru/rosbank/isapi"
    JENKINS_GROUP_DIR = "/home/jenkins-agent/workspace/${GROUP_ID}"
    MAJOR_VER_APP = "1"
    NEXUS_BASE_URL = "docker-registry.gts.rus.socgen"
    NEXUS_GRADLE_URL = "nexus.gts.rus.socgen"
    OPENSHIFT_BACKUP_DIR = "./openshift/backup"
    OPENSHIFT_BUILDS_HISTORY_DIR = "./openshift/builds_history"
    OPENSHIFT4_CERT_PROD_URL = "https://api.ocp.rosbank.rus.socgen:6443"
    OPENSHIFT4_DR_URL = "https://api.ocp1.rosbank.rus.socgen:6443"
    OPENSHIFT_DEPLOYS_HISTORY_DIR = "./openshift/deploys_history"
    OPENSHIFT_DOMAIN_PROD = "rosbank.rus.socgen"
    OPENSHIFT_DOMAIN_TEST = "trosbank.trus.tsocgen"
    OPENSHIFT_SCRIPTS_DIR = "./openshift/scripts"
    OPENSHIFT4_TEST_URL = "https://api.tpaas.trosbank.trus.tsocgen:6443"
    OUTSIDERS_RULE = "true"
    PROJECTKEY = "CI271142"
    QUAY_BASE_URL = "quay.gts.rus.socgen"
    NEXUS_FOLDER = "pport"
    REPO_PATH="infra"
    TEMPLATE_FILE_MAIN_AND_DR_POSTFIX = "template-dr"
    TEMPLATE_FILE_POSTFIX = "template"
    TEMPLATES_DIR = "./openshift/templates"
    OPENSHIFT_VER = "4"
    PROPORTAL_API_TOKEN=credentials('rbsproportal-gitlab-api-token')
    INFRA_APPS_DIR = "./openshift/apps_openshift_${OPENSHIFT_VER}/${APP_REPO_NAME.toLowerCase()}"
    WORKSPACE=pwd()
  }

  stages
  {
    stage ('checkout main repo')
    {
      steps
      {
        checkout([
          $class: 'GitSCM',
          branches: [[name: "master"]],
          doGenerateSubmoduleConfigurations: false,
          extensions: [[$class: 'CleanBeforeCheckout']],
          submoduleCfg: [],
        userRemoteConfigs:[[
          url: "${env.GIT_BASE_URL}/${env.GROUP_PATH}/${REPO_PATH}.git",
          credentialsId: "proportal_deploy"]]])
      }
    }

    stage ('check Openshift ver')
    {
      steps
      {
        script
        {
          env.OPENSHIFT_VER = "4"
          env.INFRA_APPS_DIR = "./openshift/apps_openshift_${OPENSHIFT_VER}/${APP_REPO_NAME.toLowerCase()}"
        }
      }
    }

    stage ('copy app openshift dir from base')
    {
      steps
      {
        script
        {
          sh """
            mkdir "${JENKINS_GROUP_DIR}/${JOB_BASE_NAME}-${BUILD_NUMBER}"
            mkdir "${JENKINS_GROUP_DIR}/${JOB_BASE_NAME}-${BUILD_NUMBER}/${APP_REPO_NAME}"
            mkdir ${INFRA_APPS_DIR}/openshift/scripts
            cp -r ./openshift/Makefile ${INFRA_APPS_DIR}/openshift
            cp -r ${INFRA_APPS_SCRIPTS_DIR}/scripts ${INFRA_APPS_DIR}/openshift
            cp -r ${INFRA_APPS_DIR}/openshift ${JENKINS_GROUP_DIR}/${JOB_BASE_NAME}-${BUILD_NUMBER}/${APP_REPO_NAME}/openshift/
          """
        }
      }
    }

    stage('checkout target repo')
    {
      steps
      {
        checkout([
          $class: 'GitSCM',
          branches: [[name: "${env.BRANCH}"]],
          doGenerateSubmoduleConfigurations: false,
          extensions: [[$class: 'CleanBeforeCheckout']],
          submoduleCfg: [],
          userRemoteConfigs:[[
            url: "${env.GIT_BASE_URL}/${env.GROUP_PATH}/ms/${params.APP_REPO_NAME.toLowerCase()}.git",
            credentialsId: "proportal_deploy"]]])
      }
    }
    stage ('move app openshift dir to target')
    {
      steps
      {
        script
        {
          sh "cp -r ${JENKINS_GROUP_DIR}/${JOB_BASE_NAME}-${BUILD_NUMBER}/${APP_REPO_NAME}/openshift ./"
          sh "rm -rf ${JENKINS_GROUP_DIR}/${JOB_BASE_NAME}-${BUILD_NUMBER}"
        }
      }
    }
    stage('build')
    {
      steps
      {
        withCredentials([
          usernamePassword
          (
            credentialsId: "Pro-portal-test",
            passwordVariable: 'CI_CD_TEST_PASS',
			      usernameVariable: 'CI_CD_TEST_USER'
          )])
        {
        script{
          env.MS_VERSIONIS=getVersion().trim()
          env.TAG_VER = sh (returnStdout: true, script: 'echo -n $(date +%Y%m%d-%H%M)')
          env.IMAGE_DIR= getImageBranch("${env.BRANCH}")
          env.IMAGE_DIR_TEMPLATE = "/${env.IMAGE_DIR}"
          sh """
              mv ./openshift/Makefile ./
              cp -r ${TEMPLATES_DIR}/Dockerfile ./
            """
          if (env.STRATEGY == 'build_and_deploy' || env.STRATEGY == 'build_only' || env.STRATEGY == 'check')
            {
              if (env.ENVIRONMENT == 'prod')
              {
                if (env.STRATEGY == 'build_and_deploy')
                {
                  echo "Cтратегия ${env.STRATEGY} к ${env.ENVIRONMENT} среде не применима"
                  currentBuild.result = "FAILURE"
                  error ('Cтратегия ${env.STRATEGY} к ${env.ENVIRONMENT} среде не применима')
                }
              }
              if (env.ENVIRONMENT == 'cert')
              {
                if (env.STRATEGY == 'build_and_deploy' && !(env.BRANCH.matches("master")))
                {
                  echo "Ветки, отличные от master, не применимы к стратегии ${STRATEGY} в ${ENVIRONMENT} среде"
                  currentBuild.result = "FAILURE"
                  error ('Ветки,  отличные от master, не применимы к стратегии ${STRATEGY} в ${ENVIRONMENT} среде')
                }
              }
              env.ARTIFACT_VERSION=("${env.MS_VERSIONIS}" + "-${IMAGE_DIR}" + "-${env.TAG_VER}")
              echo "${env.ARTIFACT_VERSION}"
              sh "make nexus_login"
              sh "make build_php"
              sh "make nexus_push_image"
              sh "make local_image_delete"
              IS_IMAGE_EXISTS = "true"
            }   
        }
      }
     }
  }
  stage ('deploy image')
  {
      steps
      {
        withCredentials([
          usernamePassword
          (
            credentialsId: "Pro-portal-test",
            passwordVariable: 'CI_CD_TEST_PASS',
			      usernameVariable: 'CI_CD_TEST_USER'
          ),
          usernamePassword
          (
            credentialsId: "rbsproportal-gitlab",
            passwordVariable: 'CI_CD_PASS_QUAY',
			      usernameVariable: 'CI_CD_USER_QUAY'
          ),
          usernamePassword
          (
            credentialsId: "Pro-portal-prod",
            passwordVariable: 'CI_CD_PASS',
			      usernameVariable: 'CI_CD_USER'
          )])
        {
          script
          { 
            if (env.STRATEGY == 'build_and_deploy' || env.STRATEGY == 'deploy_only')
            {
            sh " curl --header \"PRIVATE-TOKEN: ${PROPORTAL_API_TOKEN}\" \"${GITLAB_API_URL}projects/7674/repository/files/Namespace-configs%2FAfter_service_deploy_config%2FServices%2FServices.yaml/raw?ref=master\"  -o Services.yaml"
            sh " curl --header \"PRIVATE-TOKEN: ${PROPORTAL_API_TOKEN}\" \"${GITLAB_API_URL}projects/7674/repository/files/Namespace-configs%2FAfter_service_deploy_config%2FServiceMonitor%2FserviceMonitor.yaml/raw?ref=master\"  -o serviceMonitor.yaml"
              if (env.ENVIRONMENT == 'test' || env.ENVIRONMENT == 'dev')
              {
                env.CI_CD_USER_USERNAME = "${CI_CD_TEST_USER}"
                env.CI_CD_USER_PASSWORD = "${CI_CD_TEST_PASS}"
                env.OPENSHIFT_URL = "${OPENSHIFT4_TEST_URL}"
                env.DOCKER_REGISTRY_URL = "${NEXUS_BASE_URL}/${NEXUS_FOLDER}"
                env.DOMAIN = "${OPENSHIFT_DOMAIN_TEST}"
                env.SUB_DOMAIN = "tpaas"
                env.ENV_GATEWAY_IPS = "${ENVIRONMENT}-api"
              }
              if (env.ENVIRONMENT == 'cert' || env.ENVIRONMENT == 'prod')
              {
                env.CI_CD_USER_USERNAME = "${CI_CD_USER}"
                env.CI_CD_USER_PASSWORD = "${CI_CD_PASS}"
                env.OPENSHIFT_URL = "${OPENSHIFT4_CERT_PROD_URL}"
                env.DOCKER_REGISTRY_URL = "${QUAY_BASE_URL}/${NEXUS_FOLDER}"
                env.DOMAIN = "${OPENSHIFT_DOMAIN_PROD}"
                env.SUB_DOMAIN = "ocp"
                if (env.ENVIRONMENT == 'cert')
                {
                  env.ENV_GATEWAY_IPS = "${ENVIRONMENT}-api"
                }
                if (env.ENVIRONMENT == 'prod')
                {
                  env.ENV_GATEWAY_IPS = "${ENVIRONMENT}"
				        }    
              }
              if (env.STRATEGY == 'deploy_only')
              {
                sh "make nexus_login"
                env.ARTIFACT_VERSION = "${env.IMAGE_TAG.trim()}"
                env.IMAGE_DIR = getImageBranch("${env.ARTIFACT_VERSION}")
                echo "${env.ARTIFACT_VERSION}"
                env.TEMPLATE_FILE_MAIN_POSTFIX = "${TEMPLATE_FILE_POSTFIX}"
                env.CI_CD_USER_USERNAME="${CI_CD_TEST_USER}"
                env.CI_CD_USER_PASSWORD="${CI_CD_TEST_PASS}"
                env.MAJOR_VER = ""
                if (env.ENVIRONMENT == 'cert' || env.ENVIRONMENT == 'prod')
                {
                  env.CI_CD_USER_USERNAME="${CI_CD_USER}"
                  env.CI_CD_USER_PASSWORD="${CI_CD_PASS}"
                  env.IMAGE_DIR_TEMPLATE = ""
                      if (env.ENVIRONMENT == 'cert')
                      {
                        if (!env.ARTIFACT_VERSION.matches("([0-9]+)(.)([0-9]+)(.)([0-9]+)(-master-)([0-9]+)(-)([0-9]+)"))
                        {
                          echo "В ${env.ENVIRONMENT} допускаются образы, сгененированные из веток master и release"
                          currentBuild.result = "FAILURE"
                          error ("В ${env.ENVIRONMENT} допускаются образы, сгененированные из веток master и release")
                        }
                      }
                      if (env.ENVIRONMENT == 'prod')
                      {
                        if (!env.ARTIFACT_VERSION.matches("([0-9]+)(.)([0-9]+)(.)([0-9]+)(-master-)([0-9]+)(-)([0-9]+)"))
                        {
                          echo "В ${env.ENVIRONMENT} допускаются образы, сгененированные из веток master и release"
                          currentBuild.result = "FAILURE"
                          error ("В ${env.ENVIRONMENT} допускаются образы, сгененированные из веток master и release")
                        }
                      }
                    

                }
                else
                {
                  env.IMAGE_DIR_TEMPLATE = "/${env.IMAGE_DIR}"
                }
              }
              else
              {
                if (env.ENVIRONMENT == 'cert')
                {
                  env.IMAGE_DIR_TEMPLATE = ""
                }
                else
                {
                  env.IMAGE_DIR_TEMPLATE = "/${env.IMAGE_DIR}"
                }
              }
              if (env.ENVIRONMENT == 'cert' || env.ENVIRONMENT == 'prod')
              {
                sh "docker login -u ${CI_CD_USER_QUAY} -p \"${CI_CD_PASS_QUAY}\" ${QUAY_BASE_URL}"
                LOOKUP_TIMEOUT = "5"
                LOOKUP_IMAGE = "${QUAY_BASE_URL}/${NEXUS_FOLDER}/${APP_REPO_NAME}:${ARTIFACT_VERSION}"
                LOOKUP_QUERY = "Pulling|up to date|not found"
                LOOKUP_RESULT = sh(script: "timeout ${LOOKUP_TIMEOUT} docker 2>&1 pull ${LOOKUP_IMAGE} | egrep -o '${LOOKUP_QUERY}' | head -1", returnStdout: true)
                LOOKUP_408 = LOOKUP_RESULT.isEmpty()
                LOOKUP_404 = LOOKUP_RESULT.contains("not found")
                if (LOOKUP_408 || LOOKUP_404)
                {
                  echo "QUAY image lookup failed: Image not found in QUAY: ${LOOKUP_IMAGE}."
                  sh "oc image mirror ${NEXUS_BASE_URL}/${NEXUS_FOLDER}/${env.APP_REPO_NAME}/${env.IMAGE_DIR}:${env.ARTIFACT_VERSION} ${QUAY_BASE_URL}/${NEXUS_FOLDER}/${env.APP_REPO_NAME}:${env.ARTIFACT_VERSION} --insecure=true --skip-mount=true --force=true"
                }
                else
                {
                  echo "Image found in QUAY: ${LOOKUP_IMAGE}"
                  LOOKUP_PULLED_IMAGE = sh(script: "docker image ls -q --filter 'reference=${LOOKUP_IMAGE}'", returnStdout: true)
                  if (!LOOKUP_PULLED_IMAGE.isEmpty())
                  {
                     sh "docker image rm -f ${LOOKUP_PULLED_IMAGE}"
                  }
                }
              }
              if (env.ENVIRONMENT == 'prod')
              {
                if (env.TARGET_PLATFORM == 'main_and_dr' || env.TARGET_PLATFORM == 'dr')
                {
                  env.SUB_DOMAIN = "ocp1"
                  env.TEMPLATE_FILE_MAIN_POSTFIX = "${TEMPLATE_FILE_MAIN_AND_DR_POSTFIX}"
                  env.OPENSHIFT_URL = "${OPENSHIFT4_DR_URL}"
                  sh """
                    cp ${TEMPLATES_DIR}/${APP_REPO_NAME}-${TEMPLATE_FILE_POSTFIX}.yaml ${TEMPLATES_DIR}/${APP_REPO_NAME}-${TEMPLATE_FILE_MAIN_AND_DR_POSTFIX}.yaml
                    make prepare_yml
                    make deploy
                    make prepare_and_deploy_service_yml
                    make prepare_and_deploy_serviceMonitor_yml
                  """
                  env.GET_ERROR_PODS = sh (returnStdout: true, script: 'echo -n "$(oc get pods --no-headers | egrep "CrashLoopBackOff|Error" | grep -v deploy)"')
                  if (env.GET_ERROR_PODS != '')
                  {
                    echo "Имеются контейнеры, запущенные с ошибкой"
                    currentBuild.result = "UNSTABLE"
                  }
                }
                if (env.TARGET_PLATFORM == 'main_and_dr' || env.TARGET_PLATFORM == 'main')
                {
                  env.SUB_DOMAIN = "ocp"
                  env.TEMPLATE_FILE_MAIN_POSTFIX = "${TEMPLATE_FILE_POSTFIX}"
                  env.OPENSHIFT_URL = "${OPENSHIFT4_CERT_PROD_URL}"
                  sh """
                    make prepare_yml
                    make deploy
                    make prepare_and_deploy_service_yml
                    make prepare_and_deploy_serviceMonitor_yml
                  """
                  env.GET_ERROR_PODS = sh (returnStdout: true, script: 'echo -n "$(oc get pods --no-headers | egrep "CrashLoopBackOff|Error" | grep -v deploy)"')
                  if (env.GET_ERROR_PODS != '')
                  {
                    echo "Имеются контейнеры, запущенные с ошибкой"
                    currentBuild.result = "UNSTABLE"
                  }
                }
              }
              else
              {
                env.TEMPLATE_FILE_MAIN_POSTFIX = "${TEMPLATE_FILE_POSTFIX}"
                sh """
                  make nexus_login
                  make prepare_yml
                  make deploy
                  make prepare_and_deploy_service_yml
                  make prepare_and_deploy_serviceMonitor_yml
                """
                env.GET_ERROR_PODS = sh (returnStdout: true, script: 'echo -n "$(oc get pods --no-headers | egrep "CrashLoopBackOff|Error" | grep -v deploy)"')
                if (env.GET_ERROR_PODS != '')
                {
                  echo "Имеются контейнеры, запущенные с ошибкой"
                  currentBuild.result = "UNSTABLE"
                }
              }
            }
          }
         }
        }
       } 
  } 
    

  post
  {
    always
    {
      echo "Done"
    }
    cleanup
    {
      script
      {
        withEnv(['PATH+EXTRA=/usr/sbin:/usr/bin:/sbin:/bin'])
        {
          dir("/home/jenkins-agent/workspace/")
          {
            sh "ls"
            sh "rm -r ${WORKSPACE}"
          }
        }
      }
    }
  }
}


      

//*******Additional Methods*******//
String getImageBranch(String branch)
{
  def image_dir
  if (branch.contains('feature') || branch.contains('hotfix') || branch.contains('master') || branch.contains('release') || branch.contains('develop'))
  {
    if (branch.contains('release'))
    {
      image_dir = "release"
    }
    if (branch.contains('master'))
    {
      image_dir = "master"
    }
    if (branch.contains('feature'))
    {
      image_dir = "feature"
    }
    if (branch.contains('hotfix'))
    {
      image_dir = "hotfix"
    }
    if (branch.contains('develop'))
    {
      image_dir = "develop"
    }
  }
  else
  {
    image_dir = "others"
  }
  return image_dir
}

String getVersion()
{
  def MS_VERSION = readFile("version")
  return MS_VERSION
}
//*******End*******//
