import {Notification} from "@hilla/react-components/Notification.js";
import {ChessEndpoint} from "Frontend/generated/endpoints.js";
import React, {useEffect, useMemo, useState} from "react";
import {Chessboard} from "react-chessboard";
import styles from "../themes/sightlessknight/views/play-view.module.css"
import Move from "Frontend/generated/lukas/sobotik/sightlessknight/gamelogic/Move";
import PieceType from "Frontend/generated/lukas/sobotik/sightlessknight/gamelogic/entity/PieceType";
import GameInfoSideBar from "Frontend/themes/components/GameInfoSideBar";
import {useNavigate, useParams} from "react-router-dom";

function GameView({train} : {train?: boolean}) {
    const [fetchedData, setFetchedData] = useState<boolean>(false);
    const [currentFen, setCurrentFen] = useState("");
    const [validMoves, setValidMoves] = useState<Move[]>([]);
    const [gameEnded, setGameEnded] = useState("");
    const [moveHistory, setMoveHistory] = useState<string[]>([]);
    const [targetSquare, setTargetSquare] = useState<string>("");
    const [showBoard, setShowBoard] = useState<boolean>(true);
    const [showPieces, setShowPieces] = useState<boolean>(true);
    const [boardOrientation, setBoardOrientation] = useState<"white" | "black">("white");
    let { id } = useParams<{ id: string }>();
    let navigate = useNavigate();

    useEffect(() => {
        if (!train) return;
        if (id !== "king" && id !== "rook" && id !== "knight" && id !== "bishop" && id !== "queen") {
            id = null;
            navigate("/train");
        }
    }, [id]);

    useEffect(() => {
        const fetchData = async () => {
            await ChessEndpoint.initializeBoard(id ? id : null);
            await afterMoveActions();
            localStorage.getItem('showBoard') ? setShowBoard(localStorage.getItem('showBoard') == "true") : setShowBoard(true);
            localStorage.getItem('showPieces') ? setShowPieces(localStorage.getItem('showPieces') == "true") : setShowPieces(true);
        };
        fetchData().then(() => {
            console.info("Game initialized successfully.");
            setFetchedData(true);
        });
    }, []);

    async function afterMoveActions() {
        setCurrentFen(await ChessEndpoint.getCurrentPosition());
        setValidMoves(await ChessEndpoint.getValidMovesForPosition());

        // Update move history
        setMoveHistory(await ChessEndpoint.getMoveHistory());
        await showTargetSquare();
    }

    async function playMove(move: Move | string) {
        if (typeof move === 'string') {
            await ChessEndpoint.playMoveFromText(move);
        } else {
            await ChessEndpoint.playMove(move);
        }
        await afterMoveActions();
    }

    async function showTargetSquare() {
        const startSquare = await ChessEndpoint.getStartSquare();
        const targetSquare = await ChessEndpoint.getTargetSquare();
        if (startSquare == null || targetSquare == null) {
            return;
        }
        setTargetSquare(startSquare?.algebraicNotationLocation + " → " + targetSquare?.algebraicNotationLocation);
    }

    async function undoMove() {
        console.info("Undoing move...", currentFen);
        await ChessEndpoint.undoMove();
        await afterMoveActions();
    }

    async function resetGame() {
        await ChessEndpoint.resetGame();
        await afterMoveActions();
        setGameEnded("");
    }

    function findValidMove(move : any) {
        for (let validMove of validMoves) {
            if (validMove.from.algebraicNotationLocation == move.from && validMove.to.algebraicNotationLocation == move.to) {
                if (validMove.to.y == 0 && validMove.movedPiece.type == "PAWN" || validMove.to.y == 7 && validMove.movedPiece.type == "PAWN") {
                    console.log("Promotion move", getPromotionPiece(move.promotion));
                    validMove.promotionPiece = getPromotionPiece(move.promotion);
                }
                return validMove;
            }
        }
        console.info("Move is not valid in list of ", validMoves, " for move ", move);
        return null;
    }

    function getPromotionPiece(piece : string) : PieceType {
        switch (piece) {
            case "q":
                return PieceType.QUEEN;
            case "r":
                return PieceType.ROOK;
            case "b":
                return PieceType.BISHOP;
            case "n":
                return PieceType.KNIGHT;
            default:
                return PieceType.QUEEN;
        }
    }

    function onDrop(sourceSquare : any, targetSquare : any, piece : any) {
        if (gameEnded != "" && gameEnded != null) {
            return false;
        }

        const move = {
            from: sourceSquare,
            to: targetSquare,
            promotion: piece[1].toLowerCase() ?? "q"
        }

        const validMove = findValidMove(move);
        if (validMove != null) {
            playMove(validMove).then(() => {
                ChessEndpoint.checkIfGameEnded().then((gameEnded) => {
                    if (gameEnded != "" && gameEnded != null) {
                        console.info("Game ended by ", gameEnded);
                        Notification.show("Game ended by " + gameEnded);
                        setGameEnded(gameEnded);
                        return;
                    }
                });
            }).catch((e) => {
                console.error("Error playing move ", e);
            });
            return true;
        } else {
            return false;
        }
    }

    const pieces = [
        "wP",
        "wN",
        "wB",
        "wR",
        "wQ",
        "wK",
        "bP",
        "bN",
        "bB",
        "bR",
        "bQ",
        "bK",
    ];

    const customPieces = useMemo(() => {
        const pieceComponents = {};
        pieces.forEach((piece) => {
            pieceComponents[piece] = ({ squareWidth }) => (
                <div
                    style={{
                        width: squareWidth,
                        height: squareWidth,
                        backgroundImage: `url(/${piece}.png)`,
                        backgroundSize: "100%",
                    }}
                />
            );
        });
        return pieceComponents;
    }, []);

    if (!fetchedData) {
        return (
            <div>Loading...</div>
        );
    }

    return (
        <>
            <div className={styles.game_container}>
                <div className={styles.board_parent}>
                    <div id="board" className={showBoard ? styles.board : styles.board_hidden}>
                        <Chessboard id="MainBoard" arePremovesAllowed={!id} boardOrientation={boardOrientation}
                                    position={currentFen} onPieceDrop={onDrop}
                                    customPieces={showPieces ? null : customPieces}/>
                    </div>
                </div>
                <div className={showBoard ? styles.game_info : styles.game_info_extended}>
                    <GameInfoSideBar
                        moveHistory={moveHistory}
                        onResetGame={resetGame}
                        onUndo={undoMove}
                        onPlayFromText={playMove}
                        onShowBoardChange={(show) => setShowBoard(show)}
                        onShowPiecesChange={(show) => setShowPieces(show)}
                        targetSquare={targetSquare} />
                </div>
            </div>
        </>
    );
}

export default GameView;