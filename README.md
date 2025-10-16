
# 편의점 스마트 재고 관리 시스템

Kotlin 2.2 + JDK 24 기반 **단일 모듈** CLI 애플리케이션입니다.  
CSV(상품/판매)를 읽어 **유통기한 임박, 베스트셀러 TOP5(매출액 기준), 재고 회전율, 자동 발주**, 주관 가중치 기반의 **발주/할인 제안**, 그리고 **종합 운영 현황 표**를 출력합니다.

---

## 1) 요구사항(필수)
- **JDK:** 24 (런타임 포함)
- **Kotlin:** 2.2.0
- **모듈:** 단일 모듈(Gradle Kotlin/JVM)
- **라이브러리:** 외부 의존성 최소(표준 라이브러리 위주)
- **실행 가능 JAR:** 생성 및 `java -jar` 실행 가능

확인 명령:
```bash
java -version              # → 24
./gradlew -v              # → Kotlin 2.2.0
./gradlew projects        # → No sub-projects
2) 빠른 실행
bash
코드 복사
# 빌드 (테스트 제외)
./gradlew clean build -x test

# fat JAR 실행
java -jar build/libs/convenience-store-system-all.jar

# 또는 (개발 중)
./gradlew run
산출물은 build/libs/convenience-store-system-all.jar 입니다.

3) 데이터 입력 (CSV, 헤더 기반 파싱)
src/main/resources 폴더의 CSV를 읽습니다.
헤더 이름으로 매핑하므로 순서는 자유입니다.

3.1 products.csv
지원 헤더(대소문자 무관):

name, category|cat, price

ideal|target, current|stock

expiry|expire|exp|expirydate (옵션, YYYY-MM-DD)

예시:

csv
코드 복사
name,category,price,ideal,current,expiry
콜라 500ml,BEVERAGE,1800,50,10,
감자칩 오리지널,SNACK,2200,40,18,
도시락 불고기,FOOD,4500,20,0,2025-10-18
건전지 AAA(4개입),LIVING,3500,30,28,
3.2 sales.csv
지원 헤더(대소문자 무관):

name|product|productname

qty|quantity|count|sold

date (옵션)

예시:

csv
코드 복사
name,qty,date
콜라 500ml,22,2025-10-16
감자칩 오리지널,15,2025-10-16
건전지 AAA(4개입),3,2025-10-16
4) 핵심 로직 & 지표 정의
유통기한 임박: D-n ≤ expireSoonDays
→ DiscountPolicy에 따라 가격 자동 할인(예: D-1 40%, D-3 20%, D-7 10%).

베스트셀러 TOP5: 매출액(가격 × 판매수량) 기준 정렬.
상품이 5개 미만이면 **(빈 슬롯)**으로 5줄 보장.

재고율(%): current / ideal × 100 (ideal=0이면 100%로 간주).

회전율(%): 기간 판매수량 / ideal × 100 (ideal=0이면 0%로 간주).

자동 발주: 재고율 < lowStockRatio → ideal - current만큼 발주 제안.

주관 전략: SubjectiveWeights(회전/마진/안정/신선) 가중합 점수로
발주 우선순위 및 동적 할인율을 제안(최대 50% 캡).

5) 설정 변경
설정 코드는 보통 Main.kt에서 주입합니다.

kotlin
코드 복사
val manager = InventoryManager(
    products = Csv.loadProducts(),
    thresholds = Thresholds(lowStockRatio = 0.30, expireSoonDays = 3),
    discountPolicy = DiscountPolicy(steps = listOf(1 to 0.40, 3 to 0.20, 7 to 0.10)),
    subjective = SubjectiveWeights(
        weightTurnover = 0.35,
        weightMargin = 0.25,
        weightStability = 0.25,
        weightFreshPriority = 0.15
    )
)
lowStockRatio: 자동 발주 임계 재고율(기본 0.30 → 30%)

expireSoonDays: 임박 판정 기준일수(기본 3일)

DiscountPolicy.steps: D-n별 할인율

SubjectiveWeights: 주관 가중치

6) 사용 시나리오
오픈 전: [1] 자동 발주 확인 → 발주 수량 확정

피크 후: [5] 베스트셀러 TOP5 기반 진열/발주 우선순위 조정

마감 전: [2] 임박/할인과 [8] 동적 할인으로 폐기 최소화

7) 샘플 출력(발췌)
markdown
코드 복사
[1] 🚨 자동 발주 필요 품목
 - 콜라 500ml: +40개
 - 도시락 불고기: +20개

[2] ⚠ 유통기한 임박/할인
 - 도시락 불고기 (D-2) → 4,500원 → 3,600원

[5] 🏆 베스트셀러 TOP5 (매출액 기준)
 1. 콜라 500ml  22개  (₩39,600)
 2. 감자칩 오리지널  15개  (₩33,000)
 3. 건전지 AAA(4개입)  3개  (₩10,500)
 4. 도시락 불고기  0개  (₩0)
 5. (빈 슬롯)
8) 프로젝트 구조
css
코드 복사
src/main/kotlin/store/...
src/main/resources/products.csv
src/main/resources/sales.csv
build.gradle.kts
settings.gradle.kts
