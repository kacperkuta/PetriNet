package petrinet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Transition<T> {

    private final Set<InputArc<T>> inputArcs;
    private final Set<OutputArc<T>> outputArcs;
    private final Set<InhibitorArc<T>> inhibitorArcs;
    private final Set<ResetArc<T>> resetArcs;

    public Transition(Map<T, Integer> input, Collection<T> reset, Collection<T> inhibitor, Map<T, Integer> output) {
        inputArcs = new HashSet<>();
        for (Map.Entry<T, Integer> e : input.entrySet()) {
            inputArcs.add(new InputArc<>(e.getKey(), e.getValue()));
        }
        outputArcs = new HashSet<>();
        for (Map.Entry<T, Integer> e : output.entrySet()) {
            outputArcs.add(new OutputArc<>(e.getKey(), e.getValue()));
        }
        inhibitorArcs = new HashSet<>();
        for (T p : inhibitor) {
            inhibitorArcs.add(new InhibitorArc<>(p));
        }
        resetArcs = new HashSet<>();
        for (T p : reset) {
            resetArcs.add(new ResetArc<>(p));
        }
    }

    public Set<InputArc<T>> getInputArcs() {
        return new HashSet<InputArc<T>>(inputArcs);
    }

    public Set<InhibitorArc<T>> getInhibitorArcs() {
        return new HashSet<>(inhibitorArcs);
    }

    public Set<OutputArc<T>> getOutputArcs() {
        return new HashSet<>(outputArcs);
    }

    public Set<ResetArc<T>> getResetArcs() {
        return new HashSet<>(resetArcs);
    }
}
