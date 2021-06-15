# 우아한 형제들의 Querydsl 활용법

이 글은 "우아한테크콘서트2020 수십억건에서 Querydsl 사용하기" 와 발표자이신 이동욱님의 기술 블로그를 보고 작성한 글입니다.  

모든 예제와 추가로 Querydsl 사용 문법은 https://github.com/Youngerjesus/Querydsl 에 있습니다.  

***

## 1. extends / implements 사용하지 않기 

일반적으로 필요한 Repository 를 만들면 Spring Data Jpa 기능을 이용하기 위해 JpaRepository 를 상속받고 또 Querydsl-Jpa 를 위해 사용자 정의 Repository 를 만들고 이를 상속받는다. 그리고 이를 구현한 실제 구현 객체가 필요하다.  

아니면 QuerydslRepositorySupport 클래스를 만들고 이를 사용자 정의 클래스에서 상속 받도록 하는 방법도 있다. 

근데 이렇게 매번 상속받는 것이 불편하다.  사실 JpaQueryFactory 만 있다면 상관 없기 때문에 이를 이용하도록 한다. 

##### Querydsl 사용하는 방법
```java
// 기존 방법 1. 
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {} 


// 기존 방법2
public class MemberRepositoryCustom extends QuerydslRepositorySupport {}

// 추천하는 방법
@Repository
@RequiredArgsConstructor 
public class MemberRepositoryCustom {
    private final JpaQueryFactory queryFactory; // 물론 이를 위해서는 빈으로 등록을 해줘야 한다. 
}
``` 

##### JpaQueryFactory 빈으로 등록하는 방법

```java
@Configuration
public class QuerydslConfiguration {
    @Autowired
    EntityManager em;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
       return new JPAQueryFactory(em);
    }
}
```
***

## 2. 동적쿼리는 BooleanExpression 사용하기

동적쿼리를 작성하는 방법에는 BooleanBuilder 를 작성하는 방법과 Where 절과 피라미터로 Predicate 를 이용하는 방법 그리고 

Where 절과 피라미터로 Predicate 를 상속한 BooleanExpression 을 사용하는 방법 이렇게 있다.  

예제를 보면 알겠지만 BooleanBuilder 를 사용하는 방법은 어떤 쿼리가 나가는지 예측하기 힘들다는 단점이 있다. 

그리고 Predicate 보다는 BooleanExpression 을 사용하는 이유로는 BooleanExpression 은 and 와 or 같은 메소드들을 이용해서

BooleanExpression 을 조합해서 새로운 BooleanExpression 을 만들 수 있다는 장점이 있다. 그러므로 재사용성이 높다. 그리고 null 을 반환하면 조건이 무시되서 안전하다.  

##### BooleanBuilder 를 이용하는 예제 

```java
public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition){
    BooleanBuilder builder = new BooleanBuilder();

    if (hasText(condition.getUsername())) {
        builder.and(member.username.eq(condition.getUsername()));
    }

    if(hasText(condition.getTeamName())){
        builder.and(team.name.eq(condition.getTeamName()));
    }

    if(condition.getAgeGoe() != null) {
        builder.and(member.age.goe(condition.getAgeGoe()));
    }

    if(condition.getAgeLoe() != null){
        builder.and(member.age.loe(condition.getAgeLoe()));
    }

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
            .where(builder)
            .fetch();
}
```

##### Where 절과 BooleanExpression 을 이용하는 에제

```java
public List<MemberTeamDto> searchByWhere(MemberSearchCondition condition){
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
```

***

## 3. exist 메소드 사용하지 않기 

Querydsl 에서 exist 를 금지하는 것이다. 왜냐하면 Querydsl 에 있는 exist 는 count 쿼리를 사용하기 떄문이다. 

SQL 에서 exist 와 같은 역할을 해줄 수 있는게 count(1) > 0 쿼리다.

exist 의 경우에는 첫번째로 조건에 맞는 값을 찾는다면 바로 반환하도록 하지만 count 쿼리는 전체 행을 모두 조회하도록 해서 성능이 떨어진다. 

