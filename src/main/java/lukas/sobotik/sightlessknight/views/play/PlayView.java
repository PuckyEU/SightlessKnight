package lukas.sobotik.sightlessknight.views.play;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import elemental.json.Json;
import elemental.json.JsonObject;
import lukas.sobotik.sightlessknight.components.CommandLine;
import lukas.sobotik.sightlessknight.components.CustomProgressBar;
import lukas.sobotik.sightlessknight.gamelogic.AlgebraicNotationUtils;
import lukas.sobotik.sightlessknight.gamelogic.Board;
import lukas.sobotik.sightlessknight.gamelogic.BoardLocation;
import lukas.sobotik.sightlessknight.gamelogic.FenUtils;
import lukas.sobotik.sightlessknight.gamelogic.GameState;
import lukas.sobotik.sightlessknight.gamelogic.Move;
import lukas.sobotik.sightlessknight.gamelogic.Piece;
import lukas.sobotik.sightlessknight.gamelogic.Rules;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;
import lukas.sobotik.sightlessknight.views.HomeView;
import lukas.sobotik.sightlessknight.views.MainLayout;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

@PageTitle("Play")
@Route(value = "play", layout = MainLayout.class)
public class PlayView extends VerticalLayout {
    FenUtils fenUtils;
    AlgebraicNotationUtils algebraicNotationUtils;
    public GameState gameState;
    Board board;
    List<Move> validMovesForPosition;

    // Piece Game
    BoardLocation targetSquare = null;
    BoardLocation startSquare = null;
    public boolean isTrainingMode = false;
    Piece trainingPiece = null;

    Piece pieceForKinglessGames = null;
    HorizontalLayout gameContentLayout, targetSquareLayout, gameLayout;
    VerticalLayout algebraicNotationHistoryLayout, gameInfoLayout, quickSettingsLayout;
    Dialog quickSettingsDialog;
    Checkbox showBoardButton, showPiecesButton;
    public static String STARTING_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    boolean localStorageItemsLoaded = false;
    public boolean showPieces = true;
    public boolean showBoard = true;
    public static CustomProgressBar boardWidthProgressBar;

    /**
     * Generates a piece training game based on the given piece type.
     * @param s the piece type to generate the game for ("knight")
     */
    public void generatePieceTrainingGame(String s) {
        Piece[] pieces;
        Piece piece = null;
        switch (s) {
            case "knight" -> piece = new Piece(Team.WHITE, PieceType.KNIGHT);
            case "bishop" -> piece = new Piece(Team.WHITE, PieceType.BISHOP);
            case "rook" -> piece = new Piece(Team.WHITE, PieceType.ROOK);
            case "queen" -> piece = new Piece(Team.WHITE, PieceType.QUEEN);
            case "king" -> piece = new Piece(Team.WHITE, PieceType.KING);
        }

        if (piece != null) {
            trainingPiece = piece;
            pieces = generatePieceTrainingGame(piece);
            initialize(pieces, true);
            showTargetSquare(startSquare.getAlgebraicNotationLocation()
                                     + " → "
                                     + targetSquare.getAlgebraicNotationLocation());
        } else {
            Notification.show("Invalid URL");
            UI.getCurrent().navigate(HomeView.class).ifPresent(HomeView::reload);
        }
    }

    /**
     * Creates a new instance of the PlayView class.
     */
    public PlayView() {
        initialize();
    }

    /**
     * Creates a new instance of the PlayView class with a specified input string.
     *
     * @param s the input string used to generate a piece training game.
     */
    public PlayView(String s) {
        generatePieceTrainingGame(s);
    }

