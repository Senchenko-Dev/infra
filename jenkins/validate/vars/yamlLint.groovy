def call(String reponame) {
    pipeline {
        environment { // блок для описания переменных окружения
            BITBUCKET_PROJECT_KEY="PROPORT"
            BITBUCKET_REPO_PATH="${BITBUCKET_PROJECT_KEY.toLowerCase()}/${reponame}"
            BITBUCKET_BASE_URL="https://gitlab.rosbank.rus.socgen" // адрес Gitlab server
            GITLAB_PRIVATE_TOKEN = credentials('rbsproportal-gitlab-api-token')
            GITLAB_API_URL="https://gitlab.rosbank.rus.socgen/api/v4/"
            GIT_PROJECT_KEY="PROPORT"
            GIT_PROJECT_KEY_LOWER="${GIT_PROJECT_KEY.toLowerCase()}"
            GIT_REPO_PATH="${GIT_PROJECT_KEY.toLowerCase()}/${reponame}"
        }

        agent {
            label "Proportal_agent-prod"
        }

        stages {
            stage('Validate YAML File') {
                steps {
                      withCredentials([
                       usernamePassword
                       (
                         credentialsId: "Pro-portal-test",
                         passwordVariable: 'CI_CD_TEST_PASS',
		             	      usernameVariable: 'CI_CD_TEST_USER'
                       )
                    ]){
                    script {   
                        gitClone()
                        def downloadScriptUrl = "https://gitlab.rosbank.rus.socgen/api/v4/projects/${GIT_PROJECT_KEY_LOWER}%2Finfra/repository/files/openshift%2Fgroovy_files%2Fyamllint.groovy/raw?ref=master"
                        sh "curl --header 'PRIVATE-TOKEN: ${GITLAB_PRIVATE_TOKEN}' ${downloadScriptUrl} -o ${WORKSPACE}/yamllint.groovy"
                        def PROJECT_ID= sh (script: "curl --silent --header \"PRIVATE-TOKEN: ${GITLAB_PRIVATE_TOKEN}\" \"${GITLAB_API_URL}groups/${GIT_PROJECT_KEY_LOWER}/projects?search=${reponame}\"  | jq \".[] | if .name ==\\\"${reponame}\\\" then .id else {} end\" | grep --invert-match {}", returnStdout: true).trim()
                        def mrResponse = sh(script: "curl --header 'PRIVATE-TOKEN: ${GITLAB_PRIVATE_TOKEN}' 'https://gitlab.rosbank.rus.socgen/api/v4/projects/${PROJECT_ID}/merge_requests?state=opened' | jq '.[0].iid'", returnStdout: true).trim()
                        def fileNameResponse = sh(script: "curl --header 'PRIVATE-TOKEN: ${GITLAB_PRIVATE_TOKEN}' 'https://gitlab.rosbank.rus.socgen/api/v4/projects/${PROJECT_ID}/merge_requests/${mrResponse}/changes' | jq -r '.changes[].new_path'  | grep \"[a-z-]*.yaml\"", returnStdout: true).trim()
                        println ("fileNameResponse= " + fileNameResponse)
                        println(fileNameResponse.getClass())
                        fileNameResponseList=fileNameResponse.split("\n").collect{it as String}
                        def fileNameDry = fileNameResponseList.toString().replaceAll("cloud/prod/", " ").replaceAll("\\[", " ").replaceAll("\\]", " ").split(",").collect{it as String}
                        println("list = " + fileNameResponseList)
                        println("listDry = " + fileNameDry)
                        loadGroovyFunc = load "yamllint.groovy"
                        for(int i=0; i< fileNameResponseList.size(); i++){
                            println("fileNameResponseList[i] = " + fileNameResponseList[i])
                            file= null
                            file=fileNameResponseList[i]
                            fileDry=fileNameDry[i].trim()
                         def yamlFile = readFile(file)
                         def yamlLines = yamlFile.readLines() 
                         loadGroovyFunc.startYamlLint(yamlFile, fileDry)
                         loadGroovyFunc.findSymbols(yamlLines, fileDry)
                         loadGroovyFunc.findSpaceAfterVariable(yamlLines, fileDry) 
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


}
    def gitClone() {
        sh "git config --global http.sslVerify false"  //git clone branch
        checkout([$class: 'GitSCM', branches: [[name: '${CHANGE_BRANCH}']],doGenerateSubmoduleConfigurations: false,
        extensions: [[$class: 'CleanBeforeCheckout'],[$class: 'CloneOption',depth: 1,noTags: true,shallow: true]],submoduleCfg: [],
        userRemoteConfigs:[[credentialsId: "proportal_deploy",url: "${env.BITBUCKET_BASE_URL}/${env.BITBUCKET_REPO_PATH}.git"]]])
    }
