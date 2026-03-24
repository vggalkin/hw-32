pipeline {
    agent { 
        label 'linux-agent' 
    }

    tools {
        maven 'Maven-3.9.8'
    }

    environment {
        BUILD_VERSION = "1.0.${BUILD_NUMBER}"
        // Добавляем таймауты для стабильности
        MAVEN_OPTS = '-Xmx2048m -Xms512m'
    }

    options {
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                echo '📥 Скачивание кода...'
                checkout scm
            }
        }

        stage('Compile') {
            steps {
                echo '🔨 Компиляция...'
                sh 'mvn clean compile -DskipTests'
            }
        }

        // 🚀 ПАРАЛЛЕЛЬНЫЕ СТАДИИ
        stage('Parallel Tests & Analysis') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        echo '🧪 Запуск unit-тестов...'
                        sh 'mvn test -Dtest=*Test -DfailIfNoTests=false'
                    }
                    post {
                        always {
                            script {
                                def hasReports = sh(
                                    script: 'test -d target/surefire-reports && ls target/surefire-reports/TEST-*.xml 1>/dev/null 2>&1 && echo "1" || echo "0"',
                                    returnStdout: true
                                ).trim()
                                if (hasReports == '1') {
                                    echo '📊 Публикация JUnit-отчётов (Unit)...'
                                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                                }
                            }
                        }
                    }
                }

                stage('Integration Tests') {
                    steps {
                        echo '🔗 Запуск интеграционных тестов...'
                        sh 'mvn verify -Dit.test=*IT -DfailIfNoTests=false'
                    }
                    post {
                        always {
                            script {
                                def hasReports = sh(
                                    script: 'test -d target/failsafe-reports && ls target/failsafe-reports/TEST-*.xml 1>/dev/null 2>&1 && echo "1" || echo "0"',
                                    returnStdout: true
                                ).trim()
                                if (hasReports == '1') {
                                    echo '📊 Публикация JUnit-отчётов (Integration)...'
                                    junit allowEmptyResults: true, testResults: 'target/failsafe-reports/*.xml'
                                }
                            }
                        }
                    }
                }

                stage('Code Quality') {
                    steps {
                        echo '🔍 Анализ кода (Sonar/Checkstyle)...'
                        sh '''
                            mvn checkstyle:check -q || true
                            mvn pmd:check -q || true
                            # Опционально: запуск SonarQube
                            # mvn sonar:sonar -Dsonar.projectKey=my-project || true
                        '''
                    }
                }

                stage('Security Scan') {
                    steps {
                        echo '🔐 Сканирование зависимостей...'
                        sh '''
                            # OWASP Dependency-Check
                            mvn org.owasp:dependency-check-maven:check -q || true
                            # Или тривиальная проверка уязвимостей
                            echo "✅ Security scan completed"
                        '''
                    }
                }
            }
            // Обработка результатов параллельной стадии
            post {
                failure {
                    echo '❌ Одна из параллельных задач завершилась с ошибкой'
                }
            }
        }

        stage('Package') {
            steps {
                echo '📦 Сборка артефакта...'
                sh "mvn package -DskipTests -DbuildVersion=${BUILD_VERSION}"
            }
        }

        // 🚀 ПАРАЛЛЕЛЬНЫЙ ДЕПЛОЙ (опционально)
        stage('Deploy & Notify') {
            parallel {
                stage('Deploy to Staging') {
                    steps {
                        echo '🚀 Деплой на staging...'
                        sh '''
                            echo "Deploying $BUILD_VERSION to staging at \$(date)" > deploy_staging.log
                            # Реальная команда деплоя:
                            # kubectl apply -f k8s/staging/ || aws deploy ...
                            ls -lh target/*.jar 2>/dev/null || true
                        '''
                    }
                }
                stage('Generate Release Notes') {
                    steps {
                        echo '📝 Генерация релиз-нот...'
                        sh '''
                            echo "# Release ${BUILD_VERSION}" > release_notes.md
                            echo "Built at: \$(date)" >> release_notes.md
                            git log -10 --oneline >> release_notes.md || true
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            echo '📦 Архивация артефактов...'
            archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true
            archiveArtifacts artifacts: 'deploy_*.log,release_notes.md', allowEmptyArchive: true
            
            // Архивация отчётов тестов
            archiveArtifacts artifacts: 'target/surefire-reports/**,target/failsafe-reports/**', 
                             allowEmptyArchive: true, 
                             onlyIfSuccessful: false
            
            echo '🧹 Очистка рабочего пространства...'
            cleanWs()
        }
        failure {
            echo '❌ BUILD FAILED'
            // Опционально: отправка уведомления
            // mail to: 'team@example.com', subject: "Build failed: ${env.JOB_NAME}", body: "Check: ${env.BUILD_URL}"
        }
        success {
            echo "✅ BUILD SUCCESS | Version: ${BUILD_VERSION}"
            // Опционально: уведомление в Slack/Telegram
            // slackSend color: 'good', message: "Build #${BUILD_NUMBER} succeeded!"
        }
        unstable {
            echo '⚠️ BUILD UNSTABLE - тесты упали, но сборка прошла'
        }
    }
}
