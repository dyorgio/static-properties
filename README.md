Static Properties [![Build Status](https://travis-ci.org/dyorgio/static-properties.svg?branch=master)](https://travis-ci.org/dyorgio/static-properties)
===============
An annotation processor for static property navigation

Usage
-----
Using [Mysema Maven APT plugin](https://github.com/querydsl/apt-maven-plugin):

```.xml
<plugin>
    <groupId>com.mysema.maven</groupId>
    <artifactId>maven-apt-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>process</goal>
            </goals>
            <phase>generate-sources</phase>
            <configuration>
                <processor>dyorgio.apt.staticproperties.AnnotationProcessor</processor>
                <options>
                    <staticproperties.topAnnotations>annotation1,annotation2...</staticproperties.topAnnotations>
	            </options>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>dyorgio.apt</groupId>
            <artifactId>static-properties</artifactId>
            <classifier>apt</classifier>
        </dependency>
    </dependencies>
</plugin>
```

