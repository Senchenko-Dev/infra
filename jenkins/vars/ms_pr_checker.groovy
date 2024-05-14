def call(String reponame, String sonarenb) {
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
     gitLabConnection('Gitlab')
   }

   
   environment {
     APP_VERSION_PATTERN = "versions.app_version"
     BASE_PATTERN="rsb-PRODBO-"
     BITBUCKET_PROJECT_KEY="PROPORT"
     BITBUCKET_PROJECT_KEY_LOWER="${BITBUCKET_PROJECT_KEY.toLowerCase()}"
     GIT_BASE_URL="https://gitlab.rosbank.rus.socgen"
     GIT_PROJECT_KEY="PROPORT"
     GIT_PROJECT_KEY_LOWER="${GIT_PROJECT_KEY.toLowerCase()}"
     GIT_REPO_PATH="${GIT_PROJECT_KEY.toLowerCase()}/ms/${REPO_NAME}"
     GITLAB_ACCOUNT="rbsproportal-gitlab"
     GITLAB_API_URL="https://gitlab.rosbank.rus.socgen/api/v4"
     GIT_SHORT_URL="gitlab.rosbank.rus.socgen"
     GROUP_ID="proportal"
     GROUP_ID_TEMPLATE = "${GROUP_ID.toLowerCase()}"
     NEXUS_BASE_URL="docker-registry.gts.rus.socgen"
     PREFIX_PATTERN_FEATURE="feature/"
     PREFIX_PATTERN_HOTFIX="hotfix/"
     PREFIX_PATTERN_RELEASE="release/"
     REPO_NAME="${reponame}"
     NEXUS_FOLDER = "pport"
     SHELL_SCRIPTS_DIR="./jenkins"
     SONAR_IMAGE = 'docker-registry.gts.rus.socgen/general/sonarscanner:rb-4.6.2.2472'
     PROPORTAL_API_TOKEN=credentials('rbsproportal-gitlab-api-token')
     WORKSPACE=pwd()
     CI = "CI139032"
     PLATFORM_VERSION_PATTERN = "platformVersion"
   }
    
   agent {
     node {
       label "PROPORTAL_linux_1"
     }
   }

   stages 
   {
     stage('Check target branch') {
       steps {
         script {
           env.VERSIONS_MATCH = "true"
           env.CURRENT_DATE = sh (returnStdout: true, script: 'echo -n $(date +%Y%m%d-%H%M)')
           if (env.CHANGE_BRANCH) { 
             env.CHECKOUT_BRANCH = env.CHANGE_BRANCH
           } else {
             env.CHECKOUT_BRANCH = env.BRANCH_NAME
           }
         }
       }
     }
    
     stage('Branch name check'){
	    steps {
			script {
        string FULL_PATTERN = null
				if (env.CHANGE_BRANCH.toLowerCase().matches("^(feature|hotfix|release)\\/[propro|pprl|prgnt|prbs|pf|prlds|pl|prodbo|prnvr|corp]+-[0-9]+\$") || env.CHANGE_BRANCH.toLowerCase().matches("^(feature|hotfix|release)\\/[propro|pprl|prgnt|prbs|pf|prlds|pl|prodbo|prnvr|corp]+-[0-9]+_.*\$") || env.CHANGE_BRANCH.matches("FREEZE_MASTER")||  env.CHANGE_BRANCH.toLowerCase().matches("^(feature|hotfix|release)\\/[propro|pprl|prgnt|prbs|pf|prlds|pl|prodbo|prnvr|corp]+-[0-9]+-.*\$")) {
					if (env.CHANGE_BRANCH.toLowerCase().startsWith("feature/") && env.CHANGE_TARGET.toLowerCase().startsWith("release/")) {
						currentBuild.result = "FAILURE";
						error("мержить из feature/ в release/ нельзя")
					}
				} else {
					currentBuild.result = "FAILURE";
					error("Название ветки должно начинаться с feature/JIRA-1234 или hotfix/JIRA-5678. А у вас называется:'${env.CHANGE_BRANCH}. Разрешенные тикеты в Jira: [propro|pprl|prgnt|prbs|pf|prlds|pl|prodbo|prnvr|corp]'")
				}
        if (env.CHECKOUT_BRANCH.contains("${PREFIX_PATTERN_HOTFIX}")) {
                 FULL_PATTERN = "${PREFIX_PATTERN_HOTFIX}"
        }
        if (env.CHECKOUT_BRANCH.contains("${PREFIX_PATTERN_FEATURE}")) {
                 FULL_PATTERN = "${PREFIX_PATTERN_FEATURE}"
        }
			 }
		  }
     }
     stage('Checkout target branch') {
       when {
         expression {env.CHANGE_TARGET != null}
       }
       steps {
         checkout([
           $class: 'GitSCM',
           branches: [[name: "*/${env.CHANGE_TARGET}"]],
           doGenerateSubmoduleConfigurations: false,
           extensions: [[$class: 'CleanBeforeCheckout']],
           submoduleCfg: [],
           userRemoteConfigs:[[
             credentialsId: "${GITLAB_ACCOUNT}",
             url: "${env.GIT_BASE_URL}/${env.GIT_REPO_PATH}.git"
           ]]
         ])
       }
     }
  
     stage('Get app version from target repository') {
       when {
         expression {env.CHANGE_TARGET != null}
       }
       steps {
         script {
           env.TARGET_APP_VER = getVersion()
           if (env.TARGET_APP_VER == "null") {
             currentBuild.result = "FAILURE"
             error ("В ${VERSION_FILE_NAME} отсутствует или нарушен обязательный аттрибут ${APP_VERSION_PATTERN}")
           } else {
             env.TARGET_APP_MAJOR_VER = get_subversion("major_ver","${env.TARGET_APP_VER}")
             env.TARGET_APP_MINOR_VER = get_subversion("minor_ver","${env.TARGET_APP_VER}")
             env.TARGET_APP_FIX_VER = get_subversion("fix_ver","${env.TARGET_APP_VER}")
           }
         }
       }
     }
  
     stage('Checkout source branch') {
       steps {
         checkout([
           $class: 'GitSCM',
           branches: [[name: "*/${env.CHECKOUT_BRANCH}"]],
           doGenerateSubmoduleConfigurations: false,
           extensions: [[$class: 'WipeWorkspace']],
           submoduleCfg: [],
           userRemoteConfigs: [[
             credentialsId: "${GITLAB_ACCOUNT}",
             url: "${env.GIT_BASE_URL}/${env.GIT_REPO_PATH}.git"
           ]]
         ])
       }
     }

     stage('Get app version from source repository') {
       steps {
         script {
           env.SOURCE_APP_VER = getVersion()
           if (env.SOURCE_APP_VER == "null") {
             currentBuild.result = "FAILURE"
             error ("В ${env.VERSION_FILE_NAME} отсутствует или нарушен обязательный аттрибут ${APP_VERSION_PATTERN}")
           } else {
             env.SOURCE_APP_MAJOR_VER = get_subversion("major_ver","${env.SOURCE_APP_VER}")
             env.SOURCE_APP_MINOR_VER = get_subversion("minor_ver","${env.SOURCE_APP_VER}")
             env.SOURCE_APP_FIX_VER = get_subversion("fix_ver","${env.SOURCE_APP_VER}")
           }
         }
       }
     }
  
     stage('Compare source and target versions') {
       when {
         expression {env.FULL_PATTERN != "null" || env.CHANGE_TARGET != null}
       }
       steps {
         script {
           if (FULL_PATTERN == "${PREFIX_PATTERN_HOTFIX}") {
             if (!isTargetEqualsSource("${env.SOURCE_APP_MAJOR_VER.toInteger()}${env.SOURCE_APP_MINOR_VER.toInteger()}${SOURCE_APP_FIX_VER.toInteger()}","${env.TARGET_APP_MAJOR_VER.toInteger()}${env.TARGET_APP_MINOR_VER.toInteger()}${TARGET_APP_FIX_VER.toInteger() + 1}")) {
               env.VERSIONS_MATCH = "false"
               env.NEW_APP_VERSION = "${env.TARGET_APP_MAJOR_VER.toInteger()}.${env.TARGET_APP_MINOR_VER.toInteger()}.${TARGET_APP_FIX_VER.toInteger() + 1}"
             }
           }
           if (FULL_PATTERN == "${PREFIX_PATTERN_FEATURE}") {
             if (!isTargetEqualsSource("${env.SOURCE_APP_MAJOR_VER.toInteger()}${env.SOURCE_APP_MINOR_VER.toInteger()}${SOURCE_APP_FIX_VER.toInteger()}","${env.TARGET_APP_MAJOR_VER.toInteger()}${env.TARGET_APP_MINOR_VER.toInteger() + 1}0")) {
               env.VERSIONS_MATCH = "false"
               env.NEW_APP_VERSION = "${env.TARGET_APP_MAJOR_VER.toInteger()}.${env.TARGET_APP_MINOR_VER.toInteger() + 1}.0"
             }
           }
         }
       }
     }
  
     stage('Write new app version') { 
       when {
         expression {env.VERSIONS_MATCH == "false"}
       }
       steps {
         script {
           string CHECKOUT_BRANCH_NON_PATTERNED = "${env.CHECKOUT_BRANCH.replace("${FULL_PATTERN}","")}"
           file_content="version"
           env.GET_JIRA_ISSUE_KEY = CHECKOUT_BRANCH_NON_PATTERNED.substring(0,CHECKOUT_BRANCH_NON_PATTERNED.indexOf("_"))
           NEW_APP_VERSION_STRING = "${env.NEW_APP_VERSION}"
           set_new_app_version (version, "${NEW_APP_VERSION_STRING}")
           if (file_content.contains("${NEW_APP_VERSION_STRING}") && !file_content.contains(old_app_version)) {
             write_changes("${VERSION_FILE_NAME}",file_content,false,false)
           } else {
             currentBuild.result = "FAILURE"
             error ("При записи ${NEW_APP_VERSION_STRING} в ${VERSION_FILE_NAME} произошла ошибка")
           }
        }
      }
    }
  
    stage('Write changelog') {
      when {
         expression {env.VERSIONS_MATCH == "false"}
       }
       steps {
         script {
           write_changes("change.log","${env.NEW_APP_VERSION}-${env.CURRENT_DATE}-${CHANGE_TITLE}-https://jirahq.rosbank.rus.socgen/browse/${GIT_PROJECT_KEY}-${env.GET_JIRA_ISSUE_KEY}",true,true)
         }
       }
     }
  
     stage ('Commit changes to source') {
       when {
         expression {env.VERSIONS_MATCH == "false"}
       } 
       steps {
         withCredentials([
           usernamePassword (
             credentialsId: "${GITLAB_ACCOUNT}",
             passwordVariable: 'GIT_PASS',
             usernameVariable: 'GIT_USER')]) {
               script {
                 sh """
                 git config --global user.email "${GIT_USER}@rosbank.ru"
                 git config --global user.name "${GIT_USER}"
                 git add version change.log
                 git commit -m "Version changed from ${SOURCE_APP_VER} to ${env.NEW_APP_VERSION}"
                 git push https://${GIT_USER}:"${GIT_PASS}"@${env.GIT_SHORT_URL}/${env.GIT_REPO_PATH}.git HEAD:${env.CHECKOUT_BRANCH}
                 """
               }
             }
       }
     }
  
     stage('Break job if it needs') {
       when {
         expression {env.VERSIONS_MATCH == "false"}
       }
       steps {
         script {
           currentBuild.result = "ABORTED"
           error ("Плановое применение новых параметров коммита")
         }
       }
     }
     

     stage('Build pull request') {
       when {
         expression {env.REPO_NAME != 'is-api'}
       }
       steps {
         withCredentials([
           usernamePassword (
             credentialsId: "trbs-proportal-oc",
             passwordVariable: 'CI_CD_PASS',
             usernameVariable: 'CI_CD_USER'
           )]) {
           script {
             env.TAG_VER = sh (returnStdout: true, script: 'echo -n $(date +%Y%m%d-%H%M)')
             sh """
             docker build . -f docker/Dockerfile.dev -t ${reponame}${TAG_VER}
             docker run --rm --name  ${reponame}${TAG_VER} -dt ${reponame}${TAG_VER} bash
             docker exec  --user postgres ${reponame}${TAG_VER} /bin/bash -c  " pg_ctl restart --pgdata=/var/lib/postgresql/data -l /var/log/postgresql/test.log && psql -c 'create database  test'"
             docker exec  --user root ${reponame}${TAG_VER} /bin/bash -c "cd prod && php artisan migrate --env=testing && php artisan test --coverage --parallel --processes=4 >> /tmp/report.txt"
             """
             COVERAGE=sh(script: "docker exec --user root ${reponame}${TAG_VER} /bin/bash -c \"cat /tmp/report.txt | grep \'Total: [0-9\\.]*\' | grep -o [0-9\\.]*\"", returnStdout: true).toDouble()
             println("COVERAGE= " + COVERAGE)
             sh "docker rm -f ${reponame}${TAG_VER} "
             if(COVERAGE < 80){
                 currentBuild.result = "ABORTED"
                 error ("Покрытие Unit тестами < 80%")
             }
             else{
                 println("Покрытие Unit тестами=" + COVERAGE )
             }
           }
         }
       }
     }
  
     stage('Get App Version') {
       steps {
         script {
           if (env.VERSIONS_MATCH == "false") {
             env.SOURCE_APP_VER = env.NEW_APP_VERSION
           } else { 
             sh "echo ${env.SOURCE_APP_VER}"
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
                sh "sonar-scanner -X -Dsonar.host.url=https://sonarqube.gts.rus.socgen/ -Dsonar.projectVersion=1.0 -Dsonar.language=php -Dsonar.sourceEncoding=UTF-8 -Dsonar.phpCodesniffer.timeout=120 -Dsonar.projectKey=rsb-sl-ct-MOS_cicd-ProPortal-DevOPS -Dsonar.projectName=proportal::${reponame} -Dsonar.sources=. -Dsonar.test.inclusions=./tests/* -Dsonar.exclusions=./tests/*"
	             }
          }
        }

   }
   post {
    //  success {
    //     script {
    //         env.GITLAB_PROJECT_ID = sh(script: "curl --silent --header \"PRIVATE-TOKEN: ${PROPORTAL_API_TOKEN}\" \"${GITLAB_API_URL}/groups/${BITBUCKET_PROJECT_KEY_LOWER}\"  | jq \".projects[] | if .name ==\\\"${reponame}\\\" then .id else {} end\" | grep --invert-match {}", returnStdout: true).trim()
    //         env.COMMIT_SHA_NUM = sh (script: "git rev-parse HEAD", returnStdout: true).trim()
    //         sh """
    //             curl --request POST --header 'PRIVATE-TOKEN: ${PROPORTAL_API_TOKEN}' \\
    //             --data 'state=success' \\
    //             --data 'description=external' \\
    //             --data 'target_url=${RUN_DISPLAY_URL}' \\
    //             --data 'name=jenkinsci/mr-merge' \\
    //             'https://gitlab.rosbank.rus.socgen/api/v4/projects/${GITLAB_PROJECT_ID}/statuses/${COMMIT_SHA_NUM}'
    //         """
    //     }
    //  }
    // failure {
    //     script {
    //         env.GITLAB_PROJECT_ID = sh(script: "curl --silent --header \"PRIVATE-TOKEN: ${PROPORTAL_API_TOKEN}\" \"${GITLAB_API_URL}/groups/${BITBUCKET_PROJECT_KEY_LOWER}\"  | jq \".projects[] | if .name ==\\\"${reponame}\\\" then .id else {} end\" | grep --invert-match {}", returnStdout: true).trim()
    //         env.COMMIT_SHA_NUM = sh (script: "git rev-parse HEAD", returnStdout: true).trim()
    //         sh """
    //             curl --request POST --header 'PRIVATE-TOKEN: ${PROPORTAL_API_TOKEN}' \\
    //             --data 'state=failed' \\
    //             --data 'description=external' \\
    //             --data 'target_url=${RUN_DISPLAY_URL}' \\
    //             --data 'name=jenkinsci/mr-merge' \\
    //             'https://gitlab.rosbank.rus.socgen/api/v4/projects/${GITLAB_PROJECT_ID}/statuses/${COMMIT_SHA_NUM}'
    //         """
    //     }
    //  }
     
     cleanup
    {
      script
      {
        withEnv(['PATH+EXTRA=/usr/sbin:/usr/bin:/sbin:/bin'])
        {
          dir("/home/jenkins-agent/workspace/")
          {
            sh "rm -r ${WORKSPACE}*"
          }
        }
      }
    }
     }
   }
  
}

