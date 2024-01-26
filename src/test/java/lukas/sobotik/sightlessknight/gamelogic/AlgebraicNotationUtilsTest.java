package lukas.sobotik.sightlessknight.gamelogic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlgebraicNotationUtilsTest {

    @ParameterizedTest
    @MethodSource("testPositionsProvider")
    void testGetMoveFromParsedMove(String fen, String parsedMove, BoardLocation expectedFrom, BoardLocation expectedTo, boolean enPassant) {
        Piece[] pieces = new Piece[64];
        FenUtils fenUtils = new FenUtils(pieces);
        pieces = fenUtils.generatePositionFromFEN(fen);
        Board board = new Board(64, pieces, fenUtils);
        if (enPassant) {
            board.getPiece(expectedTo.transpose(0, -1)).doublePawnMoveOnMoveNumber = 0;
        }

        GameState gameState = new GameState(board, fenUtils.getStartingTeam(), false);

        AlgebraicNotationUtils algebraicNotationUtils = new AlgebraicNotationUtils(fenUtils, gameState, board);

        Move returnedMove = algebraicNotationUtils.getMoveFromParsedMove(parsedMove);
        var expectedMove = new Move(expectedFrom, expectedTo, board.getPiece(expectedFrom), board.getPiece(expectedTo.transpose(0, enPassant ? -1 : 0)));

        assertEquals(returnedMove.getFrom(), expectedMove.getFrom(),
                "from " + returnedMove.getFrom().getAlgebraicNotationLocation()
                        + " does not match expected from "
                        + expectedMove.getFrom().getAlgebraicNotationLocation());
        assertEquals(returnedMove.getTo(), expectedMove.getTo(),
                "to " + returnedMove.getTo().getAlgebraicNotationLocation()
                        + " does not match expected to "
                        + expectedMove.getTo().getAlgebraicNotationLocation());
        // Moved piece
        assertEquals(returnedMove.getSimplifiedMovedPiece().team, expectedMove.getSimplifiedMovedPiece().team,
                "moved piece " + returnedMove.getSimplifiedMovedPiece().team + " " + returnedMove.getSimplifiedMovedPiece().type
                        + " does not match expected moved piece "
                        + expectedMove.getSimplifiedMovedPiece().team + " " + expectedMove.getSimplifiedMovedPiece().type);
        assertEquals(returnedMove.getSimplifiedMovedPiece().type, expectedMove.getSimplifiedMovedPiece().type,
                     "moved piece " + returnedMove.getSimplifiedMovedPiece().team + " " + returnedMove.getSimplifiedMovedPiece().type
                             + " does not match expected moved piece "
                             + expectedMove.getSimplifiedMovedPiece().team + " " + expectedMove.getSimplifiedMovedPiece().type);
        // Captured Piece
        assertEquals(returnedMove.getSimplifiedCapturedPiece().team, expectedMove.getSimplifiedCapturedPiece().team,
                "captured piece " + returnedMove.getSimplifiedCapturedPiece().team + " " + returnedMove.getSimplifiedCapturedPiece().type
                        + " does not match expected captured piece "
                        + expectedMove.getSimplifiedCapturedPiece().team + " " + expectedMove.getSimplifiedCapturedPiece().type);
        assertEquals(returnedMove.getSimplifiedCapturedPiece().type, expectedMove.getSimplifiedCapturedPiece().type,
                     "captured piece " + returnedMove.getSimplifiedCapturedPiece().team + " " + returnedMove.getSimplifiedCapturedPiece().type
                             + " does not match expected captured piece "
                             + expectedMove.getSimplifiedCapturedPiece().team + " " + expectedMove.getSimplifiedCapturedPiece().type);
    }

    private static Stream<Arguments> testPositionsProvider() {
         return Stream.of(
                 // Pawn moves
                 Arguments.of("8/8/8/1Pp5/8/8/8/5k1K w - - 0 1", "bxc6 e.p", new BoardLocation(1, 4), new BoardLocation(2, 5), true),
                 Arguments.of("5K1k/8/8/8/8/3p4/2P5/8 w - - 0 1", "cxd3", new BoardLocation(2, 1), new BoardLocation(3, 2), false),
                 Arguments.of("5K1k/8/8/8/8/3p4/2P5/8 w - - 0 1", "c3", new BoardLocation(2, 1), new BoardLocation(2, 2), false),
                 Arguments.of("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", "e4", new BoardLocation(4, 1), new BoardLocation(4, 3), false),
                 // Move Disambiguation
                 Arguments.of("rnbqkbnr/pppppppp/8/2N5/4p3/2N1K3/PPPP1PPP/R1BQKB1R w kq - 0 1", "N5xe4", new BoardLocation(2, 4), new BoardLocation(4, 3), false),
                 Arguments.of("rnbqkbnr/pppppppp/8/2N5/4p3/2N1K3/PPPP1PPP/R1BQKB1R w kq - 0 1", "N3xe4", new BoardLocation(2, 2), new BoardLocation(4, 3), false),
                 Arguments.of("rnbqkbnr/pppppppp/8/6N1/4p3/2N1K3/PPPP1PPP/R1BQKB1R w kq - 0 1", "Ngxe4", new BoardLocation(6, 4), new BoardLocation(4, 3), false),
                 Arguments.of("rnbqkbnr/pppppppp/8/6N1/4p3/2N1K3/PPPP1PPP/R1BQKB1R w kq - 0 1", "Ncxe4", new BoardLocation(2, 2), new BoardLocation(4, 3), false),
                 // Castling
                 Arguments.of("rnbqk2r/pppppppp/8/8/8/8/PPPPPPPP/RNBQK2R w KQkq - 0 1", "O-O", new BoardLocation(4, 0), new BoardLocation(6, 0), false),
                 Arguments.of("r3kbnr/pppppppp/8/8/8/8/PPPPPPPP/R3KBNR w KQk - 0 1", "O-O-O", new BoardLocation(4, 0), new BoardLocation(2, 0), false),
                 // Bishop moves
                 Arguments.of("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 1", "Bb5", new BoardLocation(5, 0), new BoardLocation(1, 4), false)
         );
    }

//    public void refreshControlledSquares(Team team, Board board) {
//        long squares = 0L;
//        for (PieceType type : PieceType.values()) {
//            long bitboard = getBitboard(type, team);
//            for (int i = 0; i < 64; i++) {
//                if ((bitboard & (1L << i)) != 0) {
//                    Piece piece = new Piece(team, type);
//                    List<Move> moves = new ArrayList<>();
//                    if (type == PieceType.PAWN) {
//                        // For pawns, consider the squares they can capture, not the squares they can move to
//                        moves.addAll(Rules.getPawnCaptureMoves(new BoardLocation(i % 8, i / 8), team, board, true, true));
//                    } else {
//                        moves.addAll(Rules.getValidMoves(new BoardLocation(i % 8, i / 8), piece, board, false, false, true));
//                    }
//                    for (Move move : moves) {
//                        squares |= (1L << board.getArrayIndexFromLocation(move.getTo()));
//                    }
//                }
//            }
//        }
//        controlledSquares[team == Team.WHITE ? 0 : 1] = squares;
//    }

//    public void updateControlledSquares(Team team, Board board, Move move) {
//        if (getControlledSquares(team) == 0) {
//
//        }
//
//        int sourceIndex = board.getArrayIndexFromLocation(move.getFrom());
//        int destinationIndex = board.getArrayIndexFromLocation(move.getTo());
//
//        // Remove the source square from the controlled squares
//        controlledSquares[team == Team.WHITE ? 0 : 1] &= ~(1L << sourceIndex);
//
//        List<Move> pseudoLegalMoves = Rules.getPseudoLegalMoves(move.getTo(), move.getMovedPiece(), board);
//
//        if (move.getMovedPiece() != null && move.getMovedPiece().type == PieceType.PAWN) {
//            // For pawns, consider the squares they can capture, not the squares they can move to
//            pseudoLegalMoves.addAll(Rules.getPawnCaptureMoves(move.getTo(), team, board, true, true));
//
//            // Remove the old controlled squares of the pawn
//            List<Move> removeMoves = Rules.getPawnCaptureMoves(move.getFrom(), team, board, true, true);
//            for (Move removeMove : removeMoves) {
//                int removeIndex = board.getArrayIndexFromLocation(removeMove.getTo());
//                controlledSquares[team == Team.WHITE ? 0 : 1] &= ~(1L << removeIndex);
//            }
//        } else {
//            // Add the destination square to the controlled squares
//            controlledSquares[team == Team.WHITE ? 0 : 1] |= (1L << destinationIndex);
//        }
//
//        for (Move pseudoLegalMove : pseudoLegalMoves) {
//            int moveDestinationIndex = board.getArrayIndexFromLocation(pseudoLegalMove.getTo());
//            controlledSquares[team == Team.WHITE ? 0 : 1] |= (1L << moveDestinationIndex);
//        }
//
//        // If the move is a capture, remove the captured square from the controlled squares of the enemy team
//        if (move.getCapturedPiece() != null) {
//            controlledSquares[team == Team.WHITE ? 1 : 0] &= ~(1L << destinationIndex);
//        }
//    }
}