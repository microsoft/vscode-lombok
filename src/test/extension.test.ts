import * as assert from 'assert';
import * as vscode from "vscode";
import { install } from '../lombok-installer';
import { getJarPath } from '../util';
import { readFileSync, existsSync } from 'fs';
import { uninstall, getUserSettingsPath } from '../lombok-uninstaller';

suite("Extension Tests", function () {

    uninstall();

    const javaAgentArg = `-javaagent:"${getJarPath()}"`;

    test("that Lombok -javaagent is appended to the VM arguments", async function () {
        await install();

        const vmArgs: string | undefined = vscode.workspace.getConfiguration().get("java.jdt.ls.vmargs");

        if (vmArgs) {
            assert.equal(vmArgs.includes(javaAgentArg), true);
        } else {
            assert.fail();
        }
    });

    const userSettingsPath = getUserSettingsPath(process.platform);

    if (userSettingsPath && existsSync(userSettingsPath)) {
        test("that Lombok -javaagent is removed from the VM arguments", async function () {
            uninstall();

            const settings = JSON.parse(readFileSync(userSettingsPath, 'utf8'));
            const vmArgs: string = settings["java.jdt.ls.vmargs"];

            if (vmArgs) {
                assert.equal(vmArgs.match(/-javaagent:".*"/), null);
            }
        });
    }
});