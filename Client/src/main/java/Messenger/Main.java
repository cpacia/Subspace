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

public class Main extends Application {

    static Controller controller;
    static TorClient torClient;
    private static Stage stg;
    public static ApplicationParams params;
    public static MessageRetriever retriever;

    @Override
    public void start(Stage primaryStage) throws Exception{
        URL location = getClass().getResource("gui.fxml");
        FXMLLoader loader = new FXMLLoader(location);
        AnchorPane mainUI = (AnchorPane) loader.load();
        primaryStage.setTitle("Subspace");
        primaryStage.setScene(new Scene(mainUI, 252, 630));
        final String file = Main.class.getResource("gui.css").toString();
        this.stg = primaryStage;
        controller = loader.getController();
        primaryStage.getScene().getStylesheets().add(file);
        primaryStage.setResizable(false);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            torClient.stop();
            retriever.stop();
            System.exit(0);
        });
    }


    public static void main(String[] args) {
        try{params = new ApplicationParams(args);}
        catch (IOException e){e.printStackTrace();}
        if (params.getOsType() == ApplicationParams.OS_TYPE.LINUX){
            File source = new File("/etc/hosts");
            File target = new File(params.getApplicationDataFolder().toString() + "/hosts");
            if(source.exists()){
                try {Files.copy(source.toPath(), target.toPath());}
                catch (IOException e) {e.printStackTrace();}
                source.delete();
            }
        }
        Preloader.set();
        torClient = new TorClient();
        torClient.getConfig().setDataDirectory(params.getApplicationDataFolder());
        torClient.enableSocksListener();
        torClient.start();
        retriever = new MessageRetriever(new FileWriter().getAllKeys());
        launch(args);
        try{torClient.waitUntilReady(5000);}
        catch (TimeoutException | InterruptedException e){e.printStackTrace();}
        System.out.println("Tor Ready");

    }

    static Stage getStage(){
        return stg;
    }

}
