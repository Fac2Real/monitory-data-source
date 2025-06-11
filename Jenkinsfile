pipeline {
  agent any

  environment {
    /* AWS & ECR */
    AWS_DEFAULT_REGION = 'ap-northeast-2'
    ECR_REGISTRY       = '853660505909.dkr.ecr.ap-northeast-2.amazonaws.com'
    IMAGE_REPO_NAME    = 'flink'
    IMAGE_TAG          = "${env.GIT_COMMIT}"
    LATEST_TAG         = 'flink-latest'
    PROD_TAG           = 'flink-prod-latest'

    /* GitHub Checks */
    GH_CHECK_NAME      = 'Flink Build Test'

    /* Slack */
    SLACK_CHANNEL      = '#ci-cd'
    SLACK_CRED_ID      = 'slack-factoreal-token'   // Slack App OAuth Token

    /* Argo CD */
    ARGOCD_SERVER           = 'argocd.monitory.space'   // Argo CD server endpoint
    ARGOCD_APPLICATION_NAME = 'flink'
  }

  stages {
    /* 0) 환경 변수 설정 */
    stage('Environment, Config Setup') {
      steps {
        withCredentials([
          file (credentialsId: 'flink-root-pem',   variable: 'ROOT_PEM'),
          file (credentialsId: 'flink-priv-key',   variable: 'PRIV_KEY'),
          file (credentialsId: 'flink-cert-pem',   variable: 'CERT_PEM')
        ]) {
          sh '''
            sudo mkdir -p src/main/resources/certs
            sudo chmod 755 src/main/resources/certs
            sudo cp "$ROOT_PEM"  src/main/resources/certs/root.pem
            sudo chmod 644 src/main/resources/certs/root.pem
            sudo cp "$PRIV_KEY"  src/main/resources/certs/private.pem.key
            sudo chmod 644 src/main/resources/certs/private.pem.key
            sudo cp "$CERT_PEM"  src/main/resources/certs/certificate.pem.crt
            sudo chmod 644 src/main/resources/certs/certificate.pem.crt
          '''
        }

        script {
          def rawUrl = sh(script: "git config --get remote.origin.url",
                        returnStdout: true).trim()
          env.REPO_URL = rawUrl.replaceAll(/\.git$/, '')
          env.COMMIT_MSG = sh(script: "git log -1 --pretty=format:'%s'",returnStdout: true).trim()
        }
      }
    }

    /* 1) 공통 테스트 */
    stage('Test') {
      when {
        allOf {
          not { branch 'develop' }
          not { branch 'main' }
          not { changeRequest() }
        }
      }
      steps {
        withCredentials([
          file (credentialsId: 'flink-properties-ec2', variable: 'APP_PROPS')
        ]) {
          sh '''
            sudo cp "$APP_PROPS" src/main/resources/application.properties
            sudo chmod 644 src/main/resources/application.properties
          '''
        }
        publishChecks name: GH_CHECK_NAME,
                      status: 'IN_PROGRESS',
                      detailsURL: env.BUILD_URL
        withCredentials([ file(credentialsId: 'backend-env', variable: 'ENV_FILE') ]) {
        sh '''
set -o allexport
source "$ENV_FILE"
set +o allexport

./gradlew test --no-daemon
'''
        }
      }
      post {
        success {
          publishChecks name: GH_CHECK_NAME,
                        conclusion: 'SUCCESS',
                        detailsURL: env.BUILD_URL
        }
        failure {
          publishChecks name: GH_CHECK_NAME,
                        conclusion: 'FAILURE',
                        detailsURL: "${env.BUILD_URL}console"
          slackSend channel: env.SLACK_CHANNEL,
                              tokenCredentialId: env.SLACK_CRED_ID,
                              color: '#ff0000',
                              message: """:x: *Flink Test 실패*
          파이프라인: <${env.BUILD_URL}|열기>
          커밋: `${env.GIT_COMMIT}` – `${env.COMMIT_MSG}`
          (<${env.REPO_URL}/commit/${env.GIT_COMMIT}|커밋 보기>)
          """
        }
      }
    }



    /* 2) develop 전용 ─ Docker 이미지 빌드 & ECR Push & Deploy (EC2) */
    stage('Docker Build & Push (develop only)') {
      when {
        allOf {
          branch 'develop'
          not { changeRequest() }
        }
      }
      steps {
        withCredentials([
          file (credentialsId: 'flink-properties-ec2', variable: 'APP_PROPS')
        ]) {
          sh '''
            sudo cp "$APP_PROPS" src/main/resources/application.properties
            sudo chmod 644 src/main/resources/application.properties
          '''
        }

        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                          credentialsId: 'jenkins-access']]) {
          sh """
aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}

./gradlew build --no-daemon -x test

docker build -t ${ECR_REGISTRY}/${IMAGE_REPO_NAME}:${LATEST_TAG} .

docker push ${ECR_REGISTRY}/${IMAGE_REPO_NAME}:${LATEST_TAG}
"""
        }
        withCredentials([string(credentialsId: 'argo-jenkins-token', variable: 'ARGOCD_AUTH_TOKEN')]) {
          sh '''
argocd --server $ARGOCD_SERVER --insecure --grpc-web \
        app sync $ARGOCD_APPLICATION_NAME

argocd --server $ARGOCD_SERVER --insecure --grpc-web \
        app wait $ARGOCD_APPLICATION_NAME --health --timeout 300
'''
        }
      }
      /* Slack 알림 */
      post {
        success {
          slackSend channel: env.SLACK_CHANNEL,
                    tokenCredentialId: env.SLACK_CRED_ID,
                    color: '#36a64f',
                    message: """:white_check_mark: *Flink develop branch CI/CD 성공*
파이프라인: <${env.BUILD_URL}|열기>
커밋: `${env.GIT_COMMIT}` – `${env.COMMIT_MSG}`
(<${env.REPO_URL}/commit/${env.GIT_COMMIT}|커밋 보기>)
"""
        }
        failure {
          slackSend channel: env.SLACK_CHANNEL,
                    tokenCredentialId: env.SLACK_CRED_ID,
                    color: '#ff0000',
                    message: """:x: *Flink develop branch CI/CD 실패*
파이프라인: <${env.BUILD_URL}|열기>
커밋: `${env.GIT_COMMIT}` – `${env.COMMIT_MSG}`
(<${env.REPO_URL}/commit/${env.GIT_COMMIT}|커밋 보기>)
"""
        }
      }
    }


    /* 3) main 전용 ─ Docker 이미지 빌드 & ECR Push */
    stage('Docker Build & Push (main only)') {
      when {
        allOf {
          branch 'main'
          not { changeRequest() }
        }
      }
      steps {
        withCredentials([
          file (credentialsId: 'flink-properties-k8s', variable: 'APP_PROPS')
        ]) {
          sh '''
            sudo cp "$APP_PROPS" src/main/resources/application.properties
            sudo chmod 644 src/main/resources/application.properties
          '''
        }

        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                          credentialsId: 'jenkins-access']]) {
          sh """
aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}

./gradlew build --no-daemon -x test

docker build -t ${ECR_REGISTRY}/${IMAGE_REPO_NAME}:${PROD_TAG} .

docker push ${ECR_REGISTRY}/${IMAGE_REPO_NAME}:${PROD_TAG}
"""
        }
      }
      /* Slack 알림 */
      post {
        success {
          slackSend channel: env.SLACK_CHANNEL,
                    tokenCredentialId: env.SLACK_CRED_ID,
                    color: '#36a64f',
                    message: """:white_check_mark: *Flink main branch CI 성공*
파이프라인: <${env.BUILD_URL}|열기>
커밋: `${env.GIT_COMMIT}` – `${env.COMMIT_MSG}`
(<${env.REPO_URL}/commit/${env.GIT_COMMIT}|커밋 보기>)
"""
        }
        failure {
          slackSend channel: env.SLACK_CHANNEL,
                    tokenCredentialId: env.SLACK_CRED_ID,
                    color: '#ff0000',
                    message: """:x: *Flink main branch CI 실패*
파이프라인: <${env.BUILD_URL}|열기>
커밋: `${env.GIT_COMMIT}` – `${env.COMMIT_MSG}`
(<${env.REPO_URL}/commit/${env.GIT_COMMIT}|커밋 보기>)
"""
        }
      }
    }
  }
}
