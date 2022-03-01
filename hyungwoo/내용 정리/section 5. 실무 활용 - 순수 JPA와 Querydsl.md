# section 5. 실무 활용 - 순수 JPA와 Querydsl

# 1. 순수 JPA 리포지토리와 Querydsl

우선 repository를 만든다.

```java
@Repository
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findALl() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = : username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }
}
```

다음으로 위 클래스의 테스트 코드를 만든다.

```java
@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberJpaRepository.findALl();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberJpaRepository.findByUsername("member1");
        assertThat(result2).containsExactly(member);
    }
}
```

MemberJpaRepository 메서드를 querydsl로도 만들 수 있다.

```java
// 순수 JPA
public List<Member> findALl() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
}

// QueryDsl
public List<Member> findAll_Querydsl() {
    return queryFactory
            .selectFrom(member)
            .fetch();
}

// 순수 JPA
public List<Member> findByUsername(String username) {
    return em.createQuery("select m from Member m where m.username = : username", Member.class)
            .setParameter("username", username)
            .getResultList();
}

// QueryDsl
public List<Member> findByUsername_Querydsl(String username) {
    return queryFactory
            .selectFrom(member)
            .where(member.username.eq(username))
            .fetch();
}
```

테스트 코드에서 방금 만든 querydsl 메서드로 대체를 해도 테스트가 통과하게 된다.

```java
@Test
public void basicTest() {
    Member member = new Member("member1", 10);
    memberJpaRepository.save(member);

    Member findMember = memberJpaRepository.findById(member.getId()).get();
    assertThat(findMember).isEqualTo(member);

    List<Member> result1 = memberJpaRepository.findAll_Querydsl(); // 대체
    assertThat(result1).containsExactly(member);

    List<Member> result2 = memberJpaRepository.findByUsername_Querydsl("member1"); // 대체
    assertThat(result2).containsExactly(member);
}
```

참고

```java
@Repository
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }
		...
}
```

EntityManager를 스프링이 주입해주는데, 이때 동시성 문제가 생길 수 있지 않을까 고민할 수 있다. (같은 객체를 모든 스레드가 동일하게 쓰니까..)

근데 동시성 문제는 고민하지 않아도 된다. 왜냐하면 스프링이 주입해주는 EntityManager는 프록시용 가짜 엔티티 매니저이기 때문이다. 이 가짜 엔티티 매니저는 실제로 사용 시점에, `트랜잭션 단위`로 실제 엔티티 매니저(영속성 컨텍스트)를 할당해 준다.

# 2. 동적 쿼리와 성능 최적화 조회 - Builder 사용

우선 조회 최적화용 DTO 클래스를 만든다.

```java
@Data
public class MemberTeamDto {

    private Long memberId;
    private String username;
    private int age;
    private Long teamId;
    private String teamName;

    @QueryProjection
    public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}
```

`@QueryProjection`을 사용할 거기 때문에 잊지말고 compileQUerydsl을 해준다.