//*******Additional Methods*******//

//*******Global variables*******//
def file_content, old_app_version
//*****************************//
Boolean isTargetEqualsSource (String source_ver, String target_ver) {
  try {
    boolean isEquals = false
    if (target_ver == source_ver) {
      isEquals = true
    }
    return isEquals
  } catch (err) {
    println err
  }  
}

Boolean isApprove (String source_branch, String prefix_pattern, String base_pattern) {
  try {
    boolean approved
    String pattern = null
    switch (prefix_pattern) {
      case "feature/":
        pattern = "([0-9]+)(-)(.{3,})"
      break
      case "hotfix/":
        pattern = "([0-9]+)(-)(.{3,})"
      break
    }
    if (source_branch.matches(prefix_pattern + base_pattern + pattern)) {
      approved = true
    } else {
      approved = false
    }
    return approved
  } catch (err) {
    approved = false
    println err
  }
}


String getVersion()
{
  MS_VERSION = readFile("version")
  env.old_app_version= MS_VERSION
  VERSION_FILE_NAME="version"
  file_content= VERSION_FILE_NAME
  return MS_VERSION
}

String get_subversion (String subversion_type, String version) {
  try {
    String version_value = null
    switch (subversion_type) {
      case "major_ver":
        version_value = version.substring(0,version.indexOf("."))
      break
      case "minor_ver":
        version_value = version.substring(version.indexOf(".") + 1,version.lastIndexOf("."))
      break
      case "fix_ver":
        version_value = version.substring(version.lastIndexOf(".") + 1)
      break
    }
    return version_value
  } catch (err) {
    println err
  }
}

