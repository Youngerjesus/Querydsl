## 기본 문법 

***

### 기본 Q-Type 활용

##### Q클래스 인스턴스를 사용하는 2가지 방법

```java
QMember qMember = new QMember("m"); //별칭 직접 지정
QMember qMember = QMember.member; //기본 인스턴스 사용
```

##### 기본 인스턴스를 static import 와 함께 사용하는 방법

```java
import static study.querydsl.domain.QMember.*;

@Test
public void startQuerydsl3() {
     Member findMember = queryFactory
     .select(member)
     .from(member)
     .where(member.username.eq("member1"))
     .fetchOne();
     
    assertThat(findMember.getUsername()).isEqualTo("member1");
}
```

##### application.yml 에 다음 설정을 추가하면 실행되는 JPQL을 볼 수 있다.

```yaml
spring.jpa.properties.hibernate.use_sql_comments: true
```

***

### 검색 조건 쿼리 

##### 기본 검색 쿼리 - and(), or() 사 

```java
@Test
public void search() {
     Member findMember = queryFactory
     .selectFrom(member)
     .where(member.username.eq("member1")
     .and(member.age.eq(10)))
     .fetchOne();
     
    assertThat(findMember.getUsername()).isEqualTo("member1");
}
```

- 검색 조건은 and(), or() 메소드를 체인으로 이용해 연결할 수 있다. 

- select(), from() 을 합친 selectFrom() 으로 함께 사용하는게 가능하다.

##### JPQL이 제공하는 모든 검색 조건 제공한다. 

```java
member.username.eq("member1") // username = 'member1'
member.username.ne("member1") //username != 'member1'
member.username.eq("member1").not() // username != 'member1'

member.username.isNotNull() //이름이 is not null

member.age.in(10, 20) // age in (10,20)
member.age.notIn(10,20) // age not in (10, 20)
member.age.between(10,30) // between 10, 230

member.age.goe(30) // age >= 30
member.age.gt(30) // age > 30
member.age.loe(30) // age <= 30
member.age.lt(30) // age < 30

member.username.like("member%") // like 검색
member.username.contains("member") // like %member% 검색
member.username.startswith("member") // like member% 검색
```

##### AND 조건을 파라미터로 처리할 수 있다. 

```java
@Test
public void searchAndParam() {
     List<Member> result1 = queryFactory
     .selectFrom(member)
     .where(member.username.eq("member1"), member.age.eq(10))
     .fetch();
     
     assertThat(result1.size()).isEqualTo(1);
}
```
- where() 절에 피라미터로 추가한다면 and 조건이 추가되는 것이다. 

***

### 결과 조회

- fetch() 메소드로 리스트를 조회할 수 있다. 데이터가 없으면 빈 리스트가 조회된다.

- fetchOne() 단 한건의 결과를 조회한다. 

  - 데이터가 없으면 Null 이 조회된다.
  
  - 결과가 둘 이상이면 com.querydsl.core.NonUniqueResultException 예외가 발생한다.  

- fetchFirst() 로 첫번째로 발견되는 결과를 조회할 수 있다.

  - limit(1).fetchOne() 과 동일하다. 
  
- fetchResults() 로 페이징을 포함한 결과를 조회할 수 있다. 

- fetchCount() 로 count 쿼리로 변경해서 count 수 조회가 가능하다. 

```java
//List
List<Member> fetch = queryFactory
        .selectFrom(member)
        .fetch();

//단 건
Member findMember1 = queryFactory
        .selectFrom(member)
        .fetchOne();

//처음 한 건 조회
Member findMember2 = queryFactory
        .selectFrom(member)
        .fetchFirst();

//페이징에서 사용
QueryResults<Member> results = queryFactory
        .selectFrom(member)
        .fetchResults();

//count 쿼리로 변경
long count = queryFactory
        .selectFrom(member)
        .fetchCount();
```  

***

### 정렬 

바로 예제부터 보면 정렬 순서는 다음과 같다.

- 1. 회원 나이 내림차순 (desc)

- 2. 회원 이름 오름차순 

- 단 2에서 회원 이름이 빈 값이라면 마지막으로 결과가 출력하도록 하겠다.

