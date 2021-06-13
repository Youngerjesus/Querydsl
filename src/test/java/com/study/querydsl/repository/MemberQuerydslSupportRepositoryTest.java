package com.study.querydsl.repository;

import com.study.querydsl.domain.Member;
import com.study.querydsl.dto.MemberSearchCondition;
import com.study.querydsl.dto.MemberTeamDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberQuerydslSupportRepositoryTest {

    @Autowired
    MemberQuerydslSupportRepository memberQuerydslSupportRepository;

    @Test
    void searchPageSimpleTest(){
        //given
        MemberSearchCondition condition = new MemberSearchCondition();
        PageRequest pageRequest = PageRequest.of(0, 3);
        //when
        Page<Member> result = memberQuerydslSupportRepository.applyPagination(condition, pageRequest);
        //then
        assertEquals(result.getSize(), 3);
        assertThat(result.getContent()).extracting("username").containsExactly("member0","member1","member2");
    }

    @Test
    void searchPageComplexTest(){
        //given
        MemberSearchCondition condition = new MemberSearchCondition();
        PageRequest pageRequest = PageRequest.of(0, 3);
        //when
        Page<Member> result = memberQuerydslSupportRepository.applyPagination2(condition, pageRequest);
        //then
        assertEquals(result.getSize(), 3);
        assertThat(result.getContent()).extracting("username").containsExactly("member0","member1","member2");
    }
}