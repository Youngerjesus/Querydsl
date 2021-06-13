## Querydsl 실무 활용 - 스프링 데이터 JPA와 Querydsl

***

### 순수 JPA 레파지토리에서 스프링 데이터 JPA 레파지토리로 변경 

스프링 데이터 JPA 를 이용하면 순수 JPA 를 이용해서 만들었던 기본적인 기능들은 제공해주므로 간단해진다. 

즉 만들어야할 코드가 굉장히 많이 줄어든다.  

##### 스프링 데이터 JPA 레파지토리 생성

```java
public interface MemberRepository extends JpaRepository<Member, Long> {

    // select m from Member m where m.username = ?
    List<Member> findByUsername(String username);
}
```

- 기본적인 save() 함수나 findById 같은 메소드들은 모두 스프링 데이터 JPA 에서 제공해준다. 

***

### 사용자 정의 레파지토리 

복잡한 쿼리를 쓰려면 사용자 정의 레파지토리를 작성해야 한다. 

먼저 JpaRepository 를 상속한 스프링 데이터 JPA 레파지토리인 MemberRepository 를 만들고

복잡한 쿼리를 담당한 MemberRepositoryCustom 인터페이스를 만들고 여기에 선언을 해준다.

그 후 실제 구현을 담당한 MemberRepositoryImpl 이라는 클래스를 만들고 MemberRepositoryCustom 을 상속한다. 

이때 이름이 스프링 데이터 JPA 레파지토리 이름을 따라가야 실제 구현체를 찾을 수 있으므로 이름에 조심하자.

위 과정을 코드로 보면 다음과 같다.

##### 스프링 데이터 JPA - MemberRepository

```java
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom{

    // select m from Member m where m.username = ?
    List<Member> findByUsername(String username);
}
```

##### MemberRepositoryCustom interface

```java
public interface MemberRepositoryCustom {
    List<MemberTeamDto> search(MemberSearchCondition condition);
}
``` 

##### MemberRepositoryImpl 

````java
public class MemberRepositoryImpl implements MemberRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
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
                .fetch();
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
````
- 만약 Querydsl 을 이용하는 기능이 너무 특화된 기능이라면 굳이 MemberRepository 로 상속하도록 하는게 아니라  
  별도의 클래스를 만들고 거기서 쿼리를 관리하는 것도 좋다. 
  
***

### Querydsl 페이징 연동

스프링 데이터 JPA 에 있는 페이징 기능을 Querydsl 에서 활용하는 방법을 소개한다.

##### 먼저 사용자 정의 인터페이스에 페이징을 할 수 있는 메소드 추가한다. 

```java
public interface MemberRepositoryCustom {

    List<MemberTeamDto> search(MemberSearchCondition condition);
    Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable); // 새로 추가한 페이징 메소드 1
    Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable); // 새로 추가한 페이징 메소드 2
}
```

##### 전체 카운트를 한번에 조회하는 단순한 방법 - searchPageSimple() 

````java
@Override
public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
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
                .fetchResults();

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();
        return new PageImpl<>(content, pageable, total);
}
````
- fetchResults 로 결과를 조회하면 count 쿼리와 결과를 가져오는 select 쿼리가 한번에 나간다. 

##### 데이터와 전체 카운트를 별도로 조회하는 방법 - searchPageComplex() 

```java
@Override
public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
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

        long total = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetchCount();
        return new PageImpl<>(content, pageable, total);
}
```
- 데이터를 가지고 오는 쿼리와 count 를 가지고 오는 쿼리를 별도로 작성해서 이후에 조회 후 합치는 방식이다.
  이걸 사용하는 이유는 count 쿼리의 경우에는 조인을 탈 필요가 없는 경우도 있기 떄문에 별도로 작성하는게 성능을 높일수도 있다.
  
***

### CountQuery 최적화 

Count 쿼리가 생략 가능한 경우가 있고 이를 스프링 데이터 JPA 에서 지원해주기도 한다. 

- 페이지 시작하면서 컨텐츠 사이즈가 페이지 사이즈 보다 작을때

- 마지막 페이지인 경우에는 Count 쿼리를 쓸 필요가 없다. (offset + pageSize 를 하면 total 이 나오므로)

##### Count Query 최적화 

```java
@Override
public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
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
```

- getPage() 함수에는 위의 두 조건의 경우에는 Count 쿼리를 나가지 않게 해준다. 

***

### 페이징을 사용하는 컨트롤러 개발

##### /v2/members 와 /v3/members 컨트롤러 
```java
@GetMapping("/v2/members")
public Page<MemberTeamDto> searchMemberV2(MemberSearchCondition condition, Pageable pageable){
    return memberRepository.searchPageSimple(condition, pageable);
}

@GetMapping("/v3/members")
public Page<MemberTeamDto> searchMemberV3(MemberSearchCondition condition, Pageable pageable){
    return memberRepository.searchPageComplex(condition, pageable);
}
```

##### 결과 조회 

```text
http://localhost:8080/v2/members?teamName=TeamB&ageGoe=31&ageLoe=40

{
    "content": [
        {
            "memberId": 34,
            "username": "member31",
            "age": 31,
            "teamId": 2,
            "teamName": "TeamB"
        },
        {
            "memberId": 36,
            "username": "member33",
            "age": 33,
            "teamId": 2,
            "teamName": "TeamB"
        },
        {
            "memberId": 38,
            "username": "member35",
            "age": 35,
            "teamId": 2,
            "teamName": "TeamB"
        },
        {
            "memberId": 40,
            "username": "member37",
            "age": 37,
            "teamId": 2,
            "teamName": "TeamB"
        },
        {
            "memberId": 42,
            "username": "member39",
            "age": 39,
            "teamId": 2,
            "teamName": "TeamB"
        }
    ],
    "pageable": {
        "sort": {
            "sorted": false,
            "unsorted": true,
            "empty": true
        },
        "offset": 0,
        "pageNumber": 0,
        "pageSize": 20,
        "paged": true,
        "unpaged": false
    },
    "totalPages": 1,
    "totalElements": 5,
    "last": true,
    "size": 20,
    "number": 0,
    "sort": {
        "sorted": false,
        "unsorted": true,
        "empty": true
    },
    "numberOfElements": 5,
    "first": true,
    "empty": false
}
```

````text
http://localhost:8080/v2/members?size=5

{
    "content": [
        {
            "memberId": 3,
            "username": "member0",
            "age": 0,
            "teamId": 1,
            "teamName": "TeamA"
        },
        {
            "memberId": 4,
            "username": "member1",
            "age": 1,
            "teamId": 2,
            "teamName": "TeamB"
        },
        {
            "memberId": 5,
            "username": "member2",
            "age": 2,
            "teamId": 1,
            "teamName": "TeamA"
        },
        {
            "memberId": 6,
            "username": "member3",
            "age": 3,
            "teamId": 2,
            "teamName": "TeamB"
        },
        {
            "memberId": 7,
            "username": "member4",
            "age": 4,
            "teamId": 1,
            "teamName": "TeamA"
        }
    ],
    "pageable": {
        "sort": {
            "sorted": false,
            "unsorted": true,
            "empty": true
        },
        "offset": 0,
        "pageNumber": 0,
        "pageSize": 5,
        "paged": true,
        "unpaged": false
    },
    "totalPages": 20,
    "totalElements": 100,
    "last": false,
    "size": 5,
    "number": 0,
    "sort": {
        "sorted": false,
        "unsorted": true,
        "empty": true
    },
    "numberOfElements": 5,
    "first": true,
    "empty": false
}
````


 