def check_jira_fields(){
  if (env.CHECKOUT_BRANCH.contains(PREFIX_PATTERN_FEATURE) || env.CHECKOUT_BRANCH.contains(PREFIX_PATTERN_HOTFIX))
  {
  def jira_key= CHECKOUT_BRANCH.find('PRODBO-[0-9]*')
    if (jira_key.isEmpty()){
        currentBuild.result = "FAILURE"
        error ('В названии ветки отсутствует задача из Jira')
    }
    else{
    def get_data = jiraJqlSearch jql: "key=${jira_key}", site: 'DBOPro'
    def active_fixversion =  sh (script: 'curl -u ${CI_CD_USER}:${CI_CD_PASS} https://jirahq.rosbank.rus.socgen:8443/rest/agile/1.0/board/3182/sprint?state=active | jq \'[.values[].name]\' | grep -o [0-9]*', returnStdout: true)
    boolean correct_name = true
    fixversion=get_data.data.issues.fields.fixVersions.name
    microservice=get_data.data.issues.fields.customfield_16200.value
    state=get_data.data.issues.fields.status.name
    team_name=get_data.data.issues.fields.customfield_14308.value
    component=get_data.data.issues.fields.components.name
    if (fixversion.find{ it =~ ["0-9*"]} == null || microservice.find{ it =~ ["[a-z]*\\-[a-z]*"]} == null || team_name.find{it =~ ["a-zA-Z\\-0-9*"]} == null  || component.find{it =~ ["A-Za-z*\\-A-Za-z*"]} == null)
        {
          ms=microservice.find{ it =~ ["[a-z]*\\-[a-z]*"]}
          fv=fixversion.find{ it =~ ["0-9*"]}
          tm=team_name.find{it =~ ["a-zA-Z\\-0-9*"]}
          ct=component.find{it =~ ["A-Za-z*\\-A-Za-z*"]}.find{it =~ ["A-Za-z*\\-A-Za-z*"]}
          currentBuild.result = "FAILURE"
          error ("Не заполнено поле Микросервисы = ${ms}, Component/s = ${ct}, Pro Scrum Team = ${tm} или fix_version = ${fv} в задаче ${jira_key}")}
        else {
            println(fixversion.find{ it =~ ["0-9*"]})
            println(microservice.find{ it =~ ["[a-z]*\\-[a-z]*"]})
            println(team_name.find{it =~ ["a-zA-Z\\-0-9*"]})                       
            for(int i =0; i<= microservice.size(); i ++){
            if (microservice[i].find{ it =~ REPO_NAME}.toString().trim() == false){
                correct_name = false
            }
            else{
            correct_name = true
            }
            }
            if (correct_name == false){
              currentBuild.result = "FAILURE"
              error ("В задаче ${jira_key} отсутствует микросервис ${REPO_NAME}")
            }
            if (fixversion.find{ it =~ ["0-9*"]}.find{ it =~ ["0-9*"]}.toInteger() < active_fixversion.toInteger()){
              currentBuild.result = "FAILURE"
              error ("В задаче ${jira_key} fixVersion меньше ${active_fixversion}. Командам необходимо проставить полю fixVersion значение, равное номеру текущего спринта ${active_fixversion} , либо, если задача не идет в текущий спринт, изменить значение поля fixVersion ")
            }
            if (component.find{it =~ ["A-Za-z*\\-A-Za-z*"]}.find{it =~ ["A-Za-z*\\-A-Za-z*"]} != "Back-End" && component.find{it =~ ["A-Za-z*\\-A-Za-z*"]}.find{it =~ ["A-Za-z*\\-A-Za-z*"]} != "Front-End" && component.find{it =~ ["A-Za-z*\\-A-Za-z*"]}.find{it =~ ["A-Za-z*\\-A-Za-z*"]} != "Middle Layer"){
              currentBuild.result = "FAILURE"
              error ("Компонент в задаче ${jira_key} не равен Back-End,Front-End или Middle Layer") 
            }
            if (state.find{ it =~ ["a-z*"]} == "Released" || state.find{ it =~ ["a-z*"]} == "Done" || state.find{ it =~ ["a-z*"]} == "Clossed"){
              currentBuild.result = "FAILURE"
              error ("Статус задачи ${jira_key} released, clossed или done")
            }    
            }
      }
  }
}

def set_new_app_version (def filecontent, String new_app_version) {
  try {
    file_content = filecontent.replace(old_app_version,new_app_version)
  } catch (err) {
    println err
  }
}

def write_changes(String fileName, String line, boolean is_change_log_file, boolean clean_var_out_of_memory) {
  try {
    if (fileExists(fileName))  {
      if (is_change_log_file) {
        file_content = readFile fileName
        writeFile file: fileName, text: line + "\n" +  file_content
      } else {
        writeFile file: fileName, text: line
      }
    } else {
      if (is_change_log_file) {
        writeFile file: fileName, text: line
      }  
    }
    if (clean_var_out_of_memory) {
      file_content = null
    }  
  } catch (err) {
    println err
  }
}

//*******End*******//

