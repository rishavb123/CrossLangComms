import net from 'net';

class Accessor {
    constructor(host="localhost", port=8000) {
        this.host = host;
        this.port = port;
        this.initConnection();
    }

    async initConnection() {
        this.socket = await this._initConnectionAsync();
        console.log("hi");
    }

    _initConnectionAsync() {
        const socket = new net.Socket();
        return new Promise((res, rej) => {
            socket.connect(this.port, this.host, () => res(socket));
            socket.on("error", err => rej(err));
        });
    }

    _send(s) {
        self.socket.write(s + "\n");
    }

    _receive() {

    }

    close() {
        
        console.log("bye");
        this.socket.destroy();
    }
}


export default Accessor;