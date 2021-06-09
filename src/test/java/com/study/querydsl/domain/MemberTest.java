package com.study.querydsl.domain;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.core.types.dsl.Expressions.constant;
import static com.study.querydsl.domain.QMember.*;
import static com.study.querydsl.domain.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void testEntity(){
        //given
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("TeamA");
        Team teamB = new Team("TeamB");
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
        //when
        em.flush();
        em.clear();
        List<Member> members = em.createQuery("select m from Member m", Member.class).getResultList();
        //then
        for (Member member : members){
            System.out.println("member = " + member);
            System.out.println("member.team = " + member.getTeam());
        }
    }

    @Test
    @Rollback(value = false)
    void startJPQL(){
        //given
        Member member = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        //when
        //then
        assertEquals("member1", member.getUsername());
    }

    @Test
    void startQuerydsl(){
        //given
        QMember m = new QMember("m");

        //when
        Member member = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();
        //then
        assertEquals("member1", member.getUsername());
    }

    @Test
    void startQuerydslCleanVersion(){
        //given
        //when
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        //then
        assertEquals("member1", findMember.getUsername());
    }

    @Test
    void sort(){
        //given
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        //then
        assertEquals("member5", member5.getUsername());
        assertEquals("member6", member6.getUsername());
        assertNull(memberNull.getUsername());
    }

    @Test
    void paging1(){
        //given

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.asc())
                .offset(0) // 0부터 시작
                .limit(3)
                .fetch();
        Member member1 = result.get(0);
        //then
        assertEquals("member1", member1.getUsername());
        assertEquals(3, result.size());
    }

    @Test
    void paging2(){
        //given

        //when
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(0)
                .limit(2)
                .fetchResults();

        //then
        assertEquals(4, queryResults.getTotal());
        assertEquals(2, queryResults.getLimit());
        assertEquals(0, queryResults.getOffset());
        assertEquals(2, queryResults.getResults().size());
    }

    @Test
    void aggregation(){
        //given

        //when
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
        //then
        Tuple tuple = result.get(0);
        assertEquals(4, tuple.get(member.count()));
        assertEquals(100, tuple.get(member.age.sum()));
        assertEquals(25, tuple.get(member.age.avg()));
        assertEquals(40, tuple.get(member.age.max()));
        assertEquals(10, tuple.get(member.age.min()));
    }
    
    @Test
    void group(){
        //given
        
        //when
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        //then
        assertEquals("TeamA" ,teamA.get(team.name));
        assertEquals(15, teamA.get(member.age.avg()));
        assertEquals("TeamB" ,teamB.get(team.name));
        assertEquals(35, teamA.get(member.age.avg()));
    }

    @Test
    void having(){
        //given

        //when
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .having(member.age.avg().gt(20))
                .fetch();

        Tuple teamB = result.get(0);
        //then
        assertEquals("TeamB", teamB.get(team.name));
        assertEquals(35, teamB.get(member.age.avg()));
    }

    @Test
    void join(){
        //given

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("TeamA"))
                .fetch();
        //then
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    @Test
    void thetaJoin(){
        //given
        em.persist(new Member("TeamA"));
        em.persist(new Member("TeamB"));
        //when
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        //then
        Member memberA = result.get(0);
        Member memberB = result.get(1);

        assertEquals("TeamA", memberA.getUsername());
        assertEquals("TeamB", memberB.getUsername());
    }

    @Test
    @DisplayName("예) 회원과 팀을 조인하면서, 팀 이름이 TeamA인 팀만 조인 하고 회원은 모두 조회한다.")
    void joinOnFiltering(){
        //given

        //when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("TeamA"))
                .fetch();
        //then
        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }

    @Test
    @DisplayName("연관관계가 없는 엔터티 외부 조인으로 회원의 이름과 팀 이름이 같은 회원을 조인한다.")
    void joinOnNoRelation(){
        //given
        em.persist(new Member("TeamA"));
        em.persist(new Member("TeamB"));
        em.persist(new Member("TeamC"));
        //when
        List<Tuple> result = queryFactory
                .select(member,team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    @DisplayName("패치조인을 사용하지 않고 데이터를 가지고 오는 방법이다.")
    void fetchJoinNo(){
        //given
        em.flush();
        em.clear();
        //when
        Member member = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1"))
                .fetchOne();
        //then
        boolean isLoaded = emf.getPersistenceUnitUtil().isLoaded(member.getTeam());
        assertEquals(false, isLoaded);
    }

    @Test
    @DisplayName("패치조인을 사용해서 데이터를 가지고 오는 방법이다.")
    void fetchJoinYes(){
        //given
        em.flush();
        em.clear();
        //when
        Member findMember = queryFactory
                .selectFrom(QMember.member)
                .join(member.team, team).fetchJoin()
                .where(QMember.member.username.eq("member1"))
                .fetchOne();
        //then
        boolean isLoaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertEquals(true, isLoaded);
    }

    @Test
    @DisplayName("나이가 가장 많은 회원을 조회한다고 해보자.")
    void subQuery(){
        //given
        QMember qMember = new QMember("m"); // alias 가 중복되면 안되므로 QMember 를 만들어줘야 한다.

        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(qMember.age.max())
                                .from(qMember)

                ))
                .fetchOne();
        //then
        assertEquals(40, findMember.getAge());
    }

    @Test
    @DisplayName("나이가 평균 이상인 회원을 조회한다고 해보자.")
    void subQueryAvg(){
        //given
        QMember qMember = new QMember("m"); // alias 가 중복되면 안되므로 QMember 를 만들어줘야 한다.

        //when
        List<Member> findMembers = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(qMember.age.avg())
                                .from(qMember)

                ))
                .fetch();
        //then
        assertEquals(2, findMembers.size());
    }

    @Test
    @DisplayName("나이가 10인 회원을 조회한다고 해보자. - In 을 이용")
    void subQueryIn(){
        //given
        QMember qMember = new QMember("m"); // alias 가 중복되면 안되므로 QMember 를 만들어줘야 한다.

        //when
        List<Member> findMembers = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(qMember.age)
                                .from(qMember)
                                .where(qMember.age.in(10))
                ))
                .fetch();
        //then
        assertEquals(1, findMembers.size());
        assertEquals(10, findMembers.get(0).getAge());
    }

    @Test
    void baseCase(){
        //given

        //when
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        //then
        for (String s : result) {
            System.out.println(s);
        }
    }

    @Test
    void complexCase(){
        //given

        //when
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        //then
        for (String s : result){
            System.out.println(s);
        }
    }

    @Test
    @DisplayName("0 ~ 30 살이 아닌 사람을 가장 먼저 출력하고 그 다음 0 ~ 20살, 그 다음 21살 ~ 30살 출력한다.")
    void complexCase2(){
        //given
        NumberExpression<Integer> rankCase = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);
        //when
        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankCase)
                .from(member)
                .orderBy(rankCase.desc())
                .fetch();
        //then
        for(Tuple tuple : result) {
            System.out.println(tuple);
        }
    }

    @Test
    void addConstant(){
        //given
        //when
        List<Tuple> result = queryFactory
                .select(member.username, constant("A"))
                .from(member)
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }

    @Test
    void addConcat(){
        //given

        //when
        String result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        //then
        System.out.println(result);
    }
}