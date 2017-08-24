import inspect
import os


def data_file_provider(data):

    if not all(isinstance(file_path, str) for file_path in data):
        raise Exception("Need a list of strings as data...")

    def test_decorator(fn):
        def test_decorated(self, *args):
            fn_path = inspect.getsourcefile(fn)
            for file_path in data:
                data_file_path = os.path.join(
                    os.path.dirname(fn_path), file_path)
                try:
                    with open(data_file_path, "r") as f:
                        fn(self, f.read())
                except AssertionError as e:
                    raise AssertionError(
                        "{} (data set used: {} in test {})".format(
                            e, file_path, fn.__name__))
        return test_decorated
    return test_decorator
