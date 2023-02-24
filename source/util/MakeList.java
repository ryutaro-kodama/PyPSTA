package util;

import java.util.ArrayList;
import java.util.List;

public class MakeList<T> {
    public static <T> List<T> make(T... elements) {
        ArrayList<T> result = new ArrayList<>();
        for (T element: elements) {
            result.add(element);
        }
        return result;
    }
}
