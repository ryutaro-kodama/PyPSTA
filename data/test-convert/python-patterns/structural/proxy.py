from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
from typing import Union

class Subject:
    """
    As mentioned in the document, interfaces of both RealSubject and Proxy should
    be the same, because the client should be able to use RealSubject or Proxy with
    no code change.

    Not all times this interface is necessary. The point is the client should be
    able to use RealSubject or Proxy interchangeably with no change in code.
    """

    def do_the_job(self, user: str) -> None:
        raise NotImplementedError()

class RealSubject(Subject):
    """
    This is the main job doer. External services like payment gateways can be a
    good example.
    """

    def do_the_job(self, user: str) -> None:
        print(f'I am doing the job for {user}')

class Proxy(Subject):

    def __init__(self) -> None:
        self._real_subject = RealSubject()

    def do_the_job(self, user: str) -> None:
        """
        logging and controlling access are some examples of proxy usages.
        """
        print(f'[log] Doing the job for {user} is requested.')
        if user == 'admin':
            self._real_subject.do_the_job(user)
        else:
            print('[log] I can do the job just for `admins`.')

def client(job_doer: Union[RealSubject, Proxy], user: str) -> None:
    job_doer.do_the_job(user)

def main():
    proxy = Proxy()
    real_subject = RealSubject()
    client(proxy, 'admin')
    client(proxy, 'anonymous')
    client(real_subject, 'admin')
    client(real_subject, 'anonymous')
if __name__ == '__main__':
    main()