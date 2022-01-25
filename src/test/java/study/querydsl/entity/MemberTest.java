package study.querydsl.entity;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;

@SpringBootTest
@Transactional
public class MemberTest {
    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @Test

    public void testEntity() {
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
        //초기화
        em.flush();
        em.clear();
        //확인
        List<Member> members = em.createQuery("select m from Member m",
                        Member.class)
                .getResultList();
        for (Member member : members) {
            System.out.println("member=" + member);
            System.out.println("-> member.team=" + member.getTeam());
        }
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
        Member findMember = queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1")
                .and(member.age.eq(10)))
            .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        List<Member> result1 = queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1"),
                member.age.eq(10))
            .fetch();
        assertThat(result1.size()).isEqualTo(1);
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
        //count 쿼리로 변경
        long count = queryFactory
            .selectFrom(member)
            .fetchCount();
    }

}