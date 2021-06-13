package com.study.querydsl.repository;

import com.study.querydsl.domain.Member;
import com.study.querydsl.domain.QMember;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired MemberRepository memberRepository;

    @Test
    void basicTest(){
        //given
        Member member = new Member("member1", 10);
        memberRepository.save(member);
        //when
        Member findMember = memberRepository.findById(member.getId()).get();
        List<Member> result1 = memberRepository.findAll();
        List<Member> result2 = memberRepository.findByUsername("member1");
        //then
        assertEquals(member, findMember);
        assertTrue(result1.contains(member));
        assertTrue(result2.contains(member));
    }

    @Test
    void querydslPredicateExecutorTest(){
        //given
        //when
        QMember member = QMember.member;
        Iterable<Member> result = memberRepository.findAll(
                member.age.between(0, 40)
                        .and(member.username.eq("member1"))
        );
        //then
        for (Member findMember : result) {
            System.out.println(findMember);
        }
    }
}