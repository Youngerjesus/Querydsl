## 스프링 데이터 JPA가 제공하는 Querydsl 기능

***

### 인터페이스 지원 - QuerydslPredicateExecutor

QuerydslPredicateExecutor 을 이용하면 스프링 데이터 JPA 에서 Querydsl 조건으로 넣을 수 있는 Predicate 를 통해서 결과를 조회하는게 가능해진다. 

##### MemberRepository 에서 QuerydslPredicateExecutor 를 상속해준다. 
```java
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, QuerydslPredicateExecutor<Member> {
    List<Member> findByUsername(String username);
}
```

##### QuerydslPredicateExecutor 
```java
public interface QuerydslPredicateExecutor<T> {
    Optional<T> findOne(Predicate var1);

    Iterable<T> findAll(Predicate var1);

    Iterable<T> findAll(Predicate var1, Sort var2);

    Iterable<T> findAll(Predicate var1, OrderSpecifier<?>... var2);

    Iterable<T> findAll(OrderSpecifier<?>... var1);

    Page<T> findAll(Predicate var1, Pageable var2);

    long count(Predicate var1);

    boolean exists(Predicate var1);
}
```

##### QuerydslPredicateExecutor 예제 
````java
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
````

- QuerydslPredicateExecutor 에서 정의한 메소드들은 Querydsl Predicate 조건절을 넣을 수 있다. 이를 스프링 데이터 JPA에서 지원해준다.

- 다만 한계점은 조인을 할 수 없다는 단점이 있다.

- 그리고 또 다른 단점은 MemberRepository 같은 레파지토리에 Predicate 파라미터를 넘겨줘야 한다. 근데 이는
  서비스나 컨트롤러 계층에서 직접 만들어서 넘겨줘야 하는데 이는 강한 결합이 일어나서 바꾸는데 좋지 않다. 
  
   
***

### Querydsl Web 지원 

링크는 다음과 같다. https://docs.spring.io/spring-data/jpa/docs/2.2.3.RELEASE/reference/html/#core.web.type-safe

Querydsl Web 지원은 컨트롤러 레벨에서 Predicate 를 받을 수 있도록 QuerydslPredicateArgumentResolver 룰 지원해준다. 

그래서 컨트롤러가 다음과 같을때 

```java
@RequestMapping(value = "/", method = RequestMethod.GET)
String index(Model model, @QuerydslPredicate(root = User.class) Predicate predicate,    
      Pageable pageable, @RequestParam MultiValueMap<String, String> parameters) {

model.addAttribute("users", repository.findAll(predicate, pageable));

return "index";
}
```

이렇게 요청을 하면 

````
?firstname=Dave&lastname=Matthews
````

이렇게 Predicate 로 변환해준다. 
```text
QUser.user.firstname.eq("Dave").and(QUser.user.lastname.eq("Matthews"))
```

- 이 조건은 근데 eq 이나 contains 같은 조건만 사용이 가능하다. 

- 그리고 Repository 에서 binding 조건을 넣어줘야 하는데 이도 조금 복잡하다. 

***

### 레파지토리 지원 - QuerydslRepositorySupport 

#### QuerydslRepositorySupport 장점

- getQuerydsl().applyPagination() 스프링 데이터가 제공하는 페이징을 Querydsl로 편리하게 변환
가능(단! Sort는 오류발생)

- from() 으로 시작 가능(최근에는 QueryFactory를 사용해서 select() 로 시작하는 것이 더 명시적)

- EntityManager 제공

#### QuerydslRepositorySupport 단점

- Querydsl 3.x 버전을 대상으로 만듬 그러므로 Querydsl 4.x에 나온 JPAQueryFactory로 시작할 수 없음

  - 즉 select 로 시작할 수 없음 (from 으로 시작해야함) 명시적이진 않음 

- 스프링 데이터 Sort 기능이 정상 동작하지 않음


***

### Querydsl 지원 클래스 직접 만들기

스프링 데이터가 제공하는 QuerydslRepositorySupport 가 지닌 한계를 극복하기 위해 직접 Querydsl
지원 클래스를 만들어서 사용하는게 가능하다. 이 방식은 김영한님이 직접 만드신 방법이다.  

이를 통해 스프링 데이터가 제공하는 페이징을 편리하게 변환하는게 가능하고 페이징과 카운트 쿼리를 분리하는게 가능하다. 

스프링 데이터의 sort 를 지원하고 select 와 selectFrom 으로 시작하는게 가능하다. 

EntityManager 와 QueryFactory 를 제공해준다. 


##### 먼저 Querydsl4RepositorySupport abstract class 를 만든다. 

