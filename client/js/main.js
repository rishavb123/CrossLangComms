import Accessor from './accessor.mjs';

let port = process.argv.length > 2 ? parseInt(process.argv[2]) : 8000;

const accessor = new Accessor(port=port);

accessor.close();