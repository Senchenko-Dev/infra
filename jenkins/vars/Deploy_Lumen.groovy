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
        choice(name: 'ENV_SPEC', choices: ['test63','cert1','prod3,prod4,prod5,prod6','onboarding'],description: 'Выбирите куда хотите запустить Deploy')
        string(name: 'BUILD', defaultValue: '', description: 'Build number')
        string(name: 'BRANCH_NAME', defaultValue: 'master', description: 'Укажите имя ветки feature/ для CERT среды')
    }
    environment { // блок для описания переменных окружения
        BITBUCKET_PROJECT_KEY="PROPORT"
        BITBUCKET_REPO_PATH="${BITBUCKET_PROJECT_KEY.toLowerCase()}/${REPO_NAME}"
        REPO_NAME="lumen"
        APP_REPO_URL="https://gitlab.rosbank.rus.socgen/proport/lumen207.git" // адрес репозитория
        BITBUCKET_BASE_URL="https://gitlab.rosbank.rus.socgen" // адрес Gitlab server
        CREDENTIALSID = "Pro-portal-test"
        NEXUS_BASE_URL = "nexus.gts.rus.socgen"
        NEXUS_REPOSITORY_URL="https://nexus.gts.rus.socgen/repository/proportal-raw/lumen/"
        ANSIBLE_CONFIG = '${WORKSPACE}/deploy/ansible/ansible.cfg'
    }

    agent none

    stages  {
		stage('Deploy on lumen test63') {
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
                   gitClone("${params.BRANCH_NAME}")
                   buildAndPush()
                    withCredentials([file(credentialsId: 'proportal_vault_key',variable: 'VAULT_KEY_FILE')]){
                        sh "ANSIBLE_FORCE_COLOR=true ansible-playbook ${WORKSPACE}/deploy/ansible/playbooks.yml -i ${WORKSPACE}/deploy/ansible/inventory.yml --vault-password-file=$VAULT_KEY_FILE --extra-vars 'artifact_name=$ARTIFACT_NAME' --limit test63 -v" 
                    }
				 }
             }
             post {
                 cleanup{
                      deleteDir()
                  }
                  always {
                      script {
                           currentBuild.result = currentBuild.result ?: 'SUCCESS' || currentBuild.result ?: 'FAILURE'
						   sh "echo DEBUG: parameter BUILD = ${artifact_name}"
                    }
                }
            }
         }
    
	 stage('Check Website Response'){
		 agent {
		      label "PROPORTAL_linux_1"
		   }
		   when {
             expression {
                 params.ENV_SPEC.contains('test63')
               }
            }
            steps {
                script{
                    def response = sh(script: 'curl -s https://api.testproportal.trosbank.trus.tsocgen/api/v1/User/Whoaim', returnStdout: true).trim()
                         if (response.contains('[]')) {
                              echo "SUCCESS Ответ получен: []"
                          } else {
                              error "Ошибка,ответ не получен! Проверьте .env файл, routes и другие базовые вещи."
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
     stage('CERT') {
	    agent {
		    label "Proportal_agent-prod"
	    }
         when {
            expression {
                params.ENV_SPEC.contains('cert1')
            }
        }
        steps {
            script {
                def branchName = params.BRANCH_NAME
                def buildNumber = params.BUILD 

                if (branchName.isEmpty()) {
                    error("Ветка не указана")
                } else if (!buildNumber.isEmpty()) {
                    error("Нельзя указвать номер билда! нужно выбрать ветку")
                } else if (!(branchName.startsWith("feature/") || branchName.startsWith("hotfix/") || branchName == "master")) {
                    error("Название ветки должно быть master, либо начинаться с feature/, hotfix/")
                } else { 
                    gitClone("${params.BRANCH_NAME}")
                    buildAndPush()
                    withCredentials([file(credentialsId: 'proportal_vault_key_prod',variable: 'VAULT_KEY_FILE')]){
                        sh "ANSIBLE_FORCE_COLOR=true ansible-playbook ${WORKSPACE}/deploy/ansible/playbooks.yml -i ${WORKSPACE}/deploy/ansible/inventory.yml --vault-password-file=$VAULT_KEY_FILE --extra-vars 'artifact_name=$ARTIFACT_NAME' --limit cert1 -v" 
                    }
                }    
            }
        }
        post {
            cleanup{
                deleteDir()
            }
            always {
                script {
                    currentBuild.result = currentBuild.result ?: 'SUCCESS' || currentBuild.result ?: 'FAILURE' 
                    withCredentials([string(credentialsId: 'telegram_api', variable: "telegram_api")]) {
                    wrap([$class: 'BuildUser']) {
                    def fullname = env.BUILD_USER
                    def lastname = fullname.split()[0]
                    def jsonData = [
                        "token":"${telegram_api}",
                        "id":"-1001845199484",
                        "text":"Завершена сборка Lumen в CERT, билд ветки ${params.BRANCH_NAME}, результат: ${currentBuild.result}. Запустил: ${lastname}"
                    ]
                    writeJSON file: 'req.json', json: jsonData
                    sh "curl -k -X POST -H 'Content-Type: application/json' -d @req.json https://wso2ei.rsb.dmz.rus.socgen:443/telegram/v1.0/sendmessage"
                    }
                }
            }
        }
    }
}
stage('TEST ONBOARDING') {
	agent {
		label "PROPORTAL_linux_1"
	}
    when {
        expression {
            params.ENV_SPEC.contains('onboarding')
        }
    }
    steps {
        script {
            def branchName = params.BRANCH_NAME
            def buildNumber = params.BUILD 

            if (branchName.isEmpty()) {
                error("Ветка не указана")
            } else if (!buildNumber.isEmpty()) {
                error("Нельзя указвать номер билда! нужно выбрать ветку")
            } else if (!(branchName.startsWith("feature/") || branchName.startsWith("hotfix/") || branchName == "master")) {
                error("Название ветки должно быть master, либо начинаться с feature/, hotfix/")
            } else { 
                gitClone("${params.BRANCH_NAME}")
                buildAndPush()
                withCredentials([file(credentialsId: 'proportal_vault_key_prod',variable: 'VAULT_KEY_FILE')]){
                    sh "ANSIBLE_FORCE_COLOR=true ansible-playbook ${WORKSPACE}/deploy/ansible/playbooks.yml -i ${WORKSPACE}/deploy/ansible/inventory.yml --vault-password-file=$VAULT_KEY_FILE --extra-vars 'artifact_name=$ARTIFACT_NAME' --limit onboarding -v" 
                }
            }        
        }
    }
    post {
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
    stage('PROD') {
            agent {
                label "Proportal_agent-prod"
            }
            when {
                expression {
                    params.ENV_SPEC.contains('prod5,prod6')//contains проверяет строку на личее занчения
                }
            }
            steps {
                script {
                    gitClone("${params.BRANCH_NAME}")//git clone function
                    buildAndPush()
                    def hosts = params.ENV_SPEC.split(',') //парсим строку по запятой
                    def deployMap = [:] //map для добавления хостов (как пустой список в python)
                    hosts.each { host -> //цикл по каждому хосту
                        deployMap["${host}"] = { //проходися по каждому хосту и добавлем в map
                            script {
                                    deployToProd("${host}")
                                }
                            }
                        }
                        parallel deployMap
                        }
                    }
                    post {
                        success{
                            script{
                                wrap([$class: 'BuildUser']) {
                                def buildValue = params.BUILD
                                def jsonData = [
                                    "token":"5553799247:AAHxGorpa2bGOqxCodNldPfA3N6Skk7JHPw",
                                    "id":"-1001845199484",
                                    "text":"Завершена сборка Lumen в PROD, билд с номером №${buildValue}, результат: ${currentBuild.result}. Запустил: ${BUILD_USER}"
                                    ]
                                    writeJSON file: 'req.json', json: jsonData
                                    sh "curl -k -X POST -H 'Content-Type: application/json' -d @req.json https://wso2ei.rsb.dmz.rus.socgen:443/telegram/v1.0/sendmessage"
                                }
                            }
                        }
                        cleanup{
                            deleteDir()
                            sh "rm -rf /tmp/*.zip"
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
    artifact_name = sh (returnStdout: true, script: ''' echo -n "lumen---${BUILD_NUMBER}.zip" ''')
    env.artifact_name = artifact_name
    sh 'zip -1 -r ${artifact_name} ./ -x ./deploy**\\* ./vendor/symfony/finder/Tests/Fixtures**\\*  ./.git**\\*'
    withCredentials([usernamePassword(credentialsId: "${CREDENTIALSID}", passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]){
        sh 'curl -v -k -u $USERNAME:$PASSWORD --upload-file ${artifact_name} ${NEXUS_REPOSITORY_URL}'
    }
}

def deployToProd(String HOST){
    def buildNumber = params.BUILD // переопределяем params.BUILD в пременную buildNumber в которой содержится номер указанного билда в API jenkins
    if (buildNumber.isEmpty()) { // проверяем на пустую строку
            error("Надо указать номер билда!")
        } else {
            print "DEBUG: parameter ENV_SPEC = ${params.ENV_SPEC}  DEBUG: parameter BUILD = ${currentBuild.number}"
            withCredentials([file(credentialsId: 'proportal_vault_key_prod',variable: 'VAULT_KEY_FILE')]){
                env.artifact_name = "lumen---${buildNumber}.zip" //чтобы можно было запускать готовый билд указанный в API Jenkins
                sh "ANSIBLE_FORCE_COLOR=true ansible-playbook ${WORKSPACE}/deploy/ansible/playbooks.yml -i ${WORKSPACE}/deploy/ansible/inventory.yml --vault-password-file=$VAULT_KEY_FILE --extra-vars 'artifact_name=$ARTIFACT_NAME' --limit ${HOST} -v" 
            }
        }
    }
