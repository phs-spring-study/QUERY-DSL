# section 1. 프로젝트 환경설정

# 1. Querydsl 설정과 검증

## * 스프링 부트 2.6 이상, Querydsl 5.0 지원

최신 스프링 부트 2.6부터는 Querydsl 5.0을 사용한다.

스프링 부트 2.6 이상 사용시 다음과 같은 부분을 확인 해야 함.

1. `build.gradle` 설정 변경
2. `PageableExecutionUtils` ⇒ Deprecated(향후 미지원) 패키지 변경
3. Querydsl `fetchResults()`, `fetchCount()` ⇒ Deprecated(향후 미지원)

우선 검증용 엔티티를 생성한다.

```java
@Entity
@Getter @Setter
public class Hello {
    @Id @GeneratedValue
    private Long id;
}
```

`build.gradle`

```jsx
//querydsl 추가
buildscript {
	ext {
		queryDslVersion = "5.0.0"
	}
}

plugins {
	...
	//querydsl 추가
	id "com.ewerk.gradle.plugins.querydsl" version "1.0.10"
	...
}

...
  
dependencies {
	...   
	//querydsl 추가
	implementation 'com.querydsl:querydsl-jpa'
	...
}  

//querydsl 추가 시작
def querydslDir = "$buildDir/generated/querydsl"
querydsl {
    jpa = true

       querydslSourcesDir = querydslDir
  }
  sourceSets {
      main.java.srcDir querydslDir
  }
  configurations {
      querydsl.extendsFrom compileClasspath
  }
  compileQuerydsl {
      options.annotationProcessorPath = configurations.querydsl
}
//querydsl 추가 끝
```

다음과 같이 build.gradle 내용에 queryDsl내용을 추가하면

> 참고 : 위와 같이 querydslDir을 지정하여 컴파일 시에 어노테이션 프로세서와 클래스 패스들을 등록하고 QClass를 만들어 주는 역할을 그래들의 groovy 스크립트 언어가 해준다. ⇒ 인텔리제이 버전, 스프링 버전에 따라 달라지고, 멀티모듈로 들어가면 또 그때의 설정이 달라지므로 이때는  구글링하며 삽질을 해봐야 함.
> 

![Untitled](section%201%20%E1%84%91%E1%85%B3%E1%84%85%E1%85%A9%E1%84%8C%E1%85%A6%E1%86%A8%E1%84%90%E1%85%B3%20%E1%84%92%E1%85%AA%E1%86%AB%E1%84%80%E1%85%A7%E1%86%BC%E1%84%89%E1%85%A5%E1%86%AF%E1%84%8C%E1%85%A5%E1%86%BC%20eb197e2162354d26aa1512f6c41fdcf9/Untitled.png)

외부 라이브러리로 등록된다.

![Untitled](section%201%20%E1%84%91%E1%85%B3%E1%84%85%E1%85%A9%E1%84%8C%E1%85%A6%E1%86%A8%E1%84%90%E1%85%B3%20%E1%84%92%E1%85%AA%E1%86%AB%E1%84%80%E1%85%A7%E1%86%BC%E1%84%89%E1%85%A5%E1%86%AF%E1%84%8C%E1%85%A5%E1%86%BC%20eb197e2162354d26aa1512f6c41fdcf9/Untitled%201.png)

그다음 gradle의 other에서 compileQuerydsl를 눌러서 컴파일 해준다.

![Untitled](section%201%20%E1%84%91%E1%85%B3%E1%84%85%E1%85%A9%E1%84%8C%E1%85%A6%E1%86%A8%E1%84%90%E1%85%B3%20%E1%84%92%E1%85%AA%E1%86%AB%E1%84%80%E1%85%A7%E1%86%BC%E1%84%89%E1%85%A5%E1%86%AF%E1%84%8C%E1%85%A5%E1%86%BC%20eb197e2162354d26aa1512f6c41fdcf9/Untitled%202.png)

그럼 우리가 build.gradle에서 작성한 스크립트에서, queryDsl에대한 폴더가 지정된 위치에 잘 생성된다.

![Untitled](section%201%20%E1%84%91%E1%85%B3%E1%84%85%E1%85%A9%E1%84%8C%E1%85%A6%E1%86%A8%E1%84%90%E1%85%B3%20%E1%84%92%E1%85%AA%E1%86%AB%E1%84%80%E1%85%A7%E1%86%BC%E1%84%89%E1%85%A5%E1%86%AF%E1%84%8C%E1%85%A5%E1%86%BC%20eb197e2162354d26aa1512f6c41fdcf9/Untitled%203.png)

