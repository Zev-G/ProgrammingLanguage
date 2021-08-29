package parse.example.run;

import parse.ParseResult;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Checkpoints:
 * <ul>
 *     <li>0: Eval multiple is reached</li>
 *     <li>1: Eval multiple is reached with more than one item in results list</li>
 *     <li>2: After functions have been initialized in {@link Runner#run(RunContext, ParseResult, ERI)}</li>
 * </ul>
 */
public final class ERI /* ExtraRunInstructions */ {

    public static final ERI DEFAULT = new ERI();

    private boolean lookingForStaticClass = false;
    private final Map<Integer, Consumer<ERI>> checkPointActions = new HashMap<>();
    private boolean initializeFunctions = true;

    public ERI() {

    }

    public boolean isLookingForStaticClass() {
        return lookingForStaticClass;
    }

    public ERI changedLookingForStaticClass(boolean newVal) {
        ERI newEri = copy();
        newEri.lookingForStaticClass = newVal;
        return newEri;
    }
    public ERI changedInitializeFunctions(boolean newVal) {
        ERI newEri = copy();
        newEri.initializeFunctions = newVal;
        return newEri;
    }

    public ERI addCheckpoint(Integer at, Consumer<ERI> run) {
        checkPointActions.put(at, run);
        return this;
    }
    public ERI addAutoRemovingCheckpoint(Integer at, Consumer<ERI> run) {
        return addCheckpoint(at, eri -> {
            checkPointActions.remove(at);
            run.accept(eri);
        });
    }

    public void setLookingForStaticClass(boolean lookingForStaticClass) {
        this.lookingForStaticClass = lookingForStaticClass;
    }

    public void setInitializeFunctions(boolean initializeFunctions) {
        this.initializeFunctions = initializeFunctions;
    }

    public ERI copy() {
        ERI newEri = new ERI();
        newEri.lookingForStaticClass = lookingForStaticClass;
        newEri.checkPointActions.putAll(checkPointActions);
        return newEri;
    }

    public void reachedCheckpoint(Integer i) {
        if (checkPointActions.containsKey(i)) {
            checkPointActions.get(i).accept(this);
        }
    }

    public boolean initializeFunctions() {
        return initializeFunctions;
    }

}
