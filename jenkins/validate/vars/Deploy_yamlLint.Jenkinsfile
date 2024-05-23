pipeline {
    environment { // блок для описания переменных окружения
        BITBUCKET_PROJECT_KEY="PROPORT"
        BITBUCKET_REPO_PATH="${BITBUCKET_PROJECT_KEY.toLowerCase()}/${REPO_NAME}"
        REPO_NAME="configs-non-prod"
        BITBUCKET_BASE_URL="https://gitlab.rosbank.rus.socgen" // адрес Gitlab server
        CREDENTIALSID = "ProPortal_service"

        GITLAB_PRIVATE_TOKEN = '12zkGm4DsXLscqsLvyGo'
        PROJECT_ID = '7700'
    }

    agent {
        label "Proportal_agent-prod"
        //label "Proportal_agent-cert"
    }

    stages {
        stage('Validate YAML File') {
            steps {
                script {   
                    gitClone()
                    def downloadScriptUrl = "https://gitlab.rosbank.rus.socgen/api/v4/projects/proport%2Finfra/repository/files/openshift%2Fgroovy_files%2Fyamllint.groovy/raw?ref=master"
                    sh "curl --header 'PRIVATE-TOKEN: ${GITLAB_PRIVATE_TOKEN}' ${downloadScriptUrl} -o ${WORKSPACE}/yamllint.groovy"
                
                    def mrResponse = sh(script: "curl --header 'PRIVATE-TOKEN: ${GITLAB_PRIVATE_TOKEN}' 'https://gitlab.rosbank.rus.socgen/api/v4/projects/${PROJECT_ID}/merge_requests?state=opened' | jq '.[0].iid'", returnStdout: true).trim()
                    def fileNameResponse = sh(script: "curl --header 'PRIVATE-TOKEN: ${GITLAB_PRIVATE_TOKEN}' 'https://gitlab.rosbank.rus.socgen/api/v4/projects/${PROJECT_ID}/merge_requests/${mrResponse}/changes' | jq -r '.changes[].new_path' | xargs basename", returnStdout: true).trim()
                    print "${fileNameResponse}"

                    def path = sh(script: "curl --header 'PRIVATE-TOKEN: ${GITLAB_PRIVATE_TOKEN}' 'https://gitlab.rosbank.rus.socgen/api/v4/projects/${PROJECT_ID}/merge_requests/${mrResponse}/changes' | jq -r '.changes[0].new_path' | xargs -I {} dirname {}", returnStdout: true).trim()
                    dir(path){
                        sh 'ls -la'
                        def yamlFile = readFile("${fileNameResponse}")
                        def yamlLines = yamlFile.readLines() 

                        loadGroovyFunc = load "${WORKSPACE}/yamllint.groovy"
                        loadGroovyFunc.startYamlLint(yamlFile)
                        loadGroovyFunc.findSymbols(yamlLines)
                        loadGroovyFunc.findSpaceAfterVariable(yamlLines) 
                    }
                }
            }
        }
    }
}

def gitClone() {
    sh "git config --global http.sslVerify false"  //git clone branch
    checkout([$class: 'GitSCM', branches: [[name: 'master']],doGenerateSubmoduleConfigurations: false,
    extensions: [[$class: 'CleanBeforeCheckout'],[$class: 'CloneOption',depth: 1,noTags: true,shallow: true]],submoduleCfg: [],
    userRemoteConfigs:[[credentialsId: "proportal_deploy",url: "${env.BITBUCKET_BASE_URL}/${env.BITBUCKET_REPO_PATH}.git"]]])
}



// def startYamlLint(yamlFile) {
//    sh "cat <<< '${yamlFile}' | grep -vE '.*%.*%' | yamllint -c /home/jenkins-agent/.yamllint -"
// }

// def findSymbols(yamlLines) {
//     //поиск не допустимых символов в yaml файле
//     for (int i = 0; i < yamlLines.size(); i++) {
//         def line = yamlLines[i]
//         for (int j = 0; j < line.size(); j++) {
//             def ch = line[j]
//             if (ch in ['!', '#', '^',]) {
//                 error("найден сивол '${ch}' в строке ${i + 1} столбец номер ${j + 1}")
//             }
//         }
//     }
// }

// def findSpaceAfterVariable(yamlLines) {
//     //поиск лишнего пробела после переменной в yaml файле
//     for (int i = 0; i < yamlLines.size(); i++){
//         if (yamlLines[i] =~ /(\w+):\s{2}/) {
//             error("Найден лишний пробел после переменной в строке ${i + 1}: ${yamlLines[i]}")
//         }
//     }

// }
// def findTabs(yamlLines) {
//     //поиск табуляций в yaml файле
//     for (int i = 0; i < yamlLines.size(); i++) {
//         if (yamlLines[i].contains('\t')) {
//             error("YAML файл содержит табуляцию в строке:  ${i + 1}: ${yamlLines[i]}\n")
//         }
//     }
// }

// def findCronPattern(yamlLines) {
//     //поиск корректного крон задания
//     def cronPattern = '0 * * * *'
//     for (int i = 0; i < yamlLines.size(); i++) {
//         if (!yamlLines[i].contains(cronPattern) && yamlLines[i].contains('* * * * *')) {
//             error("Некорректная строка с кроном в строке: ${i + 1}: ${yamlLines[i]}")
//         }
//     }
// }

// def gitCheck(repoUrl,folderName) {
//     withCredentials([usernamePassword(credentialsId: 'rbsproportal-gitlab', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
//         def gitUrl = "https://'${USERNAME}:${PASSWORD}'@${repoUrl}/${folderName}"
//         def response = sh(script: "git ls-remote --exit-code --heads ${gitUrl} -o /dev/null", returnStatus: true)
//         if (response == 2){
//             echo "Папка ${folderName} существует в репозитории"
//         } else {
//             error("Репозиторий не доступен ${response}")
//         }
//     }   
// }



