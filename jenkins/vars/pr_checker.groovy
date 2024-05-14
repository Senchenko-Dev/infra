def call(String reponame, String sonarenb) {
  pipeline 
  {
  options
  {
    buildDiscarder(logRotator(
      artifactDaysToKeepStr: '14',
      artifactNumToKeepStr: '14',
      daysToKeepStr: '14',
      numToKeepStr: '40'))
    timestamps()
    disableConcurrentBuilds()
    skipDefaultCheckout()
  }

  environment
  {
    BITBUCKET_BASE_URL="https://gitlab.rosbank.rus.socgen"
    BITBUCKET_PROJECT_KEY="PROPORT"
    BITBUCKET_PROJECT_KEY_LOWER="${BITBUCKET_PROJECT_KEY.toLowerCase()}"
    BITBUCKET_REPO_PATH="${BITBUCKET_PROJECT_KEY.toLowerCase()}/${REPO_NAME}"
    BITBUCKET_SHORT_URL="gitlab.rosbank.rus.socgen"
    GITLAB_API_URL="https://gitlab.rosbank.rus.socgen/api/v4"
    GROUP_ID="proportal"
    JIRA_ENV="https://jirahq.rosbank.rus.socgen:8443/rest/api/2/issue"
    REPO_NAME="${reponame.toLowerCase()}"
    SHELL_SCRIPTS_DIR="./deploy/pull_request"
    SONAR_IMAGE = 'docker-registry.gts.rus.socgen/general/sonarscanner:rb-4.6.2.2472'
    PROJECT_JIRA_KEY= "PROPRO"
    VOTES_COUNT = "2"
    WORKSPACE=pwd()
    PROPORTAL_API_TOKEN=credentials('rbsproportal-gitlab-api-token')
  }
  agent
  {
    label "PROPORTAL_linux_1"
  }


  stages
  {
	stage('Branch name check'){
	    steps {
			script {
				if (env.CHANGE_BRANCH.toLowerCase().startsWith("feature/") || env.CHANGE_BRANCH.toLowerCase().startsWith("hotfix/") || env.CHANGE_BRANCH.toLowerCase().startsWith("release/")) {
					if (env.CHANGE_BRANCH.toLowerCase().startsWith("feature/") && env.CHANGE_TARGET.toLowerCase().startsWith("release/")) {
						currentBuild.result = "FAILURE";
						error("Нельзя мерджить feature/ в release/");
					}
				} else {
					currentBuild.result = "FAILURE";
					error("Название ветки должно начинаться с feature/ или hotfix/");
				}
			}
		}
	}
	stage('Checkout target branch'){
		steps {
			checkout([
			  $class: 'GitSCM',
			  branches: [[name: "*/${env.CHANGE_TARGET}"]],
			  doGenerateSubmoduleConfigurations: false,
        extensions: [[$class: 'CleanBeforeCheckout'],[$class: 'CloneOption',depth: 1,noTags: true,shallow: true]],
			  submoduleCfg: [],
			  userRemoteConfigs:[[
				credentialsId: "proportal_deploy",
				url: "${env.BITBUCKET_BASE_URL}/${env.BITBUCKET_REPO_PATH}.git"
			  ]]
			])
		}
	}
	stage('Checkout source branch'){
        steps {
            checkout([
              $class: 'GitSCM',
              branches: [[name: "*/${env.CHANGE_BRANCH}"]],
              doGenerateSubmoduleConfigurations: false,
              submoduleCfg: [],
              userRemoteConfigs:[[
                credentialsId: "proportal_deploy",
                url: "${env.BITBUCKET_BASE_URL}/${env.BITBUCKET_REPO_PATH}.git"
              ]]
            ])
        }
    }

	stage('Code quality diff'){
    when
      {
        expression {reponame == "Lumen" || reponame == 'ProPortal'}
      }
		steps {
			script
			  {
				sh """
				  php deploy/jenkins/codestyle/prs12_check.php $CHANGE_BRANCH
				"""
			  }
		}
	}
  // stage('Check approvals'){
  //      steps{
  //        script{
  //          env.COMMIT_SHA_NUM = sh (script: "git rev-parse HEAD", returnStdout: true).trim()
  //          env.GITLAB_PROJECT_ID = sh(script: "curl --silent --header \"PRIVATE-TOKEN: ${PROPORTAL_API_TOKEN}\" \"${GITLAB_API_URL}/groups/${BITBUCKET_PROJECT_KEY_LOWER}\"  | jq \".projects[] | if .name ==\\\"${reponame}\\\" then .id else {} end\" | grep --invert-match {}", returnStdout: true).trim()
  //          env.MR_IID = sh(script: " curl --silent --header \"PRIVATE-TOKEN: ${PROPORTAL_API_TOKEN}\" \"${GITLAB_API_URL}/projects/${GITLAB_PROJECT_ID}/merge_requests?state=opened\" | jq \".[] | if .sha ==\\\"${COMMIT_SHA_NUM}\\\" and .state == \\\"opened\\\" then .iid else {} end\"  | grep --invert-match {}", returnStdout: true).trim()
  //          env.APPROVALS_NAMES = sh(script: "curl --silent --header \"PRIVATE-TOKEN: ${PROPORTAL_API_TOKEN}\" \"${GITLAB_API_URL}/projects/${GITLAB_PROJECT_ID}/merge_requests/${MR_IID}/approvals\" | jq \".approved_by[].user.name\"", returnStdout: true).replaceAll("[\n\r]", "")
  //          env.APPROVALS_NAMES_LEIGHT = sh(script: "curl --silent --header \"PRIVATE-TOKEN: ${PROPORTAL_API_TOKEN}\" \"${GITLAB_API_URL}/projects/${GITLAB_PROJECT_ID}/merge_requests/${MR_IID}/approvals\" | jq \".approved_by[].user.name\"", returnStdout: true).readLines().size()
  //          if (env.APPROVALS_NAMES_LEIGHT >= env.VOTES_COUNT){
  //            sh "echo ok"
  //          }
  //          else{
  //            currentBuild.result = "FAILURE"
  //           error ("В МР нет 2-х или более аппрувов")
  //          }
  //        }
  //      }
  //    }
  
//	stage('Create JIRA Issue') {
//                steps{
//                    script{
//                        withEnv(["JIRA_SITE=${env.JIRA_ENV}"]) {
//                           //Узнаем ID проекта Jira
//                            def project = jiraGetProject idOrKey: PROJECT_JIRA_KEY
//                            env.PROJECT_JIRA_ID = project.data.id.toString()
//
//                            def NewIssue = [fields: [ project: [id: env.PROJECT_JIRA_ID],
//                                    summary: 'New JIRA Created from Jenkins.',
//                                    description: 'New JIRA Created from Jenkins.',
//                                    epicLists: 'PROPRO-2525',
//                                    issuetype: [name: 'Task']]]
//                            response = jiraNewIssue issue: NewIssue
//
//                            echo response.successful.toString()
//                            echo response.data.key.toString()
//                            //Получаем ID созданной таски
//                            env.ISSUE_JIRA_KEY = response.data.key.toString()
//                        }
//                    }
//
//                }
//            }
    // stage('SonarQube Scanning') {
    //   when
    //   {
    //     expression {sonarenb == "true"}
    //   }
    //       agent {
    //         docker {
    //           registryUrl 'https://docker-registry.gts.rus.socgen'
    //           image "${SONAR_IMAGE}"
    //           reuseNode true
    //         }
    //        }
    //        steps {
    //          withSonarQubeEnv(credentialsId: 'proportal', installationName: 'sonarqube-prod') {
    //             sh "sonar-scanner -X -Dsonar.host.url=https://sonarqube.gts.rus.socgen/ -Dsonar.projectVersion=1.0 -Dsonar.language=php -Dsonar.sourceEncoding=UTF-8 -Dsonar.phpCodesniffer.timeout=120 -Dsonar.projectKey=ProPortal -Dsonar.projectName=proportal::proportal -Dsonar.sources=. -Dsonar.test.inclusions=./tests/* -Dsonar.exclusions=./tests/*"
	  //            }
    //       }
    //     }
//  stage("Quality Gate") {
//          steps {
//               script {
//                       def getURL = readProperties file: './.scannerwork/report-task.txt'
//                       echo "${getURL['ceTaskUrl']}"
//                    // контекст для waitForQualityGate() передается из withSonarQubeEnv(...)
//                       def quality_gate = waitForQualityGate()
//                       withEnv(["JIRA_SITE=${env.JIRA_ENV}"]) {
//                          if (quality_gate.status != 'OK') {
//                               // Добавляем комментарий к задаче в Jira
//                               def comment = [ body: "Quality Gate is failed: ${getURL['dashboardUrl']}" ]
//                               jiraAddComment idOrKey: env.ISSUE_JIRA_KEY, input: comment
//
//                               // ОПЦИОНАЛЬНО: останавливаемся если не пройден QG
//                               error "Pipeline aborted due to quality gate failure: ${quality_gate.status}"
//                            } else {
//                                // Добавляем комментарий к задаче в Jira
//                                def comment = [ body: "Quality Gate is ok ${getURL['dashboardUrl']}" ]
//                                jiraAddComment idOrKey: env.ISSUE_JIRA_KEY, input: comment
//                            }
//                        }
//                    }
//                }
//            }
//  stage("Get Issue status"){
//              steps{
//                  script{
//                      withEnv(["JIRA_SITE=${env.JIRA_ENV}"]) {
//                          def issue_status = ''
//                          def infinite_loop_var = 0
//                          while ( infinite_loop_var != 1 ) {
//                              def issue = jiraGetIssue idOrKey: env.ISSUE_JIRA_KEY
//                              echo "jiraGetIssue"
//                              issue_status = issue.data.fields.status.name.toString()
//                              echo issue_status
//                              if( issue_status == 'Done' ) {
//                                  echo "Jira Issue ${env.ISSUE_JIRA_KEY} Done"
//                                  env.ISSUE_STATUS = issue_status
//                                  break
//                              }
//                              if( issue_status == 'Cancelled' ) {
//                                  error "Jira Issue ${env.ISSUE_JIRA_KEY} Cancelled"
//                              } else {
//                                  sleep(30)
//                              }
//                          }
//                      }
//                  }
//              }
//          }
//  stage('Print status'){
//      steps{
//          echo "${env.ISSUE_STATUS}"
//            }
//         }
     

    }
  
  post
  {
    success {
        script {
            env.GITLAB_PROJECT_ID = sh(script: "curl --silent --header \"PRIVATE-TOKEN: ${PROPORTAL_API_TOKEN}\" \"${GITLAB_API_URL}/groups/${BITBUCKET_PROJECT_KEY_LOWER}\"  | jq \".projects[] | if .name ==\\\"${reponame}\\\" then .id else {} end\" | grep --invert-match {}", returnStdout: true).trim()
            env.COMMIT_SHA_NUM = sh (script: "git rev-parse HEAD", returnStdout: true).trim()
            sh """
                curl --request POST --header 'PRIVATE-TOKEN: ${PROPORTAL_API_TOKEN}' \\
                --data 'state=success' \\
                --data 'description=external' \\
                --data 'target_url=${RUN_DISPLAY_URL}' \\
                --data 'name=jenkinsci/mr-merge' \\
                'https://gitlab.rosbank.rus.socgen/api/v4/projects/${GITLAB_PROJECT_ID}/statuses/${COMMIT_SHA_NUM}'
            """
        }
     }
    failure {
        script {
            env.GITLAB_PROJECT_ID = sh(script: "curl --silent --header \"PRIVATE-TOKEN: ${PROPORTAL_API_TOKEN}\" \"${GITLAB_API_URL}/groups/${BITBUCKET_PROJECT_KEY_LOWER}\"  | jq \".projects[] | if .name ==\\\"${reponame}\\\" then .id else {} end\" | grep --invert-match {}", returnStdout: true).trim()
            env.COMMIT_SHA_NUM = sh (script: "git rev-parse HEAD", returnStdout: true).trim()
            sh """
                curl --request POST --header 'PRIVATE-TOKEN: ${PROPORTAL_API_TOKEN}' \\
                --data 'state=failed' \\
                --data 'description=external' \\
                --data 'target_url=${RUN_DISPLAY_URL}' \\
                --data 'name=jenkinsci/mr-merge' \\
                'https://gitlab.rosbank.rus.socgen/api/v4/projects/${GITLAB_PROJECT_ID}/statuses/${COMMIT_SHA_NUM}'
            """
        }
     }

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
            sh "rm -r ${WORKSPACE}*"
          }
        }
      }
    }
  }
  }
}

