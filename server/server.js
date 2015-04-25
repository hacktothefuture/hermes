var days_to_expiration = 7;
var http = require("http");
var express = require('express');

var app = express();

var MongoClient = require('mongodb').MongoClient;
var assert = require('assert');
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
		"location" : point,
		"board": [message_info.content]
	});
};


var findBoards = function(db, request_info, callback) {
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

/*
http.createServer( function(request, response) {
	var pathname = require('url').parse(request.url).pathname;
	if( pathname == '/get_messages' && request.method == 'GET' ) {
	} else if ( pathname == '/send_message' && request.method == 'POST' ) {
	}
	
	response.writeHead(200, {"Content-Type": "text/plain"});
	response.write("Hello, World!");
	response.end();
}).listen(8888);
*/

// GET: /hello
app.get( '/hello', function(request, response){
	response.send('Hello, World!');
});

// GET: /get_messages
app.get( '/get_messages', function(request, response){
	var parsed_url = require('url').parse(request.url, true);
	var query = parsed_url.query;
	console.log('Received lat: ' + query.lat + ', lon: ' + query.lon);
	//response.send('Received lat: ' + query.lat + ', lon: ' + query.lon);
	var test_data = [
		{
			location : {
				type : "Point",
				coordinates : [45, 45]
			},
			board : ["test message"],
			board_id : "696969696969"
		}
	];
	response.send(test_data);
});

// POST: /send_message
app.post( '/send_message', function(request, response){
});

var server = app.listen(8888, function() {
	var host = server.address().address;
	var port = server.address().port;
	
	console.log("Listening at http://%s:%s", host, port);
});
