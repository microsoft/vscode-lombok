import { writeFileSync, readFileSync } from 'fs';

let settings;
switch (process.platform) {
    case 'win32':
        settings = process.env.APPDATA + '\\Code\\User\\settings.json';
        break;
    case 'darwin':
        settings = process.env.HOME + '/Library/Application Support/Code/User/settings.json';
        break;
    case 'linux':
        settings = process.env.HOME + '/.config/Code/User/settings.json';
        break;
    default:
        settings = null;
}

if (settings !== null) {
    const fileData = readFileSync(settings, 'utf8');
    const settingsJson = JSON.parse(fileData);

    const { lombokConfig } = require('../package.json');

    let vmArgsValue: string = settingsJson[lombokConfig.vmArgsKey];
    settingsJson[lombokConfig.vmArgsKey] = vmArgsValue.split('-javaagent:')[0].trim();

    writeFileSync(settings, JSON.stringify(settingsJson), 'utf8');
}