package Messenger;

import java.io.File;
import java.io.IOException;

/**
 * Created by chris on 1/22/15.
 */
public class ApplicationParams {
    public static final String APP_NAME = "Subspace";
    private static File applicationDataFolder;
    private static OS_TYPE osType;

    public ApplicationParams(String[] args) throws IOException{
        String tmp = null;
        try {
            osType = OS_TYPE.operationSystemFromString(System.getProperty("os.name"));
            tmp = System.getProperty("user.home");
            switch (osType) {
                case WINDOWS:
                    tmp = System.getenv("APPDATA") + File.separator;
                    break;
                case LINUX:
                    tmp += File.separator;
                    break;
                case OSX:
                    tmp += File.separator + "Library" + File.separator + "Application Support" + File.separator + "";
                    break;
            }
        } catch (WrongOperatingSystemException e){e.printStackTrace();}
        this.applicationDataFolder = new File(tmp + APP_NAME + File.separator);
        if (!applicationDataFolder.exists()) {
            if (!applicationDataFolder.mkdirs()) {
                throw new IOException("Could not create directory " + applicationDataFolder.getAbsolutePath());
            }
        }
    }

    public File getApplicationDataFolder(){
        return this.applicationDataFolder;
    }


    public enum OS_TYPE {
        WINDOWS,
        LINUX,
        OSX;

        public static OS_TYPE operationSystemFromString(String OS) throws WrongOperatingSystemException {
            if(isWindows(OS))
                return OS_TYPE.WINDOWS;
            if(isLinux(OS))
                return OS_TYPE.LINUX;
            if(isMac(OS))
                return OS_TYPE.OSX;
            throw new WrongOperatingSystemException();

        }

        public static boolean isWindows(String OS) {

            return (OS.indexOf("win") >= 0 || OS.indexOf("Win") >= 0);

        }

        public static boolean isMac(String OS) {

            return (OS.indexOf("mac") >= 0 || OS.indexOf("Mac") >= 0);

        }

        public static boolean isLinux(String OS) {

            return (OS.indexOf("linux") >= 0 || OS.indexOf("Linux") >= 0);

        }
    }

    public static class WrongOperatingSystemException extends Exception {
        public WrongOperatingSystemException() {
            super();
        }
    }
}
