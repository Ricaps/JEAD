import unittest

def run_unittests(test_path=".", pattern="test_*.py", verbosity=2):
    """
    Discover and run unittests in the given directory.

    Args:
        test_path (str): Folder containing test files.
        pattern (str): Glob pattern to match test files.
        verbosity (int): Verbosity level of the test runner.
    """
    loader = unittest.TestLoader()
    suite = loader.discover(start_dir=test_path, pattern=pattern)

    runner = unittest.TextTestRunner(verbosity=verbosity)
    return runner.run(suite)



if __name__ == "__main__":
    result = run_unittests()
    if result.wasSuccessful():
        print("All tests passed!")
    else:
        print("Some tests failed.")
        exit(len(result.failures))