```java
@Test
void sort(){
    //given
    em.persist(new Member(null, 100));
    em.persist(new Member("member5", 100));
    em.persist(new Member("member6", 100));
    //when
    List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.eq(100))
            .orderBy(member.age.desc(), member.username.asc().nullsLast())
            .fetch();

    Member member5 = result.get(0);
    Member member6 = result.get(1);
    Member memberNull = result.get(2);
    //then
    assertEquals("member5", member5.getUsername());
    assertEquals("member6", member6.getUsername());
    assertNull(memberNull.getUsername());
}
``` 
 
- orderBy() 를 통해서 정렬을 시작할 수 있다
 
- desc() 를 이용해 정렬 기준 값에 내림차순으로 asc() 를 기준으로 오름차순으로 정렬할 수 있다.

- nullLast() 나 nullFirst() 로 null 데이터에 순서를 부여할 수 있다. 

***

### 페이징

##### 페이징 조회 개수 제한

```java
@Test
void paging1(){
    //given

    //when
    List<Member> result = queryFactory
            .selectFrom(member)
            .orderBy(member.username.asc())
            .offset(0) // 0부터 시작
            .limit(3)
            .fetch();
    Member member1 = result.get(0);
    //then
    assertEquals("member1", member1.getUsername());
    assertEquals(3, result.size());
}
```

##### 전체 조회 수가 필요한 경우

```java
@Test
void paging2(){
    //given

    //when
    QueryResults<Member> queryResults = queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(0)
            .limit(2)
            .fetchResults();

    //then
    assertEquals(4, queryResults.getTotal());
    assertEquals(2, queryResults.getLimit());
    assertEquals(0, queryResults.getOffset());
    assertEquals(2, queryResults.getResults().size());
}
```

- fetchResults() 를 이용하면 count 쿼리가 한번 나가고 select 쿼리가 이따라서 나가게 된다. 

  - 실무에서는 페이징 쿼리를 작성할 때 데이터를 조회하는 쿼리는 여러 테이블을 조인해서 하지만 count 자체는 그럴 필요가 없는 경우도 많다. 그런데 fetchResults() 를 하면 자동화된 count 쿼리가 나가니까 성능상으로 안 좋을수도 있다. 이 경우에는 따로 count 쿼리를 별도로 내는걸 추천한다. 
   

***

### 집합

- select.member.count() 을 통해서 멤버 숫자를 구할 수 있다.

- select.member.age.sum() 을 통해서 멤버 나이의 합을 구할 수 있다.

- select.member.age.avg() 을 통해서 멤버 나이의 평균 값을 구할 수 있다.

- select.member.age.max() 을 통해서 멤버 중 나이가 가장 많은 값을 구할 수 있다. 

- select.member.age.min() 을 통해서 멤버 중 나이가 가장 적은 사람을 구할 수 있다. 


##### 일반적인 집합 함수 사용 예
```java
@Test
void aggregation(){
    //given

    //when
    List<Tuple> result = queryFactory
            .select(
                    member.count(),
                    member.age.sum(),
                    member.age.avg(),
                    member.age.max(),
                    member.age.min()
            )
            .from(member)
            .fetch();
    //then
    Tuple tuple = result.get(0);
    assertEquals(4, tuple.get(member.count()));
    assertEquals(100, tuple.get(member.age.sum()));
    assertEquals(25, tuple.get(member.age.avg()));
    assertEquals(40, tuple.get(member.age.max()));
    assertEquals(10, tuple.get(member.age.min()));
}
```
- select 에서 내가 원하는 데이터를 타입이 여러개라면 Tuple 로 결과를 조회한다. Tuple 은 querydsl 에서 제공하는 자료구조다.

- 사용방법은 select 에서 했던 방식과 동일하다.

- 나가는 쿼리도 SQL 이랑 같다. 

- 실무에서는 Tuple 로 뽑기보다는 DTO 로 가져오도록 많이 한다. 


##### groupBy() 예제 

````java
@Test
void group(){
    //given
    
    //when
    List<Tuple> result = queryFactory
            .select(team.name, member.age.avg())
            .from(member)
            .join(member.team, team)
            .groupBy(team.name)
            .fetch();

    Tuple teamA = result.get(0);
    Tuple teamB = result.get(1);

    //then
    assertEquals("TeamA" ,teamA.get(team.name));
    assertEquals(15, teamA.get(member.age.avg()));
    assertEquals("TeamB" ,teamB.get(team.name));
    assertEquals(35, teamA.get(member.age.avg()));
}
````