그리고 이렇게 QHello라는 자바 클래스가 만들어 지게 된다.

QueryDsl이 Hello라는 엔티티를 보고 QHello라는 엔티티를 만들어 준거다.

(위처럼 compileQuerydsl을 누르지 않더라도 build할때 포함 되있으므로 위와 같은 파일을 만들어 줌)

![Untitled](section%201%20%E1%84%91%E1%85%B3%E1%84%85%E1%85%A9%E1%84%8C%E1%85%A6%E1%86%A8%E1%84%90%E1%85%B3%20%E1%84%92%E1%85%AA%E1%86%AB%E1%84%80%E1%85%A7%E1%86%BC%E1%84%89%E1%85%A5%E1%86%AF%E1%84%8C%E1%85%A5%E1%86%BC%20eb197e2162354d26aa1512f6c41fdcf9/Untitled%204.png)

⇒ 인텔리제이는 자동으로 프로젝트 구조에 자동으로 빌드해서 넣어줌.

이제 QueryDsl이 generated해준 QClass 코드로 쿼리를 작성하게 된다.

> 참고 : genreated된 QClass는 git으로 관리하면 안됨. ⇒ .gitignore에 추가해야 함.
> 

지금은 build폴더 안에 만들어지기 때문에 이미 gitignore에 적용되어 있어서 따로 설정해줄 필요는 없음.

```java
@SpringBootTest
@Transactional
class QuerydslApplicationTests {

	@Autowired
//	@PersistenceContext // 자바 표준 (spring 아닐 때 사용)
	EntityManager em;

	@Test
	void contextLoads() {
		Hello hello = new Hello();
		em.persist(hello);

		JPAQueryFactory query = new JPAQueryFactory(em);
		QHello qHello = new QHello("h");
		//QHello qHello = QHello.hello; 이렇게 써도 됨.

		// querydsl 사용할 때 쿼리와 관련된 것들은 Q-type을 사용해야 함.
		Hello result = query
				.selectFrom(qHello)
				.fetchOne();

		assertThat(result).isEqualTo(hello);
		assertThat(result.getId()).isEqualTo(hello.getId());
	}
}
```

다음과 같이 테스트 코드를 작성하여 querydsl이 정상 작동하는지 확인하면 된다.

# 2. 라이브러리 살펴보기

![Untitled](section%201%20%E1%84%91%E1%85%B3%E1%84%85%E1%85%A9%E1%84%8C%E1%85%A6%E1%86%A8%E1%84%90%E1%85%B3%20%E1%84%92%E1%85%AA%E1%86%AB%E1%84%80%E1%85%A7%E1%86%BC%E1%84%89%E1%85%A5%E1%86%AF%E1%84%8C%E1%85%A5%E1%86%BC%20eb197e2162354d26aa1512f6c41fdcf9/Untitled%205.png)

querydsl.com에 접속하면 JPA외에도 SQL, Mongodb 등등 여러 모듈을 지원해 준다.

⇒ 쿼리들은 비슷하니까, 이걸 querydsl이 공통 적인 부분들은 묶어서 제공하고, 특정 모듈에 특화된 기능은 따로 라이브러리로 제공해서 준다.

![Untitled](section%201%20%E1%84%91%E1%85%B3%E1%84%85%E1%85%A9%E1%84%8C%E1%85%A6%E1%86%A8%E1%84%90%E1%85%B3%20%E1%84%92%E1%85%AA%E1%86%AB%E1%84%80%E1%85%A7%E1%86%BC%E1%84%89%E1%85%A5%E1%86%AF%E1%84%8C%E1%85%A5%E1%86%BC%20eb197e2162354d26aa1512f6c41fdcf9/Untitled%206.png)

그래서 querydsl-jpa ⇒ querydsl이 jpa에 특화된 라이브러리를 제공하는 라이브러리를 외부 라이브러리로 등록된 것이다.

## 쿼리 파라미터 로그 남기기

- 로그에 다음을 추가하기 `org.hibernate.type` : SQL 실행 파라미터를 로그로 남긴다.
- 외부 라이브러리 사용
    - `https://github.com/gavlyukovskiy/spring-boot-data-source-decorator`

스프링 부트를 사용하면 이 라이브러리만 추가하면 된다.

```groovy
implementation 'com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.5.8'
```

 

> 참고: 쿼리 파라미터를 로그로 남기는 외부 라이브러리는 시스템 자원을 사용하므로, 개발 단계에서는 편하게 사용해도 된다. 하지만 운영시스템에 적용하려면 꼭 성능테스트를 하고 사용하는 것이 좋다.
>