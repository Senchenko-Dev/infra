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
        environment { // блок для описания переменных окружения
            BITBUCKET_PROJECT_KEY="PROPORT"
            BITBUCKET_REPO_PATH="${BITBUCKET_PROJECT_KEY.toLowerCase()}/${REPO_NAME}"
            REPO_NAME="proportal-vue"

            BITBUCKET_BASE_URL="https://gitlab.rosbank.rus.socgen" // адрес Gitlab server
            CREDENTIALSID = "Pro-portal-test"
            NEXUS_BASE_URL = "nexus.gts.rus.socgen"
            NEXUS_REPOSITORY_URL = "https://nexus.gts.rus.socgen/repository/proportal-raw/proportal-vue/"
        }

        parameters {
            string(name: 'BRANCH_FRONT', defaultValue: 'master', description: 'Укажите имя ветки feature/для FRONT_VUE')
            choice(name: 'ENV_SPEC', choices: ['','onboarding','payboosters','proleads','agents','payroll','corp','corp2','corp3','proloans','pro_sales_front_1','pro_sales_front_2','profuture'],description: 'Выбирите куда хотите запустить Deploy. Если ветка BRANCH_FRONT НЕ master') 
            string(name: 'BRANCH_BACK', defaultValue: '', description: 'Укажите имя ветки feature/для BACK_PROPORTAL (если выбран ENV_SPEC и вставлена ветка BRANCH_BACK то запустится командый деплой)')
        }

        agent {
            label "PROPORTAL_linux_1"
        }
        
        stages  {
            stage('gitClone') {
                steps {
                    script {
                        if (fileExists("/home/jenkins-agent/workspace/loymentJobs_Proportal-Vue_master")) {
                            deleteDir()
                        }
                        gitClone("${params.BRANCH_FRONT}")
                    }
                }
            }
            stage('Build Vue') {
                steps {
                    script {
                        sh 'cd vue && npm ci && npm run build && cd ..'
                    }
                }
            }
            stage('Build Corp') {
                steps {
                    script {
                        sh 'cd vue/corp && npm ci && npm run build && cd ../..'
                    }    
                }
            }
            stage('Push to Nexus') {
                steps {
                    script {
                       // buildAndPush("${params.BRANCH_FRONT}")
                        
                        def artifactNamePrefix
                        def branchFront = params.BRANCH_FRONT
                        def branchBack = params.BRANCH_BACK
                        
                        if (branchFront == 'master' && branchBack == 'master') {
                            artifactNamePrefix = 'proportal-vue-master'
                        } else if (branchFront.startsWith('feature/')) {
                            artifactNamePrefix = 'proportal-vue-feature'
                        } else {
                            artifactNamePrefix = 'proportal-vue-master'
                        }
                        def artifactName = "${artifactNamePrefix}---${currentBuild.number}.tar.gz"
                        env.artifact_name = artifactName
                        sh "tar -czf ../${artifactName} --exclude='./deploy*' --exclude='./vue/node_modules*' --exclude='./vue/corp/node_modules*' --exclude='./vue*' --exclude='./.git*' ./"
                        withCredentials([usernamePassword(credentialsId: "${CREDENTIALSID}", passwordVariable: "PASSWORD", usernameVariable: "USERNAME")]) {
                            sh "curl -k -u $USERNAME:$PASSWORD --upload-file ../${artifactName} ${NEXUS_REPOSITORY_URL}"
                        }
                        if (params.BRANCH_FRONT == 'master') {                          
                            currentBuild.displayName = "latest"
                        } else {
                            currentBuild.displayName = "${currentBuild.number} branch: ${params.BRANCH_FRONT}"
                        }
                    }
                }
            }
            stage('Start ProPortal test63') {
                steps {
                    script {
                        if (params.BRANCH_FRONT== 'master' && params.ENV_SPEC == '' && params.BRANCH_BACK == '') {                          
                            build job: 'ProPortal/DeploymentJobs/Proportal/master', parameters: [string(name: 'ENV_SPEC', value: 'test63')]
                        } 
                    }    
                }
                post {
                    cleanup{
                        deleteDir()
                        sh "rm -rf ../*.tar.gz"
                    }
                    always {
                        script {
                            currentBuild.result = currentBuild.result ?: 'SUCCESS' || currentBuild.result ?: 'FAILURE' 
                        }
                    }
                }
            }
            stage('Start DEPLOY FOR TEAM') {
                steps {
                    script {
                        if ((params.BRANCH_FRONT == 'master' && params.ENV_SPEC != '' && params.BRANCH_BACK == 'master') || (params.BRANCH_FRONT != 'master' && params.ENV_SPEC != '' && params.BRANCH_BACK != 'master')) {                         
                            build job: 'ProPortal/DeploymentJobs/Proportal/master', parameters: [string(name: 'ENV_SPEC', value: "${params.ENV_SPEC}"), string(name: 'BRANCH_NAME', value: "${params.BRANCH_BACK}"),string(name: 'BUILD_FRONT_VUE', value: "${currentBuild.number}")]  
                        }
                    }    
                }
                post {
                    cleanup{
                        deleteDir()
                        sh "rm -rf ../*.tar.gz"
                    }
                    always {
                        script {
                            print "DEBUG: parameter ENV_SPEC = ${params.ENV_SPEC}"
                            print "DEBUG: parameter BRANCH_NAME_FRONT = ${params.BRANCH_FRONT}"
                            print "DEBUG: parameter BRANCH_NAME_BACK = ${params.BRANCH_BACK}"
                            print "DEBUG: parameter BUILD_FRONT_VUE = ${currentBuild.number}"
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

// def buildAndPush(branchName, branchFront, branchBack){
    
//      //def artifactNamePrefix = branchName.startsWith('feature/') ? 'proportal-vue-feature' : 'proportal-vue-master'
//     def artifactNamePrefix

//     if (branchFront == 'master' && branchBack == 'master') {
//         artifactNamePrefix = 'proportal-vue-master'
//     } else if (branchFront.startsWith('feature/')) {
//         artifactNamePrefix = 'proportal-vue-feature'
//     } else {
//         artifactNamePrefix = 'proportal-vue-master'
//     }

//     def artifactName = "${artifactNamePrefix}---${currentBuild.number}.tar.gz"
//     env.artifact_name = artifactName
//     sh "tar -czf ../${artifactName} --exclude='./deploy*' --exclude='./vue/node_modules*' --exclude='./vue/corp/node_modules*' --exclude='./vue*' --exclude='./.git*' ./"
//     withCredentials([usernamePassword(credentialsId: "${CREDENTIALSID}", passwordVariable: "PASSWORD", usernameVariable: "USERNAME")]) {
//         sh "curl -k -u $USERNAME:$PASSWORD --upload-file ../${artifactName} ${NEXUS_REPOSITORY_URL}"
//     }
// }
