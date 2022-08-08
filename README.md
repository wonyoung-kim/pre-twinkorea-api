# 트윈코리아 API 서버 개요
### 구성
- Kotlin 1.6으로 작성
- 자바 8로 컴파일 
- 스프링부트 2.5.5
- MySQL (AWS RDS)
- Redis (AWS ElastiCache)
- 그외 자세한 외부 패키지 구성은 `build.gradle.kts` 참조

### 제공 API
- twin-korea 어드민 관련 API 제공
- twin-korea 사전청약(+ 대기청약) 관련 API 제공
- twin-korea 사전청약(+ 대기청약)의 분양 관련 API 제공
- twin-korea 분양 관련 API 제공
- 기타 식신 서비스와 통신하는 API / 셀과 땅 소유 관련 API 등 있지만 개발 단계에서 해당 기획 삭제되어 실제로 사용 되지는 않음 (추후 재사용 가능성 있음)
- 인증 관련 API 
  - 일반 회원: 카카오 로그인
  - 어드민 회원: 이메일 로그인 및 구글 2FA

### 배포
- 컴파일 후 컴파일 된 jar 파일 실행하는 도커이미지 생성
- 도커 이미지와 jar 파일과 ElasticBeanstalk 용 Dockerfile 설정파일인 `Dockerrun.aws.json` 압축
- 생성된 압축파일(ex. `twinkorea-api-202202251810.zip`)을 AWS ElasticBeanstalk 에 업로드하여 배포
    ```shell
    # 도커 이미지/컴파일된 jar 파일을 포함하는 압축파일 생성 
    sh build.sh
    ```
  
# 상세설명
### 인수인계 문서 참조

# 유의사항
### 인수인계 문서 참조

# 기타
- 작성자: 김동현 (ddhyun93@gmail.com)