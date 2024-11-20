import {Button} from "@hilla/react-components/Button";
import styles from "Frontend/themes/sightlessknight/components/quick-settings.module.css";
import {Checkbox} from "@hilla/react-components/Checkbox";
import {Dialog} from "@hilla/react-components/Dialog";
import React, {useEffect, useState} from "react";
import RangeSlider from 'react-range-slider-input';
import 'react-range-slider-input/dist/style.css';

export default function QuickSettingsDialog({isOpen, onOpenChanged, showBoardChange, showPiecesChange} : {isOpen: boolean, onOpenChanged: (isOpen: boolean) => void, showBoardChange: (show: boolean) => void, showPiecesChange: (show: boolean) => void}) {
    const [opened, setOpened] = useState<boolean>(false);
    const [fetchedData, setFetchedData] = useState<boolean>(false);
    const [showBoard, setShowBoard] = useState<boolean>(true);
    const [showPieces, setShowPieces] = useState<boolean>(true);
    const [boardWidth, setBoardWidth] = useState<number>(100);

    useEffect(() => {
        setOpened(isOpen);
    }, [isOpen]);

    useEffect(() => {
        localStorage.getItem('boardSize') ? setBoardWidth(parseFloat(localStorage.getItem('boardSize'))) : setBoardWidth(100);
        localStorage.getItem('showBoard') ? setShowBoard(localStorage.getItem('showBoard') == "true") : setShowBoard(true);
        localStorage.getItem('showPieces') ? setShowPieces(localStorage.getItem('showPieces') == "true") : setShowPieces(true);
        setFetchedData(true);
    }, []);



    const handleOpenChange = (value : boolean) => {
        setOpened(value);
        onOpenChanged(value);
    };

    useEffect(() => {
        if (boardWidth === 0 || !fetchedData) {
            return;
        }
        const boardLayout : HTMLElement = document.getElementById('board');

        if (!boardLayout) return;

        const getNumericValue = (value: string) => {
            if (!value) return 0;
            const matchResult = value.match(/(\d+(\.\d+)?)|(\.\d+)/);
            return matchResult ? parseFloat(matchResult[0]) : 0;
        }
        const minAllowedWidth = getNumericValue(getComputedStyle(boardLayout).getPropertyValue('min-width'));
        const maxAllowedWidth = getNumericValue(getComputedStyle(boardLayout).getPropertyValue('max-width'));

        const w = boardWidth;

        const mappedWidth = (minAllowedWidth + (w * 0.01 * (maxAllowedWidth - minAllowedWidth))).toFixed(2) + 'px';
        localStorage.setItem('boardSize', w.toString());
        boardLayout.style.width = mappedWidth;
    }, [boardWidth]);

    function toggleShowBoard() {
        const show = !showBoard;
        setShowBoard(show);
        localStorage.setItem('showBoard', show.toString());
        showBoardChange(show);
    }

    function toggleShowPieces() {
        const show = !showPieces;
        setShowPieces(show);
        localStorage.setItem('showPieces', show.toString());
        showPiecesChange(show);
    }

    return (
        <Dialog
            aria-label="Quick Settings"
            opened={opened}
            onOpenedChanged={(event) => {
                handleOpenChange(event.detail.value);
            }}
            header-title="Quick Settings"
            headerRenderer={() => (
                <Button theme="tertiary" style={{fontWeight: "bolder"}} onClick={() => handleOpenChange(false)}>
                    ×
                </Button>
            )}>
            <div className={styles.settings}>
                <div className={styles.group}>
                    <div className={styles.item}>
                        <Checkbox label="Show board" checked={showBoard} onCheckedChanged={toggleShowBoard}></Checkbox>
                    </div>
                    <div className={styles.item}>
                        <Checkbox label="Show pieces" checked={showPieces} disabled={!showBoard}
                                  onCheckedChanged={toggleShowPieces}></Checkbox>
                    </div>
                </div>
                <div className={styles.group}>
                    <div className={styles.item}>
                        <label>Board size:</label>
                    </div>
                    <div className={styles.item}>
                        <RangeSlider
                            className={styles.single_thumb}
                            defaultValue={[0, boardWidth]}
                            thumbsDisabled={[true, false]}
                            rangeSlideDisabled={true}
                            disabled={!showBoard}
                            onInput={(e) => {
                                setBoardWidth(e[1]);
                            }}
                        />
                    </div>
                </div>
            </div>
        </Dialog>
    )
}