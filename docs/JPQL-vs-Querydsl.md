## JPQL vs Querydsl 

***

JPQL 대산 Querydsl 을 이용하는 이유 중 하나가 type-safe (컴파일 시점에 알 수 있는) 쿼리를 날리기 위해서 사용한다.

이 말은 JPQL 에서 쿼리에서 오타가 발생해도 컴파일 시점에서 알기 힘들다. 오로지 런타임에서만 체크가 가능하다. 하지만 Querydsl 은 컴파일 시점에 오류를 잡아줄 수 있기 떄문에 좋다.   

Querydsl 동작 하는 과정은 JPQL 을 거쳐서 SQL 로 변환되서 실행한다. 


#### Maven 에서 Querydsl 이용하기.

pom.xml 에서 다음과 같은 플러그인과 의존성을 추가해야 한다. 

그 후 Maven 에서 compile 하면 아래에 설정한 경로(target/generated-sources/java) 에 QClass 가 생긴다. 

##### 의존성 
```xml
<dependency>
    <groupId>com.querydsl</groupId>
    <artifactId>querydsl-jpa</artifactId>
    <version>4.3.1</version>
</dependency>
```

#### 플러그인 
```xml
<plugin>
    <groupId>com.mysema.maven</groupId>
    <artifactId>apt-maven-plugin</artifactId>
    <version>1.1.3</version>
    <executions>
        <execution>
            <goals>
                <goal>process</goal>
            </goals>
            <configuration>
                <outputDirectory>target/generated-sources/java</outputDirectory>
                <processor>com.querydsl.apt.jpa.JPAAnnotationProcessor</processor>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>com.querydsl</groupId>
            <artifactId>querydsl-apt</artifactId>
            <version>${querydsl.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

