# section 3. 기본 문법

# 1. 시작 - JPQL vs Querydsl

```groovy
@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    @BeforeEach
    public void before() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        // member1을 찾아라.
        String qlString = "select m from Member m " +
                "where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember m = new QMember("m");

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1")) // 파라미터 바인딩
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }
}
```

QueryDsl에서, JPAQueryFactory 생성할 때, entitymanger를 파라미터로 넘겨서 해당 entitymanger로 데이터를 찾아올 수 있도록 한다.

그리고 QMember 생성자로 “m” 별칭을 넣었는데 (이걸로 구분함), 별로 중요하진 않음. ⇒ 왜냐면 안쓰기 때문.

JPQL의 경우, username을 개발자가 직접 파라미터 바인딩 해줬는데, QueryDsl은 이미 짜여있는 PreParedStatement를 이용하여 자동으로 파라미터 바인딩을 해준다.

> 참고 : Querydsl은 JPQL 빌더!
> 

```groovy
@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        // member1을 찾아라.
        String qlString = "select m from Member m " +
                "where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        QMember m = new QMember("m");

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1")) // 파라미터 바인딩
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }
}
```

다음과 같이 JPAQueryFactory를 필드레벨로 주입해도 된다. (동시성 문제를 생각하지 않아도 됨)

⇒ 스프링 프레임워크가 주입해주는 EntityManger 자체가 멀티스레드에 문제 없도록 설계되어 있음.

(여러 쓰레드가 동시에 같은 EntityManager에 접근해도, 어느 트랜잭션에 걸려있는지에 따라 그 트랜잭션에 바인딩 되도록 영속성 컨텍스트를 각각 분배해 준다. ⇒ 그래서 필드레벨로 빼서 쓰는게 깔끔하게 코드를 짤 수 있어서 좋다.)

# 2. 기본 Q-Type 활용

### Q클래스 인스턴스를 사용하는 2가지 방법

```java
QMember qMember = new QMember("m") // 별칭 직접 지정
QMember qMember = new QMember.member; // 기본 인스턴스 사용
```

### 기본 인스턴스를 static import와 함께 사용

```java
import static study.querydsl.entity.QMember.member; 
// 이렇게 static import로 사용하면 코드를 더 간결히 작성 가능

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    ...

    @Test
    public void startQuerydsl() {
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) // 파라미터 바인딩
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }
```

⇒ queryDsl도 결국 jpql빌더이다. 그래서 로그로 jpql을 보고 싶다면

```yaml
spring:
  jpa:
    properties:
      hibernate:
        use_sql_comments:true
```

`use_sql_comments:true` 를 넣어주면 된다.

