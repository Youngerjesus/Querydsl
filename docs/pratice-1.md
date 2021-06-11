## Querydsl 실무 활용 - 순수 JPA와 Querydsl

***


### 순수 JPA 레파지토리와 Querydsl 

순수 JPA 레파지토리를 이용해서 사용하는 방법과 Querydsl 을 이용해서 사용하는 방법을 보면 다음과 같다. 


##### 순수 JPA 레파지토리 사용 

```java
@Repository
public class MemberJpaRepository {

    private final EntityManager em;

    public MemberJpaRepository(EntityManager em) {
        this.em = em;
    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member member = em.find(Member.class, id);
        return Optional.ofNullable(member);
    }

    public List<Member> findAll(){
        return em.createQuery("select m from Member m", Member.class).getResultList();
    }
    
    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username =:username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }
}
```

##### Querydsl 사용 - JpaQueryFactory 를 사용하면 된다. 

```java
@Repository
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member member = em.find(Member.class, id);
        return Optional.ofNullable(member);
    }
    
    public List<Member> findAll_Querydsl(){
        return queryFactory
                .selectFrom(member)
                .fetch();
    }
    
    public List<Member> findByUsername_Querydsl(String username){
        return queryFactory
                    .selectFrom(member)
                    .where(member.username.eq(username))
                    .fetch();
    }
}

```

- JpaQueryFactory 는 EntityManager 를 이용해서 생성해도 되고 빈으로 등록한 후 주입받아도 좋다.

- 확실히 JpaQueryFactory 를 이용해서 작성하는게 더 타입 세이프하고 간단하다.


***

### 동적 쿼리와 성능 최적화 조회 - Builder 사용 

동적 쿼리를 사용하기 위해 조건들을 Builder 로 만들고 이를 한번에 Dto 로 가지고 오도록 해서 성능을 최적화 하는 방법은 다음과 같다.

##### MemberTeamDto - 먼저 조회할 Dto

```java
@Data
public class MemberTeamDto {
    private Long memberId;
    private String username;
    private int age;
    private Long teamId;
    private String teamName;

    @QueryProjection
    public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}
```

##### MemberSearchCondition - 동적 쿼리 조건

```java
@Data
public class MemberSearchCondition {
    // 회원 명, 팀명, 나이(ageGoe >  > ageLow)를 조건으로
    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;
}
```

##### 동적 쿼리와 Builder 그리고 성능최적화를 사용한 것 

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

````text
Hibernate: 
    select
        member0_.member_id as col_0_0_,
        member0_.username as col_1_0_,
        member0_.age as col_2_0_,
        team1_.team_id as col_3_0_,
        team1_.name as col_4_0_ 
    from
        member member0_ 
    left outer join
        team team1_ 
            on member0_.team_id=team1_.team_id 
    where
        team1_.name=? 
        and member0_.age>=? 
        and member0_.age<=?
````

- String 의 경우에는 null 체크도 있지만 "" 으로 들어올 수도 있기 때문에 StringUtils.hasText() 라는 라이브러리를 이용했다.

- 동적 쿼리를 이용할 때는 여기에다가 limit 나 페이징 쿼리 또는 기본 조건을 추가해 대량의 데이터를 가지고 오지 않도록 설계가 나름 필요하다.

*** 

### 동적 쿼리와 성능 최적화 조회 -  Where 절 파라미터 사용 

이번에는 이전에 했던 Builder 를 사용하는 것 대신에 Where 절 파라미터를 사용해서 만들어 보겠다.

##### 동적 쿼리와 Where 파라미터 그리고 성능최적화를 사용한 것

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
```

````text
Hibernate: 
    select
        member0_.member_id as col_0_0_,
        member0_.username as col_1_0_,
        member0_.age as col_2_0_,
        team1_.team_id as col_3_0_,
        team1_.name as col_4_0_ 
    from
        member member0_ 
    left outer join
        team team1_ 
            on member0_.team_id=team1_.team_id 
    where
        team1_.name=? 
        and member0_.age>=? 
        and member0_.age<=?
```` 

- 확실히 Builder 를 사용한 것 보다 Where 절을 파라미터 형식으로 조건을 거는게 좀 더 가독성이 좋다. 

- Intellij 기준으로 Where 절 파라미터를 자동 생성으로 만들때 Predicate 타입을 리턴하도록 하는데 이를 BooleanExpression 으로 바꾸는게 더 활용성은 좋다.

  - BooleanExpression 도 Predicate 를 상속받고 있기도 하고 같은 BooleanExpression 끼리 조합할 수 있어서 활용성이 더 좋다. 
  
  - 즉 Projection 이 달라져도 재사용하는게 충분히 가능하다. 

##### BooleanExpression 의 조립 예제 

```java
private BooleanExpression ageBetween(Integer ageLoe, Integer ageGoe) {
        return ageLoe(ageLoe).and(ageGoe(ageGoe));
}
```
  
***

### 조회 API 컨트롤러 개발 

편리한 데이터 확인을 위해 샘플 데이터를 추가하겠다. 

여기서는 환경에 맞춰서 톰캣이 뜰때는 샘플 데이터를 넣도록 하고 테스트 실행 환경에서는 이를 넣지 않도록 구별해서 만들겠다. 

##### profile - local 환경에서 샘플 데이터 주입

```java
@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;

    @PostConstruct
    public void init() {
        initMemberService.init();
    }

    @Component
    static class InitMemberService {
        @PersistenceContext
        private EntityManager em;

        @Transactional
        public void init() {
            Team teamA = new Team("TeamA");
            Team teamB = new Team("TeamB");
            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i < 100; i++) {
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                em.persist(new Member("member" + i, i, selectedTeam));
            }
        }
    }
}
```
- @PostConstruct 와 @Transactional 을 분리시켜야 하는 이유로는 @PostConstruct 는 해당 빈 자체만 생성되었다고 가정하에 호출되지만 해당 빈에 관련한 AOP 등을 포함해서 전체
  스프링 어플리케이션 컨택스트의 초기화를 말하지는 않는다. 트랜잭션을 처리하는 AOP 등은 스프링 어플리케이션 컨택스트가 초기화가 되어야만 가능하다. 즉 @PostConstruct 만을 사용하면 @Transactional 을 이용하는게 
  가능하지 않다. 하지만 여기서는 @PostConstruct 안에서 @Transactional 을 이용하는 빈을 호출해서 사용하니까 이 빈이 초기화 되었다는건 @Transactional 을 이용할 수 있다는 시점을 말하므로 우회해서 사용하는게 가능하다.

##### 조회를 위한 MemberController  
````java
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberJpaRepository memberJpaRepository;

    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) {
        return memberJpaRepository.searchByWhere(condition);
    }
}
````


 



