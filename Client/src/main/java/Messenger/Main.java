package Messenger;

import Messenger.Utils.Preloader;
import com.subgraph.orchid.TorClient;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**Main class that loads at startup*/
public class Main extends Application {

    //The Main UI controller
    public static Controller controller;

    //The Tor client we are using for outgoing connections.
    public static TorClient torClient;

    //Object that contains the application data directory and OS type
    public static ApplicationParams params;

    //Object that makes long polling GETs to the server
    public static MessageRetriever retriever;

    //The main stage
    private static Stage stg;

    /**Launches the GUI*/
    @Override
    public void start(Stage primaryStage) throws Exception{
        URL location = getClass().getResource("gui.fxml");
        FXMLLoader loader = new FXMLLoader(location);
        AnchorPane mainUI = (AnchorPane) loader.load();
        primaryStage.setTitle("Subspace");
        primaryStage.setScene(new Scene(mainUI, 252, 630));

        //Load our css
        final String file = Main.class.getResource("gui.css").toString();

        this.stg = primaryStage;
        controller = loader.getController();
        primaryStage.getScene().getStylesheets().add(file);
        primaryStage.setResizable(false);
        primaryStage.show();

        //Shutdown the TorClient and MessageRetreiver and exit the app on close.
        primaryStage.setOnCloseRequest(event -> {
            torClient.stop();
            retriever.stop();
            System.exit(0);
        });
    }

    /**
     * Entry point for the application.
     * Loads the application parameters, starts the Tor client and lauches the GUI.
     */
    public static void main(String[] args) {
        //Load the application parameters. This will get our application data directory.
        try{params = new ApplicationParams(args);}
        catch (IOException e){e.printStackTrace();}

        //There is a bug in linux related to /etc/hosts that causes orchid to crash at startup.
        //As a workaround until we can find a fix in orchid we have to delete /etc/hosts/ and replace it after startup.
        //The file isn't deleted, just moved to our application data folder. So even if the app crashes the file should
        //shill remain on disk. Assuming the user doesn't delete the data folder.
        if (params.getOsType() == ApplicationParams.OS_TYPE.LINUX){
            File source = new File("/etc/hosts");
            File target = new File(params.getApplicationDataFolder().toString() + "/hosts");
            if(source.exists()){
                try {Files.copy(source.toPath(), target.toPath());}
                catch (IOException e) {e.printStackTrace();}
                source.delete();
            }
        }

        //Loads a hardcoded contact and chatroom on first startup.
        Preloader.set();

        //Fire up the Tor client and set the data directory.
        torClient = new TorClient();
        torClient.getConfig().setDataDirectory(params.getApplicationDataFolder());
        torClient.enableSocksListener();
        torClient.start();

        //Inintialize the message retriever. We wont start it until after Tor finishes initializing.
        retriever = new MessageRetriever(new FileWriter().getAllKeys());

        //Launch the GUI
        launch(args);
        try{torClient.waitUntilReady(5000);}
        catch (TimeoutException | InterruptedException e){e.printStackTrace();}

    }

    /**Returns the main stage*/
    public static Stage getStage(){
        return stg;
    }

}
