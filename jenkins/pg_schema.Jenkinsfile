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
        string(name: 'schema_name',defaultValue: '', description: 'Введите имя схемы')
        booleanParam(name: 'TEST',defaultValue: 'false',description: 'Запустить настройку в TEST Porstgresql')
        booleanParam(name: 'PROD',defaultValue: 'false',description: 'Запустить настройку в PROD Porstgresql')
        booleanParam(name: 'CERT',defaultValue: 'false',description: 'Запустить настройку в PROD Porstgresql')
    }
    environment { // блок для описания переменных окружения
        TEST_SERVER_POSTGRES = "193.48.98.238"
        PROD_SERVER_POSTGRES = "193.48.9.53"
        DR_SERVER_POSTGRES = "193.48.9.109"
        CERT_SERVER_POSTGRES = "193.48.68.201"
        DOMAIN_TEST = "trosbank.trus.tsocgen"
        DOMAIN_PROD = "rosbank.rus.socgen"
    }

   agent none

    stages  {
		stage('Setting to Postgres TEST') {
        	agent {
		       label "PROPORTAL_linux_1"
	        }
     		when {
              expression {
                   params.TEST == true
               }
            }
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'trbs-proportal-oc', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                        def commands = [
                              "psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'CREATE SCHEMA IF NOT EXISTS ${schema_name}\'",
                              "psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'alter schema ${schema_name} owner to dbo_admin\'",
                              "psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'grant all on schema ${schema_name} to dbo_admin with grant option\'",
                              "psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'grant usage on schema ${schema_name} to dbo_support, dbo_user\'",
                              "psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant all on tables to dbo_admin with grant option\'",
                              "psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant insert, select, update, delete on tables to dbo_support\'",
                              "psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant select on tables to dbo_user\'",
                              "psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant all on SEQUENCES to dbo_admin with grant option\'",
                              "psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant select on SEQUENCES to dbo_user, dbo_support\'",
                              "psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant all on FUNCTIONS  to dbo_admin with grant option\'",
                              "psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant execute on FUNCTIONS to dbo_user, dbo_support\'",
                        ]  

                        for (def command in commands) {
                            sh "sshpass -p ${PASS} ssh -o StrictHostKeyChecking=no ${USER}@${DOMAIN_TEST}@${TEST_SERVER_POSTGRES} \"$command\""
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
                        currentBuild.result = currentBuild.result ?: 'SUCCESS' || currentBuild.result ?: 'FAILURE'  //удаляет workspace при успешном или не успешной сборке
                    }
                }
            }        
        } 
		stage('Setting to Postgres PROD') {
        	agent {
		       label "Proportal_agent-prod"
	        }
     		when {
              expression {
                   params.PROD == true
               }
            }
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'Pro-portal-prod', usernameVariable: 'USER', passwordVariable: 'PASS'), string(credentialsId: 'PGPASSWORD', variable: "PGPASSWORD")]) {
            
                    def leaderHost
                    
                    // Проверяем первый хост
                    def host1 = sh(script: "sshpass -p '${PASS}' ssh -o StrictHostKeyChecking=no ${USER}@${DOMAIN_PROD}@${PROD_SERVER_POSTGRES} /usr/local/bin/patronictl -c /etc/patroni.yml list | grep Leader | grep -o 193.[0-9]*.[0-9]*.[0-9]*", returnStdout: true).trim()
                    if (host1) {
                        leaderHost = host1
                    }
                    
                    // Если первый хост не является лидером, проверяем второй хост
                    if (!leaderHost) {
                        def host2 = sh(script: "sshpass -p '${PASS}' ssh -o StrictHostKeyChecking=no ${USER}@${DOMAIN_PROD}@${DR_SERVER_POSTGRES} /usr/local/bin/patronictl -c /etc/patroni.yml list | grep Leader | grep -o 193.[0-9]*.[0-9]*.[0-9]*", returnStdout: true).trim()
                        if (host2) {
                            leaderHost = host2
                        }
                    }
                    if (leaderHost) {
                        def commands = [                             
                            "PGPASSWORD=${PGPASSWORD} psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'CREATE SCHEMA IF NOT EXISTS ${schema_name}\'",
                            "PGPASSWORD=${PGPASSWORD} psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'alter schema ${schema_name} owner to dbo_admin\'",
                            "PGPASSWORD=${PGPASSWORD} psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'grant all on schema ${schema_name} to dbo_admin with grant option\'",
                            "PGPASSWORD=${PGPASSWORD} psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'grant usage on schema ${schema_name} to dbo_support, dbo_user\'",
                            "PGPASSWORD=${PGPASSWORD} psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant all on tables to dbo_admin with grant option\'",
                            "PGPASSWORD=${PGPASSWORD} psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant insert, select, update, delete on tables to dbo_support\'",
                            "PGPASSWORD=${PGPASSWORD} psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant select on tables to dbo_user\'",
                            "PGPASSWORD=${PGPASSWORD} psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant all on SEQUENCES to dbo_admin with grant option\'",
                            "PGPASSWORD=${PGPASSWORD} psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant select on SEQUENCES to dbo_user, dbo_support\'",
                            "PGPASSWORD=${PGPASSWORD} psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant all on FUNCTIONS  to dbo_admin with grant option\'",
                            "PGPASSWORD=${PGPASSWORD} psql -h 127.0.0.1 -d proportal -U proportalpguser -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant execute on FUNCTIONS to dbo_user, dbo_support\'",
                            ]  

                            for (def command in commands) {
                                sh "sshpass -p '${PASS}' ssh -T ${USER}@${DOMAIN_PROD}@${leaderHost} \"$command\""
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
                            currentBuild.result = currentBuild.result ?: 'SUCCESS' || currentBuild.result ?: 'FAILURE'  //удаляет workspace при успешном или не успешной сборке
                        }
                    }
                }   
            }
		    stage('Setting to Postgres CERT') {
                agent {
                    label "Proportal_agent-prod"
                }
                when {
                    expression {
                        params.CERT == true
                }
            }
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'Pro-portal-prod', usernameVariable: 'USER', passwordVariable: 'PASS'), string(credentialsId: 'pg_cert', variable: "PGPASSWORD")]) {

                        def commands = [
                              "PGPASSWORD=${PGPASSWORD} psql -h ${CERT_SERVER_POSTGRES} -d proportal -U rbsproportal_cert_s -c \'CREATE SCHEMA IF NOT EXISTS ${schema_name}\'",
                              "PGPASSWORD=${PGPASSWORD} psql -h ${CERT_SERVER_POSTGRES} -d proportal -U rbsproportal_cert_s -c \'alter schema ${schema_name} owner to dbo_admin\'",
                              "PGPASSWORD=${PGPASSWORD} psql -h ${CERT_SERVER_POSTGRES} -d proportal -U rbsproportal_cert_s -c \'grant all on schema ${schema_name} to dbo_admin with grant option\'",
                              "PGPASSWORD=${PGPASSWORD} psql -h ${CERT_SERVER_POSTGRES} -d proportal -U rbsproportal_cert_s -c \'grant usage on schema ${schema_name} to dbo_support, dbo_user\'",
                              "PGPASSWORD=${PGPASSWORD} psql -h ${CERT_SERVER_POSTGRES} -d proportal -U rbsproportal_cert_s -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant all on tables to dbo_admin with grant option\'",
                              "PGPASSWORD=${PGPASSWORD} psql -h ${CERT_SERVER_POSTGRES} -d proportal -U rbsproportal_cert_s -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant insert, select, update, delete on tables to dbo_support\'",
                              "PGPASSWORD=${PGPASSWORD} psql -h ${CERT_SERVER_POSTGRES} -d proportal -U rbsproportal_cert_s -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant select on tables to dbo_user\'",
                              "PGPASSWORD=${PGPASSWORD} psql -h ${CERT_SERVER_POSTGRES} -d proportal -U rbsproportal_cert_s -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant all on SEQUENCES to dbo_admin with grant option\'",
                              "PGPASSWORD=${PGPASSWORD} psql -h ${CERT_SERVER_POSTGRES} -d proportal -U rbsproportal_cert_s -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant select on SEQUENCES to dbo_user, dbo_support\'",
                              "PGPASSWORD=${PGPASSWORD} psql -h ${CERT_SERVER_POSTGRES} -d proportal -U rbsproportal_cert_s -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant all on FUNCTIONS  to dbo_admin with grant option\'",
                              "PGPASSWORD=${PGPASSWORD} psql -h ${CERT_SERVER_POSTGRES} -d proportal -U rbsproportal_cert_s -c \'ALTER DEFAULT PRIVILEGES for role dbo_admin in schema ${schema_name} grant execute on FUNCTIONS to dbo_user, dbo_support\'",
                        ]  

                        for (def command in commands) {
                            sh "sshpass -p '${PASS}' ssh -o StrictHostKeyChecking=no ${USER}@${DOMAIN_PROD}@${CERT_SERVER_POSTGRES} \"$command\""
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
                            currentBuild.result = currentBuild.result ?: 'SUCCESS' || currentBuild.result ?: 'FAILURE'  //удаляет workspace при успешном или не успешной сборке
                        }
                    }
                }     
            }
        }
    }


