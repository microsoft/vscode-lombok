import { getExtension } from './extension';

const { lombokConfig } = require('../package.json');

const lombokJar: string = getExtension().extensionPath + lombokConfig.path;

lombokConfig.vmArgsValue = lombokConfig.vmArgsValue.replace(new RegExp("{lombok-jar}", 'g'), lombokJar);

export default lombokConfig;