##### having() 예제  

````java
@Test
void having(){
    //given

    //when
    List<Tuple> result = queryFactory
            .select(team.name, member.age.avg())
            .from(member)
            .join(member.team, team)
            .groupBy(team.name)
            .having(member.age.avg().gt(20))
            .fetch();

    Tuple teamB = result.get(0);
    //then
    assertEquals("TeamB", teamB.get(team.name));
    assertEquals(35, teamB.get(member.age.avg()));
}
````

***

### 조인 - 기본 조인

조인의 기본  문법은 첫번째 피라미터에 조인 대상을 지정하고 두 번째 파라미터에 별칭(alias) 를 사용할 Q 타입 클래스를 지정하면 된다. 

##### 기본 조인

```java
@Test
void join(){
    //given

    //when
    List<Member> result = queryFactory
            .selectFrom(member)
            .join(member.team, team)
            .where(team.name.eq("TeamA"))
            .fetch();
    //then
    assertThat(result)
            .extracting("username")
            .containsExactly("member1", "member2");
}
```
- join 은 innerJoin 말고 leftJoin 이나 rightJoin 도 할 수 있다. 

- join 이후에 on 을 넣어서 대상 지정을 넣을수도 있다. 

- 그리고 연관관계가 없어도 조인을 할 수 있는 세타 조인도 할 수 있다. 

##### 세타 조인

```java
@Test
void thetaJoin(){
    //given
    em.persist(new Member("TeamA"));
    em.persist(new Member("TeamB"));
    //when
    List<Member> result = queryFactory
            .select(member)
            .from(member, team)
            .where(member.username.eq(team.name))
            .fetch();
    //then
    Member memberA = result.get(0);
    Member memberB = result.get(1);

    assertEquals("TeamA", memberA.getUsername());
    assertEquals("TeamB", memberB.getUsername());
}
```

- 세타 조인은 연관관계가 없어도 데이터를 다 가지고온 다음 조인을 하는 방식이다. 

- from 에 여러 엔터티를 가지고와서 세타 조인을 한다.

***

### 조인 - On 절

ON 절을 활용한 조인은 조인 대상 을 필터링 해주고 연관관게가 없는 엔터티는 외부 조인을 활욯할 수 있다. 

- ON 절과 WHERE 절의 차이는 ON 절 같은 경우는 JOIN 할 데이터를 필터하기 위해서 사용하는 반면에 WHERE 절은 JOIN 을 하고나서 데이터를 필터하기 위해서 사용한다고 생각하면 된다. 즉 ON 절이 WHERE 절보다 먼저 실행이 되고 이는 LEFT_OUTER_JOIN 을 하면 뚜렷히 드러난다. 

##### ON 절 예제 
```java
@Test
@DisplayName("예) 회원과 팀을 조인하면서, 팀 이름이 TeamA인 팀만 조인 하고 회원은 모두 조회한다.")
void joinOnFiltering(){
    //given

    //when
    List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(member.team, team)
            .on(team.name.eq("TeamA"))
            .fetch();
    //then
    for (Tuple tuple : result) {
        System.out.println(tuple);
    }
}
```
- ON 절을 활용해서 조인 대상을 필터링 할 때 INNER JOIN 을 이용한다면 WHERE 과 결과적으로 동일하다. 

- OUTER JOIN 에서만 ON 절이 달라진다. 

##### 연관관계가 없는 엔터티를 외부 조인할 경우에 ON 절이 쓰인다. 

````java
@Test
@DisplayName("연관관계가 없는 엔터티 외부 조인으로 회원의 이름과 팀 이름이 같은 회원을 조인한다.")
void joinOnNoRelation(){
    //given
    em.persist(new Member("TeamA"));
    em.persist(new Member("TeamB"));
    em.persist(new Member("TeamC"));
    //when
    List<Tuple> result = queryFactory
            .select(member,team)
            .from(member)
            .leftJoin(team).on(member.username.eq(team.name))
            .fetch();

    for (Tuple tuple : result) {
        System.out.println(tuple);
    }
}
````

