package com.study.querydsl.repository;

import com.study.querydsl.domain.Member;
import com.study.querydsl.domain.Team;
import com.study.querydsl.dto.MemberDto;
import com.study.querydsl.dto.MemberSearchCondition;
import com.study.querydsl.dto.MemberTeamDto;
import com.study.querydsl.dto.MemberTeamDto2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class WoowahwanMemberRepositoryTest {

    @Autowired
    WoowahwanMemberRepository woowahwanMemberRepository;

    @Autowired
    EntityManager em;

    @Test
    void searchTest(){
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

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("TeamB");

        PageRequest pageRequest = PageRequest.of(0, 3);

        Page<MemberTeamDto> result = woowahwanMemberRepository.search(condition, pageRequest);
        assertEquals(result.getSize(), 3);
    }

    @Test
    void existTest(){
        //given
        Team teamA = new Team("TeamA");
        em.persist(teamA);
        Member member1 = new Member("member1", 10, teamA);
        em.persist(member1);
        //when
        Boolean exist = woowahwanMemberRepository.exist(member1.getId());
        //then
        assertTrue(exist);
    }

    @Test
    void crossJoinTest(){
        //given
        Team teamA = new Team("TeamA");
        em.persist(teamA);
        Member member1 = new Member("member1", 10, teamA);
        em.persist(member1);
        teamA.setLeader(member1);
        //when
        List<Member> members = woowahwanMemberRepository.crossJoin();
        //then
    }

    @Test
    void crossJoinToInnerJoinTest(){
        //given
        Team teamA = new Team("TeamA");
        em.persist(teamA);
        Member member1 = new Member("member1", 10, teamA);
        em.persist(member1);
        teamA.setLeader(member1);
        //when
        List<Member> members = woowahwanMemberRepository.crossJoinToInnerJoin();
        //then
    }

    @Test
    void findSameTeamMemberTest(){
        //given
        Team teamA = new Team("TeamA");
        em.persist(teamA);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        em.persist(member1);
        em.persist(member2);
        //when
        List<MemberTeamDto> sameTeamMember = woowahwanMemberRepository.findSameTeamMember(teamA.getId());
        //then
        assertEquals(sameTeamMember.size(), 2);
    }

    @Test
    void entityInSelectTest(){
        //given
        Team teamA = new Team("TeamA");
        em.persist(teamA);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        em.persist(member1);
        em.persist(member2);
        //when
        List<MemberTeamDto2> sameTeamMember = woowahwanMemberRepository.entityInSelect(teamA.getId());
        //then
        assertEquals(sameTeamMember.size(), 2);
    }

    @Test
    void orderByNullTest(){
        //given
        Team teamA = new Team("TeamA");
        em.persist(teamA);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        em.persist(member1);
        em.persist(member2);
        //when
        List<Integer> integers = woowahwanMemberRepository.useOrderByNull();
        //then
        for (Integer integer : integers){
            System.out.println(integer);
        }
    }

    @Test
    void coveringIndexTest(){
        //given
        Team teamA = new Team("TeamA");
        em.persist(teamA);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamA);
        Member member4 = new Member("member4", 40, teamA);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
        //when
        List<MemberDto> memberDtos = woowahwanMemberRepository.useCoveringIndex(0, 100);
        //then
        for (MemberDto memberDto : memberDtos) {
            System.out.println(memberDto);
        }
    }

    @Test
    void noOffsetTest(){
        //given

        //when
        List<MemberDto> memberDtos = woowahwanMemberRepository.noOffset(90L, 50);
        //then
        for (MemberDto memberDto : memberDtos){
            System.out.println(memberDto);
        }
    }

    @Test
    @CacheEvict(allEntries = true)
    public void cacheEvictTest(){
        //given
        List<Member> members = woowahwanMemberRepository.getMembers();
        //when
        woowahwanMemberRepository.batchUpdate();
        List<Member> updateMembers = woowahwanMemberRepository.getMembers();
        //then
        for (int i = 0; i < members.size() ; i++) {
            System.out.println(members.get(i));
            System.out.println(updateMembers.get(i));
        }
    }
}