package com.study.querydsl.dto;

import lombok.Data;

@Data
public class MemberSearchCondition {
    // 회원 명, 팀명, 나이(ageGoe >  > ageLow)를 조건으로
    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;
}
