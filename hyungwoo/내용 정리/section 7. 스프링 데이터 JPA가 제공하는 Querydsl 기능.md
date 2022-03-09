# section 7. 스프링 데이터 JPA가 제공하는 Querydsl 기능

여기서 소개하는 기능은 제약이 많아서 복잡한 실무 환경에서 사용하기에는 많이 부족하지만, querydsl 기능이 어떤 것들이 있는지 알아보자.

# 1. 인터페이스 지원 - QuerydslPredicateExecutor

공식 URL : [https://docs.spring.io/spring-data/jpa/docs/2.2.3.RELEASE/reference/html/#core.extensions.querydsl](https://docs.spring.io/spring-data/jpa/docs/2.2.3.RELEASE/reference/html/#core.extensions.querydsl)

```java
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, QuerydslPredicateExecutor<Member> {
    List<Member> findByUsername(String username);
}
```

`MemberRepository`에서 `QuerydslPredicateExecutor<Member>`을 상속받는다.

```java
@Test
public void querydslPredicateExecutorTest() {
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

    QMember member = QMember.member;
    Iterable<Member> result = memberRepository.findAll(
            member.age.between(10, 40)
                    .and(member.username.eq("member1")));
    for (Member findMember : result) {
        System.out.println("member1 = " + findMember);
    }
}
```

findAll 파라미터로 predicate 조건절을 넣을 수 있게 된다.

![image](https://user-images.githubusercontent.com/52458039/157453095-2a30871f-34f0-4ef2-a4b5-60b801b30cf3.png)


select 쿼리도 where절로 우리가 정한 조건이 잘 들어 가는 것을 확인할 수 있다.

⇒ 하지만 **한계**가 명확하다.

- 조인X (묵시적 조인은 가능하지만 left join은 불가능)

이런 기능들을 실무에서 쓸 수 있을지 없을지 판단해야 하는데, 실무에서는 rdb와 join을 굉장히 많이 쓰는데, **조인을 할 수 없다**. (실무에서는 테이블 한개만 가지고 쿼리를 하는 경우는 거의 없다..)

- 클라이언트가 Querydsl에 의존해야한다.

서비스나 컨트롤러에서 findAll의 파라미터를 만들어서 넘겨줘야 한다. **결국 서비스나 컨트롤러가 querydsl에 의존적일 수 밖에 없게 된다.**

> 참고 : `QuerydslPredicateExecutor`는 Pageable, Sort를 모두 지원하고 정상 동작한다.
> 

# 2. Querydsl Web 지원

공식 URL : [https://docs.spring.io/spring-data/jpa/docs/2.2.3.RELEASE/reference/html/#core.web.type-safe](https://docs.spring.io/spring-data/jpa/docs/2.2.3.RELEASE/reference/html/#core.web.type-safe)

![image](https://user-images.githubusercontent.com/52458039/157453164-dedc18af-780d-4b18-9d5f-cbffe5240fe7.png)

![image](https://user-images.githubusercontent.com/52458039/157453203-6321c5b7-fdb3-4573-aecb-e29b8a567e33.png)

query string을 predicate로 변환해준다.

이 Querydsl Web 또한 QuerydslPredicateExecutor의 한계를 모두 가지고 있기 때문에(조건을 커스텀하는 기능 또한 너무 복잡함) 실무에서 권장하지 않는 방법이다.

# 3. 리포지토리 지원 - QuerydslRepositorySupport

![image](https://user-images.githubusercontent.com/52458039/157453258-cfc903cc-7a3c-46df-b5b1-ddd70f33f7f6.png)

`QuerydslRepositorySupport` 추상 클래스로 

![image](https://user-images.githubusercontent.com/52458039/157453319-d49d9861-c392-4fdb-9092-013e95cba4b5.png)

querydsl을 사용하기 위해 만들었던 `MemberRepositoryImpl` 클래스에서 `QuerydslRepositorySupport`를 상속받아서 사용할 수 있다. 그리고 `QuerydslRepositorySupport` 의 생성자를 이용하여 EntityManager와 Querydsl이라는 유틸리티성 클래스를 사용할 수 있다.

![image](https://user-images.githubusercontent.com/52458039/157453370-c836b4e5-a4d3-44c9-ab88-6d3e7ef273e3.png)

실제로 사용할 때는 select가 아닌 from절부터 쓰게 된다.

특히, 페이지 네이션에서 좀더 유용하게 쓰일 수 있는데,

![image](https://user-images.githubusercontent.com/52458039/157453437-99e2e29f-cb75-4e87-801d-40eb4d41d08b.png)

따로 offset, limit을 설정해줄 필요 없이 Querydsl 유틸리티 클래스를 이용한 `applyPagination`을 이용하면

![image](https://user-images.githubusercontent.com/52458039/157453489-129de2ca-86fc-426d-809d-2e67a0d7ba48.png)

offset, limit을 설정해준다.

이 방법도 **한계**가 존재한다.

- Querydsl 3.x 버전을 대상으로 만들었기 때문에 from부터 시작한다. (select 로 시작하는게 sql과 같아서 더 명시적임)
- Querydsl 4.x에서 나온 JPAQueryFactory로 시작할 수 없음. (따로 주입받으면 사용할 순 있음)
- 스프링 데이터의 Sort가 정상적으로 동작하지 않음. (버그 존재)

코드 스타일도 메소드 체인으로 가다가, 중간에 끊기는게 보기 별로다.

자신의 상황에 맞춰서 알맞게 사용하자.

# 4. Querydsl 지원 클래스 직접 만들기

스프링 데이터가 제공하는 `QueryRepositorySupport`가 가졌던 한계를 극복하기 위한 Querydsl 지원 클래스를 만들어 보자.

**장점**

- 스프링 데이터가 제공하는 페이징을 편리하게 변환할 수 있다.
- 페이징과 카운트 쿼리를 분리할 수 있다.
- 스프링 데이터의 Sort가 정상 작동한다.
- from절이 아닌 `select()`, `selectFrom()`으로 시작할 수 있다.
- `EntityManager`와 `QueryFactory`를 제공한다.

우선 repository패키지에 support 패키지를 만들고 아래의 만들어진 클래스를 만들어 준다.

```java
/**
 * Querydsl 4.x 버전에 맞춘 Querydsl 지원 라이브러리
 *
 * @author Younghan Kim
 * @see
org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
 */
@Repository
public abstract class Querydsl4RepositorySupport {
    private final Class domainClass;
    private Querydsl querydsl;
    private EntityManager entityManager;
    private JPAQueryFactory queryFactory;

    public Querydsl4RepositorySupport(Class<?> domainClass) {
        Assert.notNull(domainClass, "Domain class must not be null!");
        this.domainClass = domainClass;
    }

    @Autowired
    public void setEntityManager(EntityManager entityManager) {
        Assert.notNull(entityManager, "EntityManager must not be null!");
        JpaEntityInformation entityInformation =
                JpaEntityInformationSupport.getEntityInformation(domainClass, entityManager);
        SimpleEntityPathResolver resolver = SimpleEntityPathResolver.INSTANCE;
        EntityPath path = resolver.createPath(entityInformation.getJavaType());
        this.entityManager = entityManager;
        this.querydsl = new Querydsl(entityManager, new
                PathBuilder<>(path.getType(), path.getMetadata()));
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @PostConstruct
    public void validate() {
        Assert.notNull(entityManager, "EntityManager must not be null!");
        Assert.notNull(querydsl, "Querydsl must not be null!");
        Assert.notNull(queryFactory, "QueryFactory must not be null!");
    }

    protected JPAQueryFactory getQueryFactory() {
        return queryFactory;
    }

    protected Querydsl getQuerydsl() {
        return querydsl;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected <T> JPAQuery<T> select(Expression<T> expr) {
        return getQueryFactory().select(expr);
    }

    protected <T> JPAQuery<T> selectFrom(EntityPath<T> from) {
        return getQueryFactory().selectFrom(from);
    }

    protected <T> Page<T> applyPagination(Pageable pageable,
                                          Function<JPAQueryFactory, JPAQuery> contentQuery) {
        JPAQuery jpaQuery = contentQuery.apply(getQueryFactory());
        List<T> content = getQuerydsl().applyPagination(pageable,
                jpaQuery).fetch();
        return PageableExecutionUtils.getPage(content, pageable,
                jpaQuery::fetchCount);
    }

    protected <T> Page<T> applyPagination(Pageable pageable,
                                          Function<JPAQueryFactory, JPAQuery> contentQuery, Function<JPAQueryFactory,
            JPAQuery> countQuery) {
        JPAQuery jpaContentQuery = contentQuery.apply(getQueryFactory());
        List<T> content = getQuerydsl().applyPagination(pageable,
                jpaContentQuery).fetch();
        JPAQuery countResult = countQuery.apply(getQueryFactory());
        return PageableExecutionUtils.getPage(content, pageable,
                countResult::fetchCount);
    }
}
```

위 클래스를 테스트할 용도로 `MemberTestRepository`클래스에서 `Querydsl4RepositorySupport`를 상속받는다.

```java
@Repository
public class MemberTestRepository extends Querydsl4RepositorySupport {
    public MemberTestRepository() {
        super(Member.class);
    }

    public List<Member> basicSelect() {
        return select(member)
                .from(member)
                .fetch();
    }

    public List<Member> basicSelectFrom() {
        return selectFrom(member)
                .fetch();
    }

    public Page<Member> searchPageByApplyPage(MemberSearchCondition condition, Pageable pageable) {
        JPAQuery<Member> query = selectFrom(member)
								.leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );
        // Querydsl 유틸리티 클래스에서 제공해주는 applyPagination 사용
        List<Member> content = getQuerydsl().applyPagination(pageable, query)
                .fetch();

        return PageableExecutionUtils.getPage(content, pageable, query::fetchCount);
    }

    public Page<Member> applyPagination(MemberSearchCondition condition, Pageable pageable) {
        // custom applyPagination 사용
        return applyPagination(pageable, query -> query
                        .selectFrom(member)
                        .where(usernameEq(condition.getUsername()),
                                teamNameEq(condition.getTeamName()),
                                ageGoe(condition.getAgeGoe()),
                                ageLoe(condition.getAgeLoe())
                        )
        );
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

기본적으로 `Querydsl4RepositorySupport` 에 있는 메서드를 사용하여 기능들을 구현한다.

특히, searchPageByApplyPage 메서드와 applyPagination 메서드는 완전히 동일한 기능을 수행한다.

```java
protected <T> Page<T> applyPagination(Pageable pageable,
                                          Function<JPAQueryFactory, JPAQuery> contentQuery) {
        JPAQuery jpaQuery = contentQuery.apply(getQueryFactory());
        List<T> content = getQuerydsl().applyPagination(pageable,
                jpaQuery).fetch();
        return PageableExecutionUtils.getPage(content, pageable,
                jpaQuery::fetchCount);
    }
```

`Querydsl4RepositorySupport` 에서 구현한 `applyPagination` 메서드에서 다음과 같이 Querydsl 유틸리티 클래스를 잘 사용할 수 있도록 구현해 두었다.

이렇게 되면 우리가 사용하는 코드를 훨씬 깔끔하게 작성할 수 있다.

> 참고 : applyPagination의 두번째 파라미터로 자바 8의 Function을 이용하였다. 그래서 해당 파라미터에 `apply`를 실행하면 우리가 넘겨준 쿼리가 실행되게 된다.
> 

> 참고 : 자바 8의 Function, 즉 람다를 쓰게되면서 예전에는 템플릿 메서드 패턴으로 풀던 거를 훨씬 깔끔하게 풀어낼 수 있는 방법을 제공해주고 있다.
> 

카운트 쿼리를 최적화하기 위해 applyPagination의 마지막 파라미터에 countQuery를 추가할 수 있다.

```java
public Page<Member> applyPagination2(MemberSearchCondition condition, Pageable pageable) {
    // custom applyPagination 사용
    return applyPagination(pageable, contentQuery -> contentQuery
            .selectFrom(member)
            .where(usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())
            ), countQuery -> countQuery // 카운트 쿼리 추가
            .select(member.id)
            .from(member)
            .leftJoin(member.team, team)
            .where(usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())
            )
    );
}
```