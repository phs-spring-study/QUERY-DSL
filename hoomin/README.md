# QueryDSL

## 라이브러리
- querydsl-apt : 코드 제너레이션
- querydsl-jpa : 실제 쿼리 만들때 사용하는 라이브러리, jpa 특화

## QueryDSL
- Spring framework에서 주입해주는 entity manager는 멀티스레드에서 동시성에 문제가 없게 설계되어있다.
- 같은 테이블 join해야 할 경우는 Qclass 생성자 파라미터로 이름을 넣는다.

## 결과 조회
- 성능이 중요한 쿼리는 fetchResults로 하지 않고 count 따로 구현하는게 좋을 수도 있다.
