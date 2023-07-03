package lukas.sobotik.sightlessknight;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;

public class Board {
    PieceInfo[] pieces;
    int size;
    int squareSize;
    Texture boardTexture;
    TextureRegion boardTextureRegion;
	TextureAtlas pieceAtlas;
    HashMap<String, Integer> spriteIndexMap;
    Array<Sprite> sprites;
    IntPoint2D whiteKing;
    IntPoint2D blackKing;
    IntPoint2D lastFrom;
    IntPoint2D lastTo;
    PieceInfo lastRemoved;
    IntPoint2D lastMovedDoubleWhitePawn;
    IntPoint2D lastMovedDoubleBlackPawn;
    FenUtils fenUtils;
    static final Team playerTeam = GameState.playerTeam;

    public Board(int size, TextureAtlas pieceAtlas) {
        this.size = size;
        squareSize = size / 8;
        this.pieceAtlas = pieceAtlas;
        sprites = pieceAtlas.createSprites();
        float spriteSize = (int) sprites.get(0).getHeight();
        float scale = ((float) squareSize) / spriteSize;

        for (Sprite sprite : sprites) {
            sprite.setScale(scale);
        }

        spriteIndexMap = new HashMap<>();

        int count = 0;
        for (PieceType type : PieceType.values()) {

            for (Team team : Team.values()) {
                String name = new PieceInfo(team, type).getSpriteName();
                spriteIndexMap.put(name, count++);
            }
        }

        if (playerTeam == Team.WHITE) {
            whiteKing = new IntPoint2D(4, 0);
            blackKing = new IntPoint2D(4, 7);
        } else {
            whiteKing = new IntPoint2D(3, 7);
            blackKing = new IntPoint2D(3, 0);
        }

        fenUtils = new FenUtils(pieces, whiteKing, blackKing, lastTo, lastMovedDoubleWhitePawn, lastMovedDoubleBlackPawn);
        pieces = fenUtils.generatePositionFromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        System.out.println(fenUtils.generateFenFromCurrentPosition());
        // Print the board in the console
        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                int index = rank * 8 + file;
                PieceInfo piece = pieces[index];

                if (piece == null) {
                    System.out.print(". "); // Empty square
                } else {
                    System.out.print(fenUtils.getSymbolFromPieceType(piece.type, piece.team) + " "); // Piece character
                }
            }
            System.out.println(); // Move to the next line for the next rank
        }

        generateTexture();
    }
    private void generateTexture() {
        int nextPow2 = Integer.highestOneBit(size - 1) << 1;

        Pixmap pixmap = new Pixmap(nextPow2, nextPow2, Format.RGBA8888);

        pixmap.setColor(Color.GRAY);
        pixmap.fillRectangle(0, 0, size, size);

        pixmap.setColor(Color.WHITE);

        int y = 0;
        int x = 0;
        for (int i = 0; i < 32; i++) {
            pixmap.fillRectangle(x * squareSize, y * squareSize, squareSize, squareSize);
            x += 2;
            if (x >= 8) {
                y++;
                x = 1 - x % 8;
            }
        }

        boardTexture = new Texture(pixmap);
        pixmap.dispose();
        boardTextureRegion = new TextureRegion(boardTexture, 0, 0, size, size);
    }

    public void draw(SpriteBatch batch) {
        batch.draw(boardTextureRegion, 0, 0);

        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                drawPiece(batch, rank, file);
            }
        }
    }
    private void drawPiece(SpriteBatch batch, int rank, int file) {
        int index = rank * 8 + file;
        PieceInfo info = pieces[index];

        if (info == null) {
            return;
        }

        String name = info.getSpriteName();

        Sprite sprite = sprites.get(spriteIndexMap.get(name));
        sprite.setX(file * squareSize + (float) squareSize / 2 - sprite.getWidth() / 2);
        sprite.setY(rank * squareSize + (float) squareSize / 2 - sprite.getHeight() / 2);
        sprite.draw(batch);
    }

    IntPoint2D getKing(Team team) {
        return (team == Team.WHITE) ? whiteKing : blackKing;
    }

    public PieceInfo getPiece(IntPoint2D location) {
        if (!isInBounds(location)) {
            return null;
        }
        return pieces[location.getX() + location.getY() * 8];
    }

    public void movePiece(IntPoint2D from, IntPoint2D to) {
        if (from == null || to == null) return;
        movePieceWithoutSpecialMoves(from, to);

        PieceInfo movedPiece = pieces[to.getX() + to.getY() * 8];
        if (movedPiece == null) return;

        // Move the rook when the king castles
        if (movedPiece.type == PieceType.KING && Math.abs(from.getX() - to.getX()) == 2) {
            // Queenside Castling
            if (from.getX() > to.getX()) {
                tryMovingPieces(movedPiece.team == Team.WHITE ? new IntPoint2D(0, 0) : new IntPoint2D(0, 7), movedPiece.team == Team.WHITE ? new IntPoint2D(3, 0) : new IntPoint2D(3, 7));
            } // Kingside Castling
            else {
                tryMovingPieces(movedPiece.team == Team.WHITE ? new IntPoint2D(7, 0) : new IntPoint2D(7, 7), movedPiece.team == Team.WHITE ? new IntPoint2D(5, 0) : new IntPoint2D(5, 7));
            }
        }

        // Check if the moved piece is a pawn and moved two squares
        if (movedPiece.type == PieceType.PAWN && Math.abs(from.getY() - to.getY()) == 2) {
            if (movedPiece.team == Team.WHITE) lastMovedDoubleWhitePawn = to;
            if (movedPiece.team == Team.BLACK) lastMovedDoubleBlackPawn = to;
            movedPiece.doublePawnMoveOnMoveNumber = GameState.moveNumber;
        }

        // Handle en passant capture
        IntPoint2D enPassantCapture = new IntPoint2D(to.getX(), from.getY());
        if (getPiece(enPassantCapture) != null && from.getX() != to.getX() && getPiece(enPassantCapture).doublePawnMoveOnMoveNumber == GameState.moveNumber - 1) {
            removePiece(enPassantCapture);
        }

        movedPiece.hasMoved = true;

        lastFrom = from;
        lastTo = to;
    }
    public void tryMovingPieces(IntPoint2D from, IntPoint2D to) {
        movePieceWithoutSpecialMoves(from, to);

        lastFrom = from;
        lastTo = to;
    }
    private void movePieceWithoutSpecialMoves(IntPoint2D from, IntPoint2D to) {
        if (from.equals(whiteKing)) {
            whiteKing = to;
        } else if (from.equals(blackKing)) {
            blackKing = to;
        }

        lastRemoved = pieces[to.getX() + to.getY() * 8];

        pieces[to.getX() + to.getY() * 8] = pieces[from.getX() + from.getY() * 8];
        pieces[from.getX() + from.getY() * 8] = null;
    }
    public void removePiece(IntPoint2D location) {
        if (!isInBounds(location)) {
            return;
        }
        pieces[location.getX() + location.getY() * 8] = null;
    }

    public void undoMove() {
        PieceInfo temp = lastRemoved;

        tryMovingPieces(lastTo, lastFrom);

        pieces[lastFrom.getX() + lastFrom.getY() * 8] = temp;
    }

    public IntPoint2D getPointFromArrayIndex(int index) {
        int x = index % 8;
        int y = index / 8;
        return new IntPoint2D(x, y);
    }

    public IntPoint2D getPoint(int x, int y) {
        return new IntPoint2D(x / squareSize, 7 - y / squareSize);
    }

    public IntRect getRectangle(IntPoint2D point) {
        return new IntRect(point.getX() * squareSize, (point.getY()) * squareSize, squareSize, squareSize);
    }

    public boolean isInBounds(IntPoint2D location) {
        return location.getX() < 8 && location.getX() >= 0 && location.getY() < 8 && location.getY() >= 0;
    }
}
