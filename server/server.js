var days_to_expiration = 7;
var http = require("http");
var express = require('express');
var bodyParser = require('body-parser');

var app = express();

var MongoClient = require('mongodb').MongoClient;
var assert = require('assert');
var url = 'mongodb://localhost:27017/hermes';

/*
MongoClient.connect(url, function(err, db) {
  assert.equal(null, err);
  console.log("Connected correctly to server.");
  db.close();
});
*/

var insertBoard = function(db, message_info, callback) {
	expiration_date = new Date(Date.UTC()+(86400 * 1000 * days_to_expiration));
	db.collection('boards').ensureIndex( { location: "2dsphere" } );
	var location = message_info.location;
	var point = {
		"type": "Point",
		"coordinates": location.coordinates};
	
	db.collection('boards').insert( {
		"location" : point,
		"board": [message_info.content]
	});
};


var findBoards = function(db, message_info, callback) {
	var location = message_info.location;
	var point = {
		"type": "Point",
		"coordinates": [location.lat, location.lon]
	};
	/*
	db.runCommand({geoNear: 'boards',
		near: point,1
		spherical: true,
		maxDistance: 5000 
	});
	var results = [];
	cursor.each(function(err, doc) {
		assert.equal(err, null);
		if (doc != null) {
			console.dir(doc);
			results.push(doc);
		} else {
			callback();
		}
	});
	return results;
	*/
	db.collection('boards').find({location : {$near: {$geometry : point}}}).toArray(function(err, docs) {
		if( err ) {
			return console.dir(err);
		}
		
		console.dir(docs);
		callback(docs);
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

app.use(bodyParser.json());

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
	/*
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
	*/
	MongoClient.connect(url, function(err, db) {
		assert.equal(null, err);
		console.log("Connected correctly to server.");
		findBoards(db, { location : { lat : parseFloat(query.lat), lon : parseFloat(query.lon) } }, function(docs) {
			response.send(docs);
			db.close();
		});
	});
});

// POST: /send_message
app.post( '/send_message', function(request, response){
	console.log("Received POST");
	console.log("request:");
	console.log(request.body);
	MongoClient.connect(url, function(err, db) {
		assert.equal(null, err);
		console.log("Connected correctly to server.");
		insertBoard(db, request.body, function() {
			db.close();
		});
	});
	response.sendStatus(200);
});

var server = app.listen(8888, function() {
	var host = server.address().address;
	var port = server.address().port;
	
	console.log("Listening at http://%s:%s", host, port);
});