    /**
     * Initializes the game board and pieces.
     * This method is called internally when starting a new game.
     */
    private void initialize() {
        Piece[] pieces = new Piece[64];
        fenUtils = new FenUtils(pieces);
        pieces = fenUtils.generatePositionFromFEN(STARTING_POSITION);
        board = new Board(64, pieces, fenUtils);
        initialize(pieces, false);
    }
    /**
     * Initializes the game board and pieces.
     * @param pieces an array of Piece objects representing the initial positions of the pieces on the board
     * @param kinglessGame a boolean value indicating whether the game is a kingless game or not
     */
    private void initialize(Piece[] pieces, boolean kinglessGame) {
        setAlignItems(Alignment.CENTER);
        setHeight("100%");

        gameState = new GameState(board, fenUtils.getStartingTeam(), kinglessGame);
        algebraicNotationUtils = new AlgebraicNotationUtils(fenUtils, gameState, board);
        algebraicNotationUtils.setKinglessGame(kinglessGame);
        validMovesForPosition = Rules.getAllValidMovesForTeam(GameState.currentTurn, board, true);

        gameContentLayout = new HorizontalLayout();
        gameContentLayout.addClassName("game_content_layout");

        quickSettingsLayout = new VerticalLayout();
        quickSettingsLayout.setWidth("100%");
        quickSettingsLayout.addClassName("game_content_layout_child");

        quickSettingsDialog = new Dialog();
        quickSettingsDialog.setHeaderTitle("Quick Settings");
        quickSettingsDialog.setWidth("25%");
        Button closeButton = new Button(new Icon("lumo", "cross"),
                                        (e) -> quickSettingsDialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        quickSettingsDialog.getHeader().add(closeButton);
        loadLocalStorageItems();

        quickSettingsDialog.add(quickSettingsLayout);

        gameContentLayout.setHeightFull();
        gameContentLayout.setWidthFull();
        add(gameContentLayout);

        gameLayout = new HorizontalLayout();
        gameLayout.setClassName("game_layout");

        createBoard(pieces);

        gameContentLayout.add(gameLayout);

        gameInfoLayout = new VerticalLayout();
        gameInfoLayout.setWidth("25%");
        gameInfoLayout.addClassName("game_content_layout_child");
        gameInfoLayout.addClassName("game_info_layout");

        targetSquareLayout = new HorizontalLayout();
        targetSquareLayout.setVisible(false);
        targetSquareLayout.setWidthFull();
        targetSquareLayout.setAlignItems(Alignment.CENTER);
        targetSquareLayout.addClassName("game_info_layout_child");
        gameInfoLayout.add(targetSquareLayout);

        algebraicNotationHistoryLayout = new VerticalLayout();
        algebraicNotationHistoryLayout.addClassName("move_history");
        algebraicNotationHistoryLayout.addClassName("game_info_layout_child");

        gameInfoLayout.add(algebraicNotationHistoryLayout);

        var gameInfoLayoutBottomLayout = new VerticalLayout();
        var commandLine = getCommandLine();
        gameInfoLayoutBottomLayout.add(commandLine);
        gameInfoLayoutBottomLayout.getStyle().set("display", "flex");

        var settingsButton = new Button(new Icon("lumo", "cog"), (e) -> quickSettingsDialog.open());
        var gameInfoLayoutBottomButtonsLayout = new HorizontalLayout();
        gameInfoLayoutBottomButtonsLayout.add(settingsButton);
        gameInfoLayoutBottomButtonsLayout.getStyle().set("margin-left", "auto");

        gameInfoLayoutBottomLayout.add(gameInfoLayoutBottomButtonsLayout);
        gameInfoLayout.add(gameInfoLayoutBottomLayout);

        gameContentLayout.add(gameInfoLayout);
        createGameSettings();
    }

    /**
     * Creates and returns a CommandLine object.
     *
     * @return the created CommandLine object
     */
    private CommandLine getCommandLine() {
        var commandLine = new CommandLine(this);
        commandLine.setClickListener(view -> {
            if (commandLine.getCommand().startsWith("/")) return;

            Notification.show(commandLine.getCommand());

            algebraicNotationUtils.updateVariables(fenUtils, gameState, board);
            Move move = algebraicNotationUtils.getMoveFromParsedMove(commandLine.getCommand());
            if (move == null) {
                Notification.show("Invalid Move");
                return;
            }

            System.out.println("Playing move via text: " + move.getFrom().getAlgebraicNotationLocation() + move.getTo().getAlgebraicNotationLocation() + move.getSimplifiedMovedPiece().team + move.getSimplifiedMovedPiece().type);

            playMove(move);
        });
        return commandLine;
    }

    /**
     * Method used to create the game settings.
     * It creates the layout for the board size setting and the checkboxes for showing the pieces and the board.
     * It also sets up the event listeners for the checkboxes.
     */
    private void createGameSettings() {
        var boardSizeLayout = new HorizontalLayout();
        boardSizeLayout.setWidthFull();
        boardSizeLayout.getStyle().set("position", "relative");
        boardSizeLayout.getStyle().set("display", "flex");
        boardSizeLayout.getStyle().set("align-items", "center");
        var boardSizeSettingText = new Div();
        boardSizeSettingText.setText("Board Size: ");
        boardSizeSettingText.addClassName("board_size_setting_label");
        boardSizeLayout.add(boardSizeSettingText);

        boardWidthProgressBar = new CustomProgressBar();
        boardWidthProgressBar.setLabelQuery(".board_size_setting_label");
        boardWidthProgressBar.setValue(1);

        boardWidthProgressBar.getElement().executeJs(getProgressBarCode());
        loadLocalStorageItems();
        boardSizeLayout.add(boardWidthProgressBar);

        showPiecesButton = new Checkbox("Show Pieces");
        showPiecesButton.setValue(showPieces);
        showPiecesButton.addValueChangeListener(valueChangeEvent -> {
            if (valueChangeEvent.getValue()) {
                showPieces(true);
                setLocalStorageItem("showPieces", true);
            } else {
                showPieces(false);
                setLocalStorageItem("showPieces", false);
            }
        });

        showBoardButton = new Checkbox("Show Board");
        showBoardButton.setValue(showBoard);
        showBoardButton.addValueChangeListener(valueChangeEvent -> {
            if (valueChangeEvent.getValue()) {
                showBoard(true);
                showPiecesButton.setEnabled(true);
                boardWidthProgressBar.setDisabled(false);
                setLocalStorageItem("showBoard", true);
            } else {
                showBoard(false);
                showPiecesButton.setEnabled(false);
                boardWidthProgressBar.setDisabled(true);
                setLocalStorageItem("showBoard", false);
            }
        });

        quickSettingsLayout.add(showBoardButton);
        quickSettingsLayout.add(showPiecesButton);

        quickSettingsLayout.add(boardSizeLayout);
        quickSettingsLayout.add(boardWidthProgressBar);
    }

    /**
     * Method used to load the local storage items.
     */
    public void loadLocalStorageItems() {
        getElement().executeJs("return {showPieces: localStorage.getItem('showPieces'), showBoard: localStorage.getItem('showBoard'), boardSize: localStorage.getItem('boardSize')};")
                .then(jsonValue -> {
                    JsonObject jsonObject = Json.parse(jsonValue.toJson());
                    if (!Json.createNull().jsEquals(jsonObject.get("showPieces"))) {
                        showPieces(Boolean.parseBoolean(jsonObject.getString("showPieces")));
                    }
                    if (!Json.createNull().jsEquals(jsonObject.get("showBoard"))) {
                        showBoard(Boolean.parseBoolean(jsonObject.getString("showBoard")));
                    }
                    if (!Json.createNull().jsEquals(jsonObject.get("boardSize"))) {
                        boardWidthProgressBar.setValue(Double.parseDouble(jsonObject.getString("boardSize")) / 100);
                        setBoardWidth(Double.parseDouble(jsonObject.getString("boardSize")));
                    }
                    localStorageItemsLoaded = true;
                    createBoard(board.pieces);
                });
    }

    /**
     * Method used to set the width of the game board.
     * @param value the value to set
     */
    public void setBoardWidth(Double value) {
        this.getElement()
                .executeJs("const gameLayout = document.querySelector('.game_layout');"
                                   + "const progressContainer = document.querySelector('.progress-container');"

                                   + "const getNumericValue = (value) => parseFloat(value.match(/(\\d+(\\.\\d+)?)|(\\.\\d+)/)[0]);"
                                   + "const minAllowedWidth = getNumericValue(getComputedStyle(gameLayout).getPropertyValue('min-width'));"
                                   + "const maxAllowedWidth = getNumericValue(getComputedStyle(gameLayout).getPropertyValue('max-width'));"

                                   + "const w = " + value + ";"
                                   + "const mappedWidth = (minAllowedWidth + (w * 0.01 * (maxAllowedWidth - minAllowedWidth))).toFixed(2) + 'px';"
                                   + "gameLayout.style.width = mappedWidth;");
    }

    /**
     * Method used to set a value in the local storage.
     * @param setting the setting to set
     * @param value the value to set
     */
    public void setLocalStorageItem(String setting, boolean value) {
        getElement().executeJs("localStorage.setItem('" + setting + "', '" + value + "');");
    }

    /**
     * Method that returns a string of JavaScript code that is used to control the behavior of a progress bar.
     * The progress bar is used to adjust the size of the game board in the UI.
     *
     * @return A string of JavaScript code that controls the behavior of a progress bar.
     */
    public String getProgressBarCode() {
        return """
                const container = document.querySelector('.progress-container');
                const bar = document.querySelector('.progress-bar');
                const boardSizeNumber = document.querySelector('.board_size_number');
                const gameLayout = document.querySelector('.game_layout');
                let isDragging = false;
                
                const getNumericValue = (value) => parseFloat(value.match(/(\\\\d+(\\\\.\\\\d+)?)|(\\\\.\\\\d+)/)[0]);
                const minAllowedWidth = getNumericValue(getComputedStyle(gameLayout).getPropertyValue('min-width'));
                const maxAllowedWidth = getNumericValue(getComputedStyle(gameLayout).getPropertyValue" + "('max-width'));
                
                container?.addEventListener('mousedown', (e) => {
                    isDragging = true;
                });
                
                document.addEventListener('mousemove', (e) => {
                  if (isDragging) {
                      const rect = container.getBoundingClientRect();
                      const w = (e.clientX - rect.left) / rect.width * 100;
                      console.log(w, e.clientX, minAllowedWidth, maxAllowedWidth, (minAllowedWidth + (w * 0.01 * (maxAllowedWidth - minAllowedWidth))).toFixed(2));
                      let percent = Math.min(100, Math.max(0, w));
                
                      const mappedWidth = (minAllowedWidth + (w * 0.01 * (maxAllowedWidth - minAllowedWidth))).toFixed(2) + 'px';
                      if (container.style.opacity === '1') {
                         bar.style.width = `${percent}%`;
                         localStorage.setItem('boardSize', percent);
                         gameLayout.style.width = mappedWidth;
                         container.value = percent;
                         boardSizeNumber.innerHTML = `${Math.floor(percent)}%`;
                      }
                  }
                });
                
                document.addEventListener('mouseup', () => {
                  isDragging = false;
                });
                """;
    }

    /**
     * Method used to show or hide the pieces on the game board.
     * @param show A boolean value indicating whether to show the pieces (true) or hide them (false).
     */
    public void showPieces(boolean show) {
        showPieces = show;
        showPiecesButton.setValue(show);
        gameLayout.getChildren().forEach(component -> component.getChildren().forEach(componentRow -> {
            componentRow.getChildren().forEach(componentSquare -> {
                componentSquare.getChildren().forEach(componentImage -> {
                    componentImage.setVisible(show);
                });
            });
        }));
    }

    /**
     * Method used to show or hide the game board.
     * @param show A boolean value indicating whether to show the board (true) or hide it (false).
     */
    public void showBoard(boolean show) {
        showBoard = show;
        showBoardButton.setValue(show);
        gameLayout.getChildren().forEach(component -> component.getChildren().forEach(componentRow -> {
            componentRow.getChildren().forEach(componentSquare -> {
                componentSquare.setVisible(show);
            });
        }));
    }

    /**
     * Method that highlights a square on the game board.
     * @param location which square to highlight
     */
    public void highlightSquare(BoardLocation location) {
        int row = location.getY();
        int column = location.getX();

        String squareClassName = column + "-" + row;
        String jsCode = "let element = document.getElementById(\"" + squareClassName + "\");"
                + "element?.classList.add(\"highlighted\");";
        this.getElement().executeJs(jsCode);
    }

    /**
     * Method that removes the highlighted square on the game board.
     * @param location which square to remove the highlight from
     */
    public void removeHighlightSquare(BoardLocation location) {
        int row = location.getY();
        int column = location.getX();

        String squareClassName = column + "-" + row;
        String jsCode = "let element = document.getElementById(\"" + squareClassName + "\");"
                + "element?.classList.remove(\"highlighted\");";
        this.getElement().executeJs(jsCode);
    }

    /**
     * Plays a move in the game.
     * @param move the move to be played
     */
    public void playMove(Move move) {
        updateGameStateAndBoard(move);
        checkIfGameEnded();

        if (GameState.isPawnPromotionPending) {
            handlePawnPromotion(move);
        } else {
            getAlgebraicNotation();
        }

        managePieceMoveDrills();
        updateValidMovesList();
    }

    /**
     * Updates the game state and the game board after a move is played.
     * @param move the Move object representing the move that was played
     */
    private void updateGameStateAndBoard(Move move) {
        if (isMoveValid(move)) {
            gameState.playMove(move, false);
            board = gameState.getBoard();
            printBoard(board.pieces);
            createBoard(board.pieces);
        }
    }

    /**
     * Method that checks if a move is valid.
     * @param move the move to check
     * @return if the move is valid
     */
    public boolean isMoveValid(Move move) {
        for (Move validMove : validMovesForPosition) {
            if (move.equals(validMove)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method that updates the valid moves list for the current position.
     */
    public void updateValidMovesList() {
        validMovesForPosition = Rules.getAllValidMovesForTeam(GameState.currentTurn, board, true);
    }

    /**
     * Handles the pawn promotion when a pawn reaches the opposite end of the board.
     * @param move the Move object representing the move that resulted in the pawn promotion
     */
    private void handlePawnPromotion(Move move) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Pawn Promotion");

        String[] pieceTypes = {"Queen", "Rook", "Knight", "Bishop"};
        PieceType[] pieces = {PieceType.QUEEN, PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP};

        for(int i = 0; i < 4; i++) {
            Image pieceButton = initializePawnPromotionButton("_" + pieceTypes[i].toLowerCase() + ".svg", pieceTypes[i], pieces[i], move, dialog);
            dialog.add(pieceButton);
        }

        dialog.setCloseOnOutsideClick(false);
        dialog.open();
        add(dialog);
    }

    /**
     * Manages piece move drills.
     * If there is a target square and the piece for kingless games is on that square,
     * it generates a knight game, creates the game board, and shows the target square.
     */
    private void managePieceMoveDrills() {
        if (targetSquare != null && Rules.isPieceOnSquare(pieceForKinglessGames, targetSquare, board)) {
            generatePieceTrainingGame(trainingPiece);
            createBoard(board.pieces);
            showTargetSquare(startSquare.getAlgebraicNotationLocation() + " → " + targetSquare.getAlgebraicNotationLocation());
        }
    }

    /**
     * Initializes the pawn promotion button.
     * @param imagePath The path to the image file for the button.
     * @param pieceString The string representation of the piece.
     * @param pieceType The type of the promoted piece.
     * @param move The move associated with the pawn promotion.
     * @param dialog The dialog window to close after the pawn promotion.
     * @return The initialized pawn promotion button.
     */
    private Image initializePawnPromotionButton(String imagePath, String pieceString, PieceType pieceType, Move move, Dialog dialog) {
        Image button = new Image("images/sprites/" + ((GameState.currentTurn == Team.WHITE ? "w" : "b") + imagePath), pieceString);
        button.addClickListener(view -> {
            gameState.movePieceAndEndTurn(GameState.promotionLocation);
            gameState.promotePawn(pieceType);
            move.getMovedPiece().promotion = pieceType;
            board.getPiece(move.getTo()).promotion = pieceType;
            gameState.createParsedMoveHistory(move);
            getAlgebraicNotation();
            dialog.close();
            createBoard(board.pieces);
            checkIfGameEnded();
        });
        return button;
    }

    /**
     * Displays the algebraic notation history.
     * Prints the move history, parsed move history, and fen move history to the console.
     */
    private void getAlgebraicNotation() {
        showAlgebraicNotationHistory();
        System.out.println("moveHistory: " + gameState.moveHistory);
        System.out.println("parsedMoveHistory: " + gameState.parsedMoveHistory);
        System.out.println("fenMoveHistory: " + gameState.fenMoveHistory);
    }

    /**
     * Remove old items from the algebraic notation history layout and display the new items.
     * Prints the parsed move history to the console.
     */
    private void showAlgebraicNotationHistory() {
        // Remove old items
        List<Component> componentsToRemove = algebraicNotationHistoryLayout.getChildren().toList();
        for (Component component : componentsToRemove) {
            algebraicNotationHistoryLayout.remove(component);
        }

        for (int i = ((int) algebraicNotationHistoryLayout.getChildren().count()); i < gameState.parsedMoveHistory.size(); i += 2) {
            String parsedMove = gameState.parsedMoveHistory.get(i);

            Paragraph whiteMove = new Paragraph(parsedMove);
            whiteMove.addClassName("algebraic_history_item");
            Paragraph blackMove;
            if (!(i + 1 >= gameState.moveHistory.size())) {
                String nextParsedMove = gameState.parsedMoveHistory.get(i + 1);
                blackMove = new Paragraph(nextParsedMove);
            } else {
                blackMove = new Paragraph("");
            }
            blackMove.addClassName("algebraic_history_item");

            HorizontalLayout moveText = new HorizontalLayout();
            whiteMove.getStyle().set("margin-right", "2em");
            moveText.add(whiteMove, blackMove);
            moveText.setWidth("auto");

            var moveLayout = new HorizontalLayout();
            moveLayout.addClassName("move_layout");
            moveLayout.add((i / 2 + 1) + ". ");
            moveLayout.add(moveText);

            algebraicNotationHistoryLayout.add(moveLayout);
            algebraicNotationHistoryLayout.getElement().executeJs("this.scrollTop = this.scrollHeight");
        }
    }

    /**
     * Check if the game has ended and display the appropriate message and dialog.
     */
    private void checkIfGameEnded() {
        if (gameState.hasGameEnded) {
            if (Rules.isStalemate(GameState.currentTurn, board) && !GameState.isPawnPromotionPending) {
                Notification.show("Game Over!");
                VerticalLayout dialogLayout = new VerticalLayout();
                Text dialogText = new Text("Game Drawn by Stalemate");
                createGameOverDialog(dialogLayout, dialogText);
            }
            if (Rules.isCheckmate(GameState.currentTurn, board)) {
                Notification.show("Game Over!");
                VerticalLayout dialogLayout = new VerticalLayout();
                Text dialogText = new Text((GameState.currentTurn == Team.WHITE ? "Black" : "White") + " Won by Checkmate");
                createGameOverDialog(dialogLayout, dialogText);
            }
        }
    }

    /**
     * Create the game over dialog with the specified layout and text.
     * @param dialogLayout The layout for the dialog.
     * @param dialogText   The text to be displayed in the dialog.
     */
    private void createGameOverDialog(VerticalLayout dialogLayout, Text dialogText) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Game Over!");
        dialog.setCloseOnOutsideClick(true);
        dialogLayout.add(dialogText);
        Button newGameButton = new Button("New Game");
        newGameButton.addClickListener(view -> {
            board.resetBoardPosition(STARTING_POSITION);
            gameState.resetHistory();
            GameState.moveNumber = 0;
            gameState.hasGameEnded = false;
            dialog.close();
            createBoard(board.pieces);
        });
        dialogLayout.add(newGameButton);
        dialog.add(dialogLayout);
        dialog.open();
        add(dialog);
    }

    /**
     * Method that creates the UI for the board.
     * @param pieces the pieces to create the board with
     */
    public void createBoard(Piece[] pieces) {
        if (!localStorageItemsLoaded) return;
        VerticalLayout boardLayout = findBoardLayout();
        if (boardLayout == null) {
            boardLayout = new VerticalLayout();
            boardLayout.setAlignItems(Alignment.CENTER);
            boardLayout.setClassName("board");
            gameLayout.removeAll();
            gameLayout.add(boardLayout);
        } else {
            boardLayout.removeAll();
        }

        for (int rank = 7; rank >= 0; rank--) {
            HorizontalLayout rowLayout = new HorizontalLayout();
            rowLayout.addClassName("board_row");

            for (int file = 0; file < 8; file++) {
                int index = rank * 8 + file;
                Piece piece = pieces[index];

                String cellValue;
                VerticalLayout square = new VerticalLayout();
                square.addClassName("square");
                square.setVisible(showBoard);

                if ((rank + file) % 2 == 0) {
                    square.addClassName("dark_square");
                } else {
                    square.addClassName("light_square");
                }
                square.addClassName(board.getPointFromArrayIndex(index).getX() + "-" + board.getPointFromArrayIndex(index).getY());
                square.setId(board.getPointFromArrayIndex(index).getX() + "-" + board.getPointFromArrayIndex(index).getY());
                VerticalLayout finalBoardLayout = boardLayout;
                square.addClickListener(view -> {
                    if (gameState.hasGameEnded) return;
                    gameState.playMove(new Move(board.getPointFromArrayIndex(index), new BoardLocation(-1, -1)), false);

                    AtomicReference<BoardLocation> selectedSquare = new AtomicReference<>(null);
                    finalBoardLayout.getChildren().forEach(component -> component.getChildren().forEach(componentRow -> {
                        if (componentRow.getClassNames().contains("selected")) {
                            componentRow.getClassNames().forEach(className -> {
                                if (className.contains("-")) {
                                    var coordinates = className.split("-");
                                    selectedSquare.set(new BoardLocation(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1])));
                                }
                            });
                        }
                    }));

                    AtomicReference<BoardLocation> toLocation = new AtomicReference<>(null);
                    if ((piece == null || piece.team != GameState.currentTurn) && selectedSquare.get() != null) {
                        square.getClassNames().forEach(className -> {
                            if (className.contains("-")) {
                                var coordinates = className.split("-");
                                toLocation.set(new BoardLocation(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1])));
                            }
                        });
                        System.out.println("MOVE to " + toLocation.get().getX() + "," + toLocation.get().getY() + " from " + selectedSquare.get().getX() + "," + selectedSquare.get().getY());
                        playMove(new Move(selectedSquare.get(), toLocation.get(), board.getPiece(selectedSquare.get()), board.getPiece(toLocation.get())));
                    }

                    // Remove all previously selected squares
                    if (piece != null && (piece.team == GameState.currentTurn || GameState.kinglessGame)) {
                        finalBoardLayout.getChildren().forEach(component -> component.getChildren()
                                .forEach(componentRow -> componentRow.getClassNames().remove("selected")));
                    } else {
                        return;
                    }

                    square.addClassName("selected");
                });
                if (piece == null) {
                    cellValue = "";
                } else {
                    cellValue = String.valueOf(fenUtils.getSymbolFromPieceType(piece.type, piece.team));
                    Image image = new Image(getChessPieceUrl(cellValue), cellValue);
                    image.addClassName("square_image");
                    image.setVisible(showPieces);

                    DragSource<Image> dragSource = DragSource.create(image);
                    dragSource.setDraggable(true);
                    final int finalFile = file, finalRank = rank;
                    dragSource.addDragStartListener(event -> {
                        event.setDragData(new BoardLocation(finalFile, finalRank));
                        System.out.println("DRAG from " + finalFile + "," + finalRank);
                    });

                    square.add(image);
                }

                DropTarget<VerticalLayout> dropTarget = DropTarget.create(square);
                final int finalFile1 = file, finalRank1 = rank;
                dropTarget.addDropListener(event -> {
                    System.out.println("DROP to " + finalFile1 + "," + finalRank1);
                    var from = (BoardLocation) event.getDragData().orElse(null);
                    var to = new BoardLocation(finalFile1, finalRank1);
                    var movedPiece = board.getPiece(from);
                    playMove(new Move(from, to, movedPiece));
                });

                rowLayout.add(square);
            }
            boardLayout.add(rowLayout);
        }
    }

    /**
     * Find the board layout in the game content layout.
     * @return The board layout.
     */
    private VerticalLayout findBoardLayout() {
        AtomicReference<VerticalLayout> layout = new AtomicReference<>();
        gameContentLayout.getChildren().forEach(component -> {
            if (component instanceof VerticalLayout && component.getClassNames().contains("board")) {
                layout.set((VerticalLayout) component);
            }
        });
        return layout.get();
    }


    /**
     * Get the URL of the chess piece image based on the specified piece string.
     * @param s The piece string. Must be one of the following: "P", "N", "B", "R", "Q", "K", "p", "n", "b", "r", "q", "k".
     * @return The URL of the chess piece image. Returns an empty string if the piece string is null or invalid.
     */
    private String getChessPieceUrl(String s) {
        if (s == null) {
            return "";
        }

        var url = "";
        switch (s) {
            case "P" -> url = "images/sprites/w_pawn.svg";
            case "N" -> url = "images/sprites/w_knight.svg";
            case "B" -> url = "images/sprites/w_bishop.svg";
            case "R" -> url = "images/sprites/w_rook.svg";
            case "Q" -> url = "images/sprites/w_queen.svg";
            case "K" -> url = "images/sprites/w_king.svg";
            case "p" -> url = "images/sprites/b_pawn.svg";
            case "n" -> url = "images/sprites/b_knight.svg";
            case "b" -> url = "images/sprites/b_bishop.svg";
            case "r" -> url = "images/sprites/b_rook.svg";
            case "q" -> url = "images/sprites/b_queen.svg";
            case "k" -> url = "images/sprites/b_king.svg";
        }
        return url;
    }

    /**
     * Print the chess board.
     * @param pieces An array of Piece objects representing the chess board.
     */
    private void printBoard(Piece[] pieces) {
        // Remove old boards
        String classNameToRemove = "board";
        List<Component> componentsToRemove = getChildren().toList();
        for (Component component : componentsToRemove) {
            if (component.getClassNames().contains(classNameToRemove)) {
                remove(component);
            }
        }

        for (int rank = 7; rank >= 0; rank--) {
            Paragraph rankParagraph = new Paragraph();
            StringBuilder builder = new StringBuilder();
            for (int file = 0; file < 8; file++) {
                int index = rank * 8 + file;
                Piece piece = pieces[index];

                if (piece == null) {
                    System.out.print(". "); // Empty square
                    builder.append(". ");
                } else {
                    System.out.print(fenUtils.getSymbolFromPieceType(piece.type, piece.team) + " "); // Piece character
                    builder.append(fenUtils.getSymbolFromPieceType(piece.type, piece.team)).append(" ");
                }
            }
            System.out.println();
            rankParagraph.setText(builder.toString());
            rankParagraph.setId(String.valueOf(rank));
            rankParagraph.addClassName("board");
        }
        System.out.println("-----------------------------");
    }

    /**
     * Generate a chess game with a single knight piece.
     * @return An array of Piece objects representing the chess board.
     */
    private Piece[] generatePieceTrainingGame(Piece piece) {
        pieceForKinglessGames = piece;

        Piece[] pieces = new Piece[64];
        fenUtils = new FenUtils(pieces);

        int piecePos = new Random().nextInt(64);
        pieces[piecePos] = piece;

        board = new Board(64, pieces, fenUtils);
        gameState = new GameState(board, fenUtils.getStartingTeam(), true);
        startSquare = board.getPointFromArrayIndex(piecePos);
        if (piece.type.equals(PieceType.KNIGHT)) {
            targetSquare = board.getPointFromArrayIndex(getRandomSquareWithinDistance(piecePos, 3, false));
        } else if (piece.type.equals(PieceType.BISHOP)) {
            targetSquare = board.getPointFromArrayIndex(getRandomSquareWithinDistance(piecePos, 5, true));
        } else {
            targetSquare = board.getPointFromArrayIndex(getRandomSquareWithinDistance(piecePos, 5, false));
        }
        System.out.println(fenUtils.generateFenFromPosition(pieces));
        System.out.println(targetSquare.getAlgebraicNotationLocation());
        return pieces;
    }

    /**
     * Display the target square of the chess game.
     * Used for piece move drills.
     * @param s The target square to be displayed.
     */
    public void showTargetSquare(String s) {
        targetSquareLayout.removeAll();
        targetSquareLayout.setVisible(true);
        var paragraph = new Paragraph(s);
        paragraph.setWidthFull();
        paragraph.addClassName("target_square");
        targetSquareLayout.setHeight("auto");
        targetSquareLayout.add(paragraph);
    }

    /**
     * Generates a random square within a given distance from a given square.
     * The generated square will be different from the given square.
     * Used for piece move drills.
     * @param square The square for which the random square is generated.
     * @param distance The maximum distance from the given square.
     * @return A random square within the given distance from the given square.
     */
    public static int getRandomSquareWithinDistance(int square, int distance, boolean isBishop) {
        Random random = new Random();
        int targetSquare;
        var isDarkSquare = (square / 8 + square % 8) % 2 != 0;

        do {
            int file = square % 8;
            int rank = square / 8;

            int minFile = Math.max(0, file - distance);
            int maxFile = Math.min(7, file + distance);
            int minRank = Math.max(0, rank - distance);
            int maxRank = Math.min(7, rank + distance);

            int randomFile = random.nextInt(maxFile - minFile + 1) + minFile;
            int randomRank;

            do {
                randomRank = random.nextInt(maxRank - minRank + 1) + minRank;
                targetSquare = randomRank * 8 + randomFile;
            } while (isBishop && ((targetSquare / 8 + targetSquare % 8) % 2 == 0) == isDarkSquare);

        } while (targetSquare == square);

        return targetSquare;
    }
}