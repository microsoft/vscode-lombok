//
// Note: This example test is leveraging the Mocha test framework.
// Please refer to their documentation on https://mochajs.org/ for help.
//

// The module 'assert' provides assertion methods from node
import * as assert from 'assert';
import { setLombokToVSCode, cleanLombok } from '../extension';
import lombokConfig from '../lombok-config';

// Defines a Mocha test suite to group tests of similar kind together
suite("Extension Tests", function () {

    // Defines a Mocha unit test
    test("Test that Lombok Jar is sucessfully added to VM Settings", async function() {
        assert.equal(true, await setLombokToVSCode(lombokConfig));
    });

    test("Test that Lombok Jar is sucessfully removed from VM Settings", async function() {
        assert.equal(true, await cleanLombok(lombokConfig));
    });
});