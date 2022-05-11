# section 6. 실무 활용 - 스프링 데이터 JPA와 Querydsl

# 1. 스프링 데이터 JPA 리포지토리로 변경

스프링 데이터 JPA 사용을 위해 `MemberRepository` 인터페이스를 만들고, `JpaRepository`를 상속받는다.

```java
public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByUsername(String username);
}
```

순수 JPA와 마찬가지로 테스트 코드를 작성한다.

```java
@SpringBootTest
@Transactional
class MemberRepositoryTest {
    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Test
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberRepository.findAll();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberRepository.findByUsername("member1");
        assertThat(result2).containsExactly(member);
    }
}
```

`MemberJpaRepository`에서 `MemberRepository`로만 잘 바꿔주면 된다.

# 2. 사용자 정의 리포지토리

querydsl을 쓰려면 구현 코드를 만들어야 하는데, 스프링 데이터 JPA는 인터페이스로 동작한다. 그래서, 내가 원하는 구현 코드를 넣으려면 `사용자 정의 리포지토리` 라는 조금 복잡한 방법을 사용해야 한다.

## **사용자 정의 리포지토리 사용방법**

1. 사용자 정의 인터페이스 작성
2. 사용자 정의 인터페이스 구현
3. 스프링 데이터 리포지토리에 사용자 정의 인터페이스 상속

## **사용자 정의 리포지토리 구성**

