/*
 * COPYRIGHT (c) 2018 VOCADO, LLC.  ALL RIGHTS RESERVED.  THIS SOFTWARE CONTAINS
 * TRADE SECRETS AND/OR CONFIDENTIAL INFORMATION PROPRIETARY TO VOCADO, LLC AND/OR
 * ITS LICENSORS. ACCESS TO AND USE OF THIS INFORMATION IS STRICTLY LIMITED AND
 * CONTROLLED BY VOCADO, LLC.  THIS SOFTWARE MAY NOT BE COPIED, MODIFIED, DISTRIBUTED,
 * DISPLAYED, DISCLOSED OR USED IN ANY WAY NOT EXPRESSLY AUTHORIZED BY VOCADO, LLC IN WRITING.
 */

package liquibase.ext.fix.diff;

import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import liquibase.change.Change;
import liquibase.database.Database;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.ChangeGeneratorChain;
import liquibase.diff.output.changelog.core.ChangedUniqueConstraintChangeGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.UniqueConstraint;

import java.util.function.Predicate;

public class MyChangedUniqueConstraintChangeGenerator
        extends ChangedUniqueConstraintChangeGenerator
        implements IgnorableChange {

    private static final String CLUSTERED = "clustered";

    @Override
    public int getPriority(final Class<? extends DatabaseObject> objectType, final Database database) {
        if (UniqueConstraint.class.isAssignableFrom(objectType)) {
            return THE_TOP_OF_THE_PRIORITY;
        }
        return PRIORITY_NONE;
    }

    @Override
    public Change[] fixChanged(final DatabaseObject changedObject, final ObjectDifferences differences, final DiffOutputControl control, final Database referenceDatabase, final Database comparisonDatabase, final ChangeGeneratorChain chain) {
        if (removeIgnorableChanges(differences, referenceDatabase, comparisonDatabase)) return null;

        return super.fixChanged(changedObject, differences, control, referenceDatabase, comparisonDatabase, chain);
    }

    @Override
    public Stream<Tuple2<String, Predicate<Object>>> getIgnorableChanges() {
        return Stream.of(rule(CLUSTERED, Boolean.FALSE::equals));
    }
}
