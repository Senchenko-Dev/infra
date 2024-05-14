def startYamlLint(yamlFile, fileDry) {
    sh "echo start def lint file ${fileDry}"
    sh "curl --header \"PRIVATE-TOKEN: ${GITLAB_PRIVATE_TOKEN}\" \"${GITLAB_API_URL}projects/5278/repository/files/jenkins%2Fvalidate%2F.yamllint/raw?ref=master\"  -o .yamllint"
    sh "cat <<< '${yamlFile}' | grep -vE '.*%.*%' | yamllint -c .yamllint -"
}

def gitCheck(repoUrl,folderName) {
    withCredentials([usernamePassword(credentialsId: 'rbsproportal-gitlab', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
        def gitUrl = "https://'${USERNAME}:${PASSWORD}'@${repoUrl}/${folderName}"
        def response = sh(script: "git ls-remote --exit-code --heads ${gitUrl} -o /dev/null", returnStatus: true)
        if (response == 2){
            echo "Папка ${folderName} существует в репозитории"
        } else {
            error("Репозиторий не доступен ${response}")
        }
    }   
}

def findSymbols(yamlLines, fileDry) {
    //поиск не допустимых символов в yaml файле
    for (int i = 0; i < yamlLines.size(); i++) {
        def line = yamlLines[i]
        for (int j = 0; j < line.size(); j++) {
            def ch = line[j]
            if (ch in ['!', '^']) {
                error("найден сивол '${ch}' в файле ${fileDry} в строке ${i + 1} столбец номер ${j + 1}")
            }
        }
    }
}

def findSpaceAfterVariable(yamlLines, fileDry) {
    //поиск лишнего пробела после переменной в yaml файле
    for (int i = 0; i < yamlLines.size(); i++){
        if (yamlLines[i] =~ /(\w+):\s{2}/) {
            error("Найден лишний пробел после переменной в файле ${fileDry} в строке ${i + 1}: ${yamlLines[i]}")
        }
    }

}

return this
