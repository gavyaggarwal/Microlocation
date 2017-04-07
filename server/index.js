var express = require('express');
var app = express();
var expressWs = require('express-ws')(app);

var location = {
  locations: [
    {
        device: "A",
        x: 0,
        y: 0,
        z: 1,
    },
    {
        device: "B",
        x: 1,
        y: 0,
        z: 0,
    },
    {
        device: "C",
        x: 0,
        y: 0,
        z: -1,
    },
    {
        device: "D",
        x: -1,
        y: 0,
        z: 0,
    },
    {
        device: "E",
        x: 0,
        y: 0.5,
        z: 0,
    }
  ],
  debug: [
    {
        device: "A",
        field: "RSSI Value",
        value: 12
    },
    {
        device: "C",
        field: "Accelerometer X",
        value: 8
    }
  ]
}

function noise() {
    debug[0].value += Math.random() - 0.55;
    debug[1].value += Math.random() - 0.49;
    locations[4].x += Math.random() * 0.1 - 0.05;
    locations[4].y += Math.random() * 0.1 - 0.05;
    locations[4].z += Math.random() * 0.1 - 0.05;
}


app.get('/', function(req, res){
  res.send('hello world');
});

app.ws('/socket', function(ws, req) {

  console.log("New connection has opened!");

  //send updated information every 100 ms
  setTimeout(function(){
    noise();
    ws.send(JSON.stringify(location));
  }, 100);

  ws.on('close', function() {
      console.log('The connection was closed!');
  });

  ws.on('message', function(msg) {
    console.log(msg);
  });
  //console.log('socket', req);

});

app.listen(process.env.PORT || 3000);
