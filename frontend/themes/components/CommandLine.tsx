import {TextField} from "@hilla/react-components/TextField";
import {Button} from "@hilla/react-components/Button";
import {useEffect, useState} from "react";
import styles from "Frontend/themes/sightlessknight/components/commandline.module.css";
import CommandLineTooltip from "Frontend/themes/components/CommandLineTooltip";

export default function CommandLine({onCommandSubmit} : {onCommandSubmit: (command: string) => void}) {
    const [command, setCommand] = useState<string>("");
    const [tooltipVisible, setTooltipVisible] = useState<boolean>(false);

    useEffect(() => {
        checkTooltipVisibility();
    }, [command]);

    function submitCommand() {
        if (command.length == 0) return;
        onCommandSubmit(command);
    }

    function toggleTooltip() {
        setTooltipVisible(!tooltipVisible);
    }

    function checkTooltipVisibility() {
        if (command.length != 0 && command.startsWith("/")) {
            setTooltipVisible(true);
        } else {
            setTooltipVisible(false);
        }
    }

    return (
        <div className={styles.layout}>
            <CommandLineTooltip command={command} hideTooltip={tooltipVisible}/>
            <div className={styles.parent}>
                <Button
                    className={styles.command_line_tooltip_button}
                    onClick={toggleTooltip}>?</Button>
                <TextField
                    className={styles.command_line}
                    placeholder="Enter a move or /command"
                    onFocus={() => {checkTooltipVisibility()}}
                    onBlur={() => {setTooltipVisible(false)}}
                    onValueChanged={(e) => {
                        setCommand(e.detail.value);
                    }}/>
                <Button
                    className={styles.button}
                    onClick={async () => {
                        submitCommand();
                    }}>
                    ⊳
                </Button>
            </div>
        </div>
    )
}