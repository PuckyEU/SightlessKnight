package lukas.sobotik.sightlessknight.gamelogic;

import lombok.Getter;
import lukas.sobotik.sightlessknight.gamelogic.entity.MoveFlag;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;

@Getter
public class Move {
    private BoardLocation from, to;
    private Piece movedPiece, capturedPiece;
    private PieceType promotionPiece;
    private MoveFlag moveFlag;

    public Move() {
    }

    public Move(BoardLocation from, BoardLocation to) {
        this.from = from;
        this.to = to;
        moveFlag = MoveFlag.none;
    }

    public Move(BoardLocation from, BoardLocation to, Piece movedPiece) {
        this.from = from;
        this.to = to;
        this.movedPiece = movedPiece;
        moveFlag = MoveFlag.none;
    }

    public Move(BoardLocation from, BoardLocation to, Piece movedPiece, Piece capturedPiece) {
        this.from = from;
        this.to = to;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
        moveFlag = MoveFlag.none;
    }

    public void setFrom(BoardLocation from) {
        this.from = from;
    }

    public void setTo(BoardLocation to) {
        this.to = to;
    }

    public void setMovedPiece(Piece movedPiece) {
        this.movedPiece = movedPiece;
    }

    public void setCapturedPiece(Piece capturedPiece) {
        this.capturedPiece = capturedPiece;
    }

    public void setPromotionPiece(PieceType promotionPiece) {
        this.promotionPiece = promotionPiece;
    }

    public void setMoveFlag(MoveFlag moveFlag) {
        this.moveFlag = moveFlag;
    }

    /**
     * Returns the algebraic notation of the move.
     * @param pieces the pieces on the board
     * @param gameState the current state of the game
     * @param board the game board
     * @return the algebraic notation of the move
     */
    public String getMoveNotation(Piece[] pieces, GameState gameState, Board board) {
        return new AlgebraicNotationUtils(this, new FenUtils(pieces), gameState, board).getParsedMove(this);
    }

    /**
     * Returns a simplified version of the moved piece.
     * @return a simplified version of the moved piece.
     */
    public Piece getSimplifiedMovedPiece() {
        return new Piece(movedPiece.team, movedPiece.type);
    }

    /**
     * Returns a simplified version of the captured piece.
     * @return a simplified version of the captured piece.
     */
    public Piece getSimplifiedCapturedPiece() {
        if (capturedPiece == null) return new Piece(null, null);
        return new Piece(capturedPiece.team, capturedPiece.type);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof final Move move)) return false;
        if (move.movedPiece == null || movedPiece == null) return move.from.equals(from) && move.to.equals(to);
        return move.from.equals(from) && move.to.equals(to) && move.movedPiece.equals(movedPiece);
    }
}
