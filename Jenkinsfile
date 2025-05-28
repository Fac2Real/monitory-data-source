pipeline {
  agent any

  environment {
    /* AWS & ECR */
    AWS_DEFAULT_REGION = 'ap-northeast-2'
    ECR_REGISTRY       = '853660505909.dkr.ecr.ap-northeast-2.amazonaws.com'
    IMAGE_REPO_NAME    = 'flink'
    IMAGE_TAG          = "${env.GIT_COMMIT}"
    LATEST_TAG         = 'flink-latest'

    /* GitHub Checks */
    GH_CHECK_NAME      = 'Flink Build Test'

    /* Slack */
    SLACK_CHANNEL      = '#ci-cd'
    SLACK_CRED_ID      = 'slack-factoreal-token'   // Slack App OAuth Token
  }

  stages {
    /* 0) 환경 변수 설정 */
    stage('Environment, Config Setup') {
      steps {
        withCredentials([
          file (credentialsId: 'flink-properties', variable: 'APP_PROPS'),
          file (credentialsId: 'flink-root-pem',   variable: 'ROOT_PEM'),
          file (credentialsId: 'flink-priv-key',   variable: 'PRIV_KEY'),
          file (credentialsId: 'flink-cert-pem',   variable: 'CERT_PEM')
        ]) {
          sh '''
            cp "$APP_PROPS" src/main/resources/application.properties

            mkdir -p src/main/resources/certs
            cp "$ROOT_PEM"  src/main/resources/certs/root.pem
            cp "$PRIV_KEY"  src/main/resources/certs/private.pem.key
            cp "$CERT_PEM"  src/main/resources/certs/certificate.pem.crt
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
        publishChecks name: GH_CHECK_NAME,
                      status: 'IN_PROGRESS',
                      detailsURL: env.BUILD_URL
        sh '''
./gradlew test --parallel
'''
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
                              message: """<!here> :x: *Flink Test 실패*
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
          anyOf {
            branch 'develop'
            branch 'main'
          }
          not { changeRequest() }
        }
      }
      steps {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                          credentialsId: 'jenkins-access']]) {
          sh """
aws ecr get-login-password --region ${AWS_DEFAULT_REGION} \
  | docker login --username AWS --password-stdin ${ECR_REGISTRY}

docker build -t ${ECR_REGISTRY}/${IMAGE_REPO_NAME}:${LATEST_TAG} .

docker push ${ECR_REGISTRY}/${IMAGE_REPO_NAME}:${LATEST_TAG}
          """
        }
        sshagent(credentials: ['monitory-temp']) {
          sh """
ssh -o StrictHostKeyChecking=no ec2-user@43.200.39.139 <<'EOF'
set -e
cd datastream
aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}
docker-compose -f docker-compose-source.yml down -v
docker-compose -f docker-compose-source.yml up -d --pull always
EOF
"""
        }
      }
      /* Slack 알림 */
      post {
        success {
          slackSend channel: env.SLACK_CHANNEL,
                    tokenCredentialId: env.SLACK_CRED_ID,
                    color: '#36a64f',
                    message: """<!here> :white_check_mark: *Flink CI/CD 성공*
파이프라인: <${env.BUILD_URL}|열기>
커밋: `${env.GIT_COMMIT}` – `${env.COMMIT_MSG}`
(<${env.REPO_URL}/commit/${env.GIT_COMMIT}|커밋 보기>)
"""
        }
        failure {
          slackSend channel: env.SLACK_CHANNEL,
                    tokenCredentialId: env.SLACK_CRED_ID,
                    color: '#ff0000',
                    message: """<!here> :x: *Flink CI/CD 실패*
파이프라인: <${env.BUILD_URL}|열기>
커밋: `${env.GIT_COMMIT}` – `${env.COMMIT_MSG}`
(<${env.REPO_URL}/commit/${env.GIT_COMMIT}|커밋 보기>)
"""
        }
      }
    }
  }
}
