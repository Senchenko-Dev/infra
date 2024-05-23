pipeline {
    options {
        buildDiscarder(logRotator(
            artifactDaysToKeepStr: '14',
            artifactNumToKeepStr: '14',
            daysToKeepStr: '14',
            numToKeepStr: '40'))
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout()
    }


    parameters {
        string(name: 'ENV_SPEC', defaultValue: 'premaster', description: 'deploy env')
        choice(name: 'PREMASTER_BRANCH',  choices: ['release/premaster1','release/premaster2'], description: 'premaster branch')
    }


    environment { // блок для описания переменных окружения
        ANSIBLE_IMAGE="$NEXUS_BASE_URL/general/ansible:rb-4.5.0"
        APP_REPO_URL="https://bitbucket.gts.rus.socgen/scm/proport/proportal.git" // адрес репозитория
        ARCHER_CREDENTIALS_ID="JenkinsArcher"
        ARCHER_URL = "https://sgrc.rosbank.rus.socgen/"
        BITBUCKET_BASE_URL="https://bitbucket.gts.rus.socgen" // адрес bitbucket server
        BITBUCKET_NOTIFY_URL = "https://bitbucket.gts.rus.socgen"
        BUILD_NUMBER = "${BUILD}"
        CI = "CI139032"
        CREDENTIALSID = "Pro-portal-test"
        DEFAULT_ARCHER_TIMEOUT = "180"
        FILE_DIR = "./deploy/pull_request/RFC"
        NEXUS_BASE_URL = "nexus.gts.rus.socgen"
        NEXUS_REPOSITORY_URL =  "https://nexus.gts.rus.socgen/repository/proportal-raw/proportal/"
        PREMASTER_PATH = " D:\\web\\"
        PREMASTER_PATH_END = "\\"
        PROJECTKEY = "CI139032"
        SCRIPTS_DIR="./" // каталог со скриптами для сборки
        SOLAR_PROJECT = "ProPortal"
        SONAR_ENBL="${sonarenb}"
        SONAR_IMAGE = 'docker-registry.gts.rus.socgen/general/sonarscanner:rb-4.6.2.2472'
        TEAM = "PRO Sales"
    }

    agent {
        label "Proportal_agent-prod"
    }
    // triggers { pollSCM('') } // запуск пайплайна при изменения в репозитории
    
    stages  {
        stage('Checkout: Code') {
            steps {
                sh "git config --global http.sslVerify false"
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "master"]],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [
                        [
                            $class: 'CloneOption',
                            depth: 1,
                            noTags: true,
                            shallow: true
                        ]
                    ],
                    submoduleCfg: [],
                    userRemoteConfigs: [
                        [
                            credentialsId: "${CREDENTIALSID}",
                            url: "${APP_REPO_URL}"
                        ]
                    ]
                ])
            }
        }
        stage('Solar Analysis') {
                    steps{
                        step([
                            $class: 'AnalysisBuilder',
                            analyzeJsLibs: false,
                            analyzeLib: false,
                            configUuid: '2f1699dd-052a-41cf-9646-1de22580d8ae', // из настроек плагина
                            dirExc: '',
                            dirInc: '**',
                            extrules: false,
                            incremental: false,
                            langs: [[checked: false, name: 'PHP', value: 'PHP']],
                            noBuild: true,
                            projectUuid: env.SOLAR_PROJECT, // UUID проекта из webui
                            ruleSets: [],
                            sourceEncoding: 'UTF-8',
                            visualStudio: false
                            ])
                    }
                }
                //Генерация отчета по результатам сканирования в Solar AppScreener
                stage('Generate Report') {
                    steps{
                        step([
                            $class: 'AnalysisPublisher',
                            classificationVul: 'CR',
                            comparison: [entryNum: 0, entryNumVal: 1, fixed: false, included: false, newIssue: true, saved: true, scanSettings: true, scanUuid: ''],
                            detailed: [comment: true, entriesSettings: [confirmed: true, notProcessed: true, rejected: false],
                            entryNumDetailed: 0,
                            entryNumDetailedVal: 1,
                            included: true,
                            jiraInfo: true,
                            sourceCodeNum: 1,
                            sourceCodeNumVal: 3,
                            traceNum: 0],
                            filter: [
                                classFiles: true, critical: true, fuzzy: [critical: 4, included: false, info: 4, low: 4, medium: 4],
                                info: false,
                                jira: true,
                                langs: [[checked: false, name: 'PHP', value: 'PHP']],
                                low: false,
                                medium: true,
                                standardLibs: true,
                                waf: true
                            ],
                            general: [contents: true, format: 'PDF', locale: 'en', reportSettings: true],
                            projInfo: [scanHistory: 0, scanHistoryVal: 1, securityLevelDynamics: true, vulnNumberDynamics: true],
                            scanInfo: [fileStats: false, foundVulnChart: true, included: true, langStats: true, scanErrorInfo: true, scanSettings: true, typeVulnChart: true],
                            table: [entriesSettings: [confirmed: true, notProcessed: true, rejected: false],
                            entryNumTable: 0,
                            entryNumTableVal: 1,
                            included: true],
                            waf: [entriesSettings: [confirmed: true, notProcessed: true, rejected: false],
                            f5: true, imperva: true, included: true, modSec: true]])
                    }
                }
        stage("Prepare Data for Archer"){
            steps{
                script{
                    def DATA ='{}'
                    P_DATA = readJSON text: DATA;
                    P_DATA.put("Team", env.TEAM)
                    P_DATA.put("Job_url", env.BUILD_URL)
                    P_DATA.put("Solar_Project", env.SOLAR_PROJECT)
                    P_DATA.put("Repository", env.APP_REPO_URL)
                    P_DATA.put("CI", env.CI)
                    writeJSON file: 'archer.json', json: P_DATA
                    P_DATA.clear()
                    sh (returnStdout: true, script: 'ls')
                }
            }
        }
        stage("Create Archer task") {
            steps {
                script {
                    env.POST_ARCHER_URL = "${ARCHER_URL}u/jenkins"
                    withCredentials([usernamePassword(credentialsId: ARCHER_CREDENTIALS_ID, passwordVariable: 'PASS', usernameVariable: 'USER')]){
                        env.ARCHER_DATA = sh(
                                script: "curl -u $USER:$PASS -X POST $POST_ARCHER_URL -H 'Content-Type: application/json' -d '@archer.json'",
                                returnStdout: true).trim()
                    }
                    echo env.ARCHER_DATA
                    def ARCHER_PARSE_DATA = readJSON text: env.ARCHER_DATA;

                    env.ARCHER_TASK_ID = ARCHER_PARSE_DATA["id"]
                    if (ARCHER_PARSE_DATA["timeout"] != null){
                        ARCHER_TIMEOUT = ARCHER_PARSE_DATA["timeout"]
                    } else {
                        ARCHER_TIMEOUT = DEFAULT_ARCHER_TIMEOUT
                    }

                }

            }
        }
        stage("Get Archer task status"){
            steps{
                script{
                    ARCHER_TIMEOUT = ARCHER_TIMEOUT + 5
                    timeout(time: ARCHER_TIMEOUT, unit: 'MINUTES'){
                        env.GET_ARCHER_URL = "${ARCHER_URL}u/jenkins/id/${ARCHER_TASK_ID}"
                        sh (returnStdout: true, script: 'echo -n ${ARCHER_URL}u/jenkins/id/${ARCHER_TASK_ID}')
                        curentBuild = true
                        withCredentials([usernamePassword(credentialsId: ARCHER_CREDENTIALS_ID, passwordVariable: 'PASS', usernameVariable: 'USER')]){
                        def infinite_loop_var = 0
                            while ( infinite_loop_var != 1 ) {
                                env.ARCHER_DATA = sh(
                                    script: "curl -u ${USER}:${PASS} -X GET $GET_ARCHER_URL -H 'Content-Type: application/json'",
                                    returnStdout: true).trim()
                                ARCHER_PARSE_DATA = readJSON text: env.ARCHER_DATA;
                                echo ARCHER_PARSE_DATA["status"]
                                // Approved, Rejected, InProgress, NotAvailable
                                if ( ARCHER_PARSE_DATA["status"] == 'Approved' ){
                                    echo "Archer task is Approved"
                                    curentBuild = "true"
                                    break;
                                }
                                if ( ARCHER_PARSE_DATA["status"] == 'Rejected'){
                                    error "Archer task is Rejected"
                                    curentBuild.result = "false"
                                    break;
                                }
                                if ( ARCHER_PARSE_DATA["status"] == 'NotAvailable' ){
                                    error "Archer task is NotAvailable"
                                    curentBuild.result = "false"
                                    break;
                                } else {
                                    sleep(60)
                                }
                            }
                        }
                    }
                }
            }
        }
        stage('Build') {
            steps {
                script {
                    sh 'cd vue && npm ci && npm run build && cd ..'
                    sh 'rm -Rf ./vue/node_modules'
                    artifact_name = sh (returnStdout: true, script: ''' echo -n "proportal-premaster---${BUILD_NUMBER}.zip" ''')
                    env.artifact_name = artifact_name
                    sh 'zip -2 -r ${artifact_name} ./ -x ./jenkins_deploy**\\* ./images**\\* ./leadportal_client**\\*  ./download**\\* ./test**\\* ./.git**\\*' // убрать архивирование скриптов деплоя и
                }
            }
        }
        stage('Push') {
            steps {
                script { //wbhook test
                    withCredentials([usernamePassword(credentialsId: "${CREDENTIALSID}", passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                         // the code in here can access $PASSWORD and $USER
                         sh 'curl -v -k -u $USERNAME:$PASSWORD --upload-file ${artifact_name} ${NEXUS_REPOSITORY_URL}' // не используется nexus base url
                         sh 'echo "push in nexus is disabled"'
                   }
                }
            }
        }
        stage('Deploy on premaster env') {
            steps {
               script{
                withCredentials([file(credentialsId: 'proportal_vault_key',variable: 'VAULT_KEY_FILE')]){
                    env.PREMASTER_SITE = sh (returnStdout: true, script: ''' echo -n $PREMASTER_BRANCH | grep -o premaster[0-9] ''').replaceAll("\\s","")
                    env.ARTIFACT_PATH= ("${env.PREMASTER_PATH}"+"${env.PREMASTER_SITE}"+"${env.PREMASTER_PATH_END}").replaceAll("\\s","")
                    sh 'echo !!! premaster_site= ${PREMASTER_SITE} !!!'
                    sh 'echo !!!  artifact_path= ${ARTIFACT_PATH} !!!'
                    sh './jenkins_deploy/run-ansible.sh'
                }
              }
            }
        }
        stage('createRFC'){
                  steps{
                    script{
                     withCredentials([
                        usernamePassword(
                        credentialsId: 'Pro-portal-prod',
                        usernameVariable: 'JIRA_USERNAME',
                        passwordVariable: 'JIRA_PASSWORD')
                     ]) {
                      JOB_START = sh(returnStdout: true, script: "TZ=MSK-3 date +'%Y-%m-%dT%H:%M:%S.000%z' |tr -d '\n'")
                      JOB_FINISH = sh(returnStdout: true, script: "TZ=MSK-3 date -d '+5 min' +'%Y-%m-%dT%H:%M:%S.000%z' |tr -d '\n'")
                      COMMENT = "Плановое внедрение"
                      PLAINTEXT_COMMENT = "${currentBuild.getBuildCauses()[0].shortDescription} / ${currentBuild.getBuildCauses()[0].userId}"
                      JS_FILE="${FILE_DIR}/createRFC.json"
                      artifact_name = sh (returnStdout: true, script: ''' echo -n "proportal-premaster---${BUILD_NUMBER}.zip" ''')
                      sh """
                           sed -i -e 's|%artifact_name%|${artifact_name}|g' \
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
                currentBuild.result = currentBuild.result ?: 'SUCCESS'

                notifyBitbucket(
                    commitSha1: '',
                    considerUnstableAsSuccess: false,
                    credentialsId: "${CREDENTIALSID}",
                    disableInprogressNotification: true,
                    ignoreUnverifiedSSLPeer: true,
                    includeBuildNumberInKey: true,
                    prependParentProjectKey: false,
                    projectKey: "${PROJECTKEY}",
                    stashServerBaseUrl: "${BITBUCKET_NOTIFY_URL}"
                )
            }
        }
    }
}


