목적

과제 제약(JDK 24, Kotlin 2.2, 단일 모듈, 외부 라이브러리 최소, 실행 JAR)을 지키며

편의점 재고/판매 데이터 기반 리포트·발주·할인·운영현황 기능을 단계적으로 완성하기 위해 사용한 핵심 프롬프트를 기록한다.
1) 요구사항·환경 고정

원문

JDK 24 사용 필수 / Kotlin 2.2 / 단일 모듈 / 외부 라이브러리 최소 / JAR 실행 가능해야 함.
PDF 안에 제시한 코드 로직과 동일하게 구현하고 상품 정보 입력, 판매 기록 입력, 시스템 설정, 출력 결과예시를 지켜서 프로젝트 만들자.

의도: 프로젝트 전역의 제약을 선명히 고정.
효과: Gradle/Kotlin 버전, 단일 모듈, 빌드 스크립트, 출력 포맷이 처음부터 일관되게 설정됨.
2) 리포트가 반드시 답해야 할 4문항 고지

원문

“3일 내 유통기한 임박 상품은?”, “이번 주 매출 1위는?”, “재고 회전율이 가장 낮은 상품은?”, “자동 발주가 필요한 상품들은?”

의도: 리포트의 최소 요건을 명시.
효과: InventoryManager/Report에 4개 섹션이 고정 탑재되고 테스트 데이터로 검증.
3) 단일 모듈 전환 & Gradle 에러 처리

원문

Project directory '.../app' is not part of the build ... 단일 모듈 구조로 맞는지?

의도: app/ 서브모듈을 제거하고 루트 단일 모듈로 정리.
효과: settings.gradle.kts/build.gradle.kts 재구성, ./gradlew projects로 “No sub-projects” 확인.
4) JDK 24 런타임 불일치 해결

원문

UnsupportedClassVersionError … class file version 68.0

의도: 빌드 JDK=24와 실행 JRE=21 불일치 해결.
효과: Temurin-24 설치 및 PATH/JAVA_HOME 정렬, java -version 24 고정.
5) 코드 8개 파일 일괄 수정 요청

원문

총 8개 코드 수정해서 보내

의도: PDF 로직을 반영한 구조(Policy/Product/InventoryManager/Extensions/Report/Main 등)로 재작성.
효과: 한 커밋에서 구조/로직 통일, 과제 채점 기준과 맞춤.
6)재고율·회전율·베스트셀러 정확화 + 운영현황 표

원문

재고율, 베스트셀러 상위5개, 회전율 제대로 나오게, 종합적으로 운영현황 뜨게 코드 수정.

의도: 지표 수식 명확화, TOP5 매출액 기준, 테이블 형태의 종합 현황.
효과: OperationRow 도입, [9] 표 섹션 추가, 포맷 헬퍼로 가독성 개선.
