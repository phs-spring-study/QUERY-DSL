package study.hoomin.querydsl;

import static org.assertj.core.api.Assertions.*;
import static study.hoomin.querydsl.entity.QMember.*;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.hoomin.querydsl.entity.Member;
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
}
