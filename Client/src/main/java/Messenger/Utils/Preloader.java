package Messenger.Utils;

import Messenger.*;
import Messenger.FileWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.bitcoinj.core.Utils;

import javax.imageio.ImageIO;
import java.io.*;

/**
 * Created by chris on 2/14/15.
 */
public class Preloader {

    static FileWriter f = new FileWriter();

    public static void set(){
        if (!f.getPreloaded()){
            try {
                InputStream inputStream = Main.class.getResourceAsStream("chrispacia.jpg");
                OutputStream outputStream = new FileOutputStream(
                        new File(Main.params.getApplicationDataFolder().toString() + "/avatars/chrispacia.jpg"));
                int read = 0;
                byte[] bytes = new byte[1024];
                while ((read = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
            } catch (IOException e){e.printStackTrace();}
            f.addContact("115rjnv5KYLMqunrcbqF91Jmnf94dMkWi9YdXJD3NREvHX5tj9oV", "Chris Pacia", "chrispacia");
            String roomName = "#dilithiumchamber";
            f.addChatRoom(roomName, false);
            byte[] privKey = Utils.doubleDigest(roomName.getBytes());
            ECKey ecKey = ECKey.fromPrivOnly(privKey);
            Address addr = null;
            try {addr = new Address(32, ecKey);
            } catch (InvalidPrefixLengthException e2) {e2.printStackTrace();}
            f.addKey(ecKey, roomName, 32, addr.toString(),
                    "bitcoinauthenticator.org", null, null);
            f.setPreloaded(true);
        }
    }

}
