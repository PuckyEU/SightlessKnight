package lukas.sobotik.sightlessknight.gamelogic;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;

/**
 * Represents a chess piece.
 */
@Builder (toBuilder = true)
public class Piece {
	public Team team;
	public PieceType type;
	public int doublePawnMoveOnMoveNumber = -10;
	boolean hasMoved = false;
	public boolean enPassant = false;
	@Getter
	@Setter
	public String castling = null;
	public PieceType promotion = null;
	public int index;

	public Piece() {
	}

	/**
	 * Creates a new Instance of the Piece class with the given team and type.
	 *
	 * @param team The team of the piece.
	 * @param type The type of the piece.
	 */
	public Piece(Team team, PieceType type) {
		this.team = team;
		this.type = type;
	}

    /**
     * Copy constructor for the Piece class.
     * Creates a new Piece object by copying the properties of the given Piece object.
     *
     * @param copyPiece The Piece object to be copied.
     */
    public Piece(Piece copyPiece) {
        this.team = copyPiece.team;
        this.type = copyPiece.type;
        this.doublePawnMoveOnMoveNumber = copyPiece.doublePawnMoveOnMoveNumber;
        this.hasMoved = copyPiece.hasMoved;
        this.enPassant = copyPiece.enPassant;
        this.castling = copyPiece.castling;
        this.promotion = copyPiece.promotion;
		this.index = copyPiece.index;
    }

	/**
	 * Creates a new Instance of the Piece class with the given team, type, and double pawn move number.
	 *
	 * @param team The team of the piece.
	 * @param type The type of the piece.
	 * @param doublePawnMoveOnMoveNumber The move number on which the double pawn move was made.
	 */
	public Piece(Team team, PieceType type, int doublePawnMoveOnMoveNumber) {
		this.team = team;
		this.type = type;
		this.doublePawnMoveOnMoveNumber = doublePawnMoveOnMoveNumber;
	}

	/**
	 * Creates a new Instance of the Piece class with the given team, type, double pawn move number,
	 * has moved status, en passant status, castling status, and promotion type.
	 *
	 * @param team The team of the piece.
	 * @param type The type of the piece.
	 * @param doublePawnMoveOnMoveNumber The move number on which the double pawn move was made.
	 * @param hasMoved Indicates whether the piece has moved or not.
	 * @param enPassant Indicates whether the piece is eligible for en passant capture or not.
	 * @param castling The castling status of the piece.
	 * @param promotion The type of piece to which the current piece can be promoted.
	 * @param index The index of the piece.
	 */
	public Piece (Team team, PieceType type, int doublePawnMoveOnMoveNumber, boolean hasMoved, boolean enPassant, String castling, PieceType promotion, int index) {
		this.team = team;
		this.type = type;
		this.doublePawnMoveOnMoveNumber = doublePawnMoveOnMoveNumber;
		this.hasMoved = hasMoved;
		this.enPassant = enPassant;
		this.castling = castling;
		this.promotion = promotion;
		this.index = index;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof final Piece piece)) return false;
        return piece.team == this.team && piece.type == this.type;
	}
}