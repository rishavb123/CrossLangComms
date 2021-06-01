import Accessor from './accessor.mjs';

let port = process.argv.length > 2 ? parseInt(process.argv[2]) : 8000;

const accessor = await new Accessor("localhost", port);

let x = await accessor.get("x");
x += 1;
await accessor.update("x", x);

await accessor.put("hi", 1.2)
await accessor.put("test", true)
await accessor.put("stringTest", "hisdflksdjf")
await accessor.put("stringTest2", "SPACE HERE")

console.log(await accessor.get("stringTest2"));
console.log(await accessor.getString());

await accessor.delete("stringTest2");

console.log(await accessor.doc("doc"));

for (const key of await accessor.keys())
    process.stdout.write(key + " ",);
console.log();

accessor.close();