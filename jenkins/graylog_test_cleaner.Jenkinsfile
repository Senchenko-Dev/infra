pipeline 
{
  agent 
  {
    node 
    {
      label 'PROPORTAL_linux_1'
    }
  }
  
  options 
  {
    buildDiscarder(logRotator(artifactDaysToKeepStr: '14', artifactNumToKeepStr: '14', daysToKeepStr: '14', numToKeepStr: '20'))
    timestamps()
    skipDefaultCheckout()
    disableConcurrentBuilds()
  }

  environment
  {
    TEST_SERVER_GRAYLOG = "193.48.98.87"
    GROUP_ID = "cicd-pportal"
    DOLLAR_SIGN_5 = "\$5"
    DOLLAR_SIGN_3 = "\$3"
    QUOTES = '""'
    EMAIL_FROM = "DigitalPro@rosbank.ru"
    EMAIL_SUBJECT = "Удаление индексов Graylog"
    EMAIL_TO = "Timur.Bayan@rosbank.ru;Nikita.Senchenko@rosbank.ru;Sergey.Pechenyuk@rosbank.ru;Nikitka.Kozhin@rosbank.ru;Artem.Zenov@rosbank.ru;Viktor.Zykov@rosbank.ru"
    WORKSPACE=pwd()
  }
  stages
  {
    stage('use percent value') 
    {
      steps 
      {
        withCredentials([
        sshUserPrivateKey
        (
          credentialsId: "trbs-${GROUP_ID.toLowerCase()}-ssh",
          usernameVariable: 'TEST_GRAYLOG_DEPLOY_USER',
          keyFileVariable: 'TEST_GRAYLOG_KEY_USER'
        )])
        {
          script {
          USE_PERCENT = sh(script: "ssh -o StrictHostKeyChecking=no -i ${TEST_GRAYLOG_KEY_USER} \"${TEST_GRAYLOG_DEPLOY_USER}\"@${TEST_SERVER_GRAYLOG}  \"df -h /dev/mapper/vg_root-lv_var | awk 'NR == 2{print \$DOLLAR_SIGN_5+0}'\"", returnStdout:true)
          echo "${USE_PERCENT}"
          }
        }
      }
  }

  stage('clear graylog index') 
  {
    when
    {
      expression {USE_PERCENT.toInteger() > 90}
    }
    steps 
    {
      withCredentials([
      sshUserPrivateKey
      (
        credentialsId: "trbs-${GROUP_ID.toLowerCase()}-ssh",
        usernameVariable: 'TEST_GRAYLOG_DEPLOY_USER',
        keyFileVariable: 'TEST_GRAYLOG_KEY_USER'
      )
      ])
      {
        script {
        OLDEST_INDEX = sh(script: "ssh -o StrictHostKeyChecking=no -i ${TEST_GRAYLOG_KEY_USER} \"${TEST_GRAYLOG_DEPLOY_USER}\"@${TEST_SERVER_GRAYLOG} \"curl -s http://localhost:9200/_cat/indices | grep graylog | awk '{if (min == \$QUOTES) min=\$DOLLAR_SIGN_3 ; else if (\$DOLLAR_SIGN_3 < min) min=\$DOLLAR_SIGN_3}END{print min}'\"", returnStdout:true)
        echo "${OLDEST_INDEX}"
        OLDEST_INDEX = OLDEST_INDEX.replace('\n','').replace(' ','').trim()
        CLEAR_INDEX = sh(script: "ssh -o StrictHostKeyChecking=no -i ${TEST_GRAYLOG_KEY_USER} \"${TEST_GRAYLOG_DEPLOY_USER}\"@${TEST_SERVER_GRAYLOG} \"curl -s -XDELETE 'localhost:9200/${OLDEST_INDEX}'\"", returnStdout:true)
        USE_PERCENT_POST_CLEAR = sh(script: "ssh -o StrictHostKeyChecking=no -i ${TEST_GRAYLOG_KEY_USER} \"${TEST_GRAYLOG_DEPLOY_USER}\"@${TEST_SERVER_GRAYLOG}  \"df -h /dev/mapper/vg_root-lv_var | awk 'NR == 2{print \$DOLLAR_SIGN_5+0}'\"", returnStdout:true)
        CLEAR_DATE = sh (returnStdout: true, script: 'echo -n $(+%d.%m.20%y" "%H:%M)')
        env.EMAIL_BODY = """
          Добрый день!<br><br>
          В '${CLEAR_DATE}' на TEST GRAYLOG был удален индекс ${OLDEST_INDEX} в связи с переполнением места(${USE_PERCENT}%). На данный момент занято ${USE_PERCENT_POST_CLEAR}%. Сборка №${BUILD_NUMBER}.
        """
        }
      }
    }
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
            sh "rm -r ${WORKSPACE}*"
          }
        }
      }
    }
  }
}

