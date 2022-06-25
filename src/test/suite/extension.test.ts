import * as assert from 'assert';
import * as vscode from "vscode";
import { install } from '../../lombok-installer';
import { readFileSync, existsSync } from 'fs';
import { uninstall } from '../../lombok-uninstaller';
import { VM_ARGS_KEY, getUserSettingsPath, LOMBOK_PATH_KEY } from '../../util';

const { publisher, name } = require('../../../package.json');

suite("Extension Tests", function () {
    var originalVmArgs = vscode.workspace.getConfiguration().get<string>(VM_ARGS_KEY);
    var originalLombokPath = vscode.workspace.getConfiguration(name).get(LOMBOK_PATH_KEY);
    
    this.beforeEach(() => {
        uninstall();
    });

    this.afterEach(async () => {
        await vscode.workspace.getConfiguration().update(VM_ARGS_KEY, originalVmArgs, true);
        await vscode.workspace.getConfiguration().update(LOMBOK_PATH_KEY, originalLombokPath, true);
    })

    suite("that Lombok -javaagent is appended to the VM arguments", () => {
        const builtInJarPath = vscode.extensions.getExtension(publisher + '.' + name)?.extensionPath?.replaceAll("\\", "/") + "/server/lombok.jar";

        const tests = [
            { lombokPath: undefined, expectedJarPath: builtInJarPath },
            { lombokPath: "", expectedJarPath: builtInJarPath },
            { lombokPath: " ", expectedJarPath: builtInJarPath },
            { lombokPath: "path/to/my/lombok.jar", expectedJarPath: "path/to/my/lombok.jar" }
        ];
        
        tests.forEach(({ lombokPath, expectedJarPath }) => {
            test(`when lombokPath is ${lombokPath}`, async () => {
                await vscode.workspace.getConfiguration().update(LOMBOK_PATH_KEY, lombokPath, true);

                const javaAgentArg = `-javaagent:"${expectedJarPath}"`;

                await install();

                const vmArgs: string | undefined = vscode.workspace.getConfiguration().get<string>(VM_ARGS_KEY)?.replaceAll("\\", "/");

                if (vmArgs) {
                    assert(vmArgs.includes(javaAgentArg), `${vmArgs} does not include ${javaAgentArg}`);
                } else {
                    assert.fail();
                }
            })
        })
    });

    const userSettingsPath = getUserSettingsPath(process.platform);

    if (existsSync(userSettingsPath)) {
        test("that Lombok -javaagent is removed from the VM arguments", async () => {
            const settings = JSON.parse(readFileSync(userSettingsPath, 'utf8'));
            const vmArgs: string = settings[VM_ARGS_KEY];

            if (vmArgs) {
                assert.equal(vmArgs.match(/-javaagent:".*"/), null);
            }
        });
    }
});