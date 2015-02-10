package Messenger.Utils.openname;

import Messenger.Address;
import Messenger.TorLib;
import org.apache.http.HttpException;
import org.json.JSONException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;

/**
 * Created by chris on 2/8/15.
 */
public class OpennameUtils {

    public static String blockingOpennameDownload(String openname, String dataFolderPath){
        System.getProperties().put( "proxySet", "true" );
        System.getProperties().put( "socksProxyHost", "127.0.0.1" );
        System.getProperties().put( "socksProxyPort", "9150" );
        String formatted = "";
        try {
            JSONObject obj = getOneNameJSON(openname);
            JSONObject avi = obj.getJSONObject("avatar");
            JSONObject name = obj.getJSONObject("name");
            formatted = name.getString("formatted");
            String aviLocation = avi.getString("url");
            URL url = new URL(aviLocation);
            BufferedImage img = cropDownloadedAvatarImage(ImageIO.read(url));
            File outputfile = new File(dataFolderPath + "/avatars/" + openname + ".jpg");
            ImageIO.write(img, "jpg", outputfile);
        } catch (JSONException | IOException | HttpException e) {
            System.getProperties().put( "proxySet", "false" );
            return null;
        }
        System.getProperties().put( "proxySet", "false" );
        return formatted;
    }

    public static void downloadAvatar(String openname, String dataFolderPath,
                                      OpennameListener listener, Address addr){
        System.getProperties().put( "proxySet", "true" );
        System.getProperties().put( "socksProxyHost", "127.0.0.1" );
        System.getProperties().put( "socksProxyPort", "9150" );
        Runnable task = () -> {
            try {
                JSONObject obj = getOneNameJSON(openname);
                JSONObject avi = obj.getJSONObject("avatar");
                JSONObject name = obj.getJSONObject("name");
                String formattedName = name.getString("formatted");
                String aviLocation = avi.getString("url");
                URL url = new URL(aviLocation);
                BufferedImage img = cropDownloadedAvatarImage(ImageIO.read(url));
                File outputfile = new File(dataFolderPath + "/avatars/" + openname + ".jpg");
                ImageIO.write(img, "jpg", outputfile);
                listener.onDownloadComplete(addr, formattedName);
            } catch (JSONException | IOException | HttpException e) {
                listener.onDownloadFailed();
            }
        };
        Thread t = new Thread(task);
        t.start();
        System.getProperties().put( "proxySet", "false" );
    }

    private static JSONObject getOneNameJSON(String openname) throws JSONException, HttpException, IOException{
        return TorLib.getJSON("bitcoinauthenticator.org",
                    "onename.php?id=" + openname + ".json");
    }

    private static BufferedImage cropDownloadedAvatarImage(BufferedImage image) throws IOException {
        //Scale the image
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int width;
        int height;
        if (imageWidth > imageHeight) {
            Double temp = (73 / (double) imageHeight) * (double) imageWidth;
            width = temp.intValue();
            height = 73;
        } else {
            Double temp = (73 / (double) imageWidth) * (double) imageHeight;
            width = 73;
            height = temp.intValue();
        }
        BufferedImage scaledImage = new BufferedImage(width, height, image.getType());
        Graphics2D g = scaledImage.createGraphics();
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        //Crop the image
        int x, y;
        if (width > height) {
            y = 0;
            x = (width - 73) / 2;
        } else {
            x = 0;
            y = (height - 73) / 2;
        }
        return scaledImage.getSubimage(x, y, 73, 73);
    }
}
