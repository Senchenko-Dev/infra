pipeline
{
  agent 
  {
    node 
    {
      label 'Proportal_agent-cert'
    }
  }
  triggers 
  { 
    cron('H/5 * * * *') 
  }
    
  options
  {
    buildDiscarder(logRotator(artifactDaysToKeepStr: '14', artifactNumToKeepStr: '14', daysToKeepStr: '14', numToKeepStr: '20'))
    timestamps()
    skipDefaultCheckout()
    disableConcurrentBuilds()
  }

  parameters
  {
   booleanParam(name: 'test_pro_proportal',defaultValue: 'true',description: 'Apply template on TEST')
   booleanParam(name: 'cert_pro_proportal',defaultValue: 'true',description: 'Apply template on CERT')
   booleanParam(name: 'prod_pro_proportal',defaultValue: 'false',description: 'Apply template on PROD/DR')

  }

  environment
  {
    COMMON_PATH = "Namespace-configs/Common-config"
    DIFF_PATH = "Namespace-configs/Diff-config"
    GIT_BASE_URL = "https://gitlab.rosbank.rus.socgen"
    GROUP_PATH = "proport"
    GROUP_ID = "ProPortal"
    GROUP_ID_TEMPLATE = "${GROUP_ID.toLowerCase()}"
    REPO_PATH="infra-gitops"
    OPENSHIFT_URL_TEST  = "https://api.tpaas.trosbank.trus.tsocgen:6443"
    OPENSHIFT_CERT_PROD_URL = "https://api.ocp.rosbank.rus.socgen:6443"
    OPENSHIFT_DR_URL = "https://api.ocp1.rosbank.rus.socgen:6443"
    OPENSHIFT_DOMAIN_PROD = "rosbank.rus.socgen"
    OPENSHIFT_DOMAIN_TEST = "trosbank.trus.tsocgen"
    NAMESPACE_TEST_NAME = "test-pro-proportal"
    NAMESPACE_CERT_NAME = "cert-pro-proportal"
    NAMESPACE_PROD_NAME = "prod-pro-proportal"    
  }
  stages{
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
    stage ('apply')
    {
        steps{
            withCredentials([
          usernamePassword
          (
            credentialsId: "Pro-portal-test",
            passwordVariable: 'CI_CD_USER_PASSWORD_TEST',
			usernameVariable: 'CI_CD_USER_USERNAME_TEST'
          ),
          usernamePassword
          (
            credentialsId: "Pro-portal-prod",
            passwordVariable: 'CI_CD_USER_PASSWORD',
			      usernameVariable: 'CI_CD_USER_USERNAME'
          )])
          {
            script{
                if(env.test_pro_proportal == 'true'){
                    sh "oc login ${OPENSHIFT_URL_TEST} --username=${CI_CD_USER_USERNAME_TEST}@${OPENSHIFT_DOMAIN_TEST} --password='${CI_CD_USER_PASSWORD_TEST}'  -n=${NAMESPACE_TEST_NAME} --insecure-skip-tls-verify=true "
                    sh "oc apply -R -f ${COMMON_PATH}/${NAMESPACE_TEST_NAME}"
                    sh "oc logout"
                }
                else{
                    println("Пропустить TEST")
                }
                if(env.cert_pro_proportal == 'true'){
                    sh """
                    oc login ${OPENSHIFT_CERT_PROD_URL} --username=${CI_CD_USER_USERNAME}@${OPENSHIFT_DOMAIN_PROD} --password='${CI_CD_USER_PASSWORD}'  -n=${NAMESPACE_CERT_NAME} --insecure-skip-tls-verify=true 
                    oc apply -R -f ${COMMON_PATH}/${NAMESPACE_CERT_NAME} 
                    oc logout
                    """
                }
                else{
                    println("Пропустить CERT")
                }
                if(env.prod_pro_proportal == 'true'){
                    sh """
                    oc login ${OPENSHIFT_CERT_PROD_URL} --username=${CI_CD_USER_USERNAME}@${OPENSHIFT_DOMAIN_PROD} --password='${CI_CD_USER_PASSWORD}'  -n=${NAMESPACE_PROD_NAME} --insecure-skip-tls-verify=true  
                    oc apply -R -f ${COMMON_PATH}/${NAMESPACE_PROD_NAME} 
                    oc apply -f ${DIFF_PATH}/${NAMESPACE_PROD_NAME}/configMaps/pro-prod-dbopro-map.yaml
                    oc apply -f ${DIFF_PATH}/${NAMESPACE_PROD_NAME}/volumes/prod-volumes.yaml
                    oc logout
                    """
                    sh """
                    oc login ${OPENSHIFT_DR_URL} --username=${CI_CD_USER_USERNAME}@${OPENSHIFT_DOMAIN_PROD} --password='${CI_CD_USER_PASSWORD}'  -n=${NAMESPACE_PROD_NAME} --insecure-skip-tls-verify=true 
                    oc apply -R -f ${COMMON_PATH}/${NAMESPACE_PROD_NAME} 
                    oc apply -f ${DIFF_PATH}/${NAMESPACE_PROD_NAME}/configMaps/pro-dr-dbopro-map.yaml
                    oc apply -f ${DIFF_PATH}/${NAMESPACE_PROD_NAME}/volumes/dr-volumes.yaml
                    oc logout
                    """
                }
                else{
                    println("Пропустить PROD")
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
