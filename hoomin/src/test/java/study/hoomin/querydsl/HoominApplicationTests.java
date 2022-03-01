package study.hoomin.querydsl;

import static org.assertj.core.api.Assertions.*;
import static study.hoomin.querydsl.entity.QMember.*;
import static study.hoomin.querydsl.entity.QTeam.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.hoomin.querydsl.dto.MemberDto;
import study.hoomin.querydsl.entity.Member;
import study.hoomin.querydsl.entity.QMember;
import study.hoomin.querydsl.entity.QTeam;
import study.hoomin.querydsl.entity.Team;

@SpringBootTest
@Transactional
class HoominApplicationTests {

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
		//member1을 찾아라.
		String qlString =
			"select m from Member m " +
				"where m.username = :username";
		Member findMember = em.createQuery(qlString, Member.class)
			.setParameter("username", "member1")
			.getSingleResult();
		assertThat(findMember.getUsername()).isEqualTo("member1");
	}
	@Test
	public void startQuerydsl() {
		//member1을 찾아라.
		Member findMember = queryFactory
			.select(member)
			.from(member)
			.where(member.username.eq("member1"))//파라미터 바인딩 처리
			.fetchOne();
		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void search() {
		queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1")
				.and(member.age.eq(10)))
			.fetchOne();
	}

	@Test
	public void resultFetch() {
		//List
		List<Member> fetch = queryFactory
			.selectFrom(member)
			.fetch();
		//단 건
		Member findMember1 = queryFactory
			.selectFrom(member)
			.fetchOne();
		//처음 한 건 조회
		Member findMember2 = queryFactory
			.selectFrom(member)
			.fetchFirst();
		//페이징에서 사용
		QueryResults<Member> results = queryFactory
			.selectFrom(member)
			.fetchResults();

		results.getTotal();
		final List<Member> memberList = results.getResults();

		//count 쿼리로 변경
		long total = queryFactory
			.selectFrom(member)
			.fetchCount();
	}

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

	@Test
	public void aggregation() throws Exception {
		List<Tuple> result = queryFactory
			.select(member.count(),
				member.age.sum(),
				member.age.avg(),
				member.age.max(),
				member.age.min())
			.from(member)
			.fetch();
		Tuple tuple = result.get(0);
		assertThat(tuple.get(member.count())).isEqualTo(4);
		assertThat(tuple.get(member.age.sum())).isEqualTo(100);
		assertThat(tuple.get(member.age.avg())).isEqualTo(25);
		assertThat(tuple.get(member.age.max())).isEqualTo(40);
		assertThat(tuple.get(member.age.min())).isEqualTo(10);
	}

	@Test
	public void group() throws Exception {
		List<Tuple> result = queryFactory
			.select(team.name, member.age.avg())
			.from(member)
			.join(member.team, team)
			.groupBy(team.name)
			.fetch();
		Tuple teamA = result.get(0);
		Tuple teamB = result.get(1);
		assertThat(teamA.get(team.name)).isEqualTo("teamA");
		assertThat(teamA.get(member.age.avg())).isEqualTo(15);
		assertThat(teamB.get(team.name)).isEqualTo("teamB");
		assertThat(teamB.get(member.age.avg())).isEqualTo(35);
	}

	@Test
	public void join() throws Exception {
		List<Member> result = queryFactory
			.selectFrom(member)
			.join(member.team, team) // innerjoin, leftjoin
			.where(team.name.eq("teamA"))
			.fetch();
		assertThat(result)
			.extracting("username")
			.containsExactly("member1", "member2");
	}

	@Test
	public void theta_join() throws Exception {
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));
		List<Member> result = queryFactory
			.select(member)
			.from(member, team)
			.where(member.username.eq(team.name))
			.fetch();
		assertThat(result)
			.extracting("username")
			.containsExactly("teamA", "teamB");
	}

	@Test
	public void join_on_filtering() throws Exception {
		List<Tuple> result = queryFactory
			.select(member, team)
			.from(member)
			.leftJoin(member.team, team).on(team.name.eq("teamA"))
			.fetch();
		for (Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
	}

	@Test
	public void join_on_no_relation() throws Exception {
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));
		List<Tuple> result = queryFactory
			.select(member, team)
			.from(member)
			.leftJoin(team).on(member.username.eq(team.name))
			.fetch();
		for (Tuple tuple : result) {
			System.out.println("t=" + tuple);
		}
	}

	@PersistenceUnit
	EntityManagerFactory emf;
	@Test
	public void fetchJoinNo() throws Exception {
		em.flush();
		em.clear();
		Member findMember = queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1"))
			.fetchOne();
		boolean loaded =
			emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		assertThat(loaded).as("페치 조인 미적용").isFalse();
	}

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

	@Test
	public void subQuery() throws Exception {
		QMember memberSub = new QMember("memberSub");
		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.eq(
				JPAExpressions
					.select(memberSub.age.max())
					.from(memberSub)
			))
			.fetch();
		assertThat(result).extracting("age")
			.containsExactly(40);
	}

	@Test
	public void simpleProjection() {
		final List<String> fetch = queryFactory
			.select(member.username)
			.from(member)
			.fetch();

		final List<Member> fetch1 = queryFactory
			.select(member)
			.from(member)
			.fetch();
	}

	@Test
	public void tupleProjection() {
		final List<Tuple> fetch = queryFactory
			.select(member.username, member.age)
			.from(member)
			.fetch();

		for (Tuple tuple : fetch) {
			final String s = tuple.get(member.username);
			final Integer s2 = tuple.get(member.age);
		}
	}

	@Test
	public void findDtoByJPQL() {
		final List<MemberDto> resultList = em.createQuery(
			"select new study.hoomin.querydsl.dto.MemberDto(m.username, m.age) from Member  m", MemberDto.class)
			.getResultList();
	}

	@Test
	public void findDtoBySetter() {
		final List<MemberDto> fetch = queryFactory
			.select(Projections.bean(MemberDto.class,
				member.username,
				member.age))
			.from(member)
			.fetch();
	}

	@Test
	public void findDtoByField() {
		final List<MemberDto> fetch = queryFactory
			.select(Projections.fields(MemberDto.class,
				member.username.as("userName"),
				member.age))
			.from(member)
			.fetch();

		QMember memberSub = new QMember("memberSub");

		final List<MemberDto> fetch2 = queryFactory
			.select(Projections.fields(MemberDto.class,
				member.username.as("userName"),
				ExpressionUtils.as(
					JPAExpressions.select(memberSub.age.max())
						.from(memberSub), "age")
				)
			).from(member)
			.fetch();
	}

	@Test
	public void findDtoByConstructor() {
		final List<MemberDto> fetch = queryFactory
			.select(Projections.constructor(MemberDto.class,
				member.username,
				member.age))
			.from(member)
			.fetch();
	}
}
