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
            choice(name: 'ENV_SPEC', choices: ['test63','cert1','prod3,prod4,prod5,prod6','onboarding','payboosters','proleads','agents','payroll','corp','corp2','corp3','proloans','pro_sales_front_1','pro_sales_front_2','profuture'],description: 'Выбирите куда хотите запустить Deploy')
            string(name: 'BUILD', defaultValue: '', description: 'Build number')
            string(name: 'BRANCH_NAME', defaultValue: 'master', description: 'Укажите имя ветки proportal backend. Только для TEST, CERT.')
            string(name: 'BUILD_FRONT_VUE',defaultValue: 'latest', description: 'Номер сборки vue-фронта. Только для TEST, CERT. Подробнее: https://kb.rosbank.rus.socgen/pages/viewpage.action?pageId=609086929&src=jira')
        }
        environment { // блок для описания переменных окружения
            BITBUCKET_PROJECT_KEY="PROPORT"
            BITBUCKET_REPO_PATH="${BITBUCKET_PROJECT_KEY.toLowerCase()}/${REPO_NAME}"
            REPO_NAME="proportal"

            BITBUCKET_REPO_PATH_TESTS="${BITBUCKET_PROJECT_KEY.toLowerCase()}/${REPO_NAME_TESTS}"
            REPO_NAME_TESTS="proportal_tests"

            BITBUCKET_BASE_URL="https://gitlab.rosbank.rus.socgen" // адрес Gitlab server
            CREDENTIALSID = "Pro-portal-test"
            NEXUS_BASE_URL = "nexus.gts.rus.socgen"
            NEXUS_REPOSITORY_URL =  "https://nexus.gts.rus.socgen/repository/proportal-raw/proportal/"
            NEXUS_REPOSITORY_PROPORTAL_VUE_URL =  "https://nexus.gts.rus.socgen/repository/proportal-raw/proportal-vue"
            ANSIBLE_CONFIG = '${WORKSPACE}/deploy/ansible/ansible.cfg'
            FILE_DIR = "./deploy/pull_request/RFC"
            PROJECTKEY = "CI139032"
        }

        agent {
            label "Proportal_agent-cert"
        }

        stages  {
            stage('TEST') {
                when {
                    expression {
                        params.ENV_SPEC.contains('test63') //contains проверяет строку на наличее занчения test63
                    }
                }
                steps {
                    script {
                        def buildNumber = params.BUILD 
                        def branchName = params.BRANCH_NAME
                        def buildNumberFrontVue = params.BUILD_FRONT_VUE
                        if (!buildNumber.isEmpty()) {
                            error('Нельзя указывать номер билда')
                        } else {
                            gitClone("${branchName}")
                            vue("${buildNumberFrontVue}","${branchName}")  
                            buildAndPush()
                            print "DEBUG: parameter ENV_SPEC = ${params.ENV_SPEC}  DEBUG: parameter BUILD = ${currentBuild.number}"
                            withCredentials([file(credentialsId: 'proportal_vault_key',variable: 'VAULT_KEY_FILE')]){
                                sh "ANSIBLE_FORCE_COLOR=true ansible-playbook ${WORKSPACE}/deploy/ansible/playbooks.yml -i ${WORKSPACE}/deploy/ansible/inventory.yml --vault-password-file=$VAULT_KEY_FILE --extra-vars 'artifact_name=$ARTIFACT_NAME' --limit test63 -v" 
                            }
                        }
                    }
                }
                post {
                    cleanup{
                        deleteDir()
                        sh "rm -rf /tmp/*.tar.gz"
                        sh "rm -rf ../*.tar.gz"
                    }
                    always {
                        script {
                            currentBuild.result = currentBuild.result ?: 'SUCCESS' || currentBuild.result ?: 'FAILURE'  //удаляет workspace при успешном или не успешной сборке
                        }
                    }
                }
            }
            stage('Run Api Tests') {
                when {
                    expression {
                        params.ENV_SPEC.contains('test63')
                    }
                }
                steps {
                    sh "git config --global http.sslVerify false"  //git clone branch
                    checkout([$class: 'GitSCM',branches: [[name: "master"]],doGenerateSubmoduleConfigurations: false,
                        extensions: [[$class: 'CleanBeforeCheckout'],[$class: 'CloneOption',depth: 1,noTags: true,shallow: true]],submoduleCfg: [],
                        userRemoteConfigs:[[credentialsId: "proportal_deploy",url: "${env.BITBUCKET_BASE_URL}/${env.BITBUCKET_REPO_PATH_TESTS}.git"]]
                        ])
                        sh 'npm cache clean --force'
                        sh 'npm uninstall playwright'
                        sh 'npm install -D @playwright/test'
                        sh 'npm i playwright playwright-video'
                        sh 'export PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 npm install'
                        sh 'npx playwright test'    
                        allure commandline: 'PPORT_ALLUR', includeProperties: false, jdk: '', results: [[path: 'allure-results']]
                }
                post {                   
                    failure {
                        script{
                            allure commandline: 'PPORT_ALLUR', includeProperties: false, jdk: '', results: [[path: 'allure-results']]
                            currentBuild.displayName = "!!! ОШИБКИ В ТЕСТАХ !!!"
                            emailext(
                            attachLog: true,
                            subject: "Джоб с номером N${currentBuild.number}",
                            body: "Сборка завершена с ошибкой,логи во вложении",
                            to: 'yuriy.lobanov@rosbank.ru'
                            )
                            withCredentials([usernamePassword(credentialsId: "${CREDENTIALSID}", passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                                sh "curl -X DELETE -u $USERNAME:$PASSWORD ${NEXUS_REPOSITORY_URL}proportal---${currentBuild.number}.zip"
                            }  
                        }
                    }
                    always {
                        script {
                            currentBuild.result = currentBuild.result ?: 'SUCCESS' || currentBuild.result ?: 'FAILURE'  //удаляет workspace при успешном или не успешной сборке
                        }
                    }
                    cleanup{
                        deleteDir()
                    }
                }
            }
            stage('DEPLOY TEST FOR TEAM') {
                when {
                    expression {
                        def myList = ['onboarding','payboosters','proleads','agents','payroll','corp','proloans','pro_sales_front_1','pro_sales_front_2','profuture','corp2','corp3']
                        return myList.any { value -> 
                            params.ENV_SPEC.contains(value) 
                        }
                    }
                }
                steps {
                    script {
                        def envSpec = params.ENV_SPEC
                        def branchName = params.BRANCH_NAME
                        def buildNumberFrontVue = params.BUILD_FRONT_VUE
                        
                        if (branchName.toLowerCase().startsWith("feature/") || branchName.toLowerCase().startsWith("hotfix/") || branchName == "master") {
                            gitClone("${branchName}")
                            vue("${buildNumberFrontVue}","${branchName}")  
                            buildAndPush()
                            print "DEBUG: parameter ENV_SPEC = ${params.ENV_SPEC}  DEBUG: parameter BUILD = ${currentBuild.number}"
                            withCredentials([file(credentialsId: 'proportal_vault_key',variable: 'VAULT_KEY_FILE')]){
                                sh "ANSIBLE_FORCE_COLOR=true ansible-playbook ${WORKSPACE}/deploy/ansible/playbooks.yml -i ${WORKSPACE}/deploy/ansible/inventory.yml --vault-password-file=$VAULT_KEY_FILE --extra-vars 'artifact_name=$ARTIFACT_NAME' --limit ${envSpec} -v"
                            }
                        } else {
                            error("Название ветки должно начинаться с feature/ или hotfix/")
                        }
                    }
                }
                post {
                    cleanup{
                        deleteDir()
                        sh "rm -rf /tmp/*.tar.gz"
                        sh "rm -rf ../*.tar.gz"
                    }
                    always {
                        script {
                            currentBuild.displayName = "!!! Deploy Team ${params.ENV_SPEC} !!!"
                            currentBuild.result = currentBuild.result ?: 'SUCCESS' || currentBuild.result ?: 'FAILURE' 
                            withCredentials([usernamePassword(credentialsId: "${CREDENTIALSID}", passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                                sh "curl -X DELETE -u $USERNAME:$PASSWORD ${NEXUS_REPOSITORY_URL}proportal---${currentBuild.number}.zip"
                            }   
                        }
                    }
                }
            }
            stage('CERT') {
                when {
                    expression {
                        params.ENV_SPEC.contains('cert1')
                    }
                }
                steps {
                    script {
                            def buildNumber = params.BUILD
                            def envSpec = params.ENV_SPEC
                            def branchName = params.BRANCH_NAME
                            def buildNumberFrontVue = params.BUILD_FRONT_VUE
                            if (branchName.isEmpty()) {
                                error("Ветка не указана")
                            } else if (!buildNumber.isEmpty()) {
                                error("Нельзя указвать номер билда! Нужно выбрать ветку (master , feature/... hotfix/...) ")
                            } else if (!(branchName.startsWith("feature/") || branchName.startsWith("hotfix/") || branchName == "master")) {
                                error("Название ветки должно быть master, либо начинаться с feature/, hotfix/")
                            } else { 
                                    gitClone("${branchName}")
                                    vue("${buildNumberFrontVue}","${branchName}")  
                                    buildAndPush()
                                    print "DEBUG: parameter ENV_SPEC = ${params.ENV_SPEC}  DEBUG: parameter BUILD = ${currentBuild.number}"
                                    withCredentials([file(credentialsId: 'proportal_vault_key_prod',variable: 'VAULT_KEY_FILE')]){
                                        sh "ANSIBLE_FORCE_COLOR=true ansible-playbook ${WORKSPACE}/deploy/ansible/playbooks.yml -i ${WORKSPACE}/deploy/ansible/inventory.yml --vault-password-file=$VAULT_KEY_FILE --extra-vars 'artifact_name=$ARTIFACT_NAME' --limit cert1 -v"
                                }
                            }
                        }
                    }
                    post {
                        success{
                            script {
                                wrap([$class: 'BuildUser']) {
                                def fullname = env.BUILD_USER
                                def lastname = fullname.split()[0]
                                withCredentials([string(credentialsId: 'telegram_api', variable: "telegram_api")]) {
                                def jsonData = [
                                    "token":"${telegram_api}",
                                    "id":"-1001845199484",
                                    "text":"Завершена сборка ProPortal в CERT ветки *${params.BRANCH_NAME}*, результат: ${currentBuild.result}.\nЗапустил: ${lastname}"
                                    ]
                                    writeJSON file: 'req.json', json: jsonData
                                    sh "curl -k -X POST -H 'Content-Type: application/json' -d @req.json https://wso2ei.rsb.dmz.rus.socgen:443/telegram/v1.0/sendmessage"
                                   }
                                }
                                withCredentials([usernamePassword(credentialsId: "${CREDENTIALSID}", passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                                    sh "curl -X DELETE -u $USERNAME:$PASSWORD ${NEXUS_REPOSITORY_URL}proportal---${currentBuild.number}.zip"
                                }   
                            }
                        }
                        cleanup{
                            deleteDir()
                            sh "rm -rf /tmp/*.tar.gz"
                            sh "rm -rf ../*.tar.gz"
                        }
                        always {
                            script {
                                currentBuild.result = currentBuild.result ?: 'SUCCESS' || currentBuild.result ?: 'FAILURE' 
                        }
                    }
                }
            }
            stage('Check time and permission for Deploy to PROD'){
                when {
                    expression{
                        params.ENV_SPEC.contains('prod3,prod4,prod5,prod6')
                    }
                }
                steps{
                    script{
                        wrap([$class: 'BuildUser']) {
                            def dayOfWeek = new Date().format('EEEE')
                            def allowedUsers = ['Королев Василий Сергеевич','Сенченко Никита Николаевич','Баян Тимур Радиславович','Печенюк Сергей Валерьевич','Вишневский Андрей Юрьевич','Медведев Антон Олегович','Елин Айрат Аделевич','Зенов Артем Вячеславович']
                            def currentUser = env.BUILD_USER
                            if (!allowedUsers.contains(currentUser)) {
                                if (dayOfWeek == 'Saturday' || dayOfWeek == 'Sunday') {
                                    error('ДЕПЛОЙ В PROD ЗАПРЕЩЕН В ВЫХОДНЫЕ ДНИ!!!')
                                }
                                env.TIME = sh (returnStdout: true, script: 'echo -n $(date +%H%M)')
                                if ((env.TIME < '0700' || env.TIME > '1000') && (env.TIME < '1400' || env.TIME > '1600')) {
                                    error ('Деплой в прод разрешен только с 07:00 до 10:00 и с 14:00 до 16:00')
                                }
                            }
                            echo "Build was started by user: ${BUILD_USER}"
                        }
                    }  
                }
            }
            stage('PROD') {
                    when {
                        expression {
                            params.ENV_SPEC.contains('prod3,prod4,prod5,prod6')//contains проверяет строку на личее занчения
                        }
                    }
                    steps {
                        script {
                            gitClone("${params.BRANCH_NAME}")//git clone function
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
                                success {
                                    script{
                                    currentBuild.displayName = "!!! Сборка в PROD !!!"
                                    wrap([$class: 'BuildUser']) {
                                    def buildValue = params.BUILD
                                    def fullname = env.BUILD_USER
                                    def lastname = fullname.split()[0]
                                    withCredentials([string(credentialsId: 'telegram_api', variable: "telegram_api")]) {
                                    def jsonData = [
                                        "token":"${telegram_api}",
                                        "id":"-1001845199484",
                                        "text":"Завершена сборка ProPortal в PROD ${buildValue}, результат: ${currentBuild.result}.\nЗапустил: ${lastname}\n#build_prod"
                                        ]
                                        writeJSON file: 'req.json', json: jsonData
                                        sh "curl -k -X POST -H 'Content-Type: application/json' -d @req.json https://wso2ei.rsb.dmz.rus.socgen:443/telegram/v1.0/sendmessage"
                                      }
                                    }
                                    withCredentials([
                                        usernamePassword(
                                        credentialsId: 'rbsproportal-jira',
                                        usernameVariable: 'JIRA_USERNAME',
                                        passwordVariable: 'JIRA_PASSWORD')
                                        ]){
                                        JOB_START = sh(returnStdout: true, script: "TZ=MSK-3 date +'%Y-%m-%dT%H:%M:%S.000%z' |tr -d '\n'")
                                        JOB_FINISH = sh(returnStdout: true, script: "TZ=MSK-3 date -d '+5 min' +'%Y-%m-%dT%H:%M:%S.000%z' |tr -d '\n'")
                                        COMMENT = "Плановое внедрение"
                                        PLAINTEXT_COMMENT = "${currentBuild.getBuildCauses()[0].shortDescription} / ${currentBuild.getBuildCauses()[0].userId}"
                                        JS_FILE="${FILE_DIR}/createRFC.json"
                                        sh """
                                            sed -i -e 's|%BUILD%|${BUILD}|g' \
                                            -e 's|%PROJECTKEY%|${PROJECTKEY}|g' \
                                            -e 's|%COMMENT%|${COMMENT}|g' \
                                            -e 's|%JOB_START%|${JOB_START}|g' \
                                            -e 's|%JOB_FINISH%|${JOB_FINISH}|g' \
                                            -e 's|%PLAINTEXT_COMMENT%|${PLAINTEXT_COMMENT}|g' \
                                            '${JS_FILE}'; cat ${JS_FILE}
                                            curl -u ${JIRA_USERNAME}:${JIRA_PASSWORD} -X POST --data @${JS_FILE} -H 'Content-Type: application/json' https://RBITPAS00014.GTS.rus.socgen:8443/rest/api/2/issue -o task_id.txt
                                            cat task_id.txt
                                            """
                                       }
                                        emailext(
                                            subject: "ProPortal билд с номером ${currentBuild.number} собран",
                                            body: "Сборка в PROD завершена ${currentBuild.result}",
                                            to: 'platform.proportal@rosbank.ru,developers.proportal@rosbank.ru,DeltaX-ITSupport@rosbank.ru'
                                        )
                                    }
                                }
                                cleanup{
                                    deleteDir()
                                    sh "rm -rf /tmp/*.tar.gz"
                                    sh "rm -rf ../*.tar.gz"
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

def buildAndPush() {
    def artifactName = sh (returnStdout: true, script: "echo -n proportal---${currentBuild.number}.tar.gz")
    env.artifact_name = artifactName
    sh "tar -czf ../${artifactName} --exclude='./deploy*' --exclude='./vue/node_modules*' --exclude='./vue/corp/node_modules*' --exclude='./.git*' ./"
    withCredentials([usernamePassword(credentialsId: "${CREDENTIALSID}", passwordVariable: "PASSWORD", usernameVariable: "USERNAME")]) {
        sh "curl -k -u $USERNAME:$PASSWORD --upload-file ../${artifactName} ${NEXUS_REPOSITORY_URL}"
    }
}

def vue(buildNumberFrontVue, branchName) {
    withCredentials([usernamePassword(credentialsId: "${CREDENTIALSID}", passwordVariable: "PASSWORD", usernameVariable: "USERNAME")]) {
    if (params.BUILD_FRONT_VUE == 'latest') {
            def artifact = sh(script: 'curl -u $USERNAME:$PASSWORD -X GET "https://nexus.gts.rus.socgen/service/rest/v1/search/assets?repository=proportal-raw&name=proportal-vue/proportal-vue-master---*.tar.gz" | jq -r ".items[].path" | grep -o "proportal-vue-master---[0-9]*.tar.gz" | sort | tail -1', returnStdout: true).trim()
            echo "DOWNLOADING LATEST VUE BUILD NUMBER: ${artifact}"
            sh "curl -u $USERNAME:$PASSWORD -O ${NEXUS_REPOSITORY_PROPORTAL_VUE_URL}/${artifact}"
            sh "tar -xzf ${artifact} -C ${WORKSPACE}"
            sh "rm -rf ${artifact}"
    } else if (buildNumberFrontVue && branchName == 'master') { //для того чтобы фронт мог запускать при указании веток BRANCH_FRONT == master и BRANCH_BACK == master в job'е proportal-vue
            def artifact = sh(script: 'curl -u $USERNAME:$PASSWORD -X GET "https://nexus.gts.rus.socgen/service/rest/v1/search/assets?repository=proportal-raw&name=proportal-vue/proportal-vue-master---*.tar.gz" | jq -r ".items[].path" | grep -o "proportal-vue-master---[0-9]*.tar.gz" | sort | tail -1', returnStdout: true).trim()
            echo "DOWNLOADING LATEST VUE BUILD NUMBER: ${artifact}"
            sh "curl -u $USERNAME:$PASSWORD -O ${NEXUS_REPOSITORY_PROPORTAL_VUE_URL}/${artifact}"
            sh "tar -xzf ${artifact} -C ${WORKSPACE}"
            sh "rm -rf ${artifact}"
    } else if (buildNumberFrontVue && branchName) {  
            def artifactExists = sh(script: "curl -s -I -u $USERNAME:$PASSWORD -X GET \"${NEXUS_REPOSITORY_PROPORTAL_VUE_URL}/proportal-vue-feature---${buildNumberFrontVue}.tar.gz\" | head -n 1 | grep \"200 OK\"", returnStatus: true) == 0
            if (artifactExists) {  
                    sh "curl -u $USERNAME:$PASSWORD -O ${NEXUS_REPOSITORY_PROPORTAL_VUE_URL}/proportal-vue-feature---${buildNumberFrontVue}.tar.gz"
                    sh "tar -xzf proportal-vue-feature---${buildNumberFrontVue}.tar.gz -C ${WORKSPACE}"
                    sh "rm -rf proportal-vue-feature---${buildNumberFrontVue}.tar.gz"
                } else {
                    error("Артефакта с номером proportal-vue-feature---${buildNumberFrontVue}.tar.gz нет в Nexus")
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

// def deployToProd(String HOST){
//     def buildNumber = params.BUILD // переопределяем params.BUILD в пременную buildNumber в которой содержится номер указанного билда в API jenkins
//     if (buildNumber.isEmpty()) { // проверяем на пустую строку
//             error("Надо указать номер билда!")
//         } else {
//             print "DEBUG: parameter ENV_SPEC = ${params.ENV_SPEC}  DEBUG: parameter BUILD = ${currentBuild.number}"
//             withCredentials([file(credentialsId: 'proportal_vault_key_prod',variable: 'VAULT_KEY_FILE')]){
//                 env.artifact_name = "proportal---${buildNumber}.tar.gz" //чтобы можно было запускать готовый билд указанный в API Jenkins
//                 sh "ANSIBLE_FORCE_COLOR=true ansible-playbook ${WORKSPACE}/deploy/ansible/playbooks.yml -i ${WORKSPACE}/deploy/ansible/inventory.yml --vault-password-file=$VAULT_KEY_FILE --extra-vars 'artifact_name=$ARTIFACT_NAME' --limit ${HOST} -v" 
//             }
//         }
//     }

def deployToProd(String HOST){
    def ansibleResult
    def buildNumber = params.BUILD // получаем номер билда из параметров
    if (buildNumber.isEmpty()) { // проверяем наличие номера билда
        error("Надо указать номер билда!")
    } else {
        print "DEBUG: parameter ENV_SPEC = ${params.ENV_SPEC}  DEBUG: parameter BUILD = ${currentBuild.number}"
        withCredentials([file(credentialsId: 'proportal_vault_key_prod', variable: 'VAULT_KEY_FILE')]){
            env.artifact_name = "proportal---${buildNumber}.tar.gz" //чтобы можно было запускать готовый билд указанный в API Jenkins
            ansibleResult = sh(script: "ANSIBLE_FORCE_COLOR=true ansible-playbook ${WORKSPACE}/deploy/ansible/playbooks.yml -i ${WORKSPACE}/deploy/ansible/inventory.yml --vault-password-file=$VAULT_KEY_FILE --extra-vars 'artifact_name=$ARTIFACT_NAME' --limit ${HOST} -v", returnStatus: true)
        }
        if (ansibleResult != 0) { // проверяем успешность выполнения Ansible
            currentBuild.result = 'FAILURE'
            error("Ошибка выполнения Ansible на хосте ${HOST}. Ошибки в миграциях.")
        }
    }
}