즉 이건 스캔 대상이 앞에 있을수록 성능 차이가 심해진다. 

##### Querydsl 내부 exists 구현 상태  - QuerydslJpaPredicateExecutor.class

```java
public boolean exists(Predicate predicate) {
    return this.createQuery(predicate).fetchCount() > 0L; // 보시다시피 count 로 조회한다.
}
```

즉 Querydsl 에서는 이 메소드를 사용하지 않고 우회하도록 해야하는데 이를 위해서는 fetchFirst() 를 사용하면 된다.

fetchFirst() 의 내부 구현에는 limit(1) 이 있어서 결과를 한개만 가져오도록 한다. 

##### fetchFirst 를 이용한 exist 구현하기

```java
public Boolean exist(Long memberId) {
    Integer fetchOne = queryFactory
            .selectOne()
            .from(member)
            .where(member.id.eq(memberId))
            .fetchFirst();

    return fetchOne != null;
}
```

***

## 4. Cross Join을 회피하기 

묵시적 조인이라고 하는 조인을 명시하지 않고 엔터티에서 다른 엔터티 조회를 통해 비교하는 경우 JPA 가 알아서 크로스 조인을 하게 된다. 

크로스 조인을 하게 되면 나올 수 있는 데이터가 많기 때문에 성능상에 단점이 있다.

크로스 조인을 피하기 위해서는 쿼리를 보고 크로스 조인이 나간다면 명시적 조인을 이용해서 해결하도록 하자. 

##### Cross Join 발생 예제

```java
public List<Member> crossJoin() {
    return queryFactory
            .selectFrom(member)
            .where(member.team.id.gt(member.team.leader.id))
            .fetch();
}
```

```text
select
    member0_.member_id as member_i1_0_,
    member0_.age as age2_0_,
    member0_.team_id as team_id4_0_,
    member0_.username as username3_0_ 
from
    member member0_ cross 
join
    team team1_ 
where
    member0_.team_id=team1_.team_id 
    and member0_.team_id>team1_.member_id
```

##### Cross Join 을 InnerJoin 으로 변경하

````java
public List<Member> crossJoinToInnerJoin() {
    return queryFactory
            .selectFrom(member)
            .innerJoin(member.team, team)
            .where(member.team.id.gt(member.team.leader.id))
            .fetch();
}
````
```text
select
    member0_.member_id as member_i1_0_,
    member0_.age as age2_0_,
    member0_.team_id as team_id4_0_,
    member0_.username as username3_0_ 
from
    member member0_ 
inner join
    team team1_ 
        on member0_.team_id=team1_.team_id 
where
    member0_.team_id>team1_.member_id
```

***

## 5. 조회할땐 Entity 보다는 Dto 를 우선적으로 가져오기  

많은 분들이 Entity 를 사용해서 가지고 오는걸 먼저 생각하지만 Entity 를 가지고 오면 영속성 컨택스트의 1차 캐시 기능을 사용하게 되고
불필요한 칼럼을 조회하기도 한다.

그리고 OneToOne 관계에서는 N + 1 문제가 생기기도 한다. 

OneToOne N + 1 문제는 외래 키를 가지고 있는 주인 테이블에서는 지연로딩이 제대로 동작하지만 mappedBy 로 연결된 반대편 테이블에서는 지연로딩이 동작하지 않고 N + 1 쿼리가 터지는 문제다. 

