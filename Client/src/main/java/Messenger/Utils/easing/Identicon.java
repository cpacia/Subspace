package Messenger.Utils.easing;

import com.docuverse.identicon.IdenticonRenderer;
import com.docuverse.identicon.NineBlockIdenticonRenderer2;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.MessageDigest;

/**
 * Created by chris on 1/29/15.
 */
public class Identicon {

    private static final String IDENTICON_IMAGE_FORMAT = "PNG";

    public static ImageView generate(String name, Color color) throws Exception{
        IdenticonRenderer renderer = new NineBlockIdenticonRenderer2(color);
        MessageDigest md;
        md = MessageDigest.getInstance("SHA1");
        byte[] hashedIp = md.digest(name.getBytes("UTF-8"));
        int code = ((hashedIp[0] & 0xFF) << 24) | ((hashedIp[1] & 0xFF) << 16)
                | ((hashedIp[2] & 0xFF) << 8) | (hashedIp[3] & 0xFF);
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        RenderedImage image = renderer.render(code, 50);
        ImageIO.write(image, IDENTICON_IMAGE_FORMAT, byteOut);
        byte[] imageBytes = byteOut.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        BufferedImage bf = ImageIO.read(bais);
        WritableImage wr = null;
        if (bf != null) {
            wr = new WritableImage(bf.getWidth(), bf.getHeight());
            PixelWriter pw = wr.getPixelWriter();
            for (int x = 0; x < bf.getWidth(); x++) {
                for (int y = 0; y < bf.getHeight(); y++) {
                    pw.setArgb(x, y, bf.getRGB(x, y));
                }
            }
        }

        ImageView imView = new ImageView(wr);

        return imView;
    }


}
