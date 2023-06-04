package com.tvz.java.jamb;

import com.tvz.java.jamb.model.ChatMessage;
import com.tvz.java.jamb.model.GameState;
import com.tvz.java.jamb.rmi.ChatMessagesUtil;
import com.tvz.java.jamb.rmi.RefreshChatMessagesThread;
import com.tvz.java.jamb.rmi.RemoteService;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class YambController {

    public String getDiceRepresentationOfNumber(int number) {
        if (number == 1) return "⚀";
        if (number == 2) return "⚁";
        if (number == 3) return "⚂";
        if (number == 4) return "⚃";
        if (number == 5) return "⚄";
        else return "⚅";
    }

    private static final String SAVE_GAME_FILE_NAME = "saveGame.bin";
    private static final String DOCUMENTATION_LOCATION = "src/main/resources/documentation/documentation.html";

    public static final int BROJ_KOCKICA = 5; // kockice koje se bacaju
    public static final int BROJ_KOLONA = 3; //gore, dolje, gore-dolje
    public static final int BROJ_MOGUCIH_POLJA = 10; //br1,br2,br3..
    private static final int MAX_BROJ_POTEZA = BROJ_KOLONA * BROJ_MOGUCIH_POLJA;

    private static int BROJ_POTEZA = 0;
    private static int[] DOBIVENI_BROJEVI = new int[BROJ_KOCKICA];
    private static int BROJ_BACANJA = 0; //broj bacanja u rundi, max su 3
    private static int BROJAC_GORE = BROJ_MOGUCIH_POLJA - 1; //za index moguceg_polja
    private static int BROJAC_DOLJE = 0; //za index moguceg_polja
    private static int[][] GAME_BOARD = new int[][]
            {
                    {-1, -1, -1}, // 1
                    {-1, -1, -1}, // 2
                    {-1, -1, -1}, // 3
                    {-1, -1, -1}, // 4
                    {-1, -1, -1}, // 5
                    {-1, -1, -1}, // 6
                    {-1, -1, -1}, // Dva para
                    {-1, -1, -1}, // Full
                    {-1, -1, -1}, // Poker
                    {-1, -1, -1}  // Yamb
            };

    Random rand = new Random();

    @FXML
    private GridPane plocaGridPane;
    @FXML
    private Button rollButton;
    @FXML
    private Button writeDownButton;
    @FXML
    private Button writeUpButton;
    @FXML
    private Button writeUpDownButton;

    @FXML
    private Text preostaliBrojBacanjaText;

    /* POLJA ZA PRIKAZ DOBIVENIH BROJEVA */
    @FXML
    private TextField dobiveniBroj1TextField;
    @FXML
    private TextField dobiveniBroj2TextField;
    @FXML
    private TextField dobiveniBroj3TextField;
    @FXML
    private TextField dobiveniBroj4TextField;
    @FXML
    private TextField dobiveniBroj5TextField;

    /* CHECKBOXEVI ZA ZALJUČAVANJE BROJEVA */
    @FXML
    private CheckBox zakljucajBroj1CheckBox;
    @FXML
    private CheckBox zakljucajBroj2CheckBox;
    @FXML
    private CheckBox zakljucajBroj3CheckBox;
    @FXML
    private CheckBox zakljucajBroj4CheckBox;
    @FXML
    private CheckBox zakljucajBroj5CheckBox;

    /* Ostala polja na ekranu */
    @FXML
    private Text imePlayeraText;
    @FXML
    private Text igracCijiJeRed;
    @FXML
    private TextArea chatTextArea;
    @FXML
    private TextField chatMessageTextField;

    /*---------------------------*/

    private Socket clientSocket;
    private PrintWriter output;
    private BufferedReader in;

    private RemoteService service;

    private Long PLAYER_ID;
    private String PLAYER_NAME;

    public void initialize() {
        this.rollButton.getStyleClass().add("rollButton");
        this.PLAYER_ID = YambApplication.userId;

        Registry registry = null;
        try {
            registry = LocateRegistry.getRegistry("localhost", 1099);
            service = (RemoteService) registry.lookup(RemoteService.REMOTE_OBJECT_NAME);
        } catch (RemoteException | NotBoundException e) {
            throw new RuntimeException(e);
        }

        new Thread(new RefreshChatMessagesThread(chatTextArea)).start();

        new Thread(() -> {
            try {
                this.clientSocket = new Socket("localhost", 3333);
                this.output = new PrintWriter(clientSocket.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                output.println("/game started " + this.PLAYER_ID);

                String line;
                while ((line = in.readLine()) != null) {

                    if (line.startsWith("/game")) {
                        String[] items = line.split(" ");
                        if (items[1].equals("username")) {
                            this.PLAYER_NAME = items[2];
                            this.imePlayeraText.setText(this.PLAYER_NAME + " (id: " + this.PLAYER_ID + ")");
                            output.println("/ready");
                        }
                    }

                    if (line.startsWith("/turn")){
                        Long playerIDWhoseTurnItIs = Long.parseLong(line.split(" ")[1]);
                        String playerNameWhoseTurnItIs = line.split(" ")[2];
                        this.igracCijiJeRed.setText("Na redu je igrač: "+playerNameWhoseTurnItIs);
                        if(PLAYER_ID == playerIDWhoseTurnItIs) {
                            this.setActivePlayerTurn();
                        }else{
                            this.setPasivePlayerTurn();
                        }
                    }


                }

            } catch (UnknownHostException e) {
                System.err.println("Don't know about host " + "localhost");
                System.exit(1);
            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to " +
                        "localhost");
                System.exit(1);
            }
        }).start();
    }

    public void sendChatMessage() throws RemoteException {
        String chatMessageString = chatMessageTextField.getText();
        chatMessageTextField.clear();
        ChatMessage newChatMessage = new ChatMessage(
                PLAYER_NAME,
                LocalDateTime.now(),
                chatMessageString,
                false);

        service.sendMessage(newChatMessage);
        List<ChatMessage> allChatMessages = service.getAllChatMessages();
        chatTextArea.setText(ChatMessagesUtil.convertChatMessagesToString(allChatMessages));
    }

    public void sendGameMoveChatMessage(String message) throws RemoteException {

        chatMessageTextField.clear();
        ChatMessage newChatMessage = new ChatMessage(
                PLAYER_NAME,
                LocalDateTime.now(),
                message,
                true);

        service.sendMessage(newChatMessage);
        List<ChatMessage> allChatMessages = service.getAllChatMessages();
        chatTextArea.setText(ChatMessagesUtil.convertChatMessagesToString(allChatMessages));
    }

    public void setActivePlayerTurn(){
        this.rollButton.setDisable(false);

        if (BROJAC_DOLJE + 1 < BROJ_MOGUCIH_POLJA) this.writeDownButton.setDisable(false);

        if(BROJAC_GORE > 0) this.writeUpButton.setDisable(false);

        int z = 0;
        for (int i = 0; i < BROJ_MOGUCIH_POLJA; i++) {
            if (GAME_BOARD[i][2] != -1) z++;
        }
        if (z < BROJ_MOGUCIH_POLJA) this.writeUpDownButton.setDisable(false);
    }

    public void setPasivePlayerTurn(){
        this.rollButton.setDisable(true);
        this.writeDownButton.setDisable(true);
        this.writeUpButton.setDisable(true);
        this.writeUpDownButton.setDisable(true);
    }

    @FXML
    protected void onRollButtonClicked() {
        //prvo bacanje
        if (BROJ_BACANJA == 0) {
            this.onFirstDiceRoll();
        }
        //ostala bacanja
        else {
            this.onDiceRoll();
        }
        BROJ_BACANJA++;
        this.preostaliBrojBacanjaText.setText("Preostali broj bacanja: " + (3 - BROJ_BACANJA));

        if (BROJ_BACANJA > 2) {
            this.rollButton.setDisable(true);
            this.rollButton.getStyleClass().add("rollButtonDeactivated");
        }
    }

    private void onFirstDiceRoll() {
        this.cleanUpDices();

        for (int i = 0; i < BROJ_KOCKICA; i++) {

            int broj = this.rand.nextInt(6) + 1;
            DOBIVENI_BROJEVI[i] = broj;
            if (i + 1 == 1) this.dobiveniBroj1TextField.setText(String.valueOf(DOBIVENI_BROJEVI[i]));
            if (i + 1 == 2) this.dobiveniBroj2TextField.setText(String.valueOf(DOBIVENI_BROJEVI[i]));
            if (i + 1 == 3) this.dobiveniBroj3TextField.setText(String.valueOf(DOBIVENI_BROJEVI[i]));
            if (i + 1 == 4) this.dobiveniBroj4TextField.setText(String.valueOf(DOBIVENI_BROJEVI[i]));
            if (i + 1 == 5) this.dobiveniBroj5TextField.setText(String.valueOf(DOBIVENI_BROJEVI[i]));
        }
    }

    private void onDiceRoll() {
        for (int i = 0; i < BROJ_KOCKICA; i++) {
            int broj = this.rand.nextInt(6) + 1;
            //ako korisnik ne cuva broj, zamijeni ga s novom vrijednosti
            switch (i + 1) {
                case 1:
                    if (!this.zakljucajBroj1CheckBox.isSelected()) {
                        DOBIVENI_BROJEVI[i] = broj;
                        this.dobiveniBroj1TextField.setText(String.valueOf(DOBIVENI_BROJEVI[i]));
                    }
                    break;
                case 2:
                    if (!this.zakljucajBroj2CheckBox.isSelected()) {
                        DOBIVENI_BROJEVI[i] = broj;
                        this.dobiveniBroj2TextField.setText(String.valueOf(DOBIVENI_BROJEVI[i]));
                    }
                    break;
                case 3:
                    if (!this.zakljucajBroj3CheckBox.isSelected()) {
                        DOBIVENI_BROJEVI[i] = broj;
                        this.dobiveniBroj3TextField.setText(String.valueOf(DOBIVENI_BROJEVI[i]));
                    }
                    break;
                case 4:
                    if (!this.zakljucajBroj4CheckBox.isSelected()) {
                        DOBIVENI_BROJEVI[i] = broj;
                        this.dobiveniBroj4TextField.setText(String.valueOf(DOBIVENI_BROJEVI[i]));
                    }
                    break;
                case 5:
                    if (!this.zakljucajBroj5CheckBox.isSelected()) {
                        DOBIVENI_BROJEVI[i] = broj;
                        this.dobiveniBroj5TextField.setText(String.valueOf(DOBIVENI_BROJEVI[i]));
                    }
                    break;
            }
        }
    }

    private Text getWriteScoreOnBoardTextUtil() {
        Text text = new Text();
        text.setWrappingWidth(100);
        text.setTextAlignment(TextAlignment.CENTER);
        text.setFill(Color.DIMGRAY);
        text.setFont(Font.font("Papyrus", 24));
        return text;
    }

    private Map<Integer, Integer> getListaFrekvencijeBrojeva() {
        Map<Integer, Integer> listaPonavljanjaBrojeva = new HashMap<>();

        for (int br : DOBIVENI_BROJEVI) {
            if (listaPonavljanjaBrojeva.containsKey(br)) {
                listaPonavljanjaBrojeva.put(br, listaPonavljanjaBrojeva.get(br) + 1);
            } else {
                listaPonavljanjaBrojeva.put(br, 1);
            }
        }

        return listaPonavljanjaBrojeva;
    }


    @FXML
    protected void onWriteDownButtonClicked() throws RemoteException {
        this.cleanUpDices();
        this.preostaliBrojBacanjaText.setText("Preostali broj bacanja: 3");

        for (int i = BROJAC_DOLJE; i < BROJ_MOGUCIH_POLJA; i++) {
            if (GAME_BOARD[i][0] == -1) {
                break;
            }
            BROJAC_DOLJE++;
        }

        Text text = getWriteScoreOnBoardTextUtil();
        Integer zbroj = 0;
        Map<Integer, Integer> listaPonavljanjaBrojeva = this.getListaFrekvencijeBrojeva();
        List<Integer> listaPonovljenihBrojeva = new ArrayList<>();

        //DVA PARA
        if (BROJAC_DOLJE == 6) {
            for (Map.Entry<Integer, Integer> br : listaPonavljanjaBrojeva.entrySet()) {
                if (br.getValue() == 2) listaPonovljenihBrojeva.add(br.getKey());
            }

            if (listaPonovljenihBrojeva.size() == 2) {
                zbroj = (listaPonovljenihBrojeva.get(0) + listaPonovljenihBrojeva.get(1)) * 2;
                zbroj += 10;
            }
        }

        //FULL
        if (BROJAC_DOLJE == 7) {
            for (Map.Entry<Integer, Integer> br : listaPonavljanjaBrojeva.entrySet()) {
                if (br.getValue() == 3) listaPonovljenihBrojeva.add(br.getKey() * 3);
                if (br.getValue() == 2) listaPonovljenihBrojeva.add(br.getKey() * 2);
            }

            if (listaPonovljenihBrojeva.size() == 2 && listaPonavljanjaBrojeva.size() == 2) {
                zbroj = listaPonovljenihBrojeva.get(0) + listaPonovljenihBrojeva.get(1);
                zbroj += 30;
            }
        }

        //POKER
        if (BROJAC_DOLJE == 8) {
            for (Map.Entry<Integer, Integer> br : listaPonavljanjaBrojeva.entrySet()) {
                if (br.getValue() == 4) listaPonovljenihBrojeva.add(br.getKey() * 4);
            }

            if (listaPonovljenihBrojeva.size() == 1) {
                zbroj = listaPonovljenihBrojeva.get(0);
                zbroj += 40;
            }
        }

        //YAMB
        if (BROJAC_DOLJE == 9) {
            for (Map.Entry<Integer, Integer> br : listaPonavljanjaBrojeva.entrySet()) {
                if (br.getValue() == 5) listaPonovljenihBrojeva.add(br.getKey() * 5);
            }

            if (listaPonovljenihBrojeva.size() == 1 && listaPonovljenihBrojeva.get(0) != 0) {
                zbroj = listaPonovljenihBrojeva.get(0);
                zbroj += 50;
            }
        }

        //BROJEVI OD 1 DO 6
        if (BROJAC_DOLJE < 6) {
            zbroj = Arrays.stream(DOBIVENI_BROJEVI)
                    .filter(i -> i == (BROJAC_DOLJE + 1))
                    .reduce(0, (zbr, elem) -> zbr += elem);

        }

        text.setText(String.valueOf(zbroj));
        this.sendGameMoveChatMessage(String.valueOf(zbroj));
        GAME_BOARD[BROJAC_DOLJE][0] = zbroj;
        this.plocaGridPane.add(text, 1, BROJAC_DOLJE + 1);

        if (BROJAC_DOLJE + 1 == BROJ_MOGUCIH_POLJA) {
            this.writeDownButton.setStyle("-fx-background-color: red");
            this.writeDownButton.setDisable(true);
        }

        this.endOfTurnReset();
    }

    @FXML
    protected void onWriteUpButtonClicked() throws RemoteException {
        this.cleanUpDices();
        this.preostaliBrojBacanjaText.setText("Preostali broj bacanja: 3");

        for (int i = BROJAC_GORE; i > 0; i--) {
            if (GAME_BOARD[i][1] == -1) {
                break;
            }
            BROJAC_GORE--;
        }

        Integer zbroj = 0;
        Text text = getWriteScoreOnBoardTextUtil();
        Map<Integer, Integer> listaPonavljanjaBrojeva = this.getListaFrekvencijeBrojeva();
        List<Integer> listaPonovljenihBrojeva = new ArrayList<>();

        //DVA PARA
        if (BROJAC_GORE == 6) {
            for (Map.Entry<Integer, Integer> br : listaPonavljanjaBrojeva.entrySet()) {
                if (br.getValue() == 2) listaPonovljenihBrojeva.add(br.getKey());
            }

            if (listaPonovljenihBrojeva.size() == 2) {
                zbroj = (listaPonovljenihBrojeva.get(0) + listaPonovljenihBrojeva.get(1)) * 2;
                zbroj += 10;
            }
        }

        //FULL
        if (BROJAC_GORE == 7) {
            for (Map.Entry<Integer, Integer> br : listaPonavljanjaBrojeva.entrySet()) {
                if (br.getValue() == 3) listaPonovljenihBrojeva.add(br.getKey() * 3);
                if (br.getValue() == 2) listaPonovljenihBrojeva.add(br.getKey() * 2);
            }

            if (listaPonovljenihBrojeva.size() == 2 && listaPonavljanjaBrojeva.size() == 2) {
                zbroj = listaPonovljenihBrojeva.get(0) + listaPonovljenihBrojeva.get(1);
                zbroj += 30;
            }
        }

        //POKER
        if (BROJAC_GORE == 8) {
            for (Map.Entry<Integer, Integer> br : listaPonavljanjaBrojeva.entrySet()) {
                if (br.getValue() == 4) listaPonovljenihBrojeva.add(br.getKey() * 4);
            }

            if (listaPonovljenihBrojeva.size() == 1) {
                zbroj = listaPonovljenihBrojeva.get(0);
                zbroj += 40;
            }
        }

        //YAMB
        if (BROJAC_GORE == 9) {
            for (Map.Entry<Integer, Integer> br : listaPonavljanjaBrojeva.entrySet()) {
                if (br.getValue() == 5) listaPonovljenihBrojeva.add(br.getKey() * 5);
            }

            if (listaPonovljenihBrojeva.size() == 1 && listaPonovljenihBrojeva.get(0) != 0) {
                zbroj = listaPonovljenihBrojeva.get(0);
                zbroj += 50;
            }
        }

        //BROJEVI OD 1 DO 6
        if (BROJAC_GORE < 6) {
            zbroj = Arrays.stream(DOBIVENI_BROJEVI)
                    .filter(i -> i == (BROJAC_GORE + 1))
                    .reduce(0, (zbr, elem) -> zbr += elem);

        }

        text.setText(String.valueOf(zbroj));
        this.sendGameMoveChatMessage(String.valueOf(zbroj));
        GAME_BOARD[BROJAC_GORE][1] = zbroj;
        this.plocaGridPane.add(text, 2, BROJAC_GORE + 1);

        if (BROJAC_GORE == 0) {
            this.writeUpButton.setStyle("-fx-background-color: red");
            this.writeUpButton.setDisable(true);
        }

        this.endOfTurnReset();
    }

    @FXML
    protected void onWriteUpDownButtonClicked() {
        List<Node> allPaneNodes = this.plocaGridPane.getChildren().stream().filter(n -> n instanceof Pane).toList();

        boolean areCellsOpen = allPaneNodes.stream().anyMatch(n -> n.getStyleClass().contains("cellUpDown"));

        EventHandler<MouseEvent> clickEvent = new EventHandler<>() {
            @Override
            public void handle(MouseEvent event) {
                Node source = (Node) event.getTarget();
                if (source.getStyleClass().contains("cellUpDown")) {
                    cleanUpDices();
                    preostaliBrojBacanjaText.setText("Preostali broj bacanja: 3");
                    Integer colIndex = GridPane.getColumnIndex(source);
                    Integer rowIndex = GridPane.getRowIndex(source);

                    Integer zbroj = 0;
                    Text text = getWriteScoreOnBoardTextUtil();
                    Map<Integer, Integer> listaPonavljanjaBrojeva = getListaFrekvencijeBrojeva();
                    List<Integer> listaPonovljenihBrojeva = new ArrayList<>();

                    // DVA PARA
                    if (rowIndex == 7) {
                        for (Map.Entry<Integer, Integer> br : listaPonavljanjaBrojeva.entrySet()) {
                            if (br.getValue() == 2) listaPonovljenihBrojeva.add(br.getKey());
                        }

                        if (listaPonovljenihBrojeva.size() == 2) {
                            zbroj = (listaPonovljenihBrojeva.get(0) + listaPonovljenihBrojeva.get(1)) * 2;
                            zbroj += 10;
                        }
                    }

                    // FULL
                    if (rowIndex == 8) {
                        for (Map.Entry<Integer, Integer> br : listaPonavljanjaBrojeva.entrySet()) {
                            if (br.getValue() == 3) listaPonovljenihBrojeva.add(br.getKey() * 3);
                            if (br.getValue() == 2) listaPonovljenihBrojeva.add(br.getKey() * 2);
                        }

                        if (listaPonovljenihBrojeva.size() == 2 && listaPonavljanjaBrojeva.size() == 2) {
                            zbroj = listaPonovljenihBrojeva.get(0) + listaPonovljenihBrojeva.get(1);
                            zbroj += 30;
                        }
                    }

                    //POKER
                    if (rowIndex == 9) {
                        for (Map.Entry<Integer, Integer> br : listaPonavljanjaBrojeva.entrySet()) {
                            if (br.getValue() == 4) listaPonovljenihBrojeva.add(br.getKey() * 4);
                        }

                        if (listaPonovljenihBrojeva.size() == 1) {
                            zbroj = listaPonovljenihBrojeva.get(0);
                            zbroj += 40;
                        }
                    }

                    //YAMB
                    if (rowIndex == 10) {
                        for (Map.Entry<Integer, Integer> br : listaPonavljanjaBrojeva.entrySet()) {
                            if (br.getValue() == 5) listaPonovljenihBrojeva.add(br.getKey() * 5);
                        }

                        if (listaPonovljenihBrojeva.size() == 1 && listaPonovljenihBrojeva.get(0) != 0) {
                            zbroj = listaPonovljenihBrojeva.get(0);
                            zbroj += 50;
                        }
                    }

                    //BROJEVI OD 1 DO 6
                    if (rowIndex < 7) {
                        zbroj = Arrays.stream(DOBIVENI_BROJEVI)
                                .filter(i -> i == (rowIndex))
                                .reduce(0, (zbr, elem) -> zbr += elem);
                    }

                    text.setText(String.valueOf(zbroj));
                    try {
                        sendGameMoveChatMessage(String.valueOf(zbroj));
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                    GAME_BOARD[rowIndex - 1][2] = zbroj;
                    plocaGridPane.getChildren().remove(source);
                    plocaGridPane.add(text, colIndex, rowIndex);

                    removeStyleFromPanes();

                    int z = 0;
                    for (int i = 0; i < BROJ_MOGUCIH_POLJA; i++) {
                        if (GAME_BOARD[i][2] != -1) z++;

                    }
                    if (z == BROJ_MOGUCIH_POLJA) {
                        writeUpDownButton.setStyle("-fx-background-color: red");
                        writeUpDownButton.setDisable(true);
                    }

                    endOfTurnReset();

                    allPaneNodes.forEach(n -> {
                        n.getStyleClass().remove("cellUpDown");
                        n.removeEventHandler(MouseEvent.MOUSE_CLICKED, this);
                    });
                }
            }
        };

        if (areCellsOpen) {
            allPaneNodes.forEach(p -> {
                p.getStyleClass().remove("cellUpDown");
                p.removeEventHandler(MouseEvent.MOUSE_CLICKED, clickEvent);
            });
        } else {
            allPaneNodes.forEach(p -> {
                p.getStyleClass().add("cellUpDown");
                p.addEventHandler(MouseEvent.MOUSE_CLICKED, clickEvent);
            });
        }


    }

    private void endOfTurnReset() {
        this.rollButton.setDisable(false);
        this.rollButton.getStyleClass().remove("rollButtonDeactivated");
        this.rollButton.getStyleClass().add("rollButton");

        BROJ_BACANJA = 0;
        DOBIVENI_BROJEVI = new int[BROJ_KOCKICA];
        this.checkWinCondition();
    }

    private void removeStyleFromPanes() {
        for (Node n : this.plocaGridPane.getChildren()) {
            if (n instanceof Pane) {
                n.getStyleClass().clear();
            }
        }
    }

    private void checkWinCondition() {
        BROJ_POTEZA++;
        if (BROJ_POTEZA == MAX_BROJ_POTEZA) {
            int score = 0;
            for(int i = 0; i < BROJ_MOGUCIH_POLJA; i++){
                for(int j = 0; j < BROJ_KOLONA; j++){
                    score += GAME_BOARD[i][j];
                }
            }
            this.showMessage(Alert.AlertType.CONFIRMATION, "IGRA GOTOVA", "Popunili ste cijelu ploču.", "Vaš broj bodova je: "+ score);
        }

        this.output.println("/turnend");
    }

    private void cleanUpDices() {
        this.zakljucajBroj1CheckBox.setSelected(false);
        this.zakljucajBroj2CheckBox.setSelected(false);
        this.zakljucajBroj3CheckBox.setSelected(false);
        this.zakljucajBroj4CheckBox.setSelected(false);
        this.zakljucajBroj5CheckBox.setSelected(false);

        this.dobiveniBroj1TextField.setText("");
        this.dobiveniBroj2TextField.setText("");
        this.dobiveniBroj3TextField.setText("");
        this.dobiveniBroj4TextField.setText("");
        this.dobiveniBroj5TextField.setText("");
    }

    @FXML
    private void onSaveGame() {
        GameState state = new GameState();
        state.setBROJAC_DOLJE(BROJAC_DOLJE);
        state.setBROJAC_GORE(BROJAC_GORE);
        state.setGAME_BOARD(GAME_BOARD);
        state.setBrojBacanja(BROJ_BACANJA);
        state.setDobiveniBrojevi(DOBIVENI_BROJEVI);

        try {
            ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(SAVE_GAME_FILE_NAME)
            );
            oos.writeObject(state);
            showMessage(Alert.AlertType.CONFIRMATION,
                    "SPREMANJE", "Uspješno", "Igra je uspješno spremljena.");
        } catch (IOException e) {
            showMessage(Alert.AlertType.WARNING, "SPREMANJE", "Neuspješno - Greška",
                    e.getMessage());
        }
    }

    @FXML
    public void onLoadGame() {

        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SAVE_GAME_FILE_NAME));
            if (ois.readObject() instanceof GameState gs) {
                BROJAC_DOLJE = gs.getBROJAC_DOLJE();
                BROJAC_GORE = gs.getBROJAC_GORE();
                BROJ_BACANJA = gs.getBrojBacanja();
                DOBIVENI_BROJEVI = gs.getDobiveniBrojevi();
                GAME_BOARD = gs.getGAME_BOARD();

                if (BROJ_BACANJA > 0) {
                    this.dobiveniBroj1TextField.setText(String.valueOf(DOBIVENI_BROJEVI[0]));
                    this.dobiveniBroj2TextField.setText(String.valueOf(DOBIVENI_BROJEVI[1]));
                    this.dobiveniBroj3TextField.setText(String.valueOf(DOBIVENI_BROJEVI[2]));
                    this.dobiveniBroj4TextField.setText(String.valueOf(DOBIVENI_BROJEVI[3]));
                    this.dobiveniBroj5TextField.setText(String.valueOf(DOBIVENI_BROJEVI[4]));
                }
                this.preostaliBrojBacanjaText.setText("Preostali broj bacanja: " + (3 - BROJ_BACANJA));

                if (BROJ_BACANJA > 2) {
                    this.rollButton.setDisable(true);
                    this.rollButton.getStyleClass().add("rollButtonDeactivated");
                }

                for (int i = 0; i < BROJ_MOGUCIH_POLJA; i++) {
                    if (GAME_BOARD[i][0] != -1) {
                        Text text = getWriteScoreOnBoardTextUtil();
                        text.setText(String.valueOf(GAME_BOARD[i][0]));
                        this.plocaGridPane.add(text, 1, i + 1);
                    }
                    if (GAME_BOARD[i][1] != -1) {
                        Text text = getWriteScoreOnBoardTextUtil();
                        text.setText(String.valueOf(GAME_BOARD[i][1]));
                        this.plocaGridPane.add(text, 2, i + 1);
                    }
                    if (GAME_BOARD[i][2] != -1) {
                        Text text = getWriteScoreOnBoardTextUtil();
                        text.setText(String.valueOf(GAME_BOARD[i][2]));
                        this.plocaGridPane.add(text, 3, i + 1);
                    }
                }
            }
            showMessage(Alert.AlertType.CONFIRMATION,
                    "UČITAVANJE", "Uspješno", "Igra je uspješno učitana");

        } catch (IOException | ClassNotFoundException e) {
            showMessage(Alert.AlertType.WARNING,
                    "UČITAVANJE", "Neuspješno", "Došlo je do greške. " + e.getMessage());
        }
    }

    public void onGenerateDocumentation() {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("<html><head><link rel='stylesheet' href='documentationStyle.css'>");
        stringBuilder.append("<title>Yamb</title></head>");
        stringBuilder.append("<body><h1 class='heading'>Yamb code documentation</h1>");

        Path start = Paths.get(".");
        List<Path> classFilePaths = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(start)) {
            classFilePaths.addAll(stream.collect(Collectors.toList()));
        } catch (IOException e) {
            showMessage(Alert.AlertType.WARNING, "DOKUMENTACIJA",
                    "Generiranje dokumentacije", "Došlo je do pogreške u radu aplikacije!" +
                            e.getMessage());
        }

        for (Path path : classFilePaths) {

            String className = path.getFileName().toString();

            List<String> pathElements = new ArrayList<>();
            path.forEach(p -> pathElements.add(p.toString()));

            if (className.endsWith("class") && !className.contains("module-info")) {
                stringBuilder.append("<h2 class='className'>")
                        .append(path.getFileName());
                try {
                    String packageName = "";
                    boolean startPackage = false;
                    for (String pathElement : pathElements) {
                        if (startPackage) {
                            packageName += pathElement + ".";
                            continue;
                        }
                        if (pathElement.equals("classes")) {
                            startPackage = true;
                        }
                    }

                    Class<?> myClass = Class.forName(packageName.substring(0, packageName.length() - 7));
                    Constructor<?>[] constructors = myClass.getConstructors();
                    String superclass = myClass.getSuperclass().getName();
                    Field[] fields = myClass.getFields();
                    Method[] methods = myClass.getMethods();

                    stringBuilder.append(" [extends ")
                            .append(superclass);
                    stringBuilder.append("]</h2>");

                    //-------------CONSTRUCTORS---------------------------
                    stringBuilder.append("<div class='constructors'>");
                    stringBuilder.append("<h3 style='padding-left: 20px; text-decoration: underline'>Constructors</h3>");
                    for (Constructor<?> c : constructors) {
                        String constructorName = c.getName();
                        stringBuilder.append("<p style='padding-left: 30px'>");
                        stringBuilder.append("<b>")
                                .append(constructorName)
                                .append("</b>");
                        String constructorParameters = Arrays.stream(c.getParameters()).map(p -> "<i>" + p.getType().getCanonicalName() + "</i> " + p.getName()).collect(Collectors.joining(", "));
                        if (constructorParameters.length() > 0) {
                            stringBuilder.append("(")
                                    .append(constructorParameters)
                                    .append(")");
                        } else {
                            stringBuilder.append("(no args constructor)");
                        }
                        stringBuilder.append("</p>");
                    }
                    stringBuilder.append("</div>");

                    //-------------METHODS---------------------------
                    stringBuilder.append("<div class='methods'>");
                    stringBuilder.append("<h3 style='padding-left: 20px;text-decoration: underline'>Methods</h3>");
                    for (Method m : methods) {
                        stringBuilder.append("<p style='padding-left: 30px'> <b>")
                                .append(m.getName())
                                .append("</b> [returns <i>")
                                .append(m.getReturnType().getCanonicalName())
                                .append("</i>]")
                                .append("</p>");
                    }
                    stringBuilder.append("</div>");

                    //-------------FIELDS---------------------------
                    stringBuilder.append("<div class='fields'>");
                    stringBuilder.append("<h3 style='padding-left: 20px; text-decoration: underline'>Fields</h3>");
                    for (Field f : fields) {
                        stringBuilder.append("<p style='padding-left: 30px'><b>")
                                .append(f.getName())
                                .append("</b> (<i>")
                                .append(f.getType().getCanonicalName())
                                .append("</i>)")
                                .append("</p>");
                    }
                    stringBuilder.append("</div>");

                } catch (ClassNotFoundException e) {
                    showMessage(Alert.AlertType.WARNING, "DOKUMENTACIJA",
                            "Generiranje dokumentacije", "Došlo je do pogreške u radu aplikacije!" +
                                    e.getMessage());
                }

                stringBuilder.append("<hr>");
            }
        }
        stringBuilder.append("</body></html>");

        try {
            FileWriter fileWriter = new FileWriter(DOCUMENTATION_LOCATION);
            fileWriter.write(stringBuilder.toString());
            fileWriter.close();
            System.out.println("HTML file generated successfully.");
            this.loadDocumentationPage();
        } catch (IOException | URISyntaxException e) {
            System.out.println("An error occurred while generating the HTML file.");
            e.printStackTrace();
        }
    }

    public void loadDocumentationPage() throws URISyntaxException, IOException {
        File file = new File(DOCUMENTATION_LOCATION);
        Desktop desktop = Desktop.getDesktop();
        desktop.browse(file.toURI());

    }

    private void showMessage(Alert.AlertType type, String title, String headerText, String contentText) {
        Alert alert = new Alert(type);
        alert.getButtonTypes().clear();
        alert.getButtonTypes().add(ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.showAndWait();
    }
}