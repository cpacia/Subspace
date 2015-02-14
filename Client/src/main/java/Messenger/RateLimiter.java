package Messenger;

/**
 * Created by chris on 2/14/15.
 */
public class RateLimiter {

    static int max = 60;
    static int current = 60;
    static boolean resetting = false;

    public static boolean tryAcquire(int permits){
        if (permits <= current){
            current -= permits;
            Runnable task = () -> {
                if (!resetting) {
                    resetting = true;
                    while (current < max) {
                        try {Thread.sleep(1000);}
                        catch (InterruptedException e) {e.printStackTrace();}
                        current++;
                    }
                    resetting = false;
                }

            };
            new Thread(task).start();
            return true;
        }
        else {
            return false;
        }
    }

}
