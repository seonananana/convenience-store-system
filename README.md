# 편의점 스마트 재고 관리 시스템

## 개요
Kotlin 2.2 + JDK 24 기반 **단일 모듈** CLI 프로그램.  
재고율/유통기한/회전율/매출을 종합해 발주·할인·운영현황 리포트를 출력합니다.

## 구현 방법
- 언어/런타임: Kotlin 2.2, JDK 24
- 구조: 단일 모듈(Gradle, Kotlin/JVM), 표준 라이브러리만 사용
- 데이터 입출력: `src/main/resources/products.csv`, `sales.csv` (헤더 기반 파싱)
- 핵심 로직
  - 임박 탐지: D-`expireSoonDays` 이내 → 할인 정책(`DiscountPolicy`)
  - 베스트셀러: **매출액 기준 TOP5** (판매 0도 포함)
  - 회전율: 기간 판매량 / 적정재고 × 100
  - 자동 발주: 재고율 < `lowStockRatio` → 필요 수량 산출
  - 주관 전략: `SubjectiveWeights`로 발주/할인 의사결정 가중치 적용

## 사용 방법
```bash
./gradlew clean build -x test
java -jar build/libs/convenience-store-system-all.jar
# 또는
./gradlew run
##csv 포맷 예시
products.csv

name,category,price,ideal,current,expiry
콜라 500ml,BEVERAGE,1800,50,10,
감자칩 오리지널,SNACK,2200,40,18,
도시락 불고기,FOOD,4500,20,0,2025-10-18
건전지 AAA(4개입),LIVING,3500,30,28,


sales.csv

name,qty,date
콜라 500ml,22,2025-10-16
감자칩 오리지널,15,2025-10-16
건전지 AAA(4개입),3,2025-10-16
##사용 시나리오
오픈 전: [1] 자동 발주 확인 후 발주 수량 확정

피크 후: [5] 베스트셀러로 진열/발주 우선순위 조정

마감 전: [2] 임박/할인으로 폐기 최소화, [8] 동적 할인 확인
##설정값
Thresholds(lowStockRatio=0.30, expireSoonDays=3)

DiscountPolicy(1→40%, 3→20%, 7→10%)

SubjectiveWeights(turnover=0.35, margin=0.25, stability=0.25, fresh=0.15)
##프로젝트 구조
src/main/kotlin/store/...
src/main/resources/products.csv
src/main/resources/sales.csv
build.gradle.kts
settings.gradle.kts
