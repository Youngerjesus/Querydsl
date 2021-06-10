package com.study.querydsl.dto;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.domain.Member;
import com.study.querydsl.domain.QMember;
import com.study.querydsl.domain.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static com.study.querydsl.domain.QMember.member;

@SpringBootTest
@Transactional
class UserDtoTest {

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
    void findUserDto(){
        //given

        //when
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        member.age))
                .from(member)
                .fetch();
        //then
        for (UserDto userDto : result){
            assertNotNull(userDto.getName());
        }
    }

    @Test
    void findUserDtoBySubQuery(){
        //given
        QMember memberSub = new QMember("memberSub");
        //when
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(
                                JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age"
                        )))
                .from(member)
                .fetch();
        //then
        for (UserDto userDto : result){
            assertNotNull(userDto.getName());
            assertEquals(40, userDto.getAge()); // 최대 나이가 40살 이다.
        }
    }

}