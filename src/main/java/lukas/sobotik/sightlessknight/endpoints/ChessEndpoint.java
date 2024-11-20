package lukas.sobotik.sightlessknight.endpoints;

import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import dev.hilla.Endpoint;
import lukas.sobotik.sightlessknight.ai.PerftFunction;
import lukas.sobotik.sightlessknight.gamelogic.AlgebraicNotationUtils;
import lukas.sobotik.sightlessknight.gamelogic.Board;
import lukas.sobotik.sightlessknight.gamelogic.BoardLocation;
import lukas.sobotik.sightlessknight.gamelogic.FenUtils;
import lukas.sobotik.sightlessknight.gamelogic.GameState;
import lukas.sobotik.sightlessknight.gamelogic.Move;
import lukas.sobotik.sightlessknight.gamelogic.Piece;
import lukas.sobotik.sightlessknight.gamelogic.Rules;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Endpoint
@AnonymousAllowed
public class ChessEndpoint {
    public static String STARTING_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
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

    /**
     * Initializes the game board and pieces.
     * This method is called internally when starting a new game.
     */
    public void initializeBoard(String piece) {
        try {
            if (piece != null && !piece.isEmpty()) {
                isTrainingMode = true;
                trainingPiece = new Piece(GameState.currentTurn, getPieceTypeFromString(piece));
                generatePieceTrainingGame(trainingPiece);
                initializeGame(true);
                return;
            }

            Piece[] pieces = new Piece[64];
            fenUtils = new FenUtils(pieces);
            pieces = fenUtils.generatePositionFromFEN(STARTING_POSITION);
            board = new Board(64, pieces, fenUtils);

            initializeGame(false);
        } catch (Exception e) {
            System.err.println("Error initializing board: " + e.getMessage());
        }
    }

    public void initializeGame(boolean kinglessGame) {
        gameState = new GameState(board, fenUtils.getStartingTeam(), kinglessGame);
        algebraicNotationUtils = new AlgebraicNotationUtils(fenUtils, gameState, board);
        algebraicNotationUtils.setKinglessGame(kinglessGame);
        validMovesForPosition = Rules.getAllValidMovesForTeam(GameState.currentTurn, board, true);
    }

    public void resetGame() {
        initializeBoard(null);
    }

    /**
     * Plays a move in the game.
     * @param move the move to be played
     */
    public void playMove(Move move) {
        updateGameStateAndBoard(move);

        System.out.println("current turn: " + GameState.currentTurn);

        if (GameState.isPawnPromotionPending) {
//            handlePawnPromotion(move);
        } else {
//            getAlgebraicNotation();
        }

        managePieceMoveDrills();
        updateValidMovesList();
    }

    public void playMoveFromText(String text) {
        algebraicNotationUtils.updateVariables(fenUtils, gameState, board);
        Move move = algebraicNotationUtils.getMoveFromParsedMove(text);
        if (move == null) {
            Notification.show("Invalid Move");
            return;
        }
        playMove(move);
    }

    /**
     * Preform a Perft test on the current position.
     *
     * @param depth the depth of the Perft test.
     */
    public Double playPerftTest(int depth) throws InterruptedException, ExecutionException {
        var perftFunction = new PerftFunction(gameState.board, gameState, null);
        System.out.println("Playing Perft test with depth: " + depth);
        Future<Double> future = Executors.newSingleThreadExecutor().submit(() -> {
            var positions = perftFunction.playMoves(depth, GameState.currentTurn, false, true, true);
            System.out.println("Positions: " + positions);
            return positions;
        });
        return future.get();
    }

    /**
     * Undo the last move played in the game.
     */
    public void undoMove() {
        if (!gameState.moveHistory.isEmpty()) {
            Move lastMove = gameState.moveHistory.get(gameState.moveHistory.size() - 1);
            gameState.undoMove(lastMove);
            gameState.moveHistory.remove(gameState.moveHistory.size() - 1);
        }
    }

    /**
     * Get the current FEN position of the game.
     * @return the current FEN position of the game.
     */
    public String getCurrentPosition() {
        return fenUtils.generateFenFromPosition(board.pieces);
    }

    /**
     * Get the valid moves for the current position.
     * @return the valid moves for the current position.
     */
    public List<Move> getValidMovesForPosition() {
        return validMovesForPosition;
    }

    /**
     * Get the target square of the piece for piece move drills.
     * @return the target square of the piece for piece move drills.
     */
    public BoardLocation getTargetSquare() {
        return targetSquare;
    }

    /**
     * Get the start square of the piece for piece move drills.
     * @return the start square of the piece for piece move drills.
     */
    public BoardLocation getStartSquare() {
        return startSquare;
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
     * Get the algebraic notation of the moves played in the game.
     * @return the list of the moves played in the game.
     */
    public List<String> getMoveHistory() {
        return gameState.parsedMoveHistory;
    }

    /**
     * Check if the game has ended and display the appropriate message and dialog.
     */
    public String checkIfGameEnded() {
        if (gameState.hasGameEnded) {
            if (Rules.isStalemate(GameState.currentTurn, board) && !GameState.isPawnPromotionPending) {
                return "Stalemate";
            }
            if (Rules.isCheckmate(GameState.currentTurn, board)) {
                return "Checkmate";
            }
        }
        return "";
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

    private PieceType getPieceTypeFromString(String piece) {
        String formattedPiece = piece.toUpperCase();
        try {
            return PieceType.valueOf(formattedPiece);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid piece type: " + formattedPiece);
            return null;
        }
    }

    /**
     * Manages piece move drills.
     * If there is a target square and the piece for kingless games is on that square,
     * it generates a knight game, creates the game board, and shows the target square.
     */
    private void managePieceMoveDrills() {
        if (targetSquare != null && Rules.isPieceOnSquare(pieceForKinglessGames, targetSquare, board)) {
            generatePieceTrainingGame(trainingPiece);
        }
    }

    /**
     * Print the chess board.
     */
    public void printBoard() {
        printBoard(board.pieces);
    }

    /**
     * Print the chess board.
     * @param pieces An array of Piece objects representing the chess board.
     */
    private void printBoard(Piece[] pieces) {
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
