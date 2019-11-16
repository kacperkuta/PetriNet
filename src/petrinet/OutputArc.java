package petrinet;

public class OutputArc<T> extends InputArc<T> {
    public OutputArc(T place, int weight) {
        super(place, weight);
    }
}
