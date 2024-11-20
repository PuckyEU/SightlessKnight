import styles from "Frontend/themes/sightlessknight/components/commandline.module.css";
import {useEffect, useState} from "react";

export default function CommandLineTooltip({command, hideTooltip} : {command : string, hideTooltip: boolean}){
    const [commands, setCommands] = useState<string[]>([]);

    useEffect(() => {
        console.log("Command line tooltip initialized.", command);
    }, []);

    useEffect(() => {
        setCommands(predictCommand(command));
    }, [command]);

    function isCommandValid(command: string, userCommand: string): boolean {
        const placeholderRegex = /<.*?>/g;
        const userValue = userCommand.split(' ').pop();
        if (!command.includes("<") && !command.includes(">")) return false;

        const formattedCommand = command.replace(placeholderRegex, userValue);
        return formattedCommand === userCommand;
    }


    function predictCommand(inputCommand: string): string[] {
        const predictions = [];

        // Check for commands starting with "/"s
        if (inputCommand.startsWith("/")) {
            const matchedCommands = Commands.filter((cmd) => {
                if (isCommandValid(cmd, inputCommand)) return true;
                return cmd.startsWith(inputCommand);
            }
            );
            predictions.push(...matchedCommands);
        }

        if (predictions.length === 0) {
            predictions.push("No commands found.");
        }

        return predictions;
    }

    return (
        hideTooltip && <div className={styles.tooltip}>
            <div className={styles.tooltip_content}>
                <p>Commands:</p>
                {commands.map((cmd, i) => <div key={i}>{cmd}</div>)}
            </div>
        </div>
    )
}

export const Commands = [
    "/undo",
    "/perft <depth>",
]