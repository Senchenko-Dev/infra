def call(){
// pipeline{
//     agent{
//       node
//        {
//          label "PROPORTAL_linux_1"
//        }
//     }
//     environment { // блок для описания переменных окружения test
//         APP_REPO_URL="https://gitlab.rosbank.rus.socgen/proport/proportal.git" // адрес репозитория
//         BITBUCKET_BASE_URL="https://gitlab.rosbank.rus.socgen" // адрес bitbucket server
//         BITBUCKET_NOTIFY_URL = "https://gitlab.rosbank.rus.socgen"
//         SCRIPTS_DIR="./" // каталог со скриптами для сборки
//         CREDENTIALSID = "Pro-portal-test" //учетка для доступа в nexus
//         FILE_DIR = "./deploy/pull_request/RFC"
//         NEXUS_BASE_URL = "nexus.gts.rus.socgen"
//         NEXUS_REPOSITORY_BUILD_SOURCE_URL =  "https://nexus.gts.rus.socgen/repository/proportal-raw/proportal/"
//         NEXUS_REPOSITORY_URL =  "https://nexus.gts.rus.socgen/repository/proportal-raw-release/proportal/"
//         ANSIBLE_IMAGE="$NEXUS_BASE_URL/general/ansible:rb-4.5.0"
//         artifact_name = "proportal---${BUILD}.zip"
//         TEAM = "PRO Portal"
//         CI = "CI139032"
//         PROJECTKEY = "CI139032"
//         BUILD_NUMBER = "${BUILD}"
//         ARCHER_CREDENTIALS_ID="JenkinsArcher"
//         ARCHER_URL = "https://sgrc.rosbank.rus.socgen/"
//         DEFAULT_ARCHER_TIMEOUT = "180"
//         SOLAR_PROJECT = "41069483-6ddc-424d-99fd-a044926e96c1"
//         SOLAR_PROJECTS = ""
        
//     }

    // stages{
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
        stage("Quality Gate") {
         steps {
              script {
                      def getURL = readProperties file: './.scannerwork/report-task.txt'
                      echo "${getURL['ceTaskUrl']}"
                   // контекст для waitForQualityGate() передается из withSonarQubeEnv(...)
                      def quality_gate = waitForQualityGate()
                      withEnv(["JIRA_SITE=${env.JIRA_ENV}"]) {
                         if (quality_gate.status != 'OK') {
                              // Добавляем комментарий к задаче в Jira
                              def comment = [ body: "Quality Gate is failed: ${getURL['dashboardUrl']}" ]
                              jiraAddComment idOrKey: env.ISSUE_JIRA_KEY, input: comment

                              // ОПЦИОНАЛЬНО: останавливаемся если не пройден QG
                              error "Pipeline aborted due to quality gate failure: ${quality_gate.status}"
                           } else {
                               // Добавляем комментарий к задаче в Jira
                               def comment = [ body: "Quality Gate is ok ${getURL['dashboardUrl']}" ]
                               jiraAddComment idOrKey: env.ISSUE_JIRA_KEY, input: comment
                           }
                       }
                   }
               }
           }
        stage('Solar Analysis') {
            steps{
                step([
                    $class: 'AnalysisBuilder',
                    analyzeJsLibs: false,
                    analyzeLib: false,
                    configUuid: '2f1699dd-052a-41cf-9646-1de22580d8ae', // из настроек плагина
                    dirExc: './tests/*', 
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
                    noBuild: true,
                    projectUuid: env.SOLAR_PROJECT, // UUID проекта из webui
                    ruleSets: [],
                    sourceEncoding: 'UTF-8',
                    visualStudio: false
                    ])
            }
        }
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
                    [checked: true, name: 'Config files', value: 'CONFIG'],
                    [checked: true, name: 'HTML5', value: 'HTML5'],
                    [checked: true, name: 'JavaScript', value: 'JAVASCRIPT'],
                    [checked: true, name: 'PHP', value: 'PHP'],
                    [checked: true, name: 'PL/SQL', value: 'PLSQL'],
                    [checked: true, name: 'T-SQL', value: 'TSQL']],
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
        stage('Print status'){
             steps{
               echo "${env.ISSUE_STATUS}"
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
                                    withEnv(["JIRA_SITE=${env.JIRA_ENV}"]) {
                          //Узнаем ID проекта Jira
                           def project = jiraGetProject idOrKey: PROJECT_JIRA_KEY
                           env.PROJECT_JIRA_ID = project.data.id.toString()

                           def NewIssue = [fields: [ project: [id: env.PROJECT_JIRA_ID],
                                   summary: 'New JIRA Created from Jenkins.',
                                   description: 'New JIRA Created from Jenkins.',
                                   epicLists: 'PROPRO-2525',
                                   issuetype: [name: 'Task']]]
                           response = jiraNewIssue issue: NewIssue

                           echo response.successful.toString()
                           echo response.data.key.toString()
                           //Получаем ID созданной таски
                           env.ISSUE_JIRA_KEY = response.data.key.toString()
                       }
                                    error "Archer task is Rejected"
                                    curentBuild.result = "false"
                                    break;
                                }
                                if ( ARCHER_PARSE_DATA["status"] == 'NotAvailable' ){
                                    withEnv(["JIRA_SITE=${env.JIRA_ENV}"]) {
                          //Узнаем ID проекта Jira
                           def project = jiraGetProject idOrKey: PROJECT_JIRA_KEY
                           env.PROJECT_JIRA_ID = project.data.id.toString()

                           def NewIssue = [fields: [ project: [id: env.PROJECT_JIRA_ID],
                                   summary: 'New JIRA Created from Jenkins.',
                                   description: 'New JIRA Created from Jenkins.',
                                   epicLists: 'PROPRO-2525',
                                   issuetype: [name: 'Task']]]
                           response = jiraNewIssue issue: NewIssue

                           echo response.successful.toString()
                           echo response.data.key.toString()
                           //Получаем ID созданной таски
                           env.ISSUE_JIRA_KEY = response.data.key.toString()
                       }
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
    // }
// }
}