![Untitled](https://user-images.githubusercontent.com/52458039/151105762-34390c88-1776-4114-99cd-5d3b67fc79fd.png)

위가 jpql이고 아래가 sql이다.

jpql의 alias가 현재 member1인데, Q클래스가 제공하는 기본 인스턴스의 별칭을 사용한다.

> 참고 : 같은 테이블을 조인할 때, 이름이 같으면 안되므로 이럴 때 위와같이 별칭을 따로 선언해서 쓰면 된다.
> 

# 3. 검색 조건 쿼리

검색 조건은 `.and()`, `.or()`를 메서드 체인으로 연결할 수 있다.

> 참고 : `select`, `from`을 `selectFrom`으로 합칠 수 있다.
> 

### JPQL이 제공하는 모든 검색 조건 제공

```java
member.username.eq("member1") // username = 'member1'
member.username.ne("member1") //username != 'member1'
member.username.eq("member1").not() // username != 'member1'

member.username.isNotNull() //이름이 is not null

member.age.in(10, 20) // age in (10,20)
member.age.notIn(10, 20) // age not in (10, 20)
member.age.between(10,30) //between 10, 30

member.age.goe(30) // age >= 30 (greater or equal)
member.age.gt(30) // age > 30 (greater than)
member.age.loe(30) // age <= 30 (lower or equal)
member.age.lt(30) // age < 30 (lower than)

member.username.like("member%") //like 검색 
member.username.contains("member") // like ‘%member%’ 검색 
member.username.startsWith("member") //like ‘member%’ 검색 <- 성능 최적화에 쓰임
...
```

### AND 조건을 파라미터로 처리

```java
@Test
public void searchAndParam() {
    List<Member> result1 = queryFactory
            .selectFrom(member)
            .where(
										member.username.eq("member1"),
                    member.age.eq(10))
            .fetch();
    assertThat(result1.size()).isEqualTo(1);
}
```

- `where()` 에서 파라미터로 검색 조건을 콤마로 구분하면 `AND`조건이 됨.
- 이 경우에는 `null` 값을 무시한다 ⇒ 메서드 추출을 사용해서 동적쿼리를 깔끔하게 만들 수 있다. (뒤에서 설명)

# 4. 결과 조회

- `fetch()` : 리스트 조회, 데이터 없으면 빈 리스트를 반환한다.
- `fetchOne()` : 단 건 조회
    - 결과가 없으면 : `null`
    - 결과가 둘 이상이면 : `com.querydsl.core.NonUniqueResultException`
- `fetchFirst()` : `limit(1).fetchOne()`
- `fetchResults()` : 페이징 정보 포함, total count 쿼리 추가 실행
- `fetchCount()` : count 쿼리로 변경해서 count 수 조회

```java
@Test
public void resultFetch() {
    // 페이징에서 사용
    QueryResults<Member> results = queryFactory
            .selectFrom(member)
            .fetchResults();

    List<Member> content = results.getResults();

  }
}
```

![Untitled 1](https://user-images.githubusercontent.com/52458039/151105811-1987402f-7b27-40d7-9576-fcd193a612c3.png)

`fetchResults`를 사용하면 카운트쿼리와 조회 쿼리가 동시에 나가는 것을 볼 수 있다.

(total이 있어야 페이지 어디까지 있는지 알려 줄 수 있다.)

```java
@Test
public void resultFetch() {
    // count 쿼리로 변경 => 카운트용 쿼리로 바꿔줌
        long total = queryFactory
                .selectFrom(member)
                .fetchCount();
  }
}
```

![Untitled 2](https://user-images.githubusercontent.com/52458039/151105825-73c56769-3995-4255-998c-b4d29a37cecd.png)

count 쿼리만 나간다. (jpql에서 엔티티를 직접 가리키면 id로 바뀜) ⇒ select절을 다 지우고 카운트 쿼리만 나가도록 함.

> 참고 : fetchResult의 경우, 페이징 쿼리가 복잡해 지면, 컨텐츠를 가져오는 쿼리와 실제 토탈 카운트를 가져오는 쿼리가 성능 때문에 다른 경우가 있다. (성능 때문에 카운트 쿼리를 더 심플하게 만듦) ⇒ 따라서, 성능이 중요한 페이징 처리시 이 fetchResult를 쓰면 안되고, 그냥 쿼리를 2번 따로 날려야 한다.
> 

# 5. 정렬

```java
/**
 * 회원 정렬 순서
 * 1. 회원 나이 내림차순(desc)
 * 2. 회원 이름 올림차순(asc)
 * 단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
 */
@Test
public void sort() {
    em.persist(new Member(null, 100));
    em.persist(new Member("member5", 100));
    em.persist(new Member("member6", 100));

    List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.eq(100))
            .orderBy(member.age.desc(), member.username.asc().nullsLast())
            .fetch();

    Member member5 = result.get(0);
    Member member6 = result.get(1);
    Member memberNull = result.get(2);

    assertThat(member5.getUsername()).isEqualTo("member5");
    assertThat(member6.getUsername()).isEqualTo("member6");
    assertThat(memberNull.getUsername()).isNull();
}
```

![Untitled 3](https://user-images.githubusercontent.com/52458039/151105841-d52e6487-7a79-4e60-934e-9613aebbe03e.png)

- `desc()`, `asc()` : 일반 정렬
- `nullsLast()`, `nullsFirts()` : null 데이터 순서 부여

# 6. 페이징

### 조회 건수 제한

```java
@Test
public void paging1() {
    List<Member> result = queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(1)
            .limit(2)
            .fetch();
    assertThat(result.size()).isEqualTo(2);
}
```

![Untitled 4](https://user-images.githubusercontent.com/52458039/151105860-5fef90fb-b9e2-4f97-bd12-6139af1297f9.png)

⇒ 전체 조회 수가 필요하다면?

```java
@Test
public void paging2() {
    QueryResults<Member> queryResults = queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(1)
            .limit(2)
            .fetchResults();

    assertThat(queryResults.getTotal()).isEqualTo(4);
    assertThat(queryResults.getLimit()).isEqualTo(2);
    assertThat(queryResults.getOffset()).isEqualTo(1);
    assertThat(queryResults.getResults().size()).isEqualTo(2);
}
```

![Untitled 5](https://user-images.githubusercontent.com/52458039/151105890-683210af-5a9c-4434-9d3d-f7ecb4884396.png)

카운트 쿼리와 데이터 쿼리가 같이 나간다.

> 주의 : count 쿼리가 실행되므로 성능상 주의해야 한다.
> 

> 참고 : 실무에서 페이징 쿼리를 작성할 때, 데이터를 조회하는 쿼리는 여러 테이블을 조인해야 하지만, count 쿼리는 조인이 필요 없는 경우도 있다. 그런데 이렇게 자동화된 count 쿼리는 원본 쿼리와 같이 모두 조인을 해버리기 때문에 성능이 안나올 수 있다. count 쿼리에 조인이 필요없는 성능 최적화가 필요하다면, count 전용 쿼리를 별도로 작성해야 한다.
> 

# 7. 집합

### 집합 함수 사용법

```java
/**
 * JPQL
 * select
 * COUNT(m), //회원수
 * SUM(m.age), //나이 합
 * AVG(m.age), //평균 나이
 * MAX(m.age), //최대 나이
 * MIN(m.age) //최소 나이 * from Member m
 */
@Test
public void aggregation() {
    List<Tuple> result = queryFactory
            .select(
                    member.count(),
                    member.age.sum(),
                    member.age.avg(),
                    member.age.max(),
                    member.age.min()
            )
            .from(member)
            .fetch();

    // 위 튜플의 result가 어짜피 1개 이므로 첫번 째 인덱스만 가져와서 비교
    Tuple tuple = result.get(0);
    assertThat(tuple.get(member.count())).isEqualTo(4);
    assertThat(tuple.get(member.age.sum())).isEqualTo(100);
    assertThat(tuple.get(member.age.avg())).isEqualTo(25);
    assertThat(tuple.get(member.age.max())).isEqualTo(40);
    assertThat(tuple.get(member.age.min())).isEqualTo(10);
}
```

![Untitled 6](https://user-images.githubusercontent.com/52458039/151105914-272935ed-f9c8-4653-a818-f19f3d014989.png)

tuple은 멤버와 같은 엔티티가아닌, 단일값들을 받아올때 사용한다.

> 참고 : 이 tuple방식 보다는 dto로 바로 뽑아오는 방식을 많이 쓴다.
> 
- JPQL이 제공하는 모든 집합 함수를 제공한다.
- tuple은 뒤에서 프로젝션과 결과 반환에서 설명한다.

> 참고 : tuple은 querydsl이 제공하는 거다.
> 

### GroupBy 사용

```java
/**
 * 팀의 이름과 각 팀의 평균 연령을 구해라.
 */
@Test
public void group() {
    List<Tuple> result = queryFactory
            .select(team.name, member.age.avg())
            .from(member)
            .join(member.team, team)
            .groupBy(team.name)
            .fetch();

    Tuple teamA = result.get(0);
    Tuple teamB = result.get(1);

    assertThat(teamA.get(team.name)).isEqualTo("teamA");
    assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10 + 20) / 2

    assertThat(teamB.get(team.name)).isEqualTo("teamB");
    assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30 + 40) / 2
}
```

![Untitled 7](https://user-images.githubusercontent.com/52458039/151105930-5e66ac34-6b15-4572-85c8-a0d4676e56da.png)

`groupBy` ⇒ 그룹화된 결과를 제한하려면 `having` 을 이용한다.

### groupBy(), having() 예시

```java
...
.groupBy(item.price)
.having(item.price.gt(100))
...

// 상품 가격이 100이상인 그룹만 조회
```

# 7. 조인 - 기본 조인

### 기본 조인

조인의 기본 문법은 첫 번째 파라미터에 조인 대상을 지정하고, 두 번째 파라미터에 별칭(alias)으로 사용할 Q 타입을 지정하면 된다.

`join(조인 대상, 별칭으로 사용할 Q타입)`

```java
/**
 * 팀 A에 소속된 모든 회원
 */
@Test
public void join() {
    List<Member> result = queryFactory
            .selectFrom(member)
            .join(member.team, team)
            .where(team.name.eq("teamA"))
            .fetch();

    assertThat(result)
            .extracting("username")
            .containsExactly("member1", "member2");
}
```

![Untitled 8](https://user-images.githubusercontent.com/52458039/151105943-19031117-5ec6-46bc-a1ae-5168916ef565.png)

left join을 하고 싶다면

```java
@Test
public void join() {
    List<Member> result = queryFactory
            .selectFrom(member)
            .leftJoin(member.team, team)
            .where(team.name.eq("teamA"))
            .fetch();

    assertThat(result)
            .extracting("username")
            .containsExactly("member1", "member2");
}
```

![Untitled 9](https://user-images.githubusercontent.com/52458039/151105977-79743760-0a76-40fa-ab5e-75f868e72a51.png)

- `join()` , `innerJoin()` : 내부 조인(inner join)
- `leftJoin()` : left 외부 조인(left outer join)
- `rightJoin()` : rigth 외부 조인(rigth outer join)
- JPQL의 `on` 과 성능 최적화를 위한 `fetch` 조인을 제공 한다.⇒ 다음 on 절에서 설명

### 세타 조인

연관관계가 없는 필드로 조인

```java
/**
 * 세타 조인
 * 회원의 이름이 팀 이름과 같은 회원 조회 (연관관계 x)
 */
@Test
public void theta_join() {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));
    em.persist(new Member("teamC"));

    List<Member> result = queryFactory
            .select(member)
            .from(member, team)// 막 조인!
            .where(member.username.eq(team.name))
            .fetch();

    assertThat(result)
            .extracting("username")
            .containsExactly("teamA", "teamB");
}
```

![Untitled 10](https://user-images.githubusercontent.com/52458039/151105992-d7439524-5ff2-4c99-afcf-fa820f6ae712.png)

⇒ 모든 회원과 모든 팀을 가져와서 조인을 함.(cross join) 그 다음에 where절로 필터링 한다. (물론 db가 성능 최적화를 해줌)

- from 절에 여러 엔티티를 선택해서 세타 조인 한다.
- 외부 조인이 불가능 함. 근데 세타조인을 하면서 외부조인을 하고 싶은 경우가 생길 수 있음. ⇒ 다음에 설명할 조인 on을 사용하면 외부 조인이 가능함. (하이버네이트 최신버전에서 지원)

# 8. 조인 - on절

- ON절을 활용한 조인(JPA 2.1부터 지원)
  1. 조인 대상 필터링
  2. 연관관계 없는 엔티티 외부 조인

### 1. 조인 대상 필터링

ex ) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회하는 경우

```java
/**
 * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
 * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
 */
@Test
public void join_on_filtering() {
    List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(member.team, team).on(team.name.eq("teamA"))
            .fetch();

    for (Tuple tuple : result) {
        System.out.println("tuple = " + tuple);
    }
}
```

```java
t=[Member(id=3, username=member1, age=10), Team(id=1, name=teamA)]
t=[Member(id=4, username=member2, age=20), Team(id=1, name=teamA)]
t=[Member(id=5, username=member3, age=30), null]
t=[Member(id=6, username=member4, age=40), null]
```

![Untitled 11](https://user-images.githubusercontent.com/52458039/151833906-6b0365b5-80f6-49c0-92b3-bcfd68a58241.png)

![Untitled 12](https://user-images.githubusercontent.com/52458039/151833955-ecc01c32-c1cb-4ad7-b9bb-2a039b5fa744.png)

member1, 2는 teamA소속이므로 팀 내용을 다 가져오고, member 3, 4는 teamB소속이지만 left join이므로 팀이 null인 상태로 조회된다.

```java
@Test
public void join_on_filtering() {
    List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .join(member.team, team).on(team.name.eq("teamA"))
            .fetch();

    for (Tuple tuple : result) {
        System.out.println("tuple = " + tuple);
    }
}
```

다음과 같이 내부 조인을 하게 되면

![Untitled 13](https://user-images.githubusercontent.com/52458039/151833996-e923f13a-8589-47c3-af43-a357aa0ee0bc.png)

![Untitled 14](https://user-images.githubusercontent.com/52458039/151834029-4332df4c-0e36-4ffd-b110-ef044f76dedb.png)

inner join을 해서 teamB 속속 멤버들은 조회되지 않는다.

> 참고 : on 절을 활용해 조인 대상을 필터링 할 때, 외부조인이 아니라 내부조인(inner join)을 사용하면, where  절에서 필터링 하는 것과 기능이 동일하다. 따라서 on 절을 활용한 조인 대상 필터링을 사용할 때, 내부조인 이면 익숙한 where 절로 해결하고, 정말 외부조인이 필요한 경우에만 이 기능을 사용하자.

```java
@Test
public void join_on_filtering() {
    List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .join(member.team, team)
            .where(team.name.eq("teamA"))
            .fetch();

    for (Tuple tuple : result) {
        System.out.println("tuple = " + tuple);
    }
}
```

내부조인을 쓸거면 on절을 이용하기 보다는 익숙한 where절로 사용하면 된다.

### 2. 연관관계 없는 엔티티 외부 조인

예) 회원의 이름과 팀의 이름이 같은 대상 **외부 조인**

from절에서 세타 조인일때는 left join이 안됨. 그래서 아래와 같이 leftJoin을 사용해야 함.

```java
/**
 * 2. 연관관계 없는 엔티티 외부 조인
 * 예)회원의 이름과 팀의 이름이 같은 대상 외부 조인
 * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
 * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name */
@Test
public void join_on_no_relation() {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));
    em.persist(new Member("teamC"));

    List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(team) // leftJoin에 team만 넣으면 on절의 조건에 따른 매칭만 이루어 짐 (세타조인 + left join이 가능)
            .on(member.username.eq(team.name))
            .fetch();

    for (Tuple tuple : result) {
        System.out.println("tuple = " + tuple);
    }
}
```

![Untitled 15](https://user-images.githubusercontent.com/52458039/151834078-dbc23eed-cdd8-403e-8979-da8fd545c3f8.png)

세타조인 + leftJoin이기 때문에 on 조건에 맞지 않는 값들도 조회해 온다.

- 하이버네이트 5.1부터 `on` 을 사용해서 서로 관계가 없는 필드로 외부 조인하는 기능이 추가되었다. 물론 내부 조인도 가능하다.
- 주의! 문법을 잘 봐야 한다. **leftJoin()** 부분에 일반 조인과 다르게 엔티티 하나만 들어간다.
  - 일반조인: `leftJoin(member.team, team)`
    - on조인: `from(member).leftJoin(team).on(xxx)`

# 9. 조인 - 페치 조인

페치 조인은 SQL에서 제공하는 기능은 아니다. SQL조인을 활용해서 연관된 엔티티를 SQL 한번에

조회하는 기능이다. **주로 성능 최적화에 사용하는 방법이다.**

**페치 조인 미적용**

지연로딩으로 Member, Team SQL 쿼리 각각 실행한다.

```java
@PersistenceUnit
EntityManagerFactory emf;
  
@Test
public void fetchJoinNo() {
    em.flush();
    em.clear();

    Member findMember = queryFactory
            .selectFrom(QMember.member)
            .where(QMember.member.username.eq("member1"))
            .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).as("페치 조인 미적용").isFalse();
}
```

멤버만 조회해온 뒤, team을 추가로 조회해 올때 로딩이 됬냐 안됬냐를 판단하기 위해 EntityManagerFactory의 getPersistenceUnitUtiil의 isLoaded 메서드를 이용하여 영속성 컨텍스트에 로딩이 됬는지 확인할 수 있다.

> 참고 : fetch join test시에는 영속성 컨텍스트를 깔끔하게 먼저 날려주고 테스트 하는게 좋다.

**페치 조인 적용**

즉시로딩으로 Member, Team SQL 쿼리 조인으로 한번에 조회

```java
@Test
public void fetchJoinUse() throws Exception {
    em.flush();
    em.clear();
    Member findMember = queryFactory
            .selectFrom(member)
            .join(member.team, team).fetchJoin()
            .where(member.username.eq("member1"))
            .fetchOne();
    boolean loaded =
		emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		assertThat(loaded).as("페치 조인 적용").isTrue(); 
}
```

![Untitled 16](https://user-images.githubusercontent.com/52458039/151834118-1a4924c2-a4ab-4ff7-ad95-2fb45b519499.png)

fetch join으로 인해 팀을 미리 조회해오게 된다.

**사용방법**

- join(), leftJoin() 등 조인 기능 뒤에 fetchJoin() 이라고 추가하면 된다.

# 10. 서브 쿼리

`com.querydsl.jpa.JPAExpressions`를 사용한다.

### 서브 쿼리 eq 사용

```java
/**
	* 나이가 가장 많은 회원 조회
  */
@Test
public void subQuery() throws Exception {
	  QMember memberSub = new QMember("memberSub");
	  List<Member> result = queryFactory
          .selectFrom(member)
          .where(member.age.eq(
                  JPAExpressions
                          .select(memberSub.age.max())
                          .from(memberSub)
		)) .fetch();
		assertThat(result).extracting("age").containsExactly(40);
}
```

![Untitled 17](https://user-images.githubusercontent.com/52458039/151834158-b4c25c08-be43-4ae2-bc88-18b5e1461660.png)

alias가 중복되면 안되므로 memberSub를 새롭게 정의하고 서브쿼리를 사용해야 한다.

`JPAExpressions.select(memberSub.age.max()).from(memberSub)` 결과가 40이므로 최대 나이에 대한 멤버만 조회해 오게 된다.

### 서브 쿼리 goe 사용

```java
/**
 * 나이가 평균 이상인 회원
 */
@Test
public void subQueryGoe() {
    QMember memberSub = new QMember("memberSub");
    List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.goe(
                    JPAExpressions
                            .select(memberSub.age.avg())
                            .from(memberSub)
            ))
            .fetch();

    assertThat(result).extracting("age")
            .containsExactly(40);
}
```

![Untitled 18](https://user-images.githubusercontent.com/52458039/151834199-ec8d3339-63ce-49eb-9a26-3b4e33f68667.png)

비슷한 방식으로 avg와 goe를 사용하면 해당쿼리로 쉽게 조건에 맞게 조회해올 수 있다.

### 서브쿼리 여러 건 처리 in 사용

```java
/**
 * 나이가 10 초과인 회원
 */
@Test
public void subQueryIn() {
    QMember memberSub = new QMember("memberSub");
    List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.in(
                    JPAExpressions
                            .select(memberSub.age)
                            .from(memberSub)
                            .where(memberSub.age.gt(10))
            ))
            .fetch();

    assertThat(result).extracting("age")
            .containsExactly(20, 30, 40);
}
```

![Untitled 19](https://user-images.githubusercontent.com/52458039/151834225-9210a501-ba83-4158-841d-a8a41f393049.png)

다음과 같이 in 쿼리도 잘 적용된다.

### select 절에 subquery

```java
@Test
public void selectSubQuery() {
    QMember memberSub = new QMember("memberSub");

    List<Tuple> result = queryFactory
            .select(member.username,
                    JPAExpressions
                            .select(memberSub.age.avg())
                            .from(memberSub))
            .from(member)
            .fetch();

    for (Tuple tuple : result) {
        System.out.println("tuple = " + tuple);
    }
}
```

![Untitled 20](https://user-images.githubusercontent.com/52458039/151834269-52a3013d-f248-432c-85ef-5e2ce6437722.png)

select절에서 유저이름과 유저들의 평균들을 다 조회해오게 된다.

JPAExpressions를 static import를 사용해서 아래와 같이 더 간편하게 짤 수도 있다.

```java
@Test
public void selectSubQuery() {
    QMember memberSub = new QMember("memberSub");

    List<Tuple> result = queryFactory
            .select(member.username,
                    select(memberSub.age.avg())
                            .from(memberSub))
            .from(member)
            .fetch();

    for (Tuple tuple : result) {
        System.out.println("tuple = " + tuple);
    }
}
```

**from 절의 서브쿼리 한계**

JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다. 당연히 Querydsl도 지원하지 않는다. 하이버네이트 구현체를 사용하면 select 절의 서브쿼리는 지원한다. Querydsl도 하이버네이트 구현체를 사용하면 select 절의 서브쿼리를 지원한다.

**from 절의 서브쿼리 해결방안**

1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
3. nativeSQL을 사용한다.

서브쿼리 사용 예시 ⇒ [https://m.blog.naver.com/PostView.naver?isHttpsRedirect=true&blogId=pyj721aa&logNo=221466664622](https://m.blog.naver.com/PostView.naver?isHttpsRedirect=true&blogId=pyj721aa&logNo=221466664622)

> from절에 서브 쿼리를 쓰는 이유가 굉장히 많은데, 안좋은 이유가 많다. 화면에 보여줄 데이터를 조회해 오기 위해 from절안에 from절이 계속 들어가는 경우가 많다. ⇒ SQL은 데이터를 가져오는데에 집중하고, 필요하면 어플리케이션에 로직을 돌려서 화면에 맞게 변환해야 한다. (화면에 맞추기 위해 쿼리를 억지로 복잡하게 짜기 보다는, 데이터를 최소화하여 where, grouping하여 가져오는 연습을 많이 해보면 복잡한 쿼리들을 많이 줄일 수 있다.

> 실시간 트래픽이 정말 많은 서비스에서는 쿼리 하나하나가 영향을 크게 미친다. 이럴 때는 화면에 맞는 캐시를 엄청 많이 쓴다. 근데 만약 admin페이지를 만든다면 복잡하게 쿼리를 길게 짜서 한방 쿼리를 날리기 보다는 쿼리를 2~3번 나눠서 날리는게 훨씬 효과적일 수 있다. ⇒ sql은 집합적으로 사고하여 쿼리를 짜야하므로 복잡한데, 애플리케이션 로직은 sequential하게 로직을 순차적으로 만들어서 풀 수 있다. → ( sql - antipatterns 책 참고 : [http://www.yes24.com/Product/Goods/5269099](http://www.yes24.com/Product/Goods/5269099) : 정말 복잡한 수천줄의 쿼리는 쪼개서 몇백줄씩 줄여서 날릴 수 있다.)

# 11. Case 문

**select, 조건절(where), order by에서 사용 가능하다.**

**단순한 조건 (when, then 사용)**

```java
@Test
public void basicCase() {
    List<String> result = queryFactory
            .select(member.age
                    .when(10).then("열살")
                    .when(20).then("스무살")
                    .otherwise("기타"))
            .from(member)
            .fetch();

    for (String s : result) {
        System.out.println("s = " + s);
    }
}
```

![Untitled 21](https://user-images.githubusercontent.com/52458039/151834297-8643362d-e95d-4386-8755-24e10aee40f3.png)

**복잡한 조건 (CaseBuilder 사용)**

```java
@Test
public void complexCase() {
    List<String> result = queryFactory
            .select(new CaseBuilder()
                    .when(member.age.between(0, 20)).then("0~20살")
                    .when(member.age.between(21, 30)).then("21~30살")
                    .otherwise("기타"))
            .from(member)
            .fetch();

    for (String s : result) {
        System.out.println("s = " + s);
    }
}
```

![Untitled 22](https://user-images.githubusercontent.com/52458039/151834335-e469e43f-af1f-4dff-9ec3-3dbf79d0b897.png)

> 복잡한 조건을 가진 쿼리로 디비를 검색하면 안된다. 최소한의 필터링을 통해 가져온 데이터를 애플리케이션로직으로 처리하도록 하자.

# 12. 상수, 문자 더하기

상수가 필요하면 `Expressions.constant(xxx)` 사용한다.

```java
@Test
public void constant() {
    List<Tuple> result = queryFactory
            .select(member.username, Expressions.constant("A"))
            .from(member)
            .fetch();

    for (Tuple tuple : result) {
        System.out.println("tuple = " + tuple);
    }
}
```

![Untitled 23](https://user-images.githubusercontent.com/52458039/151834381-e89f2611-4803-43ca-843e-f463fb291f8e.png)

→ JPQL에서 상수에 대한 조회 쿼리가 나가지 않는다.

**문자 더하기 → concat**

```java
@Test
public void concat() {
    // {username}_{age}
    List<String> result = queryFactory
            .select(member.username.concat("_").concat(member.age.stringValue()))
            .from(member)
            .where(member.username.eq("member1"))
            .fetch();

    for (String s : result) {
        System.out.println("s = " + s);
    }
}
```

![Untitled 24](https://user-images.githubusercontent.com/52458039/151834418-f40e7dc8-529f-4070-99db-214331ce0d66.png)

> 참고: `member.age.stringValue()` 부분이 중요한데, 문자가  아닌 다른 타입들은 `stringValue()` 로 문자로 변환할 수 있다. 이 방법은 ENUM을 처리할 때도 자주 사용한다.

> 결과가 member1_10이 안나오고, member1_1이 나오고 있는데, H2 2.0.202(2021-11-25) 부터 char타입 기본 길이가 1로 고정되어 `cast(10 as char)`가 1로 반환되어 10에서 0이 짤린 1이 나옴. https://github.com/h2database/h2database/issues/2266