/**
 * Created by chris on 2/2/15.
 */
public class Test {
    public static void main (String[] args){
        String time = String.valueOf(System.currentTimeMillis());
        time = time.substring(0, time.length() - 3) + "." + time.substring(time.length() - 3, time.length());
        System.out.print(time);
    }
}
