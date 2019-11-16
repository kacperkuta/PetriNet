package petrinet;

public class InputArc<T> extends Arc<T> {
    private int weight;

    public InputArc(T place, int weight) {
        super(place);
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