OneToOne 문제를 자세히 다루기에는 내용이 많으므로 [링크](https://yongkyu-jang.medium.com/jpa-%EB%8F%84%EC%9E%85-onetoone-%EA%B4%80%EA%B3%84%EC%97%90%EC%84%9C%EC%9D%98-lazyloading-%EC%9D%B4%EC%8A%88-1-6d19edf5f4d3) 를 남겨놓겠다.

즉 Entity 를 조회하면 성능 이슈가 될만한 사항이 많다. 

그치만 Entity 를 조회가 필요한 경우도 있으므로 다음과 같이 조건을 나눠서 조회를 하자. 

- Entity 조회는 실시간으로 Entity 변경이 필요한 경우에 조회를 하자.
 
- Dto 조회는 성능 개선이나 대량의 데이터 조회가 필요한 경우에 조회를 하자. 

Dto 조회를 할 때 좀 더 성능상으로 이득을 보기 위해서는 조회 칼럼을 최소화 하는 방법이 있다. 

##### Dto 조회할때 필요한 칼럼만 가져오기 

```java
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
```

- select 절에 보면 필요한 칼럼만 데이터베이스에서 가져오도록 하는데 teamId 같은 경우는 이미 기존에 매개변수로 받았으니까 
데이터베이스에서 가져올 필요는 없다. 이처럼 중복된 칼럼은 가져오지 않도록 해서 성능상에서 약간의 이득을 더 볼 수 있다.

- 여기 에제에서는 Projection 을 할 때 Dto 클래스 생성자에 @QueryProjection 을 해서 Dto 도 Q타입의 클래스를 만들도록 했다. 
이를 통해 기존의 Projections 할때보다 Querydsl 에 좀 더 의존적이긴 하지만 타입 세이프 하다는 장점이 있다.

***

## 6. Select 칼럼에 Entity는 자제하기  

다음과 같이 select 절 안에 Entity 를 넣도록 하면 Entity 에 있는 모든 칼럼들이 조회가 된다. 

앞서 얘기한 바와 같이 필요한 칼럼만 조회하도록 하고 Entity 를 조회하면 그에따른 OneToOne 관계에서는 N+1 문제가 발생할 수도 있으니까 조심하자.


##### Select 절에 Entity가 는 경우

```java
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
```
```text
select
    member0_.member_id as col_0_0_,
    member0_.username as col_1_0_,
    member0_.age as col_2_0_,
    member0_.team_id as col_3_0_,
    team1_.team_id as team_id1_1_,
    team1_.member_id as member_i3_1_,
    team1_.name as name2_1_ 
from
    member member0_ 
inner join
    team team1_ 
        on member0_.team_id=team1_.team_id 
where
    member0_.team_id=?
```

##### Select 절에 필요한 칼럼만 넣자. 
```java
public List<MemberTeamDto2> entityInSelect(Long teamId){
    return queryFactory
            .select(
                    new QMemberTeamDto2(
                            member.id,
                            member.username,
                            member.age,
                            member.team.id
                    )
            )
            .from(member)
            .innerJoin(member.team, team)
            .where(member.team.id.eq(teamId))
            .fetch();
}
```

```text
select
    member0_.member_id as col_0_0_,
    member0_.username as col_1_0_,
    member0_.age as col_2_0_,
    member0_.team_id as col_3_0_ 
from
    member member0_ 
inner join
    team team1_ 
        on member0_.team_id=team1_.team_id 
where
    member0_.team_id=?
```

- Select 절에 Entity 를 사용할 땐 Entity 자체가 필요한 경우도 있겠지만 Id 만 필요한 경우도 있다. 이런 경우에는 필요한 칼럼만
가져올 수 있으니까 성능상에서 이점이 있다. 
     
## 7. Group By 최적화하기  

일반적으로 MySQL 에서는 Group By 를 실행하면 GROUP BY column 에 의한 Filesort 라는 정렬 알고리즘이 추가적으로 실행된다.  

##### MySQL 5.7 Reference
> If you use GROUP BY, output rows are sorted according to the GROUP BY columns as if you had an ORDER BY for the same columns. To avoid the overhead of sorting that GROUP BY produces, add ORDER BY NULL:
>  
>  Relying on implicit GROUP BY sorting (that is, sorting in the absence of ASC or DESC designators) or explicit sorting for GROUP BY (that is, by using explicit ASC or DESC designators for GROUP BY columns) is deprecated. To produce a given sort order, provide an ORDER BY clause.

물론 이 쿼리는 index 가 없다면 발생한다. 

Filesort 가 발생하면 상대적으로 더 느려지므로 이 경우를 막으려면 order by null 을 사용하면 되는데 Querydsl 에서는 order by null 을 지원하지 않는다.

그래서 우형에서는 OrderByNull 이라는 클래스를 만들고 이를 통해서 OrderByNull 을 지원하도록 했다. 

##### OrderByNull 클래스

```java
public class OrderByNull extends OrderSpecifier {

    public static final OrderByNull DEFAULT = new OrderByNull();

    private OrderByNull(){
        super(Order.ASC, NullExpression.DEFAULT, NullHandling.Default);
    }
}
```

##### Group By 와 OrderByNull 사용 예제

```java
public List<Integer> useOrderByNull() {
    return queryFactory
        .select(member.age.sum())
        .from(member)
        .innerJoin(member.team, team)
        .groupBy(member.team)
        .orderBy(OrderByNull.DEFAULT)
        .fetch();
}
```

```text
select
    sum(member0_.age) as col_0_0_ 
from
    member member0_ 
inner join
    team team1_ 
        on member0_.team_id=team1_.team_id 
group by
    member0_.team_id 
order by
    null asc
```

그리고 정렬의 경우에는 100건이 이하라면 애플리케이션 메모리로 가져와서 정렬 하는 걸 추천한다. 

일반적으로 DB 자원보다는 애플리케이션 자원이 더 싸기 때문에 더 효율적이다. 

그리고 이런 OrderByNull 은 페이징 쿼리인 경우에는 사용하지 못한다.

***

## 8. Querydsl 에서 커버링 인덱스 사용하기

커버링 인덱스는 쿼리를 충족시키는데 필요한 모든 칼럼을 가지고 있는 인덱스다. 

select / where / order by / group by 등에서 사용되는 모든 칼럼이 인덱스에 포함된 상태로 NoOffset 방식과 더불어 페이징 조회 성능을 향상시키는
가장 보편적인 방법이다. 

커버링 인덱스와 인덱스에 대해 자세하게 알고싶다면 다음 링크를 보면 된다.

- [성능 향상을 위한 SQL 작성법](https://d2.naver.com/helloworld/1155)

- [MySQL에서 커버링 인덱스](https://gywn.net/2012/04/mysql-covering-index/) 

지금은 인덱스를 이용해서 질의를 한다면 select 절을 비롯해 order by, where 등 쿼리 내 모든 항목이 인덱스 칼럼으로만 
이루어지게 되서 인덱스 내부에서 커리가 완성되므로 DB 인덱스 페이지 i/o 만으로 이뤄지기 떄문에 성능이 올라간다는 점만 알자.  

즉 인덱스 검색으로 빠르게 처리하고 걸러진 항목에 대해서만 데이터 블록에 접근하기 때문에 성능의 이점을 얻게 된다. 


아쉽게도 Querydsl 에선 JPQL은 from 절에서 서브쿼리를 지원하지 않는다.

이를 위한 우회하는 방법으로는 다음과 같다.

- 커버링 인덱스를 활용해 조회 대상의 PK를 조회한다.

- 그 후 해당 PK로 필요한 칼럼 항목들을 조회한다. 

##### Querydsl 에서 커버링 인덱스를 사용한 예제

```java
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
```

- 이 방식의 단점으로는 너무 많은 인덱스가 필요하다는 점이다. 결국 쿼리의 모든 항목이 인덱스로 필요하기 때문에 느린 쿼리가 발생할 때마다 인덱스가 신규 생성될 수 있다.

- 인덱스의 크기가 커질 수 있다는 점이다. 인덱스도 결국 데이터이기 떄문에 들어가는 항목이 점점 많아진다면 인덱스가 비대해진다는 점이 있다. 


***

## 9. 페이징 성능 개선을 위해 No Offset 사용하기 

기존 페이징 방식인 offset 과 limit 를 이용한 방식은 서비스가 커짐에 따라서 장애를 유발할 수도 있다.

초기에는 데이터가 적어서 문제가 없지만 데이터가 점점 많아지면 느려지기 때문인데 

기존에 페이징 쿼리는 다음과 같다.

```sql
SELECT *
FROM items 
WHERE 조건문
ORDER BY id desc 
OFFSET 페이지 번호
LIMIT 페이지 사이즈 
``` 

이와 같은 형태는 페이지 번호가 뒤로 갈수록 앞에서 읽었던 행을 다시 읽어야 한다.

즉 offset이 10000이고 limit가 20이라면 10,020 행을 읽어야 한다. 그리고 나서 10,000 개의 행을 버리는 것이다. 

No Offset 방식은 시작 지점을 인덱스로 빠르게 찾아 첫 페이지부터 읽도록 하는 방식이다. 

SQL 문은 다음과 같다. 

```sql
SELECT *
FROM items
WHERE 조건문
AND id < 마지막 조회 ID 
ORDER BY id desc 
LIMIT 페이지 사이즈 
```

이전에 조회된 결과를 한번에 건너뛸 수 있게 마지막 조회 결과의 ID 를 조건문에 사용하는 방식을 이용한다. 

##### NoOffset 예제 코드 

```java
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
```

***

## 10. 일괄 Update 최적화하기 

JPA를 사용하면 영속성 컨택스트가 Dirty Checking 기능을 지원해주는데 이를 이용하면 엄청나게 많은 양의 데이터에 대해서 업데이트 쿼리가 나갈수도 있다. 

이렇게하면 일괄 Update 하는 것보다 확실히 성능상에서 낮아진다. 

##### JPA Dirty Checking 을 이용한 예제 
```java
private void dirtyChecking(){
    List<Member> result = queryFactory
            .selectFrom(member)
            .fetch();
    
    for (Member member : result){
        member.setUsername(member.getUsername() + "+");
    }
}
``` 

##### Querydsl 일괄 업데이트를 이용한 예제 

```java
public void batchUpdate(){
    queryFactory
        .update(member)
        .set(member.username, member.username + "+")
        .execute();    
}
```

주의할 점은 일괄 업데이트는 영속성 컨택스트의 1차 캐시 갱신이 안된다. 그러므로 Cache Eviction 이 필요하다

그러모르 실시간 비즈니스 처리나 실시간 단건 처리가 필요하다면 Dirty Checking 기능을 본격적으로 이용하고 대령의 데이터를 일괄 업데이트가 필요하면 
위의 방식을 사용하자.

***

## 11. JPA로 Bulk Insert는 자제하자. 

JDBC에는 rewriteBatchedStatements 로 Insert 합치기 라는 옵션이 있다. 이를 통해 여러 Insert 문을 하나의 Insert 로 작업하도록 하는 것을 망ㄹ한다. 

예를 들면 다음 쿼리들이 

```sql
INSERT INTO message (`content`, `status`, `created_by`, `created_at`,`last_modified_at`)
VALUES (:content, :status, :created_by, :created_at, :last_modified_at);
INSERT INTO message (`content`, `status`, `created_by`, `created_at`,`last_modified_at`)
VALUES (:content, :status, :created_by, :created_at, :last_modified_at);
INSERT INTO message (`content`, `status`, `created_by`, `created_at`,`last_modified_at`)
VALUES (:content, :status, :created_by, :created_at, :last_modified_at);
// ...
```

아래의 쿼리로 대체될 수 있는 것이다. 

```sql
INSERT INTO message (`content`, `status`, `created_by`, `created_at`,`last_modified_at`) 
VALUES (:content, :status, :created_by, :created_at, :last_modified_at)
, (:content, :status, :created_by, :created_at, :last_modified_at)
, (:content, :status, :created_by, :created_at, :last_modified_at)
, ...;
```

하지만 JPA 에는 auto_increment일때 insert 합치기가 적용되지 않는다. 그러므로 이 기능이 필요하다면 JdbcTemplate 를 사용하자.
 
