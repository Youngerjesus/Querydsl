## 중급 문법 

***

### Projection 과 결과 반환

Projection 은 엔터티를 그냥 그대로 가지고 오는게 아니라 필요한 필드만 가지고 오는 걸 말한다. 

Querydsl 에서는 프로젝션 대상이 하나면 명확한 타입을 지정할 수 있지만 프로젝션 대상이 둘 이상이라면 Tuple 이나 DTO 로 조회해야 한다.

##### 프로젝션 대상이 하나일 경우 예제

```java
@Test
void projectionOne(){
    //given
    
    //when
    List<String> result = queryFactory
            .select(member.username)
            .from(member)
            .fetch();
    //then
    for(String s : result){
        System.out.println(s);
    }
}
```

````text
member1
member2
member3
member4
````

##### 프로젝션 대상이 두개 이상인 경우에 예제 

```java
@Test
void projectionTwo(){
    //given

    //when
    List<Tuple> result = queryFactory
            .select(member.username, member.age)
            .from(member)
            .fetch();
    //then
    for(Tuple tuple : result) {
        System.out.println(tuple.get(member.username));
        System.out.println(tuple.get(member.age));
    }
}
```
````text
member1
10
member2
20
member3
30
member4
40
````

***

### Projection 과 결과 반환 - DTO 조회 

##### 기존에 Jpa 를 이용해서 Dto 를 만드는 예제

```java
@Test
void projectionWithJpa(){
    //given

    //when
    List<MemberDto> result = em.createQuery(
            "select new com.study.querydsl.dto.MemberDto(m.username, m.age)" +
                    "from Member m", MemberDto.class
            )
            .getResultList();
    //then
    for (MemberDto memberDto : result){
        System.out.println(memberDto.toString());
    }
}
```

````text
MemberDto(username=member1, age=10)
MemberDto(username=member2, age=20)
MemberDto(username=member3, age=30)
MemberDto(username=member4, age=40)
````

- 순수 JPA 에서 DTO 를 조회할 때는 new 키워드를 이용한 생성자를 통해서만 가능했다. 

- 그리고 package 이름을 모두 명시해야해서 좀 지저분함이 있었다. 

#### Querydsl 을 이용한 빈 생성 

DTO 를 반환하는 방법이 크게 3가지가 있다. 

- 프로퍼티로 접근하는 방식 (Setter 사용)

- 필드 직접 접근

- 생성자를 사용

#### 프로퍼티 setter 를 이용해 생성하는 방식 

````java
@Test
void findDtoBySetter(){
    //given
    //when
    List<MemberDto> result = queryFactory
            .select(Projections.bean(MemberDto.class,
                    member.username,
                    member.age))
            .from(member)
            .fetch();
    //then
    for (MemberDto memberDto : result) {
        System.out.println(memberDto.toString());
    }
}
````

````text
MemberDto(username=member1, age=10)
MemberDto(username=member2, age=20)
MemberDto(username=member3, age=30)
MemberDto(username=member4, age=40)
````

- Projections.bean() 을 사용하면 기본 생성자와 setter 를 통해서 객체를 만들게 된다.


##### 필드 직접 접근해서 생성하는 방법 

```java
@Test
void findDtoByField(){
    //given
    //when
    List<MemberDto> result = queryFactory
            .select(Projections.fields(MemberDto.class,
                    member.username,
                    member.age))
            .from(member)
            .fetch();
    //then
    for (MemberDto memberDto : result) {
        System.out.println(memberDto.toString());
    }
}
```

````text
MemberDto(username=member1, age=10)
MemberDto(username=member2, age=20)
MemberDto(username=member3, age=30)
MemberDto(username=member4, age=40)
````

- Projections.fields() 를 통해서 getter setter 필요없이 바로 필드로 직접 주입해서 사용한다.

  - private 로 선언해도 상관없다. 사실상 자바 리플렉션을 이용하면 private 상관없이 다 알수있다.
  
- 필드 주입할땐 dto 필드 이름과 QMember.member 의 필드 이름과 매칭이 되야 한다. 그래야 찾을 수 있곘지.  

##### 필드 직접 접근을 이용해 생성하는 방법 2 - 필드 이름이 서로 다를 경우

