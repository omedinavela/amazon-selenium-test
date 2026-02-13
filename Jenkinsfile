pipeline {
  agent any

  environment {
    JAVA_HOME = 'C:\\Users\\OMAR\\scoop\\apps\\temurin17-jdk\\current'
    PATH = "${JAVA_HOME}\\bin;${env.PATH}"
  }

  tools {
    maven 'Maven_3'   // el nombre exacto que pusiste en Tools
  }

  stages {
    stage('Env') {
      steps {
        bat 'where java'
        bat 'java -version'
        bat 'where javac'
        bat 'javac -version'
        bat 'mvn -v'
      }
    }

    stage('Test') {
      steps {
        bat 'mvn -Dheadless=true -Dsurefire.suiteXmlFiles=testng.xml test'
      }
    }
  }

  post {
    always {
      junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
      archiveArtifacts artifacts: 'target/surefire-reports/**', allowEmptyArchive: true
    }
  }
}
