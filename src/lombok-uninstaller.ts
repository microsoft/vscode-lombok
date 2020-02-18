import { readFileSync, writeFileSync, existsSync } from "fs";
import * as jsonic from 'jsonic';
import { VM_ARGS_KEY, getUserSettingsPath } from "./util";

export function uninstall(): void {
    const userSettingsPath: string = getUserSettingsPath(process.platform);

    if (existsSync(userSettingsPath)) {
        const settings = jsonic(readFileSync(userSettingsPath, 'utf8'));
        const vmArgs: string = settings[VM_ARGS_KEY];

        if (vmArgs && vmArgs.match(/-javaagent:".*"/)) {
            const newVmArgs = vmArgs.replace(/-javaagent:".*"/, "").trim();
            settings[VM_ARGS_KEY] = newVmArgs === "" ? undefined : newVmArgs;

            writeFileSync(userSettingsPath, JSON.stringify(settings, null, 2), 'utf8');
        }
    }
}