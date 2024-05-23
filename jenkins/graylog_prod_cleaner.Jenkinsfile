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
    PROD_SERVER_GRAYLOG_ELASTIC = "193.48.8.111"
    PROD_SERVER_ELASTIC = "193.48.8.113"
    GROUP_ID = "ProPortal"
    DOLLAR_SIGN_5 = "\$5"
    DOLLAR_SIGN_3 = "\$3"
    QUOTES = '""'
    EMAIL_FROM = "DigitalPro@rosbank.ru"
    EMAIL_SUBJECT = "Удаление индексов Graylog"
    EMAIL_TO = "Albert.Abdullin@rosbank.ru;Timur.Bayan@rosbank.ru;Nikita.Senchenko@rosbank.ru;Sergey.Pechenyuk@rosbank.ru"
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
          credentialsId: "rbsmosbiqwebcalc-ssh",
          usernameVariable: 'PROD_NGINX_DEPLOY_USER',
          keyFileVariable: 'PROD_NGINX_KEY_USER'
        )])
        {
          script {
          USE_PERCENT_PROD_SERVER_ELASTIC = sh(script: "ssh -o StrictHostKeyChecking=no -i ${PROD_NGINX_KEY_USER} \"${PROD_NGINX_DEPLOY_USER}\"@${PROD_SERVER_ELASTIC}  \"df -h /dev/mapper/vg_root-lv_var | awk 'NR == 2{print \$DOLLAR_SIGN_5+0}'\"", returnStdout:true)
          echo "USE_PERCENT_PROD_SERVER_ELASTIC(): ${USE_PERCENT_PROD_SERVER_ELASTIC}"
          USE_PERCENT_PROD_SERVER_GRAYLOG_ELASTIC = sh(script: "ssh -o StrictHostKeyChecking=no -i ${PROD_NGINX_KEY_USER} \"${PROD_NGINX_DEPLOY_USER}\"@${PROD_SERVER_GRAYLOG_ELASTIC}  \"df -h /dev/mapper/vg_root-lv_var | awk 'NR == 2{print \$DOLLAR_SIGN_5+0}'\"", returnStdout:true)
          echo "USE_PERCENT_PROD_SERVER_GRAYLOG_ELASTIC(193.48.8.111): ${USE_PERCENT_PROD_SERVER_GRAYLOG_ELASTIC}"
          }
        }
      }
  }

  stage('clear graylog index') 
  {
    when
    {
      expression {USE_PERCENT_PROD_SERVER_ELASTIC.toInteger() > 90 || USE_PERCENT_PROD_SERVER_GRAYLOG_ELASTIC.toInteger() > 90}
    }
    steps 
    {
      withCredentials([
      sshUserPrivateKey
      (
        credentialsId: "rbsmosbiqwebcalc-ssh",
        usernameVariable: 'PROD_NGINX_DEPLOY_USER',
        keyFileVariable: 'PROD_NGINX_KEY_USER'
      ),
      usernamePassword
      (
        credentialsId: "bot-viktor-token",
        passwordVariable: 'TG_BOT_TOKEN',
        usernameVariable: 'TG_BOT_TOKEN_USER'
      ),
      usernamePassword
      (
        credentialsId: "group-${GROUP_ID.toLowerCase()}-id",
        passwordVariable: 'TG_GROUP_ID',
        usernameVariable: 'TG_GROUP_ID_USER'
      )])
      {
        script {
        OLDEST_INDEX = sh(script: "ssh -o StrictHostKeyChecking=no -i ${PROD_NGINX_KEY_USER} \"${PROD_NGINX_DEPLOY_USER}\"@${PROD_SERVER_GRAYLOG_ELASTIC} \"curl -s http://193.48.8.111:9200/_cat/indices | grep graylog | awk '{if (min == \$QUOTES) min=\$DOLLAR_SIGN_3 ; else if (\$DOLLAR_SIGN_3 < min) min=\$DOLLAR_SIGN_3}END{print min}'\"", returnStdout:true)
        echo "${OLDEST_INDEX}"
        OLDEST_INDEX = OLDEST_INDEX.replace('\n','').replace(' ','').trim()
        CLEAR_INDEX = sh(script: "ssh -o StrictHostKeyChecking=no -i ${PROD_NGINX_KEY_USER} \"${PROD_NGINX_DEPLOY_USER}\"@${PROD_SERVER_GRAYLOG_ELASTIC} \"curl -s -XDELETE '193.48.8.111:9200/${OLDEST_INDEX}'\"", returnStdout:true)
        USE_PERCENT_POST_CLEAR_PROD_SERVER_ELASTIC = sh(script: "ssh -o StrictHostKeyChecking=no -i ${PROD_NGINX_KEY_USER} \"${PROD_NGINX_DEPLOY_USER}\"@${PROD_SERVER_ELASTIC}  \"df -h df -h /dev/mapper/vg_root-lv_var | awk 'NR == 2{print \$DOLLAR_SIGN_5+0}'\"", returnStdout:true)
        echo "USE_PERCENT_POST_CLEAR_PROD_SERVER_ELASTIC(193.48.8.113): ${USE_PERCENT_POST_CLEAR_PROD_SERVER_ELASTIC}"
        USE_PERCENT_POST_CLEAR_PROD_SERVER_GRAYLOG_ELASTIC = sh(script: "ssh -o StrictHostKeyChecking=no -i ${PROD_NGINX_KEY_USER} \"${PROD_NGINX_DEPLOY_USER}\"@${PROD_SERVER_GRAYLOG_ELASTIC}  \"df -h df -h /dev/mapper/vg_root-lv_var | awk 'NR == 2{print \$DOLLAR_SIGN_5+0}'\"", returnStdout:true)
        echo "USE_PERCENT_POST_CLEAR_PROD_SERVER_GRAYLOG_ELASTIC(193.48.8.111): ${USE_PERCENT_POST_CLEAR_PROD_SERVER_GRAYLOG_ELASTIC}"    
        env.EMAIL_BODY = """
          Добрый день!<br><br>
          На PROD GRAYLOG был удален индекс ${OLDEST_INDEX} в связи с переполнением места(193.48.8.111: ${USE_PERCENT_PROD_SERVER_GRAYLOG_ELASTIC}, 193.48.8.111: ${USE_PERCENT_PROD_SERVER_ELASTIC}). На данный момент занято на серверах 193.48.8.111: ${USE_PERCENT_POST_CLEAR_PROD_SERVER_GRAYLOG_ELASTIC}%, 193.48.8.113: ${USE_PERCENT_POST_CLEAR_PROD_SERVER_ELASTIC}%. Сборка №${BUILD_NUMBER}.
        """
        emailext body: env.EMAIL_BODY, from: EMAIL_FROM, recipientProviders: [[$class: 'DevelopersRecipientProvider']], subject:  EMAIL_SUBJECT, to: EMAIL_TO, mimeType: 'text/html'
        sh """
          curl -k -H "Content-type: application/json" -X POST -d '{"token":\"'${TG_BOT_TOKEN}'\","id":\"'${TG_GROUP_ID}'\","text":\"На PROD GRAYLOG был удален индекс '${OLDEST_INDEX}' в связи с переполнением места(193.48.8.111: '${USE_PERCENT_PROD_SERVER_GRAYLOG_ELASTIC.replace('\n','').replace(' ','').trim()}', 193.48.8.113: '${USE_PERCENT_PROD_SERVER_ELASTIC.replace('\n','').replace(' ','').trim()}'). На данный момент занято на серверах 193.48.8.111: '${USE_PERCENT_POST_CLEAR_PROD_SERVER_GRAYLOG_ELASTIC.replace('\n','').replace(' ','').trim()}'%, 193.48.8.113: '${USE_PERCENT_POST_CLEAR_PROD_SERVER_ELASTIC.replace('\n','').replace(' ','').trim()}'%. Сборка №'${BUILD_NUMBER}'.\"}' https://wso2ei.rsb.dmz.rus.socgen:443/telegram/v1.0/sendmessage
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

