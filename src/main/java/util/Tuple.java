package util;

public class Tuple<T1, T2> {

    //NOTE: This file is from the bachelor project.

    //This class represents a tuple with two objects.

    public T1 x; //First value of the tuple.
    public T2 y; //Second value of the tuple.

    //Constructor taking the two values for the tuple.
    public Tuple(T1 x, T2 y) {
        this.x = x;
        this.y = y;
    }

}
