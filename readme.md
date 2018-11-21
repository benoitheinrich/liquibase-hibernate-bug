# Description

Reproduces a problem with [liquibase-maven-plugin](https://github.com/liquibase/liquibase/tree/master/liquibase-maven-plugin) using the [liquibase-hibernate5](https://github.com/liquibase/liquibase-hibernate) extension.

The issue reported is about using the `liquibase:diff` goal which always drop and recreate indexes when there is no changes to the schema.

# How to reproduce

Checkout this repository and then run the following commands:

    mvn clean install
    mvn liquibase:dropAll
    mvn liquibase:update
    mvn liquibase:diff

Be aware that the `liquibase:dropAll` command will drop all tables in your DB, so make sure you run that into a test DB, or that you have a backup first.

In order to run this you'll need to make sure that you setup the properties in the pom.xml correctly.

    <db.driver>org.mariadb.jdbc.Driver</db.driver>
    <db.url>jdbc:mysql://localhost:3306/someTest?createDatabaseIfNotExist=true</db.url>
    <db.username>root</db.username>
    <db.password/>
    <db.dialect>org.hibernate.dialect.MySQL5Dialect</db.dialect>

This issue isn't only affecting MySQL but also affects Oracle DB.
I suspect this might actually affect all DBMS.
