package lukas.sobotik.sightlessknight.gamelogic;

import lombok.Getter;
import lukas.sobotik.sightlessknight.gamelogic.entity.MoveFlag;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    @Getter
    public Board board;
    public static Team currentTurn;
    List<Move> validMoves;
    BoardLocation selectedPieceLocation;

    public static int moveNumber = 0, enPassantCaptures = 0;
    static final Team playerTeam = Team.WHITE;

    public static boolean isPawnPromotionPending = false;
    public static BoardLocation promotionLocation;
    static PieceType selectedPromotionPieceType;

    public boolean hasGameEnded = false;
    public static boolean kinglessGame = false;

    public List<Move> moveHistory;
    public List<String> parsedMoveHistory;
    public List<String> fenMoveHistory;

    /**
     * Constructs a new GameState object.
     *
     * @param board the game board
     * @param startingTeam the team that starts the game
     * @param kinglessGame a boolean indicating if the game is a kingless game
     */
    public GameState(Board board, Team startingTeam, boolean kinglessGame) {
        validMoves = new ArrayList<>();
        moveHistory = new ArrayList<>();
        parsedMoveHistory = new ArrayList<>();
        fenMoveHistory = new ArrayList<>();
        currentTurn = startingTeam;
        moveNumber = 0;
        GameState.kinglessGame = kinglessGame;
        this.board = board;
    }

    /**
     * Moves the selected piece to the given destination and ends the current turn.
     * If the destination is not null, the selected piece is moved to the destination on the game board.
     * The current turn is then switched to the opposing team.
     *
     * @param destination the location on the board where the selected piece will be moved
     */
    public void movePieceAndEndTurn(BoardLocation destination) {
        if (destination != null) {
            board.movePiece(new Move(selectedPieceLocation, destination));
        }
        if (!kinglessGame) {
            currentTurn = (currentTurn == Team.WHITE) ? Team.BLACK : Team.WHITE;
        }
    }

    /**
     * Moves the selected piece to the given destination and ends the current turn.
     * If the move object has both the 'from' and 'to' locations set, the selected piece is moved from the 'from' location to the 'to' location on the game board.
     * The current turn is then switched to the opposing team.
     *
     * @param move the Move object containing the 'from' and 'to' locations
     */
    public void movePieceAndEndTurn(Move move) {
        if (move.getFrom() != null && move.getTo() != null) {
            board.movePiece(move);
        }
        if (!kinglessGame) {
            currentTurn = (currentTurn == Team.WHITE) ? Team.BLACK : Team.WHITE;
        }
    }

    /**
     * Function used for playing a move in the game.
     * @param move which move should be played
     * @param isTest whether the move is being played by a test function like the Perft test or an AI.
     */
    public void playMove(Move move, boolean isTest) {
        Piece piece = move.getMovedPiece();
        if (piece == null) return;
        selectedPieceLocation = move.getFrom();

        moveNumber++;
        // Pawn Promotion
        if (isPawnPromotion(move) && (isTest || move.getPromotionPiece() != null)) {
            promotionLocation = move.getTo();
            movePieceAndEndTurn(move);
            promotePawn(move.getPromotionPiece());
            return;
        }
        if ((move.getTo().getY() == 0 || move.getTo().getY() == 7) && piece.type == PieceType.PAWN && piece.promotion == null && !isTest) {
            promotionLocation = move.getTo();
            isPawnPromotionPending = true;
            return;
        }

        // En Passant
        else if (isEnPassantCapture(move) && isTest) {
            move.setCapturedPiece(board.getPiece(new BoardLocation(move.getTo().getX(), move.getFrom().getY())));
            move.setMoveFlag(MoveFlag.enPassant);
        }

        movePieceAndEndTurn(move);
        if (!isTest) {
            createParsedMoveHistory(move);
            hasGameEnded = Rules.isCheckmate(currentTurn, board) || Rules.isStalemate(currentTurn, board);
        }
    }

    /**
     * Determines if the given move is an en passant capture.
     * @param move the move to check
     * @return true if the move is an en passant capture, false otherwise
     */
    private boolean isEnPassantCapture(Move move) {
        BoardLocation enPassantCapture = new BoardLocation(move.getTo().getX(), move.getFrom().getY());
        return board.getPiece(enPassantCapture) != null
                && board.getPiece(enPassantCapture).type == PieceType.PAWN
                && board.getPiece(enPassantCapture).team != move.getMovedPiece().team
                && move.getMovedPiece().type == PieceType.PAWN
                && move.getFrom().getX() != move.getTo().getX()
                && (board.getPiece(enPassantCapture).doublePawnMoveOnMoveNumber == GameState.moveNumber - 1);
    }

    /**
     * Determines if the given move is a pawn promotion.
     *
     * @param move the move to check
     * @return true if the move is a pawn promotion, false otherwise
     */
    private boolean isPawnPromotion(Move move) {
        return (move.getTo().getY() == 0 || move.getTo().getY() == 7)
                && move.getMovedPiece().type == PieceType.PAWN
                && move.getPromotionPiece() != null;
    }

    /**
     * Undoes a move and reverts the game state to the previous state.
     *
     * @param move the move to undo
     */
    public void undoMove(Move move) {
        if (moveNumber - 1 >= 0) moveNumber -= 1;

        board.undoMove(move);

        currentTurn = (currentTurn == Team.WHITE) ? Team.BLACK : Team.WHITE;
    }

    /**
     * Creates a parsed move history for the given move.
     *
     * @param move the move to create the parsed move history for
     */
    public void createParsedMoveHistory(Move move) {
        boolean isPromotionSquare = (move.getTo().getY() == 0 && move.getMovedPiece().team == Team.BLACK) || (move.getTo().getY() == 7 && move.getMovedPiece().team == Team.WHITE);
        if (move.getMovedPiece().type.equals(PieceType.PAWN) && isPromotionSquare && move.getMovedPiece().promotion == null) return;

        moveHistory.add(move);
        String parsedMove = new AlgebraicNotationUtils(new FenUtils(board.pieces), this, board).getParsedMove(move);
        parsedMoveHistory.add(parsedMove);

        FenUtils fenUtils = new FenUtils(board.pieces, board.whiteKingLocation, board.blackKingLocation, board.lastToLocation, board.lastDoublePawnMoveWithWhitePieces, board.lastDoublePawnMoveWithBlackPieces);
        fenMoveHistory.add(fenUtils.generateFenFromPosition(fenUtils.pieces));
    }

    /**
     * Resets the move history, parsed move history, and fen move history.
     */
    public void resetHistory() {
        moveHistory = new ArrayList<>();
        parsedMoveHistory = new ArrayList<>();
        fenMoveHistory = new ArrayList<>();
    }

    /**
     * Promotes a pawn on the board to a selected piece type.
     *
     * @param selectedPieceType the selected piece type to promote the pawn to
     */
    public void promotePawn(PieceType selectedPieceType) {
        board.promotePawn(promotionLocation, selectedPieceType);
        currentTurn = (currentTurn == Team.WHITE) ? Team.BLACK : Team.WHITE;
        isPawnPromotionPending = false;
        promotionLocation = null;
        selectedPromotionPieceType = null;
        movePieceAndEndTurn((BoardLocation) null);
    }
}