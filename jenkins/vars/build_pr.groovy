def call(){
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
    REPO_NAME="proportal"
    reponame="ProPortal"
    SHELL_SCRIPTS_DIR="./deploy/pull_request"
    SONAR_IMAGE = 'docker-registry.gts.rus.socgen/general/sonarscanner:rb-4.6.2.2472'
    PROJECT_JIRA_KEY= "PROPRO"
    PROPORTAL_API_TOKEN=credentials('rbsproportal-gitlab-api-token')
    VOTES_COUNT = "2"
    WORKSPACE=pwd()
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
				if (env.CHANGE_BRANCH.toLowerCase().matches("^(feature|hotfix|release)\\/[propro|pprl|prgnt|prbs|pf|prlds|pl|prodbo|prnvr|corp]+-[0-9]+\$") || env.CHANGE_BRANCH.toLowerCase().matches("^(feature|hotfix|release)\\/[propro|pprl|prgnt|prbs|pf|prlds|pl|prodbo|prnvr|corp]+-[0-9]+_.*\$") || env.CHANGE_BRANCH.matches("FREEZE_MASTER")||  env.CHANGE_BRANCH.toLowerCase().matches("^(feature|hotfix|release)\\/[propro|pprl|prgnt|prbs|pf|prlds|pl|prodbo|prnvr|corp]+-[0-9]+-.*\$")) {
					if (env.CHANGE_BRANCH.toLowerCase().startsWith("feature/") && env.CHANGE_TARGET.toLowerCase().startsWith("release/")) {
						currentBuild.result = "FAILURE";
						error("мержить из feature/ в release/ нельзя")
					}
				} else {
					currentBuild.result = "FAILURE";
					error("Название ветки должно начинаться с feature/JIRA-1234 или hotfix/JIRA-5678. А у вас называется:'${env.CHANGE_BRANCH}. Разрешенные тикеты в Jira: [propro|pprl|prgnt|prbs|pf|prlds|pl|prodbo|prnvr|corp]'")
				}
			}
		}
	}
	//Переключаемся в ветку master (CHANGE_TARGET)
	stage('Checkout target branch'){
		steps {
			checkout([
			  $class: 'GitSCM',
			  branches: [[name: "*/${env.CHANGE_TARGET}"]],
			  doGenerateSubmoduleConfigurations: false,
			  //Чтобы мы могли понять список изменившихся файлов, нам нужна вся история гита. Поэтому никакие shallow:true,depth:1
              extensions: [[$class: 'CleanBeforeCheckout'],[$class: 'CloneOption',noTags: true]],
			  submoduleCfg: [],
			  userRemoteConfigs:[[
				credentialsId: "rbsproportal-gitlab",
				url: "${env.BITBUCKET_BASE_URL}/${env.BITBUCKET_REPO_PATH}.git"
			  ]]
			])
		}
	}
	//Запускаем проверку отчетов в ветке master (CHANGE_TARGET)
	stage('Report for target branch'){
		steps {
			script
			  {
			    //origin/$CHANGE_TARGET = master
			    //origin/$CHANGE_BRANCH = feature/PROPRO-1234
			    //git merge-base origin/$CHANGE_TARGET $CHANGE_BRANCH;
			    //git diff --name-only origin/$CHANGE_TARGET...origin/$CHANGE_BRANCH > changed_files.txt;
			    //должно быть что то типа такого:
			    //git diff --name-only \$(git merge-base origin/$CHANGE_TARGET origin/$CHANGE_BRANCH) origin/$CHANGE_BRANCH > changed_files.txt;

				sh """
				    echo "\$(git diff --name-only \$(git merge-base origin/$CHANGE_TARGET origin/$CHANGE_BRANCH) origin/$CHANGE_BRANCH)" > changed_files.txt;
                    echo "";
					echo "index.php" >> changed_files.txt;
					cat changed_files.txt | grep 'php\$' | grep -v '^vendor' | grep -v '^deploy'  > filtered_changed_files.txt;
					cat filtered_changed_files.txt;
					for filename in `cat filtered_changed_files.txt`; do if [[ -f "\$filename" ]]; then echo \$filename;  fi; done > existing_changed_files.txt;
					cat existing_changed_files.txt;
					php74 deploy/pull_request/codestyle/countBadWords.php badWordsMasterTarget.json;
					php74 deploy/pull_request/codestyle/checkDocumentation.php documentationMasterTarget.json;
					cat documentationMasterTarget.json;
					php74 deploy/pull_request/codestyle/phpcs.phar --config-set ignore_errors_on_exit 1;
					php74 deploy/pull_request/codestyle/phpcs.phar --config-set ignore_warnings_on_exit 1;
					php74 deploy/pull_request/codestyle/phpcs.phar --standard=deploy/pull_request/codestyle/phpcs.xml --report=checkstyle --ignore-annotations --report-file=target_report.xml --file-list=existing_changed_files.txt;
					php74 deploy/pull_request/codestyle/phpmd.phar `paste -d, -s existing_changed_files.txt` checkstyle deploy/pull_request/codestyle/phpmd.xml --ignore-errors-on-exit --ignore-violations-on-exit >> target_report.xml;
				"""
				//PROPRO-6650 - Самое время запустить скрипт, который посчитает сколько было использований "запрещенных слов" до MR'а в мастере.
			  }
		}
	}
	//Переключаемся в исходную фича-ветку (CHANGE_BRANCH)
	stage('Checkout source branch'){
        steps {
            checkout([
              $class: 'GitSCM',
              branches: [[name: "*/${env.CHANGE_BRANCH}"]],
              doGenerateSubmoduleConfigurations: false,
              submoduleCfg: [],
              userRemoteConfigs:[[
                credentialsId: "rbsproportal-gitlab",
                url: "${env.BITBUCKET_BASE_URL}/${env.BITBUCKET_REPO_PATH}.git"
              ]]
            ])
        }
    }
    //Запускаем скрипты-отчеты по фиче-ветке (CHANGE_BRANCH)
    stage('Report for source branch'){
        steps {
            script
              {
                sh """
                  echo "index.php" >> changed_files.txt;
                  cat changed_files.txt | grep 'php\$' | grep -v '^vendor' | grep -v '^deploy' > filtered_changed_files.txt;
                  cat filtered_changed_files.txt;
                  for filename in `cat filtered_changed_files.txt`; do if [[ -f "\$filename" ]]; then echo \$filename;  fi; done > existing_changed_files.txt;
                  cat existing_changed_files.txt;
                  echo "<?php" > includes/settings.php;
                  if [ -f deploy/pull_request/codestyle/getApproves.php ]; then
				    php74 deploy/pull_request/codestyle/getApproves.php ${PROPORTAL_API_TOKEN} ${BITBUCKET_PROJECT_KEY_LOWER} ${reponame} > approves.json;
                  fi
                  php74 deploy/pull_request/codestyle/countBadWords.php badWordsSource.json;
                  php74 deploy/pull_request/codestyle/checkDocumentation.php documentationSource.json;
                  cat documentationSource.json;
                  php74 deploy/pull_request/codestyle/phpcs.phar --config-set ignore_errors_on_exit 1;
                  php74 deploy/pull_request/codestyle/phpcs.phar --config-set ignore_warnings_on_exit 1;
                  php74 deploy/pull_request/codestyle/phpcs.phar --standard=deploy/pull_request/codestyle/phpcs.xml --report=checkstyle --ignore-annotations --report-file=source_report.xml --file-list=existing_changed_files.txt;
                  php74 deploy/pull_request/codestyle/phpmd.phar `paste -d, -s existing_changed_files.txt` checkstyle deploy/pull_request/codestyle/phpmd.xml --ignore-errors-on-exit --ignore-violations-on-exit >> source_report.xml;
                """
              }
        }
    }
	stage('Code quality diff'){
		steps {
			script
			  {
				sh """
				  php74 deploy/pull_request/codestyle/psr12_check.php $CHANGE_BRANCH
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
  //           error ("нужны аппрувы")
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
