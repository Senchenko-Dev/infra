pipeline {
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
    
  parameters
  {
   booleanParam(name: 'test_pro_proportal',defaultValue: 'true',description: 'Apply template on TEST')
   booleanParam(name: 'cert_pro_proportal',defaultValue: 'true',description: 'Apply template on CERT')
   booleanParam(name: 'prod_pro_proportal',defaultValue: 'false',description: 'Apply template on PROD/DR')

  }

  environment
  {
      GROUP_PATH = "proport"
      GIT_BASE_URL = "https://gitlab.rosbank.rus.socgen"
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

    stages {
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
    stage('diff'){
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
          )]){
            script{
                if(env.test_pro_proportal == 'true'){
                sh "oc login ${OPENSHIFT_URL_TEST} --username=${CI_CD_USER_USERNAME_TEST}@${OPENSHIFT_DOMAIN_TEST} --password='${CI_CD_USER_PASSWORD_TEST}'  -n=${NAMESPACE_TEST_NAME} --insecure-skip-tls-verify=true "
                sh "oc get rolebindings -o json | jq \'.items[] | select((.roleRef.name==\"edit\") or (.roleRef.name==\"admin\") or (.roleRef.name==\"view\")) | .roleRef.name + \" \" + .subjects[].name \' > Project-access/${NAMESPACE_TEST_NAME}/rolebindingsCurrent"
                if ( sh (script: "grep -vf Project-access/${NAMESPACE_TEST_NAME}/rolebindingsCurrent Project-access/${NAMESPACE_TEST_NAME}/rolebindings", returnStatus: true) == 0 ){
                    add= sh(script: "grep -vf Project-access/${NAMESPACE_TEST_NAME}/rolebindingsCurrent Project-access/${NAMESPACE_TEST_NAME}/rolebindings", returnStdout: true).replace("\"", " ")
                    add_array= add.split('\n').collect{it as String}
                    println("added =" + add_array)
                    for(int i =0; i<add_array.size(); i++){
                        sh "oc adm policy add-role-to-user${add_array[i]}"
                    }
                }
                else{
                    println("INFO: No one has been added to project")
                }
                if ( sh (script:"grep -vf Project-access/${NAMESPACE_TEST_NAME}/rolebindings Project-access/${NAMESPACE_TEST_NAME}/rolebindingsCurrent", returnStdout: true, returnStatus: true) == 0){
                    remove= sh(script: "grep -vf Project-access/${NAMESPACE_TEST_NAME}/rolebindings Project-access/${NAMESPACE_TEST_NAME}/rolebindingsCurrent", returnStdout: true).replace("\"", " ")
                    remove_array= remove.split('\n').collect{it as String}
                    println("removed =" + remove_array)
                    for(int i =0; i<remove_array.size(); i++){
                        sh "oc adm policy remove-role-from-user${remove_array[i]}"
                    }
                }
                else
                {
                    println("INFO: No one has been removed from project")
                }               
            }
            if(env.cert_pro_proportal == 'true'){
                sh "oc login ${OPENSHIFT_CERT_PROD_URL} --username=${CI_CD_USER_USERNAME}@${OPENSHIFT_DOMAIN_PROD} --password='${CI_CD_USER_PASSWORD}'  -n=${NAMESPACE_CERT_NAME} --insecure-skip-tls-verify=true "
                sh "oc get rolebindings -o json | jq \'.items[] | select((.roleRef.name==\"edit\") or (.roleRef.name==\"admin\") or (.roleRef.name==\"view\")) | .roleRef.name + \" \" + .subjects[].name \' >> Project-access/${NAMESPACE_CERT_NAME}/rolebindingsCurrent"
                if ( sh (script: "grep -vf Project-access/${NAMESPACE_CERT_NAME}/rolebindingsCurrent Project-access/${NAMESPACE_CERT_NAME}/rolebindings", returnStatus: true) == 0 ){
                    add= sh(script: "grep -vf Project-access/${NAMESPACE_CERT_NAME}/rolebindingsCurrent Project-access/${NAMESPACE_CERT_NAME}/rolebindings", returnStdout: true).replace("\"", " ")
                    add_array= add.split('\n').collect{it as String}
                    println("added =" + add_array)
                    for(int i =0; i<add_array.size(); i++){
                        sh "oc adm policy add-role-to-user${add_array[i]}"
                    }
                }
                else{
                    println("INFO: No one has been added to project")
                }
                if ( sh (script:"grep -vf Project-access/${NAMESPACE_CERT_NAME}/rolebindings Project-access/${NAMESPACE_CERT_NAME}/rolebindingsCurrent", returnStdout: true, returnStatus: true) == 0){
                    remove= sh(script: "grep -vf Project-access/${NAMESPACE_CERT_NAME}/rolebindings Project-access/${NAMESPACE_CERT_NAME}/rolebindingsCurrent", returnStdout: true).replace("\"", " ")
                    remove_array= remove.split('\n').collect{it as String}
                    println("removed =" + remove_array)
                    for(int i =0; i<remove_array.size(); i++){
                        sh "oc adm policy remove-role-from-user${remove_array[i]}"
                    }
                }
                else
                {
                    println("INFO: No one has been removed from project")
                }               
            }
            if(env.prod_pro_proportal == 'true'){
                sh "oc login ${OPENSHIFT_CERT_PROD_URL} --username=${CI_CD_USER_USERNAME}@${OPENSHIFT_DOMAIN_PROD} --password='${CI_CD_USER_PASSWORD}'  -n=${NAMESPACE_PROD_NAME} --insecure-skip-tls-verify=true "
                sh "oc get rolebindings -o json | jq \'.items[] | select((.roleRef.name==\"edit\") or (.roleRef.name==\"admin\") or (.roleRef.name==\"view\")) | .roleRef.name + \" \" + .subjects[].name \' >> Project-access/${NAMESPACE_PROD_NAME}/rolebindingsCurrent"
                if ( sh (script: "grep -vf Project-access/${NAMESPACE_PROD_NAME}/rolebindingsCurrent Project-access/${NAMESPACE_PROD_NAME}/rolebindings", returnStatus: true) == 0 ){
                    add= sh(script: "grep -vf Project-access/${NAMESPACE_PROD_NAME}/rolebindingsCurrent Project-access/${NAMESPACE_PROD_NAME}/rolebindings", returnStdout: true).replace("\"", " ")
                    add_array= add.split('\n').collect{it as String}
                    println("added =" + add_array)
                    for(int i =0; i<add_array.size(); i++){
                        sh "oc adm policy add-role-to-user${add_array[i]}"
                    }
                }
                else{
                    println("INFO: No one has been added to project")
                }
                if ( sh (script:"grep -vf Project-access/${NAMESPACE_PROD_NAME}/rolebindings Project-access/${NAMESPACE_PROD_NAME}/rolebindingsCurrent", returnStdout: true, returnStatus: true) == 0){
                    remove= sh(script: "grep -vf Project-access/${NAMESPACE_PROD_NAME}/rolebindings Project-access/${NAMESPACE_PROD_NAME}/rolebindingsCurrent", returnStdout: true).replace("\"", " ")
                    remove_array= remove.split('\n').collect{it as String}
                    println("removed =" + remove_array)
                    for(int i =0; i<remove_array.size(); i++){
                        sh "oc adm policy remove-role-from-user${remove_array[i]}"
                    }
                }
                else
                {
                    println("INFO: No one has been removed from project")
                }                            
            }
            if(env.prod_pro_proportal == 'true'){
                sh "oc login ${OPENSHIFT_DR_URL} --username=${CI_CD_USER_USERNAME}@${OPENSHIFT_DOMAIN_PROD} --password='${CI_CD_USER_PASSWORD}'  -n=${NAMESPACE_PROD_NAME} --insecure-skip-tls-verify=true "
                sh "oc get rolebindings -o json | jq \'.items[] | select((.roleRef.name==\"edit\") or (.roleRef.name==\"admin\") or (.roleRef.name==\"view\")) | .roleRef.name + \" \" + .subjects[].name \' >> Project-access/${NAMESPACE_PROD_NAME}/rolebindingsCurrent"
                if ( sh (script: "grep -vf Project-access/${NAMESPACE_PROD_NAME}/rolebindingsCurrent Project-access/${NAMESPACE_PROD_NAME}/rolebindings", returnStatus: true) == 0 ){
                    add= sh(script: "grep -vf Project-access/${NAMESPACE_PROD_NAME}/rolebindingsCurrent Project-access/${NAMESPACE_PROD_NAME}/rolebindings", returnStdout: true).replace("\"", " ")
                    add_array= add.split('\n').collect{it as String}
                    println("added =" + add_array)
                    for(int i =0; i<add_array.size(); i++){
                        sh "oc adm policy add-role-to-user${add_array[i]}"
                    }
                }
                else{
                    println("INFO: No one has been added to project")
                }
                if ( sh (script:"grep -vf Project-access/${NAMESPACE_PROD_NAME}/rolebindings Project-access/${NAMESPACE_PROD_NAME}/rolebindingsCurrent", returnStdout: true, returnStatus: true) == 0){
                    remove= sh(script: "grep -vf Project-access/${NAMESPACE_PROD_NAME}/rolebindings Project-access/${NAMESPACE_PROD_NAME}/rolebindingsCurrent", returnStdout: true).replace("\"", " ")
                    remove_array= remove.split('\n').collect{it as String}
                    println("removed =" + remove_array)
                    for(int i =0; i<remove_array.size(); i++){
                        sh "oc adm policy remove-role-from-user${remove_array[i]}"
                    }
                }
                else
                {
                    println("INFO: No one has been removed from project")
                }               
            }
            }
        }}
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
