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
    choice(name: 'HOTFIX', choices: ['no','yes'], description: 'Только для прода')
    string(name: 'HOTFIX_ISSUES', defaultValue: '', description: 'Только для HOTFIX. Номер задачи в jira(например PRODBO-18048)')
    string(name: 'PODS_COUNT', defaultValue: '1', description: 'Только для deploy_only и build_and_deploy. Количество запущенных экземпляров приложения')
    string(name: 'DEPLOYMENT_TIMEOUT_SEC', defaultValue: '600', description: 'Только для deploy_only и build_and_deploy. Таймаут ожидания деплоя (в секундах)')
    choice(name: 'GRADLE_OPTIONS', choices: ['', '--refresh-dependencies','--stacktrace','--debug'], description: 'Cборка с доп. опциями gradle')
  }

  environment
  {
    GIT_BASE_URL = "https://gitlab.rosbank.rus.socgen"
    DOCKER_FROM_11 = "openjdk:11-jdk-slim-debian_v3"
    DOCKER_FROM_17 = "openjdk:17-jdk-slim-debian_v3"
    DOCKER_USER = "admin"
    FILE_DIR = "./openshift/jenkins/RFC"
    GIT_BASE_URL_SHORT = "gitlab.rosbank.rus.socgen"
    GROOVY_SCRIPTS_DIR="./openshift/groovy_files"
    GROUP_ID = "ProPortal"
    GROUP_ID_L = (GROUP_ID.toLowerCase())
    GROUP_ID_TEMPLATE = "pport"
    GROUP_PATH = "proport"
    INFRA_APPS_SCRIPTS_DIR = "./openshift/apps_scripts"
    JDK_IMAGE_11 = "${NEXUS_BASE_URL}/${NEXUS_FOLDER}/jdk:openjdk-11.2"
    JDK_IMAGE_17 = "${NEXUS_BASE_URL}/${NEXUS_FOLDER}/jdk:openjdk-17.9"
    JENKINS_GROUP_DIR = "/home/jenkins-agent/workspace/${GROUP_ID}"
    MAJOR_VER_APP = "1"
    NEXUS_BASE_URL = "docker-registry.gts.rus.socgen"
    NEXUS_FOLDER = "pport"
    NEXUS_GRADLE_URL = "nexus.gts.rus.socgen"
    OPENSHIFT_BUILDS_HISTORY_DIR = "./openshift/builds_history"
    OPENSHIFT_DOMAIN_PROD = "rosbank.rus.socgen"
    OPENSHIFT_DOMAIN_TEST = "trosbank.trus.tsocgen"
    OPENSHIFT_SCRIPTS_DIR = "./openshift/scripts"
    OPENSHIFT4_CERT_PROD_URL = "https://api.ocp.rosbank.rus.socgen:6443"
    OPENSHIFT4_DR_URL = "https://api.ocp1.rosbank.rus.socgen:6443"
    OPENSHIFT4_TEST_URL = "https://api.tpaas.trosbank.trus.tsocgen:6443"
    OUTSIDERS_RULE = "true"
    QUAY_BASE_URL = "quay.gts.rus.socgen"
    REPO_PATH="infra"
    SONAR_IMAGE = 'docker-registry.gts.rus.socgen/general/sonarscanner:rb-4.6.2.2472'
    TEMPLATE_FILE_MAIN_AND_DR_POSTFIX = "template-dr"
    TEMPLATE_FILE_POSTFIX = "template"
    TEMPLATES_DIR = "./openshift/templates"
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


    stage('Select JDK_IMAGE and DOCKER_FROM if it needs') {
       steps {
         script {
           env.JDK_IMAGE = get_jdk_version ("gradle.properties")
           env.DOCKER_FROM = get_docker_from_version ("gradle.properties")
         }
       }
     }

    stage ('build jar and image')
    {
      steps
      {
        withCredentials([
          usernamePassword
          (
            credentialsId: "Pro-portal-test",
            passwordVariable: 'CI_CD_PASS',
			      usernameVariable: 'CI_CD_USER'
          )])
        {
          script
          {
            sh """
              mv ./openshift/Makefile ./
              cp -r ${TEMPLATES_DIR}/Dockerfile ./
            """
            env.APP_VER = get_version (get_version_file(),"versions.app_version")
            env.APP_VER_YAML = "${env.APP_VER.replace('.','-')}"
            env.TAG_VER = sh (returnStdout: true, script: 'echo -n $(date +%Y%m%d-%H%M)')
            env.COMMIT_ID = sh (returnStdout: true, script: 'git log -n1 --format="%h"')
            env.OPENSHIFT_PLATFORM_PRISMA = "ocp4-test"
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
                if (env.STRATEGY == 'build_and_deploy' && !(env.BRANCH.matches("(release/sprint-)([0-9]+)")))
                {
                  echo "Ветки, отличные от release, не применимы к стратегии ${STRATEGY} в ${ENVIRONMENT} среде"
                  currentBuild.result = "FAILURE"
                  error ('Ветки, отличные от release, не применимы к стратегии ${STRATEGY} в ${ENVIRONMENT} среде')
                }
              }
              env.IMAGE_DIR = getImageBranch("${env.BRANCH}")
              env.ARTIFACT_VERSION = "${env.APP_VER}" + "-${env.BRANCH.replace('/','_')}" + "-${env.TAG_VER}"
              if (APP_REPO_NAME == 'nginx')
              {
                 env.ARTIFACT_VERSION += "-${ENVIRONMENT}"
              }
              if (APP_REPO_NAME == 'is-api')
              {
                 env.MAJOR_VER = "-protocol-" + get_version("${IS_PROTOCOL_DIR}/Application.java","public static final String PROTOCOL").replace('.','-').replace(';','')
                 env.MAJOR_VER_IS = "${MAJOR_VER.substring(1,MAJOR_VER.length())}"
                 env.ARTIFACT_VERSION += "-${env.MAJOR_VER_IS}"
              }
              else
              {
                env.MAJOR_VER = ""
              }
              echo "${env.ARTIFACT_VERSION}"
              sh "make nexus_login"
              def jar_app = fileExists "gradlew"
              if (jar_app)
              {
                sh "make build_jar"
              }
              sh "make nexus_make_image"
              IS_IMAGE_EXISTS = "true"
            }
          }
        }
      }
    }

    stage ('push and delete local image')
    {
      steps
      {
        script
        {
           if (env.STRATEGY == 'build_and_deploy' || env.STRATEGY == 'build_only')
           {
             sh "make nexus_push_image"
           }
           if (env.STRATEGY != 'deploy_only')
           {
             sh "make local_image_delete"
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
                if (APP_REPO_NAME == 'is-api')
                {
                  env.MAJOR_VER = "-${env.ARTIFACT_VERSION.substring(env.ARTIFACT_VERSION.indexOf('-protocol-')+1)}"
                  env.MAJOR_VER_IS = "${MAJOR_VER.substring(1,MAJOR_VER.length())}"
                }
                else
                {
                  env.MAJOR_VER = ""
                }
                if (env.ENVIRONMENT == 'cert' || env.ENVIRONMENT == 'prod')
                {
                  env.IMAGE_DIR_TEMPLATE = ""
                  wrap([$class: 'BuildUser'])
                  {
                    if (BUILD_USER != 'Зенов Владислав Вячеславович')
                    {
                      if (env.ENVIRONMENT == 'cert')
                      {
                        if (!(env.ARTIFACT_VERSION.matches("([0-9]+)(.)([0-9]+)(.)([0-9]+)(-release_sprint-)([0-9]+)(-)([0-9]+)(-)([0-9]+)(-protocol-)(.*)")
                           || env.ARTIFACT_VERSION.matches("([0-9]+)(.)([0-9]+)(.)([0-9]+)(-release_sprint-)([0-9]+)(-)([0-9]+)(-)([0-9]+)")))
                        {
                          echo "В ${env.ENVIRONMENT} допускаются образы, сгененированные из веток master и release"
                          currentBuild.result = "FAILURE"
                          error ("В ${env.ENVIRONMENT} допускаются образы, сгененированные из веток master и release")
                        }
                      }
                      if (env.ENVIRONMENT == 'prod')
                      {
                        if (!(env.ARTIFACT_VERSION.matches("([0-9]+)(.)([0-9]+)(.)([0-9]+)(-release_sprint-)([0-9]+)(-)([0-9]+)(-)([0-9]+)(-protocol-)(.*)")
                           || env.ARTIFACT_VERSION.matches("([0-9]+)(.)([0-9]+)(.)([0-9]+)(-release_sprint-)([0-9]+)(-)([0-9]+)(-)([0-9]+)")))
                        {
                          echo "В ${env.ENVIRONMENT} допускаются образы, сгененированные из веток master и release"
                          currentBuild.result = "FAILURE"
                          error ("В ${env.ENVIRONMENT} допускаются образы, сгененированные из веток master и release")
                        }
                      }
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
                LOOKUP_IMAGE = "${QUAY_BASE_URL}/${GROUP_ID_TEMPLATE}/${APP_REPO_NAME}:${ARTIFACT_VERSION}"
                LOOKUP_QUERY = "Pulling|up to date|not found"
                LOOKUP_RESULT = sh(script: "timeout ${LOOKUP_TIMEOUT} docker 2>&1 pull ${LOOKUP_IMAGE} | egrep -o '${LOOKUP_QUERY}' | head -1", returnStdout: true)
                LOOKUP_408 = LOOKUP_RESULT.isEmpty()
                LOOKUP_404 = LOOKUP_RESULT.contains("not found")
                if (LOOKUP_408 || LOOKUP_404)
                {
                  echo "QUAY image lookup failed: Image not found in QUAY: ${LOOKUP_IMAGE}."
                  sh "oc image mirror ${NEXUS_BASE_URL}/${GROUP_ID_TEMPLATE}/${env.APP_REPO_NAME}/${env.IMAGE_DIR}:${env.ARTIFACT_VERSION} ${QUAY_BASE_URL}/${GROUP_ID_TEMPLATE}/${env.APP_REPO_NAME}:${env.ARTIFACT_VERSION} --insecure=true --skip-mount=true --force=true"
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
                    make deploy_java
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
                    make deploy_java
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
                  make prepare_yml
                  make deploy_java
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
String get_version_file ()
{
  try
  {
    String get_file = null
    if (fileExists("build.gradle"))
    {
      get_file = "build.gradle"
    }
    else
    {
      get_file = "build.gradle.kts"
    }
    return get_file
  }
  catch (err)
  {
    println err
    throw new Exception().printStackTrace()
  }
}

String get_version (String file, String ver_pattern)
{
  try
  {
    String app_version = null
    file_content = readFile file
    file_content.split('\n').each
    {
      String line ->
      if (line.contains(ver_pattern))
      {
        if (line.trim().substring(0,ver_pattern.trim().length()).equals(ver_pattern))
        {
          app_version = line.replace(ver_pattern,'').replace('=','').replace("'","").replace('"','').trim()
          println app_version
          return true
        }
      }
    }
    return app_version
  }
  catch (err)
  {
    println err
    throw new Exception().printStackTrace()
  }
}

String get_jdk_version (String filename) {
  try {
    def filecontent = readFile filename
    def matcher = filecontent =~ /platformVersion\s*=\s*(\d)/
    if (matcher[0][1] == "2") {
        return "${JDK_IMAGE_17}"
    }
    else{
        return "${JDK_IMAGE_11}"
    }
  } catch (err) {
    println err
  }
}
String get_docker_from_version (String filename) {
  try {
    def filecontent = readFile filename
    def matcher = filecontent =~ /platformVersion\s*=\s*(\d)/
    if (matcher[0][1] == "2") {
        return "${DOCKER_FROM_17}"
    }
    else{
        return "${DOCKER_FROM_11}"
    }
  } catch (err) {
    println err
  }
}

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
//*******End*******//