```java
@Repository
public abstract class Querydsl4RepositorySupport {

    private final Class domainClass;
    private Querydsl querydsl;
    private EntityManager entityManager;
    private JPAQueryFactory queryFactory;

    public Querydsl4RepositorySupport(Class<?> domainClass) {
        Assert.notNull(domainClass, "Domain class must not be null!");
        this.domainClass = domainClass;
    }

    @Autowired
    public void setEntityManager(EntityManager entityManager) {
        Assert.notNull(entityManager, "EntityManager must not be null!");
        JpaEntityInformation entityInformation =
                JpaEntityInformationSupport.getEntityInformation(domainClass, entityManager);
        SimpleEntityPathResolver resolver = SimpleEntityPathResolver.INSTANCE;
        EntityPath path = resolver.createPath(entityInformation.getJavaType());
        this.entityManager = entityManager;
        this.querydsl = new Querydsl(entityManager, new
                PathBuilder<>(path.getType(), path.getMetadata()));
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @PostConstruct
    public void validate() {
        Assert.notNull(entityManager, "EntityManager must not be null!");
        Assert.notNull(querydsl, "Querydsl must not be null!");
        Assert.notNull(queryFactory, "QueryFactory must not be null!");
    }

    protected JPAQueryFactory getQueryFactory() {
        return queryFactory;
    }
    protected Querydsl getQuerydsl() {
        return querydsl;
    }
    protected EntityManager getEntityManager() {
        return entityManager;
    }
    protected <T> JPAQuery<T> select(Expression<T> expr) {
        return getQueryFactory().select(expr);
    }

    protected <T> JPAQuery<T> selectFrom(EntityPath<T> from) {
        return getQueryFactory().selectFrom(from);
    }
    protected <T> Page<T> applyPagination(Pageable pageable,
                                          Function<JPAQueryFactory, JPAQuery> contentQuery) {
        JPAQuery jpaQuery = contentQuery.apply(getQueryFactory());
        List<T> content = getQuerydsl().applyPagination(pageable,
                jpaQuery).fetch();
        return PageableExecutionUtils.getPage(content, pageable,
                jpaQuery::fetchCount);
    }
    protected <T> Page<T> applyPagination(Pageable pageable,
                                          Function<JPAQueryFactory, JPAQuery> contentQuery, Function<JPAQueryFactory, JPAQuery> countQuery) {
        JPAQuery jpaContentQuery = contentQuery.apply(getQueryFactory());
        List<T> content = getQuerydsl().applyPagination(pageable,
                jpaContentQuery).fetch();
        JPAQuery countResult = countQuery.apply(getQueryFactory());
        return PageableExecutionUtils.getPage(content, pageable,
                countResult::fetchCount);
    }
}
```

##### 그 다음 이를 상속한 레파지토리를 만들자. 

```java
@Repository
public class MemberQuerydslSupportRepository extends Querydsl4RepositorySupport {
    public MemberQuerydslSupportRepository() {
        super(Member.class);
    }

    public List<Member> basicSelect(){
        return select(member)
                .from(member)
                .fetch();
    }

    public List<Member> basicSelectFrom(){
        return selectFrom(member)
                .fetch();
    }

    public Page<Member> searchPageByApplyPage(MemberSearchCondition condition, Pageable pageable){
        JPAQuery<Member> query = selectFrom(member)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );

        List<Member> content = getQuerydsl().applyPagination(pageable, query).fetch();
        return PageableExecutionUtils.getPage(content, pageable, query::fetchCount);
    }

    public Page<Member> applyPagination(MemberSearchCondition condition, Pageable pageable){
        return applyPagination(pageable, query ->
            query.selectFrom(member)
                 .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
        );
    }

    public Page<Member> applyPagination2(MemberSearchCondition condition, Pageable pageable){
            return applyPagination(
                    pageable,
                    query -> query
                        .selectFrom(member)
                        .leftJoin(member.team, team)
                        .where(
                                usernameEq(condition.getUsername()),
                                teamNameEq(condition.getTeamName()),
                                ageGoe(condition.getAgeGoe()),
                                ageLoe(condition.getAgeLoe())
                        ),
                    countQuery -> countQuery
                        .select(member.id)
                        .from(member)
                        .leftJoin(member.team, team)
                        .where(
                                usernameEq(condition.getUsername()),
                                teamNameEq(condition.getTeamName()),
                                ageGoe(condition.getAgeGoe()),
                                ageLoe(condition.getAgeLoe())
                        )
                    );
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
}
```

- 이를 통해서 기존에 MemberRepositoryImpl 에서 구현한 Pagination 보다 코드의 수가 줄어들고 우리가 딱 필요한 부분까지만 집중할 수 있어서 좋다. 

  - 뭐 페이징을 만들 때 항상 덧붙여지는 코드인 PageableExecutionUtils.getPage() 를 매번 쓰지 않아도 되고 
  
  - PageableExecutionUtils.getPage() 이 함수를 알지 않아도 된다. 
  
- 그리고 기존에 select 부터 하지 못하는 단점을 해결해줄 수 있다.   
