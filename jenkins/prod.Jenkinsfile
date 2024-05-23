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
        string(name: 'ENV_SPEC', defaultValue: 'prod1', description: 'deploy env prod proportal')
        string(name: 'BUILD', defaultValue: '', description: 'Build number')
    }
    environment { // блок для описания переменных окружения test
        APP_REPO_URL="https://gitlab.rosbank.rus.socgen/proport/proportal.git" // адрес репозитория
        BITBUCKET_BASE_URL="https://gitlab.rosbank.rus.socgen" // адрес bitbucket server
        BITBUCKET_NOTIFY_URL = "https://gitlab.rosbank.rus.socgen"
        SCRIPTS_DIR="./" // каталог со скриптами для сборки
        CREDENTIALSID = "Pro-portal-test" //учетка для доступа в nexus
        FILE_DIR = "./deploy/pull_request/RFC"
        NEXUS_BASE_URL = "nexus.gts.rus.socgen"
        NEXUS_REPOSITORY_BUILD_SOURCE_URL =  "https://nexus.gts.rus.socgen/repository/proportal-raw/proportal/"
        NEXUS_REPOSITORY_URL =  "https://nexus.gts.rus.socgen/repository/proportal-raw-release/proportal/"
        ANSIBLE_IMAGE="$NEXUS_BASE_URL/general/ansible:rb-4.5.0"
        artifact_name = "proportal---${BUILD}.zip"
        TEAM = "PRO Portal"
        CI = "CI139032"
        PROJECTKEY = "CI139032"
        BUILD_NUMBER = "${BUILD}"
        ARCHER_CREDENTIALS_ID="JenkinsArcher"
        ARCHER_URL = "https://sgrc.rosbank.rus.socgen/"
        DEFAULT_ARCHER_TIMEOUT = "180"
        SOLAR_PROJECT = "41069483-6ddc-424d-99fd-a044926e96c1"
        SOLAR_PROJECTS = ""
        SONAR_IMAGE = 'docker-registry.gts.rus.socgen/general/sonarscanner:rb-4.6.2.2472'
        
    }
     agent {
         label "PROPORTAL_linux_1"
     }
    //triggers { pollSCM('') } // запуск пайплайна при изменения в репозитории

    //1) Скачиваем репу из proportal-raw
    //2) Закидываем репу в proportal-raw-release
    stages  {
        // stage('Checkout time'){
        //     steps{
        //         script{
        //             env.TIME = sh (returnStdout: true, script: 'echo -n $(date +%H%M)')
        //             env.DATE = sh (returnStdout: true, script: 'echo -n $(date +%Y%m%d)')
        //             if ( env.TIME < '1900' || env.TIME > '2300' ){
        //                 currentBuild.result = "FAILURE"
        //                 error ('Деплоиться в прод можно только с 19:00 - 23:00')
        //             }
        //         }
        //     }
        // }
        stage('Checkout: Code') {
            steps
                {
                  withCredentials([
                  usernamePassword
                   (
                      credentialsId: "rbsproportal-gitlab",
                      passwordVariable: 'CI_CD_PASS',
			          usernameVariable: 'CI_CD_USER'
                    )])
                {

                  script {
                    sh 'git init'
                    sh "git config --global http.sslVerify false"
                    sh 'git remote add portal https://${CI_CD_USER}:${CI_CD_PASS}@gitlab.rosbank.rus.socgen/proport/proportal.git'
                    sh 'git fetch portal'
                    sh 'git checkout portal/master -- src/Access/EmployeeRole.php'
                    sh 'git checkout portal/master -- config/constants/test/openshift.yaml'
                    sh 'git checkout portal/master -- support/krm/process_40/general_jquery.js'
                    sh 'git checkout portal/master -- support/krm/process_40/portal_krm_fields.sql'
                    sh 'git checkout portal/master -- public/static/activators/index.html'
                    sh 'git checkout portal/master -- support/krm/process_40/script.sql'
                 }
                }
                }
				}
        stage('SonarQube Scanning') {
          agent {
            docker {
              registryUrl 'https://docker-registry.gts.rus.socgen'
              image "${SONAR_IMAGE}"
              reuseNode true
            }
           }
           steps {
             withSonarQubeEnv(credentialsId: 'proportal', installationName: 'sonarqube-prod') {
                sh "sonar-scanner -X -Dsonar.host.url=https://sonarqube.gts.rus.socgen/ -Dsonar.projectVersion=1.0 -Dsonar.language=php -Dsonar.sourceEncoding=UTF-8 -Dsonar.phpCodesniffer.timeout=120 -Dsonar.projectKey=ProPortal -Dsonar.projectName=proportal::proportal -Dsonar.sources=. -Dsonar.test.inclusions=./tests/* -Dsonar.exclusions=./tests/*"
	             }
          }
        }
        stage('Solar Analysis') {
            steps{
                step([
                    $class: 'AnalysisBuilder',
                    analyzeJsLibs: false,
                    analyzeLib: false,
                    builtTypeJava: 'NotBuilt', 
                    cfamilyOs: 'NIX',
                    configUuid: '12f8c0b1-d066-4b75-b8d1-e09973c7b385',
                    dirExc: '', 
                    dirInc: '**',
                    extrules: false,
                    incremental: true,
                    langs: [
                    [checked: true, name: 'Config files', value: 'CONFIG'],
                    [checked: true, name: 'HTML5', value: 'HTML5'],
                    [checked: true, name: 'JavaScript', value: 'JAVASCRIPT'],
                    [checked: true, name: 'PHP', value: 'PHP'],
                    [checked: true, name: 'PL/SQL', value: 'PLSQL'],
                    [checked: true, name: 'T-SQL', value: 'TSQL']],
                    projectUuid: env.SOLAR_PROJECT, // UUID проекта из webui
                    ruleSets: [],
                    sourceEncoding: 'UTF-8',
                    useCtu: false
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
                        langs: [
                            [checked: false, name: 'ABAP', value: 'ABAP'],
                            [checked: false, name: 'Android', value: 'ANDROID'],
                            [checked: false, name: 'Apex', value: 'APEX'],
                            [checked: false, name: 'C#', value: 'CS'],
                            [checked: false, name: 'C/C++', value: 'CCPP'],
                            [checked: false, name: 'COBOL', value: 'COBOL'],
                            [checked: false, name: 'Config files', value: 'CONFIG'],
                            [checked: false, name: 'Dart', value: 'DART'],
                            [checked: false, name: 'Delphi', value: 'DELPHI'],
                            [checked: false, name: 'Go', value: 'GO'],
                            [checked: false, name: 'Groovy', value: 'GROOVY'],
                            [checked: false, name: 'HTML5', value: 'HTML5'],
                            [checked: false, name: 'Java', value: 'JAVA'],
                            [checked: false, name: 'JavaScript', value: 'JAVASCRIPT'],
                            [checked: false, name: 'Kotlin', value: 'KOTLIN'],
                            [checked: false, name: 'LotusScript', value: 'LOTUS'],
                            [checked: false, name: 'Objective-C', value: 'OBJC'],
                            [checked: false, name: 'Pascal', value: 'PASCAL'],
                            [checked: false, name: 'PHP', value: 'PHP'],
                            [checked: false, name: 'PL/SQL', value: 'PLSQL'],
                            [checked: false, name: 'Python', value: 'PYTHON'],
                            [checked: false, name: 'Perl', value: 'PERL'],
                            [checked: false, name: 'Ruby', value: 'RUBY'],
                            [checked: false, name: 'Rust', value: 'RUST'],
                            [checked: false, name: 'Scala', value: 'SCALA'],
                            [checked: false, name: 'Solidity', value: 'SOLIDITY'],
                            [checked: false, name: 'Swift', value: 'SWIFT'],
                            [checked: false, name: 'T-SQL', value: 'TSQL'],
                            [checked: false, name: 'TypeScript', value: 'TYPESCRIPT'],
                            [checked: false, name: 'VB.NET', value: 'VBNET'],
                            [checked: false, name: 'VBA', value: 'VBA'],
                            [checked: false, name: 'VBScript', value: 'VBSCRIPT'],
                            [checked: false, name: 'Visual Basic 6', value: 'VB'],
                            [checked: false, name: 'Vyper', value: 'VYPER'],
                            [checked: false, name: '1C', value: 'ONES']
                        ],
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
                    P_DATA.put("Solar_Project", env.SOLAR_PROJECTS)
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
                                    JS_FILE="${FILE_DIR}/createArcherTask.json"
                                    sh """
                                    curl -u ${JIRA_USERNAME}:${JIRA_PASSWORD} -X POST --data @${JS_FILE} -H 'Content-Type: application/json' https://RBITPAS00014.GTS.rus.socgen:8443/rest/api/2/issue -o task_id.txt
                                    cat task_id.txt
                                    """
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
        // stage('Pull & Push') {
        //     steps {
        //         print "DEBUG: parameter ENV_SPEC = ${params.ENV_SPEC}"
        //         print "DEBUG: parameter BUILD = ${params.BUILD}"
        //         print "DEBUG: parameter artifact_name = ${artifact_name}"
        //         script { //
        //             withCredentials([usernamePassword(credentialsId: "${CREDENTIALSID}", passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]){
        //                 sh 'curl -v -k -u $USERNAME:$PASSWORD ${NEXUS_REPOSITORY_BUILD_SOURCE_URL}${artifact_name} > ${artifact_name}' //скачать репозиторий
        //                 sh 'curl -v -k -u $USERNAME:$PASSWORD --upload-file ${artifact_name} ${NEXUS_REPOSITORY_URL}' // не используется nexus base url
        //             }
        //         }
        //     }
        // }

		// stage('Deploy on prod1 and prod2') {
		// 	steps {
		// 		print "DEBUG: parameter ENV_SPEC = ${params.ENV_SPEC}"
		// 		print "DEBUG: parameter BUILD = ${params.BUILD}"
		// 		script{
        //         withCredentials([file(credentialsId: 'proportal_vault_key_prod',variable: 'VAULT_KEY_FILE')]){
        //             env.PREMASTER_SITE = "ProPortal"
        //             sh 'echo !!! premaster_site= ${PREMASTER_SITE} !!!'
        //             sh './jenkins_deploy/run-ansible.sh'
        //         }
		// 	}
        //   }
		// }
        // stage('createRFC'){
        //           steps{
        //             script{
        //              withCredentials([
        //                 usernamePassword(
        //                 credentialsId: 'Pro-portal-prod',
        //                 usernameVariable: 'JIRA_USERNAME',
        //                 passwordVariable: 'JIRA_PASSWORD')
        //              ]) {
        //               JOB_START = sh(returnStdout: true, script: "TZ=MSK-3 date +'%Y-%m-%dT%H:%M:%S.000%z' |tr -d '\n'")
        //               JOB_FINISH = sh(returnStdout: true, script: "TZ=MSK-3 date -d '+5 min' +'%Y-%m-%dT%H:%M:%S.000%z' |tr -d '\n'")
        //               COMMENT = "Плановое внедрение"
        //               PLAINTEXT_COMMENT = "${currentBuild.getBuildCauses()[0].shortDescription} / ${currentBuild.getBuildCauses()[0].userId}"
        //               JS_FILE="${FILE_DIR}/createRFC.json"
        //               sh """
        //                    sed -i -e 's|%BUILD%|${BUILD}|g' \
        //                    -e 's|%PROJECTKEY%|${PROJECTKEY}|g' \
        //                    -e 's|%COMMENT%|${COMMENT}|g' \
        //                    -e 's|%JOB_START%|${JOB_START}|g' \
        //                    -e 's|%JOB_FINISH%|${JOB_FINISH}|g' \
        //                    -e 's|%PLAINTEXT_COMMENT%|${PLAINTEXT_COMMENT}|g' \
        //                    '${JS_FILE}'; cat ${JS_FILE}

        //                     curl -u ${JIRA_USERNAME}:${JIRA_PASSWORD} -X POST --data @${JS_FILE} -H 'Content-Type: application/json' https://jirahq.rosbank.rus.socgen:8443/rest/api/2/issue -o task_id.txt
        //                     cat task_id.txt
        //                     """
        //                  }
        //              }
        //           }
		// 		}
     
    }

    post {
        aborted{
            script{
                echo "Задача завершена по таймауту ожидания изменения статуса задачи в Archer"
            }
        }
        cleanup{
            deleteDir()
        }
    }
}
