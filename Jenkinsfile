pipeline {
  agent any
  tools { maven 'Maven_3' }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Test') {
      steps {
        bat 'mvn -v'
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
