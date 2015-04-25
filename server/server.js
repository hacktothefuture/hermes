var http = require("http");

http.createServer( function(request, response) {
	response.writeHead(200, {"Content-Type": "text/plain"});
	response.write("Hello, World!");
	response.end();
}).listen(8888);

var MongoClient = require('mongodb').MongoClient;
var assert = require('assert');

var url = 'mongodb://localhost:27017/test';
MongoClient.connect(url, function(err, db) {
  assert.equal(null, err);
  console.log("Connected correctly to server.");
  db.close();
});