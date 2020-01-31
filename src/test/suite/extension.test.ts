import * as assert from 'assert';
import * as vscode from "vscode";
import { install, getJarPath } from '../../lombok-installer';
import { readFileSync, existsSync } from 'fs';
import { uninstall } from '../../lombok-uninstaller';
import { VM_ARGS_KEY, getUserSettingsPath } from '../../util';

suite("Extension Tests", function () {

    uninstall();

    const javaAgentArg = `-javaagent:"${getJarPath()}"`;

    test("that Lombok -javaagent is appended to the VM arguments", async function () {
        await install();

        const vmArgs: string | undefined = vscode.workspace.getConfiguration().get(VM_ARGS_KEY);

        if (vmArgs) {
            assert.equal(vmArgs.includes(javaAgentArg), true);
        } else {
            assert.fail();
        }
    });

    const userSettingsPath = getUserSettingsPath(process.platform);

    if (existsSync(userSettingsPath)) {
        test("that Lombok -javaagent is removed from the VM arguments", async function () {
            uninstall();

            const settings = JSON.parse(readFileSync(userSettingsPath, 'utf8'));
            const vmArgs: string = settings[VM_ARGS_KEY];

            if (vmArgs) {
                assert.equal(vmArgs.match(/-javaagent:".*"/), null);
            }
        });
    }
});