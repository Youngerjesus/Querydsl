package com.study.querydsl.dto;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.Visitor;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.domain.Member;
import com.study.querydsl.domain.QMember;
import com.study.querydsl.domain.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static com.study.querydsl.domain.QMember.member;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberDtoTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void testEntity(){
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
    void projectionWithJpa(){
        //given

        //when
        List<MemberDto> result = em.createQuery(
                "select new com.study.querydsl.dto.MemberDto(m.username, m.age)" +
                        "from Member m", MemberDto.class
        )
                .getResultList();
        //then
        for (MemberDto memberDto : result){
            System.out.println(memberDto.toString());
        }
    }

    @Test
    void findDtoBySetter(){
        //given
        //when
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        //then
        for (MemberDto memberDto : result) {
            System.out.println(memberDto.toString());
        }
    }

    @Test
    void findDtoByField(){
        //given
        //when
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        //then
        for (MemberDto memberDto : result) {
            System.out.println(memberDto.toString());
        }
    }

    @Test
    void findDtoByConstructor(){
        //given
        //when
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        //then
        for (MemberDto memberDto : result) {
            System.out.println(memberDto.toString());
        }
    }

    @Test
    void findDtoByQueryProjection(){
        //given

        //when
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        //then
        for (MemberDto memberDto : result) {
            System.out.println(memberDto.toString());
        }
    }

    @Test
    void dynamicQueryUsingBooleanBuilder(){
        //given
        String usernameParam = "member1";
        Integer ageParam = 10;
        //when
        List<Member> result = searchMember1(usernameParam, ageParam);
        //then
        assertEquals(1, result.size());
    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {
        BooleanBuilder builder = new BooleanBuilder();
        if(usernameParam != null) {
            builder.and(member.username.eq(usernameParam));
        }

        if(ageParam != null) {
            builder.and(member.age.eq(ageParam));
        }


        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    void dynamicQueryUsingWhereParameter(){
        //given
        String usernameParam = "member1";
        Integer ageParam = 10;
        //when
        List<Member> result = searchMember2(usernameParam, ageParam);
        //then
        assertEquals(1, result.size());
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private Predicate usernameEq(String usernameCond) {
        if(usernameCond == null) return null;

        return member.username.eq(usernameCond);
    }

    private Predicate ageEq(Integer ageCond) {
        if(ageCond == null) return null;

        return member.age.eq(ageCond);
    }

    @Test
    void dynamicQueryUsingWhereParameter2(){
        //given
        String usernameParam = "member1";
        Integer ageParam = 10;
        //when
        List<Member> result = searchMember3(usernameParam, ageParam);
        //then
        assertEquals(1, result.size());
    }

    private List<Member> searchMember3(String usernameParam, Integer ageParam) {
        return queryFactory
                .selectFrom(member)
                .where(allEq(usernameParam, ageParam))
                .fetch();
    }

    private BooleanExpression allEq(String usernameParam, Integer ageParam) {
        return usernameEq1(usernameParam).and(ageEq(ageParam));
    }

    private BooleanExpression usernameEq1(String usernameCond) {
        if(usernameCond == null) return null;

        return member.username.eq(usernameCond);
    }

    private BooleanExpression ageEq1(Integer ageCond) {
        if(ageCond == null) return null;

        return member.age.eq(ageCond);
    }

    @Test
    void bulkUpdate(){
        //given

        //when
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();
        //then
        assertEquals(2, count);
    }

    @Test
    @DisplayName("벌크 수정 연산 후 데이터 가져오기 - 영속성 컨택스트에서 가져오므로 반영이 안됨.")
    void bulkUpdateAndFetch(){
        //given

        //when
        queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();
        //then
        for (Member member : result) {
            System.out.println(member.getUsername() + " " + member.getAge());
        }
    }

    @Test
    void bulkAdd(){
        //given

        //when
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
        //then
        assertEquals(4, count);
    }

    @Test
    void bulkMultiply(){
        //given

        //when
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();
        //then
        assertEquals(4, count);
    }

    @Test
    void bulkDelete(){
        //given

        //when
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
        //then
    }

    @Test
    void sqlFunction(){
        //given

        //when
        List<String> result = queryFactory
                .select(Expressions.stringTemplate("function('replace',{0},{1},{2})"
                        , member.username, "member", "M"))
                .from(member)
                .fetch();
        //then
        for (String s : result){
            System.out.println(s);
        }
    }

    @Test
    void sqlFunction2(){
        //given

        //when
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(
                        Expressions.stringTemplate(
                                "function('lower', {0})",
                                member.username
                        )
                ))
                .fetch();
        //then
        for( String s : result) {
            System.out.println(s);
        }
    }
}