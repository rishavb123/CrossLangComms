import socket


class Accessor:

    SPACE_REPLACEMENT = "~`\t"

    def __init__(self, host="localhost", port=8000) -> None:
        self.host = host
        self.port = port

        self.init_connection()

    def init_connection(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.connect((self.host, self.port))

    def __send(self, s):
        self.socket.sendall(bytes((s + "\n"), "utf-8"))

    def __receive(self):
        return self.socket.recv(1024).decode("utf-8")[:-1]

    def get(self, key):
        self.__send(f"GET {key}")
        resp = self.__receive()
        if resp[0] == "P":
            _, t, val = resp.split(" ")
            return Accessor.resolve_type(t, val)
        raise Accessor.KeyException(resp[2:])

    def put(self, key, obj):
        t, val = Accessor.resolve_object(obj)
        self.__send(f"PUT {key} {t} {val}")
        resp = self.__receive()
        if resp[0] != "P":
            raise Accessor.TypeException(resp[2:])

    def set(self, key, obj):
        _, val = Accessor.resolve_object(obj)
        self.__send(f"SET {key} {val}")
        resp = self.__receive()
        if resp[0] != "P":
            raise Accessor.ServerException(
                "Something went wrong with the server as this command should never fail."
            )

    def update(self, key, obj):
        _, val = Accessor.resolve_object(obj)
        self.__send(f"UPDATE {key} {val}")
        resp = self.__receive()
        if resp[0] != "P":
            arr = resp.split(" ")
            if arr[1] == "key":
                raise Accessor.KeyException(resp[2:])
            elif arr[1] == "value":
                raise Accessor.TypeException(resp[2:])

    def delete(self, key):
        self.__send(f"DELETE {key}")
        resp = self.__receive()
        if resp[0] != "P":
            raise Accessor.KeyException(resp[2:])

    def doc(self, command):
        self.__send(f"DOC {command}")
        resp = self.__receive()
        if resp[0] != "P":
            raise Accessor.ServerException(resp[2:])
        return resp[2:]

    def keys(self):
        self.__send("KEYS")
        resp = self.__receive()
        if resp[0] != "P":
            raise Accessor.ServerException(
                "Something went wrong with the server as this command should never fail."
            )
        return resp[3:-1].split(", ")

    def clear(self):
        self.__send("CLEAR")
        resp = self.__receive()
        if resp[0] != "P":
            raise Accessor.ServerException(resp[2:])

    def __getitem__(self, key):
        return self.get(key)

    def __setitem__(self, key, val):
        self.set(key, val)

    def __str__(self):
        self.__send("DISPLAY")
        resp = self.__receive()
        if resp[0] != "P":
            raise Accessor.ServerException(
                "Something went wrong with the server as this command should never fail."
            )
        return resp[2:].replace(Accessor.SPACE_REPLACEMENT, " ")

    @staticmethod
    def resolve_object(obj):
        t = type(obj).__name__
        val = str(obj)
        if t == "int":
            if abs(obj) < 2147483648 / 10:
                return "integer", val
            else:
                return "long", val
        elif t == "float":
            return "double", val
        elif t == "str":
            return "string", val.replace(" ", Accessor.SPACE_REPLACEMENT)
        elif t == "bool":
            return "boolean", val.lower()
        return t, val

    @staticmethod
    def resolve_type(t, val):
        if t in {"int", "integer", "short", "long"}:
            return int(val)
        elif t in {"float", "double"}:
            return float(val)
        elif t in {"bool", "boolean"}:
            return val.title() == "True"
        elif t in {"str", "string", "chr", "char"}:
            return val.replace(Accessor.SPACE_REPLACEMENT, " ")
        return val

    def close(self):
        self.__send("EXIT")
        self.__receive()
        self.socket.shutdown(socket.SHUT_RDWR)
        self.socket.close()

    class ServerException(Exception):
        pass

    class KeyException(Exception):
        pass

    class TypeException(Exception):
        pass


if __name__ == "__main__":
    import sys

    port = 8000 if len(sys.argv) == 1 else int(sys.argv[1])

    accessor = Accessor(port=port)

    x = accessor.get("x")
    x += 1
    accessor.update("x", x)

    accessor.put("hi", 1.2)
    accessor.put("test", True)
    accessor.put("stringTest", "hisdflksdjf")
    accessor.put("stringTest2", "SPACE HERE")

    print(accessor.get("stringTest2"))
    print(accessor)

    accessor.delete("stringTest2")

    print(accessor.doc("doc"))

    accessor["y"] = 2.5
    print(accessor["y"])

    for key in accessor.keys():
        print(key + " ", end="")
    print()

    accessor.close()