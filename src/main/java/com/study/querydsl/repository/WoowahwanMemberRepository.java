package com.study.querydsl.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.domain.Member;
import com.study.querydsl.dto.*;
import com.study.querydsl.repository.support.OrderByNull;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.study.querydsl.domain.QMember.member;
import static com.study.querydsl.domain.QTeam.team;
import static org.springframework.util.StringUtils.hasText;

@Repository
@RequiredArgsConstructor
public class WoowahwanMemberRepository {

    private final JPAQueryFactory queryFactory;

    public List<Member> getMembers(){
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public void batchUpdate(){
        String test = "test";
        queryFactory
            .update(member)
            .set(member.username, test)
            .execute();
    }

    private void dirtyChecking(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member : result){
            member.setUsername(member.getUsername() + "+");
        }
    }

    public List<MemberDto> noOffset(Long lastMemberId, int limit){
        return queryFactory
            .select(new QMemberDto(
                    member.username,
                    member.age
            ))
            .from(member)
            .where(member.username.contains("member")
                    .and(memberIdLt(lastMemberId)))
            .orderBy(member.id.desc())
            .limit(limit)
            .fetch();
    }

    private BooleanExpression memberIdLt(Long lastMemberId) {
        return lastMemberId != null ? member.id.lt(lastMemberId): null;
    }

    public List<MemberDto> useCoveringIndex(int offset, int limit){
        List<Long> ids = queryFactory
                .select(member.id)
                .from(member)
                .where(member.username.like("member%"))
                .orderBy(member.id.desc())
                .limit(limit)
                .offset(offset)
                .fetch();

        if(ids.isEmpty()){
            return new ArrayList<>();
        }

        return queryFactory
                .select(new QMemberDto(
                        member.username,
                        member.age
                ))
                .from(member)
                .where(member.id.in(ids))
                .orderBy(member.id.desc())
                .fetch();
    }

    public List<Integer> useOrderByNull() {
        return queryFactory
            .select(member.age.sum())
            .from(member)
            .innerJoin(member.team, team)
            .groupBy(member.team)
            .orderBy(OrderByNull.DEFAULT)
            .fetch();
    }

    public List<MemberTeamDto2> entityInSelect(Long teamId){
        return queryFactory
                .select(
                        new QMemberTeamDto2(
                                member.id,
                                member.username,
                                member.age,
                                member.team
                        )
                )
                .from(member)
                .innerJoin(member.team, team)
                .where(member.team.id.eq(teamId))
                .fetch();
    }

    public List<MemberTeamDto> findSameTeamMember(Long teamId){
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        Expressions.asNumber(teamId),
                        team.name
                ))
                .from(member)
                .innerJoin(member.team, team)
                .where(member.team.id.eq(teamId))
                .fetch();
    }

    public List<Member> crossJoinToInnerJoin() {
        return queryFactory
                .selectFrom(member)
                .innerJoin(member.team, team)
                .where(member.team.id.gt(member.team.leader.id))
                .fetch();
    }

    public List<Member> crossJoin() {
        return queryFactory
                .selectFrom(member)
                .where(member.team.id.gt(member.team.leader.id))
                .fetch();
    }

    public Boolean exist(Long memberId) {
        Integer fetchOne = queryFactory
                .selectOne()
                .from(member)
                .where(member.id.eq(memberId))
                .fetchFirst();

        return fetchOne != null;
    }

    public Page<MemberTeamDto> search(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Member> countQuery = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    private BooleanExpression ageBetween(Integer ageLoe, Integer ageGoe) {
        return ageLoe(ageLoe).and(ageGoe(ageGoe));
    }
}
