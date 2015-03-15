package Messenger;

/** A class that can be use to limit the rate at which the user can send messages.*/
public class RateLimiter {

    //The maximum number of permits to accumulate
    static int max = 60;
    //The current number of permits available
    static int current = 60;
    //Boolean used to test whether the counter is resetting
    static boolean resetting = false;

    /**
     * Try to consume a number of permits.
     * If there aren't enough permits, return false.
     * Else, return true and start replenishing the permits.
     */
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