```java
@Data
public class UserDto {
    private String name;
    private int age;

    public UserDto(){}

    public UserDto(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

```java
@Test
void findUserDto(){
    //given

    //when
    List<UserDto> result = queryFactory
            .select(Projections.fields(UserDto.class,
                    member.username.as("name"),
                    member.age))
            .from(member)
            .fetch();
    //then
    for (UserDto userDto : result){
        assertNotNull(userDto.getName());
    }
}
```

##### 필드 직접 접근을 이용해 생성하는 방법 2 - subQuery 를 이용하는 경우 

```java
@Test
void findUserDtoBySubQuery(){
    //given
    QMember memberSub = new QMember("memberSub");
    //when
    List<UserDto> result = queryFactory
            .select(Projections.fields(UserDto.class,
                    member.username.as("name"),
                    ExpressionUtils.as(
                            JPAExpressions
                            .select(memberSub.age.max())
                            .from(memberSub), "age"
                    )))
            .from(member)
            .fetch();
    //then
    for (UserDto userDto : result){
        assertNotNull(userDto.getName());
        assertEquals(40, userDto.getAge()); // 최대 나이가 40살 이다. 
    }
}
```

 

##### 생성자를 이용해 생성하는 방법

```java
@Test
void findDtoByConstructor(){
    //given
    //when
    List<MemberDto> result = queryFactory
            .select(Projections.constructor(MemberDto.class,
                    member.username,
                    member.age))
            .from(member)
            .fetch();
    //then
    for (MemberDto memberDto : result) {
        System.out.println(memberDto.toString());
    }
}
```

```text
MemberDto(username=member1, age=10)
MemberDto(username=member2, age=20)
MemberDto(username=member3, age=30)
MemberDto(username=member4, age=40)
```  
 
- Projections.constructor() 를 이용해서 생성자 를 통해서 Dto 를 만들 수 있다.

***

### Projection 과 결과 반환 - @QueryProjection 

프로젝션을 이용한 방법 중에 가장 깔끔한 방법일 수 있다. 

@QueryProjection 을 이용해 DTO 도 Q타입의 클래스를 만들어서 이를 이용해 바로 만드는 방법이다. 

Q타입의 클래스를 제공해주니 type-safe 하다는 장점이 있다.

##### @QueryProjection 을 이용해 생성하는 방법

```java
@Data
public class MemberDto {
    private String username;
    private int age;

    public MemberDto(){

    }