![image](https://user-images.githubusercontent.com/52458039/156157949-31fcfe7e-e640-4f64-a09f-841804246b6f.png)

다음으로, 멤버와 팀의 검색조건을 위한 클래스를 만든다.

```java
@Data
public class MemberSearchCondition { // 이름이 너무 길면 MemberCond 등으로 줄여 사용해도 된다.
    //회원명, 팀명, 나이(ageGoe, ageLoe)

    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;
}
```

다음으로 이 검색 조건을 이용한 querydsl 메서드를 작성한다.

```java
@Repository
public class MemberJpaRepository {

    ...

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        if (hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }
        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getUsername()));
        }
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }
        
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .fetch();
    }
}
```

아래와 같이 테스트 코드를 작성하여 위 메서드 동작 테스트를 해볼 수 있다.

```java
@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

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
        condition.setTeamName("teamB"); // 동적 쿼리 조건 3가지.

        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);

        assertThat(result).extracting("username").containsExactly("member4");
    }

}
```

![image](https://user-images.githubusercontent.com/52458039/156158009-ab3aa72a-8bd9-4dcd-976b-45ae3fbb3ddb.png)

동적 쿼리로 3가지 builder조건이 쿼리에 잘 들어간 것을 확인할 수 있다.

동적 쿼리가 잘 동작하는지 여러 조건을 바꿔서 테스트 해보면

```java
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
    condition.setTeamName("teamB"); // 동적 쿼리 조건 1개

    List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);

    assertThat(result).extracting("username").containsExactly("member3","member4");
}
```

![image](https://user-images.githubusercontent.com/52458039/156158042-e8da0200-e702-4334-97f3-fffee7703e4e.png)

조건 1개만 잘 동적으로 처리가 된다.

여기서 주의해야할 점은 condition조건이 없을 경우, 쿼리가 데이터를 다 끌고 온다. 그래서 만약 db에 데이터가 3만개가 있다면 3만개를 모두다 끌고오게된다..

그래서 동적쿼리 짤때는 왠만하면 기본조건이나 limit이 있는게 좋다.

# 3. 동적 쿼리와 성능 최적화 조회 - Where절 파라미터 사용

where절을 이용한 동적쿼리를 작성해보자.

```java
@Repository
public class MemberJpaRepository {

    ...

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
        return ageLoe != null ? member.age.goe(ageLoe) : null;
    }
}
```

builder를 사용하면 search 메서드에서 쿼리를 보기 전에 유효성 검사때문에 보기 힘들었는데, where절 파라미터를 이용하면 유효성 검사를 메서드로 빼서 하기 떄문에 바로 쿼리가 어떻게 나가는지 볼 수있어 가시성이 좋다.

테스트는 위와 동일한 테스트에다가 지금의 search 메서드를 사용하면 통과하게 된다.

메서드로 뺐기 때문에 재사용도 가능하다. 

이게 where절 파라미터의 진짜 장점이다.

```java
public List<Member> searchMember(MemberSearchCondition condition) {
    return queryFactory
            .selectFrom(member)
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
```

MemberTeamDto가 아니라, 엔티티인 Member를 가져온다고 했을때, select의 프로젝션은 달라지더라도, where절의 파라미터에서 우리가 만든 메서드를 재활용 할 수 있게 된다. (메서들 끼리의 조합을 만들어서 사전에 isValid한 것들을 미리 만들어 둘 수도 있다.)

# 4. 조회 API 컨트롤러 개발

데이터를 표현하기 위해 샘플 데이터를 추가한다.

샘플 데이터를 추가할 때, 테스트 케이스 실행에 영향을 주지 않도록 프로파일 설정을 아래와 같이 분리한다.

### 프로파일 설정

`application.yml`

```yaml
spring:
  profiles:
    active: local
  datasource:
    url: jdbc:h2:tcp://localhost/~/querydsl
    username: sa
    password:
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: create
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
logging.level:
  org.hibernate.SQL: debug

```

`application.yml[test]`

```yaml
spring:
  profiles:
    active: test
  datasource:
    url: jdbc:h2:tcp://localhost/~/querydsl
    username: sa
    password:
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: create
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
logging.level:
  org.hibernate.SQL: debug
```

그리고 샘플데이터를 추가한다.

```java
@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;

    @PostConstruct
    public void init() {
        initMemberService.init();
    }

    @Component
    static class InitMemberService {

        @PersistenceContext
        private EntityManager em;

        @Transactional
        public void init() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i < 100; i++) {
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                em.persist(new Member("member" + i, i, selectedTeam));
            }
        }

    }
}
```

이때 InitMemberService의 init() 메서드 내용을 `@PostConstruct`에 사용하고싶지만 스프링의 라이프 사이클에서 `@PostConstruct`와 `@Transactional`을 같이 사용할 수 없기 때문에 static class로 구분하여 작성하였다.

그리고 멤버 조회 api를 추가한다.

```java
@RestController
@RequiredArgsConstructor
public class MemberController {
    private final MemberJpaRepository memberJpaRepository;

    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) {
        return memberJpaRepository.search(condition);
    }
}
```

![image](https://user-images.githubusercontent.com/52458039/156158103-3adf8720-3061-4203-a8a7-825a3b9c5fd1.png)

우리가 미리 만들어둔 search 검색 조건이 잘 적용되는 것을 확인할 수 있다.