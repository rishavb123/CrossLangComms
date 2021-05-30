import socket

class Accessor:

    def __init__(self, host="localhost", port=8000) -> None:
        self.host = host
        self.port = port

        self.init_connection()

    def init_connection(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.connect((self.host, self.port))

    def __send(self, s):
        self.socket.sendall(bytes(s, 'utf-8'))

    def __receive(self):
        return repr(self.socket.recv(1024))

    def close(self):
        self.socket.shutdown(socket.SHUT_RDWR)
        self.socket.close()

if __name__ == "__main__":
    accessor = Accessor()
    accessor.close()