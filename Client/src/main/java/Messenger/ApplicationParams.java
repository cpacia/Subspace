package Messenger;

import java.io.File;
import java.io.IOException;

/**
 * This class holds the location of the application data directory as well as the OS type
 */
public class ApplicationParams {

    //Name of the appliction. Will be used when creating the data folder.
    public static final String APP_NAME = "Subspace";
    //File object of the data directory
    private static File applicationDataFolder;
    //The type of OS the application is running on.
    private static OS_TYPE osType;

    /**Gets the OS type and data folder. Creates a new data folder if it doesn't exist.*/
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

    /**Returns the application data directory*/
    public File getApplicationDataFolder(){
        return this.applicationDataFolder;
    }

    /**Returns the type of OS*/
    public OS_TYPE getOsType(){
        return osType;
    }

    /**Enum for defining the OS type*/
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

    /**Exception if we don't recognize the OS*/
    public static class WrongOperatingSystemException extends Exception {
        public WrongOperatingSystemException() {
            super();
        }
    }
}