    @QueryProjection // 생성자에 @QueryProjection 이 붙는다. 이후 빌드 툴을 이용해 compile 하면 Q타입의 클래스가 생성된다.
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
``` 

```java
@Test
void findDtoByQueryProjection(){
    //given

    //when
    List<MemberDto> result = queryFactory
            .select(new QMemberDto(member.username, member.age))
            .from(member)
            .fetch();
    //then
    for (MemberDto memberDto : result) {
        System.out.println(memberDto.toString());
    }
}
```

```text
MemberDto(username=member1, age=10)
MemberDto(username=member2, age=20)
MemberDto(username=member3, age=30)
MemberDto(username=member4, age=40)
```

- Projections.constructor() 와의 차이는 컴파일 오류를 못잡는다는 문제가 생긴다. 위 방식이 좀 더 안정성이 있다. 

- 다만 이 방식의 문제점은 Querydsl 에 대한 의존성을 가지게 된다는 점이다. 라이브러리를 바꾸게 된다면 고쳐야할 DTO 가 많아진다는 단점이 있다.

***

### 동적 쿼리 - BooleanBuilder 사용 

실행시에 쿼리 문장이 만들어져 실행되는 쿼리문을 동적 쿼리라고 하는데 동적으로 변수를 받아서 쿼리가 완성되는 걸 말한다.

Querydsl 에서 동적 쿼리를 만드는 방법은 두가지 방식이 있다.

- BooleanBuilder

- Where 다중 피라미터 사용 

##### BooleanBuilder 사용예제

```java
@Test
void dynamicQueryUsingBooleanBuilder(){
    //given
    String usernameParam = "member1";
    Integer ageParam = 10;
    //when
    List<Member> result = searchMember1(usernameParam, ageParam);
    //then
    assertEquals(1, result.size());
}

private List<Member> searchMember1(String usernameParam, Integer ageParam) {
    BooleanBuilder builder = new BooleanBuilder();
    if(usernameParam != null) {
        builder.and(member.username.eq(usernameParam));
    }

    if(ageParam != null) {
        builder.and(member.age.eq(ageParam));
    }


    return queryFactory
            .selectFrom(member)
            .where(builder)
            .fetch();
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
        and member0_.age=?
````

- BooleanBuilder 객체를 생성할때 초기값을 넣어줄 수도 있다. 

***

### 동적 쿼리 - Where 다중 파라미터 사용

이 방법이 더 코드가 깔끔하게 나온다. 실무에서 좀 더 사용하기에 좋다. 

##### Where 피라미터 이용 예제 

````java
@Test
void dynamicQueryUsingWhereParameter(){
    //given
    String usernameParam = "member1";
    Integer ageParam = 10;
    //when
    List<Member> result = searchMember2(usernameParam, ageParam);
    //then
    assertEquals(1, result.size());
}

private List<Member> searchMember2(String usernameCond, Integer ageCond) {
    return queryFactory
            .selectFrom(member)
            .where(usernameEq(usernameCond), ageEq(ageCond))
            .fetch();
}

private Predicate usernameEq(String usernameCond) {
    if(usernameCond == null) return null;

    return member.username.eq(usernameCond);
}

private Predicate ageEq(Integer ageCond) {
    if(ageCond == null) return null;

    return member.age.eq(ageCond);
}
````

- usernameEq() 메소드가 null 을 리턴하게 되면 Where() 에 null 값이 들어가게 되는데 이는 무시가 된다. 그러므로 동적 쿼리가 될 수 있다.

- BooleanBuilder 를 보는 것보다 Where 절에 적절한 메소드를 넣음으로써 가독성을 높일 수 있다. BooleanBuilder 는 객체를 또 봐야한다. 

##### Where 피라미터 조립 예제 

```java
@Test
void dynamicQueryUsingWhereParameter2(){
    //given
    String usernameParam = "member1";
    Integer ageParam = 10;
    //when
    List<Member> result = searchMember3(usernameParam, ageParam);
    //then
    assertEquals(1, result.size());
}

private List<Member> searchMember3(String usernameParam, Integer ageParam) {
    return queryFactory
            .selectFrom(member)
            .where(allEq(usernameParam, ageParam))
            .fetch();
}

private BooleanExpression allEq(String usernameParam, Integer ageParam) {
    return usernameEq1(usernameParam).and(ageEq(ageParam));
}

private BooleanExpression usernameEq1(String usernameCond) {
    if(usernameCond == null) return null;

    return member.username.eq(usernameCond);
}

private BooleanExpression ageEq1(Integer ageCond) {
    if(ageCond == null) return null;

    return member.age.eq(ageCond);
}
```

- 조건 조립을 통해서 추상화를 적절히 할 수 있다는 장점과 재사용성이 높다는 장점이 있다. 
 
***

## 수정 및 삭제 배치쿼리

쿼리 한번으로 대량의 데이터를 수정하는 방식에 관한 것이다. 이를 벌크 연산이라고 한다.

##### 벌크 연산 예제 - 수정

```java
@Test
void bulkUpdate(){
    //given

    //when
    long count = queryFactory
            .update(member)
            .set(member.username, "비회원")
            .where(member.age.lt(28))
            .execute();
    //then
    assertEquals(2, count);
}
```  
```text
Hibernate: 
    update
        member 
    set
        username=? 
    where
        age<?
```

- 벌크 연산은 조심해야 되는게 있다. JPA 에는 영속성 컨택스트가 메모리에 올라와 있다. 하지만 벌크 연산은 DB 에 바로 반영하는거기 때문에 영속성 컨택스트의 상태와 DB 의 상태가 달라지게 된다. 

- 즉 벌크 연산을 한 후에 fetch() 로 데이터를 조회할려고 해도 영속성 컨택스트에 값이 있다면 변경된 값을 DB 에서 가지고 와도 1차 캐시에 있는 값을 전달해준다. 


##### 벌크 수정 연산 후 데이터 가져오기 - 영속성 컨택스트에서 가져오므로 반영이 안됨.  
```java
@Test
@DisplayName("벌크 수정 연산 후 데이터 가져오기 - 영속성 컨택스트에서 가져오므로 반영이 안됨.")
void bulkUpdateAndFetch(){
    //given

    //when
    queryFactory
            .update(member)
            .set(member.username, "비회원")
            .where(member.age.lt(28))
            .execute();

    List<Member> result = queryFactory
            .selectFrom(member)
            .fetch();
    //then
    for (Member member : result) {
        System.out.println(member.getUsername() + " " + member.getAge());
    }
}
```

````text
member1 10
member2 20
member3 30
member4 40
````

- 변경된 값을 가지고 오기 위해서는 em.flush() 와 em.clear() 를 통해서 영속성 컨택스트 값을 버리면 된다. 

##### 벌크 연산 - 모든 나이 + 1 하기.

```java
@Test
void bulkAdd(){
    //given

    //when
    long count = queryFactory
            .update(member)
            .set(member.age, member.age.add(1))
            .execute();
    //then
    assertEquals(4, count);
}
```

````text
Hibernate: 
    update
        member 
    set
        age=age+?
````

##### 벌크 연산 - 모든 나이 * 2 하기.

```java
@Test
void bulkMultiply(){
    //given

    //when
    long count = queryFactory
            .update(member)
            .set(member.age, member.age.multiply(2))
            .execute();
    //then
    assertEquals(4, count);
}
```

##### 벌크 연산 - 18세 이상 모든 회원 지우기   

```java
@Test
void bulkDelete(){
    //given

    //when
    long count = queryFactory
            .delete(member)
            .where(member.age.gt(18))
            .execute();
    //then
}
```

```text
Hibernate: 
    delete 
    from
        member 
    where
        age>?
```

***

### SQL Function 호출하기 

SQL Function 은 JPA 와 같이 Dialect 에 등록된 내용만 호출할 수 있다. 

##### replace 이용하기 

```java
@Test
void sqlFunction(){
    //given

    //when
    List<String> result = queryFactory
            .select(Expressions.stringTemplate("function('replace',{0},{1},{2})"
                    , member.username, "member", "M"))
            .from(member)
            .fetch();
    //then
    for (String s : result){
        System.out.println(s);
    }
}
```

````text
M1
M2
M3
M4
````

- replace 는 지금 여기서 사용하고 있는 h2 dialect 에 등록되어있는 함수다. 이는 H2Dialect 클래스에서 볼 수 있다. 

