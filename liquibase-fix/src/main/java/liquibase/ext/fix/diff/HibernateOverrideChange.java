package liquibase.ext.fix.diff;

import liquibase.database.Database;
import liquibase.ext.hibernate.database.HibernateDatabase;

import static liquibase.diff.output.changelog.ChangeGenerator.PRIORITY_ADDITIONAL;

interface HibernateOverrideChange {
    // Need to use higher than the highest priority in order to make sure our class get run instead of the hibernate plugin class
    int THE_TOP_OF_THE_PRIORITY = PRIORITY_ADDITIONAL + 1;

    default boolean isHibernatePlugin(final Database referenceDatabase, final Database comparisonDatabase) {
        return referenceDatabase instanceof HibernateDatabase || comparisonDatabase instanceof HibernateDatabase;
    }
}