- 원래 join 을 할 때는 leftJoin(member.team, team) 을 통해서 member 가 가진 FK 를 통해서 조인을 한다. 하지만 여기서는 연관관계가 없으므로 이렇게 하지 않고 leftJoin(team).on()  절을 통해서 team 으로 조인을 하는데 이 조건이 ON 절에 담기게 해서 조인을 한다. 
 
***

### 조인 - 패치조인

패치조인은 SQL 에서 제공하는 기능은 아니다. JPA 에서 주로 성능 최적화를 위해서 사용하는 기능이다. 

##### 패치 조인 미적용

```java
@PersistenceUnit
EntityManagerFactory emf;

@Test
@DisplayName("패치조인을 사용하지 않고 데이터를 가지고 오는 방법이다.")
void fetchJoinNo(){
    //given
    em.flush();
    em.clear();
    //when
    Member member = queryFactory
            .selectFrom(QMember.member)
            .where(QMember.member.username.eq("member1"))
            .fetchOne();
    
    //then
    boolean isLoaded = emf.getPersistenceUnitUtil().isLoaded(member.getTeam());
    assertEquals(false, isLoaded);
}
```

````text
Hibernate: 
    select
        member0_.member_id as member_i1_0_,
        member0_.age as age2_0_,
        member0_.team_id as team_id4_0_,
        member0_.username as username3_0_ 
    from
        member member0_ 
    where
        member0_.username=?
Member(id=3, username=member1, age=10)
````

- Member 를 조회하면 Lazy 로딩 적용을 했으므로 쿼리를 보면 팀을 조회하지 않고 멤버만 조회한다.

##### 패치 조인을 사용해서 데이터를 가지고 오는 방법이다. 

```java
@Test
@DisplayName("패치조인을 사용해서 데이터를 가지고 오는 방법이다.")
void fetchJoinYes(){
    //given
    em.flush();
    em.clear();
    //when
    Member findMember = queryFactory
            .selectFrom(QMember.member)
            .join(member.team, team).fetchJoin()
            .where(QMember.member.username.eq("member1"))
            .fetchOne();
    //then
    boolean isLoaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertEquals(true, isLoaded);
}
```
```text
Hibernate: 
    select
        member0_.member_id as member_i1_0_0_,
        team1_.team_id as team_id1_1_1_,
        member0_.age as age2_0_0_,
        member0_.team_id as team_id4_0_0_,
        member0_.username as username3_0_0_,
        team1_.name as name2_1_1_ 
    from
        member member0_ 
    inner join
        team team1_ 
            on member0_.team_id=team1_.team_id 
    where
        member0_.username=?
```

- 쿼리를 보면 멤버를 조회할 때 쿼리도 같이 가지고 오는 걸 볼 수 있다.

***

### 서브 쿼리 

서브 쿼리란 SELECT 문 안에 다시 SELECT 문이 기술된 형태의 쿼리로 안에 있는 결과를 밖에서 받아서 처리하는 구조다.

단일 SELECT 문으로 조건식을 만들기가 복잡한 경우에 또는 완전히 다른 테이블에서 값을 조회해서 메인 쿼리로 이용하고자 할 때 사용한다.

서브 쿼리는 from 절은 안되고 select 절이나 where 절에서만 가능하다. (원래는 select 절에도 안된다.)

JPAExpressions 을 static import 해서 줄이는 것도 추천한다. 

##### SubQuery 예제 - 나이가 가장 많은 사람.

```java
@Test
@DisplayName("나이가 가장 많은 회원을 조회한다고 해보자.")
void subQuery(){
    //given
    QMember qMember = new QMember("m"); // alias 가 중복되면 안되므로 QMember 를 만들어줘야 한다.

    //when
    Member findMember = queryFactory
            .selectFrom(member)
            .where(member.age.eq(
                    JPAExpressions
                            .select(qMember.age.max())
                            .from(qMember)

            ))
            .fetchOne();
    //then
    assertEquals(40, findMember.getAge());
}
```

##### SubQuery 예제 - 나이가 평균 보다 같거나 큰 사람들 조회.

