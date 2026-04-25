package api.giybat.uz.api.giybat.uz.util;

public class PageUtil {
    public static int page (int value){

        return value == 0 ? 1 : value - 1;
    }
}
