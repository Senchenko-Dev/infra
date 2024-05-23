def call(){
    pipeline {
    options {
        buildDiscarder(logRotator(
            artifactDaysToKeepStr: '10',
            artifactNumToKeepStr: '100',
            daysToKeepStr: '30',
            numToKeepStr: '100'))
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout()
        ansiColor('xterm')
    }
    parameters {
        choice(name: 'ENV_SPEC', choices: ['test63','prod'],description: 'Выбирите куда хотите запустить Deploy')
        string(name: 'BRANCH_NAME', defaultValue: 'master', description: 'Укажите имя ветки feature/ или /hostfi xдля test среды')
    }
    environment { // блок для описания переменных окружения
        BITBUCKET_PROJECT_KEY="PROPORT"
        BITBUCKET_REPO_PATH="${BITBUCKET_PROJECT_KEY.toLowerCase()}/${REPO_NAME}"
        REPO_NAME="proagents"

        BITBUCKET_BASE_URL="https://gitlab.rosbank.rus.socgen" // адрес Gitlab server
        CREDENTIALSID = "Pro-portal-test"
        NEXUS_BASE_URL = "nexus.gts.rus.socgen"
        NEXUS_REPOSITORY_URL = "https://nexus.gts.rus.socgen/repository/proportal-raw/proagent/"
        ANSIBLE_CONFIG = '${WORKSPACE}/deploy/ansible/ansible.cfg'
    }

    agent none

    stages  {
//+----------------Deploy on Linux TEST--193.48.98.63-----------------+
		stage('Deploy on ProAgent test63') {
		   agent {
		      label "PROPORTAL_linux_1"
		   }
           when {
             expression {
                 params.ENV_SPEC.contains('test63')
               }
            }
			steps {
			  script {
                def branchName = params.BRANCH_NAME
                if (branchName.startsWith("master") || branchName.startsWith("feature/") || branchName.startsWith("hotfix/")) {
                    gitClone("${params.BRANCH_NAME}")
                    buildAndPush()
                    withCredentials([file(credentialsId: 'proportal_vault_key',variable: 'VAULT_KEY_FILE')]){
                        sh "ANSIBLE_FORCE_COLOR=true ansible-playbook ${WORKSPACE}/deploy/ansible/playbooks.yml -i ${WORKSPACE}/deploy/ansible/inventory.yml --vault-password-file=$VAULT_KEY_FILE --extra-vars 'artifact_name=$ARTIFACT_NAME' --limit test63 -v" 
                    }
				} else {
                    error("Название ветки должно начинаться с feature/ или hotfix/ или master")
                }
            }
        }
        post {
            cleanup{
                deleteDir()
            }
            always {
                script {
                    currentBuild.result = currentBuild.result ?: 'SUCCESS' || currentBuild.result ?: 'FAILURE'  //удаляет workspace при успешном или не успешной сборке
                }
            }
        }
    }
	stage('Deploy on ProAgent PROD') {
	    agent {
		    label "Proportal_agent-prod"
	    }
        when {
            expression {
                params.ENV_SPEC.contains('prod')
               }
            }
			steps {
			    script {
                    gitClone("${params.BRANCH_NAME}")
                    buildAndPush()
                    withCredentials([file(credentialsId: 'proportal_vault_key_prod',variable: 'VAULT_KEY_FILE')]){
                        sh "ANSIBLE_FORCE_COLOR=true ansible-playbook ${WORKSPACE}/deploy/ansible/playbooks.yml -i ${WORKSPACE}/deploy/ansible/inventory.yml --vault-password-file=$VAULT_KEY_FILE --extra-vars 'artifact_name=$ARTIFACT_NAME' --limit prod -v" 
					}
				}
             }
             post {
                success{
                    script {
                        currentBuild.result = currentBuild.result ?: 'SUCCESS' || currentBuild.result ?: 'FAILURE'  //удаляет workspace при успешном или не успешной сборке
                        withCredentials([string(credentialsId: 'telegram_api', variable: "telegram_api")]) {
                        wrap([$class: 'BuildUser']) {
                        def jsonData = [
                            "token":"${telegram_api}",
                            "id":"-1001845199484",
                            "text":"Завершена сборка ProAgentDMZ в PROD, билд с номером №${currentBuild.number}, результат: ${currentBuild.result}. Запустил: ${BUILD_USER}"
                            ]
                            writeJSON file: 'req.json', json: jsonData
                                sh "curl -k -X POST -H 'Content-Type: application/json' -d @req.json https://wso2ei.rsb.dmz.rus.socgen:443/telegram/v1.0/sendmessage"
                            }
                        }
                    }
                }
                cleanup{
                    deleteDir()
                    }
                    always {
                        script {
                            currentBuild.result = currentBuild.result ?: 'SUCCESS' || currentBuild.result ?: 'FAILURE' 
                        }
                    }
                }
            }
        }
    }
}

def gitClone(branchName) {
    sh "git config --global http.sslVerify false"  //git clone branch
    checkout([$class: 'GitSCM', branches: [[name: branchName]],doGenerateSubmoduleConfigurations: false,
    extensions: [[$class: 'CleanBeforeCheckout'],[$class: 'CloneOption',depth: 1,noTags: true,shallow: true]],submoduleCfg: [],
    userRemoteConfigs:[[credentialsId: "proportal_deploy",url: "${env.BITBUCKET_BASE_URL}/${env.BITBUCKET_REPO_PATH}.git"]]])
}

def buildAndPush() {
    print "DEBUG: parameter ENV_SPEC = ${params.ENV_SPEC}"
	print "DEBUG: parameter BUILD = ${BUILD_NUMBER}"
    sh 'cd vue && npm ci && npm run build && cd ..'
    sh 'rm -Rf ./vue/node_modules'
    artifact_name = sh (returnStdout: true, script: ''' echo -n "proagent---${BUILD_NUMBER}.zip" ''') 
    env.artifact_name = artifact_name
    sh 'zip -1 -r ${artifact_name} ./ -x ./deploy**\\* ./.git**\\*'
    withCredentials([usernamePassword(credentialsId: "${CREDENTIALSID}", passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
    sh 'curl -v -k -u $USERNAME:$PASSWORD --upload-file ${artifact_name} ${NEXUS_REPOSITORY_URL}'
    }
}
