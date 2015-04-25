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

var appendBoard = function(db, message_info, callback) {
	console.log("POST RECEIVED");
	console.dir(message_info);
	var ObjectID = require('mongodb').ObjectID;
	object_id = new ObjectID(message_info.board_id);
	db.collection('boards').update(
		{ '_id' : object_id },
		{
			$push : {
				'board' : message_info.content
			}
		}
	);
};

var insertBoard = function(db, message_info, callback) {
	expiration_date = new Date(Date.UTC()+(86400 * 1000 * days_to_expiration));
	db.collection('boards').ensureIndex( { location: "2dsphere" } );
	var location = message_info.location;
	console.dir(location.coordinates);
	var point = {
		"type": "Point",
		"coordinates": [location.coordinates[1], location.coordinates[0]]
	};
	
	if( typeof message_info.key == 'undefined' ) {
		message_info.key = "";
	}
	
	db.collection('boards').insert( {
		"location" : point,
		"key" : message_info.key,
		"board": [message_info.content]
	});
};


var findBoards = function(db, message_info, callback) {
	var location = message_info.location;
	var point = {
		"type": "Point",
		"coordinates": [location.lon, location.lat]
	};
	if( typeof message_info.keys == 'undefined' ) {
		message_info.keys = [];
	}
	message_info.keys.push("");
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
	db.collection('boards').find({location : {$near: {$geometry : point}}, key : { $in : message_info.keys }}).toArray(function(err, docs) {
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
	if( typeof query.keys == 'undefined' ) {
		query.keys = [""];
	} else {
		query.keys = query.keys.split(",");
	}
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
		findBoards(db, { location : { lat : parseFloat(query.lat), lon : parseFloat(query.lon) }, keys: query.keys }, function(docs) {
			response.send(docs);
			db.close();
		});
	});
});

// POST: /reply_message
app.post( '/append_message', function(request, response){
	MongoClient.connect(url, function(err, db) {
		assert.equal(null, err);
		console.log("Connect correctly to server.");
		appendBoard(db, request.body, function() {
			db.close();
		});
	});
	response.send(null);
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
	response.send(null);
});

var server = app.listen(8888, function() {
	var host = server.address().address;
	var port = server.address().port;
	
	console.log("Listening at http://%s:%s", host, port);
	
	MongoClient.connect(url, function(err, db) {
		assert.equal(null, err);
		console.log("Connected correctly to server.");
		db.collection('boards').count(function(err, count) {
			if( !err && count === 0 ) {
				db.collection('boards').createIndex( { location: "2dsphere" } );
				
				var point = {
					"type": "Point",
					"coordinates": [0, 0]
				};
				
				db.collection('boards').insert({
					"location" : point,
					"key" : "first",
					"board": ["Congratulations, you won the game"]
				});
				console.log("DB population complete");
			} else {
				console.dir(err);
				console.log("DB already populated");
			}
			
			db.close();
		});
	});
});
