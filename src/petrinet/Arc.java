package petrinet;

public abstract class Arc<T> {
    private T place;

    public T getPlace() {
        return place;
    }

    public Arc(T place) {
        this.place = place;
    }
}
