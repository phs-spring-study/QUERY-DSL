# section 4. 중급 문법

# 1. 프로젝션과 결과 반환 - 기본

프로젝션 → select로 뭘 가져올지 대상을 지정한다.

## 프로젝션 대상이 하나일 때

- 프로젝션 대상이 하나면 타입을 명확하게 지정할 수 있다.
- 프로젝션 대상이 둘 이상이면 튜플이나 DTO로 조회한다.

```java
@Test
public void simpleProjection() {
    List<String> result = queryFactory
            .select(member.username)
            .from(member)
            .fetch();

    for (String s : result) {
        System.out.println("s = " + s);
    }
}
```

![image](https://user-images.githubusercontent.com/52458039/156155872-40ca8a66-e018-4a12-94d0-f33d91eb496a.png)


멤버 이름만 잘 가져온다.

## 튜플로 조회

- 프로젝션 대상이 둘 이상일때 사용한다.

튜플 : QueryDSL이 여러개를 조회할 때를 대비해서 만든 Type이다.

```java
@Test
public void tupleProjection() {
    List<Tuple> result = queryFactory
            .select(member.username, member.age)
            .from(member)
            .fetch();

    for (Tuple tuple : result) {
        String username = tuple.get(member.username);
        Integer age = tuple.get(member.age);
        System.out.println("username = " + username);
        System.out.println("age = " + age);
    }
}
```

![image](https://user-images.githubusercontent.com/52458039/156156017-bcdb65e0-2f1d-45fd-a163-db50e227eb13.png)

이름과 나이 2개를 잘 가져온다.

![image](https://user-images.githubusercontent.com/52458039/156156099-3dedc471-87c3-4782-a0d0-4284780e97ad.png)

이 튜플은 querydsl.core 패키지에 존재하는 인터페이스다.

⇒ 이 튜플을 repository 계층에서 쓰는건 괜찮은데, service, controller 계층까지 넘어서서 쓰는건 좋은 설계가 아님. 하부 구현 기술(ex: tuple) 을 비즈니스 로직이나 컨트롤러 단에서 아는건 좋지 않기 때문이다.

ex : jdbc의 결과인 ResultSet은 repository나 dao에서 계층에서 쓰도록하고, 나머지 계층에서는 의존이 없게 설계하는게 좋은 설계다. 그래야 나중에 하부 기술을 querydsl에서 다른 걸로 바꾸더라도 앞단인 컨트롤러나 비즈니스 로직을 바꿀 필요가 없기 때문이다. (스프링이 이런식의 설계를 유도하고 있다.)

**결론 : tuple도 결국은 querydsl에 종속적인 타입이기 때문에 repository 계층에서만 쓰고 다른 계층으로 넘길때는 dto로 변환하여 넘기는 것이 좋다.**

# 2. 프로젝션과 결과 반환 - DTO 조회

## **순수 JPA에서 DTO 조회 방법**

우선 MemberDto 클래스를 만들고 테스트 코드를 돌린다.

```java
@Data
public class MemberDto {
    private String username;
    private int age;

    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
```

```java
@Test
public void findDtoByJPQL() {
    List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
            .getResultList();

    for (MemberDto memberDto : result) {
        System.out.println("memberDto = " + memberDto);
    }
}
```

![image](https://user-images.githubusercontent.com/52458039/156156176-3808f5a5-cc1a-4574-9ff1-ce656ddc0bb9.png)

dto만 잘 조해해 오는 것을 확인할 수 있다.

- 순수 JPA에서 DTO를 조회할 때는 new 명령어를 사용해야 한다.
- DTO 의 package이름을 다 적어줘야해서 지저분하다.
- 생성자 방식만 지원한다.

## Querydsl 빈 생성(Bean population)

Querydsl에서 결과를 DTO로 반환할 때 사용한다.

다음의 3가지 방법을 지원한다.

- 프로퍼티 접근
- 필드 직접 접근
- 생성자 사용

### 1. 프로퍼티 접근 - Setter

```java
@Test
public void findDtoBySetter() {
    List<MemberDto> result = queryFactory
            .select(Projections.bean(MemberDto.class,
                    member.username,
                    member.age))
            .from(member)
            .fetch();

    for (MemberDto memberDto : result) {
        System.out.println("memberDto = " + memberDto);
    }
}
```

![image](https://user-images.githubusercontent.com/52458039/156156241-fe34e512-6c7a-448d-9820-bfe5fc743d8f.png)

queryDsl Projections.bean을 이용하여 setter 주입을 구현할 수 있다.

> MemberDto 기본생성자와 setter가 있어야 동작한다. Querydsl이 MemberDto를 기본생성자로 만들고 그 안의 필드 값들을 setter로 채워넣기 때문이다.
> 

> 참고 : MemberDto 클래스 애노테이션으로 `@Data`를 달았기 때문에 `toString` 메서드가 재정의 되어 필드값들이 출력된다.
> 

### 2. 필드 직접 접근

```java
@Test
public void findDtoByField() {
    List<MemberDto> result = queryFactory
            .select(Projections.fields(MemberDto.class,
                    member.username,
                    member.age))
            .from(member)
            .fetch();

    for (MemberDto memberDto : result) {
        System.out.println("memberDto = " + memberDto);
    }
}
```

![image](https://user-images.githubusercontent.com/52458039/156156326-8aef148a-748d-452c-afa3-fc1d7ba41163.png)

얘는 MemberDto에 getter / setter가 없어도 된다. 바로 필드에 주입해 준다.

> 참고 : MemberDto 필드가 private이지만 자바 라이브러리 리플렉션을 이용하여 주입할 수 있다.
> 

### 3. 생성자 사용

```java
@Test
public void findDtoByConstructor() {
    List<MemberDto> result = queryFactory
            .select(Projections.constructor(MemberDto.class,
                    member.username,
                    member.age))
            .from(member)
            .fetch();

    for (MemberDto memberDto : result) {
        System.out.println("memberDto = " + memberDto);
    }
}
```

![image](https://user-images.githubusercontent.com/52458039/156156370-cc1ddeb8-2990-4fdb-9543-f957a6bd0ec3.png)

Projections.constructor를 이용하여 생성자 파라미터의 타입만 잘 맞추면 생성자를 통해 DTO 필드를 주입할 수 있다.

MemberDto와 비슷하게 UserDto를 만들고, 필드이름을 username대신 name으로 정했다.

```java
@Data
public class UserDto {
    private String name;
    private int age;
}
```

```java
@Test
public void findUserDto() {
    List<UserDto> result = queryFactory
            .select(Projections.fields(UserDto.class,
                    member.username,
                    member.age))
            .from(member)
            .fetch();

    for (UserDto userDto : result) {
        System.out.println("userDto = " + userDto);
    }
}
```

![image](https://user-images.githubusercontent.com/52458039/156156414-a5ea3a57-abd6-4001-956f-2066b00d684e.png)

UserDto를 찾아오고 싶은데, Member의 username과 매칭이 안되서 가져오지 못한다.

```java
@Test
public void findUserDto() {
    List<UserDto> result = queryFactory
            .select(Projections.fields(UserDto.class,
                    member.username.as("name"),
                    member.age))
            .from(member)
            .fetch();

    for (UserDto userDto : result) {
        System.out.println("userDto = " + userDto);
    }
}
```

![image](https://user-images.githubusercontent.com/52458039/156156491-2cf2745c-66bb-4cb6-a43e-682abbb076ee.png)

그래서 다음과 같이 `.as(”name”)` 을 붙여서 별칭을 UserDto 필드와 같게 만들어 주면 값을 잘 가져오게 된다.

```java
@Test
public void findUserDto() {
    QMember memberSub = new QMember("memberSub");
    List<UserDto> result = queryFactory
            .select(Projections.fields(UserDto.class,
                    member.username.as("name"),

                    ExpressionUtils.as(JPAExpressions
                            .select(memberSub.age.max())
                            .from(memberSub), "age")))
            .from(member)
            .fetch();

    for (UserDto userDto : result) {
        System.out.println("userDto = " + userDto);
    }
}
```

![image](https://user-images.githubusercontent.com/52458039/156156536-92444664-aca6-40a2-8220-a761a4e20d27.png)

서브쿼리를 쓸때도 별칭을 만들어 줘야 하는데, (위 상황은 억지 쿼리긴 하지만, 예시를 위해 서브쿼리를 만들었음.) `Projections.fields` 에 서브쿼리를 쓰게되면 이름이 없기 때문에, 이때는 `ExpressionUtils.as` 를 사용해서 2번째 파라미터로 alias를 줄 수 있다.

```java
@Test
public void findUserDto() {
    QMember memberSub = new QMember("memberSub");
    List<UserDto> result = queryFactory
            .select(Projections.fields(UserDto.class,
                    ExpressionUtils.as(member.username, "name"),

                    ExpressionUtils.as(JPAExpressions
                            .select(memberSub.age.max())
                            .from(memberSub), "age")))
            .from(member)
            .fetch();

    for (UserDto userDto : result) {
        System.out.println("userDto = " + userDto);
    }
}
```

위 username을 `ExpressionUtils.as` 로 사용해도 된다.

⇒ 가독성 때문에 필드의 경우에는 바로 `.as`로 쓰는게 좋고, 서브쿼리를 쓸때는 방법이 없으므로 `ExpressionUtils.as` 를 사용한다.

UserDto를 생성자 방식으로 할 경우

```java
@Data
public class UserDto {
    private String name;
    private int age;

    public UserDto() {
    }

    public UserDto(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

```java
@Test
public void findDtoByConstructor() {
    List<UserDto> result = queryFactory
            .select(Projections.constructor(UserDto.class,
                    member.username,
                    member.age))
            .from(member)
            .fetch();

    for (UserDto userDto : result) {
        System.out.println("userDto = " + userDto);
    }
}
```

![image](https://user-images.githubusercontent.com/52458039/156156627-42997bd1-d8b2-4f17-9f4a-d6e8b7bed6db.png)

다음과 같이 생성자 파라미터 타입만 잘 맞춰준면 된다. (UserDto와 Member 필드 이름이 달라도 상관없다.)

# 3. 프로젝션과 결과 반환 - @QueryProjection

```java
@Data
@NoArgsConstructor
public class MemberDto {
    private String username;
    private int age;

    @QueryProjection
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
```

생성자 위에 `@QueryProjection` 을 붙인다.

![image](https://user-images.githubusercontent.com/52458039/156156708-2951d23a-8c1a-400a-bba5-62356cd850c2.png)

그리고 compileQuerydsl을 하면 (혹은 `./gradlew compileQuerydsl` 명령어)

![image](https://user-images.githubusercontent.com/52458039/156156759-fd5be684-6e3a-4e33-872c-01f6ccdd3962.png)

MemberDto도 Q파일로 생성이 된다. 여기에 있는 QMemberDto 생성자를 사용하게 된다.

```java
@Test
public void findDtoByQueryProjection() {
    List<MemberDto> result = queryFactory
            .select(new QMemberDto(member.username, member.age))
            .from(member)
            .fetch();

    for (MemberDto memberDto : result) {
        System.out.println("memberDto = " + memberDto);
    }
}
```

![image](https://user-images.githubusercontent.com/52458039/156156810-6024cf82-c421-44e5-9337-22e0c051e286.png)

얘는 생성자를 그대로 가져오기 때문에 타입과 그 갯수까지 정확히 맞춰 줘야 한다. (컴파일 시점에 타입이 안맞으면 오류 뜬다.) ⇒ 실제 MemberDto 생성자를 호출 해준다.

```java
Projections.constructor(UserDto.class,
	member.username,
	member.age,
	member.id) // 생성자에 없는 파라미터를 넣음. 하지만 컴파일 에러가 아님.
```

`Projections.constructor` 같은 경우에는 유저가 실제로 파라미터를 잘못 집어넣거나 파라미터갯수에 맞지않게 넣고 돌려도 컴파일 에러가 발생하지 않고 실행 시점에서 에러(런타임 오류)가 발생하게 된다.

실무에서는 컴파일 시점에 타입 체크, 갯수 체크 모두 되기 때문에 가장 안전한 방법이다. 

하지만 단점으로는 QMemberDto 파일을 만들어 줘야하고, MemberDto 생성자에 `@QueryProjection`애노테이션을 달아야 한다.

또 다른 문제는 **의존관계적인 문제**다. 기존의 MemberDto를 Querydsl에 대해 전혀 몰랐는데, `@QueryProjection` 를 붙이게 되는 순간 MemberDto 객체가 Querydsl에 대한 의존성을 가지게 된다. (DTO 특성상 서비스레이어, 컨트롤러 레이어에서 쓰게 되는데, QueryDsl에 의존을 가지게 되면 순수한 DTO를 유지하지 못한다.)

⇒ 아키텍쳐 적으로 DTO를 깔끔하게 가지고 가고싶다면 **앞선 방식의 bean, field, constructor를 쓰는게 맞다.** 반대로 실용적인 관점에서 이정도 의존은 그냥 가져갈 수 있다. Querydsl을 많이 쓰고, 하부 기술도 크게 바뀔것 같지 않다면 유연하게 사용해도 된다.

### distinct

```java
List<String> result = queryFactory
            .select(member.username).distinct()
            .from(member)
            .fetch();
```

→ distinct는 JPQL의 distinct와 같다.

# 4. 동적 쿼리 - BooleanBuilder 사용

## 동적 쿼리를 해결하는 두가지 방식

- BooleanBuilder
- Where 다중 파라미터 사용

먼저 BooleanBuilder 부터 살펴보자.

```java
@Test
public void dynamicQuery_BooleanBuilder() {
    // 동적 쿼리를 위한 검색 조건 값들
    String usernameParam = "member1";
    Integer ageParam = 10;

    List<Member> result = searchMember1(usernameParam, ageParam);
    assertThat(result.size()).isEqualTo(1);
}

private List<Member> searchMember1(String usernameCond, Integer ageCond) {
    // 파라미터 값이 null이냐 아니냐에 따라 쿼리가 동적으로 바뀌어야 하는 상황이다.

    BooleanBuilder builder = new BooleanBuilder();
    if (usernameCond != null) {
        builder.and(member.username.eq(usernameCond)); // BooleanBuilder에 and 조건을 넣어줌. (or 조건도 가능)
    }

    if (ageCond != null) {
        builder.and(member.age.eq(ageCond));
    }

    return queryFactory
            .selectFrom(member)
            .where(builder)
            .fetch();
}
```

![image](https://user-images.githubusercontent.com/52458039/156156874-67be5b31-c53b-4034-b8fb-7c4c29b2ebbc.png)

where에 and 조건이 잘 들어간 것을 확인할 수 있다.

만약 Integer ageParam = null; 로 설정하면

![image](https://user-images.githubusercontent.com/52458039/156156914-5f656458-7bb8-4205-aa4a-72e1b20141fc.png)

usernameParam 조건만 적용된다.

만약 member username을 필수조건으로 넣어야 한다면

```java
BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond));
```

생성자 파라미터로 초기값을 넣어주면 된다. (물론 넣기전에 usernameCond가 null 이 아니라는 방어코드를 넣어 줘야함.)

# 5. 동적 쿼리 - Where 다중 파라미터 사용

where 다중 파라미터를 사용하면 실무에서 깔끔하게 코드를 작성할 수 있다.

```java
@Test
public void dynamicQuery_WhereParam() {
    String usernameParam = "member1";
    Integer ageParam = 10;

    List<Member> result = searchMember2(usernameParam, ageParam);
    assertThat(result.size()).isEqualTo(1);
}

private List<Member> searchMember2(String usernameCond, Integer ageCond) {
    return queryFactory
            .selectFrom(member)
            .where(usernameEq(usernameCond), ageEq(ageCond))
            .fetch();
}

private Predicate usernameEq(String usernameCond) {
    return usernameCond != null ? member.username.eq(usernameCond) : null;
}

private Predicate ageEq(Integer ageCond) {
    return ageCond != null ? member.age.eq(ageCond) : null;
}
```

![image](https://user-images.githubusercontent.com/52458039/156156964-43a828f8-c224-4c10-9355-9214bf54d6eb.png)

기본적으로 where 파라미터에 조건이 여러개 들어가면 and 절로 이어지는데, 그 중 하나가 null이 들어가면 조건에서 무시된다. ⇒ 동적 쿼리 사용이 가능해진다.

usernameEq, ageEq 등 메서드가 많아지지만 주요 쿼리인 searchMember2 함수의 가독성을 높일 수 있다. (BooleanBuilder를 썼다면 유효성 처리를 하는 로직 뒤에 메인 쿼리가 나오기 때문에 가독성이 떨어진다.)

만약 ageCond가 null이라면 

![image](https://user-images.githubusercontent.com/52458039/156157016-d4b06ead-dda8-46ca-a9ef-6ba2fd995c30.png)

age조건을 제외한 username 조건만 들어가게 된다.

이 where절을 이용하여 얻을 수 있는 또다른 장점은 usernameEq와 ageEq이 메서드로 빠졌기 때문에 이를 통해 조건을 조합할 수 있다.

```java
private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

private BooleanExpression ageEq(Integer ageCond) {
    return ageCond != null ? member.age.eq(ageCond) : null;
}

private Predicate allEq(String usernameCond, Integer ageCond) {
    return usernameEq(usernameCond).and(ageEq(ageCond));
}

private List<Member> searchMember2(String usernameCond, Integer ageCond {
      return queryFactory
              .selectFrom(member)
//                .where(usernameEq(usernameCond), ageEq(ageCond))
              .where(allEq(usernameCond, ageCond))
              .fetch();
  }
```

> allEq처럼 여러 메서드로 빼놓은 조건을 조합할 경우에는 뺴놓은 메서드의 반환타입이 `Predicate`가 아니라 `BooleanExpression`여야 한다.
> 

**장점**

- 자바 코드기 때문에 composition으로 조건들을 합칠 수 있다.
- 조건 메서드로 뽑았기 때문에 다른 쿼리의 조건에 재사용할 수 있다.
- 쿼리 자체의 가독성이 높아진다.

**단점**

- null 처리를 주의해서 체크해 줘야 한다.

# 6. 수정, 삭제 벌크 연산

## 수정, 삭제 배치 쿼리

### 쿼리 한번으로 대량 데이터 수정

```java
@Test
@Commit // 트랜잭션 커밋하여 db에 적용함.
public void bulkUpdate() {

    // member1 = 10 -> 비회원
    // member2 = 20 -> 비회원
    // member3 = 30 -> 유지
    // member4 = 40 -> 유지

    // count는 영향을 받은 회원수가 카운트 된다.
    long count = queryFactory
            .update(member)
            .set(member.username, "비회원")
            .where(member.age.lt(28))
            .execute();
}
```

![image](https://user-images.githubusercontent.com/52458039/156157096-157f4361-c46c-4f56-adde-c97a87c2a378.png)

기대한 대로 멤버 나이가 10, 20인 멤버만 이름이 “비회원”으로 바뀌게 된다.

이 bulk 연산은 항상 조심해야 될 게 있다. bulk 연산은 영속성 컨텍스트와 상관없이 db에 커밋을 해버리기 때문에, **영속성 컨텍스트 데이터와 db의 데이터가 달라지게 된다. (JPQL 배치와 동일한 현상이다.)**

```java
@Test
@Commit // 트랜잭션 커밋하여 db에 적용함.
public void bulkUpdate() {

    // Bulk 쿼리 실행 전
    // pk 1 : member1 = 10 -> pk 1 : [DB] member1
    // pk 2 : member2 = 20 -> pk 2 : [DB] member2
    // pk 3 : member3 = 30 -> pk 3 :[DB] member3
    // pk 4 : member4 = 40 -> pk 4 : [DB] member4

    // count는 영향을 받은 회원수가 카운트 된다.
    long count = queryFactory
            .update(member)
            .set(member.username, "비회원")
            .where(member.age.lt(28))
            .execute();

    // Bulk 쿼리 실행 후
    // pk 1 : member1 = 10 -> pk 1 : [DB] 비회원 (영속성 컨텍스트는 member1으로 남아 있음)
    // pk 2 : member2 = 20 -> pk 2 : [DB] 비회원 (영속성 컨텍스트는 member2으로 남아 있음)
    // pk 3 : member3 = 30 -> pk 3 : [DB] member3
    // pk 4 : member4 = 40 -> pk 4 : [DB] member4

    List<Member> result = queryFactory
            .selectFrom(member)
            .fetch();   

    for (Member member1 : result) {
        System.out.println("member1 = " + member1);
    }
}
```

![image](https://user-images.githubusercontent.com/52458039/156157146-306d7410-d5f6-4cdc-9643-dfed7d7ac127.png)

select 쿼리 실행 후 => db에서 데이터를 가져온 뒤, JPA가 기본적으로 영속성 컨텍스트에 넣어준다. 

근데 pk 1번을 db에서 가져오면 "비회원"인데, 영속성 컨텍스트에 넣으려고 헀는데, 이미 member1이 있다. 이때 JPA는 db에서 데이터를 가져왔어도 영속성 컨텍스트에 값이 있으면 db에서 가져온 값을 버린다. (영속성 컨텍스트가 항상 우선순위를 가진다.)

그래서 결국 select 쿼리가 실행되도 영속성 컨텍스트에 데이터가 반영되지 않기 때문에 실제 db 데이터와 값이 여전히 달라지게 된다.

> 참고 : 위 상황을 repeatable read 라고 한다.
> 

```java
@Test
@Commit
public void bulkUpdate() {

    long count = queryFactory
            .update(member)
            .set(member.username, "비회원")
            .where(member.age.lt(28))
            .execute();

    // 영속성 컨텍스트를 초기화 한다.
    em.flush();
    em.clear(); 

    List<Member> result = queryFactory
            .selectFrom(member)
            .fetch();

    for (Member member1 : result) {
        System.out.println("member1 = " + member1);
    }
}
```

![image](https://user-images.githubusercontent.com/52458039/156157215-ff0834a3-4f44-4495-909d-c7bf284f132d.png)

그래서 항상 벌크성 쿼리를 날릴때는 영속성 컨텍스트를 초기화를 하면 조회할때 db의 데이터가 영속성 컨텍스트에 셋팅되기 때문에 db와 데이터를 맞출 수 있다.

이렇게 값을 change하는 경우도 있지만, 기존 값을 더하거나 곱하고 싶을 때도 있다.

```java
@Test
public void bulkAdd() {
    long count = queryFactory
            .update(member)
            .set(member.age, member.age.add(1)) // 뺴고싶을 때는 - 붙이면 됨.
            .execute();
}
```

![image](https://user-images.githubusercontent.com/52458039/156157245-437be6d1-f559-4efe-8299-168290ee4485.png)

이렇게 set에 age가 더해지는 쿼리가 나가게 된다.

```java
@Test
public void bulkMultiply() {
    long count = queryFactory
            .update(member)
            .set(member.age, member.age.multiply(2)) // 곱하기는 multiply !
            .execute();
}
```

![image](https://user-images.githubusercontent.com/52458039/156157289-7e5a4640-bac0-4da4-9a1e-bd86e606fcae.png)

곱하기도 마찬가지로 update 쿼리로 잘 나간다.

또, 정책상 나이가 18 초과인 회원을 삭제하고 싶을때

```java
@Test
public void bulkDelete() {
    long count = queryFactory
            .delete(member)
            .where(member.age.gt(18))
            .execute();
}
```

![image](https://user-images.githubusercontent.com/52458039/156157370-1a2d0410-9aec-4479-8bef-e7e8e7b5f1d7.png)

delete 쿼리가 잘 나간다.

# 7. SQL function 호출하기

SQL function은 JPA와 같이 Dialect에 등록된 내용만 호출할 수 있다.

예시 : member → M으로 변경하는 replace 함수 사용 예시

```java
@Test
public void sqlFunction() {
    List<String> result = queryFactory
            .select(Expressions.stringTemplate(
                    "function('replace', {0}, {1}, {2})",
                    member.username, "member", "M"))
            .from(member)
            .fetch();

    for (String s : result) {
        System.out.println("s = " + s);
    }
}
```

![image](https://user-images.githubusercontent.com/52458039/156157431-5e5001c7-3311-4d00-a2c3-9ee531c85312.png)

sql function이 jpql와 sql에 잘 나가는 것을 확인할 수 있다.

[참고] : 각 데이터베이스 dialect에 다음과같이 registerFuncton 형태로 등록되어 있어야 사용이 가능하다. 만약 내가 임의로 db에서 함수를 만들고 싶다면 H2Dialect를 상속받는 얘를 만들어서 application.yml 파일에 직접 등록해서 써야 한다.

![image](https://user-images.githubusercontent.com/52458039/156157469-f062fbb6-6726-4a16-b42e-9228805a7c3e.png)

예시 : member username 소문자로 바꾸기

```java
@Test
public void sqlFunction2() {
    List<String> result = queryFactory
            .select(member.username)
            .from(member)
            .where(member.username.eq(
                    Expressions.stringTemplate("function('lower', {0})", member.username)))
            .fetch();

    for (String s : result) {
        System.out.println("s = " + s);
    }
}
```

![image](https://user-images.githubusercontent.com/52458039/156157565-e0a5511f-43de-4298-ad65-7df43268620e.png)

sql의 lower가 잘 호출되는 것을 확인할 수 있다.

⇒ 이런 소문자, 대문자 바꾸는 간단한 것들은(일반적으로 db에서 많이 쓰이는 ansi 표준 함수들) querydsl이 내장하고 있다. 따라서 아래와 같이 바꿔도 결과는 동일하다.

```java
@Test
public void sqlFunction2() {
    List<String> result = queryFactory
            .select(member.username)
            .from(member)
            .where(member.username.eq(member.username.lower())) // querydsl 내장
            .fetch();

    for (String s : result) {
        System.out.println("s = " + s);
    }
}
```