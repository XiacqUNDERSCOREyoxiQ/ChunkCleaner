package xiacq.chunkcleaner.core.utility;

public class Pair<T> {

    private final T KEY;
    private final T VALUE;

    public Pair(T key, T value) {
            this.KEY = key;
            this.VALUE = value;
    }

    public T getKey() {return this.KEY;}
    public T getValue() {return this.VALUE;}

}