![image](https://user-images.githubusercontent.com/52458039/157162581-ec44bedc-d717-4132-8f38-74d24ccbbce1.png)

스프링 데이터 JPA를 사용하면서, querydsl을 이용하여 내가 직접 구현한 search 메서드를 사용하고 싶을때 사용자 정의 리포지 토리(`MemberRepositoryCustom`)를 사용한다.

```java
public interface MemberRepositoryCustom { // 이름은 아무거나 적어도 된다.
    List<MemberTeamDto> search(MemberSearchCondition condition);
}
```

위에서 커스텀하게 만든 인터페이스를 구현한 `MemberRepositoryImpl` 클래스를 만들고, search 메서드를 구현한다.

```java
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

}
```

그리고 `MemberRepository`인터페이스에서 `MemberRepositoryCustom`인터페이스를 상속받는다.

```java
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom{
    List<Member> findByUsername(String username);
}
```

이제 테스트 코드를 작성하면 잘 동작한다.

```java
@SpringBootTest
@Transactional
class MemberRepositoryTest {
    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    ...

    @Test
    public void searchTest() {
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

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> result = memberRepository.search(condition); // querydsl을 이용한 메서드

        assertThat(result).extracting("username").containsExactly("member4");
    }
}
```

> search 메서드를 구현한 코드를 인터페이스에 구현할 수 없기 때문에 어쩔 수 없이 Custom하게 클래스를 만들어줘야 한다.
> 

만약 조회쿼리가 너무 복잡해진다면? 예를들어 특정 기능(or 특정 화면에 포커싱)에 맞춰진 조회기능이라면, `MemberRepository`에서 만드는게 아니라, 그냥 별도로 `MemberQueryRepository`를 만들어서

```java
@Repository
public class MemberQueryRepository {
    private final JPAQueryFactory queryFactory;

    public MemberQueryRepository(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }
}
```

굳이 Custom에 꼭 다 구현할 필요 없다.

핵심 비즈니스로직에서 자주 쓰고, 공통적으로 쓰인다면 MemberRepository에 넣고 

만약 전혀 공통점이 없고, 특정 API에 종속적이라면 조회용 리포지토리를 만드는게 더 좋을 수 있다. (이런 경우는 라이프 사이클 상 API 설계가 변경되면 이 클래스만 찾아서 바꿔주면 되서 훨씬 수월하게 바꿀 수 있음)

# 3. 스프링 데이터 페이징 활용1 - Querydsl 페이징 연동

## 스프링 데이터의 Page, Pageable을 활용한다.

1. 전체 카운트를 한번에 조회하는 단순한 방법
2. 데이터 내용과 전체 카운트를 별도로 조회하는 방법

## 1. 전체 카운트를 한번에 조회하는 단순한 방법

우선 조회할 메서드를 인터페이스에 정의한다.

```java
public interface MemberRepositoryCustom {
    List<MemberTeamDto> search(MemberSearchCondition condition);
    Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);
    Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);
}
```

그리고 정의된 메서드를 구현한다.

이때, querydsl에서 `.fetch()`의 반환 타입은 데이터 컨텐츠를 바로 가져온다. 카운트 쿼리가 없음. (위의 경우 `Page<MemberTeamDto>`)

`.fetchResults()`를 쓰게 되면 컨텐츠용 쿼리와, 카운트용 쿼리, 총 2개를 날린다. (페이징 처리를 위해 전체 데이터 개수를 알고 있어야 하기 때문)

```java
@Override
public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
    QueryResults<MemberTeamDto> results = queryFactory
            .select(new QMemberTeamDto(
                    member.id,
                    member.username,
                    member.age,
                    team.id,
                    team.name))
            .from(member)
            .leftJoin(member.team, team)
            .where(
                    usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())
            )
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetchResults();

    List<MemberTeamDto> content = results.getResults();
    long total = results.getTotal();

    return new PageImpl<>(content, pageable, total);
}
```

![image](https://user-images.githubusercontent.com/52458039/157162641-2e40f6c3-a1b3-410d-92e8-021826976538.png)

카운트 쿼리가 나가고, search 쿼리가 나간다.

> 참고 : 만약 querydsl에서 orderby를 쓴다면, 정렬은 전체 카운트 쿼리와는 상관이 없기 때문에  fetchResults를 쓴다면 querydsl이 최적화하여 orderby 쿼리는 빼준다. (정렬이 필요없기 때문)
> 

> 참고 : fetchResults는 현재 deprecated 되었음. 그래서 카운트 쿼리를 따로 날려주는걸 권장함. 위의 경우라면 fetch를 사용하여 List를 가져오고, 그 List의 size() 메서드를 사용하여 PageImpl의 인자로 넘겨주면 된다.

> 참고 : `select count(*)`를 사용하고 싶다면 `select(Wildcard.count)`를 사용하면 된다.


## 2. 데이터 내용과 전체 카운트를 별도로 조회하는 방법

```java
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    ...

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }

    ...

}
```

- 직접 total count 쿼리를 날리면 select projection을 최소화하여 count 쿼리를 날릴 수 있다. (상황마다 다르긴 한데, count 쿼리를 날릴 때, join이 필요 없을 수 있다.) → fetchResults에서는 count 쿼리를 최적화 하지 못했지만, 이렇게 따로 최적화된 count 쿼리를 날릴 수 있다.
- 코드를 리팩토링 해서 데이터 쿼리 / 카운트 쿼리를 메서드로 추출하여 분리하면 더 읽기 좋다.

# 4. 스프링 데이터 페이징 활용2 - CountQuery 최적화

스프링 데이터 라이브러리가 **`PageableExecutionUtils.getPage()`** 를 제공한다.

- count 쿼리가 생략 가능한 경우 생략하여 처리한다.
    - 첫번째 페이지면서, 컨텐츠 사이즈가 페이지 사이즈보다 작은 경우
    - 마지막 페이지 일때 (offset + 컨텐츠 사이즈를 더하여 총 사이즈를 구함)

```java
@Override
public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
    List<MemberTeamDto> content = queryFactory
            .select(new QMemberTeamDto(
                    member.id,
                    member.username,
                    member.age,
                    team.id,
                    team.name))
            .from(member)
            .leftJoin(member.team, team)
            .where(
                    usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())
            )
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    JPAQuery<Member> countQuery = queryFactory
            .select(member)
            .from(member)
            .leftJoin(member.team, team)
            .where(
                    usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())
            );

    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
}
```

countQuery를 날릴때, fetchCount대신, 우선 JPAQuery로 뽑아놓고, 

`PageableExecutionUtils.getPage` 를 사용한다.

![image](https://user-images.githubusercontent.com/52458039/157162663-cb55b26b-09a1-4fee-aeb4-7ee907dab444.png)

# 5. 스프링 데이터 페이징 활용3 - 컨트롤러 개발

지금까지 만든 search 메서드를 이용하여 화면에 뿌려줄 컨트롤러를 만든다.

```java
@RestController
@RequiredArgsConstructor
public class MemberController {
    private final MemberJpaRepository memberJpaRepository;
    private final MemberRepository memberRepository;

    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) {
        return memberJpaRepository.search(condition);
    }

    @GetMapping("/v2/members")
    public Page<MemberTeamDto> searchMemberV2(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageSimple(condition, pageable);
    }

    @GetMapping("/v3/members")
    public Page<MemberTeamDto> searchMemberV3(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageComplex(condition, pageable);
    }
}
```

![image](https://user-images.githubusercontent.com/52458039/157162700-7462200e-0799-4689-b3f9-9de0935140cb.png)

![image](https://user-images.githubusercontent.com/52458039/157162717-75fa2779-1a7c-481c-b044-6c60646b985f.png)

size=5 이므로 총 페이지가 20페이지로 잘 계산되어 나온다. (v2, v3 결과 같게 나옴)

![image](https://user-images.githubusercontent.com/52458039/157162742-0dadc17f-7ef8-4649-bd83-c09e7e2f5686.png)

카운트 쿼리와 페이징 쿼리도 잘 나간다.

v3의 경우, 카운트 쿼리 최적화가 잘 되있는지 확인해보자.

![image](https://user-images.githubusercontent.com/52458039/157162776-27e62e29-030f-40e1-8bea-5c88e8bde16d.png)

![image](https://user-images.githubusercontent.com/52458039/157162794-16db6d15-6507-4e37-b35a-ac9ded9cb3a1.png)

페이지 번호가 0이고, 현재 데이터가 100개가 있으므로, 페이징 쿼리에서 사이즈를 200을 주게 되면 카운트 쿼리가 나가지 않게 된다.

### 참고 : 스프링 데이터 정렬 (Sort)

스프링 데이터 JPA는 자신의 정렬(Sort)을 Querydsl의 정렬(OrderSpecifier)로 편리하게 변경하는
기능을 제공한다. 이 부분은 뒤에 스프링 데이터 JPA가 제공하는 Querydsl 기능을 참고하자.

**스프링 데이터 Sort를 Querydsl의 OrderSpecifier로 변환**

```java
JPAQuery<Member> query = queryFactory.selectFrom(member);
for (Sort.Order o : pageable.getSort()) {
    PathBuilder pathBuilder = new PathBuilder(member.getType(),
    member.getMetadata());
    query.orderBy(new OrderSpecifier(o.isAscending() ? Order.ASC : Order.DESC,
                pathBuilder.get(o.getProperty())));
}
List<Member> result = query.fetch();
```

근데 이 방법은 조인을 사용하게 되면 안된다. 단순한 엔티티 하나만 가져올 때는 되는데, 조회 조건이 복잡해지면 `Pageable`의 `sort` 기능을 사용하기 어려워 진다.

범위를 넘어가는 `**동적 정렬 기능**`이 필요하면 스프링 데이터 페이징이 제공하는 `Sort`를 사용하기 보다 파라미터를 받아서 직접 처리하는 것을 권장한다.
