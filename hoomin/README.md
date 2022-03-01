# QueryDSL

## 라이브러리
- querydsl-apt : 코드 제너레이션
- querydsl-jpa : 실제 쿼리 만들때 사용하는 라이브러리, jpa 특화

## QueryDSL
- Spring framework에서 주입해주는 entity manager는 멀티스레드에서 동시성에 문제가 없게 설계되어있다.
- 같은 테이블 join해야 할 경우는 Qclass 생성자 파라미터로 이름을 넣는다.

## 페이징
- 성능이 중요한 쿼리는 fetchResults로 하지 않고 count 따로 구현하는게 좋을 수도 있다.
- join등
 
## 집합
- having은 group by 다음에 수행됨
- 결국 sql를 잘 알아야 한다.

## Join
- join시 on도 지정 가능, 없이도 가능

## on
- innerjoin이면 on에서 조건 거는것과 where에서 조건 거는게 동일
- 연관관계 없는 외부 조인시 명시적으로 on절에 적는것으로 사용 가능

## 서브쿼리
- JPA 한계로 from절에선 서브쿼리 불가능
    - 하이버네이트는 select 서브쿼리는 지원
    - 서브쿼리는 join으로 바꾼다.
    - 쿼리를 2번 나눈다.
    - native SQL
    
## Case
- DB에서 보여주는 걸 바꾸는 일은 하지 말자.
    - 화면은 화면 로직에서

## 상수
- .stringValue()는 enum 처리할때 자주 사용

# 중급 문법

## 프로젝션과 결과 반환

- 프로젝션: select하는 대상을 지정
- 2개 이상 select하려면 tuple이나 dto 사용
- tuple은(com.querydsl.core 패키지 등) repository계층 넘어서 service나 controller에서 사용하는건 비추
  - 핵심 비즈니스, 서비스 로직에서 jpa를 사용한다는걸 알지 못해야 좋다.