# Monitory Data Source

IoT, 환경, 설비, 웨어러블 등 다양한 센서 데이터를 AWS Kinesis, MQTT 등에서 수집하여 Flink로 실시간 처리하고, Kafka 및 S3 등으로 저장/전송하는 데이터 스트림 파이프라인입니다.

## 🏗️ 프로젝트 구조

```
.
├── src/main/java/com/monitory/data/
│   ├── FlinkSourceApplication.java      # 메인 실행 파일
│   ├── config/                         # Kafka, MQTT 설정 로더
│   ├── sinks/                          # S3, Kafka 등 Sink 관련 클래스
│   ├── sources/                        # Kinesis, MQTT 등 Source 관련 클래스
│   ├── transformations/                # 데이터 변환/가공 로직
│   └── utils/                          # 유틸리티 클래스
├── src/main/resources/
│   ├── application.properties          # 환경별 설정 파일 (gitignore)
│   ├── certs/                          # 인증서 폴더 (gitignore)
│   ├── log4j.properties                # 로그 설정
│   └── flink-conf.yaml                 # Flink 설정
├── build.gradle                        # Gradle 빌드 스크립트
├── Dockerfile                          # Docker 이미지 빌드 파일
├── Jenkinsfile                         # Jenkins CI/CD 파이프라인
└── ...
```

## 🚀 주요 기능

- **AWS Kinesis, MQTT** 등 다양한 소스에서 실시간 데이터 수집
- **Flink** 기반 실시간 스트림 처리 및 이상치/라벨링 변환
- **Kafka** 토픽별 데이터 분류 및 전송
- **S3**로 시간/장비별 집계 데이터 저장
- **Prometheus** 기반 모니터링 지원

## ⚙️ 빌드 및 실행

### 1. 환경 준비

- Java 11 이상 (Amazon Corretto 11 권장)
- Gradle 8.x
- AWS 자격증명 및 S3, Kinesis, Kafka, MQTT 브로커 접근 권한 필요

### 2. 빌드

```sh
./gradlew build
```

- 실행 파일은 `build/libs/monitory-data-1.0-SNAPSHOT.jar`로 생성됩니다.

### 3. 실행

```sh
java -jar build/libs/monitory-data-1.0-SNAPSHOT.jar
```

또는 Docker로 실행:

```sh
docker build -t monitory-data .
docker run --env-file .env -p 8081:8081 monitory-data
```

### 4. 환경설정

- `src/main/resources/application.properties`에 Kafka, MQTT, AWS 등 연결 정보를 설정해야 합니다.
- 인증서 파일은 `src/main/resources/certs/`에 위치해야 하며, git에는 포함되지 않습니다.

## 📝 주요 클래스

- [`FlinkSourceApplication`](src/main/java/com/monitory/data/FlinkSourceApplication.java): Flink 스트림 파이프라인의 메인 엔트리포인트
- [`KinesisSourceUtil`](src/main/java/com/monitory/data/utils/KinesisSourceUtil.java): Kinesis Consumer 생성
- [`KafkaUtil`](src/main/java/com/monitory/data/utils/KafkaUtil.java): Kafka Sink 생성 및 토픽 분기
- [`S3SinkUtil`](src/main/java/com/monitory/data/utils/S3SinkUtil.java): S3 Sink 생성
- [`TimeStampAssigner`](src/main/java/com/monitory/data/transformations/TimeStampAssigner.java): UTC 타임스탬프 → KST 변환
- [`EnvironmentDangerLevelAssigner`](src/main/java/com/monitory/data/transformations/EnvironmentDangerLevelAssigner.java): 환경 센서 위험도 라벨링
- [`FaultyAssigner`](src/main/java/com/monitory/data/transformations/FaultyAssigner.java): 설비 센서 이상치 탐지
- [`WearableDangerLevelAssigner`](src/main/java/com/monitory/data/transformations/WearableDangerLevelAssigner.java): 웨어러블 센서 위험도 라벨링

## 🛠️ 개발/운영 참고

- CI/CD: Jenkins, GitHub Actions, ArgoCD, Slack 연동
- 로그: log4j 설정(`log4j.properties`)
- Flink 설정: `flink-conf.yaml` 참고

## 📄 라이선스

본 프로젝트는 Apache 2.0 라이선스를 따릅니다.

---

문의 및 기여는 [Issues](https://github.com/Fac2Real/monitory-data-source/issues) 또는 PR로
