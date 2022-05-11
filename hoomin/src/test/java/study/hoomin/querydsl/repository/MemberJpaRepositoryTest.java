package study.hoomin.querydsl.repository;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import javax.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.jpa.impl.JPAQueryFactory;

import study.hoomin.querydsl.dto.MemberSearchCondition;
import study.hoomin.querydsl.dto.MemberTeamDto;
import study.hoomin.querydsl.entity.Member;
import study.hoomin.querydsl.entity.Team;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

	@Autowired
	EntityManager em;

	@Autowired
	MemberJpaRepository memberJpaRepository;

	@Test
	public void basicTest() {
		final Member member = new Member("member1", 10);
		memberJpaRepository.save(member);

		final Member findMember = memberJpaRepository.findById(member.getId()).get();
		assertThat(findMember).isEqualTo(member);

		final List<Member> result1 = memberJpaRepository.findAll_QueryDSL();
		assertThat(result1).containsExactly(member);

		final List<Member> result2 = memberJpaRepository.findByUsername_queryDSL("member1");
		assertThat(result2).containsExactly(member);
	}

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

		final List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
		assertThat(result).extracting("username").containsExactly("member4");
	}

}