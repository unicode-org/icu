
mergeInto(LibraryManager.library, {
  system__deps: ['__setErrNo'],
  system: function(commandAddr) {
    const command = UTF8ToString(commandAddr)
    // int system(const char *command);
    // http://pubs.opengroup.org/onlinepubs/000095399/functions/system.html
    // Can't call external programs.
    try {
        const stdout = require('child_process').execSync(command);
    	console.log(`system:fine:${stdout}`);
        return 0;
    } catch (e) {
    	console.log(`system:failed:${command}:${e}`);
    	___setErrNo({{{ cDefine('EAGAIN') }}});
    	return -1;
    }

  },
});
