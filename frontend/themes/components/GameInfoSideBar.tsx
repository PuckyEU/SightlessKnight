import styles from "Frontend/themes/sightlessknight/components/game-sidebar.module.css";
import CommandLine from "Frontend/themes/components/CommandLine";
import {Button} from "@hilla/react-components/Button";
import {ChessEndpoint} from "Frontend/generated/endpoints";
import React, {useState} from "react";
import {Notification} from "@hilla/react-components/Notification";
import QuickSettingsDialog from "Frontend/themes/components/QuickSettingsDialog";


export default function GameInfoSideBar({moveHistory, onResetGame, onUndo, onPlayFromText, onShowBoardChange, onShowPiecesChange, targetSquare}: {
    moveHistory: string[],
    onResetGame: () => void,
    onUndo: () => void,
    onPlayFromText: (move: string) => void
    onShowBoardChange: (show: boolean) => void,
    onShowPiecesChange: (show: boolean) => void,
    targetSquare: string
}) {
    const [settingsDialogOpened, setSettingsDialogOpened] = useState<boolean>(false);

    async function onCommandSubmit(command : string) {
        if (command.startsWith("/")) {
            console.log("Command is a command");
            if (command == "/undo") {
                onUndo();
            } else if (command.startsWith("/perft")) {
                const depth = parseInt(command.split(" ")[1]);
                if (depth > 4) {
                    console.warn("Perft depth is too high.");
                    Notification.show("Perft depth is too high.");
                    return;
                }
                const result = await ChessEndpoint.playPerftTest(depth);
                console.info("Perft result: ", result);
                Notification.show("Perft result: " + result);
            }
        } else {
            onPlayFromText(command);
        }
    }

    return (
        <>
            {targetSquare ? <div className={styles.target_square}>
                {targetSquare}
            </div> : null}
            <div className={styles.move_history}>
                <div className={styles.moves}>
                    <div className={styles.white_moves}>
                        {moveHistory.map((move, index) => {
                            const moveNumber = Math.floor(index / 2) + 1;
                            if (index % 2 == 0) {
                                return (<div id={moveNumber.toString()} key={index}
                                             className={styles.move}>{moveNumber}: {move}</div>)
                            } else return null;
                        })}
                    </div>
                    <div className={styles.black_moves}>
                        {moveHistory.map((move, index) => {
                            if (index % 2 != 0) {
                                return (<div key={index} className={styles.move}>{move}</div>)
                            } else return null;
                        })}
                    </div>
                </div>
            </div>
            <div className={styles.game_actions}>
                <div className={styles.command_line}>
                    <CommandLine onCommandSubmit={onCommandSubmit}/>
                </div>
                <div className={styles.game_actions_buttons}>
                    <div className={styles.game_actions_group}>
                        <Button className={styles.button} onClick={async () => {
                            onResetGame();
                        }}>Reset</Button>
                        <Button className={styles.button} onClick={async () => {
                            onUndo();
                        }}>Undo</Button>
                    </div>
                    <Button className={styles.button} onClick={async () => {
                        setSettingsDialogOpened(true);
                    }}>Settings</Button>
                </div>
            </div>
            <QuickSettingsDialog
                isOpen={settingsDialogOpened}
                onOpenChanged={(isOpen) => setSettingsDialogOpened(isOpen)}
                showBoardChange={onShowBoardChange}
                showPiecesChange={onShowPiecesChange}/>
        </>
    )
}