```java
@Test
@DisplayName("나이가 평균 이상인 회원을 조회한다고 해보자.")
void subQueryAvg(){
    //given
    QMember qMember = new QMember("m"); // alias 가 중복되면 안되므로 QMember 를 만들어줘야 한다.

    //when
    List<Member> findMembers = queryFactory
            .selectFrom(member)
            .where(member.age.goe(
                    JPAExpressions
                            .select(qMember.age.avg())
                            .from(qMember)

            ))
            .fetch();
    //then
    assertEquals(2, findMembers.size());
}
```

##### SubQuery 예제 - In 을 이용 

```java
@Test
@DisplayName("나이가 10인 회원을 조회한다고 해보자. - In 을 이용")
void subQueryIn(){
    //given
    QMember qMember = new QMember("m"); // alias 가 중복되면 안되므로 QMember 를 만들어줘야 한다.

    //when
    List<Member> findMembers = queryFactory
            .selectFrom(member)
            .where(member.age.in(
                    JPAExpressions
                            .select(qMember.age)
                            .from(qMember)
                            .where(qMember.age.in(10))
            ))
            .fetch();
    //then
    assertEquals(1, findMembers.size());
    assertEquals(10, findMembers.get(0).getAge());
}
```

***

### Case 문

조건에 따라서 값을 지정해주는 CASE 문은 select, 조건절(where), orderBy 에서 사용이 가능하다.

##### 일반적인 Case 문

```java
@Test
void baseCase(){
    //given

    //when
    List<String> result = queryFactory
            .select(member.age
                    .when(10).then("열살")
                    .when(20).then("스무살")
                    .otherwise("기타"))
            .from(member)
            .fetch();
    //then
    for (String s : result) {
        System.out.println(s);
    }
}
```

##### 조금 복잡한 Case 문 

```java
@Test
void complexCase(){
    //given

    //when
    List<String> result = queryFactory
            .select(new CaseBuilder()
                    .when(member.age.between(0, 20)).then("0~20살")
                    .when(member.age.between(21, 30)).then("21~30살")
                    .otherwise("기타")
            )
            .from(member)
            .fetch();

    //then
    for (String s : result){
        System.out.println(s);
    }
}
```

- CaseBuilder() 를 이용하면 간단하다. 

##### 복잡한 Case 문 

- 0 ~ 30살이 아닌 회원을 가장 먼저 출력

- 0 ~ 20살 회원 출력

- 21 ~ 30살 회원 출력

````java
@Test
@DisplayName("0 ~ 30 살이 아닌 사람을 가장 먼저 출력하고 그 다음 0 ~ 20살, 그 다음 21살 ~ 30살 출력한다.")
void complexCase2(){
    //given
    NumberExpression<Integer> rankCase = new CaseBuilder()
            .when(member.age.between(0, 20)).then(2)
            .when(member.age.between(21, 30)).then(1)
            .otherwise(3);
    //when
    List<Tuple> result = queryFactory
            .select(member.username, member.age, rankCase)
            .from(member)
            .orderBy(rankCase.desc())
            .fetch();
    //then
    for(Tuple tuple : result) {
        System.out.println(tuple);
    }
}
````

````text
[member4, 40, 3]
[member1, 10, 2]
[member2, 20, 2]
[member3, 30, 1]
````
***

### 상수 더하기 

상수가 필요하다면 Expressions.constant() 를 사용하면 된다. 줄여서 쓰고 싶다면 static import 를 사용하자.

##### 상수 더하기 예제 

```java
@Test
void addConstant(){
    //given
    //when
    List<Tuple> result = queryFactory
            .select(member.username, constant("A"))
            .from(member)
            .fetch();
    
    //then
    for (Tuple tuple : result) {
        System.out.println(tuple);
    }
}
```

````text
[member1, A]
[member2, A]
[member3, A]
[member4, A]
````

***

### 문자 더하기 

문자를 더할거면 concat 을 이용하면 편하다. 

##### 문자 더하기 예제 

````java
@Test
void addConcat(){
    //given

    //when
    String result = queryFactory
            .select(member.username.concat("_").concat(member.age.stringValue()))
            .from(member)
            .where(member.username.eq("member1"))
            .fetchOne();
    //then
    System.out.println(result);
}
````

```text
member1_10
```