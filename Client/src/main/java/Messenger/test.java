package Messenger;

import javafx.application.Application;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import javax.sound.sampled.*;
import java.io.IOException;

/**
 * Created by chris on 2/15/15.
 */
public class test extends Application{

    @Override
    public void start(Stage primaryStage) {
        try{AudioInputStream audioInputStream =
                AudioSystem.getAudioInputStream(Main.class.getResourceAsStream("chime.wav"));
        Clip clip = AudioSystem.getClip();
        clip.open(audioInputStream);
        clip.start();}
        catch (IOException | UnsupportedAudioFileException | LineUnavailableException e){e.printStackTrace();}
    }

    public static void main(String[] args) {
        launch(args);
    }


}
