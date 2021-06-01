import net from 'net';

class Accessor {

    static SPACE_REPLACEMENT = "~`\t";

    constructor(host="localhost", port=8000) {
        this.host = host;
        this.port = port;
        this._receiveListeners = [];
        const _this = this;
        // this.obj = new Proxy({}, {
        //     get: async (proxy, key) => await _this.get(key),
        //     set: async (proxy, key, value) => {
        //         return await _this.set(key, value) === undefined;
        //     }
        // });
        return this.initConnection();
    }

    async initConnection() {
        const socket = new net.Socket();
        const _this = this;
        this.socket = await new Promise((res, rej) => {
            socket.connect(this.port, this.host, () => {
                socket.on("data", (data) => {
                    let str = String.fromCharCode(...data)
                    str = str.substring(0, str.length - 1);
                    for (const listener of _this._receiveListeners)
                        listener(str);
                    _this._receiveListeners = [];
                });
                res(socket);
            });
            socket.on("error", err => rej(err));
        });;
        return this;
    }

    _send(s) {
        this.socket.write(s + "\n");
    }

    async _receive() {
        const _this = this;
        return await new Promise((res, rej) => {
            _this._receiveListeners.push(res);
        });
    }

    async get(key) {
        this._send(`GET ${key}`);
        const resp = await this._receive();
        if (resp[0] === "P") {
            const [_, t, val] = resp.split(" ");
            return Accessor.resolveType(t, val);
        }
        throw new Accessor.KeyException(resp.substring(2));
    }

    async put(key, obj) {
        const [t, val] = Accessor.resolveObject(obj);
        this._send(`PUT ${key} ${t} ${val}`);
        const resp = await this._receive();
        if (resp[0] !== "P")
            throw new Accessor.TypeException(resp.substring(2));
    }

    async set(key, obj) {
        const [_, val] = Accessor.resolveObject(obj);
        this._send(`SET ${key} ${val}`);
        const resp = await this._receive();
        if (resp[0] !== "P")
            throw new Accessor.ServerException("Something went wrong with the server as this command should never fail.");
    }

    async update(key, obj) {
        const [_, val] = Accessor.resolveObject(obj);
        this._send(`UPDATE ${key} ${val}`);
        const resp = await this._receive();
        if (resp[0] !== "P") {
            const arr = resp.split(" ");
            if (arr[1] === "key") {
                throw new Accessor.KeyException(resp.substring(2));
            } else {
                throw new Accessor.TypeException(resp.substring(2));
            }
        }
    }

    async delete(key) {
        this._send(`DELETE ${key}`);
        const resp = await this._receive();
        if (resp[0] !== "P")
            throw new Accessor.KeyException(resp.substring(2));
    }

    async doc(command) {
        this._send(`DOC ${command}`);
        const resp = await this._receive();
        if (resp[0] !== "P")
            throw new Accessor.ServerException(resp.substring(2));
        return resp.substring(2);
    }

    async keys() {
        this._send("KEYS");
        const resp = await this._receive();
        if (resp[0] !== "P")
            throw new Accessor.ServerException("Something went wrong with the server as this command should never fail.");
        return resp.substring(3, resp.length - 1).split(", ");
    }

    async clear() {
        this._send("CLEAR");
        const resp = await this._receive();
        if (resp[0] !== "P")
            throw new Accessor.ServerException(resp.substring(2));
    }

    async getString() {
        this._send("DISPLAY");
        const resp = await this._receive();
        if (resp[0] !== "P")
            throw new Accessor.ServerException("Something went wrong with the server as this command should never fail.");
        return resp.substring(2).replaceAll(Accessor.SPACE_REPLACEMENT, " ")
    }

    static resolveObject(obj) {
        const t = typeof obj;
        const val = obj.toString();
        switch (t) {
            case "number":
                if (obj % 1 == 0) {
                    if (Math.abs(obj) < 2147483648 / 10)
                        return ["integer", val];
                    else
                        return ["long", val];
                } else
                    return ["double", val];
            case "string":
                return [t, val.replaceAll(" ", Accessor.SPACE_REPLACEMENT)];
            case "boolean":
                return [t, val];
            default:
                return [t, val];
        }
    }

    static resolveType(t, val) {
        switch (t) {
            case "int":
            case "integer":
            case "short":
            case "long":
                return parseInt(val);
            case "float":
            case "double":
                return parseFloat(val);
            case "bool":
            case "boolean":
                return val.toLowerCase() === "true";
            case "str":
            case "string":
            case "chr":
            case "char":
                return val.replaceAll(Accessor.SPACE_REPLACEMENT, " ");
        }
        return val;
    }        

    close() {
        this._send("EXIT");
        this.socket.destroy();
    }

    static ServerException = class extends Error {
        constructor(...params) {
            super(...params);
        }
    };

    static KeyException = class extends Accessor.ServerException {
        constructor(...params) {
            super(...params);
        }
    };

    static TypeException = class extends Accessor.ServerException {
        constructor(...params) {
            super(...params);
        }
    };
}


export default Accessor;