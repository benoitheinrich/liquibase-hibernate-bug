package liquibase.ext.fix.diff;

import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import liquibase.change.Change;
import liquibase.database.Database;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.ChangeGeneratorChain;
import liquibase.diff.output.changelog.core.ChangedIndexChangeGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Index;

import java.util.function.Predicate;

public class MyChangedIndexChangeGenerator
        extends ChangedIndexChangeGenerator
        implements IgnorableChange {

    private static final String UNIQUE = "unique";

    @Override
    public int getPriority(final Class<? extends DatabaseObject> objectType, final Database database) {
        if (Index.class.isAssignableFrom(objectType)) {
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
        return Stream.of(rule(UNIQUE, Boolean.FALSE::equals));
    }
}
