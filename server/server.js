var days_to_expiration = 7;
var http = require("http");

http.createServer( function(request, response) {
	response.writeHead(200, {"Content-Type": "text/plain"});
	response.write("Hello, World!");
	response.end();
}).listen(8888);

var MongoClient = require('mongodb').MongoClient;
var assert = require('assert');
var ObjectId = require('mongodb').ObjectID;
var url = 'mongodb://localhost:27017/test';

MongoClient.connect(url, function(err, db) {
  assert.equal(null, err);
  console.log("Connected correctly to server.");
  db.close();
});

var insertBoard = function(db, message_info, callback) {
	expiration_date = new Date(Date.UTC()+(86400 * 1000 * days_to_expiration));
	db.collection('board').createIndex( { "location": "2dsphere" } );
	var location = message_info.location;
	var point = {"type": "Point",
					"coordinates": [location.lat, location.lon]};
	db.collection('board').insertOne( {
		"location" : point;
		"board": [message_info.content];
	});
};


var findBoards = function(db, request_info callback) {
	var location = message_info.location;
	var point = {"type": "Point",
					"coordinates": [location.lat, location.lon]};
   db.runCommand({geoNear: db.collection('board'),
   					near: point,
   					spherical: true,
   					maxDistance: 1000 });
   cursor.each(function(err, doc) {
      assert.equal(err, null);
      if (doc != null) {
         console.dir(doc);
      } else {
         callback();
      }
   });
};

// MongoClient.connect(url, function(err, db) {
// 	assert.equal(null, err);
// 	insertBoard(db, message_info, function() {
// 		db.close();
// 	});
// });