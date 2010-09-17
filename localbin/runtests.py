#!/usr/bin/env python
#
#  
#
# To run the script, execute: 
#	 python runtests.py  --scj-path ./build/src/:./build/spec/
#	 python runtests.py  --scj-path ./build/src/:/Users/plsek/_work/workspace_RT/Spec-SRC/bin
#
#   python runtests.py  --scj-path ./build/src/:/Users/plsek/_work/workspace_RT/scj-current/bin


import re
import sys

JAVAC='./localbin/checkers/binary/javac'

TEST_DIR = 'tests'
RUN_CMD = JAVAC + ' -proc:only -cp testsuite/build:lib/scjChecker.jar:%s: -processor checkers.SCJChecker %s'

def get_java_files(test_dir):
    import os
    import fnmatch
    for path, dirs, files in os.walk(os.path.abspath(test_dir)):
        if not 'javax' in path:
            for filename in fnmatch.filter(files, "*.java"):
                yield os.path.join(path, filename)

_ERROR_RE = re.compile(r'^[^:]+:\d+:\s+(.*)$')
def clean_output(lines):
    errors = []
    for line in lines:
        m = _ERROR_RE.match(line)
        if m:
            errors.append(m.group(1))
    return errors

def run_tests(test_dir):
    import commands
    import pprint
    all_errors = []
    success, failure = 0, 0
    for test in get_java_files(test_dir):
        header = []
        try:
            f = open(test, 'r')
            for line in f:
                if line.startswith('//'):
                    header.append(line[2:].lstrip())
                else:
                    break
        finally:
            f.close()

        expected_errors = clean_output(header)
        cmd = RUN_CMD % test
        #print "----------------------"
        #print "%s" % cmd
        errors = clean_output(commands.getoutput(cmd).split('\n'))
        if expected_errors != errors:
            all_errors.append([test, expected_errors, errors])
            failure += 1
            sys.stderr.write('E')
        else:
            success += 1
            sys.stderr.write('.')
    sys.stderr.write('\n')
    for error in all_errors:
        print 'Test case at %s failed:' % error[0]
        print 'Expected errors:'
        pprint.pprint(error[1])
        print 'Actual errors:'
        pprint.pprint(error[2])
        print '\n'
    
    
    print '%d tests total, %d failed, %d succeeded.' % (success + failure,
                                                            failure, success)

if __name__ == '__main__':
    import optparse
    parser = optparse.OptionParser()
    parser.add_option('-d', '--directory', help='specify root test directory',
                      action='store', type='string', dest='directory',
                      default=TEST_DIR)
    parser.add_option('-s', '--scj-path', help='specify path to SCJ classes',
                      action='store', type='string', dest='scj_path',
                      default='')
    options, args = parser.parse_args()
    if args:
        parser.parse_args(['-h'])

    RUN_CMD = RUN_CMD % (options.scj_path, '%s')
    #print 'Running tests with root directory %s' % test_dir
    run_tests(options.directory)
